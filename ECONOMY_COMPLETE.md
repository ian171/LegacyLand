# LegacyLand 经济系统完整文档

## 系统概述

LegacyLand 经济系统是一个四阶段渐进式的复杂经济模拟系统，实现了从基础货币到宏观调控的完整经济生态。

### 核心特性

- **多国货币体系**：每个国家拥有独立货币和国库
- **汇率浮动机制**：基于储备金和信用系数的动态汇率
- **信用扩张**：部分准备金制度，银行可创造货币
- **期货交易**：资源远期合约，对冲价格风险
- **宏观统计**：M0/M1/M2、GDP、通胀率实时监控
- **经济战争**：禁运、制裁、通胀攻击等经济手段

---

## 阶段 1：基础货币系统

### 1.1 国库管理

#### 创建国库
```
/economy create
```
- 只有国王可以创建
- 初始储备金：0
- 初始信用系数：1.0

#### 获取国库印章
```
/economy seal
```
- 国库印章用于铸造货币
- 只有国王可以获取
- 印章不会被消耗

#### 查看国库信息
```
/economy info [国家]
```
显示：
- 储备金（SBC Reserve）
- 已发行货币（Currency Issued）
- 信用系数（Credit Score）
- 当前汇率

### 1.2 货币铸造

#### 命令铸造
```
/economy mint <面值>
```
- 需要持有国库印章
- 自动生成序列号
- 增加已发行货币量

#### 工作台合成
**配方**：国库印章 + 纸张 + 墨囊
1. 在工作台放入材料
2. 系统提示输入面值
3. 在聊天框输入数字
4. 获得货币物品

**注意**：
- 印章不会被消耗
- 纸张和墨囊会被消耗
- 输入 `cancel` 取消

### 1.3 储备金管理

#### 存入储备金
```
/economy deposit <数量>
```
- 手持金锭执行
- 增加国库储备金
- 提高汇率

#### 提取储备金
```
/economy withdraw <数量>
```
- 只有国王可以提取
- 减少国库储备金
- 降低汇率

### 1.4 汇率系统

#### 汇率计算公式
```
汇率 = (储备金 / 已发行货币) × 信用系数
```

#### 查看汇率
```
/economy rate [国家]
```
- 不指定国家：显示所有国家汇率
- 指定国家：显示该国详细汇率

#### 汇率影响因素
1. **储备金增加** → 汇率上升
2. **货币发行增加** → 汇率下降
3. **信用系数** → 直接影响汇率

---

## 阶段 2：银行与跨国交易

### 2.1 银行系统

#### 存款（M0 → M1）
```
/bank deposit
```
- 手持货币物品执行
- 实体货币转为电子余额
- 不占用背包空间

#### 取款（M1 → M0）
```
/bank withdraw <金额> [国家]
```
- 电子余额转为实体货币
- 生成新的货币物品
- 序列号格式：BANK-时间戳

#### 查询余额
```
/bank balance [国家]
```
- 不指定国家：显示所有余额
- 指定国家：显示该国余额

### 2.2 转账系统

#### 国内转账
```
/bank transfer <玩家> <金额> [国家]
```
- 即时到账
- 无手续费
- 记录交易历史

### 2.3 跨国兑换

#### 货币兑换
```
/bank exchange <源国家> <目标国家> <金额>
```

**计算流程**：
1. 源货币 → SBC：`金额 × 源国汇率`
2. 扣除关税（15%）：`SBC × 0.85`
3. SBC → 目标货币：`扣税后SBC ÷ 目标国汇率`

**示例**：
```
假设：
- 中华帝国汇率：1 NC = 0.5 SBC
- 日本帝国汇率：1 NC = 0.4 SBC

兑换 100 中华帝国货币：
1. 100 × 0.5 = 50 SBC
2. 50 × 0.85 = 42.5 SBC（扣除15%关税）
3. 42.5 ÷ 0.4 = 106.25 日本帝国货币
```

---

## 阶段 3：信用扩张与期货

### 3.1 贷款系统

#### 申请贷款
```
/finance loan <金额> [国家]
```

**贷款条件**：
- 银行有足够可贷资金
- 玩家无未还清贷款
- 可贷资金 = 总存款 × 90% - 已贷出

**贷款参数**：
- 利率：5% 年化
- 期限：30 天
- 应还总额 = 本金 × 1.05

#### 还款
```
/finance repay <贷款ID> <金额>
```
- 可以部分还款
- 还清后状态变为 "已还清"
- 还款实际上是销毁货币

#### 查看贷款
```
/finance loans
```
显示：
- 贷款 ID
- 本金
- 应还总额
- 已还金额
- 剩余金额
- 状态

### 3.2 部分准备金制度

**原理**：
- 银行保留 10% 存款作为准备金
- 其余 90% 可以贷出
- 贷款创造新的货币（M2）

**货币层次**：
- **M0**：实体货币（已发行货币）
- **M1**：M0 + 活期存款
- **M2**：M1 + 贷款（信用扩张）

**示例**：
```
初始：玩家 A 存入 1000 NC
- M0: 1000
- M1: 1000
- M2: 1000

银行贷款给玩家 B：900 NC
- M0: 1000（不变）
- M1: 1000 + 900 = 1900
- M2: 1900

货币乘数 = 1 / 准备金率 = 1 / 0.1 = 10
理论最大 M2 = 1000 × 10 = 10000
```

### 3.3 期货系统

#### 创建期货合约
```
/finance future <物品> <数量> <价格> <交割天数>
```

**示例**：
```
/finance future DIAMOND 64 1000 7
```
- 卖方承诺 7 天后交付 64 个钻石
- 买方支付 1000 货币
- 锁定未来价格

#### 购买期货
```
/finance buy <合约ID>
```
- 立即支付价格
- 资金转给卖方
- 等待交割日期

#### 交割期货
```
/finance deliver <合约ID>
```
- 卖方到期后交付物品
- 需要持有足够物品
- 物品转移给买方

#### 领取物品
```
/finance claim <合约ID>
```
- 买方领取已交割的物品
- 完成整个期货流程

#### 查看期货市场
```
/finance futures
```
显示所有开放的期货合约

---

## 阶段 4：宏观调控与经济战争

### 4.1 经济统计

#### 查看经济数据
```
/ecowar stats [国家]
```

显示：
- **M0**：实体货币总量
- **M1**：M0 + 银行存款
- **M2**：M1 + 贷款
- **GDP**：过去 24 小时交易总额
- **通胀率**：货币供应量变化率
- **汇率**：当前兑换比率

#### GDP 排行榜
```
/ecowar gdp
```
- 显示所有国家 GDP 排名
- 反映经济活跃度

#### 通胀率监控
```
/ecowar inflation [国家]
```

**通胀率等级**：
- **> 50%**：严重通货膨胀（触发熔断）
- **10% ~ 50%**：温和通货膨胀
- **-10% ~ 10%**：经济稳定
- **-30% ~ -10%**：温和通货紧缩
- **< -30%**：严重通货紧缩（触发熔断）

### 4.2 熔断机制

**触发条件**：
- 通胀率 > 50%
- 通缩率 < -30%

**熔断效果**：
1. 暂停该国所有货币交易
2. 禁止新增贷款
3. 冻结期货交易
4. 全服广播警告

**解除条件**：
- 国王采取措施稳定经济
- 通胀率回归正常区间

### 4.3 经济战争

#### 资源禁运
```
/ecowar embargo <目标国家> <天数>
```

**效果**：
- 禁止与目标国家进行货币兑换
- 禁止跨国转账
- 禁止期货交易

**用途**：
- 经济孤立
- 削弱敌国贸易
- 施加政治压力

#### 解除禁运
```
/ecowar lift <目标国家>
```
- 只有发起国可以解除
- 恢复正常贸易

#### 经济制裁
```
/ecowar sanction <目标国家> <额外关税率>
```

**效果**：
- 提高与目标国的贸易关税
- 原关税 15% + 额外关税
- 增加贸易成本

**示例**：
```
/ecowar sanction 日本帝国 0.2

原关税：15%
新关税：15% + 20% = 35%
```

#### 查看经济战争
```
/ecowar wars
```
显示：
- 当前进行中的经济战争
- 禁运列表
- 制裁列表

---

## 数据库结构

### treasuries（国库表）
| 字段 | 类型 | 说明 |
|------|------|------|
| nation_name | TEXT | 国家名称（主键） |
| sbc_reserve | REAL | 储备金 |
| currency_issued | REAL | 已发行货币 |
| credit_score | REAL | 信用系数 |
| created_at | INTEGER | 创建时间 |
| last_updated | INTEGER | 最后更新时间 |

### bank_accounts（银行账户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| player_uuid | TEXT | 玩家 UUID |
| nation_name | TEXT | 国家名称 |
| balance | REAL | 余额 |
| created_at | INTEGER | 创建时间 |
| last_transaction | INTEGER | 最后交易时间 |

**主键**：(player_uuid, nation_name)

### transactions（交易记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 自增 ID |
| type | TEXT | 交易类型 |
| from_player | TEXT | 转出玩家 |
| to_player | TEXT | 转入玩家 |
| nation_name | TEXT | 国家名称 |
| amount | REAL | 金额 |
| description | TEXT | 描述 |
| timestamp | INTEGER | 时间戳 |

**交易类型**：
- DEPOSIT：存款
- WITHDRAW：取款
- TRANSFER：转账
- EXCHANGE：兑换

### loans（贷款表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 自增 ID |
| player_uuid | TEXT | 玩家 UUID |
| nation_name | TEXT | 国家名称 |
| amount | REAL | 贷款金额 |
| interest_rate | REAL | 利率 |
| issued_at | INTEGER | 发放时间 |
| due_at | INTEGER | 到期时间 |
| repaid_amount | REAL | 已还金额 |
| status | TEXT | 状态 |

**状态**：
- active：未还清
- repaid：已还清

### futures（期货表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 自增 ID |
| seller_uuid | TEXT | 卖方 UUID |
| buyer_uuid | TEXT | 买方 UUID |
| nation_name | TEXT | 国家名称 |
| material | TEXT | 物品类型 |
| amount | INTEGER | 数量 |
| price | REAL | 价格 |
| delivery_date | INTEGER | 交割日期 |
| created_at | INTEGER | 创建时间 |
| status | TEXT | 状态 |

**状态**：
- open：待购买
- sold：已售出
- delivered：已交割
- claimed：已领取

### economy_stats（经济统计表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 自增 ID |
| nation_name | TEXT | 国家名称 |
| timestamp | INTEGER | 时间戳 |
| m0 | REAL | 实体货币 |
| m1 | REAL | M0 + 存款 |
| m2 | REAL | M1 + 贷款 |
| gdp | REAL | GDP |
| inflation_rate | REAL | 通胀率 |
| exchange_rate | REAL | 汇率 |

### economy_wars（经济战争表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 自增 ID |
| attacker_nation | TEXT | 攻击国 |
| target_nation | TEXT | 目标国 |
| war_type | TEXT | 战争类型 |
| started_at | INTEGER | 开始时间 |
| ended_at | INTEGER | 结束时间 |
| status | TEXT | 状态 |

**战争类型**：
- INFLATION_ATTACK：通胀攻击
- EMBARGO：资源禁运
- SANCTION：经济制裁

---

## 命令总览

### 基础货币（/economy）
| 命令 | 说明 | 权限 |
|------|------|------|
| /economy create | 创建国库 | 国王 |
| /economy seal | 获取国库印章 | 国王 |
| /economy mint <面值> | 铸造货币 | 持有印章 |
| /economy info [国家] | 查看国库信息 | 所有人 |
| /economy deposit <数量> | 存入储备金 | 国王 |
| /economy withdraw <数量> | 提取储备金 | 国王 |
| /economy rate [国家] | 查看汇率 | 所有人 |

### 银行系统（/bank）
| 命令 | 说明 | 权限 |
|------|------|------|
| /bank deposit | 存款 | 所有人 |
| /bank withdraw <金额> [国家] | 取款 | 所有人 |
| /bank balance [国家] | 查询余额 | 所有人 |
| /bank transfer <玩家> <金额> [国家] | 转账 | 所有人 |
| /bank exchange <源> <目标> <金额> | 兑换 | 所有人 |

### 金融系统（/finance）
| 命令 | 说明 | 权限 |
|------|------|------|
| /finance loan <金额> [国家] | 申请贷款 | 所有人 |
| /finance repay <ID> <金额> | 还款 | 所有人 |
| /finance loans | 查看贷款 | 所有人 |
| /finance future <物品> <数量> <价格> <天数> | 创建期货 | 所有人 |
| /finance buy <ID> | 购买期货 | 所有人 |
| /finance deliver <ID> | 交割期货 | 卖方 |
| /finance claim <ID> | 领取物品 | 买方 |
| /finance futures | 查看期货市场 | 所有人 |

### 经济战争（/ecowar）
| 命令 | 说明 | 权限 |
|------|------|------|
| /ecowar stats [国家] | 查看经济统计 | 所有人 |
| /ecowar gdp | GDP 排行榜 | 所有人 |
| /ecowar inflation [国家] | 查看通胀率 | 所有人 |
| /ecowar embargo <目标> <天数> | 实施禁运 | 国王 |
| /ecowar lift <目标> | 解除禁运 | 国王 |
| /ecowar sanction <目标> <关税> | 经济制裁 | 国王 |
| /ecowar wars | 查看经济战争 | 所有人 |

---

## 经济策略指南

### 提高汇率策略

1. **增加储备金**
   - 国王存入更多金锭
   - 提高货币含金量
   - 增强国际信心

2. **减少货币发行**
   - 控制铸币数量
   - 回收市场货币
   - 紧缩货币政策

3. **提高信用系数**
   - 保持经济稳定
   - 避免通货膨胀
   - 建立良好信誉

### 套利机会

**汇率差套利**：
```
假设：
A 国汇率：1 NC = 0.8 SBC
B 国汇率：1 NC = 0.5 SBC

操作：
1. 用 100 A 国货币兑换 B 国
2. 100 × 0.8 = 80 SBC
3. 80 × 0.85 = 68 SBC（扣税）
4. 68 ÷ 0.5 = 136 B 国货币

收益：36%（扣除关税后）
```

### 经济战争策略

**通胀攻击**：
1. 大量兑换目标国货币
2. 在市场上抛售
3. 导致货币贬值
4. 削弱敌国经济

**资源禁运**：
1. 切断贸易通道
2. 限制资源流通
3. 提高敌国成本
4. 迫使谈判

**经济制裁**：
1. 提高贸易关税
2. 增加交易成本
3. 减少贸易量
4. 孤立目标国

---

## 常见问题

### Q: 如何提高国家汇率？
A: 增加储备金或减少货币发行量。汇率 = 储备金 / 已发行货币 × 信用系数。

### Q: 贷款有什么风险？
A: 需要按时还款，否则影响信用。贷款创造货币，过度贷款会导致通货膨胀。

### Q: 期货合约有什么用？
A: 锁定未来价格，对冲价格风险。卖方提前获得资金，买方保证供应。

### Q: 熔断机制如何解除？
A: 国王需要采取措施稳定经济，如增加储备金、回收货币、控制贷款等。

### Q: 经济战争会有什么后果？
A: 可能导致贸易中断、货币贬值、经济衰退，需要谨慎使用。

---

## 开发信息

**版本**：1.0-Beta2.0
**数据库**：SQLite (economy.db)
**依赖**：Towny, Folia/Paper
**作者**：LegacyLand Team

**文件结构**：
```
net.chen.legacyLand.economy/
├── EconomyDatabase.java          # 数据库管理
├── TreasuryManager.java           # 国库管理
├── BankManager.java               # 银行管理
├── LoanManager.java               # 贷款管理
├── FuturesManager.java            # 期货管理
├── EconomyStatsManager.java       # 统计管理
├── EconomyWarManager.java         # 经济战争管理
├── CurrencyItem.java              # 货币物品
├── commands/
│   ├── EconomyCommand.java        # 基础货币命令
│   ├── BankCommand.java           # 银行命令
│   ├── FinanceCommand.java        # 金融命令
│   └── EcoWarCommand.java         # 经济战争命令
└── listeners/
    ├── CurrencyCraftListener.java # 合成监听
    ├── CurrencyMintSession.java   # 铸币会话
    └── CurrencyMintChatListener.java # 聊天监听
```

---

## 更新日志

### v1.0-Beta2.0 (2025-03-03)
- ✅ 实现四阶段经济系统
- ✅ 基础货币与国库管理
- ✅ 银行系统与跨国交易
- ✅ 信用扩张与期货交易
- ✅ 宏观调控与经济战争
- ✅ 完整的数据库持久化
- ✅ 熔断机制与风险控制

---

**祝你在 LegacyLand 建立繁荣的经济帝国！**
