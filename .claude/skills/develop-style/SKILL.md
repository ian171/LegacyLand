---
name: develop-style
description: |
  当需要开发 一切 功能时自动使用此 Skill。

  触发场景：
  - 编写任意代码
  - 开发任意功能

  触发词：开发 创建 编写 设计 读取 制作 develop create design read make
---

# LegacyLand 开发风格与规范

## 项目概述

LegacyLand 是一个基于 Minecraft Paper 1.21 的服务器插件，扩展了 Towny 插件的国家系统，提供了完整的国家管理、外交、战争、玩家状态等游戏机制。

**技术栈**
- Java 21
- Paper API 1.21.11
- Gradle 8.8
- SQLite (数据持久化)
- Lombok (代码简化)
- Towny 0.102.0.0 (依赖)
- ItemsAdder 3.6.3 (依赖)

**游戏系统**
- 国家系统（政体、税收、外交、战争、科技树、公会、领土）
- 玩家系统（职业、身体状态、受伤状态、温度值）
- 经济系统（货币、交易、贷款）
- 世界系统（季节、气候、作物、物种繁殖）
- 战争系统（攻城战、补给线、前哨战）

## ItemsAdder 物品设计规范

### 1. 物品命名规范

**物品 ID 命名**
- 使用 snake_case（小写+下划线）
- 格式：`类型_名称_变体`
- 示例：`carrot_seed`, `leather_helmet_fur`, `supply_crate`

**显示名称**
- 使用中文
- 简洁明了
- 示例：`"胡萝卜种子"`, `"加绒皮质头盔"`, `"补给箱"`

### 2. 物品分类

**作物类**
- 种子：`{crop}_seed`（如 `carrot_seed`, `wheat_seed`）
- 未成熟作物：`unripe_{crop}`（如 `unripe_carrot`）
- 成熟作物：`ripe_{crop}` 或 `{crop}`（如 `ripe_carrot`, `wheat`）
- 水果：`{fruit}_fruit`（如 `apple_fruit`）

**装备类**
- 格式：`{material}_{slot}_{variant}`
- 材质：`leather`, `chainmail`, `iron`, `diamond`
- 部位：`helmet`, `chestplate`, `leggings`, `boots`
- 变体：`basic`, `fur`（加绒）
- 示例：`leather_helmet_fur`, `iron_chestplate_basic`

**工具类**
- 农业工具：`{tool_name}`（如 `water_wheel`, `scarecrow`）
- 牧场设施：`{facility_name}`（如 `water_trough`, `feed_trough`）
- 储存设施：`{storage_name}`（如 `wooden_barrel`, `wine_barrel`）

**战争物资**
- 补给：`supply_crate`
- 武器：`{weapon_type}_{material}`

### 3. 物品属性配置

**基础属性**
```yaml
item_id:
  display_name: "中文显示名"
  permission: legacyland.item_id
  resource:
    material: MINECRAFT_MATERIAL
    generate: true
    textures:
      - item/item_id.png
  lore:
    - "§7描述行1"
    - "§e特殊属性"
```

**耐久度配置**
```yaml
durability:
  max_custom_durability: 数值
```
- 种子：1（一次性）
- 皮质装备：100-180
- 锁链装备：150-200
- 铁质装备：200-280

**装备属性**
```yaml
attribute_modifiers:
  部位:  # head, chest, legs, feet
    armor: 护甲值
    armorToughness: 护甲韧性
```

**食物属性**
```yaml
behaviours:
  eat:
    hunger: 饱食度
    saturation: 饱和度
```

### 4. Lore 规范

**颜色代码**
- `§7` - 灰色：普通描述
- `§a` - 绿色：正面效果
- `§e` - 黄色：特殊属性
- `§c` - 红色：负面效果/警告
- `§b` - 青色：温度相关

**Lore 结构**
1. 第一行：物品基本描述
2. 第二行：使用条件/季节信息
3. 第三行：特殊效果/属性加成

**示例**
```yaml
lore:
  - "§7适宜春季种植"
  - "§7春分-夏季节收获"
  - "§e产量: +30% (春季)"
```

### 5. 季节与作物对应

**春季作物**
- 胡萝卜、马铃薯、甜菜根、西瓜
- 产量加成：+30%
- 收获季节：春分-夏

**夏季作物**
- 小麦、果树
- 生长速度：+10%（小麦）、+5%（果树）
- 收获季节：秋-秋分

**全季作物**
- 南瓜、甘蔗、蘑菇
- 无季节限制

### 6. 温度系统装备

**装备温度效果**
| 装备类型 | 夏季温度 | 冬季温度 |
|---------|---------|---------|
| 皮质盔甲 | +5°C | +3°C |
| 加绒皮质 | +8°C | +5°C |
| 锁链盔甲 | +2°C | +1°C |
| 铁甲 | +10°C | +4°C |
| 加绒铁甲 | +15°C | +8°C |

**Lore 格式**
```yaml
lore:
  - "§7装备类型描述"
  - "§e温度: +X°C (夏) / +Y°C (冬)"
```

### 7. 物品文件组织

**文件结构**
```
src/main/resources/itemsadder/
├── crops_and_items.yml      # 作物和通用物品
├── medical_items.yml         # 医疗物品（已存在）
├── armor_and_clothing.yml    # 装备和衣物（可选）
└── war_supplies.yml          # 战争物资（可选）
```

**命名空间**
- 统一使用 `legacyland` 作为命名空间
- 配置文件开头必须声明：
```yaml
info:
  namespace: legacyland
```

### 8. 物品设计原则

**游戏平衡性**
- 高级装备提供更高防护，但温度影响更大
- 作物产量受季节影响，鼓励多样化种植
- 战争物资稀缺性，增加战略深度

**中文本地化**
- 所有显示名称使用中文
- Lore 描述清晰易懂
- 符合中文表达习惯

**与策划案对应**
- 严格按照策划案设计物品属性
- 温度值、产量加成等数值与策划案一致
- 季节系统与作物生长周期匹配

**可扩展性**
- 预留变体空间（basic, advanced, master）
- 支持未来添加新材质和等级
- 模块化设计，便于维护

### 9. 物品实现流程

**设计新物品**
1. 查阅策划案，确定物品属性和效果
2. 确定物品分类和命名
3. 编写 ItemsAdder 配置
4. 准备材质文件（如需要）
5. 测试物品功能

**配置检查清单**
- [ ] 物品 ID 符合命名规范
- [ ] 显示名称使用中文
- [ ] Permission 正确设置
- [ ] Material 选择合适
- [ ] Lore 描述完整清晰
- [ ] 属性数值与策划案一致
- [ ] 颜色代码使用正确

### 10. 常见物品模板

**种子模板**
```yaml
{crop}_seed:
  display_name: "{作物名}种子"
  permission: legacyland.{crop}_seed
  resource:
    material: WHEAT_SEEDS
    generate: true
    textures:
      - item/{crop}_seed.png
  durability:
    max_custom_durability: 1
  lore:
    - "§7适宜{季节}种植"
    - "§7{收获季节}收获"
    - "§e{特殊效果}"
```

**装备模板**
```yaml
{material}_{slot}_{variant}:
  display_name: "{中文名}"
  permission: legacyland.{material}_{slot}_{variant}
  resource:
    material: {MINECRAFT_MATERIAL}
    generate: true
    textures:
      - item/{material}_{slot}_{variant}.png
  durability:
    max_custom_durability: {数值}
  attribute_modifiers:
    {slot}:
      armor: {护甲值}
      armorToughness: {韧性值}
  lore:
    - "§7{装备描述}"
    - "§e温度: +X°C (夏) / +Y°C (冬)"
```

**工具模板**
```yaml
{tool_name}:
  display_name: "{中文名}"
  permission: legacyland.{tool_name}
  resource:
    material: {MINECRAFT_MATERIAL}
    generate: true
    textures:
      - item/{tool_name}.png
  lore:
    - "§7{工具描述}"
    - "§e{特殊效果}"
    - "§7{使用条件}"
```

**技术栈**
- Java 21
- Paper API 1.21.11
- Gradle 8.8
- SQLite (数据持久化)
- Lombok (代码简化)
- Towny 0.102.0.0 (依赖)
- ItemsAdder 3.6.3 (依赖)

## 代码风格规范

### 1. 命名约定

**类名**
- 使用 PascalCase
- Manager 类：功能管理器，如 `NationManager`, `PlayerManager`, `WarManager`
- Command 类：命令处理器，如 `LegacyCommand`, `DiplomacyCommand`
- Listener 类：事件监听器，如 `WarEventListener`, `PlayerStatusListener`
- Event 类：自定义事件，如 `WarStartEvent`, `OutpostEstablishedEvent`
- 枚举类：功能类型，如 `GovernmentType`, `NationRole`, `WarStatus`

**方法名**
- 使用 camelCase
- 获取器：`getPlayerData()`, `getNationWars()`
- 设置器：`setGovernmentType()`, `setPlayerRole()`
- 布尔判断：`hasPermission()`, `isAtWar()`, `canChooseSubProfession()`
- 操作方法：`createWar()`, `addParticipant()`, `applyBodyStatus()`

**变量名**
- 使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 集合类型明确表达内容：`playerWarMap`, `nationGovernments`, `playerBodyStatus`

**包结构**
```
net.chen.legacyLand/
├── achievements/          # 成就系统
├── config/               # 配置管理
├── database/             # 数据库管理
├── events/               # 自定义事件
├── listeners/            # 事件监听器
├── nation/               # 国家系统
│   ├── commands/         # 国家相关命令
│   └── diplomacy/        # 外交系统
├── player/               # 玩家系统
│   ├── commands/         # 玩家相关命令
│   ├── listeners/        # 玩家事件监听
│   └── status/           # 玩家状态系统
└── war/                  # 战争系统
    ├── commands/         # 战争相关命令
    └── siege/            # 攻城战系统
```

### 2. 设计模式

**单例模式 (Singleton)**
- 所有 Manager 类使用单例模式
- 实现方式：
```java
public class ExampleManager {
    private static ExampleManager instance;

    private ExampleManager() {
        // 私有构造函数
    }

    public static ExampleManager getInstance() {
        if (instance == null) {
            instance = new ExampleManager();
        }
        return instance;
    }
}
```

**管理器模式**
- 每个功能模块有对应的 Manager 类
- Manager 负责数据管理、业务逻辑、数据持久化
- 示例：`NationManager`, `WarManager`, `PlayerManager`

**命令模式**
- 所有命令类实现 `CommandExecutor` 和 `TabCompleter`
- 命令处理逻辑集中在 `onCommand()` 方法
- Tab 补全逻辑在 `onTabComplete()` 方法

### 3. 数据管理

**数据持久化**
- 使用 SQLite 作为默认数据库
- `DatabaseManager` 统一管理所有数据库操作
- 使用 PreparedStatement 防止 SQL 注入
- 数据保存时机：
  - 玩家退出时保存玩家数据
  - 状态变更时实时保存
  - 插件关闭时保存所有数据

**内存缓存**
- Manager 类使用 Map 缓存数据
- 常见模式：
```java
private final Map<UUID, PlayerData> players;
private final Map<String, War> wars;
private final Map<String, GovernmentType> nationGovernments;
```

**数据加载**
- 插件启动时初始化 Manager
- 玩家加入时从数据库加载数据
- 懒加载：需要时才从数据库读取

### 4. 事件系统

**自定义事件**
- 继承 `org.bukkit.event.Event`
- 提供必要的 getter 方法
- 实现 `getHandlers()` 和静态 `getHandlerList()`
- 示例：
```java
public class WarStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final War war;

    public WarStartEvent(War war) {
        this.war = war;
    }

    public War getWar() {
        return war;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
```

**事件监听器**
- 使用 `@EventHandler` 注解
- 在主类的 `registerListeners()` 方法中注册
- 监听器类命名：`功能名 + EventListener` 或 `功能名 + Listener`

### 5. 日志规范

**日志使用**
- 使用 `LegacyLand.logger` 进行日志输出
- 日志级别：
  - `info()`: 正常信息（系统加载、功能启用）
  - `warning()`: 警告信息（配置问题、非致命错误）
  - `severe()`: 严重错误（数据库连接失败、依赖缺失）

**日志示例**
```java
LegacyLand.logger.info("国家系统已加载。");
LegacyLand.logger.warning("未知的成就类型: " + achievementName);
LegacyLand.logger.severe("数据库连接失败: " + e.getMessage());
```

### 6. 注释规范

**类注释**
```java
/**
 * 国家扩展数据管理器 - 存储 Towny 国家的扩展信息
 */
public class NationManager {
    // ...
}
```

**方法注释**
```java
/**
 * 设置国家政体
 */
public void setGovernmentType(String nationName, GovernmentType governmentType) {
    // ...
}
```

**注释原则**
- 类和公共方法必须有注释
- 注释使用中文
- 简洁明了，说明功能而非实现
- 复杂逻辑添加行内注释

### 7. Lombok 使用

**常用注解**
- `@Getter`: 自动生成 getter 方法
- `@Setter`: 自动生成 setter 方法
- `@Data`: 生成 getter/setter/toString/equals/hashCode
- 在主类和数据类中使用

**示例**
```java
@Getter
public final class LegacyLand extends JavaPlugin {
    private ConfigManager configManager;
    private NationManager nationManager;
    // ...
}
```

### 8. 配置管理

**配置文件**
- 使用 YAML 格式
- 配置文件位置：`src/main/resources/config.yml`
- 通过 `ConfigManager` 读取配置
- 配置项分类清晰，使用层级结构

**配置读取**
```java
configManager = new ConfigManager(this);
```

### 9. 国际化

**消息系统**
- 所有玩家可见消息使用中文
- 使用 Minecraft 颜色代码：`§6`, `§a`, `§c`, `§e`
- 消息格式统一：
  - 成功消息：`§a` (绿色)
  - 错误消息：`§c` (红色)
  - 警告消息：`§e` (黄色)
  - 信息消息：`§6` (金色)

**命令别名**
- 提供中文别名支持
- 示例：`/legacy` = `/ll` = `/yichan`
- 示例：`/diplomacy` = `/diplo` = `/waijiao`

### 10. 错误处理

**异常处理**
- 数据库操作必须捕获 `SQLException`
- 记录错误日志并提供有意义的错误信息
- 不向玩家暴露技术细节

**示例**
```java
try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    pstmt.setString(1, nationName);
    pstmt.executeUpdate();
} catch (SQLException e) {
    LegacyLand.logger.severe("保存国家政体失败: " + e.getMessage());
}
```

### 11. 性能优化

**定时任务**
- 使用 BukkitRunnable 执行定时任务
- 合理设置任务间隔，避免频繁执行
- 示例：
```java
new OutpostMonitorTask(instance).runTaskTimer(instance, 1200L, 1200L); // 每分钟
new StatusUpdateTask().runTaskTimer(instance, 100L, 100L); // 每5秒
```

**数据结构选择**
- 使用 HashMap 进行快速查找
- 使用 HashSet 存储唯一集合
- 使用 Collections.unmodifiableCollection() 返回只读集合

**流式操作**
- 使用 Java Stream API 进行集合操作
- 示例：
```java
return wars.values().stream()
    .filter(war -> !war.getStatus().isEnded())
    .toList();
```

### 12. 依赖集成

**Towny 集成**
- 使用 `TownyAPI.getInstance()` 获取 API
- 通过 Towny 管理国家、城镇、居民基础数据
- LegacyLand 只存储扩展数据（政体、角色、外交等）

**ItemsAdder 集成**
- 在插件启动时复制物品配置到 ItemsAdder 目录
- 配置文件位置：`src/main/resources/itemsadder/medical_items.yml`

### 13. 测试与调试

**插件测试**
- 使用 `xyz.jpenilla.run-paper` 插件快速测试
- 运行命令：`./gradlew runServer`
- 测试环境：Minecraft 1.21

**调试信息**
- 在配置文件中提供 debug 选项
- 调试模式下输出详细日志

## 开发工作流

### 1. 添加新功能

1. 在对应包下创建必要的类（Manager, Command, Listener, Event）
2. 在 Manager 中实现业务逻辑
3. 在 DatabaseManager 中添加数据持久化方法
4. 在主类中注册命令和监听器
5. 在 plugin.yml 中声明命令
6. 在 config.yml 中添加配置项
7. 测试功能并修复 bug

### 2. 数据库变更

1. 在 `DatabaseManager.createTables()` 中添加新表
2. 实现对应的 save/load/delete 方法
3. 在 Manager 中调用数据库方法
4. 测试数据持久化

### 3. 命令开发

1. 创建 Command 类，实现 `CommandExecutor` 和 `TabCompleter`
2. 在 `onCommand()` 中处理子命令
3. 在 `onTabComplete()` 中提供自动补全
4. 在主类的 `registerCommands()` 中注册
5. 在 plugin.yml 中声明命令和别名

### 4. 事件开发

1. 创建自定义事件类
2. 在适当位置触发事件：`Bukkit.getPluginManager().callEvent(event)`
3. 创建监听器类处理事件
4. 在主类中注册监听器

## 代码质量标准

### 必须遵守
- 所有公共 API 必须有注释
- 数据库操作必须使用 PreparedStatement
- 异常必须被捕获并记录日志
- Manager 类必须使用单例模式
- 命令必须提供 Tab 补全

### 推荐实践
- 使用 Lombok 简化代码
- 使用 Stream API 处理集合
- 合理使用设计模式
- 保持方法简短（不超过 50 行）
- 避免重复代码

### 避免事项
- 不要在主线程执行耗时操作
- 不要硬编码字符串，使用配置文件
- 不要直接操作 Towny 的核心数据
- 不要忽略异常
- 不要使用过时的 API

## 版本管理

**版本号格式**
- 格式：`主版本.次版本-阶段版本号`
- 当前版本：`1.0-Beta1`
- 示例：`1.0-Beta1`, `1.0-Beta2`, `1.0-Release`, `1.1-Beta1`

**版本更新位置**
- `build.gradle`: `version = '1.0-Beta1'`
- `plugin.yml`: `version: '1.0-Beta1'`

## 构建与发布

**构建命令**
```bash
./gradlew build
```

**构建产物**
- 位置：`build/libs/LegacyLand-1.0-Beta1.jar`
- 部署：复制到服务器的 `plugins/` 目录

**依赖要求**
- Towny 插件必须安装
- ItemsAdder 插件必须安装
- Java 21 运行环境

## 项目特色

### 1. 模块化设计
- 每个功能模块独立管理
- 清晰的包结构
- 低耦合高内聚

### 2. 扩展性
- 基于 Towny 扩展，不修改核心
- 易于添加新功能模块
- 配置文件灵活可调

### 3. 中文友好
- 完整的中文界面
- 中文命令别名
- 中文注释和文档

### 4. 数据持久化
- SQLite 数据库存储
- 自动保存机制
- 数据完整性保证

### 5. 游戏性设计
- 复杂的国家政体系统
- 完整的战争机制
- 丰富的玩家状态系统
- 成就系统

## 常见问题

**Q: 如何添加新的国家角色？**
A: 在 `NationRole` 枚举中添加新角色，在 `NationPermission` 中定义权限，更新 `hasPermission()` 方法。

**Q: 如何添加新的战争类型？**
A: 在 `WarType` 枚举中添加类型，在 `WarManager.createWar()` 中处理新类型的逻辑。

**Q: 如何修改数据库结构？**
A: 修改 `DatabaseManager.createTables()` 中的 SQL 语句，更新对应的 save/load 方法。注意：需要删除旧数据库或手动迁移数据。

**Q: 如何调试插件？**
A: 使用 `./gradlew runServer` 启动测试服务器，查看控制台日志，使用 `/reload` 重载插件。

## 参考资源

- [Paper API 文档](https://docs.papermc.io/)
- [Towny API 文档](https://github.com/TownyAdvanced/Towny)
- [Bukkit API 文档](https://hub.spigotmc.org/javadocs/bukkit/)
- [Lombok 文档](https://projectlombok.org/)