package net.chen.legacyLand.war;

import lombok.Data;

import java.util.UUID;

/**
 * 战争参与者
 */
@Data
public class WarParticipant {
    private final UUID playerId;
    private final String playerName;
    private final String townName;
    private final WarRole role;
    private int supplies;
    private boolean active;

    public WarParticipant(UUID playerId, String playerName, String townName, WarRole role, int supplies) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.townName = townName;
        this.role = role;
        this.supplies = supplies;
        this.active = true;
    }

    /**
     * 消耗补给
     */
    public boolean consumeSupply(int amount) {
        if (supplies >= amount) {
            supplies -= amount;
            if (supplies <= 0) {
                active = false;
            }
            return true;
        }
        return false;
    }

    /**
     * 补充补给
     */
    public void addSupply(int amount) {
        supplies += amount;
        if (supplies > 0) {
            active = true;
        }
    }
}
