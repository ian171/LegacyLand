package net.chen.legacyLand.resource.pricing.event;

import lombok.Getter;
import net.chen.legacyLand.resource.pricing.ChunkResourceData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * 区块资源耗竭事件（P2）。
 * <p>
 * 由 {@link net.chen.legacyLand.resource.pricing.ChunkResourceManager#flushPendingDecrements()}
 * 在某区块 currentValue 由 &gt;0 降至 ≤0 时触发一次。后续模块（P3 Towny 地价、P4 BlueMap）
 * 可订阅以更新地价或热力图。
 */
@Getter
public class ChunkExhaustedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private final ChunkResourceData data;

    public ChunkExhaustedEvent(String world, int chunkX, int chunkZ, ChunkResourceData data) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.data = data;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
