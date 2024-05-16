package gg.auroramc.auroralib.expansions.region;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.events.region.RegionBlockPlaceEvent;
import gg.auroramc.auroralib.api.events.region.RegionBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class RegionBlockListener implements Listener {
    private final AuroraLib plugin;
    private final RegionExpansion regionExpansion;
    private final BlockFace[] blockFaces = new BlockFace[] {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};

    public RegionBlockListener(AuroraLib plugin, RegionExpansion regionExpansion) {
        this.plugin = plugin;
        this.regionExpansion = regionExpansion;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        regionExpansion.addPlacedBlock(block, event.getPlayer().getUniqueId());
        Bukkit.getPluginManager().callEvent(new RegionBlockPlaceEvent(event.getPlayer(), block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSandFall(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        if (!regionExpansion.isPlacedBlock(block)) return;
        Material type = block.getType();
        if (type == Material.SAND || type.toString().equals("RED_SAND") || type == Material.GRAVEL) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType() == Material.AIR || below.getType().toString().equals("CAVE_AIR") || below.getType().toString().equals("VOID_AIR")
                    || below.getType() == Material.WATER || below.getType().toString().equals("BUBBLE_COLUMN") || below.getType() == Material.LAVA) {
                BlockData blockData = regionExpansion.getPlacedBlockData(block);
                regionExpansion.removePlacedBlock(block);
                Entity entity = event.getEntity();
                AtomicInteger counter = new AtomicInteger();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Block currentBlock = entity.getLocation().getBlock();
                        if (entity.isDead() || !entity.isValid()) {
                            if (currentBlock.getType() == type) {
                                regionExpansion.addPlacedBlock(entity.getLocation().getBlock(), blockData.playerId());
                            }
                            cancel();
                        } else if (currentBlock.getType().toString().contains("WEB")) {
                            cancel();
                        } else if (counter.incrementAndGet() >= 200) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if(regionExpansion.isPlacedBlock(block)) {
            var whoPlaced = regionExpansion.getPlacedBlockData(block).playerId();
            regionExpansion.removePlacedBlock(block);
            Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(event.getPlayer(), Bukkit.getOfflinePlayer(whoPlaced), block, false));
        } else {
            Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(event.getPlayer(), null, block, true));
        }
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.SUGAR_CANE);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.BAMBOO);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.CACTUS);
        checkBlocksRequiringSupportBelow(event.getPlayer(), block);
        checkAmethystCluster(event.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if(regionExpansion.isPlacedBlock(block)) {
                BlockData blockData = regionExpansion.getPlacedBlockData(block);
                regionExpansion.addPlacedBlock(block.getRelative(event.getDirection()), blockData.playerId());
            }
        }
        regionExpansion.removePlacedBlock(event.getBlock().getRelative(event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        Block lastBlock = event.getBlock();
        for (Block block : event.getBlocks()) {
            if (regionExpansion.isPlacedBlock(block)) {
                BlockData blockData = regionExpansion.getPlacedBlockData(block);
                regionExpansion.addPlacedBlock(block.getRelative(event.getDirection()), blockData.playerId());
                if (block.getLocation().distanceSquared(event.getBlock().getLocation()) > lastBlock.getLocation().distanceSquared(event.getBlock().getLocation())) {
                    lastBlock = block;
                }
            }
        }

        if(lastBlock != event.getBlock()) {
            regionExpansion.removePlacedBlock(lastBlock);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        int growY = event.getLocation().getBlockY();
        for (var state : event.getBlocks()) {
            // Only remove placed blocks at same y level as sapling
            if (state.getLocation().getY() != growY) continue;

            regionExpansion.removePlacedBlock(state.getBlock());
        }
    }

    private void checkTallPlant(Player player, Block block, int num, Predicate<Material> isMaterial) {
        if (num < 20) {
            Block above = block.getRelative(BlockFace.UP);
            if (isMaterial.test(above.getType())) {
                if (regionExpansion.isPlacedBlock(above)) {

                    if(regionExpansion.isPlacedBlock(block)) {
                        Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, Bukkit.getOfflinePlayer(regionExpansion.getPlacedBlockData(block).playerId()), block, false));
                        regionExpansion.removePlacedBlock(above);
                    }
                    checkTallPlant(player, above, num + 1, isMaterial);
                } else {
                    Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, null, block, true));
                }
            }
        }
    }

    private void checkBlocksRequiringSupportBelow(Player player, Block block) {
        // Check if the block above requires support
        Block above = block.getRelative(BlockFace.UP);
        Material source = above.getType();
        if ((source == Material.MOSS_CARPET || source == Material.AZALEA || source == Material.FLOWERING_AZALEA || source == Material.PINK_PETALS)) {
            if(regionExpansion.isPlacedBlock(above)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Remove if block was destroyed
                        if (source == block.getType()) {
                            Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, Bukkit.getOfflinePlayer(regionExpansion.getPlacedBlockData(block).playerId()), block, false));
                            regionExpansion.removePlacedBlock(above);
                        }
                    }
                }.runTaskLater(plugin, 1);
            } else {
                Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, null, block, true));
            }
        }
    }

    private void checkAmethystCluster(Player player, Block block) {
        // Check each side
        for (BlockFace face : blockFaces) {
            Block checkedBlock = block.getRelative(face);
            if (Material.AMETHYST_CLUSTER == block.getType()) {
                if(regionExpansion.isPlacedBlock(checkedBlock)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Remove if block was destroyed
                            if (Material.AMETHYST_CLUSTER != block.getType()) {
                                Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, Bukkit.getOfflinePlayer(regionExpansion.getPlacedBlockData(block).playerId()), block, false));
                                regionExpansion.removePlacedBlock(block);
                            }
                        }
                    }.runTaskLater(plugin, 1);
                } else {
                    Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, null, block, true));
                }
            }
        }
    }
}
