package gg.auroramc.auroralib.api.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.user.event.AuroraUserLoadedEvent;
import gg.auroramc.auroralib.api.user.event.AuroraUserUnloadedEvent;
import gg.auroramc.auroralib.api.user.storage.LatencyMeasure;
import gg.auroramc.auroralib.api.user.storage.SaveReason;
import gg.auroramc.auroralib.api.user.storage.UserStorage;
import gg.auroramc.auroralib.api.user.storage.YamlStorage;
import gg.auroramc.auroralib.api.user.storage.sql.MySqlStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class UserManager implements Listener {
    private final UserStorage storage;
    private final ConcurrentHashMap<UUID, Object> playerLocks = new ConcurrentHashMap<>();
    private final Set<Class<? extends UserDataHolder>> dataHolders = new HashSet<>();
    private final ScheduledTask autoSaveTask;
    @Getter
    private final LatencyMeasure loadLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure saveLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure syncFlagLatencyMeasure = new LatencyMeasure();

    // Stores actual online users data
    private final Cache<UUID, AuroraUser> cache = CacheBuilder.newBuilder()
            .removalListener(notification -> {
                AuroraUser user = (AuroraUser) notification.getValue();
                CompletableFuture.runAsync(() -> saveUserData(user, SaveReason.QUIT));
            })
            .build();


    // Stores user data for offline players that are loaded for some reason
    private final Cache<UUID, AuroraUser> offlineCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    public UserManager() {
        Bukkit.getPluginManager().registerEvents(this, AuroraLib.getInstance());

        if (AuroraLib.getLibConfig().getStorageType().equals("mysql")) {
            storage = new MySqlStorage();
        } else {
            storage = new YamlStorage();
        }

        this.autoSaveTask = Bukkit.getAsyncScheduler().runAtFixedRate(AuroraLib.getInstance(), (task) -> {
            var values = cache.asMap().values();
            var successCount = 0;
            var all = values.size();
            for (var user : values) {
                if (!user.isLoaded()) {
                    successCount++;
                    continue;
                }
                var success = saveUserData(user, SaveReason.AUTO_SAVE);
                if (success) successCount++;
            }
            if (!Bukkit.getOnlinePlayers().isEmpty() && !values.isEmpty()) {
                AuroraLib.logger().info("Auto background saved user data for " + successCount + "/" + all + " online players");
            }
        }, AuroraLib.getLibConfig().getUserAutoSaveInMinutes(), AuroraLib.getLibConfig().getUserAutoSaveInMinutes(), TimeUnit.MINUTES);
    }

    public void walkUserData(Consumer<AuroraUser> callback) {
        storage.walkUserData(callback, dataHolders);
    }

    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        synchronized (getPlayerLock(user.getUniqueId())) {
            var result = storage.saveUser(user, reason);
            if (reason == SaveReason.QUIT && !Bukkit.isStopping() && !AuroraLib.isDisabling()) {
                AuroraLib.logger().debug("Saved user " + user.getUniqueId() + " into storage");
                Bukkit.getGlobalRegionScheduler().run(AuroraLib.getInstance(),
                        (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserUnloadedEvent(user)));
            }
            return result;
        }
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
                storage.loadUser(uuid, dataHolders, user -> {
                    if (Bukkit.getPlayer(uuid) == null) return;

                    var maybeUser = cache.getIfPresent(uuid);

                    if (maybeUser != null && !maybeUser.isLoaded()) {
                        maybeUser.setLoaded(true);
                        maybeUser.initData(user.getConfiguration(), dataHolders);
                    } else {
                        cache.put(uuid, user);
                    }

                    AuroraLib.logger().debug("Loaded user " + user.getUniqueId() + " into cache");
                    Bukkit.getGlobalRegionScheduler().run(AuroraLib.getInstance(),
                            (task) -> Bukkit.getPluginManager().callEvent(new AuroraUserLoadedEvent(user)));
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

    /**
     * Stop cacheTTL heartbeat task for online players and autosave task.
     * Saves all cached data sync.
     * Should be used only in onDisable()
     */
    public void stopTasksAndSaveAllData() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        for (var user : cache.asMap().values()) {
            saveUserData(user, SaveReason.QUIT);
        }
    }

    public void invalidate(Player player) {
        cache.invalidate(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUser(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        invalidate(e.getPlayer());
    }
}
