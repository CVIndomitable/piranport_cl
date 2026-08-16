# 舰娘实体纹理目录

本目录存放舰娘实体的 64×64 / 256×256 贴图资源。

## 当前状态（2026-08-16）

### `kitchen_goddess.png`（女灶神）

- **尺寸**：256×256 RGBA
- **来源**：`tools/kitchen_goddess_texture.py` 自动生成
- **状态**：**程序化占位图**，与 `KitchenGoddessModel` 的颜色调色板一致
  - 主色：红色外套 (166, 38, 48)、深棕发色 (91, 55, 57)、金色装饰 (226, 177, 58)
  - 其余像素透明
- **完成度**：可用于开发联调，但缺乏手绘的细节、阴影渐变和花纹
- **待办**：美术资产需由美工同学基于 `KitchenGoddessModel.java` 的几何参数手绘精修
  - 参考：`女灶神模型使用说明.md`、`女灶神模型审查报告.md`（已并入 git history）

### `unicorn.png`（独角兽）

- **尺寸**：256×256 RGBA
- **来源**：`tools/unicorn_model_tools.py` 自动生成
- **状态**：**程序化占位图**，与 `UnicornModel` 的颜色调色板一致
  - 主色：白蓝发色 (240, 248, 255)、银色蕾丝 (143, 155, 163)、浅蓝 (200, 225, 249)
  - 其余像素透明
- **完成度**：可用于开发联调
- **待办**：与女灶神同理，需要美术同学基于 `UnicornModel.java` 的几何参数手绘精修

## 重新生成方法

```bash
# 生成两个舰娘的程序化占位贴图
cd tools
python3 kitchen_goddess_texture.py
python3 unicorn_model_tools.py
```

这两个脚本会同时输出到：
1. `src/main/resources/assets/piranport/textures/entity/shipgirl/<name>.png` —— 游戏内使用
2. `build/offline-renders/<name>_*_preview.png` —— 离线预览
3. `build/offline-renders/<name>_*_texture_sheet.png` —— UV sheet 检查

## 与代码审查报告的关系

代码审查报告（`docs/代码审查报告/测试版全局代码审查报告260816.md`）中：

- **C1**（女灶神纹理）—— 之前是空缺/小占位，现已通过工具脚本生成 256×256 RGBA 程序化贴图
- **C2**（独角兽纹理）—— 之前是空缺/小占位，现已通过工具脚本生成 256×256 RGBA 程序化贴图

**注意**：这两份贴图只是程序化占位图，已保证游戏能渲染出有颜色的实体而非透明/紫色错误纹理。
**真正的美术资产由美术同学完成**，上面文档是给美术的工作交接说明。