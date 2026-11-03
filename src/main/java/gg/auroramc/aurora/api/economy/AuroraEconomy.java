package gg.auroramc.aurora.api.economy;

import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.entity.Player;

public interface AuroraEconomy {
    /**
     * False when no economy plugin is installed. Then balances are 0 and nothing can be taken or given.
     */
    default boolean isAvailable() {
        return true;
    }

    default void withdraw(Player player, double amount) {
        withdraw(player, "default", amount);
    }
    default void deposit(Player player, double amount) {
        deposit(player, "default", amount);
    }
    default double getBalance(Player player) {
        return getBalance(player, "default");
    }
    default boolean hasBalance(Player player, double amount) {
        return hasBalance(player, "default", amount);
    }

    void withdraw(Player player, String currency, double amount);
    void deposit(Player player, String currency, double amount);
    double getBalance(Player player, String currency);
    boolean hasBalance(Player player, String currency, double amount);

    /**
     * Takes the money only if they have all of it.
     *
     * @return whether the money was taken
     */
    default boolean tryWithdraw(Player player, String currency, double amount) {
        if (!hasBalance(player, currency, amount)) return false;
        withdraw(player, currency, amount);
        return true;
    }
    boolean validateCurrency(String currency);
    boolean supportsCurrency();

    ThreadSafety getThreadSafety();
}
