package gg.auroramc.aurora.expansions.region;

import lombok.Getter;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Region {

    private final WeakReference<World> world;
    @Getter
    private final String worldName;
    @Getter
    private final int x;
    @Getter
    private final int z;
    private final ConcurrentMap<ChunkCoordinate, ChunkData> chunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkCoordinate, ChunkData> tempChunks = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final Queue<Consumer<Region>> taskQueue = new ConcurrentLinkedQueue<>();

    public Region(World world, int x, int z) {
        this.world = new WeakReference<>(world);
        this.worldName = world.getName();
        this.x = x;
        this.z = z;
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public long getPlacedBlockCount() {
        return chunks.values().stream().mapToLong(ChunkData::getPlacedBlockCount).sum();
    }

    public World getWorld() {
        return world.get();
    }

    public void clear() {
        if(isLoaded()) {
            chunks.clear();
        } else {
            tempChunks.clear();
            taskQueue.add(region -> region.chunks.clear());
        }
    }

    public ChunkData getChunkData(ChunkCoordinate chunkCoordinate) {
        if (!isLoaded()) return tempChunks.get(chunkCoordinate);
        return chunks.get(chunkCoordinate);
    }

    public void addPlacedBlock(ChunkCoordinate chunkCoordinate, BlockPosition blockPosition, UUID playerId) {
        if (isLoaded()) {
            chunks.computeIfAbsent(chunkCoordinate, k -> new ChunkData(this, chunkCoordinate.x(), chunkCoordinate.z()))
                    .addPlacedBlock(blockPosition, playerId);
        } else {
            tempChunks.computeIfAbsent(chunkCoordinate, k -> new ChunkData(this, chunkCoordinate.x(), chunkCoordinate.z()))
                    .addPlacedBlock(blockPosition, playerId);
            taskQueue.add(region -> region.chunks.computeIfAbsent(chunkCoordinate, k -> new ChunkData(this, chunkCoordinate.x(), chunkCoordinate.z()))
                    .addPlacedBlock(blockPosition, playerId));
        }
    }

    public void removePlacedBlock(ChunkCoordinate chunkCoordinate, BlockPosition blockPosition) {
        if (isLoaded()) {
            var chunk = chunks.get(chunkCoordinate);
            if (chunk != null) chunk.removePlacedBlock(blockPosition);
        } else {
            var tempChunk = tempChunks.get(chunkCoordinate);
            if (tempChunk != null) tempChunk.removePlacedBlock(blockPosition);
            taskQueue.add(region -> {
                var chunk = region.chunks.get(chunkCoordinate);
                if (chunk != null) chunk.removePlacedBlock(blockPosition);
            });
        }
    }

    public void removeChunkData(ChunkCoordinate chunkCoordinate) {
        if (isLoaded()) {
            chunks.remove(chunkCoordinate);
        } else {
            tempChunks.remove(chunkCoordinate);
            taskQueue.add(region -> region.chunks.remove(chunkCoordinate));
        }
    }

    @ApiStatus.Internal
    public void setChunkData(ChunkCoordinate chunkCoordinate, ChunkData chunkData) {
        if(tempChunks.containsKey(chunkCoordinate)) {
            var tempChunk = tempChunks.get(chunkCoordinate);
            for(var block : tempChunk.getPlacedBlocks().keySet()) {
                chunkData.addPlacedBlock(block, tempChunk.getPlacedBlocks().get(block).playerId());
            }
        }
        chunks.put(chunkCoordinate, chunkData);
    }

    @ApiStatus.Internal
    public ConcurrentMap<ChunkCoordinate, ChunkData> getChunks() {
        return chunks;
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void markLoaded() {
        Consumer<Region> task;
        while ((task = taskQueue.poll()) != null) {
            task.accept(this);
        }
        this.loaded.set(true);
        tempChunks.clear();
    }
}
