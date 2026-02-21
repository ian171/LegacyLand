package net.chen.legacyLand.player;

import lombok.Data;
import net.chen.legacyLand.achievements.Achievements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 玩家数据模型
 */
@Data
public class PlayerData {
    private final UUID playerId;
    private String playerName;

    // 基础属性
    private double maxHealth;           // 最大血量 (15-30)
    private int hydration;              // 饮水值 (0-10)
    private double temperature;         // 体温 (°C)

    // 职业系统
    private Profession mainProfession;  // 主职业
    private int mainProfessionLevel;    // 主职业等级
    private int mainProfessionExp;      // 主职业经验

    private Profession subProfession;   // 副职业
    private int subProfessionLevel;     // 副职业等级 (最高5级)
    private int subProfessionExp;       // 副职业经验

    private int talentPoints;           // 天赋点 (初始10点)

    // 状态效果
    private boolean isDowned;           // 是否倒地等待救援
    private long downedTime;// 倒地时间戳

    private List<Achievements> achievements;

    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.maxHealth = 15.0;
        this.hydration = 10;
        this.temperature = 22.0;
        this.talentPoints = 10;
        this.mainProfessionLevel = 0;
        this.subProfessionLevel = 0;
        this.isDowned = false;
        this.achievements = new ArrayList<>();
    }

    /**
     * 增加主职业经验
     */
    public boolean addMainProfessionExp(int exp) {
        if (mainProfession == null) return false;

        mainProfessionExp += exp;
        int requiredExp = getRequiredExp(mainProfessionLevel);

        if (mainProfessionExp >= requiredExp) {
            mainProfessionLevel++;
            mainProfessionExp -= requiredExp;

            // 每升一级增加2点血量上限，封顶30
            if (maxHealth < 30.0) {
                maxHealth = Math.min(30.0, maxHealth + 2.0);
            }
            return true;
        }
        return false;
    }

    /**
     * 增加副职业经验
     */
    public boolean addSubProfessionExp(int exp) {
        if (subProfession == null || subProfessionLevel >= 5) return false;

        subProfessionExp += exp;

        if (subProfessionExp >= 500) {
            subProfessionLevel++;
            subProfessionExp = 0;
            return true;
        }
        return false;
    }

    /**
     * 消耗饮水值
     */
    public void consumeHydration(int amount) {
        hydration = Math.max(0, hydration - amount);
    }

    /**
     * 恢复饮水值
     */
    public void restoreHydration(int amount) {
        hydration = Math.min(10, hydration + amount);
    }

    /**
     * 调整体温
     */
    public void adjustTemperature(double delta) {
        temperature += delta;
    }

    /**
     * 获取升级所需经验
     */
    private int getRequiredExp(int level) {
        return 100 + (level * 50);
    }

    /**
     * 检查是否可以选择副职业
     */
    public boolean canChooseSubProfession() {
        return mainProfessionLevel >= 20 && subProfession == null;
    }

    public void addAchievement(Achievements achievement) {
        achievements.add(achievement);
    }
    public List<Achievements> getAchievements() {
        return Collections.unmodifiableList(achievements);
    }
    public boolean hasAchievement(Achievements achievement) {
        return achievements.contains(achievement);
    }
    public boolean removeAchievement(Achievements achievement) {
        return achievements.remove(achievement);
    }
}
