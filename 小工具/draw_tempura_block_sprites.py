#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""炸虾天妇罗方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_20.png（黑色方盘垫白纸，中央立着一簇
      金黄炸虾天妇罗（红色虾尾朝上），旁边有深褐蔬菜天妇罗、
      白色萝卜泥和绿色点缀）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 黑盘圆角/白纸边缘靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/tempura_*.png
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


# ---------------------------------------------------------------- 黑盘(顶)
def plate_top():
    """黑色釉面方盘：圆角矩形 + 斜向的柔和高光。"""
    im = new()
    base = hx("#1C1C20")
    sheen = hx("#33333A")
    deep = hx("#0E0E10")
    edge = hx("#08080A")
    x0, y0, x1, y1 = 0.6, 0.6, 15.4, 15.4
    r = 2.8
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
            else:
                c = base
                # 左上斜向釉面反光
                if (xc + yc) < 12:
                    c = lerp(base, sheen, 0.45)
                elif (xc + yc) > 24:
                    c = deep
            put(im, x, y, c)
    # 盘面亮带
    for i in range(5):
        put(im, 3 + i, 4, hx("#44444C"))
        put(im, 8 + i, 3, hx("#3C3C44"))
    save(im, "tempura_plate_top.png")


# ---------------------------------------------------------------- 黑盘(侧)
def plate_side():
    im = new()
    top = hx("#2A2A30")
    bot = hx("#0E0E10")
    hi = hx("#3E3E46")
    for x in range(2, 14):
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        put(im, 1, y, lerp(c, hi, 0.4))
        put(im, 14, y, lerp(c, hi, 0.25))
    save(im, "tempura_plate_side.png")


# ---------------------------------------------------------------- 垫纸(顶)
def paper_top():
    """白纸垫：米白纸面 + 两道折痕，圆角透明。"""
    im = new()
    base = hx("#F7F5EF")
    hi = hx("#FFFFFF")
    shade = hx("#E4E0D6")
    crease = hx("#D8D4CA")
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                continue
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 23:
                c = shade
            put(im, x, y, c)
    for t in range(2, 14):
        put(im, t, 5 + t // 4, crease)
        put(im, 10 - t // 5, t, crease)
    save(im, "tempura_paper_top.png")


# ---------------------------------------------------------------- 垫纸(侧)
def paper_side():
    im = new()
    top = hx("#FAF8F3")
    bot = hx("#DDD9CE")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tempura_paper_side.png")


# ---------------------------------------------------------------- 炸虾(侧)
def shrimp_side():
    """天妇罗面衣：金黄酥脆外壳，凹凸颗粒与深色裂纹。"""
    im = new()
    rng = random.Random(2020)
    base = hx("#E0A64A")
    light = hx("#F2C46B")
    hi = hx("#FBE3A0")
    dark = hx("#B8762A")
    brown = hx("#8A4E14")
    for y in range(16):
        for x in range(16):
            t = (math.sin(x * 1.1 + y * 0.7) + 1.0) * 0.5
            c = lerp(base, light, t * 0.55)
            if x <= 1 or x >= 14:
                c = lerp(c, dark, 0.4)
            if y <= 1:
                c = lerp(c, hi, 0.4)
            put(im, x, y, c)
    # 面衣颗粒与裂纹
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, dark, brown]))
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        put(im, x, y, dark)
        put(im, x + 1, y, light)
    save(im, "tempura_shrimp_side.png")


def shrimp_top():
    im = new()
    c = hx("#E8B050")
    for y in range(16):
        for x in range(16):
            put(im, x, y, c)
    save(im, "tempura_shrimp_top.png")


# ---------------------------------------------------------------- 虾尾
def shrimp_tail():
    """炸虾尾：橙红虾壳 + 浅色分节。"""
    im = new()
    base = hx("#D4502A")
    hi = hx("#E87448")
    pale = hx("#F2C9A0")
    deep = hx("#A8331A")
    for y in range(16):
        c = lerp(base, hi, y / 15.0)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.5)
            elif x >= 13:
                cc = deep
            put(im, x, y, cc)
    # 分节纹
    for y in (4, 8, 12):
        for x in range(4, 13):
            put(im, x, y, pale)
    save(im, "tempura_shrimp_tail.png")


# ---------------------------------------------------------------- 蔬菜天妇罗
def veg_top():
    im = new()
    rng = random.Random(2021)
    base = hx("#A96A2E")
    light = hx("#C08440")
    dark = hx("#6E3E14")
    hi = hx("#D9A25C")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (12 - (x + y)) / 28.0))
            put(im, x, y, c)
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, dark]))
    save(im, "tempura_veg_top.png")


def veg_side():
    im = new()
    top = hx("#A96A2E")
    bot = hx("#5E3410")
    hi = hx("#C08440")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            put(im, x, y, cc)
    save(im, "tempura_veg_side.png")


# ---------------------------------------------------------------- 萝卜泥
def daikon_top():
    im = new()
    rng = random.Random(2022)
    base = hx("#F5F4F0")
    hi = hx("#FFFFFF")
    shade = hx("#E4E2DA")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 22:
                c = shade
            put(im, x, y, c)
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, shade, shade]))
    save(im, "tempura_daikon_top.png")


def daikon_side():
    im = new()
    top = hx("#FBF9F4")
    bot = hx("#DDDAD1")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tempura_daikon_side.png")


# ---------------------------------------------------------------- 绿叶点缀
def garnish():
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
    save(im, "tempura_garnish.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    paper_top()
    paper_side()
    shrimp_side()
    shrimp_top()
    shrimp_tail()
    veg_top()
    veg_side()
    daikon_top()
    daikon_side()
    garnish()
