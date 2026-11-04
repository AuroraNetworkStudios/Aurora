package gg.auroramc.aurora;

import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.internal.expansions.ExpansionManager;
import gg.auroramc.aurora.internal.localization.AuroraLanguageProvider;
import gg.auroramc.aurora.api.localization.LanguageProvider;
import gg.auroramc.aurora.api.localization.LocalizationProvider;
import gg.auroramc.aurora.internal.menu.MenuManager;
import gg.auroramc.aurora.api.user.UserLocalizationHolder;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.user.UserMetaHolder;
import gg.auroramc.aurora.api.user.UserStashHolder;
import gg.auroramc.aurora.internal.commands.CommandManager;
import gg.auroramc.aurora.api.config.ConfigFile;
import gg.auroramc.aurora.internal.config.Config;
import gg.auroramc.aurora.internal.config.MessageConfig;
import gg.auroramc.aurora.internal.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.internal.expansions.entity.EntityExpansion;
import gg.auroramc.aurora.internal.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.internal.expansions.item.ItemExpansion;
import gg.auroramc.aurora.internal.expansions.itemstash.ItemStashExpansion;
import gg.auroramc.aurora.internal.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.internal.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.internal.expansions.placeholder.PlaceholderExpansion;
import gg.auroramc.aurora.internal.expansions.region.RegionExpansion;
import gg.auroramc.aurora.internal.expansions.worldguard.WorldGuardExpansion;
import gg.auroramc.aurora.internal.hooks.LuckPermsHook;
import gg.auroramc.aurora.internal.hooks.MythicMobsHook;
import gg.auroramc.aurora.internal.hooks.WildToolsHook;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import gg.auroramc.aurora.api.AuroraAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class Aurora extends JavaPlugin implements Listener {

    @Getter
    private CommandManager commandManager;

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private static ConfigFile<Config> libConfigFile;
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
    // Loading, editing and saving player data. Not the JVM's common pool, so a slow database doesn't hold up every
    // plugin that uses it, and other plugins' tasks don't hold up player data.
    @Getter
    private static final ExecutorService storageExecutor = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            var thread = new Thread(runnable, "Aurora Storage #" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final AuroraLogger l = new AuroraLogger();

    public static AuroraLogger logger() {
        return l;
    }

    @Override
    public void onLoad() {
        languageProvider = new AuroraLanguageProvider();

        saveDefaultConfig();
        instance = this;
        libConfigFile = ConfigFile.builder(this, "config.yml", Config.class).migrations(Config.migrations()).build();
        libConfigFile.get();

        var oldFile = new File(getDataFolder(), "messages.yml");
        if (oldFile.exists()) {
            try {
                Files.move(oldFile.toPath(), getDataFolder().toPath().resolve("messages_en.yml"));
            } catch (IOException ignored) {
            }
        }

        var locales = new ArrayList<Locale>();

        for (var lang : getLibConfig().getSupportedLanguages()) {
            var locale = Locale.forLanguageTag(lang);
            var messageConfig = MessageConfig.load(lang);
            messageConfigs.put(locale, messageConfig);
            locales.add(locale);
        }

        languageProvider.setSupportedLocales(locales);
        languageProvider.setFallbackLocale(Locale.forLanguageTag(getLibConfig().getLocale()));

        expansionManager = new ExpansionManager();
        expansionManager.preloadExpansion(LeaderboardExpansion.class);
        // Before plugins that depend on Aurora run their onLoad, where old ones already fail
        warnAboutOldPlugins();
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
            runInSafeMode(() -> LuckPermsHook.registerListeners(), "Failed to register LuckPerms listeners.");
        }

        if (DependencyManager.hasDep("WildTools")) {
            runInSafeMode(() -> WildToolsHook.hook(), "Failed to hook into WildTools.");
        }

        if (DependencyManager.hasDep(Dep.MYTHICMOBS)) {
            runInSafeMode(() -> MythicMobsHook.hook(), "Failed to hook into MythicMobs.");
        }

        commandManager.reload();

        var metrics = new Metrics(this, 23780);
        metrics.addCustomChart(new SimplePie("storage_type", () -> getLibConfig().getStorageType().equals("mysql") ? "mysql" : "yaml"));
    }

    // Plugins built for an older Aurora fail with errors deep in their code, say which ones and why up front
    private void warnAboutOldPlugins() {
        for (var plugin : Bukkit.getPluginManager().getPlugins()) {
            var meta = plugin.getPluginMeta();
            if (plugin == this || !meta.getPluginDependencies().contains("Aurora") && !meta.getPluginSoftDependencies().contains("Aurora")) continue;
            if (auroraApiVersion(plugin) < AuroraAPI.API_VERSION) {
                logger().severe(plugin.getName() + " " + meta.getVersion() + " was made for an older Aurora and won't work with Aurora "
                        + getPluginMeta().getVersion() + ". Update " + plugin.getName() + " to a version made for Aurora " + AuroraAPI.API_VERSION + ".");
            }
        }
    }

    // aurora-api from the plugin's paper-plugin.yml or plugin.yml, 0 if it has none
    private static int auroraApiVersion(org.bukkit.plugin.Plugin plugin) {
        for (var file : List.of("paper-plugin.yml", "plugin.yml")) {
            try (var in = plugin.getResource(file)) {
                if (in == null) continue;
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)).getInt("aurora-api", 0);
            } catch (IOException ignored) {
            }
        }
        return 0;
    }

    public static Config getLibConfig() {
        return libConfigFile.get();
    }

    public static void runInSafeMode(Runnable runnable, String errorMessage) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if(errorMessage != null) {
                logger().warning(errorMessage);
            }
            throwable.printStackTrace();
        }
    }

    public static void runInSafeMode(Runnable runnable) {
        runInSafeMode(runnable, null);
    }

    @Override
    public void onDisable() {
        disabling = true;
        userManager.stopTasksAndSaveAllData(true);
        expansionManager.getExpansion(LeaderboardExpansion.class).dispose();
        storageExecutor.shutdown();
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
            runInSafeMode(() -> expansionManager.loadExpansion(WorldGuardExpansion.class), "Failed to hook into WorldGuard.");
        }

        if (getLibConfig().getBlockTracker().getEnabled()) {
            expansionManager.loadExpansion(RegionExpansion.class);
        }
    }

    public void reload() {
        // Reloading lib config is considered unsafe if storage options are changed
        libConfigFile.reload();

        var locales = new ArrayList<Locale>();

        for (var lang : getLibConfig().getSupportedLanguages()) {
            var locale = Locale.forLanguageTag(lang);
            var messageConfig = MessageConfig.load(lang);
            messageConfigs.put(locale, messageConfig);
            locales.add(locale);
        }

        if (languageProvider instanceof AuroraLanguageProvider) {
            languageProvider.setSupportedLocales(locales);
            languageProvider.setFallbackLocale(Locale.forLanguageTag(getLibConfig().getLocale()));
        }

        localizationProvider.clear();

        for (var msg : messageConfigs.entrySet()) {
            localizationProvider.setLocaleValues(msg.getKey(), msg.getValue().toFlatMap());
        }

        commandManager.reload();
        expansionManager.reloadExpansions();
    }

    public static MessageConfig getMsg(Player player) {
        if (getLibConfig().getUsePerPlayerLocale()) {
            return messageConfigs.get(languageProvider.getPlayerLocale(player));
        }
        return messageConfigs.get(Locale.forLanguageTag(getLibConfig().getLocale()));
    }

    public static MessageConfig getMsg(CommandSender sender) {
        if (sender instanceof Player player) {
            return getMsg(player);
        } else {
            return messageConfigs.get(Locale.forLanguageTag(getLibConfig().getLocale()));
        }
    }
}
