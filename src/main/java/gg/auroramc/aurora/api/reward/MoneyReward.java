package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ThreadSafety;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class MoneyReward extends NumberReward {
    private String economy;
    private String currency = null;
    private boolean valid = true;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        var econ = getEconomy();
        if (!valid || econ == null) {
            Aurora.logger().warning("Money reward isn't configured properly, can't give money to " + player.getName() + "!");
            return;
        }

        econ.deposit(player, currency == null ? "default" : currency, getValue(placeholders));
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        this.economy = args.getString("economy");
        if (this.economy == null) {
            var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
            if (expansion != null) {
                this.economy = expansion.getDefaultEconomyId();
            } else {
                valid = false;
                Aurora.logger().warning("Can't create money reward, no economy provider available.");
                return;
            }
        }
        this.currency = args.getString("currency", null);
        var econ = getEconomy();

        if (AuroraAPI.getEconomy(economy) == null && econ != null) {
            Aurora.logger().warning("Economy provider " + economy + " is not available, please check your configuration. We will use the default economy provider.");
        }

        if (econ == null) {
            valid = false;
            Aurora.logger().warning("Can't create money reward, no economy provider available.");
            return;
        }

        if (currency != null && !isCurrencySupported(econ, currency)) {
            valid = false;
            Aurora.logger().warning("Currency " + currency + " is not supported by economy provider " + economy + ". Please check your configuration.");
        }
    }

    private AuroraEconomy getEconomy() {
        var econ = AuroraAPI.getEconomy(economy);
        if (econ == null) econ = AuroraAPI.getDefaultEconomy();
        return econ;
    }

    private boolean isCurrencySupported(AuroraEconomy economy, String currency) {
        return economy.supportsCurrency() && economy.validateCurrency(currency);
    }

    @Override
    public ThreadSafety getThreadSafety() {
        var econ = getEconomy();
        return econ == null ? ThreadSafety.SYNC_ONLY : econ.getThreadSafety();
    }
}
