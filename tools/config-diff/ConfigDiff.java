import gg.auroramc.aurora.api.config.ConfigLoader;
import gg.auroramc.aurora.api.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Compares the old config loader (ConfigManager) with the new one (ConfigLoader) on real config files, so converting a
 * plugin's config classes can't silently change what a server's files load to.
 * <p>
 * usage: java -cp <Aurora jar>:<paper-api jar and its libraries>:<plugin jar> ConfigDiff.java <list file>
 * <p>
 * Each line of the list file: {@code <config class> <yaml file> [<bundled defaults file>]}. The class needs a
 * constructor without arguments, and its migrations (if any) in a static {@code migrations()} method returning
 * {@code List<Consumer<YamlConfiguration>>}. Lines starting with # are skipped.
 * <p>
 * For each line, after running the migrations on the file:
 * <ul>
 *     <li>LOADER: the old and the new loader on the file alone. Must be equal, except where the old one gave -1 for a
 *     missing number (the new one keeps the field's default).</li>
 *     <li>DEFAULTS: the new loader on the file alone and with the bundled file under it (what ConfigFile does): where
 *     a server's file misses a key, the bundled value is used now. Listed for review.</li>
 * </ul>
 * Exits with 1 if any LOADER difference is found.
 */
public class ConfigDiff {
    public static void main(String[] args) throws Exception {
        int loaderDiffs = 0, defaultDiffs = 0, checked = 0;
        for (var line : Files.readAllLines(Path.of(args[0]))) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var parts = line.split("\\s+");
            var type = Class.forName(parts[0]);
            var file = new File(parts[1]);
            // Read back like the server's next start does: a migration can set plain Java maps, which only become
            // sections in the saved file (the old loader skipped those on the start that migrated)
            var yaml = reload(migrate(type, YamlConfiguration.loadConfiguration(file)));
            var problems = new ArrayList<String>();

            var old = ConfigManager.load(newInstance(type), yaml);
            var fresh = ConfigLoader.load(type, file.getName(), List.of(yaml), problems::add);
            var diffs = new ArrayList<String>();
            compare("", dump(old), dump(fresh), diffs);
            diffs.removeIf(d -> d.contains("old=-1 ") || d.contains("old=-1.0 ") || d.contains("old=-1.0f "));
            checked++;
            for (var d : diffs) System.out.println("LOADER   " + type.getSimpleName() + " " + file + ": " + d);
            loaderDiffs += diffs.size();
            for (var p : problems) System.out.println("PROBLEM  " + p);

            if (parts.length > 2) {
                var defaults = reload(migrate(type, YamlConfiguration.loadConfiguration(new File(parts[2]))));
                var layered = ConfigLoader.load(type, file.getName(), List.of(defaults, yaml), p -> {
                });
                var defaultsDiff = new ArrayList<String>();
                compare("", dump(fresh), dump(layered), defaultsDiff);
                for (var d : defaultsDiff) System.out.println("DEFAULTS " + type.getSimpleName() + " " + file + ": " + d);
                defaultDiffs += defaultsDiff.size();
            }
        }
        System.out.println(checked + " files checked, " + loaderDiffs + " loader differences, " + defaultDiffs + " values now from the bundled defaults");
        System.exit(loaderDiffs > 0 ? 1 : 0);
    }

    @SuppressWarnings("unchecked")
    private static YamlConfiguration migrate(Class<?> type, YamlConfiguration yaml) {
        try {
            var method = type.getDeclaredMethod("migrations");
            method.setAccessible(true);
            var steps = (List<Consumer<YamlConfiguration>>) method.invoke(null);
            var from = yaml.getInt("config-version", 0);
            if (from < steps.size()) steps.subList(from, steps.size()).forEach(step -> step.accept(yaml));
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            System.out.println("MIGRATE  " + type.getSimpleName() + ": " + e);
        }
        return yaml;
    }

    private static YamlConfiguration reload(YamlConfiguration yaml) throws Exception {
        var read = new YamlConfiguration();
        read.loadFromString(yaml.saveToString());
        return read;
    }

    private static Object newInstance(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    // A comparable tree: maps by key, sets sorted, objects as their fields by YAML key
    private static Object dump(Object value) {
        if (value == null) return null;
        if (value instanceof String || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>) return value;
        if (value instanceof Number n) return n.doubleValue() == Math.rint(n.doubleValue()) && !(n instanceof Double || n instanceof Float) ? n.longValue() : n.doubleValue();
        if (value instanceof ConfigurationSection section) return dump(section.getValues(false));
        if (value instanceof Map<?, ?> map) {
            var tree = new TreeMap<String, Object>();
            map.forEach((k, v) -> tree.put(String.valueOf(k), dump(v)));
            return tree;
        }
        if (value instanceof Set<?> set) return set.stream().map(ConfigDiff::dump).map(String::valueOf).sorted().collect(Collectors.toList());
        if (value instanceof Collection<?> list) return list.stream().map(ConfigDiff::dump).collect(Collectors.toList());
        if (value.getClass().getName().startsWith("java.") || value.getClass().getName().startsWith("org.bukkit.")) return String.valueOf(value);
        var tree = new TreeMap<String, Object>();
        for (var field : ConfigLoader.fields(value.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                tree.put(ConfigLoader.key(field), dump(field.get(value)));
            } catch (IllegalAccessException e) {
                tree.put(ConfigLoader.key(field), "<" + e + ">");
            }
        }
        return tree;
    }

    @SuppressWarnings("unchecked")
    private static void compare(String path, Object a, Object b, List<String> diffs) {
        if (Objects.equals(a, b)) return;
        if (a instanceof Map && b instanceof Map) {
            var keys = new TreeSet<String>();
            keys.addAll(((Map<String, Object>) a).keySet());
            keys.addAll(((Map<String, Object>) b).keySet());
            for (var key : keys) {
                compare(path.isEmpty() ? key : path + "." + key, ((Map<String, Object>) a).get(key), ((Map<String, Object>) b).get(key), diffs);
            }
            return;
        }
        diffs.add((path.isEmpty() ? "<root>" : path) + ": old=" + a + " new=" + b);
    }
}
