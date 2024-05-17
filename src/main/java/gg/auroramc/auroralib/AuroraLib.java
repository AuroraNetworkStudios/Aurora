package gg.auroramc.auroralib;

import gg.auroramc.auroralib.api.AuroraLogger;
import gg.auroramc.auroralib.api.dependency.Dep;
import gg.auroramc.auroralib.api.dependency.DependencyManager;
import gg.auroramc.auroralib.api.menu.MenuManager;
import gg.auroramc.auroralib.api.placeholder.AuroraPlaceholderExpansion;
import gg.auroramc.auroralib.api.user.UserManager;
import gg.auroramc.auroralib.config.Config;
import gg.auroramc.auroralib.api.expansions.ExpansionManager;
import gg.auroramc.auroralib.expansions.economy.EconomyExpansion;
import gg.auroramc.auroralib.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.auroralib.expansions.region.RegionExpansion;
import gg.auroramc.auroralib.expansions.worldguard.WorldGuardExpansion;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuroraLib extends JavaPlugin implements Listener {

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static Config libConfig;
    @Getter
    private static MenuManager menuManager;
    @Getter
    private static AuroraLib instance;
    @Getter
    private static UserManager userManager;
    @Getter
    private static ExpansionManager expansionManager;
    @Getter
    private static boolean disabling = false;

    private static final AuroraLogger l = new AuroraLogger();

    public static AuroraLogger logger() {
        return l;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        libConfig = new Config();
        libConfig.load();

        userManager = new UserManager();
        menuManager = new MenuManager(this);
        setupExpansions();

        if (DependencyManager.hasDep(Dep.PAPI)) {
            if (new AuroraPlaceholderExpansion().register()) {
                logger().info("PlaceholderAPI expansion registered");
            } else {
                logger().warning("Couldn't register PlaceholderAPI expansion");
            }
        }
    }

    @Override
    public void onDisable() {
        disabling = true;
        userManager.stopTasksAndSaveAllData();
        expansionManager.getExpansion(RegionExpansion.class).saveAllRegions(false);
    }

    private void setupExpansions() {
        expansionManager = new ExpansionManager();
        expansionManager.loadExpansion(EconomyExpansion.class);
        expansionManager.loadExpansion(NumberFormatExpansion.class);
        expansionManager.loadExpansion(RegionExpansion.class);
        if (DependencyManager.hasDep(Dep.WORLDGUARD)) {
            expansionManager.loadExpansion(WorldGuardExpansion.class);
        }
    }
}
