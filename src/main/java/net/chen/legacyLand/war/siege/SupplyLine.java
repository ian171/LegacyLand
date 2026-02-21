package net.chen.legacyLand.war.siege;

import lombok.Data;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * 补给线
 */
@Data
public class SupplyLine {
    private final String warId;
    private final String town1;
    private final String town2;
    private final List<Location> path;
    private boolean active;
    private boolean blocked;

    public SupplyLine(String warId, String town1, String town2) {
        this.warId = warId;
        this.town1 = town1;
        this.town2 = town2;
        this.path = new ArrayList<>();
        this.active = true;
        this.blocked = false;
    }

    /**
     * 添加路径点
     */
    public void addPathPoint(Location location) {
        path.add(location);
    }

    /**
     * 检查补给线是否被切断
     */
    public boolean isCut() {
        return blocked || !active;
    }

    /**
     * 切断补给线
     */
    public void cut() {
        this.blocked = true;
    }

    /**
     * 恢复补给线
     */
    public void restore() {
        this.blocked = false;
    }

    /**
     * 获取补给线长度（区块数）
     */
    public int getLength() {
        return path.size();
    }
}
