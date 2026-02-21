package net.chen.legacyLand.player;

import lombok.Getter;
import lombok.Setter;
import net.chen.legacyLand.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 玩家管理器
 */
public class PlayerManager {
    private static PlayerManager instance;
    protected final Map<UUID, PlayerData> players;

    @Setter
    private DatabaseManager database;

    private PlayerManager() {
        this.players = new HashMap<>();
    }

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    /**
     * 获取玩家数据
     */
    public PlayerData getPlayerData(UUID playerId) {
        return players.get(playerId);
    }

    /**
     * 获取玩家数据
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * 创建玩家数据
     */
    public PlayerData createPlayerData(UUID playerId, String playerName) {
        PlayerData data = new PlayerData(playerId, playerName);
        players.put(playerId, data);
        return data;
    }

    /**
     * 加载玩家数据
     */
    public PlayerData loadPlayerData(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            // 尝试从数据库加载
            if (database != null) {
                data = database.loadPlayerData(player.getUniqueId());
            }

            // 如果数据库中也没有，创建新数据
            if (data == null) {
                data = createPlayerData(player.getUniqueId(), player.getName());
            } else {
                players.put(player.getUniqueId(), data);
            }
        }
        return data;
    }

    /**
     * 保存玩家数据
     */
    public void savePlayerData(UUID playerId) {
        PlayerData data = players.get(playerId);
        if (data != null && database != null) {
            database.savePlayerData(data);
        }
    }

    /**
     * 移除玩家数据
     */
    public void removePlayerData(UUID playerId) {
        savePlayerData(playerId);
        players.remove(playerId);
    }

    /**
     * 设置主职业
     */
    public boolean setMainProfession(UUID playerId, Profession profession) {
        PlayerData data = players.get(playerId);
        if (data == null || data.getMainProfession() != null) {
            return false;
        }

        data.setMainProfession(profession);
        data.setMaxHealth(16.0); // 选择职业后血量变为16
        return true;
    }

    /**
     * 设置副职业
     */
    public boolean setSubProfession(UUID playerId, Profession profession) {
        PlayerData data = players.get(playerId);
        if (data == null || !data.canChooseSubProfession()) {
            return false;
        }

        data.setSubProfession(profession);
        return true;
    }

    /**
     * 获取所有在线玩家数据
     */
    public Collection<PlayerData> getAllPlayerData() {
        return Collections.unmodifiableCollection(players.values());
    }
}
