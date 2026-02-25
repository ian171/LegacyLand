package net.chen.legacyLand.nation;

import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 国库取物申请
 */
@Data
public class TreasuryRequest {
    private final String requestId;
    private final UUID playerId;
    private final String nationName;
    private final ItemStack requestedItem;
    private final long requestTime;
}
