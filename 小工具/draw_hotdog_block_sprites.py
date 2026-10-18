#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""热狗方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_40.png（棕色油纸垫着两只热狗：
      金黄面包夹烤肠，配生菜、番茄酱与芥末）
输出到: src/main/resources/assets/piranport/textures/block/hotdog_*.png
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


def paper_top():
    im = new()
    rng = random.Random(4040)
    base = hx("#A8794A")
    light = hx("#C99A6A")
    dark = hx("#8A5A2E")
    deep = hx("#6E4220")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            # 八角形油纸边
            edge = 7.6 + 0.5 * math.sin(ang * 4.0)
            if r > edge:
                continue
            c = base
            if dx + dy < -2:
                c = light
            elif dx + dy > 4:
                c = dark
            put(im, x, y, c)
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([light, dark, deep]))
    save(im, "hotdog_paper_top.png")


def paper_side():
    im = new()
    top = hx("#B8895A")
    bot = hx("#79502A")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "hotdog_paper_side.png")


def bun_top():
    im = new()
    base = hx("#D89A48")
    light = hx("#F0BC66")
    hi = hx("#FBE0A0")
    inner = hx("#F5E6C0")
    dark = hx("#A9641E")
    for y in range(16):
        for x in range(16):
            c = base
            # 中间夹缝：浅色面包瓤
            if 5 <= y <= 10:
                c = inner if 7 <= y <= 8 else lerp(base, inner, 0.5)
            elif y <= 2 or y >= 13:
                c = dark
            if x <= 1 or x >= 14:
                c = dark
            if x in (7, 8) and 5 <= y <= 10:
                c = lerp(inner, hi, 0.3)
            put(im, x, y, c)
    save(im, "hotdog_bun_top.png")


def bun_side():
    im = new()
    top = hx("#D89A48")
    bot = hx("#9A5E24")
    hi = hx("#F0BC66")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.35) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.35))
        put(im, 14, y, lerp(c, bot, 0.35))
    save(im, "hotdog_bun_side.png")


def sausage_top():
    im = new()
    rng = random.Random(4041)
    base = hx("#A84A2A")
    light = hx("#C96A42")
    hi = hx("#E08A5A")
    dark = hx("#6E2A14")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 28.0))
            if y in (3, 8, 13):
                c = dark
            elif y in (2, 7, 12):
                c = hi
            if x <= 1 or x >= 14:
                c = dark
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(2, 14), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "hotdog_sausage_top.png")


def sausage_side():
    im = new()
    top = hx("#A84A2A")
    bot = hx("#5E2410")
    hi = hx("#C96A42")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.35) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.4))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "hotdog_sausage_side.png")


def lettuce():
    im = new()
    rng = random.Random(4042)
    dark = hx("#3E6A1E")
    base = hx("#5E9A2E")
    mid = hx("#7AB53E")
    hi = hx("#9ACB5A")
    for y in range(16):
        for x in range(16):
            top = 3 + int(1.2 * math.sin(x * 1.4))
            bot = 12 + int(1.0 * math.sin(x * 0.9 + 1.0))
            if not (top <= y <= bot):
                continue
            c = base if (x + y) % 3 else mid
            if y <= top + 1 or y >= bot - 1:
                c = dark
            if x < 3:
                c = hi
            put(im, x, y, c)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(4, 12)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark]))
    save(im, "hotdog_lettuce.png")


def ketchup():
    im = new()
    red = hx("#C0301E")
    hi = hx("#E05A3A")
    dark = hx("#8A1A0E")
    for y in range(16):
        for x in range(16):
            c = red
            if y in (6, 7, 8):
                c = hi
            elif y <= 2 or y >= 13:
                c = dark
            put(im, x, y, c)
    for x in range(3, 13, 3):
        put(im, x, 7, hx("#FF8A6A"))
    save(im, "hotdog_ketchup.png")


if __name__ == "__main__":
    paper_top()
    paper_side()
    bun_top()
    bun_side()
    sausage_top()
    sausage_side()
    lettuce()
    ketchup()
