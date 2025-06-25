package gg.auroramc.aurora.expansions.region;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

public class RegionExpansion implements AuroraExpansion {
    private NamespacedKey createKey(Block block) {
        var loc = Integer.toHexString(block.getLocation().hashCode());
        return new NamespacedKey(Aurora.getInstance(), loc);
    }

    private NamespacedKey createKey(Location location) {
        var loc = Integer.toHexString(location.hashCode());
        return new NamespacedKey(Aurora.getInstance(), loc);
    }

    public boolean isPlacedBlock(Block block) {
        return block.getChunk().getPersistentDataContainer().has(createKey(block));
    }

    public boolean isPlacedBlock(Location location) {
        return location.getChunk().getPersistentDataContainer().has(createKey(location));
    }

    public void addPlacedBlock(Block block) {
        block.getChunk().getPersistentDataContainer().set(createKey(block), PersistentDataType.BYTE, (byte) 1);
    }

    public void addPlacedBlock(Location location) {
        location.getChunk().getPersistentDataContainer().set(createKey(location), PersistentDataType.BYTE, (byte) 1);
    }

    public void removePlacedBlock(Block block) {
        block.getChunk().getPersistentDataContainer().remove(createKey(block));
    }

    @Override
    public void hook() {
        var plugin = Aurora.getInstance();
        Bukkit.getPluginManager().registerEvents(new RegionBlockListener(plugin, this), plugin);
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
