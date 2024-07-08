package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class MoneyReward extends NumberReward {
    private String economy;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        var econ = getEconomy();
        if(econ == null) {
            Aurora.logger().warning("There isn't any economy provider available. Can't give money reward to " + player.getName() + "!");
            return;
        }

        econ.deposit(player, getValue(placeholders));
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        economy = args.getString("economy", "Vault");

        if(AuroraAPI.getEconomy(economy) == null && getEconomy() != null) {
            Aurora.logger().warning("Economy provider " + economy + " is not available, please check your configuration. We will use the default economy provider.");
        }

        if(getEconomy() == null) {
            Aurora.logger().warning("Can't create money reward, no economy provider available.");
        }
    }

    private AuroraEconomy getEconomy() {
        var econ = AuroraAPI.getEconomy(economy);
        if(econ == null) econ = AuroraAPI.getDefaultEconomy();
        return econ;
    }
}
