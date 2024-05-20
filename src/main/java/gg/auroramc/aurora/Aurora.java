package gg.auroramc.aurora;

import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.menu.MenuManager;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.user.UserMetaHolder;
import gg.auroramc.aurora.config.Config;
import gg.auroramc.aurora.api.expansions.ExpansionManager;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.expansions.placeholder.PlaceholderExpansion;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import gg.auroramc.aurora.expansions.worldguard.WorldGuardExpansion;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Aurora extends JavaPlugin implements Listener {

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static Config libConfig;
    @Getter
    private static MenuManager menuManager;
    @Getter
    private static Aurora instance;
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
        userManager.registerUserDataHolder(UserMetaHolder.class);

        menuManager = new MenuManager(this);
        setupExpansions();
    }

    @Override
    public void onDisable() {
        disabling = true;
        userManager.stopTasksAndSaveAllData();
        expansionManager.getExpansion(RegionExpansion.class).saveAllRegions(false);
    }

    private void setupExpansions() {
        expansionManager = new ExpansionManager();
        expansionManager.loadExpansion(PlaceholderExpansion.class);
        expansionManager.loadExpansion(EconomyExpansion.class);
        expansionManager.loadExpansion(NumberFormatExpansion.class);

        if (DependencyManager.hasDep(Dep.WORLDGUARD)) {
            expansionManager.loadExpansion(WorldGuardExpansion.class);
        }

        if (libConfig.getBlockTracker().getEnabled()) {
            expansionManager.loadExpansion(RegionExpansion.class);
        }
    }
}
