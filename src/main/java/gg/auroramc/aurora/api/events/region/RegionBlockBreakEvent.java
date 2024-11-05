package gg.auroramc.aurora.api.events.region;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class RegionBlockBreakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Player player;
    private final Block block;

    private final boolean natural;

    public RegionBlockBreakEvent(Player player, Block block, boolean natural) {
        this.player = player;
        this.block = block;
        this.natural = natural;
    }

    public Player getPlayerWhoBroke() {
        return player;
    }
}
