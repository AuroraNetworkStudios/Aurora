package gg.auroramc.auroralib.api.user.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.user.AuroraUser;
import gg.auroramc.auroralib.api.user.DataHolder;
import gg.auroramc.auroralib.api.user.UserDataHolder;
import gg.auroramc.auroralib.api.user.storage.SaveReason;
import gg.auroramc.auroralib.api.user.storage.UserStorage;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MySqlStorage implements UserStorage {
    private final HikariDataSource dataSource;
    private final DatabaseCredentials credentials;
    private final String tableName = "auroralib_user_data";
    private final String syncTableName = "auroralib_sync";
    private final int networkLatency;
    private final int syncRetryCount;

    @SneakyThrows
    public MySqlStorage() {
        int poolSize = AuroraLib.getInstance().getConfig().getInt("mysql.pool-size", 10);
        networkLatency = AuroraLib.getInstance().getConfig().getInt("mysql.network-latency", 500);
        syncRetryCount = AuroraLib.getInstance().getConfig().getInt("mysql.sync-retry-count", 3);
        credentials = readCredentials();
        HikariConfig config = new HikariConfig();
        config.setPoolName("AuroraCore-pool");
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
        Bukkit.getAsyncScheduler().runDelayed(AuroraLib.getInstance(), (task) -> {
            try {
                try (Connection connection = connection()) {
                    if (Bukkit.getPlayer(uuid) == null) {
                        AuroraLib.logger().debug("Player: " + uuid + " is left, aborting load.");
                        return;
                    }
                    if (count <= 0) {
                        AuroraLib.logger().debug("We are still in sync lock after " + syncRetryCount + " retry for player: " + uuid + ". We won't wait anymore. Loading form database...");
                        var user = loadUserForReal(connection, uuid, dataHolders);
                        if (user == null) return;
                        handler.accept(user);
                        createSyncFlag(uuid, connection);
                        AuroraLib.logger().debug("Player: " + uuid + " loaded from database.");
                        return;
                    }

                    if (!isLocked(uuid, connection)) {
                        var user = loadUserForReal(connection, uuid, dataHolders);
                        if (user == null) return;
                        handler.accept(user);
                        createSyncFlag(uuid, connection);
                        AuroraLib.logger().debug("Player: " + uuid + " loaded from database.");
                    } else {
                        AuroraLib.logger().debug("Sync lock detected for player: " + uuid + ", retrying...");
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

                var rawYaml = resultSet.getString("data");
                var user = new AuroraUser(uuid);
                var config = new YamlConfiguration();
                config.loadFromString(rawYaml);
                user.initData(config, dataHolders);
                final var end = System.nanoTime();
                AuroraLib.getUserManager().getLoadLatencyMeasure().addLatency(end - start);
                return user;
            }
        } catch (Exception e) {
            AuroraLib.logger().severe("Failed to load user data for player: " + uuid);
            return null;
        }
    }

    @Override
    public boolean saveUser(AuroraUser user, SaveReason reason) {
        var uuid = user.getUniqueId();
        String saveQuery = "INSERT INTO " + tableName + " (player_uuid, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data=?;";

        try {
            final var start = System.nanoTime();
            try (Connection connection = connection()) {
                try (PreparedStatement statement = connection.prepareStatement(saveQuery)) {
                    var data = user.serializeData().saveToString();
                    statement.setString(1, uuid.toString());
                    statement.setString(2, data);
                    statement.setString(3, data);
                    statement.executeUpdate();
                }
                final var end = System.nanoTime();
                AuroraLib.getUserManager().getSaveLatencyMeasure().addLatency(end - start);
                if (reason == SaveReason.QUIT) {
                    removeSyncFlag(uuid, connection);
                }
                return true;
            }
        } catch (Exception e) {
            AuroraLib.logger().severe("Failed to save user data for player: " + uuid);
            return false;
        }
    }

    @Override
    @SneakyThrows
    public void walkUserData(Consumer<AuroraUser> callback, Set<Class<? extends UserDataHolder>> dataHolders) {
        String loadQuery = "SELECT * FROM " + tableName + ";";

        try (Connection connection = connection()) {
            try (PreparedStatement statement = connection.prepareStatement(loadQuery)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return;
                    }

                    var uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    var rawYaml = resultSet.getString("data");
                    var user = new AuroraUser(uuid);
                    var config = new YamlConfiguration();
                    config.loadFromString(rawYaml);
                    user.initData(config, dataHolders);

                    callback.accept(user);
                }
            }
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
            AuroraLib.getUserManager().getSyncFlagLatencyMeasure().addLatency(end - start);
        } catch (SQLException e) {
            AuroraLib.logger().warning("Failed to add sync flag for player: " + uuid);
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
            AuroraLib.logger().warning("Failed to remove sync flag for player: " + uuid);
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
            AuroraLib.logger().warning("Error checking sync lock status for player: " + uuid);
            return false;
        }
    }

    private DatabaseCredentials readCredentials() {
        var config = AuroraLib.getInstance().getConfig();
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
                AuroraLib.getInstance().getResource("database/schema.sql").readAllBytes(),
                StandardCharsets.UTF_8)
                .replaceAll("%user_table%", tableName)
                .replaceAll("%sync_table%", syncTableName)
                .split(";");
    }
}
