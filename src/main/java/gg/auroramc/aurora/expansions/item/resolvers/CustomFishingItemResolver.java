package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import net.momirealms.customfishing.api.CustomFishingPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomFishingItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return CustomFishingPlugin.get().getItemManager().isCustomFishingItem(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("customfishing", CustomFishingPlugin.get().getItemManager().getCustomFishingItemID(item));
    }

    @Override
    public ItemStack resolveItem(String id, Player player) {
        return CustomFishingPlugin.get().getItemManager().build(player, "item", id);
    }
}
