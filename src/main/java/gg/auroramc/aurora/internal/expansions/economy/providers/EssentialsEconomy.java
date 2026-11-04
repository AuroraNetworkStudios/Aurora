package gg.auroramc.aurora.internal.expansions.economy.providers;

import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.api.economy.EconomyProvider;
import org.bukkit.entity.Player;

public class EssentialsEconomy implements EconomyProvider {
    @Override
    public void withdraw(Player player, String currency, double amount) {
        DependencyManager.getEssentials().withdrawMoney(player, amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        DependencyManager.getEssentials().depositMoney(player, amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return DependencyManager.getEssentials().getBalance(player);
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
        return ThreadSafety.ANY;
    }
}
