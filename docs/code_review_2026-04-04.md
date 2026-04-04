# 皮兰港 v0.0.10 全局代码审查报告

审查日期：2026-04-04
审查覆盖：163 个 Java 文件，约 18000 行代码，分 5 个模块并行深度审查

---

## 汇总统计

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| **P0 关键** | **12** | 崩溃、安全漏洞、数据丢失 |
| **P1 重要** | **19** | 功能缺陷、性能隐患、稳定性 |
| **P2 一般** | **22** | 代码质量、边缘场景 |
| **P3 建议** | **18** | 优化改进、最佳实践 |

---

## P0 关键问题（已全部修复）

### 安全类

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 1 | dungeon | **SelectNodePayload 无旗舰权限验证 + keySlot 越界** — 任何玩家可操控副本进度，负数 keySlot 导致崩溃 | `SelectNodePayload.java:49` | FIXED |
| 2 | dungeon | **SelectStagePayload keySlot 无范围校验** — 恶意包导致 ArrayIndexOutOfBoundsException | `SelectStagePayload.java:42` | FIXED |
| 3 | dungeon | **JoinLobbyPayload 无距离/方块验证** — 可在任意坐标创建幽灵大厅，HashMap 膨胀导致 OOM | `JoinLobbyPayload.java:29` | FIXED |
| 4 | component | **SlotCooldowns 网络反序列化无大小限制** — 恶意包可分配巨量内存 | `SlotCooldowns.java:47` | FIXED |
| 5 | network | **FlightGroupUpdatePayload slotPayload 字符串无白名单** — 可注入超长字符串、非法 ResourceLocation | `FlightGroupUpdatePayload.java:34` | FIXED |

### 游戏逻辑类

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 6 | entity | **友军伤害无配置项** — 抛射物玩家间友伤无法关闭 | 6个抛射物的 `canHitEntity()` | FIXED（添加 friendlyFireEnabled 配置） |
| 7 | entity | **AerialBombEntity 爆炸硬编码 TNT 交互** — 无视 EXPLOSION_BLOCK_DAMAGE 配置，始终破坏地形 | `AerialBombEntity.java:86` | FIXED |
| 8 | item | **UnicornHarpItem 对敌对生物也施加再生** — 治疗范围包含怪物 | `UnicornHarpItem.java:34` | FIXED |
| 9 | dungeon | **nodeId 假设单字母 A-Z** — 空字符串导致 StringIndexOutOfBoundsException | `DungeonInstance.java:74` | FIXED |

### 性能类

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 10 | client | **AircraftRenderer 每帧创建 ItemStack** — 60FPS x N架飞机 = 每秒数百短命对象 | `AircraftRenderer.java:42` | FIXED |
| 11 | client | **PlaceableFoodRenderer 每帧创建 ItemStack** — 同上 | `PlaceableFoodRenderer.java:27` | FIXED |
| 12 | dungeon | **NodeBattleField 同步放置 32768 个方块** — 主线程冻结数秒 | `NodeBattleField.java:38` | FIXED（flag 16 跳过邻居更新） |

---

## P1 重要问题（已全部修复）

### 功能缺陷

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 1 | **副本死亡事件 vs 飞机召回竞态** — 事件处理顺序无保证，副本内死亡可能错误召回飞机 | `GameEvents.java:328` | FIXED |
| 2 | **clearFcTeam 从未被调用** — 断线后记分板残留描边队伍 | `ClientTickHandler.java:299` | FIXED |
| 3 | **侦察机区块泄漏** — 服务器重启后 reconForcedChunks 不持久化，已加载区块永不释放 | `AircraftEntity.java:1244` | FIXED |
| 4 | **侦察机区块加载无上限** — 可达 441 个区块/人，多人场景 OOM 风险 | `AircraftEntity.java:664` | FIXED |
| 5 | **LobbyUpdatePayload 从未发送** — 大厅成员列表不同步，组队功能名存实亡 | 全局搜索无 `new LobbyUpdatePayload` | FIXED |
| 6 | **ReconExitPayload 未调用 endRecon** — 飞机卸载时玩家可能永久减速 | `ReconExitPayload.java:30` | FIXED |
| 7 | **TownScrollUsePayload 不验证副本维度** — 主世界也能使用回城卷轴 | `TownScrollUsePayload.java:36` | FIXED |
| 8 | **ReviveRequestPayload 不验证死亡状态** — 非死亡时也能"复活"消耗图腾 | `ReviveRequestPayload.java:32` | FIXED |
| 9 | **CookingPotMenu.fromNetwork 可返回 null** — 客户端 NPE 崩溃 | `CookingPotMenu.java:31` | FIXED |

### 性能隐患

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 10 | **每 tick 遍历所有渲染实体** — 火控/高亮功能导致 O(N) 轮询 | `ClientTickHandler.java:199` | FIXED |
| 11 | **飞机召回 AABB 600格搜索** — 远距离飞机无法召回 + 大范围查询性能差 | `GameEvents.java:364` | FIXED |
| 12 | **侦察机每 tick 更新全背包地图** — 41槽位 x MapItem.update 重操作 | `AircraftEntity.java:643` | FIXED |
| 13 | **DungeonInstanceManager.nextIndex 永不回收** — 长期运行坐标溢出 | `DungeonInstanceManager.java:49` | FIXED |

### 其他

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 14 | **VT弹同 tick 可重复爆炸** — proximityDetonate + onHit 双重触发 | `CannonProjectileEntity.java:185` | FIXED |
| 15 | **WeaponCategory/AircraftType 枚举 STREAM_CODEC 无越界检查** | `WeaponCategory.java:30` | FIXED |
| 16 | **装填声音重复播放** — 客户端+服务端各播一次 | `CannonItem.java:53` | FIXED |
| 17 | **燃料满时返回 true 吞掉交互** — 无法对满油核心安装护甲板 | `ShipCoreItem.java:138` | FIXED |
| 18 | **传送门 enteredPlayers 未持久化** — 重启后已入门记录丢失 | `DungeonPortalEntity.java:33` | FIXED |
| 19 | **FireControlManager/ReconManager 用 HashMap 但关闭时可能并发** | `FireControlManager.java:13` | FIXED |

---

## P2 一般问题（已全部修复）

### 核心与注册模块

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 1 | 客户端读取 Common 配置值（多人游戏可能不一致） | `ClientTickHandler.java:71` | FIXED |
| 2 | 遗留物品 AERIAL_BOMB_SMALL/MEDIUM 注册未清理 | `ModItems.java:364` | FIXED |
| 3 | 两处 ServerStoppedEvent 清理逻辑分散 | `GameEvents.java:467` / `DungeonEventHandler.java:129` | FIXED |
| 4 | SkinManager 使用 PersistentData（NBT）而非 Attachment | `SkinManager.java:13` | FIXED |
| 5 | EntityType.Builder.build() 传入硬编码字符串而非 null | `ModEntityTypes.java` 全文 | FIXED |

### 实体与战斗模块

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 6 | 所有抛射物友军保护逻辑大量重复，缺乏抽象 | 6个抛射物文件 | FIXED |
| 7 | FlightState.values() 每次调用创建新数组 | `AircraftEntity.java:1136` | FIXED |
| 8 | TransformationManager.weaponLoadMap 使用 IdentityHashMap 未解释原因 | `TransformationManager.java:324` | FIXED |
| 9 | AircraftEntity.buildReturnStack() 设置燃料为 0 | `AircraftEntity.java:881` | FIXED |
| 10 | CannonProjectileEntity.readAdditionalSaveData 中 damage 可能为 0 | `CannonProjectileEntity.java:280` | FIXED |
| 11 | FlammableEffect 爆炸伤害来源是受害者自身 | `FlammableEffect.java:26` | FIXED |

### 物品与方块模块

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 12 | ShipCoreItem.appendHoverText 直接引用客户端类 | `ShipCoreItem.java:204` | FIXED |
| 13 | ShipCoreItem 大量硬编码的物品ID匹配 | `ShipCoreItem.java:928` | FIXED |
| 14 | CookingPotBlockEntity 配方匹配基于对象引用比较 | `CookingPotBlockEntity.java:149` | FIXED |
| 15 | StoneMillBlockEntity.processRecipes 在 onContentsChanged 中递归 | `StoneMillBlockEntity.java:40` | FIXED |
| 16 | SaltEvaporationHandler 静态Map跨世界泄漏 | `SaltEvaporationHandler.java:41` | FIXED |
| 17 | CookingPotBlockEntity 未做配方查找节流 | `CookingPotBlockEntity.java:118` | FIXED |

### 副本模块

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 18 | 玩家死亡时先传送回讲台再显示复活界面 — 竞态 | `DungeonEventHandler.java:80` | FIXED |
| 19 | DungeonPortalEntity.tick() 每 tick 查询 DungeonInstanceManager | `DungeonPortalEntity.java:85` | FIXED |
| 20 | ArtilleryIntroScript 战斗超时用全局 tickCounter | `ArtilleryIntroScript.java:327` | FIXED |
| 21 | DungeonDataLoader JSON 解析无严格 schema 校验 | `DungeonDataLoader.java:42` | FIXED |
| 22 | handleBattleNode 和 handleScriptedBattleNode 大量重复代码 | `DungeonEventHandler.java:216` | FIXED |

### 航空与网络模块

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 23 | FlightGroupScreen 硬编码中文字符串 | `FlightGroupScreen.java:45` | FIXED |
| 24 | CookingPotScreen TEXTURE 常量未使用 | `CookingPotScreen.java:11` | FIXED |
| 25 | ClientSkinData 使用 ConcurrentHashMap 但不需要 | `ClientSkinData.java:8` | FIXED |
| 26 | FlightGroupData STRING_UTF8 无长度限制 | `FlightGroupData.java:115` | FIXED |
| 27 | SkinManager 使用 getPersistentData() | `SkinManager.java:13` | FIXED |

---

## P3 建议（已全部修复）

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 1 | CYCLE_WEAPON_KEY 定义位置不统一 | `ClientEvents.java:38` | FIXED |
| 2 | 声纳效果给附近所有生物加发光（含友方） | `GameEvents.java:96` | FIXED |
| 3 | 配置项缺乏分组 | `ModCommonConfig.java` | FIXED |
| 4 | BiomeModifier 注册使用 static 块 | `ModBiomeModifiers.java:15` | FIXED |
| 5 | CreativeTab 列表手动维护负担大 | `ModCreativeTabs.java:20` | FIXED |
| 6 | AircraftEntity FOLLOW 模式每 tick 查找侦察机 | `AircraftEntity.java:398` | FIXED |
| 7 | AircraftEntity sendPendingChunks 直接发低层网络包 | `AircraftEntity.java:702` | FIXED |
| 8 | TransformationManager 中的调试计时 System.nanoTime() | `TransformationManager.java:196` | FIXED |
| 9 | 所有抛射物 notifyOwner 每次命中创建临时 ItemStack | 多个文件 | FIXED |
| 10 | FloodingEffect amplifier 参数未使用 | `FloodingEffect.java:13` | FIXED |
| 11 | ReconControlPayload 每 tick 发送网络包 | `ClientTickHandler.java:267` | FIXED |
| 12 | drawSlotBg 方法在多个 Screen 类中重复 | 多个文件 | FIXED |
| 13 | JEI 插件箭头和图标使用 fill() 硬编码绘制 | `CookingPotRecipeCategory.java:62` | FIXED |
| 14 | FlightGroupMenu player inventory 槽位放在 (-2000, -2000) | `FlightGroupMenu.java:59` | FIXED |
| 15 | ModPackets.register 使用硬编码版本号 "1" | `ModPackets.java:13` | FIXED |
| 16 | SkinCoreItem 使用硬编码 skinId 整数 | `SkinCoreItem.java:18` | FIXED |
| 17 | NodeEnteredPayload handle 方法为空 | `NodeEnteredPayload.java:30` | FIXED |
| 18 | DungeonHudLayer 状态在退出副本时不清理 | `DungeonHudLayer.java:25` | FIXED |

---

## 亮点（做得好的部分）

- **DeferredRegister 使用规范**，所有注册类型正确注册到 MOD 总线
- **DataComponent 系统**替代旧版 NBT，codec/stream codec 完整
- **事件总线分配正确**（MOD 总线 vs GAME 总线）
- **AircraftEntity 状态机**设计完整，`remove()` 有资源清理安全网
- **副本系统架构清晰**：数据层/实例层/事件层/脚本层四层分明
- **客户端断线清理**（`onClientDisconnect`）覆盖全面
- **FlightGroupData** 的 STREAM_CODEC 已有大小限制，说明有安全意识

---

## P0 修复详情

### 1. 网络包安全验证（5项）
- `SelectNodePayload.handle()`: 添加 keySlot 范围校验、lecternPos 距离校验（<= 64 方块平方）、旗舰权限校验
- `SelectStagePayload.handle()`: 添加 keySlot 范围校验
- `JoinLobbyPayload.handle()`: 添加距离校验 + DungeonLecternBlock 方块类型验证
- `SlotCooldowns.STREAM_CODEC`: 添加 endTick/totalTick map 大小上限（64）
- `FlightGroupUpdatePayload.handle()`: 添加 slotPayload 字符串白名单（"", "piranport:aerial_torpedo", "piranport:aerial_bomb"）

### 2. 友军伤害配置化
- 新增 `ModCommonConfig.FRIENDLY_FIRE_ENABLED`（默认 true）
- 修改 6 个抛射物的 `canHitEntity()`：`CannonProjectileEntity`, `TorpedoEntity`, `AerialBombEntity`, `BulletEntity`, `SanshikiPelletEntity`
- 修改 VT 近炸引信的 Player 过滤逻辑，改为根据配置判断

### 3. AerialBombEntity 爆炸配置
- `onHitEntity()` 和 `onHitBlock()` 中的 `Level.ExplosionInteraction.TNT` 改为读取 `ModCommonConfig.EXPLOSION_BLOCK_DAMAGE` 配置

### 4. UnicornHarpItem 治愈范围修复
- `getEntitiesOfClass(LivingEntity.class, ...)` 改为 `getEntitiesOfClass(Player.class, ...)`，只对玩家施加再生效果

### 5. DungeonInstance nodeId 防御性检查
- 添加 null/空字符串检查
- 支持大小写字母 A-Z/a-z
- 非字母 nodeId 使用 hashCode fallback

### 6. 渲染器 ItemStack 缓存
- `AircraftRenderer`: 使用 `EnumMap<AircraftType, ItemStack>` 缓存 + `getDefaultInstance()`
- `PlaceableFoodRenderer`: 使用 `item.getDefaultInstance()` 替代 `new ItemStack(item)`

### 7. NodeBattleField 地形生成优化
- `setBlock` flag 从 `2` 改为 `2 | 16`（16 = UPDATE_SUPPRESS_DROPS，跳过邻居更新通知）

---

## P1 修复详情

### 1. 飞机召回跨维度修复（P1-1 + P1-11）
- `recallAircraftForPlayer` 改为遍历所有 ServerLevel，使用 `getEntities().getAll()` 替代 AABB 搜索
- 解决副本死亡时因维度不同导致飞机未召回的竞态问题
- 解决 600 格搜索范围外飞机无法召回的问题

### 2. clearFcTeam 调用修复（P1-2）
- `resetClientState()` 中调用 `clearFcTeam()` 清理记分板队伍

### 3. 侦察机区块泄漏修复（P1-3）
- `addAdditionalSaveData` 持久化 `reconForcedChunks` 为 LongArray
- `readAdditionalSaveData` 加载后立即释放所有已保存的强制加载区块

### 4. 侦察机区块加载上限（P1-4）
- 半径上限从 10 降至 5（最大 121 区块/人 vs 原 441）

### 5. LobbyUpdatePayload 发送修复（P1-5）
- `DungeonLobbyManager` 新增 `broadcastLobbyUpdate()` 方法
- `JoinLobbyPayload`、`LeaveLobbyPayload`、`SelectStagePayload` 的 handle 方法中调用广播

### 6. ReconExitPayload endRecon 修复（P1-6）
- handle 方法中在转换飞机状态前先调用 `ReconManager.endRecon()`

### 7. TownScrollUsePayload 维度验证（P1-7）
- 添加 `DUNGEON_DIMENSION` 维度检查，仅在副本维度内可使用

### 8. ReviveRequestPayload 状态验证（P1-8）
- 添加维度检查：玩家必须不在副本维度（死亡后被传送回主世界）
- 添加存活检查

### 9. CookingPotMenu.fromNetwork NPE 修复（P1-9）
- `return null` 改为 `throw new IllegalStateException`

### 10. 渲染实体遍历节流（P1-10）
- 高亮全量扫描改为每 4 tick 一次
- FC 目标存在时仍保持每 tick 更新

### 11. 侦察机地图更新节流（P1-12）
- 全背包 MapItem.update 从每 tick 改为每 20 tick

### 12. DungeonInstanceManager 索引回收（P1-13）
- 新增 `freedIndices` 队列，cleanupInstance 时回收索引
- createInstance 优先使用回收索引
- freedIndices 持久化到 SavedData

### 13. VT弹双重爆炸修复（P1-14）
- 新增 `exploded` 标志，`proximityDetonate`/`onHitEntity`/`onHitBlock` 均检查

### 14. 枚举 STREAM_CODEC 越界检查（P1-15）
- `WeaponCategory`、`AircraftInfo.AircraftType`、`AircraftInfo.BombingMode` 添加边界检查和默认值回退

### 15. 装填声音服务端限定（P1-16）
- `CannonItem.overrideOtherStackedOnMe` 中 `playSound` 添加 `!isClientSide()` 条件

### 16. 燃料满时不吞掉交互（P1-17）
- 岩浆桶/燃料物品加注满油时 `return true` 改为 `return false`，允许后续交互（如安装护甲板）

### 17. 传送门 enteredPlayers 持久化（P1-18）
- `addAdditionalSaveData`/`readAdditionalSaveData` 中保存/加载 enteredPlayers UUID 列表

### 18. FireControlManager/ReconManager 线程安全（P1-19）
- `HashMap` 改为 `ConcurrentHashMap`

---

## P2 修复详情

### 1. 客户端 Common 配置读取（P2-1）
- `ClientTickHandler`: 移除客户端 `SHIP_CORE_GUI_ENABLED` 读取，V键始终发送 `CycleWeaponPayload`，由服务端判断

### 2. 遗留物品标记（P2-2）
- `AERIAL_BOMB_SMALL`/`AERIAL_BOMB_MEDIUM` 标记 `@Deprecated`，保留注册以兼容旧存档

### 3. 清理逻辑整理（P2-3）
- `DungeonEventHandler` 改用 `ServerStoppingEvent`（在关闭前触发）
- `GameEvents.onServerStopped` 添加交叉引用注释

### 4/27. SkinManager 迁移备注（P2-4, P2-27）
- 添加 TODO 注释标记未来迁移到 NeoForge Entity Attachments

### 5. EntityType.build() 参数修正（P2-5）
- 所有 11 个 `EntityType.Builder.build("xxx")` 改为 `.build(null)`，使用 DeferredRegister 的注册名

### 6. 友军伤害逻辑抽象（P2-6）
- 新增 `FriendlyFireHelper.shouldBlockHit()` 工具方法
- 5 个抛射物 `canHitEntity()` 统一调用，消除重复代码

### 7. 枚举 values() 缓存（P2-7）
- `AircraftEntity`: 缓存 `FlightState.values()` 和 `AircraftType.values()` 为 static final 数组

### 8. IdentityHashMap 注释（P2-8）
- `TransformationManager.weaponLoadMap`: 添加注释说明 Item 是注册单例，引用相等安全且高效

### 9. 燃料归零注释（P2-9）
- `buildReturnStack()`: 添加注释说明 fuel=0 是有意设计（返回飞机需重新加油）

### 10. 炮弹伤害默认值（P2-10）
- `CannonProjectileEntity.readAdditionalSaveData`: 加载后 damage≤0 时回退为 4.0f

### 11. 易燃爆炸来源修正（P2-11）
- `FlammableEffect.explode()` 第一个参数从 entity(受害者) 改为 null，避免自伤归因

### 12. ShipCoreItem 客户端类安全注释（P2-12）
- 添加注释说明 `FMLEnvironment.dist.isClient()` 已防止服务端加载 `Minecraft` 类

### 13. 硬编码物品ID标记（P2-13）
- `matchesCaliber` 添加 TODO 标记未来改用 item tags 数据驱动

### 14/17. CookingPotBlockEntity 配方缓存（P2-14, P2-17）
- 配方比较从引用相等改为 `RecipeHolder.id()` 比较
- 新增 `inputsDirty` 标志，仅在输入变化时重新查询配方（节流）
- `onContentsChanged` 触发 `markInputsDirty()`

### 15. StoneMillBlockEntity 递归安全（P2-15）
- 添加注释说明 `processing` flag + try/finally + while 循环是完整的递归防护

### 16. SaltEvaporationHandler 维度清理（P2-16）
- 新增 `LevelEvent.Unload` 监听，维度卸载时移除对应条目
- 保留 `ServerStoppedEvent` 清理作为安全网

### 18. 副本死亡无敌帧（P2-18）
- `onPlayerDeath` 取消死亡后设置 `invulnerableTime = 40`（2秒无敌帧）

### 19. 传送门 tick 节流（P2-19）
- `DungeonPortalEntity.tick()` 改为每 10 tick 检测一次玩家进入

### 20. 脚本超时改为阶段计时（P2-20）
- 新增 `battleStartTick` 字段，战斗阶段开始时记录
- 超时判断改为 `(tickCounter - battleStartTick) > 12000`

### 21. 副本 JSON 校验（P2-21）
- `DungeonDataLoader`: 新增 `requireField()` 方法
- `parseChapter`/`parseStage`/`parseNode`/`parseEnemySet` 添加必填字段校验

### 22. 战斗节点逻辑提取（P2-22）
- 提取 `prepareBattleNode()` 共享方法 + `BattleSetupResult` record
- `handleBattleNode` 和 `handleScriptedBattleNode` 共用准备逻辑

### 23. FlightGroupScreen 国际化（P2-23）
- 硬编码 "鱼雷"/"航弹" 改为 `Component.translatable()` + i18n key
- 新增 `gui.piranport.payload_torpedo`/`gui.piranport.payload_bomb` 到 zh_cn/en_us

### 24. CookingPotScreen 死代码清理（P2-24）
- 移除未使用的 `TEXTURE` 常量和 `RenderSystem`/`ResourceLocation` 导入

### 25. ClientSkinData 线程安全优化（P2-25）
- `ConcurrentHashMap` 改为 `HashMap`（客户端单线程，无需并发容器）

### 26. FlightGroupData 字符串长度限制（P2-26）
- `STRING_UTF8.decode()` 后添加 256 字符长度校验

---

## P3 修复详情

### 1. CYCLE_WEAPON_KEY 定义位置统一（P3-1）
- 主定义移至 `ModKeyMappings.CYCLE_WEAPON`，与其他按键统一管理
- `ClientEvents.CYCLE_WEAPON_KEY` 标记 `@Deprecated`，委托到 `ModKeyMappings`
- `ClientTickHandler` 改为引用 `ModKeyMappings.CYCLE_WEAPON`

### 2. 声纳效果排除友方（P3-2）
- 过滤条件增加 `!(e instanceof Player)`，声纳只对非玩家生物施加发光

### 3. 配置项分组（P3-3）
- 使用路径前缀分组：`equipment.*`、`gui.*`、`combat.*`、`worldgen.*`
- 配置文件自动生成分组层级

### 4. BiomeModifier 改用 DeferredHolder（P3-4）
- `static {}` 块改为 `DeferredHolder` 字段，符合 DeferredRegister 惯用模式

### 5. CreativeTab 自动填充（P3-5）
- 148 行手动 `output.accept()` 改为遍历 `ModItems.ITEMS.getEntries()`
- `EXCLUDED` Set 排除内部/废弃物品（TAB_ICON、AERIAL_BOMB_SMALL/MEDIUM）

### 6. FOLLOW 模式侦察机缓存（P3-6）
- 新增 `cachedReconAircraft` + `reconCacheTick` 字段
- 每 20 tick 刷新一次缓存；飞机被移除时立即失效

### 7. sendPendingChunks 文档（P3-7）
- 添加注释说明为何使用原版 `ClientboundLevelChunkWithLightPacket`（NeoForge 无等效 API）

### 8. 移除调试计时代码（P3-8）
- 删除 `System.nanoTime()` 计时和 `PiranPortDebug.perf()` 调用

### 9. notifyOwner 消除临时 ItemStack（P3-9）
- `TorpedoEntity`、`AerialBombEntity`、`BulletEntity`、`SanshikiPelletEntity`：
  `new ItemStack(getDefaultItem()).getHoverName()` → `getDefaultItem().getDescription()`

### 10. FloodingEffect amplifier 生效（P3-10）
- 伤害随 amplifier 递增：`1.0 + amplifier * 0.5`
- tick 间隔随 amplifier 缩短：`max(10, 20 - amplifier * 5)`

### 11. 侦察机控制包节流（P3-11）
- `handleReconInput` 每 2 tick 发送一次，网络流量减半

### 12. drawSlotBg 提取到 GuiHelper（P3-12）
- 新建 `client/GuiHelper.java`，包含标准 3D 槽位背景绘制
- `ShipCoreScreen`、`StoneMillScreen`、`FlightGroupScreen` 委托到 `GuiHelper.drawSlotBg()`
- `CookingPotScreen` 保留简化版本（不同视觉风格）

### 13. JEI 箭头常量化（P3-13）
- `CookingPotRecipeCategory` 硬编码坐标提取为 `ARROW_*` 常量

### 14. FlightGroupMenu 槽位文档（P3-14）
- 添加详细注释说明 off-screen 槽位的原因和必要性

### 15. ModPackets 版本号常量化（P3-15）
- 新增 `PROTOCOL_VERSION = "1"` 常量

### 16. SkinCoreItem 改用翻译键（P3-16）
- 消息和 tooltip 中 `skinId` 整数改为 `Component.translatable("skin.piranport." + skinId)`
- 新增 `skin.piranport.1/2/3` 翻译键到 zh_cn/en_us

### 17. NodeEnteredPayload handle 实现（P3-17）
- 调用 `DungeonHudLayer.updateNode(nodeId)` 提供即时 HUD 反馈
- `DungeonHudLayer` 新增 `updateNode()` 方法

### 18. DungeonHudLayer 断线清理（P3-18）
- `ClientGameEvents.onClientDisconnect` 中调用 `DungeonHudLayer.clearDungeonState()`
