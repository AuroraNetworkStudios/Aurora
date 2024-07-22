package gg.auroramc.aurora.expansions.entity.resolvers.ecomobs;

import com.willfp.eco.core.entities.TestableEntity;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class EcoMobsEntitySpawner implements EntitySpawner {
    private final TestableEntity entityType;

    public EcoMobsEntitySpawner(TestableEntity entityType) {
        this.entityType = entityType;
    }

    @Override
    public void spawn(Location location, Map<String, Object> args) {
        if(entityType == null) {
            Aurora.logger().warning("Failed to spawn entity, because eco entity is null");
            return;
        }
        Bukkit.getRegionScheduler().run(Aurora.getInstance(), location, (task) -> entityType.spawn(location));
    }
}
