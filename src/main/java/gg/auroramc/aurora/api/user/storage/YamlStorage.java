package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class YamlStorage implements UserStorage {
    public DirectoryStream<Path> getFileStream() throws IOException {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata");
        return Files.newDirectoryStream(file.toPath(), "*.yml");
    }


    @Override
    public void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler) {
        final var start = System.nanoTime();
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Aurora.logger().severe("Failed to create data file for player: " + uuid);
            }
        }
        var data = YamlConfiguration.loadConfiguration(file);
        var user = new AuroraUser(uuid);
        user.initData(data, dataHolders);
        final var end = System.nanoTime();
        Aurora.getUserManager().getLoadLatencyMeasure().addLatency(end - start);
        handler.accept(user);
    }

    @Override
    public AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Aurora.logger().severe("Failed to create data file for player: " + uuid);
            }
        }
        var data = YamlConfiguration.loadConfiguration(file);
        var user = new AuroraUser(uuid);
        user.initData(data, dataHolders);
        return user;
    }

    @Override
    public synchronized boolean saveUser(AuroraUser user, SaveReason reason) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", user.getUniqueId() + ".yml");

        try {
            final var start = System.nanoTime();
            user.serializeData().save(file);
            final var end = System.nanoTime();
            Aurora.getUserManager().getSaveLatencyMeasure().addLatency(end - start);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public int bulkSaveUsers(List<AuroraUser> users, SaveReason reason) {
        int saved = 0;
        for (var user : users) {
            var success = saveUser(user, reason);
            if (success) saved++;
        }
        return saved;
    }

    @Override
    public void purgeUser(UUID uuid) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
