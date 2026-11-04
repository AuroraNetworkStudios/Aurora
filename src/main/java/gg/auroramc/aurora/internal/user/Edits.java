package gg.auroramc.aurora.internal.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.internal.user.UserState.PendingEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static gg.auroramc.aurora.internal.user.UserState.runAsync;
import static gg.auroramc.aurora.internal.user.UserState.test;

/**
 * Changing and reading players' data, online or offline: see UserManager's editUser, editUserNow and getUserNow. Edits for a player whose data is still loading wait here until it's loaded.
 */
public final class Edits {
    private final UserState state;
    private final OfflineCopies offline;

    public Edits(UserState state, OfflineCopies offline) {
        this.state = state;
        this.offline = offline;
    }

    public CompletableFuture<Boolean> editUser(UUID uuid, Predicate<AuroraUser> edit) {
        var result = new CompletableFuture<Boolean>();
        runAsync(() -> {
            synchronized (state.playerLock(uuid)) {
                // Their data is being moved to another storage, or saved for shutting down
                if (state.stopping || state.migrator.isMigrating()) {
                    result.complete(false);
                    return;
                }
                var cached = state.cache.getIfPresent(uuid);
                if (cached != null && cached.isLoaded()) {
                    result.complete(test(edit, cached));
                    return;
                }
                if (state.online.containsKey(uuid)) {
                    state.pendingEdits.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new PendingEdit(edit, result));
                    return;
                }
                result.complete(offline.editOffline(uuid, List.of(edit)).getFirst());
            }
        }).exceptionally(e -> {
            Aurora.logger().severe("Failed to edit the data of player: " + uuid + " error: " + e);
            result.complete(false);
            return null;
        });
        // Callbacks run after the player's lock is released, so one waiting for another edit can't deadlock
        return result.thenApplyAsync(Function.identity());
    }

    public boolean editUserNow(UUID uuid, Predicate<AuroraUser> edit) {
        synchronized (state.playerLock(uuid)) {
            if (state.stopping || state.migrator.isMigrating()) return false;
            var cached = state.cache.getIfPresent(uuid);
            if (cached != null && cached.isLoaded()) return test(edit, cached);
            if (state.online.containsKey(uuid)) return false;
            return offline.editOffline(uuid, List.of(edit)).getFirst();
        }
    }

    public AuroraUser getUserNow(UUID uuid) {
        synchronized (state.playerLock(uuid)) {
            if (state.stopping || state.migrator.isMigrating()) return null;
            var cached = state.cache.getIfPresent(uuid);
            if (cached != null && cached.isLoaded()) return cached;
            if (state.online.containsKey(uuid)) return null;
            return offline.getOfflineCopy(uuid);
        }
    }

    /**
     * Waits for the edits that are being made right now (they hold their player's lock), and refuses the ones still
     * waiting for a player's data to load. New edits are refused once stopping or migrating is set.
     */
    void finishEdits() {
        for (var lock : state.playerLocks) {
            synchronized (lock) {
                // Nothing, holding it once means whoever had it is done
            }
        }
        for (var uuid : List.copyOf(state.pendingEdits.keySet())) {
            synchronized (state.playerLock(uuid)) {
                makePendingEdits(uuid, null);
            }
        }
    }

    // Call with the player's lock held
    void makePendingEdits(UUID uuid, AuroraUser user) {
        var edits = state.pendingEdits.remove(uuid);
        if (edits == null) return;
        for (var pending : edits) {
            pending.result().complete(user != null && test(pending.edit(), user));
        }
    }

    // Call with the player's lock held. They left before their data loaded, so their offline copy is edited.
    void makePendingEditsOffline(UUID uuid) {
        if (state.stopping || state.migrator.isMigrating()) {
            makePendingEdits(uuid, null);
            return;
        }
        var edits = state.pendingEdits.remove(uuid);
        if (edits == null) return;
        List<Boolean> results;
        try {
            results = offline.editOffline(uuid, edits.stream().map(PendingEdit::edit).toList());
        } catch (Throwable e) {
            Aurora.logger().severe("Failed to edit the data of player: " + uuid + " error: " + e);
            results = edits.stream().map(edit -> false).toList();
        }
        for (int i = 0; i < edits.size(); i++) {
            edits.get(i).result().complete(results.get(i));
        }
    }
}
