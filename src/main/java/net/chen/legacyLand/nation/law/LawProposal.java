package net.chen.legacyLand.nation.law;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 法令投票提案
 */
@Getter
public class LawProposal {
    private final String id;
    private final String nationName;
    private final LawType type;
    private final String paramsJson;
    private final String proposedBy;
    private final long proposedAt;
    private final long voteDeadline;
    private boolean closed;

    // 内存中的投票记录（持久化在 law_votes 表）
    private final Map<UUID, Boolean> votes = new HashMap<>();

    public LawProposal(String id, String nationName, LawType type, String paramsJson,
                       String proposedBy, long proposedAt, long voteDeadline, boolean closed) {
        this.id = id;
        this.nationName = nationName;
        this.type = type;
        this.paramsJson = paramsJson;
        this.proposedBy = proposedBy;
        this.proposedAt = proposedAt;
        this.voteDeadline = voteDeadline;
        this.closed = closed;
    }

    public void addVote(UUID playerUuid, boolean approve) {
        votes.put(playerUuid, approve);
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > voteDeadline;
    }

    public int getApproveCount() {
        return (int) votes.values().stream().filter(v -> v).count();
    }

    public int getRejectCount() {
        return (int) votes.values().stream().filter(v -> !v).count();
    }

    public boolean isPassed(int totalVoters) {
        if (totalVoters == 0) return false;
        return getApproveCount() > totalVoters / 2;
    }
}
