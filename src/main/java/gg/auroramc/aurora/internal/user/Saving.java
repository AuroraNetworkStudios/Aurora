package gg.auroramc.aurora.internal.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.internal.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.internal.expansions.leaderboard.storage.LeaderboardStorage;
import gg.auroramc.aurora.internal.user.storage.YamlStorage;
import gg.auroramc.aurora.internal.user.storage.sql.MySqlStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static gg.auroramc.aurora.internal.user.UserState.supplyAsync;

/**
 * Writing players' data to the storage: single saves (in order per player, see {@link UserState}), the autosave, the
 * leaderboard updates, saving a player who left until it works, and saving everyone when shutting down or migrating.
 */
public final class Saving {
    private static final long MAX_QUIT_SAVE_RETRY_SECONDS = 30;

    private final UserState state;
    private final Edits edits;
    private final UserEvents events;
    private ScheduledTask autoSaveTask;
    private ScheduledTask leaderboardUpdateTask;

    private record ToSave(AuroraUser user, SaveReason reason) {
    }

    public Saving(UserState state, Edits edits, UserEvents events) {
        this.state = state;
        this.edits = edits;
        this.events = events;
    }

    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        return save(user.getUniqueId(), () -> new ToSave(user, reason), (saved, written) -> {
            if (written && reason == SaveReason.QUIT) events.unloaded(user);
        });
    }

    /**
     * Saves a player's data without holding their lock while it's written, so their edits and reads never wait for
     * the storage. Under their lock, pick chooses what to save (null for nothing) and it's serialized right away.
     * After writing it, done gets it with whether it was written, under their lock again.
     *
     * @return false if it couldn't be written
     */
    private boolean save(UUID uuid, Supplier<ToSave> pick, BiConsumer<ToSave, Boolean> done) {
        state.writes.readLock().lock();
        try {
            synchronized (state.saveLock(uuid)) {
                ToSave toSave;
                UserStorage.PendingSave pending;
                synchronized (state.playerLock(uuid)) {
                    toSave = pick.get();
                    if (toSave == null) return true;
                    try {
                        pending = state.storage.prepareSave(toSave.user(), toSave.reason());
                    } catch (Exception e) {
                        Aurora.logger().severe("Failed to save user data for player: " + uuid + " error: " + e);
                        pending = () -> false;
                    }
                }
                boolean written;
                try {
                    written = pending.write();
                } catch (Exception e) {
                    Aurora.logger().severe("Failed to save user data for player: " + uuid + " error: " + e);
                    written = false;
                }
                // Still holding their save lock, so no newer copy was taken to be written meanwhile
                synchronized (state.playerLock(uuid)) {
                    done.accept(toSave, written);
                }
                return written;
            }
        } finally {
            state.writes.readLock().unlock();
        }
    }

    /**
     * Starts the autosave and the leaderboard updates.
     */
    public void startTasks() {
        autoSaveTask();
        leaderboardUpdateTask();
    }

    private void autoSaveTask() {
        this.autoSaveTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            var values = state.cache.asMap().values();
            var toSave = values.stream().filter(u -> u.isLoaded() && u.isDirty()).toList();
            var offline = state.unsavedOfflineCopies.size();
            if (offline > 0) {
                var saved = saveOfflineEdits(SaveReason.AUTO_SAVE);
                Aurora.logger().info("Auto background saved user data for " + saved + "/" + offline + " edited offline players");
            }
            if (!toSave.isEmpty()) {
                var successCount = toSave.stream().filter(this::autoSave).count();
                var all = toSave.size();
                if (!Bukkit.getOnlinePlayers().isEmpty() && !values.isEmpty()) {
                    Aurora.logger().info("Auto background saved user data for " + successCount + "/" + all + " online players");
                }
            }
            autoSaveTask();
        }, Aurora.getLibConfig().getUserAutoSaveInMinutes(), TimeUnit.MINUTES);
    }

    // Only while it's still their session here. If they left since the autosave started, it's their offline copy now,
    // saved on its own.
    private boolean autoSave(AuroraUser user) {
        var uuid = user.getUniqueId();
        return save(uuid, () -> user.isLoaded() && state.cache.getIfPresent(uuid) == user ? new ToSave(user, SaveReason.AUTO_SAVE) : null,
                (saved, written) -> {
                });
    }

    /**
     * Saves the offline copies with changes the storage doesn't have yet.
     *
     * @return how many of them were saved
     */
    private int saveOfflineEdits(SaveReason reason) {
        int saved = 0;
        for (var uuid : List.copyOf(state.unsavedOfflineCopies)) {
            if (saveOfflineCopy(uuid, reason)) {
                saved++;
            } else {
                Aurora.logger().severe("Failed to save the data of offline player: " + uuid);
            }
        }
        return saved;
    }

    /**
     * Saves a player's offline copy if it has changes the storage doesn't have yet. For a player who left, it's saved
     * as their quit, and dropped from memory after that unless it was edited meanwhile.
     *
     * @return false if it couldn't be written
     */
    private boolean saveOfflineCopy(UUID uuid, SaveReason reason) {
        return save(uuid, () -> {
            var copy = state.offlineCache.getIfPresent(uuid);
            // Joined in the meantime (their session has it now), or nothing changed since it was saved
            if (!state.unsavedOfflineCopies.remove(uuid) || copy == null) return null;
            return new ToSave(copy, state.unsavedQuits.contains(uuid) ? SaveReason.QUIT : reason);
        }, (saved, written) -> {
            // Joined meanwhile, their session goes on with it and saves it
            if (state.offlineCache.getIfPresent(uuid) != saved.user()) return;
            if (!written) {
                state.unsavedOfflineCopies.add(uuid);
                return;
            }
            if (saved.reason() != SaveReason.QUIT) return;
            state.unsavedQuits.remove(uuid);
            events.unloaded(saved.user());
            // Kept if it was edited after this was taken to be saved. Also if they joined again: their load may have read
            // the storage before this was written, it goes on with this copy instead.
            if (!state.unsavedOfflineCopies.contains(uuid) && !state.online.containsKey(uuid)) state.offlineCache.invalidate(uuid);
        });
    }

    /**
     * Saves the offline copy of a player who left, again later if that fails, until it's saved (or they come back,
     * or the server stops).
     */
    void saveQuit(UUID uuid, int attempt) {
        if (saveOfflineCopy(uuid, SaveReason.QUIT) || !state.unsavedQuits.contains(uuid) || state.stopping) return;
        var delay = Math.min(MAX_QUIT_SAVE_RETRY_SECONDS, 1L << Math.min(attempt, 5));
        Aurora.logger().severe("Failed to save the data of " + uuid + " after they left (attempt " + attempt + "), it's kept in memory and saving it again in " + delay + " seconds");
        Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> saveQuit(uuid, attempt + 1), delay, TimeUnit.SECONDS);
    }

    private void leaderboardUpdateTask() {
        this.leaderboardUpdateTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
            var values = state.cache.asMap().values();
            var toUpdate = new HashMap<UUID, Collection<String>>();
            for (var user : values) {
                var dirtyBoards = user.getDirtyLeaderboards();
                if (!user.isLoaded() || dirtyBoards.isEmpty()) {
                    continue;
                }
                user.updateOriginalLeaderboardDataFromCurrent();
                toUpdate.put(user.getUniqueId(), dirtyBoards.keySet());
            }
            if (toUpdate.isEmpty()) {
                lbm.updateLeaderBoards();
                leaderboardUpdateTask();
                return;
            }
            lbm.bulkUpdateUsers(toUpdate).thenRunAsync(lbm::updateLeaderBoards).thenRun(this::leaderboardUpdateTask);
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * Stops the autosave and leaderboard updates, and saves everyone in memory on this thread. With dispose (shutting
     * down), edits are refused from now on and the storage is closed afterwards.
     */
    public void stopTasksAndSaveAllData(boolean dispose) {
        // Edits after this save would be lost
        if (dispose) {
            state.stopping = true;
            edits.finishEdits();
        }
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (leaderboardUpdateTask != null) leaderboardUpdateTask.cancel();
        // After the writes that are running, and none start until everyone is saved
        state.writes.writeLock().lock();
        try {
            var users = state.cache.asMap().values().stream().filter(AuroraUser::isLoaded).toList();
            if (state.storage.bulkSaveUsers(users, SaveReason.QUIT) == 0 && !users.isEmpty()) {
                // One player's data that can't be saved shouldn't stop everyone else's
                Aurora.logger().severe("Failed to save everyone's data at once while shutting down, saving them one by one");
                for (var user : users) {
                    if (!state.storage.saveUser(user, SaveReason.QUIT)) {
                        Aurora.logger().severe("Failed to save the data of " + user.getUniqueId() + " while shutting down, changes since their last save are lost");
                    }
                }
            }
            // After the sessions, so one that just became an offline copy (its player left meanwhile) is saved either way
            saveOfflineEdits(SaveReason.CUSTOM);
        } finally {
            state.writes.writeLock().unlock();
        }
        var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        lbm.bulkUpdateUsers(state.cache.asMap().values().stream().filter(AuroraUser::isLoaded).collect(HashMap::new, (m, u) -> m.put(u.getUniqueId(), u.getDirtyLeaderboards().keySet()), HashMap::putAll)).join();
        if (dispose) state.storage.dispose();
    }

    /**
     * Moves everyone's data to the other storage (MySQL to YAML or the other way around), and uses that one from now
     * on. Everyone is saved first and kicked, and nobody can join until it's done.
     */
    public CompletableFuture<Boolean> migrate(int threadCount) {
        state.migrator.markMigrating();
        edits.finishEdits();

        return supplyAsync(() -> {
            // No write gets between saving everyone and copying the storage
            state.writes.writeLock().lock();
            try {
                stopTasksAndSaveAllData(false);
                if (!state.unsavedOfflineCopies.isEmpty()) {
                    // The migration copies the storage, edits it doesn't have yet would be lost
                    Aurora.logger().severe("Not migrating, the edits of " + state.unsavedOfflineCopies.size() + " offline players couldn't be saved. Check the errors above!");
                    autoSaveTask();
                    leaderboardUpdateTask();
                    state.migrator.markFinished();
                    return false;
                }
                state.cache.invalidateAll();
                state.offlineCache.invalidateAll();
                state.unsavedOfflineCopies.clear();
                state.unsavedQuits.clear();

                // Each on their own thread, on Folia a player can only be kicked from there
                for (var player : Bukkit.getOnlinePlayers()) {
                    player.getScheduler().run(Aurora.getInstance(),
                            (task) -> player.kick(Text.component(Aurora.getMsg(player).getKickedByDbMigration())), null);
                }

                UserStorage newStorage = state.storage;
                String newStorageId = state.storage instanceof MySqlStorage ? "yaml" : "mysql";
                boolean success = false;

                if (state.storage instanceof MySqlStorage mySqlStorage) {
                    newStorage = new YamlStorage();
                    success = state.migrator.migrateUserData(mySqlStorage, (YamlStorage) newStorage, threadCount);
                } else if (state.storage instanceof YamlStorage yamlStorage) {
                    newStorage = new MySqlStorage();
                    success = state.migrator.migrateUserData(yamlStorage, (MySqlStorage) newStorage, threadCount);
                }

                if (success) {
                    state.storage.dispose();
                    state.storage = newStorage;
                    Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class).setStorage(LeaderboardStorage.forUserStorage(newStorage));
                    // Only that line of the file changes
                    Aurora.getLibConfigFile().set("storage-type", newStorageId);
                }

                // With whichever storage is used now
                autoSaveTask();
                leaderboardUpdateTask();
                state.migrator.markFinished();

                return success;
            } finally {
                state.writes.writeLock().unlock();
            }
        });
    }
}
