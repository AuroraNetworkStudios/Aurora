package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CraftEngineItemResolver implements ItemResolver {
    @Override
    public boolean matches(ItemStack item) {
        var id = CraftEngineItems.getCustomItemId(item);
        return id != null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("craftengine", CraftEngineItems.getCustomItemId(item).asString());
    }

    @Override
    public ItemStack resolveItem(String id, Player player) {
        return CraftEngineItems.byId(Key.from(id)).buildItemStack();
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        var id = CraftEngineItems.getCustomItemId(item);
        if (id == null) return null;
        return new TypeId("craftengine", id.asString());
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.CRAFT_ENGINE.getId());
    }
}
