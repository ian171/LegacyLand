# LegacyLand - 国家系统实现文档

## 已完成功能

### 第一大模块：国家成员系统 (1.1)

基于 Towny 插件，已实现完整的国家成员管理系统，包含以下核心功能：

## 核心架构

### 1. 政体系统 (GovernmentType)
- **分封制 (FEUDAL)** - 君主制国家体系
- **城市共和制 (REPUBLIC)** - 议会制国家体系

### 2. 角色与权限系统

#### 分封制角色
- **国王 (KINGDOM)** - 拥有所有权限
  - 任免所有职位
  - 任免市长
  - 宣战与结盟
  - 所有财政、司法、执法、军事权限

- **财政大臣 (CHANCELLOR)** - 财政管理
  - 调整税收比例
  - 调整土地税
  - 城市拨款
  - 军需拨款
  - 封臣税管理

- **司法大臣 (ATTORNEY_GENERAL)** - 司法审判
  - 审判被拘留者

- **执法大臣 (MINISTER_OF_JUSTICE)** - 执法管理
  - 发布逮捕令

- **军需大臣 (MINISTER_OF_DEFENSE)** - 军事后勤
  - 管理补给线
  - 发布补给运送任务
  - 调整补给奖励
  - 决定攻城名单

#### 城市共和制角色
- **总督 (GOVERNOR)** - 行政首脑
  - 提名/罢免职位
  - 任免市长
  - 所有财政、司法、执法、军事权限

- **财政官 (FINANCE_OFFICER)** - 财政管理
- **司法官 (JUDICIAL_OFFICER)** - 司法审判
- **执法官 (LEGAL_OFFICER)** - 执法管理
- **军需官 (MILITARY_SUPPLY_OFFICER)** - 军事后勤
- **议会议员 (PARLIAMENT_MEMBER)** - 议会投票
  - 投票选举总督
  - 投票官员提名
  - 提出外交议案
  - 投票外交议案

#### 通用角色
- **公民 (CITIZEN)** - 普通成员

### 3. 权限系统 (NationPermission)

完整的权限控制系统，包含：
- 人事权限（任免、提名、罢免）
- 外交权限（宣战、结盟、议案）
- 财政权限（税收、拨款）
- 司法权限（审判、逮捕）
- 军事权限（补给、攻城）
- 议会权限（投票、选举）

## 命令系统

### 基础命令
```
/nation create <国家名> <政体类型>  - 创建国家
/nation info [国家名]              - 查看国家信息
/nation list                       - 列出所有国家
/nation leave                      - 离开国家
/nation delete                     - 删除国家（仅领袖）
```

### 管理命令
```
/nation invite <玩家名>            - 邀请玩家加入
/nation kick <玩家名>              - 踢出玩家
/nation setrank <玩家名> <角色>    - 设置成员角色
```

### 财政命令
```
/nation treasury                   - 查看国库余额
/nation treasury deposit <金额>    - 存入国库
/nation treasury withdraw <金额>   - 从国库取款（需权限）
```

### 命令别名
- `/nation` = `/n` = `/guo`

## 数据模型

### Nation (国家)
- 国家名称
- 政体类型
- 领袖 ID
- 成员列表
- 建国时间
- 国库余额

### NationMember (国家成员)
- 玩家 UUID
- 玩家名称
- 所属国家
- 角色
- 加入时间

### NationManager (国家管理器)
- 单例模式管理所有国家
- 玩家-国家映射
- 权限检查
- 成员管理

## 技术特性

1. **权限系统** - 基于角色的细粒度权限控制
2. **政体系统** - 不同政体有不同的角色和权限
3. **Tab 补全** - 所有命令支持 Tab 自动补全
4. **中文支持** - 完整的中文界面和提示
5. **Towny 集成** - 依赖 Towny 插件，可扩展领地功能

## 构建信息

- **插件版本**: 1.0-Beta1
- **Minecraft 版本**: 1.21
- **Java 版本**: 21
- **依赖**: Towny 0.102.0.0
- **构建工具**: Gradle
- **构建状态**: ✅ 成功

## 文件结构

```
src/main/java/net/chen/legacyLand/
├── LegacyLand.java                    # 主插件类
└── nation/
    ├── GovernmentType.java            # 政体类型枚举
    ├── Nation.java                    # 国家数据模型
    ├── NationManager.java             # 国家管理器
    ├── NationMember.java              # 国家成员模型
    ├── NationPermission.java          # 权限枚举
    ├── NationRole.java                # 角色枚举
    └── commands/
        └── NationCommand.java         # 命令处理器
```

## 使用示例

### 创建分封制国家
```
/nation create 大明帝国 FEUDAL
```

### 创建共和制国家
```
/nation create 罗马共和国 REPUBLIC
```

### 邀请成员并设置角色
```
/nation invite PlayerName
/nation setrank PlayerName CHANCELLOR
```

### 查看国家信息
```
/nation info
/nation info 大明帝国
```

## 下一步开发计划

根据 plan 文件，后续可实现：
- 1.2 国家税收系统
- 1.3 国家外交系统
- 1.4 国家战争系统
- 1.5 攻城战系统
- 1.6 国家科技树
- 1.7 国家公会系统
- 1.8 国家领土系统
- 1.9 国策系统

## 注意事项

1. 需要安装 Towny 插件作为前置
2. 当前版本为内存存储，重启后数据会丢失（后续需添加数据持久化）
3. 部分权限功能（如逮捕、审判）需要配合其他系统实现
4. 建议在测试服务器上先进行测试

## 构建产物

插件 JAR 文件位置：`build/libs/LegacyLand-1.0-Beta1.jar`
