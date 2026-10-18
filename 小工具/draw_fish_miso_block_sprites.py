#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""烤鱼味增汤方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_39.png（左侧深红漆碗味增汤，里面有豆腐和葱花；
      右侧淡蓝白长盘，摆着深色烤鱼和金黄烤鱼柳，配紫苏叶与萝卜泥）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
输出到: src/main/resources/assets/piranport/textures/block/fish_miso_*.png
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


def bowl_side():
    im = new()
    top = hx("#7A2A1A")
    bot = hx("#3E120A")
    hi = hx("#A8502E")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.15
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.05)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            if y <= 2:
                c = lerp(c, hi, 0.5)
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        put(im, 1, y, lerp(c, hi, 0.35))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "fish_miso_bowl_side.png")


def bowl_top():
    im = new()
    dark = hx("#5A1E12")
    rim = hx("#7A2A1A")
    hi = hx("#A8502E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < 6.4:
                continue
            if r > 7.8:
                continue
            c = rim if r < 7.2 else dark
            if x + y < 11:
                c = hi
            put(im, x, y, c)
    save(im, "fish_miso_bowl_top.png")


def soup():
    im = new()
    rng = random.Random(3939)
    base = hx("#6B4A2A")
    light = hx("#8A6238")
    dark = hx("#4E3018")
    tofu = hx("#F0E8D4")
    tofu_d = hx("#DCD2B8")
    scall = hx("#5E8C2A")
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
    for bx, by in ((3, 5), (7, 4), (5, 8), (9, 7), (4, 10), (8, 10)):
        put(im, bx, by, tofu)
        put(im, bx + 1, by, tofu_d)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, scall)
    save(im, "fish_miso_soup.png")


def plate_top():
    im = new()
    white = hx("#E8EEF0")
    hi = hx("#F7FAFB")
    blue = hx("#D2DDE1")
    edge = hx("#AEBEC4")
    x0, y0, x1, y1, r = 0.8, 0.8, 15.2, 15.2, 1.6
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            qx = min(max(xc, x0 + r), x1 - r)
            qy = min(max(yc, y0 + r), y1 - r)
            d = math.hypot(xc - qx, yc - qy) - r
            if d > 0.3:
                continue
            if d > -0.5:
                c = edge
            elif xc + yc < 12:
                c = hi
            elif xc + yc > 22:
                c = blue
            else:
                c = white
            put(im, x, y, c)
    save(im, "fish_miso_plate_top.png")


def plate_side():
    im = new()
    top = hx("#F2F6F7")
    bot = hx("#C6D2D6")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "fish_miso_plate_side.png")


def dark_fish():
    im = new()
    rng = random.Random(3940)
    base = hx("#4A2A1A")
    light = hx("#6E4426")
    char = hx("#241008")
    edge = hx("#8A5A34")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 30.0))
            if x in (2, 6, 10, 14):
                c = char
            if x <= 1 or x >= 14:
                c = edge
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([char, light]))
    save(im, "fish_miso_dark_top.png")


def dark_fish_side():
    im = new()
    top = hx("#4A2A1A")
    bot = hx("#241008")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, c)
        put(im, 1, y, lerp(c, bot, 0.4))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "fish_miso_dark_side.png")


def fillet():
    im = new()
    base = hx("#C98A4A")
    light = hx("#E8B06A")
    hi = hx("#F5D08A")
    dark = hx("#9A5E24")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
            # 烤纹
            if y in (4, 8, 12) and 2 <= x <= 13:
                c = dark
            elif y in (3, 7, 11) and 2 <= x <= 13:
                c = hi
            if x <= 1 or x >= 14:
                c = lerp(c, dark, 0.4)
            put(im, x, y, c)
    save(im, "fish_miso_fillet_top.png")


def fillet_side():
    im = new()
    top = hx("#C98A4A")
    bot = hx("#7A4214")
    hi = hx("#E8B06A")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.35) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.4))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "fish_miso_fillet_side.png")


def shiso():
    im = new()
    dark = hx("#2E5A16")
    base = hx("#4E8A24")
    mid = hx("#6AA832")
    hi = hx("#8ECB4E")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 7.5, y + 0.5 - 7.5
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            edge = 7.3 + 0.8 * math.sin(ang * 6.0)
            if r > edge:
                continue
            c = mid if (x + y) % 3 else base
            if dx + dy < -2:
                c = hi
            elif dx + dy > 4:
                c = dark
            put(im, x, y, c)
    save(im, "fish_miso_shiso.png")


def daikon_top():
    im = new()
    base = hx("#F2F0E8")
    hi = hx("#FFFFFF")
    shade = hx("#DDD9CC")
    yellow = hx("#D9C24A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.2:
                continue
            c = base
            if x + y < 11:
                c = hi
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    for p in ((7, 6), (8, 6), (7, 7), (8, 7)):
        put(im, p[0], p[1], yellow)
    save(im, "fish_miso_daikon_top.png")


def daikon_side():
    im = new()
    top = hx("#F8F6EF")
    bot = hx("#DAD5C8")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "fish_miso_daikon_side.png")


if __name__ == "__main__":
    bowl_side()
    bowl_top()
    soup()
    plate_top()
    plate_side()
    dark_fish()
    dark_fish_side()
    fillet()
    fillet_side()
    shiso()
    daikon_top()
    daikon_side()
