package gg.auroramc.aurora.api.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.events.user.AuroraUserUnloadedEvent;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.change.UserChange;
import gg.auroramc.aurora.api.user.migration.StorageMigrator;
import gg.auroramc.aurora.api.user.storage.LatencyMeasure;
import gg.auroramc.aurora.api.user.storage.PendingChange;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.api.user.storage.YamlStorage;
import gg.auroramc.aurora.api.user.storage.sql.MySqlStorage;
import gg.auroramc.aurora.api.util.NamespacedId;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import gg.auroramc.aurora.expansions.leaderboard.storage.sqlite.SqliteLeaderboardStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private ScheduledTask changeTask;
    private ScheduledTask requestTask;
    private final Map<String, Function<ConfigurationSection, UserChange>> changeTypes = new ConcurrentHashMap<>();
    // Requests made on this server that aren't answered yet, by change id
    private final Map<Long, Request> requests = new ConcurrentHashMap<>();
    private static final long REQUEST_TIMEOUT_MS = 60_000;
    // Null if only this server uses the storage
    private ServerNotifier notifier;
    // Changes and quits are saved on these threads, so the ones still running can be waited for on shutdown
    private final ExecutorService changeExecutor = Executors.newFixedThreadPool(4, r -> new Thread(r, "Aurora user changes"));
    @Getter
    private final LatencyMeasure loadLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure saveLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure syncFlagLatencyMeasure = new LatencyMeasure();
    private StorageMigrator migrator = new StorageMigrator(this);

    // Stores actual online users data
    private final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder().build();


    public UserManager() {
        Bukkit.getPluginManager().registerEvents(this, Aurora.getInstance());

        if (Aurora.getLibConfig().getStorageType().equals("mysql")) {
            storage = new MySqlStorage();
        } else {
            storage = new YamlStorage();
        }

        autoSaveTask();
        leaderboardUpdateTask();
        if (isCrossServer()) {
            changeTask();
            requestTask();
            var config = Aurora.getInstance().getConfig();
            if (config.getBoolean("mysql.redis.enabled", false)) {
                notifier = new RedisNotifier(config.getConfigurationSection("mysql.redis"), this::onChangesNotified, this::onAnswersNotified);
            } else if (Bukkit.getServer().getServerConfig().isProxyEnabled()) {
                // Only behind a proxy, otherwise players could send us these plugin messages themselves
                notifier = new ProxyNotifier(this::onChangesNotified, this::onAnswersNotified);
            } else {
                Aurora.logger().info("Not behind a proxy and Redis isn't enabled, so other servers apply changes with their next check (within a second)");
            }
        } else {
            // Changes left over if something went wrong last time, once the plugins registered their change types
            Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(),
                    (task) -> storage.getUsersWithChanges().forEach(this::applyPendingChanges), 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Whether other servers use the same storage, so players' data has to be kept in sync between them.
     */
    private boolean isCrossServer() {
        return storage instanceof MySqlStorage mySqlStorage && mySqlStorage.isCrossServerSync();
    }

    public CompletableFuture<Boolean> attemptMigration(int threadCount) {
        migrator.markMigrating();

        return CompletableFuture.supplyAsync(() -> {
            stopTasksAndSaveAllData(false);
            cache.invalidateAll();

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
                // Notifying other servers starts after a restart
                if (isCrossServer()) {
                    changeTask();
                    requestTask();
                }
            }

            migrator.markFinished();

            return success;
        });
    }

    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        synchronized (getPlayerLock(user.getUniqueId())) {
            var result = storage.saveUser(user, reason);
            if (reason == SaveReason.QUIT && !Bukkit.isStopping() && !Aurora.isDisabling()) {
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
                var successCount = storage.bulkSaveUsers(toSave, SaveReason.AUTO_SAVE);
                var all = toSave.size();
                if (!Bukkit.getOnlinePlayers().isEmpty() && !values.isEmpty()) {
                    Aurora.logger().info("Auto background saved user data for " + successCount + "/" + all + " online players");
                }
            }
            autoSaveTask();
        }, Aurora.getLibConfig().getUserAutoSaveInMinutes(), TimeUnit.MINUTES);
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

    private void changeTask() {
        this.changeTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            for (var uuid : storage.getUsersWithChanges()) {
                applyPendingChanges(uuid);
            }
            changeTask();
        }, 1, TimeUnit.SECONDS);
    }

    private void requestTask() {
        this.requestTask = Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            if (!requests.isEmpty()) {
                answerRequests(false);
            }
            requestTask();
        }, 100, TimeUnit.MILLISECONDS);
    }

    private Object getPlayerLock(UUID playerId) {
        return playerLocks[Math.floorMod(playerId.hashCode(), playerLocks.length)];
    }

    public <T extends UserDataHolder> void registerUserDataHolder(Class<T> clazz) {
        dataHolders.add(clazz);
    }

    /**
     * Registers a change type, so stored changes of this type can be read back.
     *
     * @param type   the type returned by {@link UserChange#getType()}
     * @param reader creates the change from the data saved by {@link UserChange#serializeInto}
     */
    public void registerChangeType(NamespacedId type, Function<ConfigurationSection, UserChange> reader) {
        changeTypes.put(type.toString(), reader);
    }

    /**
     * Changes a player's data, whether they are online on this server, on another server or offline.
     * If their data is loaded on this server, the change is applied right away. Otherwise, it is stored and
     * applied within a second by the server that has their data loaded, or by any server if they are offline.
     *
     * @param uuid   player's uuid
     * @param change the change to apply
     */
    public void applyChange(UUID uuid, UserChange change) {
        CompletableFuture.runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                var user = cache.getIfPresent(uuid);
                if (user != null && user.isLoaded()) {
                    change.apply(user);
                    return;
                }

                storage.addChange(uuid, change.getType().toString(), serialize(change), false);
                if (isCrossServer()) {
                    notifyOtherServers(uuid);
                } else {
                    // Nobody else uses the storage, so it can be applied right away
                    applyPendingChanges(uuid);
                }
            }
        }, changeExecutor);
    }

    /**
     * Like {@link #applyChange}, but tells whether the change was applied (see {@link UserChange#apply}).
     * Use it for the part of an action that can fail, like taking items or money, and do the rest
     * (the parts that can't fail, like giving them to someone) with applyChange once this was applied.
     * <p>
     * Answered right away if their data is loaded on this server or if they are offline (their data is loaded,
     * changed and saved), within about a second if they are online on another server.
     *
     * @param uuid   player's uuid
     * @param change the change to apply
     * @return whether it was applied, false if it was refused or their data couldn't be reached for a minute
     */
    public CompletableFuture<Boolean> requestChange(UUID uuid, UserChange change) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                var user = cache.getIfPresent(uuid);
                if (user != null && user.isLoaded()) {
                    return CompletableFuture.completedFuture(change.apply(user));
                }

                var id = storage.addChange(uuid, change.getType().toString(), serialize(change), true);
                if (id < 0) return CompletableFuture.completedFuture(false);

                var request = new Request(uuid, new CompletableFuture<>(), System.currentTimeMillis());
                requests.put(id, request);
                // If they are offline, this applies and answers it right away
                applyPendingChanges(uuid);
                if (requests.containsKey(id)) {
                    if (isCrossServer()) {
                        notifyOtherServers(uuid);
                    } else if (storage.cancelChange(id)) {
                        // Nobody else uses the storage, so it should have been applied, but something went wrong
                        requests.remove(id);
                        return CompletableFuture.completedFuture(false);
                    }
                }
                return request.answer();
            }
        }, changeExecutor).thenCompose(answer -> answer);
    }

    /**
     * Wakes up the server they are online on (if that's another server), so it applies their changes right away.
     */
    private void notifyOtherServers(UUID uuid) {
        if (notifier != null) {
            notifier.notifyChanges(uuid);
        }
    }

    private void onChangesNotified(UUID uuid) {
        var user = cache.getIfPresent(uuid);
        if (user != null && user.isLoaded()) {
            CompletableFuture.runAsync(() -> applyPendingChanges(uuid));
        }
    }

    private void onAnswersNotified() {
        if (!requests.isEmpty()) {
            CompletableFuture.runAsync(() -> answerRequests(false));
        }
    }

    private String serialize(UserChange change) {
        var data = new YamlConfiguration();
        change.serializeInto(data);
        return data.saveToString();
    }

    /**
     * Applies the stored changes of a player, if this server has their data loaded or can load it.
     */
    private void applyPendingChanges(UUID uuid) {
        synchronized (getPlayerLock(uuid)) {
            var cached = cache.getIfPresent(uuid);
            if (cached != null && cached.isLoaded()) {
                // Online here, the changed data is saved by the next auto save or on quit
                complete(uuid, applyChanges(cached, getKnownChanges(uuid)));
                return;
            }

            // Fails if they are online on another server (that server applies the changes),
            // or if another server is applying them right now
            if (!storage.lockUser(uuid)) return;

            try {
                var changes = getKnownChanges(uuid);
                if (changes.isEmpty()) return;

                var user = storage.loadUser(uuid, dataHolders);
                if (!user.isLoaded()) return; // Failed to load, try again later

                var results = applyChanges(user, changes);
                if (storage.saveUser(user, SaveReason.CUSTOM)) {
                    complete(uuid, results);
                }
            } finally {
                storage.unlockUser(uuid);
            }
        }
    }

    private List<PendingChange> getKnownChanges(UUID uuid) {
        // Changes of types not registered on this server are left for a server that knows them
        return storage.getChanges(uuid).stream().filter(change -> changeTypes.containsKey(change.type())).toList();
    }

    private Map<PendingChange, Boolean> applyChanges(AuroraUser user, List<PendingChange> changes) {
        var results = new LinkedHashMap<PendingChange, Boolean>();
        for (var change : changes) {
            results.put(change, apply(user, change));
        }
        return results;
    }

    private boolean apply(AuroraUser user, PendingChange change) {
        try {
            var data = new YamlConfiguration();
            data.loadFromString(change.data());
            var applied = changeTypes.get(change.type()).apply(data).apply(user);
            Aurora.logger().debug((applied ? "Applied " : "Refused ") + change.type() + " change for player " + user.getUniqueId());
            return applied;
        } catch (Exception e) {
            Aurora.logger().severe("Failed to apply " + change.type() + " change for player " + user.getUniqueId() + " error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes the applied changes from the storage and answers the requests among them.
     * Requests made on this server are answered directly, the others through the storage.
     */
    private void complete(UUID uuid, Map<PendingChange, Boolean> results) {
        var answers = new HashMap<Long, Boolean>();
        results.forEach((change, applied) -> {
            if (change.awaiting() && !requests.containsKey(change.id())) answers.put(change.id(), applied);
        });
        var applied = results.keySet().stream().filter(change -> !answers.containsKey(change.id())).toList();

        if (!storage.completeChanges(uuid, applied, answers)) return;
        // The servers waiting for these can take them right away
        if (!answers.isEmpty() && notifier != null) notifier.notifyAnswers();

        results.forEach((change, result) -> {
            var request = requests.remove(change.id());
            // Async, so whatever the requester does next doesn't run while we hold this player's lock
            if (request != null) request.answer().completeAsync(() -> result);
        });
    }

    /**
     * Answers the requests that were applied on another server, and gives up on the ones
     * that weren't applied for too long (or on all of them, when shutting down).
     */
    private void answerRequests(boolean giveUpAll) {
        var now = System.currentTimeMillis();
        requests.forEach((id, request) -> {
            if (!giveUpAll && now - request.sentAt() < REQUEST_TIMEOUT_MS) return;
            synchronized (getPlayerLock(request.uuid())) {
                // Fails if it was applied in the meantime, then it's answered below
                if (storage.cancelChange(id)) {
                    requests.remove(id);
                    request.answer().completeAsync(() -> false);
                }
            }
        });

        storage.takeAnswers(requests.keySet()).forEach((id, applied) -> {
            var request = requests.remove(id);
            if (request != null) request.answer().completeAsync(() -> applied);
        });
    }

    private record Request(UUID uuid, CompletableFuture<Boolean> answer, long sentAt) {
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
        CompletableFuture.runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);

                storage.loadUser(uuid, dataHolders, user -> {
                    // Same lock as quitting and applying changes, so they all see the same state
                    synchronized (getPlayerLock(uuid)) {
                        if (Bukkit.getPlayer(uuid) == null) {
                            // Left while we were loading. If an earlier join has their data loaded, their quit saves and releases it
                            var loaded = cache.getIfPresent(uuid);
                            if (loaded == null || !loaded.isLoaded()) storage.unlockUser(uuid);
                            return;
                        }

                        var maybeUser = cache.getIfPresent(uuid);
                        // Joined again before the previous load or quit finished, keep the data that's already loaded (and maybe changed)
                        if (maybeUser != null && maybeUser.isLoaded()) {
                            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                    (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(maybeUser)));
                            return;
                        }

                        if (maybeUser != null && !maybeUser.isLoaded()) {
                            lbm.loadUser(user.getUniqueId()).thenAcceptAsync(maybeUser.getLeaderboardEntries()::putAll);

                            maybeUser.loadFromUser(user);
                            Aurora.logger().debug("Updated user " + user.getUniqueId() + " in cache");

                            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                    (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(user)));
                        } else {
                            lbm.loadUser(user.getUniqueId()).thenAcceptAsync(user.getLeaderboardEntries()::putAll);

                            cache.put(uuid, user);
                            Aurora.logger().debug("Loaded user " + user.getUniqueId() + " into cache");

                            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                                    (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(user)));
                        }

                        // Changes sent while they were joining
                        CompletableFuture.runAsync(() -> applyPendingChanges(uuid));
                    }
                });
            }
        });
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
                if (cache.getIfPresent(uuid) != null) {
                    var oldUser = cache.getIfPresent(uuid);
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
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (leaderboardUpdateTask != null) leaderboardUpdateTask.cancel();
        if (changeTask != null) changeTask.cancel();
        if (requestTask != null) requestTask.cancel();
        if (dispose) {
            // Changes made and players who quit right before shutting down
            changeExecutor.shutdown();
            try {
                changeExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        storage.getUsersWithChanges().forEach(this::applyPendingChanges);
        // Nobody would be here for the answers of the ones still waiting, so they shouldn't be applied
        answerRequests(true);
        storage.bulkSaveUsers(cache.asMap().values().stream().filter(AuroraUser::isLoaded).toList(), SaveReason.QUIT);
        var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        lbm.bulkUpdateUsers(cache.asMap().values().stream().filter(AuroraUser::isLoaded).collect(HashMap::new, (m, u) -> m.put(u.getUniqueId(), u.getDirtyLeaderboards().keySet()), HashMap::putAll)).join();
        if (dispose) {
            if (notifier != null) notifier.close();
            storage.dispose();
        }
    }

    public void invalidate(Player player) {
        var user = cache.getIfPresent(player.getUniqueId());
        // When shutting down, they are saved with everyone else
        if (user == null || changeExecutor.isShutdown()) return;
        // On the change threads, so shutting down waits for quits that are being saved
        CompletableFuture.runAsync(() -> {
            var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
            lbm.updateUser(user, Collections.emptyList()).join();
            synchronized (getPlayerLock(user.getUniqueId())) {
                // Rejoined before we got here, their session just goes on
                if (user.getPlayer() != null) {
                    Aurora.logger().debug("Failed to remove user " + user.getUniqueId() + " from cache, because player is still online");
                    return;
                }
                // Changes from now on are stored, and applied once this save is done
                user.setLoaded(false);
                saveUserData(user, SaveReason.QUIT);
                cache.asMap().remove(user.getUniqueId(), user);
                Aurora.logger().debug("Removed user " + user.getUniqueId() + " from cache");
            }
        }, changeExecutor);
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
