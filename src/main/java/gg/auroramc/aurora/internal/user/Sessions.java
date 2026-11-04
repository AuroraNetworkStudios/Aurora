package gg.auroramc.aurora.internal.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserUnloadedEvent;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.internal.expansions.leaderboard.LeaderboardExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static gg.auroramc.aurora.internal.user.UserState.runAsync;

/**
 * Players' sessions here: their data is loaded when they join, and when they leave it becomes their offline copy
 * until it's saved. Also purging a player's data.
 */
public final class Sessions {
    private static final int LOAD_ATTEMPTS = 3;
    private static final long LOAD_RETRY_SECONDS = 2;

    private final UserState state;
    private final Edits edits;
    private final OfflineCopies offline;
    private final Saving saving;
    private final UserEvents events;

    public Sessions(UserState state, Edits edits, OfflineCopies offline, Saving saving, UserEvents events) {
        this.state = state;
        this.edits = edits;
        this.offline = offline;
        this.saving = saving;
        this.events = events;
    }

    public boolean isUserCached(UUID uuid) {
        return state.cache.getIfPresent(uuid) != null;
    }

    public AuroraUser getUser(UUID uuid) {
        var user = state.cache.getIfPresent(uuid);
        if (user != null) return user;

        // Try to compute atomically, preventing race
        // In very rare scenarios it was possible that loadUser was put the actual
        // loaded user into cache after the getIfPresent check here.
        // This computeIfAbsent instead if put will hopefully prevent that
        return state.cache.asMap().computeIfAbsent(uuid, (key) -> {
            var fakeUser = new AuroraUser(key, false);
            fakeUser.initData(null, state.dataHolders);
            Aurora.logger().debug("Created fake user " + key + " in cache");
            return fakeUser;
        });
    }

    public void loadUser(UUID uuid) {
        loadUser(uuid, 1);
    }

    private void loadUser(UUID uuid, int attempt) {
        runAsync(() -> {
            synchronized (state.playerLock(uuid)) {
                var cached = state.cache.getIfPresent(uuid);
                // Joined again before their quit was handled, their session just goes on
                if (cached != null && cached.isLoaded()) {
                    edits.makePendingEdits(uuid, cached);
                    events.callEvents(uuid, new AuroraUserLoadedEvent(cached));
                    return;
                }
            }

            // Not holding their lock while it's read
            var purgesBefore = state.purges.getOrDefault(uuid, 0L);
            state.storage.loadUser(uuid, state.dataHolders, loaded -> {
                // Not waiting for a save of everyone (this may hold a database connection the migration needs)
                if (state.stopping || state.migrator.isMigrating()) {
                    synchronized (state.playerLock(uuid)) {
                        edits.makePendingEdits(uuid, null);
                    }
                    return;
                }
                // After a write of theirs that's running (so a quit save finishes before their session starts), and
                // not while everyone is being saved
                state.writes.readLock().lock();
                try {
                    synchronized (state.saveLock(uuid)) {
                        synchronized (state.playerLock(uuid)) {
                            onLoaded(uuid, attempt, loaded, purgesBefore);
                        }
                    }
                } finally {
                    state.writes.readLock().unlock();
                }
            });
        });
    }

    // Call with the player's save and player lock held
    private void onLoaded(UUID uuid, int attempt, AuroraUser loaded, long purgesBefore) {
        if (state.stopping || state.migrator.isMigrating()) {
            edits.makePendingEdits(uuid, null);
            return;
        }
        if (!state.online.containsKey(uuid)) {
            edits.makePendingEditsOffline(uuid);
            offline.dropIfSaved(uuid);
            return;
        }
        // Purged after this was read, it has to be read again
        if (state.purges.getOrDefault(uuid, 0L) != purgesBefore) {
            loadUser(uuid, attempt);
            return;
        }

        var maybeUser = state.cache.getIfPresent(uuid);
        // An earlier join's load finished first, their session goes on with what's in memory
        if (maybeUser != null && maybeUser.isLoaded()) {
            edits.makePendingEdits(uuid, maybeUser);
            events.callEvents(uuid, new AuroraUserLoadedEvent(maybeUser));
            return;
        }

        // Kept since they left, or edited while offline (maybe after this load read their data), that
        // copy is the newest. From now on their session is the only copy, and saves it.
        // It may have changes that aren't saved yet, so it's used even if offline-player-cache is off.
        var user = state.offlineCache.getIfPresent(uuid);
        state.offlineCache.invalidate(uuid);
        state.unsavedOfflineCopies.remove(uuid);
        state.unsavedQuits.remove(uuid);
        if (user == null) user = loaded;

        if (!user.isLoaded()) {
            // Never go on with empty data, it would overwrite their real data once saved
            onLoadFailed(uuid, attempt);
            return;
        }

        var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        if (maybeUser != null) {
            lbm.loadUser(uuid).thenAcceptAsync(maybeUser.getLeaderboardEntries()::putAll);
            maybeUser.loadFromUser(user);
            user = maybeUser;
            Aurora.logger().debug("Updated user " + uuid + " in cache");
        } else {
            lbm.loadUser(uuid).thenAcceptAsync(user.getLeaderboardEntries()::putAll);
            state.cache.put(uuid, user);
            Aurora.logger().debug("Loaded user " + uuid + " into cache");
        }

        edits.makePendingEdits(uuid, user);
        events.callEvents(uuid, new AuroraUserLoadedEvent(user));
    }

    private void onLoadFailed(UUID uuid, int attempt) {
        if (attempt < LOAD_ATTEMPTS) {
            Aurora.logger().warning("Failed to load the data of " + uuid + " (attempt " + attempt + "/" + LOAD_ATTEMPTS + "), trying again in " + LOAD_RETRY_SECONDS + " seconds");
            Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
                if (state.online.containsKey(uuid)) loadUser(uuid, attempt + 1);
            }, LOAD_RETRY_SECONDS, TimeUnit.SECONDS);
            return;
        }

        Aurora.logger().severe("Failed to load the data of " + uuid + " " + LOAD_ATTEMPTS + " times, kicking them so nothing they do is lost. Check the errors above!");
        edits.makePendingEdits(uuid, null);
        var player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getScheduler().run(Aurora.getInstance(),
                    (task) -> player.kick(Text.component(Aurora.getMsg(player).getKickedByFailedLoad())), null);
        }
    }

    public void purgeUserData(UUID uuid) {
        runAsync(() -> {
            // After a write of theirs that's running, so it can't bring the data back. The storage is purged under their
            // player lock, so an edit can't load the old data meanwhile.
            state.writes.readLock().lock();
            try {
                synchronized (state.saveLock(uuid)) {
                    synchronized (state.playerLock(uuid)) {
                        purge(uuid);
                    }
                }
            } finally {
                state.writes.readLock().unlock();
            }
        });
    }

    // Call with the player's save and player lock held
    private void purge(UUID uuid) {
        state.purges.merge(uuid, 1L, Long::sum);
        state.offlineCache.invalidate(uuid);
        state.unsavedOfflineCopies.remove(uuid);
        state.unsavedQuits.remove(uuid);
        var oldUser = state.cache.getIfPresent(uuid);
        if (oldUser != null && oldUser.isLoaded()) {
            // So nothing still holding the old object saves it
            oldUser.setLoaded(false);
            state.cache.invalidate(uuid);
            var newUser = new AuroraUser(uuid, true);
            newUser.initData(new YamlConfiguration(), state.dataHolders);
            state.cache.put(uuid, newUser);
            state.storage.purgeUser(uuid);
            events.callEvents(uuid, new AuroraUserUnloadedEvent(oldUser), new AuroraUserLoadedEvent(newUser));
        } else {
            if (oldUser != null) state.cache.invalidate(uuid);
            state.storage.purgeUser(uuid);
        }
    }

    public void invalidate(Player player) {
        var user = state.cache.getIfPresent(player.getUniqueId());
        if (user == null) {
            // Left before their data loaded, edits waiting for it are made to their saved data
            var uuid = player.getUniqueId();
            runAsync(() -> {
                synchronized (state.playerLock(uuid)) {
                    if (!state.online.containsKey(uuid)) edits.makePendingEditsOffline(uuid);
                }
                offline.dropIfSavedLocking(uuid);
            });
            return;
        }
        runAsync(() -> {
            try {
                if (user.isLoaded()) {
                    var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
                    lbm.updateUser(user, Collections.emptyList()).join();
                }
            } finally {
                saveOnQuit(user);
            }
        });
    }

    private void saveOnQuit(AuroraUser user) {
        var uuid = user.getUniqueId();
        synchronized (state.playerLock(uuid)) {
            // Rejoined before we got here, their session just goes on
            if (state.online.containsKey(uuid) || state.cache.getIfPresent(uuid) != user) return;
            state.cache.asMap().remove(uuid, user);
            Aurora.logger().debug("Removed user " + uuid + " from cache");
            if (!user.isLoaded()) {
                // Their data never loaded, there is nothing to save. Saving it would overwrite their real data.
                edits.makePendingEditsOffline(uuid);
                Bukkit.getAsyncScheduler().runNow(Aurora.getInstance(), (task) -> offline.dropIfSavedLocking(uuid));
                return;
            }
            // Their offline copy from now on: edits are made to it (if offline-player-cache is on), a rejoin goes on
            // with it, and it's kept until it's saved
            state.offlineCache.put(uuid, user);
            state.unsavedOfflineCopies.add(uuid);
            state.unsavedQuits.add(uuid);
        }
        saving.saveQuit(uuid, 1);
    }

    // At the start of their join, before every other join listener, and for the players already online when Aurora
    // starts
    public void joinedEarly(Player player) {
        // Kicked by an earlier listener already, their quit came before this
        if (!player.isConnected()) return;
        state.online.put(player.getUniqueId(), player);
    }

    public void quit(Player player) {
        state.online.remove(player.getUniqueId(), player);
        if (state.migrator.isMigrating()) return;
        Bukkit.getGlobalRegionScheduler().runDelayed(Aurora.getInstance(),
                (task) -> invalidate(player), 1);
    }

    // New players are turned away while the data moves to another storage
    public boolean isMigrating() {
        return state.migrator.isMigrating();
    }

    public boolean isOnline(UUID uuid) {
        return state.online.containsKey(uuid);
    }
}
