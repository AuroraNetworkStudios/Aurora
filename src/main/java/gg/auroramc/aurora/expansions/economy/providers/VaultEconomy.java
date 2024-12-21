package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class VaultEconomy implements AuroraEconomy {
    private final Economy economy;

    public VaultEconomy() {
        this.economy = resolveEconomy();
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        economy.withdrawPlayer(player, amount);
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        economy.depositPlayer(player, amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        return economy.getBalance(player);
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return economy.getBalance(player) >= amount;
    }

    @Override
    public boolean supportsCurrency() {
        return false;
    }

    @Override
    public boolean validateCurrency(String currency) {
        return false;
    }

    private Economy resolveEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.SYNC_ONLY;
    }
}
