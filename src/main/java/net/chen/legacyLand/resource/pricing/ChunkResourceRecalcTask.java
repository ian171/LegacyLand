package net.chen.legacyLand.resource.pricing;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * 资源区块衰减定时任务（P2）。
 * <p>
 * 由 {@link net.chen.legacyLand.LegacyLand} 在启动时以
 * {@code recalc-interval-ticks} 周期调度（默认 1200 ticks = 60 秒）。
 * 仅负责把 {@link ChunkResourceManager#accumulateDecrement} 累积的待 flush
 * 数据落库 + 触发耗竭事件，单次破坏不会立即引起 DB 写。
 */
public class ChunkResourceRecalcTask extends BukkitRunnable {

    @Override
    public void run() {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null) return;
        manager.flushPendingDecrements();
    }
}
