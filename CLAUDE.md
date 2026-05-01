# CLAUDE.md — 皮兰港 1.20.1 Forge 迁移线

## 身份

- **源分支**：`../1.0/` （MC 1.21.1 + NeoForge 21.1.220，稳定 v1.0.0）
- **本分支**：MC 1.20.1 + Forge 47.3.0 迁移版本
- **mod_version**：`1.0.0-1.20.1forge`
- **Java**：17（1.0 是 21）
- **Gradle 插件**：ForgeGradle 6（1.0 是 ModDevGradle）
- **加载器包**：`net.minecraftforge.*`（1.0 是 `net.neoforged.*`）

## 当前状态

**阶段 4 完成**（`2026-04-27`，`./gradlew compileJava` 4s 通过）：

- BlockEntity：新增 `ReloadFacilityBlockEntity`（装填设施）、`PlaceableFoodBlockEntity`（可放置食物）、`ModelDebugBlockEntity`（模型调试）。
- Block：新增 `ReloadFacilityBlock`、`PlaceableFoodBlock`（Plate/Bowl/Cake 三个子类）、`ModelDebugBlock`。
- Menu：新增 `ReloadFacilityMenu`，使用 `IForgeMenuType.create()` 工厂方法（1.20.1 Forge API）。
- 1.20.1 API 适配：
  - BlockEntity 的 `saveAdditional/load` 方法签名从 1.21.1 的 `(CompoundTag, HolderLookup.Provider)` 降级为 1.20.1 的 `(CompoundTag)`。
  - `getUpdateTag` 方法从 1.21.1 的 `(HolderLookup.Provider)` 降级为 1.20.1 的无参数。
  - `FoodProperties` API：`nutrition()` → `getNutrition()`，`saturation()` → `getSaturationModifier()`，`effects()` → `getEffects()`，返回类型从 `List<PossibleEffect>` 变为 `List<Pair<MobEffectInstance, Float>>`。
  - `ResourceLocation` 构造：1.21.1 的 `ResourceLocation.parse()` 降级为 1.20.1 的 `new ResourceLocation(ns, path)`。
  - `ReloadFacilityBlock#use` 方法：1.21.1 的 `useWithoutItem` 降级为 1.20.1 的 `use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)`。
  - `NetworkHooks.openScreen()` 替代 1.21.1 的 `ServerPlayer#openMenu(MenuProvider, Consumer<FriendlyByteBuf>)`。
  - Capability 系统：使用 Forge 1.20.1 的 `LazyOptional<IItemHandler>` + `getCapability(Capability, Direction)` 替代 NeoForge 1.21.1 的直接方法调用。
- 占位 Item 类：新增 `TorpedoItem`、`TorpedoLauncherItem`、`MissileItem`、`MissileLauncherItem`（阶段 7 NBT 重写时回填完整逻辑）。
- 翻译：已添加 `reload_facility`、`model_debug`、`plate_food`、`bowl_food`、`cake_food` 及容器翻译。

**尚未迁移**：Entity + Network（阶段 5）；自定义配方类型（阶段 6）；component → NBT 重写（阶段 7）；Client 层（阶段 8）；Compat（阶段 9）；Worldgen/Config/Effect（阶段 10）。

## 关键 API 差异（与 1.0 对比，迁移时必须翻译）

| 维度 | 1.0 (NeoForge 1.21.1) | 本分支 (Forge 1.20.1) |
|---|---|---|
| 主类构造 | `public PiranPort(IEventBus, ModContainer)` | `public PiranPort()` + `FMLJavaModLoadingContext.get().getModEventBus()` |
| 事件总线 | 单一 `modEventBus` + `NeoForge.EVENT_BUS` | 双总线：`MOD_BUS`（注册） + `MinecraftForge.EVENT_BUS`（游戏事件） |
| 注册表 | `BuiltInRegistries.XXX` | `ForgeRegistries.XXX` |
| 资源 ID | `ResourceLocation.fromNamespaceAndPath/parse` | `new ResourceLocation(ns, path)` |
| 物品数据 | **DataComponents**（`ModDataComponents`） | **旧 NBT**（`CompoundTag` + `ItemStack#getOrCreateTag()`） |
| 网络包 | `StreamCodec` + `CustomPacketPayload` | `SimpleChannel` + 手写 `encode/decode(FriendlyByteBuf)` |
| 实体数据 | `AttachmentTypes` | `Capability<T>` |
| 配置 | `modContainer.registerConfig` | `ModLoadingContext.get().registerConfig` |
| 酿造事件 | `RegisterBrewingRecipesEvent` | `FMLCommonSetupEvent` 里 `BrewingRecipeRegistry.addRecipe` |
| Codec | `StreamCodec` | 老 `Codec` 组合子或手写 |
| 创造模式标签 | `Registries.CREATIVE_MODE_TAB` | 同（1.19.3+ 都走 DeferredRegister） |

## 1.0 有但本分支暂时砍掉的注册项

- `ModDataComponents` — 1.20.1 无此 API，物品数据改用 NBT
- `ModAttachmentTypes` — 1.20.1 无此 API，改用 Forge Capability
- `ModArmorMaterials` — 1.20.1 用 `ArmorMaterial` 枚举实现类，不经 Registry
- `ModBiomeModifiers` — 注册机制差异大，按需补

（对应 `PiranPort.java` 中已经删除这些引用。）

## 软依赖

1.0 的 Patchouli / JEI / Farmer's Delight 都要换 **1.20.1-Forge** 对应版本，`build.gradle` 的 `dependencies` 块暂未加入，等迁到具体功能时再开。

## 目录结构

```
1.20.1/
├── build.gradle              # ForgeGradle 6
├── gradle.properties         # MC 1.20.1 / Forge 47.3.0 / Java 17
├── settings.gradle
├── gradle/, gradlew, gradlew.bat  # 从 1.0 复制（Gradle 8.10）
└── src/main/
    ├── java/com/piranport/
    │   ├── PiranPort.java    # Forge @Mod 主类
    │   └── registry/         # 9 个空 DeferredRegister 骨架
    └── resources/
        ├── META-INF/mods.toml
        ├── assets/piranport/lang/{zh_cn,en_us}.json
        ├── logo.png
        ├── log4j2.xml
        └── pack.mcmeta        # pack_format=15
```

## 迁移分期

完整 10 阶段计划见 `../docs/皮兰港 1.20.1 Forge 迁移计划.md`。

- ✅ 阶段 0：空壳骨架编译通过（47s）
- ✅ 阶段 1：3 个基础方块 + BlockItem + `raw_aluminum`（compileJava 7s）
- ✅ 阶段 2：3 个剩余纯方块 + 119 个简单 Item 占位（compileJava 6s）
- ✅ 阶段 3：食物物品 + Recipe 骨架（compileJava 4s）
- ✅ 阶段 4：BlockEntity + Menu（GUI）（compileJava 4s）
- ⏳ 阶段 5：Entity + Network 数据包
- 待开工：阶段 6–10

## 占位贴图

阶段 2 有若干 item 原 1.0 仓库和 `美术素材/` 都没有贴图，先以占位方式注册，不打断编译链路：

- 所有 `*_torpedo_*` 命名变体 → 用同口径的 `torpedo_533mm.png` 或 `torpedo_610mm.png` 顶替
- `single_small_gun` → 复用 `small_gun.png`
- `pie_crust` → 复用 `flour.png`
- `small_vt_shell` / 部分香料 / 酵母 / 醋 / 生意面 / 发酵鱼 / 披萨饼底 / 切片香肠 / 圆面包 / 3 种皮肤核心 / 深水炸弹 / 标准声纳 / 高压锅炉 / 柴油机 / 鱼雷再装填 / 鱼叉导弹 / 小猎犬导弹 / 小猎犬发射器 / 舰载火箭弹发射器 / 燃料 / 浮动靶 / 航行手册 → 用 `raw_aluminum.png` 顶替

这批后续阶段补回美术后直接覆盖同名贴图即可，Java 层无需改动。

## 关键实现细节

- `FlareLightBlock` 的可替换性：1.20.1 `BlockBehaviour.Properties` 没有 `replaceable()`（1.20.5+ 才加），改为在 `FlareLightBlock` 覆写 `canBeReplaced`。
- `SmokeScreenBlock` / `FlareLightBlock` 无 BlockItem（运行时由物品生成，生存模式不可获取）。`ModBlocks` 提供 `register` / `registerNoItem` 两个辅助方法区分。

## 约束

- 不引入 Porting Lib 等兼容库。所有 API 差异手工翻译。
- 本目录不是 git 仓库，`feedback_auto_commit` 规则不适用。
