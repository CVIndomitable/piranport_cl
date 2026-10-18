#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""威尼斯墨鱼面方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_50.png（白椭圆盘上墨黑的墨鱼汁意面，
      点缀几圈橙色墨鱼圈和绿色香芹）
输出到: src/main/resources/assets/piranport/textures/block/venice_*.png
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
    save(im, "venice_plate_top.png")


def plate_side():
    im = new()
    top = hx("#FCFBF8")
    bot = hx("#DAD6CE")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "venice_plate_side.png")


def noodle_top():
    im = new()
    rng = random.Random(5050)
    base = hx("#1A1A22")
    light = hx("#2E2E3A")
    hi = hx("#4A4A5A")
    blue = hx("#242A44")
    green = hx("#5E9A2E")
    green_hi = hx("#8ECB4E")
    for y in range(16):
        for x in range(16):
            c = base
            # 面条：斜向细纹
            if (x * 2 + y) % 5 == 0:
                c = light
            elif (x + y * 2) % 7 == 0:
                c = blue
            elif (x * 3 + y) % 11 == 0:
                c = hi
            put(im, x, y, c)
    # 绿色香芹碎
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, green if rng.random() < 0.6 else green_hi)
    save(im, "venice_noodle_top.png")


def noodle_side():
    im = new()
    top = hx("#24242E")
    bot = hx("#0E0E14")
    light = hx("#363644")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if (x + y) % 4 == 0:
                cc = light
            put(im, x, y, cc)
    save(im, "venice_noodle_side.png")


def ring_top():
    im = new()
    orange = hx("#E08A2A")
    hi = hx("#F5B45A")
    dark = hx("#B5601A")
    hole = hx("#1A1A22")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.3:
                continue
            if r < 3.4:
                c = hole
            elif r < 4.4:
                c = dark
            elif r < 6.4:
                c = orange
                if dx < -1 and dy < -1:
                    c = hi
            else:
                c = dark
            put(im, x, y, c)
    save(im, "venice_ring_top.png")


def ring_side():
    im = new()
    top = hx("#E08A2A")
    bot = hx("#A85414")
    hi = hx("#F5B45A")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            put(im, x, y, lerp(c, hi, 0.25) if y <= 3 else c)
    save(im, "venice_ring_side.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    noodle_top()
    noodle_side()
    ring_top()
    ring_side()
