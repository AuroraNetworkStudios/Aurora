package gg.auroramc.aurora.api.menu;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.aurora.Aurora;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class MenuManager implements Listener {
    private final MenuDupeFixer dupeFixer;
    private final MenuRefresher refresher;

    private final Cache<UUID, Long> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(75L, TimeUnit.MILLISECONDS).build();

    private final Cache<UUID, Long> shiftCache = CacheBuilder.newBuilder()
            .expireAfterWrite(200L, TimeUnit.MILLISECONDS).build();

    public MenuManager(Aurora plugin) {
        dupeFixer = new MenuDupeFixer(plugin);
        refresher = new MenuRefresher(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AuroraMenu menu) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null) {
                if (!(event.getClickedInventory().getHolder() instanceof AuroraMenu)) {
                    if (menu.hasFreeSlots()) {
                        event.setCancelled(false);
                        if (event.isShiftClick()) {
                            menu.handleFreeSlotUpdate(event.getInventory());
                        }
                    }
                    return;
                }
            }

            if (cache.getIfPresent(event.getWhoClicked().getUniqueId()) != null) {
                return;
            }

            if (shiftCache.getIfPresent(event.getWhoClicked().getUniqueId()) != null) {
                return;
            }

            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                return;
            }

            if (event.getClick() == ClickType.SHIFT_LEFT) {
                shiftCache.put(event.getWhoClicked().getUniqueId(), System.currentTimeMillis());
            }

            var valid = menu.handleEvent(event);

            if (valid) {
                cache.put(event.getWhoClicked().getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AuroraMenu menu) {
            if (menu.hasFreeSlots()) {
                for (var slot : event.getInventorySlots()) {
                    if (menu.isFreeSlot(slot)) {
                        menu.handleFreeSlotUpdate(event.getInventory());
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMenuClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AuroraMenu menu) {
            menu.handleEvent(event);
            event.getPlayer().getScheduler().runDelayed(Aurora.getInstance(), (task) -> {
                cleanInventory((Player) event.getPlayer(), dupeFixer.getMarker());
            }, null, 3L);
        }
    }

    public void cleanInventory(Player player, MenuItemMarker marker) {
        if (player == null) {
            return;
        }
        for (var itemStack : player.getInventory().getContents()) {
            if (itemStack != null &&
                    marker.isMarked(itemStack)) {
                Aurora.getInstance().getLogger().warning("Found a AuroraMenu item in a player's inventory. Removing it.");
                player.getInventory().remove(itemStack);
            }
        }
        player.updateInventory();
    }
}
