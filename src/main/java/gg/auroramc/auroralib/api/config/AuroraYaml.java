package gg.auroramc.auroralib.api.config;

import gg.auroramc.auroralib.api.config.decorators.IgnoreField;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public abstract class AuroraYaml {
    @Getter
    @IgnoreField
    protected File file;

    @IgnoreField
    protected YamlConfiguration rawConfiguration;

    @IgnoreField
    protected JavaPlugin plugin;

    @Getter
    @IgnoreField
    protected String path;


    public AuroraYaml(JavaPlugin plugin, String path) {
        if(plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }
        this.plugin = plugin;
        this.path = path;
        this.file = new File(plugin.getDataFolder(), path);
        this.rawConfiguration = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public AuroraYaml(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.path = file.getAbsolutePath();
        this.file = file;
        this.rawConfiguration = YamlConfiguration.loadConfiguration(file);
        load();
    }

    protected void load() {
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
