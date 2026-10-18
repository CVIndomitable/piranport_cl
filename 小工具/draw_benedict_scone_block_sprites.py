#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""班尼迪克蛋 / 红茶司康饼方块模型 — 贴图绘制
原画: zjsnrwiki 文件:Cookbook_34.png（淡蓝白方盘：英式松饼叠荷包蛋，
      淋黄色荷兰酱，配欧芹）
      文件:Cookbook_38.png（淡蓝白椭圆盘：四只带葡萄干的司康，
      左后方一套红茶杯碟）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/
  benedict_*.png、scone_*.png
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


# ---------------------------------------------------------------- 淡蓝白盘(顶)
def plate_top(name, square=True):
    """淡蓝白瓷盘：方盘或椭圆盘，圆角透明。"""
    im = new()
    white = hx("#E8EEF0")
    hi = hx("#F7FAFB")
    blue = hx("#D2DDE1")
    edge = hx("#AEBEC4")
    if square:
        x0, y0, x1, y1, r = 0.8, 0.8, 15.2, 15.2, 2.2
    else:
        x0, y0, x1, y1, r = 0.8, 1.0, 15.2, 15.0, 7.0
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
            elif xc + yc < 12:
                c = hi
            elif xc + yc > 22:
                c = blue
            else:
                c = white
            put(im, x, y, c)
    save(im, name)


def plate_side(name):
    im = new()
    top = hx("#F2F6F7")
    bot = hx("#C6D2D6")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, name)


# ---------------------------------------------------------------- 英式松饼
def muffin_top():
    im = new()
    rng = random.Random(3434)
    base = hx("#C98A44")
    light = hx("#E0A860")
    hi = hx("#F0C078")
    dark = hx("#8A541E")
    pale = hx("#E8D2A0")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
            if x + y > 22:
                c = dark
            put(im, x, y, c)
    # 烤面包屑与浅色瓤点
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, pale]))
    save(im, "benedict_muffin_top.png")


def muffin_side():
    im = new()
    top = hx("#B87A3A")
    mid = hx("#E8CFA0")
    bot = hx("#9A5E24")
    for y in range(16):
        if y < 5:
            c = lerp(top, mid, y / 5.0)
        else:
            c = lerp(mid, bot, (y - 5) / 10.0)
        for x in range(16):
            put(im, x, y, c)
    save(im, "benedict_muffin_side.png")


# ---------------------------------------------------------------- 荷包蛋
def egg_top():
    im = new()
    base = hx("#F8F5EC")
    hi = hx("#FFFFFF")
    shade = hx("#E2DCCC")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            c = base
            if dx < -1 and dy < -1:
                c = hi
            elif r > 6.3:
                c = shade
            put(im, x, y, c)
    save(im, "benedict_egg_top.png")


def egg_side():
    im = new()
    top = hx("#FAF7EE")
    bot = hx("#DED8C8")
    hi = hx("#FFFFFF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(3, 13):
            put(im, x, y, lerp(c, hi, 0.25) if x <= 5 else c)
        put(im, 2, y, lerp(c, bot, 0.3))
        put(im, 13, y, lerp(c, bot, 0.3))
    save(im, "benedict_egg_side.png")


# ---------------------------------------------------------------- 荷兰酱
def sauce():
    im = new()
    rng = random.Random(3435)
    base = hx("#E8B23E")
    light = hx("#F5D06A")
    hi = hx("#FBE7A0")
    dark = hx("#C98A2A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            edge = 7.5 + 0.6 * math.sin(ang * 5.0 + 0.6) + 0.35 * math.sin(ang * 3.0)
            if r > edge:
                continue
            c = base
            if dx + dy < -3:
                c = light
            elif dx + dy > 4:
                c = dark
            put(im, x, y, c)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark]))
    save(im, "benedict_sauce.png")


# ---------------------------------------------------------------- 欧芹
def parsley():
    im = new()
    leaf = hx("#4E7A22")
    leaf_hi = hx("#6D9A34")
    leaf_dk = hx("#35571A")
    vein = hx("#8FB553")

    def draw_leaf(cx, cy, length, width, angle_deg):
        ang = math.radians(angle_deg)
        ca, sa = math.cos(ang), math.sin(ang)
        for y in range(16):
            for x in range(16):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                u = dx * ca + dy * sa
                v = -dx * sa + dy * ca
                t = (u + length / 2) / length
                if not (0.05 < t < 0.95):
                    continue
                env = math.sin(math.pi * t) ** 0.7
                if abs(v) > width * env:
                    continue
                if abs(v) > width * env * 0.72:
                    c = leaf_dk
                elif abs(v) < width * env * 0.22 and t > 0.12:
                    c = vein
                elif u < 0:
                    c = leaf_hi
                else:
                    c = leaf
                put(im, x, y, c)

    draw_leaf(7.0, 8.2, 11.0, 3.4, -35.0)
    draw_leaf(9.6, 6.4, 6.6, 2.6, 30.0)
    draw_leaf(9.8, 10.6, 5.2, 2.2, 120.0)
    save(im, "benedict_parsley.png")


# ---------------------------------------------------------------- 司康
def scone_top():
    im = new()
    rng = random.Random(3838)
    base = hx("#D89A4A")
    light = hx("#F0BC66")
    hi = hx("#FBE0A0")
    dark = hx("#A9641E")
    raisin = hx("#3A1E12")
    raisin_l = hx("#5E3420")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
            if r > 6.2:
                c = dark
            put(im, x, y, c)
    # 葡萄干
    for _ in range(9):
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        if im.getpixel((x, y))[3]:
            put(im, x, y, raisin if rng.random() < 0.75 else raisin_l)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark, light]))
    save(im, "scone_top.png")


def scone_side():
    im = new()
    rng = random.Random(3839)
    top = hx("#D89A4A")
    bot = hx("#9A5E24")
    hi = hx("#F0BC66")
    dark = hx("#6E3810")
    raisin = hx("#3A1E12")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.4)
            put(im, x, y, cc)
        put(im, 1, y, lerp(c, dark, 0.35))
        put(im, 14, y, lerp(c, dark, 0.35))
    for _ in range(8):
        x, y = rng.randrange(2, 14), rng.randrange(16)
        put(im, x, y, rng.choice([raisin, hi, dark]))
    save(im, "scone_side.png")


if __name__ == "__main__":
    plate_top("benedict_plate_top.png", square=True)
    plate_side("benedict_plate_side.png")
    muffin_top()
    muffin_side()
    egg_top()
    egg_side()
    sauce()
    parsley()
    plate_top("scone_plate_top.png", square=False)
    plate_side("scone_plate_side.png")
    scone_top()
    scone_side()
