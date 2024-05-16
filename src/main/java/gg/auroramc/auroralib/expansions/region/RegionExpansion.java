package gg.auroramc.auroralib.expansions.region;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.dependency.Dep;
import gg.auroramc.auroralib.api.dependency.DependencyManager;
import gg.auroramc.auroralib.api.expansions.AuroraExpansion;
import gg.auroramc.auroralib.expansions.region.integrations.MVCore;
import gg.auroramc.auroralib.expansions.region.integrations.WildRegeneration;
import gg.auroramc.auroralib.expansions.region.storage.FileRegionStorage;
import gg.auroramc.auroralib.expansions.region.storage.H2RegionStorage;
import gg.auroramc.auroralib.expansions.region.storage.RegionStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RegionExpansion implements AuroraExpansion {

    private final ConcurrentMap<RegionCoordinate, Region> regions = new ConcurrentHashMap<>();
    private RegionStorage storage;


    public Region getRegion(RegionCoordinate regionCoordinate) {
        return regions.get(regionCoordinate);
    }

    public boolean isPlacedBlock(Block block) {
        return getPlacedBlockData(block) != null;
    }

    public void clearChunk(Chunk chunk) {
        clearChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public void clearChunk(String name, int x, int z) {
        int regionX = (int) Math.floor((double) z / 32.0);
        int regionZ = (int) Math.floor((double) x / 32.0);
        Region region = regions.get(new RegionCoordinate(Bukkit.getWorld(name), regionX, regionZ));
        byte regionChunkX = (byte) (x- regionX * 32);
        byte regionChunkZ = (byte) (z - regionZ * 32);

        if(region != null) {
            region.removeChunkData(new ChunkCoordinate(regionChunkX, regionChunkZ));
        } else {
            Bukkit.getAsyncScheduler().runNow(AuroraLib.getInstance(),
                    (task) -> storage.deleteChunkData(name, regionX, regionZ, regionChunkX, regionChunkZ));
        }
    }

    public void clearWorld(String worldName) {
        for(var region : regions.values()) {
            if(region.getWorldName().equals(worldName)) {
                region.clear();
                Bukkit.getAsyncScheduler().runNow(AuroraLib.getInstance(), (task) -> storage.deleteRegionsInWorld(worldName));
            }
        }
    }

    public BlockData getPlacedBlockData(Block block) {
        int chunkX = block.getChunk().getX();
        int chunkZ = block.getChunk().getZ();

        int regionX = (int) Math.floor((double) chunkX / 32.0);
        int regionZ = (int) Math.floor((double) chunkZ / 32.0);

        Region region = regions.get(new RegionCoordinate(block.getWorld(), regionX, regionZ));
        if (region != null) {
            byte regionChunkX = (byte) (chunkX - regionX * 32);
            byte regionChunkZ = (byte) (chunkZ - regionZ * 32);
            ChunkData chunkData = region.getChunkData(new ChunkCoordinate(regionChunkX, regionChunkZ));
            if (chunkData != null) {
                BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
                return chunkData.getBlockData(blockPosition);
            }
        }
        return null;
    }

    public void addPlacedBlock(Block block, UUID playerId) {
        int chunkX = block.getChunk().getX();
        int chunkZ = block.getChunk().getZ();

        int regionX = (int) Math.floor((double) chunkX / 32.0);
        int regionZ = (int) Math.floor((double) chunkZ / 32.0);

        RegionCoordinate regionCoordinate = new RegionCoordinate(block.getWorld(), regionX, regionZ);
        Region region = regions.get(regionCoordinate);
        // Create region if does not exist
        if (region == null) {
            region = new Region(block.getWorld(), regionX, regionZ);
            loadRegion(region);
        }
        byte regionChunkX = (byte) (chunkX - regionX * 32);
        byte regionChunkZ = (byte) (chunkZ - regionZ * 32);

        region.addPlacedBlock(new ChunkCoordinate(regionChunkX, regionChunkZ), new BlockPosition(block.getX(), block.getY(), block.getZ()), playerId);
    }

    public void removePlacedBlock(Block block) {
        int chunkX = block.getChunk().getX();
        int chunkZ = block.getChunk().getZ();

        int regionX = (int) Math.floor((double) chunkX / 32.0);
        int regionZ = (int) Math.floor((double) chunkZ / 32.0);

        RegionCoordinate regionCoordinate = new RegionCoordinate(block.getWorld(), regionX, regionZ);
        Region region = regions.get(regionCoordinate);

        byte regionChunkX = (byte) (chunkX - regionX * 32);
        byte regionChunkZ = (byte) (chunkZ - regionZ * 32);

        region.removePlacedBlock(new ChunkCoordinate(regionChunkX, regionChunkZ), new BlockPosition(block.getX(), block.getY(), block.getZ()));
    }

    public void loadRegion(Region region) {
        regions.put(new RegionCoordinate(region.getWorldName(), region.getX(), region.getZ()), region);
        Bukkit.getAsyncScheduler().runNow(AuroraLib.getInstance(), (task) -> {
            storage.loadRegion(region);
            region.markLoaded();
        });
    }

    public void saveAllRegions(boolean clearUnused) {
        for (Region region : regions.values()) {
            if (!region.isLoaded()) continue;
            storage.saveRegion(region);
            // Clear region from memory if no chunks are loaded in it
            if (clearUnused) {
                if (isRegionUnused(region)) {
                    regions.remove(new RegionCoordinate(region.getWorldName(), region.getX(), region.getZ()));
                }
            }
        }
    }

    private boolean isRegionUnused(Region region) {
        for (int chunkX = region.getX() * 32; chunkX < region.getX() * 32 + 32; chunkX++) {
            for (int chunkZ = region.getZ() * 32; chunkZ < region.getZ() * 32 + 32; chunkZ++) {
                if (region.getWorld() != null && region.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void hook() {
        if (AuroraLib.getLibConfig().getBlockTrackerStorage().equals("file")) {
            storage = new FileRegionStorage();
        } else {
            storage = new H2RegionStorage();
        }

        var plugin = AuroraLib.getInstance();
        Bukkit.getPluginManager().registerEvents(new RegionListener(plugin, this), plugin);
        Bukkit.getPluginManager().registerEvents(new RegionBlockListener(plugin, this), plugin);

        if(DependencyManager.hasDep(Dep.MULTIVERSECORE)) {
            Bukkit.getPluginManager().registerEvents(new MVCore(), plugin);
        }

        if(DependencyManager.hasDep(Dep.WILDREGENERATION)) {
            Bukkit.getPluginManager().registerEvents(new WildRegeneration(), plugin);
        }
    }

    @Override
    public boolean canHook() {
        return true;
    }
}
