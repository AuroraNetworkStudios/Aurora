package gg.auroramc.auroralib.expansions.economy.providers;

import com.Zrips.CMI.CMI;
import gg.auroramc.auroralib.expansions.economy.AuroraEconomy;
import org.bukkit.entity.Player;

public class CMIEconomy implements AuroraEconomy {
    @Override
    public void withdraw(Player player, double amount) {
        CMI.getInstance().getPlayerManager().getUser(player).withdraw(amount);
    }

    @Override
    public void deposit(Player player, double amount) {
        CMI.getInstance().getPlayerManager().getUser(player).deposit(amount);
    }

    @Override
    public double getBalance(Player player) {
        return CMI.getInstance().getPlayerManager().getUser(player).getBalance();
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        return CMI.getInstance().getPlayerManager().getUser(player).getBalance() >= amount;
    }
}
