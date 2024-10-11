package gg.auroramc.aurora.expansions.economy.providers;

import com.willfp.ecobits.currencies.Currencies;
import com.willfp.ecobits.currencies.Currency;
import com.willfp.ecobits.currencies.CurrencyUtils;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class EcoBitsEconomy implements AuroraEconomy {
    private Currency getCurrency(String currency) {
        if (currency == null || Currencies.getByID(currency) == null) {
            return Currencies.values().getFirst();
        }
        return Currencies.getByID(currency);
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        CurrencyUtils.adjustBalance(player, getCurrency(currency), BigDecimal.valueOf(-amount));
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        CurrencyUtils.adjustBalance(player, getCurrency(currency), BigDecimal.valueOf(amount));
    }

    @Override
    public double getBalance(Player player, String currency) {
        return CurrencyUtils.getBalance(player, getCurrency(currency)).doubleValue();
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return Currencies.getByID(currency) != null;
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
