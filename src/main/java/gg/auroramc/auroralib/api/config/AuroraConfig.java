package gg.auroramc.auroralib.api.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public abstract class AuroraConfig extends AuroraYaml {

    protected int configVersion = 1;

    public AuroraConfig(JavaPlugin plugin, String path) {
        super(plugin, path);
    }

    @Override
    protected void load() {
        if (rawConfiguration.getInt("config_version") < getVersion()) {
            ConfigManager.syncConfigurations(
                    rawConfiguration,
                    YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(path), StandardCharsets.UTF_8))
            );

            try {
                rawConfiguration.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update config file " + file.getAbsolutePath());
            }

            ConfigManager.load(this, rawConfiguration);
        } else {
            ConfigManager.load(this, rawConfiguration);
        }
    }

    public abstract int getVersion();
}
