package gg.auroramc.auroralib.config;

import gg.auroramc.auroralib.config.decorators.IClassList;
import gg.auroramc.auroralib.config.decorators.IMapDecor;
import gg.auroramc.auroralib.config.decorators.IYamlMapping;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Load and save YamlConfigurations in a Spigot plugin
 * Was written for the version 1.19.4 but should be compatible with all versions
 */
public final class ConfigManager {

    private static boolean debug = false;
    /**
     * Loads the configuration values from a ConfigurationSection into the provided object.
     *
     * @param config  The object to load the configuration values into.
     * @param section The ConfigurationSection containing the values to load.
     */
    public static void load(Object config, ConfigurationSection section) {
        // Get all the declared fields of the object's class
        Field[] fields = config.getClass().getDeclaredFields();

        // Iterate over each field
        for(Field field : fields) {
            // Skip fields that are not annotated with @IYamlMapping
            if(!field.isAnnotationPresent(IYamlMapping.class)) continue;

            if(field.isAnnotationPresent(IClassList.class)) {
                handleClassListLoading(field, config, section);
                continue;
            }

            if(field.isAnnotationPresent(IMapDecor.class)) {
                handleMapLoading(field, config, section);
                continue;
            }

            // Retrieve the @IYamlMapping annotation of the field
            IYamlMapping mapping = field.getAnnotation(IYamlMapping.class);
            String key = mapping.v();
            if(debug) System.out.println( "trying to load " + key );
            try {
                if(isInteger(field)) {
                    if(debug) System.out.println( " | Given value is an integer" );
                    Object number = section.getInt(key);
                    setValue(field, config, number);
                } else if(isDouble(field)) {
                    if(debug) System.out.println( " | Given value is a Double" );
                    Object number = section.getDouble(key);
                    setValue(field, config, number);
                } else if(isBoolean(field)) {
                    if(debug) System.out.println( " | Given value is a Boolean" );
                    Object number = section.getBoolean(key);
                    setValue(field, config, number);
                } else if(isFloat(field)) {
                    if(debug) System.out.println( " | Given value is a Float" );
                    Object number = ((Double) section.getDouble(key)).floatValue();
                    setValue(field, config, number);
                } else if(isChar(field)) {
                    if(debug) System.out.println( " | Given value is a char" );
                    Object number = section.getString(key);
                    setValue(field, config, number);
                } else if(isList(field)) {
                    if(debug) System.out.println( " | Given value is a List" );
                    Object number = section.getList(key);
                    setValue(field, config, number);
                } else if(isString(field)) {
                    if(debug) System.out.println( " | Given value is a String" );
                    Object number = section.getString(key);
                    setValue(field, config, number);
                } else if(isItemStack(field)) {
                    if(debug) System.out.println( " | Given value is an ItemStack" );
                    Object number = section.getItemStack(key);
                    setValue(field, config, number);
                } else if(isConfigurationSection(field)) {
                    if(debug) System.out.println( " | Given value is a ConfigSection" );
                    Object number = section.getConfigurationSection(key);
                    setValue(field, config, number);
                } else if(isColor(field)) {
                    if(debug) System.out.println( " | Given value is a Color" );
                    Object number = section.getColor(key);
                    setValue(field, config, number);
                } else if(isLocation(field)) {
                    if(debug) System.out.println( " | Given value is a Location" );
                    Object number = section.getLocation(key);
                    setValue(field, config, number);
                } else if(isOfflinePlayer(field)) {
                    if(debug) System.out.println( " | Given value is an OfflinePlayer" );
                    Object number = section.getOfflinePlayer(key);
                    setValue(field, config, number);
                } else if(isVector(field)) {
                    if(debug) System.out.println( " | Given value is a Vector" );
                    Object number = section.getVector(key);
                    setValue(field, config, number);
                } else if(isSet(field)) {
                    if(debug) System.out.println( " | Given value is a Set" );
                    List<?> number = section.getList(key);
                    if(number == null) continue;

                    Set<?> set = new HashSet<>(number);
                    setValue(field, config, set);
                } else {
                    // Get the nested ConfigurationSection using the specified key
                    ConfigurationSection nestedSection = section.getConfigurationSection(key);

                    if(debug) System.out.println(" | Given value is else");

                    // If the nested section exists, create a new instance of the nested object
                    if(nestedSection == null) continue;
                    Object nestedObject = field.getType().newInstance();

                    // Recursively call the load method to load values into the nested object
                    load(nestedObject, nestedSection);

                    // Set the nested object to the field
                    field.setAccessible(true);
                    field.set(config, nestedObject);
                }
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }

    }

    private static void handleMapLoading(Field field, Object config, ConfigurationSection section) {
        // Check if the field is a Map
        if (!Map.class.isAssignableFrom(field.getType())) {
            System.out.println("Found @IYamlMap decorator but the field was not a Map!");
            return;
        }

        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) return;

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length < 2) return;

        Type valueType = ((ParameterizedType) typeArguments[1]).getActualTypeArguments()[0];
        Map<String, List<Object>> map = new HashMap<>();

        for (String key : section.getKeys(false)) {
            List<?> list = section.getList(key);
            List<Object> values = new ArrayList<>();
            if(list == null) continue;
            for (Object obj : list) {
                if (valueType == Number.class && obj instanceof String) {
                    try {
                        values.add(Double.parseDouble((String) obj));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    values.add(obj);
                }
            }
            map.put(key, values);
        }

        try {
            field.setAccessible(true);
            field.set(config, map);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void handleClassListLoading(Field field, Object config, ConfigurationSection section) {
        // we loop each key in section and map it to the class
        if(field.getType() != List.class) {
            System.out.println( " Found @IClassList decorator but the type was not a list! " );
            return;
        }

        Type genericType = field.getGenericType();
        if(!(genericType instanceof ParameterizedType)) return;

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArgument = parameterizedType.getActualTypeArguments();
        if(typeArgument.length == 0) return;
        Class<?> classType = (Class<?>) typeArgument[0];

        List<Object> values = new ArrayList<>();

        for(String key : section.getKeys(false)) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if(subSection == null) continue;

            try {
                Object instance = classType.newInstance();
                load(instance, subSection);
                values.add(instance);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            field.setAccessible(true);
            field.set(config, values);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


    }


    private static boolean isVector(Field field) {
        return field.getType() == Vector.class;
    }
    private static boolean isOfflinePlayer(Field field) {
        return field.getType() == OfflinePlayer.class;
    }
    private static boolean isLocation(Field field) {
        return field.getType() == Location.class;
    }
    private static boolean isColor(Field field) {
        return field.getType() == Color.class;
    }
    private static Boolean isInteger(Field f) {
        if(f.getType() == Integer.class) return true;
        return f.getType() == int.class;
    }
    private static Boolean isDouble(Field f) {
        if(f.getType() == Double.class) return true;
        return f.getType() == double.class;
    }
    private static Boolean isBoolean(Field f) {
        if(f.getType() == Boolean.class) return true;
        return f.getType() == boolean.class;
    }
    private static Boolean isFloat(Field f) {
        if(f.getType() == Float.class) return true;
        return f.getType() == float.class;
    }
    private static Boolean isSet(Field f) {
        return f.getType() == Set.class;
    }
    private static Boolean isConfigurationSection(Field f) {
        return f.getType() == ConfigurationSection.class;
    }
    private static Boolean isItemStack(Field f) {
        return f.getType() == ItemStack.class;
    }
    private static Boolean isChar(Field f) {
        return f.getType() == char.class;
    }
    private static Boolean isList(Field f) {
        return f.getType() == List.class;
    }
    private static Boolean isString(Field f) {
        return f.getType() == String.class;
    }
    private static Boolean isLong(Field f) {
        if(f.getType() == Long.class) return true;
        return f.getType() == long.class;
    }

    private static Boolean isSupported(Field field) {
        return isVector(field) ||
                isOfflinePlayer(field) ||
                isLocation(field) ||
                isColor(field) ||
                isInteger(field) ||
                isDouble(field) ||
                isBoolean(field) ||
                isFloat(field) ||
                isSet(field) ||
                isConfigurationSection(field) ||
                isItemStack(field) ||
                isChar(field) ||
                isList(field) ||
                isString(field) ||
                isLong(field);
    }

    private static void setValue(Field field, Object config, Object number) {
        try {

            field.setAccessible(true);
            field.set(config, number);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively saves the values of the provided object into a ConfigurationSection.
     *
     * @param config  The object to save the values from.
     * @param section The ConfigurationSection to store the values in.
     */
    private static void saveObject(Object config, ConfigurationSection section) {
        // Get all the declared fields of the object's class
        Field[] fields = config.getClass().getDeclaredFields();

        // Iterate over each field
        for(Field field : fields) {
            // Skip fields that are not annotated with @IYamlMapping
            if(!field.isAnnotationPresent(IYamlMapping.class)) continue;
            // Retrieve the @IYamlMapping annotation of the field
            IYamlMapping mapping = field.getAnnotation(IYamlMapping.class);
            String key = mapping.v();

            try {
                // Allow access to private fields
                field.setAccessible(true);
                // Get the value of the field from the object
                Object fieldValue = field.get(config);
                // Skip null values
                if(fieldValue == null) continue;

                if( isSupported(field) ) {
                    section.set(key, fieldValue);
                } else {
                    // For nested objects, create a new ConfigurationSection and recursively save the object
                    ConfigurationSection nestedSecton = section.createSection(key);
                    saveObject(fieldValue, nestedSecton);
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
            e.printStackTrace();
        }
    }
    /**
     * Saves the configuration values from the provided object to a YamlConfiguration and writes it to a file.
     *
     * @param config The object containing the configuration values.
     * @param file   The file to write the configuration to.
     */
    public static void save(Object config, File file) {
        // Save the object's values to the YamlConfiguration
        YamlConfiguration yaml = new YamlConfiguration();
        saveObject(config, yaml);

        try {
            // Save the YamlConfiguration to the file
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean getDebug() {
        return debug;
    }
    public static void setDebug(boolean d) {
        debug = d;
    }
}
