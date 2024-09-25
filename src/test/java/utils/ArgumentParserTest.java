package utils;

import gg.auroramc.aurora.api.command.ArgumentParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArgumentParserTest {
    @Test
    public void testComplexParsing() {
        var args = ArgumentParser.parseString("1000        economy={Vault} key2={value2 abc} key3={abc  value3  abc}");

        // 4 because the first key-value pair is the prefix
        assertEquals(4, args.size());
        assertEquals("Vault", args.get("economy"));
        assertEquals("value2 abc", args.get("key2"));
        assertEquals("abc  value3  abc", args.get("key3"));
        assertEquals("1000", args.get("prefix"));
    }

    public void testArgumentOnlyParsing() {
        var args = ArgumentParser.parseString("key1={value1} key2={value2 abc} key3={abc  value3  abc}");

        // 4 because the first key-value pair is the prefix
        assertEquals(4, args.size());
        assertEquals("value1", args.get("key1"));
        assertEquals("value2 abc", args.get("key2"));
        assertEquals("abc  value3  abc", args.get("key3"));
        assertEquals("", args.get("prefix"));
    }

    public void testPrefixOnlyParsing() {
        var args = ArgumentParser.parseString("my menu");
        assertEquals("my menu", args.get("prefix"));
    }
}
