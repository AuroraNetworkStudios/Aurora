package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AbstractReward implements Reward {
    protected String display;

    @Override
    public void init(ConfigurationSection args) {
        display = args.getString("display", "");
    }

    @Override
    public String getDisplay(Player player, List<Placeholder<?>> placeholders) {
        return display;
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.SYNC_ONLY;
    }
}
