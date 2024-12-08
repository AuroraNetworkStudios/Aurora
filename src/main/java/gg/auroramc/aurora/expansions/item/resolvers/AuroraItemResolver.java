package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.expansions.item.store.ItemStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AuroraItemResolver implements ItemResolver {
    private final ItemStore itemStore;
    private final boolean enableIdResolver;

    public AuroraItemResolver(ItemStore itemStore, boolean enableIdResolver) {
        this.itemStore = itemStore;
        this.enableIdResolver = enableIdResolver;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (!enableIdResolver) return false;
        return itemStore.getIdFromItem(item) != null;
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        if (!enableIdResolver) return null;
        var id = itemStore.getIdFromItem(item);
        if (id != null) {
            return new TypeId("aurora", id);
        }
        return null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return oneStepMatch(item);
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return itemStore.getItem(id);
    }
}
