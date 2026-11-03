package gg.auroramc.aurora.internal.expansions.region;

import gg.auroramc.aurora.api.region.RegionManager;
import org.bukkit.Location;
import org.bukkit.block.Block;

// The block tracker is turned off: nothing is tracked
public final class NoRegions implements RegionManager {
    public static final NoRegions INSTANCE = new NoRegions();

    private NoRegions() {
    }

    @Override
    public boolean isPlacedBlock(Block block) {
        return false;
    }

    @Override
    public boolean isPlacedBlock(Location location) {
        return false;
    }

    @Override
    public void addPlacedBlock(Block block) {
    }

    @Override
    public void addPlacedBlock(Location location) {
    }

    @Override
    public void removePlacedBlock(Block block) {
    }

    @Override
    public void removePlacedBlock(Location location) {
    }
}
