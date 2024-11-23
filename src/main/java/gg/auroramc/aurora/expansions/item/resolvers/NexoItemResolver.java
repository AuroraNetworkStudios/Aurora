package gg.auroramc.aurora.expansions.item.resolvers;

import com.nexomc.nexo.api.NexoItems;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NexoItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return NexoItems.exists(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("nexo", NexoItems.idFromItem(item));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return NexoItems.itemFromId(id).build();
    }
}
