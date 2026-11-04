package gg.auroramc.aurora.api.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserUnloadedEvent;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.internal.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.internal.expansions.leaderboard.storage.LeaderboardStorage;
import gg.auroramc.aurora.internal.user.migration.StorageMigrator;
import gg.auroramc.aurora.internal.user.storage.LatencyMeasure;
import gg.auroramc.aurora.internal.user.storage.YamlStorage;
import gg.auroramc.aurora.internal.user.storage.sql.MySqlStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class UserManager implements Listener {
    @Getter
    private volatile UserStorage storage;
    // A fixed set of locks shared by players, so a player's lock is never replaced while someone holds it. Nothing
    // waits for the storage while holding one, except loading an offline player the first time they are edited or
    // read, and purging a player.
    private final Object[] playerLocks = Stream.generate(Object::new).limit(1024).toArray();
    // Keep the writes of each player's data in order, so an older copy is never written over a newer one. Taken before
    // their player lock, never while holding it.
    private final Object[] saveLocks = Stream.generate(Object::new).limit(1024).toArray();
    // Every write holds it shared, saving everyone at once (shutting down, migrating) holds it alone. Taken first.
    private final ReentrantReadWriteLock writes = new ReentrantReadWriteLock();
    @Getter
    private final Set<Class<? extends UserDataHolder>> dataHolders = new HashSet<>();
    private ScheduledTask autoSaveTask;
    private ScheduledTask leaderboardUpdateTask;
    @Getter
    private final LatencyMeasure loadLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure saveLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure syncFlagLatencyMeasure = new LatencyMeasure();
    private StorageMigrator migrator = new StorageMigrator(this);
    // Players who left, whose offline copy (see offlineCache) isn't saved for their quit yet
    private final Set<UUID> unsavedQuits = ConcurrentHashMap.newKeySet();
    // How many times each player was purged, a load that read their data before a purge reads it again
    private final Map<UUID, Long> purges = new ConcurrentHashMap<>();
    private static final int LOAD_ATTEMPTS = 3;
    private static final long LOAD_RETRY_SECONDS = 2;
    private static final long MAX_QUIT_SAVE_RETRY_SECONDS = 30;

    // Stores actual online users data
    private final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder().build();
    // Edits for players who are online here, but whose data is still loading. Made as soon as it's loaded.
    private final Map<UUID, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();
    // Offline players in memory: loaded to be edited, kept until they join here (then their session takes over), so
    // they are only loaded once. Players who leave are kept here too until their quit is saved (longer if they are
    // edited meanwhile). Like online players, they are saved by the autosave and when shutting down.
    private final Cache<UUID, AuroraUser> offlineCache = CacheBuilder.newBuilder().build();
    // The offline copies above with changes the storage doesn't have yet
    private final Set<UUID> unsavedOfflineCopies = ConcurrentHashMap.newKeySet();
    private volatile boolean stopping = false;
    // Players online here, from their join until their quit event, by session (a quit only ends its own session). Not
    // Bukkit.getPlayer: on Folia a player who just left can still be found there for a moment from other threads, which
    // looked like a rejoin and skipped their save.
    private final Map<UUID, Player> online = new ConcurrentHashMap<>();

    private record PendingEdit(Predicate<AuroraUser> edit, CompletableFuture<Boolean> result) {
    }

    private record ToSave(AuroraUser user, SaveReason reason) {
    }

    public UserManager() {
        Bukkit.getPluginManager().registerEvents(this, Aurora.getInstance());
        Bukkit.getOnlinePlayers().forEach(player -> online.put(player.getUniqueId(), player));

        if (Aurora.getLibConfig().getStorageType().equals("mysql")) {
            storage = new MySqlStorage();
        } else {
            storage = new YamlStorage();
        }

        autoSaveTask();
        leaderboardUpdateTask();
    }

    public CompletableFuture<Boolean> attemptMigration(int threadCount) {
        migrator.markMigrating();
        finishEdits();

        return supplyAsync(() -> {
            // No write gets between saving everyone and copying the storage
            writes.writeLock().lock();
            try {
                stopTasksAndSaveAllData(false);
                if (!unsavedOfflineCopies.isEmpty()) {
                    // The migration copies the storage, edits it doesn't have yet would be lost
                    Aurora.logger().severe("Not migrating, the edits of " + unsavedOfflineCopies.size() + " offline players couldn't be saved. Check the errors above!");
                    autoSaveTask();
                    leaderboardUpdateTask();
                    migrator.markFinished();
                    return false;
                }
                cache.invalidateAll();
                offlineCache.invalidateAll();
                unsavedOfflineCopies.clear();
                unsavedQuits.clear();

                // Each on their own thread, on Folia a player can only be kicked from there
                for (var player : Bukkit.getOnlinePlayers()) {
                    player.getScheduler().run(Aurora.getInstance(),
                            (task) -> player.kick(Text.component(Aurora.getMsg(player).getKickedByDbMigration())), null);
                }

                UserStorage newStorage = this.storage;
                String newStorageId = storage instanceof MySqlStorage ? "yaml" : "mysql";
                boolean success = false;

                if (storage instanceof MySqlStorage mySqlStorage) {
                    newStorage = new YamlStorage();
                    success = migrator.migrateUserData(mySqlStorage, (YamlStorage) newStorage, threadCount);
                } else if (storage instanceof YamlStorage yamlStorage) {
                    newStorage = new MySqlStorage();
                    success = migrator.migrateUserData(yamlStorage, (MySqlStorage) newStorage, threadCount);
                }

                if (success) {
                    storage.dispose();
                    storage = newStorage;
                    Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class).setStorage(LeaderboardStorage.forUserStorage(newStorage));
                    // Only that line of the file changes
                    Aurora.getLibConfigFile().set("storage-type", newStorageId);
                }

                // With whichever storage is used now
                autoSaveTask();
                leaderboardUpdateTask();
                migrator.markFinished();

                return success;
            } finally {
                writes.writeLock().unlock();
            }
        });
    }

    /**
     * Saves the user's data now, on this thread. It waits for a save of the same player that's running, and while a
     * storage migration runs, so don't call it on the main thread. Don't call it from inside an edit (editUser,
     * editUserNow), that could deadlock.
     */
    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        return save(user.getUniqueId(), () -> new ToSave(user, reason), (saved, written) -> {
            if (written && reason == SaveReason.QUIT) unloaded(user);
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
        writes.readLock().lock();
        try {
            synchronized (getSaveLock(uuid)) {
                ToSave toSave;
                UserStorage.PendingSave pending;
                synchronized (getPlayerLock(uuid)) {
                    toSave = pick.get();
                    if (toSave == null) return true;
                    try {
                        pending = storage.prepareSave(toSave.user(), toSave.reason());
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
                synchronized (getPlayerLock(uuid)) {
                    done.accept(toSave, written);
                }
                return written;
            }
        } finally {
            writes.readLock().unlock();
        }
    }

    private void unloaded(AuroraUser user) {
        if (Bukkit.isStopping() || Aurora.isDisabling()) return;
        Aurora.logger().debug("Saved user " + user.getUniqueId() + " into storage");
        callEvents(user.getUniqueId(), new AuroraUserUnloadedEvent(user));
    }

    // Each player's events, called one after another, so a quick rejoin's loaded event can't overtake the unloaded
    // event of the session before (they may run on different threads on Folia)
    private final Map<UUID, CompletableFuture<Void>> eventChains = new ConcurrentHashMap<>();

    /**
     * Calls the events in order, after the player's earlier events, on the player's own thread if they are online
     * (listeners touch the player, on Folia only that thread may), otherwise (or if they leave before it runs) on the
     * global thread.
     */
    private void callEvents(UUID uuid, Event... events) {
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
                var player = online.containsKey(uuid) ? Bukkit.getPlayer(uuid) : null;
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

    private void autoSaveTask() {
        this.autoSaveTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            var values = cache.asMap().values();
            var toSave = values.stream().filter(u -> u.isLoaded() && u.isDirty()).toList();
            var offline = unsavedOfflineCopies.size();
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
        return save(uuid, () -> user.isLoaded() && cache.getIfPresent(uuid) == user ? new ToSave(user, SaveReason.AUTO_SAVE) : null,
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
        for (var uuid : List.copyOf(unsavedOfflineCopies)) {
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
            var copy = offlineCache.getIfPresent(uuid);
            // Joined in the meantime (their session has it now), or nothing changed since it was saved
            if (!unsavedOfflineCopies.remove(uuid) || copy == null) return null;
            return new ToSave(copy, unsavedQuits.contains(uuid) ? SaveReason.QUIT : reason);
        }, (saved, written) -> {
            // Joined meanwhile, their session goes on with it and saves it
            if (offlineCache.getIfPresent(uuid) != saved.user()) return;
            if (!written) {
                unsavedOfflineCopies.add(uuid);
                return;
            }
            if (saved.reason() != SaveReason.QUIT) return;
            unsavedQuits.remove(uuid);
            unloaded(saved.user());
            // Kept if it was edited after this was taken to be saved. Also if they joined again: their load may have read
            // the storage before this was written, it goes on with this copy instead.
            if (!unsavedOfflineCopies.contains(uuid) && !online.containsKey(uuid)) offlineCache.invalidate(uuid);
        });
    }

    private void leaderboardUpdateTask() {
        this.leaderboardUpdateTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
            var values = cache.asMap().values();
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
     * Waits for the edits that are being made right now (they hold their player's lock), and refuses the ones still
     * waiting for a player's data to load. New edits are refused once stopping or migrating is set.
     */
    private void finishEdits() {
        for (var lock : playerLocks) {
            synchronized (lock) {
                // Nothing, holding it once means whoever had it is done
            }
        }
        for (var uuid : List.copyOf(pendingEdits.keySet())) {
            synchronized (getPlayerLock(uuid)) {
                makePendingEdits(uuid, null);
            }
        }
    }

    // On Aurora's storage threads, see Aurora#getStorageExecutor
    // After shutting down, it fails instead of throwing at the caller
    private static CompletableFuture<Void> runAsync(Runnable task) {
        try {
            return CompletableFuture.runAsync(task, Aurora.getStorageExecutor());
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, Aurora.getStorageExecutor());
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private Object getPlayerLock(UUID playerId) {
        return playerLocks[Math.floorMod(playerId.hashCode(), playerLocks.length)];
    }

    private Object getSaveLock(UUID playerId) {
        return saveLocks[Math.floorMod(playerId.hashCode(), saveLocks.length)];
    }

    public <T extends UserDataHolder> void registerUserDataHolder(Class<T> clazz) {
        dataHolders.add(clazz);
    }

    /**
     * Changes a player's data, whether they are online on this server or offline.
     * If they are online here, the edit is made to their loaded data, which is saved like any other change they make
     * (if their data is still loading, the edit is made as soon as it's loaded). If they are offline, their data is
     * loaded once and kept in memory, edits are made to it, and it's saved like an online player's (by the autosave,
     * when shutting down, or by their session if they join).
     * <p>
     * This server must be the only one changing the player's data while they are offline. It doesn't work for a player
     * who is online on another server sharing the same database: that server would overwrite the edit. Offline players
     * stay loaded after an edit until they join here, so on a database shared by several servers, saving them can also
     * overwrite what the player did on another server in the meantime. With offline-player-cache turned off in the
     * config, offline players aren't changed at all and this returns false for them.
     *
     * @param uuid player's uuid
     * @param edit changes the user's data, and returns whether it did (like taking items only if they are there).
     *             When it returns false, it must not have changed anything. It must not call editUserNow, getUserNow or
     *             saveUserData itself (that could deadlock).
     * @return whether the edit was made. False if it refused, or their data couldn't be loaded, in which case nothing
     * was changed.
     */
    public CompletableFuture<Boolean> editUser(UUID uuid, Predicate<AuroraUser> edit) {
        var result = new CompletableFuture<Boolean>();
        runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                // Their data is being moved to another storage, or saved for shutting down
                if (stopping || migrator.isMigrating()) {
                    result.complete(false);
                    return;
                }
                var cached = cache.getIfPresent(uuid);
                if (cached != null && cached.isLoaded()) {
                    result.complete(test(edit, cached));
                    return;
                }
                if (online.containsKey(uuid)) {
                    pendingEdits.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new PendingEdit(edit, result));
                    return;
                }
                result.complete(editOffline(uuid, List.of(edit)).getFirst());
            }
        }).exceptionally(e -> {
            Aurora.logger().severe("Failed to edit the data of player: " + uuid + " error: " + e);
            result.complete(false);
            return null;
        });
        // Callbacks run after the player's lock is released, so one waiting for another edit can't deadlock
        return result.thenApplyAsync(Function.identity());
    }

    private static boolean test(Predicate<AuroraUser> edit, AuroraUser user) {
        try {
            return edit.test(user);
        } catch (Exception e) {
            Aurora.logger().severe("Failed to edit the data of player: " + user.getUniqueId() + " error: " + e);
            return false;
        }
    }

    /**
     * Changes a player's data right away on the calling thread, for APIs that have to answer at once (like an economy
     * behind Vault). Works like {@link #editUser}, except for these: an offline player who isn't in memory yet is
     * loaded on this thread (only the first time, they stay in memory after that), and a player who is online here
     * but whose data is still loading can't be changed, false is returned for them.
     *
     * @param uuid player's uuid
     * @param edit changes the user's data, and returns whether it did. When it returns false, it must not have changed
     *             anything. It must not call editUserNow, getUserNow or saveUserData itself (that could deadlock), make
     *             changes to more players one after another instead.
     * @return whether the edit was made
     */
    public boolean editUserNow(UUID uuid, Predicate<AuroraUser> edit) {
        synchronized (getPlayerLock(uuid)) {
            if (stopping || migrator.isMigrating()) return false;
            var cached = cache.getIfPresent(uuid);
            if (cached != null && cached.isLoaded()) return test(edit, cached);
            if (online.containsKey(uuid)) return false;
            return editOffline(uuid, List.of(edit)).getFirst();
        }
    }

    /**
     * Gets a player's data right away on the calling thread, for reading it, like {@link #editUserNow}: an offline
     * player who isn't in memory yet is loaded on this thread, and stays in memory after that.
     * Don't change what this returns, use editUser or editUserNow for that.
     *
     * @param uuid player's uuid
     * @return their data, or null if it isn't available (it's still loading, couldn't be loaded, or offline-player-cache
     * is turned off and they are offline)
     */
    public AuroraUser getUserNow(UUID uuid) {
        synchronized (getPlayerLock(uuid)) {
            if (stopping || migrator.isMigrating()) return null;
            var cached = cache.getIfPresent(uuid);
            if (cached != null && cached.isLoaded()) return cached;
            if (online.containsKey(uuid)) return null;
            return getOfflineCopy(uuid);
        }
    }

    // Call with the player's lock held. Null if offline-player-cache is turned off, or their data couldn't be loaded.
    private AuroraUser getOfflineCopy(UUID uuid) {
        // Turned off for servers sharing a database, offline players are left alone then
        if (!Aurora.getLibConfig().getOfflinePlayerCache()) return null;
        var user = offlineCache.getIfPresent(uuid);
        if (user == null) {
            user = storage.loadUser(uuid, dataHolders);
            if (!user.isLoaded()) return null;
            offlineCache.put(uuid, user);
        }
        return user;
    }

    /**
     * Edits an offline player's copy, loading it first if needed. Call with their lock held.
     *
     * @return the result of each edit, all false if their data couldn't be loaded
     */
    private List<Boolean> editOffline(UUID uuid, List<Predicate<AuroraUser>> edits) {
        var edited = getOfflineCopy(uuid);
        if (edited == null) return edits.stream().map(edit -> false).toList();
        var results = edits.stream().map(edit -> test(edit, edited)).toList();
        // Saved later, like an online player
        if (results.contains(true)) unsavedOfflineCopies.add(uuid);
        return results;
    }

    // Call with the player's save and player lock held (so it isn't being written right now). An offline copy kept for
    // a rejoin (see saveOfflineCopy) whose session never took it over is dropped here once it's saved, it could be
    // outdated when they join next time (by playing on another server sharing the database).
    private void dropIfSaved(UUID uuid) {
        if (online.containsKey(uuid) || unsavedOfflineCopies.contains(uuid) || unsavedQuits.contains(uuid)) return;
        offlineCache.invalidate(uuid);
    }

    // Like dropIfSaved, taking the locks. Call without holding any.
    private void dropIfSavedLocking(UUID uuid) {
        writes.readLock().lock();
        try {
            synchronized (getSaveLock(uuid)) {
                synchronized (getPlayerLock(uuid)) {
                    dropIfSaved(uuid);
                }
            }
        } finally {
            writes.readLock().unlock();
        }
    }

    // Call with the player's lock held
    private void makePendingEdits(UUID uuid, AuroraUser user) {
        var edits = pendingEdits.remove(uuid);
        if (edits == null) return;
        for (var pending : edits) {
            pending.result().complete(user != null && test(pending.edit(), user));
        }
    }

    // Call with the player's lock held. They left before their data loaded, so their offline copy is edited.
    private void makePendingEditsOffline(UUID uuid) {
        if (stopping || migrator.isMigrating()) {
            makePendingEdits(uuid, null);
            return;
        }
        var edits = pendingEdits.remove(uuid);
        if (edits == null) return;
        List<Boolean> results;
        try {
            results = editOffline(uuid, edits.stream().map(PendingEdit::edit).toList());
        } catch (Throwable e) {
            Aurora.logger().severe("Failed to edit the data of player: " + uuid + " error: " + e);
            results = edits.stream().map(edit -> false).toList();
        }
        for (int i = 0; i < edits.size(); i++) {
            edits.get(i).result().complete(results.get(i));
        }
    }

    /**
     * Checks if the user is in the cache
     * Use this if you are working with offline players and load data inside a CompletableFuture!
     *
     * @param uuid player's uuid
     * @return whether the players data is loaded into the cache or not
     */
    public boolean isUserCached(UUID uuid) {
        return cache.getIfPresent(uuid) != null;
    }

    /**
     * Gets the user from cache
     *
     * @param player player to get user data for
     * @return loaded AuroraUser
     */
    public AuroraUser getUser(OfflinePlayer player) {
        return getUser(player.getUniqueId());
    }

    /**
     * Gets the user from cache
     *
     * @param player player to get user data for
     * @return loaded AuroraUser
     */
    public AuroraUser getUser(Player player) {
        return getUser(player.getUniqueId());
    }

    /**
     * Gets the user from cache.
     *
     * @param uuid player's uuid to load data for
     * @return loaded AuroraUser
     */
    public AuroraUser getUser(UUID uuid) {
        var user = cache.getIfPresent(uuid);
        if (user != null) return user;

        // Try to compute atomically, preventing race
        // In very rare scenarios it was possible that loadUser was put the actual
        // loaded user into cache after the getIfPresent check here.
        // This computeIfAbsent instead if put will hopefully prevent that
        return cache.asMap().computeIfAbsent(uuid, (key) -> {
            var fakeUser = new AuroraUser(key, false);
            fakeUser.initData(null, dataHolders);
            Aurora.logger().debug("Created fake user " + key + " in cache");
            return fakeUser;
        });
    }

    /**
     * Loads user into the cache.
     *
     * @param uuid player's uuid to load data for
     */
    public void loadUser(UUID uuid) {
        loadUser(uuid, 1);
    }

    private void loadUser(UUID uuid, int attempt) {
        runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                var cached = cache.getIfPresent(uuid);
                // Joined again before their quit was handled, their session just goes on
                if (cached != null && cached.isLoaded()) {
                    makePendingEdits(uuid, cached);
                    callEvents(uuid, new AuroraUserLoadedEvent(cached));
                    return;
                }
            }

            // Not holding their lock while it's read
            var purgesBefore = purges.getOrDefault(uuid, 0L);
            storage.loadUser(uuid, dataHolders, loaded -> {
                // Not waiting for a save of everyone (this may hold a database connection the migration needs)
                if (stopping || migrator.isMigrating()) {
                    synchronized (getPlayerLock(uuid)) {
                        makePendingEdits(uuid, null);
                    }
                    return;
                }
                // After a write of theirs that's running (so a quit save finishes before their session starts), and
                // not while everyone is being saved
                writes.readLock().lock();
                try {
                    synchronized (getSaveLock(uuid)) {
                        synchronized (getPlayerLock(uuid)) {
                            onLoaded(uuid, attempt, loaded, purgesBefore);
                        }
                    }
                } finally {
                    writes.readLock().unlock();
                }
            });
        });
    }

    // Call with the player's save and player lock held
    private void onLoaded(UUID uuid, int attempt, AuroraUser loaded, long purgesBefore) {
        if (stopping || migrator.isMigrating()) {
            makePendingEdits(uuid, null);
            return;
        }
        if (!online.containsKey(uuid)) {
            makePendingEditsOffline(uuid);
            dropIfSaved(uuid);
            return;
        }
        // Purged after this was read, it has to be read again
        if (purges.getOrDefault(uuid, 0L) != purgesBefore) {
            loadUser(uuid, attempt);
            return;
        }

        var maybeUser = cache.getIfPresent(uuid);
        // An earlier join's load finished first, their session goes on with what's in memory
        if (maybeUser != null && maybeUser.isLoaded()) {
            makePendingEdits(uuid, maybeUser);
            callEvents(uuid, new AuroraUserLoadedEvent(maybeUser));
            return;
        }

        // Kept since they left, or edited while offline (maybe after this load read their data), that
        // copy is the newest. From now on their session is the only copy, and saves it.
        // It may have changes that aren't saved yet, so it's used even if offline-player-cache is off.
        var user = offlineCache.getIfPresent(uuid);
        offlineCache.invalidate(uuid);
        unsavedOfflineCopies.remove(uuid);
        unsavedQuits.remove(uuid);
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
            cache.put(uuid, user);
            Aurora.logger().debug("Loaded user " + uuid + " into cache");
        }

        makePendingEdits(uuid, user);
        callEvents(uuid, new AuroraUserLoadedEvent(user));
    }

    private void onLoadFailed(UUID uuid, int attempt) {
        if (attempt < LOAD_ATTEMPTS) {
            Aurora.logger().warning("Failed to load the data of " + uuid + " (attempt " + attempt + "/" + LOAD_ATTEMPTS + "), trying again in " + LOAD_RETRY_SECONDS + " seconds");
            Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
                if (online.containsKey(uuid)) loadUser(uuid, attempt + 1);
            }, LOAD_RETRY_SECONDS, TimeUnit.SECONDS);
            return;
        }

        Aurora.logger().severe("Failed to load the data of " + uuid + " " + LOAD_ATTEMPTS + " times, kicking them so nothing they do is lost. Check the errors above!");
        makePendingEdits(uuid, null);
        var player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getScheduler().run(Aurora.getInstance(),
                    (task) -> player.kick(Text.component(Aurora.getMsg(player).getKickedByFailedLoad())), null);
        }
    }

    /**
     * Loads user directly from storage. For an offline player loaded to be edited (see {@link #editUser}), it's that
     * copy instead, since the storage may not have their latest edits yet. Don't change it, use editUser for that.
     *
     * @param uuid player's uuid to load data for
     */
    public CompletableFuture<AuroraUser> loadUserFromStorage(UUID uuid) {
        return supplyAsync(() -> {
            var edited = offlineCache.getIfPresent(uuid);
            return edited != null ? edited : storage.loadUser(uuid, dataHolders);
        // Callbacks run off the storage threads, one waiting for another storage task can't starve them
        }).thenApplyAsync(Function.identity());
    }

    public void purgeUserData(UUID uuid) {
        runAsync(() -> {
            // After a write of theirs that's running, so it can't bring the data back. The storage is purged under their
            // player lock, so an edit can't load the old data meanwhile.
            writes.readLock().lock();
            try {
                synchronized (getSaveLock(uuid)) {
                    synchronized (getPlayerLock(uuid)) {
                        purge(uuid);
                    }
                }
            } finally {
                writes.readLock().unlock();
            }
        });
    }

    // Call with the player's save and player lock held
    private void purge(UUID uuid) {
        purges.merge(uuid, 1L, Long::sum);
        offlineCache.invalidate(uuid);
        unsavedOfflineCopies.remove(uuid);
        unsavedQuits.remove(uuid);
        var oldUser = cache.getIfPresent(uuid);
        if (oldUser != null && oldUser.isLoaded()) {
            // So nothing still holding the old object saves it
            oldUser.setLoaded(false);
            cache.invalidate(uuid);
            var newUser = new AuroraUser(uuid, true);
            newUser.initData(new YamlConfiguration(), dataHolders);
            cache.put(uuid, newUser);
            storage.purgeUser(uuid);
            callEvents(uuid, new AuroraUserUnloadedEvent(oldUser), new AuroraUserLoadedEvent(newUser));
        } else {
            if (oldUser != null) cache.invalidate(uuid);
            storage.purgeUser(uuid);
        }
    }

    /**
     * Stop cacheTTL heartbeat task for online players and autosave task.
     * Saves all cached data sync.
     * Should be used only in onDisable()
     */
    public void stopTasksAndSaveAllData(boolean dispose) {
        // Edits after this save would be lost
        if (dispose) {
            stopping = true;
            finishEdits();
        }
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (leaderboardUpdateTask != null) leaderboardUpdateTask.cancel();
        // After the writes that are running, and none start until everyone is saved
        writes.writeLock().lock();
        try {
            var users = cache.asMap().values().stream().filter(AuroraUser::isLoaded).toList();
            if (storage.bulkSaveUsers(users, SaveReason.QUIT) == 0 && !users.isEmpty()) {
                // One player's data that can't be saved shouldn't stop everyone else's
                Aurora.logger().severe("Failed to save everyone's data at once while shutting down, saving them one by one");
                for (var user : users) {
                    if (!storage.saveUser(user, SaveReason.QUIT)) {
                        Aurora.logger().severe("Failed to save the data of " + user.getUniqueId() + " while shutting down, changes since their last save are lost");
                    }
                }
            }
            // After the sessions, so one that just became an offline copy (its player left meanwhile) is saved either way
            saveOfflineEdits(SaveReason.CUSTOM);
        } finally {
            writes.writeLock().unlock();
        }
        var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        lbm.bulkUpdateUsers(cache.asMap().values().stream().filter(AuroraUser::isLoaded).collect(HashMap::new, (m, u) -> m.put(u.getUniqueId(), u.getDirtyLeaderboards().keySet()), HashMap::putAll)).join();
        if (dispose) storage.dispose();
    }

    public void invalidate(Player player) {
        var user = cache.getIfPresent(player.getUniqueId());
        if (user == null) {
            // Left before their data loaded, edits waiting for it are made to their saved data
            var uuid = player.getUniqueId();
            runAsync(() -> {
                synchronized (getPlayerLock(uuid)) {
                    if (!online.containsKey(uuid)) makePendingEditsOffline(uuid);
                }
                dropIfSavedLocking(uuid);
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
        synchronized (getPlayerLock(uuid)) {
            // Rejoined before we got here, their session just goes on
            if (online.containsKey(uuid) || cache.getIfPresent(uuid) != user) return;
            cache.asMap().remove(uuid, user);
            Aurora.logger().debug("Removed user " + uuid + " from cache");
            if (!user.isLoaded()) {
                // Their data never loaded, there is nothing to save. Saving it would overwrite their real data.
                makePendingEditsOffline(uuid);
                Bukkit.getAsyncScheduler().runNow(Aurora.getInstance(), (task) -> dropIfSavedLocking(uuid));
                return;
            }
            // Their offline copy from now on: edits are made to it (if offline-player-cache is on), a rejoin goes on
            // with it, and it's kept until it's saved
            offlineCache.put(uuid, user);
            unsavedOfflineCopies.add(uuid);
            unsavedQuits.add(uuid);
        }
        saveQuit(uuid, 1);
    }

    private void saveQuit(UUID uuid, int attempt) {
        if (saveOfflineCopy(uuid, SaveReason.QUIT) || !unsavedQuits.contains(uuid) || stopping) return;
        var delay = Math.min(MAX_QUIT_SAVE_RETRY_SECONDS, 1L << Math.min(attempt, 5));
        Aurora.logger().severe("Failed to save the data of " + uuid + " after they left (attempt " + attempt + "), it's kept in memory and saving it again in " + delay + " seconds");
        Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> saveQuit(uuid, attempt + 1), delay, TimeUnit.SECONDS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUser(e.getPlayer().getUniqueId());
    }

    // Before every other join listener, so they all see the player as online here (their edits wait for the load)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEarly(PlayerJoinEvent e) {
        // Kicked by an earlier listener already, their quit came before this
        if (!e.getPlayer().isConnected()) return;
        online.put(e.getPlayer().getUniqueId(), e.getPlayer());
    }

    /**
     * Whether the player is online on this server, as far as their data is concerned: from the start of their join
     * until their quit.
     */
    public boolean isOnline(UUID uuid) {
        return online.containsKey(uuid);
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent e) {
        if (migrator.isMigrating()) {
            e.kickMessage(Text.component("&cUnder maintenance, please try again later."));
            e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        online.remove(e.getPlayer().getUniqueId(), e.getPlayer());
        if (migrator.isMigrating()) return;
        Bukkit.getGlobalRegionScheduler().runDelayed(Aurora.getInstance(),
                (task) -> invalidate(e.getPlayer()), 1);
    }
}
