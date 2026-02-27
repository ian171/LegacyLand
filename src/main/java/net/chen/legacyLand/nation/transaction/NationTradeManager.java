package net.chen.legacyLand.nation.transaction;

import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import net.chen.legacyLand.nation.diplomacy.DiplomacyManager;
import net.chen.legacyLand.nation.diplomacy.RelationType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NationTradeManager {
    private final NationManager nationManager;

    public NationTradeManager(NationManager nationManager) {
        this.nationManager = nationManager;
    }

    /**
     * 执行国家间贸易：source 卖出物品，target 买入并付款。
     *
     * 事务顺序（保证可回滚）：
     *   1. 前置校验（参数、权限、外交、余额）
     *   2. 买方扣款
     *   3. 卖方国库取出物品（失败 → 退款）
     *   4. 买方国库存入物品（失败 → 还物品 + 退款）
     *   5. 卖方收款（扣除贸易税）
     *
     * @param source  卖方国家
     * @param target  买方国家
     * @param control 操作者（须属于 source 或 target，且持有 PROPOSE_DIPLOMACY 权限）
     * @param item    交易物品
     * @param price   交易价格（金币，必须 > 0）
     * @return 交易结果
     */
    public TradeResult purchaseFrom(Nation source, Nation target, Player control, ItemStack item, int price) {
        // 1. 基础参数校验
        if (source == null || target == null || item == null || item.getType().isAir()) {
            return TradeResult.INVALID_PARAMS;
        }
        if (price <= 0) {
            return TradeResult.INVALID_PARAMS;
        }
        if (source.equals(target)) {
            return TradeResult.SAME_NATION;
        }

        // 2. 操作者权限校验：必须属于交易双方之一，且持有外交提案权限
        Nation controlNation = nationManager.getPlayerNation(control);
        if (controlNation == null
                || (!controlNation.equals(source) && !controlNation.equals(target))) {
            return TradeResult.NO_PERMISSION;
        }
        if (!nationManager.hasPermission(control, NationPermission.PROPOSE_DIPLOMACY)) {
            return TradeResult.NO_PERMISSION;
        }

        // 3. 外交关系校验：敌对/战争状态禁止贸易
        RelationType relation = DiplomacyManager.getInstance()
                .getRelation(source.getName(), target.getName());
        if (relation.isHostile()) {
            return TradeResult.HOSTILE_RELATION;
        }

        // 4. 买方余额预检（避免扣款后才发现余额不足）
        double buyerBalance;
        try {
            buyerBalance = target.getAccount().getHoldingBalance();
        } catch (Exception e) {
            LegacyLand.logger.warning("[Trade] 获取买方余额失败: " + e.getMessage());
            return TradeResult.ECONOMY_ERROR;
        }
        if (buyerBalance < price) {
            return TradeResult.INSUFFICIENT_FUNDS;
        }

        // 5. 计算贸易税（从 config 读取，范围限制在 [0, 1]）
        double taxRate = LegacyLand.getInstance().getConfig()
                .getDouble("tax.default.trade", 0.0) / 100.0;
        taxRate = Math.max(0.0, Math.min(taxRate, 1.0));
        double sellerReceives = price * (1.0 - taxRate);

        // 6. 买方扣款
        try {
            target.getAccount().withdraw(price, "国家贸易支付");
        } catch (Exception e) {
            LegacyLand.logger.warning("[Trade] 买方扣款失败: " + e.getMessage());
            return TradeResult.ECONOMY_ERROR;
        }

        // 7. 从卖方国库取出物品（失败 → 退款）
        if (!nationManager.withdrawFromNationTreasury(source.getName(), item)) {
            refundMoney(target, price, "国家贸易退款（卖方库存不足）");
            return TradeResult.SOURCE_TREASURY_EMPTY;
        }

        // 8. 将物品存入买方国库（失败 → 还物品 + 退款）
        if (!nationManager.donateToTreasury(target, item)) {
            nationManager.donateToTreasury(source, item);
            refundMoney(target, price, "国家贸易退款（买方国库已满）");
            return TradeResult.TARGET_TREASURY_FULL;
        }

        // 9. 卖方收款（扣除贸易税后）
        try {
            source.getAccount().deposit(sellerReceives, "国家贸易收入");
        } catch (Exception e) {
            // 物品已完成转移，仅记录日志，不中断流程
            LegacyLand.logger.severe("[Trade] 卖方收款失败！"
                    + source.getName() + " 应收 " + sellerReceives + " - " + e.getMessage());
        }

        LegacyLand.logger.info(String.format("[Trade] %s → %s | %dx%s | 价格:%d | 税率:%.1f%% | 操作者:%s",
                source.getName(), target.getName(),
                item.getAmount(), item.getType().name(),
                price, taxRate * 100, control.getName()));

        return TradeResult.SUCCESS;
    }

    private void refundMoney(Nation nation, double amount, String reason) {
        try {
            nation.getAccount().deposit(amount, reason);
        } catch (Exception e) {
            LegacyLand.logger.severe("[Trade] 退款失败！国家: "
                    + nation.getName() + ", 金额: " + amount + " - " + e.getMessage());
        }
    }

    public enum TradeResult {
        SUCCESS,
        INVALID_PARAMS,
        SAME_NATION,
        NO_PERMISSION,
        HOSTILE_RELATION,
        INSUFFICIENT_FUNDS,
        SOURCE_TREASURY_EMPTY,
        TARGET_TREASURY_FULL,
        ECONOMY_ERROR
    }
}
