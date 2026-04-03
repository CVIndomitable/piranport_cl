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
| v0.0.1-alpha | MVP | ✅ DONE | 基础注册、世界生成、合成、舰装核心GUI、火炮战斗 |
| v0.0.2-alpha | Torpedo | ✅ DONE | 鱼雷系统、舰装栏GUI完善、负重平衡、装填机制 |
| v0.0.3-alpha | Kitchen | ✅ DONE | 食物烹饪 + Buff系统、作物种植、加工站 |
| v0.0.4-alpha | Aviation | ✅ DONE | 航空系统（4种飞机+编组GUI+火控+AI）+ Patchouli手册 |
| v0.0.5-alpha | Combat+ | ✅ DONE | 装填进度Decorator、装填加速/高速规避Buff、Buff食物注册 |
| v0.0.6-alpha | Skin | ⏳ PLANNED | 皮肤/模型渲染系统 |
| v0.0.7-alpha | Aviation+ | ✅ DONE | 侦察机视角切换、空战、编队跟随侦察机、加工站自动化 |
| v0.0.8-alpha | Salt & Rice | ✅ DONE | 稻田种植、盐蒸发系统、条件性盐矿生成、全面代码审查修复(35项P0-P3) |
| v0.0.9-alpha | Dungeon | ✅ DONE | 副本系统（大厅/实例/节点战斗/传送门/箱子船/钥匙/回城卷轴）+ JEI兼容 + 武器冷却条 + 友军伤害防护 + 彩色描边 + 击落归因 |

> 已完成 Phase 详情见 `docs/phases_archive.md`
> 代码审查修复（35项）详情见 `docs/code_review_2026-03-31.md`

---

## Project Structure (v0.0.10)

```
src/main/java/com/piranport/
├── PiranPort.java
├── ClientEvents.java               # MOD总线客户端事件（屏幕/按键/渲染器/ItemProperties注册）
├── ClientGameEvents.java           # GAME总线客户端事件
├── ClientTickHandler.java          # 每tick按键检测（火控/侦察机/高亮/FC描边同步）
├── GameEvents.java                 # GAME总线服务端事件（变身/水上行走/飞机召回等）
├── CommonEvents.java               # 属性注册等通用事件
├── registry/
│   ├── ModItems.java               # 所有物品注册（食材/食物/武器/飞机/副本钥匙/回城卷轴）
│   ├── ModBlocks.java              # 含 DUNGEON_LECTERN
│   ├── ModCreativeTabs.java
│   ├── ModEntityTypes.java         # 含 DUNGEON_PORTAL, LOOT_SHIP, DUNGEON_TRANSPORT_PLANE
│   ├── ModDataComponents.java      # 含 DUNGEON_STAGE_ID, DUNGEON_INSTANCE_ID, DUNGEON_PROGRESS, WEAPON_COOLDOWN
│   ├── ModMobEffects.java          # FlammableEffect, ReloadBoostEffect, EvasionEffect, FloodingEffect
│   ├── ModBlockEntityTypes.java
│   ├── ModMenuTypes.java           # 含 DUNGEON_BOOK_MENU
│   ├── ModRecipeTypes.java
│   └── ModKeyMappings.java         # P/O/I 火控、U 编组、V 循环/退侦察、Y 高亮、F8 调试
├── config/
│   ├── ModCommonConfig.java
│   └── ModClientConfig.java        # 含 showLegacyReloadHud, flightGroupEnabled
├── item/
│   ├── ShipCoreItem.java           # GUI/无GUI双模式发射、右键召回飞机、无GUI背包扫描
│   ├── CannonItem.java
│   ├── TorpedoItem.java / TorpedoLauncherItem.java
│   ├── ArmorPlateItem.java
│   ├── ModFoodItem.java / BottleFoodItem.java
│   ├── AircraftItem.java           # 发射入口 + 手动加油（overrideOtherStackedOnMe）
│   ├── FloatingTargetItem.java
│   └── GuidebookItem.java
├── block/
│   ├── RiceCropBlock.java          # 水稻（3阶段waterlogged作物）
│   ├── SaltChipBlock.java          # 盐片薄层方块（蒸发产物）
│   ├── SaltEvaporationHandler.java # 熔炉上方水→盐蒸发逻辑
│   ├── CookingPotBlock.java / StoneMillBlock.java / CuttingBoardBlock.java
│   ├── PlaceableFoodBlock.java
│   └── FourStageCropBlock.java / ThreeStageCropBlock.java
├── block/entity/
├── entity/
│   ├── CannonProjectileEntity.java # 含友军伤害防护 + 击落归因
│   ├── TorpedoEntity.java          # 含友军伤害防护 + 击落归因
│   ├── AircraftEntity.java         # 状态机 + 五种飞机AI + isCurrentlyGlowing() + getTeamColor()
│   ├── AerialBombEntity.java       # 含友军伤害防护 + 击落归因
│   ├── BulletEntity.java           # 含友军伤害防护 + 击落归因
│   ├── FloatingTargetEntity.java
│   └── DungeonTransportPlaneEntity.java # 副本运输机（空投脚本用）
├── effect/
│   ├── FloodingEffect.java
│   ├── FlammableEffect.java
│   ├── ReloadBoostEffect.java
│   └── EvasionEffect.java
├── menu/
│   ├── ShipCoreMenu.java / ShipCoreScreen.java
│   ├── CookingPotMenu.java / CookingPotScreen.java
│   ├── StoneMillMenu.java / StoneMillScreen.java
│   └── FlightGroupMenu.java / FlightGroupScreen.java
├── component/
│   ├── PlaceableInfo.java
│   ├── AircraftInfo.java
│   ├── FlightGroupData.java        # 4编组 + slotAmmoTypes + attackMode
│   ├── LoadedAmmo.java
│   ├── SlotCooldowns.java
│   ├── WeaponCategory.java
│   └── WeaponCooldown.java         # endTick/totalTick/getFraction 冷却数据
├── aviation/
│   ├── FireControlManager.java     # 服务端锁定列表
│   ├── ClientFireControlData.java
│   ├── ClientReconData.java
│   └── ReconManager.java           # 服务端侦察机控制
├── combat/
│   ├── TransformationManager.java  # 变身属性、负重、武器槽、冷却
│   └── EvasionHandler.java
├── client/
│   ├── ShipCoreHudLayer.java
│   ├── FireControlHudLayer.java
│   ├── ReloadBarDecorator.java
│   ├── WeaponReloadDecorator.java  # IItemDecorator 武器冷却条（橙=火炮/蓝=鱼雷）
│   ├── AircraftRenderer.java
│   ├── CuttingBoardRenderer.java
│   └── PlaceableFoodRenderer.java
├── compat/
│   └── jei/
│       ├── PiranPortJEIPlugin.java      # JEI 插件入口
│       ├── CookingPotRecipeCategory.java
│       ├── StoneMillRecipeCategory.java
│       └── CuttingBoardRecipeCategory.java
├── dungeon/                        # 副本系统
│   ├── DungeonConstants.java
│   ├── block/
│   │   └── DungeonLecternBlock.java     # 副本入口讲台
│   ├── client/
│   │   ├── DungeonBookScreen.java       # 副本选关界面
│   │   ├── DungeonResultScreen.java     # 战斗结算
│   │   ├── DungeonReviveScreen.java     # 复活界面
│   │   ├── TownScrollScreen.java        # 回城卷轴确认
│   │   ├── DungeonHudLayer.java         # 副本HUD
│   │   ├── DungeonPortalRenderer.java
│   │   └── LootShipRenderer.java
│   ├── data/
│   │   ├── DungeonDataLoader.java       # JSON数据加载
│   │   ├── DungeonRegistry.java
│   │   ├── ChapterData.java / StageData.java / NodeData.java (含script字段) / EnemySetData.java
│   ├── entity/
│   │   ├── DungeonPortalEntity.java     # 副本传送门
│   │   └── LootShipEntity.java          # 箱子船（战利品）
│   ├── event/
│   │   └── DungeonEventHandler.java     # 死亡/登出/进度事件/脚本节点分发
│   ├── script/                          # 脚本化节点流程
│   │   ├── DungeonScript.java           # 脚本接口
│   │   ├── DungeonScriptManager.java    # 脚本管理器（ServerTick驱动）
│   │   └── ArtilleryIntroScript.java    # 炮击引入3阶段脚本
│   ├── instance/
│   │   ├── DungeonInstance.java
│   │   ├── DungeonInstanceManager.java
│   │   └── NodeBattleField.java         # 节点战斗场
│   ├── item/
│   │   └── TownScrollItem.java          # 回城卷轴
│   ├── key/
│   │   ├── DungeonKeyItem.java          # 副本钥匙
│   │   ├── DungeonProgress.java
│   │   └── FlagshipManager.java
│   ├── lobby/
│   │   └── DungeonLobbyManager.java     # 大厅管理
│   ├── menu/
│   │   └── DungeonBookMenu.java
│   ├── network/                         # 12个 C2S/S2C Payload
│   │   ├── ClientDungeonData.java
│   │   ├── DungeonStatePayload.java / DungeonResultPayload.java
│   │   ├── JoinLobbyPayload.java / LeaveLobbyPayload.java / LobbyUpdatePayload.java
│   │   ├── SelectStagePayload.java / SelectNodePayload.java / NodeEnteredPayload.java
│   │   ├── PlayerDiedInDungeonPayload.java / ReviveRequestPayload.java
│   │   └── TownScrollUsePayload.java
│   └── saved/
│       ├── DungeonSavedData.java
│       └── DungeonLeaderboard.java
├── debug/
│   └── PiranPortDebug.java
├── network/                        # 所有 CustomPacketPayload
├── recipe/
├── worldgen/
│   └── SaltGenBiomeModifier.java
└── registry/
    └── ModBiomeModifiers.java
```

---

## Technical Reference

### NeoForge 21.1.220 API 坑（已踩过，勿重蹈）

| 问题 | 错误用法 | 正确用法 |
|------|---------|---------|
| 酿造配方注册 | `BrewingRecipeRegistry.addRecipe(recipe)` (静态方法已废) | 监听 `RegisterBrewingRecipesEvent`，用 `event.getBuilder().addRecipe(recipe)` |
| 酿造事件总线 | `modEventBus.addListener(RegisterBrewingRecipesEvent)` | `NeoForge.EVENT_BUS.addListener(...)` — 此事件在游戏总线 |
| 食物饱和度获取 | `food.saturationModifier()` | `food.saturation()` |
| 熔炉热源检测 | `AbstractFurnaceBlockEntity.isLit()` (private) | `bs.hasProperty(BlockStateProperties.LIT) && bs.getValue(...LIT) && bs.getBlock() instanceof AbstractFurnaceBlock` |
| 方块掉落物品 | `Containers.dropContents(level, pos, blockEntity)` | 手动 loop `itemHandler.getStackInSlot(i)` + `Containers.dropItemStack()` |
| PlaceableFoodBlock codec | 单个基类无法用 `simpleCodec(Base::new)` | 用3个静态内部类各自 `simpleCodec` |
| StreamCodec 超过6字段 | `StreamCodec.composite(...)` 最多6个 | `StreamCodec.of(encoder, decoder)` 手写 |
| ContainerMenu 无槽位 | — | player inv槽放 x=-2000 隐藏，交互通过 C2S payload |
| 从Screen开另一个Menu | — | 发 C2S 包 → 服务端调 `serverPlayer.openMenu` |
| 设置实体着火时长 | `entity.setSecondsOnFire(int)` (不存在) | `entity.setRemainingFireTicks(int ticks)` |
| 从非LivingEntity发射抛射物 | 构造函数会把位置设到shooter | `new Entity(type, level)` 后手动 `setPos/setDeltaMovement/setOwner` |
| MobEffect应用/移除 | — | `player.removeEffect(DeferredHolder)` 直接传 |
| Inventory offhand槽位 | 无常量 | `Inventory.getItem(40)` 即副手；主手用 `selected` |
| 客户端实体描边 | `setGlowingTag(true)` 客户端无效 | 重写 `isCurrentlyGlowing()`，客户端读静态标志 |
| 物品动态贴图 | — | `ItemProperties.register()` 在 `FMLClientSetupEvent.enqueueWork()` 中注册；model用overrides |
| Patchouli 书籍结构 | `data/` 下 → `use_resource_pack` 异常 | `book.json` 加 `"use_resource_pack": true`；JSON 放 `assets/` |
| Patchouli 软依赖 | 直接Maven成硬依赖 | `localRuntime` + BlameJared maven + 反射调用 |
| 条件性BiomeModifier | 标准 `add_features` 无法动态开关 | 自定义 `BiomeModifier` + config 读取 |
| 熔炉配方返还容器 | — | 1.21.1 原生支持 crafting remainder |
| 彩色实体描边 | `entity.setGlowingTag` + team color 不同步 | 客户端记分板队伍 + `ChatFormatting.RED` 等颜色，每tick `syncFcTeam()` |
| 自定义维度注册 | 代码注册 dimension | `data/piranport/dimension/` + `dimension_type/` JSON 文件 |

### 关键技术要点

| 要点 | 说明 |
|------|------|
| AircraftEntity | 继承 Entity（非Mob），自管状态机。状态：LAUNCHING→CRUISING→ATTACKING→RETURNING→REMOVED |
| 编组数据 | 存在舰装核心 DataComponent 上（`FlightGroupData`），弹种每架飞机独立（`slotAmmoTypes`） |
| 火控同步 | 客户端 KeyMapping → C2S → 服务端 FireControlManager → S2C 广播 |
| 火控描边 | 客户端记分板队伍 `pp_fc_target`（RED色），`ClientTickHandler.syncFcTeam()` 每tick同步 |
| 友军伤害防护 | 所有抛射物 `hurt()` 中检查 owner 是否为玩家，跳过对玩家及其飞机的伤害 |
| 击落归因 | 抛射物记录 owner UUID，击落飞机时通过 owner 追溯到发射者 |
| 侦察机视角 | `Minecraft.setCameraEntity(entity)` + 每tick同步玩家旋转到飞机 |
| 侦察机区块加载 | `ServerLevel.setChunkForced` 维护 3×3，`remove()` 中释放 |
| 无GUI模式 | 副手驱动自动变身；变身检测用 `TransformationManager.findTransformedCore()` 扫描全背包 |
| 核心护甲存储 | `SHIP_CORE_ARMOR` DataComponent，`overrideOtherStackedOnMe` 收纳袋逻辑 |
| 熔炉蒸发制盐 | `SaltEvaporationHandler`：`NeighborNotifyEvent` → 200tick计时 → 水源→盐块/流动水→盐片 |
| 武器冷却条 | `WeaponReloadDecorator` 实现 `IItemDecorator`，读取 `WEAPON_COOLDOWN` 渲染进度条 |
| 副本系统 | 独立维度 `piranport:dungeon`，JSON驱动关卡数据，实例化战斗场，大厅组队机制 |
| 副本数据 | `data/piranport/dungeon/` 下 chapters/stages/enemy_sets JSON，`DungeonDataLoader` 加载 |
| 脚本化节点 | NodeData.script字段触发脚本分发；`DungeonScriptManager` 每tick驱动；`ArtilleryIntroScript` 实现3阶段（空投→拾取→战斗） |
| 运输机 | `DungeonTransportPlaneEntity` 直线飞行+空投+爬升消失；LootShipEntity.dropping模式实现垂直下落 |
| JEI兼容 | `PiranPortJEIPlugin` 注册3个加工站配方Category，`localRuntime` 软依赖 |

### 配置系统

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `saltGenerationEnabled` | Common | `false` | 河流盐矿自然生成 |
| `shipCoreGuiEnabled` | Common | `false` | 舰装核心GUI开关 |
| `autoResupplyEnabled` | Common | `false` | 自动装填模式 |
| `fighterAmmoEnabled` | Common | `false` | 战斗机子弹消耗 |
| `flammableEffectEnabled` | Common | `false` | 易燃易爆Buff |
| `weaponPickupToInventory` | Common | `false` | 武器拾取进背包 |
| `hotbarOnlyLoad` | Common | `false` | 仅快捷栏负重（无GUI模式） |
| `showLegacyReloadHud` | Client | `false` | 显示旧版装填HUD条（已被物品Decorator替代） |
| `flightGroupEnabled` | Client | `false` | 启用编组配置UI |

### 外部依赖

**Patchouli 1.21.1-93-NEOFORGE**（教程手册，软依赖）：
- `localRuntime` + BlameJared maven，Java 反射调用 API

**JEI (Just Enough Items)**（配方查看，软依赖）：
- `localRuntime`，`PiranPortJEIPlugin` 注册加工站配方Category

---

## Conventions & Rules

### 命名规范
- 类名: `PascalCase` — `CookingPotBlock`
- 注册 ID: `snake_case` — `cooking_pot`
- 常量: `UPPER_SNAKE_CASE`
- 翻译 key: `block.piranport.cooking_pot`

### 代码规范
- 所有注册走 `DeferredRegister`
- 物品数据用 `DataComponents`，不用旧版 NBT
- Client-only 放 `@Mod.EventBusSubscriber(value = Dist.CLIENT)`
- 同时维护 `zh_cn.json` 和 `en_us.json`

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-0.0.10.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.0.10
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.1
neo_version=21.1.220
```

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle
- Minecraft Wiki: https://minecraft.wiki/
- Patchouli Wiki: https://vazkiimods.github.io/Patchouli/
- 原始策划案：`docs/总策划案.docx`
- GitHub: https://github.com/CVIndomitable/piranport_cl.git
