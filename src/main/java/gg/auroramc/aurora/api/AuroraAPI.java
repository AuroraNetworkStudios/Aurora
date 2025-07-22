package gg.auroramc.aurora.api;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.entity.EntityManager;
import gg.auroramc.aurora.api.expansions.ExpansionManager;
import gg.auroramc.aurora.api.item.ItemManager;
import gg.auroramc.aurora.api.localization.LanguageProvider;
import gg.auroramc.aurora.api.menu.Requirement;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandlerRegistry;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.expansions.entity.EntityExpansion;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AuroraAPI {
    public static ExpansionManager getExpansions() {
        return Aurora.getExpansionManager();
    }

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
     * Get the default economy provider.
     *
     * @return the default economy provider
     */
    public static AuroraEconomy getDefaultEconomy() {
        return Aurora.getExpansionManager().getExpansion(EconomyExpansion.class).getDefaultEconomy();
    }

    /**
     * Get an economy provider by its plugin name.
     *
     * @param providerPluginName the plugin name of the economy provider
     * @return the economy provider
     */
    public static AuroraEconomy getEconomy(String providerPluginName) {
        return Aurora.getExpansionManager().getExpansion(EconomyExpansion.class).getEconomy(providerPluginName);
    }

    /**
     * Get the leaderboard expansion.
     *
     * @return the leaderboard expansion
     */
    public static LeaderboardExpansion getLeaderboards() {
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

    public static RegionExpansion getRegionManager() {
        return Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
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
}
