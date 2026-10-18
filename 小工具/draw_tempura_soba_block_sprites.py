#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""天妇罗荞麦面方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_42.png（竹编荞麦面托盘上堆着棕色荞麦面，
      右侧深棕碗里放着两只炸虾天妇罗和一片海苔）
输出到: src/main/resources/assets/piranport/textures/block/tempura_soba_*.png
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


def bamboo_top():
    im = new()
    rng = random.Random(4242)
    base = hx("#D9B66A")
    hi = hx("#EACB87")
    dark = hx("#A8833E")
    deep = hx("#8A6A2E")
    for y in range(16):
        for x in range(16):
            c = base
            # 竹条：竖条 + 横向绑线
            if x % 4 == 0:
                c = hi
            elif x % 4 == 3:
                c = dark
            if y % 6 == 0:
                c = deep
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "tempura_soba_bamboo_top.png")


def bamboo_side():
    im = new()
    top = hx("#E0BF76")
    bot = hx("#A8833E")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tempura_soba_bamboo_side.png")


def soba_top():
    im = new()
    rng = random.Random(4243)
    base = hx("#8A5A2E")
    light = hx("#A8753E")
    dark = hx("#5E3A16")
    hi = hx("#C08A4A")
    for y in range(16):
        for x in range(16):
            c = base
            # 荞麦面条：细横纹
            if (x + y * 2) % 4 == 0:
                c = light
            elif (x + y) % 7 == 0:
                c = dark
            put(im, x, y, c)
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, light]))
    save(im, "tempura_soba_noodle_top.png")


def soba_side():
    im = new()
    top = hx("#8A5A2E")
    bot = hx("#4E2E10")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tempura_soba_noodle_side.png")


def bowl_side():
    im = new()
    top = hx("#5E2E14")
    bot = hx("#2A1208")
    hi = hx("#8A4A24")
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
    save(im, "tempura_soba_bowl_side.png")


def bowl_top():
    im = new()
    dark = hx("#3E1E0C")
    rim = hx("#5E2E14")
    hi = hx("#8A4A24")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < 6.0:
                continue
            if r > 7.7:
                continue
            c = rim if r < 7.1 else dark
            if x + y < 11:
                c = hi
            put(im, x, y, c)
    save(im, "tempura_soba_bowl_top.png")


def sauce():
    im = new()
    base = hx("#3A1A0C")
    light = hx("#5E2E14")
    hi = hx("#7A4526")
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
                c = hi
            put(im, x, y, c)
    save(im, "tempura_soba_sauce.png")


def nori():
    im = new()
    base = hx("#1E3A1E")
    light = hx("#2E5228")
    dark = hx("#122612")
    for y in range(16):
        for x in range(16):
            c = base
            if (x + y) % 5 == 0:
                c = light
            elif (x * 2 + y) % 9 == 0:
                c = dark
            put(im, x, y, c)
    save(im, "tempura_soba_nori.png")


if __name__ == "__main__":
    bamboo_top()
    bamboo_side()
    soba_top()
    soba_side()
    bowl_side()
    bowl_top()
    sauce()
    nori()
