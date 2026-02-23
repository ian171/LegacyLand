# LegacyLand - 国家系统完整实现文档

## 版本信息
- **插件版本**: 1.0-Beta1
- **Minecraft 版本**: 1.21
- **构建状态**: ✅ 成功

## 已完成模块

### 1.1 国家成员系统 ✅
完整的国家成员管理系统，包含两种政体和多种角色。

#### 政体系统
- **分封制 (FEUDAL)** - 君主制国家
- **城市共和制 (REPUBLIC)** - 议会制国家

#### 角色系统
**分封制角色：**
- 国王 (KINGDOM) - 最高统治者
- 财政大臣 (CHANCELLOR) - 财政管理
- 司法大臣 (ATTORNEY_GENERAL) - 司法审判
- 执法大臣 (MINISTER_OF_JUSTICE) - 执法管理
- 军需大臣 (MINISTER_OF_DEFENSE) - 军事后勤

**共和制角色：**
- 总督 (GOVERNOR) - 行政首脑
- 财政官 (FINANCE_OFFICER)
- 司法官 (JUDICIAL_OFFICER)
- 执法官 (LEGAL_OFFICER)
- 军需官 (MILITARY_SUPPLY_OFFICER)
- 议会议员 (PARLIAMENT_MEMBER) - 投票权

### 1.2 国家税收系统 ✅
完整的税收管理系统，支持多种税收类型。

#### 税收类型
1. **交易税**
   - 面对面交易税 (trade)
   - 固定商店税 (shop)
   - 拍卖行税 (auction)

2. **土地税**
   - 土地出售抽成 (landsale)
   - 土地租赁抽成 (landrent)

3. **封臣税**
   - 封臣税 (vassal) - 按周收取

#### 税收功能
- 税率范围：0-100%
- 权限控制：需要财政权限才能调整
- 自动计算：提供税额计算方法
- 数据持久化：保存到 SQLite 数据库

### 1.3 国家外交系统 ✅
完整的外交关系管理系统。

#### 外交关系类型
1. **战争 (WAR)** - 敌对状态
2. **中立 (NEUTRAL)** - 默认状态
3. **友好 (FRIENDLY)** - 友好状态
4. **共同防御同盟 (ALLIANCE_DEFENSIVE)**
5. **共同进攻同盟 (ALLIANCE_OFFENSIVE)**
6. **贸易协议 (TRADE_AGREEMENT)**
7. **科技协议 (TECH_AGREEMENT)**

#### 外交功能
- 宣战与和谈
- 结盟（防御/进攻）
- 签订协议（贸易/科技）
- 关系查询
- 自动通知相关国家成员

### 2. 季节系统 ✅
完整的四季循环系统，每个季节包含3个子季节。

#### 季节划分
- **春季 (SPRING)**: 早春、仲春、晚春
- **夏季 (SUMMER)**: 初夏、盛夏、晚夏
- **秋季 (AUTUMN)**: 早秋、中秋、晚秋
- **冬季 (WINTER)**: 初冬、隆冬、残冬

#### 季节特性
- **温度系统**: 每个季节有不同的基础温度
  - 春季: 22°C
  - 夏季: 30°C
  - 秋季: 21°C
  - 冬季: 15°C
- **天气效果**: 自动调整天气
  - 春季: 30%降雨概率
  - 夏季: 晴朗
  - 秋季: 20%降雨概率
  - 冬季: 40%降雪概率
- **自动循环**: 每8个游戏日自动切换子季节（可配置）

#### 季节命令
```
/season info          - 查看当前季节信息
/season set <季节>    - 设置季节（管理员）
/season config        - 查看季节配置
```

### 3. 玩家状态系统 ✅
完整的玩家生存状态管理系统。

#### 状态指标
- **体温 (Temperature)**: 受季节和环境影响
  - 加热速度: 0.5°C/秒
  - 冷却速度: 0.75°C/秒（1.5倍）
  - 季节影响: 根据当前季节调整目标温度
- **水分 (Hydration)**: 口渴系统
- **职业系统**: 主职业和副职业
  - 等级系统
  - 经验值系统
  - 天赋点系统

#### ActionBar显示
- 更新频率: 0.5秒（10 ticks）
- 显示内容: 生命值、饥饿值、水分、体温
- 可通过配置关闭，使用PlaceholderAPI自定义显示

#### 数据持久化
- 玩家加入时自动加载数据
- 玩家退出时自动保存数据
- 温度数据实时同步到数据库

### 4. PlaceholderAPI集成 ✅
提供20+个占位符变量供其他插件使用。

#### 可用变量
- 玩家状态: health, food, hydration, temperature
- 职业信息: profession, profession_level, profession_exp
- 季节信息: season, season_day, season_temperature
- 国家信息: nation, nation_role, nation_leader
- 等等...

#### 使用场景
- TAB插件: 自定义玩家列表显示
- DeluxeMenus: 创建自定义菜单
- 其他支持PlaceholderAPI的插件

## 数据持久化

### 多数据库支持 ✅
插件支持三种数据库类型，可在配置文件中自由切换：

#### 1. SQLite（默认）
- **适用场景**：小型服务器、单机测试
- **优点**：无需额外配置，开箱即用
- **文件位置**：`plugins/LegacyLand/legacyland.db`

#### 2. MySQL
- **适用场景**：中大型服务器、高并发环境
- **优点**：使用HikariCP连接池，性能优异
- **配置项**：host, port, database, username, password, pool settings

#### 3. MongoDB
- **适用场景**：需要灵活数据结构的场景
- **优点**：NoSQL支持，文档型存储
- **配置项**：host, port, database, username, password

### 数据库架构
- **IDatabase接口**：统一的数据库操作接口
- **DatabaseManager**：工厂模式管理器，根据配置自动选择数据库
- **连接池优化**：MySQL使用HikariCP，MongoDB使用内置连接池

### 存储的数据
所有数据自动保存到数据库，包括：
- 国家信息（名称、领袖、政体、国库、税收配置）
- 成员信息（玩家、角色、加入时间）
- 外交关系（国家间关系、建立时间）
- 玩家数据（温度、水分、职业、等级、经验）
- 战争数据（战争状态、参与者、补给）
- 攻城战数据（前哨战、补给站、核心）
- 玩家成就
- 季节数据

### 数据库表结构
```sql
-- 国家表
nations (name, leader_id, government_type, founded_time, treasury,
         trade_tax, shop_tax, auction_tax, land_sale_tax, land_rent_tax, vassal_tax)

-- 成员表
nation_members (player_id, player_name, nation_name, role, join_time)

-- 外交关系表
diplomacy_relations (id, nation1, nation2, relation_type, established_time)

-- 玩家数据表
players (player_id, player_name, max_health, hydration, temperature,
         main_profession, main_profession_level, main_profession_exp,
         sub_profession, sub_profession_level, sub_profession_exp, talent_points)

-- 战争表
wars (war_name, war_type, attacker_nation, defender_nation,
      attacker_town, defender_town, status, start_time, end_time,
      attacker_supplies, defender_supplies)

-- 战争参与者表
war_participants (war_name, player_id, role)

-- 攻城战表
siege_wars (siege_id, war_name, attacker_town, defender_town,
            outpost_location, outpost_establish_time, outpost_active)

-- 玩家成就表
player_achievements (player_id, achievement_id, unlock_time)

-- 季节数据表
season_data (id, current_season, current_day, days_per_sub_season)
```

## 命令系统

### 国家命令 (/nation, /n, /guo)
```
/nation create <国家名> <FEUDAL|REPUBLIC>  - 创建国家
/nation info [国家名]                      - 查看国家信息
/nation list                               - 列出所有国家
/nation invite <玩家名>                    - 邀请成员
/nation kick <玩家名>                      - 踢出成员
/nation setrank <玩家名> <角色>            - 设置角色
/nation leave                              - 离开国家
/nation delete                             - 删除国家
/nation treasury [deposit|withdraw] [金额] - 国库管理
```

### 税收命令 (/tax, /shui)
```
/tax info                          - 查看税收信息
/tax set <税种> <税率>             - 设置税率
```

**税种选项：**
- trade - 面对面交易税
- shop - 固定商店税
- auction - 拍卖行税
- landsale - 土地出售抽成
- landrent - 土地租赁抽成
- vassal - 封臣税

### 外交命令 (/diplomacy, /diplo, /waijiao)
```
/diplomacy info [国家名]                        - 查看外交关系
/diplomacy war <国家名>                         - 宣战
/diplomacy peace <国家名>                       - 和谈
/diplomacy ally <国家名> <defensive|offensive>  - 结盟
/diplomacy trade <国家名>                       - 签订贸易协议
/diplomacy tech <国家名>                        - 签订科技协议
/diplomacy neutral <国家名>                     - 恢复中立
```

## 权限系统

### 完整权限列表
- **人事权限**: 任免职位、提名、罢免、任免市长
- **外交权限**: 宣战、结盟、提出议案、投票
- **财政权限**: 调整税率、拨款、管理国库
- **司法权限**: 审判、逮捕
- **军事权限**: 管理补给线、决定攻城名单
- **议会权限**: 投票选举、投票提名

## 技术特性

### 核心技术
- **Lombok** - 简化代码，自动生成 Getter/Setter
- **SQLite** - 轻量级数据库，数据持久化
- **Towny 集成** - 依赖 Towny 插件
- **权限系统** - 基于角色的细粒度权限控制

### 设计模式
- **单例模式** - NationManager, DiplomacyManager
- **策略模式** - 不同政体有不同的角色和权限
- **观察者模式** - 外交事件通知相关国家成员

## 项目结构

```
src/main/java/net/chen/legacyLand/
├── LegacyLand.java                          # 主插件类
├── database/
│   ├── IDatabase.java                       # 数据库接口
│   ├── DatabaseManager.java                # 数据库管理器（工厂模式）
│   ├── SQLiteDatabase.java                 # SQLite实现
│   ├── MySQLDatabase.java                  # MySQL实现（HikariCP）
│   └── MongoDatabase.java                  # MongoDB实现
├── config/
│   └── ConfigManager.java                  # 配置管理器（自动更新配置）
├── nation/
│   ├── GovernmentType.java                  # 政体类型
│   ├── Nation.java                          # 国家模型
│   ├── NationManager.java                   # 国家管理器
│   ├── NationMember.java                    # 成员模型
│   ├── NationPermission.java                # 权限枚举
│   ├── NationRole.java                      # 角色枚举
│   ├── TaxConfig.java                       # 税收配置
│   ├── commands/
│   │   ├── NationCommand.java               # 国家命令
│   │   ├── TaxCommand.java                  # 税收命令
│   │   └── DiplomacyCommand.java            # 外交命令
│   └── diplomacy/
│       ├── RelationType.java                # 关系类型
│       ├── DiplomacyRelation.java           # 外交关系
│       └── DiplomacyManager.java            # 外交管理器
├── player/
│   ├── PlayerData.java                      # 玩家数据模型
│   ├── Profession.java                      # 职业枚举
│   └── status/
│       ├── TemperatureManager.java          # 温度管理器
│       └── ActionBarUpdateTask.java         # ActionBar更新任务
├── season/
│   ├── Season.java                          # 季节枚举（12个子季节）
│   ├── SeasonManager.java                   # 季节管理器
│   └── SeasonCommand.java                   # 季节命令
├── placeholder/
│   └── LegacyLandPlaceholder.java          # PlaceholderAPI集成
├── war/
│   ├── War.java                             # 战争模型
│   ├── WarManager.java                      # 战争管理器
│   ├── WarType.java                         # 战争类型
│   ├── WarStatus.java                       # 战争状态
│   ├── commands/
│   │   ├── WarCommand.java                  # 战争命令
│   │   └── SiegeCommand.java                # 攻城命令
│   └── siege/
│       ├── SiegeWar.java                    # 攻城战模型
│       ├── SiegeWarManager.java             # 攻城战管理器
│       ├── Outpost.java                     # 前哨战
│       ├── SupplyLine.java                  # 补给线
│       ├── SupplyStation.java               # 补给站
│       └── OutpostMonitorTask.java          # 前哨战监控任务
└── achievement/
    ├── Achievement.java                     # 成就模型
    └── AchievementManager.java             # 成就管理器
```

## 使用示例

### 创建国家
```
/nation create 大明帝国 FEUDAL
/nation create 罗马共和国 REPUBLIC
```

### 管理成员
```
/nation invite PlayerName
/nation setrank PlayerName CHANCELLOR
/nation kick PlayerName
```

### 设置税收
```
/tax info
/tax set trade 10
/tax set shop 5
/tax set landsale 15
```

### 外交操作
```
/diplomacy war 敌国
/diplomacy ally 盟友 defensive
/diplomacy trade 贸易伙伴
/diplomacy peace 敌国
```

### 国库管理
```
/nation treasury
/nation treasury deposit 1000
/nation treasury withdraw 500
```

## 依赖项

### 必需依赖
- **Towny** 0.102.0.0 - 领地管理插件
- **ItemsAdder** - 物品扩展插件
- **Paper API** 1.21.11 - 服务器 API

### 可选依赖
- **PlaceholderAPI** - 变量占位符支持

### 内置依赖
- **SQLite JDBC** 3.45.0.0 - SQLite数据库驱动
- **MySQL Connector** 8.3.0 - MySQL数据库驱动
- **HikariCP** 5.1.0 - 高性能JDBC连接池
- **MongoDB Driver** 4.11.1 - MongoDB数据库驱动
- **Lombok** 1.18.30 - 代码简化

## 配置系统

### 自动配置更新
ConfigManager会自动检测并添加新的配置项，无需手动修改配置文件。

### 配置文件示例 (config.yml)
```yaml
# 数据库配置
database:
  type: sqlite  # 可选: sqlite, mysql, mongodb

  sqlite:
    filename: legacyland.db

  mysql:
    host: localhost
    port: 3306
    database: legacyland
    username: root
    password: password
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

  mongodb:
    host: localhost
    port: 27017
    database: legacyland
    username: ""
    password: ""

# 玩家状态系统
player-status:
  enable-actionbar: true  # 是否启用内置ActionBar显示

# 季节系统
season:
  days-per-sub-season: 8  # 每个子季节的游戏天数
```

### PlaceholderAPI变量
插件提供20+个占位符变量，可用于其他插件（TAB、DeluxeMenus等）：
- `%legacyland_health%` - 玩家生命值
- `%legacyland_food%` - 饥饿值
- `%legacyland_hydration%` - 水分值
- `%legacyland_temperature%` - 体温
- `%legacyland_profession%` - 主职业
- `%legacyland_profession_level%` - 主职业等级
- `%legacyland_season%` - 当前季节
- `%legacyland_season_day%` - 季节天数
- 等等...

## 数据库文件位置

插件数据保存在：`plugins/LegacyLand/legacyland.db`

## 构建产物

JAR 文件位置：`build/libs/LegacyLand-1.0-Beta1.jar`

## 后续开发计划

根据 plan 文件，后续可实现：
- ✅ 1.1 国家成员系统
- ✅ 1.2 国家税收系统
- ✅ 1.3 国家外交系统
- ⏳ 1.4 国家战争系统
- ⏳ 1.5 攻城战系统
- ⏳ 1.6 国家科技树
- ⏳ 1.7 国家公会系统
- ⏳ 1.8 国家领土系统
- ⏳ 1.9 国策系统

## 特色功能

### 1. 完整的权限控制
每个角色都有明确的权限范围，确保国家管理有序。

### 2. 灵活的政体系统
支持分封制和共和制两种政体，不同政体有不同的治理方式。

### 3. 多样的外交关系
支持战争、同盟、贸易等多种外交关系，丰富国家间互动。

### 4. 完善的税收系统
支持多种税收类型，为国家提供稳定的财政收入。

### 5. 数据持久化
所有数据自动保存到数据库，服务器重启后数据不丢失。

### 6. 中文支持
完整的中文界面和提示，适合中文玩家使用。

## 注意事项

1. 需要安装 Towny 插件作为前置
2. 建议在测试服务器上先进行测试
3. 定期备份数据库文件
4. 部分功能（如逮捕、审判）需要配合其他系统实现
5. 税收计算功能已实现，但实际扣税需要配合经济插件

## 更新日志

### v1.0-Beta2 (2026-02-22)
- ✅ 实现多数据库支持系统
  - SQLite（默认）
  - MySQL（HikariCP连接池）
  - MongoDB（NoSQL支持）
- ✅ 实现季节系统（12个子季节）
- ✅ 实现玩家状态系统（温度、水分、职业）
- ✅ 实现PlaceholderAPI集成（20+变量）
- ✅ 实现配置自动更新系统
- ✅ 优化ActionBar更新频率（0.5秒）
- ✅ 完善数据持久化（玩家温度、季节数据）

### v1.0-Beta1 (2026-02-15)
- ✅ 实现国家成员系统 (1.1)
- ✅ 实现国家税收系统 (1.2)
- ✅ 实现国家外交系统 (1.3)
- ✅ 添加 SQLite 数据持久化
- ✅ 完整的命令系统和 Tab 补全
- ✅ 基于角色的权限控制系统
