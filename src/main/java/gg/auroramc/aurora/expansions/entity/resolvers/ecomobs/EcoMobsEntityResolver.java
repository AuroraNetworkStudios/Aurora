package gg.auroramc.aurora.expansions.entity.resolvers.ecomobs;

import com.willfp.eco.core.entities.Entities;
import gg.auroramc.aurora.api.entity.EntityResolver;
import gg.auroramc.aurora.api.entity.EntitySpawner;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class EcoMobsEntityResolver implements EntityResolver {
    private final NamespacedKey namespacedKey = new NamespacedKey("ecomobs", "mob");

    @Override
    public boolean matches(Entity entity) {
        return entity.getPersistentDataContainer().has(namespacedKey);
    }

    @Override
    public TypeId resolveId(Entity entity) {
        String id = entity.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        return new TypeId("ecomobs", id);
    }

    @Override
    public EntitySpawner resolveEntitySpawner(String id, @Nullable Player player) {
        return new EcoMobsEntitySpawner(Entities.lookup("ecomobs:" + id));
    }
}
