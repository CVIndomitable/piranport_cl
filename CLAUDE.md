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

## Version & Documentation

- **当前版本**: v1.1.49-dev (测试版)
- **稳定版本**: v1.0.0 (main 分支，仅修 bug)
- **开发策略**: main = 稳定版，dev = 测试版新玩法
- **版本路线图**: `../docs/皮兰港 版本路线图.md`
- **技术参考手册**: `../docs/皮兰港技术参考手册.md` — 完整目录结构、API 坑、核心系统详解、配置系统
- **更新日志**: `../docs/版本记录/CHANGELOG-1.1.6-dev.md`
- **开发工具**: `../docs/开发工具/` — Excel数据表管理工具等
- **问题排查**: `../docs/troubleshooting/` — 已知问题与调查记录

---

## Conventions & Rules

### 策划决策优先级（最高优先级规则）
- **一切策划决策以 `../docs/策划决策/` 为准**。该目录已覆盖 NPC / 版本策略 / 版本分支 / 工艺 / 海图 / 航空 / 火控 / 架构 / 兼容 / 舰装 / 迁移 / 深海 / 食物 / 数值 / 数值配置 / 通用 / 武器 / 消耗品 / 遗迹 / 资源 / 战斗 等分类。
- 若 `../docs/策划决策/` 中已存在对应决策，**无需向用户提问，直接按决策执行**。
- 仓库其他位置的文档（如 `docs/皮兰港技术参考手册.md`、`文档/总策划案*.md`、模组教程、代码注释、README 等）若与 `docs/策划决策/` 冲突，**一律以 `docs/策划决策/` 为准**。

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
- **MC 1.21.1 配方格式**: 所有配方 `key`/`ingredients`/`ingredient` 必须用 `{item:...}` 对象，不接受裸字符串（1.21.2+ 才支持）
- **MC 1.21.1 数据目录名单数**: 1.21 起数据包目录为单数——配方必须放 `data/<ns>/recipe/`（不是 `recipes/`），同理 `loot_table/`、`advancement/`、`structure/`。目录名错误时整目录静默不加载（无报错，仅 Patchouli 之类引用方会报 Recipe not found）
- **物品模型**: 注册物品时必须主动创建 `models/item/*.json` 模型文件，不能只放贴图
- **注释语言**: 全部使用中文注释，保持中文团队维护一致性。复杂逻辑必须注释 WHY 而不只是 WHAT
- **村民职业**（经济/02）: 三职业在 `registry/ModVillagerProfessions`，交易表在 `handler/VillagerTradeHandler`，不往原版职业塞模组商品。新 POI 必须同时登记 `data/minecraft/tags/point_of_interest_type/acquirable_job_site.json`，否则无业村民不认领。战利品 `hentai_trophy` 只在支付侧，交易补货用 `TROPHY_USES`（低于绿宝石）
- **鱼雷发射器负重**（数值/08）: 一律走 `TorpedoLauncherItem#computeWeight` = `ceil((1+联装)×口径英寸²×0.0055)`（533→21/610→24/720→28 英寸），不要再往 `TransformationManager.getWeaponLoadMap` 加鱼雷条目。稀有度用 `component.EquipmentTier`。图鉴发射器集中在 `WeaponItems.CATALOG_TORPEDO_LAUNCHERS`，创造栏和装填条装饰器遍历它注册
- **鱼雷散布角**: `CombatFireUtils.getSpreadAngles(n)` 返回长度必须 = 管数。2/3/4 管固定表；5/6/7 管在 ±最大偏角内均分，最大偏角走调试终端 `equipment.torpedo_spread.tubes{5,6,7}_max_angle`（默认 5°，待策划实测）

### 火控可视化预测（策划决策/火控/06）
- 渲染统一在 `client/FireControlVisualRenderer`（原 `BallisticAimMarkerRenderer` 已并入删除），由 `ClientInputCoordinator` 每 tick 调用。纯客户端，不发包。
- 纯数学在 `combat/FireControlPrediction`（5 tick 采样速度、鱼雷水平拦截闭式解、准星入球判定、扇形角）；火炮预测落点走 `BallisticSolver.predictImpactPoint`（客户端专用）。
- 扇形圆心角 = 2×max|`CombatFireUtils.getSpreadAngles(管数)`|，散布表改了扇形自动跟随。
- 颜色为终端参数 `global.fire_control_visual.prediction_line_color` / `prediction_line_active_color`（RGB 整数）。
- **预瞄点画在 HUD，不在世界空间**：世界空间按透视缩小，远距离落点会糊掉。落点由 `FireControlPrediction.projectToScreen` 用本帧真实投影矩阵投影到 GUI 坐标，标记固定像素尺寸（屏幕内为圆圈 `HUD_RADIUS`=7px、环宽 1px、中心留空；`drawHudMarker` 逐行扫描线画圆环）；相机背后的点返回 `null`（投影是镜像假点，只能整帧丢弃），视野外钳到屏幕边缘并画成实心块。开镜缩放改的是投影矩阵，因此自动跟随。鱼雷的线/扇形是 3D 跨度，保留在世界空间

### 装填（策划决策/武器/07、09）
- **一次按 R = 一次完整装填**：按下 R 只启动读条（写物品上的 `WEAPON_COOLDOWN`），到期由服务端结算——火炮在 `CannonReloading.tickCannonAutoReload`，舰载机在 `AircraftFireStrategy.tickAircraftReload`，都由 `PlayerTickHandler` 每 tick 调用。自动**启动**关闭（只能 R 键起读条），但自动**结算**必须保留
- ⚠️ `a48acb7f`（2026-09-26「固定关闭火炮自动装填」）曾把火炮弹到期结算的 `CannonReloadPhase.COMPLETE` 分支连同自动启动一起删掉，导致读条满了仍是空膛、要在读条满后再按一次 R。2026-10-02 已恢复：`resolve` 固定传 `automatic=false`
- **舰载机装填复用火炮那一套**：同一个 `WEAPON_COOLDOWN` 计时组件 → 装填条（`ReloadProgressHudLayer`）、物品图标进度条（`WeaponReloadDecorator`）、离开快捷栏清理（`WeaponReloadLifecycle`）全部免费复用；R 键统一走 `ManualReloadPayload`（曾经的 `AircraftReloadPayload` 长按读条已删除）
- 舰载机一次装填同时补**航空燃料 + 对海挂载**（先全量校验再消耗，缺任一项整体不执行）；出击准备判定统一在 `aviation/AircraftSortieReadiness`，读条前置校验/到期结算/放飞校验共用同一份规则，避免"提示已准备却被放飞拦下"
- 读条期间补给被拿走 → 到期结算失败、清读条退回未准备态（不消耗已拿到的部分）

### 蓝图与武器制造台配方（项目所有者 2026-09-30 授权实现方编配方）
- 三档武器蓝图：`standard_weapon_blueprint`（标准型武器蓝图，可合成）/ `improved_weapon_blueprint`（改良型）/ `advanced_weapon_blueprint`（先进型）。**按档位不按口径**——口径是玩法选择，不是进度；鱼雷发射器同一套
- 档位→蓝图映射在 `WeaponWorkbenchRecipeRegistry.blueprintFor`；初期档 `null` = 无需蓝图。`blueprintSatisfied` 是唯一校验入口：**`requiredBlueprint == null` 表示无需蓝图（蓝图格可空）**，旧实现把它当"不可合成"，导致鱼雷/导弹/深弹/飞机配方在生存模式永远做不出来（副本/21 §1.2）。旧中型火炮配方同样不再要求旧中型火炮蓝图
- 火炮/鱼雷配方由 `CannonCatalog` / `CATALOG_TORPEDO_LAUNCHERS` 循环生成：材料按口径族（小/中/大）+ 联装数 + 档位稀有材料推算，**材料种类上限 5**（+蓝图正好 6 格，制造台需求区 3 列×2 行）；制造时间 小 100 / 中 200 / 大 400，乘档位系数
- 蓝图不消耗，可在蓝图箱复制；蓝图物品同时登记在 `isWorkbenchBlueprint`（制造台蓝图格投递 + 蓝图箱存放共用一份名单）
- 07 表炮不再有原版工作台配方（`british_triple_16inch_gun` / `japanese_127mm_twin_gun` 的 `recipe/*.json` 已删），否则绕过制造台的蓝图门槛。Patchouli 火炮基础页已改指武器制造台；三张档位蓝图模型引用各自贴图 ID，贴图暂缺时按用户要求显示紫黑色缺失材质占位

### 火炮数值（策划决策/数值/06、07）
- 公式集中在 `artillery/CannonStatFormula`：面板、齐射、装填（tick，可带小数）、负重、DPS；稀有度乘数取 `EquipmentTier`
- 07 表的 60 门炮清单在 `artillery/CannonCatalog`，并在 `WeaponItems.CATALOG_GUNS` 循环注册（日本12.7厘米连装炮、德国双联380毫米炮仍是独立字段）；`allCatalogGuns()` 按表顺序返回全部 60 门
- 炮的 JSON（`data/piranport/artillery/cannons/<id>.json`）写 `caliberInches`、`barrels`、`tier`、可选 `velocityClass`（standard、high、low）。不写 damage 和 reloadTime 时由公式推导
- 旧 int `caliber` 由英寸派生：5 英寸以下记 4（小口径），5 到 13 英寸之间记 8（中口径），13 英寸及以上记 16（大口径）。旧判定照常可用，例如 AP 过穿要求 `>8`
- `reloadTime` 是 float。计时器用 `CannonStatFormula.resolveTicks` 按小数部分随机进位，长期平均等于表值
- 07 表炮的负重由 `ArtilleryItem.getFormulaWeight()` 计算，`TransformationManager.getWeaponLoadMap` 只保留旧炮和鱼雷
- `large_gun`（大型火炮）已删除，原引用改指 `british_triple_16inch_gun`（英国三联16英寸炮）。`large_gun_blueprint`（大型火炮蓝图）保留但**不再卡任何配方**（档位蓝图已取代它），村民老手档位改卖改良型武器蓝图；贴图仍在复用
- 对表测试：`CannonStatFormulaTest`

### 副本战斗与路线（《副本/00》《副本/22》《经济/01》）
- **缩放公式**集中在 `dungeon/instance/DungeonScaling`（纯逻辑，有单测）：血量 ×(1+0.5(n−1))；波数 ceil(基础波数×(1+0.2(n−1)))；n 在节点首次激活时快照（`DungeonNodeRouter.prepareBattleNode`）。
- **波次**：原代码无多波系统，节点 `enemies` 敌人组 = 一波；节点 JSON 可选 `waves`（基础波数，默认 1）。全灭后 `ServerGameEvents.onDungeonFlagshipDeath` → `NodeBattleField.trySpawnNextWave` 刷下一波，最后一波才开门。脚本节点波数恒为 1。
- **difficulty_scale**：节点级（>0）优先、关卡级兜底。血量直接乘；伤害写入实体 persistentData，由 `DungeonDamageScaleHandler`（LivingIncomingDamageEvent，HIGH）按来源/射弹 owner 放大——舰炮/鱼雷伤害走射弹参数，改 ATTACK_DAMAGE 无效。装甲不缩放。
- **分歧带路**：节点 JSON `branches:[{when:{hull,escorts,role,carry,chance},to}]` + `branch_default`（有 branches 必须写 default；目标必须是本节点出边）。判定 `BranchEvaluator`（有序、AND、default 兜底），接线 `DungeonBranchRouter`：出口传送门当场判定→快照 `DungeonInstance.branchChoices`→聊天 ✔/✘ + 标题→个人传送。讲台选点走 `DungeonEntryRules.canEnter(…, choiceFor)`。随从未开发：escorts=0、role=无；捆绑传送随从未做。
- **Boss**：激活时 `DungeonBossBroadcast` 全实例标题+音效+节点名；铭牌/血条每 5 tick 推给实例内全员。
- **首通**：只发给 Boss 击杀瞬间在 Boss 节点 128×128 内的玩家（`FirstClearRules`，快照存实例 NBT；旧存档无快照=全员）。
- **结语**：`DungeonResultPayload.ending` 由服务端下发。
- **记录点光柱**：`DungeonCheckpointBeacon` 用 END_ROD 粒子柱，不新增贴图。
- **迷路的运输舰**（`entity.piranport.lost_transport`）：第二章起每波 10% 把排序后末位（非旗舰）替换为补给舰 + `piranport_lost_transport` 标签；死亡只掉 1 个战利品（`hentai_trophy`），跳过全部原掉落。

### 副本进入链路（副本/00 §2.2/§3.1、副本/17 §三）
- **书台即入口**：入口传送门已作废，`DungeonPortalBlockEntity` 不再调 `DungeonEntryService.enter`，只提示去书台；传送门方块注册保留（兼容旧存档）。
- **插钥匙即建实例**：`DungeonLecternBlockEntity.tryInsertKey` → 钥匙已有 instanceId 则沿用（换书台不新建），否则 `DungeonInstanceManager.createInstanceForLectern`（无参与者、初始 SUSPENDED）并写回钥匙。
- **建造挂在实例上**：`LecternBuildScheduler.tick` 由 `ServerGameEvents` 每 tick 推进起点节点地形，书台被拆也不中断。
- **纹路三态**（`LecternPattern`）：白=无钥匙，红=建造中/满员(4人)/实例失效，绿=可进。只有绿才打开进本界面。纹路面 `tintindex 0` + `ClientModEvents.registerBlockColors` 染色，不另画贴图。
- **交互**：空手潜行右键撤钥匙（建造中锁定）；普通右键（空手/持物）开书台；持物潜行右键 = 使用手中物品。破坏/爆炸 `onRemove` 掉钥匙，不清绑定。
- **悬浮字幕**：`DungeonLecternRenderer`，关卡名 / 已通关或未通关+节点数 / 人数 x/4，空书台不显示。
- **已进入过副本标记**：`ModAttachmentTypes.ENTERED_DUNGEON`（copyOnDeath），`DungeonEntryService` 成功传入副本维度时写入。
- **书台遗迹**：`portal_ruin_1/2.nbt` 已去掉传送门框架、放空白书台；`AbandonedPortalStructure` 不再摆框架。开箱战利品 `chests/portal_ruin.json` 用条件 `piranport:entered_dungeon`：没进过 → 1-1 钥匙，进过 → 金猫猫钥匙（规则见 `RuinKeyRule`）。补给站/前哨站/深海基地不掉主线钥匙。

### 手感参数一律进调试终端（2026-10-05，强约定）
- **凡需策划调手感的数值，一律走 `config/TerminalConfigValue.number/integer/bool(group, target, property, base, min, max)`**，注册在 `ModEquipmentConfig` / `ModProjectilesConfig` / `ModArtilleryConfig`。键自动生成为 `global.<target>.<property>`，经 `TerminalConfigValue.specs()` 被 `TerminalParameterCatalog` 收集进终端；读取用同一对象的 `.get()`。
- 鱼雷空投衰减 `air_drop_horizontal_decay` / `air_drop_vertical_accel`、非空投回退 `air_fall_horizontal_decay` / `air_fall_vertical_accel`、线导垂直输入死区 `wire_vertical_deadzone` 都由 `ModEquipmentConfig` 注册，默认仍为 `0.98/0.08`、`0.70/0.25`、`0.05`；空投与空中回退是不同运动阶段，参数不合并
- **不要为手感参数手写 `TerminalParameterCatalog.add(specs, ...)`**：它生成的键是 `<group>.<target>.<property>`（如 `equipment.<target>.<property>`），而 `TerminalConfigValue.get()` 只读 `global.` 键 → 终端里「能改但不生效」的死参数。`add()` 仅用于读取侧也按同一非 global 键直读的目录项（雷达/声呐的 `equipment.<注册名>.*`，以及导弹发射器的 `missile_launcher.<注册路径>.*`，读取在 `MissileLauncherItem#parameterKey`）。
- **命名避开终端名字启发式**（`TerminalParameterSpec.isLinearSpeed` / `isTickDuration`）：property 恰为 `speed` / `panel_speed` / `initial_speed` / `full_load_speed` / `empty_speed` / `movement_speed` 会按「格/tick × 20」显示成 t/s；property 为 `reload_time` / `fire_cooldown` / `salvo_interval` 或以 `_cooldown` 结尾会按「× 0.05」显示成秒。单位对不上就换名，例如导弹速度曲线用 `speed_initial` / `speed_increment` / `speed_max`（避开 `initial_speed` / `max_speed`）。
- 本次下沉的主要分组（均为 `global.<target>.*`）：`torpedo`、`torpedo_salvo`、`shell`、`tracking`、`depth_charge`、`missile` / `missile_anti_ship` / `missile_anti_air` / `missile_rocket`、`aerial_bomb`（重力）、`small_ship`、`fire_debuff`、`aircraft_flight`、`fighter_combat` / `dive_bomber_combat` / `torpedo_bomber_combat` / `rocket_fighter_combat`、`level_bomber`、`type3`（终端分组 artillery）、`ciws_20mm` / `ciws_40mm` / `ciws_76mm`（`.damage`）；另有导弹发射器 `missile_launcher.<注册路径>.*`（独立分类「导弹」，不再混在 enhancement）与共享默认值 `global.missile_launcher.*`（单型号键 > 共享值 > 物品基准，判定「已设置」走 `TerminalParameters.isOverridden`）。
- 默认值 = 下沉前的写死值（纯重构）。NPC 炮弹/鱼雷手感统一经 `npc/ai/NpcCombatTuning` 读取，消除 `TorpedoAttackGoal` / `ShipGirlCombatGoal` 等处的逐字复制。

### 火控瞄准点 = 目标眼睛高度（2026-10-05 起统一）
- 统一真源：`combat/CombatTargeting.aimPoint(entity)` = `Entity#getEyeY()`（原版溺水/窒息判定用的那个位置），返回 `(getX, getEyeY, getZ)`。之所以取 `Entity` 而非 `LivingEntity` 的眼睛 API：`AircraftEntity` 不继承 `LivingEntity`。
- 覆盖：HUD 预瞄圈（`FireControlVisualRenderer`）、火控包视线校验（`FireControlPayload`）、炮弹追踪/转向（`CannonProjectileEntity`）、导弹锁定与制导（`MissileEntity`、`MissileFireStrategy`）。此前各点各写 `getBbHeight() * 0.4~0.5`，导致「圈画在头高、炮弹打腰线」。
- 例外（保持原样，勿顺手统一）：炮弹 VT 近炸引信取实体几何中心（`CannonProjectileEntity` 约 :453）；鱼雷水平拦截点的 y 取发射高度（`FireControlVisualRenderer` 把 `torpedoIntercept` 的 y 覆盖为 `start.y`）。

### 已删除功能（勿照旧文档/旧提示加回）
- **Y 键战场高亮已删除**（`ac9ec5bb`）：模组侧高亮只剩两档——原版发光恒最优先 > 火控红框 > 声呐黄框。`EntityHighlightHandler` 仅保留火控高亮与 ASW 声呐高亮两块；`ModKeyMappings` 已无对应键位。
- **火控雷达 0 键准星吸附已删除**（`9003fc00`）：火控雷达只保留炮弹追踪——等级 1/2/3 对应追踪范围 8/12/16 格、转向系数 0.02/0.04/0.06（`ModEquipmentConfig.FC_RADAR_L*`，读取在 `combat/cannon/FireControlRadarTracking.forLevel`）。`SnapDecision`、`FireControlRadarSnapHandler`、`ToggleFcRadarPayload`、`FcRangeRequestPayload`、`FcRangeSyncPayload`、数据组件 `SHIP_FC_RADAR_ON` 均已删除。

### 鱼雷：水下贴水面巡航 + 线导客户端权威（易被误当 bug，勿改回）
- **目标行为是「水面之下贴水面」潜航，不是浮在水面之上**：目标深度 = 水面顶面 − `global.torpedo.surface_depth`（默认 0.5），雷体中心对齐；垂直收敛用 `surface_vertical_adjust` / `surface_vertical_max_speed`，捕获窗口 `surface_capture_range`（见 `TorpedoEntity.solveSurfaceVy`）。
- **深水不强行拉回**：水面捕获窗口之外（潜艇深水、水中发射）保持既有垂直运动（`solveSurfaceVy` 返回当前 vy），避免把深雷硬拽上水面；上方无水时禁止上浮。
- **线导期间客户端权威**（`178c9af1`）：`TorpedoEntity` 覆写 `isControlledByLocalInstance()` 与 `lerpMotion()`——仅对「本地玩家正在引导的线导鱼雷」返回 true / 忽略服务端速度包，使原版跳过服务端位置与传送校正。服务端仍按上传输入包（`combat/TorpedoGuidanceManager`）跑一份权威副本做命中/爆炸裁决。WHY：消除线导第一人称的镜头回拉。**这是消除网络延迟的正当设计，不要当成「客户端作弊通道」删掉。**
- 客户端插值走自研 `lerpTo`（与 `CannonProjectileEntity` 同一套，修复原版 `lerpTo` 直接 `setPos` 导致的航行抽搐）；航速与五个模式标志（磁引信/线导/声导/氧气/空投）走 `SynchedEntityData` 同步，客户端每 tick `syncClientState` 刷新。

### 水平轰炸机：保留前抛 + 提前量投弹
- 航弹保留前抛——继承载机水平速度 × `global.level_bomber.horizontal_velocity_multiplier`（默认 0.5）。
- 投弹判据不再是固定「< 3 格」：由 `combat/LevelBombLead.releaseDistance(...)` 按航弹真实物理（复刻 `ThrowableProjectile#tick` 的「先位移 → 阻尼 0.99 → 重力」逐行顺序）反推提前距离，水平距离 ≤ 提前距离才投（下限 `global.level_bomber.min_release_distance`）。落点标记与实体弹道同源。
- 保留 ±0.5 格随机散布（`global.level_bomber.bomb_spread` 默认 1.0 → `(rand − 0.5) × 1.0`）。
- 飞行手感不变：Phase 1 爬升 `climb_speed_multiplier` 0.4、Phase 2 航线 `run_speed_multiplier` 0.6、投弹高度 `altitude_offset` +32。

### 自动索敌的敌我识别（2026-10-05）
- 自动索敌与「自动进入攻击态」只打敌对目标，不得自动开打 `Animal` / `Villager` / 友军。对地自动索敌走 `CombatTargeting.isHostileTarget`（只认 `Enemy` 与敌对飞机）。战斗机空对空自动扫描及巡航攻击态入口也共用该判据；机枪仍只自动搜索飞机。
- **声呐标记不受影响**：仍标记所有水生生物（含鲑鱼/海豚/玩家，策划既定玩法）。声呐判据 `AircraftAswRecon.isAswTarget` 本体保持不变；自动索敌/自动进入攻击态另用 `AircraftAswRecon.isHostileAswTarget`（= `isAswTarget` && `isHostileTarget`），别把过滤加回 `isAswTarget`。
- 手动中键/火控锁定不限制（尊重玩家意图）。

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-1.1.49-dev.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=1.1.49-dev
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.1
neo_version=21.1.220
```

**重要**: 测试版 `mod_version` 必须保留 `-dev` 后缀，打包前检查补上。

---

## Reference Links

- NeoForge Docs: https://docs.neoforged.net/
- MDK Template: https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle
- Minecraft Wiki: https://minecraft.wiki/
- Patchouli Wiki: https://vazkiimods.github.io/Patchouli/
- GitHub: https://github.com/CVIndomitable/piranport_cl.git
- **原始策划案**: `../文档/总策划案20260510.md`
- **所有文档**: `../docs/` — 文档统一在仓库根目录，本仓库不保留 docs/
- **踩坑记录**: `/Users/lianran/IndomitableCache/ai记忆/mc模组开发踩坑记录.md` — 统一的 MC 模组开发踩坑经验库

---

## Tools & Scripts

### 文档处理工具

**处理 Word 文档 (.docx)**：
- **工具**: `python-docx` 库
- **安装**: 由于 macOS 使用 externally-managed-environment，需要创建虚拟环境：
  ```bash
  python3 -m venv .venv_docx
  source .venv_docx/bin/activate
  pip install python-docx
  ```
- **脚本位置**: `../小工具/` 目录
- **可用样式**: 使用前先检查文档中的可用样式（运行 `check_styles.py`）
  - 常用样式: `Normal`, `Subtitle`, `List Paragraph`, `Heading 1-5`
  - 避免使用: `List Bullet`, `List Number`（可能不存在）
- **注意事项**:
  - 不能直接用 Read 工具读取 .docx（二进制格式）
  - 必须使用 python-docx 库处理
  - 样式名称区分大小写，使用前需验证
  - 修改文档后记得保存到新文件，避免覆盖原文件

