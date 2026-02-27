package net.chen.legacyLand.nation.tech;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.law.LawManager;
import net.chen.legacyLand.nation.law.LawType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 科技树管理器
 */
public class TechManager {

    private static TechManager instance;

    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;

    // 内存缓存
    private final Map<String, NationTechState> techStates = new ConcurrentHashMap<>();
    private final Map<String, TechNode> nodeDefinitions = new LinkedHashMap<>();
    private final Map<String, List<TechNode>> lineNodes = new LinkedHashMap<>();
    private final Map<String, Set<String>> governmentLocks = new HashMap<>();

    private TechManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static TechManager getInstance(LegacyLand plugin) {
        if (instance == null && plugin != null) {
            instance = new TechManager(plugin);
        }
        return instance;
    }

    /** 获取已初始化的实例（不创建新实例） */
    public static TechManager getInstance() {
        return instance;
    }

    // ===================== 初始化 =====================

    public void loadConfig(LegacyLand plugin) {
        File techFile = new File(plugin.getDataFolder(), "tech.yml");
        if (!techFile.exists()) {
            plugin.saveResource("tech.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(techFile);
        loadNodes(cfg);
        loadGovernmentLocks(cfg);
        logger.info("[Tech] 科技树配置已加载，共 " + nodeDefinitions.size() + " 个节点。");
    }

    private void loadNodes(FileConfiguration cfg) {
        ConfigurationSection nodesSection = cfg.getConfigurationSection("nodes");
        if (nodesSection == null) return;
        for (String nodeId : nodesSection.getKeys(false)) {
            ConfigurationSection ns = nodesSection.getConfigurationSection(nodeId);
            if (ns == null) continue;
            String lineId = ns.getString("line", "UNKNOWN");
            int tier = ns.getInt("tier", 1);
            String displayName = ns.getString("display-name", nodeId);
            String description = ns.getString("description", "");
            int cost = ns.getInt("cost", 100);
            List<String> prerequisites = ns.getStringList("prerequisites");
            Map<String, Double> effects = new HashMap<>();
            ConfigurationSection effectsSection = ns.getConfigurationSection("effects");
            if (effectsSection != null) {
                for (String key : effectsSection.getKeys(false)) {
                    effects.put(key, effectsSection.getDouble(key));
                }
            }
            TechNode node = new TechNode(nodeId, lineId, tier, displayName, description, cost, prerequisites, effects);
            nodeDefinitions.put(nodeId, node);
            lineNodes.computeIfAbsent(lineId, k -> new ArrayList<>()).add(node);
        }
        // 按 tier 排序
        lineNodes.values().forEach(list -> list.sort(Comparator.comparingInt(TechNode::tier)));
    }

    private void loadGovernmentLocks(FileConfiguration cfg) {
        ConfigurationSection lockSection = cfg.getConfigurationSection("government-locks");
        if (lockSection == null) return;
        for (String govId : lockSection.getKeys(false)) {
            List<String> locked = lockSection.getStringList(govId + ".locked-lines");
            governmentLocks.put(govId.toUpperCase(), new HashSet<>(locked));
        }
    }

    public void init(Connection conn) {
        this.connection = conn;
        createTables();
        loadAll();
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_tech_state (
                    nation_name TEXT PRIMARY KEY,
                    research_points INTEGER NOT NULL DEFAULT 0
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_tech_completed (
                    nation_name TEXT NOT NULL,
                    node_id TEXT NOT NULL,
                    completed_at INTEGER NOT NULL,
                    PRIMARY KEY (nation_name, node_id)
                )""");
        } catch (SQLException e) {
            logger.severe("[Tech] 创建数据库表失败: " + e.getMessage());
        }
    }

    private void loadAll() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM nation_tech_state")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("nation_name");
                int points = rs.getInt("research_points");
                techStates.put(name, new NationTechState(name, points));
            }
        } catch (SQLException e) {
            logger.severe("[Tech] 加载科技状态失败: " + e.getMessage());
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM nation_tech_completed")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("nation_name");
                String nodeId = rs.getString("node_id");
                techStates.computeIfAbsent(name, n -> new NationTechState(n, 0)).addCompleted(nodeId);
            }
        } catch (SQLException e) {
            logger.severe("[Tech] 加载已完成科技失败: " + e.getMessage());
        }
    }

    // ===================== 结果枚举 =====================

    public enum ResearchResult {
        SUCCESS, NOT_FOUND, ALREADY_COMPLETED, PREREQUISITES_NOT_MET,
        INSUFFICIENT_POINTS, LINE_LOCKED, NO_NATION
    }

    // ===================== 核心方法 =====================

    public ResearchResult research(String nationName, String nodeId) {
        TechNode node = nodeDefinitions.get(nodeId);
        if (node == null) return ResearchResult.NOT_FOUND;

        NationTechState state = getOrCreateState(nationName);
        if (state.hasCompleted(nodeId)) return ResearchResult.ALREADY_COMPLETED;

        // 检查政体锁定
        if (isLineLocked(nationName, node.lineId())) return ResearchResult.LINE_LOCKED;

        // 检查前置
        for (String prereq : node.prerequisites()) {
            if (!state.hasCompleted(prereq)) return ResearchResult.PREREQUISITES_NOT_MET;
        }

        // 检查研究点
        if (!state.deductPoints(node.cost())) return ResearchResult.INSUFFICIENT_POINTS;

        state.addCompleted(nodeId);
        persistState(state);
        persistCompleted(nationName, nodeId);
        logger.info("[Tech] " + nationName + " 完成科技: " + node.displayName());
        return ResearchResult.SUCCESS;
    }

    public TechStatus getNodeStatus(String nationName, String nodeId) {
        TechNode node = nodeDefinitions.get(nodeId);
        if (node == null) return TechStatus.LOCKED;
        NationTechState state = getOrCreateState(nationName);
        if (state.hasCompleted(nodeId)) return TechStatus.COMPLETED;
        if (isLineLocked(nationName, node.lineId())) return TechStatus.LOCKED;
        for (String prereq : node.prerequisites()) {
            if (!state.hasCompleted(prereq)) return TechStatus.LOCKED;
        }
        return TechStatus.AVAILABLE;
    }

    /** 累加所有已完成节点对指定效果键的贡献 */
    public double getTotalEffect(String nationName, String effectKey) {
        NationTechState state = techStates.get(nationName);
        if (state == null) return 0.0;
        double total = 0.0;
        for (String nodeId : state.getCompletedNodes()) {
            TechNode node = nodeDefinitions.get(nodeId);
            if (node != null) total += node.getEffect(effectKey);
        }
        return total;
    }

    public boolean hasUnlocked(String nationName, String effectKey) {
        NationTechState state = techStates.get(nationName);
        if (state == null) return false;
        for (String nodeId : state.getCompletedNodes()) {
            TechNode node = nodeDefinitions.get(nodeId);
            if (node != null && node.hasEffect(effectKey)) return true;
        }
        return false;
    }

    public void addResearchPoints(String nationName, int amount) {
        NationTechState state = getOrCreateState(nationName);
        state.addPoints(amount);
        persistState(state);
    }

    public boolean isLineLocked(String nationName, String lineId) {
        // 通过 NationManager 获取政体 ID
        String govId = net.chen.legacyLand.nation.NationManager.getInstance()
                .getPoliticalSystemId(nationName);
        if (govId == null) return false;
        Set<String> locked = governmentLocks.get(govId.toUpperCase());
        return locked != null && locked.contains(lineId.toUpperCase());
    }

    public NationTechState getState(String nationName) {
        return techStates.get(nationName);
    }

    public Map<String, TechNode> getNodeDefinitions() { return Collections.unmodifiableMap(nodeDefinitions); }
    public Map<String, List<TechNode>> getLineNodes() { return Collections.unmodifiableMap(lineNodes); }

    // ===================== 定时任务 =====================

    public void generateResearchPoints() {
        int basePoints = plugin.getConfig().getInt("tech.points-per-member", 2);
        for (Nation nation : TownyAPI.getInstance().getNations()) {
            int onlineCount = 0;
            for (Resident resident : nation.getResidents()) {
                Player p = Bukkit.getPlayer(resident.getUUID());
                if (p != null && p.isOnline()) onlineCount++;
            }
            if (onlineCount == 0) continue;

            int points = onlineCount * basePoints;
            // RESEARCH_BOOST 法令加成
            LawManager lawManager = LawManager.getInstance(null);
            if (lawManager != null && lawManager.hasActiveLaw(nation.getName(), LawType.RESEARCH_BOOST)) {
                double multiplier = lawManager.getLawParamDouble(
                        nation.getName(), LawType.RESEARCH_BOOST, "multiplier", 1.0);
                points = (int) (points * multiplier);
            }
            addResearchPoints(nation.getName(), points);
        }
    }

    // ===================== 持久化 =====================

    private void persistState(NationTechState state) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO nation_tech_state (nation_name, research_points) VALUES (?,?)")) {
            ps.setString(1, state.getNationName());
            ps.setInt(2, state.getResearchPoints());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Tech] 保存科技状态失败: " + e.getMessage());
        }
    }

    private void persistCompleted(String nationName, String nodeId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO nation_tech_completed (nation_name, node_id, completed_at) VALUES (?,?,?)")) {
            ps.setString(1, nationName);
            ps.setString(2, nodeId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Tech] 保存已完成科技失败: " + e.getMessage());
        }
    }

    private NationTechState getOrCreateState(String nationName) {
        return techStates.computeIfAbsent(nationName, n -> new NationTechState(n, 0));
    }
}
