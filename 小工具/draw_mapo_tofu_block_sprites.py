#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""麻婆豆腐方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_11.png（白色椭圆浅盘，红亮辣油里
      浮着大量淡黄豆腐块，撒青葱与深色肉末/花椒）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 盘沿/辣油边缘的圆形轮廓靠贴图透明角 + render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/mapo_tofu_*.png
"""
import math
import os
import random
from PIL import Image

OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport/textures/block",
)


def hx(s):
    """#RRGGBB 或 #RRGGBBAA -> (r,g,b,a)"""
    s = s.lstrip("#")
    if len(s) == 8:
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), int(s[6:8], 16))
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(im, x, y, c):
    if c is None:
        return
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c if len(c) == 4 else (*c, 255))


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def save(im, name):
    path = os.path.join(OUT_DIR, name)
    im.save(path)
    print("saved", path)


# ---------------------------------------------------------------- 盘沿(顶)
def bowl_top():
    """白瓷盘沿俯视：一圈白边，中央镂空（透出下方辣油）。"""
    im = new()
    white = hx("#FBFBF9")
    hi = hx("#FFFFFF")
    rim = hx("#F1EFEB")
    lip = hx("#E4E0DA")
    edge = hx("#D5D1CC")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx * 0.97, dy)
            if r < 6.8:
                continue  # 中央透明：露出辣油
            if r > 7.85:
                continue  # 圆角
            if r > 7.35:
                c = edge
            elif r > 6.95:
                c = white
            elif r > 6.8:
                c = lip  # 内沿阴影
            else:
                c = rim
            if dx < -1 and dy < -1 and r > 6.9:
                c = hi
            put(im, x, y, c)
    save(im, "mapo_tofu_bowl_top.png")


# ---------------------------------------------------------------- 盘沿(侧)
def bowl_side():
    im = new()
    top = hx("#FBFAF7")
    bot = hx("#D9D6D1")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.14
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.15)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.15)
        put(im, 2, y, lerp(c, hx("#E8E5E0"), 0.5))
        put(im, 13, y, lerp(c, hx("#E8E5E0"), 0.5))
    save(im, "mapo_tofu_bowl_side.png")


# ---------------------------------------------------------------- 辣油
def sauce():
    """红亮辣油表面：橙红渐变 + 油光 + 辣椒/花椒碎点 + 隐约豆腐色斑。"""
    im = new()
    rng = random.Random(1111)
    base = hx("#C3351C")
    light = hx("#DE5A2C")
    bright = hx("#F08A4A")
    dark = hx("#9E2410")
    deep = hx("#7A1608")
    pale = hx("#EBCF9C")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.9:
                continue
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 26.0))
            if r > 7.1:
                c = lerp(c, deep, 0.45)
            put(im, x, y, c)
    # 油光与深色碎料
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([bright, bright, light]))
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([deep, dark, dark]))
    # 隐约透出的豆腐块边缘
    for _ in range(10):
        x, y = rng.randrange(15), rng.randrange(15)
        if im.getpixel((x, y))[3]:
            put(im, x, y, pale)
    save(im, "mapo_tofu_sauce.png")


# ---------------------------------------------------------------- 豆腐块(顶)
def tofu_top():
    im = new()
    base = hx("#F3E7B8")
    hi = hx("#FBF4D6")
    shade = hx("#DCCB92")
    # 轻微切角，让方块边角不那么硬
    for y in range(16):
        for x in range(16):
            corner = min(x, y, 15 - x, 15 - y)
            if corner == 0 and (x in (0, 15) or y in (0, 15)):
                # 只切最外侧一像素的角点
                if (x == 0 or x == 15) and (y == 0 or y == 15):
                    continue
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    # 豆腐表面的细孔与光泽
    for p in ((4, 5), (10, 4), (6, 10), (11, 11), (8, 7)):
        put(im, p[0], p[1], hx("#E8DAA6"))
    save(im, "mapo_tofu_tofu_top.png")


# ---------------------------------------------------------------- 豆腐块(侧)
def tofu_side():
    im = new()
    top = hx("#F1E4B2")
    bot = hx("#D5C187")
    hi = hx("#FAF1D0")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.4)
            elif y >= 13:
                cc = lerp(c, bot, 0.4)
            put(im, x, y, cc)
    save(im, "mapo_tofu_tofu_side.png")


# ---------------------------------------------------------------- 葱花 / 肉末
def scallion():
    im = new()
    dark = hx("#3E6A1E")
    base = hx("#5E8C2A")
    hi = hx("#83AE3E")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 10:
                c = hi
            elif x + y > 22:
                c = dark
            put(im, x, y, c)
    save(im, "mapo_tofu_scallion.png")


def bits():
    im = new()
    dark = hx("#3A1A0C")
    base = hx("#4A2412")
    hi = hx("#6B3A22")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 21:
                c = dark
            put(im, x, y, c)
    save(im, "mapo_tofu_bits.png")


if __name__ == "__main__":
    bowl_top()
    bowl_side()
    sauce()
    tofu_top()
    tofu_side()
    scallion()
    bits()
