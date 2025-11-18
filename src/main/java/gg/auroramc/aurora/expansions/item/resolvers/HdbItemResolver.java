package gg.auroramc.aurora.expansions.item.resolvers;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class HdbItemResolver implements ItemResolver, Listener {
    private HeadDatabaseAPI api;

    @Override
    public boolean matches(ItemStack item) {
        return api != null && api.getItemID(item) != null;
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return new TypeId("hdb", api.getItemID(item));
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return api == null ? null : api.getItemHead(id);
    }


    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        this.api = new HeadDatabaseAPI();
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(Dep.HEAD_DATABASE.getId());
    }
}
