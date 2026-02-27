package net.chen.legacyLand.nation.transaction;

import com.palmergames.bukkit.towny.object.Nation;
import net.chen.legacyLand.LegacyLand;
import net.chen.legacyLand.nation.NationManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NationTradeManager {
    private final NationManager nationManager;

    public NationTradeManager(NationManager nationManager){
        this.nationManager = nationManager;
    }

    public void purchaseFrom(Nation source, Nation target, Player control, ItemStack item, int price){
        // 校验：卖方国库有物品、买方国库有空间、买方账户有余额
        if (!nationManager.withdrawFromNationTreasury(source.getName(), item)) {
            control.sendMessage("§c卖方国库中没有足够的该物品或国库不可用。");
            return;
        }

        if (!nationManager.donateToTreasury(target, item)) {
            // 回滚：物品还回卖方国库
            nationManager.donateToTreasury(source, item);
            control.sendMessage("§c买方国库已满，无法存入物品。");
            return;
        }

        try {
            target.getAccount().withdraw(price, "交易支付");
            source.getAccount().deposit(price, "交易获得");
        } catch (Exception e) {
            LegacyLand.logger.warning("❌"+target.getName()+"和"+source.getName()+"的交易金额操作失败: " + e.getMessage());
            control.sendMessage("§c交易金额操作失败。");
            return;
        }

        control.sendMessage("§a交易完成：" + item.getAmount() + " 个 " + item.getType().name()
                + "，价格 " + price + " 金币。");
    }
}
