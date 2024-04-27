package gg.auroramc.auroralib;

import gg.auroramc.auroralib.api.menu.MenuManager;
import gg.auroramc.auroralib.config.Config;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuroraLib extends JavaPlugin implements Listener {

    @Getter
    private static boolean PAPIEnabled = false;
    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static Config libConfig;
    @Getter
    private static MenuManager menuManager;


    @Override
    public void onEnable() {
        libConfig = new Config(this, "config.yml");
        menuManager = new MenuManager(this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) PAPIEnabled = true;
    }
}
