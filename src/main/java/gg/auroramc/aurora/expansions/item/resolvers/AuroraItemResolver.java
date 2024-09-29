package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.expansions.item.store.ItemStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AuroraItemResolver implements ItemResolver {
    private final ItemStore itemStore;

    public AuroraItemResolver(ItemStore itemStore) {
        this.itemStore = itemStore;
    }

    @Override
    public boolean matches(ItemStack item) {
        return false;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return null;
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return itemStore.getItem(id);
    }
}
