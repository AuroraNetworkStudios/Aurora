package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.message.Placeholder;
import gg.auroramc.auroralib.api.message.Text;
import gg.auroramc.auroralib.api.util.Platform;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class AuroraMenu implements InventoryHolder {
    private final Inventory inventory;
    private ItemStack filler;
    private final Map<Integer, MenuEntry> menuItems = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> freeSlotHandlers = new HashMap<>();
    private Set<Integer> freeSlots;
    private List<ItemStack> freeItems;
    private BiConsumer<AuroraMenu, InventoryCloseEvent> closeHandler;

    public AuroraMenu(Player player, String title, int size, boolean refreshEnabled, Placeholder<?>... placeholders) {
        this.inventory = Platform.createChestInventory(this, size, Text.component(player, title, placeholders));
        this.filler = ItemBuilder.filler();
        if (refreshEnabled) {
            AuroraLib.getMenuManager().getRefresher().add(this);
        }
    }

    public AuroraMenu onClose(BiConsumer<AuroraMenu, InventoryCloseEvent> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public void addItem(MenuItem item, Function<InventoryClickEvent, MenuAction> handler) {
        this.menuItems.put(item.getSlot(), new MenuEntry(item, handler));
    }

    public void addItem(MenuItem item, Consumer<InventoryClickEvent> handler) {
        this.menuItems.put(item.getSlot(), new MenuEntry(item, handler));
    }

    public void addItem(MenuItem item) {
        this.menuItems.put(item.getSlot(), new MenuEntry(item));
    }

    public void freeSlotHandler(int slot, Consumer<InventoryClickEvent> handler) {
        this.freeSlotHandlers.put(slot, handler);
    }

    public AuroraMenu addFiller(ItemStack filler) {
        this.filler = filler;
        return this;
    }

    public AuroraMenu freeSlots(List<Integer> slots) {
        if (freeSlots == null) {
            freeSlots = new HashSet<>(slots.size());
        }
        freeSlots.addAll(slots);
        return this;
    }

    public AuroraMenu freeSlots(int start, int end) {
        if (freeSlots == null) {
            freeSlots = new HashSet<>(end - start);
        }
        for (int i = start; i < end; i++) {
            freeSlots.add(i);
        }
        return this;
    }

    public AuroraMenu freeSlots(int end) {
        return freeSlots(0, end);
    }

    public AuroraMenu setFreeSlotsContent(List<ItemStack> items) {
        if (freeItems == null) {
            freeItems = new ArrayList<>(items.size());
        }
        freeItems.addAll(items);
        return this;
    }

    public boolean handleEvent(InventoryClickEvent e) {
        if (freeSlots != null && freeSlots.contains(e.getSlot())) {
            e.setCancelled(false);
            if (freeSlotHandlers.containsKey(e.getSlot())) {
                freeSlotHandlers.get(e.getSlot()).accept(e);
            }
            return false;
        } else {
            e.setCancelled(true);
        }

        if (!(e.getWhoClicked() instanceof Player player)) return false;

        if (menuItems.containsKey(e.getSlot())) {
            var menuEntry = menuItems.get(e.getSlot());
            var result = menuEntry.handleEvent(e);

            if (result == MenuAction.REFRESH_SLOT) {
                menuEntry.getItem().refresh();
                player.updateInventory();
            } else if (result == MenuAction.CLOSE) {
                player.closeInventory();
            } else if (result == MenuAction.REFRESH_MENU) {
                for (var menuItem : menuItems.values()) {
                    menuItem.getItem().refresh();
                }
                player.updateInventory();
            }
            return true;
        }

        return false;
    }

    public void handleEvent(InventoryCloseEvent e) {
        AuroraLib.getMenuManager().getRefresher().remove(this);
        if (closeHandler != null) {
            this.closeHandler.accept(this, e);
        }
    }

    public void open(Player player) {
        if(player.isSleeping()) return;
        AuroraLib.getMenuManager().getDupeFixer().getMarker().mark(filler);

        int j = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (freeSlots != null && freeSlots.contains(i)) {
                if (freeItems != null && j < freeItems.size()) {
                    inventory.setItem(i, freeItems.get(j));
                    j++;
                }
            } else {
                inventory.setItem(i, filler);
            }
        }

        for (var menuEntry : menuItems.values()) {
            AuroraLib.getMenuManager().getDupeFixer().getMarker().mark(menuEntry.getItem().getItemStack());
            menuEntry.getItem().applyToInventory(inventory);
        }

        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void refresh() {
        for (var menuEntry : menuItems.values()) {
            if (menuEntry.getItem().isRefreshEnabled()) {
                menuEntry.getItem().refresh();
            }
        }
    }
}
