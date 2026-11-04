package gg.auroramc.aurora.internal.expansions.leaderboard.storage;

import gg.auroramc.aurora.api.leaderboard.LbEntry;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.internal.expansions.leaderboard.storage.sqlite.SqliteLeaderboardStorage;
import gg.auroramc.aurora.internal.user.storage.sql.MySqlLeaderboardStorage;
import gg.auroramc.aurora.internal.user.storage.sql.MySqlStorage;

import java.util.*;

public interface LeaderboardStorage {
    /**
     * The leaderboard storage that goes with a user storage: MySQL's own database, or SQLite next to YAML files.
     */
    static LeaderboardStorage forUserStorage(UserStorage storage) {
        return storage instanceof MySqlStorage mySqlStorage ? new MySqlLeaderboardStorage(mySqlStorage) : new SqliteLeaderboardStorage();
    }

    List<LbEntry> getTopEntries(String board, int limit);
    Map<String, LbEntry> getPlayerEntries(UUID uuid);
    long getTotalEntryCount(String board);
    void clearBoard(String board);
    void updateEntry(UUID uuid, Set<BoardValue> values);
    void bulkUpdateEntries(Map<UUID, Set<BoardValue>> values);
    void dispose();
}
