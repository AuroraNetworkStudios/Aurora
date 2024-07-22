package gg.auroramc.aurora.api.entity;

import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface EntityResolver {
    boolean matches(Entity entity);

    TypeId resolveId(Entity entity);

    EntitySpawner resolveEntitySpawner(String id, @Nullable Player player);

    default EntitySpawner resolveEntitySpawner(String id) {
        return resolveEntitySpawner(id, null);
    }
}
