# 副本模块代码审查修复 (2026-04-18)

模块：`com.piranport.dungeon`（45 文件 / ~4913 行）
范围：大厅 / 副本实例 / 节点战斗 / 传送门 / 箱子船 / 钥匙 / 回城卷轴 / 剧本 / 客户端 HUD 与 GUI / 网络协议 / 持久化

共 30 项修复。下面按等级与全局序号列出。

## P0（5 项 — 物品/经济安全 + 持久化）

### 1. TownScrollUsePayload 物品错配 + 无来源校验
- 文件：`network/TownScrollUsePayload.java`、`item/TownScrollItem.java`、`DungeonConstants.java`
- 修复：在 `TownScrollItem.use` 服务端分支记录玩家右键的 slot + 10 秒过期时间的 intent；payload 处理时强制要求该 intent 存在，并优先消耗记录的 slot；找不到活跃 instance 时不消耗 intent，避免 intent 被错误消费。

### 2. ReviveRequestPayload 先扣末影/复活图腾后判 instance
- 文件：`network/ReviveRequestPayload.java`
- 修复：先完成"找到 instance + currentNode + dungeonLevel + 背包有图腾"的全部前置校验，再 shrink 图腾。任何前置失败都给玩家系统消息并重新弹出 ReviveScreen。

### 3. 节点重入与旗手身份持久化（合并 P0-3 + P0-6）
- 文件：`instance/DungeonInstance.java`、`instance/DungeonInstanceManager.java`、`network/SelectNodePayload.java`、`event/DungeonEventHandler.java`、`event/DungeonNodeRouter.java`
- 修复：
  - `DungeonInstance` 新增 `flagshipUuid` 字段并持久化（NBT `FlagshipUuid`）。
  - `createInstance` 把 flagship UUID 写入 instance。
  - `SelectNodePayload` 优先校验 `instance.getFlagshipUuid()`，与 lobby 是否存在解耦；lobby 不存在也能继续推进。
  - `advanceNode` 改为幂等：`if (clearedNodes.contains) return false`，返回布尔表示是否真的推进。
  - `handleResourceNode` / `handleCostNode` 入口先调 `advanceNode` 原子地"占有"节点，再发奖励/扣物品；返回 false 直接退出，杜绝双发包刷物品。

### 4. SelectStagePayload 防止副本进行中改 stageId
- 文件：`network/SelectStagePayload.java`
- 修复：写入 stageId 前判断 `getInstanceId(keyStack) == null`；非空发回 `dungeon.piranport.stage_locked` 提示并 return。

### 5. DungeonScriptManager 固定使用 overworld dataStorage
- 文件：`script/DungeonScriptManager.java`
- 修复：移除"dungeon dim 优先 + overworld 回退"的双 dataStorage 路径，统一使用 `server.overworld()`，与 `DungeonInstanceManager` 一致。避免 dim 加载顺序不同导致两份 SavedData 互不可见、剧本 phase 跨重启丢失。

## P1（7 项 — 玩法严重 bug）

### 6. （并入 #3）旗手校验持久化
见 P0-3。

### 7. DungeonInstance 永不清理
- 文件：`instance/DungeonInstanceManager.java`、`GameEvents.java`
- 修复：新增 `sweepLeaks(dungeonLevel)`：每 12000 tick（10 分钟）promote pending freed indices；扫描 SUSPENDED 超过 24 小时的 instance 自动 cleanup（含 cleanupRegion）。`GameEvents.onServerTick` 已接入定时调用。

### 8. DungeonPortalEntity 在 instanceId 缺失时永驻
- 文件：`entity/DungeonPortalEntity.java`
- 修复：tick 入口若 `instanceId == null` 或 instance 已被清理，直接 discard；新增 `ORPHAN_DESPAWN_TICKS = 12000`（10 分钟无人靠近自销）。

### 9. LootShipEntity 跨实例 entity UUID 错位
- 文件：`instance/NodeBattleField.java`、`instance/DungeonInstanceManager.java`
- 修复：
  - `cleanupRegion` 新增显式按类型 discard `DungeonPortalEntity` / `LootShipEntity`，扫描时排除 `ServerPlayer`。
  - `cleanupInstance` 释放的 instanceIndex 进入 `pendingFreedIndices`，下一次 `sweepLeaks` 才 promote 到 `freedIndices`，避免新 instance 立刻复用同 region 区域捡到旧 crate UUID。

### 10. onPlayerDeath 移除全部药水效果
- 文件：`event/DungeonEventHandler.java`
- 修复：`removeAllEffects()` 改为只移除 `MobEffectCategory.HARMFUL` 的 effect，保留装填加速、规避加成、食物 buff 等玩法 buff，符合 v0.0.5 Combat+ 设计。

### 11. DungeonRegistrySyncPayload 整张图 JSON + 主线程解析
- 文件：`network/DungeonRegistrySyncPayload.java`
- 修复：
  - STREAM_CODEC 显式写 length 前缀 + 4 MiB 上限（`MAX_JSON_BYTES`），超限抛 `EncoderException` / `DecoderException`。
  - JSON 解析在 `STREAM_CODEC.decode`（netty IO 线程）完成，结果挂在 record 字段 `parsedChapters` / `parsedStages` 上；`handle` 只做轻量 set，不阻塞客户端 game thread。
  - 老接收者的兼容 fallback：当 parsed 字段为 null 时仍走同步解析。

### 12. EnemySet 缺失实体回退 ZOMBIE 卡死节点
- 文件：`instance/NodeBattleField.java`
- 修复：
  - `createEntity` 缺失实体类型时返回 null，不再生成无 dungeon 标签的 placeholder。
  - `spawnEnemies` 检测到 `enemySet == null` / 配了 flagship 但全部创建失败 / 无 flagship 且 spawnList 全失败时，立即调用新加的 `spawnCompletionPortal` 让玩家通过节点。
  - 普通 spawnList entity 也补打 `dungeon_instance_*` / `dungeon_node_*` 标签，便于 cleanupRegion 区分。

## P2（9 项 — 体验问题）

### 13. onPlayerLogout 早退导致副本外掉线 instance 不挂起
- 文件：`event/DungeonEventHandler.java`
- 修复：移除 `if (!isInDungeon) return`，改为统一遍历背包钥匙；同时若被移除者是 flagship，调用 `pickNextFlagshipPlayer` 选下一个在线成员，把 instanceId / stageId 写入新 flagship 的 key（接入第 19 项 transferKey 思路），并发送系统消息提示。

### 14. lecternDimension 还原失败静默回退
- 文件：`event/DungeonEventHandler.java`
- 修复：`teleportToLectern` 在 `ResourceLocation.tryParse` 失败 / `getLevel` 返回 null 时打 warn 日志，再回退 overworld。

### 15. fillCrateSupplies 极端时序覆盖玩家放入物品
- 文件：`entity/LootShipEntity.java`
- 修复：新增独立 `filled` 标志（与 `lootGenerated` 解耦）+ NBT 持久化 `Filled`；`fillInventory` 入口判 `filled` 直接 return；填充时只填 `inventory.getItem(i).isEmpty()` 的 slot。

### 16. DungeonLecternBlock 跨 lobby 幻影成员
- 文件：`block/DungeonLecternBlock.java`
- 修复：右键 lectern 加入新 lobby 前调 `findLobbyOf(player)`，若已有不同的 lobby 先 leaveLobby + broadcastLobbyUpdate。

### 17. nodeId 多字符用 hashCode 高碰撞
- 文件：`data/StageData.java`、`instance/DungeonInstance.java`、`data/DungeonDataLoader.java`
- 修复：
  - `StageData` 新增 `nodeIndexOf(nodeId)`：基于 `TreeMap` 的字典序确定性 index，缓存到全局 `INDEX_CACHE`。
  - `DungeonInstance.getNodeSpawnPos` 优先用 `stage.nodeIndexOf`，找不到 stage 时才回退到老的首字母映射。
  - DataLoader 重载时调用 `StageData.invalidateCaches()` 清缓存。

### 18. DungeonScriptManager 每 tick setDirty
- 文件：`script/DungeonScriptManager.java`、`script/DungeonScript.java`、`script/ArtilleryIntroScript.java`
- 修复：
  - `DungeonScript.tick` / `onEntityDeath` 接口签名改为返回 `boolean`，表示是否真的有持久化状态变更。
  - `tickAll` 累计 `changed` 标志，仅 changed 时 setDirty。
  - `ArtilleryIntroScript` 全部 phase tick 方法返回 boolean，phase 切换 / 内部状态变化才返回 true。

### 19. FlagshipManager.transferKey 未接入
- 文件：`event/DungeonEventHandler.java`
- 修复：见第 13 项。flagship logout 时 stamp instanceId / stageId 到新 flagship 的钥匙上。原 `FlagshipManager.transferKey` 保留（其他场景仍可用）。

### 20. DungeonBookMenu.removed 总是 leaveLobby
- 文件：`menu/DungeonBookMenu.java`
- 修复：`removed()` 不再自动调用 `leaveLobby`。玩家通过显式按钮（`LeaveLobbyPayload`）或 logout 才离开 lobby。多人组队 ESC 不再解散 lobby。

### 21. LobbyUpdatePayload size 越界静默归零
- 文件：`network/LobbyUpdatePayload.java`
- 修复：size < 0 或 > 64 直接 throw `DecoderException`，让网络层断开连接，避免后续 string 读取错位。

## P3（9 项 — 代码质量 + 重构）

### 22. DungeonEventHandler 494 行职责过多 → 拆分 DungeonNodeRouter
- 文件：新增 `event/DungeonNodeRouter.java`，缩减 `event/DungeonEventHandler.java`
- 修复：把 `enterNode / handleResourceNode / handleCostNode / handleBattleNode / handleScriptedBattleNode / prepareBattleNode / countItem / removeItems` 全部迁出到 `DungeonNodeRouter`。`DungeonEventHandler.enterNode` 仅作转发，文件聚焦于 lifecycle 事件。

### 23. RewardEntry / CostEntry 反复 BuiltInRegistries 查表
- 文件：`data/NodeData.java`
- 修复：`RewardEntry` / `CostEntry` 新增 `resolvedItem()` 方法封装 `ResourceLocation.tryParse + BuiltInRegistries.ITEM.getOptional`。Cost 节点修改后只在循环里调 `resolvedItem()` 一次。

### 24. RewardDispatcher 抽出
- 文件：新增 `event/RewardDispatcher.java`
- 修复：统一处理 chance roll / item 查表 / count 构造 / inventory add / drop / 翻译名收集。`completeDungeon` 与 resource node handler 都改为调 `RewardDispatcher.give`。

### 25. advanceNode 缺幂等保护
- 文件：`instance/DungeonInstanceManager.java`
- 修复：见第 3 项。

### 26. 切维度后 HUD 计时器跑飞
- 文件：`client/DungeonHudLayer.java`
- 修复：`render` 入口检查本地玩家 `level().dimension()`，如不在 `DUNGEON_DIMENSION` 直接 `clearDungeonState` 并 return。

### 27. LootShipEntity 自实现 Slot{i} 序列化
- 文件：`entity/LootShipEntity.java`
- 修复：改用 `ContainerHelper.saveAllItems` / `ContainerHelper.loadAllItems` + `NonNullList`，与原版一致，components / mod-item 兼容。

### 28. StageData.getReachableFrom 每次重建
- 文件：`data/StageData.java`
- 修复：新增静态 `ADJ_CACHE`，首次 `getReachableFrom(stageId)` 时构建 `Map<from, Set<to>>` 并缓存。`invalidateCaches()` 在 DataLoader 重载时清空。

### 29. onEntityDeath 无脑 setDirty
- 文件：`script/DungeonScript.java`、`script/DungeonScriptManager.java`、`script/ArtilleryIntroScript.java`
- 修复：见第 18 项。`onEntityDeath` 返回 boolean，manager 仅 changed 时 setDirty。

### 30. DungeonInstance.getPlayerUuids 反复 Set.copyOf
- 文件：`instance/DungeonInstance.java`
- 修复：`getPlayerUuids` / `getClearedNodes` 改为 `Collections.unmodifiableSet`，避免热路径的 copy 开销。

## 配套改动

- 新增翻译条目：`dungeon.piranport.stage_locked` / `revive_unavailable` / `revive_no_totem` / `flagship_promoted`（zh_cn / en_us 同步）。
- 新增文件：`event/DungeonNodeRouter.java`、`event/RewardDispatcher.java`。
- DungeonRegistrySyncPayload 协议升级（length-prefixed 字节而非 STRING_UTF8），与旧客户端不兼容；alpha 阶段可接受。

## 验证

- `./gradlew compileJava` ✅ BUILD SUCCESSFUL（仅 4 个无关的 NeoForge 1.22 待删 API 警告）。
- 不影响 NPC / 美食 / 装备 / 渲染等其他模块。
