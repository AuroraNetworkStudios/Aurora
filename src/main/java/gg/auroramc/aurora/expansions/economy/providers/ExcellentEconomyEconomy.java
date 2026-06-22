package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget;
import su.nightexpress.excellenteconomy.api.currency.operation.OperationContext;

public class ExcellentEconomyEconomy implements AuroraEconomy {
    private final ExcellentEconomyAPI api;
    private final OperationContext context = OperationContext.custom("Aurora")
            .silentFor(NotificationTarget.USER, NotificationTarget.EXECUTOR, NotificationTarget.CONSOLE_LOGGER);

    public ExcellentEconomyEconomy() {
        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServicesManager().getRegistration(ExcellentEconomyAPI.class);
        if (provider == null) {
            throw new IllegalStateException("ExcellentEconomy API service is not available.");
        }
        this.api = provider.getProvider();
    }

    public static boolean isApiAvailable() {
        return Bukkit.getServicesManager().getRegistration(ExcellentEconomyAPI.class) != null;
    }

    private String getCurrencyId(String currency) {
        return currency == null || currency.equals("default") ? "coins" : currency;
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        api.withdraw(player, getCurrencyId(currency), amount, context);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        api.deposit(player, getCurrencyId(currency), amount, context);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return api.getBalance(player, getCurrencyId(currency));
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return api.hasCurrency(getCurrencyId(currency));
    }

    @Override
    public boolean supportsCurrency() {
        return true;
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.SYNC_ONLY;
    }
}
