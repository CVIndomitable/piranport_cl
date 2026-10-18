#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""烧烤方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_45.png（黑色圆形烤炉上架着烤网，
      上面有大块烤肉、两根香肠和三串彩椒肉串）
输出到: src/main/resources/assets/piranport/textures/block/bbq_*.png
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


def grate():
    im = new()
    dark = hx("#24242A")
    bar = hx("#4A4A54")
    hi = hx("#66666E")
    for y in range(16):
        for x in range(16):
            c = dark
            if y % 3 == 0:
                c = bar
                if y % 6 == 0:
                    c = hi
            put(im, x, y, c)
    save(im, "bbq_grate.png")


def grill_side():
    im = new()
    top = hx("#3E3E4A")
    bot = hx("#18181E")
    hi = hx("#5A5A66")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.15
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.05)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            if y <= 2:
                c = lerp(c, hi, 0.4)
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        put(im, 1, y, lerp(c, hi, 0.35))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "bbq_grill_side.png")


def meat_top():
    im = new()
    rng = random.Random(4545)
    base = hx("#B8732E")
    light = hx("#D99A48")
    hi = hx("#EFC070")
    dark = hx("#7A4214")
    char = hx("#4A2208")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 28.0))
            if y % 4 == 0:
                c = dark
            elif y % 4 == 1:
                c = hi
            put(im, x, y, c)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([char, hi, light]))
    save(im, "bbq_meat_top.png")


def meat_side():
    im = new()
    top = hx("#B8732E")
    bot = hx("#6E3A10")
    hi = hx("#D99A48")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.35) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.35))
        put(im, 14, y, lerp(c, bot, 0.35))
    save(im, "bbq_meat_side.png")


def sausage_top():
    im = new()
    base = hx("#A84A2A")
    light = hx("#C96A42")
    hi = hx("#E08A5A")
    dark = hx("#6E2A14")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 28.0))
            if y in (4, 11):
                c = dark
            elif y in (3, 10):
                c = hi
            if x <= 1 or x >= 14:
                c = dark
            put(im, x, y, c)
    save(im, "bbq_sausage_top.png")


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
    save(im, "bbq_sausage_side.png")


def chunk(name, base, hi, shade, seed):
    im = new()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, shade]))
    save(im, name)


def stick():
    im = new()
    base = hx("#D9B87A")
    hi = hx("#F0D8A0")
    dark = hx("#A8864A")
    for y in range(16):
        for x in range(16):
            c = base
            if y <= 3:
                c = hi
            elif y >= 12:
                c = dark
            put(im, x, y, c)
    save(im, "bbq_stick.png")


if __name__ == "__main__":
    grate()
    grill_side()
    meat_top()
    meat_side()
    sausage_top()
    sausage_side()
    chunk("bbq_pepper.png", hx("#C0301E"), hx("#E05A3A"), hx("#8A1A0E"), 4546)
    chunk("bbq_dark_meat.png", hx("#6B3A22"), hx("#8A5232"), hx("#3E1E0C"), 4547)
    chunk("bbq_green.png", hx("#4E7A22"), hx("#6D9A34"), hx("#35571A"), 4548)
    chunk("bbq_onion.png", hx("#E8D8B0"), hx("#F5E8C8"), hx("#C9B888"), 4549)
    stick()
