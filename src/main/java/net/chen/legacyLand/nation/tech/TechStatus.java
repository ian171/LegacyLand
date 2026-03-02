package net.chen.legacyLand.nation.tech;

/**
 * 科技节点研究状态
 */
public enum TechStatus {
    LOCKED,      // 前置未完成，无法研究
    AVAILABLE,   // 可以研究
    COMPLETED    // 已完成
}
