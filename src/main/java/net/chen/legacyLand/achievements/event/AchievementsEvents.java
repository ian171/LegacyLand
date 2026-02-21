package net.chen.legacyLand.achievements.event;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AchievementsEvents {
    public static void init(){
    }

    // ========== 矿工类事件 ==========

    @Getter
    public static class PlayerObtainRareOre extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material oreType;
        private final int totalMined;

        public PlayerObtainRareOre(Player player, Material oreType, int totalMined) {
            this.player = player;
            this.oreType = oreType;
            this.totalMined = totalMined;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerMineStone extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final int totalMined;

        public PlayerMineStone(Player player, int totalMined) {
            this.player = player;
            this.totalMined = totalMined;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerMineCoal extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final int totalMined;

        public PlayerMineCoal(Player player, int totalMined) {
            this.player = player;
            this.totalMined = totalMined;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 伐木工类事件 ==========

    @Getter
    public static class PlayerObtainBranches extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final int totalCollected;

        public PlayerObtainBranches(Player player, int totalCollected) {
            this.player = player;
            this.totalCollected = totalCollected;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerChopLog extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material logType;
        private final int totalChopped;

        public PlayerChopLog(Player player, Material logType, int totalChopped) {
            this.player = player;
            this.logType = logType;
            this.totalChopped = totalChopped;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 农业类事件 ==========

    @Getter
    public static class PlayerObtainSeedFirstly extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material seedType;

        public PlayerObtainSeedFirstly(Player player, Material seedType) {
            this.player = player;
            this.seedType = seedType;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerHarvestCrop extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material cropType;
        private final int totalHarvested;

        public PlayerHarvestCrop(Player player, Material cropType, int totalHarvested) {
            this.player = player;
            this.cropType = cropType;
            this.totalHarvested = totalHarvested;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerPlantCrop extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material cropType;
        private final int totalPlanted;

        public PlayerPlantCrop(Player player, Material cropType, int totalPlanted) {
            this.player = player;
            this.cropType = cropType;
            this.totalPlanted = totalPlanted;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 战斗类事件 ==========

    @Getter
    public static class PlayerKillMonster extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final EntityType monsterType;
        private final int totalKills;

        public PlayerKillMonster(Player player, EntityType monsterType, int totalKills) {
            this.player = player;
            this.monsterType = monsterType;
            this.totalKills = totalKills;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerCollectLoot extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material lootType;
        private final int totalCollected;

        public PlayerCollectLoot(Player player, Material lootType, int totalCollected) {
            this.player = player;
            this.lootType = lootType;
            this.totalCollected = totalCollected;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 钓鱼类事件 ==========

    @Getter
    public static class PlayerCatchFish extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material fishType;
        private final int totalCaught;
        private final boolean isRare;

        public PlayerCatchFish(Player player, Material fishType, int totalCaught, boolean isRare) {
            this.player = player;
            this.fishType = fishType;
            this.totalCaught = totalCaught;
            this.isRare = isRare;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 附魔类事件 ==========

    @Getter
    public static class PlayerEnchantItem extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material itemType;
        private final int totalEnchanted;

        public PlayerEnchantItem(Player player, Material itemType, int totalEnchanted) {
            this.player = player;
            this.itemType = itemType;
            this.totalEnchanted = totalEnchanted;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 药水酿造类事件 ==========

    @Getter
    public static class PlayerBrewPotion extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final int totalBrewed;

        public PlayerBrewPotion(Player player, int totalBrewed) {
            this.player = player;
            this.totalBrewed = totalBrewed;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 驯服类事件 ==========

    @Getter
    public static class PlayerTameAnimal extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final EntityType animalType;
        private final int totalTamed;

        public PlayerTameAnimal(Player player, EntityType animalType, int totalTamed) {
            this.player = player;
            this.animalType = animalType;
            this.totalTamed = totalTamed;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 修复类事件 ==========

    @Getter
    public static class PlayerRepairItem extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final Material itemType;
        private final int totalRepaired;

        public PlayerRepairItem(Player player, Material itemType, int totalRepaired) {
            this.player = player;
            this.itemType = itemType;
            this.totalRepaired = totalRepaired;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 探索类事件 ==========

    @Getter
    public static class PlayerVisitBiome extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final String biomeName;
        private final int totalVisited;

        public PlayerVisitBiome(Player player, String biomeName, int totalVisited) {
            this.player = player;
            this.biomeName = biomeName;
            this.totalVisited = totalVisited;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @Getter
    public static class PlayerEnterDimension extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final String dimensionName;

        public PlayerEnterDimension(Player player, String dimensionName) {
            this.player = player;
            this.dimensionName = dimensionName;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    // ========== 建筑类事件 ==========

    @Getter
    public static class PlayerPlaceBlocks extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;
        private final int totalPlaced;

        public PlayerPlaceBlocks(Player player, int totalPlaced) {
            this.player = player;
            this.totalPlaced = totalPlaced;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }
}
