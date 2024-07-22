package gg.auroramc.aurora.api.entity;

import org.bukkit.Location;

import java.util.Map;

public interface EntitySpawner {
    void spawn(Location location, Map<String, Object> args);
}
