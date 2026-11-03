package gg.auroramc.aurora.api.region;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Tracks blocks placed by players, so breaking them doesn't count as gathering (block-tracker in Aurora's config). With
 * the tracker off, no block counts as placed.
 */
public interface RegionManager {
    boolean isPlacedBlock(Block block);

    boolean isPlacedBlock(Location location);

    void addPlacedBlock(Block block);

    void addPlacedBlock(Location location);

    void removePlacedBlock(Block block);

    void removePlacedBlock(Location location);
}
