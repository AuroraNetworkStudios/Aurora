package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import me.qKing12.RoyaleEconomy.RoyaleEconomy;
import org.bukkit.entity.Player;

public class RoyaleEconomyBank implements AuroraEconomy {
    @Override
    public void withdraw(Player player, String currency, double amount) {
        RoyaleEconomy.apiHandler.balance.removeBankBalance(player.getUniqueId().toString(), amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        RoyaleEconomy.apiHandler.balance.addBankBalance(player.getUniqueId().toString(), amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return RoyaleEconomy.apiHandler.balance.getBankBalance(player.getUniqueId().toString());
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return false;
    }

    @Override
    public boolean supportsCurrency() {
        return false;
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.SYNC_ONLY;
    }
}
