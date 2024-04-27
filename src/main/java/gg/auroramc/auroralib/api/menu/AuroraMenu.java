package gg.auroramc.auroralib.api.menu;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.message.Placeholder;
import gg.auroramc.auroralib.api.message.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class AuroraMenu implements InventoryHolder {
    private final Inventory inventory;
    private ItemStack filler;
    private final Map<Integer, MenuEntry> menuItems = new HashMap<>();

    public AuroraMenu(Player player, String title, int size, boolean refreshEnabled,  Placeholder... placeholders) {
        this.inventory = Bukkit.createInventory(this, size, Text.component(player, title, placeholders));
        var filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        var fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        this.filler = filler;
        if(refreshEnabled) {
            AuroraLib.getMenuManager().getRefresher().add(this);
        }
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

    public AuroraMenu addFiller(ItemStack filler) {
        this.filler = filler;
        return this;
    }

    public void handleEvent(InventoryClickEvent e) {
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

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
        }
    }

    public void open(Player player) {
        AuroraLib.getMenuManager().getDupeFixer().getMarker().mark(filler);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
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
