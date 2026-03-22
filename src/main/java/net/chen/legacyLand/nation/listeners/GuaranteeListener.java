package net.chen.legacyLand.nation.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.events.WarStartEvent;
import net.chen.legacyLand.nation.diplomacy.GuaranteeManager;
import net.chen.legacyLand.util.LanguageManager;
import net.chen.legacyLand.war.War;
import net.chen.legacyLand.war.WarManager;
import net.chen.legacyLand.war.WarParticipant;
import net.chen.legacyLand.war.WarRole;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 外交保卫事件监听器
 */
public class GuaranteeListener implements Listener {

    private final GuaranteeManager guaranteeManager;
    private final WarManager warManager;
    private final TownyAPI townyAPI;
    private final Map<UUID, BossBar> playerBossBars;

    public GuaranteeListener() {
        this.guaranteeManager = GuaranteeManager.getInstance();
        this.warManager = WarManager.getInstance();
        this.townyAPI = TownyAPI.getInstance();
        this.playerBossBars = new HashMap<>();
    }

    /**
     * 监听战争开始事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onWarStart(WarStartEvent event) {
        War war = event.getWar();
        String defenderNation = war.getDefenderNation();

        // 检查防守方是否受保护
        List<String> guarantors = guaranteeManager.getGuarantors(defenderNation);
        if (guarantors.isEmpty()) {
            return;
        }

        // 将所有保卫国的在线玩家加入战争
        for (String guarantorNationName : guarantors) {
            Nation guarantorNation = townyAPI.getNation(guarantorNationName);
            if (guarantorNation == null) {
                continue;
            }

            // 通知保卫国
            Bukkit.broadcastMessage(LanguageManager.getInstance().translate("guarantee.join_war", guarantorNationName, defenderNation));

            // 将保卫国的在线玩家加入防守方
            for (Resident resident : guarantorNation.getResidents()) {
                Player player = Bukkit.getPlayer(resident.getUUID());
                if (player != null && player.isOnline()) {
                    // 获取玩家所在城镇
                    String townName = resident.hasTown() ? resident.getTownOrNull().getName() : "";

                    // 创建战争参与者
                    WarParticipant participant = new WarParticipant(
                            player.getUniqueId(),
                            player.getName(),
                            townName,
                            WarRole.SOLDIER,
                            100 // 初始补给
                    );
                    war.getDefenders().put(player.getUniqueId(), participant);
                    player.sendMessage(LanguageManager.getInstance().translate("guarantee.joined_war", defenderNation));
                }
            }
        }
    }

    /**
     * 监听玩家攻击事件 - 检查违约
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // 获取攻击者和受害者的国家
        Resident attackerResident = townyAPI.getResident(attacker);
        Resident victimResident = townyAPI.getResident(victim);

        if (attackerResident == null || !attackerResident.hasNation() ||
            victimResident == null || !victimResident.hasNation()) {
            return;
        }

        Nation attackerNation = attackerResident.getNationOrNull();
        Nation victimNation = victimResident.getNationOrNull();

        if (attackerNation == null || victimNation == null) {
            return;
        }

        // 检查攻击者的国家是否保卫受害者的国家
        if (guaranteeManager.hasGuarantee(attackerNation.getName(), victimNation.getName())) {
            // 触发违约惩罚
            guaranteeManager.handleBetrayal(attackerNation.getName(), victimNation.getName());
            attacker.sendMessage(LanguageManager.getInstance().translate("guarantee.betrayal_warning"));
        }
    }

    /**
     * 监听玩家移动事件 - 显示 BossBar
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Resident resident = townyAPI.getResident(player);

        if (resident == null || !resident.hasNation()) {
            // 移除 BossBar
            removeBossBar(player);
            return;
        }

        Nation playerNation = resident.getNationOrNull();
        if (playerNation == null) {
            removeBossBar(player);
            return;
        }

        // 检查玩家是否在受保护的领地中
        List<String> guarantors = guaranteeManager.getGuarantors(playerNation.getName());
        if (!guarantors.isEmpty()) {
            // 显示 BossBar
            showBossBar(player, LanguageManager.getInstance().translate("guarantee.protected_status", String.join(", ", guarantors)));
        } else {
            removeBossBar(player);
        }
    }

    /**
     * 显示 BossBar
     */
    private void showBossBar(Player player, String message) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(message, BarColor.GREEN, BarStyle.SOLID);
            bossBar.addPlayer(player);
            playerBossBars.put(player.getUniqueId(), bossBar);
        } else {
            bossBar.setTitle(message);
        }
    }

    /**
     * 移除 BossBar
     */
    private void removeBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
}
