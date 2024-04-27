package gg.auroramc.auroralib.config;

import gg.auroramc.auroralib.api.config.AuroraConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class Config extends AuroraConfig {
    private boolean debug;

    public Config(JavaPlugin plugin, String path) {
        super(plugin, path);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public boolean isDebug() {
        return debug;
    }
}
