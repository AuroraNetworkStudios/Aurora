package gg.auroramc.aurora.api.plugin;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.config.ConfigFile;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * A plugin's messages, one {@code messages_<language>.yml} per language Aurora supports. A language without a bundled
 * file starts as a copy of English ({@code messages_en.yml}).
 */
public final class Messages<M> {
    private final Plugin plugin;
    private final Class<M> type;
    private final List<Consumer<YamlConfiguration>> migrations;
    // Swapped in at once, so a message sent during a reload never sees half of the old and half of the new
    private record State<M>(Map<Locale, ConfigFile<M>> files, Locale defaultLocale, boolean perPlayerLocale) {
    }

    private volatile State<M> state = new State<>(Map.of(), Locale.ENGLISH, false);

    /**
     * @param migrations the message files' migration steps by config-version, see {@link ConfigFile.Builder#migrations}
     */
    public Messages(Plugin plugin, Class<M> type, List<Consumer<YamlConfiguration>> migrations) {
        this.plugin = plugin;
        this.type = type;
        this.migrations = List.copyOf(migrations);
    }

    /**
     * (Re)loads every language Aurora supports, and the default one.
     *
     * @param defaultLanguage the language used when the per-player language is off, or a player's isn't supported
     * @param perPlayerLocale whether each player gets their own language
     */
    public void load(String defaultLanguage, boolean perPlayerLocale) {
        var locales = new LinkedHashSet<Locale>(AuroraAPI.getLanguageProvider().getSupportedLocales());
        var defaultLocale = Locale.forLanguageTag(defaultLanguage);
        locales.add(defaultLocale);
        var previous = this.state.files();
        var files = new LinkedHashMap<Locale, ConfigFile<M>>();
        for (var locale : locales) {
            // Reloaded if it was loaded before, so a file broken while the server runs keeps the messages from before
            var file = previous.get(locale);
            if (file != null) {
                file.reload();
            } else {
                var name = "messages_" + locale.getLanguage() + ".yml";
                file = ConfigFile.builder(plugin, name, type)
                        .bundled(hasResource(name) ? name : "messages_en.yml")
                        .migrations(migrations)
                        .build();
                file.get();
            }
            files.put(locale, file);
        }
        this.state = new State<>(Collections.unmodifiableMap(files), defaultLocale, perPlayerLocale);
    }

    private boolean hasResource(String name) {
        try (var in = plugin.getResource(name)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
     * The messages for whoever is told something: the player's language if per-player languages are on.
     */
    public M get(CommandSender sender) {
        var state = this.state;
        if (state.perPlayerLocale()) {
            var locale = sender instanceof Player player ? AuroraAPI.getLanguageProvider().getPlayerLocale(player) : AuroraAPI.getLanguageProvider().getFallbackLocale();
            var file = state.files().get(locale);
            if (file != null) return file.get();
        }
        return getDefault(state);
    }

    public M get(Locale locale) {
        var state = this.state;
        var file = state.files().get(locale);
        return file != null ? file.get() : getDefault(state);
    }

    public M getDefault() {
        return getDefault(state);
    }

    private M getDefault(State<M> state) {
        if (state.files().isEmpty()) throw new IllegalStateException("Messages are used before they were loaded, call load first");
        var file = state.files().get(state.defaultLocale());
        return file != null ? file.get() : state.files().values().iterator().next().get();
    }

    public Set<Locale> getLocales() {
        return state.files().keySet();
    }

    /**
     * Every key of a language's file, keys under {@code custom} flattened, for a {@code LocalizationProvider}.
     */
    public Map<String, String> toFlatMap(Locale locale) {
        var file = state.files().get(locale);
        if (file == null) return Map.of();
        var yaml = file.getYaml();
        var map = new HashMap<String, String>();
        for (var key : yaml.getKeys(false)) {
            if (key.equals("custom") && yaml.isConfigurationSection("custom")) {
                for (var customKey : yaml.getConfigurationSection("custom").getKeys(true)) {
                    map.put(customKey, yaml.getString("custom." + customKey));
                }
            } else {
                map.put(key, yaml.getString(key));
            }
        }
        return map;
    }
}
