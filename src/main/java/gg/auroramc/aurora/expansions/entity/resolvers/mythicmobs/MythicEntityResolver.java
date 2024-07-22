package gg.auroramc.aurora.expansions.entity.resolvers.mythicmobs;

import gg.auroramc.aurora.api.entity.EntityResolver;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import gg.auroramc.aurora.api.item.TypeId;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MythicEntityResolver implements EntityResolver {

    @Override
    public boolean matches(Entity entity) {
        return MythicBukkit.inst().getMobManager().isMythicMob(entity);
    }

    @Override
    public TypeId resolveId(Entity entity) {
        var id = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity).getType().getInternalName();
        return new TypeId("mythicmobs", id);
    }

    @Override
    public EntitySpawner resolveEntitySpawner(String id, @Nullable Player player) {
        return new MythicEntitySpawner(id);
    }
}
