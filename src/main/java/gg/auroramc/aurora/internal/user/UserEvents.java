package gg.auroramc.aurora.internal.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.user.AuroraUserUnloadedEvent;
import gg.auroramc.aurora.api.user.AuroraUser;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The loaded and unloaded events of players' data.
 */
public final class UserEvents {
    private final UserState state;
    // Each player's events, called one after another, so a quick rejoin's loaded event can't overtake the unloaded
    // event of the session before (they may run on different threads on Folia)
    private final Map<UUID, CompletableFuture<Void>> eventChains = new ConcurrentHashMap<>();

    public UserEvents(UserState state) {
        this.state = state;
    }

    void unloaded(AuroraUser user) {
        if (Bukkit.isStopping() || Aurora.isDisabling()) return;
        Aurora.logger().debug("Saved user " + user.getUniqueId() + " into storage");
        callEvents(user.getUniqueId(), new AuroraUserUnloadedEvent(user));
    }

    /**
     * Calls the events in order, after the player's earlier events, on the player's own thread if they are online
     * (listeners touch the player, on Folia only that thread may), otherwise (or if they leave before it runs) on the
     * global thread.
     */
    void callEvents(UUID uuid, Event... events) {
        var chained = new AtomicReference<CompletableFuture<Void>>();
        eventChains.compute(uuid, (k, previous) -> {
            // After the previous ones, also if they failed
            var next = (previous == null ? CompletableFuture.<Void>completedFuture(null) : previous.handle((v, e) -> (Void) null)).thenCompose(v -> {
                var done = new CompletableFuture<Void>();
                Runnable call = () -> {
                    try {
                        for (var event : events) Bukkit.getPluginManager().callEvent(event);
                    } finally {
                        done.complete(null);
                    }
                };
                Runnable onGlobal = () -> Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> call.run());
                var player = state.online.containsKey(uuid) ? Bukkit.getPlayer(uuid) : null;
                // Null if they left already, then neither runs
                if (player == null || player.getScheduler().run(Aurora.getInstance(), (task) -> call.run(), onGlobal) == null) {
                    onGlobal.run();
                }
                return done;
            });
            chained.set(next);
            return next;
        });
        // Forgotten once nothing waits behind it
        chained.get().whenComplete((v, e) -> eventChains.remove(uuid, chained.get()));
    }
}
