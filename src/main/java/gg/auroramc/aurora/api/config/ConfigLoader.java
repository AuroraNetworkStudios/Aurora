package gg.auroramc.aurora.api.config;

import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import gg.auroramc.aurora.api.config.decorators.Key;
import gg.auroramc.aurora.api.config.decorators.Range;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Builds config objects from YAML.
 * <p>
 * A config class has a no-argument constructor (it may be private) and fields; what a field holds after construction
 * is its default. Every non-static field of the class and its superclasses is read, except {@link IgnoreField} ones.
 * The YAML key of a field is {@link Key} if given, otherwise its name in kebab-case ({@code customItems} is
 * {@code custom-items}).
 * <p>
 * Values come from layers, later layers override earlier ones, like a plugin's bundled defaults under the server's
 * file. Nested config objects are merged key by key across the layers, so an override only has to list what it
 * changes. Lists, sets, maps and plain values are taken whole from the last layer that has them.
 * <p>
 * A value that can't be used (wrong type, out of {@link Range}) is reported with its file and path, and the value
 * from the layer below is used instead, or the field's default. A config always loads.
 */
public final class ConfigLoader {
    private static final Logger LOGGER = Logger.getLogger("Aurora");
    private static final Map<Class<?>, TypeAdapter<?>> adapters = new ConcurrentHashMap<>();

    private ConfigLoader() {
    }

    /**
     * Lets fields of this type be read from YAML, like a type from another plugin. Adapters registered later replace
     * earlier ones for the same type.
     */
    public static <T> void registerAdapter(Class<T> type, TypeAdapter<T> adapter) {
        adapters.put(type, adapter);
    }

    /**
     * Builds a config object from the layers, later ones overriding earlier ones.
     *
     * @param source a name for problems found, usually the file name
     */
    public static <T> T load(Class<T> type, String source, ConfigurationSection... layers) {
        return load(type, source, Arrays.stream(layers).filter(Objects::nonNull).toList(), LOGGER::warning);
    }

    /**
     * Like {@link #load(Class, String, ConfigurationSection...)}, reporting problems to the given consumer.
     */
    public static <T> T load(Class<T> type, String source, List<ConfigurationSection> layers, Consumer<String> problems) {
        return new Context(source, problems).object(type, layers, "");
    }

    /**
     * The YAML key of a field: {@link Key} if given, otherwise the field's name in kebab-case.
     */
    public static String key(Field field) {
        var key = field.getAnnotation(Key.class);
        return key != null ? key.value() : kebab(field.getName());
    }

    public static String kebab(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase(Locale.ROOT);
    }

    /**
     * The fields the loader reads, in declaration order, superclass fields first.
     */
    public static List<Field> fields(Class<?> type) {
        var chain = new ArrayDeque<Class<?>>();
        for (var c = type; c != null && c != Object.class; c = c.getSuperclass()) chain.push(c);
        var fields = new ArrayList<Field>();
        for (var c : chain) {
            for (var field : c.getDeclaredFields()) {
                var mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic()) continue;
                if (field.isAnnotationPresent(IgnoreField.class)) continue;
                fields.add(field);
            }
        }
        return fields;
    }

    private record Context(String source, Consumer<String> problems) {
        void problem(String path, String message) {
            problems.accept(source + ": " + (path.isEmpty() ? "" : path + ": ") + message);
        }

        <T> T object(Class<T> type, List<ConfigurationSection> layers, String path) {
            T object = instantiate(type, path);
            if (object == null) return null;
            for (var field : fields(type)) {
                field.setAccessible(true);
                var key = key(field);
                var fieldPath = path.isEmpty() ? key : path + "." + key;
                try {
                    var value = field(field, layers, key, fieldPath, field.get(object));
                    if (value != Unset.UNSET) field.set(object, fit(field.getType(), value));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    problem(fieldPath, "can't be set, keeping the default: " + e.getMessage());
                }
            }
            return object;
        }

        @SuppressWarnings("unchecked")
        private <T> T instantiate(Class<T> type, String path) {
            try {
                Constructor<T> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                problem(path, type.getSimpleName() + " can't be created, it needs a constructor without arguments: " + e);
                return null;
            }
        }

        // The field's value, or UNSET to keep its default
        private Object field(Field field, List<ConfigurationSection> layers, String key, String path, Object current) {
            var type = field.getType();
            var generic = field.getGenericType();
            // Nested config objects merge across the layers
            if (isObject(type)) {
                var sections = new ArrayList<ConfigurationSection>();
                for (var layer : layers) {
                    var section = layer.getConfigurationSection(key);
                    if (section != null) sections.add(section);
                }
                if (sections.isEmpty()) return Unset.UNSET;
                var object = object(type, sections, path);
                return object == null ? Unset.UNSET : object;
            }
            // Everything else: the last layer that has a usable value wins
            for (int i = layers.size() - 1; i >= 0; i--) {
                var layer = layers.get(i);
                if (!layer.contains(key, true)) continue;
                var raw = layer.get(key);
                var value = value(type, generic, raw, path, true);
                if (value == Unset.UNSET) continue;
                if (!inRange(field, value, path)) continue;
                return value;
            }
            // Never null, like before 3.0
            if (current == null) {
                if (Set.class.isAssignableFrom(type)) return new LinkedHashSet<>();
                if (List.class.isAssignableFrom(type) || type == Collection.class) return new ArrayList<>();
                if (Map.class.isAssignableFrom(type)) return new LinkedHashMap<>();
            }
            return Unset.UNSET;
        }

        private boolean inRange(Field field, Object value, String path) {
            var range = field.getAnnotation(Range.class);
            if (range == null || !(value instanceof Number number)) return true;
            var d = number.doubleValue();
            if (d >= range.min() && d <= range.max()) return true;
            problem(path, value + " has to be between " + format(range.min()) + " and " + format(range.max()) + ", ignoring it");
            return false;
        }

        private static String format(double d) {
            return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
        }

        // UNSET when it can't be used (reported). A single value (not a list element) converts like before 3.0: a
        // decimal for a whole number is cut, and text takes anything.
        private Object value(Class<?> type, Type generic, Object raw, String path, boolean single) {
            if (raw == null) return null;
            if (type == Object.class) return raw;
            if (type == ConfigurationSection.class) {
                var section = asSection(raw);
                if (section != null) return section;
                problem(path, "has to be a section, ignoring it");
                return Unset.UNSET;
            }
            if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type) || Collection.class == type) {
                return collection(type, generic, raw, path);
            }
            if (Map.class.isAssignableFrom(type)) {
                return map(generic, raw, path);
            }
            if (isObject(type)) {
                var section = asSection(raw);
                if (section == null) {
                    problem(path, "has to be a section, ignoring it");
                    return Unset.UNSET;
                }
                var object = object(type, List.of(section), path);
                return object == null ? Unset.UNSET : object;
            }
            if ((raw instanceof ConfigurationSection || raw instanceof Map) && !isValueType(type) && !type.isEnum() && !adapters.containsKey(type)) {
                problem(path, type.getSimpleName() + " can't be read from a section, it has to be a class with a constructor without arguments, ignoring it");
                return Unset.UNSET;
            }
            var converted = single ? singleScalar(type, raw) : scalar(type, raw);
            if (converted == null) {
                problem(path, "'" + raw + "' isn't " + describe(type) + ", ignoring it");
                return Unset.UNSET;
            }
            return converted;
        }

        private Object collection(Class<?> type, Type generic, Object raw, String path) {
            if (!(raw instanceof List<?> list)) {
                problem(path, "has to be a list, ignoring it");
                return Unset.UNSET;
            }
            var element = typeArgument(generic, 0);
            var elementClass = rawClass(element);
            Collection<Object> result = Set.class.isAssignableFrom(type) ? new LinkedHashSet<>() : new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                var item = list.get(i);
                if (item == null) {
                    // An empty "-": kept for plain values like before, an object or enum can't be null
                    if (isValueType(elementClass)) result.add(null);
                    continue;
                }
                var value = value(elementClass, element, item, path + "[" + i + "]", false);
                if (value == Unset.UNSET) continue;
                result.add(value);
            }
            return result;
        }

        private Object map(Type generic, Object raw, String path) {
            var section = asSection(raw);
            if (section == null) {
                problem(path, "has to be a section, ignoring it");
                return Unset.UNSET;
            }
            var keyType = rawClass(typeArgument(generic, 0));
            var valueType = typeArgument(generic, 1);
            var result = new LinkedHashMap<Object, Object>();
            for (var key : section.getKeys(false)) {
                var entryPath = path + "." + key;
                var mapKey = scalar(keyType, key);
                if (mapKey == null) {
                    problem(entryPath, "'" + key + "' isn't " + describe(keyType) + ", skipping it");
                    continue;
                }
                var value = value(rawClass(valueType), valueType, section.get(key), entryPath, true);
                if (value == Unset.UNSET) continue;
                result.put(mapKey, value);
            }
            return result;
        }

        @Nullable
        private static ConfigurationSection asSection(Object raw) {
            if (raw instanceof ConfigurationSection section) return section;
            if (raw instanceof Map<?, ?> map) {
                var section = new MemoryConfiguration();
                map.forEach((k, v) -> section.set(String.valueOf(k), v instanceof Map<?, ?> nested ? asSection(nested) : v));
                return section;
            }
            return null;
        }
    }

    private enum Unset {UNSET}

    private static Type typeArgument(Type generic, int index) {
        if (generic instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length > index) {
            return parameterized.getActualTypeArguments()[index];
        }
        return Object.class;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType p) return (Class<?>) p.getRawType();
        if (type instanceof WildcardType w && w.getUpperBounds().length > 0) return rawClass(w.getUpperBounds()[0]);
        if (type instanceof TypeVariable<?> v && v.getBounds().length > 0) return rawClass(v.getBounds()[0]);
        return Object.class;
    }

    // A config object read from a section: a plain class with a constructor without arguments, not a value type, a
    // collection, or something with an adapter
    private static boolean isObject(Class<?> type) {
        if (type.isPrimitive() || type.isEnum() || type.isArray() || type.isInterface() || type.isRecord()
                || Modifier.isAbstract(type.getModifiers()) || adapters.containsKey(type) || isValueType(type)
                || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)
                || type == Object.class || ConfigurationSection.class.isAssignableFrom(type)
                || type.getName().startsWith("java.") || type.getName().startsWith("org.bukkit.")) {
            return false;
        }
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // A built list, set or map in the field's own type, if the field wants a specific one (like a TreeMap)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object fit(Class<?> type, Object value) throws IllegalArgumentException {
        if (value == null || type.isInstance(value) || type.isPrimitive()) return value;
        if (!(value instanceof Collection || value instanceof Map) || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return value;
        }
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            var target = constructor.newInstance();
            if (target instanceof Collection c && value instanceof Collection v) c.addAll(v);
            else if (target instanceof Map m && value instanceof Map v) m.putAll(v);
            else return value;
            return target;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalArgumentException(type.getSimpleName() + " can't hold the value: " + e);
        }
    }

    private static boolean isValueType(Class<?> type) {
        var boxed = box(type);
        return boxed == String.class || boxed == Boolean.class || boxed == Character.class || Number.class.isAssignableFrom(boxed);
    }

    private static String describe(Class<?> type) {
        var boxed = box(type);
        if (boxed == Integer.class || boxed == Long.class || boxed == Short.class || boxed == Byte.class) return "a whole number";
        if (Number.class.isAssignableFrom(boxed)) return "a number";
        if (boxed == Boolean.class) return "true or false";
        if (boxed == Character.class) return "a single character";
        if (type.isEnum()) {
            var names = Arrays.stream(type.getEnumConstants()).map(c -> ((Enum<?>) c).name().toLowerCase(Locale.ROOT)).toList();
            return names.size() <= 12 ? "one of " + String.join(", ", names) : "a " + type.getSimpleName() + " (like " + String.join(", ", names.subList(0, 5)) + ")";
        }
        return "a " + type.getSimpleName();
    }

    /**
     * Converts a plain YAML value to the type: numbers, text, true/false, characters, enums (any case, '-' for '_')
     * and types with an adapter. An unquoted number in a text field is that text, and a whole number in a decimal
     * field is that decimal.
     *
     * @return null if it can't be converted
     */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object scalar(Class<?> type, Object value) {
        if (value == null) return null;
        var adapter = adapters.get(type);
        if (adapter != null) {
            try {
                return adapter.read(value);
            } catch (RuntimeException e) {
                return null;
            }
        }
        var boxed = box(type);
        if (boxed == Object.class) return value;
        if (boxed.isInstance(value)) return value;
        if (type.isEnum()) {
            var name = String.valueOf(value).strip().replace('-', '_');
            for (var constant : type.getEnumConstants()) {
                if (((Enum) constant).name().equalsIgnoreCase(name)) return constant;
            }
            return null;
        }
        if (boxed == String.class) {
            return value instanceof Number || value instanceof Boolean || value instanceof Character ? String.valueOf(value) : null;
        }
        if (value instanceof Number number) {
            var integral = boxed == Integer.class || boxed == Long.class || boxed == Short.class || boxed == Byte.class;
            // 1.5 for a whole number is a mistake, not 1
            if (integral && (number instanceof Double || number instanceof Float) && number.doubleValue() % 1 != 0) return null;
            if (boxed == Integer.class) return number.intValue();
            if (boxed == Double.class) return number.doubleValue();
            if (boxed == Long.class) return number.longValue();
            if (boxed == Float.class) return number.floatValue();
            if (boxed == Short.class) return number.shortValue();
            if (boxed == Byte.class) return number.byteValue();
            return null;
        }
        if (value instanceof String text) {
            var t = text.strip();
            try {
                if (boxed == Integer.class) return Integer.parseInt(t);
                if (boxed == Double.class) return Double.parseDouble(t);
                if (boxed == Long.class) return Long.parseLong(t);
                if (boxed == Float.class) return Float.parseFloat(t);
                if (boxed == Short.class) return Short.parseShort(t);
                if (boxed == Byte.class) return Byte.parseByte(t);
            } catch (NumberFormatException e) {
                return null;
            }
            if (boxed == Boolean.class && (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false"))) return Boolean.parseBoolean(t);
            if (boxed == Character.class && text.length() == 1) return text.charAt(0);
        }
        return null;
    }

    // Like scalar, but a decimal for a whole number is cut (1.5 is 1), text takes any plain value or list, and a
    // character takes the first one of a text, as single values loaded before 3.0
    @Nullable
    private static Object singleScalar(Class<?> type, Object value) {
        var boxed = box(type);
        if (value instanceof Number number && !adapters.containsKey(type)) {
            if (boxed == Integer.class) return number.intValue();
            if (boxed == Long.class) return number.longValue();
            if (boxed == Short.class) return number.shortValue();
            if (boxed == Byte.class) return number.byteValue();
        }
        if (boxed == String.class && !adapters.containsKey(type) && !(value instanceof ConfigurationSection) && !(value instanceof Map)) {
            return String.valueOf(value);
        }
        if (boxed == Character.class && value instanceof String text && !text.isEmpty()) return text.charAt(0);
        return scalar(type, value);
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == double.class) return Double.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
