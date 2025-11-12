package gg.auroramc.aurora.expansions.worldguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.events.region.PlayerRegionEnterEvent;
import gg.auroramc.aurora.api.events.region.PlayerRegionLeaveEvent;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class WorldGuardExpansion implements AuroraExpansion, Listener {

    private final Map<UUID, Set<ProtectedRegion>> previousRegions = new HashMap<>();
    private final Map<UUID, Location> teleportMoves = new HashMap<>();

    protected void onRegionUpdate(UUID playerUUID, Set<ProtectedRegion> newRegions) {
        var player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        var oldRegions = previousRegions.get(playerUUID);
        var regions = new ArrayList<>(newRegions);
        if (regions.isEmpty()) {

            if (oldRegions != null && !oldRegions.isEmpty()) {
                previousRegions.remove(playerUUID);
                Bukkit.getPluginManager().callEvent(new PlayerRegionLeaveEvent(player, oldRegions.stream().toList()));
            }
            return;
        }

        var enterRegions = newRegions.stream().filter(r -> oldRegions == null || !oldRegions.contains(r)).toList();
        var leaveRegions = oldRegions == null ? null : oldRegions.stream().filter(r -> !newRegions.contains(r)).toList();

        previousRegions.put(playerUUID, newRegions);

        if (leaveRegions != null && !leaveRegions.isEmpty()) {
            Bukkit.getPluginManager().callEvent(new PlayerRegionLeaveEvent(player, leaveRegions));
        }
        if (!enterRegions.isEmpty()) {
            Bukkit.getPluginManager().callEvent(new PlayerRegionEnterEvent(player, enterRegions));
        }
    }

    protected void addTeleportMove(UUID playerUUID, Location location) {
        teleportMoves.put(playerUUID, location);
    }

    protected void removeTeleportMove(UUID playerUUID) {
        teleportMoves.remove(playerUUID);
    }

    protected boolean checkFreeMoveAfterTeleport(UUID playerUUID, Location location) {
        if (teleportMoves.containsKey(playerUUID)) {
            var oldLocation = teleportMoves.get(playerUUID);
            removeTeleportMove(playerUUID);
            if (oldLocation == null) return false;
            if (!oldLocation.isWorldLoaded()) return false;
            if (oldLocation.getWorld() != location.getWorld()) return false;
            return oldLocation.distance(location) <= 5;
        }
        return false;
    }

    @Override
    public void hook() {
        WorldGuardEntryAndLeaveHandler.Factory handler = WorldGuardEntryAndLeaveHandler.FACTORY(this);
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(handler, null);
    }

    @Override
    public boolean canHook() {
        return DependencyManager.hasDep(Dep.WORLDGUARD);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        previousRegions.remove(e.getPlayer().getUniqueId());
        teleportMoves.remove(e.getPlayer().getUniqueId());
    }
}
