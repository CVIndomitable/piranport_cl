#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""东坡肉方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_46.png（白椭圆盘上红亮酱汁里一大块
      划了十字花刀的东坡肉，四周点缀青菜叶）
输出到: src/main/resources/assets/piranport/textures/block/dongpo_*.png
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


def plate_top():
    im = new()
    white = hx("#FAF9F6")
    hi = hx("#FFFFFF")
    shade = hx("#EDEBE4")
    edge = hx("#D8D5CE")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.8:
                continue
            if r > 7.25:
                c = edge
            elif r > 6.6:
                c = white
            else:
                c = white
                if dx + dy > 3:
                    c = shade
            if dx < -1 and dy < -1:
                c = hi
            put(im, x, y, c)
    save(im, "dongpo_plate_top.png")


def plate_side():
    im = new()
    top = hx("#FCFBF8")
    bot = hx("#DAD6CE")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "dongpo_plate_side.png")


def sauce():
    im = new()
    rng = random.Random(4646)
    base = hx("#7A1E10")
    light = hx("#A33A24")
    hi = hx("#C45A38")
    dark = hx("#4E1008")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.7:
                continue
            c = base
            if dx + dy < -2:
                c = light
            elif dx + dy > 4:
                c = dark
            put(im, x, y, c)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark]))
    save(im, "dongpo_sauce.png")


def pork_top():
    """东坡肉顶面：红亮肉皮，划 3×3 十字花刀。"""
    im = new()
    rng = random.Random(4647)
    base = hx("#8A2A18")
    light = hx("#B84A2E")
    hi = hx("#D96A44")
    dark = hx("#5A160C")
    deep = hx("#3A0E06")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 26.0))
            # 十字花刀网格（3×3）
            if x in (4, 8, 12) or y in (4, 8, 12):
                c = deep
            elif x in (3, 5, 7, 9, 11, 13) or y in (3, 5, 7, 9, 11, 13):
                c = lerp(c, hi, 0.35)
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, light]))
    save(im, "dongpo_top.png")


def pork_side():
    """侧面：红亮肉皮 + 琥珀脂肪层 + 瘦肉的横向分层。"""
    im = new()
    layers = [(0, 2, "#5A160C"), (3, 5, "#B84A2E"), (6, 8, "#E0A860"),
              (9, 11, "#A33A24"), (12, 15, "#6E2412")]
    for y0, y1, col in layers:
        for y in range(y0, y1 + 1):
            c0 = hx(col)
            for x in range(16):
                c = c0
                if y == y0:
                    c = lerp(c0, hx("#FFFFFF"), 0.18)
                    c = c[:3] + (255,)
                if x <= 1 or x >= 14:
                    c = lerp(c, hx("#2A0A04"), 0.35)
                put(im, x, y, c)
    save(im, "dongpo_side.png")


def leaf():
    im = new()
    green = hx("#5E9A2E")
    hi = hx("#8ECB4E")
    dark = hx("#3E6A1E")
    pale = hx("#D9E8B0")
    cx, cy = 5.0, 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            # 叶形：长椭圆
            if (dx / 4.6) ** 2 + (dy / 2.6) ** 2 > 1.0:
                continue
            c = green
            if dx < -1:
                c = hi
            elif dx > 2:
                c = dark
            # 叶柄/浅色叶脉
            if abs(dy) < 0.7 and dx > -2:
                c = pale
            put(im, x, y, c)
    save(im, "dongpo_leaf.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    sauce()
    pork_top()
    pork_side()
    leaf()
