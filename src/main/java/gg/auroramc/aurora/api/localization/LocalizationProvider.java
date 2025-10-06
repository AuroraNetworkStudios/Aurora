package gg.auroramc.aurora.api.localization;

import gg.auroramc.aurora.api.message.Placeholder;
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

    public String fillVariables(Player player, String input, Placeholder<?>... placeholders) {
        return fillVariables(getPlayerLocale(player), input, placeholders);
    }

    public String fillVariables(Locale locale, String input, Placeholder<?>... placeholders) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Map<String, String> primary = values.get(locale != Locale.ROOT ? locale : languageProvider.getFallbackLocale());
        Map<String, String> fallback = values.get(languageProvider.getFallbackLocale());
        if (primary == null) primary = Collections.emptyMap();
        if (fallback == null) fallback = Collections.emptyMap();

        return resolveRecursive(input, primary, fallback, placeholders, 0);
    }

    public String fillVariables(Player player, String input, List<Placeholder<?>> placeholders) {
        return fillVariables(getPlayerLocale(player), input, placeholders);
    }

    public String fillVariables(Locale locale, String input, List<Placeholder<?>> placeholders) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Map<String, String> primary = values.get(locale != Locale.ROOT ? locale : languageProvider.getFallbackLocale());
        Map<String, String> fallback = values.get(languageProvider.getFallbackLocale());
        if (primary == null) primary = Collections.emptyMap();
        if (fallback == null) fallback = Collections.emptyMap();

        return resolveRecursive(input, primary, fallback, placeholders, 0);
    }

    private static final int MAX_RECURSION = 10;

    private String resolveRecursive(String input,
                                    Map<String, String> primary,
                                    Map<String, String> fallback,
                                    Placeholder<?>[] placeholders,
                                    int depth) {
        if (depth > MAX_RECURSION || input == null || input.isEmpty()) {
            return input; // safety stop
        }

        StringBuilder out = new StringBuilder(input.length());
        boolean replaced = false;
        int len = input.length();

        for (int i = 0; i < len; ) {
            char c = input.charAt(i);

            // --- Language placeholder: {{key}} ---
            if (c == '{' && i + 1 < len && input.charAt(i + 1) == '{') {
                int close = input.indexOf("}}", i + 2);
                if (close != -1) {
                    String key = input.substring(i + 2, close);
                    String val = primary.getOrDefault(key, fallback.get(key));
                    if (val != null) {
                        out.append(val);
                        replaced = true;
                    } else {
                        out.append("{{").append(key).append("}}");
                    }
                    i = close + 2;
                    continue;
                }
            }

            // --- Normal char ---
            out.append(c);
            i++;
        }

        String result = out.toString();
        String newResult = Placeholder.execute(result, placeholders);
        replaced = replaced || !newResult.equals(result);
        result = newResult;

        return replaced ? resolveRecursive(result, primary, fallback, placeholders, depth + 1) : result;
    }

    private String resolveRecursive(String input,
                                    Map<String, String> primary,
                                    Map<String, String> fallback,
                                    List<Placeholder<?>> placeholders,
                                    int depth) {
        if (depth > MAX_RECURSION || input == null || input.isEmpty()) {
            return input; // safety stop
        }

        StringBuilder out = new StringBuilder(input.length());
        boolean replaced = false;
        int len = input.length();

        for (int i = 0; i < len; ) {
            char c = input.charAt(i);

            // --- Language placeholder: {{key}} ---
            if (c == '{' && i + 1 < len && input.charAt(i + 1) == '{') {
                int close = input.indexOf("}}", i + 2);
                if (close != -1) {
                    String key = input.substring(i + 2, close);
                    String val = primary.getOrDefault(key, fallback.get(key));
                    if (val != null) {
                        out.append(val);
                        replaced = true;
                    } else {
                        out.append("{{").append(key).append("}}");
                    }
                    i = close + 2;
                    continue;
                }
            }

            // --- Normal char ---
            out.append(c);
            i++;
        }

        String result = out.toString();
        String newResult = Placeholder.execute(result, placeholders);
        replaced = replaced || !newResult.equals(result);
        result = newResult;

        return replaced ? resolveRecursive(result, primary, fallback, placeholders, depth + 1) : result;
    }
}
