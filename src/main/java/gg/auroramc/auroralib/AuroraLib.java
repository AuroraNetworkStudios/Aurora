package gg.auroramc.auroralib;

import gg.auroramc.auroralib.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuroraLib extends JavaPlugin {

    private static boolean PAPIEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) PAPIEnabled = true;

        var debug = getConfig().getBoolean("debug", false);
        ConfigManager.setDebug(debug);

        if (debug) {
            getLogger().info("AuroraLib enabled in debug mode!");
        } else {
            getLogger().info("AuroraLib enabled!");
        }
    }

    public static boolean isPAPIEnabled() {
        return PAPIEnabled;
    }
}
