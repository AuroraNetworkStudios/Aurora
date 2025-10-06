package localization;

import gg.auroramc.aurora.api.localization.LanguageProvider;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.localization.LocalizationProvider;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalizationProviderTest {

    private LocalizationProvider provider;
    private final Locale en = Locale.ENGLISH;
    private final Locale hu = new Locale("hu");

    @BeforeEach
    void setup() {
        LanguageProvider delegate = new LanguageProvider() {
            private Locale fallback = en;

            @Override
            public Locale getPlayerLocale(Player player) {
                return en;
            }

            @Override
            public void setPlayerLocale(Player player, Locale locale) {

            }

            @Override
            public Locale getFallbackLocale() {
                return fallback;
            }

            @Override
            public void setFallbackLocale(Locale locale) {
                this.fallback = locale;
            }

            @Override
            public List<Locale> getSupportedLocales() {
                return List.of(en, hu);
            }

            @Override
            public void setSupportedLocales(List<Locale> locales) {
            }
        };

        provider = new LocalizationProvider(delegate);

        // Setup English lang
        provider.setLocaleValues(en, Map.of(
                "hello", "Hello",
                "welcome", "Welcome to {{server}}!",
                "server", "Aurora",
                "nested", "Outer -> {{inner}}",
                "inner", "Inner -> {player}",
                "loopA", "{{loopB}}",
                "loopB", "{{loopA}}"
        ));

        // Setup Hungarian lang
        provider.setLocaleValues(hu, Map.of(
                "hello", "Szia",
                "server", "Aurora HU"
        ));
    }

    @Test
    void simpleLanguageReplacement() {
        String result = provider.fillVariables(en, "Message: {{hello}}");
        assertEquals("Message: Hello", result);
    }

    @Test
    void simplePlaceholderReplacement() {
        String result = provider.fillVariables(en, "Player: {player}",
                Placeholder.of("{player}", "Bela"));
        assertEquals("Player: Bela", result);
    }

    @Test
    void nestedLanguagePlaceholders() {
        String result = provider.fillVariables(en, "Msg: {{welcome}}");
        assertEquals("Msg: Welcome to Aurora!", result);
    }

    @Test
    void languageIntoPlaceholder() {
        String result = provider.fillVariables(en, "{{nested}}",
                Placeholder.of("{player}", "Bela"));
        assertEquals("Outer -> Inner -> Bela", result);
    }

    @Test
    void placeholderIntoLanguage() {
        String result = provider.fillVariables(en, "{key}",
                Placeholder.of("{key}", "{{hello}}"));
        assertEquals("Hello", result);
    }

    @Test
    void multiLevelRecursiveExpansion() {
        String result = provider.fillVariables(en, ">>> {{nested}} <<<",
                Placeholder.of("{player}", "{{welcome}}"));
        // {player} expands to "{{welcome}}" -> "Welcome to {{server}}!" -> Aurora
        assertEquals(">>> Outer -> Inner -> Welcome to Aurora! <<<", result);
    }

    @Test
    void fallbackLocaleUsed() {
        String result = provider.fillVariables(hu, "X: {{welcome}}");
        // welcome only defined in EN, fallback should kick in
        assertEquals("X: Welcome to Aurora HU!", result);
    }

    @Test
    void cyclicReferencesStopAtMaxDepth() {
        String result = provider.fillVariables(en, "Cycle: {{loopA}}");
        // Should not infinite loop, expect it to leave something unresolved
        assertTrue(result.startsWith("Cycle:"));
        assertTrue(result.contains("{{") || result.contains("Aurora"));
    }

    @Test
    void noChangeReturnsInput() {
        String result = provider.fillVariables(en, "Plain text without placeholders");
        assertEquals("Plain text without placeholders", result);
    }

    @Test
    void nullAndEmptyHandled() {
        assertNull(provider.fillVariables(en, null));
        assertEquals("", provider.fillVariables(en, ""));
    }
}
