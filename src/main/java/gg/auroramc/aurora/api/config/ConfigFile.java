package gg.auroramc.aurora.api.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * One config file of a plugin, loaded into a config object (see {@link ConfigLoader}).
 * <p>
 * Loading:
 * <ol>
 *     <li>the file is created from the plugin's bundled copy if it doesn't exist,</li>
 *     <li>the migrations run on the raw YAML, from the file's {@code config-version} on (each step sets the version
 *     itself), and the file is saved if any ran,</li>
 *     <li>the config object is built with the bundled copy (if there is one) as the defaults under the file.</li>
 * </ol>
 * {@link #reload()} builds a new object and swaps it in at once, {@link #get()} never sees a half loaded one.
 */
public final class ConfigFile<T> {
    private static final Logger LOGGER = Logger.getLogger("Aurora");

    private final Plugin plugin;
    private final String path;
    private final Class<T> type;
    private final List<Consumer<YamlConfiguration>> migrations;
    private final @Nullable String bundled;
    private final List<Supplier<? extends ConfigurationSection>> extraDefaults;
    private volatile T current;
    private volatile YamlConfiguration yaml = new YamlConfiguration();

    private ConfigFile(Builder<T> builder) {
        this.plugin = builder.plugin;
        this.path = builder.path;
        this.type = builder.type;
        this.migrations = List.copyOf(builder.migrations);
        this.bundled = builder.bundled;
        this.extraDefaults = List.copyOf(builder.extraDefaults);
    }

    /**
     * @param path the file's path in the plugin's data folder, also where its bundled copy is in the jar
     */
    public static <T> Builder<T> builder(Plugin plugin, String path, Class<T> type) {
        return new Builder<>(plugin, path, type);
    }

    public static final class Builder<T> {
        private final Plugin plugin;
        private final String path;
        private final Class<T> type;
        private final List<Consumer<YamlConfiguration>> migrations = new ArrayList<>();
        private @Nullable String bundled;
        private final List<Supplier<? extends ConfigurationSection>> extraDefaults = new ArrayList<>();

        private Builder(Plugin plugin, String path, Class<T> type) {
            this.plugin = plugin;
            this.path = path;
            this.type = type;
            this.bundled = path;
        }

        /**
         * The migration steps, by {@code config-version}: step 0 upgrades version 0, and so on. Steps are only ever
         * appended. Each step sets {@code config-version} itself.
         */
        public Builder<T> migrations(List<Consumer<YamlConfiguration>> steps) {
            migrations.addAll(steps);
            return this;
        }

        /**
         * Where the bundled copy is in the jar, if not at the same path. Null for none: then the file isn't created
         * and missing keys keep the fields' defaults.
         */
        public Builder<T> bundled(@Nullable String resourcePath) {
            this.bundled = resourcePath;
            return this;
        }

        /**
         * Other values that count as defaults for this file, under the file but over the bundled copy, like shared
         * menu defaults for each workbench. Asked again on every reload.
         */
        public Builder<T> defaults(Supplier<? extends ConfigurationSection> defaults) {
            extraDefaults.add(defaults);
            return this;
        }

        public ConfigFile<T> build() {
            return new ConfigFile<>(this);
        }
    }

    /**
     * The server's file as it was last loaded (after its migrations), for reading what the config object doesn't
     * have. Don't change it.
     */
    public YamlConfiguration getYaml() {
        get();
        return yaml;
    }

    public File getFile() {
        return new File(plugin.getDataFolder(), path);
    }

    /**
     * The loaded config, loading it the first time.
     */
    public T get() {
        var config = current;
        if (config == null) {
            synchronized (this) {
                if (current == null) current = read(true);
                config = current;
            }
        }
        return config;
    }

    /**
     * Reads the file again and swaps the new config in.
     */
    public synchronized T reload() {
        current = read(false);
        return current;
    }

    private T read(boolean first) {
        var file = getFile();
        var defaults = bundledYaml();
        if (!file.exists() && bundled != null) {
            // To this file's path, the bundled copy may be another resource
            try (var in = plugin.getResource(bundled)) {
                if (in != null) {
                    file.getParentFile().mkdirs();
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                LOGGER.severe("Failed to create " + path + ": " + e.getMessage());
            }
        }
        var yaml = new YamlConfiguration();
        if (file.exists()) {
            try {
                yaml.load(file);
                // Only a file that was read: migrating an empty stand-in would save it over the server's file
                migrate(yaml, file);
            } catch (IOException | InvalidConfigurationException e) {
                // Loading the defaults instead would make a typo silently reset everything
                LOGGER.severe(path + " can't be read, " + (first || current == null ? "using the defaults" : "keeping the config from before")
                        + " until it's fixed. Error: " + e.getMessage());
                if (!first && current != null) return current;
                yaml = new YamlConfiguration();
            }
        }
        var layers = new ArrayList<ConfigurationSection>();
        if (defaults != null) layers.add(defaults);
        for (var extra : extraDefaults) {
            var section = extra.get();
            if (section != null) layers.add(section);
        }
        layers.add(yaml);
        this.yaml = yaml;
        return ConfigLoader.load(type, path, layers, LOGGER::warning);
    }

    private void migrate(YamlConfiguration yaml, File file) {
        if (migrations.isEmpty()) return;
        var from = yaml.getInt("config-version", 0);
        if (from >= migrations.size()) return;
        for (var step : migrations.subList(from, migrations.size())) step.accept(yaml);
        try {
            yaml.save(file);
        } catch (IOException e) {
            LOGGER.severe("Failed to save " + path + " after updating it: " + e.getMessage());
        }
    }

    @Nullable
    private YamlConfiguration bundledYaml() {
        if (bundled == null) return null;
        try (var in = plugin.getResource(bundled)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Changes one value in the file and saves it, keeping the rest of the file as it is (comments, quotes, layout)
     * where possible, then reloads the config.
     *
     * @param key the dotted path of the value
     */
    public synchronized void set(String key, Object value) {
        var file = getFile();
        try {
            if (!YamlEditor.setScalar(file, key, value)) {
                // Not a plain value on its own line: save it the normal way
                var yaml = new YamlConfiguration();
                if (file.exists()) yaml.load(file);
                yaml.set(key, value);
                yaml.save(file);
            }
        } catch (IOException | InvalidConfigurationException e) {
            // Never saved over a file that can't be read, that would lose everything else in it
            LOGGER.severe("Failed to save " + key + " in " + path + ": " + e.getMessage());
        }
        reload();
    }
}
