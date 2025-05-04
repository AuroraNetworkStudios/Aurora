package gg.auroramc.aurora.config;

import com.google.common.collect.Lists;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.AuroraConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            "Eco",
            "ExecutableItems",
            "ExecutableBlocks",
            "HeadDatabase",
            "ItemsAdder",
            "MMOItems",
            "MythicMobs",
            "Nexo",
            "Oraxen"
    );
    private ItemIdResolverConfig auroraItems;


    @Getter
    public final static class ItemIdResolverConfig {
        private Boolean enableIdResolver = false;
        private Boolean requireExactMatch = false;
        private Map<String, Boolean> hashIncludes;
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
                            "Supported plugins: Vault, Essentials, CMI, PlayerPoints, CoinsEngine, EcoBits, EliteMobs", "RoyaleEconomy", "RoyaleEconomyBank",
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
                }
        );
    }
}
