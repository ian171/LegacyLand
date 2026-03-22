package net.chen.legacyLand.nation.diplomacy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.database.DatabaseManager;
import net.chen.legacyLand.util.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 外交保卫管理器
 */
public class GuaranteeManager {
    private static GuaranteeManager instance;
    private final Map<String, List<GuaranteeRelation>> guarantees;
    private final DatabaseManager database;
    private final Economy economy;
    private final TownyAPI townyAPI;

    // 维持费用常量
    private static final double MAINTENANCE_COST_MONEY = 500.0;
    private static final int MAINTENANCE_COST_TRADE_XP = 10;

    // 违约惩罚常量
    private static final double BETRAYAL_PENALTY_RATE = 0.5; // 50%

    private GuaranteeManager() {
        this.guarantees = new HashMap<>();
        this.database = LegacyLand.getInstance().getDatabaseManager();
        this.economy = LegacyLand.getEcon();
        this.townyAPI = TownyAPI.getInstance();
    }

    public static GuaranteeManager getInstance() {
        if (instance == null) {
            instance = new GuaranteeManager();
        }
        return instance;
    }

    /**
     * 建立保卫关系
     */
    public boolean establishGuarantee(String guarantorNation, String protectedNation) {
        if (guarantorNation.equals(protectedNation)) {
            return false;
        }

        // 检查是否已经存在保卫关系
        if (hasGuarantee(guarantorNation, protectedNation)) {
            return false;
        }

        GuaranteeRelation relation = new GuaranteeRelation(guarantorNation, protectedNation);

        // 添加到保卫国的保卫列表
        guarantees.computeIfAbsent(guarantorNation, k -> new ArrayList<>()).add(relation);

        // 保存到数据库
        database.saveGuaranteeRelation(relation);

        return true;
    }

    /**
     * 取消保卫关系
     */
    public boolean removeGuarantee(String guarantorNation, String protectedNation) {
        List<GuaranteeRelation> guarantorList = guarantees.get(guarantorNation);
        if (guarantorList == null) {
            return false;
        }

        boolean removed = guarantorList.removeIf(r ->
                r.getGuarantorNation().equals(guarantorNation) &&
                r.getProtectedNation().equals(protectedNation));

        if (removed) {
            database.deleteGuaranteeRelation(guarantorNation, protectedNation);
        }

        return removed;
    }

    /**
     * 检查是否存在保卫关系
     */
    public boolean hasGuarantee(String guarantorNation, String protectedNation) {
        List<GuaranteeRelation> guarantorList = guarantees.get(guarantorNation);
        if (guarantorList == null) {
            return false;
        }

        return guarantorList.stream()
                .anyMatch(r -> r.getProtectedNation().equals(protectedNation) && r.isActive());
    }

    /**
     * 获取保卫国保卫的所有国家
     */
    public List<String> getGuaranteedNations(String guarantorNation) {
        return guarantees.getOrDefault(guarantorNation, new ArrayList<>()).stream()
                .filter(GuaranteeRelation::isActive)
                .map(GuaranteeRelation::getProtectedNation)
                .collect(Collectors.toList());
    }

    /**
     * 获取保护某个国家的所有保卫国
     */
    public List<String> getGuarantors(String protectedNation) {
        List<String> guarantors = new ArrayList<>();
        for (Map.Entry<String, List<GuaranteeRelation>> entry : guarantees.entrySet()) {
            for (GuaranteeRelation relation : entry.getValue()) {
                if (relation.getProtectedNation().equals(protectedNation) && relation.isActive()) {
                    guarantors.add(relation.getGuarantorNation());
                }
            }
        }
        return guarantors;
    }

    /**
     * 支付维持费用
     */
    public boolean payMaintenanceCost(GuaranteeRelation relation) {
        Nation guarantorNation = townyAPI.getNation(relation.getGuarantorNation());
        if (guarantorNation == null) {
            return false;
        }

        // 检查国家账户余额
        if (!economy.has(guarantorNation.getName(), MAINTENANCE_COST_MONEY)) {
            relation.setActive(false);
            database.updateGuaranteeRelation(relation);
            return false;
        }

        // 扣除金钱
        economy.withdrawPlayer(guarantorNation.getName(), MAINTENANCE_COST_MONEY);

        // TODO: 扣除 Trade_XP (需要实现 Trade_XP 系统)
        // 暂时跳过 Trade_XP 扣除

        // 更新维持费用支付时间
        relation.updateMaintenanceTime();
        database.updateGuaranteeRelation(relation);

        return true;
    }

    /**
     * 检查并支付所有保卫关系的维持费用
     */
    public void checkAndPayMaintenance() {
        for (List<GuaranteeRelation> relationList : guarantees.values()) {
            for (GuaranteeRelation relation : relationList) {
                if (relation.isActive() && relation.needsMaintenance()) {
                    if (!payMaintenanceCost(relation)) {
                        // 支付失败，通知相关国家
                        Bukkit.broadcastMessage(LanguageManager.getInstance().translate("guarantee.maintenance_failed", relation.getGuarantorNation(), relation.getProtectedNation()));
                    }
                }
            }
        }
    }

    /**
     * 处理违约惩罚
     */
    public void handleBetrayal(String guarantorNation, String protectedNation) {
        Nation nation = townyAPI.getNation(guarantorNation);
        if (nation == null) {
            return;
        }

        // 扣除国家国库 50% 余额
        double balance = economy.getBalance(nation.getName());
        double penalty = balance * BETRAYAL_PENALTY_RATE;
        economy.withdrawPlayer(nation.getName(), penalty);

        // 取消保卫关系
        removeGuarantee(guarantorNation, protectedNation);

        // 全服公告
        Bukkit.broadcastMessage(LanguageManager.getInstance().translate("guarantee.betrayal_title"));
        Bukkit.broadcastMessage(LanguageManager.getInstance().translate("guarantee.betrayal_attack", guarantorNation, protectedNation));
        Bukkit.broadcastMessage(LanguageManager.getInstance().translate("guarantee.betrayal_penalty", guarantorNation, String.format("%.2f", penalty)));
    }

    /**
     * 检查国家是否受保护
     */
    public boolean isProtected(String nationName) {
        return !getGuarantors(nationName).isEmpty();
    }

    /**
     * 获取所有激活的保卫关系
     */
    public List<GuaranteeRelation> getActiveGuarantees() {
        List<GuaranteeRelation> activeGuarantees = new ArrayList<>();
        for (List<GuaranteeRelation> relationList : guarantees.values()) {
            activeGuarantees.addAll(relationList.stream()
                    .filter(GuaranteeRelation::isActive)
                    .collect(Collectors.toList()));
        }
        return activeGuarantees;
    }

    /**
     * 清除国家的所有保卫关系
     */
    public void clearNationGuarantees(String nationName) {
        // 清除作为保卫国的关系
        guarantees.remove(nationName);

        // 清除作为受保护国的关系
        for (List<GuaranteeRelation> relationList : guarantees.values()) {
            relationList.removeIf(r -> r.getProtectedNation().equals(nationName));
        }

        database.deleteNationGuarantees(nationName);
    }

    /**
     * 获取保卫关系
     */
    public GuaranteeRelation getGuarantee(String guarantorNation, String protectedNation) {
        List<GuaranteeRelation> guarantorList = guarantees.get(guarantorNation);
        if (guarantorList == null) {
            return null;
        }

        return guarantorList.stream()
                .filter(r -> r.getProtectedNation().equals(protectedNation))
                .findFirst()
                .orElse(null);
    }

    /**
     * 加载所有保卫关系
     */
    public void loadAll() {
        if (database != null) {
            Map<String, List<GuaranteeRelation>> loadedGuarantees = database.loadAllGuarantees();
            guarantees.putAll(loadedGuarantees);
        }
    }
}
