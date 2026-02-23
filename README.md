# LegacyLand - Minecraft国家系统插件

## 法律许可证
Unauthorized modification and redistribution prohibited.
````
License Change Notice

Important: Starting from version 1.0-Beta2.0, this project is licensed under MS-RSL.

This means:

- Source code is viewable only
- Modification and redistribution are no longer permitted
- Every violation of MS-RSL will be subject to prosecution.

Versions before 1.0-Beta2.0 remain under MS-RL.
````
## 项目简介

LegacyLand是一个基于Paper 1.21的Minecraft服务器插件，提供完整的国家系统、季节系统、玩家状态管理等功能。

## 核心功能

### 1. 国家系统 ✅
- **政体系统**: 5种可配置政体（分封制、共和制、君主立宪制、军事独裁、商业联邦）
- **自定义效果**: 速度加成、粒子效果等可扩展效果系统
- **角色系统**: 国王、财政大臣、司法大臣等多种角色
- **权限系统**: 基于角色的细粒度权限控制
- **税收系统**: 交易税、土地税、封臣税等多种税收类型
- **外交系统**: 战争、同盟、贸易协议、科技协议等

### 2. 季节系统 ✅
- **四季循环**: 春夏秋冬，每季3个子季节（共12个）
- **温度系统**: 每个季节有不同的基础温度
- **天气效果**: 自动调整降雨、降雪概率
- **自动循环**: 每8个游戏日切换子季节（可配置）

### 3. 玩家状态系统 ✅
- **体温管理**: 受季节和环境影响
- **水分系统**: 口渴机制
- **职业系统**: 主职业、副职业、等级、经验值
- **ActionBar显示**: 实时显示玩家状态

### 4. 多数据库支持 ✅
- **SQLite**: 默认，适合小型服务器
- **MySQL**: HikariCP连接池，适合中大型服务器
- **MongoDB**: NoSQL支持，灵活的数据结构

### 5. PlaceholderAPI集成 ✅
- 提供20+个占位符变量
- 支持TAB、DeluxeMenus等插件
- 自定义显示玩家状态和国家信息

## 版本信息
- **插件版本**: 1.0-Beta2
- **Minecraft版本**: 1.21
- **构建状态**: ✅ 成功

## 快速开始

### 📚 详细文档
- [实现细节文档](instructions/IMPLEMENTATION.md) - 系统架构和实现细节
- [PlaceholderAPI变量](instructions/PLACEHOLDERS.md) - 完整的占位符变量列表
- [FlagWar系统](instructions/FLAGWAR_README.md) - 旗帜战争系统说明

### 安装
1. 下载插件JAR文件
2. 将JAR文件放入服务器的`plugins`目录
3. 确保已安装Towny和ItemsAdder插件
4. 重启服务器

### 配置数据库
编辑`plugins/LegacyLand/config.yml`：

```yaml
database:
  type: sqlite  # 可选: sqlite, mysql, mongodb

  # MySQL配置（如果使用MySQL）
  mysql:
    host: localhost
    port: 3306
    database: legacyland
    username: root
    password: your_password
```

### 创建第一个国家
```
/nation create 我的国家 FEUDAL
```

## 命令列表

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

### 政治体制命令 (/legacy politics)
```
/legacy politics list           - 查看所有可用政体
/legacy politics set <政体ID>   - 切换国家政体（需要国王权限）
/legacy politics info           - 查看当前政体详情
```

### 税收命令 (/tax, /shui)
```
/tax info                          - 查看税收信息
/tax set <税种> <税率>             - 设置税率
```

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

### 季节命令 (/season)
```
/season info          - 查看当前季节信息
/season set <季节>    - 设置季节（管理员）
/season config        - 查看季节配置
```

## 依赖项

### 必需依赖
- **Towny** 0.102.0.0 - 领地管理插件
- **ItemsAdder** - 物品扩展插件
- **Paper** 1.21+ - 服务器核心

### 可选依赖
- **PlaceholderAPI** - 变量占位符支持（推荐安装）

## 技术特性

### 数据库支持
- **多数据库**: SQLite、MySQL、MongoDB三选一
- **连接池优化**: MySQL使用HikariCP高性能连接池
- **自动迁移**: 配置文件自动更新，无需手动修改

### 性能优化
- **异步处理**: 数据库操作异步执行
- **缓存机制**: 内存缓存减少数据库查询
- **批量操作**: 支持批量数据保存

### 扩展性
- **PlaceholderAPI**: 20+个占位符变量
- **事件系统**: 完整的事件API供其他插件使用
- **模块化设计**: 各系统独立，易于扩展

## PlaceholderAPI变量

```
%legacyland_health%              - 玩家生命值
%legacyland_food%                - 饥饿值
%legacyland_hydration%           - 水分值
%legacyland_temperature%         - 体温
%legacyland_profession%          - 主职业
%legacyland_profession_level%    - 主职业等级
%legacyland_season%              - 当前季节
%legacyland_season_day%          - 季节天数
%legacyland_nation%              - 所属国家
%legacyland_nation_role%         - 国家角色
```

## 配置文件

### config.yml
```yaml
# 数据库配置
database:
  type: sqlite

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
  enable-actionbar: true

# 季节系统
season:
  days-per-sub-season: 8
```

### politics.yml
政治体制配置文件，定义所有可用政体及其效果：

```yaml
types:
  FEUDAL:
    display-name: "分封制"
    description: "国王拥有绝对权力..."
    roles:
      - KINGDOM
      - CHANCELLOR
    effects:
      tax-efficiency: 0.8
      military-strength: 1.3
      treasury-income: 1.0
    custom-effects:
      particle-effect:
        particle: "FLAME"
        pattern: "STAR"
```

详细配置说明请参考 [实现细节文档](instructions/IMPLEMENTATION.md)。

## 核心架构

### 1. 政体系统 (PoliticalSystem)

#### 可用政体类型
- **分封制 (FEUDAL)** - 国王绝对权力，军事强大（1.3x），税收较低（0.8x）
  - 粒子效果：火焰五角星
- **共和制 (REPUBLIC)** - 总督+议会制，高国库收入（1.3x），军事较弱（0.9x）
  - 自定义效果：全员速度加成
- **君主立宪制 (CONSTITUTIONAL_MONARCHY)** - 国王+议会共治，各方面均衡
  - 粒子效果：末地烛圆形
- **军事独裁 (MILITARY_DICTATORSHIP)** - 军事领袖掌权，极强军事（1.6x），外交受限
  - 粒子效果：红石正方形
- **商业联邦 (TRADE_FEDERATION)** - 贸易为核心，高税收（1.5x），军事薄弱（0.7x）
  - 粒子效果：村民高兴圆形

#### 政体效果系统
- **标准效果**: 税收效率、军事力量、国库收入、战争冷却等倍率
- **自定义效果**: 可扩展的效果系统（速度加成、粒子效果等）
- **配置驱动**: 通过 `politics.yml` 配置所有政体参数
- **数据库持久化**: 政体选择自动保存到数据库

### 2. 角色与权限系统

#### 分封制角色
- **国王 (KINGDOM)** - 拥有所有权限
- **财政大臣 (CHANCELLOR)** - 财政管理
- **司法大臣 (ATTORNEY_GENERAL)** - 司法审判
- **执法大臣 (MINISTER_OF_JUSTICE)** - 执法管理
- **军需大臣 (MINISTER_OF_DEFENSE)** - 军事后勤

#### 共和制角色
- **总督 (GOVERNOR)** - 行政首脑
- **财政官 (FINANCE_OFFICER)** - 财政管理
- **司法官 (JUDICIAL_OFFICER)** - 司法审判
- **执法官 (LEGAL_OFFICER)** - 执法管理
- **军需官 (MILITARY_SUPPLY_OFFICER)** - 军事后勤
- **议会议员 (PARLIAMENT_MEMBER)** - 议会投票

### 3. 季节系统
- **12个子季节**: 春夏秋冬各3个子季节
- **温度系统**: 季节影响环境温度
- **天气效果**: 自动调整降雨降雪
- **自动循环**: 可配置的季节周期

## 使用示例

### 创建国家
```
/nation create 大明帝国 FEUDAL
/nation create 罗马共和国 REPUBLIC
```

### 切换政体
```
/legacy politics list                    # 查看所有可用政体
/legacy politics set MILITARY_DICTATORSHIP  # 切换为军事独裁
/legacy politics info                    # 查看当前政体详情
```

### 管理成员
```
/nation invite PlayerName
/nation setrank PlayerName CHANCELLOR
```

### 设置税收
```
/tax set trade 10
/tax set shop 5
```

### 外交操作
```
/diplomacy war 敌国
/diplomacy ally 盟友 defensive
/diplomacy trade 贸易伙伴
```

## 开发计划

### 已完成 ✅
- 1.1 国家成员系统
- 1.2 国家税收系统
- 1.3 国家外交系统
- 1.4 配置驱动的政治体制系统（5种政体）
- 1.5 自定义效果系统（速度加成、粒子效果）
- 季节系统（12子季节）
- 玩家状态系统
- 多数据库支持
- PlaceholderAPI集成

### 进行中 🚧
- 1.6 国家战争系统
- 1.7 攻城战系统
- 1.8 旗帜战争系统

### 计划中 📋
- 1.6 国家科技树
- 1.7 国家公会系统
- 1.8 国家领土系统
- 1.9 国策系统

## 构建信息

### 构建产物
JAR文件位置：`build/libs/LegacyLand-1.0-Beta2.jar`

### 数据文件
- SQLite数据库：`plugins/LegacyLand/legacyland.db`
- 配置文件：`plugins/LegacyLand/config.yml`

## 注意事项

1. 需要安装Towny和ItemsAdder作为前置插件
2. 推荐安装PlaceholderAPI以使用占位符功能
3. 定期备份数据库文件
4. 建议在测试服务器上先进行测试
5. MySQL和MongoDB需要额外配置

## 更新日志

### v1.0-Beta2.0 (2026-02-23)
- ✅ 配置驱动的政治体制系统
  - 5种预置政体（分封制、共和制、君主立宪制、军事独裁、商业联邦）
  - 可扩展的自定义效果系统
  - 速度加成效果（共和制）
  - 粒子效果系统（五角星、圆形、正方形图案）
  - 政体数据库持久化
- ✅ 多数据库支持（SQLite/MySQL/MongoDB）
- ✅ 季节系统（12个子季节）
- ✅ 玩家状态系统（温度、水分、职业）
- ✅ PlaceholderAPI集成
- ✅ 配置自动更新系统

### v1.0-Beta1 (2026-02-15)
- ✅ 国家成员系统
- ✅ 国家税收系统
- ✅ 国家外交系统
- ✅ SQLite数据持久化

## 许可证

本项目为私有项目，未经授权不得使用。

## Contributor
[@ian171](https://github.com/ian171)

