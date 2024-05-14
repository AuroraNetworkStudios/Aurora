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

import java.util.UUID;

public class AuroraAPI {
    public static ExpansionManager getExpansions() {
        return AuroraLib.getExpansionManager();
    }

    public static AuroraLogger getLogger() {
        return AuroraLib.logger();
    }

    public static UserManager getUserManager() {
        return AuroraLib.getUserManager();
    }

    public static AuroraEconomy getDefaultEconomy() {
        return AuroraLib.getExpansionManager().getExpansion(EconomyExpansion.class).getDefaultEconomy();
    }

    public static AuroraEconomy getEconomy(String providerPluginName) {
        return AuroraLib.getExpansionManager().getExpansion(EconomyExpansion.class).getEconomy(providerPluginName);
    }

    public static AuroraUser getUser(UUID uuid) {
        return AuroraLib.getUserManager().getUser(uuid);
    }

    public static String formatNumber(long number) {
        return AuroraLib.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatWholeNumber(number);
    }

    public static String formatNumber(double number) {
        return AuroraLib.getExpansionManager().getExpansion(NumberFormatExpansion.class).formatDecimalNumber(number);
    }

    public static void registerPlaceholderHandler(PlaceholderHandler handler) {
        PlaceholderHandlerRegistry.addHandler(handler);
    }

    public static void removePlaceholderHandler(PlaceholderHandler handler) {
        PlaceholderHandlerRegistry.removeHandler(handler);
    }
}
