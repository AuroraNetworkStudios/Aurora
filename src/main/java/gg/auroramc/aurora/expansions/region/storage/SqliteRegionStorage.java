package gg.auroramc.aurora.expansions.region.storage;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.region.BlockPosition;
import gg.auroramc.aurora.expansions.region.ChunkCoordinate;
import gg.auroramc.aurora.expansions.region.ChunkData;
import gg.auroramc.aurora.expansions.region.Region;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

public class SqliteRegionStorage implements RegionStorage {
    private final String DB_URL = "jdbc:sqlite:" + Aurora.getInstance().getDataFolder() + "/regiondata.db";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    @SneakyThrows
    public SqliteRegionStorage() {
        var tables = new String(Aurora.getInstance().getResource("database/region_schema.sql").readAllBytes(), StandardCharsets.UTF_8).split(";");
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                for(String table : tables) {
                    stmt.addBatch(table);
                }
                stmt.executeBatch();
            }
            conn.commit();
        }
    }

    @Override
    public void loadRegion(Region region) {
        try (Connection conn = getConnection()) {
            String query = "SELECT * FROM chunks JOIN blocks ON chunks.id = blocks.chunk_id WHERE region_id = (SELECT id FROM regions WHERE world_name = ? AND region_x = ? AND region_z = ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, region.getWorldName());
                stmt.setInt(2, region.getX());
                stmt.setInt(3, region.getZ());

                var chunks = new HashMap<ChunkCoordinate, ChunkData>();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        byte chunkX = rs.getByte("chunk_x");
                        byte chunkZ = rs.getByte("chunk_z");
                        int blockX = rs.getInt("block_x");
                        int blockY = rs.getInt("block_y");
                        int blockZ = rs.getInt("block_z");
                        UUID playerId = UUID.fromString(rs.getString("player_uuid"));

                        ChunkCoordinate chunkCoord = new ChunkCoordinate(chunkX, chunkZ);
                        BlockPosition blockPos = new BlockPosition(blockX, blockY, blockZ);
                        chunks.computeIfAbsent(chunkCoord, k -> new ChunkData(region, chunkCoord.x(), chunkCoord.z()))
                                .addPlacedBlock(blockPos, playerId);
                    }
                    for(var chunk : chunks.entrySet()) {
                        region.setChunkData(chunk.getKey(), chunk.getValue());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveRegion(Region region) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int regionId = getOrCreateRegionId(conn, region.getWorldName(), region.getX(), region.getZ());
                for (ChunkData chunk : region.getChunks().values()) {
                    int chunkId = getOrCreateChunkId(conn, regionId, chunk.getX(), chunk.getZ());
                    saveChunk(conn, chunkId, chunk);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getOrCreateRegionId(Connection conn, String worldName, int regionX, int regionZ) throws SQLException {
        String query = "SELECT id FROM regions WHERE world_name = ? AND region_x = ? AND region_z = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, worldName);
            stmt.setInt(2, regionX);
            stmt.setInt(3, regionZ);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                String insert = "INSERT INTO regions (world_name, region_x, region_z) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, worldName);
                    insertStmt.setInt(2, regionX);
                    insertStmt.setInt(3, regionZ);
                    insertStmt.executeUpdate();
                    rs = insertStmt.getGeneratedKeys();
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create or find region ID");
    }

    private int getOrCreateChunkId(Connection conn, int regionId, byte chunkX, byte chunkZ) throws SQLException {
        String query = "SELECT id FROM chunks WHERE region_id = ? AND chunk_x = ? AND chunk_z = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, regionId);
            stmt.setByte(2, chunkX);
            stmt.setByte(3, chunkZ);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                String insert = "INSERT INTO chunks (region_id, chunk_x, chunk_z) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, regionId);
                    insertStmt.setByte(2, chunkX);
                    insertStmt.setByte(3, chunkZ);
                    insertStmt.executeUpdate();
                    rs = insertStmt.getGeneratedKeys();
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        throw new SQLException("Failed to create or find chunk ID");
    }

    private void saveChunk(Connection conn, int chunkId, ChunkData chunkData) throws SQLException {
        String delete = "DELETE FROM blocks WHERE chunk_id = ?";
        try (PreparedStatement delStmt = conn.prepareStatement(delete)) {
            delStmt.setInt(1, chunkId);
            delStmt.executeUpdate();
        }

        String insert = "INSERT INTO blocks (chunk_id, block_x, block_y, block_z, player_uuid) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
            for (BlockPosition block : chunkData.getPlacedBlocks().keySet()) {
                UUID uuid = chunkData.getPlacedBlocks().get(block).playerId();
                insertStmt.setInt(1, chunkId);
                insertStmt.setInt(2, block.x());
                insertStmt.setInt(3, block.y());
                insertStmt.setInt(4, block.z());
                insertStmt.setString(5, uuid.toString());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    @Override
    public void deleteRegionsInWorld(String worldName) {
        try (Connection conn = getConnection()) {
            String delete = "DELETE FROM regions WHERE world_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(delete)) {
                stmt.setString(1, worldName);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteChunkData(String worldName, int regionX, int regionZ, byte chunkX, byte chunkZ) {
        try (Connection conn = getConnection()) {
            int regionId = getOrCreateRegionId(conn, worldName, regionX, regionZ);
            String delete = "DELETE FROM chunks WHERE region_id = ? AND chunk_x = ? AND chunk_z = ?";
            try (PreparedStatement stmt = conn.prepareStatement(delete)) {
                stmt.setInt(1, regionId);
                stmt.setByte(2, chunkX);
                stmt.setByte(3, chunkZ);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

