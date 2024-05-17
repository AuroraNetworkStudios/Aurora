package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.Aurora;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;

public class MenuDupeFixer implements Listener {
    private final Aurora plugin;

    @Getter
    private final MenuItemMarker marker;

    public MenuDupeFixer(Aurora plugin) {
        this.plugin = plugin;
        this.marker = new MenuItemMarker(plugin, "aurora");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPickup(EntityPickupItemEvent event) {
        if (!this.marker.isMarked(event.getItem().getItemStack()))
            return;

        plugin.getLogger().warning(event.getEntity().getName() + " picked up a DeluxeMenus item. Removing it.");
        event.getItem().remove();
    }

    @EventHandler
    private void onDrop(PlayerDropItemEvent event) {
        if (!this.marker.isMarked(event.getItemDrop().getItemStack()))
            return;
        Aurora.logger().warning("An AuroraCore menu item was dropped in the world by "+ event.getPlayer().getName() +". Removing it.");
        event.getItemDrop().remove();
    }

    @EventHandler
    private void onLogin(PlayerLoginEvent event) {
        event.getPlayer().getScheduler().runDelayed(this.plugin, (task) -> {
            for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
                if (itemStack != null && this.marker.isMarked(itemStack)) {
                    plugin.getLogger().warning(event.getPlayer().getName() + " logged in with a AuroraMenu item in their inventory. Removing it.");
                    event.getPlayer().getInventory().remove(itemStack);
                }
            }
        }, null, 10L);
    }
}
