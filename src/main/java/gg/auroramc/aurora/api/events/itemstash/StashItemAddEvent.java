package gg.auroramc.aurora.api.events.itemstash;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class StashItemAddEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    private final ItemStack item;
    private final UUID playerUniqueId;
    private boolean cancelled = false;

    public StashItemAddEvent(UUID uuid, ItemStack item) {
        super(!Bukkit.isPrimaryThread());
        this.item = item;
        this.playerUniqueId = uuid;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
