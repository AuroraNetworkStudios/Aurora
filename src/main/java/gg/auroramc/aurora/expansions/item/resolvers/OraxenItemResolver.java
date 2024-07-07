package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OraxenItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return OraxenItems.getIdByItem(item) != null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("oraxen", OraxenItems.getIdByItem(item));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return OraxenItems.getItemById(id).build();
    }
}
