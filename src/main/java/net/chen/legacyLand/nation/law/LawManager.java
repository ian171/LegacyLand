package net.chen.legacyLand.nation.law;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import lombok.Getter;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.NationPermission;
import net.chen.legacyLand.nation.NationRole;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 法令管理器 - 处理法令颁布、投票、过期等逻辑
 */
public class LawManager {

    /**
     * -- GETTER --
     * 获取已初始化的实例（不创建新实例）
     */
    @Getter
    private static LawManager instance;

    private final LegacyLand plugin;
    private final Logger logger;
    private Connection connection;

    // 内存缓存
    private final Map<String, List<ActiveLaw>> activeLaws = new ConcurrentHashMap<>();
    private final Map<String, List<LawProposal>> openProposals = new ConcurrentHashMap<>();
    private final Map<String, LawProposal> proposalById = new ConcurrentHashMap<>();

    private LawManager(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static LawManager getInstance(LegacyLand plugin) {
        if (instance == null && plugin != null) {
            instance = new LawManager(plugin);
        }
        return instance;
    }

    // ===================== 初始化 =====================

    public void init(Connection conn) {
        this.connection = conn;
        createTables();
        loadAll();
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nation_laws (
                    id TEXT PRIMARY KEY,
                    nation_name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    params TEXT NOT NULL DEFAULT '{}',
                    enacted_by TEXT NOT NULL,
                    enacted_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL DEFAULT 0,
                    active INTEGER NOT NULL DEFAULT 1
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS law_proposals (
                    id TEXT PRIMARY KEY,
                    nation_name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    params TEXT NOT NULL DEFAULT '{}',
                    proposed_by TEXT NOT NULL,
                    proposed_at INTEGER NOT NULL,
                    vote_deadline INTEGER NOT NULL,
                    closed INTEGER NOT NULL DEFAULT 0
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS law_votes (
                    proposal_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    approve INTEGER NOT NULL,
                    voted_at INTEGER NOT NULL,
                    PRIMARY KEY (proposal_id, player_uuid)
                )""");
        } catch (SQLException e) {
            logger.severe("[Law] 创建数据库表失败: " + e.getMessage());
        }
    }

    private void loadAll() {
        loadActiveLaws();
        loadOpenProposals();
    }

    private void loadActiveLaws() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM nation_laws WHERE active = 1")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ActiveLaw law = new ActiveLaw(
                        rs.getString("id"),
                        rs.getString("nation_name"),
                        LawType.valueOf(rs.getString("type")),
                        rs.getString("params"),
                        rs.getString("enacted_by"),
                        rs.getLong("enacted_at"),
                        rs.getLong("expires_at")
                );
                activeLaws.computeIfAbsent(law.nationName(), k -> new ArrayList<>()).add(law);
            }
        } catch (SQLException e) {
            logger.severe("[Law] 加载法令失败: " + e.getMessage());
        }
    }

    private void loadOpenProposals() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM law_proposals WHERE closed = 0")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LawProposal proposal = new LawProposal(
                        rs.getString("id"),
                        rs.getString("nation_name"),
                        LawType.valueOf(rs.getString("type")),
                        rs.getString("params"),
                        rs.getString("proposed_by"),
                        rs.getLong("proposed_at"),
                        rs.getLong("vote_deadline"),
                        false
                );
                openProposals.computeIfAbsent(proposal.getNationName(), k -> new ArrayList<>()).add(proposal);
                proposalById.put(proposal.getId(), proposal);
            }
        } catch (SQLException e) {
            logger.severe("[Law] 加载提案失败: " + e.getMessage());
        }
        for (LawProposal proposal : proposalById.values()) {
            loadVotesForProposal(proposal);
        }
    }

    private void loadVotesForProposal(LawProposal proposal) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM law_votes WHERE proposal_id = ?")) {
            ps.setString(1, proposal.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                proposal.addVote(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getInt("approve") == 1
                );
            }
        } catch (SQLException e) {
            logger.severe("[Law] 加载投票记录失败: " + e.getMessage());
        }
    }

    // ===================== 结果枚举 =====================

    public enum EnactResult { SUCCESS, NO_PERMISSION, INVALID_PARAMS, DB_ERROR }
    public enum ProposeResult { SUCCESS, NO_PERMISSION, INVALID_PARAMS, DB_ERROR }
    public enum VoteResult { SUCCESS, NOT_FOUND, ALREADY_VOTED, NO_PERMISSION, CLOSED }

    // ===================== 核心方法 =====================

    /**
     * 直接颁布法令（封建/军事独裁政体，需要 ENACT_LAW 权限）
     */
    public EnactResult enactLaw(Player player, String nationName, LawType type,
                                String paramsJson, long durationHours) {
        if (!NationManager.getInstance().hasPermission(player, NationPermission.ENACT_LAW)) {
            return EnactResult.NO_PERMISSION;
        }
        if (TownyAPI.getInstance().getNation(nationName) == null) {
            return EnactResult.INVALID_PARAMS;
        }

        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expiresAt = durationHours > 0 ? now + durationHours * 3_600_000L : 0;

        ActiveLaw law = new ActiveLaw(id, nationName, type, paramsJson,
                player.getName(), now, expiresAt);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nation_laws (id,nation_name,type,params,enacted_by,enacted_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, id);
            ps.setString(2, nationName);
            ps.setString(3, type.name());
            ps.setString(4, paramsJson);
            ps.setString(5, player.getName());
            ps.setLong(6, now);
            ps.setLong(7, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Law] 保存法令失败: " + e.getMessage());
            return EnactResult.DB_ERROR;
        }
        activeLaws.computeIfAbsent(nationName, k -> new ArrayList<>()).add(law);
        logger.info("[Law] " + player.getName() + " 在 " + nationName + " 颁布了 " + type.getDisplayName());
        return EnactResult.SUCCESS;
    }

    /**
     * 提交法令提案（需要投票的政体，需要 PROPOSE_LAW 权限）
     */
    public ProposeResult proposeLaw(Player player, String nationName, LawType type,
                                    String paramsJson, long voteHours) {
        if (!NationManager.getInstance().hasPermission(player, NationPermission.PROPOSE_LAW)) {
            return ProposeResult.NO_PERMISSION;
        }
        if (TownyAPI.getInstance().getNation(nationName) == null) {
            return ProposeResult.INVALID_PARAMS;
        }

        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long deadline = now + voteHours * 3_600_000L;

        LawProposal proposal = new LawProposal(id, nationName, type, paramsJson,
                player.getName(), now, deadline, false);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO law_proposals (id,nation_name,type,params,proposed_by,proposed_at,vote_deadline,closed) VALUES (?,?,?,?,?,?,?,0)")) {
            ps.setString(1, id);
            ps.setString(2, nationName);
            ps.setString(3, type.name());
            ps.setString(4, paramsJson);
            ps.setString(5, player.getName());
            ps.setLong(6, now);
            ps.setLong(7, deadline);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Law] 保存提案失败: " + e.getMessage());
            return ProposeResult.DB_ERROR;
        }
        openProposals.computeIfAbsent(nationName, k -> new ArrayList<>()).add(proposal);
        proposalById.put(id, proposal);
        logger.info("[Law] " + player.getName() + " 在 " + nationName + " 提交了 " + type.getDisplayName() + " 提案 (id=" + id + ")");
        return ProposeResult.SUCCESS;
    }

    /**
     * 投票（需要 VOTE_LAW 权限）
     */
    public VoteResult castVote(Player player, String proposalId, boolean approve) {
        LawProposal proposal = proposalById.get(proposalId);
        if (proposal == null) return VoteResult.NOT_FOUND;
        if (proposal.isClosed()) return VoteResult.CLOSED;
        if (!NationManager.getInstance().hasPermission(player, NationPermission.VOTE_LAW)) {
            return VoteResult.NO_PERMISSION;
        }
        if (proposal.getVotes().containsKey(player.getUniqueId())) return VoteResult.ALREADY_VOTED;

        proposal.addVote(player.getUniqueId(), approve);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO law_votes (proposal_id,player_uuid,approve,voted_at) VALUES (?,?,?,?)")) {
            ps.setString(1, proposalId);
            ps.setString(2, player.getUniqueId().toString());
            ps.setInt(3, approve ? 1 : 0);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Law] 保存投票失败: " + e.getMessage());
        }
        checkProposalResult(proposal);
        return VoteResult.SUCCESS;
    }

    /**
     * 废除法令（需要 ENACT_LAW 权限）
     */
    public boolean repealLaw(Player player, String nationName, String lawId) {
        if (!NationManager.getInstance().hasPermission(player, NationPermission.ENACT_LAW)) {
            return false;
        }
        List<ActiveLaw> laws = activeLaws.getOrDefault(nationName, Collections.emptyList());
        ActiveLaw target = laws.stream().filter(l -> l.id().equals(lawId)).findFirst().orElse(null);
        if (target == null) return false;

        laws.remove(target);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE nation_laws SET active = 0 WHERE id = ?")) {
            ps.setString(1, lawId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Law] 废除法令失败: " + e.getMessage());
        }
        logger.info("[Law] " + player.getName() + " 废除了 " + nationName + " 的法令: " + target.type().getDisplayName());
        return true;
    }

    // ===================== 查询方法 =====================

    public boolean hasActiveLaw(String nationName, LawType type) {
        return activeLaws.getOrDefault(nationName, Collections.emptyList())
                .stream().anyMatch(l -> l.type() == type && !l.isExpired());
    }

    public double getLawParamDouble(String nationName, LawType type, String key, double defaultValue) {
        for (ActiveLaw law : activeLaws.getOrDefault(nationName, Collections.emptyList())) {
            if (law.type() == type && !law.isExpired()) {
                try {
                    JsonObject obj = JsonParser.parseString(law.paramsJson()).getAsJsonObject();
                    if (obj.has(key)) return obj.get(key).getAsDouble();
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    public String getLawParamString(String nationName, LawType type, String key, String defaultValue) {
        for (ActiveLaw law : activeLaws.getOrDefault(nationName, Collections.emptyList())) {
            if (law.type() == type && !law.isExpired()) {
                try {
                    JsonObject obj = JsonParser.parseString(law.paramsJson()).getAsJsonObject();
                    if (obj.has(key)) return obj.get(key).getAsString();
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    public List<ActiveLaw> getActiveLaws(String nationName) {
        return activeLaws.getOrDefault(nationName, Collections.emptyList())
                .stream().filter(l -> !l.isExpired()).collect(Collectors.toList());
    }

    public List<LawProposal> getOpenProposals(String nationName) {
        return openProposals.getOrDefault(nationName, Collections.emptyList())
                .stream().filter(p -> !p.isClosed()).collect(Collectors.toList());
    }

    public LawProposal getProposalById(String id) {
        return proposalById.get(id);
    }

    // ===================== 定时任务 =====================

    public void checkExpiredLaws() {
        for (Map.Entry<String, List<ActiveLaw>> entry : activeLaws.entrySet()) {
            List<ActiveLaw> expired = entry.getValue().stream()
                    .filter(ActiveLaw::isExpired).collect(Collectors.toList());
            for (ActiveLaw law : expired) {
                entry.getValue().remove(law);
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE nation_laws SET active = 0 WHERE id = ?")) {
                    ps.setString(1, law.id());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    logger.warning("[Law] 清理过期法令失败: " + e.getMessage());
                }
                logger.info("[Law] 法令已过期: " + law.nationName() + " - " + law.type().getDisplayName());
            }
        }
    }

    public void checkVoteDeadlines() {
        for (List<LawProposal> proposals : openProposals.values()) {
            new ArrayList<>(proposals).stream()
                    .filter(p -> !p.isClosed() && p.isExpired())
                    .forEach(this::checkProposalResult);
        }
    }

    // ===================== 内部辅助 =====================

    private void checkProposalResult(LawProposal proposal) {
        Nation nation = TownyAPI.getInstance().getNation(proposal.getNationName());
        if (nation == null) {
            closeProposal(proposal);
            return;
        }
        int totalVoters = countEligibleVoters(proposal.getNationName());
        boolean passed = proposal.isPassed(totalVoters);

        if (passed || proposal.isExpired()) {
            closeProposal(proposal);
            if (passed) {
                enactFromProposal(proposal);
            } else {
                logger.info("[Law] 提案未通过: " + proposal.getNationName() + " - " + proposal.getType().getDisplayName());
            }
        }
    }

    private void enactFromProposal(LawProposal proposal) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long defaultHours = plugin.getConfig().getLong("law.vote-duration-hours", 24);
        long expiresAt = now + defaultHours * 3_600_000L;

        ActiveLaw law = new ActiveLaw(id, proposal.getNationName(), proposal.getType(),
                proposal.getParamsJson(), proposal.getProposedBy(), now, expiresAt);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nation_laws (id,nation_name,type,params,enacted_by,enacted_at,expires_at,active) VALUES (?,?,?,?,?,?,?,1)")) {
            ps.setString(1, id);
            ps.setString(2, proposal.getNationName());
            ps.setString(3, proposal.getType().name());
            ps.setString(4, proposal.getParamsJson());
            ps.setString(5, proposal.getProposedBy());
            ps.setLong(6, now);
            ps.setLong(7, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Law] 提案通过后保存法令失败: " + e.getMessage());
            return;
        }
        activeLaws.computeIfAbsent(proposal.getNationName(), k -> new ArrayList<>()).add(law);
        logger.info("[Law] 提案通过，法令已颁布: " + proposal.getNationName() + " - " + proposal.getType().getDisplayName());
    }

    private void closeProposal(LawProposal proposal) {
        proposal.setClosed(true);
        openProposals.getOrDefault(proposal.getNationName(), Collections.emptyList()).remove(proposal);
        proposalById.remove(proposal.getId());
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE law_proposals SET closed = 1 WHERE id = ?")) {
            ps.setString(1, proposal.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[Law] 关闭提案失败: " + e.getMessage());
        }
    }

    private int countEligibleVoters(String nationName) {
        Nation nation = TownyAPI.getInstance().getNation(nationName);
        if (nation == null) return 1;
        int count = 0;
        for (Resident resident : nation.getResidents()) {
            NationRole role = NationManager.getInstance().getPlayerRole(nationName, resident.getUUID());
            if (role == NationRole.PARLIAMENT_MEMBER || (role != null && role.isLeader())) {
                count++;
            }
        }
        return count == 0 ? Math.max(1, nation.getResidents().size() / 2) : count;
    }
}
