package gg.auroramc.aurora;

import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.ExpansionManager;
import gg.auroramc.aurora.api.menu.MenuManager;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.user.UserMetaHolder;
import gg.auroramc.aurora.commands.CommandManager;
import gg.auroramc.aurora.config.Config;
import gg.auroramc.aurora.config.MessageConfig;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.expansions.entity.EntityExpansion;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.expansions.placeholder.PlaceholderExpansion;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import gg.auroramc.aurora.expansions.worldguard.WorldGuardExpansion;
import gg.auroramc.aurora.hooks.LuckPermsHook;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Aurora extends JavaPlugin implements Listener {

    private CommandManager commandManager;

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static Config libConfig;
    @Getter
    private static MessageConfig messageConfig;
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
    public void onLoad() {
        expansionManager = new ExpansionManager();
        expansionManager.preloadExpansion(LeaderboardExpansion.class);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        libConfig = new Config();
        libConfig.load();
        MessageConfig.saveDefault();
        messageConfig = new MessageConfig();
        messageConfig.load();

        commandManager = new CommandManager(this);
        commandManager.reload();

        userManager = new UserManager();
        userManager.registerUserDataHolder(UserMetaHolder.class);

        menuManager = new MenuManager(this);
        setupExpansions();

        if(DependencyManager.hasDep("LuckPerms")) {
            LuckPermsHook.registerListeners();
        }
    }

    @Override
    public void onDisable() {
        disabling = true;
        userManager.stopTasksAndSaveAllData(true);
        if (libConfig.getBlockTracker().getEnabled()) {
            expansionManager.getExpansion(RegionExpansion.class).saveAllRegions(false);
            expansionManager.getExpansion(RegionExpansion.class).dispose();
        }
        expansionManager.getExpansion(LeaderboardExpansion.class).dispose();
    }

    private void setupExpansions() {
        expansionManager.loadExpansion(PlaceholderExpansion.class);
        expansionManager.loadExpansion(EconomyExpansion.class);
        expansionManager.loadExpansion(NumberFormatExpansion.class);
        expansionManager.loadExpansion(ItemExpansion.class);
        expansionManager.loadExpansion(EntityExpansion.class);
        expansionManager.loadExpansion(LeaderboardExpansion.class);
        expansionManager.loadExpansion(GuiExpansion.class);

        if (DependencyManager.hasDep(Dep.WORLDGUARD)) {
            expansionManager.loadExpansion(WorldGuardExpansion.class);
        }

        if (libConfig.getBlockTracker().getEnabled()) {
            expansionManager.loadExpansion(RegionExpansion.class);
        }
    }

    public void reload() {
        // Reloading lib config is considered unsafe if storage options are changed
        libConfig = new Config();
        libConfig.load();
        messageConfig = new MessageConfig();
        messageConfig.load();
        commandManager.reload();
        expansionManager.getExpansion(GuiExpansion.class).reload();
    }
}
