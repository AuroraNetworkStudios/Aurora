package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;

public class EssentialsEconomy implements AuroraEconomy {
    @Override
    public void withdraw(Player player, double amount) {
        DependencyManager.getEssentials().withdrawMoney(player, amount);
    }

    @Override
    public void deposit(Player player, double amount) {
        DependencyManager.getEssentials().depositMoney(player, amount);
    }

    @Override
    public double getBalance(Player player) {
        return DependencyManager.getEssentials().getBalance(player);
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return getBalance(player) >= amount;
    }
}
