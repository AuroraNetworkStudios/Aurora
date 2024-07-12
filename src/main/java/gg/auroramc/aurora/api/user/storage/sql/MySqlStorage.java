package gg.auroramc.aurora.api.user.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import gg.auroramc.aurora.expansions.leaderboard.storage.LeaderboardStorage;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MySqlStorage implements UserStorage, LeaderboardStorage {
    private final HikariDataSource dataSource;
    private final DatabaseCredentials credentials;
    private final String tableName = "aurora_user_data";
    private final String syncTableName = "aurora_sync";
    private final String leaderboardTableName = "aurora_leaderboard";
    private final int networkLatency;
    private final int syncRetryCount;

    @SneakyThrows
    public MySqlStorage() {
        int poolSize = Aurora.getInstance().getConfig().getInt("mysql.pool-size", 10);
        networkLatency = Aurora.getInstance().getConfig().getInt("mysql.network-latency", 500);
        syncRetryCount = Aurora.getInstance().getConfig().getInt("mysql.sync-retry-count", 3);
        credentials = readCredentials();
        HikariConfig config = new HikariConfig();
        config.setPoolName("aurora-pool");
        config.setConnectionTimeout(5000);
        config.setJdbcUrl("jdbc:mysql://" + credentials.host() + ":" + credentials.port() + "/" + credentials.database() + "?useSSL=" + credentials.ssl());
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());
        config.setMaximumPoolSize(poolSize);
        dataSource = new HikariDataSource(config);
        createTable();
    }

    @Override
    public void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler) {
        loadUser(uuid, dataHolders, syncRetryCount, handler);
    }

    @Override
    public AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        try (Connection connection = connection()) {
            return loadUserForReal(connection, uuid, dataHolders);
        } catch (Exception e) {
            return createEmptyUser(uuid, dataHolders, false);
        }
    }

    public void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, int count, Consumer<AuroraUser> handler) {
        Bukkit.getAsyncScheduler().runDelayed(Aurora.getInstance(), (task) -> {
            try {
                try (Connection connection = connection()) {
                    if (Bukkit.getPlayer(uuid) == null) {
                        Aurora.logger().debug("Player: " + uuid + " is left, aborting load.");
                        return;
                    }
                    if (count <= 0) {
                        Aurora.logger().debug("We are still in sync lock after " + syncRetryCount + " retry for player: " + uuid + ". We won't wait anymore. Loading form database...");
                        var user = loadUserForReal(connection, uuid, dataHolders);
                        if (user == null) return;
                        handler.accept(user);
                        createSyncFlag(uuid, connection);
                        Aurora.logger().debug("Player: " + uuid + " loaded from database.");
                        return;
                    }

                    if (!isLocked(uuid, connection)) {
                        var user = loadUserForReal(connection, uuid, dataHolders);
                        if (user == null) return;
                        handler.accept(user);
                        createSyncFlag(uuid, connection);
                        Aurora.logger().debug("Player: " + uuid + " loaded from database.");
                    } else {
                        Aurora.logger().debug("Sync lock detected for player: " + uuid + ", retrying...");
                        loadUser(uuid, dataHolders, count - 1, handler);
                    }
                }
            } catch (Exception ignored) {
            }
        }, networkLatency, TimeUnit.MILLISECONDS);
    }

    public AuroraUser loadUserForReal(Connection connection, UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        String loadQuery = "SELECT * FROM " + tableName + " WHERE player_uuid=?;";

        final var start = System.nanoTime();
        try (PreparedStatement statement = connection.prepareStatement(loadQuery)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return createEmptyUser(uuid, dataHolders, true);
                }

                var user = new AuroraUser(uuid);
                var config = new YamlConfiguration();

                do {
                    String holder = resultSet.getString("holder");
                    String rawYaml = resultSet.getString("data");
                    var section = new YamlConfiguration();
                    section.loadFromString(rawYaml);
                    config.set(holder, section);
                } while (resultSet.next());

                user.initData(config, dataHolders);
                final var end = System.nanoTime();
                Aurora.getUserManager().getLoadLatencyMeasure().addLatency(end - start);
                return user;
            }
        } catch (Exception e) {
            Aurora.logger().severe("Failed to load user data for player: " + uuid);
            return createEmptyUser(uuid, dataHolders, false);
        }
    }

    @Override
    public boolean saveUser(AuroraUser user, SaveReason reason) {
        var uuid = user.getUniqueId();
        String saveQuery = "INSERT INTO " + tableName + " (player_uuid, holder, data) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE data=?;";

        synchronized (user.getSerializeLock()) {
            try {
                final var start = System.nanoTime();
                try (Connection connection = connection()) {
                    connection.setAutoCommit(false);


                    try (PreparedStatement statement = connection.prepareStatement(saveQuery)) {
                        for (var holder : user.getDataHolders().stream().filter(UserDataHolder::isDirty).toList()) {
                            var data = new YamlConfiguration();
                            holder.serializeInto(data);
                            var serializedData = data.saveToString();

                            statement.setString(1, uuid.toString());
                            statement.setString(2, holder.getId().toString());
                            statement.setString(3, serializedData);
                            statement.setString(4, serializedData);
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }

                    final var end = System.nanoTime();
                    Aurora.getUserManager().getSaveLatencyMeasure().addLatency(end - start);

                    if (reason == SaveReason.QUIT) {
                        removeSyncFlag(uuid, connection);
                    }

                    connection.commit();

                    return true;
                }
            } catch (Exception e) {
                Aurora.logger().severe("Failed to save user data for player: " + uuid);
                return false;
            }
        }
    }

    @Override
    public void purgeUser(UUID uuid) {
        String deleteStmt = "DELETE FROM " + tableName + " WHERE player_uuid=?;";

        try {
            try (Connection connection = connection()) {
                try (PreparedStatement statement = connection.prepareStatement(deleteStmt)) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                }
            }
        } catch (Exception e) {
            Aurora.logger().severe("Failed to purge user data for player: " + uuid);
        }
    }

    private void createSyncFlag(UUID uuid, Connection connection) {
        String insertQuery = "INSERT INTO " + syncTableName + " (player_uuid) VALUES (?) ON DUPLICATE KEY UPDATE created=NOW();";

        try {
            final var start = System.nanoTime();
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
            final var end = System.nanoTime();
            Aurora.getUserManager().getSyncFlagLatencyMeasure().addLatency(end - start);
        } catch (SQLException e) {
            Aurora.logger().warning("Failed to add sync flag for player: " + uuid);
        }
    }

    private void removeSyncFlag(UUID uuid, Connection connection) {
        String deleteQuery = "DELETE FROM " + syncTableName + " WHERE player_uuid=? OR created < NOW() - INTERVAL 2 DAY;";

        try {
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            Aurora.logger().warning("Failed to remove sync flag for player: " + uuid);
        }
    }

    private boolean isLocked(UUID uuid, Connection connection) {
        String query = "SELECT * FROM " + syncTableName + " WHERE player_uuid=? AND created > NOW() - INTERVAL 2 DAY;";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            Aurora.logger().warning("Error checking sync lock status for player: " + uuid);
            return false;
        }
    }

    private DatabaseCredentials readCredentials() {
        var config = Aurora.getInstance().getConfig();
        return new DatabaseCredentials(
                config.getString("mysql.host", "127.0.0.1"),
                config.getInt("mysql.port", 3306),
                config.getString("mysql.database", "AuroraCore"),
                config.getString("mysql.username"),
                config.getString("mysql.password"),
                config.getBoolean("mysql.ssl", false)
        );
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTable() throws SQLException {
        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                for (var schema : getTableSchema()) {
                    statement.execute(schema);
                }
            }
        }
    }

    private AuroraUser createEmptyUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, boolean markAsLoaded) {
        var user = new AuroraUser(uuid, markAsLoaded);
        user.initData(null, dataHolders);
        return user;
    }

    @SneakyThrows
    private String[] getTableSchema() {
        return new String(
                Aurora.getInstance().getResource("database/schema.sql").readAllBytes(),
                StandardCharsets.UTF_8)
                .replaceAll("%user_table%", tableName)
                .replaceAll("%sync_table%", syncTableName)
                .replaceAll("%leaderboard_table%", leaderboardTableName)
                .split(";");
    }

    @Override
    public List<LbEntry> getTopEntries(String board, int limit) {
        List<LbEntry> entries = new ArrayList<>();
        String query = "SELECT player_uuid, name, board, value, " +
                "RANK() OVER (ORDER BY value DESC) as position " +
                "FROM " + leaderboardTableName + " WHERE board = ? " +
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
        String query = "SELECT COUNT(*) as total FROM " + leaderboardTableName + " WHERE board = ?";
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
        String existsQuery = "SELECT player_uuid FROM " + leaderboardTableName + " WHERE player_uuid = ? AND board = ?";
        String query = "INSERT INTO " + leaderboardTableName + " (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?, name = ?";
        try (Connection conn = connection();
             PreparedStatement psCheck = conn.prepareStatement(existsQuery);
             PreparedStatement ps = conn.prepareStatement(query)) {
            psCheck.setString(1, uuid.toString());
            psCheck.setString(2, board);
            var rs = psCheck.executeQuery();

            ps.setString(1, uuid.toString());
            ps.setString(2, Bukkit.getOfflinePlayer(uuid).getName()); // Replace with actual player name retrieval
            ps.setString(3, board);
            ps.setDouble(4, value);
            ps.setDouble(5, value);
            ps.setString(6, Bukkit.getOfflinePlayer(uuid).getName());

            boolean exists = rs.next();
            ps.executeUpdate();
            return !exists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
