package gg.auroramc.aurora.api.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BooleanSupplier;
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

        // Shift clicks only get special handling if they are configured, otherwise they act as normal left/right clicks
        var shiftLeft = event.isShiftClick() && event.isLeftClick() && config.hasShiftLeftClickConfig();
        var shiftRight = event.isShiftClick() && event.isRightClick() && config.hasShiftRightClickConfig();

        var clickTypeRequirements = shiftLeft ? config.getShiftLeftClickRequirements()
                : shiftRight ? config.getShiftRightClickRequirements()
                : event.isLeftClick() ? config.getLeftClickRequirements()
                : event.isRightClick() ? config.getRightClickRequirements()
                : null;

        // Checked again after every [delay] in the actions, so the requirements can't be bypassed while waiting
        BooleanSupplier requirements = () -> Requirement.passes(item.getPlayer(), config.getClickRequirements(), item.getPlaceholders())
                && Requirement.passes(item.getPlayer(), clickTypeRequirements, item.getPlaceholders());

        if (!requirements.getAsBoolean()) {
            return MenuAction.NONE;
        }

        var action = MenuAction.NONE;

        if (smartConsumer != null) {
            action = smartConsumer.apply(event);
        } else if (consumer != null) {
            consumer.accept(event);
        }

        item.runOnClickCommands(item.getPlayer(), requirements);

        if (shiftLeft) {
            item.runOnShiftLeftClickCommands(item.getPlayer(), requirements);
        } else if (shiftRight) {
            item.runOnShiftRightClickCommands(item.getPlayer(), requirements);
        } else if (event.isLeftClick()) {
            item.runOnLeftClickCommands(item.getPlayer(), requirements);
        } else if (event.isRightClick()) {
            item.runOnRightClickCommands(item.getPlayer(), requirements);
        }

        return action;
    }
}
