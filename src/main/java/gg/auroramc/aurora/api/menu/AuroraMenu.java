package gg.auroramc.aurora.api.menu;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.util.NamespacedId;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
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
    private final Map<Integer, List<MenuEntry>> menuItems = new HashMap<>();
    @Getter
    private Set<Integer> freeSlots;
    private Consumer<Inventory> freeSlotUpdateHandler;
    private Set<Integer> managedSlots;
    private BiConsumer<InventoryClickEvent, Integer> managedSlotClickHandler;

    private List<ItemStack> freeItems;
    private BiConsumer<AuroraMenu, InventoryCloseEvent> closeHandler;
    @Getter
    private final Player player;
    @Getter
    @Setter
    private NamespacedId id;

    public AuroraMenu(Player player, String title, int size, boolean refreshEnabled, Placeholder<?>... placeholders) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, Text.component(player, title, placeholders));
        this.filler = ItemBuilder.filler();
        if (refreshEnabled) {
            Aurora.getMenuManager().getRefresher().add(this);
        }
    }

    public AuroraMenu(Player player, String title, int size, boolean refreshEnabled, NamespacedId id, Placeholder<?>... placeholders) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, Text.component(player, title, placeholders));
        this.filler = ItemBuilder.filler();
        this.id = id;
        if (refreshEnabled) {
            Aurora.getMenuManager().getRefresher().add(this);
        }
    }

    public AuroraMenu onClose(BiConsumer<AuroraMenu, InventoryCloseEvent> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public AuroraMenu onBeforeFreeSlotsUpdate(Consumer<Inventory> freeSlotUpdateHandler) {
        this.freeSlotUpdateHandler = freeSlotUpdateHandler;
        return this;
    }

    public AuroraMenu onManagedSlotClick(BiConsumer<InventoryClickEvent, Integer> managedSlotClickHandler) {
        this.managedSlotClickHandler = managedSlotClickHandler;
        return this;
    }

    public void addItem(MenuItem item, Function<InventoryClickEvent, MenuAction> handler) {
        var menuEntry = new MenuEntry(item, handler);
        for (var slot : item.getSlots()) {
            if (!menuItems.containsKey(slot)) {
                menuItems.put(slot, new ArrayList<>());
            }
            menuItems.get(slot).add(menuEntry);
            menuItems.get(slot).sort((entry1, entry2) -> Integer.compare(entry2.getPriority(), entry1.getPriority()));
        }
    }

    public void addItem(MenuItem item, Consumer<InventoryClickEvent> handler) {
        var menuEntry = new MenuEntry(item, handler);
        for (var slot : item.getSlots()) {
            if (!menuItems.containsKey(slot)) {
                menuItems.put(slot, new ArrayList<>());
            }
            menuItems.get(slot).add(menuEntry);
            menuItems.get(slot).sort((entry1, entry2) -> Integer.compare(entry2.getPriority(), entry1.getPriority()));
        }
    }

    public void addItem(MenuItem item) {
        var menuEntry = new MenuEntry(item);
        for (var slot : item.getSlots()) {
            if (!menuItems.containsKey(slot)) {
                menuItems.put(slot, new ArrayList<>());
            }
            menuItems.get(slot).add(menuEntry);
            menuItems.get(slot).sort((entry1, entry2) -> Integer.compare(entry2.getPriority(), entry1.getPriority()));
        }
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

    public AuroraMenu managedSlots(List<Integer> slots) {
        if (managedSlots == null) {
            managedSlots = new HashSet<>(slots.size());
        }
        managedSlots.addAll(slots);
        return this;
    }

    public AuroraMenu managedSlots(int start, int end) {
        if (managedSlots == null) {
            managedSlots = new HashSet<>(end - start);
        }
        for (int i = start; i < end; i++) {
            managedSlots.add(i);
        }
        return this;
    }

    public AuroraMenu managedSlots(int end) {
        return managedSlots(0, end);
    }

    public AuroraMenu setFreeSlotsContent(List<ItemStack> items) {
        if (freeItems == null) {
            freeItems = new ArrayList<>(items.size());
        }
        freeItems.addAll(items);
        return this;
    }

    private void recalcSlotItem(List<MenuEntry> menuEntries, int slot) {
        boolean found = false;
        for (var menuEntry : menuEntries) {
            if (found) {
                menuEntry.setActive(false);
                continue;
            }
            var builder = menuEntry.getItem().getItemBuilder();
            if (Requirement.isAllMet(player, builder.getConfig().getViewRequirements(), builder.getPlaceholders())) {
                found = true;
                menuEntry.setActive(true);
                Aurora.getMenuManager().getDupeFixer().getMarker().mark(menuEntry.getItem().getItemStack());
                menuEntry.getItem().refresh();
                inventory.setItem(slot, menuEntry.getItem().getItemStack());
            } else {
                menuEntry.setActive(false);
            }
        }
    }

    public boolean hasFreeSlots() {
        return freeSlots != null;
    }

    public boolean isFreeSlot(int slot) {
        return freeSlots != null && freeSlots.contains(slot);
    }

    public boolean handleEvent(InventoryClickEvent e) {
        if (freeSlots != null && freeSlots.contains(e.getSlot())) {
            e.setCancelled(false);
            handleFreeSlotUpdate(e.getInventory());
            return false;
        } else {
            e.setCancelled(true);
        }

        if (!(e.getWhoClicked() instanceof Player player)) return false;

        if (managedSlots != null && managedSlots.contains(e.getSlot())) {
            if (managedSlotClickHandler != null) {
                managedSlotClickHandler.accept(e, e.getSlot());
            }
            return true;
        }

        if (menuItems.containsKey(e.getSlot())) {
            var menuEntries = menuItems.get(e.getSlot());
            for (var menuEntry : menuEntries) {
                if (menuEntry.isActive()) {
                    var result = menuEntry.handleEvent(e);

                    if (result == MenuAction.REFRESH_SLOT) {
                        recalcSlotItem(menuEntries, e.getSlot());
                        player.updateInventory();
                    } else if (result == MenuAction.CLOSE) {
                        player.closeInventory();
                    } else if (result == MenuAction.REFRESH_MENU) {
                        refresh();
                    } else if (result == MenuAction.REFRESH_MENU_DELAYED) {
                        refreshDelayed(2);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public void handleEvent(InventoryCloseEvent e) {
        Aurora.getMenuManager().getRefresher().remove(this);
        if (closeHandler != null) {
            this.closeHandler.accept(this, e);
        }
    }

    public void open() {
        open(player);
    }

    public void open(Player player) {
        open(player, true, null);
    }

    public void open(Player player, boolean useScheduler, Consumer<AuroraMenu> onOpen) {
        if (player.isSleeping()) return;
        if (!player.isOnline()) return;

        populateInventory(player, false);

        if (useScheduler) {
            player.getScheduler().run(Aurora.getInstance(), (task) -> {
                player.openInventory(inventory);
                if (onOpen != null) {
                    onOpen.accept(this);
                }
            }, null);
        } else {
            player.openInventory(inventory);
            if (onOpen != null) {
                onOpen.accept(this);
            }
        }
    }

    private void populateInventory(Player player, boolean isRefresh) {
        inventory.clear();
        Aurora.getMenuManager().getDupeFixer().getMarker().mark(filler);

        int j = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (freeSlots != null && freeSlots.contains(i)) {
                if (freeItems != null && j < freeItems.size()) {
                    inventory.setItem(i, freeItems.get(j));
                    j++;
                }
            } else if (managedSlots == null || !managedSlots.contains(i)) {
                inventory.setItem(i, filler);
            }
        }

        for (var menuEntries : menuItems.entrySet()) {
            boolean found = false;
            for (var menuEntry : menuEntries.getValue()) {
                if (found) {
                    menuEntry.setActive(false);
                    continue;
                }
                var builder = menuEntry.getItem().getItemBuilder();
                if (Requirement.isAllMet(player, builder.getConfig().getViewRequirements(), builder.getPlaceholders())) {
                    found = true;
                    menuEntry.setActive(true);
                    Aurora.getMenuManager().getDupeFixer().getMarker().mark(menuEntry.getItem().getItemStack());
                    if (isRefresh && menuEntry.getItem().isRefreshEnabled()) {
                        menuEntry.getItem().refresh();
                    }
                    inventory.setItem(menuEntries.getKey(), menuEntry.getItem().getItemStack());
                } else {
                    menuEntry.setActive(false);
                }
            }
        }
    }


    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void refresh() {
        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            populateInventory(player, true);
            player.updateInventory();
        }, null);
    }

    public void refreshDelayed(int delayTicks) {
        player.getScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            populateInventory(player, true);
            player.updateInventory();
        }, null, delayTicks);
    }

    public void handleFreeSlotUpdate(Inventory inventory) {
        if (freeSlotUpdateHandler != null) {
            freeSlotUpdateHandler.accept(inventory);
        }
    }
}
