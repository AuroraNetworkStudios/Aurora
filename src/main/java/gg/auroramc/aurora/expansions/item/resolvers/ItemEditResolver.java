package gg.auroramc.aurora.expansions.item.resolvers;

import emanondev.itemedit.ItemEdit;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemEditResolver implements ItemResolver {
    private static final String prefix = "itemedit";

    @Override
    public boolean matches(ItemStack item) {
        return ItemEdit.get().getServerStorage().contains(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId(prefix, ItemEdit.get().getServerStorage().getId(item));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return ItemEdit.get().getServerStorage().getItem(id, player);
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        var id = ItemEdit.get().getServerStorage().getId(item);

        if (id != null) {
            return new TypeId(prefix, id);
        } else {
            return null;
        }
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.ITEM_EDIT.getId());
    }
}
