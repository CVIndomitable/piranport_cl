#!/usr/bin/env python3
"""
奥托双联76毫米速射炮 - 32x32像素贴图
参考：舰R百科 Equip_L_402 原画
特征：现代化紧凑型双联炮塔，炮管较短粗，炮塔呈方形
画风：对齐项目中 japanese_127mm_twin_gun / german_twin_380mm_gun
"""
from PIL import Image, ImageDraw
import numpy as np

# 32x32画布
img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
pixels = img.load()

def set_pixel(x, y, color):
    if 0 <= x < 32 and 0 <= y < 32:
        pixels[x, y] = color

# 配色方案（现代化灰色系）
dark_gray = (60, 60, 65, 255)      # 主体深灰
mid_gray = (95, 95, 100, 255)       # 中间色
light_gray = (130, 130, 135, 255)   # 亮面
metal_shine = (165, 165, 170, 255)  # 金属高光
shadow = (35, 35, 40, 255)          # 阴影
barrel_dark = (50, 50, 55, 255)     # 炮管暗部
barrel_light = (110, 110, 115, 255) # 炮管亮部

# ===== 炮塔基座（方形紧凑型）=====
# 底部基座轮廓（15x8）
for x in range(9, 24):
    for y in range(20, 28):
        if x == 9 or x == 23 or y == 20 or y == 27:
            set_pixel(x, y, shadow)
        elif x < 16:
            set_pixel(x, y, mid_gray)
        else:
            set_pixel(x, y, dark_gray)

# 基座顶部高光
for x in range(10, 16):
    set_pixel(x, 21, light_gray)

# ===== 主炮塔体（紧凑方形设计）=====
# 炮塔主体（12x7）
for x in range(10, 22):
    for y in range(13, 20):
        if x == 10 or x == 21 or y == 13:
            set_pixel(x, y, shadow)
        elif x < 16:
            set_pixel(x, y, light_gray)
        else:
            set_pixel(x, y, mid_gray)

# 炮塔顶部细节（通风/观瞄设备）
for x in range(12, 20):
    set_pixel(x, 14, metal_shine)
for x in range(14, 18):
    set_pixel(x, 13, dark_gray)

# ===== 双联炮管（短粗型，76mm特征）=====
# 左炮管（上方）
for y in range(6, 17):
    # 炮管主体（宽度3）
    set_pixel(12, y, shadow)
    set_pixel(13, y, barrel_dark)
    set_pixel(14, y, barrel_light)

# 右炮管（下方稍错开）
for y in range(8, 18):
    set_pixel(17, y, shadow)
    set_pixel(18, y, barrel_dark)
    set_pixel(19, y, barrel_light)

# 炮口制退器（短粗特征）
for x in range(12, 15):
    set_pixel(x, 5, dark_gray)
    set_pixel(x, 6, shadow)
for x in range(17, 20):
    set_pixel(x, 7, dark_gray)
    set_pixel(x, 8, shadow)

# 炮管根部连接
for x in range(12, 20):
    if x < 15 or x >= 17:
        set_pixel(x, 16, mid_gray)
        set_pixel(x, 17, shadow)

# ===== 炮塔侧面细节（现代化装甲板）=====
# 左侧装甲板
for y in range(15, 19):
    set_pixel(11, y, light_gray)
    set_pixel(10, y, shadow)

# 右侧散热口/观察窗
for y in range(15, 17):
    set_pixel(20, y, shadow)
    set_pixel(21, y, dark_gray)

# ===== 细节强化 =====
# 炮管分离线
for y in range(10, 16):
    set_pixel(15, y, shadow)
    set_pixel(16, y, shadow)

# 炮塔顶部边缘高光
for x in range(11, 21):
    if pixels[x, 14][3] > 0:
        r, g, b, a = pixels[x, 14]
        set_pixel(x, 14, (min(255, r+20), min(255, g+20), min(255, b+20), a))

# 基座底部阴影加深
for x in range(9, 24):
    set_pixel(x, 27, shadow)

img.save('preview_oto_twin_76mm.png')
print("✓ 奥托双联76mm速射炮贴图已生成: preview_oto_twin_76mm.png")
print("  特征: 现代化紧凑方形炮塔 + 短粗双联炮管")
