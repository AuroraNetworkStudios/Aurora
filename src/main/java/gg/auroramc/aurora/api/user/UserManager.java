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

public class UserManager implements Listener {
    @Getter
    private volatile UserStorage storage;
    private final ConcurrentHashMap<UUID, Object> playerLocks = new ConcurrentHashMap<>();
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

    // Stores actual online users data
    private final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder().build();


    // Stores user data for offline players that are loaded for some reason
    private final Cache<UUID, AuroraUser> offlineCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

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

        return CompletableFuture.supplyAsync(() -> {
            stopTasksAndSaveAllData(false);
            cache.invalidateAll();
            offlineCache.invalidateAll();

            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
                Bukkit.getOnlinePlayers().forEach(player -> player.kick(Text.component(Aurora.getMessageConfig().getKickedByDbMigration())));
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

    private Object getPlayerLock(UUID playerId) {
        return playerLocks.computeIfAbsent(playerId, k -> new Object());
    }

    public <T extends UserDataHolder> void registerUserDataHolder(Class<T> clazz) {
        dataHolders.add(clazz);
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
        if (cache.getIfPresent(uuid) == null) {
            var fakeUser = new AuroraUser(uuid, false);
            fakeUser.initData(null, dataHolders);
            cache.put(uuid, fakeUser);
            Aurora.logger().debug("Created fake user " + uuid + " in cache");
        }
        return cache.getIfPresent(uuid);
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
                    if (Bukkit.getPlayer(uuid) == null) return;

                    var maybeUser = cache.getIfPresent(uuid);

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
        if (offlineCache.getIfPresent(uuid) != null) {
            return CompletableFuture.completedFuture(offlineCache.getIfPresent(uuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            var user = storage.loadUser(uuid, dataHolders);
            offlineCache.put(uuid, user);
            return user;
        });
    }

    public void purgeUserData(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            synchronized (getPlayerLock(uuid)) {
                if (offlineCache.getIfPresent(uuid) != null) {
                    offlineCache.invalidate(uuid);
                }
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
        storage.bulkSaveUsers(cache.asMap().values().stream().filter(AuroraUser::isLoaded).toList(), SaveReason.QUIT);
        var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
        lbm.bulkUpdateUsers(cache.asMap().values().stream().filter(AuroraUser::isLoaded).collect(HashMap::new, (m, u) -> m.put(u.getUniqueId(), u.getDirtyLeaderboards().keySet()), HashMap::putAll)).join();
        if (dispose) storage.dispose();
    }

    public void invalidate(Player player) {
        var user = cache.getIfPresent(player.getUniqueId());
        if (user == null) return;
        CompletableFuture.supplyAsync(() -> {
            var lbm = Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class);
            lbm.updateUser(user, Collections.emptyList()).join();
            return saveUserData(user, SaveReason.QUIT);
        }).thenAcceptAsync(success -> {
            if (user.getPlayer() == null || !user.getPlayer().isOnline()) {
                Aurora.logger().debug("Removed user " + user.getUniqueId() + " from cache");
                playerLocks.remove(user.getUniqueId());
                cache.invalidate(player.getUniqueId());
            } else {
                Aurora.logger().debug("Failed to remove user " + user.getUniqueId() + " from cache, because player is still online");
            }
        });
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
