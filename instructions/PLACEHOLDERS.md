# LegacyLand PlaceholderAPI 变量列表

## 配置说明

在 `config.yml` 中可以配置是否启用 ActionBar 显示：

```yaml
player-status:
  # 是否启用 ActionBar 显示
  enable-actionbar: true
  # ActionBar 更新间隔 (ticks)
  actionbar-update-interval: 10  # 0.5秒
```

- 设置 `enable-actionbar: false` 可以禁用内置的 ActionBar，避免与其他插件冲突
- 禁用后，可以使用 PlaceholderAPI 变量在其他插件中自定义显示

## 可用变量

### 生命值相关
- `%legacyland_health%` - 当前生命值（如：18.5）
- `%legacyland_health_max%` - 最大生命值（如：20.0）
- `%legacyland_health_percent%` - 生命值百分比（如：92）
- `%legacyland_health_icon%` - 生命值图标（❤）
- `%legacyland_health_color%` - 生命值颜色代码（§a/§e/§c）

### 饱食度相关
- `%legacyland_food%` - 当前饱食度（如：18）
- `%legacyland_food_max%` - 最大饱食度（20）
- `%legacyland_food_percent%` - 饱食度百分比（如：90）
- `%legacyland_food_icon%` - 饱食度图标（🍖）
- `%legacyland_food_color%` - 饱食度颜色代码（§a/§e/§c）

### 饮水值相关
- `%legacyland_hydration%` - 当前饮水值（如：8）
- `%legacyland_hydration_max%` - 最大饮水值（10）
- `%legacyland_hydration_percent%` - 饮水值百分比（如：80）
- `%legacyland_hydration_icon%` - 饮水值图标（💧）
- `%legacyland_hydration_color%` - 饮水值颜色代码（§b/§e/§c）

### 体温相关
- `%legacyland_temperature%` - 当前体温（如：22.5）
- `%legacyland_temperature_icon%` - 体温图标（🔥/❄/🌡）
- `%legacyland_temperature_color%` - 体温颜色代码（§1/§9/§a/§6/§c）

### 身体状态相关
- `%legacyland_status%` - 当前身体状态（如：正常、发烧、感冒等）
- `%legacyland_status_icon%` - 状态图标（⚠ 或空）
- `%legacyland_status_color%` - 状态颜色代码（§a/§d/§e/§c）

### 职业相关
- `%legacyland_profession_main%` - 主职业名称（如：战士）
- `%legacyland_profession_main_level%` - 主职业等级（如：5）
- `%legacyland_profession_sub%` - 副职业名称（如：矿工）
- `%legacyland_profession_sub_level%` - 副职业等级（如：3）

### 季节相关
- `%legacyland_season%` - 当前季节（如：初春）
- `%legacyland_season_type%` - 季节类型（如：春季）
- `%legacyland_season_day%` - 当前季节第几天（如：3）
- `%legacyland_season_day_max%` - 季节总天数（如：8）
- `%legacyland_season_progress%` - 季节进度百分比（如：38）
- `%legacyland_season_base_temp%` - 季节基础温度（如：22.0）

## 使用示例

### 在 DeluxeMenus 中使用
```yaml
display_name: '%legacyland_health_color%%legacyland_health_icon% %legacyland_health%/%legacyland_health_max%'
```

### 在 TAB 插件中使用
```yaml
tabprefix: '%legacyland_health_color%❤%legacyland_health% %legacyland_temperature_icon%%legacyland_temperature%°C'
```

### 在 ScoreboardAPI 中使用
```yaml
lines:
  - '&e生命: %legacyland_health_color%%legacyland_health%/%legacyland_health_max%'
  - '&b饮水: %legacyland_hydration_color%%legacyland_hydration%/%legacyland_hydration_max%'
  - '&6体温: %legacyland_temperature_color%%legacyland_temperature%°C'
  - '&a季节: %legacyland_season%'
```

### 自定义 ActionBar（使用其他插件）
如果你使用支持 PlaceholderAPI 的 ActionBar 插件，可以这样配置：

```
%legacyland_health_color%%legacyland_health_icon% %legacyland_health% &8| %legacyland_food_color%%legacyland_food_icon% %legacyland_food% &8| %legacyland_hydration_color%%legacyland_hydration_icon% %legacyland_hydration% &8| %legacyland_temperature_color%%legacyland_temperature_icon% %legacyland_temperature%°C
```

## 颜色说明

### 生命值颜色
- §a (绿色) - 生命值 > 60%
- §e (黄色) - 生命值 30%-60%
- §c (红色) - 生命值 < 30%

### 饱食度颜色
- §a (绿色) - 饱食度 > 60%
- §e (黄色) - 饱食度 30%-60%
- §c (红色) - 饱食度 < 30%

### 饮水值颜色
- §b (青色) - 饮水值 > 60%
- §e (黄色) - 饮水值 30%-60%
- §c (红色) - 饮水值 < 30%

### 体温颜色
- §1 (深蓝) - 体温 ≤ 0°C（重度寒冷）
- §9 (浅蓝) - 体温 0-15°C（轻度寒冷）
- §a (绿色) - 体温 15-27°C（正常）
- §6 (金色) - 体温 27-35°C（轻度炎热）
- §c (红色) - 体温 > 35°C（重度炎热）

### 状态颜色
- §a (绿色) - 正常状态
- §d (粉色) - 愉悦状态
- §e (黄色) - 紧张状态
- §c (红色) - 异常状态
