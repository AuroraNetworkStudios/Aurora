package gg.auroramc.aurora.expansions.region;

import com.google.common.base.Objects;
import org.bukkit.block.Block;

public record BlockPosition(int x, int y, int z) {

    public static BlockPosition fromBlock(Block block) {
        return new BlockPosition(block.getX(), block.getY(), block.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof BlockPosition other)) {
            return false;
        } else {
            return this.x == other.x && this.y == other.y && this.z == other.z;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, z);
    }

}
