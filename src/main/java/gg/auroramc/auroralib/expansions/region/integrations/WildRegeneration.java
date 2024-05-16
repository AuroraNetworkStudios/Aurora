package gg.auroramc.auroralib.expansions.region.integrations;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.expansions.region.RegionExpansion;
import me.angeschossen.wildregeneration.api.events.chunk.ChunkRegenerateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WildRegeneration implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkRegen(ChunkRegenerateEvent e) {
        // Will be called everytime WildRegeneration regenerates a chunk
        AuroraLib.getExpansionManager().getExpansion(RegionExpansion.class)
                .clearChunk(e.getChunk().getWorld().getWorld().getName(), e.getChunk().getX(), e.getChunk().getZ());
    }
}
