package gg.auroramc.aurora.api.leaderboard;

import gg.auroramc.aurora.api.user.AuroraUser;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Leaderboards of values computed from player data, see {@code AuroraAPI.getLeaderboards()}.
 */
public interface Leaderboards {
    /**
     * Registers a board, values formatted as numbers. Call it in your plugin's onLoad or onEnable.
     *
     * @param valueMapper a player's value on the board
     * @param cacheSize   how many of the top entries are kept in memory
     */
    void registerBoard(String board, Function<AuroraUser, Double> valueMapper, int cacheSize);

    /**
     * @param formatMapper how an entry's value is shown
     */
    void registerBoard(String board, Function<AuroraUser, Double> valueMapper, Function<LbEntry, String> formatMapper, int cacheSize);

    /**
     * @param minValue players below this aren't on the board
     */
    void registerBoard(String board, Function<AuroraUser, Double> valueMapper, Function<LbEntry, String> formatMapper, int cacheSize, double minValue);

    /**
     * The cached top entries of the board, best first.
     */
    List<LbEntry> getBoard(String board);

    /**
     * How many players are on the board.
     */
    long getBoardSize(String board);

    /**
     * Stores the player's current values on the given boards (all of them if none are given).
     */
    CompletableFuture<Void> updateUser(AuroraUser user, Collection<String> boards);

    CompletableFuture<Void> updateUser(AuroraUser user, String... boards);

    CompletableFuture<Void> clearBoard(String board);

    String formatValue(LbEntry entry);

    Set<String> getBoards();

    /**
     * What to show for an empty place on a board.
     */
    String getEmptyPlaceholder();
}
