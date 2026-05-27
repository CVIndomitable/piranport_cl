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

- **当前版本**: v1.1.7-dev (测试版)
- **稳定版本**: v1.0.0 (main 分支，仅修 bug)
- **开发策略**: main = 稳定版，dev = 测试版新玩法
- **版本路线图**: `../docs/皮兰港 版本路线图.md`
- **技术参考手册**: `../docs/皮兰港技术参考手册.md` — 完整目录结构、API 坑、核心系统详解、配置系统
- **更新日志**: `../docs/版本记录/CHANGELOG-1.1.6-dev.md`
- **开发工具**: `../docs/开发工具/` — Excel数据表管理工具等
- **问题排查**: `../docs/troubleshooting/` — 已知问题与调查记录

---

## Conventions & Rules

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
- **物品模型**: 注册物品时必须主动创建 `models/item/*.json` 模型文件，不能只放贴图
- **注释语言**: 全部使用中文注释，保持中文团队维护一致性。复杂逻辑必须注释 WHY 而不只是 WHAT

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-1.1.4-dev.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=1.1.4-dev
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
- **原始策划案**: `../docs/总策划案.docx`
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

---

## Recent Changes

### v1.1.7-dev

**帕秋莉手册（2026-05-26）**：
- 创建完整的中文游戏内手册系统
- **入门指南**（7个条目）：欢迎、变身、武器、燃料、火控、飞机、生存
- **进阶篇**（8个分类，21个条目）：
  - 武器系统：火炮、鱼雷、导弹、深弹、特殊武器、火控详解
  - 航空系统：飞机类型、AI控制、燃料管理
  - 辅助系统：声呐、烟幕照明、高亮
  - 装备强化：装甲引擎、特殊装备、皮肤核心
  - 生产合成：工作台、装填设施、食物系统
  - 副本战斗：深海副本
  - 配置调试：按键绑定、客户端配置、调试工具
- 手册位置：`src/main/resources/assets/piranport/patchouli_books/guidebook/zh_cn/`
- 入门篇通俗易懂，进阶篇包含详细数据和机制说明

### v1.1.7-dev

**代码审查修复（2026-05-26）**：
- **P0 安全修复**：
  - 火控系统添加视线检查，防止客户端伪造透视锁定
  - 鱼雷制导添加频率限制（50ms），防止DoS攻击
- **P1 功能修复**：
  - 调整玩家登出清理顺序，防止火控状态泄漏
  - 提高鱼雷目标切换阈值至0.7，增加锁定稳定期至60tick
  - 飞机燃料消耗添加容量为0的边界检查
- **P2 性能优化**：
  - 无GUI模式负重检查降低至每5tick
  - 声呐扫描动态限制范围，避免超出服务器模拟距离
  - 水面行走缓存三角函数计算
  - FireControlManager改用HashMap（单线程访问）
  - 传送检测阈值降至64格，避免误判鞘翅飞行
- **P3 代码质量**：
  - 定义Tick间隔常量，统一魔法数字
  - 所有Manager类添加私有构造函数
  - 添加清理顺序注释和统一清理方法

**新增武器**：
- **齐射测试** (`salvo_test_gun`) — 测试用极限齐射火炮
  - 12联装，三倍大型火炮数值
  - 伤害：60.0（大型火炮的3倍）
  - 装填时间：80 tick（与大型火炮相同）
  - 初速：10.5（大型火炮的3倍）
  - 爆炸威力：6.0（大型火炮的3倍）
  - 用途：测试极限齐射性能和散布系统
