package gg.auroramc.aurora.api.events.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class PlayerRegionLeaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Player player;
    private final List<ProtectedRegion> regions;

    public PlayerRegionLeaveEvent(Player player, List<ProtectedRegion> regions) {
        this.player = player;
        this.regions = regions;
    }

}
