package gg.auroramc.aurora.expansions.entity.resolvers.elitemobs;

import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import gg.auroramc.aurora.api.entity.EntityResolver;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class EliteMobsResolver implements EntityResolver {
    @Override
    public boolean matches(Entity entity) {
        return EntityTracker.isEliteMob(entity);
    }

    @Override
    public TypeId resolveId(Entity entity) {
        var eliteEntity = EntityTracker.getEliteMobEntity(entity);
        if (eliteEntity instanceof CustomBossEntity boss) {
            return new TypeId("elitemobs", boss.getCustomBossesConfigFields().getFilename().replace(".yml", ""));
        }
        return new TypeId("elitemobs", entity.getType().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public EntitySpawner resolveEntitySpawner(String id, @Nullable Player player) {
        return new EliteMobsEntitySpawner(id);
    }
}
