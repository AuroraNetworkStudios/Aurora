package gg.auroramc.auroralib.api.user.event;

import gg.auroramc.auroralib.api.user.AuroraUser;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class AuroraUserUnloadedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final AuroraUser user;

    public AuroraUserUnloadedEvent(AuroraUser user) {
        this.user = user;
    }
}
