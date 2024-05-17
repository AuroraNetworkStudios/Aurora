package gg.auroramc.aurora.expansions.region.integrations;

import com.onarandombox.MultiverseCore.event.MVWorldDeleteEvent;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MVCore implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldDelete(MVWorldDeleteEvent e) {
        // Will be called everytime Multiverse-Core deletes or regenerates a world
        Aurora.getExpansionManager().getExpansion(RegionExpansion.class).clearWorld(e.getWorld().getName());
    }
}
