package gg.auroramc.aurora.api.entity;

import gg.auroramc.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class VanillaEntitySpawner implements EntitySpawner {
    private final EntityType entityType;

    public VanillaEntitySpawner(EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public void spawn(Location location, Map<String, Object> args) {
        Bukkit.getRegionScheduler().run(Aurora.getInstance(), location, (task) -> {
            location.getWorld().spawnEntity(location, entityType);
        });
    }
}
