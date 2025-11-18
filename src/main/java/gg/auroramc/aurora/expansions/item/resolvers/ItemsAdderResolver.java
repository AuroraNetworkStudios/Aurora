package gg.auroramc.aurora.expansions.item.resolvers;

import dev.lone.itemsadder.api.CustomStack;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return CustomStack.byItemStack(item) != null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        var iaItem = CustomStack.byItemStack(item);
        return new TypeId("ia", iaItem.getNamespacedID());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return CustomStack.getInstance(id).getItemStack();
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.ITEMS_ADDER.getId());
    }
}
