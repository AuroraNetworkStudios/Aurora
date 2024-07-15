package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomFishingItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item) != null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("customfishing", BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item));
    }

    @Override
    public ItemStack resolveItem(String id, Player player) {
        return BukkitCustomFishingPlugin.getInstance().getItemManager().buildAny(Context.player(player), "CustomFishing:" + id);
    }
}
