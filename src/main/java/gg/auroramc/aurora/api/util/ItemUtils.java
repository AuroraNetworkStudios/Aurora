package gg.auroramc.aurora.api.util;

import gg.auroramc.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemUtils {
    public static ItemStack[] createStacksFromAmount(ItemStack item, int amount) {
        if (amount <= 0) {
            return new ItemStack[0];
        }

        int maxStackSize = item.getMaxStackSize();
        int stackCount = (amount + maxStackSize - 1) / maxStackSize;

        ItemStack[] stacks = new ItemStack[stackCount];

        for (int i = 0; i < stackCount; i++) {
            var stack = item.clone();
            stack.setAmount(Math.min(item.getMaxStackSize(), amount));
            amount -= item.getMaxStackSize();
            stacks[i] = stack;
        }

        return stacks;
    }

    /**
     * Adds the items to the player's stash, also while their data is still loading. The items that can't be added
     * (a plugin cancelled it, or their data couldn't be loaded or saved) are dropped at the player instead of lost.
     */
    public static void addToStash(Player player, Collection<ItemStack> items) {
        var rejected = new ArrayList<ItemStack>();
        Aurora.getUserManager().editUser(player.getUniqueId(), user -> {
            var stash = user.getStashData();
            for (var item : items) {
                // A copy, adding may change the amount of the stack it's given
                if (!stash.addItem(item.clone())) rejected.add(item);
            }
            // The ones that were added are saved, the rest are dropped below
            return rejected.size() < items.size();
        }).thenAccept(added -> {
            var toDrop = added ? rejected : List.copyOf(items);
            if (toDrop.isEmpty()) return;
            Runnable dropWhereTheyLeft = () -> {
                try {
                    var location = player.getLocation();
                    Bukkit.getRegionScheduler().run(Aurora.getInstance(), location,
                            (task) -> toDrop.forEach(item -> location.getWorld().dropItem(location, item)));
                } catch (Exception e) {
                    Aurora.logger().severe("Couldn't drop the stash items of " + player.getName() + " that couldn't be added: " + toDrop + " error: " + e);
                }
            };
            var scheduled = player.getScheduler().run(Aurora.getInstance(),
                    (task) -> toDrop.forEach(item -> player.getWorld().dropItem(player.getLocation(), item)), dropWhereTheyLeft);
            // Null if they had left already, then neither runs
            if (scheduled == null) dropWhereTheyLeft.run();
        });
    }
}
