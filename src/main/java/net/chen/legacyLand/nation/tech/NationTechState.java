package net.chen.legacyLand.nation.tech;

import java.util.HashSet;
import java.util.Set;

/**
 * 国家科技状态（内存缓存）
 */
public class NationTechState {
    private final String nationName;
    private int researchPoints;
    private final Set<String> completedNodes = new HashSet<>();

    public NationTechState(String nationName, int researchPoints) {
        this.nationName = nationName;
        this.researchPoints = researchPoints;
    }

    public String getNationName() { return nationName; }
    public int getResearchPoints() { return researchPoints; }

    public void addPoints(int amount) { researchPoints += amount; }

    public boolean deductPoints(int amount) {
        if (researchPoints < amount) return false;
        researchPoints -= amount;
        return true;
    }

    public void addCompleted(String nodeId) { completedNodes.add(nodeId); }
    public boolean hasCompleted(String nodeId) { return completedNodes.contains(nodeId); }
    public Set<String> getCompletedNodes() { return completedNodes; }
}
