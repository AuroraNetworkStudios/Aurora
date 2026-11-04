package gg.auroramc.aurora.internal.config;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.ConfigFile;
import gg.auroramc.aurora.api.config.decorators.IgnoreField;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@Getter
public class MessageConfig {
    private String reloaded = "&aReloaded configuration!";
    private String dataNotLoadedYet = "&cData for this player hasn't loaded yet, try again later!";
    private String dataNotLoadedYetSelf = "&cYour data isn't loaded yet, please try again later!";
    private String playerOnlyCommand = "&cThis command can only be executed by a player!";
    private String noPermission = "&cYou don't have permission to execute this command!";
    private String invalidSyntax = "&cInvalid command syntax!";
    private String mustBeNumber = "&cArgument must be a number!";
    private String playerNotFound = "&cPlayer not found!";
    private String commandError = "&cAn error occurred while executing this command!";
    private String kickedByDbMigration = "&cUnder maintenance, please try again later!";
    private String kickedByFailedLoad = "&cYour data couldn't be loaded, please rejoin in a moment!";
    private String dbMigrateStarted = "&aAttempting to migrate storage, please wait...";
    private String dbMigrateFinished = "&aStorage migration completed! It is advised to restart the server now, although it is not required.";
    private String dbMigrateFailed = "&cStorage migration failed! Please check the console for more information.";
    private String guiReloaded = "&aSuccessfully reloaded &2{amount} &aguis";
    private String metaSet = "&aSet meta key &2{key} &ato &2{value}";
    private String metaRemoved = "&aRemoved meta key &2{key}";
    private String metaIncremented = "&aIncremented meta key &2{key} &aby &2{value}. Current value: &2{current}";
    private String metaDecremented = "&aDecremented meta key &2{key} &aby &2{value}. Current value: &2{current}";
    private String metaNotFound = "&cMeta with key: &4{key} &cwas not found";
    private String stashAvailable = "&aYou have unclaimed items in your stash!";
    private String stashItemAdded = "&aItems are added to the stash!";
    private String stashItemsCleared = "&aItems cleared!";
    private String stashTakeFailed = "&cThose items aren't in the stash of {player} anymore!";
    private String itemRegistered = "&aItem registered with id: &2{id}!";
    private String itemUnregistered = "&aItem unregistered with id: &2{id}!";
    private String itemNotFound = "&cItem with id &4{id} &cwas not found";
    private String leaderboardNotExists = "&cLeaderboard with id: &4{board} &cdoes not exists!";
    private String leaderboardCleared = "&aLeaderboard with id: &2{board} &acleared and will be updated shortly!";
    private String unknownCommand = "&cAuroraLib doesn't have a command like that.";
    private String localeNotSupported = "&cThis language isn't supported at the moment";
    private String localeInvalid = "&cInvalid language code";
    private String localeChanged = "&cLocale changed to english.";

    // Every key of the file, sections under "custom" flattened, for the localization provider
    @IgnoreField
    private Map<String, String> flat = Map.of();

    // Kept across reloads, so a file broken while the server runs keeps the messages from before
    private static final Map<String, ConfigFile<MessageConfig>> files = new ConcurrentHashMap<>();

    /**
     * Loads the language's messages, or reloads them if they were loaded before.
     */
    public static MessageConfig load(String language) {
        var existing = files.get(language);
        var file = existing != null ? existing : files.computeIfAbsent(language, k -> ConfigFile.builder(Aurora.getInstance(), "messages_" + language + ".yml", MessageConfig.class)
                .bundled(hasResource("messages_" + language + ".yml") ? "messages_" + language + ".yml" : "messages_en.yml")
                .migrations(migrations(language))
                .build());
        var config = existing != null ? file.reload() : file.get();
        config.flat = flatten(file.getYaml());
        return config;
    }

    private static boolean hasResource(String name) {
        try (var in = Aurora.getInstance().getResource(name)) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    // Not versioned: it runs on every load and copies the keys missing from the file
    private static List<Consumer<YamlConfiguration>> migrations(String language) {
        return List.of(
                (yaml) -> {

                    try (var in = getInternalFile(language)) {
                        var original = YamlConfiguration.loadConfiguration(new InputStreamReader(in));

                        for (var key : original.getKeys(false)) {
                            if (yaml.contains(key)) continue;
                            yaml.set(key, original.get(key));
                        }
                    } catch (Exception e) {
                        Aurora.logger().severe("Failed to run migrations on messages_" + language + ".yml");
                        e.printStackTrace();
                    }
                }
        );
    }

    private static InputStream getInternalFile(String language) {
        var in = Aurora.getInstance().getResource("messages_" + language + ".yml");
        if (in == null) {
            return Aurora.getInstance().getResource("messages_en.yml");
        }
        return in;
    }

    public Map<String, String> toFlatMap() {
        return flat;
    }

    private static Map<String, String> flatten(YamlConfiguration yaml) {
        var map = new HashMap<String, String>();

        for (var key : yaml.getKeys(false)) {
            if (key.equals("custom")) {
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
