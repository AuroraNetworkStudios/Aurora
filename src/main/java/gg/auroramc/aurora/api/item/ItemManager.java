package gg.auroramc.aurora.api.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ItemManager {
    private final Map<String, ItemResolver> resolvers = new LinkedHashMap<>();

    public void registerResolver(String plugin, ItemResolver resolver) {
        resolvers.put(plugin, resolver);
    }

    public void registerResolver(Dep plugin, ItemResolver resolver) {
        resolvers.put(plugin.getId().toLowerCase(Locale.ROOT), resolver);
    }

    public ItemResolver getResolver(String plugin) {
        return resolvers.get(plugin);
    }

    public void unregisterResolver(String plugin) {
        resolvers.remove(plugin.toLowerCase(Locale.ROOT));
    }

    public TypeId resolveId(ItemStack item) {
        if (item.getType() == Material.AIR) {
            return TypeId.from(Material.AIR);
        }
        for (ItemResolver resolver : resolvers.values()) {
            var res = resolver.oneStepMatch(item);
            if (res != null) {
                return res;
            }
        }
        return TypeId.from(item.getType());
    }

    public ItemStack resolveItem(TypeId typeId, @Nullable Player player) {
        if (typeId.namespace().equalsIgnoreCase("minecraft")) {
            return resolveVanilla(typeId);
        }

        for (var resolver : resolvers.entrySet()) {
            if (resolver.getKey().equalsIgnoreCase(typeId.namespace())) {
                return resolver.getValue().resolveItem(typeId.id(), player);
            }
        }

        return resolveVanilla(typeId);
    }

    private ItemStack resolveVanilla(TypeId typeId) {
        try {
            return new ItemStack(Material.valueOf(typeId.id().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            Aurora.logger().warning("Failed to resolve item: " + typeId + " using AIR instead.");
            return new ItemStack(Material.AIR);
        }

    }

    public ItemStack resolveItem(TypeId typeId) {
        return resolveItem(typeId, null);
    }
}
