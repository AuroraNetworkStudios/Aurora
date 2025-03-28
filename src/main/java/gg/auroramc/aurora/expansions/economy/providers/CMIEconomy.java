package gg.auroramc.aurora.expansions.economy.providers;

import com.Zrips.CMI.CMI;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;

public class CMIEconomy implements AuroraEconomy {
    @Override
    public void withdraw(Player player, String currency, double amount) {
        CMI.getInstance().getPlayerManager().getUser(player).withdraw(amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        CMI.getInstance().getPlayerManager().getUser(player).deposit(amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return CMI.getInstance().getPlayerManager().getUser(player).getBalance();
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return CMI.getInstance().getPlayerManager().getUser(player).getBalance() >= amount;
    }

    @Override
    public boolean supportsCurrency() {
        return false;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return false;
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.ANY;
    }
}
