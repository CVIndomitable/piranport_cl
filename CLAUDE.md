# CLAUDE.md — 皮兰港 (Piran Port) Minecraft Mod

## Project Identity

- **Mod Name**: 皮兰港 / Piran Port
- **Mod ID**: `piranport`
- **Package**: `com.piranport`
- **License**: All Rights Reserved (同人项目)
- **Language**: Java 21
- **Platform**: NeoForge 21.11.x for Minecraft 1.21.11
- **Gradle Plugin**: ModDevGradle (推荐) 或 NeoGradle
- **MDK 模板**: https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle

## What This Mod Is

基于战舰少女R世界观的 Minecraft 综合性模组。玩家可以扮演舰娘角色，装备舰载武器，与深海敌人战斗。本文档定义 **MVP（最小可玩版本）**，代号 **v0.1.0-alpha**。

---

## MVP Scope (v0.1.0-alpha)

**只做以下内容，其余一律不做：**

### Phase 1: 基础注册 (Items, Blocks, Creative Tab)
1. 矿物：矾土矿 (Bauxite Ore) → 铝锭 (Aluminum Ingot) → 铝块 (Aluminum Block)
2. 矿物：盐块 (Salt Block) → 盐 (Salt)
3. 舰装核心物品：小型/中型/大型 三种 (SmallShipCore, MediumShipCore, LargeShipCore)
4. 基础弹药物品：HE炮弹 (HE Shell)、AP炮弹 (AP Shell)，分小/中/大口径
5. 基础火炮物品：3种口径火炮 (SmallGun, MediumGun, LargeGun)
6. Creative Tab: `piranport_tab`

### Phase 2: 世界生成 (World Gen)
1. 矾土矿脉：高度 Y=-16 ~ 63，最大矿脉 9，替代 stone，全生态群系
2. 盐块：河流群系河床，替代 dirt，盘状半径 1-3

### Phase 3: 合成与冶炼 (Recipes)
1. 矾土 → 铝锭（熔炉）
2. 铝锭 ↔ 铝块（9:1）
3. 盐块 ↔ 盐（9:1）
4. 舰装核心合成（工作台：铁锭 + 铝锭）
5. HE/AP炮弹合成（工作台：铁锭 + 火药 + 红石）
6. 火炮合成（工作台：铁锭 + 铝锭 + 红石）

### Phase 4: 舰装核心机制 (Data Components + Capability)
1. 舰装核心作为容器物品，内部存储武器栏（通过 DataComponents）
2. 右键打开舰装核心 GUI（Container + Screen）
3. GUI 包含：武器栏 (4-6 slots)、弹药库 (4 slots)、负重显示
4. 负重计算：每件装备有固定负重值，总负重不超过核心上限

### Phase 5: 火炮战斗 (Entity + Combat)
1. 自定义投射物实体：CannonProjectileEntity（炮弹）
2. 炮弹沿抛物线飞行，受重力影响
3. HE 命中爆炸（小范围，可配置是否破坏方块）
4. AP 命中造成直接伤害，穿甲
5. 变身状态（简化版）：手持舰装核心按 Shift+右键 进入/退出
6. 变身后：从舰装核心读取武器栏，允许切换使用火炮

---

## NOT In MVP (明确排除)

- ❌ 鱼雷、航空、导弹、反潜系统
- ❌ 皮肤/模型渲染系统
- ❌ NPC (深海、舰娘)
- ❌ 海图副本、深海世界维度
- ❌ 食物/Buff 系统
- ❌ 装饰方块、功能方块（武器合成台等）
- ❌ 树木、作物
- ❌ 剧情系统

---

## Project Structure

```
src/main/java/com/piranport/
├── PiranPort.java                  # @Mod 主类
├── registry/
│   ├── ModItems.java               # DeferredRegister.Items
│   ├── ModBlocks.java              # DeferredRegister.Blocks
│   ├── ModCreativeTabs.java        # Creative Tab
│   ├── ModEntityTypes.java         # 投射物实体注册
│   └── ModDataComponents.java      # 自定义 DataComponent 类型
├── item/
│   ├── ShipCoreItem.java           # 舰装核心物品（容器）
│   ├── CannonItem.java             # 火炮物品
│   └── ShellItem.java              # 炮弹物品
├── block/
│   ├── BauxiteOreBlock.java        # 矾土矿
│   └── SaltBlock.java              # 盐块
├── entity/
│   └── CannonProjectileEntity.java # 炮弹投射物
├── menu/
│   ├── ShipCoreMenu.java           # 舰装栏 Container
│   └── ShipCoreScreen.java         # 舰装栏 GUI (client)
├── worldgen/
│   └── ModOrePlacement.java        # 矿物生成配置
├── combat/
│   └── TransformationManager.java  # 变身状态管理
├── data/                           # DataGen
│   ├── ModBlockStateProvider.java
│   ├── ModItemModelProvider.java
│   ├── ModRecipeProvider.java
│   ├── ModLootTableProvider.java
│   └── ModWorldGenProvider.java
└── network/
    └── ModPackets.java             # 自定义网络包（变身同步等）

src/main/resources/
├── META-INF/
│   └── neoforge.mods.toml
├── assets/piranport/
│   ├── textures/
│   │   ├── item/                   # 物品贴图 16x16 PNG
│   │   └── block/                  # 方块贴图 16x16 PNG
│   ├── models/
│   │   ├── item/                   # 物品模型 JSON
│   │   └── block/                  # 方块模型 JSON
│   ├── blockstates/                # 方块状态 JSON
│   └── lang/
│       ├── zh_cn.json              # 简体中文
│       └── en_us.json              # English
└── data/piranport/
    ├── recipe/                     # 合成表 JSON
    ├── loot_table/blocks/          # 方块掉落
    └── worldgen/                   # 世界生成 JSON
        ├── configured_feature/
        └── placed_feature/
```

---

## Technical Reference

### NeoForge 1.21.11 核心 API 用法

#### 注册系统 — 使用 DeferredRegister

```java
// ModItems.java
public class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(PiranPort.MOD_ID);

    public static final DeferredItem<Item> ALUMINUM_INGOT =
        ITEMS.registerSimpleItem("aluminum_ingot");

    public static final DeferredItem<Item> SALT =
        ITEMS.registerSimpleItem("salt");

    public static final DeferredItem<ShipCoreItem> SMALL_SHIP_CORE =
        ITEMS.register("small_ship_core",
            () -> new ShipCoreItem(new Item.Properties().stacksTo(1),
                ShipCoreItem.ShipType.SMALL));
}

// ModBlocks.java
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(PiranPort.MOD_ID);

    public static final DeferredBlock<Block> BAUXITE_ORE =
        BLOCKS.register("bauxite_ore",
            () -> new DropExperienceBlock(
                UniformInt.of(1, 3),
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()));
}
```

#### 主类结构

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

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 非注册类初始化
    }
}
```

#### 投射物实体

```java
public class CannonProjectileEntity extends ThrowableProjectile {
    // 关键：重写 onHitEntity / onHitBlock
    // HE: 在 onHitBlock/onHitEntity 中调用 level.explode(...)
    // AP: 在 onHitEntity 中 entity.hurt(damageSource, damage)
    // 重力通过 getGravity() 返回 0.05f 左右
    // 用 EntityType.Builder.of(...).sized(0.25f, 0.25f).build() 注册
}
```

#### 自定义 DataComponent（1.21.x 取代旧 NBT/Capability）

```java
// 舰装核心内部的武器存储，用 DataComponent 而非旧版 NBT
public record ShipCoreContents(
    List<ItemStack> weapons,
    List<ItemStack> ammo,
    int currentLoad
) {
    public static final Codec<ShipCoreContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        ItemStack.CODEC.listOf().fieldOf("weapons").forGetter(ShipCoreContents::weapons),
        ItemStack.CODEC.listOf().fieldOf("ammo").forGetter(ShipCoreContents::ammo),
        Codec.INT.fieldOf("current_load").forGetter(ShipCoreContents::currentLoad)
    ).apply(i, ShipCoreContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipCoreContents> STREAM_CODEC = ...;
}

// 在 ModDataComponents.java 注册
public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShipCoreContents>>
    SHIP_CORE_CONTENTS = DATA_COMPONENTS.register("ship_core_contents",
        () -> DataComponentType.<ShipCoreContents>builder()
            .persistent(ShipCoreContents.CODEC)
            .networkSynchronized(ShipCoreContents.STREAM_CODEC)
            .build());
```

#### 世界生成 (Data-Driven)

世界生成通过 JSON datagen 或手写 JSON，放在 `data/piranport/worldgen/`：

```
configured_feature/bauxite_ore.json  → OreFeature, target: stone → bauxite_ore
placed_feature/bauxite_ore.json      → HeightRangePlacement(-16, 63), CountPlacement(8), BiomeFilter
configured_feature/salt_block.json   → DiskFeature, target: dirt → salt_block
placed_feature/salt_block.json       → river biome only
```

用 DataGen 的 `BiomeModifiers` 注入原版生态群系。

#### 网络同步（变身状态）

```java
// 使用 NeoForge 的 payload 系统
public record TransformPacket(boolean transformed) implements CustomPacketPayload {
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "transform");
    // 实现 write / 静态 read / id 方法
}

// 在 RegisterPayloadHandlersEvent 中注册
event.registrar(PiranPort.MOD_ID)
    .playToServer(TransformPacket.TYPE, TransformPacket.CODEC, handler);
```

---

## Key Design Decisions

### 负重系统数值表

| 核心类型 | 血量加成 | 负重上限 | 武器栏数 | 弹药库 |
|---------|---------|---------|---------|-------|
| 小型    | +0      | 40      | 4       | 4     |
| 中型    | +5      | 64      | 5       | 4     |
| 大型    | +10     | 112     | 6       | 4     |

### 火炮数值表 (MVP)

| 火炮    | 口径     | 伤害  | 装填(tick) | 负重 |
|--------|---------|------|-----------|------|
| 小型火炮 | 3-6 in  | 6    | 30        | 6    |
| 中型火炮 | 7-13 in | 12   | 50        | 16   |
| 大型火炮 | 14-21in | 20   | 80        | 30   |

### 炮弹数值表 (MVP)

| 炮弹 | 效果 | 特性 |
|------|------|------|
| HE   | 爆炸伤害 | 命中时小范围爆炸 (power=1.5)，可伤害多目标 |
| AP   | 穿甲伤害 | 直击伤害 130% of HE，忽略部分护甲，无 AOE |

---

## Conventions & Rules

### 命名规范
- 类名: `PascalCase` — `ShipCoreItem`, `CannonProjectileEntity`
- 注册 ID: `snake_case` — `small_ship_core`, `bauxite_ore`
- 常量: `UPPER_SNAKE_CASE` — `SMALL_SHIP_CORE`, `BAUXITE_ORE`
- 包名: 全小写 — `com.piranport.registry`
- 翻译 key: `item.piranport.small_ship_core`, `block.piranport.bauxite_ore`

### 代码规范
- 所有注册走 `DeferredRegister`，禁止直接 `Registry.register()`
- 所有物品数据存储用 `DataComponents`，不用旧版 NBT 的 `CompoundTag`
- Client-only 代码放在 `@Mod.EventBusSubscriber(value = Dist.CLIENT)` 中
- 所有硬编码数值提取为常量或 config
- 每个系统一个包，不要把所有东西塞在一个类里

### 资源文件规范
- 贴图：16x16 PNG，风格统一（像素风，配色参考战舰少女R）
- 模型 JSON：先用基础 `item/generated` 和 `block/cube_all`
- 语言文件：同时维护 `zh_cn.json` 和 `en_us.json`
- 用 DataGen 生成所有能生成的 JSON（模型、方块状态、合成表、Loot Table、世界生成）

### 测试规范
- 每完成一个 Phase 必须能 `gradlew runClient` 启动并验证
- 矿物生成：进创造模式 /locate 或 spectator 飞行确认
- 火炮：确认投射物生成、飞行轨迹、爆炸/伤害
- GUI：确认打开/关闭、物品放入取出、负重计算

---

## Build & Run

```bash
# 首次设置
./gradlew wrapper --gradle-version=8.10
./gradlew build

# 运行客户端测试
./gradlew runClient

# 运行 DataGen
./gradlew runData

# 构建发布 JAR
./gradlew build
# 输出: build/libs/piranport-0.1.0-alpha.jar
```

### gradle.properties 关键配置

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.1.0-alpha
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.11
neo_version=21.11.0-beta
```

---

## Phase 实施顺序

严格按以下顺序，**每个 Phase 完成后必须可运行验证**：

1. **Phase 1** → 注册所有物品和方块，Creative Tab 可见，能拿到所有物品
2. **Phase 2** → 新建世界能找到矾土矿和盐块
3. **Phase 3** → 能在工作台/熔炉合成所有物品
4. **Phase 4** → 舰装核心右键打开 GUI，能放入武器和弹药，负重显示正确
5. **Phase 5** → 能变身、切换火炮、发射炮弹、造成伤害

**不要跳步。不要提前做后续 Phase 的内容。**

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- NeoForge 21.11 Release Notes: https://neoforged.net/news/21.11release/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle
- Minecraft Wiki (Technical): https://minecraft.wiki/
- 原始策划案：见项目根目录 `docs/总策划案.docx`
