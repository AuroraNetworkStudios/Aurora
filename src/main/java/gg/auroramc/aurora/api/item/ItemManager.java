package gg.auroramc.aurora.api.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ItemManager {
    private final List<RegisteredResolver> resolvers = new CopyOnWriteArrayList<>();
    private final Map<String, RegisteredResolver> resolverMap = new ConcurrentHashMap<>();

    public record RegisteredResolver(String plugin, ItemResolver resolver, Integer priority) {
    }

    public void registerResolver(String plugin, ItemResolver resolver) {
        String pluginId = plugin.toLowerCase(Locale.ROOT);
        int priority = Aurora.getLibConfig().getItemResolverPriorities().getOrDefault(pluginId, 0);
        registerResolver(pluginId, resolver, priority);
    }

    public void registerResolver(Dep plugin, ItemResolver resolver) {
        String pluginId = plugin.getId().toLowerCase(Locale.ROOT);
        int priority = Aurora.getLibConfig().getItemResolverPriorities().getOrDefault(pluginId, 0);
        registerResolver(pluginId, resolver, priority);
    }

    public void registerResolver(String plugin, ItemResolver resolver, int priority) {
        String pluginId = plugin.toLowerCase(Locale.ROOT);
        insertSorted(new RegisteredResolver(pluginId, resolver, priority));
        Aurora.logger().info("Registered item resolver " + pluginId + " with priority " + priority);
    }

    public void registerResolver(Dep plugin, ItemResolver resolver, int priority) {
        registerResolver(plugin.getId().toLowerCase(Locale.ROOT), resolver, priority);
    }

    public void unregisterResolver(String plugin) {
        resolvers.removeIf(r -> r.plugin().equalsIgnoreCase(plugin));
        resolverMap.remove(plugin);
    }

    public @Nullable ItemResolver getResolver(String plugin) {
        return resolverMap.get(plugin).resolver;
    }

    public TypeId resolveId(ItemStack item) {
        if (item.getType() == Material.AIR) {
            return TypeId.from(Material.AIR);
        }

        for (RegisteredResolver r : resolvers) {
            try {
                TypeId res = r.resolver().oneStepMatch(item);
                if (res != null) return res;
            } catch (IncompatibleClassChangeError | NoClassDefFoundError e) {
                Aurora.logger().severe("Failed to resolve item id using resolver: " + r.plugin() + ", removing resolver!");
                Aurora.logger().severe(r.resolver().isPluginEnabled() ? "Integration is probably outdated!" : ("Plugin: " + r.plugin() + " is disabled, check your startup logs!"));
                Aurora.logger().severe(e.getClass().getSimpleName() + ": " + e.getMessage());
                unregisterResolver(r.plugin());
            }
        }
        return TypeId.from(item.getType());
    }

    public ItemStack resolveItem(TypeId typeId, @Nullable Player player) {
        if (typeId.namespace().equalsIgnoreCase("minecraft")) {
            return resolveVanilla(typeId);
        }

        if (resolverMap.containsKey(typeId.namespace())) {
            var r = resolverMap.get(typeId.namespace());
            try {
                var item = r.resolver().resolveItem(typeId.id(), player);
                if (item != null && item.getType() != Material.AIR) {
                    return item;
                }
            } catch (IncompatibleClassChangeError | NoClassDefFoundError e) {
                Aurora.logger().severe("Failed to resolve item: " + typeId + " using resolver: " + r.plugin() + ", removing resolver!");
                Aurora.logger().severe(r.resolver().isPluginEnabled() ? "Integration is probably outdated!" : ("Plugin: " + r.plugin() + " is disabled, check your startup logs!"));
                Aurora.logger().severe(e.getClass().getSimpleName() + ": " + e.getMessage());
                unregisterResolver(r.plugin());
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

    public List<RegisteredResolver> getResolvers() {
        return Collections.unmodifiableList(this.resolvers);
    }

    private void insertSorted(RegisteredResolver newResolver) {
        resolverMap.put(newResolver.plugin(), newResolver);

        for (int i = 0; i < resolvers.size(); i++) {
            int existingPriority = resolvers.get(i).priority();
            int newPriority = newResolver.priority();

            if (newPriority > existingPriority) {
                resolvers.add(i, newResolver);
                return;
            }
        }

        resolvers.add(newResolver);
    }
}
