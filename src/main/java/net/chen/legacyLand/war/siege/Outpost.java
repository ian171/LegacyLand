package net.chen.legacyLand.war.siege;

import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 前哨战
 */
@Data
public class Outpost {
    private final String warId;
    private final Location location;
    private final UUID creatorId;
    private final long establishTime;
    private boolean active;
    private boolean discovered;
    private boolean completed;

    // 前哨战需要2人在附近维持1小时
    private int nearbyPlayerCount;
    private long lastCheckTime;
    private int progress; // 进度（分钟）

    public Outpost(String warId, Location location, UUID creatorId) {
        this.warId = warId;
        this.location = location;
        this.creatorId = creatorId;
        this.establishTime = System.currentTimeMillis();
        this.active = true;
        this.discovered = false;
        this.completed = false;
        this.nearbyPlayerCount = 0;
        this.lastCheckTime = System.currentTimeMillis();
        this.progress = 0;
    }

    /**
     * 增加进度
     */
    public void addProgress(int minutes) {
        this.progress += minutes;
    }

    /**
     * 重置进度
     */
    public void resetProgress() {
        this.progress = 0;
    }

    /**
     * 检查前哨战是否已维持1小时
     */
    public boolean isReady() {
        if (!active || discovered) return false;
        return System.currentTimeMillis() - establishTime >= 3600000; // 1小时
    }

    /**
     * 更新附近玩家数量
     */
    public void updateNearbyPlayers(int count) {
        this.nearbyPlayerCount = count;
        this.lastCheckTime = System.currentTimeMillis();
    }

    /**
     * 检查是否有效（需要至少2人在附近）
     */
    public boolean isValid() {
        return active && !discovered && nearbyPlayerCount >= 2;
    }

    /**
     * 被发现
     */
    public void discover() {
        this.discovered = true;
        this.active = false;
    }

    /**
     * 摧毁前哨战
     */
    public void destroy() {
        this.active = false;
    }
}
