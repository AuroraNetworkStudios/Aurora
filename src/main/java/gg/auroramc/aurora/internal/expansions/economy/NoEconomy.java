package gg.auroramc.aurora.internal.expansions.economy;

import gg.auroramc.aurora.api.economy.AuroraEconomy;
import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.entity.Player;

// No economy plugin is installed: everyone has nothing, and nothing can be taken or given
public final class NoEconomy implements AuroraEconomy {
    public static final NoEconomy INSTANCE = new NoEconomy();

    private NoEconomy() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
    }

    @Override
    public double getBalance(Player player, String currency) {
        return 0;
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return amount <= 0;
    }

    @Override
    public boolean tryWithdraw(Player player, String currency, double amount) {
        return amount <= 0;
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
        return ThreadSafety.ANY;
    }
}
