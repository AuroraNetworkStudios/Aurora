package gg.auroramc.aurora.config;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.AuroraConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
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
    private MySqlConfig mysql;
    private LeaderboardConfig leaderboards;


    public Config() {
        super(new File(Aurora.getInstance().getDataFolder(), "config.yml"));
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of(
                (yaml) -> {
                    yaml.set("block-tracker.enabled", true);
                    yaml.set("block-tracker.storage-type", yaml.get("block-tracker-storage", "file"));
                    yaml.set("block-tracker-storage", null);
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
                }
        );
    }
}
