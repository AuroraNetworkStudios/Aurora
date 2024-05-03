package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;

public class MenuDupeFixer implements Listener {
    private final AuroraLib plugin;

    @Getter
    private final MenuItemMarker marker;

    public MenuDupeFixer(AuroraLib plugin) {
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
        plugin.getLogger().warning("An AuroraCore menu item was dropped in the world by "+ event.getPlayer().getName() +". Removing it.");
        event.getItemDrop().remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof AuroraMenu) return;
        if(event.getCurrentItem() == null) return;
        if (!this.marker.isMarked(event.getCurrentItem()))
            return;


        plugin.getLogger().warning("An AuroraCore menu item was clicked by "+ event.getWhoClicked().getName() +" in a non menu inventory. Removing it.");

        plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (ItemStack itemStack : event.getWhoClicked().getInventory().getContents()) {
                if (itemStack != null && this.marker.isMarked(itemStack)) {
                    event.getWhoClicked().getInventory().remove(itemStack);
                }
            }
        }, 10L);

        plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            event.getWhoClicked().setItemOnCursor(null);
        }, 10L);

        event.setCurrentItem(new ItemStack(Material.AIR));
        event.setCancelled(true);
    }

    @EventHandler
    private void onLogin(PlayerLoginEvent event) {
        plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
                if (itemStack != null && this.marker.isMarked(itemStack)) {
                    plugin.getLogger().warning(event.getPlayer().getName() + " logged in with a AuroraMenu item in their inventory. Removing it.");
                    event.getPlayer().getInventory().remove(itemStack);
                }
            }
        }, 10L);
    }
}
