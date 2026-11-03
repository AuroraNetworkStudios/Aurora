package gg.auroramc.aurora.api.user.migration;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.UserManager;
import gg.auroramc.aurora.api.user.storage.SaveReason;
import gg.auroramc.aurora.api.user.storage.YamlStorage;
import gg.auroramc.aurora.api.user.storage.sql.MySqlStorage;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StorageMigrator {
    private final UserManager userManager;
    private final AtomicBoolean migrating = new AtomicBoolean(false);

    public StorageMigrator(UserManager userManager) {
        this.userManager = userManager;
    }

    public boolean migrateUserData(MySqlStorage from, YamlStorage to, int threadCount) {
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            final AtomicLong counter = new AtomicLong(0);
            final AtomicLong failed = new AtomicLong(0);
            int batchSize = 1000;
            var after = "";

            Aurora.logger().info("Starting migration of user data from MySQL to Yaml storage with thread count: " + threadCount);

            while (true) {
                var uuids = from.getUserIdsAfter(after, batchSize);
                if (uuids.isEmpty()) {
                    break;
                }
                after = uuids.getLast();
                for (var uuid : uuids) {
                    executor.submit(() -> {
                        var parsedId = UUID.fromString(uuid);
                        var loadedUser = from.loadUser(parsedId, userManager.getDataHolders());
                        // Saving data that couldn't be loaded would replace their real data with nothing
                        if (!loadedUser.isLoaded()) {
                            failed.incrementAndGet();
                            Aurora.logger().severe("Failed to load the data of " + parsedId + " to migrate it");
                            return;
                        }
                        loadedUser.getDataHolders().forEach(dataHolder -> dataHolder.setDirty(true));
                        to.purgeUser(parsedId);
                        if (!to.saveUser(loadedUser, SaveReason.CUSTOM)) {
                            failed.incrementAndGet();
                            Aurora.logger().severe("Failed to save the migrated data of " + parsedId);
                            return;
                        }
                        var currentCount = counter.incrementAndGet();
                        Aurora.logger().info("[" + currentCount + "] Migrated user data for " + parsedId + " with name: " + Bukkit.getOfflinePlayer(parsedId).getName());
                    });
                }

            }

            try {
                executor.shutdown();
                var success = executor.awaitTermination(1, TimeUnit.HOURS);
                if (success && failed.get() > 0) {
                    Aurora.logger().severe("Failed to migrate the data of " + failed.get() + " users (see the errors above), not switching to the new storage. " + counter.get() + " users were migrated.");
                    return false;
                }
                if (success) {
                    Aurora.logger().info("Successfully migrated all user data for: " + counter.get() + " users.");
                } else {
                    Aurora.logger().severe("Failed to migrate all user data, but " + counter.get() + " users were migrated.");
                }
                return success;
            } catch (InterruptedException e) {
                Aurora.logger().severe("Thread interupted while waiting for migration to finish: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            Aurora.logger().severe("Failed to migrate: " + e.getMessage());
            return false;
        } finally {
            // Also when listing the players failed partway, the workers mustn't be left running
            executor.shutdownNow();
        }
    }

    public boolean migrateUserData(YamlStorage from, MySqlStorage to, int threadCount) {
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            final AtomicLong counter = new AtomicLong(0);
            final AtomicLong failed = new AtomicLong(0);

            Aurora.logger().info("Starting migration of user data from Yaml to MySQL storage with thread count: " + threadCount);

            var pathStream = from.getFileStream();

            for (Path path : pathStream) {
                executor.submit(() -> {
                    var uuid = UUID.fromString(path.getFileName().toString().replace(".yml", ""));
                    var loadedUser = from.loadUser(uuid, userManager.getDataHolders());
                    // Saving data that couldn't be loaded would replace their real data with nothing
                    if (!loadedUser.isLoaded()) {
                        failed.incrementAndGet();
                        Aurora.logger().severe("Failed to load the data of " + uuid + " to migrate it");
                        return;
                    }
                    loadedUser.getDataHolders().forEach(dataHolder -> dataHolder.setDirty(true));
                    to.purgeUser(uuid);
                    if (!to.saveUser(loadedUser, SaveReason.CUSTOM)) {
                        failed.incrementAndGet();
                        Aurora.logger().severe("Failed to save the migrated data of " + uuid);
                        return;
                    }
                    var currentCount = counter.incrementAndGet();
                    Aurora.logger().info("[" + currentCount + "] Migrated user data for " + uuid + " with name: " + Bukkit.getOfflinePlayer(uuid).getName());
                });
            }

            try {
                executor.shutdown();
                var success = executor.awaitTermination(1, TimeUnit.HOURS);
                if (success && failed.get() > 0) {
                    Aurora.logger().severe("Failed to migrate the data of " + failed.get() + " users (see the errors above), not switching to the new storage. " + counter.get() + " users were migrated.");
                    return false;
                }
                if (success) {
                    Aurora.logger().info("Successfully migrated all user data for: " + counter.get() + " users.");
                } else {
                    Aurora.logger().severe("Failed to migrate all user data, but " + counter.get() + " users were migrated.");
                }
                return success;
            } catch (InterruptedException e) {
                Aurora.logger().severe("Thread interupted while waiting for migration to finish: " + e.getMessage());
            } finally {
                pathStream.close();
            }
            return false;
        } catch (IOException e) {
            Aurora.logger().severe("Failed to get file stream for Yaml storage: " + e.getMessage());
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    public boolean isMigrating() {
        return migrating.get();
    }

    public void markMigrating() {
        migrating.set(true);
    }

    public void markFinished() {
        migrating.set(false);
    }
}
