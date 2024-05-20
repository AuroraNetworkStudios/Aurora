package gg.auroramc.aurora.expansions.placeholder;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class InRegionHandler implements PlaceholderHandler {
    @Override
    public String getIdentifier() {
        return "inregion";
    }

    @Override
    public String onPlaceholderRequest(Player player, String[] args) {
        if(!DependencyManager.hasDep(Dep.WORLDGUARD)) return null;
        if(args.length < 1) return null;

        WorldGuardPlatform wpl = WorldGuard.getInstance().getPlatform();
        RegionContainer rg = wpl.getRegionContainer();
        String check = args[0];
        RegionQuery rq = rg.createQuery();

        var regions = rq.getApplicableRegions(BukkitAdapter.adapt(Bukkit.getPlayer(player.getUniqueId()).getLocation()));

        for(ProtectedRegion prg : regions) {
            if(prg.getId().equalsIgnoreCase(check)) return "true";
        }

        return "false";
    }

    @Override
    public List<String> getPatterns() {
        return List.of(
                "",
                "<region-name>"
        );
    }
}
