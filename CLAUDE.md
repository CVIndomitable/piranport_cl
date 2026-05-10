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

- **当前版本**: v1.1.4-dev (测试版)
- **稳定版本**: v1.0.0 (main 分支，仅修 bug)
- **开发策略**: main = 稳定版，dev = 测试版新玩法
- **版本路线图**: `../docs/皮兰港 版本路线图.md`
- **技术参考手册**: `../docs/皮兰港技术参考手册.md` — 完整目录结构、API 坑、核心系统详解、配置系统
- **更新日志**: `../docs/版本记录/CHANGELOG-1.1.4-dev.md`
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
