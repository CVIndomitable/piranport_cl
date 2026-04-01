# 已完成 Phase 归档

本文档归档 v0.0.1~v0.0.7 所有已完成 Phase 的详细记录，从 CLAUDE.md 迁移。

---

## v0.0.1-alpha — MVP (Phase 1-5)

- 矿物注册（矾土矿→铝锭、盐块→盐）、舰装核心×3、炮弹（HE/AP）×6、火炮×3
- 世界生成（矾土矿脉、河床盐块）
- 合成与冶炼配方
- 舰装核心 GUI（DataComponents + Container，武器栏+弹药库+负重）
- 火炮战斗（CannonProjectileEntity，HE爆炸/AP穿甲，Shift+右键变身）

---

## v0.0.2-alpha — Torpedo (Phase 6-10)

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

---

## v0.0.3-alpha — Kitchen (Phase 11-17)

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
放置触发：手持有 `PlaceableInfo` DataComponent 的食物，Shift+右键地面。
每口饱食度：`ceil(总饱食度 / 总次数)`。Buff 时长：`ceil(总Buff时长 / totalServings)`。碗装最后一口掉落碗。

#### 配方系统（Data-Driven）

| 加工站 | RecipeType ID | 输入 | 输出 | 额外参数 |
|--------|--------------|------|------|---------|
| 厨锅 | `piranport:cooking_pot` | 1-9 个物品 | 1 个 ItemStack + 数量 | `cookingTime`(tick) |
| 石磨 | `piranport:stone_mill` | 1-4 个物品 | 1-2 个 ItemStack | 无（瞬时） |
| 砧板 | `piranport:cutting_board` | 1 个物品 | 1 个 ItemStack + 数量 | `cuts`（默认4） |

#### 酿造

复用原版酿造台，通过 `RegisterBrewingRecipesEvent` 注册。

#### 食物物品注册管道

所有食物共用 `ModFoodItem extends Item`，构造时传入 `FoodProperties` + `PlaceableInfo`。

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

### Phase 12: 石磨方块

有 GUI 功能方块，瞬时研磨。完整方块+`FACING`，4输入+2输出，漏斗兼容。
配方：小麦→面粉、米→米粉、辣椒+盐→辣椒粉、生猪排→猪肉糜、桃子→桃核、桃核→杏仁粉。

### Phase 13: 砧板方块

无 GUI 交互方块。扁平碰撞箱(16×2×16)+`FACING`，存储物品+进度。
交互：手持原料右键放入→空手右键切(进度+1)→达标后产物弹出→Shift+右键取回。
配方：香肠→切片香肠×4、熟猪排→培根×4、面包→吐司面包片×3、萨拉米比萨→切片×8。

### Phase 14: 厨锅方块（核心）

有 GUI 核心烹饪方块。非完整方块+`FACING`，9输入+1产物，热源检测。
热源：FIRE/SOUL_FIRE/CAMPFIRE(lit)/SOUL_CAMPFIRE(lit)/MAGMA_BLOCK/LAVA/燃烧中熔炉。
配方：吐司面包(200t)、海军烘豆子(200t)、辣条(100t)、麻婆豆腐(300t)、海军咖喱(300t)、司康饼(200t)、炸鱼薯条(200t)、咸蛋拌豆腐(100t)、甜豆花(200t)。

### Phase 15: 酿造扩展

| 基底 | 试剂 | 产物 |
|------|------|------|
| 水瓶 | 面粉/米粉/糖 | 酵母瓶 |
| 水瓶 | 盐 | 盐水 |
| 盐水 | 大豆 | 味噌 |
| 酵母瓶 | 大豆 | 酱油 |
| 酵母瓶 | 米 | 醋 |
| 酵母瓶 | 米 | 料酒 |
| 酵母瓶 | 小麦 | 啤酒 |

### Phase 16: 可放置食物方块 + 代表性食物注册

代表性食物 ~15 个（详见 `ModItems.java`）。
所有食物 `alwaysEdible=true`。

### Phase 17: 端到端验证

全部验证通过。

---

## v0.0.4-alpha — Aviation (Phase 18-24)

**目标：实现航空战斗系统，包含4种飞机编队、编组GUI、多目标火控、4种攻击AI，以及 Patchouli 教程手册。**

### 架构决策

#### 飞机定位
飞机 = 武器栏物品，手持右键放飞，返航自动回武器栏。每架飞机为"编队"——单实体，伤害 = 面板伤害 × 编队系数。

#### 编组GUI
从舰装栏GUI中的"编组"按钮打开。固定4组。弹种是每架飞机单独配置。编组GUI完全取代弹药库顺序（航空线）。

#### 燃料系统
变身时自动从弹药库扣航空燃料填满飞机。有燃料飞机挂FlammableEffect。

#### 火控系统
P锁定/O加选/I取消。FOCUS全组打一个目标，SPREAD各自打最近。

#### AircraftEntity
继承 Entity。状态机：LAUNCHING → CRUISING → ATTACKING → RETURNING → REMOVED
四种攻击：FIGHTER(近战)、DIVE_BOMBER(俯冲)、TORPEDO_BOMBER(投鱼雷)、LEVEL_BOMBER(水平投弹)

#### 防御机制
滞空硬上限10分钟、距离>48格TP回、卡死60tick检测、右键回收、死亡/下线回收。

### Phase 18: 飞机物品 + 弹药物品 + 编组GUI框架
4种飞机 + 4种弹药 + AircraftInfo/FlightGroupData DataComponent + FlightGroupMenu/Screen

### Phase 19: AircraftEntity + 飞行物理 + 防御机制 + 浮动靶子

### Phase 20: 火控系统 P/O/I + HUD + 网络同步

### Phase 21: 攻击AI（四种模式）+ AerialBombEntity

### Phase 22: 燃料系统 + FlammableEffect + 放飞弹药消耗

### Phase 23: Patchouli 教程手册

### Phase 24: 端到端验证
修复 returnItemToOwner 只检查主手的bug。FlightGroupData结构变更为每架飞机独立弹种。

---

## v0.0.5-alpha — Combat+ (Phase 25-28)

### Phase 25: 装填进度物品装饰器 (`ReloadBarDecorator`) + 客户端 Config
### Phase 26: 自定义战斗 Buff（ReloadBoostEffect / EvasionEffect + EvasionHandler）
### Phase 27: Buff 食物 + 菠萝作物（BottleFoodItem、4种食物、8条配方JSON）
### Phase 28: 端到端验证

---

## v0.0.7-alpha — Aviation+ (Phase 29-37)

### Phase 29: 厨锅自动化（IItemHandler capability，顶/侧面输入、底面输出）
### Phase 30: 砧板自动化（IItemHandler 单槽 + 发射器切割）
### Phase 31: 石磨 AE2 兼容性测试（代码无改动，测试通过）
### Phase 32: 侦察机（RECON_ACTIVE 状态机、视角切换、WASD 操控、区块加载）
### Phase 33: 空战系统（aircraft health/hurt、战斗机目标优先级扩展）
### Phase 34: 编队跟随侦察机（FOLLOW 模式、飞机状态字幕、命中/击杀通知）
### Phase 35: 端到端验证（修复 FOLLOW 模式 ATTACKING 过渡 bug）
### Phase 36: 美术素材补全（60 张贴图、飞机双态贴图、火控HUD无GUI模式修复）

**Phase 36 变更明细：**

| 变更 | 内容 |
|------|------|
| `textures/block/` | bauxite_ore、salt_block、菠萝/番茄/生菜/大豆/水稻作物各阶段（21张） |
| `textures/item/` | 食物17种、鱼雷+发射管、炮弹6种、火炮、装甲3种、铝锭、航空燃料、飞机双态×5、食材4种、种子果实8种（39张） |
| 飞机双态贴图 | 无燃料显示 `xxx_empty.png`，有燃料显示 `xxx.png`；通过 `piranport:fueled` ItemProperty + model overrides 实现 |
| 暂缺贴图 | 葱/蒜/辣椒（作物+物品）、ship core 3种、中/大型火炮独立图、pasta/培根等中间食材约20种 |

### Phase 37: 盐获取系统重构（条件性世界生成 + 水桶烧炼 + 熔炉蒸发水）

- `saltGenerationEnabled` 配置开关（默认关闭）
- `SaltGenBiomeModifier` 自定义 BiomeModifier
- 水桶熔炉烧炼 200 tick → 盐 + 空桶
- `SaltEvaporationHandler`：熔炉上方水 → 200tick蒸发 → 水源→盐块，流动水→盐片
- `SaltChipBlock`：盐片薄层方块（地毯形，1像素高）
