#!/usr/bin/env python3
"""重画扁豆汤 - 16×16 MC原版风格"""
from PIL import Image, ImageDraw
import numpy as np

# 16×16画布
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pixels = img.load()

# 调色板 - MC原版暖色调
bowl_dark = (80, 60, 50, 255)      # 碗外侧深棕
bowl_mid = (120, 90, 70, 255)      # 碗中间色
bowl_light = (160, 120, 90, 255)   # 碗高光
soup_base = (200, 180, 140, 255)   # 汤底色（奶白扁豆汤）
soup_shadow = (160, 140, 100, 255) # 汤阴影
bean_green = (100, 130, 80, 255)   # 扁豆绿色
bean_dark = (70, 90, 60, 255)      # 扁豆暗部

# 碗的轮廓（俯视角度，椭圆形碗）
# 外圈
for x in range(2, 14):
    pixels[x, 4] = bowl_dark
    pixels[x, 14] = bowl_dark
pixels[1, 5] = bowl_dark
pixels[1, 6] = bowl_dark
pixels[14, 5] = bowl_dark
pixels[14, 6] = bowl_dark
for y in range(7, 14):
    pixels[1, y] = bowl_dark
    pixels[14, y] = bowl_dark
pixels[2, 13] = bowl_dark
pixels[13, 13] = bowl_dark

# 碗内侧
for x in range(3, 13):
    pixels[x, 5] = bowl_mid
    pixels[x, 13] = bowl_mid
for y in range(6, 13):
    pixels[2, y] = bowl_mid
    pixels[13, y] = bowl_mid

# 碗内部高光（左上）
pixels[3, 6] = bowl_light
pixels[4, 6] = bowl_light
pixels[3, 7] = bowl_light

# 汤的表面（奶白色扁豆汤）
for y in range(7, 12):
    for x in range(4, 12):
        if y == 7 and x in [4, 5, 10, 11]:
            continue  # 边缘留空
        pixels[x, y] = soup_base

# 汤的阴影（右下角）
for y in range(10, 12):
    for x in range(9, 12):
        pixels[x, y] = soup_shadow
pixels[8, 11] = soup_shadow

# 浮在汤面的扁豆（3-4颗）
# 扁豆1（左上）
pixels[5, 8] = bean_green
pixels[6, 8] = bean_green
pixels[5, 9] = bean_dark
pixels[6, 9] = bean_green

# 扁豆2（中间）
pixels[7, 9] = bean_green
pixels[8, 9] = bean_green
pixels[7, 10] = bean_dark
pixels[8, 10] = bean_green

# 扁豆3（右边）
pixels[10, 9] = bean_green
pixels[10, 10] = bean_dark

# 扁豆4（左下小颗）
pixels[5, 10] = bean_green

# 汤面高光（左上角光泽）
pixels[4, 8] = (220, 200, 160, 255)
pixels[5, 7] = (220, 200, 160, 255)

img.save('src/main/resources/assets/piranport/textures/item/lablab_soup.png')
print("✓ 扁豆汤贴图已保存 (16×16)")

# 预览
preview = img.resize((128, 128), Image.NEAREST)
preview.save('preview_lablab_soup.png')
print("✓ 预览已保存")
