package net.chen.legacyLand.resource.pricing.listener;

import net.chen.legacyLand.resource.pricing.ChunkResourceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * 区块加载监听器（P1）。
 * 触发储量普查：当区块首次加载且 DB 无记录时，启动异步扫描任务。
 * 监听 newChunk=false/true 都处理，因为我们关心"普查覆盖率"而非"是否新生成"。
 */
public class ChunkLoadResourceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        ChunkResourceManager manager = ChunkResourceManager.getInstance();
        if (manager == null) return;
        manager.scanIfAbsent(event.getChunk());
    }
}
