package gg.auroramc.aurora.expansions.region;

import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ChunkData {

    private final Region region;
    private final byte x;
    private final byte z;
    private final ConcurrentMap<BlockPosition, BlockData> placedBlocks = new ConcurrentHashMap<>();

    private final Set<BlockPosition> placedDiff = Sets.newConcurrentHashSet();
    private final Set<BlockPosition> deletedDiff = Sets.newConcurrentHashSet();

    public ChunkData(Region region, byte x, byte z) {
        this.region = region;
        this.x = x;
        this.z = z;
    }

    public int getPlacedBlockCount() {
        return placedBlocks.size();
    }

    public boolean isPlacedBlock(BlockPosition blockPosition) {
        return placedBlocks.containsKey(blockPosition);
    }

    public BlockData getBlockData(BlockPosition blockPosition) {
        return placedBlocks.get(blockPosition);
    }

    public void addPlacedBlock(BlockPosition blockPosition, UUID playerId) {
        this.placedBlocks.put(blockPosition, new BlockData(blockPosition, playerId));
        if (!region.isLoaded()) return;
        if (!this.deletedDiff.remove(blockPosition)) {
            this.placedDiff.add(blockPosition);
        }
    }

    public void removePlacedBlock(BlockPosition blockPosition) {
        this.placedBlocks.remove(blockPosition);
        if (!region.isLoaded()) return;
        if (!this.placedDiff.remove(blockPosition)) {
            this.deletedDiff.add(blockPosition);
        }
    }

    public void clearDiffs() {
        this.placedDiff.clear();
        this.deletedDiff.clear();
    }
}
