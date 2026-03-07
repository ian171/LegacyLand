package net.chen.legacyLand.nation;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 国库取物申请
 */
public record TreasuryRequest(String requestId, UUID playerId, String nationName, ItemStack requestedItem,
                              long requestTime) {
}
