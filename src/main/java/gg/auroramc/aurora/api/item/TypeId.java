package gg.auroramc.aurora.api.item;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record TypeId(String namespace, String id) {
    @Override
    public String toString() {
        return namespace + ":" + id;
    }

    public static TypeId fromString(String string) {
        String[] split = string.split(":");
        if (split.length != 2) {
            return TypeId.fromDefault(string);
        } else {
            return new TypeId(split[0], split[0].equalsIgnoreCase("minecraft") ? split[1].toLowerCase() : split[1]);
        }
    }

    public static TypeId fromDefault(String string) {
        String[] split = string.split(":");
        if (split.length == 1) {
            return new TypeId("minecraft", split[0].toLowerCase());
        } else if (split.length != 2) {
            throw new IllegalArgumentException("Invalid TypeId: " + string);
        } else {
            return new TypeId(split[0], split[1]);
        }
    }

    public static TypeId from(Material material) {
        return new TypeId("minecraft", material.name().toLowerCase());
    }

    public static TypeId from(EntityType entityType) {
        return new TypeId("minecraft", entityType.name().toUpperCase());
    }
}
