package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class YamlStorage implements UserStorage {
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
    public boolean saveUser(AuroraUser user, SaveReason reason) {
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
    public void purgeUser(UUID uuid) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if(file.exists()) {
            file.delete();
        }
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
