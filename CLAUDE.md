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

## Version Roadmap

完整版本列表与每版核心内容见 **`../docs/皮兰港 版本路线图.md`**。
- **当前版本**: v1.1.2-dev (测试版)
- **稳定版本**: v1.0.0 (main 分支，仅修 bug)
- **开发策略**: main = 稳定版，dev = 测试版新玩法

---

## Technical Reference

完整的目录结构、NeoForge 21.1.220 API 坑、关键技术要点、配置系统、外部依赖均已移至 **`../docs/皮兰港技术参考手册.md`**。开发前请先查阅该文件。

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

---

## Key Systems (1.1.1-dev)

### 鱼雷系统
- **鱼雷发射器**: 支持更换弹药类型，装备再装填设施后支持按 R 键和拖动装填
- **自动装填**: 严格同种鱼雷，避免混乱
- **线导鱼雷**: 第一人称视角操纵
- **近炸功能**: 接近目标时自动引爆

### 再装填设施
- **水陆差异化**: 水上和陆地装填时间不同
- **兼容性**: 支持鱼雷发射器的 R 键和拖动装填

### 战斗系统
- **齐射穿甲**: 多发炮弹齐射时穿甲效果增强
- **大型船只免击退**: 大型舰船不受击退效果影响
- **装甲减免摔落**: 装甲保护可减免摔落伤害
- **测试模式**: 无限弹药

### 移动系统
- **水面行走加速补偿**: 在水面行走时提供加速补偿
- **水上免阻力**: 水上移动不受水阻力影响

### 飞机系统
- **火箭机**: 新增 `ROCKET_FIGHTER` 机型，支持火箭弹攻击
- **多弹药智能投放**: 按目标血量计算单次投放弹药数量
- **侦察机输入失效问题**: 可能原因包括 `ReconManager.activeRecon` Map 未正确注册玩家UUID、网络包丢失或时序问题、实体在区块卸载时无法消费输入。已添加诊断日志和防御性状态同步检查。

### 兼容性框架
- **软依赖框架**: 可扩展的 Mod 兼容系统 (`com.piranport.compat`)
- **女仆舰装 compat**: 与女仆 Mod 的兼容支持

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-1.1.2-dev.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=1.1.2-dev
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
- 原始策划案：`../docs/总策划案.docx`
- 所有文档统一在仓库根目录 `../docs/`，本仓库不保留 docs/
- GitHub: https://github.com/CVIndomitable/piranport_cl.git
- **踩坑记录**：`/Users/lianran/IndomitableCache/ai记忆/mc模组开发踩坑记录.md` — 统一的 MC 模组开发踩坑经验库
- **更新日志**: `../docs/CHANGELOG-1.1.2-dev.md` — 详细的版本变更记录

---

## Development Tools

### Excel数据表管理工具

位置：`../小工具/update_excel_from_code.py`

**功能**：
1. 从Java代码（`ShropshireModItems.java`）中提取已注册的物品ID
2. 自动更新Excel表格中的"是否已实现"状态
3. 翻译合成配方中的英文ID为中文名称（如 `aluminum_ingot` → `铝锭`）
4. 生成实现情况统计报告

**使用方法**：
```bash
cd /Users/lianran/apps/皮兰港实验/小工具
python3 update_excel_from_code.py
```

**依赖**：
```bash
pip install openpyxl
```

**工作原理**：
- 通过正则表达式 `REGISTRY\.register\("([^"]+)"` 提取Java代码中所有注册的物品ID
- 对比Excel表格中的物品ID列，更新"是否已实现"状态
- 使用ID映射表（包含mod物品和原版物品）翻译合成配方
- 支持格式：`物品ID×数量` 或 `物品ID`

**注意事项**：
- Excel文件路径硬编码在脚本中，修改项目结构时需同步更新
- 原版物品映射表需手动维护，新增原版物品时需添加到 `vanilla_items` 字典
- 合成配方格式必须符合 `item_id×数量` 或 `item_id` 的模式

---

## Known Issues & Investigation

### 砧板配方JEI不显示问题（待测试确认）

**问题描述**：测试版中砧板配方在JEI中不显示

**排查情况**（2026-05-10）：
- ✅ 配方文件存在：5个砧板配方文件位于 `src/main/resources/data/piranport/recipe/cutting_board_*.json`
- ✅ 配方格式正确：使用 `{"id": "...", "count": ...}` 格式，与料理锅、石磨配方一致
- ✅ 配方类型已注册：`ModRecipeTypes.CUTTING_BOARD_TYPE` 和 `CUTTING_BOARD_SERIALIZER` 已在主类注册到事件总线
- ✅ JEI插件配置正确：`PiranPortJEIPlugin` 类有 `@JeiPlugin` 注解，实现了 `IModPlugin` 接口
- ✅ JEI类别已注册：`CuttingBoardRecipeCategory` 在 `registerCategories` 中注册
- ✅ JEI配方已注册：在 `registerRecipes` 中通过 `ModRecipeTypes.CUTTING_BOARD_TYPE.get()` 获取配方
- ✅ JEI催化剂已注册：砧板方块在 `registerRecipeCatalysts` 中注册
- ✅ 砧板方块已注册：`ModBlocks.CUTTING_BOARD` 存在
- ✅ 语言文件包含翻译：`jei.piranport.cutting_board` 在 `zh_cn.json` 和 `en_us.json` 中
- ✅ JEI依赖正确配置：`build.gradle` 中有 compileOnly 和 localRuntime 依赖，`neoforge.mods.toml` 中声明为可选依赖
- ✅ 配方文件已打包：`build/resources/main/data/piranport/recipe/` 中包含所有砧板配方
- ✅ 构建无错误：`./gradlew clean build` 无警告或错误
- ⚠️ 游戏日志无信息：`run/logs/latest.log` 中完全没有砧板相关的加载信息（既无成功也无错误）

**可能原因**：
1. 配方目录命名问题：当前使用 `recipe`（单数），Minecraft 1.13+ 标准可能要求 `recipes`（复数）
2. 如果目录名是问题，则所有自定义配方（料理锅、石磨、砧板）都会受影响

**待确认**：
- 料理锅和石磨的配方在JEI中是否能正常显示？
- 如果它们能显示，则问题特定于砧板配方，需要深入对比差异
- 如果它们也不显示，则可能是配方目录命名或JEI插件整体问题

**相关文件**：
- 配方文件：`src/main/resources/data/piranport/recipe/cutting_board_*.json`
- 配方类：`src/main/java/com/piranport/recipe/CuttingBoardRecipe.java`
- 配方类型注册：`src/main/java/com/piranport/registry/ModRecipeTypes.java`
- JEI插件：`src/main/java/com/piranport/compat/jei/PiranPortJEIPlugin.java`
- JEI类别：`src/main/java/com/piranport/compat/jei/CuttingBoardRecipeCategory.java`

---

## Recent Changes (1.1.2-dev)

最近更新（2026-05-10）:
- 石磨配方系统：添加可配置加工时间（默认200 tick = 10秒）
- 石磨GUI：添加加工进度条显示
- 修复反潜机深弹无法命中深水目标问题
- 修复火箭机攻击能力问题
- 修复鱼雷机发射鱼雷方向反向bug（飞机远离敌舰时向机尾发射）

最近更新（2026-05-09）:
- 版本升级至 1.1.2-dev
- 新增Excel数据表管理工具

完整变更记录见 `../docs/版本记录/CHANGELOG-1.1.2-dev.md`。
