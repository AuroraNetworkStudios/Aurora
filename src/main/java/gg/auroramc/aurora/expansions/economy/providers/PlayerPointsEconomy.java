package gg.auroramc.aurora.expansions.economy.providers;

import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class PlayerPointsEconomy implements AuroraEconomy {
    private final PlayerPointsAPI playerPointsAPI;

    public PlayerPointsEconomy() {
        this.playerPointsAPI = PlayerPoints.getInstance().getAPI();
    }

    @Override
    public void withdraw(Player player, String currency, double amount) {
        playerPointsAPI.take(player.getUniqueId(), ((Double) amount).intValue());
    }

    @Override
    public void deposit(Player player, String currency, double amount) {
        playerPointsAPI.give(player.getUniqueId(), ((Double) amount).intValue());
    }

    @Override
    public double getBalance(Player player, String currency) {
        return playerPointsAPI.look(player.getUniqueId());
    }

    @Override
    public boolean hasBalance(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
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
        return ThreadSafety.SYNC_ONLY;
    }
}
