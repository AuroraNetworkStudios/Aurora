package gg.auroramc.aurora.expansions.economy;

import org.bukkit.entity.Player;

public interface AuroraEconomy {
    void withdraw(Player player, double amount);
    void deposit(Player player, double amount);
    double getBalance(Player player);
    boolean hasBalance(Player player, double amount);
}
