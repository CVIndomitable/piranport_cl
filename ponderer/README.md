# Piran Port Ponderer 教程包

适用于皮兰港测试版 Minecraft 1.21.1 + NeoForge，当前验证版本为 Ponderer 1.10.6.1（内含 Ponder 1.0.69）。

## 安装与软依赖

1. 客户端安装皮兰港及 Ponderer 的 **1.21.1 NeoForge** 版本。
2. 将本目录生成的 `PiranPort-Ponderer-1.0.zip` 放入该实例的 `resourcepacks/` 目录，避免同一教程包有多个 ZIP 副本。
3. 重启客户端，或进入世界执行 `/ponderer reload`，重新扫描资源包并注册场景。
4. 打开背包，将鼠标悬停在石磨、厨锅或砧板物品上。应出现“按住 W 以思索”提示，**持续按住 W** 直到进入教程。若改过键位，以控制设置中的 Ponder 思索键为准。

Ponderer 是可选客户端依赖；未安装时，皮兰港照常运行，保留 Patchouli 等文字教程。可选依赖声明不会自动安装 Ponderer，也不会自动注册教程。当前教程单独以 ZIP 分发，**没有嵌入皮兰港模组 JAR**，发布时需要同时提供教程包。

Ponderer 直接扫描 `resourcepacks/` 中的教程 ZIP，不要求先在原版资源包界面启用。这里的教程绑定的是背包物品，不是世界中的准星目标；`C` 是另一套场景触发入口，不作为本教程的 W 替代键。

## 开发与打包

- 开发客户端需要将 Ponderer JAR 放在 `run/mods/`；避免重复安装其内嵌的 Ponder/Flywheel。
- `./gradlew pondererPack` 从本目录的 `pack.json`、`pack.mcmeta` 和 `data/` 构建发布 ZIP。
- `./gradlew installPondererPack` 还会将生成文件更新到 `run/resourcepacks/[Ponderer] Piran Port.zip`。
- `./gradlew runClient` 会先执行上述打包和复制；`./gradlew assemble` 也会生成发布 ZIP。
- Ponderer 可从 Modrinth Maven 获取。本项目的回归测试使用 `maven.modrinth:the-ponderer:YzFWxoks`，对应 1.10.6.1 的 NeoForge 构建；这是仅测试依赖，不会打入模组。
- `./gradlew test --tests com.piranport.compat.ponderer.PondererPackTest` 调用该版本实际的 `PonderPackInfo.fromZip`，并检查 ZIP 与源码一致、脚本解析、双语文本和字幕等待间隔。

当前一个脚本 `data/ponderer/scripts/piranport_processing.json` 绑定三个物品，包含石磨、厨锅、砧板三个章节。任一绑定物品都能进入这组章节。

## 排查没有 W 提示

先确认该实例加载了 Ponderer 和 Ponder，再在其 `logs/latest.log` 中检查 `PonderPackInfo`、`SceneStore` 的警告。原先包名写成了 `PiranPort`，1.10.6.1 会输出：

```text
Ignoring pack with invalid portable pack name 'PiranPort'
```

这意味着整个包被拒绝，物品上不会出现 W 提示。内部名称现为 `piranport`，必须使用小写安全字符；ZIP 的展示文件名不受此项限制。

`/ponderer reload` 后，有默认示例且没有其他教程的实例应报告加载 **2 个脚本场景**：默认示例和 `piranport:processing`。后三个章节属于同一个脚本，不能用加载数是否为 3 判断是否成功。

检查三个物品时可依次执行：

```text
/give @s piranport:stone_mill
/give @s piranport:cooking_pot
/give @s piranport:cutting_board
```

若包已识别但仍是旧内容，检查 Ponderer 是否存在此前手动导入的可编辑副本；该副本可能优先于 ZIP 的只读内容，不要直接删除有自己编辑内容的副本。`pack.json not found` 则需检查实际被扫描 ZIP 的根目录结构，并不必然是运行目录错误。
