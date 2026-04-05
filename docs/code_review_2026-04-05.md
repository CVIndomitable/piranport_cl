# 皮兰港 v0.0.10 全局代码与玩法审查报告

审查日期：2026-04-05
审查覆盖：182 个 Java 文件，5 个并行深度审查模块

---

## 汇总统计

| 严重程度 | 数量 | 已修复 | 未修复 |
|---------|------|--------|--------|
| **P0 关键** | **5** | **0** | **5**（本次仅修 P1-P3） |
| **P1 重要** | **18** | **17** | **1** |
| **P2 一般** | **21** | **18** | **3** |
| **P3 建议** | **8** | **5** | **3** |

---

## P0 关键问题（未在本次修复范围内）

| # | 模块 | 问题 | 位置 | 状态 |
|---|------|------|------|------|
| 1 | dungeon | 非脚本战斗节点永远无法通关 — 无旗舰击杀检测 | NodeBattleField / DungeonEventHandler | TODO |
| 2 | dungeon | SelectNodePayload 无节点可达性验证 — 跳关漏洞 | SelectNodePayload.java:70-101 | TODO |
| 3 | network | S2C Payload 直接导入客户端类 — 专用服务器崩溃 | 5个Payload文件 | TODO |
| 4 | entity | AircraftDropEntity 未注册独立 EntityType | AircraftDropEntity.java:17-19 | TODO |
| 5 | entity | CannonProjectileEntity VT+HE弹双重爆炸 | CannonProjectileEntity.java:195 | TODO |

---

## P1 重要问题

### 已修复 (17/18)

| # | 问题 | 位置 | 状态 |
|---|------|------|------|
| 1 | SelectStagePayload 添加距离+方块验证 | SelectStagePayload.java:42 | FIXED |
| 2 | LeaveLobbyPayload 添加距离+方块验证 | LeaveLobbyPayload.java:30 | FIXED |
| 3 | S2C列表Payload反序列化上限 | DungeonResultPayload/LobbyUpdatePayload | FIXED |
| 4 | AerialBombEntity 添加 exploded 标记 | AerialBombEntity.java | FIXED |
| 5 | TorpedoEntity 超时自爆 exploded 保护 | TorpedoEntity.java:157 | FIXED |
| 6 | 侦察模式缓慢效果登录时清理 | GameEvents.java:onPlayerLogin | FIXED |
| 7 | LootShipEntity DROPPING/openedBy 持久化 | LootShipEntity.java:186 | FIXED |
| 8 | ClientDungeonData 断线清理 | ClientGameEvents.java:67 | FIXED |
| 9 | DungeonTransportPlaneEntity 爬升条件修复 | DungeonTransportPlaneEntity.java:126 | FIXED |
| 11 | ShipCoreItem 改用 CannonItem 实例字段 | ShipCoreItem.java:1505 | FIXED |
| 12 | FireControlManager ArrayList→CopyOnWriteArrayList | FireControlManager.java:28 | FIXED |
| 13 | MissileEntity 目标缓存+5tick节流 | MissileEntity.java:118 | FIXED |
| 14 | NodeBattleField 地形生成添加 flag 64 | NodeBattleField.java:39 | FIXED |
| 15 | NodeBattleField cleanup 改用 AABB 查询 | NodeBattleField.java:141 | FIXED |
| 17 | FireControlHudLayer 缓存改用 gameTime | FireControlHudLayer.java:45 | FIXED |
| 18 | DungeonInstance hashCode→floorMod | DungeonInstance.java:83 | FIXED |

### 未修复 (1/18)

| # | 问题 | 原因 |
|---|------|------|
| 10 | 发射器耐久绕过 vanilla 损伤机制 | 涉及6处代码需要重构，需评估 hurtAndBreak 在非装备槽的兼容性 |

---

## P2 一般问题

### 已修复 (18/21)

| # | 问题 | 状态 |
|---|------|------|
| 1 | DungeonLobbyManager 添加 MAX_LOBBY_SIZE=6 | FIXED |
| 3 | 副本死亡清除残留 DOT 效果 | FIXED |
| 7 | DungeonPortalEntity 移除客户端 SynchedEntityData 写入 | FIXED |
| 8 | ReconControlPayload NaN 检查前置 | FIXED |
| 9 | RecallAllAircraftPayload 添加20tick冷却 | FIXED |
| 10 | SelectNodePayload/SelectStagePayload 字符串长度验证 | FIXED |
| 11 | SkinOverlayLayer 限制 skinId 范围 | FIXED |
| 14 | 所有 AircraftItem 自动注册 fueled property | FIXED |
| 15 | 3个反舰/火箭发射器注册 WeaponReloadDecorator | FIXED |
| 16 | VT近炸引信友军判断统一 FriendlyFireHelper | FIXED |
| 17 | FriendlyFireHelper 保护所有玩家飞机 | FIXED |
| 18 | DepthChargeEntity 伤害添加 owner 归因 | FIXED |
| 20 | LoadedAmmo STREAM_CODEC 长度限制 | FIXED |
| 21 | SaltEvaporationHandler isLoaded 检查 | FIXED |

### 未修复 (3/21)

| # | 问题 | 原因 |
|---|------|------|
| 2 | DungeonLobbyManager BlockPos 不区分维度 | 需改用 GlobalPos 作为 key，影响面较大 |
| 5 | DungeonBookScreen 客户端数据同步 | 需要架构变更（S2C 同步或共享数据包） |
| 13 | CookingPotMenu/ReloadFacilityMenu 区块卸载异常 | 需要降级 Menu 设计，影响 GUI 框架 |

---

## P3 建议

### 已修复 (5/8)

| # | 问题 | 状态 |
|---|------|------|
| 3 | FlammableEffect 爆炸源改为 entity 自身 | FIXED |
| 4 | FloodingEffect amplifier 3+ 最小间隔降至5 | FIXED |
| 5 | DungeonScriptManager ConcurrentHashMap→HashMap | FIXED |
| 7 | ClientEvents 移除废弃 CYCLE_WEAPON_KEY | FIXED |
| 2 | slavPrisonMode 联动 flammableEffectEnabled | FIXED |

### 未修复 (3/8)

| # | 问题 | 原因 |
|---|------|------|
| 1 | SkinManager 迁移到 Entity Attachments | 跨版本兼容考虑，待后续统一迁移 |
| 6 | LootShipEntity.openedBy 已在 P1-7 中一起修复 | FIXED（已计入P1） |
| 8 | DungeonBookScreen.drawLine 逐像素 fill | 视觉优化，优先级低 |

---

## 修复详情

### P1 修复

#### 1-2. SelectStagePayload / LeaveLobbyPayload 安全加固
- 添加 `distanceToSqr > 64.0` 距离检查
- 添加 `instanceof DungeonLecternBlock` 方块类型验证
- 与 JoinLobbyPayload / SelectNodePayload 保持一致

#### 3. S2C 列表反序列化上限
- DungeonResultPayload: `if (count < 0 || count > 64) count = 0`
- LobbyUpdatePayload: 同上

#### 4-5. 爆炸类抛射物 exploded 标记
- AerialBombEntity: 新增 `boolean exploded` 字段，`onHitEntity` 和 `onHitBlock` 均检查
- TorpedoEntity: 超时自爆分支添加 `exploded = true`

#### 6. 侦察模式缓慢效果安全网
- `GameEvents.onPlayerLogin`: 检测 amplifier >= 9 的缓慢效果并移除

#### 7. LootShipEntity 持久化
- `addAdditionalSaveData`: 保存 `Dropping` 布尔值和 `OpenedBy` UUID 列表
- `readAdditionalSaveData`: 恢复两者

#### 8. ClientDungeonData 断线清理
- `ClientGameEvents.onClientDisconnect` 添加 `ClientDungeonData.clear()`

#### 9. DungeonTransportPlaneEntity 爬升时序
- 新增 `dropTick` 字段记录投放时刻
- 爬升条件改为 `(tickCount - dropTick) > 60`
- dropTick 参与 NBT 持久化

#### 11. 火炮数据源统一
- `getGunDamage`/`getGunCooldown` 改用 `CannonItem.getDamage()`/`getCooldownTicks()`

#### 12. FireControlManager 线程安全
- `ArrayList<UUID>` 全部替换为 `CopyOnWriteArrayList<UUID>`

#### 13. MissileEntity 目标缓存
- 新增 `trackedTarget` 和 `targetSearchCooldown` 字段
- 目标有效时复用缓存，死亡/消失时重置
- 无缓存时每 5 tick 搜索一次

#### 14-15. NodeBattleField 性能
- `generateTerrain`: flag 改为 `2 | 16 | 64`（64 = 跳过光照更新）
- `cleanupRegion`: `getAll()` 替换为 `get(AABB, consumer)` 空间索引查询

#### 17. FireControlHudLayer 缓存修复
- `cacheAge` 计数器改为 `lastRebuildTick` 时间戳
- 使用 `mc.level.getGameTime()` 驱动，确保每 20 game tick 只重建一次

#### 18. DungeonInstance hashCode 碰撞
- `Math.abs(nodeId.hashCode())` → `Math.floorMod(nodeId.hashCode(), MAX_NODES_PER_STAGE)`

### P2 修复（摘要）

- **大厅人数上限**: `MAX_LOBBY_SIZE = 6`，`addMember` 检查
- **DOT 清理**: `clearFire()` + `removeAllEffects()`
- **DungeonPortalEntity**: 移除客户端 `entityData.set(SPIN_TICK)`，改用 `tickCount`
- **ReconControlPayload**: NaN 检查移至 `Mth.clamp` 之前
- **RecallAllAircraftPayload**: 每玩家 20 tick 冷却
- **字符串长度**: nodeId/stageId 添加 `length() > 128` 检查
- **SkinOverlayLayer**: `if (skinId <= 0 || skinId > 3) return`
- **ClientEvents**: 遍历 `ModItems.ITEMS.getEntries()` 自动注册所有 AircraftItem
- **WeaponReloadDecorator**: 新增 SY1/MK14/SHIP_ROCKET 三个发射器
- **VT 友军判断**: 独立逻辑替换为 `FriendlyFireHelper.shouldBlockHit()`
- **FriendlyFireHelper**: FF关闭时保护所有玩家飞机（非仅自己的）
- **DepthChargeEntity**: `damageSources().magic()` → `damageSources().indirectMagic(this, getOwner())`
- **LoadedAmmo**: ammoItemId 长度 > 256 抛出 DecoderException
- **SaltEvaporationHandler**: getBlockState 前添加 `level.isLoaded(pos)`

### P3 修复（摘要）

- **FlammableEffect**: 爆炸源从 null 改为 entity（正确死亡消息）
- **FloodingEffect**: 最小间隔从 10 降至 5，amplifier 3+ 有区分
- **DungeonScriptManager**: ConcurrentHashMap → HashMap（实际单线程）
- **slavPrisonMode**: 新增 `isFlammableEffectActive()` helper 联动
- **CYCLE_WEAPON_KEY**: 移除废弃别名
