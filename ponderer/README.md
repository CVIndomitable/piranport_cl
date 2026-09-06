# Piran Port Ponderer 教程包

适用于皮兰港测试版 Minecraft 1.21.1 + NeoForge，当前验证版本为 Ponderer 1.10.6.1（内含 Ponder 1.0.69）。

## 安装与软依赖

1. 客户端安装皮兰港及 Ponderer 的 **1.21.1 NeoForge** 版本。
2. 启动客户端。皮兰港检测到 Ponderer 后，会自动将内嵌教程安装到 `resourcepacks/[Ponderer] Piran Port.zip`，无需额外下载或启用资源包。
3. 首次启动即可加载教程，不需要执行命令。仅在游戏运行中手动修改教程后，才需要 `/ponderer reload`。
4. 打开背包，将鼠标悬停在石磨、厨锅或砧板物品上。应出现“按住 W 以思索”提示，**持续按住 W** 直到进入教程。若改过键位，以控制设置中的 Ponder 思索键为准。

Ponderer 是可选客户端依赖；未安装时，皮兰港照常运行，不释放教程文件，保留 Patchouli 等文字教程。服务器不运行安装器。皮兰港不会自动下载 Ponderer，玩家仍需自行安装该模组；教程 ZIP 已内嵌在皮兰港 JAR 的 `META-INF/resourcepacks/piranport-ponderer.zip` 中。

Ponderer 直接扫描 `resourcepacks/` 中的教程 ZIP，不要求先在原版资源包界面启用。这里的教程绑定的是背包物品，不是世界中的准星目标；`C` 是另一套场景触发入口，不作为本教程的 W 替代键。

## 开发与打包

- 开发客户端需要将 Ponderer JAR 放在 `run/mods/`；避免重复安装其内嵌的 Ponder/Flywheel。
- `./gradlew pondererPack` 从本目录的 `pack.json`、`pack.mcmeta` 和 `data/` 构建发布 ZIP。
- `processResources` 将生成的 ZIP 放进 `META-INF/resourcepacks/`，`./gradlew build` 生成的模组 JAR 已包含教程，不需要同时分发独立 ZIP。
- `./gradlew runClient` 使用同一个内嵌资源和客户端安装器，不再由 Gradle 预先复制教程到 `run/resourcepacks/`，避免开发环境掩盖正式发布的安装问题。
- Ponderer 可从 Modrinth Maven 获取。本项目的回归测试使用 `maven.modrinth:the-ponderer:YzFWxoks`，对应 1.10.6.1 的 NeoForge 构建；这是仅测试依赖，不会打入模组。
- `./gradlew test --tests 'com.piranport.compat.ponderer.*'` 检查正式 JAR 内嵌资源、首次安装、重复启动、升级、手改保护，并调用 Ponderer 实际的 `PonderPackInfo.fromZip`，检查脚本解析、双语文本和字幕等待间隔。

当前共 6 个脚本，覆盖 8 个物品：3 个工作台系列（弹药工作台、武器工作台）、加工三件套（石磨/厨锅/砧板）、再装填设施、舰核改装台、烟雾弹。任一绑定物品都能进入其脚本对应的章节。

| 脚本 ID | 绑定物品 | 章节数 |
|---|---|---|
| `piranport:ammo_workbench` | `ammo_workbench` | 2（五大分类、自动化） |
| `piranport:weapon_workbench` | `weapon_workbench` | 2（GUI 流程、占用保护） |
| `piranport:reload_facility` | `reload_facility` | 2（四槽布局、装填规则与比较器） |
| `piranport:ship_core_modifier` | `ship_core_modifier` | 1（投入/改装/取出） |
| `piranport:smoke_candle` | `smoke_candle` | 2（3×3×2 烟雾区、隐身与衰减） |
| `piranport:processing` | `stone_mill`, `cooking_pot`, `cutting_board` | 3 |

## 安装时序与更新

- 安装器位于 `com.piranport.compat.ponderer`，只订阅客户端的 `FMLCommonSetupEvent`，同步完成文件安装。Ponderer 1.10.6.1 在之后的 `FMLClientSetupEvent` 延迟任务中扫描资源包，因此首次启动就能发现文件。
- 生产代码不引用 Ponderer/Ponder 的类，无需编译期 API 依赖。是否安装 Ponderer 由 `ModList` 检查；文件权限等安装异常只记日志，不中断皮兰港启动。
- 自动安装记录位于 `config/piranport/ponderer-pack.sha256`。文件内容没有变化时，不重写 ZIP；升级时，仅替换仍与上次记录一致的 ZIP，使用临时文件及原子移动减少半写文件风险。
- 已存在、内容相同的手动安装包会被接管。没有安装记录的不同内容、玩家改过的 ZIP、同名目录或符号链接均会保留，并在日志提示跳过自动更新。
- 遇到保留提示，先备份自定义内容，再将同名文件移出 `resourcepacks/` 或改成非 `.zip` 扩展名，重启即可恢复内置教程。不要留下同一内部包名的多个 ZIP，以免 Ponderer 按修改时间选中另一份。
- 安装器不改动 Ponderer 的可编辑导入副本。该副本可能优先于资源包，更新时需在 Ponderer 中自行处理；不要删除仍有个人编辑内容的副本。

## 排查没有 W 提示

先确认该实例加载了 Ponderer 和 Ponder，再在其 `logs/latest.log` 中检查皮兰港的 `[Ponderer]` 安装日志，以及 `PonderPackInfo`、`SceneStore` 的警告。首次安装应先出现 `INSTALLED bundled tutorials`，随后才是场景加载日志。原先包名写成了 `PiranPort`，1.10.6.1 会输出：

```text
Ignoring pack with invalid portable pack name 'PiranPort'
```

这意味着整个包被拒绝，物品上不会出现 W 提示。内部名称现为 `piranport`，必须使用小写安全字符；ZIP 的展示文件名不受此项限制。

`/ponderer reload` 后，有默认示例且没有其他教程的实例应报告加载 **2 个脚本场景**：默认示例和 `piranport:processing`。后三个章节属于同一个脚本，不能用加载数是否为 3 判断是否成功。

检查绑定物品时可依次执行：

```text
/give @s piranport:ammo_workbench
/give @s piranport:weapon_workbench
/give @s piranport:reload_facility
/give @s piranport:ship_core_modifier
/give @s piranport:smoke_candle
/give @s piranport:stone_mill
/give @s piranport:cooking_pot
/give @s piranport:cutting_board
```

若包已识别但仍是旧内容，检查 Ponderer 是否存在此前手动导入的可编辑副本；该副本可能优先于 ZIP 的只读内容，不要直接删除有自己编辑内容的副本。`pack.json not found` 则需检查实际被扫描 ZIP 的根目录结构，并不必然是运行目录错误。
