package gg.auroramc.aurora.api.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemResolver {
    boolean matches(ItemStack item);

    default TypeId oneStepMatch(ItemStack item) {
        if (matches(item)) {
            return resolveId(item);
        }
        return null;
    }

    TypeId resolveId(ItemStack item);

    ItemStack resolveItem(String id, @Nullable Player player);

    default ItemStack resolveItem(String id) {
        return resolveItem(id, null);
    }
}
