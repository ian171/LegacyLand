package net.chen.legacyLand.economy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.util.FoliaSchedule;
import net.chen.legacyLand.util.FoliaScheduler;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 经济战争管理器
 * 实现通胀攻击、资源禁运、经济制裁
 */
public class EconomyWarManager {
    @Getter
    private static EconomyWarManager instance;
    private final LegacyLand plugin;
    private final Logger logger;
    private final EconomyDatabase database;
    private final TreasuryManager treasuryManager;

    // 禁运列表：国家 -> 被禁运的国家列表
    private final Map<String, Map<String, Long>> embargoes = new HashMap<>();

    private EconomyWarManager(LegacyLand plugin, TreasuryManager treasuryManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.database = EconomyDatabase.getInstance();
        this.treasuryManager = treasuryManager;
    }

    public static EconomyWarManager getInstance(LegacyLand plugin, TreasuryManager treasuryManager) {
        if (instance == null) {
            instance = new EconomyWarManager(plugin, treasuryManager);
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        createWarTable();
        loadEmbargoes();
        logger.info("经济战争系统已加载");
    }

    /**
     * 创建经济战争表
     */
    private void createWarTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS economy_wars (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                attacker_nation TEXT NOT NULL,
                target_nation TEXT NOT NULL,
                war_type TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                ended_at INTEGER,
                status TEXT NOT NULL DEFAULT 'active',
                FOREIGN KEY (attacker_nation) REFERENCES treasuries(nation_name),
                FOREIGN KEY (target_nation) REFERENCES treasuries(nation_name)
            )
        """;

        try (Statement stmt = database.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("经济战争表已创建");
        } catch (SQLException e) {
            logger.severe("创建经济战争表失败: " + e.getMessage());
        }
    }

    /**
     * 加载禁运列表
     */
    private void loadEmbargoes() {
        String sql = "SELECT * FROM economy_wars WHERE war_type = 'EMBARGO' AND status = 'active'";

        try (Statement stmt = database.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String attacker = rs.getString("attacker_nation");
                String target = rs.getString("target_nation");
                long startedAt = rs.getLong("started_at");

                embargoes.computeIfAbsent(attacker, k -> new HashMap<>()).put(target, startedAt);
            }
        } catch (SQLException e) {
            logger.warning("加载禁运列表失败: " + e.getMessage());
        }
    }

    /**
     * 发动通胀攻击
     * 向目标国家大量倾销货币，导致通货膨胀
     *
     * @param attackerNation 攻击国
     * @param targetNation 目标国
     * @param amount 倾销货币数量
     * @return 是否成功
     */
    public boolean launchInflationAttack(String attackerNation, String targetNation, double amount) {
        // 检查攻击国是否有足够的目标国货币
        // 这需要攻击国先通过兑换获取大量目标国货币

        // 记录经济战争
        String sql = """
            INSERT INTO economy_wars (attacker_nation, target_nation, war_type, started_at, status)
            VALUES (?, ?, 'INFLATION_ATTACK', ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, attackerNation);
            ps.setString(2, targetNation);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();

            // 广播消息
            Bukkit.broadcastMessage("§c§l[经济战争] " + attackerNation + " 对 " + targetNation + " 发动通胀攻击！");
            Bukkit.broadcastMessage("§7倾销货币数量: " + String.format("%.2f", amount));

            return true;
        } catch (SQLException e) {
            logger.severe("发动通胀攻击失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 实施资源禁运
     * 禁止与目标国家进行贸易
     *
     * @param attackerNation 攻击国
     * @param targetNation 目标国
     * @param durationDays 禁运天数
     * @return 是否成功
     */
    public boolean imposeEmbargo(String attackerNation, String targetNation, int durationDays) {
        // 检查是否已经在禁运
        if (isEmbargoed(attackerNation, targetNation)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long endTime = now + (durationDays * 24L * 60 * 60 * 1000);

        String sql = """
            INSERT INTO economy_wars (attacker_nation, target_nation, war_type, started_at, ended_at, status)
            VALUES (?, ?, 'EMBARGO', ?, ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, attackerNation);
            ps.setString(2, targetNation);
            ps.setLong(3, now);
            ps.setLong(4, endTime);
            ps.executeUpdate();

            // 更新内存缓存
            embargoes.computeIfAbsent(attackerNation, k -> new HashMap<>()).put(targetNation, now);

            // 广播消息
            Bukkit.broadcastMessage("§c§l[经济战争] " + attackerNation + " 对 " + targetNation + " 实施资源禁运！");
            Bukkit.broadcastMessage("§7禁运期限: " + durationDays + " 天");

            // 定时解除禁运
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                liftEmbargo(attackerNation, targetNation);
            }, durationDays * 24L * 60 * 60 * 20); // 转换为 tick

            return true;
        } catch (SQLException e) {
            logger.severe("实施禁运失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 解除禁运
     */
    public boolean liftEmbargo(String attackerNation, String targetNation) {
        String sql = """
            UPDATE economy_wars
            SET status = 'ended', ended_at = ?
            WHERE attacker_nation = ? AND target_nation = ? AND war_type = 'EMBARGO' AND status = 'active'
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, attackerNation);
            ps.setString(3, targetNation);
            ps.executeUpdate();

            // 更新内存缓存
            if (embargoes.containsKey(attackerNation)) {
                embargoes.get(attackerNation).remove(targetNation);
            }

            // 广播消息
            Bukkit.broadcastMessage("§a[经济战争] " + attackerNation + " 解除对 " + targetNation + " 的禁运");

            return true;
        } catch (SQLException e) {
            logger.severe("解除禁运失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否被禁运
     */
    public boolean isEmbargoed(String nation1, String nation2) {
        // 双向检查
        return (embargoes.containsKey(nation1) && embargoes.get(nation1).containsKey(nation2)) ||
               (embargoes.containsKey(nation2) && embargoes.get(nation2).containsKey(nation1));
    }

    /**
     * 实施经济制裁
     * 冻结目标国家的部分储备金
     *
     * @param attackerNation 攻击国
     * @param targetNation 目标国
     * @param freezeRatio 冻结比例 (0-1)
     * @return 是否成功
     */
    public boolean imposeSanctions(String attackerNation, String targetNation, double freezeRatio) {
        if (freezeRatio < 0 || freezeRatio > 1) {
            return false;
        }

        TreasuryManager.Treasury targetTreasury = treasuryManager.getTreasury(targetNation);
        if (targetTreasury == null) {
            return false;
        }

        double frozenAmount = targetTreasury.getSbcReserve() * freezeRatio;

        String sql = """
            INSERT INTO economy_wars (attacker_nation, target_nation, war_type, started_at, status)
            VALUES (?, ?, 'SANCTIONS', ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, attackerNation);
            ps.setString(2, targetNation);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();

            // 广播消息
            Bukkit.broadcastMessage("§c§l[经济战争] " + attackerNation + " 对 " + targetNation + " 实施经济制裁！");
            Bukkit.broadcastMessage("§7冻结储备金: " + String.format("%.2f", frozenAmount) + " SBC (" +
                String.format("%.0f%%", freezeRatio * 100) + ")");

            return true;
        } catch (SQLException e) {
            logger.severe("实施制裁失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 货币战争：操纵汇率
     * 通过大量买入/卖出来影响汇率
     */
    public boolean manipulateExchangeRate(String attackerNation, String targetNation, boolean buyOrSell, double amount) {
        // buyOrSell: true = 买入目标货币（推高汇率），false = 卖出（压低汇率）

        String action = buyOrSell ? "买入" : "卖出";

        String sql = """
            INSERT INTO economy_wars (attacker_nation, target_nation, war_type, started_at, status)
            VALUES (?, ?, 'CURRENCY_WAR', ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, attackerNation);
            ps.setString(2, targetNation);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();

            // 广播消息
            Bukkit.broadcastMessage("§c§l[经济战争] " + attackerNation + " 对 " + targetNation + " 发动货币战争！");
            Bukkit.broadcastMessage("§7操作: " + action + " " + String.format("%.2f", amount) + " 货币");

            return true;
        } catch (SQLException e) {
            logger.severe("货币战争失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 实施经济制裁
     * 提高与目标国家的贸易关税
     *
     * @param attackerNation 攻击国
     * @param targetNation 目标国
     * @param tariffRate 额外关税率（0-1）
     * @return 是否成功
     */
    public boolean imposeSanction(String attackerNation, String targetNation, double tariffRate) {
        String sql = """
            INSERT INTO economy_wars (attacker_nation, target_nation, war_type, started_at, status)
            VALUES (?, ?, 'SANCTION', ?, 'active')
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, attackerNation);
            ps.setString(2, targetNation);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();

            // 广播消息
            Bukkit.broadcastMessage("§c§l[经济战争] " + attackerNation + " 对 " + targetNation + " 实施经济制裁！");
            Bukkit.broadcastMessage("§7额外关税: " + String.format("%.1f%%", tariffRate * 100));

            return true;
        } catch (SQLException e) {
            logger.severe("实施制裁失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取国家的经济战争历史
     */
    public Map<String, Object> getWarHistory(String nationName) {
        Map<String, Object> history = new HashMap<>();
        String sql = """
            SELECT war_type, COUNT(*) as count
            FROM economy_wars
            WHERE attacker_nation = ? OR target_nation = ?
            GROUP BY war_type
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, nationName);
            ps.setString(2, nationName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                history.put(rs.getString("war_type"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            logger.warning("查询战争历史失败: " + e.getMessage());
        }

        return history;
    }
}
