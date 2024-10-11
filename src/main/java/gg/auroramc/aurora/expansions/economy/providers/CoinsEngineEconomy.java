package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

public class CoinsEngineEconomy implements AuroraEconomy {
    private String getCurrencyId(String currency) {
        return currency == null || currency.equals("default") ? "coins" : currency;
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        CoinsEngineAPI.removeBalance(player.getUniqueId(), getCurrencyId(currency), amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        CoinsEngineAPI.addBalance(player.getUniqueId(), getCurrencyId(currency), amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return CoinsEngineAPI.getBalance(player.getUniqueId(), getCurrencyId(currency));
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return CoinsEngineAPI.hasCurrency(currency);
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
