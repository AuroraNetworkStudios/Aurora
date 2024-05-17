package gg.auroramc.auroralib.expansions.region;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class RegionListener implements Listener {

    private final RegionExpansion regionExpansion;

    public RegionListener(RegionExpansion regionExpansion) {
        this.regionExpansion = regionExpansion;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int regionX = (int) Math.floor((double) chunk.getX() / 32.0);
        int regionZ = (int) Math.floor((double) chunk.getZ() / 32.0);
        RegionCoordinate regionCoordinate = new RegionCoordinate(event.getWorld(), regionX, regionZ);
        Region obtainedRegion = regionExpansion.getRegion(regionCoordinate);
        if (obtainedRegion == null) {
            obtainedRegion = new Region(event.getWorld(), regionX, regionZ);
            regionExpansion.loadRegion(obtainedRegion);
        }
        // When people pruning chunks by hand, we need to clear the chunk data
        if (event.isNewChunk()) {
            regionExpansion.clearChunk(event.getChunk());
        }
    }
}
