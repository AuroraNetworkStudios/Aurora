package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class VaultEconomy implements AuroraEconomy {
    private volatile Economy economy;
    private boolean warnedMissing = false;

    public VaultEconomy() {
        this.economy = resolveEconomy();
    }

    // The economy plugin may register its provider after we hooked, so it's looked up again until found
    private Economy economy() {
        if (economy == null) {
            economy = resolveEconomy();
            if (economy == null && !warnedMissing) {
                warnedMissing = true;
                Aurora.logger().warning("Vault is installed, but there is no economy plugin registered to it.");
            }
        }
        return economy;
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        var economy = economy();
        if (economy == null) return;
        economy.withdrawPlayer(player, amount);
    }

    @Override
    public boolean tryWithdraw(Player player, String currency, double amount) {
        var economy = economy();
        if (economy == null || !economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        var economy = economy();
        if (economy == null) return;
        economy.depositPlayer(player, amount);
    }

    @Override
    public double getBalance(Player player, String currency) {
        var economy = economy();
        return economy == null ? 0 : economy.getBalance(player);
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        var economy = economy();
        return economy != null && economy.getBalance(player) >= amount;
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
