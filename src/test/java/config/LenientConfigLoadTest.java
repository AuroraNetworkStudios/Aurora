package config;

import gg.auroramc.aurora.api.config.ConfigLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Values that used to throw and stop the whole file from loading.
 */
public class LenientConfigLoadTest {
    public static class Nested {
        private String name;
    }

    public static class Config {
        private List<String> lore;
        private List<Double> values;
        private List<Integer> slots;
        private Map<Integer, Nested> levels;
        private String title = "default";
        private int rows = 6;
    }

    private static Config load(String yaml) throws Exception {
        var config = new YamlConfiguration();
        config.loadFromString(yaml);
        return ConfigLoader.load(Config.class, "test", config);
    }

    @Test
    public void testUnquotedNumbersInStringList() throws Exception {
        var config = load("lore:\n  - first line\n  - 100\n  - true\n");
        assertEquals(List.of("first line", "100", "true"), config.lore);
    }

    @Test
    public void testWholeNumbersInDecimalList() throws Exception {
        var config = load("values:\n  - 1\n  - 2.5\n");
        assertEquals(List.of(1.0, 2.5), config.values);
    }

    @Test
    public void testInvalidElementsAreSkipped() throws Exception {
        var config = load("slots:\n  - 1\n  - 0-8\n  - \"3\"\n");
        assertEquals(List.of(1, 3), config.slots);
    }

    @Test
    public void testDecimalsInWholeNumberListAreSkipped() throws Exception {
        var config = load("slots:\n  - 1\n  - 1.5\n  - 2.0\n");
        assertEquals(List.of(1, 2), config.slots);
    }

    @Test
    public void testNonNumericMapKeysAreSkipped() throws Exception {
        var config = load("levels:\n  1:\n    name: one\n  abc:\n    name: bad\n  2:\n    name: two\n");
        assertEquals(2, config.levels.size());
        assertEquals("one", config.levels.get(1).name);
        assertEquals("two", config.levels.get(2).name);
    }

    @Test
    public void testOtherFieldsStillLoad() throws Exception {
        var config = load("lore:\n  - 100\ntitle: My menu\nrows: 3\n");
        assertEquals("My menu", config.title);
        assertEquals(3, config.rows);
    }
}
