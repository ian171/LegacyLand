package net.chen.legacyLand.resource.listeners;

import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.resource.*;
import net.chen.legacyLand.util.FoliaScheduler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

/**
 * 资源产出监听器
 * 根据生物群落修改矿物掉落
 */
public class BiomeResourceListener implements Listener {

    private final LegacyLand plugin;
    private final Logger logger;
    private final Random random;

    public BiomeResourceListener(LegacyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        // 如果事件已被取消，不处理
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // 只处理矿石
        if (!isOre(blockType)) {
            handleNormalDrop(event, block.getLocation());
            return;
        }

        // 创造模式不处理
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // 获取生物群落
        Biome biome = block.getBiome();
        Location location = block.getLocation();

        // 检查是否有特殊掉落配置
        Optional<BiomeResourceConfig.ResourceDrop> specialDrop =
                BiomeResourceConfig.getResourceDrop(biome, blockType);

        if (specialDrop.isPresent() && specialDrop.get().shouldDrop()) {
            // 特殊掉落 - 高纯度资源
            handleSpecialDrop(event, player, location, specialDrop.get().resourceType());
        } else {
            // 默认掉落 - 杂质资源
            ResourceType defaultType = BiomeResourceConfig.getDefaultDrop(blockType);
            if (defaultType != null) {
                handleDefaultDrop(event, player, location, defaultType);
            }
        }
    }
    private void handleNormalDrop(BlockBreakEvent event, Location location){
        int amount = random.nextInt(13);
        if (event.getBlock().getType() == Material.STONE){
            if (amount == 12) {
                event.setDropItems(false);
                location.getWorld().dropItemNaturally(location,ItemStack.of(Material.RAW_GOLD));
                return;
            }else if (amount % 4 == 3){
                event.setDropItems(false);
                location.getWorld().dropItemNaturally(location,ItemStack.of(Material.RAW_IRON));
                return;
            }
            event.setDropItems(true);
        }
    }
    /**
     * 处理特殊掉落（高纯度资源）
     */
    private void handleSpecialDrop(BlockBreakEvent event, Player player, Location location, ResourceType resourceType) {
        // 取消默认掉落
        event.setDropItems(false);

        // 在玩家所在区域的线程上执行掉落（Folia 线程安全）
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            try {
                // 创建特殊资源物品
                ItemStack resourceItem = ResourceItemFactory.createResourceItem(resourceType, 1);

                // 掉落物品
                location.getWorld().dropItemNaturally(location, resourceItem);

                // 发送消息
                player.sendMessage("§a§l[资源系统] §e你发现了 " + resourceType.getColoredName() + "§e！");
                player.sendMessage("§7这是一种稀有资源，可以在国家熔炉中精炼。");

            } catch (Exception e) {
                logger.warning("处理特殊资源掉落时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理默认掉落（杂质资源）
     */
    private void handleDefaultDrop(BlockBreakEvent event, Player player, Location location, ResourceType resourceType) {
        // 取消默认掉落
        event.setDropItems(false);

        // 在玩家所在区域的线程上执行掉落（Folia 线程安全）
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            try {
                // 创建杂质资源物品
                ItemStack resourceItem = ResourceItemFactory.createResourceItem(resourceType, 1);

                // 掉落物品
                location.getWorld().dropItemNaturally(location, resourceItem);

            } catch (Exception e) {
                logger.warning("处理默认资源掉落时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 检查是否是矿石
     */
    private boolean isOre(Material material) {
        return switch (material) {
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
            default -> false;
        };
    }
}
