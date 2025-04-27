package gg.auroramc.aurora.api.entity;

import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class VanillaEntityResolver implements EntityResolver {

    @Override
    public boolean matches(Entity entity) {
        return true;
    }

    @Override
    public TypeId resolveId(Entity entity) {
        return TypeId.from(entity.getType());
    }

    @Override
    public EntitySpawner resolveEntitySpawner(String id, @Nullable Player player) {
        return new VanillaEntitySpawner(EntityType.valueOf(id.toUpperCase(Locale.ROOT)));
    }
}
