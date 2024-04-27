package config;

import gg.auroramc.auroralib.api.config.ConfigManager;
import gg.auroramc.auroralib.api.config.decorators.IgnoreField;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConfigLoadTest {
    private YamlConfiguration yaml;

    public static class NestedClass {
        public String longNestedName;

        public NestedClass() {}

        public NestedClass(String longNestedName) {
            this.longNestedName = longNestedName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NestedClass that = (NestedClass) o;
            return Objects.equals(longNestedName, that.longNestedName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(longNestedName);
        }
    }

    public static class MapClass {
        public String key1;
        public String key2;
        public NestedClass key3;

        public MapClass() {}

        public MapClass(String key1, String key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        public MapClass(String key1, String key2, NestedClass key3) {
            this.key1 = key1;
            this.key2 = key2;
            this.key3 = key3;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapClass mapClass = (MapClass) o;
            return Objects.equals(key1, mapClass.key1) && Objects.equals(key2, mapClass.key2) && Objects.equals(key3, mapClass.key3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key1, key2);
        }
    }

    public static class Config {
        @IgnoreField
        private Integer ignore = -10;
        private Integer defaultValue = -10;
        private Integer shouldBeNullInteger;
        private int shouldBeNegative1int;
        private Double shouldBeNullDouble;
        private double shouldBeNegative1double;
        private String shouldBeNullString;
        private Integer simpleInt;
        private Double simpleDouble;
        private Float simpleFloat;
        private Boolean simpleBoolean;
        private Long simpleLong;
        private String simpleString;
        private NestedClass nestedObject;
        private NestedClass nullNestedObject;
        private List<Integer> listInteger;
        private List<Integer> nullListInteger;
        private List<Double> listDouble;
        private List<String> listString;
        private List<String> defaultListString = List.of("a", "b", "c");
        private List<MapClass> mapClassList;
        private List<MapClass> nullMapClassList;
        private Map<String, NestedClass> nestedObjectMap;
        private Map<Integer, NestedClass> nestedObjectMapInt;
    }

    @BeforeEach
    public void setUp() {
        yaml = new YamlConfiguration();
        yaml.set("simple_int", 10);
        yaml.set("simple_string", "ExampleName");
        yaml.set("simple_double", 12.5);
        yaml.set("simple_boolean", true);
        yaml.set("simple_long", 1L);
        yaml.set("simple_float", (float) 1.5);
        yaml.set("nested_object.long_nested_name", "Nested name");
        yaml.set("list_string", List.of("one", "two", "three"));
        yaml.set("list_integer", List.of(1, 2, 3));
        yaml.set("list_double", List.of(1.5, 2.5, 3.5));
        yaml.set("map_class_list", List.of(Map.of("key1", "value1", "key2", "value2", "key3", new NestedClass("longname"))));
        yaml.set("nested_object_map.key1.long_nested_name", "longname");
        yaml.set("nested_object_map.key2.long_nested_name", "longname2");
        yaml.set("nested_object_map_int.1.long_nested_name", "longname");
        yaml.set("nested_object_map_int.2.long_nested_name", "longname2");
    }

    @Test
    public void testPrimitiveTypes() {
        var config = ConfigManager.load(new Config(), yaml);
        assertEquals(10, config.simpleInt);
        assertEquals("ExampleName", config.simpleString);
        assertEquals(12.5, config.simpleDouble, 0);
        assertEquals(true, config.simpleBoolean);
        assertEquals(1L, config.simpleLong);
        assertEquals(1.5F, config.simpleFloat);
        assertNull(config.shouldBeNullInteger);
        assertEquals(-1, config.shouldBeNegative1int);
        assertNull(config.shouldBeNullDouble);
        assertEquals(-1, config.shouldBeNegative1double);
        assertNull(config.shouldBeNullString);
        assertEquals(-10, config.ignore);
        assertEquals(-10, config.defaultValue);
    }

    @Test
    public void testNestedObjects() {
        var config = ConfigManager.load(new Config(), yaml);
        assertEquals("Nested name", config.nestedObject.longNestedName);
        assertNull(config.nullNestedObject);
    }

    @Test
    public void testPrimitiveLists() {
        var config = ConfigManager.load(new Config(), yaml);
        assertEquals(List.of("one", "two", "three"), config.listString);
        assertEquals(List.of(1, 2, 3), config.listInteger);
        assertEquals(List.of(1.5, 2.5, 3.5), config.listDouble);
        assertEquals(new ArrayList<>(), config.nullListInteger);
        assertEquals(List.of("a", "b", "c"), config.defaultListString);
    }

    @Test
    public void testMapClasses() {
        var config = ConfigManager.load(new Config(), yaml);

        assertEquals(
                new MapClass("value1", "value2", new NestedClass("longname")),
                config.mapClassList.get(0)
        );
        assertEquals(new ArrayList<>(), config.nullMapClassList);
    }

    @Test
    public void testMaps() {
        var config = ConfigManager.load(new Config(), yaml);

        assertEquals(
                new NestedClass("longname"),
                config.nestedObjectMap.get("key1")
        );
        assertEquals(
                new NestedClass("longname2"),
                config.nestedObjectMap.get("key2")
        );

        assertEquals(
                new NestedClass("longname"),
                config.nestedObjectMapInt.get(1)
        );
        assertEquals(
                new NestedClass("longname2"),
                config.nestedObjectMapInt.get(2)
        );
    }
}
