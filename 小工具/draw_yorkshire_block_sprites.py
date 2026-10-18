#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""约克郡布丁方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_44.png（白盘黑边，四只金黄膨起的约克郡布丁，
      中间是浅色凹陷）
输出到: src/main/resources/assets/piranport/textures/block/yorkshire_*.png
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
    dark = hx("#3A3A3E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.8:
                continue
            if r > 7.3:
                c = edge
            elif r > 7.0:
                c = dark
            elif r > 6.6:
                c = white
            else:
                c = white
                if dx + dy > 3:
                    c = shade
            if dx < -1 and dy < -1:
                c = hi
            put(im, x, y, c)
    save(im, "yorkshire_plate_top.png")


def plate_side():
    im = new()
    top = hx("#FCFBF8")
    bot = hx("#DAD6CE")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "yorkshire_plate_side.png")


def pudding_top():
    """布丁顶：金黄膨起的环，中间浅色凹陷。"""
    im = new()
    rng = random.Random(4444)
    base = hx("#B8732E")
    light = hx("#D99A48")
    hi = hx("#EFC070")
    well = hx("#E8D08A")
    well_d = hx("#D9B860")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            if r < 2.6:
                c = well if dx + dy < 0 else well_d
            elif r < 5.2:
                c = lerp(base, light, max(0.0, (8 - (x + y)) / 20.0))
            else:
                c = light
                if r < 6.3:
                    c = hi
            put(im, x, y, c)
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, base, well_d]))
    save(im, "yorkshire_top.png")


def pudding_side():
    im = new()
    top = hx("#C9883A")
    bot = hx("#8A5220")
    hi = hx("#E8B060")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.35) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.35))
        put(im, 14, y, lerp(c, bot, 0.35))
    save(im, "yorkshire_side.png")


def pudding_center():
    im = new()
    base = hx("#E8D08A")
    hi = hx("#F5E4A8")
    shade = hx("#D9B860")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    save(im, "yorkshire_center.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    pudding_top()
    pudding_side()
    pudding_center()
