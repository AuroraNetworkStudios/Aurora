package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.internal.user.*;
import gg.auroramc.aurora.internal.user.migration.StorageMigrator;
import gg.auroramc.aurora.internal.user.storage.LatencyMeasure;
import gg.auroramc.aurora.internal.user.storage.YamlStorage;
import gg.auroramc.aurora.internal.user.storage.sql.MySqlStorage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Players' data: loading it when they join, saving it, and changing it for online and offline players.
 * <p>
 * The work is done by the parts in {@code gg.auroramc.aurora.internal.user}: {@link Sessions} (join, quit, rejoin),
 * {@link OfflineCopies}, {@link Saving} (the autosave, saving everyone, migrating), {@link Edits} and
 * {@link UserEvents}. What they share, and the locking rules, are in {@link UserState}.
 */
public class UserManager implements Listener {
    private final UserState state;
    private final OfflineCopies offline;
    private final Edits edits;
    private final Saving saving;
    private final Sessions sessions;
    @Getter
    private final LatencyMeasure loadLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure saveLatencyMeasure = new LatencyMeasure();
    @Getter
    private final LatencyMeasure syncFlagLatencyMeasure = new LatencyMeasure();

    public UserManager() {
        state = new UserState(new StorageMigrator(this));
        offline = new OfflineCopies(state);
        edits = new Edits(state, offline);
        var events = new UserEvents(state);
        saving = new Saving(state, edits, events);
        sessions = new Sessions(state, edits, offline, saving, events);

        Bukkit.getPluginManager().registerEvents(this, Aurora.getInstance());
        Bukkit.getOnlinePlayers().forEach(sessions::joinedEarly);

        if (Aurora.getLibConfig().getStorageType().equals("mysql")) {
            state.setStorage(new MySqlStorage());
        } else {
            state.setStorage(new YamlStorage());
        }

        saving.startTasks();
    }

    public UserStorage getStorage() {
        return state.getStorage();
    }

    public Set<Class<? extends UserDataHolder>> getDataHolders() {
        return state.getDataHolders();
    }

    public CompletableFuture<Boolean> attemptMigration(int threadCount) {
        return saving.migrate(threadCount);
    }

    /**
     * Saves the user's data now, on this thread. It waits for a save of the same player that's running, and while a
     * storage migration runs, so don't call it on the main thread. Don't call it from inside an edit (editUser,
     * editUserNow), that could deadlock.
     */
    public boolean saveUserData(AuroraUser user, SaveReason reason) {
        return saving.saveUserData(user, reason);
    }

    public <T extends UserDataHolder> void registerUserDataHolder(Class<T> clazz) {
        state.getDataHolders().add(clazz);
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
        return edits.editUser(uuid, edit);
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
        return edits.editUserNow(uuid, edit);
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
        return edits.getUserNow(uuid);
    }

    /**
     * Checks if the user is in the cache
     * Use this if you are working with offline players and load data inside a CompletableFuture!
     *
     * @param uuid player's uuid
     * @return whether the players data is loaded into the cache or not
     */
    public boolean isUserCached(UUID uuid) {
        return sessions.isUserCached(uuid);
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
        return sessions.getUser(uuid);
    }

    /**
     * Loads user into the cache.
     *
     * @param uuid player's uuid to load data for
     */
    public void loadUser(UUID uuid) {
        sessions.loadUser(uuid);
    }

    /**
     * Loads user directly from storage. For an offline player loaded to be edited (see {@link #editUser}), it's that
     * copy instead, since the storage may not have their latest edits yet. Don't change it, use editUser for that.
     *
     * @param uuid player's uuid to load data for
     */
    public CompletableFuture<AuroraUser> loadUserFromStorage(UUID uuid) {
        return offline.loadUserFromStorage(uuid);
    }

    public void purgeUserData(UUID uuid) {
        sessions.purgeUserData(uuid);
    }

    /**
     * Stop cacheTTL heartbeat task for online players and autosave task.
     * Saves all cached data sync.
     * Should be used only in onDisable()
     */
    public void stopTasksAndSaveAllData(boolean dispose) {
        saving.stopTasksAndSaveAllData(dispose);
    }

    public void invalidate(Player player) {
        sessions.invalidate(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadUser(e.getPlayer().getUniqueId());
    }

    // Before every other join listener, so they all see the player as online here (their edits wait for the load)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEarly(PlayerJoinEvent e) {
        sessions.joinedEarly(e.getPlayer());
    }

    /**
     * Whether the player is online on this server, as far as their data is concerned: from the start of their join
     * until their quit.
     */
    public boolean isOnline(UUID uuid) {
        return sessions.isOnline(uuid);
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent e) {
        if (sessions.isMigrating()) {
            e.kickMessage(Text.component("&cUnder maintenance, please try again later."));
            e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        sessions.quit(e.getPlayer());
    }
}
