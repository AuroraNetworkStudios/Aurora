package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.Aurora;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuroraUser {
    private final Map<Class<? extends UserDataHolder>, UserDataHolder> dataHolderMap = new ConcurrentHashMap<>();
    @Getter
    private YamlConfiguration configuration;
    @Getter
    private final UUID uuid;
    @Getter
    private final Object serializeLock = new Object();

    private final AtomicBoolean loaded = new AtomicBoolean(true);

    public AuroraUser(UUID uuid) {
        this.uuid = uuid;
    }

    public AuroraUser(UUID uuid, boolean isLoaded) {
        this.uuid = uuid;
        this.loaded.set(isLoaded);
    }

    public void initData(YamlConfiguration data, Set<Class<? extends UserDataHolder>> dataHolders) {
        synchronized (serializeLock) {
            this.configuration = data;
            for(var holderClass : dataHolders) {
                try {
                    var holder = holderClass.getDeclaredConstructor().newInstance();
                    holder.setUser(this);
                    if(loaded.get() && data != null) {
                        holder.initFrom(data.getConfigurationSection(holder.getId().toString()));
                    }
                    dataHolderMap.put(holderClass, holder);
                } catch (Exception e) {
                    Aurora.logger().warning("Failed to initialize data holder: " + holderClass.getSimpleName() + " error: " + e.getMessage());
                }
            }
        }
    }

    public <T extends DataHolder> T getData(Class<T> holderClass) {
        // Refresh access timer
        Aurora.getUserManager().isUserCached(uuid);

        if(dataHolderMap.containsKey(holderClass)) {
            return holderClass.cast(dataHolderMap.get(holderClass));
        }

        return null;
    }

    public UserMetaHolder getMetaData() {
        return getData(UserMetaHolder.class);
    }

    public YamlConfiguration serializeData() {
        synchronized (serializeLock) {
            for(var holder : dataHolderMap.values()) {
                holder.serializeInto(getOrCreateSection(holder.getId().toString()));
            }
            return configuration;
        }
    }

    public Collection<UserDataHolder> getDataHolders() {
        return dataHolderMap.values();
    }

    private ConfigurationSection getOrCreateSection(String path) {
        if(configuration.getConfigurationSection(path) != null) {
            return configuration.getConfigurationSection(path);
        }
        return configuration.createSection(path);
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void setLoaded(boolean loaded) {
        this.loaded.set(loaded);
    }

    public boolean isDirty() {
        return dataHolderMap.values().stream().anyMatch(UserDataHolder::isDirty);
    }
}