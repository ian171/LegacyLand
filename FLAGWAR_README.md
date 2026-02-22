# FlagWar 旗帜战争系统

## 概述

FlagWar 是 LegacyLand 插件集成的旗帜战争系统，基于 Towny Advanced 的 FlagWar 机制实现。允许敌对国家通过放置旗帜来争夺对方的领土。

## 核心机制

### 战争流程

1. **宣战**: 使用 `/nation enemy add <国家名>` 将目标国家设为敌对
2. **放置旗帜**: 在敌方边界地块放置橡木栅栏（Oak Fence）
3. **计时器**: 旗帜上方会出现计时器方块（羊毛），颜色从绿→黄→橙→红
4. **天空信标**: 在高空放置发光石标记，方便防守方找到位置
5. **防守**: 防守方破坏计时器方块可以取消攻击
6. **胜利**: 计时器完成（100%）后，攻击方获得该地块

### 旗帜放置规则

**攻击方要求**:
- 必须属于一个国家
- 国家不能是中立状态
- 与目标国家处于敌对关系
- 不能是服务器管理员（OP）
- 国家在线人数达到最低要求（默认1人）
- 每个玩家同时只能有一个进行中的旗帜战争

**目标地块要求**:
- 必须是敌对国家的城镇地块
- 目标国家不能是中立状态
- 目标国家在线人数达到最低要求（默认1人）
- 只能攻击边界地块（可配置）
- 天空不能被遮挡
- 必须在地表放置

### 计时器系统

- **持续时间**: 默认 5 分钟（300秒），可配置
- **颜色变化**:
  - 0-25%: 绿色羊毛（Lime Wool）
  - 25-50%: 黄色羊毛（Yellow Wool）
  - 50-75%: 橙色羊毛（Orange Wool）
  - 75-100%: 红色羊毛（Red Wool）
- **光源**: 计时器方块上方自动放置火把
- **信标**: 高空（+50格）放置发光石标记

### 经济成本

| 费用类型 | 默认金额 | 说明 |
|---------|---------|------|
| 放置费用 | 100 金币 | 放置旗帜时扣除 |
| 防守破坏费用 | 200 金币 | 防守方破坏旗帜时，攻击方需支付 |
| 胜利奖励 | 500 金币 | 攻击方胜利时获得 |

**注意**: 攻击主城方块（Home Block）时，所有费用翻倍。

## 命令

### /flagwar info
查看当前玩家的旗帜战争信息。

**显示内容**:
- 攻击方和防守方国家/城镇
- 目标地块坐标
- 是否为主城方块
- 计时器进度
- 战争状态
- 相关费用

### /flagwar list
列出所有活跃的旗帜战争。

**显示内容**:
- 攻击方城镇 → 防守方城镇
- 计时器进度百分比
- 目标地块坐标

### /flagwar cancel
取消当前玩家发起的旗帜战争。

**效果**:
- 移除旗帜和计时器方块
- 清理天空信标
- 不退还放置费用

**别名**: `/fw`, `/qizhan`

## 配置

在 `config.yml` 中的 `flagwar` 部分：

```yaml
flagwar:
  # 是否启用 FlagWar 系统
  enabled: true
  # 战争持续时间（秒），计时器完成所需时间
  war-duration-seconds: 300  # 5分钟
  # 是否只允许攻击边界地块
  only-border-plots: true
  # 在线人数要求
  min-online-attackers: 1
  min-online-defenders: 1
  # 经济成本
  costs:
    # 放置旗帜的押金
    staking-fee: 100.0
    # 防守方破坏旗帜时攻击方需支付的费用
    defense-break-fee: 200.0
    # 攻击方胜利时获得的奖励
    victory-cost: 500.0
```

## 数据持久化

FlagWar 数据存储在 SQLite 数据库的 `flag_wars` 表中：

**存储内容**:
- 旗帜战争 ID
- 攻击方和防守方信息
- 旗帜、计时器、信标位置
- 开始/结束时间
- 战争状态和进度
- 经济成本数据
- 地块坐标和类型

**自动恢复**: 服务器重启后，活跃的旗帜战争会自动从数据库恢复。

## 游戏策略

### 攻击方策略

1. **选择目标**: 优先攻击边界地块，避免深入敌方领土
2. **时机选择**: 在敌方在线人数较少时发起攻击
3. **保护旗帜**: 放置旗帜后需要守护 5 分钟
4. **成本考虑**: 避免攻击主城方块，除非有充足资金

### 防守方策略

1. **快速响应**: 注意全局广播，立即前往坐标
2. **寻找旗帜**: 查看天空中的发光石信标
3. **破坏计时器**: 破坏旗帜上方的羊毛方块即可取消攻击
4. **边界防御**: 在边界地块设置防御工事

## 与其他系统的集成

### Towny 集成
- 自动同步敌对关系
- 攻击时自动将攻击方添加到防守方的敌人列表
- 胜利后自动转移地块所有权

### 战争系统集成
- FlagWar 可以作为大规模战争的一部分
- 与攻城战系统互补，提供小规模领土争夺

### 外交系统集成
- 只能攻击敌对国家
- 中立国家免疫攻击
- 同盟国家无法互相攻击

## 技术细节

### 方块类型
- **旗帜**: Oak Fence（橡木栅栏）
- **计时器**: Wool（羊毛，颜色变化）
- **光源**: Torch（火把）
- **信标**: Glowstone（发光石）

### 更新频率
- 计时器每秒更新一次（20 ticks）
- 进度百分比根据经过时间计算
- 羊毛颜色根据进度自动变化

### 事件触发
- 放置橡木栅栏时检查并创建 FlagWar
- 破坏计时器方块时触发防守方胜利
- 计时器完成时触发攻击方胜利

## 常见问题

**Q: 为什么无法放置旗帜？**
A: 检查以下条件：
- 是否与目标国家处于敌对关系
- 双方在线人数是否达到要求
- 是否在边界地块
- 天空是否被遮挡
- 是否已有进行中的旗帜战争

**Q: 旗帜战争会自动结束吗？**
A: 是的，以下情况会结束：
- 计时器完成（攻击方胜利）
- 防守方破坏计时器（防守方胜利）
- 攻击方使用 `/flagwar cancel` 取消

**Q: 服务器重启后旗帜战争会消失吗？**
A: 不会，活跃的旗帜战争会自动从数据库恢复。

**Q: 可以同时发起多个旗帜战争吗？**
A: 每个玩家同时只能有一个进行中的旗帜战争。

## 版本信息

- **实现版本**: LegacyLand 1.0-Beta1
- **基于**: Towny Advanced FlagWar 机制
- **兼容**: Minecraft 1.21, Paper API, Towny 0.102.0.0

## 开发者信息

### 核心类
- `FlagWarManager`: 管理所有旗帜战争实例
- `FlagWarData`: 旗帜战争数据模型
- `FlagWarListener`: 监听旗帜放置和破坏事件
- `FlagWarTimerTask`: 计时器更新任务
- `FlagWarCommand`: 命令处理器

### 数据库表
```sql
CREATE TABLE flag_wars (
    flag_war_id TEXT PRIMARY KEY,
    attacker_id TEXT NOT NULL,
    attacker_nation TEXT NOT NULL,
    attacker_town TEXT NOT NULL,
    defender_nation TEXT NOT NULL,
    defender_town TEXT NOT NULL,
    flag_location TEXT NOT NULL,
    timer_block_location TEXT NOT NULL,
    beacon_location TEXT NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    status TEXT NOT NULL,
    timer_progress INTEGER DEFAULT 0,
    staking_fee REAL DEFAULT 0,
    defense_break_fee REAL DEFAULT 0,
    victory_cost REAL DEFAULT 0,
    town_block_coords TEXT,
    is_home_block INTEGER DEFAULT 0
);
```
