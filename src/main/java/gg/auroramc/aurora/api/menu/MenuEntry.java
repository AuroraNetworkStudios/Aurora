package gg.auroramc.aurora.api.menu;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;
import java.util.function.Function;

public class MenuEntry {
    @Getter
    private final MenuItem item;
    private final Consumer<InventoryClickEvent> consumer;
    private final Function<InventoryClickEvent, MenuAction> smartConsumer;

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

    public MenuAction handleEvent(InventoryClickEvent event) {
        var action = MenuAction.NONE;

        if (smartConsumer != null) {
            action = smartConsumer.apply(event);
        } else if (consumer != null) {
            consumer.accept(event);
        }

        item.runOnClickCommands(item.getPlayer());

        if (event.isLeftClick()) {
            item.runOnLeftClickCommands(item.getPlayer());
        } else if (event.isRightClick()) {
            item.runOnRightClickCommands(item.getPlayer());
        }

        return action;
    }
}
