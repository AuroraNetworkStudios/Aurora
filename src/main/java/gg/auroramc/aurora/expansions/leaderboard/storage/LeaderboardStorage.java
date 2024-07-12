package gg.auroramc.aurora.expansions.leaderboard.storage;

import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import org.bukkit.entity.Player;

import java.util.*;

public interface LeaderboardStorage {
    List<LbEntry> getTopEntries(String board, int limit);
    Map<String, LbEntry> getPlayerEntries(UUID uuid);
    long getTotalEntryCount(String board);
    void updateEntry(UUID uuid, Set<BoardValue> values);
}
