# 代码审查修复记录 (2026-03-31)

**背景**: 全面代码审查（5个并行深度审查代理，94个Java文件），共修复 35 个问题。

**总计**: P0 (5) + P1 (11) + P2 (12) + P3 (7) = **35个** — 全部 DONE ✅

---

## P0 修复清单 (5个 — 必须立即修复的安全漏洞)

| # | 类型 | 文件 | 问题 | 修复 |
|---|------|------|------|------|
| 1 | 内存泄漏 | `AircraftEntity.java` | 侦察机 `setChunkForced()` 加载区块，多个删除路径（`/kill`、崩溃）跳过清理，导致永久泄漏 | 重写 `remove(RemovalReason)`，释放强制加载区块 + 清理侦察状态 + 移除减速效果；`lastForcedChunkX/Z` 持久化 NBT |
| 2 | 多人同步 | `FlammableEffect.java` | `explosionTimer` 是实例变量，`MobEffect` 单例导致所有玩家共享爆炸频率 | 移除实例计数器，改用 `entity.tickCount % 39 == 0` |
| 3 | 输入校验 | `ReconControlPayload.java` | 客户端 `dx/dy/dz` 无范围校验，恶意客户端可发送 `Float.MAX_VALUE` 或 `NaN` 瞬移飞机 | `Mth.clamp(-1,1)` + `Float.isNaN` 检查 |
| 4 | OOM 攻击 | `FlightGroupData.java` | StreamCodec 反序列化无大小上限，恶意包可发送 `size=Integer.MAX_VALUE` 触发崩溃 | groups/indices/bullets/payloads 数组加上限校验；`AttackMode` 加 ordinal 边界防护 |
| 5 | 永久锁定 | `GameEvents.java` | 侦察机减速效果 duration=`Integer.MAX_VALUE`，若实体被 `/kill` 则玩家永久无法移动 | 每20tick检查：有 amplifier≥9 减速但无活跃侦察机时自动移除 |

---

## P1 修复清单 (11个 — 物品丢失/数据验证/网络安全)

| # | 类型 | 文件 | 问题 | 修复 |
|---|------|------|------|------|
| 6 | 物品丢失 | `AircraftEntity.java` | GUI模式返航直接覆盖武器槽，飞行期间新放入的武器被静默覆盖 | 检查槽位是否为空，被占用则放入玩家背包 |
| 7 | 数据验证 | `FlightGroupUpdatePayload.java` | `slotIndices` 未验证范围，恶意包可注入 index=999 | 验证每个 index ∈ [0, weaponSlots) |
| 8 | 边界保护 | `FlightGroupData.java` AttackMode | 已在P0#4中修复 | — |
| 9 | 异常安全 | `CannonProjectileEntity.java` | AP弹 `hurt()` 异常时 modifier 永久残留 | try-finally 保护 modifier 清理 |
| 10 | 距离限制 | `FireControlPayload.java` | 火控锁定无距离验证 | 限制在模拟距离内（simDist × 16 格） |
| 11 | TOCTOU | `ShipCoreItem.java` | 鱼雷先发射实体后扣弹药 | 先消耗弹药确认成功再生成实体 |
| 12 | 状态振荡 | `AircraftEntity.java` | 死亡目标UUID残留导致CRUISING/ATTACKING循环振荡 | 转ATTACKING前验证锁定目标存活 |
| 13 | 维度泄漏 | `GameEvents.java` | 维度切换不清理火控/侦察状态 | 新增 `PlayerChangedDimensionEvent` 监听器 |
| 14 | 绕过防护 | `EvasionHandler.java` | 高速规避可闪避 `/kill`、虚空伤害 | 排除 `BYPASSES_INVULNERABILITY` 和 `STARVE` |
| 15 | 配方破坏 | `CookingPotRecipe.java` | 超集匹配 | 改为精确匹配 `available.size() == ingredients.size()` |
| 16 | 权限泄漏 | `SnapshotRequestPayload.java` | 无权限校验 | 添加 `hasPermissions(2)` 检查 |
| 21 | 物品丢失 | `AircraftEntity.java` findCoreStack | 无GUI模式核心找不到 | 添加全背包遍历兜底 |

---

## P2 修复清单 (12个 — 持久化/生命周期/null安全/网络防护)

| # | 类型 | 文件 | 问题 | 修复 |
|---|------|------|------|------|
| 17 | 持久化 | `AircraftEntity.java` | `airtimeTicks` runtime字段，区块卸载后重置 | 持久化到NBT |
| 18 | 持久化 | `AircraftEntity.java` | `hasFired` 反序列化后重置，可重复投弹 | 持久化到NBT |
| 19 | 生命周期 | `AerialBombEntity/BulletEntity` | 无lifetime限制 | 添加600tick上限 |
| 20 | 持久化 | `AircraftEntity.java` | `aircraftHealth` 默认值与设计值不一致 | 持久化到NBT |
| 22 | 配方逻辑 | `CookingPotRecipe/BlockEntity` | 堆叠物品不匹配 | 基于计数匹配 |
| 23 | 数值错误 | `PlaceableFoodBlockEntity` | Buff时长除数错误 | 除数改为 `totalServings` |
| 24 | Null安全 | `CookingPotMenu.java` | fromNetwork 无null检查 | 添加instanceof检查 |
| 25 | 输入验证 | `OpenFlightGroupPayload` | 无coreSlot验证 | 验证范围+物品类型 |
| 26 | 网络防护 | `FireControlSyncPayload` | UUID列表无size上限 | 添加 `size > 16` 上限 |
| 27 | 客户端清理 | 多个客户端类 | 静态状态跨服务器残留 | LoggingOut事件清理 |
| 28 | 范围错误 | `ShipCoreItem.java` | 弹药搜索包含强化槽 | 改为 `i < weaponSlots + ammoSlots` |
| 29 | 数据丢失 | `AircraftEntity.java` | buildReturnStack丢失自定义数据 | 序列化完整originalStack到NBT |

---

## P3 修复清单 (7个 — 性能优化/代码质量)

| # | 类型 | 文件 | 问题 | 修复 |
|---|------|------|------|------|
| 30 | 性能 | `AircraftEntity.java` | 无锁定时每tick做AABB查询 | `autoSeekCooldown` 每20tick一次 |
| 31 | 性能 | `AircraftEntity/ShipCoreItem` | 弹药匹配用字符串比较 | 缓存Item引用直接 `==` |
| 32 | 性能 | `FireControlHudLayer` | 每帧遍历全部实体 | UUID→Entity 缓存 |
| 33 | 性能 | `ClientTickHandler` | List O(n) contains | 转 HashSet O(1) |
| 34 | 代码质量 | 6处重复 | 变身检测逻辑重复 | 抽取 `TransformationManager.findTransformedCore()` |
| 35 | 代码质量 | `FireControlManager/ReconManager` | 单线程用ConcurrentHashMap | 改 HashMap |
| 36 | 代码质量 | `TransformationManager` | 硬编码物品比较链 | IdentityHashMap 查表 |
