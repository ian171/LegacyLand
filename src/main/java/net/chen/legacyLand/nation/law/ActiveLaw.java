package net.chen.legacyLand.nation.law;

/**
 * 生效中的法令数据
 *
 * @param expiresAt 0 = 永久
 */
public record ActiveLaw(String id, String nationName, LawType type, String paramsJson, String enactedBy, long enactedAt,
                        long expiresAt) {

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt == 0;
    }
}
