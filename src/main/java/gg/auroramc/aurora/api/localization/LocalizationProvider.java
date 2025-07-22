package gg.auroramc.aurora.api.localization;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizationProvider implements LanguageProvider {
    private final Map<Locale, Map<String, String>> values = new ConcurrentHashMap<>();
    private final LanguageProvider languageProvider;

    public LocalizationProvider(LanguageProvider languageProvider) {
        this.languageProvider = languageProvider;
    }

    @Override
    public Locale getPlayerLocale(Player player) {
        return languageProvider.getPlayerLocale(player);
    }

    @Override
    public void setPlayerLocale(Player player, Locale locale) {
        languageProvider.setPlayerLocale(player, locale);
    }

    @Override
    public Locale getFallbackLocale() {
        return languageProvider.getFallbackLocale();
    }

    @Override
    public void setFallbackLocale(Locale locale) {
        languageProvider.setFallbackLocale(locale);
    }

    @Override
    public List<Locale> getSupportedLocales() {
        return languageProvider.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(List<Locale> locales) {
        languageProvider.setSupportedLocales(locales);
    }

    public void clear() {
        values.clear();
    }

    public void setLocaleValues(Locale locale, Map<String, String> values) {
        this.values.put(locale, values);
    }

    public String fillVariables(Player player, String input) {
        return fillVariables(getPlayerLocale(player), input);
    }

    public String fillVariables(Locale locale, String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Map<String, String> primary = values.get(locale != Locale.ROOT ? locale : languageProvider.getFallbackLocale());
        Map<String, String> fallback = values.get(languageProvider.getFallbackLocale());
        if (primary == null) {
            primary = Collections.emptyMap();
        }
        if (fallback == null) {
            fallback = Collections.emptyMap();
        }

        StringBuilder out = new StringBuilder(input.length());
        int len = input.length();
        int i = 0;

        while (i < len) {
            // look for opening {{
            if (input.charAt(i) == '{' && i + 1 < len && input.charAt(i + 1) == '{') {
                int close = input.indexOf("}}", i + 2);
                if (close != -1) {
                    // extract the key
                    String key = input.substring(i + 2, close);
                    // try primary, then fallback
                    String val = primary.get(key);
                    if (val == null) {
                        val = fallback.get(key);
                    }
                    if (val != null) {
                        out.append(val);
                    } else {
                        // leave placeholder intact
                        out.append("{{").append(key).append("}}");
                    }
                    i = close + 2;
                    continue;
                }
                // no closing }}, treat as literal
            }
            // normal character
            out.append(input.charAt(i));
            i++;
        }

        return out.toString();
    }
}
