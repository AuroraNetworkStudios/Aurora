package gg.auroramc.auroralib.api.user;

import gg.auroramc.auroralib.AuroraLib;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuroraUser {
    private final Map<Class<? extends UserDataHolder>, UserDataHolder> dataHolderMap = new HashMap<>();
    @Getter
    private YamlConfiguration configuration;
    @Getter
    private final UUID uuid;
    @Getter
    @Setter
    private boolean loaded = true;

    public AuroraUser(UUID uuid) {
        this.uuid = uuid;
    }

    public AuroraUser(UUID uuid, boolean isLoaded) {
        this.uuid = uuid;
        this.loaded = isLoaded;
    }

    public void initData(YamlConfiguration data, Set<Class<? extends UserDataHolder>> dataHolders) {
        this.configuration = data;
        for(var holderClass : dataHolders) {
            try {
                var holder = holderClass.getDeclaredConstructor().newInstance();
                holder.setUser(this);
                if(loaded) {
                    holder.initFrom(data.getConfigurationSection(holder.getId()));
                }
                dataHolderMap.put(holderClass, holder);
            } catch (Exception e) {
                AuroraLib.logger().warning("Failed to initialize data holder: " + holderClass.getSimpleName() + " error: " + e.getMessage());
            }
        }
    }

    public <T extends DataHolder> T getData(Class<T> holderClass) {
        // Refresh access timer
        AuroraLib.getUserManager().isUserCached(uuid);

        if(dataHolderMap.containsKey(holderClass)) {
            return holderClass.cast(dataHolderMap.get(holderClass));
        }

        return null;
    }

    public YamlConfiguration serializeData() {
        for(var holder : dataHolderMap.values()) {
            holder.serializeInto(getOrCreateSection(holder.getId()));
        }
        return configuration;
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
}
