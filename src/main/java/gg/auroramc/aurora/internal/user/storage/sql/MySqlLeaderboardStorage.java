package gg.auroramc.aurora.internal.user.storage.sql;

import gg.auroramc.aurora.api.leaderboard.LbEntry;
import gg.auroramc.aurora.internal.expansions.leaderboard.storage.BoardValue;
import gg.auroramc.aurora.internal.expansions.leaderboard.storage.LeaderboardStorage;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * The leaderboards in MySQL, through the user storage's connection pool (and in its database, next to the player
 * data). The pool belongs to the user storage, so disposing this does nothing.
 */
public class MySqlLeaderboardStorage implements LeaderboardStorage {
    private final MySqlStorage storage;

    public MySqlLeaderboardStorage(MySqlStorage storage) {
        this.storage = storage;
    }

    @Override
    public List<LbEntry> getTopEntries(String board, int limit) {
        List<LbEntry> entries = new ArrayList<>();
        String query = "SELECT player_uuid, name, board, value, " +
                "RANK() OVER (ORDER BY value DESC) as position " +
                "FROM " + MySqlStorage.LEADERBOARD_TABLE + " WHERE board = ? " +
                "ORDER BY value DESC LIMIT ?";

        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, board);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String name = rs.getString("name");
                String boardName = rs.getString("board");
                double value = rs.getDouble("value");
                long position = rs.getLong("position");

                entries.add(new LbEntry(uuid, name, boardName, value, position));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    @Override
    public Map<String, LbEntry> getPlayerEntries(UUID uuid) {
        Map<String, LbEntry> entries = new HashMap<>();

        String query = """
                    WITH RankedEntries AS (
                        SELECT
                            player_uuid,
                            name,
                            board,
                            value,
                            RANK() OVER (PARTITION BY board ORDER BY value DESC) as position
                        FROM %s
                    )
                    SELECT player_uuid, name, board, value, position
                    FROM RankedEntries
                    WHERE player_uuid = ?
                """.formatted(MySqlStorage.LEADERBOARD_TABLE);

        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            // Bind the player UUID to the prepared statement
            ps.setString(1, uuid.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String boardName = rs.getString("board");
                String name = rs.getString("name");
                double value = rs.getDouble("value");
                long position = rs.getLong("position");

                entries.put(boardName, new LbEntry(uuid, name, boardName, value, position));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    @Override
    public long getTotalEntryCount(String board) {
        String query = "SELECT COUNT(*) as total FROM " + MySqlStorage.LEADERBOARD_TABLE + " WHERE board = ?";
        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, board);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void clearBoard(String board) {
        String query = "DELETE FROM " + MySqlStorage.LEADERBOARD_TABLE + " WHERE board = ?";

        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, board);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEntry(UUID uuid, Set<BoardValue> values) {
        String query = "INSERT INTO " + MySqlStorage.LEADERBOARD_TABLE + " (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?, name = ?";

        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            var name = Bukkit.getOfflinePlayer(uuid).getName();

            for (BoardValue boardValue : values) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, boardValue.board());
                ps.setDouble(4, boardValue.value());
                ps.setDouble(5, boardValue.value());
                ps.setString(6, name);
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void bulkUpdateEntries(Map<UUID, Set<BoardValue>> values) {
        String query = "INSERT INTO " + MySqlStorage.LEADERBOARD_TABLE + " (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?, name = ?";

        final int BATCH_SIZE = 100;
        int batchCount = 0;

        try (Connection conn = storage.connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            for (var entry : values.entrySet()) {
                var uuid = entry.getKey();
                var name = Bukkit.getOfflinePlayer(uuid).getName();

                for (var boardValue : entry.getValue()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, boardValue.board());
                    ps.setDouble(4, boardValue.value());
                    ps.setDouble(5, boardValue.value());
                    ps.setString(6, name);
                    ps.addBatch();

                    batchCount++;

                    // Execute and clear the batch if the batch size limit is reached
                    if (batchCount >= BATCH_SIZE) {
                        ps.executeBatch();
                        ps.clearBatch();
                        batchCount = 0;
                    }
                }
            }

            if (batchCount > 0) {
                ps.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
    }
}
