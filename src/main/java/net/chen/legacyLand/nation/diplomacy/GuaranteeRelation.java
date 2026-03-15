package net.chen.legacyLand.nation.diplomacy;

import lombok.Getter;
import lombok.Setter;

/**
 * 外交保卫关系
 */
@Getter
@Setter
public class GuaranteeRelation {
    private final String guarantorNation;  // 保卫国
    private final String protectedNation;  // 受保护国
    private final long establishedTime;    // 建立时间
    private long lastMaintenanceTime;      // 上次维持费用支付时间
    private boolean active;                // 是否激活

    public GuaranteeRelation(String guarantorNation, String protectedNation) {
        this.guarantorNation = guarantorNation;
        this.protectedNation = protectedNation;
        this.establishedTime = System.currentTimeMillis();
        this.lastMaintenanceTime = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * 用于从数据库加载的构造函数
     */
    public GuaranteeRelation(String guarantorNation, String protectedNation,
                           long establishedTime, long lastMaintenanceTime, boolean active) {
        this.guarantorNation = guarantorNation;
        this.protectedNation = protectedNation;
        this.establishedTime = establishedTime;
        this.lastMaintenanceTime = lastMaintenanceTime;
        this.active = active;
    }

    /**
     * 检查是否涉及指定国家
     */
    public boolean involves(String nationName) {
        return guarantorNation.equals(nationName) || protectedNation.equals(nationName);
    }

    /**
     * 检查是否需要支付维持费用（每小时）
     */
    public boolean needsMaintenance() {
        long hourInMillis = 60 * 60 * 1000;
        return System.currentTimeMillis() - lastMaintenanceTime >= hourInMillis;
    }

    /**
     * 更新维持费用支付时间
     */
    public void updateMaintenanceTime() {
        this.lastMaintenanceTime = System.currentTimeMillis();
    }
}
