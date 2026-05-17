package net.chen.legacyLand.resource.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区块资源储量数据模型（P1）
 * <p>
 * initialValue：首次扫描时计算的加权储量基线，永不变化。
 * currentValue：当前剩余储量，P2 阶段由 BlockBreakEvent 监听器递减。
 * biomeFactor：群系系数，参与地价公式 V = α·R + β·biomeFactor + γ·location。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResourceData {

    private String world;
    private int chunkX;
    private int chunkZ;
    private String biome;
    private double initialValue;
    private double currentValue;
    private double biomeFactor;
    private long lastScan;

    public String key() {
        return key(world, chunkX, chunkZ);
    }

    public static String key(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }
}