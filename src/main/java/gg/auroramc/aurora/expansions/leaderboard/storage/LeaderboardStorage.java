package gg.auroramc.aurora.expansions.leaderboard.storage;

import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import org.bukkit.entity.Player;

import java.util.*;

public interface LeaderboardStorage {
    List<LbEntry> getTopEntries(String board, int limit);
    Map<String, LbEntry> getPlayerEntries(UUID uuid, Set<String> boards);
    Map<UUID, Map<String, LbEntry>> getPlayerEntries(Collection<? extends Player> player, Set<String> boards);
    long getTotalEntryCount(String board);
    boolean updateEntry(String board, UUID uuid, double value);
}
