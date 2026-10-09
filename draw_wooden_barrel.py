#!/usr/bin/env python3
"""重画木桶贴图 - 16×16 参考皇家海军咸牛肉画风"""
from PIL import Image, ImageDraw
import numpy as np

# 16×16 画布
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pixels = img.load()

# 参考咸牛肉：木桶主体棕色调，铁箍灰色，整体立体暖色
barrel_wood = (139, 90, 60)      # 木桶主体（深棕）
barrel_highlight = (180, 120, 80) # 高光（浅棕）
barrel_shadow = (90, 60, 40)     # 阴影（暗棕）
iron_band = (110, 110, 120)      # 铁箍（灰）
iron_dark = (70, 70, 80)         # 铁箍暗部

# 木桶轮廓 - 上下窄中间宽的桶形（正视图）
# 顶部开口圆形
for x in range(5, 11):
    for y in range(1, 3):
        pixels[x, y] = barrel_shadow

# 桶身主体
for y in range(3, 13):
    width_offset = 1 if 5 <= y <= 10 else 0  # 中间部分稍宽
    for x in range(4 - width_offset, 12 + width_offset):
        pixels[x, y] = barrel_wood

# 高光条纹（木板拼接纹理）
for y in range(4, 12):
    pixels[6, y] = barrel_highlight
    pixels[9, y] = barrel_highlight

# 阴影侧面
for y in range(3, 13):
    if 4 <= y <= 11:
        pixels[4, y] = barrel_shadow
        pixels[11, y] = barrel_shadow

# 铁箍（顶部和底部）
for x in range(4, 12):
    pixels[x, 3] = iron_band
    pixels[x, 12] = iron_band

# 铁箍高光
for x in range(6, 10):
    pixels[x, 3] = (140, 140, 150)
    pixels[x, 12] = (140, 140, 150)

# 铁箍暗边
pixels[4, 3] = iron_dark
pixels[11, 3] = iron_dark
pixels[4, 12] = iron_dark
pixels[11, 12] = iron_dark

# 底部圆形
for x in range(5, 11):
    pixels[x, 13] = barrel_shadow
    pixels[x, 14] = barrel_shadow

# 预览并保存
img = img.resize((160, 160), Image.NEAREST)
img.save('preview_wooden_barrel.png')
print("✓ 预览已保存: preview_wooden_barrel.png")

# 保存最终 16×16
img_final = img.resize((16, 16), Image.NEAREST)
img_final.save('src/main/resources/assets/piranport/textures/item/wooden_barrel.png')
print("✓ 贴图已保存: wooden_barrel.png (16×16)")
