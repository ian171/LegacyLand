# 更新日志 (Changelog)

## [1.0-Beta2.2] - 2026-05-17

### 🎉 新增功能 (Added)

#### 区块资源稀缺度定价系统（基于 Ricardo 级差地租）

> 灵感来自经济学中的级差地租理论：同一商品的不同生产单元因稀缺度差异产生差异化租金。
> 本系统把"矿物储量 + 群系 + 位置"映射为一个可演变的地价指数，随玩家采集而衰减。

**P1：储量普查（Lazy Survey）**
- 新增 `ChunkResourceManager`：玩家进入区块时若 DB 无记录则触发一次扫描
- 新增 `ChunkScanTask`：使用 **Palette Early-Exit** 算法（`ChunkSnapshot.isSectionEmpty(sy)`）跳过纯空气子区段，扫描性能数量级提升
- 快照在事件线程获取，扫描+持久化在 **虚拟线程** 执行，零阻塞
- 储量公式：`initialValue = Σ weights[material] × biomeFactor`
- 内置 19 种矿物权重（古老残骸 500 → 煤炭 1）与 10 种群系系数（深暗 1.5、沙漠 0.3）
- 新增数据库表 `chunk_resources`，三库同步实现（SQLite / MySQL / MongoDB）

**P2：采集追踪 + 延迟衰减**
- 新增 `ResourceBlockBreakListener`：监听 BlockBreak / BlockExplode / EntityExplode
- 设计要点：**地价不在矿物被破坏后立即计算，而是定时延迟计算**
  - BlockBreak 仅在内存 `pendingDecrements` 累加，不触发任何 DB I/O
  - 新增 `ChunkResourceRecalcTask`，默认每 60 秒（1200 ticks）批量 flush 衰减到 DB
- 防作弊：`playerPlacedBlocks` 集合标记玩家放置的矿物方块（坐标位编码），破坏时一次性消耗、跳过累加
- 爆炸衰减：TNT / 苦力怕 / 末影水晶按 `explosion-decay-factor`（默认 0.5）折算
- 新增自定义事件 `ChunkExhaustedEvent`：`currentValue` 跌至 ≤0 时触发，供 P4 BlueMap 热力图等模块订阅
- DB 层 `decrementChunkResource`：SQLite/MySQL 用 `MAX(0, ...)` / `GREATEST(0, ...)`，MongoDB 用 `$inc` + clamp，保证不下溢

**P3：地价公式 + /landprice 玩家交互**
- 新增 `LandPriceCalculator`：`V = base × (α·R + β·biomeFactor + γ·locationFactor)`
  - `locationFactor = 1 - min(1, dist / location-max-distance)`，距离取区块中心到 Town spawn 的欧氏距离
  - 默认 α=1.0、β=200.0、γ=100.0、base=1.0、max-distance=1000
- 新增命令 `/landprice`（别名 `/ld`、`/landp`）：
  - `/landprice` — 查看脚下区块地价（权限：未声明→公开；已声明→仅成员或被 show 公开）
  - `/landprice ask [留言]` — 非成员向所属城镇发起询问，自动广播给在线成员
  - `/landprice reply <ID> <价格>` — 城镇成员回复询问，私信通知 asker
  - `/landprice show` / `hide` — 切换脚下地块公开展示状态（仅成员可操作）
  - `/landprice list` — 列出脚下地块所有未过期询问
- 新增 `LandPriceManager`：维护 inquiries（TTL 1 小时自动清理）+ shownByPlayer 双索引
- `LandPriceInquiry` 使用 record + `withReply` 不可变更新

#### 国际化（i18n）
- 新增 30 条 `landprice.*` 翻译键，中英双语完整覆盖

### 🔧 配置项 (Config)

```yaml
resource-pricing:
  enabled: true
  scan:
    y-min: -64
    y-max: 64
    rescan-on-load: false
  recalc-interval-ticks: 1200      # P2 衰减刷库周期
  explosion-decay-factor: 0.5      # P2 爆炸衰减系数
  valuation:                       # P3 地价公式
    alpha: 1.0
    beta: 200.0
    gamma: 100.0
    base: 1.0
    location-max-distance: 1000.0
  inquiry-ttl-seconds: 3600        # P3 询问有效期
  weights: { ... 19 种矿物 ... }
  biome-factors: { ... 10 种群系 ... }
```

### 🗄️ 数据库变更 (Database)

- 新增表 `chunk_resources`（PRIMARY KEY `(world, chunk_x, chunk_z)`）
- 字段：`biome`、`initial_value`、`current_value`、`biome_factor`、`last_scan`
- SQLite / MySQL / MongoDB 三库同步实现，自动建表/建索引

### 🛡️ ProGuard 规则

- 新增 keep 规则：`ChunkResourceData`、`ChunkExhaustedEvent`、`LandPriceInquiry`

### 🐛 已知限制 (Known Issues)

- `playerPlacedBlocks` 标记仅内存持有，服务器重启后丢失（接受"信任重置"语义）
- 地价 V(chunk) 暂为数值指数，未与 Towny `PlotEconomy` 自动售价直接绑定（计划在 P4 接入）
- 高强度建造区块的 placed-set 体积未设上限（未来加 LRU 淘汰）

### 🔮 未来计划 (Future)

- [ ] P4：BlueMap 地价热力图叠加层 + 耗竭区块红色标记
- [ ] P5：Towny PlotEconomy 自动报价 / 国库地价税收
- [ ] PlaceholderAPI 扩展：`%legacy_landprice_here%`
- [ ] `/landprice` 区块邻近平均价格、城镇地价排行

### 📊 代码统计

- 新增文件：8 个（`ChunkResourceManager`、`ChunkScanTask`、`ChunkResourceData`、`ResourcePricingConfig`、`ChunkResourceRecalcTask`、`ChunkLoadResourceListener`、`ResourceBlockBreakListener`、`ChunkExhaustedEvent`、`LandPriceCalculator`、`LandPriceInquiry`、`LandPriceManager`）
- 修改文件：`LegacyLand`、`IDatabase`、`DatabaseManager`、`SQLiteDatabase`、`MySQLDatabase`、`MongoDatabase`、`PlayerEventListener`、`LandPriceCommand`、`config.yml`、`zh-cn.yml`、`en-us.yml`、`proguard-rules.pro`
- 编译状态：✅ BUILD SUCCESSFUL（Java 21 / Gradle 8.8）

---

## [1.0-Beta2.1] - 2025-01-XX

### 🎉 新增功能 (Added)

#### 经济系统 - 阶段 1：基础货币
- **国库系统**
  - 新增 `/economy create` - 创建国家国库
  - 新增 `/economy seal` - 获取国库印章（仅国王）
  - 新增 `/economy info [国家]` - 查看国库信息
  - 新增 `/economy deposit <数量>` - 存入储备金（金锭）
  - 新增 `/economy withdraw <数量>` - 提取储备金（仅国王）

- **货币铸造**
  - 新增 `/economy mint <面值>` - 铸造货币
  - 新增工作台合成货币功能（国库印章 + 纸张 + 墨囊）
  - 货币物品包含完整 NBT 数据（国家、面值、序列号、时间戳）

- **汇率系统**
  - 新增 `/economy rate [国家]` - 查看汇率
  - 动态汇率计算：`汇率 = (储备金 / 已发行货币) × 信用系数`
  - 支持多国货币独立运行

#### 经济系统 - 阶段 2：银行与跨国交易
- **银行系统**
  - 新增 `/bank deposit` - 存款（实体货币 → 电子余额）
  - 新增 `/bank withdraw <金额> [国家]` - 取款（电子余额 → 实体货币）
  - 新增 `/bank balance [国家]` - 查询余额
  - 新增 `/bank transfer <玩家> <金额> [国家]` - 转账
  - 实现余额缓存机制，提升查询性能

- **跨国交易**
  - 新增 `/bank exchange <源国家> <目标国家> <金额>` - 货币兑换
  - 实现 15% 跨国交易关税
  - 支持基于汇率的自动换算

- **交易记录**
  - 完整的交易历史记录（存款、取款、转账、兑换）
  - 包含时间戳、金额、描述等详细信息

#### 经济系统 - 阶段 3：信用扩张与期货
- **贷款系统**
  - 新增 `/finance loan <金额> [国家]` - 申请贷款
  - 新增 `/finance repay <贷款ID> <金额>` - 还款
  - 新增 `/finance loans` - 查看我的贷款
  - 实现部分准备金制度（10% 准备金率）
  - 贷款参数：5% 年化利率，30 天期限

- **期货交易**
  - 新增 `/finance future <物品> <数量> <价格> <交割天数>` - 创建期货合约
  - 新增 `/finance buy <合约ID>` - 购买期货
  - 新增 `/finance deliver <合约ID>` - 交割期货（卖方）
  - 新增 `/finance claim <合约ID>` - 领取物品（买方）
  - 新增 `/finance futures` - 查看期货市场

- **信用扩张**
  - 实现 M0（实体货币）、M1（M0 + 存款）、M2（M1 + 贷款）统计
  - 货币乘数效应：理论最大 M2 = M0 × 10

#### 经济系统 - 阶段 4：宏观调控与经济战争
- **经济统计**
  - 新增 `/ecowar stats [国家]` - 查看经济统计
  - 新增 `/ecowar gdp` - GDP 排行榜
  - 新增 `/ecowar inflation [国家]` - 查看通胀率
  - 自动监控系统（每小时统计一次）
  - 统计指标：M0/M1/M2、GDP、通胀率、汇率

- **熔断机制**
  - 通胀率 > 50% 触发熔断
  - 通缩率 < -30% 触发熔断
  - 熔断时暂停所有货币交易、贷款、期货
  - 全服广播警告

- **经济战争**
  - 新增 `/ecowar embargo <目标国家> <天数>` - 实施资源禁运
  - 新增 `/ecowar lift <目标国家>` - 解除禁运
  - 新增 `/ecowar sanction <目标国家> <额外关税率>` - 经济制裁
  - 新增 `/ecowar wars` - 查看经济战争列表
  - 禁运效果：禁止货币兑换、转账、期货交易

### 🗄️ 数据库 (Database)

#### 新增数据表
- `treasuries` - 国库表（储备金、已发行货币、信用系数）
- `bank_accounts` - 银行账户表（玩家余额）
- `transactions` - 交易记录表（完整历史）
- `loans` - 贷款表（本金、利率、还款记录）
- `futures` - 期货表（合约信息、交割状态）
- `economy_stats` - 经济统计表（M0/M1/M2、GDP、通胀率）
- `economy_wars` - 经济战争表（禁运、制裁记录）

#### 数据库优化
- 使用独立的 SQLite 数据库（economy.db）
- 实现外键约束确保数据一致性
- 添加索引优化查询性能
- 实现余额缓存减少数据库访问

### 🎨 用户体验 (UX)

#### 命令别名
- `/economy` → `/eco`, `/jingji`
- `/bank` → `/yinhang`
- `/finance` → `/jinrong`
- `/ecowar` → `/jingjizhanzhen`

#### 交互优化
- 工作台合成货币时通过聊天输入面值
- 支持输入 `cancel` 取消操作
- 完善的错误提示和权限检查
- 彩色消息提示（成功、警告、错误）

#### 信息展示
- 格式化的数据展示（保留 2-4 位小数）
- 清晰的表格布局
- 实时汇率显示
- 详细的统计数据

### 🔧 技术改进 (Technical)

#### 架构优化
- 实现单例模式管理器（7 个核心管理器）
- 清晰的依赖关系：Database → Treasury → Bank → Loan/Futures → Stats/War
- 模块化设计，易于扩展和维护

#### 性能优化
- 余额缓存机制（内存 + 数据库双层）
- 异步任务处理（统计监控）
- 数据库连接池管理
- 批量查询优化

#### 兼容性
- 支持 Folia 调度器（FoliaSchedule）
- 兼容 Towny 插件（国家系统集成）
- 适配虚拟线程（命令注册优化）

#### 代码质量
- 完整的 JavaDoc 注释
- 清晰的方法命名
- 异常处理和日志记录
- 事务保证数据一致性

### 🐛 修复 (Fixed)

#### 初始化问题
- 修复 `treasuryManager` 为 null 导致的 NullPointerException
- 调整初始化顺序：经济系统在虚拟线程启动前初始化
- 确保所有管理器按依赖顺序正确初始化

#### 类型冲突
- 删除重复的 `Treasury.java` 文件
- 统一使用 `TreasuryManager.Treasury` 内部类
- 修复导入语句和类型引用

#### 编译错误
- 补全 `calculateExchangeRate()` 方法实现
- 添加缺失的 `getAllBalances()` 方法
- 添加缺失的 `exchangeCurrency()` 方法
- 添加缺失的 `getPlayerLoans()` 方法
- 添加缺失的 `getOpenFutures()` 方法
- 添加缺失的 `getLatestStats()` 方法
- 添加缺失的 `getGDPRanking()` 方法
- 添加缺失的 `imposeSanction()` 方法

### 📚 文档 (Documentation)

#### 新增文档
- `ECONOMY_STAGE2.md` - 阶段 2 详细文档（银行与跨国交易）
- `ECONOMY_COMPLETE.md` - 完整经济系统文档（四阶段全覆盖）

#### 文档内容
- 完整的命令使用说明
- 详细的功能介绍
- 数据库结构说明
- 使用示例和最佳实践
- 经济策略指南

### 🎯 游戏平衡 (Balance)

#### 经济参数
- 准备金率：10%（银行可贷出 90% 存款）
- 贷款利率：5% 年化
- 贷款期限：30 天
- 跨国关税：15%
- 通胀熔断阈值：±50%/±30%

#### 权限控制
- 国库创建：仅国王
- 国库印章：仅国王
- 储备金提取：仅国王
- 货币铸造：持有印章的国家成员
- 其他功能：所有玩家

### ⚠️ 已知问题 (Known Issues)

- 期货交割需要手动触发，未实现自动交割
- 经济战争的通胀攻击功能暂未完全实现
- 熔断机制的自动解除条件需要进一步完善

### 🔮 未来计划 (Future)

- [ ] 实现股票市场系统
- [ ] 添加保险系统
- [ ] 实现央行利率调控
- [ ] 添加经济周期模拟
- [ ] 实现自动化交易机器人
- [ ] 添加经济数据可视化（图表）
- [ ] 实现跨服经济联盟

---

## 技术栈 (Tech Stack)

- **语言**: Java 21
- **服务器**: Folia 1.21.8
- **依赖**: Towny, PlaceholderAPI
- **数据库**: SQLite 3
- **构建工具**: Gradle 8.8

## 贡献者 (Contributors)

- 经济系统设计与实现：@chen
- 文档编写：@chen
- 测试与调优：待定

---

**完整更新内容**：
- 新增 7 个核心管理器
- 新增 4 个命令处理器
- 新增 3 个事件监听器
- 新增 7 张数据库表
- 新增 27 个命令
- 新增 2 份完整文档

**代码统计**：
- 新增代码：约 3500 行
- 新增文件：15 个
- 编译状态：✅ BUILD SUCCESSFUL
