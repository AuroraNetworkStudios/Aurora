package gg.auroramc.aurora;

import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.ExpansionManager;
import gg.auroramc.aurora.api.localization.AuroraLanguageProvider;
import gg.auroramc.aurora.api.localization.LanguageProvider;
import gg.auroramc.aurora.api.localization.LocalizationProvider;
import gg.auroramc.aurora.api.menu.MenuManager;
import gg.auroramc.aurora.api.user.UserLocalizationHolder;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.user.UserMetaHolder;
import gg.auroramc.aurora.commands.CommandManager;
import gg.auroramc.aurora.config.Config;
import gg.auroramc.aurora.config.MessageConfig;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.expansions.entity.EntityExpansion;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import gg.auroramc.aurora.api.user.UserStashHolder;
import gg.auroramc.aurora.expansions.itemstash.ItemStashExpansion;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.expansions.placeholder.PlaceholderExpansion;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import gg.auroramc.aurora.expansions.worldguard.WorldGuardExpansion;
import gg.auroramc.aurora.hooks.LuckPermsHook;
import gg.auroramc.aurora.hooks.MythicMobsHook;
import gg.auroramc.aurora.hooks.WildToolsHook;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Aurora extends JavaPlugin implements Listener {

    @Getter
    private CommandManager commandManager;

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static Config libConfig;
    @Getter
    private static Map<Locale, MessageConfig> messageConfigs = new HashMap<>();
    @Getter
    private static MenuManager menuManager;
    @Getter
    private static Aurora instance;
    @Getter
    private static UserManager userManager;
    @Getter
    private static ExpansionManager expansionManager;
    @Getter
    @Setter
    private static LanguageProvider languageProvider;
    @Getter
    private static LocalizationProvider localizationProvider;
    @Getter
    private static boolean disabling = false;

    private static final AuroraLogger l = new AuroraLogger();

    public static AuroraLogger logger() {
        return l;
    }

    @Override
    public void onLoad() {
        languageProvider = new AuroraLanguageProvider();

        saveDefaultConfig();
        instance = this;
        libConfig = new Config();
        libConfig.load();

        var oldFile = new File(getDataFolder(), "messages.yml");
        if (oldFile.exists()) {
            try {
                Files.move(oldFile.toPath(), getDataPath().resolve("messages_en.yml"));
            } catch (IOException ignored) {
            }
        }

        var locales = new ArrayList<Locale>();

        for (var lang : libConfig.getSupportedLanguages()) {
            var locale = Locale.forLanguageTag(lang);
            MessageConfig.saveDefault(lang);
            var messageConfig = new MessageConfig(lang);
            messageConfig.load();
            messageConfigs.put(locale, messageConfig);
            locales.add(locale);
        }

        languageProvider.setSupportedLocales(locales);
        languageProvider.setFallbackLocale(Locale.forLanguageTag(libConfig.getFallbackLocale()));

        expansionManager = new ExpansionManager();
        expansionManager.preloadExpansion(LeaderboardExpansion.class);

    }

    @Override
    public void onEnable() {


        // Plugins who wish to override languageProvider, should do it in their onLoad lifecycle method.
        localizationProvider = new LocalizationProvider(languageProvider);

        for (var msg : messageConfigs.entrySet()) {
            localizationProvider.setLocaleValues(msg.getKey(), msg.getValue().toFlatMap());
        }

        commandManager = new CommandManager(this);

        userManager = new UserManager();
        userManager.registerUserDataHolder(UserMetaHolder.class);
        userManager.registerUserDataHolder(UserStashHolder.class);
        userManager.registerUserDataHolder(UserLocalizationHolder.class);

        menuManager = new MenuManager(this);
        setupExpansions();

        if (DependencyManager.hasDep("LuckPerms")) {
            LuckPermsHook.registerListeners();
        }

        if (DependencyManager.hasDep("WildTools")) {
            WildToolsHook.hook();
        }

        if (DependencyManager.hasDep(Dep.MYTHICMOBS)) {
            MythicMobsHook.hook();
        }

        commandManager.reload();

        var metrics = new Metrics(this, 23780);
        metrics.addCustomChart(new SimplePie("storage_type", () -> libConfig.getStorageType().equals("mysql") ? "mysql" : "yaml"));
    }

    @Override
    public void onDisable() {
        disabling = true;
        userManager.stopTasksAndSaveAllData(true);
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
        expansionManager.loadExpansion(ItemStashExpansion.class);

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

        var locales = new ArrayList<Locale>();

        for (var lang : libConfig.getSupportedLanguages()) {
            var locale = Locale.forLanguageTag(lang);
            MessageConfig.saveDefault(lang);
            var messageConfig = new MessageConfig(lang);
            messageConfig.load();
            messageConfigs.put(locale, messageConfig);
            locales.add(locale);
        }

        if (languageProvider instanceof AuroraLanguageProvider) {
            languageProvider.setSupportedLocales(locales);
            languageProvider.setFallbackLocale(Locale.forLanguageTag(libConfig.getFallbackLocale()));
        }

        localizationProvider.clear();

        for (var msg : messageConfigs.entrySet()) {
            localizationProvider.setLocaleValues(msg.getKey(), msg.getValue().toFlatMap());
        }


        commandManager.reload();
        expansionManager.reloadExpansions();
    }

    public static MessageConfig getMsg(Player player) {
        return messageConfigs.get(languageProvider.getPlayerLocale(player));
    }

    public static MessageConfig getMsg(CommandSender sender) {
        if (sender instanceof Player player) {
            return getMsg(player);
        } else {
            return messageConfigs.get(Locale.forLanguageTag(Aurora.getLibConfig().getFallbackLocale()));
        }
    }
}
