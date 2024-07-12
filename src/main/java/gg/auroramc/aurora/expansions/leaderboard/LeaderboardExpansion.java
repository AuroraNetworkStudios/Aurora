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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class LeaderboardExpansion implements AuroraExpansion, Listener {
    private final Map<UUID, Object> updateLocks = Maps.newConcurrentMap();
    private final Map<String, Function<AuroraUser, Double>> valueMappers = Maps.newConcurrentMap();
    private final Map<String, Function<LbEntry, String>> formatMappers = Maps.newConcurrentMap();
    private final Map<String, List<LbEntry>> boards = Maps.newConcurrentMap();
    private final Map<String, Long> boardSizes = Maps.newConcurrentMap();
    private final Map<String, Integer> boardCacheSizes = Maps.newHashMap();
    private LeaderboardStorage storage;

    @Override
    public void hook() {
        if (Aurora.getLibConfig().getStorageType().equalsIgnoreCase("mysql")) {
            storage = (MySqlStorage) Aurora.getUserManager().getStorage();
        } else {
            storage = new SqliteLeaderboardStorage();
        }

        PlaceholderHandlerRegistry.addHandler(new LbPlaceholderHandler(this));

        for (var board : valueMappers.keySet()) {
            boards.putIfAbsent(board, List.of());
            boards.put(board, storage.getTopEntries(board, boardCacheSizes.get(board)));
            boardSizes.put(board, storage.getTotalEntryCount(board));
        }

        updateTask();
    }

    public void updateTask() {
        Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            for (var board : valueMappers.keySet()) {
                boards.put(board, storage.getTopEntries(board, boardCacheSizes.get(board)));
                boardSizes.put(board, storage.getTotalEntryCount(board));
            }

            Bukkit.getOnlinePlayers().forEach(player -> {
                if (Bukkit.isStopping() || Aurora.isDisabling()) return;
                var user = Aurora.getUserManager().getUser(player.getUniqueId());
                user.getLeaderboardEntries().putAll(storage.getPlayerEntries(player.getUniqueId()));
            });

            if (!Bukkit.isStopping() && !Aurora.isDisabling()) {
                updateTask();
            }
        }, 5, TimeUnit.MINUTES);
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
        valueMappers.put(board, valueMapper);
        boardCacheSizes.put(board, cacheSize);
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
        valueMappers.put(board, valueMapper);
        boardCacheSizes.put(board, cacheSize);
        formatMappers.put(board, formatMapper);
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

    /**
     * Updates the player on the leaderboard in the storage.
     *
     * @param updateBoards name of the boards to update
     * @param user         user to update
     */
    public CompletableFuture<Void> updateUser(AuroraUser user, String... updateBoards) {
        return CompletableFuture.runAsync(() -> {
            synchronized (getUpdateLock(user.getUniqueId())) {
                var toUpdate = new HashSet<BoardValue>(updateBoards.length == 0 ? valueMappers.keySet().size() : updateBoards.length);

                for (var board : updateBoards.length == 0 ? valueMappers.keySet() : Arrays.asList(updateBoards)) {
                    double value = valueMappers.get(board).apply(user);
                    toUpdate.add(new BoardValue(board, value));
                }

                storage.updateEntry(user.getUniqueId(), toUpdate);
            }
        });
    }

    public String formatValue(LbEntry entry) {
        return formatMappers.getOrDefault(entry.getBoard(), (e) -> AuroraAPI.formatNumber(e.getValue())).apply(entry);
    }

    public Set<String> getBoards() {
        return valueMappers.keySet();
    }

    public String getEmptyPlaceholder() {
        return Aurora.getLibConfig().getLeaderboards().getEmptyPlaceholder();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        updateLocks.remove(event.getPlayer().getUniqueId());
    }
}
