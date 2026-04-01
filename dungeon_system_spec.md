# 副本子系统技术方案

> 版本：v1.0  
> 目标版本：v0.0.8-alpha (Dungeon)  
> 基于：NeoForge 21.1.220 / Minecraft 1.21.1  
> 包路径：`com.piranport.dungeon`

---

## 1. 系统概览

本模组实现一套以**海域节点地图**为核心的实例副本框架。玩家组队进入独立副本维度，在多片海域节点间推进，击败深海舰队并获取战利品。

核心循环：`遗迹入口 → 钥匙开启/续接 → 书台选关/选节点 → 节点推进 → 击沉旗舰过门 → 通关结算`

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| 数据驱动 | 所有关卡、节点、敌舰配置用 JSON 定义，程序只写通用引擎 |
| 偏向原版 | 死亡机制用原版不死图腾，传送门逻辑参考下界门，战利品用箱子船实体 |
| 轻松体验 | 死亡不惩罚进度，复活代价低，补给节点产出不死图腾 |
| 还原舰R | 节点地图有向图结构、过路费节点、击沉掉落均参考舰R机制 |

### 1.2 三层数据结构

```
Chapter（章节）
  └─ Stage（关卡）= 一把钥匙 = 一次完整副本体验
       └─ Node（节点）= 舰R地图上的 A/B/C/D/E/F 各点
```

---

## 2. 数据驱动配置系统

### 2.1 文件目录结构

所有配置放在 datapack 路径下：

```
data/piranport/dungeon/
├── chapters/
│   ├── chapter_1.json
│   └── chapter_2.json
├── stages/
│   ├── 1-1.json
│   ├── 1-2.json
│   └── 1-3.json
└── enemy_sets/
    ├── enemy_set_1a.json
    ├── boss_1_1.json
    └── ...
```

### 2.2 章节配置 (Chapter)

```json
{
  "chapter_id": "chapter_1",
  "display_name": "第一章 近海防御",
  "sort_order": 1,
  "stages": ["1-1", "1-2", "1-3", "1-4"]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `chapter_id` | string | 唯一标识 |
| `display_name` | string | 书台GUI中显示的章节名，支持翻译key |
| `sort_order` | int | 章节排列顺序 |
| `stages` | string[] | 该章节包含的关卡ID列表，按顺序排列 |

### 2.3 关卡配置 (Stage)

```json
{
  "stage_id": "1-1",
  "chapter": "chapter_1",
  "display_name": "1-1 近海巡逻",
  "nodes": {
    "A": {
      "type": "battle",
      "enemies": "enemy_set_1a",
      "display_x": 50,
      "display_y": 200
    },
    "B": {
      "type": "resource",
      "rewards": [
        { "item": "piranport:fuel", "count": 32 },
        { "item": "minecraft:totem_of_undying", "count": 1, "chance": 0.15 }
      ],
      "display_x": 150,
      "display_y": 100
    },
    "C": {
      "type": "cost",
      "cost": [
        { "item": "piranport:fuel", "count": 20 }
      ],
      "cost_message": "海域湍流，消耗20燃料",
      "display_x": 150,
      "display_y": 300
    },
    "D": {
      "type": "battle",
      "enemies": "enemy_set_1d",
      "display_x": 250,
      "display_y": 200
    },
    "E": {
      "type": "boss",
      "enemies": "boss_1_1",
      "display_x": 350,
      "display_y": 200
    }
  },
  "edges": [
    { "from": "A", "to": "B" },
    { "from": "A", "to": "C" },
    { "from": "B", "to": "D" },
    { "from": "C", "to": "D" },
    { "from": "D", "to": "E" }
  ],
  "start_node": "A",
  "boss_nodes": ["E"],
  "first_clear_rewards": [
    { "item": "piranport:blueprint_example", "count": 1 }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `stage_id` | string | 唯一标识，对应钥匙物品的 DataComponent |
| `chapter` | string | 所属章节ID |
| `display_name` | string | 书台GUI中显示的关卡名 |
| `nodes` | map<string, Node> | 节点定义，key为节点ID（A/B/C...） |
| `edges` | array | 有向航线，定义节点间连通关系 |
| `start_node` | string | 起始节点ID |
| `boss_nodes` | string[] | Boss节点ID列表，全部通过即通关 |
| `first_clear_rewards` | array | 首通奖励物品列表 |

### 2.4 节点类型 (Node)

| type | 说明 | 进入行为 |
|------|------|---------|
| `battle` | 战斗节点 | 传送到独立海域战场，生成 enemies 引用的敌舰配置 |
| `boss` | Boss节点 | 同 battle，击沉后触发通关结算传送门 |
| `resource` | 补给节点 | 无战斗，进入即按 rewards 发放物品 |
| `cost` | 过路费节点 | 进入时按 cost 扣除物品，不足则弹出提示阻止进入 |

每个节点额外包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `display_x` | int | 书台GUI中节点图标的X坐标（像素） |
| `display_y` | int | 书台GUI中节点图标的Y坐标（像素） |
| `enemies` | string | 引用 enemy_sets/ 下的配置ID（仅 battle/boss） |
| `rewards` | array | 物品奖励列表，可含 chance 字段（仅 resource） |
| `cost` | array | 消耗物品列表（仅 cost） |
| `cost_message` | string | 扣除资源时的提示文本（仅 cost） |

### 2.5 敌舰配置 (EnemySet)

```json
{
  "enemy_set_id": "enemy_set_1a",
  "spawn_list": [
    { "entity": "piranport:deep_sea_destroyer", "count": 3 },
    { "entity": "piranport:deep_sea_cruiser", "count": 1 }
  ],
  "flagship": {
    "entity": "piranport:deep_sea_flagship_dd",
    "count": 1
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `spawn_list` | array | 普通敌舰列表，entity引用实体注册ID |
| `flagship` | object | 节点旗舰配置，击沉后生成传送门 |

敌舰配置与节点配置分离，同一套敌舰可被多个关卡复用。

### 2.6 配置加载

- 使用 NeoForge 的 `AddReloadListenerEvent` 注册自定义 `DungeonDataLoader`
- 启动/`/reload` 时扫描 `data/*/dungeon/` 路径，解析所有 JSON
- 校验引用完整性（edge引用的节点存在、enemy_set引用存在等），校验失败打 WARN 日志并跳过该关卡
- 解析结果缓存到 `DungeonRegistry` 单例，供运行时查询

---

## 3. 钥匙与旗舰系统

### 3.1 钥匙物品

```java
public class DungeonKeyItem extends Item {
    // DataComponent 存储：
    // - DUNGEON_STAGE_ID: string       关联的关卡ID
    // - DUNGEON_INSTANCE_ID: UUID      关联的副本实例UUID（首次进入时写入）
    // - DUNGEON_PROGRESS: CompoundTag  当前进度（已通过的节点列表、当前节点等）
}
```

注册ID：`piranport:dungeon_key`

钥匙通过以下方式获取（具体获取方式待定，以下为占位）：
- 遗迹箱子掉落
- 补给站战利品
- 合成

### 3.2 旗舰机制

| 规则 | 实现 |
|------|------|
| 持有钥匙的玩家 = 旗舰 | 服务端每tick扫描副本内玩家背包，持有匹配钥匙者为旗舰 |
| 钥匙转交 | 直接丢给对方捡起，DataComponent不变 |
| 旗舰离开时自动转交 | `PlayerLoggedOutEvent` / 回城卷轴使用时，遍历副本内其他在线玩家，转交给在线时间最长者 |
| 全员离开 → 挂起 | 副本实例状态设为 SUSPENDED，进度保留在钥匙 DataComponent 中 |
| 续接 | 旗舰持钥匙进入遗迹书台，系统读取 DUNGEON_INSTANCE_ID 加载对应实例 |

### 3.3 进度存储

钥匙的 `DUNGEON_PROGRESS` DataComponent 包含：

```java
public record DungeonProgress(
    String currentNode,           // 当前所在节点ID，null表示尚未进入
    Set<String> clearedNodes,     // 已通关的节点ID集合
    long startTimeMillis,         // 计时开始时间戳（毫秒）
    boolean timerStarted          // 计时是否已开始
) { }
```

进度存储在钥匙物品上（而非服务端存档），好处：
- 钥匙丢失 = 进度丢失，符合物品属性直觉
- 多把钥匙可同时存在，互不干扰
- 无需维护服务端持久化存储（SavedData），降低复杂度

---

## 4. 遗迹——副本入口

### 4.1 遗迹结构

遗迹为世界自然生成的结构（Structure），使用 NeoForge 的 `Structure` + Jigsaw 系统或 NBT 模板生成。

核心方块：**书台**（`piranport:dungeon_lectern`）

生成位置：海洋群系水面上的建筑结构内部。具体结构蓝图待美工设计，程序侧只需实现书台方块与交互逻辑。

### 4.2 书台方块

```java
public class DungeonLecternBlock extends BaseEntityBlock {
    // 右键交互：
    // 1. 检查玩家手持/背包中是否有 DungeonKeyItem
    // 2. 有钥匙 → 打开海域出击书GUI（DungeonBookScreen）
    // 3. 无钥匙 → 提示"需要副本钥匙"
}
```

### 4.3 书台GUI（DungeonBookScreen）

书台GUI是整个副本系统的中枢入口，包含两层界面：

**第一层：关卡选择界面**

```
┌──────────────────────────────────────┐
│ [第一章] [第二章] [第三章]  ← 章节页签 │
├──────────────────────────────────────┤
│                                      │
│  [1-1 近海巡逻]    ← 关卡按钮       │
│  [1-2 远洋护航]       已通关的显示√  │
│  [1-3 夜战突袭]       未解锁的灰色   │
│  [1-4 港口防御]                      │
│                                      │
├──────────────────────────────────────┤
│          [出击]  [退出]              │
└──────────────────────────────────────┘
```

- 章节页签从 `DungeonRegistry` 读取，按 sort_order 排列
- 关卡按钮从章节配置读取，按 stages 列表顺序排列
- 关卡解锁条件：上一关首通记录存在（通过 `DungeonSavedData` 查询）
- 已通关关卡可重复选择（刷速通/刷掉落）
- 点击"出击"后进入第二层

**第二层：节点地图界面**

```
┌──────────────────────────────────────┐
│ 1-1 近海巡逻           计时: 00:00  │
├──────────────────────────────────────┤
│                                      │
│   (A)──→(B)──→(D)──→(E)            │
│    │            ↑                    │
│    └──→(C)──────┘                    │
│                                      │
│  [A] 战斗  [B] 补给  [C] 漩涡      │
│  [D] 战斗  [E] ★Boss               │
│                                      │
├──────────────────────────────────────┤
│        [选择节点出击]  [返回]        │
└──────────────────────────────────────┘
```

- 节点位置由 `display_x` / `display_y` 确定（手工布局，还原舰R地图）
- 已通关节点标记为绿色，当前可前往的节点高亮，不可前往的灰色
- 旗舰点击可前往的节点 → 全队传送到对应海域
- 航线（edge）用直线或贝塞尔曲线连接节点图标

### 4.4 联机大厅

书台GUI兼任联机大厅功能（替代总策划案中的"出击渠"）：

- 旗舰打开书台时，创建一个大厅 session，绑定到该遗迹 BlockPos
- 其他玩家对同一书台右键 → 加入该大厅，看到旗舰选择的关卡
- 旗舰点击"出击"→ 大厅内所有玩家一起传送
- 关闭GUI = 离开大厅
- 大厅成员列表显示在GUI侧边，队长（旗舰）名字金色

实现方式：
- 服务端维护 `DungeonLobbyManager`，key为书台 BlockPos，value为玩家列表
- 开关GUI通过 C2S/S2C payload 同步

---

## 5. 副本实例管理

### 5.1 副本维度

使用单个共享维度 `piranport:dungeon`，内部通过**区域隔离**实现多副本并存：

- 每个副本实例分配一个不重叠的区域（如每个实例占 1024×1024 方块的正方形）
- 实例区域起始坐标 = `instanceIndex * 1024`
- 每个节点在实例区域内的子区域生成战场

替代方案：每个实例创建独立 DynamicDimension。但 NeoForge 1.21.1 对动态维度支持有限，建议先用区域隔离方案。

### 5.2 实例生命周期

```
CREATING → ACTIVE → SUSPENDED → ACTIVE → ... → COMPLETED → CLEANUP
```

| 状态 | 条件 | 行为 |
|------|------|------|
| CREATING | 旗舰在书台点击"出击" | 分配区域、生成起始节点战场 |
| ACTIVE | 有玩家在副本内 | 正常运行，计时进行 |
| SUSPENDED | 所有玩家离开副本 | 保留战场状态，计时不中断 |
| COMPLETED | 所有 boss_nodes 通关 | 触发通关结算，发放首通奖励 |
| CLEANUP | 通关后或旗舰放弃钥匙 | 清除区域内所有方块和实体 |

### 5.3 服务端管理器

```java
public class DungeonInstanceManager {
    // SavedData 存储：
    // - 活跃实例列表（instanceId → 区域坐标、关卡ID、状态）
    // - 实例索引计数器（分配不重叠区域）
    
    // 方法：
    DungeonInstance createInstance(String stageId, ServerPlayer flagship);
    DungeonInstance loadInstance(UUID instanceId);
    void suspendInstance(UUID instanceId);
    void completeInstance(UUID instanceId);
    void cleanupInstance(UUID instanceId);
}
```

### 5.4 节点战场生成

每个节点传送时：
1. 在实例区域内的子区域生成海域战场（平坦水面 + 天空，用 `ServerLevel.setBlock` 批量放置或预制NBT结构）
2. 按 `EnemySet` 配置生成敌舰实体
3. 标记旗舰实体（深海旗舰），绑定击沉监听
4. 传送所有队员到战场指定出生点

---

## 6. 节点推进流程

### 6.1 战斗节点 (battle)

```
进入节点 → 生成敌舰 → 玩家战斗 → 击沉深海旗舰 → 生成传送门
  → 全队过门 → 触发击沉掉落（箱子船） → 标记节点通关 → 返回节点地图选择下一节点
```

### 6.2 Boss节点 (boss)

同战斗节点，但传送门连接至**通关结算区域**而非节点地图。

### 6.3 补给节点 (resource)

```
选择该节点 → 直接按 rewards 发放物品到旗舰背包 → 标记节点通关 → 返回节点地图
```

不传送到战场，在书台GUI内直接结算。

### 6.4 过路费节点 (cost)

```
选择该节点 → 检查旗舰背包是否满足 cost → 不足则弹出提示取消
  → 满足则扣除物品，弹出 cost_message 提示 → 标记节点通关 → 返回节点地图
```

同样不传送到战场，在书台GUI内直接结算。

---

## 7. 传送门与击沉掉落

### 7.1 深海旗舰击沉 → 传送门

- 每个战斗/Boss节点的 `EnemySet.flagship` 实体被击沉时，触发 `FlagshipKilledEvent`（自定义事件）
- 在旗舰沉没位置生成 `DungeonPortalEntity`（传送门实体）
- 传送门无时间限制，节点内剩余敌舰继续存在，队伍可选择清场后再过门

### 7.2 传送门实体

```java
public class DungeonPortalEntity extends Entity {
    // 碰撞检测：玩家进入碰撞箱后标记为"已进入"
    // 全队过门判定：所有副本内存活玩家均标记"已进入"后触发节点结束
    // 渲染：旋转的传送门视觉效果（客户端渲染器）
}
```

### 7.3 箱子船系统

所有被击沉的敌舰在沉没位置生成**箱子船实体**，替代直接掉落物品（防止物品飘到水面上丢失）。

```java
public class LootShipEntity extends Entity {
    // 内含 SimpleContainer，根据敌舰等级填充对应战利品表
    // 玩家右键打开容器GUI（类似箱子）
    // 300秒后未开启则自动消失
    // 同一敌舰只生成一次
}
```

| 敌舰等级 | 箱子船外观 | 战利品表引用 |
|---------|-----------|------------|
| 普通深海 | 橡木箱子船模型 | `loot_tables/dungeon/normal_ship.json` |
| 精英深海 | 深色橡木箱子船模型 | `loot_tables/dungeon/elite_ship.json` |
| 深海旗舰 | 樱花箱子船模型 | `loot_tables/dungeon/boss_ship.json` |

战利品表使用原版 LootTable 系统（`data/piranport/loot_table/dungeon/`），可被数据包覆盖。

---

## 8. 死亡与复活系统

### 8.1 设计目标

轻松体验，死亡不惩罚进度，不影响队伍推进。

### 8.2 死亡流程

```
玩家在副本内死亡
  → 物品不掉落（gamerule keepInventory 在副本维度强制为 true）
  → 玩家传送回开启本副本的遗迹位置（书台 BlockPos）
  → 玩家在遗迹中与书台交互，打开复活界面
```

### 8.3 复活界面

```
┌─────────────────────────┐
│   你已阵亡              │
│                         │
│   消耗 1个不死图腾      │
│   返回战场？            │
│                         │
│   [复活]  [放弃]        │
└─────────────────────────┘
```

- **复活**：消耗背包中1个 `minecraft:totem_of_undying` → 传送到队友所在节点的战场入口（出生点），而非队友精确位置
- **放弃**：留在遗迹，等待队友通关。通关时仍计入首通资格（只要玩家UUID在实例玩家列表中且未主动退出）
- 背包中无不死图腾 → 复活按钮灰色不可点击，提示"需要不死图腾"

### 8.4 实现要点

- 监听 `LivingDeathEvent`，检查玩家是否在副本维度
- 在副本维度取消原版死亡画面，改为直接传送 + 打开复活GUI
- 副本维度 gamerule `keepInventory = true` 在实例创建时设置
- 复活传送目标从 `DungeonInstanceManager` 获取当前节点的出生点坐标

---

## 9. 通关结算

### 9.1 触发条件

所有 `boss_nodes` 中的节点均被标记通关 → 最终传送门过门后触发。

### 9.2 结算流程

```
全队过最终传送门
  → 计时停止，记录耗时
  → 遍历实例内玩家列表：
      → 检查 UUID 是否在该关卡的首通记录中
      → 不在 → 发放 first_clear_rewards，写入首通记录
      → 已在 → 跳过首通奖励
  → 显示结算画面（耗时、获得物品）
  → 提交排行榜成绩（如果有效）
  → 传送所有玩家回遗迹
  → 副本实例进入 CLEANUP
```

### 9.3 首通记录存储

```java
public class DungeonSavedData extends SavedData {
    // Map<String, Set<UUID>>  stageId → 已首通的玩家UUID集合
    // 存储在世界存档的 data/piranport_dungeon.dat
    
    boolean hasFirstCleared(String stageId, UUID playerUuid);
    void markFirstCleared(String stageId, UUID playerUuid);
}
```

### 9.4 计时与排行榜

| 规则 | 说明 |
|------|------|
| 计时开始 | 旗舰选择第一个节点并传送的瞬间 |
| 计时结束 | 最终Boss节点全队过门的瞬间 |
| 计时单位 | 毫秒，显示为 `mm:ss.SSS` |
| 不中断 | 死亡、离开、旗舰交接均不暂停计时 |
| 成绩无效 | 存在手动撤离的节点 → 不提交 |

排行榜存储：

```java
public class DungeonLeaderboard extends SavedData {
    // Map<String, List<LeaderboardEntry>> stageId → 排行记录
    // LeaderboardEntry: { playerUuid, playerName, timeMillis, timestamp }
    // 按 timeMillis 升序排列，保留前100条
}
```

---

## 10. 回城卷轴

### 10.1 物品

```java
public class TownScrollItem extends Item {
    // 进入副本时自动发放1个
    // 使用后弹出确认GUI
    // 旗舰使用时额外提示钥匙交接
    // 冷却时间3秒（防误触）
}
```

### 10.2 使用流程

1. 右键使用 → 弹出确认界面"确认离开副本？"
2. 若为旗舰 → 额外显示"将钥匙交接给：[队友列表下拉] 或 [带走钥匙]"
3. 确认 → 传送回遗迹/世界出生点，卷轴消耗
4. 旗舰带走钥匙离开 + 无其他成员 → 副本 SUSPENDED
5. 离开不触发通关判定，不提交排行榜

---

## 11. 新增注册内容总览

### 11.1 物品

| 注册ID | 类 | 说明 |
|--------|---|------|
| `dungeon_key` | `DungeonKeyItem` | 副本钥匙 |
| `town_scroll` | `TownScrollItem` | 回城卷轴 |

### 11.2 方块

| 注册ID | 类 | 说明 |
|--------|---|------|
| `dungeon_lectern` | `DungeonLecternBlock` | 书台（遗迹核心交互方块） |

### 11.3 实体

| 注册ID | 类 | 说明 |
|--------|---|------|
| `dungeon_portal` | `DungeonPortalEntity` | 节点传送门 |
| `loot_ship` | `LootShipEntity` | 箱子船（战利品容器） |

### 11.4 DataComponents

| 注册ID | 类 | 所在物品 | 说明 |
|--------|---|---------|------|
| `dungeon_stage_id` | `String` | DungeonKeyItem | 关卡ID |
| `dungeon_instance_id` | `UUID` | DungeonKeyItem | 实例UUID |
| `dungeon_progress` | `DungeonProgress` | DungeonKeyItem | 进度数据 |

### 11.5 GUI/Menu

| 注册ID | Screen | Menu | 说明 |
|--------|--------|------|------|
| `dungeon_book` | `DungeonBookScreen` | `DungeonBookMenu` | 书台主界面（选关+地图+大厅） |
| `dungeon_revive` | `DungeonReviveScreen` | — | 复活确认界面（纯客户端Screen+C2S） |
| `dungeon_result` | `DungeonResultScreen` | — | 通关结算画面 |
| `loot_ship_menu` | `LootShipScreen` | `LootShipMenu` | 箱子船容器 |
| `town_scroll_confirm` | `TownScrollScreen` | — | 回城确认界面 |

### 11.6 网络包 (Payload)

| 方向 | 名称 | 说明 |
|------|------|------|
| C2S | `JoinLobbyPayload` | 玩家加入书台大厅 |
| C2S | `LeaveLobbyPayload` | 玩家离开大厅 |
| C2S | `SelectStagePayload` | 旗舰选择关卡 |
| C2S | `SelectNodePayload` | 旗舰选择出击节点 |
| C2S | `ReviveRequestPayload` | 请求复活（消耗图腾） |
| C2S | `TownScrollUsePayload` | 确认使用回城卷轴 |
| S2C | `LobbyUpdatePayload` | 同步大厅成员列表 |
| S2C | `DungeonStatePayload` | 同步副本状态（节点地图、计时等） |
| S2C | `NodeEnteredPayload` | 通知客户端进入新节点 |
| S2C | `DungeonResultPayload` | 通关结算数据 |
| S2C | `PlayerDiedInDungeonPayload` | 通知客户端打开复活界面 |

### 11.7 SavedData

| 类 | 存储文件 | 说明 |
|---|---------|------|
| `DungeonSavedData` | `piranport_dungeon.dat` | 首通记录 |
| `DungeonLeaderboard` | `piranport_leaderboard.dat` | 排行榜 |
| `DungeonInstanceManager` (内含SavedData) | `piranport_instances.dat` | 活跃副本实例 |

---

## 12. 包结构规划

```
src/main/java/com/piranport/dungeon/
├── DungeonConstants.java              # 常量（区域大小、默认超时等）
├── data/
│   ├── ChapterData.java               # 章节配置POJO
│   ├── StageData.java                 # 关卡配置POJO
│   ├── NodeData.java                  # 节点配置POJO
│   ├── EnemySetData.java             # 敌舰配置POJO
│   ├── DungeonDataLoader.java        # JSON加载器（ReloadListener）
│   └── DungeonRegistry.java          # 配置查询单例
├── instance/
│   ├── DungeonInstance.java           # 单个副本实例状态
│   ├── DungeonInstanceManager.java   # 实例生命周期管理
│   └── NodeBattleField.java          # 节点战场生成逻辑
├── key/
│   ├── DungeonKeyItem.java
│   ├── DungeonProgress.java           # DataComponent record
│   └── FlagshipManager.java          # 旗舰判定与转交
├── lobby/
│   └── DungeonLobbyManager.java      # 联机大厅管理
├── block/
│   ├── DungeonLecternBlock.java
│   └── DungeonLecternBlockEntity.java
├── entity/
│   ├── DungeonPortalEntity.java
│   └── LootShipEntity.java
├── item/
│   └── TownScrollItem.java
├── menu/
│   ├── DungeonBookMenu.java
│   └── LootShipMenu.java
├── client/
│   ├── DungeonBookScreen.java         # 书台GUI（选关+地图+大厅）
│   ├── DungeonReviveScreen.java       # 复活界面
│   ├── DungeonResultScreen.java       # 通关结算
│   ├── TownScrollScreen.java          # 回城确认
│   ├── LootShipScreen.java            # 箱子船容器
│   ├── NodeMapRenderer.java           # 节点地图渲染（有向图绘制）
│   └── DungeonHudLayer.java           # 副本内HUD（计时、当前节点等）
├── network/
│   ├── JoinLobbyPayload.java
│   ├── LeaveLobbyPayload.java
│   ├── SelectStagePayload.java
│   ├── SelectNodePayload.java
│   ├── ReviveRequestPayload.java
│   ├── TownScrollUsePayload.java
│   ├── LobbyUpdatePayload.java
│   ├── DungeonStatePayload.java
│   ├── NodeEnteredPayload.java
│   ├── DungeonResultPayload.java
│   └── PlayerDiedInDungeonPayload.java
├── saved/
│   ├── DungeonSavedData.java          # 首通记录
│   └── DungeonLeaderboard.java        # 排行榜
└── event/
    └── DungeonEventHandler.java       # 监听死亡/登出/维度变更等
```

---

## 13. 实现优先级建议

建议分3个子阶段推进：

### Phase 8A：数据与骨架
1. `DungeonDataLoader` + `DungeonRegistry`（JSON加载）
2. `DungeonKeyItem` + `DungeonProgress` DataComponent
3. `DungeonLecternBlock` + 基础交互
4. `DungeonBookScreen` 关卡选择界面（先不做节点地图）
5. `DungeonInstanceManager` 实例创建/挂起/续接
6. 副本维度注册 + 区域隔离基础设施

### Phase 8B：节点战斗
7. 节点地图GUI（`NodeMapRenderer`）
8. 战场生成（`NodeBattleField`）
9. 敌舰生成（依赖NPC系统，可先用原版怪物占位）
10. `DungeonPortalEntity` 传送门
11. `LootShipEntity` 箱子船
12. 节点推进逻辑（过路费/补给/战斗/Boss流转）

### Phase 8C：完善体验
13. 死亡/复活系统
14. 回城卷轴
15. 联机大厅
16. 通关结算 + 首通奖励
17. 计时 + 排行榜
18. `DungeonHudLayer` 副本内HUD

---

## 14. 与现有系统的交互

| 现有系统 | 交互点 |
|---------|--------|
| 舰装系统 (ShipCoreItem) | 副本内玩家仍使用舰装核心进行战斗，无需修改 |
| 火控系统 (FireControlManager) | 副本内正常使用，敌舰实体需实现可被火控锁定 |
| 航空系统 (AircraftEntity) | 副本内正常使用 |
| 食物Buff系统 | 副本内正常使用，补给节点可发放食物 |
| TransformationManager | 副本内变身逻辑不变 |
| 配置系统 (ModCommonConfig) | 新增副本相关配置项（区域大小、箱子船超时等） |

---

## 15. 待定与扩展

| 项目 | 状态 | 说明 |
|------|------|------|
| 遗迹结构蓝图 | 待美工 | 需要 NBT 结构文件 |
| 深海NPC实体 | 依赖v0.0.6+ | 当前可用原版怪物占位测试 |
| 钥匙获取方式 | 待定 | 暂定遗迹箱子+补给站掉落 |
| 传送门视觉效果 | 待美工 | 旋转粒子效果或自定义渲染 |
| 箱子船模型 | 待美工 | 三种等级的船模型 |
| 玩家自建传送门 | 后续版本 | 导航石+框架方块→绑定遗迹 |
| 人机队友系统 | 后续版本 | AI舰娘填补空位 |
| 结算画面UI | 待设计 | 展示耗时、掉落、首通标记 |
