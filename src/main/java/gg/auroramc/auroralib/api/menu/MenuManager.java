package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

@Getter
public class MenuManager implements Listener {
    private final MenuDupeFixer dupeFixer;
    private final MenuRefresher refresher;

    public MenuManager(AuroraLib plugin) {
        dupeFixer = new MenuDupeFixer(plugin);
        refresher = new MenuRefresher(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof AuroraMenu menu) {
            event.setCancelled(true);
            menu.handleEvent(event);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof AuroraMenu menu) {
            menu.handleEvent(event);
            refresher.remove(menu);
        }
    }
}
