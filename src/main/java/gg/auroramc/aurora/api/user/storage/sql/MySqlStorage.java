package gg.auroramc.aurora.api.user.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.UserStorage;
import gg.auroramc.aurora.expansions.leaderboard.model.LbEntry;
import gg.auroramc.aurora.expansions.leaderboard.storage.BoardValue;
import gg.auroramc.aurora.expansions.leaderboard.storage.LeaderboardStorage;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

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
        // config.setJdbcUrl("jdbc:mysql://" + credentials.host() + ":" + credentials.port() + "/" + credentials.database() + "?useSSL=" + credentials.ssl() + "&rewriteBatchedStatements=true");
        config.setJdbcUrl("jdbc:mysql://" + credentials.host() + ":" + credentials.port() + "/" + credentials.database() + "?useSSL=" + credentials.ssl());
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());
        config.setMaximumPoolSize(poolSize);
        dataSource = new HikariDataSource(config);
        createTable();
    }

    public Set<String> getUserIds(int limit, int offset) {
        Set<String> ids = new HashSet<>();
        String query = "SELECT DISTINCT player_uuid FROM " + tableName + " ORDER BY id LIMIT ? OFFSET ?";

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
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
    public int bulkSaveUsers(List<AuroraUser> users, SaveReason reason) {
        String saveQuery = "INSERT INTO " + tableName + " (player_uuid, holder, data) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE data=?;";

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);

            final int BATCH_SIZE = 100;
            int batchCount = 0;

            try (PreparedStatement statement = connection.prepareStatement(saveQuery)) {
                for (var user : users) {
                    var uuid = user.getUniqueId();
                    synchronized (user.getSerializeLock()) {
                        for (var holder : user.getDataHolders().stream().filter(UserDataHolder::isDirty).toList()) {
                            var data = new YamlConfiguration();
                            holder.serializeInto(data);
                            var serializedData = data.saveToString();

                            statement.setString(1, uuid.toString());
                            statement.setString(2, holder.getId().toString());
                            statement.setString(3, serializedData);
                            statement.setString(4, serializedData);
                            statement.addBatch();

                            batchCount++;

                            if (batchCount >= BATCH_SIZE) {
                                statement.executeBatch();
                                statement.clearBatch();
                                batchCount = 0;
                            }
                        }
                    }
                }

                if (batchCount > 0) {
                    statement.executeBatch();
                }
            }

            if (reason == SaveReason.QUIT) {
                String deleteQuery = "DELETE FROM " + syncTableName + " WHERE player_uuid=? OR created < NOW() - INTERVAL 2 DAY;";

                try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                    for (var user : users) {
                        statement.setString(1, user.getUniqueId().toString());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }

            connection.commit();
            return users.size();
        } catch (Exception e) {
            Aurora.logger().severe("Failed to save user data for players.");
            e.printStackTrace();
            return 0;
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
    public void clearBoard(String board) {
        String query = "DELETE FROM " + leaderboardTableName + " WHERE board = ?";

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, board);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEntry(UUID uuid, Set<BoardValue> values) {
        String query = "INSERT INTO " + leaderboardTableName + " (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?, name = ?";

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

    @Override
    public void bulkUpdateEntries(Map<UUID, Set<BoardValue>> values) {
        String query = "INSERT INTO " + leaderboardTableName + " (player_uuid, name, board, value) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?, name = ?";

        final int BATCH_SIZE = 100;
        int batchCount = 0;

        try (Connection conn = connection();
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
        if (dataSource.isClosed()) return;
        dataSource.close();
    }
}
