package gg.auroramc.aurora.api.util;

import org.bukkit.inventory.ItemStack;

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
}
