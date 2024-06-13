package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class MoneyReward extends NumberReward {
    private String economy;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        var econ = AuroraAPI.getEconomy(economy);
        if(econ == null) econ = AuroraAPI.getDefaultEconomy();

        econ.deposit(player, getValue(placeholders));
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        economy = args.getString("economy", "Vault");
    }
}
