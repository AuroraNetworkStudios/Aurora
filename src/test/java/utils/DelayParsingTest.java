package utils;

import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.message.Placeholder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DelayParsingTest {
    private static long delay(String line) {
        return CommandDispatcher.getTotalDelay(List.of(line), null);
    }

    @Test
    public void testDelayFormats() {
        assertEquals(20, delay("[delay: 20]"));
        assertEquals(20, delay("[delay:20]"));
        assertEquals(20, delay("[delay : 20]"));
        assertEquals(20, delay("[delay 20]"));
        assertEquals(20, delay("[delay] 20"));
        assertEquals(20, delay("[DELAY: 20]"));
        assertEquals(20, delay("  [delay: 20]  "));
    }

    @Test
    public void testInvalidDelays() {
        assertEquals(0, delay("[delay]"));
        assertEquals(0, delay("[delay: ]"));
        assertEquals(0, delay("[delay: -5]"));
        assertEquals(0, delay("[delay: abc]"));
        assertEquals(0, delay("[delay: 1.5]"));
        assertEquals(0, delay("[delay: 20] 30"));
        assertEquals(0, delay("[delay: 99999999999]"));
    }

    @Test
    public void testNotDelays() {
        assertEquals(0, delay("[delayed] 20"));
        assertEquals(0, delay("[console] say [delay: 20]"));
        assertEquals(0, delay("delay 20"));
        assertEquals(0, delay("[delay: 20"));
    }

    @Test
    public void testPlaceholdersAndSum() {
        List<Placeholder<?>> placeholders = List.of(Placeholder.of("{arg_ticks}", "15"));
        assertEquals(15, CommandDispatcher.getTotalDelay(List.of("[delay: {arg_ticks}]"), placeholders));

        var actions = List.of(
                "[close]",
                "[delay: 20]",
                "[console] say first",
                "[delay] 10",
                "[console] say second");
        assertEquals(30, CommandDispatcher.getTotalDelay(actions, null));
        assertEquals(0, CommandDispatcher.getTotalDelay(null, null));
    }
}
