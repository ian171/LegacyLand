# Changelog

## [1.0-Beta2.1] - 2026-02-27

### Fixed

- `OrganizationManager.createOutpost()` 权限检查错误（误用 `DELETE_OUTPOST` → 修正为 `CREATE_OUTPOST`）

### Added

**组织系统 × 国家权限整合**

- `NationPermission` 新增 `MANAGE_ORGANIZATION`（管理国家组织）
- `NationRole.KINGDOM` / `GOVERNOR` 默认持有 `MANAGE_ORGANIZATION` 权限
- `OrganizationPermission` 新增 `CREATE_OUTPOST`（创建据点），MANAGER 角色默认获得该权限
- 创建国家组织时强制校验操作者的 `MANAGE_ORGANIZATION` 权限
- 解散国家组织时同样校验 `MANAGE_ORGANIZATION` 权限
- `OrganizationCreateResult` 新增 `NO_NATION_PERMISSION` 枚举值

**国家贸易系统重构**

- 重写 `NationTradeManager.purchaseFrom()`，修复无原子性问题
  - 事务顺序：买方扣款 → 卖方取物 → 买方存物 → 卖方收款
  - 每步失败均执行对应回滚，杜绝物品/金币丢失
- 加入买方余额预检，避免扣款后才发现余额不足
- 加入外交关系门控：敌对 / 战争状态禁止贸易
- 加入操作者权限校验：须属于交易双方之一且持有 `PROPOSE_DIPLOMACY`
- 读取并应用 `tax.default.trade` 贸易税率
- 返回值从 `void` 改为 `TradeResult` 枚举（9 种状态）
- `LegacyCommand` 对每种 `TradeResult` 给出明确提示
- 所有贸易操作写入服务端日志（操作者、双方国家、物品、价格、税率）
