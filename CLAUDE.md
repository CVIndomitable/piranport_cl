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

## Recent Changes (1.1.2-dev)

最近更新（2026-05-09）:
- 版本升级至 1.1.2-dev
- 新增Excel数据表管理工具

完整变更记录见 `../docs/CHANGELOG-1.1.2-dev.md`。
