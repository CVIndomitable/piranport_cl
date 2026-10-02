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

- **当前版本**: v1.1.48-dev (测试版)
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
- **预瞄点画在 HUD，不在世界空间**：世界空间按透视缩小，远距离落点会糊掉。落点由 `FireControlPrediction.projectToScreen` 用本帧真实投影矩阵投影到 GUI 坐标，标记固定像素尺寸；相机背后的点返回 `null`（投影是镜像假点，只能整帧丢弃），视野外钳到屏幕边缘并画成实心块。开镜缩放改的是投影矩阵，因此自动跟随。鱼雷的线/扇形是 3D 跨度，保留在世界空间

### 装填（策划决策/武器/07、09）
- **一次按 R = 一次完整装填**：按下 R 只启动读条（写物品上的 `WEAPON_COOLDOWN`），到期由服务端结算——火炮在 `CannonReloading.tickCannonAutoReload`，舰载机在 `AircraftFireStrategy.tickAircraftReload`，都由 `PlayerTickHandler` 每 tick 调用。自动**启动**关闭（只能 R 键起读条），但自动**结算**必须保留
- ⚠️ `a48acb7f`（2026-09-26「固定关闭火炮自动装填」）曾把火炮弹到期结算的 `CannonReloadPhase.COMPLETE` 分支连同自动启动一起删掉，导致读条满了仍是空膛、要在读条满后再按一次 R。2026-10-02 已恢复：`resolve` 固定传 `automatic=false`
- **舰载机装填复用火炮那一套**：同一个 `WEAPON_COOLDOWN` 计时组件 → 装填条（`ReloadProgressHudLayer`）、物品图标进度条（`WeaponReloadDecorator`）、离开快捷栏清理（`WeaponReloadLifecycle`）全部免费复用；R 键统一走 `ManualReloadPayload`（曾经的 `AircraftReloadPayload` 长按读条已删除）
- 舰载机一次装填同时补**航空燃料 + 对海挂载**（先全量校验再消耗，缺任一项整体不执行）；出击准备判定统一在 `aviation/AircraftSortieReadiness`，读条前置校验/到期结算/放飞校验共用同一份规则，避免"提示已准备却被放飞拦下"
- 读条期间补给被拿走 → 到期结算失败、清读条退回未准备态（不消耗已拿到的部分）

### 蓝图与武器制造台配方（项目所有者 2026-09-30 授权实现方编配方）
- 三档武器蓝图：`standard_weapon_blueprint`（标准型武器蓝图，可合成）/ `improved_weapon_blueprint`（改良型）/ `advanced_weapon_blueprint`（先进型）。**按档位不按口径**——口径是玩法选择，不是进度；鱼雷发射器同一套
- 档位→蓝图映射在 `WeaponWorkbenchRecipeRegistry.blueprintFor`；初期档 `null` = 无需蓝图。`blueprintSatisfied` 是唯一校验入口：**`requiredBlueprint == null` 表示无需蓝图（蓝图格可空）**，旧实现把它当"不可合成"，导致鱼雷/导弹/深弹/飞机配方在生存模式永远做不出来（副本/21 §1.2）
- 火炮/鱼雷配方由 `CannonCatalog` / `CATALOG_TORPEDO_LAUNCHERS` 循环生成：材料按口径族（小/中/大）+ 联装数 + 档位稀有材料推算，**材料种类上限 5**（+蓝图正好 6 格，制造台需求区 3 列×2 行）；制造时间 小 100 / 中 200 / 大 400，乘档位系数
- 蓝图不消耗，可在蓝图箱复制；蓝图物品同时登记在 `isWorkbenchBlueprint`（制造台蓝图格投递 + 蓝图箱存放共用一份名单）
- 07 表炮不再有原版工作台配方（`british_triple_16inch_gun` / `japanese_127mm_twin_gun` 的 `recipe/*.json` 已删），否则绕过制造台的蓝图门槛。Patchouli 火炮基础页已改指武器制造台

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

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-1.1.48-dev.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=1.1.48-dev
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

