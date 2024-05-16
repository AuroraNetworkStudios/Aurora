package gg.auroramc.auroralib.api;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.expansions.ExpansionManager;
import gg.auroramc.auroralib.api.placeholder.PlaceholderHandler;
import gg.auroramc.auroralib.api.placeholder.PlaceholderHandlerRegistry;
import gg.auroramc.auroralib.api.user.AuroraUser;
import gg.auroramc.auroralib.api.user.UserManager;
import gg.auroramc.auroralib.expansions.economy.AuroraEconomy;
import gg.auroramc.auroralib.expansions.economy.EconomyExpansion;
import gg.auroramc.auroralib.expansions.numberformat.NumberFormatExpansion;
import gg.auroramc.auroralib.expansions.region.RegionExpansion;

import java.util.UUID;

public class AuroraAPI {
    public static ExpansionManager getExpansions() {
        return AuroraLib.getExpansionManager();
    }

    /**
     * @return the logger instance of AuroraLib
     */
    public static AuroraLogger getLogger() {
        return AuroraLib.logger();
    }

    /**
     * Use this to interact with user data.
     *
     * @return the user manager instance
     */
    public static UserManager getUserManager() {
        return AuroraLib.getUserManager();
    }

    /**
     * Get the default economy provider.
     *
     * @return the default economy provider
     */
    public static AuroraEconomy getDefaultEconomy() {
        return AuroraLib.getExpansionManager().getExpansion(EconomyExpansion.class).getDefaultEconomy();
    }

    /**
     * Get an economy provider by its plugin name.
     *
     * @param providerPluginName the plugin name of the economy provider
     * @return the economy provider
     */
    public static AuroraEconomy getEconomy(String providerPluginName) {
        return AuroraLib.getExpansionManager().getExpansion(EconomyExpansion.class).getEconomy(providerPluginName);
    }

    /**
     * Get an AuroraUser by its UUID.
     *
     * @param uuid the UUID of the user
     * @return the AuroraUser object
     */
    public static AuroraUser getUser(UUID uuid) {
        return AuroraLib.getUserManager().getUser(uuid);
    }

    /**
     * Format a whole number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(long number) {
        return AuroraLib.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatWholeNumber(number);
    }

    /**
     * Format a decimal number into a human-readable format.
     *
     * @param number the number to format
     * @return the formatted number
     */
    public static String formatNumber(double number) {
        return AuroraLib.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatDecimalNumber(number);
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
        return AuroraLib.getExpansionManager().getExpansion(RegionExpansion.class);
    }
}
