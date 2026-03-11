package net.chen.legacyLand.resource;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * 物流价值链计算器
 * 计算运输成本和保险费用
 */
public final class LogisticsCalculator {

    // 基础运输费率（每格距离每单位重量）
    private static final double BASE_RATE = 0.1;

    // 保险费率（运输费用的百分比）
    private static final double INSURANCE_RATE = 0.05;

    // 最小运输费用
    private static final double MIN_FEE = 1.0;

    /**
     * 计算两点之间的运输费用
     *
     * @param from 起点
     * @param to 终点
     * @param items 运输的物品
     * @return 运输费用（包含保险）
     */
    public static TransportCost calculateTransportCost(Location from, Location to, ItemStack... items) {
        if (!from.getWorld().equals(to.getWorld())) {
            throw new IllegalArgumentException("起点和终点必须在同一世界");
        }

        // 计算距离
        double distance = from.distance(to);

        // 计算总重量
        double totalWeight = 0.0;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            ResourceType resourceType = ResourceItemFactory.getResourceType(item);
            double weight = resourceType != null ? resourceType.getWeight() : 1.0;
            totalWeight += weight * item.getAmount();
        }

        // 计算基础运输费
        double baseFee = Math.max(MIN_FEE, distance * totalWeight * BASE_RATE);

        // 计算保险费
        double insuranceFee = baseFee * INSURANCE_RATE;

        // 总费用
        double totalFee = baseFee + insuranceFee;

        return new TransportCost(distance, totalWeight, baseFee, insuranceFee, totalFee);
    }

    /**
     * 计算运输费用（简化版，只传入重量）
     */
    public static double calculateSimpleCost(Location from, Location to, double weight) {
        if (!from.getWorld().equals(to.getWorld())) {
            return 0.0;
        }

        double distance = from.distance(to);
        double baseFee = Math.max(MIN_FEE, distance * weight * BASE_RATE);
        double insuranceFee = baseFee * INSURANCE_RATE;

        return baseFee + insuranceFee;
    }

    /**
     * 运输成本详情
     */
    public record TransportCost(
            double distance,
            double weight,
            double baseFee,
            double insuranceFee,
            double totalFee
    ) {
        public String getDetailedInfo() {
            return String.format(
                    "§e运输详情:\n" +
                    "§7距离: §f%.1f 格\n" +
                    "§7重量: §f%.2f 单位\n" +
                    "§7基础运费: §a$%.2f\n" +
                    "§7保险费: §a$%.2f\n" +
                    "§e总费用: §6$%.2f",
                    distance, weight, baseFee, insuranceFee, totalFee
            );
        }

        @Override
        public @NonNull String toString() {
            return String.format(
                    "§e运输详情:\n" +
                            "§7距离: §f%.1f 格\n" +
                            "§7重量: §f%.2f 单位\n" +
                            "§7基础运费: §a$%.2f\n" +
                            "§7保险费: §a$%.2f\n" +
                            "§e总费用: §6$%.2f",
                    distance, weight, baseFee, insuranceFee, totalFee
            );
        }
    }
}
