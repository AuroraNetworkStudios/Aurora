package config;

import gg.auroramc.aurora.api.config.ConfigManager;
import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoadTest {
    private YamlConfiguration yaml;

    public static class NestedClass {
        public String longNestedName;

        public NestedClass() {
        }

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

        public MapClass() {
        }

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
        private Boolean simpleBoolean = false;
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
        public Map<String, NestedClass> nestedObjectMap;
        private Map<Integer, NestedClass> nestedObjectMapInt;
        private Map<String, ItemConfig> tags;
        private Set<String> set;
        private List<ItemConfig> items;
    }

    @BeforeEach
    public void setUp() {
        yaml = new YamlConfiguration();
        yaml.set("simple-int", 10);
        yaml.set("simple-string", "ExampleName");
        yaml.set("simple-double", 12.5);
        yaml.set("simple-boolean", true);
        yaml.set("simple-long", 1L);
        yaml.set("simple-float", (float) 1.5);
        yaml.set("nested-object.long-nested-name", "Nested name");
        yaml.set("list-string", List.of("one", "two", "three"));
        yaml.set("list-integer", List.of(1, 2, 3));
        yaml.set("list-double", List.of(1.5, 2.5, 3.5));
        yaml.set("map-class-list", List.of(Map.of("key1", "value1", "key2", "value2", "key3", Map.of("long-nested-name", "longname"))));
        yaml.set("nested-object-map.key1.long-nested-name", "longname");
        yaml.set("nested-object-map.key2.long-nested-name", "longname2");
        yaml.set("nested-object-map-int.1.long-nested-name", "longname");
        yaml.set("nested-object-map-int.2.long-nested-name", "longname2");
        yaml.set("tags.test-tag.name", "hello");
        yaml.set("set", List.of("a", "b", "c"));

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("material", "diamond", "potion", Map.of("type", "SPEED"), "enchantments", Map.of("protection", 1)));
        items.add(Map.of("material", "emerald", "potion", Map.of("type", "INVISIBILITY"), "enchantments", Map.of("sharpness", 1)));
        yaml.set("items", items);

    }

    @Test
    public void testComplexList() {
        var config = ConfigManager.load(new Config(), yaml);
        assertEquals(2, config.items.size());
        assertEquals("diamond", config.items.get(0).getMaterial());
        assertEquals("emerald", config.items.get(1).getMaterial());
        assertEquals("SPEED", config.items.get(0).getPotion().getType());
        assertEquals("INVISIBILITY", config.items.get(1).getPotion().getType());
        assertEquals(1, config.items.get(0).getEnchantments().get("protection"));
        assertEquals(1, config.items.get(1).getEnchantments().get("sharpness"));
    }

    @Test
    public void testPrimitiveTypes() {
        var config = ConfigManager.load(new Config(), yaml);
        assertEquals(10, config.simpleInt);
        assertEquals("ExampleName", config.simpleString);
        assertEquals(12.5, config.simpleDouble, 0);
        assertTrue(config.simpleBoolean);
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
        assertEquals(Set.of("a", "b", "c"), config.set);
    }

    @Test
    public void testMapClasses() {
        var config = ConfigManager.load(new Config(), yaml);

        assertEquals("value1", config.mapClassList.get(0).key1);
        assertEquals("value2", config.mapClassList.get(0).key2);
        assertEquals("longname", config.mapClassList.get(0).key3.longNestedName);

        assertEquals(new ArrayList<>(), config.nullMapClassList);
    }

    @Test
    public void testMaps() {
        var config = ConfigManager.load(new Config(), yaml);

        assertEquals("hello", config.tags.get("test-tag").getName());

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
