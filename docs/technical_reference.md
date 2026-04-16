# 皮兰港技术参考手册

> 本文件是从 `CLAUDE.md` 剥离出的详细技术参考。CLAUDE.md 只保留项目身份与路线图等高频信息，本文件汇集完整目录结构、NeoForge API 注意事项、关键技术要点、配置系统与外部依赖。

---

## Project Structure (v0.0.11)

```
src/main/java/com/piranport/
├── PiranPort.java
├── ClientEvents.java               # MOD总线客户端事件（屏幕/按键/渲染器/ItemProperties注册）
├── ClientGameEvents.java           # GAME总线客户端事件
├── ClientTickHandler.java          # 每tick按键检测（火控/侦察机/高亮/FC描边同步）
├── GameEvents.java                 # GAME总线服务端事件（变身/水上行走/飞机召回等）
├── CommonEvents.java               # 属性注册等通用事件
├── registry/
│   ├── ModItems.java               # 所有物品注册（食材/食物/武器/飞机/导弹/深弹/道具/副本钥匙/回城卷轴/占位物品/深海刷怪蛋）
│   ├── ModBlocks.java              # 含 DUNGEON_LECTERN, RELOAD_FACILITY, AMMO/WEAPON_WORKBENCH, SMOKE_SCREEN, FLARE_LIGHT, ABYSSAL_PORTAL/FRAME/SPAWNER
│   ├── ModCreativeTabs.java
│   ├── ModEntityTypes.java         # 含副本实体 + 武器弹丸 + 9种深海NPC + DeepOceanProjectile + ShipGirl
│   ├── ModDataComponents.java      # 含 DUNGEON_*, WEAPON_COOLDOWN, FUEL_DATA
│   ├── ModMobEffects.java          # FlammableEffect, ReloadBoostEffect, EvasionEffect, FloodingEffect, ExperienceBoostEffect
│   ├── ModBlockEntityTypes.java    # 含 RELOAD_FACILITY, AMMO_WORKBENCH, WEAPON_WORKBENCH, ABYSSAL_SPAWNER
│   ├── ModMenuTypes.java           # 含 DUNGEON_BOOK_MENU, RELOAD_FACILITY_MENU, AMMO_WORKBENCH_MENU, WEAPON_WORKBENCH_MENU
│   ├── ModRecipeTypes.java
│   └── ModKeyMappings.java         # P/O/I 火控、U 编组、V 循环/退侦察、Y 高亮、H 自动升空、F8 调试
├── config/
│   ├── ModCommonConfig.java
│   └── ModClientConfig.java        # 含 showLegacyReloadHud, flightGroupEnabled
├── item/
│   ├── ShipCoreItem.java           # GUI/无GUI双模式发射、右键召回飞机、无GUI背包扫描
│   ├── CannonItem.java
│   ├── TorpedoItem.java / TorpedoLauncherItem.java
│   ├── ArmorPlateItem.java / EngineItem.java / SonarItem.java / TorpedoReloadItem.java
│   ├── ModFoodItem.java / BottleFoodItem.java
│   ├── AircraftItem.java           # 发射入口 + 手动加油（overrideOtherStackedOnMe）
│   ├── AmmoItem.java               # 通用弹药物品
│   ├── MissileItem.java            # 导弹物品（反舰/防空/火箭三种类型）
│   ├── MissileLauncherItem.java    # 导弹发射器
│   ├── DepthChargeLauncherItem.java # 深弹投射器三型号
│   ├── FlareLauncherItem.java      # 照明弹发射器
│   ├── SmokeCandleItem.java        # 发烟筒
│   ├── MysteriousWeaponItem.java   # 电磁炮（无视重力高速弹丸）
│   ├── GungnirItem.java            # 冈格尼尔投掷武器
│   ├── CommandSwordItem.java       # 黎塞留的指挥刀
│   ├── HatsuyukiMainGunItem.java   # 初雪的主炮（斧类）
│   ├── EugenShieldItem.java        # 欧根的舰盾（盾牌型，150度格挡）
│   ├── TaihouUmbrellaItem.java     # 大凤的伞
│   ├── UnicornHarpItem.java        # 独角兽的竖琴
│   ├── DamageControlItem.java      # 损害管制（消除debuff+灭火）
│   ├── QuickRepairItem.java        # 快速修复（一次性瞬间治疗）
│   ├── RepairKitItem.java          # 维修台（持续对准生物恢复）
│   ├── FootballArmorItem.java      # 足球巨星套装（经验提升Buff）
│   ├── KirinHeadbandItem.java      # 基林级发带（恒定隐身）
│   ├── SkinCoreItem.java           # 皮肤核心（3种变体）
│   ├── PlaceholderItem.java        # 通用占位物品（自定义tooltip）
│   ├── FloatingTargetItem.java
│   └── GuidebookItem.java
├── block/
│   ├── RiceCropBlock.java          # 水稻（3阶段waterlogged作物）
│   ├── SaltChipBlock.java          # 盐片薄层方块（蒸发产物）
│   ├── SaltEvaporationHandler.java # 熔炉上方水→盐蒸发逻辑
│   ├── CookingPotBlock.java / StoneMillBlock.java / CuttingBoardBlock.java
│   ├── PlaceableFoodBlock.java
│   ├── FourStageCropBlock.java / ThreeStageCropBlock.java
│   ├── ReloadFacilityBlock.java    # 装填设施（发射器+弹药装填）
│   ├── AmmoWorkbenchBlock.java     # 弹药合成台
│   ├── WeaponWorkbenchBlock.java   # 武器合成台（5标签页/下拉/蓝图/定时合成）
│   ├── YubariWaterBucketBlock.java # 夕张的水桶（无限水源）
│   ├── SmokeScreenBlock.java       # 烟幕方块（非实体）
│   ├── FlareLightBlock.java        # 照明弹光源方块
│   ├── PortalFrameBlock.java       # 深海传送门框架方块
│   ├── AbyssalPortalBlock.java     # 深海传送门方块（非实体，发光）
│   └── AbyssalSpawnerBlock.java    # 一次性刷怪方块（结构用，自毁）
├── block/entity/                   # 含 ReloadFacilityBlockEntity, AmmoWorkbenchBlockEntity, WeaponWorkbenchBlockEntity, AbyssalSpawnerBlockEntity
├── entity/
│   ├── CannonProjectileEntity.java # 含友军伤害防护 + 击落归因
│   ├── TorpedoEntity.java          # 伤害/速度/航程由装填鱼雷物品决定
│   ├── AircraftEntity.java         # 状态机 + 五种飞机AI + isCurrentlyGlowing() + getTeamColor()
│   ├── AerialBombEntity.java       # 含友军伤害防护 + 击落归因
│   ├── BulletEntity.java           # 含友军伤害防护 + 击落归因
│   ├── MissileEntity.java          # 导弹实体（制导追踪）
│   ├── DepthChargeEntity.java      # 深水炸弹（近炸引信 + 区域魔法伤害）
│   ├── RailgunProjectileEntity.java # 电磁炮弹丸（无视重力）
│   ├── FlareProjectileEntity.java  # 照明弹（附着方块发光+命中发光击退）
│   ├── GungnirEntity.java          # 冈格尼尔投掷实体
│   ├── SanshikiPelletEntity.java   # 三式弹散射弹丸
│   ├── AircraftDropEntity.java     # 飞机空投物品
│   ├── LowTierDestroyerEntity.java # 副本敌人（低级驱逐）
│   ├── DeepOceanProjectileEntity.java # 深海NPC多模式弹丸（抛物线/追踪/直射）
│   ├── FloatingTargetEntity.java
│   └── DungeonTransportPlaneEntity.java # 副本运输机（空投脚本用）
├── effect/
│   ├── FloodingEffect.java
│   ├── FlammableEffect.java
│   ├── ReloadBoostEffect.java
│   ├── EvasionEffect.java
│   └── ExperienceBoostEffect.java  # 经验提升（足球套装触发）
├── menu/
│   ├── ShipCoreMenu.java / ShipCoreScreen.java
│   ├── CookingPotMenu.java / CookingPotScreen.java
│   ├── StoneMillMenu.java / StoneMillScreen.java
│   ├── FlightGroupMenu.java / FlightGroupScreen.java
│   ├── ReloadFacilityMenu.java / ReloadFacilityScreen.java
│   ├── AmmoWorkbenchMenu.java / AmmoWorkbenchScreen.java
│   └── WeaponWorkbenchMenu.java / WeaponWorkbenchScreen.java
├── component/
│   ├── PlaceableInfo.java
│   ├── AircraftInfo.java
│   ├── FlightGroupData.java        # 4编组 + slotAmmoTypes + attackMode
│   ├── LoadedAmmo.java
│   ├── SlotCooldowns.java
│   ├── WeaponCategory.java         # 含 DEPTH_CHARGE 分类
│   ├── WeaponCooldown.java         # endTick/totalTick/getFraction 冷却数据
│   └── FuelData.java               # 舰装核心燃油数据
├── ammo/                           # 弹药系统
│   ├── AmmoCategory.java           # 弹药分类
│   ├── AmmoRecipe.java             # 弹药配方结构
│   └── AmmoRecipeRegistry.java     # 弹药配方注册表
├── crafting/                       # 武器合成系统
│   ├── WeaponWorkbenchRecipe.java  # 武器合成配方
│   └── WeaponWorkbenchRecipeRegistry.java
├── skin/                           # 皮肤系统
│   ├── SkinManager.java            # 服务端皮肤管理
│   └── ClientSkinData.java         # 客户端皮肤数据
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
│   ├── WeaponReloadDecorator.java  # IItemDecorator 武器冷却条（红→黄→绿原版配色）
│   ├── GuiHelper.java              # 共享GUI绘制工具类
│   ├── AircraftRenderer.java
│   ├── CuttingBoardRenderer.java
│   ├── PlaceableFoodRenderer.java
│   ├── SkinOverlayLayer.java       # 自定义皮肤渲染层
│   ├── DungeonTransportPlaneRenderer.java
│   ├── LowTierDestroyerRenderer.java
│   ├── DeepOceanRenderer.java      # 深海NPC统一渲染器
│   └── ShipGirlRenderer.java       # 舰娘NPC渲染器
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
├── npc/                            # NPC系统 (v0.0.11)
│   ├── deepocean/
│   │   ├── AbstractDeepOceanEntity.java       # 深海NPC基类
│   │   ├── DeepOceanSupplyEntity.java         # 补给酱
│   │   ├── DeepOceanDestroyerEntity.java      # 深海驱逐
│   │   ├── DeepOceanLightCruiserEntity.java   # 深海轻巡
│   │   ├── DeepOceanHeavyCruiserEntity.java   # 深海重巡
│   │   ├── DeepOceanBattleCruiserEntity.java  # 深海战巡
│   │   ├── DeepOceanBattleshipEntity.java     # 深海战列
│   │   ├── DeepOceanLightCarrierEntity.java   # 深海轻母
│   │   ├── DeepOceanCarrierEntity.java        # 深海航母
│   │   └── DeepOceanSubmarineEntity.java      # 深海潜艇
│   ├── shipgirl/
│   │   └── ShipGirlEntity.java                # 舰娘NPC
│   ├── data/
│   │   ├── DeepOceanData.java                 # NPC属性数据结构
│   │   └── DeepOceanDataLoader.java           # JSON驱动NPC数据加载
│   └── ai/
│       ├── FleetGroup.java                    # 舰队组数据
│       ├── FleetGroupManager.java             # 舰队组管理器(SavedData)
│       ├── goal/
│       │   ├── OrbitTargetGoal.java           # 环绕AI
│       │   ├── CannonAttackGoal.java          # 炮击AI（含提前量）
│       │   ├── TorpedoAttackGoal.java         # 鱼雷攻击AI
│       │   ├── AircraftLaunchGoal.java        # 航母放飞AI
│       │   ├── SubmergeGoal.java              # 潜艇下潜AI
│       │   ├── FleetAlertGoal.java            # 集群共享警戒
│       │   └── IdleWanderGoal.java            # 待机漫游
│       └── ballistic/
│           ├── ParabolicCalculator.java       # 抛物线弹道计算
│           ├── TrackingCalculator.java        # 比例导航制导
│           └── ProximityFuse.java             # 近炸引信
├── debug/
│   ├── PiranPortDebug.java
│   └── PiranPortCommands.java      # /ppd 调试命令
├── network/                        # 所有 CustomPacketPayload（含 SkinSyncPayload, SkinRevertPayload, AmmoWorkbenchCraftPayload 等）
├── recipe/
├── worldgen/
│   ├── SaltGenBiomeModifier.java
│   ├── AbyssalOceanBiomeModifier.java  # 预留，v0.0.11暂不激活
│   ├── LootChestProcessor.java         # 结构箱子战利品注入
│   ├── RuinDegradationProcessor.java   # 遗迹损坏处理器
│   ├── ModStructureProcessors.java     # 处理器注册
│   └── ModStructures.java              # 结构常量
└── registry/
    └── ModBiomeModifiers.java
```

---

## NeoForge 21.1.220 API 坑（已踩过，勿重蹈）

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
| 导弹发射器构造NPE | `getDefaultItem()` 在 `super()` 期间 displayItemId 为 null | 延迟初始化或 null 检查 |
| 防空导弹/H模式漏识别幻翼 | `Phantom extends FlyingMob`，不是 `Monster` 子类 | 敌对筛选用 `instanceof Enemy` 接口，覆盖 Monster/Phantom/Vex/Ghast |

---

## 关键技术要点

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
| 武器冷却条 | `WeaponReloadDecorator` 实现 `IItemDecorator`，读取 `WEAPON_COOLDOWN` 渲染进度条（红→黄→绿原版配色，导弹独立黄色） |
| 导弹系统 | 三种类型（ANTI_SHIP/ANTI_AIR/ROCKET），`MissileEntity` 制导追踪，防空导弹自动升空模式下自动打击空中目标 |
| 深弹系统 | `DepthChargeLauncherItem` 三型号，`DepthChargeEntity` 近炸引信 + 区域魔法伤害，独立DEPTH_CHARGE武器分类 |
| 命名鱼雷 | 15种（G7a/G7e/MK系列/九式/零式），各有独立伤害/航程/速度属性，`TorpedoEntity` 读取装填物品数据 |
| 武器/弹药合成 | `WeaponWorkbenchBlock`（5标签页/下拉选择/蓝图/定时合成）+ `AmmoWorkbenchBlock`，配方注册在 `ammo/` 和 `crafting/` 包 |
| 装填设施 | `ReloadFacilityBlock` 支持火炮/鱼雷/导弹发射器的弹药装填 |
| 皮肤系统 | `SkinManager`（服务端） + `ClientSkinData` + `SkinOverlayLayer`（渲染层），`SkinCoreItem` 3种变体 |
| 特殊道具 | 电磁炮（无视重力弹丸）、照明弹（光源+发光）、发烟筒（烟幕方块）、损管（消debuff）、快速修复（瞬间治疗）、维修台（持续恢复）、足球套装（经验提升）、基林发带（恒定隐身） |
| 无GUI模式负重 | 固定只计算快捷栏（移除hotbarOnlyLoad配置项），副手核心即生效核心 |
| 潜艇隐身 | 潜艇核心变身后眼睛没入水中时自动获得隐身效果 |
| 副本系统 | 独立维度 `piranport:dungeon`，JSON驱动关卡数据，实例化战斗场，大厅组队机制 |
| 副本数据 | `data/piranport/dungeon/` 下 chapters/stages/enemy_sets JSON，`DungeonDataLoader` 加载 |
| 脚本化节点 | NodeData.script字段触发脚本分发；`DungeonScriptManager` 每tick驱动；`ArtilleryIntroScript` 实现3阶段（空投→拾取→战斗） |
| 运输机 | `DungeonTransportPlaneEntity` 直线飞行+空投+爬升消失；LootShipEntity.dropping模式实现垂直下落 |
| JEI兼容 | `PiranPortJEIPlugin` 注册3个加工站配方Category，`localRuntime` 软依赖 |
| 深海NPC基类 | `AbstractDeepOceanEntity extends PathfinderMob`，含环绕/炮击/集群三大AI Goal |
| 抛物线弹道 | 根据距离线性插值抛高，发射时计算提前量 |
| 追踪弹制导 | 过最高点后启用比例导航，设最大过载限制转弯率 |
| 集群警戒 | 同 clusterUUID 实体共享首个目标，已交战者不切换 |
| 结构生成 | Jigsaw/SinglePool + StructureProcessor 注入战利品表和敌人 |
| 一次性Spawner | AbyssalSpawnerBlock 在结构首次加载时生成NPC后自毁 |
| 深海NPC数据驱动 | `data/piranport/npc/deep_ocean/` 下9种JSON配置（血量/护甲/速度/武器/战利品表），`DeepOceanDataLoader` 加载 |
| 深海弹丸多模式 | `DeepOceanProjectileEntity` 支持抛物线/追踪/直射三种模式 |
| 舰娘NPC | `ShipGirlEntity extends PathfinderMob`，友好NPC框架，可交互，对深海敌人作战 |
| 占位物品 | `PlaceholderItem` 通用占位（国旗×7/混沌碎片×9/传送门核心/经验弹/战斗报告），战利品和合成材料 |
| 调试命令 | `/ppd spawn_ruin <type>` 强制生成遗迹、`/ppd spawn_abyssal <type> [count]` 刷怪、`/ppd locate_ruin <type>` 定位 |
| 遗迹战利品表 | 4级战利品（portal_ruin/supply_depot/outpost/abyssal_base），`LootChestProcessor` 注入 |

---

## 配置系统

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `saltGenerationEnabled` | Common | `false` | 河流盐矿自然生成 |
| `shipCoreGuiEnabled` | Common | `false` | 舰装核心GUI开关 |
| `autoResupplyEnabled` | Common | `false` | 自动装填模式 |
| `fighterAmmoEnabled` | Common | `false` | 战斗机子弹消耗 |
| `flammableEffectEnabled` | Common | `false` | 易燃易爆Buff |
| `weaponPickupToInventory` | Common | `false` | 武器拾取进背包 |
| `ruinGenerationEnabled` | Common | `true` | 主世界遗迹生成总开关 |
| `portalRuinSpacing` | Common | `40` | 传送门遗迹间距（参考值） |
| `supplyDepotSpacing` | Common | `32` | 补给站间距（参考值） |
| `outpostSpacing` | Common | `48` | 前哨站间距（参考值） |
| `abyssalBaseSpacing` | Common | `64` | 深海基地间距（参考值） |
| `abyssalEnemyDifficultyMultiplier` | Common | `1.0` | 深海NPC属性倍率 |
| `showLegacyReloadHud` | Client | `false` | 显示旧版装填HUD条（已被物品Decorator替代） |
| `flightGroupEnabled` | Client | `false` | 启用编组配置UI |

---

## 外部依赖

**Patchouli 1.21.1-93-NEOFORGE**（教程手册，软依赖）：
- `localRuntime` + BlameJared maven，Java 反射调用 API

**JEI (Just Enough Items)**（配方查看，软依赖）：
- `localRuntime`，`PiranPortJEIPlugin` 注册加工站配方Category
