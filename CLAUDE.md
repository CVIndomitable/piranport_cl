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
当前 1.0 稳定线聚焦已验证玩法：舰娘变身/核心、武器系统（炮/鱼雷/导弹/深弹）、舰载机、火控、侦察、食物农场（料理锅/石磨/切板）、盐蒸发、装填设施、弹药合成台、武器师交易、Patchouli 手册。

**1.0 不包含的内容**（从 dev 线或未来版本引入）：舰装 GUI、编组 GUI、副本系统、武器合成台、遗迹结构、深海 NPC、舰娘 NPC、传送门。

**无 GUI 模式限制**（设计决策）：1.0 版本采用无 GUI 模式，不支持编组配置和自动升空功能。飞机通过右键直接发射，武器通过副手槽切换。

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

### 术语规范
- **飞机分类**：攻击机 = 轰炸机（dive/level bomber）+ 鱼雷机（torpedo bomber）；轰炸机 = 水平轰炸机 + 俯冲轰炸机
- **海军术语**：联装数 = 管数/弹夹容量（magazine capacity），再装填 = reload，鱼雷管数 = tubeCount
- **高亮优先级**：原版高亮（Glowing effect）> 玩法高亮（火控）> Y键战场高亮

---

## Build & Run

```bash
./gradlew runClient    # 运行客户端
./gradlew runData      # DataGen
./gradlew build        # 构建 → build/libs/piranport-0.0.11.jar
```

### gradle.properties

```properties
mod_id=piranport
mod_name=Piran Port
mod_license=All Rights Reserved
mod_version=0.0.11
mod_group_id=com.piranport
mod_authors=PiranPort Dev Team
mod_description=Minecraft mod based on Warship Girls R
minecraft_version=1.21.1
neo_version=21.1.220
```

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
