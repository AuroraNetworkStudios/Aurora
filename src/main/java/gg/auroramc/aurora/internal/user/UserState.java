package gg.auroramc.aurora.internal.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.internal.user.migration.StorageMigrator;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Everything the parts of the user manager share: the players' data in memory, and the locks.
 * <p>
 * The locks, always taken in this order (a later one is never held while taking an earlier one):
 * <ol>
 *     <li>{@link #writes}: every write holds it shared, saving everyone at once (shutting down, migrating) holds it
 *     alone.</li>
 *     <li>A player's save lock ({@link #saveLock}): keeps the writes of each player's data in order, so an older copy
 *     is never written over a newer one.</li>
 *     <li>A player's lock ({@link #playerLock}): held while their data in memory is read or changed, and while deciding
 *     what to save. Nothing waits for the storage while holding one, except loading an offline player the first time
 *     they are edited or read, and purging a player.</li>
 * </ol>
 * The save and player locks are a fixed set shared by players, so a player's lock is never replaced while someone
 * holds it.
 */
public final class UserState {
    final Object[] playerLocks = Stream.generate(Object::new).limit(1024).toArray();
    final Object[] saveLocks = Stream.generate(Object::new).limit(1024).toArray();
    final ReentrantReadWriteLock writes = new ReentrantReadWriteLock();

    volatile UserStorage storage;
    final Set<Class<? extends UserDataHolder>> dataHolders = new HashSet<>();
    final StorageMigrator migrator;
    volatile boolean stopping = false;

    // Stores actual online users data
    final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder().build();
    // Edits for players who are online here, but whose data is still loading. Made as soon as it's loaded.
    final Map<UUID, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();
    // Offline players in memory: loaded to be edited, kept until they join here (then their session takes over), so
    // they are only loaded once. Players who leave are kept here too until their quit is saved (longer if they are
    // edited meanwhile). Like online players, they are saved by the autosave and when shutting down.
    final Cache<UUID, AuroraUser> offlineCache = CacheBuilder.newBuilder().build();
    // The offline copies above with changes the storage doesn't have yet
    final Set<UUID> unsavedOfflineCopies = ConcurrentHashMap.newKeySet();
    // Players who left, whose offline copy (see offlineCache) isn't saved for their quit yet
    final Set<UUID> unsavedQuits = ConcurrentHashMap.newKeySet();
    // How many times each player was purged, a load that read their data before a purge reads it again
    final Map<UUID, Long> purges = new ConcurrentHashMap<>();
    // Players online here, from their join until their quit event, by session (a quit only ends its own session). Not
    // Bukkit.getPlayer: on Folia a player who just left can still be found there for a moment from other threads, which
    // looked like a rejoin and skipped their save.
    final Map<UUID, Player> online = new ConcurrentHashMap<>();

    record PendingEdit(Predicate<AuroraUser> edit, CompletableFuture<Boolean> result) {
    }

    // The storage is set right after, see setStorage
    public UserState(StorageMigrator migrator) {
        this.migrator = migrator;
    }

    public UserStorage getStorage() {
        return storage;
    }

    public void setStorage(UserStorage storage) {
        this.storage = storage;
    }

    public Set<Class<? extends UserDataHolder>> getDataHolders() {
        return dataHolders;
    }

    Object playerLock(UUID playerId) {
        return playerLocks[Math.floorMod(playerId.hashCode(), playerLocks.length)];
    }

    Object saveLock(UUID playerId) {
        return saveLocks[Math.floorMod(playerId.hashCode(), saveLocks.length)];
    }

    // On Aurora's storage threads, see Aurora#getStorageExecutor
    // After shutting down, it fails instead of throwing at the caller
    static CompletableFuture<Void> runAsync(Runnable task) {
        try {
            return CompletableFuture.runAsync(task, Aurora.getStorageExecutor());
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    static <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, Aurora.getStorageExecutor());
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    static boolean test(Predicate<AuroraUser> edit, AuroraUser user) {
        try {
            return edit.test(user);
        } catch (Exception e) {
            Aurora.logger().severe("Failed to edit the data of player: " + user.getUniqueId() + " error: " + e);
            return false;
        }
    }
}
