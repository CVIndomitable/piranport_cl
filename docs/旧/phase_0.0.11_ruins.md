# Phase v0.0.11-alpha — Ruins（主世界遗迹）

> 对应策划案 §8.3 主世界建筑 + §7.1 深海NPC（部分）
> 基线版本：v0.0.10-alpha (Arsenal)
> 平台：NeoForge 21.1.220 / Minecraft 1.21.1 / Java 21

---

## 目标概述

在主世界海洋生成四类遗迹建筑（传送门遗迹、补给站、前哨站、深海基地），配套深海NPC扩展、战利品系统、模组新群系，以及与副本系统的联动（出击渠掉落）。本 Phase 是模组进入"开放世界探索"阶段的核心里程碑。

---

## Step 0 — 前置：新群系注册

**目的**：深海基地需要在模组自定义群系中生成，其余三类建筑在原版海洋群系生成。

### 0.1 注册 `piranport:abyssal_ocean` 群系

- 创建 `data/piranport/worldgen/biome/abyssal_ocean.json`
- 基于 `minecraft:deep_ocean` 派生，调整以下参数：
  - `water_color` / `water_fog_color`：深紫或暗红色调，区别于原版深海
  - `fog_color`：压抑偏暗
  - `sky_color`：暗灰
  - `creature_spawn_probability`：0（不生成原版水生生物）
  - `music`：可先留空，后续版本补音乐
- 不添加原版鱼/鱿鱼/海豚的 spawn 列表

### 0.2 群系放置

- 创建自定义 `BiomeModifier`（参考现有 `SaltGenBiomeModifier` 模式）或使用 Multi-Noise 配置
- **推荐方案**：在 `data/piranport/worldgen/` 下通过 `dimension` 覆盖不可行（会替换整个主世界），改用以下方案之一：
  - **方案A（推荐）**：使用 NeoForge 的 `AddSurfaceBiomeModifier` 把 `abyssal_ocean` 注入到主世界的 deep_ocean 区域，以一定权重替换。需要自定义 `BiomeModifier` codec。
  - **方案B（备选）**：不创建真正的新群系，改用**结构集合（Structure Set）** 的 `biomes` 过滤器，让深海基地只在 `deep_ocean` / `deep_frozen_ocean` 等深海群系中生成。此方案更简单但不改变水色/雾色。
- **决策**：先用方案B快速推进，后续版本再补方案A的沉浸感优化。在代码和配置中预留 `abyssal_ocean` 群系 JSON 文件但暂不注入主世界。

### 产出文件
```
data/piranport/worldgen/biome/abyssal_ocean.json          # 群系定义（预留）
src/.../worldgen/AbyssalOceanBiomeModifier.java            # 预留，本phase暂不激活
```

---

## Step 1 — 结构模板与世界生成

**目的**：使四类遗迹在主世界海洋群系正确生成。

### 1.1 NBT 结构模板

使用 Minecraft 结构方块导出 `.nbt` 模板，放入 `data/piranport/structure/`。

每类建筑先做 **1个基础模板**，后续版本可追加变体：

| 建筑类型 | 模板文件 | 大致尺寸 | 说明 |
|----------|---------|---------|------|
| 传送门遗迹 | `portal_ruin_1.nbt` | ~16×10×16 | 含深海传送门框架方块（新方块），无敌人 |
| 补给站 | `supply_depot_1.nbt` | ~24×12×24 | 简单建筑+残骸，含战利品箱位置标记 |
| 前哨站 | `outpost_1.nbt` | ~48×16×64 | 含1个4格宽×32格长跑道结构 |
| 深海基地 | `abyssal_base_1.nbt` | ~80×20×80 | 含1-2个5格宽×32格长跑道 |

> **重要**：模板中用 `jigsaw` 方块或 `structure_void` + `data` command block 标记以下位置：
> - 战利品箱生成点（标记 `piranport:loot_chest`）
> - 敌人刷出点（标记 `piranport:enemy_spawn`，附带难度标签）
> - 传送门框架位置（仅传送门遗迹）

### 1.2 结构注册（Structure / Structure Set）

#### 1.2.1 四个 Structure JSON

路径：`data/piranport/worldgen/structure/`

```json
// portal_ruin.json 示例
{
  "type": "minecraft:single_pool_element",  // 或 jigsaw
  "biomes": "#minecraft:is_ocean",
  "step": "surface_structures",
  "spawn_overrides": {},
  "terrain_adaptation": "beard_box",
  "start_pool": "piranport:portal_ruin/start",
  "size": 1,
  "start_height": { "type": "minecraft:ocean_floor" },
  "project_start_to_heightmap": "OCEAN_FLOOR_WG",
  "max_distance_from_center": 16
}
```

每个结构的 `biomes` 过滤：

| 结构 | biomes 过滤 |
|------|------------|
| `portal_ruin` | `#minecraft:is_ocean` |
| `supply_depot` | `#minecraft:is_ocean` |
| `outpost` | `#minecraft:is_ocean`（排除 frozen） |
| `abyssal_base` | `#minecraft:is_deep_ocean`（仅深海群系） |

#### 1.2.2 四个 Structure Set JSON

路径：`data/piranport/worldgen/structure_set/`

| Structure Set | placement | spacing | separation | salt |
|---------------|-----------|---------|------------|------|
| `portal_ruins` | `random_spread` | 40 | 20 | 随机 |
| `supply_depots` | `random_spread` | 32 | 16 | 随机 |
| `outposts` | `random_spread` | 48 | 24 | 随机 |
| `abyssal_bases` | `random_spread` | 64 | 32 | 随机 |

> spacing / separation 值为初始测试值，后续通过 config 可调。

#### 1.2.3 Template Pool JSON

路径：`data/piranport/worldgen/template_pool/`

每个结构一个 pool，引用对应 `.nbt` 文件。

### 1.3 Structure Processor（可选）

- 创建 `RuinProcessor` 对补给站、前哨站模板做随机损坏（一定概率把方块替换为 air/cobblestone），模拟战场残骸效果。
- 传送门遗迹参考原版 `ruined_portal` 的 processor 做法。

### 产出文件
```
src/.../worldgen/ModStructures.java              # 结构相关 DataGen 或手写 JSON 注册辅助
data/piranport/structure/portal_ruin_1.nbt       # (手动创建)
data/piranport/structure/supply_depot_1.nbt
data/piranport/structure/outpost_1.nbt
data/piranport/structure/abyssal_base_1.nbt
data/piranport/worldgen/structure/*.json          # 4个
data/piranport/worldgen/structure_set/*.json      # 4个
data/piranport/worldgen/template_pool/*.json      # 4个
data/piranport/worldgen/processor_list/ruin_degradation.json
```

---

## Step 2 — 新方块：深海传送门框架

**目的**：传送门遗迹中包含可收集的传送门框架材料（策划案提到"含有传送门框架材料可供收集"）。

### 2.1 `PortalFrameBlock`

- 注册 `ModBlocks.ABYSSAL_PORTAL_FRAME`
- 外观类似下界传送门框架但采用深紫/暗色调贴图
- 普通方块，可被镐采集
- 合成用途：后续版本用于建造深海世界传送门（§8.2），本Phase仅注册方块

### 2.2 `AbyssalPortalBlock`（预留）

- 类似下界传送门的不可采集方块，本Phase仅注册，不实现传送逻辑
- 传送门遗迹模板中的框架区域留空（无激活态传送门），玩家只能采集框架材料

### 产出文件
```
src/.../block/PortalFrameBlock.java
src/.../block/AbyssalPortalBlock.java              # 预留
ModBlocks.java                                      # +2注册
assets/piranport/blockstates/abyssal_portal_frame.json
assets/piranport/models/block/abyssal_portal_frame.json
assets/piranport/textures/block/abyssal_portal_frame.png   # 贴图
```

---

## Step 3 — 深海NPC扩展

**目的**：当前仅有 `LowTierDestroyerEntity`（低级驱逐），需要扩展更多舰种以配合不同难度的遗迹。

### 3.1 NPC 舰种规划

基于策划案 §7.1 + §8.3 的敌人配置需求：

| 实体类 | 注册ID | 出现建筑 | AI概述 |
|--------|--------|---------|--------|
| `AbyssalSupplyEntity` | `abyssal_supply` | 补给站 | 补给酱，低血量无攻击，被动型 |
| `AbyssalDestroyerEntity` | `abyssal_destroyer` | 补给站/前哨站/深海基地 | 炮击（抛物线），环绕AI |
| `AbyssalLightCruiserEntity` | `abyssal_light_cruiser` | 补给站/前哨站/深海基地 | 炮击+鱼雷，环绕AI |
| `AbyssalHeavyCruiserEntity` | `abyssal_heavy_cruiser` | 前哨站/深海基地 | 重炮+装甲较高 |
| `AbyssalBattlecruiserEntity` | `abyssal_battlecruiser` | 前哨站/深海基地 | 高伤重炮，追踪弹混合 |
| `AbyssalBattleshipEntity` | `abyssal_battleship` | 深海基地 | 最高伤害/装甲，追踪弹频率高 |
| `AbyssalLightCarrierEntity` | `abyssal_light_carrier` | 前哨站/深海基地 | 放飞战斗机+攻击机AI |
| `AbyssalCarrierEntity` | `abyssal_carrier` | 深海基地 | 大量舰载机 |
| `AbyssalSubmarineEntity` | `abyssal_submarine` | 深海基地 | 水下隐身+鱼雷 |

### 3.2 实体基类重构

创建 `AbstractAbyssalEntity extends PathfinderMob` 作为所有深海NPC的共同基类：

```java
public abstract class AbstractAbyssalEntity extends PathfinderMob {
    // 共有属性
    protected double orbitRadius;       // 环绕半径
    protected double attackRange;       // 攻击射程
    protected int reloadTicks;          // 装填间隔
    protected int burstCount;           // 连射数
    
    // 共有AI Goal
    // - OrbitTargetGoal    (环绕AI，策划案§7.1)
    // - AbyssalShootGoal   (炮击，含抛物线+提前量)
    // - ClusterAlertGoal   (集群共享警戒，策划案§7.1)
    
    // 共有弹道
    // - 发射 AbyssalShellEntity (抛物线弹道)
    // - 每N发混入一枚追踪弹（策划案抛物线-追踪机制）
}
```

### 3.3 深海NPC弹道系统

#### 3.3.1 `AbyssalShellEntity`（深海炮弹）

- 继承 `AbstractHurtingProjectile` 或自定义 `Entity`
- **抛物线弹道**：根据与目标距离线性插值抛高（minArcHeight ~ maxArcHeight）
- **提前量计算**：发射时读取目标速度向量，预测落点
- 命中后行为：爆炸（参考策划案HE机制），爆炸范围由config控制

#### 3.3.2 `AbyssalTrackingShellEntity`（深海追踪弹）

- 抛物线上升阶段同普通弹
- 经过最高点后启用**比例导航制导**（Proportional Navigation）
- 设置最大过载（转弯率上限），使弹道看起来自然
- 追踪目标为发射者的当前 target

#### 3.3.3 弹道参数表（初始测试值）

| 参数 | 驱逐 | 轻巡 | 重巡 | 战巡 | 战列 |
|------|------|------|------|------|------|
| 炮弹伤害 | 4 | 6 | 10 | 14 | 20 |
| 装填间隔(tick) | 60 | 50 | 80 | 100 | 120 |
| 连射数 | 2 | 3 | 2 | 3 | 4 |
| minArcHeight | 2 | 3 | 4 | 5 | 6 |
| maxArcHeight | 8 | 12 | 16 | 20 | 24 |
| 追踪弹最小间隔 | 4 | 3 | 3 | 2 | 2 |
| 追踪弹最大间隔 | 8 | 6 | 6 | 5 | 4 |
| 环绕半径 | 16 | 20 | 24 | 28 | 32 |
| 攻击射程 | 24 | 28 | 32 | 40 | 48 |

### 3.4 集群警戒AI（`ClusterAlertGoal`）

策划案 §7.1：

- 同一集群的深海共享 `clusterUUID`（生成时由结构processor写入或由spawner统一分配）
- 任一单位 `setTarget(player)` 时，遍历附近同 `clusterUUID` 的实体，若处于待机则也将该玩家设为 target
- 已有 target 的单位不受影响（不切换目标）

### 3.5 航母类NPC（简化版）

- `AbyssalLightCarrierEntity` / `AbyssalCarrierEntity` 复用现有 `AircraftEntity` 系统
- 每隔一定 tick 生成 `AircraftEntity`（标记 owner 为 carrier 实体），设置 `attackTarget` 为玩家
- 飞机AI复用已有的战斗机/攻击机行为（v0.0.4/v0.0.7已实现）
- 航母自身仅有较弱近距离自卫炮

### 3.6 潜艇NPC

- 水下时恒定隐身（同策划案潜艇核心逻辑）
- 周期性发射鱼雷实体（复用现有 `TorpedoEntity`）
- 被声呐效果发现后失去隐身

### 3.7 模型与渲染

- 每种深海NPC需要独立的模型和渲染器
- **本Phase先用占位模型**（基于 `LowTierDestroyerEntity` 的模型+不同颜色/缩放），后续版本替换为正式美术
- 每种NPC注册独立 Renderer

### 产出文件
```
src/.../entity/abyssal/
├── AbstractAbyssalEntity.java           # 基类
├── AbyssalSupplyEntity.java
├── AbyssalDestroyerEntity.java          # 替代/升级现有 LowTierDestroyerEntity
├── AbyssalLightCruiserEntity.java
├── AbyssalHeavyCruiserEntity.java
├── AbyssalBattlecruiserEntity.java
├── AbyssalBattleshipEntity.java
├── AbyssalLightCarrierEntity.java
├── AbyssalCarrierEntity.java
├── AbyssalSubmarineEntity.java
├── AbyssalShellEntity.java              # 深海炮弹（抛物线）
├── AbyssalTrackingShellEntity.java      # 深海追踪弹
└── goal/
    ├── OrbitTargetGoal.java             # 环绕AI
    ├── AbyssalShootGoal.java            # 炮击AI（含提前量）
    └── ClusterAlertGoal.java            # 集群共享警戒

src/.../client/
├── abyssal/
│   ├── AbyssalEntityModel.java          # 共用占位模型（参数化缩放/颜色）
│   ├── AbyssalDestroyerRenderer.java
│   ├── AbyssalLightCruiserRenderer.java
│   ├── ... (每种一个Renderer)
│   └── AbyssalShellRenderer.java

ModEntityTypes.java                       # +11实体注册
ClientEvents.java                         # +11渲染器注册
```

---

## Step 4 — 结构敌人生成器

**目的**：遗迹生成后，在标记点刷出对应类型和数量的深海NPC。

### 4.1 `AbyssalSpawnerBlock`

- 新功能方块，放置在结构模板的敌人刷出标记点
- 类似原版 spawner 但仅在结构初次加载时触发一次
- 通过 `BlockEntity` 的 NBT 指定：
  - `entityType`：要生成的深海实体类型
  - `count`：生成数量（1-4）
  - `clusterUUID`：集群ID（同一建筑内共享）
  - `patrolRadius`：巡逻半径
- 生成后自身变为空气（一次性触发器）

### 4.2 各建筑敌人配置

| 建筑 | 敌人种类 | 总数量（约） |
|------|---------|-------------|
| 传送门遗迹 | 无 | 0 |
| 补给站 | 补给酱×2, 驱逐×2, 轻巡×1, 重巡×1 | 6 |
| 前哨站 | 驱逐×4, 轻巡×3, 重巡×2, 战巡×1, 轻母×1 | 11 |
| 深海基地 | 驱逐×4, 轻巡×3, 重巡×3, 战巡×2, 战列×1, 轻母×1, 航母×1, 潜艇×2 | 17 |

> 以上为单个建筑实例的初始值，通过 spawner NBT 在模板中配置，可逐个调整。

### 产出文件
```
src/.../block/AbyssalSpawnerBlock.java
src/.../block/entity/AbyssalSpawnerBlockEntity.java
ModBlocks.java                            # +1
ModBlockEntityTypes.java                  # +1
```

---

## Step 5 — 战利品系统

**目的**：四类遗迹包含不同等级的战利品箱。

### 5.1 战利品表

路径：`data/piranport/loot_table/chests/`

| 战利品表 | 内容概要 |
|---------|---------|
| `portal_ruin.json` | 基础材料(铁/铝/火药), 少量深海传送门框架, 低级工具 |
| `supply_depot.json` | 小型通用装备(高概率), 中型装备(低概率), 少量国旗, 部分家具蓝图, 油弹钢铝资源, **出击渠(小概率)** |
| `outpost.json` | 小/中/大型装备, 小/中型特殊装备, 国旗, 经验炮弹, 舰载机, 深海传送门激活材料(部分) |
| `abyssal_base.json` | 全等级通用+特殊装备, 国旗, 一式穿甲弹, 经验炮弹, 舰载机, 深海作战档案, 无序意志碎片, 传送门激活材料 |

### 5.2 出击渠物品

- 注册 `ModItems.SORTIE_CHANNEL`（出击渠物品），放置后变为 `DungeonLecternBlock`（复用已有副本入口讲台）
- 或注册为独立方块 `SortieChannelBlock`，与 `DungeonLecternBlock` 共享同一个 Menu
- **推荐**：直接复用 `DungeonLecternBlock`，出击渠物品就是其 BlockItem，无需新方块

### 5.3 暂缺物品的占位处理

策划案中提到的一些尚未实现的物品（深海作战档案、无序意志碎片、国旗等），本Phase先注册为**简单占位物品**（`Item` 无特殊逻辑，仅有名称/贴图/tooltip），后续版本赋予功能。

需要新注册的占位物品：

| 物品 | 注册ID | 说明 |
|------|--------|------|
| 深海作战档案 | `abyssal_report` | 剧情道具，暂无功能 |
| 无序意志碎片α~ι | `chaos_shard_alpha` ~ `chaos_shard_iota` | 9种，传送门激活材料 |
| 传送门激活核心 | `portal_activation_core` | 传送门激活材料，暂无功能 |
| 各国国旗 | `flag_j` / `flag_e` / `flag_u` / `flag_g` / `flag_f` / `flag_i` / `flag_c` | 7种国旗，皮肤合成材料 |
| 经验炮弹 | `exp_shell` | 强化装备，暂以placeholder注册 |

### 产出文件
```
data/piranport/loot_table/chests/
├── portal_ruin.json
├── supply_depot.json
├── outpost.json
└── abyssal_base.json

src/.../item/PlaceholderItem.java          # 通用占位物品类（带tooltip"功能开发中"）
ModItems.java                              # +18左右新物品注册
```

---

## Step 6 — 结构箱子处理器

**目的**：结构加载时自动将标记位置的箱子设置正确的战利品表。

### 6.1 `LootChestProcessor` (StructureProcessor)

- 继承 `StructureProcessor`
- 检测模板中的箱子方块（`Blocks.CHEST`）
- 根据结构类型标签（通过 processor list 参数传入）设置 `LootTable` 引用
- 注册 codec 到 `BuiltInRegistries.STRUCTURE_PROCESSOR`

### 6.2 处理流程

1. 模板中放置普通箱子
2. Processor List JSON 中引用 `piranport:loot_chest_processor`，携带参数 `loot_table: "piranport:chests/supply_depot"`
3. 结构生成时 processor 自动将箱子的 `BlockEntity` 数据中注入 `LootTable`

### 产出文件
```
src/.../worldgen/LootChestProcessor.java
data/piranport/worldgen/processor_list/portal_ruin_processors.json
data/piranport/worldgen/processor_list/supply_depot_processors.json
data/piranport/worldgen/processor_list/outpost_processors.json
data/piranport/worldgen/processor_list/abyssal_base_processors.json
```

---

## Step 7 — Config 与调试命令

### 7.1 新增 Config

在 `ModCommonConfig` 中新增：

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `ruinGenerationEnabled` | boolean | `true` | 主世界遗迹生成总开关 |
| `portalRuinSpacing` | int | 40 | 传送门遗迹间距 |
| `supplyDepotSpacing` | int | 32 | 补给站间距 |
| `outpostSpacing` | int | 48 | 前哨站间距 |
| `abyssalBaseSpacing` | int | 64 | 深海基地间距 |
| `abyssalEnemyDifficultyMultiplier` | double | 1.0 | 深海NPC属性倍率 |

> 注意：Structure Set 的 spacing/separation 是数据驱动的 JSON，不能直接被 config 覆盖。需要通过自定义 `StructurePlacement` 读取 config，或在 DataGen 时写入。**推荐本Phase先硬编码JSON值，config条目作为预留接口。**

### 7.2 调试命令

扩展 `PiranPortDebug`，新增：

- `/ppd spawn_ruin <type>` — 在玩家位置强制生成指定遗迹（测试用）
- `/ppd spawn_abyssal <entity_type> [count]` — 生成指定深海NPC
- `/ppd locate_ruin <type>` — 定位最近的指定遗迹（包装原版 `/locate`）

### 产出文件
```
ModCommonConfig.java                      # +6 config 条目
src/.../debug/PiranPortDebug.java         # +3 命令
```

---

## Step 8 — 翻译与手册

### 8.1 翻译 Key

在 `zh_cn.json` 和 `en_us.json` 中添加：
- 所有新方块、物品、实体的翻译
- 战利品箱tooltip
- 遗迹名称（`structure.piranport.portal_ruin` 等）

### 8.2 Patchouli 手册

在手册中新增"主世界遗迹"章节：
- 四种遗迹的简介、难度提示
- 深海NPC图鉴（附占位图）
- 战利品预览

### 产出文件
```
assets/piranport/lang/zh_cn.json          # 更新
assets/piranport/lang/en_us.json          # 更新
assets/piranport/patchouli_books/guidebook/zh_cn/entries/ruins/   # 新章节
```

---

## Step 9 — 代码审查清单

完成所有功能后，进行以下检查：

- [ ] 所有新 Entity 在 `ModEntityTypes` 正确注册，属性在 `CommonEvents` 中通过 `EntityAttributeCreationEvent` 注册
- [ ] 所有 Renderer 在 `ClientEvents` 的 `EntityRenderersEvent.RegisterRenderers` 中注册
- [ ] Structure JSON 中的 `namespace:path` 引用与实际文件路径一致
- [ ] Loot Table 中引用的物品 ID 全部存在
- [ ] 深海NPC的 `LivingEntity.createAttributes()` 被正确调用
- [ ] `AbyssalSpawnerBlock` 生成敌人后正确自毁，不留残余 BlockEntity
- [ ] 集群AI在单位全灭后不会导致空指针
- [ ] 追踪弹的制导算法在目标死亡后不会无限追踪（需要超时销毁）
- [ ] 所有新物品出现在创造模式标签页中
- [ ] 新方块有正确的挖掘标签（`mineable/pickaxe` 等）
- [ ] 结构在超平坦世界不会生成（`biomes` 过滤正确）
- [ ] 联机测试：多人服务器下集群AI和战利品箱不重复触发

---

## 执行顺序建议

```
Step 2 (传送门框架方块)     ← 最简单，先热身
  ↓
Step 3.1-3.3 (基类+驱逐+轻巡+炮弹)  ← 核心战斗循环
  ↓
Step 3.4 (集群AI)
  ↓
Step 4 (Spawner方块)
  ↓
Step 1 (结构模板+世界生成)    ← 需要NBT模板，此时有敌人可验证
  ↓
Step 5 (战利品+占位物品)
  ↓
Step 6 (战利品处理器)
  ↓
Step 3.5-3.6 (航母NPC+潜艇NPC)  ← 较复杂，放后面
  ↓
Step 3.7 (占位模型+渲染器)
  ↓
Step 0 (新群系预留)
  ↓
Step 7 (Config+调试命令)
  ↓
Step 8 (翻译+手册)
  ↓
Step 9 (代码审查)
```

---

## 更新 CLAUDE.md 的内容

完成后在 Version Roadmap 表中追加：

```
| v0.0.11-alpha | Ruins | 🔧 WIP | 主世界四类遗迹(传送门/补给站/前哨站/深海基地) + 9种深海NPC + 集群AI + 抛物线/追踪弹道 + 战利品系统 + 占位物品(国旗/碎片/档案) + 调试命令 |
```

在 Project Structure 中追加：
```
├── entity/abyssal/                     # 深海NPC系统
│   ├── AbstractAbyssalEntity.java
│   ├── Abyssal*Entity.java (×9)
│   ├── AbyssalShellEntity.java
│   ├── AbyssalTrackingShellEntity.java
│   └── goal/ (×3 AI Goal)
├── block/
│   ├── PortalFrameBlock.java
│   └── AbyssalSpawnerBlock.java
├── worldgen/
│   ├── LootChestProcessor.java
│   └── AbyssalOceanBiomeModifier.java  # (预留)
```

在 Technical Reference 的关键技术要点中追加：

| 要点 | 说明 |
|------|------|
| 深海NPC基类 | `AbstractAbyssalEntity extends PathfinderMob`，含环绕/炮击/集群三大AI Goal |
| 抛物线弹道 | 根据距离线性插值抛高，发射时计算提前量 |
| 追踪弹制导 | 过最高点后启用比例导航，设最大过载限制转弯率 |
| 集群警戒 | 同 clusterUUID 实体共享首个目标，已交战者不切换 |
| 结构生成 | Jigsaw/SinglePool + StructureProcessor 注入战利品表和敌人 |
| 一次性Spawner | AbyssalSpawnerBlock 在结构首次加载时生成NPC后自毁 |

---

## 已知风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| NBT结构模板需要手动在游戏中搭建导出 | 阻塞Step 1 | 先用代码生成极简占位结构（如 `StructureTemplate` API 动态构建小型建筑），正式模板后续替换 |
| 抛物线+提前量弹道计算复杂 | Step 3 耗时长 | 先实现无提前量的简单抛物线，提前量作为二期优化 |
| 9种NPC × 渲染器 工作量大 | Step 3.7 耗时 | 共用参数化模型（缩放+颜色），每种NPC仅改参数 |
| Structure Set 的 spacing 不能动态受 config 控制 | Config条目形同虚设 | 文档说明需要修改JSON重新加载，config仅作参考值记录 |
| 航母NPC放飞飞机可能导致实体爆炸（大量飞机同时存在） | 性能 | 限制每个航母最大同时在空飞机数（如3架），超出则不再放飞 |
