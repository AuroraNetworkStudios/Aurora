package gg.auroramc.aurora.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class YamlEditorTest {
    @TempDir
    Path dir;

    private static final String YAML = """
            # Which storage to use
            storage-type: "yaml" # yaml or mysql
            mysql:
              host: 127.0.0.1
              # the port
              port: 3306
              password: 'secret'
            list:
              - a
            section:
              key: value
            """;

    private File file() throws Exception {
        var file = dir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), YAML);
        return file;
    }

    @Test
    public void changesOnlyThatLine() throws Exception {
        var file = file();
        assertTrue(YamlEditor.setScalar(file, "storage-type", "mysql"));
        assertEquals(YAML.replace("storage-type: \"yaml\" # yaml or mysql", "storage-type: \"mysql\" # yaml or mysql"), Files.readString(file.toPath()));
    }

    @Test
    public void nestedKeysAndQuoteStyles() throws Exception {
        var file = file();
        assertTrue(YamlEditor.setScalar(file, "mysql.port", 3307));
        assertTrue(YamlEditor.setScalar(file, "mysql.password", "it's"));
        assertTrue(YamlEditor.setScalar(file, "mysql.host", "db: main"));
        var text = Files.readString(file.toPath());
        assertTrue(text.contains("  port: 3307\n"), text);
        assertTrue(text.contains("  password: 'it''s'\n"), text);
        assertTrue(text.contains("  host: \"db: main\"\n"), text);
        assertTrue(text.contains("  # the port\n"), text);
    }

    @Test
    public void refusesWhatIsntAPlainValue() throws Exception {
        var file = file();
        assertFalse(YamlEditor.setScalar(file, "mysql", "x"));
        assertFalse(YamlEditor.setScalar(file, "list", "x"));
        assertFalse(YamlEditor.setScalar(file, "missing", "x"));
        assertFalse(YamlEditor.setScalar(file, "section.missing", "x"));
        assertEquals(YAML, Files.readString(file.toPath()));
    }

    @Test
    public void keysWithColonsAndListsDontConfuseTheParent() throws Exception {
        var file = dir.resolve("c.yml").toFile();
        Files.writeString(file.toPath(), "a:\n  minecraft:stone:\n    amount: 1\n  amount: 2\nlist:\n  - amount: 3\n");
        assertTrue(YamlEditor.setScalar(file, "a.amount", 9));
        assertEquals("a:\n  minecraft:stone:\n    amount: 1\n  amount: 9\nlist:\n  - amount: 3\n", Files.readString(file.toPath()));
        assertTrue(YamlEditor.setScalar(file, "a.minecraft:stone.amount", 5));
        assertFalse(YamlEditor.setScalar(file, "list.amount", 1));
    }

    @Test
    public void refusesValuesContinuingOnTheNextLines() throws Exception {
        var file = dir.resolve("m.yml").toFile();
        var yaml = "message: \"hello\n  world\"\nplain: a long\n  continued text\nok: 1\n";
        Files.writeString(file.toPath(), yaml);
        assertFalse(YamlEditor.setScalar(file, "message", "x"));
        assertFalse(YamlEditor.setScalar(file, "plain", "x"));
        assertEquals(yaml, Files.readString(file.toPath()));
    }

    @Test
    public void newlinesAreEscapedAndLineEndingsKept() throws Exception {
        var file = dir.resolve("n.yml").toFile();
        Files.writeString(file.toPath(), "prefix: a\r\nother: b\r\n");
        assertTrue(YamlEditor.setScalar(file, "prefix", "line1\nline2"));
        assertEquals("prefix: \"line1\\nline2\"\r\nother: b\r\n", Files.readString(file.toPath()));
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.load(file);
        assertEquals("line1\nline2", yaml.getString("prefix"));
    }

    @Test
    public void textYamlWouldReadAsSomethingElseIsQuoted() throws Exception {
        var file = dir.resolve("q.yml").toFile();
        Files.writeString(file.toPath(), "a: x\nb: x\nc: x\n");
        assertTrue(YamlEditor.setScalar(file, "a", "2024-01-01"));
        assertTrue(YamlEditor.setScalar(file, "b", "0x1F"));
        assertTrue(YamlEditor.setScalar(file, "c", "plain text"));
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.load(file);
        assertEquals("2024-01-01", yaml.get("a"));
        assertEquals("0x1F", yaml.get("b"));
        assertTrue(Files.readString(file.toPath()).contains("c: plain text\n"));
    }
}
