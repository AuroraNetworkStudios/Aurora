package gg.auroramc.aurora.api.item;

import gg.auroramc.aurora.api.dependency.Dep;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemManager {
    private final Map<String, ItemResolver> resolvers = new LinkedHashMap<>();

    public void registerResolver(String plugin, ItemResolver resolver) {
        resolvers.put(plugin, resolver);
    }

    public void registerResolver(Dep plugin, ItemResolver resolver) {
        resolvers.put(plugin.getId().toLowerCase(), resolver);
    }

    public ItemResolver getResolver(String plugin) {
        return resolvers.get(plugin);
    }

    public void unregisterResolver(String plugin) {
        resolvers.remove(plugin.toLowerCase());
    }

    public TypeId resolveId(ItemStack item) {
        for (ItemResolver resolver : resolvers.values()) {
            if (resolver.matches(item)) {
                return resolver.resolveId(item);
            }
        }
        return TypeId.from(item.getType());
    }

    public ItemStack resolveItem(TypeId typeId, @Nullable Player player) {
        if (typeId.namespace().equalsIgnoreCase("minecraft"))
            return new ItemStack(Material.valueOf(typeId.id().toUpperCase()));

        for (var resolver : resolvers.entrySet()) {
            if (resolver.getKey().equalsIgnoreCase(typeId.namespace())) {
                return resolver.getValue().resolveItem(typeId.id(), player);
            }
        }
        return new ItemStack(Material.valueOf(typeId.id().toUpperCase()));
    }

    public ItemStack resolveItem(TypeId typeId) {
        return resolveItem(typeId, null);
    }
}