package gg.auroramc.aurora.expansions.item.resolvers;

import com.willfp.eco.core.items.Items;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EcoItemsResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return Items.isCustomItem(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        var ecoItem = Items.getCustomItem(item).getKey();
        return new TypeId("eco", ecoItem.toString());
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return Items.lookup(id).getItem();
    }
}
