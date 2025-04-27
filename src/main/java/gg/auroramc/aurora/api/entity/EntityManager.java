package gg.auroramc.aurora.api.entity;


import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EntityManager {
    private final VanillaEntityResolver vanillaEntityResolver = new VanillaEntityResolver();
    private final Map<String, EntityResolver> resolvers = new LinkedHashMap<>();

    public void registerResolver(String plugin, EntityResolver resolver) {
        resolvers.put(plugin, resolver);
    }

    public void registerResolver(Dep plugin, EntityResolver resolver) {
        resolvers.put(plugin.getId().toLowerCase(Locale.ROOT), resolver);
    }

    public EntityResolver getResolver(String plugin) {
        return resolvers.get(plugin);
    }

    public void unregisterResolver(String plugin) {
        resolvers.remove(plugin.toLowerCase(Locale.ROOT));
    }

    public TypeId resolveId(Entity entity) {
        for (EntityResolver resolver : resolvers.values()) {
            if (resolver.matches(entity)) {
                return resolver.resolveId(entity);
            }
        }
        return TypeId.from(entity.getType());
    }

    public EntitySpawner resolveEntitySpawner(TypeId typeId, @Nullable Player player) {
        if (typeId.namespace().equalsIgnoreCase("minecraft"))
            return vanillaEntityResolver.resolveEntitySpawner(typeId.id(), player);

        for (var resolver : resolvers.entrySet()) {
            if (resolver.getKey().equalsIgnoreCase(typeId.namespace())) {
                return resolver.getValue().resolveEntitySpawner(typeId.id(), player);
            }
        }
        return vanillaEntityResolver.resolveEntitySpawner(typeId.id(), player);
    }

    public EntitySpawner resolveEntitySpawner(TypeId typeId) {
        return resolveEntitySpawner(typeId, null);
    }
}
