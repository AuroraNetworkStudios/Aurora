package gg.auroramc.aurora.internal.menu;

import gg.auroramc.aurora.api.menu.AuroraMenu;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.IntPredicate;

/**
 * Decides which clicks can move items or press buttons in an {@link AuroraMenu}.
 * Everything not allowed here stays cancelled.
 */
public final class MenuClickPolicy {
    private MenuClickPolicy() {
    }

    /**
     * Buttons only react to the plain mouse clicks. Number keys, offhand swap, drop, middle click
     * and double click would otherwise run the button's actions too.
     */
    public static boolean isButtonClick(ClickType click) {
        return click == ClickType.LEFT || click == ClickType.RIGHT
                || click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;
    }

    /**
     * A click in a free slot of the menu moves items like in a normal inventory, except collecting to the cursor,
     * which would also pull items out of the other menu slots.
     */
    public static boolean allowsFreeSlotClick(InventoryAction action) {
        return action != InventoryAction.COLLECT_TO_CURSOR;
    }

    /**
     * A click in the player's own inventory while a menu with free slots is open.
     * Collecting to the cursor would pull items out of the menu, and shift clicking puts the items in the
     * first empty slot of the menu, so it's only allowed when that can only be a free slot.
     *
     * @param action    the action of the click
     * @param menuSize  the size of the menu
     * @param isFree    whether a menu slot is a free slot
     * @param isEmpty   whether a menu slot is empty
     */
    public static boolean allowsBottomClick(InventoryAction action, int menuSize, IntPredicate isFree, IntPredicate isEmpty) {
        if (action == InventoryAction.COLLECT_TO_CURSOR) return false;
        if (action != InventoryAction.MOVE_TO_OTHER_INVENTORY) return true;

        for (int slot = 0; slot < menuSize; slot++) {
            if (isEmpty.test(slot) && !isFree.test(slot)) return false;
        }
        return true;
    }

    static boolean allowsBottomClick(AuroraMenu menu, InventoryClickEvent event) {
        var inventory = menu.getInventory();
        return allowsBottomClick(event.getAction(), inventory.getSize(), menu::isFreeSlot, (slot) -> {
            var item = inventory.getItem(slot);
            return item == null || item.isEmpty();
        });
    }
}
