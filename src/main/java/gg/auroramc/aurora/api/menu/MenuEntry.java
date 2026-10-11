package gg.auroramc.aurora.api.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;
import java.util.function.Function;

public class MenuEntry {
    @Getter
    private final MenuItem item;
    private final Consumer<InventoryClickEvent> consumer;
    private final Function<InventoryClickEvent, MenuAction> smartConsumer;
    @Getter
    @Setter
    private boolean active = false;

    public MenuEntry(MenuItem item, Function<InventoryClickEvent, MenuAction> smartConsumer) {
        this.item = item;
        this.consumer = null;
        this.smartConsumer = smartConsumer;
    }

    public MenuEntry(MenuItem item, Consumer<InventoryClickEvent> handler) {
        this.item = item;
        this.consumer = handler;
        this.smartConsumer = null;
    }

    public MenuEntry(MenuItem item) {
        this.item = item;
        this.consumer = null;
        this.smartConsumer = null;
    }

    public int getPriority() {
        return item.getItemBuilder().getConfig().getPriority();
    }

    public MenuAction handleEvent(InventoryClickEvent event) {
        var config = item.getItemBuilder().getConfig();

        if (!Requirement.passes(item.getPlayer(), config.getClickRequirements(), item.getPlaceholders())) {
            return MenuAction.NONE;
        }

        // Shift clicks only get special handling if they are configured, otherwise they act as normal left/right clicks
        var shiftLeft = event.isShiftClick() && event.isLeftClick() && config.hasShiftLeftClickConfig();
        var shiftRight = event.isShiftClick() && event.isRightClick() && config.hasShiftRightClickConfig();

        if (shiftLeft) {
            if (!Requirement.passes(item.getPlayer(), config.getShiftLeftClickRequirements(), item.getPlaceholders())) {
                return MenuAction.NONE;
            }
        } else if (shiftRight) {
            if (!Requirement.passes(item.getPlayer(), config.getShiftRightClickRequirements(), item.getPlaceholders())) {
                return MenuAction.NONE;
            }
        } else if (event.isLeftClick()) {
            if (!Requirement.passes(item.getPlayer(), config.getLeftClickRequirements(), item.getPlaceholders())) {
                return MenuAction.NONE;
            }
        } else if (event.isRightClick()) {
            if (!Requirement.passes(item.getPlayer(), config.getRightClickRequirements(), item.getPlaceholders())) {
                return MenuAction.NONE;
            }
        }

        var action = MenuAction.NONE;

        if (smartConsumer != null) {
            action = smartConsumer.apply(event);
        } else if (consumer != null) {
            consumer.accept(event);
        }

        item.runOnClickCommands(item.getPlayer());

        if (shiftLeft) {
            item.runOnShiftLeftClickCommands(item.getPlayer());
        } else if (shiftRight) {
            item.runOnShiftRightClickCommands(item.getPlayer());
        } else if (event.isLeftClick()) {
            item.runOnLeftClickCommands(item.getPlayer());
        } else if (event.isRightClick()) {
            item.runOnRightClickCommands(item.getPlayer());
        }

        return action;
    }
}
