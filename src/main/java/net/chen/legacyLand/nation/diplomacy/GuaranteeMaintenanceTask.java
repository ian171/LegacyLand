package net.chen.legacyLand.nation.diplomacy;

import net.chen.legacyLand.LegacyLand;

/**
 * 外交保卫维持费用定时任务
 * 每小时检查并支付所有保卫关系的维持费用
 */
public class GuaranteeMaintenanceTask implements Runnable {

    private final GuaranteeManager guaranteeManager;

    public GuaranteeMaintenanceTask() {
        this.guaranteeManager = GuaranteeManager.getInstance();
    }

    @Override
    public void run() {
        try {
            guaranteeManager.checkAndPayMaintenance();
        } catch (Exception e) {
            LegacyLand.logger.severe("[GuaranteeMaintenanceTask] 检查维持费用时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
