package gg.auroramc.aurora.hooks;

import com.bgsoftware.wildtools.api.events.BuilderWandUseEvent;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.region.RegionExpansion;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WildToolsHook implements Listener {
    public static void hook() {
        var expansion = Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
        if (expansion != null) {
            Bukkit.getPluginManager().registerEvents(new WildToolsHook(), Aurora.getInstance());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBuildWandUse(BuilderWandUseEvent event) {
        var expansion = Aurora.getExpansionManager().getExpansion(RegionExpansion.class);
        if (expansion == null) return;

        for (var location : event.getBlocks()) {
            expansion.addPlacedBlock(location);
        }
    }
}
