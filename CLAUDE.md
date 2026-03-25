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
| v0.3.0-alpha | Kitchen | 🔨 CURRENT | 食物烹饪 + Buff系统、作物种植、加工站 |
| v0.4.0-alpha | Deco | ⏳ PLANNED | 资源扩充、装饰方块、功能方块 |
| v0.5.0-alpha | Skin | ⏳ PLANNED | 皮肤/模型渲染系统 |

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

## 🔨 v0.3.0-alpha — Kitchen (CURRENT)

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

**验证清单：**
- [ ] 7种作物种植/生长/收获
- [ ] 石磨6配方+漏斗
- [ ] 砧板4配方+进度渲染
- [ ] 厨锅9配方+热源检测
- [ ] 酿造7配方
- [ ] 食物Buff正确
- [ ] 食物方块放置/食用/碗掉落
- [ ] zh_cn + en_us 翻译
- [ ] Creative Tab 分类

---

## NOT In v0.3.0 (明确排除)

- ❌ 灶方块（留 v0.4.0）
- ❌ 夕张的水桶（留 v0.4.0）
- ❌ 厨锅自动弹出/侧面容器输出
- ❌ 锅内物品/流体/碗架渲染
- ❌ 食物方块自定义3D模型
- ❌ 食物方块右键食材变换
- ❌ 剩余50+种食物注册
- ❌ 自定义战斗Buff（高速规避/经验提升/装填加速）
- ❌ 喂狗/流浪商人交易

---

## Project Structure (v0.3.0 更新)

```
src/main/java/com/piranport/
├── PiranPort.java
├── registry/
│   ├── ModItems.java               # + 食材、调料、食物、种子
│   ├── ModBlocks.java              # + 厨锅、石磨、砧板、食物方块、作物
│   ├── ModCreativeTabs.java
│   ├── ModEntityTypes.java
│   ├── ModDataComponents.java      # + PlaceableInfo
│   ├── ModMobEffects.java
│   ├── ModBlockEntityTypes.java    # 🆕 方块实体类型注册
│   ├── ModMenuTypes.java           # 🆕 菜单类型注册
│   └── ModRecipeTypes.java         # 🆕 配方类型注册
├── item/
│   ├── ShipCoreItem.java
│   ├── CannonItem.java / ShellItem.java
│   ├── TorpedoItem.java / TorpedoLauncherItem.java
│   ├── ArmorPlateItem.java
│   └── ModFoodItem.java            # 🆕 通用食物物品
├── block/
│   ├── BauxiteOreBlock.java / SaltBlock.java
│   ├── CookingPotBlock.java        # 🆕
│   ├── StoneMillBlock.java         # 🆕
│   ├── CuttingBoardBlock.java      # 🆕
│   ├── PlaceableFoodBlock.java     # 🆕
│   └── crop/
│       ├── ModCropBlock.java       # 🆕
│       └── RiceCropBlock.java      # 🆕
├── block/entity/
│   ├── CookingPotBlockEntity.java  # 🆕
│   ├── StoneMillBlockEntity.java   # 🆕
│   ├── CuttingBoardBlockEntity.java# 🆕
│   └── PlaceableFoodBlockEntity.java # 🆕
├── entity/
│   ├── CannonProjectileEntity.java
│   └── TorpedoEntity.java
├── effect/
│   └── FloodingEffect.java
├── menu/
│   ├── ShipCoreMenu.java / ShipCoreScreen.java
│   ├── CookingPotMenu.java / CookingPotScreen.java   # 🆕
│   └── StoneMillMenu.java / StoneMillScreen.java      # 🆕
├── recipe/
│   ├── CookingPotRecipe.java       # 🆕
│   ├── StoneMillRecipe.java        # 🆕
│   └── CuttingBoardRecipe.java     # 🆕
├── component/
│   └── PlaceableInfo.java          # 🆕
├── client/
│   ├── ShipCoreHudLayer.java / TorpedoRenderer.java
│   ├── CuttingBoardRenderer.java   # 🆕
│   └── PlaceableFoodRenderer.java  # 🆕
├── worldgen/ / combat/ / data/ / network/
│   └── (同前，data 增加新配方和掉落表)
```

---

## Technical Reference

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
        ModMenuTypes.MENU_TYPES.register(modEventBus);                 // v0.3.0
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);             // v0.3.0
        modEventBus.addListener(this::commonSetup);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModBrewingRecipes.register());
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
- 见 Phase 17 验证清单

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-0.0.2.jar（v0.2完成），Phase 11起改为 0.0.3
```

> **自动构建 Hook**：每次 Claude Code 会话结束时自动执行 `./gradlew build` 并将 JAR 复制到 Minecraft mods 目录（配置在 `.claude/settings.local.json` Stop hook）。

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.0.3
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.1
neo_version=21.1.220
```

---

## Phase 实施顺序 (v0.3.0)

1. **Phase 11** → 食材/调料/种子注册，作物可种植生长收获
2. **Phase 12** → 石磨 GUI，小麦→面粉，漏斗兼容
3. **Phase 13** → 砧板交互，面包→切4次→吐司面包片
4. **Phase 14** → 厨锅热源检测+烹饪，产出食物
5. **Phase 15** → 酿造台产出酵母瓶、酱油等
6. **Phase 16** → 食物手持食用+放置+分次食用+碗掉落
7. **Phase 17** → 全链路测试通过

**不要跳步。不要提前做后续 Phase 的内容。**

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- NeoForge 21.1 Release Notes: https://neoforged.net/news/21.1release/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle
- Minecraft Wiki (Technical): https://minecraft.wiki/
- 原始策划案：见项目根目录 `docs/总策划案.docx`
- GitHub 远程仓库: https://github.com/CVIndomitable/piranport_cl.git
