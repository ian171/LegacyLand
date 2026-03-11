# 资源平衡系统 - 使用文档

## 系统概述

这是一个基于地理位置的资源平衡系统，包含三个核心功能：

1. **资源产出分化** - 根据生物群落修改矿物掉落
2. **工业中心化** - 特定区域才能精炼高级资源
3. **物流价值链** - 基于距离和物品重量计算运输费用

## 核心特性

### 1. 资源产出分化

#### 生物群落资源配置

| 生物群落 | 矿石类型 | 特殊掉落 | 概率 |
|---------|---------|---------|------|
| 沙漠 (DESERT) | 铁矿 | 高纯度铁 | 10% |
| 沙漠 (DESERT) | 金矿 | 高纯度金 | 15% |
| 恶地 (BADLANDS) | 金矿 | 高纯度金 | 20% |
| 山地 (PEAKS) | 铜矿 | 高纯度铜 | 12% |
| 其他群落 | 所有矿石 | 杂质资源 | 100% |

#### 资源类型

- **高纯度资源**：稀有资源，只能在国家熔炉精炼
  - 高纯度铁 (重量: 1.5)
  - 高纯度金 (重量: 2.0)
  - 高纯度铜 (重量: 1.3)

- **杂质资源**：普通资源，可在任何熔炉精炼
  - 杂质铁 (重量: 1.0)
  - 杂质金 (重量: 1.0)
  - 杂质铜 (重量: 1.0)

### 2. 工业中心化

#### 国家熔炉系统

- 高纯度资源只能在国家熔炉区域内精炼
- 精炼时自动扣除 5% 的精炼税
- 税费自动转入国家账户

#### 工业区域类型

- `NATIONAL_FURNACE` - 国家熔炉（用于精炼高纯度资源）
- `MINT_FACTORY` - 国家印钞厂
- `PROCESSING_PLANT` - 加工厂

### 3. 物流价值链

#### 运输成本计算

```
基础运费 = 距离 × 重量 × 基础费率 (0.1)
保险费 = 基础运费 × 保险费率 (5%)
总费用 = 基础运费 + 保险费
```

## 命令使用

### 基础命令

```bash
/resource help                    # 查看帮助
/resource info                    # 查看当前资源信息
```

### 管理员命令

#### 给予资源
```bash
/resource give <玩家> <资源类型> <数量>

# 示例
/resource give Steve HIGH_PURITY_IRON 10
/resource give Alex IMPURE_GOLD 5
```

#### 注册工业区域
```bash
/resource zone register <国家名> <半径> <区域类型>

# 示例 - 在当前位置注册半径50格的国家熔炉
/resource zone register MyNation 50 NATIONAL_FURNACE

# 示例 - 注册印钞厂
/resource zone register MyNation 30 MINT_FACTORY
```

#### 查看区域信息
```bash
/resource zone info              # 查看当前位置的工业区域信息
```

#### 计算运输成本
```bash
/resource transport <x> <y> <z>

# 示例 - 计算到坐标 (100, 64, 200) 的运输成本
/resource transport 100 64 200
```

## API 接口

### 初始化

```java
// 在插件启动时自动初始化
ResourceSystemManager manager = ResourceSystemManager.getInstance();
```

### 注册工业区域

```java
// 注册国家熔炉
Location center = new Location(world, 100, 64, 200);
manager.registerIndustrialZone(
    "MyNation",                              // 国家名称
    center,                                  // 区域中心
    50.0,                                    // 半径
    IndustrialZoneManager.ZoneType.NATIONAL_FURNACE  // 区域类型
);
```

### 创建资源物品

```java
// 创建高纯度铁
ItemStack item = manager.createResourceItem(ResourceType.HIGH_PURITY_IRON, 10);

// 给予玩家
manager.giveResourceToPlayer(player, ResourceType.HIGH_PURITY_GOLD, 5);
```

### 检查资源类型

```java
// 检查物品是否是特殊资源
if (manager.isResourceItem(itemStack)) {
    ResourceType type = manager.getResourceType(itemStack);
    player.sendMessage("这是: " + type.getDisplayName());
}
```

### 计算运输成本

```java
// 计算运输成本
Location from = player.getLocation();
Location to = new Location(world, 1000, 64, 1000);

LogisticsCalculator.TransportCost cost =
    manager.calculateTransportCost(from, to, items);

player.sendMessage(cost.getDetailedInfo());
```

### 获取玩家背包重量

```java
double weight = manager.getPlayerInventoryWeight(player);
player.sendMessage("背包重量: " + weight + " 单位");
```

## 权限节点

```yaml
legacyland.resource.give          # 给予资源物品
legacyland.resource.zone          # 管理工业区域
```

## 配置示例

### 在服务器启动时注册工业区域

```java
@EventHandler
public void onServerLoad(ServerLoadEvent event) {
    ResourceSystemManager manager = ResourceSystemManager.getInstance();

    // 注册国家熔炉
    World world = Bukkit.getWorld("world");
    Location furnaceCenter = new Location(world, 0, 64, 0);
    manager.registerIndustrialZone(
        "MainNation",
        furnaceCenter,
        100.0,
        IndustrialZoneManager.ZoneType.NATIONAL_FURNACE
    );
}
```

## 线程安全

系统完全支持 Folia 多线程环境：

- 使用 `ConcurrentHashMap` 存储所有共享数据
- 使用 `FoliaScheduler.runForPlayer()` 处理玩家相关操作
- 使用 `FoliaScheduler.runTaskGlobal()` 处理全局任务
- 所有单例模式使用双重检查锁定

## 性能优化

- 区域检测使用距离计算，O(1) 复杂度
- 资源类型使用 NBT 标签存储，避免字符串比较
- 事件监听器使用 `EventPriority.HIGH` 优先级
- 异步处理税收扣除，避免阻塞主线程

## 扩展性

### 添加新的资源类型

在 `ResourceType.java` 中添加：

```java
HIGH_PURITY_DIAMOND("高纯度钻石", Material.DIAMOND, 3.0, "§b高纯度钻石"),
```

### 添加新的生物群落配置

在 `BiomeResourceConfig.java` 的静态块中添加：

```java
Map<Material, ResourceDrop> jungleDrops = new HashMap<>();
jungleDrops.put(Material.EMERALD_ORE,
    new ResourceDrop(ResourceType.HIGH_PURITY_EMERALD, 0.08));
BIOME_DROPS.put(Biome.JUNGLE, jungleDrops);
```

### 自定义运输费率

修改 `LogisticsCalculator.java` 中的常量：

```java
private static final double BASE_RATE = 0.1;        // 基础费率
private static final double INSURANCE_RATE = 0.05;  // 保险费率
private static final double MIN_FEE = 1.0;          // 最小费用
```

## 故障排除

### 问题：高纯度资源无法精炼

**解决方案**：
1. 检查是否在国家熔炉区域内：`/resource zone info`
2. 确认区域已正确注册
3. 检查玩家余额是否足够支付精炼税

### 问题：矿石不掉落特殊资源

**解决方案**：
1. 确认生物群落配置正确
2. 检查是否在创造模式（创造模式不处理）
3. 特殊掉落有概率，多次尝试

### 问题：运输成本计算错误

**解决方案**：
1. 确认起点和终点在同一世界
2. 检查物品是否有正确的 NBT 标签
3. 使用 `/resource info` 查看背包重量

## 技术规范

- **Paper API**: 1.20+
- **Java**: 17+
- **依赖**: Vault, Towny
- **线程安全**: 完全支持 Folia
- **异常处理**: 所有关键操作都有 try-catch 保护
- **日志记录**: 使用 Logger 记录所有重要事件
