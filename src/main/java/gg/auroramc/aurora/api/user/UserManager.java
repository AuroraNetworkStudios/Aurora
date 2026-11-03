package gg.auroramc.aurora.api.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserUnloadedEvent;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.migration.StorageMigrator;
import gg.auroramc.aurora.api.user.storage.LatencyMeasure;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.api.user.storage.YamlStorage;
import gg.auroramc.aurora.api.user.storage.sql.MySqlStorage;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.leaderboard.storage.sqlite.SqliteLeaderboardStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class UserManager implements Listener {
    @Getter
    private volatile UserStorage storage;
    // A fixed set of locks shared by players, so a player's lock is never replaced while someone holds it
    private final Object[] playerLocks = Stream.generate(Object::new).limit(1024).toArray();
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
    // Players who left, but whose data couldn't be saved yet. It stays in memory and the save is tried again.
    private final Set<UUID> unsavedQuits = ConcurrentHashMap.newKeySet();
    private static final int LOAD_ATTEMPTS = 3;
    private static final long LOAD_RETRY_SECONDS = 2;
    private static final long MAX_QUIT_SAVE_RETRY_SECONDS = 30;

    // Stores actual online users data
    private final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder().build();
    // Edits for players who are online here, but whose data is still loading. Made as soon as it's loaded.
    private final Map<UUID, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();
    // Offline players loaded to be edited, kept until they join here (then their session takes over), so they are only
    // loaded once. Every edit is saved right away, so this is always the same as their saved data, as long as only this
    // server changes it.
    private final Cache<UUID, AuroraUser> offlineCache = CacheBuilder.newBuilder().build();
    private volatile boolean stopping = false;

    private record PendingEdit(Predicate<AuroraUser> edit, CompletableFuture<Boolean> result) {
    }


    public UserManager() {
        Bukkit.getPluginManager().registerEvents(this, Aurora.getInstance());

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

        return CompletableFuture.supplyAsync(() -> {
            stopTasksAndSaveAllData(false);
            cache.invalidateAll();
            offlineCache.invalidateAll();
            unsavedQuits.clear();

            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
                Bukkit.getOnlinePlayers().forEach(player -> player.kick(
                        Text.component(Aurora.getMsg(player).getKickedByDbMigration())
                ));
            });

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
                if (newStorage instanceof MySqlStorage mySqlStorage) {
                    Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class).setStorage(mySqlStorage);
                } else {
                    Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class).setStorage(new SqliteLeaderboardStorage());
                }
                Aurora.getLibConfig().setStorageType(newStorageId);
                Aurora.getLibConfig().saveChanges();
                autoSaveTask();
                leaderboardUpdateTask();
            }

            migrator.markFinished();

            return success;
        });
    }

    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        synchronized (getPlayerLock(user.getUniqueId())) {
            var result = storage.saveUser(user, reason);
            if (result && reason == SaveReason.QUIT && !Bukkit.isStopping() && !Aurora.isDisabling()) {
                Aurora.logger().debug("Saved user " + user.getUniqueId() + " into storage");
                Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                        (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserUnloadedEvent(user)));
            }
            return result;
        }
    }

    private void autoSaveTask() {
        this.autoSaveTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            var values = cache.asMap().values();
            var toSave = values.stream().filter(u -> u.isLoaded() && u.isDirty()).toList();
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

    // Each player on their own and under their lock, only while it's still their session here. If they left since
    // the autosave started, their quit saved them, and an edit saved since then would be overwritten by this older copy.
    private boolean autoSave(AuroraUser user) {
        synchronized (getPlayerLock(user.getUniqueId())) {
            if (!user.isLoaded() || cache.getIfPresent(user.getUniqueId()) != user) return false;
            return storage.saveUser(user, SaveReason.AUTO_SAVE);
        }
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

    private Object getPlayerLock(UUID playerId) {
        return playerLocks[Math.floorMod(playerId.hashCode(), playerLocks.length)];
    }

    public <T extends UserDataHolder> void registerUserDataHolder(Class<T> clazz) {
        dataHolders.add(clazz);
    }

    /**
     * Changes a player's data, whether they are online on this server or offline.
     * If they are online here, the edit is made to their loaded data, which is saved like any other change they make
     * (if their data is still loading, the edit is made as soon as it's loaded). If they are offline, their data is
     * loaded, edited and saved right away.
     * <p>
     * This server must be the only one changing the player's data while they are offline. It doesn't work for a player
     * who is online on another server sharing the same database: that server would overwrite the edit. Offline players
     * stay loaded after an edit until they join here, so on a database shared by several servers, an edit can also
     * overwrite what the player did on another server in the meantime. With offline-player-cache turned off in the
     * config, offline players aren't changed at all and this returns false for them.
     *
     * @param uuid player's uuid
     * @param edit changes the user's data, and returns whether it did (like taking items only if they are there)
     * @return whether the edit was made, and saved if they are offline. False if it refused, or their data couldn't
     * be loaded or saved, in which case nothing was changed.
     */
    public CompletableFuture<Boolean> editUser(UUID uuid, Predicate<AuroraUser> edit) {
        var result = new CompletableFuture<Boolean>();
        CompletableFuture.runAsync(() -> {
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
                // They left, but their data in memory couldn't be saved yet. Edits wait until it is.
                if (unsavedQuits.contains(uuid)) {
                    result.complete(false);
                    return;
                }
                if (Bukkit.getPlayer(uuid) != null) {
                    pendingEdits.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new PendingEdit(edit, result));
                    return;
                }
                result.complete(editOffline(uuid, List.of(edit)).getFirst());
            }
        }).exceptionally(e -> {
            Aurora.logger().severe("Failed to edit the data of player: " + uuid + " error: " + e);
            offlineCache.invalidate(uuid);
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
     * Loads, edits and saves an offline player. Call with their lock held.
     *
     * @return the result of each edit, all false if their data couldn't be loaded or saved
     */
    private List<Boolean> editOffline(UUID uuid, List<Predicate<AuroraUser>> edits) {
        var refused = edits.stream().map(edit -> false).toList();
        // Turned off for servers sharing a database, offline players are left alone then
        if (!Aurora.getLibConfig().getOfflinePlayerCache()) return refused;
        var user = offlineCache.getIfPresent(uuid);
        if (user == null) {
            user = storage.loadUser(uuid, dataHolders);
            if (!user.isLoaded()) return refused;
            offlineCache.put(uuid, user);
        }
        var edited = user;
        var results = edits.stream().map(edit -> test(edit, edited)).toList();
        if (!results.contains(true)) {
            // A refused edit may have changed some of it, that mustn't be saved by the next edit
            offlineCache.invalidate(uuid);
            return results;
        }
        if (!storage.saveUser(user, SaveReason.CUSTOM)) {
            // The copy in memory has the edits, the storage doesn't, so it's loaded again next time
            offlineCache.invalidate(uuid);
            return refused;
        }
        return results;
    }

    // Call with the player's lock held
    private void makePendingEdits(UUID uuid, AuroraUser user) {
        var edits = pendingEdits.remove(uuid);
        if (edits == null) return;
        for (var pending : edits) {
            pending.result().complete(user != null && test(pending.edit(), user));
        }
    }

    // Call with the player's lock held. They left before their data loaded, so it's edited in the storage.
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
            // It may have been changed before it failed, that mustn't be saved by the next edit
            offlineCache.invalidate(uuid);
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
        CompletableFuture.runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                var cached = cache.getIfPresent(uuid);
                // Joined again before their quit was saved (or while it can't be saved), what we have in memory
                // is newer than the storage
                if (cached != null && (cached.isLoaded() || unsavedQuits.contains(uuid))) {
                    offlineCache.invalidate(uuid);
                    unsavedQuits.remove(uuid);
                    cached.setLoaded(true);
                    makePendingEdits(uuid, cached);
                    Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                            (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(cached)));
                    return;
                }

                storage.loadUser(uuid, dataHolders, loaded -> {
                    synchronized (getPlayerLock(uuid)) {
                        if (Bukkit.getPlayer(uuid) == null) {
                            makePendingEditsOffline(uuid);
                            return;
                        }

                        var maybeUser = cache.getIfPresent(uuid);
                        // An earlier join's load finished first (or their last quit isn't saved yet), their session
                        // goes on with what's in memory
                        if (maybeUser != null && (maybeUser.isLoaded() || unsavedQuits.contains(uuid))) {
                            offlineCache.invalidate(uuid);
                            unsavedQuits.remove(uuid);
                            maybeUser.setLoaded(true);
                            makePendingEdits(uuid, maybeUser);
                            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                    (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(maybeUser)));
                            return;
                        }

                        // Edited while offline (maybe after this load read their data), that copy is the newest.
                        // From now on their session is the only copy.
                        // (Only while offline edits are on, a copy from before they were turned off may be outdated.)
                        var user = Aurora.getLibConfig().getOfflinePlayerCache() ? offlineCache.getIfPresent(uuid) : null;
                        offlineCache.invalidate(uuid);
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
                        var loadedUser = user;
                        Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(loadedUser)));
                    }
                });
            }
        });
    }

    private void onLoadFailed(UUID uuid, int attempt) {
        if (attempt < LOAD_ATTEMPTS) {
            Aurora.logger().warning("Failed to load the data of " + uuid + " (attempt " + attempt + "/" + LOAD_ATTEMPTS + "), trying again in " + LOAD_RETRY_SECONDS + " seconds");
            Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
                if (Bukkit.getPlayer(uuid) != null) loadUser(uuid, attempt + 1);
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
     * Loads user directly from storage.
     *
     * @param uuid player's uuid to load data for
     */
    public CompletableFuture<AuroraUser> loadUserFromStorage(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> storage.loadUser(uuid, dataHolders));
    }

    public void purgeUserData(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                offlineCache.invalidate(uuid);
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
                    Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
                        Bukkit.getPluginManager().callEvent(new AuroraUserUnloadedEvent(oldUser));
                        Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(newUser));
                    });
                } else {
                    if (oldUser != null) cache.invalidate(uuid);
                    storage.purgeUser(uuid);
                }
            }
        });
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
        var users = cache.asMap().values().stream().filter(u -> u.isLoaded() || unsavedQuits.contains(u.getUniqueId())).toList();
        if (storage.bulkSaveUsers(users, SaveReason.QUIT) == 0 && !users.isEmpty()) {
            // One player's data that can't be saved shouldn't stop everyone else's
            Aurora.logger().severe("Failed to save everyone's data at once while shutting down, saving them one by one");
            for (var user : users) {
                if (!storage.saveUser(user, SaveReason.QUIT)) {
                    Aurora.logger().severe("Failed to save the data of " + user.getUniqueId() + " while shutting down, changes since their last save are lost");
                }
            }
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
            CompletableFuture.runAsync(() -> {
                synchronized (getPlayerLock(uuid)) {
                    if (Bukkit.getPlayer(uuid) == null) makePendingEditsOffline(uuid);
                }
            });
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                if (user.isLoaded()) {
                    var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
                    lbm.updateUser(user, Collections.emptyList()).join();
                }
            } finally {
                saveOnQuit(user, 1);
            }
        });
    }

    private void saveOnQuit(AuroraUser user, int attempt) {
        var uuid = user.getUniqueId();
        synchronized (getPlayerLock(uuid)) {
            // Rejoined before we got here, their session just goes on
            if (user.getPlayer() != null || cache.getIfPresent(uuid) != user) return;
            if (!user.isLoaded() && !unsavedQuits.contains(uuid)) {
                // Their data never loaded, there is nothing to save. Saving it would overwrite their real data.
                cache.asMap().remove(uuid, user);
                makePendingEditsOffline(uuid);
                return;
            }
            // Edits from now on load their data from the storage, after this save
            user.setLoaded(false);
            boolean saved;
            try {
                saved = saveUserData(user, SaveReason.QUIT);
            } catch (Exception e) {
                Aurora.logger().severe("Failed to save user data for player: " + uuid + " error: " + e);
                saved = false;
            }
            if (saved) {
                unsavedQuits.remove(uuid);
                cache.asMap().remove(uuid, user);
                Aurora.logger().debug("Removed user " + uuid + " from cache");
                return;
            }
            unsavedQuits.add(uuid);
        }

        var delay = Math.min(MAX_QUIT_SAVE_RETRY_SECONDS, 1L << Math.min(attempt, 5));
        Aurora.logger().severe("Failed to save the data of " + uuid + " after they left (attempt " + attempt + "), it's kept in memory and saving it again in " + delay + " seconds");
        Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> saveOnQuit(user, attempt + 1), delay, TimeUnit.SECONDS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUser(e.getPlayer().getUniqueId());
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
        if (migrator.isMigrating()) return;
        Bukkit.getGlobalRegionScheduler().runDelayed(Aurora.getInstance(),
                (task) -> invalidate(e.getPlayer()), 1);
    }
}
