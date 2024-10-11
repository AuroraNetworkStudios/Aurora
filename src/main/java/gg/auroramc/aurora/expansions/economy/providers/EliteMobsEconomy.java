package gg.auroramc.aurora.expansions.economy.providers;

import com.magmaguy.elitemobs.economy.EconomyHandler;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;

public class EliteMobsEconomy implements AuroraEconomy {
    @Override
    public void withdraw(Player player, String currency, double amount) {
        EconomyHandler.subtractCurrency(player.getUniqueId(), amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        EconomyHandler.addCurrency(player.getUniqueId(), amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return EconomyHandler.checkCurrency(player.getUniqueId());
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player) >= amount;
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
        return ThreadSafety.SYNC_ONLY;
    }
}
