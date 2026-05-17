package net.chen.legacyLand.resource.pricing.listener;

import net.chen.legacyLand.resource.pricing.ChunkResourceManager;
import net.chen.legacyLand.resource.pricing.ResourcePricingConfig;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * 区块资源采集监听器（P2）。
 * <p>
 * 设计：
 * <ul>
 *   <li>所有衰减仅在内存累加（{@link ChunkResourceManager#accumulateDecrement}），
 *       由 {@code ChunkResourceRecalcTask} 定时 flush 写库与触发耗竭事件，
 *       避免每次破坏触发 DB I/O。</li>
 *   <li>防作弊：{@link #onBlockPlace} 标记玩家放置的矿物方块，
 *       破坏时若命中则跳过累加（{@link ChunkResourceManager#consumePlayerPlaced}）。</li>
 *   <li>爆炸：BlockExplode / EntityExplode 中遍历 blockList，
 *       按 {@code explosion-decay-factor} 衰减计入。</li>
 * </ul>
 */
public class ResourceBlockBreakListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null || !manager.getConfig().isEnabled()) return;

        Block block = event.getBlock();
        Material mat = block.getType();
        double weight = manager.getConfig().weightOf(mat);
        if (weight <= 0.0) return;

        Chunk chunk = block.getChunk();
        String world = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        if (manager.consumePlayerPlaced(world, cx, cz, block.getX(), block.getY(), block.getZ())) {
            return; // 玩家放置后再挖，不计入耗竭
        }

        manager.accumulateDecrement(world, cx, cz, weight);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null || !manager.getConfig().isEnabled()) return;

        Block block = event.getBlockPlaced();
        if (manager.getConfig().weightOf(block.getType()) <= 0.0) return; // 非矿物不打标记

        Chunk chunk = block.getChunk();
        manager.markPlayerPlaced(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ(),
                block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null || !manager.getConfig().isEnabled()) return;
        accumulateExplosion(manager, event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null || !manager.getConfig().isEnabled()) return;
        accumulateExplosion(manager, event.blockList());
    }

    private void accumulateExplosion(ChunkResourceManager manager, Iterable<Block> blocks) {
        ResourcePricingConfig cfg = manager.getConfig();
        double factor = cfg.getExplosionDecayFactor();
        if (factor <= 0.0) return;

        for (Block block : blocks) {
            double weight = cfg.weightOf(block.getType());
            if (weight <= 0.0) continue;

            Chunk chunk = block.getChunk();
            String world = chunk.getWorld().getName();
            int cx = chunk.getX();
            int cz = chunk.getZ();

            if (manager.consumePlayerPlaced(world, cx, cz, block.getX(), block.getY(), block.getZ())) {
                continue;
            }

            manager.accumulateDecrement(world, cx, cz, weight * factor);
        }
    }
}
