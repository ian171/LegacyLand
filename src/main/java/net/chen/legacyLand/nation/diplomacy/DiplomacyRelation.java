package net.chen.legacyLand.nation.diplomacy;

import lombok.Getter;
import lombok.Setter;

/**
 * 外交关系
 */
@Getter
@Setter
public class DiplomacyRelation {
    private final String nation1;
    private final String nation2;
    private RelationType relationType;
    private final long establishedTime;

    public DiplomacyRelation(String nation1, String nation2, RelationType relationType) {
        this.nation1 = nation1;
        this.nation2 = nation2;
        this.relationType = relationType;
        this.establishedTime = System.currentTimeMillis();
    }

    /**
     * 用于从数据库加载的构造函数
     */
    public DiplomacyRelation(String nation1, String nation2, RelationType relationType, long establishedTime) {
        this.nation1 = nation1;
        this.nation2 = nation2;
        this.relationType = relationType;
        this.establishedTime = establishedTime;
    }

    /**
     * 检查是否涉及指定国家
     */
    public boolean involves(String nationName) {
        return nation1.equals(nationName) || nation2.equals(nationName);
    }

    /**
     * 获取另一个国家的名称
     */
    public String getOtherNation(String nationName) {
        if (nation1.equals(nationName)) {
            return nation2;
        } else if (nation2.equals(nationName)) {
            return nation1;
        }
        return null;
    }

    /**
     * 检查两个国家是否匹配
     */
    public boolean matches(String n1, String n2) {
        return (nation1.equals(n1) && nation2.equals(n2)) ||
               (nation1.equals(n2) && nation2.equals(n1));
    }
}
