package gg.auroramc.auroralib.expansions.region.storage;

import gg.auroramc.auroralib.expansions.region.Region;

public interface RegionStorage {
    void loadRegion(Region region);
    void saveRegion(Region region);
    void deleteRegionsInWorld(String worldName);
    void deleteChunkData(String worldName, int regionX, int regionZ, byte chunkX, byte chunkZ);
}
