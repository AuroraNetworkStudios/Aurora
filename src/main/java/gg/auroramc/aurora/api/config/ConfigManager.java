package gg.auroramc.aurora.api.config;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Load and save YamlConfigurations in a Paper plugin
 */
public final class ConfigManager {

    /**
     * Loads the configuration values from a ConfigurationSection into the provided object.
     *
     * @param config  The object to load the configuration values into.
     * @param section The ConfigurationSection containing the values to load.
     */
    public static <T> T load(T config, ConfigurationSection section) {
        Field[] fields = config.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IgnoreField.class)) continue;

            field.setAccessible(true);
            String key = serializeKey(field.getName());

            try {
                if (Map.class.isAssignableFrom(field.getType())) {
                    handleMapLoading(field, config, section.getConfigurationSection(key));
                } else if (List.class.isAssignableFrom(field.getType())) {
                    handleListLoading(field, config, section, key);
                } else if (Set.class.isAssignableFrom(field.getType())) {
                    handleSetLoading(field, config, section, key);
                } else if (isPrimitiveOrWrapper(field.getType())) {
                    Object value = loadPrimitiveType(config, field, section, key);
                    field.set(config, value);
                } else if (ConfigurationSection.class.isAssignableFrom(field.getType())) {
                    field.set(config, section.getConfigurationSection(key));
                } else {
                    // Handle custom classes as nested sections
                    ConfigurationSection nestedSection = section.getConfigurationSection(key);
                    if (nestedSection != null) {
                        Object nestedObject = field.getType().newInstance();
                        load(nestedObject, nestedSection);
                        field.set(config, nestedObject);
                    }
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    private static void handleMapLoading(Field field, Object config, ConfigurationSection section) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) return;

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length != 2) return;

        // Determine the types of the keys and values of the map
        Class<?> keyType = (Class<?>) typeArguments[0];
        Class<?> valueType = (Class<?>) typeArguments[1];

        Map<Object, Object> map = new HashMap<>();

        if (section == null) {
            try {
                field.setAccessible(true);
                if (field.get(config) == null) field.set(config, map);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return;
        }

        for (String keyString : section.getKeys(false)) {
            Object mapKey = convertKey(keyType, keyString);
            if (mapKey == null) continue;

            Object mapValue;
            if (isPrimitiveOrWrapper(valueType)) {
                mapValue = getPrimitive(valueType, section, keyString);
            } else if(ConfigurationSection.class.isAssignableFrom(valueType)) {
                mapValue = section.getConfigurationSection(keyString);
            } else {
                // If it's a custom class, we assume it has a no-arg constructor and use reflection to instantiate it.
                ConfigurationSection subSection = section.getConfigurationSection(keyString);
                if (subSection != null) {
                    try {
                        mapValue = valueType.getDeclaredConstructor().newInstance();
                        load(mapValue, subSection);
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                        continue;
                    }
                } else {
                    continue;
                }
            }
            map.put(mapKey, mapValue);
        }

        try {
            field.setAccessible(true);
            field.set(config, map);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ConfigurationSection deepMapToConfigurationSection(Map<?, ?> map) {
        ConfigurationSection section = new MemoryConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                section.set(entry.getKey().toString(), deepMapToConfigurationSection((Map<?, ?>) entry.getValue()));
            } else {
                section.set(entry.getKey().toString(), entry.getValue());
            }
        }
        return section;
    }

    private static void handleListLoading(Field field, Object config, ConfigurationSection section, String key) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<?> listSection = section.getList(key);
        if (listSection == null) {
            if (field.get(config) == null) field.set(config, new ArrayList<>());
            return;
        }

        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) return;

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        if (!(typeArgument instanceof Class<?>)) return;
        Class<?> itemType = (Class<?>) typeArgument;

        List<Object> list = new ArrayList<>();
        for (Object elem : listSection) {
            if (elem instanceof Map) {
                // Handle complex object types
                Map<?, ?> subSection = (Map<?, ?>) elem;
                Object item = itemType.getDeclaredConstructor().newInstance();
                var ss = deepMapToConfigurationSection(subSection);

                load(item, ss);

                list.add(item);
            } else if (isPrimitiveOrWrapper(itemType)) {
                // Directly add primitives or their wrappers if elem matches expected type
                list.add(loadPrimitiveType(itemType, elem));
            }
        }
        field.set(config, list);
    }

    private static void handleSetLoading(Field field, Object config, ConfigurationSection section, String key) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<?> listSection = section.getList(key);
        if (listSection == null) {
            if (field.get(config) == null) field.set(config, new HashSet<>());
            return;
        }

        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) return;

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        if (!(typeArgument instanceof Class<?>)) return;
        Class<?> itemType = (Class<?>) typeArgument;

        Set<Object> list = new HashSet<>();
        for (Object elem : listSection) {
            if (elem instanceof Map) {
                // Handle complex object types
                Map<?, ?> subSection = (Map<?, ?>) elem;
                Object item = itemType.getDeclaredConstructor().newInstance();
                var ss = deepMapToConfigurationSection(subSection);

                load(item, ss);

                list.add(item);
            } else if (isPrimitiveOrWrapper(itemType)) {
                // Directly add primitives or their wrappers if elem matches expected type
                list.add(loadPrimitiveType(itemType, elem));
            }
        }
        field.set(config, list);
    }


    public static String serializeKey(String fieldName) {
        return fieldName.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase(Locale.ROOT);
    }

    private static Object loadPrimitiveType(Object config, Field field, ConfigurationSection section, String key) throws IllegalAccessException {
        var type = field.getType();
        if (!section.contains(key) && field.get(config) != null) {
            if (type == int.class || type == double.class || type == long.class || type == float.class)
                return -1;
            return field.get(config);
        }
        return getPrimitive(type, section, key);
    }

    @Nullable
    public static Object getPrimitive(Class<?> type, ConfigurationSection section, String key) {
        if (type == Integer.class || type == int.class) {
            return section.contains(key) || type == int.class ? section.getInt(key, -1) : null;
        } else if (type == Double.class || type == double.class) {
            return section.contains(key) || type == double.class ? section.getDouble(key, -1) : null;
        } else if (type == Boolean.class || type == boolean.class) {
            return section.contains(key) || type == boolean.class ? section.getBoolean(key) : null;
        } else if (type == Long.class || type == long.class) {
            return section.contains(key) || type == long.class ? section.getLong(key) : null;
        } else if (type == Float.class || type == float.class) {
            return section.contains(key) || type == float.class ? (float) section.getDouble(key, -1) : null;
        } else if (type == String.class) {
            return section.getString(key);
        } else if (type == Character.class || type == char.class) {
            String str = section.getString(key);
            return str != null && !str.isEmpty() ? str.charAt(0) : null;
        }
        return null;
    }

    private static Object loadPrimitiveType(Class<?> type, Object value) {
        if (value == null) return null;
        return type.cast(value);
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || type == Double.class || type == Float.class ||
                type == Long.class || type == Integer.class || type == Short.class ||
                type == Character.class || type == Byte.class || type == Boolean.class || type == String.class;
    }

    private static Object convertKey(Class<?> keyType, String key) {
        if (keyType == String.class) {
            return key;
        } else if (keyType == Integer.class || keyType == int.class) {
            return Integer.parseInt(key);
        } else if (keyType == Long.class || keyType == long.class) {
            return Long.parseLong(key);
        }
        return null; // Extend as needed
    }

    private static Map<String, Object> deepMapObjectToMap(Object object) {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            var key = serializeKey(field.getName());
            try {
                if (isPrimitiveOrWrapper(field.getType())) {
                    map.put(key, field.get(object));
                } else if (Set.class.isAssignableFrom(field.getType())) {
                    Set<?> list = (Set<?>) field.get(object);
                    if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) continue;

                    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                    if (!(typeArgument instanceof Class<?>)) continue;
                    Class<?> itemType = (Class<?>) typeArgument;
                    if (isPrimitiveOrWrapper(itemType)) {
                        map.put(key, list);
                    } else {
                        List<Map<String, Object>> complexList = new ArrayList<>();
                        for (Object elem : list) {
                            if (elem != null) {
                                complexList.add(deepMapObjectToMap(elem));
                            }
                        }
                        map.put(key, complexList);
                    }
                } else if (List.class.isAssignableFrom(field.getType())) {
                    List<?> list = (List<?>) field.get(object);
                    if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) continue;

                    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                    if (!(typeArgument instanceof Class<?>)) continue;
                    Class<?> itemType = (Class<?>) typeArgument;
                    if (isPrimitiveOrWrapper(itemType)) {
                        map.put(key, list);
                    } else {
                        List<Map<String, Object>> complexList = new ArrayList<>();
                        for (Object elem : list) {
                            if (elem != null) {
                                complexList.add(deepMapObjectToMap(elem));
                            }
                        }
                        map.put(key, complexList);
                    }
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    Map<?, ?> mapObject = (Map<?, ?>) field.get(object);
                    Map<String, Object> serializedMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : mapObject.entrySet()) {
                        if (isPrimitiveOrWrapper(entry.getValue().getClass())) {
                            serializedMap.put(entry.getKey().toString(), entry.getValue());
                        } else {
                            serializedMap.put(entry.getKey().toString(), deepMapObjectToMap(entry.getValue()));
                        }
                    }
                    map.put(key, serializedMap);
                } else {
                    map.put(key, deepMapObjectToMap(field.get(object)));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Recursively saves the values of the provided object into a ConfigurationSection.
     *
     * @param config  The object to save the values from.
     * @param section The ConfigurationSection to store the values in.
     */
    public static void saveObject(Object config, ConfigurationSection section) {
        // Get all the declared fields of the object's class
        Field[] fields = config.getClass().getDeclaredFields();

        // Iterate over each field
        for (Field field : fields) {
            if (field.isAnnotationPresent(IgnoreField.class)) continue;
            String key = serializeKey(field.getName());

            try {
                // Allow access to private fields
                field.setAccessible(true);
                // Get the value of the field from the object
                Object fieldValue = field.get(config);
                // Skip null values
                if (fieldValue == null) continue;

                if (isPrimitiveOrWrapper(field.getType())) {
                    section.set(key, fieldValue);
                } else if (List.class.isAssignableFrom(field.getType())) {
                    List<?> list = (List<?>) fieldValue;
                    // Check if the list's generic type is primitive or wrapper
                    if (!list.isEmpty() && isPrimitiveOrWrapper(list.get(0).getClass())) {
                        section.set(key, list); // Directly set primitive lists
                    } else {
                        // Handle list of complex objects
                        List<Map<String, Object>> complexList = new ArrayList<>();
                        for (Object elem : list) {
                            if (elem != null) {
                                complexList.add(deepMapObjectToMap(elem));
                            }
                        }
                        section.set(key, complexList); // Set the serialized list of maps
                    }
                } else if (Set.class.isAssignableFrom(field.getType())) {
                    Set<?> set = (Set<?>) fieldValue;
                    // Check if the list's generic type is primitive or wrapper
                    if (!set.isEmpty() && isPrimitiveOrWrapper(set.stream().findFirst().get().getClass())) {
                        section.set(key, new ArrayList<>(set)); // Directly set primitive lists
                    } else {
                        // Handle list of complex objects
                        List<Map<String, Object>> complexList = new ArrayList<>();
                        for (Object elem : set) {
                            if (elem != null) {
                                complexList.add(deepMapObjectToMap(elem));
                            }
                        }
                        section.set(key, complexList); // Set the serialized list of maps
                    }
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    Map<?, ?> map = (Map<?, ?>) fieldValue;
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String mapKey = String.valueOf(entry.getKey());
                        Object mapValue = entry.getValue();
                        if (isPrimitiveOrWrapper(mapValue.getClass())) {
                            section.set(key + "." + mapKey, mapValue);
                        } else {
                            ConfigurationSection mapSection = section.createSection(key + "." + mapKey);
                            saveObject(mapValue, mapSection);
                        }
                    }
                } else if (ConfigurationSection.class.isAssignableFrom(field.getType())) {
                    section.set(key, fieldValue);
                } else {
                    // For nested objects, create a new ConfigurationSection and recursively save the object
                    ConfigurationSection nestedSection = section.createSection(key);
                    saveObject(fieldValue, nestedSection);
                }


            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the configuration values from the provided object to a YamlConfiguration and writes it to a file.
     *
     * @param config The object containing the configuration values.
     * @param yaml   The YamlConfiguration to store the values in.
     * @param file   The file to write the configuration to.
     */
    public static void save(Object config, YamlConfiguration yaml, File file) {
        // Save the object's values to the YamlConfiguration
        saveObject(config, yaml);

        try {
            // Save the YamlConfiguration to the file
            yaml.save(file);
        } catch (IOException e) {
            Aurora.getPlugin(Aurora.class).getLogger().warning("Failed to save " + file.getAbsolutePath());
        }
    }


    /**
     * Synchronizes the old configuration to match the structure and keys of the new configuration.
     * This method adds missing keys from the new configuration and removes keys not present in the new configuration from the old one.
     *
     * @param oldConfig The old configuration to be updated.
     * @param newConfig The new configuration to synchronize against.
     */
    public static void syncConfigurations(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        // First, remove keys from oldConfig that are not in newConfig
        removeExtraKeys(oldConfig, newConfig);

        // Second, add keys from newConfig that are not in oldConfig
        addMissingKeys(oldConfig, newConfig);
    }

    private static void removeExtraKeys(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        oldConfig.getKeys(true).forEach(key -> {
            if (!newConfig.contains(key)) {
                // Remove the key from the oldConfig if it doesn't exist in newConfig
                oldConfig.set(key, null);
            }
        });
    }

    private static void addMissingKeys(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        for (String key : newConfig.getKeys(false)) {
            if (!oldConfig.contains(key)) {
                // Add the key from newConfig to oldConfig if it's missing
                oldConfig.set(key, newConfig.get(key));
            } else if (newConfig.isConfigurationSection(key) && oldConfig.isConfigurationSection(key)) {
                // If both keys are configuration sections, recurse into them
                addMissingKeys(oldConfig.getConfigurationSection(key), newConfig.getConfigurationSection(key));
            }
        }
    }
}
