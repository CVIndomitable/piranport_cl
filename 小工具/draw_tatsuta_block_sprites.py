#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""龙田烧方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_30.png（几块金黄酥脆的龙田烧炸鸡，
      上面压两条青椒，前面摆两片柠檬）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/tatsuta_*.png
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


# ---------------------------------------------------------------- 炸鸡(顶)
def chicken_top():
    """龙田烧顶面：金黄酥脆面衣 + 深橙焦斑 + 凹凸颗粒。"""
    im = new()
    rng = random.Random(3030)
    base = hx("#D89A44")
    light = hx("#F0BC66")
    hi = hx("#FBE0A0")
    dark = hx("#A9641E")
    brown = hx("#7A3E10")
    for y in range(16):
        for x in range(16):
            t = (math.sin(x * 1.1 + y * 0.6) + 1.0) * 0.5
            c = lerp(base, light, t * 0.6)
            if x + y > 23:
                c = dark
            put(im, x, y, c)
    # 面衣颗粒/焦斑
    for _ in range(24):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark, brown, light]))
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        put(im, x, y, dark)
        put(im, x + 1, y, light)
    save(im, "tatsuta_chicken_top.png")


# ---------------------------------------------------------------- 炸鸡(侧)
def chicken_side():
    im = new()
    rng = random.Random(3031)
    top = hx("#DBA050")
    bot = hx("#8A4E16")
    hi = hx("#F2C26C")
    dark = hx("#6E3810")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.1
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.0)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            if y <= 2:
                c = lerp(c, hi, 0.45)
            put(im, x, y, c)
    for _ in range(14):
        x, y = rng.randrange(2, 14), rng.randrange(16)
        put(im, x, y, rng.choice([hi, dark]))
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.0)
        put(im, 1, y, lerp(c, hi, 0.3))
        put(im, 14, y, lerp(c, dark, 0.35))
    save(im, "tatsuta_chicken_side.png")


# ---------------------------------------------------------------- 青椒条
def pepper():
    im = new()
    dark = hx("#2E5A16")
    base = hx("#4E8A24")
    mid = hx("#6AA832")
    hi = hx("#8ECB4E")
    for y in range(16):
        for x in range(16):
            c = base
            if y <= 3:
                c = hi
            elif y >= 12:
                c = dark
            elif (x + y) % 5 == 0:
                c = mid
            put(im, x, y, c)
    save(im, "tatsuta_pepper.png")


# ---------------------------------------------------------------- 柠檬片
def lemon_top():
    im = new()
    rind = hx("#E8C23A")
    rind_d = hx("#C99A1E")
    pith = hx("#F8F2CC")
    flesh = hx("#F6E27A")
    flesh_d = hx("#EBD05C")
    seg_line = hx("#FFFBDD")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.8:
                c = rind_d
            elif r > 5.9:
                c = rind
            elif r > 5.1:
                c = pith
            else:
                c = flesh
                frac = (math.degrees(math.atan2(dy, dx)) % 360) / 45.0
                frac -= int(frac)
                if min(frac, 1 - frac) < 0.14:
                    c = seg_line
                elif frac < 0.26 or frac > 0.74:
                    c = flesh_d
            put(im, x, y, c)
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, p[0], p[1], hx("#FDF8D2"))
    save(im, "tatsuta_lemon_top.png")


def lemon_side():
    im = new()
    rind = hx("#E8C23A")
    rind_d = hx("#C99A1E")
    pith = hx("#F8F2CC")
    for y in range(16):
        c = rind if 3 <= y <= 12 else rind_d
        for x in range(16):
            cc = c
            if x <= 3:
                cc = pith if 4 <= y <= 11 else rind_d
            put(im, x, y, cc)
    save(im, "tatsuta_lemon_side.png")


if __name__ == "__main__":
    chicken_top()
    chicken_side()
    pepper()
    lemon_top()
    lemon_side()
