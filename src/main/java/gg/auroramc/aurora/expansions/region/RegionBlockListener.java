package gg.auroramc.aurora.expansions.region;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.events.region.RegionBlockBreakEvent;
import gg.auroramc.aurora.api.events.region.RegionBlockPlaceEvent;
import gg.auroramc.aurora.api.util.Version;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class RegionBlockListener implements Listener {
    private final Aurora plugin;
    private final RegionExpansion regionExpansion;
    private final BlockFace[] blockFaces = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};


    public RegionBlockListener(Aurora plugin, RegionExpansion regionExpansion) {
        this.plugin = plugin;
        this.regionExpansion = regionExpansion;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        regionExpansion.addPlacedBlock(block);
        Bukkit.getPluginManager().callEvent(new RegionBlockPlaceEvent(event.getPlayer(), block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSandFall(EntityChangeBlockEvent event) {
        // Don't do anything on folia.
        if (Version.isFolia()) return;

        Block block = event.getBlock();
        if (!regionExpansion.isPlacedBlock(block)) return;
        if (!(event.getEntity() instanceof FallingBlock)) return;
        Material type = block.getType();
        if (type == Material.SAND || type == Material.RED_SAND || type == Material.GRAVEL) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType() == Material.AIR || below.getType() == Material.CAVE_AIR || below.getType() == Material.VOID_AIR
                    || below.getType() == Material.WATER || below.getType() == Material.BUBBLE_COLUMN || below.getType() == Material.LAVA) {

                regionExpansion.removePlacedBlock(block);
                Entity entity = event.getEntity();
                AtomicInteger counter = new AtomicInteger();
                Bukkit.getRegionScheduler().runAtFixedRate(plugin, entity.getLocation(), (task) -> {
                    Block currentBlock = entity.getLocation().getBlock();
                    if (entity.isDead() || !entity.isValid()) {
                        if (currentBlock.getType() == type) {
                            regionExpansion.addPlacedBlock(entity.getLocation().getBlock());
                        }
                        task.cancel();
                    } else if (currentBlock.getType().toString().contains("WEB")) {
                        task.cancel();
                    } else if (counter.incrementAndGet() >= 200) {
                        task.cancel();
                    }
                }, 1, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void checkBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        removeBlockWithEvent(player, block);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.SUGAR_CANE);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.BAMBOO);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.CACTUS);
        checkTallPlant(event.getPlayer(), block, 0, mat -> mat == Material.KELP_PLANT);
        checkBlocksRequiringSupportBelow(event.getPlayer(), block);
        checkAmethystCluster(event.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (regionExpansion.isPlacedBlock(block)) {
                regionExpansion.addPlacedBlock(block.getRelative(event.getDirection()));
            }
        }
        regionExpansion.removePlacedBlock(event.getBlock().getRelative(event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        Block lastBlock = event.getBlock();
        for (Block block : event.getBlocks()) {
            if (regionExpansion.isPlacedBlock(block)) {
                regionExpansion.addPlacedBlock(block.getRelative(event.getDirection()));
                if (block.getLocation().distanceSquared(event.getBlock().getLocation()) > lastBlock.getLocation().distanceSquared(event.getBlock().getLocation())) {
                    lastBlock = block;
                }
            }
        }

        if (lastBlock != event.getBlock()) {
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
        if (num < 26) {
            Block above = block.getRelative(BlockFace.UP);
            if (isMaterial.test(above.getType())) {
                removeBlockWithEvent(player, above);
                checkTallPlant(player, above, num + 1, isMaterial);
            }
        }
    }

    private void checkBlocksRequiringSupportBelow(Player player, Block block) {
        // Check if the block above requires support
        Block above = block.getRelative(BlockFace.UP);
        Material source = above.getType();
        if ((source == Material.MOSS_CARPET || source == Material.AZALEA || source == Material.FLOWERING_AZALEA || source == Material.PINK_PETALS)) {
            removeBlockWithEvent(player, above);
        }
    }

    private void checkAmethystCluster(Player player, Block block) {
        // Check each side
        for (BlockFace face : blockFaces) {
            Block checkedBlock = block.getRelative(face);
            if (Material.AMETHYST_CLUSTER == checkedBlock.getType()) {
                removeBlockWithEvent(player, checkedBlock);
            }
        }
    }

    private void removeBlockWithEvent(Player player, Block checkedBlock) {
        // Emit the event sync so proper block data is available
        var natural = !regionExpansion.isPlacedBlock(checkedBlock);
        Bukkit.getPluginManager().callEvent(new RegionBlockBreakEvent(player, checkedBlock, natural));

        if (!natural) {
            // Remove it a tick later, so we have the info in the bukkit block drop event still
            Bukkit.getRegionScheduler().run(plugin, checkedBlock.getLocation(),
                    (t) -> regionExpansion.removePlacedBlock(checkedBlock));
        }

    }
}
