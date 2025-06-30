package gg.auroramc.aurora.api.item;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemManager {
    private final List<RegisteredResolver> resolvers = new ArrayList<>();

    public record RegisteredResolver(String plugin, ItemResolver resolver, @Nullable Integer priority) {}

    public void registerResolver(String plugin, ItemResolver resolver) {
        int priority = Aurora.getLibConfig().getRawConfig().getInt("resolvers-priority." + plugin.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
        Aurora.logger().info("[Aurora] Hooked in resolver " + plugin + " with priority " + priority);
        registerResolver(plugin.toLowerCase(Locale.ROOT), resolver, priority);
    }

    public void registerResolver(Dep plugin, ItemResolver resolver) {
        String pluginId = plugin.getId().toLowerCase(Locale.ROOT);
        int priority = Aurora.getLibConfig().getRawConfig().getInt("resolvers-priority." + pluginId, Integer.MAX_VALUE);
        Aurora.logger().info("[Aurora] Hooked in resolver " + pluginId + " with priority " + priority);
        registerResolver(pluginId, resolver, priority);
    }

    public void registerResolver(String plugin, ItemResolver resolver, int priority) {
        resolvers.add(new RegisteredResolver(plugin.toLowerCase(Locale.ROOT), resolver, priority));
    }

    public void registerResolver(Dep plugin, ItemResolver resolver, int priority) {
        registerResolver(plugin.getId().toLowerCase(Locale.ROOT), resolver, priority);
    }

    public void unregisterResolver(String plugin) {
        resolvers.removeIf(r -> r.plugin().equalsIgnoreCase(plugin));
    }

    public @Nullable ItemResolver getResolver(String plugin) {
        return resolvers.stream()
                .filter(r -> r.plugin().equalsIgnoreCase(plugin))
                .findFirst()
                .map(RegisteredResolver::resolver)
                .orElse(null);
    }

    public TypeId resolveId(ItemStack item) {
        if (item.getType() == Material.AIR) {
            return TypeId.from(Material.AIR);
        }
        for (RegisteredResolver registered : resolvers) {
            var res = registered.resolver().oneStepMatch(item);
            if (res != null) return res;
        }
        return TypeId.from(item.getType());
    }

    public ItemStack resolveItem(TypeId typeId, @Nullable Player player) {
        if (typeId.namespace().equalsIgnoreCase("minecraft")) {
            return resolveVanilla(typeId);
        }

        return resolvers.stream()
                .filter(r -> r.plugin().equalsIgnoreCase(typeId.namespace()))
                .sorted(Comparator.comparingInt(r -> r.priority() != null ? r.priority() : Integer.MAX_VALUE))
                .map(r -> r.resolver().resolveItem(typeId.id(), player))
                .filter(item -> item != null && item.getType() != Material.AIR)
                .findFirst()
                .orElseGet(() -> resolveVanilla(typeId));
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
