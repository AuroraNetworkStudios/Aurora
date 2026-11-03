package gg.auroramc.aurora.internal.menu;

import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.MenuItemMarker;
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
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public class MenuManager implements Listener {
    private final MenuDupeFixer dupeFixer;
    private final MenuRefresher refresher;

    private final Cache<UUID, Long> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(75L, TimeUnit.MILLISECONDS).build();

    private final Cache<UUID, Long> shiftCache = CacheBuilder.newBuilder()
            .expireAfterWrite(200L, TimeUnit.MILLISECONDS).build();

    // Players whose last click still has actions waiting to run. Console and player commands run on the next tick,
    // so another click before that could pass the same requirements again (e.g. [money] before the money is taken)
    private final Set<UUID> clicksInFlight = ConcurrentHashMap.newKeySet();

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
                    if (menu.hasFreeSlots() && MenuClickPolicy.allowsBottomClick(menu, event)) {
                        event.setCancelled(false);
                        if (event.isShiftClick()) {
                            menu.handleFreeSlotUpdate(event.getInventory());
                        }
                    }
                    return;
                }
            }

            var uuid = event.getWhoClicked().getUniqueId();

            if (cache.getIfPresent(uuid) != null) {
                return;
            }

            if (shiftCache.getIfPresent(uuid) != null) {
                return;
            }

            if (clicksInFlight.contains(uuid)) {
                return;
            }

            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                return;
            }

            // Free slots handle every click type themselves, buttons only react to the plain mouse clicks
            if (!menu.isFreeSlot(event.getSlot()) && !MenuClickPolicy.isButtonClick(event.getClick())) {
                return;
            }

            if (event.getClick() == ClickType.SHIFT_LEFT) {
                shiftCache.put(uuid, System.currentTimeMillis());
            }

            var valid = menu.handleEvent(event);

            if (valid) {
                cache.put(uuid, System.currentTimeMillis());
                if (event.getWhoClicked() instanceof Player player) {
                    holdClicksUntilActionsRan(player);
                }
            }
        }
    }

    // Actions after a [delay] check the requirements again when they run, so only the ones that run right away are waited for
    private void holdClicksUntilActionsRan(Player player) {
        var uuid = player.getUniqueId();
        if (!clicksInFlight.add(uuid)) return;

        // The click's console commands are already queued on the global scheduler and its player commands on the
        // player's, so a task queued on the global scheduler from the next player task only runs after both
        var task = player.getScheduler().run(Aurora.getInstance(),
                (t) -> Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (g) -> clicksInFlight.remove(uuid)),
                () -> clicksInFlight.remove(uuid));
        // null means the player is already gone
        if (task == null) clicksInFlight.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AuroraMenu menu) {
            var topSize = event.getView().getTopInventory().getSize();
            for (var rawSlot : event.getRawSlots()) {
                // Dragging over the player's own inventory is fine, but items must never land in a menu slot
                if (rawSlot < topSize && !menu.isFreeSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (menu.hasFreeSlots()) {
                for (var rawSlot : event.getRawSlots()) {
                    if (rawSlot < topSize) {
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clicksInFlight.remove(event.getPlayer().getUniqueId());
    }

    public void cleanInventory(Player player, MenuItemMarker marker) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        // Every slot, so armor and offhand too, not just the storage contents
        for (int i = 0; i < inventory.getSize(); i++) {
            var itemStack = inventory.getItem(i);
            if (itemStack != null && marker.isMarked(itemStack)) {
                Aurora.getInstance().getLogger().warning("Found a AuroraMenu item in " + player.getName() + "'s inventory. Removing it.");
                inventory.setItem(i, null);
            }
        }
        var cursor = player.getItemOnCursor();
        if (!cursor.isEmpty() && marker.isMarked(cursor)) {
            Aurora.getInstance().getLogger().warning("Found a AuroraMenu item on " + player.getName() + "'s cursor. Removing it.");
            player.setItemOnCursor(null);
        }
        player.updateInventory();
    }
}
