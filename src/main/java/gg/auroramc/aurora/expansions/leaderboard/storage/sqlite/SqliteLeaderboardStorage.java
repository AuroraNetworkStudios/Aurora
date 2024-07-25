package gg.auroramc.aurora.expansions.leaderboard.storage.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import gg.auroramc.aurora.expansions.leaderboard.storage.BoardValue;
import gg.auroramc.aurora.expansions.leaderboard.storage.LeaderboardStorage;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class SqliteLeaderboardStorage implements LeaderboardStorage {
    private HikariDataSource dataSource;

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
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + Aurora.getInstance().getDataFolder() + "/leaderboards.db");
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(10);
        config.setDriverClassName("org.sqlite.JDBC");
        this.dataSource = new HikariDataSource(config);

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
            return dataSource.getConnection();
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
                        FROM aurora_leaderboard
                    )
                    SELECT player_uuid, name, board, value, position
                    FROM RankedEntries
                    WHERE player_uuid = ?
                """;

        try (Connection conn = connection();
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
    public void updateEntry(UUID uuid, Set<BoardValue> values) {
        String query = "INSERT INTO aurora_leaderboard (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, board) DO UPDATE SET value = ?, name = ?";

        try (Connection conn = connection();
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
}
