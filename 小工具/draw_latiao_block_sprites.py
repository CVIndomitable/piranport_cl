#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""辣条方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_23.png（米白方形浅盘里堆着一把
      红亮油润的辣条，长短条交错叠放）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/latiao_*.png
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


# ---------------------------------------------------------------- 方盘(顶)
def plate_top():
    """米白方形浅盘：盘沿提亮、内圈压暗，四角切一像素。"""
    im = new()
    base = hx("#F0ECE0")
    hi = hx("#FAF7EE")
    inner = hx("#E2DCCB")
    shadow = hx("#D8D1BF")
    edge = hx("#C9C2B0")
    for y in range(16):
        for x in range(16):
            if (x in (0, 15)) and (y in (0, 15)):
                continue  # 切角
            c = base
            if x in (0, 15) or y in (0, 15):
                c = edge
            elif x in (1, 14) or y in (1, 14):
                c = hi
            elif x in (2, 13) or y in (2, 13):
                c = inner
            elif x + y > 21:
                c = shadow
            put(im, x, y, c)
    # 内圈阴影线
    for t in range(3, 13):
        put(im, t, 3, inner)
        put(im, 3, t, hi)
    save(im, "latiao_plate_top.png")


# ---------------------------------------------------------------- 方盘(侧)
def plate_side():
    im = new()
    top = hx("#F7F3E8")
    bot = hx("#D2CBB8")
    hi = hx("#FDFAF2")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    for y in range(16):
        put(im, 0, y, lerp(lerp(top, bot, (y / 15.0) ** 1.1), hi, 0.4))
    save(im, "latiao_plate_side.png")


# ---------------------------------------------------------------- 辣条(顶)
def strip_top():
    """辣条顶面：红亮油润，沿长边的高光带 + 纤维纹 + 深色卷边。"""
    im = new()
    rng = random.Random(2323)
    base = hx("#C93E22")
    light = hx("#E8643A")
    gloss = hx("#F08A5A")
    dark = hx("#9A2A14")
    deep = hx("#6E1A0A")
    for y in range(16):
        for x in range(16):
            c = base
            if y in (6, 7, 8):
                c = gloss
            elif y in (5, 9):
                c = light
            elif y <= 2 or y >= 13:
                c = dark
            if x <= 1 or x >= 14:
                c = deep
            put(im, x, y, c)
    # 纤维纹与油光点
    for _ in range(14):
        x = rng.randrange(2, 14)
        y = rng.randrange(3, 13)
        put(im, x, y, rng.choice([light, dark, gloss]))
    for x in range(3, 13, 3):
        put(im, x, 7, hx("#FFB07A"))
    save(im, "latiao_strip_top.png")


# ---------------------------------------------------------------- 辣条(侧)
def strip_side():
    im = new()
    rng = random.Random(2324)
    top = hx("#B8361E")
    bot = hx("#6E1A0A")
    hi = hx("#D9552E")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            put(im, x, y, cc)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(3, 13)
        put(im, x, y, rng.choice([hi, bot]))
    save(im, "latiao_strip_side.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    strip_top()
    strip_side()
