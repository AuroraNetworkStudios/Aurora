package gg.auroramc.aurora.expansions.region.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.region.*;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FileRegionStorage implements RegionStorage {
    private final LoadingCache<String, Object> lockCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public @NotNull Object load(@NotNull String key) {
                    return new Object();
                }
            });

    @Override
    public void loadRegion(Region region) {
        String worldName = region.getWorldName();
        int regionX = region.getX();
        int regionZ = region.getZ();
        var path = Aurora.getInstance().getDataFolder() + "/regiondata/" + worldName + "/r." + regionX + "." + regionZ + ".txt";

        synchronized (lockCache.getUnchecked(path)) {
            long start = System.currentTimeMillis();
            Path filePath = Paths.get(path);
            File file = filePath.toFile();

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("chunk")) {
                            String[] parts = line.split(",");
                            byte chunkX = Byte.parseByte(parts[1]);
                            byte chunkZ = Byte.parseByte(parts[2]);
                            loadChunk(region, new ChunkCoordinate(chunkX, chunkZ), reader);
                        }
                    }
                    long end = System.currentTimeMillis();
                    Aurora.logger().debug("Loaded region " + worldName + " " + regionX + "." + regionZ + " in " + (end - start) + "ms");
                } catch (Exception e) {
                    if (file.delete()) {
                        Aurora.logger().warning("Deleted " + file.getName() + " because it was corrupted, this won't affect anything.");
                    }
                }
            }
        }
    }

    @Override
    public void saveRegion(Region region) {
        var path = Aurora.getInstance().getDataFolder() + "/regiondata/" + region.getWorldName() + "/r." + region.getX() + "." + region.getZ() + ".txt";

        synchronized (lockCache.getUnchecked(path)) {
            long start = System.currentTimeMillis();
            Path filePath = Paths.get(path);
            File file = filePath.toFile();

            if (!file.exists() && region.getChunks().isEmpty()) return;

            file.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (ChunkData chunkData : region.getChunks().values()) {
                    saveChunk(writer, chunkData);
                }
                long end = System.currentTimeMillis();
                Aurora.logger().debug("Saved region " + region.getWorldName() + " " + region.getX() + "." + region.getZ() + " in " + (end - start) + "ms");
            } catch (Exception e) {
                if (file.delete()) {
                    Aurora.logger().warning("Deleted " + file.getName() + " because it was corrupted, this won't affect anything.");
                }
            }
        }
    }

    @Override
    public void deleteRegionsInWorld(String worldName) {
        Path filePath = Paths.get(Aurora.getInstance().getDataFolder() + "/regiondata/" + worldName);
        File file = filePath.toFile();
        if (!file.exists() || !file.isDirectory()) return;
        for (File regionFile : file.listFiles()) {
            synchronized (lockCache.getUnchecked(regionFile.getAbsolutePath())) {
                if (regionFile.getName().endsWith(".txt")) {
                    regionFile.delete();
                }
            }
        }
    }

    @Override
    public void deleteChunkData(String worldName, int regionX, int regionZ, byte chunkX, byte chunkZ) {
        var path = Aurora.getInstance().getDataFolder() + "/regiondata/" + worldName + "/r." + regionX + "." + regionZ + ".txt";
        Path filePath = Paths.get(path);
        File file = filePath.toFile();
        if (!file.exists()) return;

        synchronized (lockCache.getUnchecked(path)) {
            var region = new Region(Bukkit.getWorld(worldName), regionX, regionZ);
            loadRegion(region);
            region.markLoaded();
            region.removeChunkData(new ChunkCoordinate(chunkX, chunkZ));
            saveRegion(region);
        }
    }


    private void loadChunk(Region region, ChunkCoordinate chunkCoordinate, BufferedReader reader) throws IOException {
        ChunkData chunkData = new ChunkData(region, chunkCoordinate.x(), chunkCoordinate.z());

        String line;
        while ((line = reader.readLine()) != null && !line.equals("end_chunk")) {
            String[] parts = line.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            UUID uuid = UUID.fromString(parts[3]);
            chunkData.addPlacedBlock(new BlockPosition(x, y, z), uuid);
        }
        region.setChunkData(chunkCoordinate, chunkData);
    }

    private void saveChunk(BufferedWriter writer, ChunkData chunkData) throws IOException {
        writer.write("chunk," + chunkData.getX() + "," + chunkData.getZ() + "\n");
        for (BlockPosition block : chunkData.getPlacedBlocks().keySet()) {
            UUID uuid = chunkData.getPlacedBlocks().get(block).playerId();
            writer.write(block.x() + "," + block.y() + "," + block.z() + "," + uuid + "\n");
        }
        writer.write("end_chunk\n");
        chunkData.clearDiffs();
    }

    @Override
    public void dispose() {

    }
}
