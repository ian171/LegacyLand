package net.chen.legacyLand.achievements.listener;

import net.chen.legacyLand.achievements.AchievementManager;
import net.chen.legacyLand.achievements.Achievements;
import net.chen.legacyLand.achievements.event.AchievementsEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class PlayerAchievementsListener implements Listener {

    // 玩家统计数据追踪
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    private PlayerStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
    }

    // ========== 自定义事件监听器 ==========

    @EventHandler
    public void onPlayerHasRareOre(AchievementsEvents.PlayerObtainRareOre event) {
        Player player = event.getPlayer();
        grantAchievement(player, Achievements.UNCOVERED_ORES);
    }

    @EventHandler
    public void onPlayerObtainBranches(AchievementsEvents.PlayerObtainBranches event) {
        Player player = event.getPlayer();
        grantAchievement(player, Achievements.RUBBISH_BAGGER);
    }

    @EventHandler
    public void onPlayerObtainSeedFirstly(AchievementsEvents.PlayerObtainSeedFirstly event) {
        Player player = event.getPlayer();
        grantAchievement(player, Achievements.FANNY_FARMER);
    }

    @EventHandler
    public void onPlayerMineStone(AchievementsEvents.PlayerMineStone event) {
        if (event.getTotalMined() >= 100) {
            grantAchievement(event.getPlayer(), Achievements.STONE_AGE);
        }
    }

    @EventHandler
    public void onPlayerMineCoal(AchievementsEvents.PlayerMineCoal event) {
        if (event.getTotalMined() >= 50) {
            grantAchievement(event.getPlayer(), Achievements.COAL_MINER);
        }
    }

    @EventHandler
    public void onPlayerChopLog(AchievementsEvents.PlayerChopLog event) {
        if (event.getTotalChopped() >= 100) {
            grantAchievement(event.getPlayer(), Achievements.LUMBERJACK);
        }
    }

    @EventHandler
    public void onPlayerHarvestCrop(AchievementsEvents.PlayerHarvestCrop event) {
        if (event.getTotalHarvested() == 1) {
            grantAchievement(event.getPlayer(), Achievements.FIRST_HARVEST);
        }
    }

    @EventHandler
    public void onPlayerPlantCrop(AchievementsEvents.PlayerPlantCrop event) {
        if (event.getTotalPlanted() >= 50) {
            grantAchievement(event.getPlayer(), Achievements.GREEN_THUMB);
        }
    }

    @EventHandler
    public void onPlayerKillMonster(AchievementsEvents.PlayerKillMonster event) {
        if (event.getTotalKills() >= 100) {
            grantAchievement(event.getPlayer(), Achievements.MONSTER_SLAYER);
        }
    }

    @EventHandler
    public void onPlayerCollectLoot(AchievementsEvents.PlayerCollectLoot event) {
        Material loot = event.getLootType();
        int total = event.getTotalCollected();

        switch (loot) {
            case BONE -> {
                if (total >= 50) grantAchievement(event.getPlayer(), Achievements.BONE_COLLECTOR);
            }
            case GUNPOWDER -> {
                if (total >= 30) grantAchievement(event.getPlayer(), Achievements.GUNPOWDER_EXPERT);
            }
            case ENDER_PEARL -> {
                if (total >= 10) grantAchievement(event.getPlayer(), Achievements.ENDER_HUNTER);
            }
            case BLAZE_ROD -> {
                if (total >= 10) grantAchievement(event.getPlayer(), Achievements.BLAZE_SLAYER);
            }
        }
    }

    @EventHandler
    public void onPlayerCatchFish(AchievementsEvents.PlayerCatchFish event) {
        if (event.getTotalCaught() == 1) {
            grantAchievement(event.getPlayer(), Achievements.FIRST_CATCH);
        }
        if (event.getTotalCaught() >= 50) {
            grantAchievement(event.getPlayer(), Achievements.FISHERMAN);
        }
        if (event.isRare()) {
            grantAchievement(event.getPlayer(), Achievements.MASTER_ANGLER);
        }
    }

    @EventHandler
    public void onPlayerEnchantItem(AchievementsEvents.PlayerEnchantItem event) {
        if (event.getTotalEnchanted() >= 10) {
            grantAchievement(event.getPlayer(), Achievements.ENCHANTER);
        }
    }

    @EventHandler
    public void onPlayerBrewPotion(AchievementsEvents.PlayerBrewPotion event) {
        if (event.getTotalBrewed() >= 10) {
            grantAchievement(event.getPlayer(), Achievements.POTION_BREWER);
        }
    }

    @EventHandler
    public void onPlayerTameAnimal(AchievementsEvents.PlayerTameAnimal event) {
        if (event.getTotalTamed() >= 5) {
            grantAchievement(event.getPlayer(), Achievements.ANIMAL_TAMER);
        }
    }

    @EventHandler
    public void onPlayerRepairItem(AchievementsEvents.PlayerRepairItem event) {
        if (event.getTotalRepaired() >= 10) {
            grantAchievement(event.getPlayer(), Achievements.BLACKSMITH);
        }
    }

    @EventHandler
    public void onPlayerVisitBiome(AchievementsEvents.PlayerVisitBiome event) {
        if (event.getTotalVisited() >= 5) {
            grantAchievement(event.getPlayer(), Achievements.EXPLORER);
        }
    }

    @EventHandler
    public void onPlayerEnterDimension(AchievementsEvents.PlayerEnterDimension event) {
        String dimension = event.getDimensionName();
        if (dimension.equals("NETHER")) {
            grantAchievement(event.getPlayer(), Achievements.NETHER_TRAVELER);
        } else if (dimension.equals("THE_END")) {
            grantAchievement(event.getPlayer(), Achievements.END_VOYAGER);
        }
    }

    @EventHandler
    public void onPlayerPlaceBlocks(AchievementsEvents.PlayerPlaceBlocks event) {
        if (event.getTotalPlaced() >= 1000) {
            grantAchievement(event.getPlayer(), Achievements.ARCHITECT);
        }
        if (event.getTotalPlaced() >= 5000) {
            grantAchievement(event.getPlayer(), Achievements.MASTER_BUILDER);
        }
    }

    // ========== 原生事件监听器（触发自定义事件）==========

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material block = event.getBlock().getType();
        PlayerStats stats = getStats(player.getUniqueId());

        // 矿石挖掘
        switch (block) {
            case COBBLESTONE, STONE -> {
                stats.cobblestoneCount++;
                Bukkit.getPluginManager().callEvent(
                    new AchievementsEvents.PlayerMineStone(player, stats.cobblestoneCount));
            }
            case COAL_ORE, DEEPSLATE_COAL_ORE -> {
                stats.coalCount++;
                Bukkit.getPluginManager().callEvent(
                    new AchievementsEvents.PlayerMineCoal(player, stats.coalCount));
            }
            case IRON_ORE, DEEPSLATE_IRON_ORE -> {
                stats.ironCount++;
                stats.oreTypes.add(block);
                if (stats.oreTypes.size() >= 10) {
                    Bukkit.getPluginManager().callEvent(
                        new AchievementsEvents.PlayerObtainRareOre(player, block, stats.oreTypes.size()));
                }
            }
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> {
                stats.goldCount++;
                stats.oreTypes.add(Material.GOLD_ORE);
            }
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> {
                stats.diamondCount++;
                stats.oreTypes.add(Material.DIAMOND_ORE);
            }
            case ANCIENT_DEBRIS -> {
                stats.ancientDebrisCount++;
                stats.oreTypes.add(block);
            }
        }

        // 伐木
        if (isLog(block)) {
            stats.logCount++;
            stats.woodTypes.add(block);
            Bukkit.getPluginManager().callEvent(
                new AchievementsEvents.PlayerChopLog(player, block, stats.logCount));

            if (stats.woodTypes.size() >= 5) {
                grantAchievement(player, Achievements.WOOD_COLLECTOR);
            }
            if (stats.woodTypes.size() >= 10) {
                grantAchievement(player, Achievements.MASTER_LUMBERJACK);
            }
        }

        // 农业
        if (isCrop(block)) {
            stats.cropCount++;
            stats.cropTypes.add(block);
            Bukkit.getPluginManager().callEvent(
                new AchievementsEvents.PlayerHarvestCrop(player, block, stats.cropCount));

            if (stats.cropTypes.size() >= 8) {
                grantAchievement(player, Achievements.MASTER_FARMER);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = getStats(player.getUniqueId());

        stats.blocksPlaced++;
        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerPlaceBlocks(player, stats.blocksPlaced));

        // 种植作物
        Material block = event.getBlock().getType();
        if (isSeed(block)) {
            stats.cropsPlanted++;
            Bukkit.getPluginManager().callEvent(
                new AchievementsEvents.PlayerPlantCrop(player, block, stats.cropsPlanted));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;

        Player player = event.getEntity().getKiller();
        PlayerStats stats = getStats(player.getUniqueId());
        EntityType type = event.getEntityType();

        // 怪物击杀
        if (isMonster(type)) {
            stats.monsterKills++;
            Bukkit.getPluginManager().callEvent(
                new AchievementsEvents.PlayerKillMonster(player, type, stats.monsterKills));
        }

        // 战利品收集
        event.getDrops().forEach(item -> {
            Material mat = item.getType();
            int amount = item.getAmount();

            switch (mat) {
                case BONE -> {
                    stats.boneCount += amount;
                    Bukkit.getPluginManager().callEvent(
                        new AchievementsEvents.PlayerCollectLoot(player, mat, stats.boneCount));
                }
                case GUNPOWDER -> {
                    stats.gunpowderCount += amount;
                    Bukkit.getPluginManager().callEvent(
                        new AchievementsEvents.PlayerCollectLoot(player, mat, stats.gunpowderCount));
                }
                case ENDER_PEARL -> {
                    stats.enderPearlCount += amount;
                    Bukkit.getPluginManager().callEvent(
                        new AchievementsEvents.PlayerCollectLoot(player, mat, stats.enderPearlCount));
                }
                case BLAZE_ROD -> {
                    stats.blazeRodCount += amount;
                    Bukkit.getPluginManager().callEvent(
                        new AchievementsEvents.PlayerCollectLoot(player, mat, stats.blazeRodCount));
                }
            }
        });
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        PlayerStats stats = getStats(player.getUniqueId());

        stats.fishCaught++;
        boolean isRare = event.getCaught() != null &&
                        event.getCaught().getType() == org.bukkit.entity.EntityType.ITEM;

        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerCatchFish(player, Material.COD, stats.fishCaught, isRare));
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        PlayerStats stats = getStats(player.getUniqueId());

        stats.enchantCount++;
        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerEnchantItem(player, event.getItem().getType(), stats.enchantCount));
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (event.getContents().getViewers().isEmpty()) return;

        Player player = (Player) event.getContents().getViewers().getFirst();
        PlayerStats stats = getStats(player.getUniqueId());

        stats.potionsBrewed++;
        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerBrewPotion(player, stats.potionsBrewed));
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;

        PlayerStats stats = getStats(player.getUniqueId());
        stats.animalsTamed++;

        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerTameAnimal(player, event.getEntityType(), stats.animalsTamed));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getEnvironment().name();

        Bukkit.getPluginManager().callEvent(
            new AchievementsEvents.PlayerEnterDimension(player, worldName));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = getStats(player.getUniqueId());

        String biomeName = player.getLocation().getBlock().getBiome().getKey().toString();
        if (stats.visitedBiomes.add(biomeName)) {
            Bukkit.getPluginManager().callEvent(
                new AchievementsEvents.PlayerVisitBiome(player, biomeName, stats.visitedBiomes.size()));
        }
    }

    // ========== 辅助方法 ==========

    private void grantAchievement(Player player, Achievements achievement) {
        AchievementManager manager = AchievementManager.getInstance();
        if (manager != null && !manager.hasAchievement(player.getUniqueId(), achievement)) {
            manager.grantAchievement(player.getUniqueId(), achievement);
            player.sendMessage(Component.text("🏆 恭喜你获得成就: ", NamedTextColor.GOLD)
                    .append(Component.text(achievement.getName(), NamedTextColor.YELLOW)));
            player.sendActionBar(Component.text(achievement.getDescription(), NamedTextColor.GREEN));
        }
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_STEM");
    }

    private boolean isCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS ||
               material == Material.POTATOES || material == Material.BEETROOTS ||
               material == Material.MELON || material == Material.PUMPKIN ||
               material == Material.SUGAR_CANE || material == Material.CACTUS;
    }

    private boolean isSeed(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS ||
               material == Material.POTATOES || material == Material.BEETROOTS;
    }

    private boolean isMonster(EntityType type) {
        return type == EntityType.ZOMBIE || type == EntityType.SKELETON ||
               type == EntityType.CREEPER || type == EntityType.SPIDER ||
               type == EntityType.ENDERMAN || type == EntityType.BLAZE ||
               type == EntityType.WITCH || type == EntityType.SLIME;
    }

    // ========== 玩家统计数据类 ==========

    private static class PlayerStats {
        // 矿工统计
        int cobblestoneCount = 0;
        int coalCount = 0;
        int ironCount = 0;
        int goldCount = 0;
        int diamondCount = 0;
        int ancientDebrisCount = 0;
        Set<Material> oreTypes = new HashSet<>();

        // 伐木工统计
        int logCount = 0;
        Set<Material> woodTypes = new HashSet<>();

        // 农业统计
        int cropCount = 0;
        int cropsPlanted = 0;
        Set<Material> cropTypes = new HashSet<>();

        // 建筑统计
        int blocksPlaced = 0;

        // 战斗统计
        int monsterKills = 0;
        int boneCount = 0;
        int gunpowderCount = 0;
        int enderPearlCount = 0;
        int blazeRodCount = 0;

        // 其他统计
        int fishCaught = 0;
        int enchantCount = 0;
        int potionsBrewed = 0;
        int animalsTamed = 0;

        // 探索统计
        Set<String> visitedBiomes = new HashSet<>();
    }
}
