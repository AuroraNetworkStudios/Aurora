package gg.auroramc.aurora.api;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.entity.EntityManager;
import gg.auroramc.aurora.api.item.ItemManager;
import gg.auroramc.aurora.api.localization.LanguageProvider;
import gg.auroramc.aurora.api.menu.Requirement;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import gg.auroramc.aurora.internal.placeholder.PlaceholderHandlerRegistry;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.economy.AuroraEconomy;
import gg.auroramc.aurora.api.leaderboard.Leaderboards;
import gg.auroramc.aurora.api.region.RegionManager;
import gg.auroramc.aurora.internal.expansions.economy.NoEconomy;
import gg.auroramc.aurora.internal.expansions.region.NoRegions;
import gg.auroramc.aurora.internal.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.internal.expansions.entity.EntityExpansion;
import gg.auroramc.aurora.internal.expansions.item.ItemExpansion;
import gg.auroramc.aurora.internal.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.internal.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.internal.expansions.region.RegionExpansion;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AuroraAPI {
    /**
     * The version of Aurora's API. It changes when the API changes in a way plugins built for an older one break.
     * A plugin built for this API sets {@code aurora-api: 3} in its plugin.yml, Aurora warns about those that don't.
     */
    public static final int API_VERSION = 3;

    /**
     * @return the logger instance of Aurora
     */
    public static AuroraLogger getLogger() {
        return Aurora.logger();
    }

    /**
     * Creates a custom logger with you plugin name as prefix.
     *
     * @param plugin    the name of your plugin
     * @param debugMode the supplier for the debug parameter
     * @return the newly created logger
     */
    public static AuroraLogger createLogger(String plugin, Supplier<Boolean> debugMode) {
        return new AuroraLogger(plugin, debugMode);
    }

    /**
     * Use this to interact with user data.
     *
     * @return the user manager instance
     */
    public static UserManager getUserManager() {
        return Aurora.getUserManager();
    }

    /**
     * The default economy, never null. Without an economy plugin it isn't {@link AuroraEconomy#isAvailable() available}:
     * balances are 0 and nothing can be taken or given.
     */
    public static AuroraEconomy getEconomy() {
        var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
        var economy = expansion == null ? null : expansion.getDefaultEconomy();
        return economy != null ? economy : NoEconomy.INSTANCE;
    }

    /**
     * Get an economy provider by its plugin name.
     *
     * @param providerPluginName the plugin name of the economy provider
     * @return the economy provider, or null if it isn't installed (or no economy plugin is)
     */
    public static AuroraEconomy getEconomy(String providerPluginName) {
        var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
        return expansion == null || providerPluginName == null ? null : expansion.getEconomy(providerPluginName);
    }

    /**
     * The leaderboards.
     */
    public static Leaderboards getLeaderboards() {
        return Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
    }

    /**
     * Get an AuroraUser by its UUID.
     *
     * @param uuid the UUID of the user
     * @return the AuroraUser object
     */
    public static AuroraUser getUser(UUID uuid) {
        return Aurora.getUserManager().getUser(uuid);
    }

    /**
     * Format a whole number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(long number) {
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatWholeNumber(number);
    }

    /**
     * Format a decimal number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(double number) {
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatDecimalNumber(number);
    }

    /**
     * Format a decimal number into its human-readable short format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumberShort(double number) {
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatNumberShort(number);
    }

    /**
     * Format a whole number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(Player player, long number) {
        var locale = Aurora.getLanguageProvider().getPlayerLocale(player);
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatWholeNumber(locale, number);
    }

    /**
     * Format a decimal number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(Player player, double number) {
        var locale = Aurora.getLanguageProvider().getPlayerLocale(player);
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatDecimalNumber(locale, number);
    }

    /**
     * Format a decimal number into its human-readable short format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumberShort(Player player, double number) {
        var locale = Aurora.getLanguageProvider().getPlayerLocale(player);
        return Aurora.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatNumberShort(locale, number);
    }

    /**
     * Register a placeholder handler.
     *
     * @param handler the handler to register
     */
    public static void registerPlaceholderHandler(PlaceholderHandler handler) {
        PlaceholderHandlerRegistry.addHandler(handler);
    }

    /**
     * Remove a placeholder handler.
     *
     * @param handler the handler to remove
     */
    public static void removePlaceholderHandler(PlaceholderHandler handler) {
        PlaceholderHandlerRegistry.removeHandler(handler);
    }

    public static RegionManager getRegionManager() {
        var regions = Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
        return regions != null ? regions : NoRegions.INSTANCE;
    }

    public static ItemManager getItemManager() {
        return Aurora.getExpansionManager().getExpansion(ItemExpansion.class).getItemManager();
    }

    public static EntityManager getEntityManager() {
        return Aurora.getExpansionManager().getExpansion(EntityExpansion.class).getEntityManager();
    }

    public static void registerRequirementHandler(String name, BiFunction<Player, String[], Boolean> resolver) {
        Requirement.register(name, resolver);
    }

    public static void registerCommandDispatcherActionHandler(String id, BiConsumer<Player, String> handler) {
        CommandDispatcher.registerActionHandler(id, handler);
    }

    public static void setLanguageProvider(LanguageProvider provider) {
        Aurora.setLanguageProvider(provider);
    }

    public static LanguageProvider getLanguageProvider() {
        return Aurora.getLanguageProvider();
    }
}
