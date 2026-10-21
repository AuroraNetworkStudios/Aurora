package gg.auroramc.aurora.config;

import com.google.common.collect.Lists;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.AuroraConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@Getter
public class Config extends AuroraConfig {
    private Boolean debug = false;
    private NumberFormatConfig numberFormat;
    private Integer userAutoSaveInMinutes = 30;
    @Setter
    private String storageType = "yaml";
    private BlockTrackerConfig blockTracker;
    private Boolean modernBlockTracker = true;
    private MySqlConfig mysql;
    private LeaderboardConfig leaderboards;
    private String defaultEconomyProvider = "auto-detect";
    private Set<String> itemMatchers = Set.of(
            "CustomFishing",
            "CraftEngine",
            "Eco",
            "ExecutableItems",
            "ExecutableBlocks",
            "HeadDatabase",
            "ItemsAdder",
            "MMOItems",
            "MythicMobs",
            "Nexo",
            "Oraxen",
            "ItemEdit",
            "EvenMoreFish",
            "KGenerators",
            "CrackShot"
    );
    private ItemIdResolverConfig auroraItems;
    private Map<String, Integer> itemResolverPriorities = new HashMap<>() {{
        put("crackshot", 210);
        put("customfishing", 200);
        put("craftengine", 195);
        put("eb", 190);
        put("emf", 180);
        put("ei", 170);
        put("mmoitems", 160);
        put("mythicmobs", 150);
        put("eco", 140);
        put("nexo", 130);
        put("oraxen", 120);
        put("ia", 110);
        put("itemedit", 100);
        put("kgenerators", 95);
        put("hdb", 90);
        put("aurora", 80);
    }};

    private List<String> supportedLanguages = Lists.newArrayList("en");
    private String locale = "en";
    private Boolean usePerPlayerLocale = false;

    @Getter
    public final static class ItemIdResolverConfig {
        private Boolean enableIdResolver = false;
    }


    public Config() {
        super(new File(Aurora.getInstance().getDataFolder(), "config.yml"));
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of(
                (yaml) -> {
                    yaml.set("block-tracker.enabled", true);
                    yaml.set("config-version", 1);
                },
                (yaml) -> {
                    yaml.set("number-format.short-number-format.format", "#,##0.##");
                    yaml.set("number-format.short-number-format.suffixes",
                            Map.of("thousand", "K", "million", "M", "billion", "B", "trillion", "T", "quadrillion", "Q"));
                    yaml.set("config-version", 2);
                },
                (yaml) -> {
                    yaml.set("leaderboards.empty-placeholder", "---");
                    yaml.set("config-version", null);
                    yaml.set("config-version", 3);
                },
                (yaml) -> {
                    yaml.set("default-economy-provider", "auto-detect");
                    yaml.setComments("default-economy-provider", List.of(
                            "Use 'auto-detect' to automatically detect the economy provider. Otherwise use the plugin name you want.",
                            "Supported plugins: Vault, Essentials, CMI, PlayerPoints, ExcellentEconomy, CoinsEngine, EcoBits, EliteMobs", "RoyaleEconomy", "RoyaleEconomyBank",
                            "Changing this requires a full restart"));
                    yaml.set("config-version", null);
                    yaml.set("config-version", 4);
                },
                (yaml) -> {
                    yaml.set("item-matchers", List.of(
                            "CustomFishing",
                            "Eco",
                            "ExecutableItems",
                            "ExecutableBlocks",
                            "HeadDatabase",
                            "ItemsAdder",
                            "MMOItems",
                            "MythicMobs",
                            "Nexo",
                            "Oraxen"
                    ));
                    yaml.set("config-version", null);
                    yaml.set("config-version", 5);
                },
                (yaml) -> {
                    yaml.set("aurora-items.enable-id-resolver", false);
                    yaml.setComments("aurora-items", List.of(
                            "Should we try resolve IDs for items registered through /aurora registeritem <id>?",
                            "This is experimental and may not work for every use case.",
                            "Changing these values requires a full restart."
                    ));
                    yaml.set("config-version", null);
                    yaml.set("config-version", 6);
                },
                (yaml) -> {
                    var list = new ArrayList<>(yaml.getStringList("item-matchers"));
                    list.add("ItemEdit");
                    yaml.set("item-matchers", list);
                    yaml.set("config-version", 7);
                },
                (yaml) -> {
                    var list = new ArrayList<>(yaml.getStringList("item-matchers"));
                    list.add("EvenMoreFish");
                    yaml.set("item-matchers", list);
                    yaml.set("config-version", 8);
                },
                (yaml) -> {
                    yaml.set("item-matchers", List.of(
                            "CustomFishing",
                            "Eco",
                            "ExecutableItems",
                            "ExecutableBlocks",
                            "HeadDatabase",
                            "ItemsAdder",
                            "MMOItems",
                            "MythicMobs",
                            "Nexo",
                            "Oraxen",
                            "KGenerators"
                    ));
                    yaml.set("item-resolver-priorities", new LinkedHashMap<String, Integer>() {{
                        put("customfishing", 200);
                        put("eb", 190);
                        put("emf", 180);
                        put("ei", 170);
                        put("mmoitems", 160);
                        put("mythicmobs", 150);
                        put("eco", 140);
                        put("nexo", 130);
                        put("oraxen", 120);
                        put("ia", 110);
                        put("itemedit", 100);
                        put("kgenerators", 95);
                        put("hdb", 90);
                        put("aurora", 80);
                    }});
                    yaml.set("config-version", 9);
                },
                (yaml) -> {
                    yaml.set("supported-languages", List.of("en"));
                    yaml.set("fallback-locale", "en");
                    yaml.set("config-version", null);
                    yaml.set("config-version", 10);
                },
                (yaml) -> {
                    yaml.set("locale", yaml.getString("fallback-locale", "en"));
                    yaml.set("fallback-locale", null);
                    yaml.set("use-per-player-locale", false);
                    yaml.set("config-version", 11);
                },
                (yaml) -> {
                    var matcherList = yaml.getStringList("item-matchers");
                    matcherList.add("CraftEngine");
                    yaml.set("item-matchers", matcherList);

                    var priorities = yaml.getConfigurationSection("item-resolver-priorities");
                    if(priorities == null) {
                        priorities = yaml.createSection("item-resolver-priorities");
                    }
                    priorities.set("craftengine", 195);
                    yaml.set("item-resolver-priorities", priorities);
                    yaml.set("config-version", null);
                    yaml.set("config-version", 12);
                },
                (yaml) -> {
                    var matcherList = yaml.getStringList("item-matchers");
                    matcherList.add("CrackShot");
                    yaml.set("item-matchers", matcherList);
                    yaml.set("item-resolver-priorities.crackshot", 210);
                    yaml.set("config-version", 13);
                },
                (yaml) -> {
                    yaml.setComments("default-economy-provider", List.of(
                            "Use 'auto-detect' to automatically detect the economy provider. Otherwise use the plugin name you want.",
                            "Supported plugins: Vault, Essentials, CMI, PlayerPoints, ExcellentEconomy, CoinsEngine, EcoBits, EliteMobs", "RoyaleEconomy", "RoyaleEconomyBank",
                            "Changing this requires a full restart"));
                    yaml.set("config-version", 14);
                },
                // Early 3.0.0 dev builds used version 15, the next step undoes what they changed
                (yaml) -> yaml.set("config-version", 15),
                (yaml) -> {
                    if (!yaml.contains("mysql.sync-retry-count")) yaml.set("mysql.sync-retry-count", 3);
                    yaml.set("mysql.operation-poll-interval", null);
                    yaml.set("mysql.cross-server-sync", true);
                    yaml.setComments("mysql.cross-server-sync", List.of(
                            "Keeps the data of players in sync between the servers using this database, and lets you change the data",
                            "of players who are offline or online on another server. Only turn it off if just one server uses this database.",
                            "Changing this requires a restart"));
                    yaml.set("mysql.redis.enabled", false);
                    yaml.set("mysql.redis.host", "127.0.0.1");
                    yaml.set("mysql.redis.port", 6379);
                    yaml.set("mysql.redis.username", "");
                    yaml.set("mysql.redis.password", "");
                    yaml.set("mysql.redis.ssl", false);
                    yaml.setComments("mysql.redis", List.of(
                            "Servers tell each other about changes through Redis if it's enabled, otherwise through your proxy (BungeeCord or Velocity)",
                            "if the server is set up for one. Redis also works when nobody is online on the server making the change, and it's faster.",
                            "Without either, servers still find the changes within a second.",
                            "Changing this requires a restart"));
                    yaml.set("config-version", 16);
                }
        );
    }
}
