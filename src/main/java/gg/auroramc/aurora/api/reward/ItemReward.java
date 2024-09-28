package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.ConfigManager;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ItemReward extends AbstractReward {
    enum StashHandle {
        NONE,
        OVERFLOW,
        FORCE
    }

    private ItemConfig itemConfig;
    private StashHandle stash = StashHandle.NONE;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (itemConfig == null) {
            Aurora.logger().warning("Item reward doesn't have a valid item configuration!");
            return;
        }

        if (stash == StashHandle.FORCE) {
            var item = ItemBuilder.of(itemConfig).placeholder(placeholders).toItemStack(player);
            var stashHolder = Aurora.getUserManager().getUser(player.getUniqueId()).getStashData();
            stashHolder.addItem(item);
            return;
        }

        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var item = ItemBuilder.of(itemConfig).placeholder(placeholders).toItemStack(player);

            if (stash == StashHandle.NONE) {
                var failed = player.getInventory().addItem(item);
                if (failed.isEmpty()) return;
                Bukkit.getRegionScheduler().run(Aurora.getInstance(), player.getLocation(), (t) -> {
                    failed.forEach((slot, fitem) -> player.getWorld().dropItem(player.getLocation(), fitem));
                });
            } else if (stash == StashHandle.OVERFLOW) {
                var failed = player.getInventory().addItem(item);
                if (failed.isEmpty()) return;
                CompletableFuture.runAsync(() -> {
                    var stashHolder = Aurora.getUserManager().getUser(player.getUniqueId()).getStashData();
                    failed.forEach((slot, fitem) -> stashHolder.addItem(fitem));
                });
            }

        }, null);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        var config = args.getConfigurationSection("item");
        if (config == null) {
            Aurora.logger().warning("Item reward doesn't have a valid item configuration under the key 'item'!");
            return;
        }
        itemConfig = new ItemConfig();
        ConfigManager.load(itemConfig, config);
        if (args.contains("stash")) {
            stash = StashHandle.valueOf(args.getString("stash", "none").toUpperCase(Locale.ROOT));
        }
    }

    @Override
    public ThreadSafety getThreadSafety() {
        // Any since it will be scheduled back to the player's thread anyway
        return ThreadSafety.ANY;
    }
}
