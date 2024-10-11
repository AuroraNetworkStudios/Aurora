package gg.auroramc.aurora.expansions.economy;

import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.entity.Player;

public interface AuroraEconomy {
    default void withdraw(Player player, double amount) {
        withdraw(player, null, amount);
    }
    default void deposit(Player player, double amount) {
        deposit(player, null, amount);
    }
    default double getBalance(Player player) {
        return getBalance(player, null);
    }
    default boolean hasBalance(Player player, double amount) {
        return hasBalance(player, null, amount);
    }

    void withdraw(Player player, String currency, double amount);
    void deposit(Player player, String currency, double amount);
    double getBalance(Player player, String currency);
    boolean hasBalance(Player player, String currency, double amount);
    boolean validateCurrency(String currency);
    boolean supportsCurrency();

    ThreadSafety getThreadSafety();
}
