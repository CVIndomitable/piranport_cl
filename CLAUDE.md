# CLAUDE.md — 皮兰港 (Piran Port) Minecraft Mod

## Project Identity

- **Mod Name**: 皮兰港 / Piran Port
- **Mod ID**: `piranport`
- **Package**: `com.piranport`
- **License**: All Rights Reserved (同人项目)
- **Language**: Java 21
- **Platform**: NeoForge 21.1.220 for Minecraft 1.21.1
- **Gradle Plugin**: ModDevGradle (推荐) 或 NeoGradle
- **MDK 模板**: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

## What This Mod Is

基于战舰少女R世界观的 Minecraft 综合性模组。玩家可以扮演舰娘角色，装备舰载武器，与深海敌人战斗。

---

## Version Roadmap

| 版本 | 代号 | 状态 | 核心内容 |
|------|------|------|---------|
| v0.1.0-alpha | MVP | ✅ DONE | 基础注册、世界生成、合成、舰装核心GUI、火炮战斗 |
| v0.2.0-alpha | Torpedo | ✅ DONE | 鱼雷系统、舰装栏GUI完善、负重平衡、装填机制 |
| v0.3.0-alpha | Kitchen | ✅ DONE | 食物烹饪 + Buff系统、作物种植、加工站 |
| v0.4.0-alpha | Aviation | 🔨 CURRENT | 航空系统（4种飞机+编组GUI+火控+AI）+ Patchouli手册 |
| v0.5.0-alpha | Deco | ⏳ PLANNED | 资源扩充、装饰方块、功能方块、灶、剩余食物批量注册 |
| v0.6.0-alpha | Skin | ⏳ PLANNED | 皮肤/模型渲染系统 |
| v0.7.0-alpha | Aviation+ | ⏳ PLANNED | 侦察机视角切换、空战、编队跟随 |

---

## ✅ v0.1.0-alpha — MVP (DONE)

<details>
<summary>已完成内容（折叠）</summary>

### Phase 1-5 概要
- 矿物注册（矾土矿→铝锭、盐块→盐）、舰装核心×3、炮弹（HE/AP）×6、火炮×3
- 世界生成（矾土矿脉、河床盐块）
- 合成与冶炼配方
- 舰装核心 GUI（DataComponents + Container，武器栏+弹药库+负重）
- 火炮战斗（CannonProjectileEntity，HE爆炸/AP穿甲，Shift+右键变身）

</details>

---

## ✅ v0.2.0-alpha — Torpedo (DONE)

<details>
<summary>已完成内容（折叠）</summary>

### Phase 6-10 概要
- 鱼雷物品（533mm/610mm弹药，双/三/四联发射器）+ TorpedoEntity
- 鱼雷水面贴合物理（空气+下方水→水平航行；水中→上浮；空中→下坠）
- 进水 debuff（FloodingEffect，每秒1点魔法伤害）
- 发射器扇形散布（2联±3°、3联±4°、4联±6°/±2°）
- 舰装栏强化栏（附加装甲+2/+4/+6）、变身锁定、负重→移速
- 装填进度 HUD（RenderGuiLayerEvent.Post）
- 数值平衡验证（Phase 10，全部通过）

**Phase 10 数值验证结果：**
- 场景1：小型核心+2小炮+双联鱼雷 = 20/40(50%)，移速70% ✅
- 场景2：大型核心+2大炮+四联鱼雷+大装甲 = 110/112(98%)，移速41% ✅
- 场景3：中型核心2把大炮=60/64，第3把被拒 ✅
- AP×1.3 vs HE爆炸场景区分明显 ✅

</details>

---

## ✅ v0.3.0-alpha — Kitchen (DONE)

<details>
<summary>已完成内容（折叠）</summary>

**目标：实现食物烹饪 + Buff 系统的基础设施，包含作物种植、加工站（石磨/砧板/厨锅）、酿造扩展、可放置食物方块框架，注册 ~15 个代表性食物验证全链路。**

**设计原则：基础设施优先，内容管道化。** 所有加工站、配方系统、可放置食物方块做成通用框架，使得后续添加一个新食物只需要：注册物品 + 写一条 JSON 配方 + 配置 Buff，不需要改 Java 代码。

### 架构决策

#### 可放置食物方块

不为每种食物注册一个 Block。用 3 种"容器形状"通用方块 + BlockEntity 存储食物类型和剩余次数：

| 注册 ID | 形状 | 碰撞箱 | 适用 |
|---------|------|--------|------|
| `plate_food` | 盘装 | 16×4×16 | 大部分食物 |
| `bowl_food` | 碗装 | 16×6×16 | 需要碗的食物 |
| `cake_food` | 蛋糕型 | 16×8×16 | 高体积食物 |

`PlaceableFoodBlockEntity` 存储：`foodItemId`（ResourceLocation）、`remainingServings`、`totalServings`。
`PlaceableFoodBlockEntityRenderer` 在方块上方渲染对应食物物品模型。
放置触发：手持有 `PlaceableInfo` DataComponent 的食物，Shift+右键地面。
每口饱食度：`ceil(总饱食度 / 总次数)`。Buff 时长：`ceil(总Buff时长 / (总次数-1))`。碗装最后一口掉落碗。

#### 配方系统（Data-Driven）

| 加工站 | RecipeType ID | 输入 | 输出 | 额外参数 |
|--------|--------------|------|------|---------|
| 厨锅 | `piranport:cooking_pot` | 1-9 个物品 | 1 个 ItemStack + 数量 | `cookingTime`(tick) |
| 石磨 | `piranport:stone_mill` | 1-4 个物品 | 1-2 个 ItemStack | 无（瞬时） |
| 砧板 | `piranport:cutting_board` | 1 个物品 | 1 个 ItemStack + 数量 | `cuts`（默认4） |

配方全部由 DataGen 生成 JSON。

#### 酿造

复用原版酿造台，通过自定义 `IBrewingRecipe` 实现在 `FMLCommonSetupEvent` 中注册。

#### 食物物品注册管道

所有食物共用 `ModFoodItem extends Item`，构造时传入 `FoodProperties` + `PlaceableInfo`：

```java
public static final DeferredItem<Item> TOAST_BREAD = register("toast_bread",
    foodProps(15, 18.8f).build(),
    placeableAs(PLATE, 3)  // 盘装，3次食用
);
```

---

### Phase 11: 食材/调料物品 + 新作物种植

#### 11a. 食材/调料物品

| 注册 ID | 中文名 | 获取方式 |
|---------|--------|---------|
| `flour` | 面粉 | 石磨（小麦） |
| `rice_flour` | 米粉 | 石磨（米） |
| `chili_powder` | 辣椒粉 | 石磨（辣椒+盐） |
| `pork_paste` | 猪肉糜 | 石磨（生猪排） |
| `edible_oil` | 食用油 | 工作台（大豆/生鱼） |
| `butter` | 黄油 | 工作台（食用油+盐） |
| `cream` | 奶油 | 工作台（鸡蛋+牛奶+糖） |
| `soybean_milk` | 豆浆 | 工作台（大豆+玻璃瓶） |
| `tofu` | 豆腐 | 工作台（豆浆+生石灰） |
| `cheese` | 奶酪 | 工作台（奶油+酵母瓶） |
| `yeast` | 酵母瓶 | 酿造（面粉/米粉/糖+水瓶） |
| `soy_sauce` | 酱油 | 酿造（大豆+酵母瓶） |
| `vinegar` | 醋 | 酿造（米+酵母瓶） |
| `cooking_wine` | 料酒 | 酿造（糯米+酵母瓶） |
| `miso` | 味噌 | 酿造（大豆+盐水） |
| `brine` | 盐水 | 酿造（盐+水瓶） |
| `pie_crust` | 馅饼酥皮 | 工作台（面粉×2+食用油+牛奶） |
| `raw_pasta` | 生意面 | 工作台（面粉×2+水+盐） |
| `fermented_fish` | 发酵鱼 | 工作台（生鱼+酵母瓶） |
| `pizza_base` | 披萨饼底 | 工作台（面粉×3+水桶） |
| `gypsum_chip` | 石膏碎片 | 工作台（石头/闪长岩） |
| `quicklime` | 生石灰 | 熔炉（骨头） |

#### 11b. 新作物

| 注册 ID | 中文名 | 类型 | 产物 |
|---------|--------|------|------|
| `tomato` | 番茄 | CropBlock（4阶段） | 番茄×1-3 |
| `soybean` | 大豆 | CropBlock（4阶段） | 大豆×1-3 |
| `chili` | 辣椒 | CropBlock（4阶段） | 辣椒×1-2 |
| `lettuce` | 生菜 | CropBlock（3阶段） | 生菜×1-2 |
| `rice` | 稻/米 | 水生作物（类似甘蔗） | 米×1-3 |
| `onion` | 洋葱 | CropBlock（4阶段） | 洋葱×1-2 |
| `garlic` | 大蒜 | CropBlock（3阶段） | 大蒜×1-2 |

种子来源：破草掉落 + 流浪商人 + 箱子战利品。

**验证：** Creative Tab 出现所有物品，作物可种植生长收获。

---

### Phase 12: 石磨方块

有 GUI 功能方块，瞬时研磨。完整方块+`FACING`，4输入+2输出，漏斗兼容。

**配方（`piranport:stone_mill`）：** 小麦→面粉、米→米粉、辣椒+盐→辣椒粉、生猪排→猪肉糜、桃子→桃核、桃核→杏仁粉。

**验证：** 放入小麦→输出面粉，漏斗可输入/抽取。

---

### Phase 13: 砧板方块

无 GUI 交互方块。扁平碰撞箱(16×2×16)+`FACING`，存储物品+进度。

**交互：** 手持原料右键放入→空手右键切(进度+1)→达标后产物弹出→Shift+右键取回。

**配方（`piranport:cutting_board`）：** 香肠→切片香肠×4、熟猪排→培根×4、面包→吐司面包片×3、萨拉米比萨→切片×8。

**渲染：** `CuttingBoardBlockEntityRenderer` 在砧板上渲染当前物品。

**验证：** 放面包→右键4次→吐司面包片弹出。

---

### Phase 14: 厨锅方块（核心）

有 GUI 核心烹饪方块。非完整方块+`FACING`，9输入+1产物，热源检测。

**热源：** FIRE/SOUL_FIRE/CAMPFIRE(lit)/SOUL_CAMPFIRE(lit)/MAGMA_BLOCK/LAVA/燃烧中熔炉。

**逻辑：** 每tick匹配配方→计时→完成消耗输入产出结果。无热源不推进。v0.3.0产物手动取。

**配方（`piranport:cooking_pot`）：** 吐司面包(200t)、海军烘豆子(200t)、辣条(100t)、麻婆豆腐(300t)、海军咖喱(300t)、司康饼(200t)、炸鱼薯条(200t)、咸蛋拌豆腐(100t)、甜豆花(200t)。

**验证：** 锅+营火→放材料→进度推进→产出。无热源不推进。

---

### Phase 15: 酿造扩展

复用原版酿造台，`FMLCommonSetupEvent` 中注册 `IBrewingRecipe`。

| 基底 | 试剂 | 产物 |
|------|------|------|
| 水瓶 | 面粉/米粉/糖 | 酵母瓶 |
| 水瓶 | 盐 | 盐水 |
| 盐水 | 大豆 | 味噌 |
| 酵母瓶 | 大豆 | 酱油 |
| 酵母瓶 | 米 | 醋 |
| 酵母瓶 | 米 | 料酒 |
| 酵母瓶 | 小麦 | 啤酒 |

**验证：** 酿造台水瓶×3+面粉→酵母瓶×3。

---

### Phase 16: 可放置食物方块 + 代表性食物注册

`PlaceableFoodBlock`(3实例) + `PlaceableFoodBlockEntity` + Renderer + `PlaceableInfo` DataComponent。

**代表性食物 ~15 个：**

| 注册 ID | 中文名 | 饱食度 | 饱和度 | Buff | 容器 | 次数 |
|---------|--------|--------|--------|------|------|------|
| `toast_bread` | 吐司面包 | 15 | 18.8 | — | plate | 3 |
| `naval_baked_beans` | 海军烘豆子 | 4 | 5 | — | plate | 2 |
| `latiao` | 辣条 | 2 | 2.5 | 迅捷III 90s | plate | 2 |
| `mapo_tofu` | 麻婆豆腐 | 4 | 5 | 力量II+抗火I 180s | plate | 3 |
| `naval_curry` | 海军咖喱 | 5 | 6.3 | 夜视I 240s | plate | 3 |
| `fried_fish_and_chips` | 炸鱼薯条 | 5 | 6.3 | 跳跃提升II 180s | plate | 2 |
| `scone` | 司康饼 | 3 | 3.8 | — | plate | 4 |
| `salted_egg_tofu` | 咸蛋拌豆腐 | 3 | 3.8 | — | plate | 1 |
| `surstromming` | 鲱鱼罐头 | 4 | 5 | 凋零II 2s+反胃IV 14s+力量II 240s | plate | 2 |
| `american_burger` | 美式汉堡 | 8 | 10 | 急迫II 180s | plate | 2 |
| `hotdog` | 热狗 | 4 | 5 | 急迫I 180s | — | 1 |
| `pasta` | 意面 | 4 | 5 | — | plate | 2 |
| `cooked_rice` | 米饭 | 5 | 6.3 | — | plate | 2 |
| `beet_blossom` | 甜豆花 | 3 | 3.8 | 力量I+水下呼吸I 180s | bowl | 1 |
| `miso_soup` | 味噌汤 | 6 | 7.5 | — | bowl | 2 |

所有食物 `alwaysEdible=true`（无视饱食度上限）。同时注册中间产物：切片香肠、培根、圆面包等。

**验证：** 制作→手持食用+Buff→Shift+右键放置→分次食用→碗装掉碗。

---

### Phase 17: 端到端验证

不新增自定义 MobEffect。全部用原版效果。

**测试场景：** ①面粉→吐司→放置3次食用 ②大豆全链路→麻婆豆腐+Buff ③酿造链路 ④砧板链路

**验证清单（构建通过，待 runClient 确认）：**
- [x] 7种作物种植/生长/收获（Phase 11）
- [x] 石磨6配方+漏斗（Phase 12）
- [x] 砧板4配方+进度渲染（Phase 13）
- [x] 厨锅9配方+热源检测（Phase 14）
- [x] 酿造7配方（Phase 15，RegisterBrewingRecipesEvent API）
- [x] 食物Buff正确（Phase 16，FoodProperties）
- [x] 食物方块放置/食用/碗掉落（Phase 16）
- [x] zh_cn + en_us 翻译（Phase 17）
- [x] Creative Tab 分类（Phase 17）

</details>

---

## NOT In v0.3.0 (明确排除)

- ❌ 灶方块（留 v0.5.0）
- ❌ 夕张的水桶（留 v0.5.0）
- ❌ 厨锅自动弹出/侧面容器输出
- ❌ 锅内物品/流体/碗架渲染
- ❌ 食物方块自定义3D模型
- ❌ 食物方块右键食材变换
- ❌ 剩余50+种食物注册
- ❌ 自定义战斗Buff（高速规避/经验提升/装填加速）
- ❌ 喂狗/流浪商人交易

---

## 🔨 v0.4.0-alpha — Aviation (CURRENT)

**目标：实现航空战斗系统，包含4种飞机编队、编组GUI、多目标火控、4种攻击AI，以及 Patchouli 教程手册。**

**设计原则：编队单实体化，编组GUI集中管理，四重防御保安全。**

### 架构决策

#### 飞机定位

飞机 = 武器栏物品，手持右键放飞，返航自动回武器栏。与火炮、鱼雷发射器并列，计算负重。每架飞机为"编队"——单实体，模型和名称显示为飞行编队，伤害 = 面板伤害 × 编队系数。

#### 编组GUI

从舰装栏GUI中的"编组"按钮打开。变身前后均可编辑。固定4组（正航可用4组，轻航可用3组，由舰装核心决定）。

| 编辑项 | 说明 |
|--------|------|
| 成员 | 从武器栏中的飞机拖入/拖出（引用，不移动物品） |
| 弹药指定 | 为本组指定弹种，放飞时从弹药库扣对应弹药，**未指定则不可放飞** |
| 出击顺序 | 组序号即默认顺序（1→2→3→4），可调整 |
| 攻击模式 | FOCUS（全组打同一个火控目标）/ SPREAD（各自打最近目标） |

飞行中的飞机在编组GUI中变灰不可编辑，返航后恢复。

编组数据存储在舰装核心的 DataComponent `FlightGroupData` 中：

```java
// FlightGroupData — 存储在舰装核心上
groups: [
  {
    slotIndices: [0, 2],      // 武器栏中的飞机槽位索引
    ammoType: "piranport:aerial_torpedo",
    attackMode: FOCUS,         // FOCUS | SPREAD
    sortOrder: 1
  },
  // ... 共4组
]
```

**编组GUI完全取代弹药库顺序（航空线）。** 炮弹和鱼雷仍走弹药库顺序。

#### 燃料系统

| 环节 | 行为 |
|------|------|
| 变身时 | 自动从弹药库扣航空燃料，填满所有飞机的 `currentFuel`。弹药库无燃料则该飞机不补 |
| 易燃易爆 | 变身状态 + 有 `currentFuel > 0` 的飞机 → 挂 `FlammableEffect`（受伤加成 + 概率爆炸额外伤害） |
| 放飞检查 | `currentFuel == 0` → 拒绝放飞，HUD提示"燃料不足" |
| 返航后 | `currentFuel` 清零 |
| 解除变身 | 不清空燃料，但移除易燃易爆debuff |

#### 放飞操作

| 操作 | 行为 |
|------|------|
| 右键 | 按出击顺序放飞下一个未出击编组 |
| 数字键(1-4) + 右键 | 放飞指定编组 |

放飞时从弹药库扣编组指定弹药。弹药不足则拒绝。飞机实体在玩家前方0.5格生成。

#### 火控系统

| 按键 | 功能 |
|------|------|
| P | 准星指向生物时按下 → 锁定/切换目标 |
| O | 加选目标（追加到锁定列表） |
| I | 取消所有锁定 |

服务端维护 `lockedTargets: List<UUID>`，通过 CustomPacketPayload 同步。

- FOCUS 编组：全组攻击锁定列表第一个目标
- SPREAD 编组：各自攻击距自身最近的锁定目标；无锁定时自动索敌32格内敌对生物

#### AircraftEntity

继承 Entity（不继承 Mob）。状态机：

```
LAUNCHING → CRUISING → ATTACKING → RETURNING → REMOVED
```

**四种攻击行为：**

| 类型 | AI行为 | 伤害方式 |
|------|--------|---------|
| FIGHTER | 接近目标后近战咬尾，每秒面板伤害 | 直接近战伤害 |
| DIVE_BOMBER | 爬升→俯冲接触，类似幻翼 | 面板伤害 + 50%概率着火30s |
| TORPEDO_BOMBER | 贴水面飞行→距目标10-16格投鱼雷 | 复用TorpedoEntity |
| LEVEL_BOMBER | 爬升至目标上方32格→水平飞越投弹 | AerialBombEntity（自由落体+HE爆炸） |

**DataComponent `AircraftInfo`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `aircraftType` | enum | FIGHTER / DIVE_BOMBER / TORPEDO_BOMBER / LEVEL_BOMBER |
| `fuelCapacity` | int | 燃料容量 |
| `ammoCapacity` | int | 弹药容量 |
| `currentFuel` | int | 当前燃料值（0=未整备） |
| `panelDamage` | float | 面板伤害 |
| `panelSpeed` | float | 面板航速 |
| `weight` | int | 负重 |

#### 防御机制

| 机制 | 触发条件 | 行为 |
|------|---------|------|
| 滞空硬上限 | 10分钟（12000 tick） | 强制 RETURNING，超时则 REMOVED + 回武器栏 |
| 距离上限 | 与玩家 > 48格 | TP回玩家附近5格，切为 RETURNING |
| 卡死检测 | 连续60tick位置变化 < 0.1格 | 强制 REMOVED + 回武器栏 |
| 空手右键 | 玩家右键自己的飞机 | 立即 REMOVED + 回武器栏 |
| 玩家死亡 | PlayerDeathEvent | 全部飞机 REMOVED + 回武器栏 |
| 玩家下线 | PlayerLoggedOutEvent | 同上 |

#### 浮动靶子（测试工具）

`FloatingTargetEntity`：继承盔甲架模型和盔甲槽逻辑，水面漂浮，可穿原版盔甲获得护甲效果。手持物品右键水面放置。

---

### ✅ Phase 18: 飞机物品 + 弹药物品 + 编组GUI框架 (DONE)

- 飞机物品×4：`fighter_squadron`、`dive_bomber_squadron`、`torpedo_bomber_squadron`、`level_bomber_squadron`
- 弹药物品：`aviation_fuel`（航空燃料）、`aerial_bomb_small`（小型航弹）、`aerial_bomb_medium`（中型航弹）、`aerial_torpedo`（航空鱼雷）
- DataComponent：`AircraftInfo`（7字段，手写StreamCodec）、`FlightGroupData`（核心上的4组配置）
- `FlightGroupMenu` + `FlightGroupScreen`，从舰装栏"编组"按钮发包→服务端openMenu打开
- 编组GUI功能：4组显示、点击分配/取消飞机槽位、弹药类型循环（---/燃料/小弹/中弹/鱼雷）、集火/分散切换
- 合成表（工作台，铝锭为主要材料）
- `ShipCoreMenu.isWeapon()` 和 `getWeight()` 已扩展支持 AircraftItem
- `isAmmo()` 已扩展支持航空弹药

**验证（构建通过，待 runClient 确认）：**
- [x] 4架飞机编队 + 4种航空弹药物品注册
- [x] Creative Tab 可见
- [x] 编组GUI可开（ShipCoreScreen "编组"按钮 → OpenFlightGroupPayload → 服务端openMenu）
- [x] 编组GUI分组功能（点击飞机槽位切换分配）
- [x] 指定弹药（点击弹药类型按钮循环）
- [x] 攻击模式切换（集火/分散）
- [x] FlightGroupUpdatePayload 保存到核心 DataComponent
- [x] 飞机放入武器栏负重正确（AircraftItem.weight 从 AircraftInfo 读取）
- [x] zh_cn + en_us 翻译

---

### Phase 19: AircraftEntity + 飞行物理 + 防御机制

- `AircraftEntity` 注册（继承 Entity）
- 状态机 LAUNCHING → CRUISING → ATTACKING → RETURNING → REMOVED
- 飞行物理：爬升、巡航、盘旋、返航
- 放飞逻辑：右键 / 数字键+右键 → 检查燃料弹药 → 生成实体 → 扣弹药
- 返航逻辑：飞向玩家 → 到达后消失 → 物品回武器栏
- 全部防御机制：滞空上限、距离TP、卡死检测、右键回收、死亡/下线回收
- `FloatingTargetEntity`（浮动靶子）：盔甲架模型 + 水面漂浮 + 可穿盔甲
- 占位渲染器

**验证：** 放飞→飞行→盘旋→返航，四重防御正常，死亡/下线回收正常。

---

### Phase 20: 火控系统

- KeyMapping 注册：P/O/I
- 锁定逻辑：准星射线检测生物 → 加入/切换/取消
- 锁定 HUD：目标显示菱形标记 + 血量/名称
- 网络同步：CustomPacketPayload 同步 lockedTargets
- AircraftEntity 读取所属玩家锁定列表确定目标

**验证：** P锁定、O加选、I取消，HUD显示标记，飞机飞向锁定目标。

---

### Phase 21: 攻击AI（四种模式）

- FIGHTER AI：接近→咬尾近战→持续攻击至目标死亡或燃料尽→返航
- DIVE_BOMBER AI：爬升→俯冲接触→伤害+50%着火→拉起返航
- TORPEDO_BOMBER AI：贴水面→距10-16格投鱼雷（复用TorpedoEntity）→返航
- LEVEL_BOMBER AI：爬升32格→计算提前量投弹→返航
- `AerialBombEntity`：自由落体 + 接触HE爆炸（复用火炮爆炸逻辑）
- 自动索敌：无锁定时32格内寻找敌对生物
- FOCUS/SPREAD 攻击模式逻辑

**验证：** 四种飞机各自攻击行为正确，鱼雷机复用鱼雷正常，集火/分散模式正确。

---

### Phase 22: 燃料系统 + 舰装栏集成 + 数值平衡

- 变身时自动补燃料（扣弹药库航空燃料 → 填飞机 currentFuel）
- `FlammableEffect`（易燃易爆 MobEffect）
- 舰装栏集成：飞机负重计算
- 编组弹药消耗：放飞时按编组弹种扣弹药库
- 3个配装场景数值验证

**数值验证场景：**
- 场景1：中型核心 + 2副炮 + 2战斗机 + 2鱼雷机 → 负重合理
- 场景2：大型核心 + 4组攻击机满载 → 负重接近上限
- 场景3：小型核心装飞机 → 负重超标或只能装1-2架

**验证：** 变身补燃料+debuff正确，无燃料拒绝放飞，负重正确，三场景通过。

---

### Phase 23: Patchouli 教程手册

- 引入 Patchouli 依赖（1.21.1-93-NEOFORGE）
- `piranport:guidebook` 物品，配方：书+铝锭
- 书籍内容（JSON data-driven）：入门/火炮/鱼雷/烹饪/航空 五章
- zh_cn + en_us 双语

**验证：** 合成书→右键打开→全部章节可浏览→合成配方页正确。

---

### Phase 24: 端到端验证

| # | 场景 | 验证点 |
|---|------|--------|
| 1 | 航空全链路 | 装飞机→编组→变身→补燃料→火控锁定→放飞→攻击→返航→回武器栏 |
| 2 | 四种攻击 | 四种飞机各自执行正确攻击模式 |
| 3 | 编组操作 | 4组不同弹种+攻击模式，顺序/指定放飞 |
| 4 | 防御机制 | 滞空超时/超距TP/卡死/手动回收/死亡/下线 |
| 5 | 火控多目标 | P→O加选→集火打A、分散打最近 |
| 6 | 教程手册 | 合成→翻阅→配方展示 |
| 7 | 浮动靶子 | 水面放置→穿铁甲→四种飞机打靶→护甲减伤 |

---

## NOT In v0.4.0 (明确排除)

- ❌ 侦察机（视角切换+假人+区块加载）→ 留 v0.7.0 Aviation+
- ❌ 飞机间空战（战斗机攻击敌方飞机）
- ❌ 编队跟随侦察机
- ❌ 放飞动画（依赖皮肤系统）
- ❌ 飞机自定义3D模型 → 先用占位模型
- ❌ 弹种切换冷却
- ❌ 飞机GUI、民用机型（F4U冰激凌等）
- ❌ 校射联动、编队国籍限制
- ❌ 武器合成台/弹药合成台 → v0.4.0 用工作台合成
- ❌ 飞机残骸回收系统

---

## Project Structure (v0.4.0 更新)

```
src/main/java/com/piranport/
├── PiranPort.java
├── registry/
│   ├── ModItems.java               # + 飞机编队、航空弹药、教程书、浮动靶子
│   ├── ModBlocks.java
│   ├── ModCreativeTabs.java
│   ├── ModEntityTypes.java         # + AircraftEntity, AerialBombEntity, FloatingTargetEntity
│   ├── ModDataComponents.java      # + AircraftInfo, AircraftAmmoInfo, FlightGroupData
│   ├── ModMobEffects.java          # + FlammableEffect
│   ├── ModBlockEntityTypes.java
│   ├── ModMenuTypes.java           # + FlightGroupMenu
│   ├── ModRecipeTypes.java
│   └── ModKeyMappings.java         # 🆕 P/O/I 火控按键
├── item/
│   ├── ShipCoreItem.java
│   ├── CannonItem.java / ShellItem.java
│   ├── TorpedoItem.java / TorpedoLauncherItem.java
│   ├── ArmorPlateItem.java
│   ├── ModFoodItem.java
│   ├── AircraftItem.java           # 🆕 飞机编队物品
│   └── GuidebookItem.java          # 🆕 Patchouli教程书
├── block/ (同v0.3.0)
├── block/entity/ (同v0.3.0)
├── entity/
│   ├── CannonProjectileEntity.java
│   ├── TorpedoEntity.java
│   ├── AircraftEntity.java         # 🆕 飞机实体（状态机+四种攻击AI）
│   ├── AerialBombEntity.java       # 🆕 航空炸弹投射物
│   └── FloatingTargetEntity.java   # 🆕 浮动靶子（测试工具）
├── effect/
│   ├── FloodingEffect.java
│   └── FlammableEffect.java        # 🆕 易燃易爆
├── menu/
│   ├── ShipCoreMenu.java / ShipCoreScreen.java
│   ├── CookingPotMenu.java / CookingPotScreen.java
│   ├── StoneMillMenu.java / StoneMillScreen.java
│   └── FlightGroupMenu.java / FlightGroupScreen.java  # 🆕
├── component/
│   ├── PlaceableInfo.java
│   ├── AircraftInfo.java            # 🆕
│   ├── AircraftAmmoInfo.java        # 🆕
│   └── FlightGroupData.java         # 🆕
├── aviation/                        # 🆕 航空系统包
│   ├── AircraftAI.java              # 状态机+四种攻击行为
│   └── FireControlManager.java      # 火控锁定管理
├── client/
│   ├── ShipCoreHudLayer.java / TorpedoRenderer.java
│   ├── CuttingBoardRenderer.java / PlaceableFoodRenderer.java
│   ├── AircraftRenderer.java       # 🆕
│   ├── AerialBombRenderer.java     # 🆕
│   └── FireControlHudLayer.java    # 🆕 锁定标记HUD
├── recipe/ (同v0.3.0)
├── worldgen/ / combat/ / data/ / network/
│   └── (network 增加火控同步包)
```

---

## Technical Reference

### NeoForge 21.1.220 API 坑（已踩过，勿重蹈）

| 问题 | 错误用法 | 正确用法 |
|------|---------|---------|
| 酿造配方注册 | `BrewingRecipeRegistry.addRecipe(recipe)` (静态方法已废) | 监听 `RegisterBrewingRecipesEvent`，用 `event.getBuilder().addRecipe(recipe)` |
| 酿造事件总线 | `modEventBus.addListener(RegisterBrewingRecipesEvent)` (IllegalArgumentException) | `NeoForge.EVENT_BUS.addListener(...)` — 此事件在游戏总线，不在 mod 总线 |
| 食物饱和度获取 | `food.saturationModifier()` | `food.saturation()` |
| 熔炉热源检测 | `AbstractFurnaceBlockEntity.isLit()` (private) | `bs.hasProperty(BlockStateProperties.LIT) && bs.getValue(...LIT) && bs.getBlock() instanceof AbstractFurnaceBlock` |
| 方块掉落物品 | `Containers.dropContents(level, pos, blockEntity)` (需实现 Container) | 手动 loop `itemHandler.getStackInSlot(i)` + `Containers.dropItemStack()` |
| PlaceableFoodBlock codec | 单个基类无法用 `simpleCodec(Base::new)` | 用3个静态内部类 (Plate/Bowl/Cake)，各自实现 `simpleCodec(ClassName::new)` |
| StreamCodec 超过6字段 | `StreamCodec.composite(...)` 最多支持6个字段 | 改用 `StreamCodec.of(encoder, decoder)` 手写编解码逻辑 |
| ContainerMenu 无槽位 | AbstractContainerMenu 可以0个容器槽（如编组GUI），player inv槽放 x=-2000 隐藏 | GUI交互通过 C2S payload，不依赖 slot 机制 |
| 从Screen开另一个Menu | 客户端Screen无法直接调 openMenu，需发 C2S 包 → 服务端调 serverPlayer.openMenu | `OpenFlightGroupPayload` 模式 |

### v0.4.0 外部依赖

**Patchouli 1.21.1-93-NEOFORGE**（教程手册）：
- Maven: `vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE`
- 书籍内容放在 `src/main/resources/data/piranport/patchouli_books/guidebook/`
- 纯 JSON data-driven，不需要写 Java 代码
- 书籍物品通过 Patchouli API 注册

### v0.4.0 技术要点

| 要点 | 说明 |
|------|------|
| AircraftEntity 不继承 Mob | 继承 Entity，自己管理状态机和移动，不用 Mob 的 AI 系统 |
| 火控同步 | 客户端 KeyMapping → C2S Packet → 服务端更新 lockedTargets → S2C 广播给附近玩家 |
| 编组数据位置 | 存在舰装核心的 DataComponent 上，不在飞机物品上 |
| 浮动靶子 | 继承/参考 ArmorStand，覆写物理使其水面漂浮 |
| AerialBombEntity | 类似 CannonProjectileEntity，但无初速度水平分量，纯自由落体 |

---

### 注册系统

```java
public class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(PiranPort.MOD_ID);
    // v0.3.0 食材
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    // v0.3.0 食物
    public static final DeferredItem<Item> TOAST_BREAD = ITEMS.register("toast_bread",
        () -> new ModFoodItem(new Item.Properties()
            .food(new FoodProperties.Builder()
                .nutrition(15).saturationModifier(18.8f / (15 * 2))
                .alwaysEdible().build())
            .component(ModDataComponents.PLACEABLE_INFO.get(),
                new PlaceableInfo("plate", 3))));
}
```

### 主类结构

```java
@Mod(PiranPort.MOD_ID)
public class PiranPort {
    public static final String MOD_ID = "piranport";
    public PiranPort(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);  // v0.3.0
        ModMenuTypes.MENU_TYPES.register(modEventBus);                 // v0.3.0 + FlightGroupMenu
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);             // v0.3.0
        modEventBus.addListener(this::registerBrewingRecipes);
    }
    // ⚠️ NeoForge 21.1.220: 不用 FMLCommonSetupEvent，改用 RegisterBrewingRecipesEvent
    private void registerBrewingRecipes(final RegisterBrewingRecipesEvent event) {
        ModBrewingRecipes.register(event.getBuilder());
    }
}

// v0.4.0 客户端: 注册 KeyMapping（P/O/I 火控按键）
@Mod.EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.FIRE_CONTROL_LOCK);   // P
        event.register(ModKeyMappings.FIRE_CONTROL_ADD);    // O
        event.register(ModKeyMappings.FIRE_CONTROL_CANCEL); // I
    }
}
```

### BlockEntity/Menu/RecipeType 注册 (v0.3.0)

```java
public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PiranPort.MOD_ID);
    public static final Supplier<BlockEntityType<CookingPotBlockEntity>> COOKING_POT =
        BLOCK_ENTITY_TYPES.register("cooking_pot",
            () -> BlockEntityType.Builder.of(CookingPotBlockEntity::new,
                ModBlocks.COOKING_POT.get()).build(null));
    // STONE_MILL, CUTTING_BOARD, PLACEABLE_FOOD 同理
}

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, PiranPort.MOD_ID);
    public static final Supplier<MenuType<CookingPotMenu>> COOKING_POT =
        MENU_TYPES.register("cooking_pot",
            () -> IMenuTypeExtension.create(CookingPotMenu::new));
    // STONE_MILL 同理
}

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, PiranPort.MOD_ID);
    public static final Supplier<RecipeType<CookingPotRecipe>> COOKING_POT =
        RECIPE_TYPES.register("cooking_pot",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(
                PiranPort.MOD_ID, "cooking_pot")));
    // STONE_MILL, CUTTING_BOARD 同理
}
```

### CookingPotBlockEntity tick 逻辑

```java
public static void serverTick(Level level, BlockPos pos,
        BlockState state, CookingPotBlockEntity be) {
    if (!be.hasHeatSource(level, pos)) {
        if (be.cookingProgress > 0) { be.cookingProgress = 0; be.setChanged(); }
        return;
    }
    CookingPotRecipe recipe = be.findMatchingRecipe(level);
    if (recipe == null) { be.cookingProgress = 0; be.currentRecipe = null; return; }
    if (be.currentRecipe != recipe) {
        be.currentRecipe = recipe;
        be.cookingProgress = 0;
        be.cookingTotalTime = recipe.getCookingTime();
    }
    be.cookingProgress++;
    if (be.cookingProgress >= be.cookingTotalTime) {
        be.craftItem(recipe);
        be.cookingProgress = 0;
        be.currentRecipe = null;
    }
    be.setChanged();
}

private boolean hasHeatSource(Level level, BlockPos pos) {
    BlockPos below = pos.below();
    BlockState bs = level.getBlockState(below);
    if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) return true;
    if (bs.is(Blocks.MAGMA_BLOCK)) return true;
    if (bs.getFluidState().is(Fluids.LAVA)) return true;
    if (bs.is(Blocks.CAMPFIRE) && bs.getValue(CampfireBlock.LIT)) return true;
    if (bs.is(Blocks.SOUL_CAMPFIRE) && bs.getValue(CampfireBlock.LIT)) return true;
    if (level.getBlockEntity(below) instanceof AbstractFurnaceBlockEntity f) return f.isLit();
    return false;
}
```

### PlaceableFoodBlockEntity eat 逻辑

```java
public void eat(Player player) {
    if (remainingServings <= 0) return;
    Item foodItem = BuiltInRegistries.ITEM.get(foodItemId);
    FoodProperties food = foodItem.getDefaultInstance().get(DataComponents.FOOD);
    if (food == null) return;
    int nutritionPerBite = (int) Math.ceil((double) food.nutrition() / totalServings);
    float satPerBite = (float) Math.ceil(food.saturation() / totalServings * 10) / 10f;
    player.getFoodData().eat(nutritionPerBite, satPerBite);
    for (FoodProperties.PossibleEffect pe : food.effects()) {
        MobEffectInstance orig = pe.effect();
        int dur = totalServings > 1
            ? (int) Math.ceil((double) orig.getDuration() / (totalServings - 1))
            : orig.getDuration();
        if (pe.probability() >= player.getRandom().nextFloat())
            player.addEffect(new MobEffectInstance(orig.getEffect(), dur, orig.getAmplifier()));
    }
    remainingServings--;
    if (remainingServings <= 0) {
        if (level.getBlockState(worldPosition).is(ModBlocks.BOWL_FOOD.get()))
            Block.popResource(level, worldPosition, new ItemStack(Items.BOWL));
        level.removeBlock(worldPosition, false);
    }
    setChanged();
    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
}
```

---

## Conventions & Rules

### 命名规范
- 类名: `PascalCase` — `CookingPotBlock`, `PlaceableFoodBlockEntity`
- 注册 ID: `snake_case` — `cooking_pot`, `toast_bread`
- 常量: `UPPER_SNAKE_CASE`
- 包名: 全小写 — `com.piranport.recipe`
- 翻译 key: `block.piranport.cooking_pot`, `item.piranport.toast_bread`

### 代码规范
- 所有注册走 `DeferredRegister`
- 物品数据用 `DataComponents`，不用旧版 NBT
- Client-only 放 `@Mod.EventBusSubscriber(value = Dist.CLIENT)`
- 硬编码数值提取为常量或 config
- 每系统一个包
- 新 BlockEntity 同时注册渲染器（如需要）和 MenuType

### 资源文件规范
- 贴图 16x16 PNG，像素风
- 先用 `item/generated` 和 `block/cube_all`
- 同时维护 `zh_cn.json` 和 `en_us.json`
- DataGen 生成所有能生成的 JSON

### 测试规范
- 每 Phase 完成后 `gradlew runClient` 验证
- 见 Phase 24 验证清单

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-0.0.4.jar
```

> **自动构建 Hook**：每次 Claude Code 会话结束时自动执行 `./gradlew build` 并将 JAR 复制到 Minecraft mods 目录（配置在 `.claude/settings.local.json` Stop hook）。

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.0.4
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.1
neo_version=21.1.220
```

---

## Phase 实施顺序 (v0.4.0)

1. ~~**Phase 18**~~ ✅ DONE — 飞机/弹药物品 + DataComponent + 编组GUI
2. **Phase 19** → AircraftEntity + 飞行物理 + 防御机制 + 浮动靶子
3. **Phase 20** → 火控系统 P/O/I + HUD + 网络同步
4. **Phase 21** → 四种攻击AI + AerialBombEntity
5. **Phase 22** → 燃料系统 + 易燃易爆 + 舰装栏集成 + 数值验证
6. **Phase 23** → Patchouli 教程手册
7. **Phase 24** → 端到端验证

**不要跳步。不要提前做后续 Phase 的内容。**

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- NeoForge 21.1 Release Notes: https://neoforged.net/news/21.1release/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle
- Minecraft Wiki (Technical): https://minecraft.wiki/
- Patchouli Wiki: https://vazkiimods.github.io/Patchouli/
- 原始策划案：见项目根目录 `docs/总策划案.docx`
- v0.4.0 航空规划：见项目根目录 `docs/v0.4.0_Aviation_规划.md`
- 开发纪要：见项目根目录 `docs/皮兰港开发纪要260325上午.md`、`docs/皮兰港开发纪要260325下午.md`
- GitHub 远程仓库: https://github.com/CVIndomitable/piranport_cl.git
