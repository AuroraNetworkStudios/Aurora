package gg.auroramc.aurora.expansions.leaderboard;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandlerRegistry;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.storage.sql.MySqlStorage;
import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import gg.auroramc.aurora.expansions.leaderboard.storage.BoardValue;
import gg.auroramc.aurora.expansions.leaderboard.storage.LeaderboardStorage;
import gg.auroramc.aurora.expansions.leaderboard.storage.sqlite.SqliteLeaderboardStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


public class LeaderboardExpansion implements AuroraExpansion, Listener {
    private final Map<UUID, Object> updateLocks = Maps.newConcurrentMap();
    private final Map<String, List<LbEntry>> boards = Maps.newConcurrentMap();
    private final Map<String, LbDescriptor> descriptors = Maps.newConcurrentMap();
    private final Map<String, Long> boardSizes = Maps.newConcurrentMap();
    private volatile LeaderboardStorage storage;

    public record LbDescriptor(String name, Function<AuroraUser, Double> valueMapper,
                               Function<LbEntry, String> formatMapper, int cacheSize, double minValue) {
    }

    public void setStorage(LeaderboardStorage storage) {
        this.storage.dispose();
        this.storage = storage;
    }

    @Override
    public void hook() {
        if (Aurora.getLibConfig().getStorageType().equalsIgnoreCase("mysql")) {
            storage = (MySqlStorage) Aurora.getUserManager().getStorage();
        } else {
            storage = new SqliteLeaderboardStorage();
        }

        PlaceholderHandlerRegistry.addHandler(new LbPlaceholderHandler(this));


        var wildcard = new Permission("aurora.leaderboard.prevent.*", PermissionDefault.FALSE);
        Bukkit.getPluginManager().addPermission(wildcard);

        for (var board : descriptors.keySet()) {
            var perm = new Permission("aurora.leaderboard.prevent." + board, PermissionDefault.FALSE);
            Bukkit.getPluginManager().addPermission(perm);
            perm.addParent(wildcard, true);
        }


        for (var board : descriptors.keySet()) {
            boards.putIfAbsent(board, List.of());
            boards.put(board, storage.getTopEntries(board, descriptors.get(board).cacheSize));
            boardSizes.put(board, storage.getTotalEntryCount(board));
        }
    }

    public void updateLeaderBoards() {
        for (var board : descriptors.keySet()) {
            boards.put(board, storage.getTopEntries(board, descriptors.get(board).cacheSize));
            boardSizes.put(board, storage.getTotalEntryCount(board));
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (Bukkit.isStopping() || Aurora.isDisabling()) return;
            var user = Aurora.getUserManager().getUser(player.getUniqueId());
            user.getLeaderboardEntries().putAll(storage.getPlayerEntries(player.getUniqueId()));
        });

        Aurora.logger().debug("Leaderboards updated.");
    }

    @Override
    public boolean canHook() {
        return true;
    }

    private Object getUpdateLock(UUID uuid) {
        return updateLocks.computeIfAbsent(uuid, k -> new Object());
    }

    /**
     * Registers a new leaderboard board.
     * Call this method in your plugin onLoad method.
     *
     * @param board       the name of the board
     * @param valueMapper a function that maps a user to a value
     * @param cacheSize   the size of the cache
     */
    public void registerBoard(String board, Function<AuroraUser, Double> valueMapper, int cacheSize) {
        registerBoard(board, valueMapper, (e) -> AuroraAPI.formatNumber(e.getValue()), cacheSize, 0D);
    }

    /**
     * Registers a new leaderboard board.
     * Call this method in your plugin onLoad method.
     *
     * @param board        the name of the board
     * @param valueMapper  a function that maps a user to a value
     * @param formatMapper a function that maps a leaderboard entry value to a string
     * @param cacheSize    the size of the cache
     */
    public void registerBoard(String board, Function<AuroraUser, Double> valueMapper, Function<LbEntry, String> formatMapper, int cacheSize) {
        registerBoard(board, valueMapper, formatMapper, cacheSize, 0D);
    }

    /**
     * Registers a new leaderboard board.
     * Call this method in your plugin onLoad method.
     *
     * @param board        the name of the board
     * @param valueMapper  a function that maps a user to a value
     * @param formatMapper a function that maps a leaderboard entry value to a string
     * @param cacheSize    the size of the cache
     * @param minValue     the minimum value to be displayed on the leaderboard
     */
    public void registerBoard(String board, Function<AuroraUser, Double> valueMapper, Function<LbEntry, String> formatMapper, int cacheSize, double minValue) {
        descriptors.put(board, new LbDescriptor(board, valueMapper, formatMapper, cacheSize, minValue));
    }

    /**
     * Get the leaderboard list.
     *
     * @param board the name of the board
     * @return the leaderboard list
     */
    public List<LbEntry> getBoard(String board) {
        return boards.getOrDefault(board, List.of());
    }

    /**
     * Get the actual size of a leaderboard.
     *
     * @param board the name of the board
     * @return the size of the leaderboard
     */
    public long getBoardSize(String board) {
        return boardSizes.getOrDefault(board, 0L);
    }

    /**
     * Load the user leaderboard entries for every board.
     *
     * @param uuid player uuid
     * @return a map of leaderboard entries
     */
    public CompletableFuture<Map<String, LbEntry>> loadUser(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (getUpdateLock(uuid)) {
                return storage.getPlayerEntries(uuid);
            }
        });
    }

    public CompletableFuture<Void> bulkUpdateUsers(Map<UUID, Collection<String>> data) {
        return CompletableFuture.runAsync(() -> {
            var toUpdate = new HashMap<UUID, Set<BoardValue>>(data.size());
            for (var entry : data.entrySet()) {
                var user = Aurora.getUserManager().getUser(entry.getKey());
                if (user == null || !user.isLoaded()) {
                    continue;
                }
                var player = user.getPlayer();

                var values = new HashSet<BoardValue>(entry.getValue().size());

                for (var board : entry.getValue()) {
                    populateBoardValues(user, player, values, board);
                }

                if (!values.isEmpty()) {
                    toUpdate.put(entry.getKey(), values);
                }
            }
            if (!toUpdate.isEmpty()) {
                storage.bulkUpdateEntries(toUpdate);
            }
        });
    }

    private void populateBoardValues(AuroraUser user, Player player, HashSet<BoardValue> values, String board) {
        if (player != null && player.hasPermission("aurora.leaderboard.prevent." + board)) {
            return;
        }
        double value = descriptors.get(board).valueMapper.apply(user);
        if (value >= descriptors.get(board).minValue) {
            values.add(new BoardValue(board, value));
        }
    }

    /**
     * Updates the player on the leaderboard in the storage.
     *
     * @param updateBoards name of the boards to update
     * @param user         user to update
     */
    public CompletableFuture<Void> updateUser(AuroraUser user, Collection<String> updateBoards) {
        return CompletableFuture.runAsync(() -> {
            synchronized (getUpdateLock(user.getUniqueId())) {
                var player = user.getPlayer();
                var toUpdate = new HashSet<BoardValue>(updateBoards.isEmpty() ? descriptors.keySet().size() : updateBoards.size());

                for (var board : updateBoards.isEmpty() ? descriptors.keySet() : updateBoards) {
                    populateBoardValues(user, player, toUpdate, board);
                }

                if (!toUpdate.isEmpty()) {
                    storage.updateEntry(user.getUniqueId(), toUpdate);
                }
            }
        });
    }

    /**
     * Updates the player cached values in the leaderboard.
     *
     * @param updateBoards name of the boards to update
     * @param user         user to update
     */
    public CompletableFuture<Void> updateUser(AuroraUser user, String... updateBoards) {
        var player = user.getPlayer();

        for (var board : updateBoards.length == 0 ? descriptors.keySet() : Arrays.asList(updateBoards)) {
            if (player != null && player.hasPermission("aurora.leaderboard.prevent." + board)) {
                continue;
            }
            double value = descriptors.get(board).valueMapper.apply(user);
            if (value >= descriptors.get(board).minValue) {
                var entries = user.getLeaderboardEntries();
                if (entries.containsKey(board)) {
                    entries.get(board).setValue(value);
                } else {
                    entries.put(board, new LbEntry(user.getUniqueId(), player.getName(), board, value, 0));
                }
            }
        }
        // Didn't change the method signature because it's used in a lot of places
        return CompletableFuture.completedFuture(null);
    }

    public String formatValue(LbEntry entry) {
        var mapper = descriptors.get(entry.getBoard()).formatMapper;
        if (mapper != null) {
            return mapper.apply(entry);
        } else {
            return AuroraAPI.formatNumber(entry.getValue());
        }
    }

    public Set<String> getBoards() {
        return descriptors.keySet();
    }

    public String getEmptyPlaceholder() {
        return Aurora.getLibConfig().getLeaderboards().getEmptyPlaceholder();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        updateLocks.remove(event.getPlayer().getUniqueId());
    }

    public void dispose() {
        storage.dispose();
    }
}
