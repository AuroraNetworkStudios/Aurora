package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MythicItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        return MythicBukkit.inst().getItemManager().isMythicItem(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("mythicmobs", MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return MythicBukkit.inst().getItemManager().getItemStack(id);
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.MYTHICMOBS.getId());
    }
}
