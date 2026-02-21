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

## 数据持久化

### SQLite 数据库
所有数据自动保存到 SQLite 数据库，包括：
- 国家信息（名称、领袖、政体、国库、税收配置）
- 成员信息（玩家、角色、加入时间）
- 外交关系（国家间关系、建立时间）

### 数据库表结构
```sql
-- 国家表
nations (name, leader_id, government_type, founded_time, treasury,
         trade_tax, shop_tax, auction_tax, land_sale_tax, land_rent_tax, vassal_tax)

-- 成员表
nation_members (player_id, player_name, nation_name, role, join_time)

-- 外交关系表
diplomacy_relations (id, nation1, nation2, relation_type, established_time)
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
│   └── DatabaseManager.java                # 数据库管理器
└── nation/
    ├── GovernmentType.java                  # 政体类型
    ├── Nation.java                          # 国家模型
    ├── NationManager.java                   # 国家管理器
    ├── NationMember.java                    # 成员模型
    ├── NationPermission.java                # 权限枚举
    ├── NationRole.java                      # 角色枚举
    ├── TaxConfig.java                       # 税收配置
    ├── commands/
    │   ├── NationCommand.java               # 国家命令
    │   ├── TaxCommand.java                  # 税收命令
    │   └── DiplomacyCommand.java            # 外交命令
    └── diplomacy/
        ├── RelationType.java                # 关系类型
        ├── DiplomacyRelation.java           # 外交关系
        └── DiplomacyManager.java            # 外交管理器
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
- **Paper API** 1.21.11 - 服务器 API

### 内置依赖
- **SQLite JDBC** 3.45.0.0 - 数据库驱动
- **Lombok** 1.18.30 - 代码简化

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

### v1.0-Beta1 (2026-02-15)
- ✅ 实现国家成员系统 (1.1)
- ✅ 实现国家税收系统 (1.2)
- ✅ 实现国家外交系统 (1.3)
- ✅ 添加 SQLite 数据持久化
- ✅ 完整的命令系统和 Tab 补全
- ✅ 基于角色的权限控制系统
