package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.ConfigManager;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class ItemReward extends AbstractReward {
    private ItemConfig itemConfig;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if(itemConfig == null) {
            Aurora.logger().warning("Item reward doesn't have a valid item configuration!");
            return;
        }
        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var failed = player.getInventory().addItem(ItemBuilder.of(itemConfig).placeholder(placeholders).toItemStack(player));
            if(failed.isEmpty()) return;
            failed.forEach((slot, item) -> player.getWorld().dropItem(player.getLocation(), item));
        }, null);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        var config = args.getConfigurationSection("item");
        if(config == null) {
            Aurora.logger().warning("Item reward doesn't have a valid item configuration under the key 'item'!");
            return;
        }
        itemConfig = new ItemConfig();
        ConfigManager.load(itemConfig, config);
    }
}
