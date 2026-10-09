# CLAUDE.md — 皮兰港 (Piran Port) Minecraft Mod

基于战舰少女R世界观的 Minecraft 综合模组（同人项目，All Rights Reserved）：玩家扮演舰娘，装备舰载武器，与深海敌人战斗。
Mod ID `piranport`，包名 `com.piranport`；Java 21 / NeoForge 21.1.220 / MC 1.21.1 / ModDevGradle。

## 分支与文档
- dev = 测试版（本仓库，新玩法）；main = 稳定版 v1.0.0（仅修 bug）
- 版本号以 `gradle.properties` 为准。**测试版 `mod_version` 必须带 `-dev` 后缀**，打包前检查
- 文档统一在 `../docs/`（独立 git 仓库），本仓库不放 docs/：
  - `../docs/策划决策/`：一切策划决策的真源（见下节）
  - `../docs/皮兰港技术参考手册.md`：目录结构、API 坑、配置系统。其中**「子系统实现备忘」一节**记录火控可视化、装填、蓝图配方、火炮数值、副本、终端参数、鱼雷、轰炸机、索敌等的类位置与实现细节，**改这些系统前先查**
  - 路线图 `../docs/皮兰港 版本路线图.md`；更新日志 `../docs/版本记录/`；问题排查 `../docs/troubleshooting/`；原始策划案 `../文档/总策划案20260510.md`
- 踩坑经验库：`/Users/lianran/IndomitableCache/ai记忆/mc模组开发踩坑记录.md`
- GitHub: https://github.com/CVIndomitable/piranport_cl.git

## 策划决策优先级（最高优先级规则）
- 一切策划决策以 `../docs/策划决策/` 为准。已有对应决策时**不向用户提问，直接按决策执行**
- 其他文档（技术手册、总策划案、教程、代码注释、README 等）与之冲突时，一律以 `策划决策/` 为准

## 代码规范
- 命名：类 `PascalCase`、注册 ID `snake_case`、常量 `UPPER_SNAKE_CASE`、翻译 key `block.piranport.cooking_pot`
- 注册一律走 `DeferredRegister`；物品数据用 `DataComponents`，不用旧版 NBT
- Client-only 代码放 `@EventBusSubscriber(value = Dist.CLIENT)`
- 同时维护 `zh_cn.json` 和 `en_us.json`
- 注册物品必须同时创建 `models/item/*.json`，不能只放贴图
- 注释全部用中文；复杂逻辑注释 WHY 而不只是 WHAT
- **1.21.1 配方格式**：`key` / `ingredients` / `ingredient` 必须写 `{item:...}` 对象，不接受裸字符串（1.21.2+ 才支持）
- **1.21.1 数据目录为单数**：`recipe/`、`loot_table/`、`advancement/`、`structure/`。写错时整目录静默不加载，不报错
- 新村民 POI 必须同时登记 `data/minecraft/tags/point_of_interest_type/acquirable_job_site.json`，否则无业村民不认领；不往原版职业塞模组商品

### 手感参数一律进调试终端（强约定）
- 需要策划调手感的数值，一律用 `config/TerminalConfigValue.number/integer/bool(group, target, property, base, min, max)`，注册在 `ModEquipmentConfig` / `ModProjectilesConfig` / `ModArtilleryConfig`。键自动生成为 `global.<target>.<property>`，读取用同一对象的 `.get()`。下沉时默认值 = 原写死值
- **不要为手感参数手写 `TerminalParameterCatalog.add(...)`**：它生成非 `global.` 键，而 `.get()` 只读 `global.` 键，结果是终端里能改但不生效的死参数。`add()` 仅用于读取侧也按同一非 global 键直读的条目（雷达/声呐、导弹发射器）
- property 命名避开终端单位启发式：恰为 `speed` / `initial_speed` / `movement_speed` 等会按「格/tick × 20」显示；为 `reload_time` / `fire_cooldown` / `salvo_interval` 或以 `_cooldown` 结尾会按「× 0.05」显示成秒。单位对不上就换名（如 `speed_initial`）

## 既定设计：勿改回 / 勿顺手统一
细节见技术手册「子系统实现备忘」。
- **装填到期结算必须保留**：R 键只启动读条，到期由服务端结算（火炮 `CannonReloading.tickCannonAutoReload`、舰载机 `AircraftFireStrategy.tickAircraftReload`）。自动*启动*关闭，自动*结算*不能删（`a48acb7f` 曾误删，2026-10-02 已恢复）
- **蓝图校验**：`WeaponWorkbenchRecipeRegistry.blueprintSatisfied` 中 `requiredBlueprint == null` 表示无需蓝图，不是不可合成。07 表炮不得有原版工作台配方（会绕过蓝图门槛）
- **负重**：鱼雷发射器走 `TorpedoLauncherItem#computeWeight`，07 表炮走 `ArtilleryItem.getFormulaWeight()`，不要往 `TransformationManager.getWeaponLoadMap` 加条目
- **鱼雷散布**：`CombatFireUtils.getSpreadAngles(n)` 返回长度必须等于管数（火控扇形也依赖它）
- **火控瞄准点 = 目标眼睛高度**，统一走 `CombatTargeting.aimPoint`（`getEyeY`）。例外保持原样：炮弹 VT 近炸取几何中心；鱼雷拦截点 y 取发射高度
- **鱼雷在水面之下贴水面潜航**，不是浮在水面上；捕获窗口外不强行把深雷拉回水面（`TorpedoEntity.solveSurfaceVy`）
- **线导鱼雷客户端权威**（覆写 `isControlledByLocalInstance` / `lerpMotion`）是消除延迟回拉的正当设计，不是作弊通道，勿删
- **水平轰炸机**保留前抛；投弹时机由 `LevelBombLead.releaseDistance` 按真实弹道反推，不要改回固定距离
- **自动索敌只打敌对目标**（`CombatTargeting.isHostileTarget`）。声呐仍标记所有水生生物：过滤只放在 `isHostileAswTarget`，别加回 `isAswTarget`。手动锁定不限制
- **副本伤害缩放**走 `DungeonDamageScaleHandler`；舰炮/鱼雷伤害来自射弹参数，改 `ATTACK_DAMAGE` 无效

## 已删除功能（勿照旧文档加回）
- Y 键战场高亮（`ac9ec5bb`）：模组高亮只剩火控红框 > 声呐黄框（原版发光最优先）
- 火控雷达 0 键准星吸附（`9003fc00`）：火控雷达只保留炮弹追踪
- `large_gun` 大型火炮：引用已改指 `british_triple_16inch_gun`；`large_gun_blueprint` 保留但不卡任何配方
- 副本入口传送门：书台即入口；传送门方块注册仅为兼容旧存档保留
- `AircraftReloadPayload` 长按读条：R 键统一走 `ManualReloadPayload`

## Build & Run
```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # → build/libs/piranport-<mod_version>.jar
```
