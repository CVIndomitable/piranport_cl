# 皮兰港 v0.0.11-alpha 代码与玩法审查报告

**审查日期**：2026-04-17
**审查范围**：269 个 Java 文件，覆盖 aviation、weapons、dungeon、ruins、ship core、NPC/AI、skin、events、config、worldgen、lang
**审查策略**：三层递进（风格 → 常规 → 深度），对 P0/P1 问题做根因分析
**版本基线**：NeoForge 21.1.220 / Minecraft 1.21.1 / Java 21

---

## 问题统计

| 等级 | 数量 | 说明 |
|------|------|------|
| P0（严重） | 1 | 阻塞性玩法 bug |
| P1（高） | 2 | 状态丢失 / 资源泄漏 |
| P2（中） | 4 | 逻辑瑕疵 / 性能 |
| P3（低） | 6 | 清洁 / 一致性 |
| 风格 | 5 类 | 翻译完整性通过（629/629） |

---

## P0：严重问题

### P0-1 | Dungeon 脚本不持久化，服务器重启丢失副本进度

- **位置**：`dungeon/script/DungeonScriptManager.java:18`
- **证据**：`ACTIVE_SCRIPTS` 是纯静态 `HashMap`；项目已有 SavedData 范式（`DungeonSavedData`、`DungeonLeaderboard`、`FleetGroupManager`），但 script 状态未加入
- **后果**：玩家处于 `ArtilleryIntroScript` 的 `AIRDROP`/`LOOTING`/`BATTLE` 阶段时服务器重启 → 实例仍存在（由 `DungeonInstanceManager` 持久化），但脚本消失 → 玩家被困副本无法推进，只能用 `/ppd` 命令或回城卷轴跳出
- **修复思路**：
  1. 为 `DungeonScript` 添加 `writeNbt(CompoundTag)` / `readNbt(CompoundTag)` 抽象方法
  2. 用 `SavedData` 持久化 `ACTIVE_SCRIPTS`，用 stage registry 名字做 factory 映射
  3. 或者简化：服务器启动时遍历 `DungeonInstance` 列表，根据 `selectedStageId` 重建 script

---

## P1：高优先级

### P1-1 | DeepOceanProjectileEntity 追踪状态不持久化

- **位置**：`entity/DeepOceanProjectileEntity.java`（184 行）
- **证据**：`trackingTargetId` 和 `pastApex` 字段存在于内存，但 `addAdditionalSaveData` / `readAdditionalSaveData` 未写入。且 `trackingTargetId` 是 int（entity ID，不是 UUID），重启后该 ID 无意义
- **后果**：`PARABOLIC_TRACKING` 弹道在飞行途中 save/reload → 退化为普通抛物线弹，爆炸点偏离
- **修复思路**：存 UUID 而非 int ID，tick 时通过 `ServerLevel.getEntity(UUID)` 回查。找不到时降级成 PARABOLIC

### P1-2 | AircraftEntity.reconForcedChunks 读档未恢复 Set

- **位置**：`entity/AircraftEntity.java`（2062 行，write ~2042-2047，read ~1996-2004）
- **证据**：NBT 写入时保存了 chunk 列表；读取时只调用 `forceChunk(..., false)` 释放旧块，未把 chunk key 重新 add 到内存 Set
- **后果**：Set 状态与实际 force-load 状态脱节；最终无资源泄漏但逻辑偶然正确，读起来让人困惑
- **修复思路**：要么读档时把 key 加回 Set，要么注释说明"read 时有意只释放不恢复"

---

## P2：中优先级

### P2-1 | MissileEntity.manualTarget 字段形同"死状态"

- **位置**：`entity/MissileEntity.java`（385 行）
- **证据**：字段在 `setTrackedTarget()`（~line 105）被设置、`tick()` 中被重置（~line 165、171）、NBT 持久化（~line 362、380），但 `tick()` 的自动搜索逻辑**从未读取此字段**
- **后果**：手动锁定失败后导弹依然自动搜索新目标，与"手动制导"语义不一致

### P2-2 | GameEvents.recallAircraftForPlayer 用 getAllEntities() 扫描全世界

- **位置**：`GameEvents.java:480` 附近
- **证据**：`PlayerDeathEvent` / `PlayerLoggedOutEvent` / `PlayerChangedDimensionEvent` 都遍历目标维度所有实体
- **后果**：大型生存服务器（>10000 实体）时卡顿。推荐按 ownerUuid 索引（`FireControlManager` 已有此模式）

### P2-3 | TorpedoEntity.sourceAircraftName 不持久化

- **位置**：`entity/TorpedoEntity.java`（443 行）
- **后果**：鱼雷飞行中存档 reload → 命中时击落归因消息显示空机型名

### P2-4 | 公式哈希值碰撞隐患

- **位置**：`GameEvents.java:233`
- **证据**：`cacheKey = (weaponLoad + armorLoad) * 1000 + maxLoad + (int)(engineBonus * 10000)`
- **后果**：极罕见参数组合下产生同一 key；推荐用 `Objects.hash` 或 `long`

---

## P3：低优先级

### P3-1 | 混用 Math.random() 而非 level.random / mob.getRandom()

- **位置**：`dungeon/event/DungeonEventHandler.java:166`、`dungeon/instance/NodeBattleField.java:85-86`、`dungeon/script/ArtilleryIntroScript.java:302-303`
- **后果**：破坏 Minecraft 随机数一致性

### P3-2 | ArtilleryIntroScript.java:85 使用 new Random()

- 同类问题

### P3-3 | 登出未清理大厅成员

- **位置**：`dungeon/lobby/DungeonLobbyManager.java`（135 行）和登出钩子
- **证据**：Logout 事件处理飞机回收 + 实例暂停，**不调用** `DungeonLobbyManager.leaveLobby()`
- **后果**：玩家在讲台大厅登出 → 名字仍显示在大厅列表

### P3-4 | SkinManager 使用 legacy getPersistentData()

- **位置**：`skin/SkinManager.java:17,22,27`
- **后果**：作者已注释 TODO 迁移到 NeoForge Entity Attachments

### P3-5 | SubmergeGoal waterSurfaceY 遗留变量

- **位置**：`npc/ai/goal/SubmergeGoal.java:71`
- 不影响功能

### P3-6 | 潜艇水下发射鱼雷（设计行为，非 bug）

- 与 `feedback_submarine_torpedo` 一致

---

## 风格问题

| # | 问题 | 位置 |
|---|------|------|
| S1 | 通配符 import | `aviation/ClientAswSonarData.java:6`、`ammo/AmmoRecipeRegistry.java:7`、`CommonEvents.java:9`、8 个 `DeepOcean*Entity.java`、`ArtilleryIntroScript.java:25`、`B25Model.java:10`、`F4FModel.java:10`、`DungeonRegistrySyncPayload.java:3,5,13` |
| S2 | 遗留 TODO | `skin/SkinManager.java:11`、`item/ShipCoreItem.java:1508` |
| S3 | Tech debt 注释 | `entity/AircraftEntity.java:1115-1118` |
| S4 | 翻译完整性 | zh_cn.json 和 en_us.json 各 629 keys，零缺失零空值 — **通过** |
| S5 | ModStructures 空类 | `worldgen/ModStructures.java:1-25`（.nbt 模板未到位） |

---

## 亮点

- ✅ 翻译完整性 100%（629/629 keys 中英文对齐）
- ✅ `FriendlyFireHelper`、`FireControlManager`、`FleetGroupManager`、`ReconManager` 切面类设计干净、ConcurrentHashMap 线程安全
- ✅ `ParabolicCalculator` / `TrackingCalculator` 有 NAV_CONSTANT、最大转向率、最小飞行时间守护，弹道数学严谨（Rodrigues' rotation + 比例导航）
- ✅ `AbstractDeepOceanEntity.die()` 40 tick 沉没动画衔接得体
- ✅ `AircraftEntity` 动态返航距离根据 `server.simulationDistance` 自适应
- ✅ enum valueOf 三处都包了 `IllegalArgumentException` catch（兼容存档删改）
- ✅ Config 使用 NeoForge ModConfigSpec 规范 + helper 方法实现"模式叠加"优雅

---

## 玩法合理性评估

- ✅ 舰载机编队、火控、侦察机视角切换、副本大厅/旗舰/传送门/回城/JEI、武器冷却条、友军防护、彩色描边、击落归因 — 完整闭环
- ✅ 深海 NPC 九型（驱/轻/重/战巡/战/轻航/航/潜/补）AI 含集群警戒、追踪炮、鱼雷齐射、航空轰炸；`canSubmerge()` 隐身逻辑正确
- ✅ 四类遗迹（传送门/补给站/前哨站/深海基地）数据驱动结构 + `RuinDegradationProcessor` 70/30 降级 + `LootChestProcessor` 战利品注入
- ⚠️ 主要风险点是 **P0-1（服务器重启丢进度）**，单机影响小，服务器影响严重

---

## 优先修复建议

1. **立即修复**：P0-1 `DungeonScriptManager` 持久化
2. **近期修复**：P1-1 `DeepOceanProjectileEntity` 存 UUID；P1-2 `AircraftEntity.reconForcedChunks` 读档逻辑显式化
3. **计划重构**：
   - P2-1 `MissileEntity.manualTarget` 确认意图，实现 `&& !manualTarget` 检查或删掉死字段
   - P2-2 `recallAircraftForPlayer` 改为按 owner 索引
4. **清洁/对齐**：P2-3 鱼雷 sourceName 持久化；P3-1~P3-2 统一随机数源；P3-3 登出清理大厅；P3-4 SkinManager 迁移到 Entity Attachments；S1 清理通配符 import
