package gg.auroramc.aurora.expansions.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;

import java.util.Set;

public class WorldGuardEntryAndLeaveHandler extends Handler {

    private final WorldGuardExpansion expansion;

    public static Factory FACTORY(WorldGuardExpansion expansion)
    {
        return new Factory(expansion);
    }

    public static class Factory extends Handler.Factory<WorldGuardEntryAndLeaveHandler>
    {
        private final WorldGuardExpansion expansion;

        public Factory(WorldGuardExpansion expansion) {
            this.expansion = expansion;
        }

        @Override
        public WorldGuardEntryAndLeaveHandler create(Session session)
        {
            return new WorldGuardEntryAndLeaveHandler(session, expansion);
        }
    }

    /**
     * Create a new handler.
     *
     * @param session The session
     */
    protected WorldGuardEntryAndLeaveHandler(Session session, WorldGuardExpansion expansion) {
        super(session);
        this.expansion = expansion;
    }

    @Override
    public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set) {
        expansion.onRegionUpdate(player.getUniqueId(), set.getRegions());
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        if(expansion.checkFreeMoveAfterTeleport(player.getUniqueId(), BukkitAdapter.adapt(to).getBlock().getLocation())) {
            return true;
        }

        if(moveType == MoveType.TELEPORT) {
            var l = BukkitAdapter.adapt(from).getBlock().getLocation();
            expansion.addTeleportMove(player.getUniqueId(), l);
        }

        expansion.onRegionUpdate(player.getUniqueId(), toSet.getRegions());
        return true;
    }
}
