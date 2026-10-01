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

