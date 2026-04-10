# 皮兰港 v0.0.11 全局代码审查报告

审查日期：2026-04-10
审查范围：自上次审查(2026-04-05)以来116个Java文件，+9346行代码
审查模块：NPC系统 / 新物品与战斗 / 合成台与方块 / 航空系统 / 世界生成与注册

---

## 汇总统计

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| **P0 关键** | **13** | 服务器崩溃/物品复制/保护绕过/系统不可用 |
| **P1 重要** | **18** | 逻辑错误/数据丢失/性能/安全隐患 |
| **P2 一般** | **27** | 平衡/兼容/代码质量 |
| **P3 建议** | **24** | 死代码/微优化/风格 |

---

## P0 关键问题（13项）

### 旧P0遗留（3项，上次审查未修复）

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 1 | dungeon | **非脚本战斗节点永远无法通关** — 无旗舰击杀检测。`handleBattleNode` 生成敌人后返回，无任何机制检测旗舰死亡并清关。玩家在副本维度永久卡住 | DungeonEventHandler.java:290-299 | ✅ 已修复 |
| 2 | dungeon | **SelectNodePayload 无节点可达性验证** — 跳关漏洞。恶意客户端可发送任意 nodeId 直接跳到 Boss 节点 | SelectNodePayload.java:78-103 | ✅ 已修复 |
| 3 | network | **S2C Payload 导入客户端类** — `ReconStartPayload` 和 `ReconEndPayload` 在类级别 import `net.minecraft.client.Minecraft`，专用服务器加载此类时 `NoClassDefFoundError` 崩溃 | ReconStartPayload.java:6, ReconEndPayload.java:6 | ✅ 已修复 |

### 新P0（10项）

| # | 模块 | 问题 | 位置 | 建议修复 |
|---|------|------|------|---------|
| 4 | item | **发烟筒方块放置绕过保护** — `level.setBlock()` 未检查 `level.mayInteract()`，可在出生保护/领地保护区内放置不可破坏的烟幕方块 | SmokeCandleItem.java:41 | ✅ 已修复 |
| 5 | item | **发烟筒无冷却** — 16耐久无CD，可在不到1秒内放置288个烟幕方块 | SmokeCandleItem.java:27 | ✅ 已修复 |
| 6 | entity | **照明弹方块放置绕过保护** — 同发烟筒问题，照明弹可射入保护区域放置光源方块 | FlareProjectileEntity.java:59-68 | ✅ 已修复 |
| 7 | item | **冈格尼尔无冷却** — 投掷→返回→再投掷循环可实现机关枪式输出 | GungnirItem.java:39-55 | ✅ 已修复 |
| 8 | block | **武器合成台物品复制** — 破坏方块时 `onRemove` 掉落物品 + `Menu.removed()` 退还物品，GUI打开时破坏方块导致物品翻倍 | WeaponWorkbenchBlock.java:110-124 + WeaponWorkbenchMenu.java:161-177 | ✅ 已修复 |
| 9 | network | **弹药合成台数量无上限** — 恶意客户端可发 `quantity=999`，消耗999份材料但只产出64个物品（ItemStack上限截断） | AmmoWorkbenchCraftPayload.java:47 | ✅ 已修复 |
| 10 | entity | **DeepOceanProjectileEntity 双重引爆** — `onHit()` 中 `super.onHit()` 触发 `detonate()`，随后 `onHit()` 又调用 `discard()`，近炸引信+命中可能产生两次爆炸 | DeepOceanProjectileEntity.java:123-147 | ✅ 已修复 |
| 11 | npc | **航母放飞系统不可用** — `AircraftLaunchGoal` 生成的飞机无 owner/target/AI配置，立即被 `AircraftEntity.tick()` 回收。`activeAircraft` 计数器只增不减，达到上限后航母永久停飞 | AircraftLaunchGoal.java:50-74 | ✅ 已修复：改为DeepOceanProjectileEntity抛物线齐射模拟空袭 |
| 12 | aircraft | **反潜机无GUI模式无默认载荷** — `launchAircraftInventoryMode` 缺少 `case ASW` 分支，ASW飞机默认 `hasBullets=true`，行为等同战斗机而非投深弹 | ShipCoreItem.java:1334-1339 | ✅ 已修复 |
| 13 | aircraft | **反潜机目标范围过宽** — `isAswTarget()` 对所有 `Monster && isInWater()` 返回 true，浅水中的僵尸/骷髅/苦力怕都会被攻击 | AircraftEntity.java:893 | ✅ 已修复：改用isUnderWater() |

---

## P1 重要问题（18项）

| # | 模块 | 问题 | 位置 | 建议修复 |
|---|------|------|------|---------|
| 1 | npc | **死亡NPC沉没期间仍运行AI** — `die()` 设 `isSinking=true` 但 `tick()` 继续执行完整AI（瞄准/射击），死亡实体在40tick沉没动画中继续攻击 | AbstractDeepOceanEntity.java:177-192 | `tick()` 顶部添加 `if (isSinking) { sinkingTicks++; /*仅沉没+粒子*/; return; }` |
| 2 | npc | **舰队跨维度警戒失败** — `alertGroup()` 只查找包含首个成员的 ServerLevel，其他维度的成员不会收到警戒 | FleetGroupManager.java:107-128 | 对每个成员遍历所有 ServerLevel，或约束舰队组为单维度 |
| 3 | npc | **`cleanup()` 从未被调用** — 死亡/卸载成员和空舰队组永久累积，SavedData 无限增长 | FleetGroupManager.java:152 | 注册 `ServerTickEvent`，每6000tick调用一次 `cleanup()` |
| 4 | npc | **NPC炮弹可误伤友军** — `CannonAttackGoal` 使用 `CannonProjectileEntity`，其 `FriendlyFireHelper` 仅检查玩家，NPC间无友军保护 | CannonAttackGoal.java:71-86 | 改用 `DeepOceanProjectileEntity`，或在 `FriendlyFireHelper` 添加NPC友军判断 |
| 5 | combat | **UmbrellaBlockHandler 零向量NaN** — 伤害源在玩家中心时 `normalize()` 产生 NaN | UmbrellaBlockHandler.java:43 | 添加 `if (dirToSource.lengthSqr() < 1e-6) return;` |
| 6 | combat | **EugenShieldBlockHandler 垂直朝向NaN** — 玩家正上/正下看时水平方向向量为零，normalize 产生 NaN，盾牌失效 | EugenShieldBlockHandler.java:43 | 添加 `if (lookHorizontal.lengthSqr() < 1e-6) return;` |
| 7 | combat | **格挡处理器未检查 BYPASSES_SHIELD** — 可格挡原版声明应穿透盾牌的伤害（荆棘等）| EugenShieldBlockHandler.java:36 + UmbrellaBlockHandler.java:36 | 添加 `if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) return;` |
| 8 | entity | **冈格尼尔无友军伤害保护** — 与其他所有抛射物不同，未使用 `FriendlyFireHelper`，多人游戏中可伤害友方玩家 | GungnirEntity.java:154-162 | `onHitEntity` 添加 `FriendlyFireHelper.shouldBlockHit()` |
| 9 | item | **维修台零成本无限治疗** — 无耐久、无消耗、无冷却，对任意目标（含敌对生物和Boss）提供无限再生II | RepairKitItem.java:55-89 | 注册时添加 `.durability(N)`，`onUseTick` 定期扣除耐久，限制目标为玩家/友好生物 |
| 10 | network | **弹药合成台无距离验证** — 恶意客户端可对任意已加载区块的合成台发起合成请求 | AmmoWorkbenchCraftPayload.java:35-89 | 添加 `if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) return;` |
| 11 | block | **武器合成台BlockEntity无NBT持久化** — 服务器崩溃时合成台内物品全部丢失（`Menu.removed()` 未执行） | WeaponWorkbenchBlockEntity.java | 添加 `saveAdditional`/`loadAdditional` 序列化 itemHandler 和合成进度 |
| 12 | block | **弹药合成台输出槽满时物品丢失** — 合成完成时输出槽无法容纳结果，材料已消耗但产出被丢弃 | AmmoWorkbenchBlockEntity.java:85-96 | 输出槽满时暂停完成（保持合成状态等待取走），或原地掉落物品 |
| 13 | aircraft | **ASW声呐扫描无大小限制** — 检测列表无上界，水边大型养殖场可能导致大量网络包 | AircraftEntity.java:905-911 | 循环内添加 `if (detectedIds.size() >= 128) break;` |
| 14 | aircraft | **一架飞机清空全部火控目标** — 所有FC目标死亡时调用 `clearTargets()`，影响同一玩家所有飞机的目标列表 | AircraftEntity.java:482-484 | 改为只移除死亡UUID，不清空整个列表 |
| 15 | aircraft | **召回飞机遍历全服实体** — `recallAircraftForPlayer` 对所有维度调用 `getEntities().getAll()`，大型服务器性能问题 | GameEvents.java:456-469 | 改用 `getEntitiesOfClass(AircraftEntity.class, AABB)` 或维护玩家→飞机UUID索引 |
| 16 | config | **5个世界生成配置项无效** — `ruinGenerationEnabled`、4个间距值、难度倍率均已定义但从未读取，用户修改无效果 | ModCommonConfig.java:91-116 | 移除死配置项，或在 BiomeModifier/DataLoader 中实际读取 |
| 17 | worldgen | **遗迹退化处理器可摧毁刷怪器** — `RuinDegradationProcessor` 未豁免 `AbyssalSpawnerBlock`，可能将其退化为空气/圆石 | RuinDegradationProcessor.java:47-49 | 跳过条件添加 `state.is(ModBlocks.ABYSSAL_SPAWNER.get())` |
| 18 | network | **AmmoWorkbenchCraftPayload readUtf()无长度限制** — 默认最大32767字符，恶意客户端可发送大字符串造成GC压力 | AmmoWorkbenchCraftPayload.java:29 | 改用 `buf.readUtf(256)` |

---

## P2 一般问题（27项）

| # | 模块 | 问题 | 位置 |
|---|------|------|------|
| 1 | npc | 潜艇 `isCurrentlyGlowing()` 返回 true 破坏隐身机制 | AbstractDeepOceanEntity.java:283 |
| 2 | npc | `DeepOceanDataLoader.REGISTRY` 非线程安全 HashMap | DeepOceanDataLoader.java:26 |
| 3 | npc | 潜艇鱼雷从水下发射，对水面目标不准 | SubmergeGoal.java + TorpedoAttackGoal.java:74 |
| 4 | npc | 舰队警戒递归防护依赖代码顺序，脆弱 | FleetGroupManager.java:95-128 |
| 5 | npc | JSON加载器缺少必填字段验证 | DeepOceanDataLoader.java:56-81 |
| 6 | npc | 环绕移动绕过寻路和碰撞检测 | OrbitTargetGoal.java:54-121 |
| 7 | item | 快速修复瞬间治疗XV治疗32768HP，过度溢出 | QuickRepairItem.java:33 |
| 8 | entity | 冈格尼尔返回时 `getUsedItemHand()` 返回错误手 | GungnirEntity.java:169 |
| 9 | entity | 冈格尼尔 `noPhysics = true` 无重置机制 | GungnirEntity.java:77-129 |
| 10 | item | 独角兽竖琴治疗包含PvP对手 | UnicornHarpItem.java:34-38 |
| 11 | entity | 电磁炮弹丸NBT伤害值无范围验证 | RailgunProjectileEntity.java:87-96 |
| 12 | block | 武器合成台 `currentUser` 互斥锁可能泄漏 | WeaponWorkbenchBlockEntity.java:93-106 |
| 13 | block | 弹药合成台 `craftingTotalTime` 整数溢出风险 | AmmoWorkbenchBlockEntity.java:70-77 |
| 14 | menu | 弹药合成台Screen每帧扫描2次玩家背包 | AmmoWorkbenchScreen.java:119-129 |
| 15 | block | 武器合成台合成中切换标签页/配方无警告取消 | WeaponWorkbenchMenu.java:114-125 |
| 16 | block | 武器合成台完成时二次验证材料，中途材料被取走则白等 | WeaponWorkbenchBlockEntity.java:175-198 |
| 17 | menu | 弹药合成台 `stillValid()` 方块类型检查不够精确 | AmmoWorkbenchMenu.java:93-96 |
| 18 | aircraft | ASW深弹缺少 `setSourceAircraftName` 击杀归因 | AircraftEntity.java:833-835 |
| 19 | aircraft | ASW默认载荷赋值逻辑脆弱，依赖执行顺序 | AircraftEntity.java:176-186 |
| 20 | aircraft | `ClientAswSonarData` 静态HashMap非线程安全 | ClientAswSonarData.java:17-20 |
| 21 | aircraft | 召回AABB 600格半径过大 | ShipCoreItem.java:1898-1914 |
| 22 | aircraft | 高亮集合每tick清理无节流 | ClientTickHandler.java:265 |
| 23 | aircraft | 侦察机区块加载半径无下限 | AircraftEntity.java:972-1003 |
| 24 | registry | 弃用 `AERIAL_BOMB_SMALL/MEDIUM` 仍注册占用命名空间 | ModItems.java:724-729 |
| 25 | registry | `ModStructures.java` 常量从未被引用 | ModStructures.java:17-21 |
| 26 | registry | 导弹弹药在两个创造标签中重复 | ModCreativeTabs.java:306-310 |
| 27 | worldgen | AbyssalSpawnerBlockEntity 世界生成时可能在无玩家范围时触发 | AbyssalSpawnerBlockEntity.java:72-96 |

---

## P3 建议（24项）

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 1 | npc | NPC鱼雷伤害值未从mob属性读取，全部默认18f | TorpedoAttackGoal.java:63-83 | **已修复** ✅ |
| 2 | npc | `combatAiEnabled` 字段已持久化但从未被检查 | ShipGirlEntity.java:27 | **已修复** ✅ |
| 3 | npc | `maxWanderRadius` 参数存储但未实现限制 | IdleWanderGoal.java:34-42 | **已修复** ✅ |
| 4 | npc | `FleetGroup.getMembers()` 暴露内部可变Set | FleetGroup.java:39 | **已修复** ✅ |
| 5 | npc | `cleanup()` 直接修改 FleetGroup 内部集合绕过 removeMember() | FleetGroupManager.java:152-161 | **已修复** ✅ |
| 6 | npc | 抛物线零距离射击时提前量计算退化 | ParabolicCalculator.java:33 | **已修复** ✅ |
| 7 | item | 基林发带摘除后隐身残留3秒可换装利用 | KirinHeadbandItem.java + GameEvents.java:54-59 | **已修复** ✅ |
| 8 | item | 足球套装经验Buff检测间隔1秒、残留5秒 | FootballArmorItem.java + GameEvents.java:62-74 | **已修复** ✅ |
| 9 | item | 盾牌物品未实现 `canPerformAction(SHIELD_BLOCK)` | EugenShieldItem.java + TaihouUmbrellaItem.java | **已修复** ✅ |
| 10 | entity | 冈格尼尔飞行破坏树叶可用于刷叶子 | GungnirEntity.java:131-137 | **已修复** ✅ |
| 11 | entity | 自主飞机炸弹无 `setOwner` 友军判断失效 | AircraftEntity.java:1221-1226 | **已修复** ✅ |
| 12 | block | 烟幕方块定时tick区块卸载后可能丢失，导致烟幕永久存在 | SmokeScreenBlock.java | **已修复** ✅ |
| 13 | block | 照明弹方块同上定时tick丢失风险 | FlareLightBlock.java | **已修复** ✅ |
| 14 | block | 夕张水桶每20tick扫描6方向无缓存 | YubariWaterBucketBlockEntity.java:24-35 | 跳过（O(6)×50ms开销可忽略） |
| 15 | block | AbyssalSpawnerBlockEntity `entity == null` 时无日志 | AbyssalSpawnerBlockEntity.java:72-73 | **已修复** ✅ |
| 16 | crafting | `WeaponWorkbenchRecipeRegistry` 延迟初始化非线程安全 | WeaponWorkbenchRecipeRegistry.java:17-25 | **已修复** ✅ |
| 17 | ammo | `AmmoRecipeRegistry` 延迟初始化非线程安全 | AmmoRecipeRegistry.java:14-16 | **已修复** ✅ |
| 18 | npc | ShipGirlEntity.hurt() 无实际差异化处理，玩家打了不反击依赖无HurtByTargetGoal | ShipGirlEntity.java:71-79 | **已修复** ✅ |
| 19 | worldgen | `AbyssalOceanBiomeModifier.modify()` 为空占位 | AbyssalOceanBiomeModifier.java:25-28 | 跳过（有意预留） |
| 20 | registry | `PiranPort.java` import风格不一致 | PiranPort.java:43 | **已修复** ✅ |
| 21 | registry | 足球套装可用皮革修复（可能非预期） | ModArmorMaterials.java:24-38 | **已修复** ✅ |
| 22 | debug | `targetFire` 命令向执行者发射导弹，多人服可能误伤OP | PiranPortCommands.java:204-250 | **已修复** ✅ |
| 23 | aircraft | 飞机CRUISING→ATTACKING状态可能因目标快速死亡产生单tick振荡 | AircraftEntity.java:460-484 | **已修复** ✅ |
| 24 | aircraft | GameEvents中3个ConcurrentHashMap仅服务端线程访问，开销无必要 | GameEvents.java:307-313 | **已修复** ✅ |

---

## 与上次审查对比

| 指标 | 2026-04-05 | 2026-04-10 |
|------|-----------|-----------|
| 审查文件数 | 182 | 116（新增/变更） |
| P0 | 5 | 13（含3项旧遗留） |
| P1 | 18 | 18 |
| P2 | 21 | 27 |
| P3 | 8 | 24 |
| 上次P0修复进度 | 0/5 | 1/5（P0-5 VT双爆已修） |

### 上次P0状态跟踪

| # | 问题 | 本次状态 |
|---|------|---------|
| 旧P0-1 | 非脚本战斗节点无法通关 | **仍未修复** |
| 旧P0-2 | SelectNodePayload 跳关漏洞 | **仍未修复** |
| 旧P0-3 | S2C Payload 服务器崩溃 | **仍未修复** |
| 旧P0-4 | AircraftDropEntity 未注册独立EntityType | **仍未修复**（当前功能可用但脆弱） |
| 旧P0-5 | VT+HE双重爆炸 | **已修复** ✅ |

---

## 建议修复优先级

### 立即修复（阻断性问题）
1. **P0-3** S2C Payload 服务器崩溃 — 阻止专用服务器使用本模组
2. **P0-4** 发烟筒保护绕过 — 可在任意保护区域放置不可破坏方块
3. **P0-6** 照明弹保护绕过 — 同上
4. **P0-8** 武器合成台物品复制 — 生存模式可利用
5. **P0-9** 弹药合成台数量溢出 — 材料消耗与产出不匹配

### 尽快修复（下次发布前）
6. **P0-5** 发烟筒冷却 — 防止刷屏
7. **P0-7** 冈格尼尔冷却 — 防止机关枪
8. **P0-12** 反潜机默认载荷 — 功能不可用
9. **P0-13** 反潜机目标范围 — 误攻无关目标
10. **P0-1** 副本通关检测 — 核心玩法断裂
11. **P0-2** 节点跳关验证 — 安全漏洞
12. **P0-10** 深海弹丸双爆 — 伤害异常
13. **P0-11** 航母放飞系统 — 功能不可用

### 正常迭代
14. 所有P1问题（按模块批量修复）
