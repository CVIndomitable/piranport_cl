#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""塔坦苹果挞方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_41.png（淡蓝白蓝边椭圆盘上两只苹果挞，
      一只切开露出深色焦糖馅，前侧两颗红樱桃）
输出到: src/main/resources/assets/piranport/textures/block/tarte_tatin_*.png
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
    white = hx("#E8EEF0")
    hi = hx("#F7FAFB")
    blue = hx("#C9D8DE")
    blue_line = hx("#7FA8BC")
    edge = hx("#AEBEC4")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.8:
                continue
            if r > 7.2:
                c = edge
            elif r > 6.9:
                c = blue_line
            elif r > 6.2:
                c = blue
            else:
                c = white
                if dx + dy < -2:
                    c = hi
            put(im, x, y, c)
    save(im, "tarte_tatin_plate_top.png")


def plate_side():
    im = new()
    top = hx("#F2F6F7")
    bot = hx("#C6D2D6")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tarte_tatin_plate_side.png")


def tart_top():
    """苹果挞顶面：焦糖底 + 一圈圈苹果片 + 釉面高光。"""
    im = new()
    rng = random.Random(4141)
    caramel = hx("#8A4A1E")
    apple = hx("#E0A850")
    apple_l = hx("#F2C46A")
    apple_h = hx("#FBDE9A")
    deep = hx("#5E2E10")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            # 同心苹果片纹理
            band = int(r * 2.2 + math.atan2(dy, dx) * 0.5) % 3
            c = apple if band == 0 else (apple_l if band == 1 else apple_h)
            if r > 6.3:
                c = caramel
            if r < 1.2:
                c = apple_h
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([deep, caramel, apple_h]))
    save(im, "tarte_tatin_top.png")


def tart_side():
    im = new()
    top = hx("#D89A48")
    bot = hx("#8A521E")
    hi = hx("#F0BC66")
    dark = hx("#6E3E10")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            # 波浪花边
            if (x // 2) % 2 == 0:
                cc = lerp(c, hi, 0.25)
            if y >= 13:
                cc = lerp(cc, dark, 0.4)
            put(im, x, y, cc)
    save(im, "tarte_tatin_side.png")


def caramel():
    im = new()
    base = hx("#7A3A14")
    light = hx("#A05A24")
    dark = hx("#4A1E08")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 28.0))
            if x + y > 22:
                c = dark
            put(im, x, y, c)
    save(im, "tarte_tatin_caramel.png")


def cherry():
    im = new()
    base = hx("#B30318")
    hi = hx("#E4566A")
    dark = hx("#87020F")
    spec = hx("#FFE9E6")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.3:
                continue
            if r > 6.2:
                c = dark
            elif dx < -1.5 and dy < -1.5:
                c = hi
            else:
                c = base
            put(im, x, y, c)
    for p in ((4, 4), (5, 4), (4, 5)):
        put(im, p[0], p[1], spec)
    save(im, "tarte_tatin_cherry.png")


def stem():
    im = new()
    dark = hx("#35571A")
    base = hx("#4E7A22")
    hi = hx("#6D9A34")
    for y in range(16):
        for x in range(16):
            c = base
            if x <= 3:
                c = hi
            elif x >= 12:
                c = dark
            put(im, x, y, c)
    save(im, "tarte_tatin_stem.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    tart_top()
    tart_side()
    caramel()
    cherry()
    stem()
