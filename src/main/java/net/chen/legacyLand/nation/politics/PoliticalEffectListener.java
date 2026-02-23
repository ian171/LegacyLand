package net.chen.legacyLand.nation.politics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.chen.legacyLand.nation.NationManager;
import net.chen.legacyLand.nation.politics.effects.ParticleEffect;
import net.chen.legacyLand.nation.politics.effects.SpeedBoostEffect;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

/**
 * 政治体制效果监听器 - 处理玩家加入/离开时的效果应用
 */
public class PoliticalEffectListener implements Listener {

    private final TownyAPI townyAPI;
    private final NationManager nationManager;
    private final PoliticalSystemManager politicalSystemManager;

    public PoliticalEffectListener() {
        this.townyAPI = TownyAPI.getInstance();
        this.nationManager = NationManager.getInstance();
        this.politicalSystemManager = PoliticalSystemManager.getInstance();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Resident resident = townyAPI.getResident(player);

        if (resident == null || !resident.hasNation()) {
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            return;
        }

        // 应用国家政体的自定义效果
        PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());
        if (system == null) {
            return;
        }

        // 检查是否有速度加成效果
        if (system.customEffects().containsKey("speed-boost")) {
            Object config = system.customEffects().get("speed-boost");
            if (config instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> configMap = (java.util.Map<String, Object>) config;
                int amplifier = configMap.containsKey("amplifier")
                    ? ((Number) configMap.get("amplifier")).intValue()
                    : 0;
                SpeedBoostEffect effect = new SpeedBoostEffect(amplifier);
                effect.applyToPlayer(player);
            }
        }
        if (system.customEffects().containsKey("particle-effect")){
            Object config = system.customEffects().get("particle-effect");
            if (config instanceof Map<?,?>){
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) config;

                // 只为国王应用粒子效果
                if (nation.isKing(resident)) {
                    String particleName = (String) configMap.get("particle");
                    String patternName = (String) configMap.get("pattern");

                    if (particleName != null && patternName != null) {
                        try {
                            Particle particle = Particle.valueOf(particleName.toUpperCase());
                            ParticleEffect.ParticlePattern pattern = ParticleEffect.ParticlePattern.valueOf(patternName.toUpperCase());
                            ParticleEffect particleEffect = new ParticleEffect(particle, pattern);
                            particleEffect.startParticleEffect(player, nation.getName());
                        } catch (IllegalArgumentException e) {
                            // 无效的粒子或图案名称
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Resident resident = townyAPI.getResident(player);

        if (resident == null || !resident.hasNation()) {
            return;
        }

        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            return;
        }

        // 移除速度效果（玩家离线时自动清除）
        PoliticalSystem system = nationManager.getPoliticalSystem(nation.getName());
        if (system != null && system.customEffects().containsKey("speed-boost")) {
            SpeedBoostEffect effect = new SpeedBoostEffect(0);
            effect.removeFromPlayer(player);
        }

    }
}
