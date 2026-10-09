#!/usr/bin/env python3
"""
罗宋汤物品贴图重绘
按照菜品重画规范：纯黑描边、16×16像素、参考已有菜品画风
构图：白色椭圆盘子 + 深红色汤碗 + 汤料细节（牛肉块、土豆、番茄、甜菜根）
"""
from PIL import Image, ImageDraw

# 创建16×16透明画布
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pixels = img.load()

def set_pixel(x, y, color):
    """安全设置像素"""
    if 0 <= x < 16 and 0 <= y < 16:
        pixels[x, y] = color

# 颜色定义
BLACK = (0, 0, 0, 255)           # 纯黑描边
PLATE_LIGHT = (230, 230, 230, 255)  # 盘子高光
PLATE_MID = (200, 200, 200, 255)    # 盘子中调
PLATE_SHADOW = (160, 160, 160, 255) # 盘子暗部

BOWL_DARK = (180, 180, 190, 255)    # 碗外壁
BOWL_LIGHT = (210, 210, 220, 255)   # 碗内壁高光

SOUP_DEEP = (140, 25, 30, 255)      # 汤底深红（甜菜根色）
SOUP_MID = (170, 35, 40, 255)       # 汤中调
SOUP_LIGHT = (190, 50, 55, 255)     # 汤高光

BEEF = (100, 60, 50, 255)           # 牛肉块
POTATO = (220, 200, 150, 255)       # 土豆块
TOMATO = (200, 60, 50, 255)         # 番茄块
CREAM = (245, 240, 230, 255)        # 酸奶油点缀

# ===== 椭圆盘子底座 =====
# 盘子轮廓（椭圆，俯视角度）
plate_outline = [
    (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),  # 底边
    (4, 11), (11, 11),
    (3, 10), (12, 10),
    (2, 9), (13, 9),
    (2, 8), (13, 8),
]
for x, y in plate_outline:
    set_pixel(x, y, BLACK)

# 盘子填充（浅灰白色，带光泽渐变）
plate_fill = [
    # y=11
    (5, 11, PLATE_LIGHT), (6, 11, PLATE_LIGHT), (7, 11, PLATE_LIGHT),
    (8, 11, PLATE_MID), (9, 11, PLATE_MID), (10, 11, PLATE_MID),
    # y=10
    (4, 10, PLATE_LIGHT), (5, 10, PLATE_LIGHT), (6, 10, PLATE_LIGHT),
    (7, 10, PLATE_MID), (8, 10, PLATE_MID), (9, 10, PLATE_MID),
    (10, 10, PLATE_SHADOW), (11, 10, PLATE_SHADOW),
    # y=9
    (3, 9, PLATE_MID), (4, 9, PLATE_MID), (5, 9, PLATE_MID),
    (6, 9, PLATE_MID), (7, 9, PLATE_SHADOW), (8, 9, PLATE_SHADOW),
    (9, 9, PLATE_SHADOW), (10, 9, PLATE_SHADOW), (11, 9, PLATE_SHADOW), (12, 9, PLATE_SHADOW),
    # y=8
    (3, 8, PLATE_MID), (4, 8, PLATE_MID), (5, 8, PLATE_SHADOW),
    (6, 8, PLATE_SHADOW), (7, 8, PLATE_SHADOW), (8, 8, PLATE_SHADOW),
    (9, 8, PLATE_SHADOW), (10, 8, PLATE_SHADOW), (11, 8, PLATE_SHADOW), (12, 8, PLATE_SHADOW),
]
for x, y, c in plate_fill:
    set_pixel(x, y, c)

# ===== 汤碗（圆形，俯视） =====
# 碗外壁黑色描边
bowl_outline = [
    (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3),  # 顶边
    (4, 4), (11, 4),
    (3, 5), (12, 5),
    (3, 6), (12, 6),
    (3, 7), (12, 7),
    (4, 8), (11, 8),
    (5, 9), (10, 9),
]
for x, y in bowl_outline:
    set_pixel(x, y, BLACK)

# 碗内壁高光（左上）
bowl_rim = [
    (4, 4, BOWL_LIGHT), (5, 4, BOWL_LIGHT), (6, 4, BOWL_LIGHT),
    (4, 5, BOWL_LIGHT), (5, 5, BOWL_LIGHT),
]
for x, y, c in bowl_rim:
    set_pixel(x, y, c)

# 碗外壁暗部（右下）
bowl_shadow = [
    (10, 4, BOWL_DARK), (11, 5, BOWL_DARK), (11, 6, BOWL_DARK),
    (11, 7, BOWL_DARK), (10, 8, BOWL_DARK),
]
for x, y, c in bowl_shadow:
    set_pixel(x, y, c)

# ===== 汤液（深红色，甜菜根特色） =====
# 汤底深红
soup_base = [
    # y=5
    (4, 5, SOUP_DEEP), (6, 5, SOUP_DEEP), (7, 5, SOUP_DEEP),
    (8, 5, SOUP_MID), (9, 5, SOUP_MID), (10, 5, SOUP_MID), (11, 5, SOUP_MID),
    # y=6
    (4, 6, SOUP_DEEP), (5, 6, SOUP_DEEP), (6, 6, SOUP_MID),
    (7, 6, SOUP_MID), (8, 6, SOUP_MID), (9, 6, SOUP_LIGHT),
    (10, 6, SOUP_MID), (11, 6, SOUP_MID),
    # y=7
    (4, 7, SOUP_DEEP), (5, 7, SOUP_DEEP), (6, 7, SOUP_MID),
    (7, 7, SOUP_MID), (8, 7, SOUP_MID), (9, 7, SOUP_MID),
    (10, 7, SOUP_MID), (11, 7, SOUP_MID),
    # y=8
    (5, 8, SOUP_DEEP), (6, 8, SOUP_DEEP), (7, 8, SOUP_DEEP),
    (8, 8, SOUP_MID), (9, 8, SOUP_MID),
]
for x, y, c in soup_base:
    set_pixel(x, y, c)

# ===== 汤料细节 =====
# 牛肉块（左侧，深棕色）
set_pixel(5, 6, BLACK)  # 描边
set_pixel(6, 6, BEEF)
set_pixel(5, 7, BEEF)

# 土豆块（中上，浅黄）
set_pixel(7, 5, BLACK)  # 描边
set_pixel(8, 5, POTATO)
set_pixel(7, 6, POTATO)

# 番茄块（右侧，橙红）
set_pixel(10, 6, BLACK)  # 描边
set_pixel(9, 6, TOMATO)
set_pixel(10, 7, TOMATO)

# 酸奶油点缀（中心白色漩涡）
set_pixel(8, 6, CREAM)
set_pixel(7, 7, CREAM)

# 保存
output_path = 'src/main/resources/assets/piranport/textures/item/borscht.png'
img.save(output_path)
print(f"✓ 罗宋汤贴图已生成: {output_path}")

# 生成预览
preview = img.resize((256, 256), Image.NEAREST)
preview_path = 'preview_borscht.png'
preview.save(preview_path)
print(f"✓ 预览图: {preview_path}")
