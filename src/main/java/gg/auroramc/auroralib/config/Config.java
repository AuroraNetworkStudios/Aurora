package gg.auroramc.auroralib.config;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.config.AuroraConfig;
import gg.auroramc.auroralib.api.config.ConfigManager;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("FieldMayBeFinal")
@Getter
public class Config extends AuroraConfig {
    private Boolean debug = false;
    private NumberFormatConfig numberFormat;
    private Integer userAutoSaveInMinutes = 30;
    private String storageType = "yaml";
    private MySqlConfig mysql;


    public Config() {
        super(new File(AuroraLib.getInstance().getDataFolder(), "config.yml"));
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return List.of(
                (yaml) -> {
                    yaml.set("user-auto-save-in-minutes", 30);
                    yaml.set("storage-type", "yaml");
                    ConfigManager.saveObject(new NumberFormatConfig(), yaml.createSection("number-format"));
                    ConfigManager.saveObject(new MySqlConfig(), yaml.createSection("mysql"));
                    yaml.set("config-version", 1);
                }
        );
    }
}
