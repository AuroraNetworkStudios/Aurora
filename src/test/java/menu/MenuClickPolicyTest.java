package menu;

import gg.auroramc.aurora.api.menu.MenuClickPolicy;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MenuClickPolicyTest {
    private static final Set<ClickType> BUTTON_CLICKS = EnumSet.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT);

    @Test
    public void testOnlyMouseClicksPressButtons() {
        for (var click : ClickType.values()) {
            assertEquals(BUTTON_CLICKS.contains(click), MenuClickPolicy.isButtonClick(click), click.name());
        }
    }

    @Test
    public void testFreeSlotsNeverCollectToCursor() {
        for (var action : InventoryAction.values()) {
            assertEquals(action != InventoryAction.COLLECT_TO_CURSOR, MenuClickPolicy.allowsFreeSlotClick(action), action.name());
        }
    }

    @Test
    public void testBottomClicksNeverCollectToCursor() {
        assertFalse(MenuClickPolicy.allowsBottomClick(InventoryAction.COLLECT_TO_CURSOR, 9, slot -> true, slot -> true));
    }

    @Test
    public void testBottomClicksThatStayInThePlayerInventory() {
        for (var action : InventoryAction.values()) {
            if (action == InventoryAction.COLLECT_TO_CURSOR || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) continue;
            assertTrue(MenuClickPolicy.allowsBottomClick(action, 9, slot -> false, slot -> true), action.name());
        }
    }

    @Test
    public void testShiftClickOnlyWhenItCanOnlyLandInAFreeSlot() {
        // Slots 0-3 free, the rest are buttons or filler
        var free = (java.util.function.IntPredicate) slot -> slot < 4;

        // Every empty slot is free
        assertTrue(MenuClickPolicy.allowsBottomClick(InventoryAction.MOVE_TO_OTHER_INVENTORY, 9, free, slot -> slot < 2));
        // An empty slot that isn't free, like an AIR filler or a managed slot
        assertFalse(MenuClickPolicy.allowsBottomClick(InventoryAction.MOVE_TO_OTHER_INVENTORY, 9, free, slot -> slot == 1 || slot == 7));
        // Nothing empty, the items stay where they are
        assertTrue(MenuClickPolicy.allowsBottomClick(InventoryAction.MOVE_TO_OTHER_INVENTORY, 9, free, slot -> false));
    }
}
