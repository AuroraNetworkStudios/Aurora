package config;

import gg.auroramc.aurora.api.config.ConfigLoader;
import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import gg.auroramc.aurora.api.config.decorators.Key;
import gg.auroramc.aurora.api.config.decorators.Range;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTest {
    public enum Mode {SIMPLE, VERY_STRICT}

    public static class Item {
        private String material;
        private String name;
        private List<String> lore;
        private int amount = 1;
    }

    public static class Menu {
        private String title = "Menu";
        @Range(min = 1, max = 6)
        private Integer rows = 6;
        private Item filler;
        private Item back;
        private Map<String, Item> customItems;
        private List<Integer> slots;
        private Set<String> tags;
        private Mode mode = Mode.SIMPLE;
        @Key("result-slot-number")
        private int resultSlot = 25;
        private boolean enabled = true;
        private double chance = 0.5;
        @IgnoreField
        private String ignored = "kept";
        private static String STATIC = "static";
        private ConfigurationSection raw;
    }

    public static class Base {
        private String inherited = "base";
    }

    public static class Child extends Base {
        private String own;
    }

    private static YamlConfiguration yaml(String text) {
        var yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return yaml;
    }

    private static <T> T load(Class<T> type, List<String> problems, String... layers) {
        return ConfigLoader.load(type, "test.yml", Arrays.stream(layers).map(l -> (ConfigurationSection) yaml(l)).toList(), problems::add);
    }

    private static <T> T load(Class<T> type, String... layers) {
        return load(type, new ArrayList<>(), layers);
    }

    @Test
    public void missingKeysKeepTheFieldDefaults() {
        var menu = load(Menu.class, "other: 1\n");
        assertEquals("Menu", menu.title);
        assertEquals(6, menu.rows);
        assertEquals(25, menu.resultSlot);
        assertTrue(menu.enabled);
        assertEquals(0.5, menu.chance);
        assertEquals(Mode.SIMPLE, menu.mode);
        assertNull(menu.filler);
    }

    @Test
    public void missingCollectionsAreEmptyNotNull() {
        var menu = load(Menu.class, "other: 1\n");
        assertEquals(Map.of(), menu.customItems);
        assertEquals(List.of(), menu.slots);
        assertEquals(Set.of(), menu.tags);
    }

    @Test
    public void keysAreKebabCaseOrKey() {
        var menu = load(Menu.class, "custom-items:\n  a:\n    material: stone\nresult-slot-number: 13\nresult-slot: 99\n");
        assertEquals("stone", menu.customItems.get("a").material);
        assertEquals(13, menu.resultSlot);
    }

    @Test
    public void ignoredAndStaticFieldsAreNotRead() {
        var menu = load(Menu.class, "ignored: changed\nstatic: changed\n");
        assertEquals("kept", menu.ignored);
        assertEquals("static", Menu.STATIC);
    }

    @Test
    public void inheritedFieldsAreRead() {
        var child = load(Child.class, "inherited: parent\nown: mine\n");
        assertEquals("parent", ((Base) child).inherited);
        assertEquals("mine", child.own);
    }

    @Test
    public void enumsIgnoreCaseAndDashes() {
        assertEquals(Mode.VERY_STRICT, load(Menu.class, "mode: very-strict\n").mode);
        assertEquals(Mode.VERY_STRICT, load(Menu.class, "mode: Very_Strict\n").mode);
    }

    @Test
    public void badValuesFallBackAndAreReported() {
        var problems = new ArrayList<String>();
        var menu = load(Menu.class, problems, "rows: lots\nmode: weird\nresult-slot-number: 1.5\n");
        assertEquals(6, menu.rows);
        assertEquals(Mode.SIMPLE, menu.mode);
        // Cut like before 3.0
        assertEquals(1, menu.resultSlot);
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.getFirst().startsWith("test.yml: rows: "), problems.getFirst());
    }

    @Test
    public void rangeIsChecked() {
        var problems = new ArrayList<String>();
        var menu = load(Menu.class, problems, "rows: 9\n");
        assertEquals(6, menu.rows);
        assertTrue(problems.getFirst().contains("between 1 and 6"), problems.getFirst());
    }

    @Test
    public void badValueFallsBackToTheLayerBelow() {
        var menu = load(Menu.class, "rows: 3\n", "rows: 9\n");
        assertEquals(3, menu.rows);
    }

    @Test
    public void laterLayersOverrideEarlierOnes() {
        var menu = load(Menu.class, "title: Default\nrows: 3\n", "title: Mine\n");
        assertEquals("Mine", menu.title);
        assertEquals(3, menu.rows);
    }

    @Test
    public void nestedObjectsMergeKeyByKey() {
        var menu = load(Menu.class,
                "back:\n  material: arrow\n  name: Back\n  lore: [Click]\n",
                "back:\n  name: Return\n");
        assertEquals("arrow", menu.back.material);
        assertEquals("Return", menu.back.name);
        assertEquals(List.of("Click"), menu.back.lore);
    }

    @Test
    public void mapsAndListsAreReplacedWhole() {
        var menu = load(Menu.class,
                "custom-items:\n  a:\n    material: stone\n  b:\n    material: dirt\nslots: [1, 2]\n",
                "custom-items:\n  c:\n    material: glass\nslots: [5]\n");
        assertEquals(List.of("c"), List.copyOf(menu.customItems.keySet()));
        assertEquals(List.of(5), menu.slots);
    }

    @Test
    public void mapsKeepTheirOrder() {
        var menu = load(Menu.class, "custom-items:\n  z:\n    material: a\n  a:\n    material: b\n  m:\n    material: c\n");
        assertEquals(List.of("z", "a", "m"), List.copyOf(menu.customItems.keySet()));
    }

    @Test
    public void nestedObjectsGetTheirOwnDefaults() {
        var menu = load(Menu.class, "custom-items:\n  a:\n    material: stone\n");
        assertEquals(1, menu.customItems.get("a").amount);
    }

    @Test
    public void listElementsAreConvertedOrSkipped() {
        var problems = new ArrayList<String>();
        var menu = load(Menu.class, problems, "slots: [1, '2', 0-8, 1.5, 3.0]\ntags: [a, 1, true]\n");
        assertEquals(List.of(1, 2, 3), menu.slots);
        assertEquals(new LinkedHashSet<>(List.of("a", "1", "true")), menu.tags);
        assertEquals(2, problems.size(), problems.toString());
    }

    @Test
    public void listsOfObjects() {
        var yaml = "items:\n  - material: stone\n    amount: 3\n  - material: dirt\n";
        var holder = load(WithList.class, yaml);
        assertEquals(2, holder.items.size());
        assertEquals(3, holder.items.get(0).amount);
        assertEquals(1, holder.items.get(1).amount);
    }

    public static class WithList {
        private List<Item> items;
    }

    @Test
    public void numberKeysInMaps() {
        var problems = new ArrayList<String>();
        var levels = load(WithLevels.class, problems, "levels:\n  1:\n    material: a\n  two:\n    material: b\n  3:\n    material: c\n");
        assertEquals(List.of(1, 3), List.copyOf(levels.levels.keySet()));
        assertEquals(1, problems.size());
    }

    public static class WithLevels {
        private Map<Integer, Item> levels;
    }

    @Test
    public void rawSections() {
        var menu = load(Menu.class, "raw:\n  anything: 1\n");
        assertEquals(1, menu.raw.getInt("anything"));
    }

    @Test
    public void textFieldsAcceptNumbers() {
        var menu = load(Menu.class, "title: 100\n");
        assertEquals("100", menu.title);
    }

    @Test
    public void singleValuesConvertLikeBefore() {
        var item = load(Item.class, "amount: 2.7\nname: [a, b]\n");
        assertEquals(2, item.amount);
        assertEquals("[a, b]", item.name);
    }

    @Test
    public void emptyListItemsOfObjectsAreSkipped() {
        var holder = load(WithList.class, "items:\n  -\n  - material: stone\n");
        assertEquals(1, holder.items.size());
    }

    public static class Exotic {
        private TreeMap<String, Integer> sorted;
        private Runnable notConfigurable = () -> {
        };
    }

    @Test
    public void specificCollectionTypesAndUnreadableTypes() {
        var problems = new ArrayList<String>();
        var exotic = load(Exotic.class, problems, "sorted:\n  b: 2\n  a: 1\nnot-configurable:\n  x: 1\n");
        assertEquals(List.of("a", "b"), List.copyOf(exotic.sorted.keySet()));
        assertNotNull(exotic.notConfigurable);
    }

    @Test
    public void eachLoadIsANewObject() {
        assertNotSame(load(Menu.class, "title: a\n"), load(Menu.class, "title: a\n"));
    }
}
