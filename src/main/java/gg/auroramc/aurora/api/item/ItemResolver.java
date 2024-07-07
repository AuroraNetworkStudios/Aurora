package gg.auroramc.aurora.api.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemResolver {
    boolean matches(ItemStack item);

    TypeId resolveId(ItemStack item);

    ItemStack resolveItem(String id, @Nullable Player player);

    default ItemStack resolveItem(String id) {
        return resolveItem(id, null);
    }
}
