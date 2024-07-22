package gg.auroramc.aurora.expansions.entity.resolvers.mythicmobs;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class MythicEntitySpawner implements EntitySpawner {
    private final String id;

    public MythicEntitySpawner(String id) {
        this.id = id;
    }

    @Override
    public void spawn(Location location, Map<String, Object> args) {
        Bukkit.getRegionScheduler().run(Aurora.getInstance(), location, (task) -> {
            var level = args.containsKey("level") ? (double) args.get("level") : 1.0;
            var mob = MythicBukkit.inst().getMobManager().spawnMob(id, location, level);
            if (mob == null) {
                Aurora.logger().warning("Failed to spawn mythic mob with id " + id);
            }
        });
    }
}
