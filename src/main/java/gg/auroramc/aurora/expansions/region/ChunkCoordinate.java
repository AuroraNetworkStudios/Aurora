package gg.auroramc.aurora.expansions.region;

import com.google.common.base.Objects;

public record ChunkCoordinate(byte x, byte z) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof ChunkCoordinate other)) {
            return false;
        } else {
            return this.x == other.x && this.z == other.z;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, z);
    }

}
