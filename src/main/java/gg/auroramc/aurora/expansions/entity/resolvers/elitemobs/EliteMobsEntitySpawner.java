package gg.auroramc.aurora.expansions.entity.resolvers.elitemobs;

import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Map;

public class EliteMobsEntitySpawner implements EntitySpawner {
    private final String id;

    public EliteMobsEntitySpawner(String id) {
        this.id = id;
    }

    @Override
    public void spawn(Location location, Map<String, Object> args) {
        var boss = CustomBossEntity.createCustomBossEntity(id + ".yml");
        if (boss == null) return;
        Bukkit.getRegionScheduler().run(Aurora.getInstance(), location, (task) -> boss.spawn(location, true));
    }
}
