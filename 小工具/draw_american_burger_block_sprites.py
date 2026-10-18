#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""美式汉堡方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_27.png（芝麻面包顶 + 融化芝士片 +
      焦褐牛肉饼 + 番茄片 + 生菜 + 底面包，层层叠起的经典美式汉堡）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 面包/番茄圆边与生菜/芝士不规则边靠贴图透明角 + cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/burger_*.png
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


# ---------------------------------------------------------------- 面包(顶)
def bun_top():
    """芝麻面包顶面：金黄烤面包 + 白芝麻，圆角透明。"""
    im = new()
    rng = random.Random(2727)
    base = hx("#E8B84E")
    light = hx("#F4D078")
    hi = hx("#FBE5A8")
    shade = hx("#C98E2E")
    edge = hx("#A96F22")
    sesame = hx("#F7EED6")
    x0, y0, x1, y1 = 0.8, 0.8, 15.2, 15.2
    r = 3.4
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
                c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
                if x + y > 23:
                    c = shade
            put(im, x, y, c)
    # 芝麻
    for _ in range(22):
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        if im.getpixel((x, y))[3]:
            put(im, x, y, sesame if rng.random() < 0.8 else hi)
    save(im, "burger_bun_top.png")


# ---------------------------------------------------------------- 面包(侧)
def bun_side():
    im = new()
    top = hx("#E3AE45")
    bot = hx("#B97A28")
    hi = hx("#F2C867")
    shade = hx("#9A5E18")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.1
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.05)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            if y <= 2:
                c = lerp(c, hi, 0.45)
            elif y >= 14:
                c = lerp(c, shade, 0.4)
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        put(im, 1, y, lerp(c, hi, 0.35))
        put(im, 14, y, lerp(c, shade, 0.35))
    save(im, "burger_bun_side.png")


# ---------------------------------------------------------------- 生菜
def lettuce():
    im = new()
    rng = random.Random(2728)
    dark = hx("#3E6A1E")
    base = hx("#5E9A2E")
    mid = hx("#7AB53E")
    hi = hx("#9ACB5A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            # 波浪边：生菜边沿的褶
            edge = 7.6 + 0.7 * math.sin(ang * 9.0) + 0.3 * math.sin(ang * 4.0)
            if r > edge:
                continue
            n = math.sin(x * 1.7 + y * 0.9) + math.cos(y * 1.3 - x * 0.4)
            c = mid if n > 0.4 else (base if n > -0.6 else dark)
            if dx + dy < -3:
                c = hi
            put(im, x, y, c)
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark, mid]))
    save(im, "burger_lettuce.png")


def lettuce_side():
    """生菜侧壁：波浪状的绿色菜叶带，上下缘透明成褶。"""
    im = new()
    rng = random.Random(2732)
    dark = hx("#3E6A1E")
    base = hx("#5E9A2E")
    mid = hx("#7AB53E")
    hi = hx("#9ACB5A")
    for x in range(16):
        top = 3 + int(1.4 * math.sin(x * 1.3))
        bot = 12 + int(1.1 * math.sin(x * 0.9 + 1.0))
        for y in range(top, bot + 1):
            c = base if (x + y) % 3 else mid
            if y <= top + 1 or y >= bot - 1:
                c = dark
            if x < 3:
                c = hi
            put(im, x, y, c)
    for _ in range(10):
        x = rng.randrange(16)
        y = rng.randrange(4, 12)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, dark]))
    save(im, "burger_lettuce_side.png")


def tomato_side():
    """番茄侧壁：红色果肉 + 上下深色果皮。"""
    im = new()
    rind = hx("#9E2418")
    base = hx("#C93A2E")
    flesh = hx("#E06A50")
    for y in range(16):
        for x in range(16):
            c = base
            if y <= 3 or y >= 12:
                c = rind
            elif 6 <= y <= 9:
                c = flesh
            put(im, x, y, c)
    save(im, "burger_tomato_side.png")


# ---------------------------------------------------------------- 番茄片
def tomato():
    im = new()
    rng = random.Random(2729)
    rind = hx("#B52A1E")
    base = hx("#C93A2E")
    flesh = hx("#E06A50")
    hi = hx("#F09A80")
    seed = hx("#F2C9A0")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            if r > 6.9:
                c = rind
            elif r > 5.6:
                c = base
            else:
                c = flesh
                # 番茄籽
                ang = math.atan2(dy, dx)
                frac = ((math.degrees(ang) + 360) % 60) / 60.0
                if min(frac, 1 - frac) < 0.12 and 2.0 < r < 5.4:
                    c = seed
            if dx < -2 and dy < -1:
                c = hi
            put(im, x, y, c)
    save(im, "burger_tomato.png")


# ---------------------------------------------------------------- 牛肉饼
def patty():
    im = new()
    rng = random.Random(2730)
    base = hx("#6B3A22")
    light = hx("#8A5232")
    dark = hx("#3E1E0C")
    char = hx("#2A1206")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 28.0))
            if x + y > 22:
                c = dark
            put(im, x, y, c)
    # 煎烤纹与焦斑
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([dark, char, light]))
    for x in range(3, 13, 4):
        for y in range(2, 14):
            if (x + y) % 3:
                put(im, x, y, char)
    save(im, "burger_patty_top.png")


def patty_side():
    im = new()
    top = hx("#5E3018")
    bot = hx("#2E1408")
    hi = hx("#7A4426")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.25) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.4))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "burger_patty_side.png")


# ---------------------------------------------------------------- 芝士片
def cheese():
    im = new()
    rng = random.Random(2731)
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
            # 融化芝士的不规则边
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
    save(im, "burger_cheese_top.png")


def cheese_side():
    im = new()
    top = hx("#EDBC4E")
    bot = hx("#C98A2A")
    hi = hx("#F8DE8A")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, lerp(c, hi, 0.3) if y <= 2 else c)
        put(im, 1, y, lerp(c, bot, 0.35))
        put(im, 14, y, lerp(c, bot, 0.35))
    save(im, "burger_cheese_side.png")


if __name__ == "__main__":
    bun_top()
    bun_side()
    lettuce()
    lettuce_side()
    tomato()
    tomato_side()
    patty()
    patty_side()
    cheese()
    cheese_side()
