package gg.auroramc.auroralib.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public abstract class AuroraConfig {

    private final File dataFolder;
    private final String filename;
    private final File configFile;
    private YamlConfiguration rawConfiguration;

    public AuroraConfig(JavaPlugin plugin, String filename) {
        this.dataFolder = plugin.getDataFolder();
        this.filename = filename;
        this.configFile = new File(dataFolder, filename);
    }

    public void save() {
        ConfigManager.save(this, rawConfiguration, configFile);
    }

    public void load() {
        rawConfiguration = YamlConfiguration.loadConfiguration(configFile);
        ConfigManager.load(this, rawConfiguration);
    }

    public void saveDefaultConfig(JavaPlugin plugin) {
        plugin.saveResource(filename, false);
    }

    public abstract void saveDefaultConfig();

    public File getConfigFile() {
        return configFile;
    }

    public YamlConfiguration getRawConfiguration() {
        return rawConfiguration;
    }

    public String getFilename() {
        return filename;
    }
}
