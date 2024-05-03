package gg.auroramc.auroralib.api.config;

import gg.auroramc.auroralib.api.config.decorators.IgnoreField;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public abstract class AuroraConfig {
    @IgnoreField
    private final File file;

    @IgnoreField
    private final YamlConfiguration rawConfiguration;

    public AuroraConfig(File file) {
        this.file = file;
        this.rawConfiguration = YamlConfiguration.loadConfiguration(file);
        ConfigManager.load(this, rawConfiguration);
    }

    public void saveChanges() {
        ConfigManager.save(this, rawConfiguration, file);
    }

    public CompletableFuture<Void> saveChangesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            ConfigManager.save(this, rawConfiguration, file);
            return null;
        });
    }

    public YamlConfiguration getRawConfig() {
        return rawConfiguration;
    }
}
