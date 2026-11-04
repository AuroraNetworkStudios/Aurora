package gg.auroramc.aurora.internal.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static gg.auroramc.aurora.internal.user.UserState.supplyAsync;
import static gg.auroramc.aurora.internal.user.UserState.test;

/**
 * Offline players' data in memory (see {@link UserState#offlineCache}): loaded once to be edited or read, and dropped
 * once it's saved and nothing needs it anymore.
 */
public final class OfflineCopies {
    private final UserState state;

    public OfflineCopies(UserState state) {
        this.state = state;
    }

    // Call with the player's lock held. Null if offline-player-cache is turned off, or their data couldn't be loaded.
    AuroraUser getOfflineCopy(UUID uuid) {
        // Turned off for servers sharing a database, offline players are left alone then
        if (!Aurora.getLibConfig().getOfflinePlayerCache()) return null;
        var user = state.offlineCache.getIfPresent(uuid);
        if (user == null) {
            user = state.storage.loadUser(uuid, state.dataHolders);
            if (!user.isLoaded()) return null;
            state.offlineCache.put(uuid, user);
        }
        return user;
    }

    /**
     * Edits an offline player's copy, loading it first if needed. Call with their lock held.
     *
     * @return the result of each edit, all false if their data couldn't be loaded
     */
    List<Boolean> editOffline(UUID uuid, List<Predicate<AuroraUser>> edits) {
        var edited = getOfflineCopy(uuid);
        if (edited == null) return edits.stream().map(edit -> false).toList();
        var results = edits.stream().map(edit -> test(edit, edited)).toList();
        // Saved later, like an online player
        if (results.contains(true)) state.unsavedOfflineCopies.add(uuid);
        return results;
    }

    // Call with the player's save and player lock held (so it isn't being written right now). An offline copy kept for
    // a rejoin (see Saving#saveOfflineCopy) whose session never took it over is dropped here once it's saved, it could
    // be outdated when they join next time (by playing on another server sharing the database).
    void dropIfSaved(UUID uuid) {
        if (state.online.containsKey(uuid) || state.unsavedOfflineCopies.contains(uuid) || state.unsavedQuits.contains(uuid)) return;
        state.offlineCache.invalidate(uuid);
    }

    // Like dropIfSaved, taking the locks. Call without holding any.
    void dropIfSavedLocking(UUID uuid) {
        state.writes.readLock().lock();
        try {
            synchronized (state.saveLock(uuid)) {
                synchronized (state.playerLock(uuid)) {
                    dropIfSaved(uuid);
                }
            }
        } finally {
            state.writes.readLock().unlock();
        }
    }

    /**
     * Loads user directly from storage. For an offline player loaded to be edited, it's that copy instead, since the
     * storage may not have their latest edits yet.
     */
    public CompletableFuture<AuroraUser> loadUserFromStorage(UUID uuid) {
        return supplyAsync(() -> {
            var edited = state.offlineCache.getIfPresent(uuid);
            return edited != null ? edited : state.storage.loadUser(uuid, state.dataHolders);
        // Callbacks run off the storage threads, one waiting for another storage task can't starve them
        }).thenApplyAsync(Function.identity());
    }
}
