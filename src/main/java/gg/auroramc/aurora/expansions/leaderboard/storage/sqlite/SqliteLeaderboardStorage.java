package gg.auroramc.aurora.expansions.leaderboard.storage.sqlite;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import gg.auroramc.aurora.expansions.leaderboard.storage.LeaderboardStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class SqliteLeaderboardStorage implements LeaderboardStorage {
    private final static String table = """
                    CREATE TABLE IF NOT EXISTS aurora_leaderboard
                    (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid VARCHAR(36) NOT NULL,
                        name        VARCHAR(50) NOT NULL,
                        board       VARCHAR(50) NOT NULL,
                        value       DOUBLE DEFAULT 0.0,
                        UNIQUE (player_uuid, board)
                    );
            """;

    private final static String[] indexes = new String[]{
            "CREATE INDEX idx_board_value ON aurora_leaderboard (board, value);",
            "CREATE INDEX idx_player_board ON aurora_leaderboard (player_uuid, board);"
    };


    public SqliteLeaderboardStorage() {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(table);
            for (String index : indexes) {
                createIndexIfNotExists(conn, index);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createIndexIfNotExists(Connection conn, String index) throws SQLException {
        String indexName = index.split(" ")[2];
        String checkIndexQuery = "PRAGMA index_list('aurora_leaderboard')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkIndexQuery)) {
            boolean exists = false;
            while (rs.next()) {
                if (indexName.equals(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                try (Statement createStmt = conn.createStatement()) {
                    createStmt.executeUpdate(index);
                }
            }
        }
    }

    private Connection connection() {
        try {
            // Replace with the path to your SQLite database file
            String url = "jdbc:sqlite:" + Aurora.getInstance().getDataFolder() + "/leaderboards.db";
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to the database", e);
        }
    }

    @Override
    public List<LbEntry> getTopEntries(String board, int limit) {
        List<LbEntry> entries = new ArrayList<>();
        String query = "SELECT player_uuid, name, board, value, " +
                "RANK() OVER (ORDER BY value DESC) as position " +
                "FROM aurora_leaderboard WHERE board = ? " +
                "ORDER BY value DESC LIMIT ?";

        try (Connection conn = connection();
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
    public Map<String, LbEntry> getPlayerEntries(UUID uuid, Set<String> boards) {
        Map<String, LbEntry> entries = new HashMap<>();

        String boardNamesPlaceholders = String.join(",", Collections.nCopies(boards.size(), "?"));

        String query = """
                WITH RankedEntries AS (
                    SELECT
                        player_uuid,
                        name,
                        board,
                        value,
                        RANK() OVER (PARTITION BY board ORDER BY value DESC) as position
                    FROM aurora_leaderboard
                    WHERE board IN (""" + boardNamesPlaceholders + """
                ))
                    SELECT player_uuid, name, board, value, position
                    FROM RankedEntries
                    WHERE player_uuid = ?
                """;

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int index = 1;

            // Bind board names to the prepared statement
            for (String board : boards) {
                ps.setString(index, board);
                index++;
            }

            // Bind the player UUID to the prepared statement
            ps.setString(index, uuid.toString());

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
    public Map<UUID, Map<String, LbEntry>> getPlayerEntries(Collection<? extends Player> players, Set<String> boards) {
        Map<UUID, Map<String, LbEntry>> entries = new HashMap<>();

        String playerUuids = String.join(",", Collections.nCopies(players.size(), "?"));
        String boardNames = String.join(",", Collections.nCopies(boards.size(), "?"));

        String query = """
                WITH RankedEntries AS (
                    SELECT
                        player_uuid,
                        name,
                        board,
                        value,
                        RANK() OVER (PARTITION BY board ORDER BY value DESC) as position
                    FROM aurora_leaderboard
                    WHERE board IN (""" + boardNames + """
                ))
                SELECT player_uuid, name, board, value, position
                FROM RankedEntries
                WHERE player_uuid IN (""" + playerUuids + """
                )""";

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int index = 1;
            for (String board : boards) {
                ps.setString(index++, board);
            }
            for (Player player : players) {
                ps.setString(index++, player.getUniqueId().toString());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String boardName = rs.getString("board");
                String name = rs.getString("name");
                double value = rs.getDouble("value");
                long position = rs.getLong("position");

                entries.computeIfAbsent(uuid, k -> new HashMap<>())
                        .put(boardName, new LbEntry(uuid, name, boardName, value, position));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    @Override
    public long getTotalEntryCount(String board) {
        String query = "SELECT COUNT(*) as total FROM aurora_leaderboard WHERE board = ?";
        try (Connection conn = connection();
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
    public boolean updateEntry(String board, UUID uuid, double value) {
        String existsQuery = "SELECT player_uuid FROM aurora_leaderboard WHERE player_uuid = ? AND board = ?";
        String query = "INSERT INTO aurora_leaderboard (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, board) DO UPDATE SET value = ?, name = ?";

        try (Connection conn = connection();
             PreparedStatement psCheck = conn.prepareStatement(existsQuery);
             PreparedStatement ps = conn.prepareStatement(query)) {
            psCheck.setString(1, uuid.toString());
            psCheck.setString(2, board);
            ResultSet rs = psCheck.executeQuery();
            boolean exists = rs.next();

            ps.setString(1, uuid.toString());
            ps.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            ps.setString(3, board);
            ps.setDouble(4, value);
            ps.setDouble(5, value);
            ps.setString(6, Bukkit.getOfflinePlayer(uuid).getName());

            ps.executeUpdate();

            return !exists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
