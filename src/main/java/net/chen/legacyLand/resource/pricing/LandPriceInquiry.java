package net.chen.legacyLand.resource.pricing;

import java.util.UUID;

/**
 * 一次地价询问记录（P3）。
 * <p>
 * 由 {@code /landprice ask} 创建，等待目标地块所属城镇任一成员通过
 * {@code /landprice reply} 给出报价。{@link LandPriceManager} 内存持有，
 * 超过 TTL 自动清理。
 */
public record LandPriceInquiry(
        long id,
        UUID askerId,
        String askerName,
        String chunkKey,
        String world,
        int chunkX,
        int chunkZ,
        String message,
        long createdAt,
        UUID repliedBy,
        String repliedByName,
        Double quotedPrice,
        long repliedAt
) {

    public LandPriceInquiry withReply(UUID replier, String replierName, double price, long now) {
        return new LandPriceInquiry(id, askerId, askerName, chunkKey, world, chunkX, chunkZ,
                message, createdAt, replier, replierName, price, now);
    }

    public boolean isReplied() {
        return quotedPrice != null;
    }
}
