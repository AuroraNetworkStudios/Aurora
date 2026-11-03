package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class YamlStorage implements UserStorage {
    public DirectoryStream<Path> getFileStream() throws IOException {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata");
        // Nobody saved yet
        file.mkdirs();
        return Files.newDirectoryStream(file.toPath(), "*.yml");
    }


    @Override
    public void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler) {
        final var start = System.nanoTime();
        var user = loadUser(uuid, dataHolders);
        final var end = System.nanoTime();
        Aurora.getUserManager().getLoadLatencyMeasure().addLatency(end - start);
        handler.accept(user);
    }

    @Override
    public AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        var file = getFile(uuid);
        var data = new YamlConfiguration();
        if (file.exists()) {
            try {
                data.load(file);
            } catch (InvalidConfigurationException e) {
                // A broken file would otherwise load as empty, and the next save would make that permanent
                var broken = new File(file.getParentFile(), file.getName() + ".broken-" + System.currentTimeMillis());
                Aurora.logger().severe("The data file of player " + uuid + " is broken, it's moved to " + broken.getName()
                        + " and they start with empty data. Fix that file and put it back while the server is stopped to restore it. Error: " + e.getMessage());
                if (!file.renameTo(broken)) {
                    Aurora.logger().severe("Failed to move the broken data file of player " + uuid + ", not loading them so it isn't overwritten");
                    return notLoaded(uuid, dataHolders);
                }
                data = new YamlConfiguration();
            } catch (IOException e) {
                Aurora.logger().severe("Failed to read the data file of player " + uuid + " error: " + e.getMessage());
                return notLoaded(uuid, dataHolders);
            }
        }
        var user = new AuroraUser(uuid);
        user.initData(data, dataHolders);
        return user;
    }

    private AuroraUser notLoaded(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        var user = new AuroraUser(uuid, false);
        user.initData(null, dataHolders);
        return user;
    }

    private File getFile(UUID uuid) {
        return new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
    }

    @Override
    public boolean saveUser(AuroraUser user, SaveReason reason) {
        return prepareSave(user, reason).write();
    }

    @Override
    public PendingSave prepareSave(AuroraUser user, SaveReason reason) {
        var uuid = user.getUniqueId();
        // The whole file is written, so every holder counts as saved. A change made meanwhile marks it again.
        var taken = new ArrayList<UserDataHolder>();
        String data;
        try {
            synchronized (user.getSerializeLock()) {
                for (var holder : user.getDataHolders()) {
                    if (holder.clearDirty()) taken.add(holder);
                }
                data = user.serializeToString();
            }
        } catch (Exception e) {
            taken.forEach(holder -> holder.setDirty(true));
            Aurora.logger().severe("Failed to save user data for player: " + uuid + " error: " + e);
            return () -> false;
        }
        return () -> {
            if (write(uuid, data)) return true;
            taken.forEach(holder -> holder.setDirty(true));
            return false;
        };
    }

    private synchronized boolean write(UUID uuid, String data) {
        var file = getFile(uuid);

        try {
            final var start = System.nanoTime();
            file.getParentFile().mkdirs();
            // Written next to it and then moved over it, so a crash while writing can't leave a half written file
            var temp = new File(file.getParentFile(), file.getName() + ".tmp");
            Files.writeString(temp.toPath(), data, StandardCharsets.UTF_8);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            final var end = System.nanoTime();
            Aurora.getUserManager().getSaveLatencyMeasure().addLatency(end - start);
            return true;
        } catch (Exception e) {
            Aurora.logger().severe("Failed to save user data for player: " + uuid + " error: " + e);
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
