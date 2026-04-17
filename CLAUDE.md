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

| 版本 | 代号 | 状态 | 核心内容 |
|------|------|------|---------|
| v0.0.1-alpha | MVP | ✅ DONE | 基础注册、世界生成、合成、舰装核心GUI、火炮战斗 |
| v0.0.2-alpha | Torpedo | ✅ DONE | 鱼雷系统、舰装栏GUI完善、负重平衡、装填机制 |
| v0.0.3-alpha | Kitchen | ✅ DONE | 食物烹饪 + Buff系统、作物种植、加工站 |
| v0.0.4-alpha | Aviation | ✅ DONE | 航空系统（4种飞机+编组GUI+火控+AI）+ Patchouli手册 |
| v0.0.5-alpha | Combat+ | ✅ DONE | 装填进度Decorator、装填加速/高速规避Buff、Buff食物注册 |
| v0.0.6-alpha | Skin | ✅ DONE | 皮肤/模型渲染系统（SkinManager + SkinOverlayLayer + SkinCoreItem） |
| v0.0.7-alpha | Aviation+ | ✅ DONE | 侦察机视角切换、空战、编队跟随侦察机、加工站自动化 |
| v0.0.8-alpha | Salt & Rice | ✅ DONE | 稻田种植、盐蒸发系统、条件性盐矿生成、全面代码审查修复(35项P0-P3) |
| v0.0.9-alpha | Dungeon | ✅ DONE | 副本系统（大厅/实例/节点战斗/传送门/箱子船/钥匙/回城卷轴）+ JEI兼容 + 武器冷却条 + 友军伤害防护 + 彩色描边 + 击落归因 |
| v0.0.10-alpha | Arsenal | ✅ DONE | 导弹系统 + 深弹系统 + 命名鱼雷 + 武器/弹药合成台 + 装填设施 + 特殊道具(烟幕/照明弹/电磁炮/损管/维修台/足球套装等) + 皮肤系统 + 全局代码审查修复(47项) |
| v0.0.11-alpha | Ruins | ✅ DONE | 主世界四类遗迹(传送门/补给站/前哨站/深海基地) + 9种深海NPC + 集群AI + 抛物线/追踪弹道 + 战利品系统 + 占位物品(国旗/碎片/档案) + 调试命令 + 舰娘NPC框架 |

> 已完成 Phase 详情见 `docs/phases_archive.md`
> 代码审查修复详情见 `docs/code_review_2026-03-31.md`、`docs/code_review_2026-04-04.md`、`docs/code_review_2026-04-05.md`、`docs/code_review_missile_2026-04-18.md`

---

## Technical Reference

完整的目录结构、NeoForge 21.1.220 API 坑、关键技术要点、配置系统、外部依赖均已移至 **`docs/technical_reference.md`**。开发前请先查阅该文件。

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
- 原始策划案：`docs/总策划案.docx`
- GitHub: https://github.com/CVIndomitable/piranport_cl.git
