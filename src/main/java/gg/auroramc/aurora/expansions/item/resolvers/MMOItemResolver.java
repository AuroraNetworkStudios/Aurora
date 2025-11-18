package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MMOItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return NBTItem.get(item).hasType();
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        var nbtItem = NBTItem.get(item);
        return new TypeId("mmoitems", nbtItem.getType() + ":" + nbtItem.getString("MMOITEMS_ITEM_ID"));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        String[] split = id.split(":");
        return MMOItems.plugin.getItem(split[0], split[1]);
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.MMOITEMS.getId());
    }

}
