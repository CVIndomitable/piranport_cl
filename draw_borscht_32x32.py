#!/usr/bin/env python3
"""
罗宋汤物品贴图重绘 32×32
参考战舰少女R原画 Cookbook_3.png：白色碗托、深红色汤、番茄块、顶部绿叶
按菜品规范：纯黑描边、32×32像素、对齐原画构图
"""
from PIL import Image

# 创建32×32透明画布
img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
pixels = img.load()

def set_pixel(x, y, color):
    """安全设置像素"""
    if 0 <= x < 32 and 0 <= y < 32:
        pixels[x, y] = color

# 颜色定义
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
GRAY_LIGHT = (220, 220, 220, 255)
GRAY_MID = (180, 180, 180, 255)
GRAY_DARK = (140, 140, 140, 255)

# 碗壁
BOWL_LIGHT = (240, 240, 245, 255)
BOWL_MID = (200, 200, 210, 255)
BOWL_SHADOW = (160, 160, 170, 255)

# 汤液（深红色，甜菜根特色）
SOUP_DARK = (150, 30, 35, 255)
SOUP_MID = (180, 45, 50, 255)
SOUP_LIGHT = (200, 60, 65, 255)
SOUP_HIGHLIGHT = (220, 80, 85, 255)

# 食材
TOMATO_DARK = (180, 50, 45, 255)
TOMATO_LIGHT = (210, 70, 60, 255)
BEEF_DARK = (90, 55, 45, 255)
BEEF_LIGHT = (120, 75, 60, 255)
POTATO = (210, 190, 140, 255)
CREAM = (245, 240, 230, 255)

# 绿叶装饰
LEAF_DARK = (60, 120, 70, 255)
LEAF_LIGHT = (80, 150, 90, 255)

# ===== 碗托底座（椭圆形，白色） =====
# 底座外轮廓（y=22-28，椭圆）
plate_outline = [
    # y=28 (底边)
    (12, 28), (13, 28), (14, 28), (15, 28), (16, 28), (17, 28), (18, 28), (19, 28),
    # y=27
    (10, 27), (11, 27), (20, 27), (21, 27),
    # y=26
    (9, 26), (22, 26),
    # y=25
    (8, 25), (23, 25),
    # y=24
    (7, 24), (24, 24),
    # y=23
    (7, 23), (24, 23),
    # y=22 (顶边，与碗底接触)
    (8, 22), (9, 22), (10, 22), (21, 22), (22, 22), (23, 22),
]
for x, y in plate_outline:
    set_pixel(x, y, BLACK)

# 底座填充（灰白色渐变）
for y in range(23, 28):
    for x in range(8, 24):
        if pixels[x, y] == (0, 0, 0, 0):  # 未被描边占用
            if x < 15:
                set_pixel(x, y, GRAY_LIGHT if y < 25 else WHITE)
            elif x < 18:
                set_pixel(x, y, WHITE if y < 26 else GRAY_LIGHT)
            else:
                set_pixel(x, y, GRAY_MID if y > 25 else GRAY_LIGHT)

# ===== 碗体（圆形，俯视） =====
# 碗外壁黑色描边（y=6-22，圆形）
bowl_outline_coords = [
    # y=6 (顶边)
    (11, 6), (12, 6), (13, 6), (14, 6), (15, 6), (16, 6), (17, 6), (18, 6), (19, 6), (20, 6),
    # y=7
    (9, 7), (10, 7), (21, 7), (22, 7),
    # y=8
    (8, 8), (23, 8),
    # y=9-20 (两侧)
    (7, 9), (24, 9),
    (6, 10), (25, 10),
    (6, 11), (25, 11),
    (5, 12), (26, 12),
    (5, 13), (26, 13),
    (5, 14), (26, 14),
    (5, 15), (26, 15),
    (5, 16), (26, 16),
    (6, 17), (25, 17),
    (6, 18), (25, 18),
    (7, 19), (24, 19),
    (8, 20), (23, 20),
    # y=21
    (9, 21), (10, 21), (21, 21), (22, 21),
    # y=22 (底边)
    (11, 22), (12, 22), (13, 22), (14, 22), (15, 22), (16, 22), (17, 22), (18, 22), (19, 22), (20, 22),
]
for x, y in bowl_outline_coords:
    set_pixel(x, y, BLACK)

# 碗内壁高光（左上区域）
for y in range(7, 12):
    for x in range(8, 16):
        if pixels[x, y] == (0, 0, 0, 0):
            set_pixel(x, y, BOWL_LIGHT if (x < 12 and y < 10) else BOWL_MID)

# 碗外壁暗部（右下）
for y in range(17, 22):
    for x in range(20, 25):
        if pixels[x, y] == (0, 0, 0, 0):
            set_pixel(x, y, BOWL_SHADOW)

# ===== 汤液（深红色甜菜根汤） =====
# 汤面填充（y=12-20）
for y in range(12, 21):
    for x in range(6, 26):
        if pixels[x, y] == (0, 0, 0, 0):
            # 左上高光区
            if x < 14 and y < 15:
                set_pixel(x, y, SOUP_LIGHT if (x < 12 or y == 12) else SOUP_MID)
            # 中心区域
            elif 14 <= x < 20 and 14 <= y < 18:
                set_pixel(x, y, SOUP_MID)
            # 深色区域（左下、右侧）
            else:
                set_pixel(x, y, SOUP_DARK if (x < 10 or x > 22) else SOUP_MID)

# ===== 食材细节 =====
# 番茄块（右侧，红色方块）
tomato_coords = [
    (20, 14, BLACK), (21, 14, BLACK), (22, 14, BLACK),
    (20, 15, BLACK), (21, 15, TOMATO_LIGHT), (22, 15, BLACK),
    (20, 16, BLACK), (21, 16, TOMATO_DARK), (22, 16, BLACK),
    (20, 17, BLACK), (21, 17, BLACK), (22, 17, BLACK),
]
for x, y, c in tomato_coords:
    set_pixel(x, y, c)

# 牛肉块（左下，深棕色）
beef_coords = [
    (9, 17, BLACK), (10, 17, BLACK), (11, 17, BLACK),
    (9, 18, BLACK), (10, 18, BEEF_LIGHT), (11, 18, BLACK),
    (9, 19, BLACK), (10, 19, BEEF_DARK), (11, 19, BLACK),
    (9, 20, BLACK), (10, 20, BLACK), (11, 20, BLACK),
]
for x, y, c in beef_coords:
    set_pixel(x, y, c)

# 土豆块（中上，浅黄）
potato_coords = [
    (14, 13, BLACK), (15, 13, BLACK),
    (14, 14, BLACK), (15, 14, POTATO), (16, 14, BLACK),
    (14, 15, BLACK), (15, 15, BLACK),
]
for x, y, c in potato_coords:
    set_pixel(x, y, c)

# 酸奶油漩涡（中心白色点）
cream_coords = [
    (15, 16, CREAM), (16, 16, CREAM),
    (14, 17, CREAM), (15, 17, CREAM),
]
for x, y, c in cream_coords:
    set_pixel(x, y, c)

# ===== 绿叶装饰（顶部，欧芹） =====
# 左叶片
leaf_left = [
    (12, 9, BLACK), (13, 9, BLACK),
    (11, 10, BLACK), (12, 10, LEAF_LIGHT), (13, 10, LEAF_DARK), (14, 10, BLACK),
    (11, 11, BLACK), (12, 11, LEAF_DARK), (13, 11, BLACK),
]
for x, y, c in leaf_left:
    set_pixel(x, y, c)

# 右叶片
leaf_right = [
    (17, 10, BLACK), (18, 10, BLACK),
    (17, 11, BLACK), (18, 11, LEAF_LIGHT), (19, 11, LEAF_DARK), (20, 11, BLACK),
    (18, 12, BLACK), (19, 12, LEAF_DARK), (20, 12, BLACK),
]
for x, y, c in leaf_right:
    set_pixel(x, y, c)

# 叶柄（中心茎）
stem_coords = [
    (15, 10, LEAF_DARK), (16, 10, LEAF_DARK),
    (15, 11, LEAF_DARK), (16, 11, LEAF_DARK),
]
for x, y, c in stem_coords:
    set_pixel(x, y, c)

# 保存
output_path = 'src/main/resources/assets/piranport/textures/item/borscht.png'
img.save(output_path)
print(f"✓ 罗宋汤贴图已生成 (32×32): {output_path}")

# 生成预览
preview = img.resize((256, 256), Image.NEAREST)
preview_path = 'preview_borscht_32x32.png'
preview.save(preview_path)
print(f"✓ 预览图: {preview_path}")
