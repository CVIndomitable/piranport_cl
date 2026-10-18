#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""羊羹方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_19.png（淡蓝灰长方瓷盘上，
      一条切成四片的暗红棕羊羹，盘面散着几粒红豆）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 长方盘圆角靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/yokan_*.png
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


# ---------------------------------------------------------------- 长方盘(顶)
def plate_top():
    """淡蓝灰长方瓷盘：圆角矩形 + 釉面高光 + 几粒红豆。"""
    im = new()
    rng = random.Random(1919)
    base = hx("#C4D2D5")
    light = hx("#DDE7E9")
    warm = hx("#CFDCDD")
    edge = hx("#8FA3A8")
    rim = hx("#AFC0C4")
    bean = hx("#A33A2E")
    bean_hi = hx("#C86A5A")
    x0, y0, x1, y1 = 0.7, 1.6, 15.3, 14.4
    r = 2.4
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            qx = min(max(xc, x0 + r), x1 - r)
            qy = min(max(yc, y0 + r), y1 - r)
            d = math.hypot(xc - qx, yc - qy) - r
            if d > 0.3:
                continue
            if d > -0.6:
                c = edge
            elif d > -1.4:
                c = rim
            elif xc + yc < 13:
                c = light
            elif xc + yc > 24:
                c = warm
            else:
                c = base
            put(im, x, y, c)
    # 斜向釉面高光带
    for x in range(3, 12):
        y = 4 + (x - 3) // 3
        if im.getpixel((x, y))[3]:
            put(im, x, y, hx("#EAF2F3"))
    # 盘面散落的红豆（单像素小点，避免变成大血滴）
    for _ in range(6):
        x, y = rng.randrange(10, 15), rng.randrange(5, 13)
        if im.getpixel((x, y))[3]:
            put(im, x, y, bean if rng.random() < 0.6 else bean_hi)
    save(im, "yokan_plate_top.png")


# ---------------------------------------------------------------- 长方盘(侧)
def plate_side():
    im = new()
    top = hx("#D2DEE0")
    bot = hx("#93A6AB")
    hi = hx("#E6EFF0")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.1
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.15)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.15)
        put(im, 1, y, lerp(c, hi, 0.4))
        put(im, 14, y, lerp(c, hi, 0.4))
    save(im, "yokan_plate_side.png")


# ---------------------------------------------------------------- 羊羹(顶)
def slice_top():
    """羊羹顶面：暗红棕半透明凝胶 + 釉面反光 + 红豆粒。"""
    im = new()
    rng = random.Random(1920)
    base = hx("#5A2418")
    light = hx("#7A3A28")
    gloss = hx("#9A563C")
    deep = hx("#3E150C")
    bean = hx("#C98B7A")
    bean_d = hx("#A33A2E")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 26.0))
            if x + y > 22:
                c = deep
            put(im, x, y, c)
    # 凝胶釉面：一道横向亮带
    for x in range(3, 13):
        put(im, x, 5, gloss)
        put(im, x, 6, lerp(gloss, light, 0.5))
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([bean, bean_d, gloss]))
    save(im, "yokan_slice_top.png")


# ---------------------------------------------------------------- 羊羹(侧)
def slice_side():
    im = new()
    rng = random.Random(1921)
    top = hx("#6A2C1E")
    bot = hx("#3A120A")
    hi = hx("#8E4A34")
    deep = hx("#2A0C06")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 0.95)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.5)
            elif y >= 14:
                cc = lerp(c, deep, 0.5)
            if 3 <= x <= 5:
                cc = lerp(cc, hi, 0.25)
            elif x >= 12:
                cc = lerp(cc, deep, 0.25)
            put(im, x, y, cc)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, deep]))
    save(im, "yokan_slice_side.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    slice_top()
    slice_side()
