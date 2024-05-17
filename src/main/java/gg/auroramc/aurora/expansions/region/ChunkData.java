package gg.auroramc.aurora.expansions.region;

import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ChunkData {

    private final Region region;
    private final byte x;
    private final byte z;
    private final ConcurrentMap<BlockPosition, BlockData> placedBlocks = new ConcurrentHashMap<>();

    public ChunkData(Region region, byte x, byte z) {
        this.region = region;
        this.x = x;
        this.z = z;
    }

    public boolean isPlacedBlock(BlockPosition blockPosition) {
        return placedBlocks.containsKey(blockPosition);
    }

    public BlockData getBlockData(BlockPosition blockPosition) {
        return placedBlocks.get(blockPosition);
    }

    public void addPlacedBlock(BlockPosition blockPosition, UUID playerId) {
        this.placedBlocks.put(blockPosition, new BlockData(blockPosition, playerId));
    }

    public void removePlacedBlock(BlockPosition blockPosition) {
        this.placedBlocks.remove(blockPosition);
    }

}
