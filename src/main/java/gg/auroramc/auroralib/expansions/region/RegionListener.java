package gg.auroramc.auroralib.expansions.region;

import gg.auroramc.auroralib.AuroraLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.concurrent.TimeUnit;

public class RegionListener implements Listener {

    private final AuroraLib plugin;
    private final RegionExpansion regionExpansion;

    public RegionListener(AuroraLib plugin, RegionExpansion regionExpansion) {
        this.plugin = plugin;
        this.regionExpansion = regionExpansion;
        startSaveTimer();
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

    public void startSaveTimer() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                (task) -> regionExpansion.saveAllRegions(true), 300, 300, TimeUnit.SECONDS);
    }
}
