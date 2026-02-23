package net.chen.legacyLand.nation.politics;

import com.palmergames.bukkit.towny.object.Nation;

/**
 * 政体效果接口 - 预留实现接口，可通过扩展实现自定义效果
 * <p>
 * 实现此接口以定义政体切换时的自定义行为。
 * 例如：给全国玩家添加药水效果、修改城镇属性等。
 */
public interface PoliticalEffect {

    /**
     * 当国家采用此政体时触发
     *
     * @param nation 采用政体的国家
     */
    void onApply(Nation nation);

    /**
     * 当国家放弃此政体时触发（切换到其他政体前）
     *
     * @param nation 放弃政体的国家
     */
    void onRemove(Nation nation);

    /**
     * 获取效果的唯一标识
     *
     * @return 效果ID
     */
    String getId();

    /**
     * 获取效果的描述
     *
     * @return 效果描述
     */
    String getDescription();
}
