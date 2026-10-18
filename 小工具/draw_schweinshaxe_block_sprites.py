#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""烤猪肘方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_29.png（红亮脆皮烤猪肘，骨头朝上，
      底下垫着浅黄酸菜/土豆泥，右侧一小碟深色肉汁）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/schweinshaxe_*.png
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


# ---------------------------------------------------------------- 脆皮(顶)
def meat_top():
    """烤猪肘顶面：红亮脆皮 + 油光 + 焦斑。"""
    im = new()
    rng = random.Random(2929)
    base = hx("#8A3A1E")
    light = hx("#B85A2E")
    gloss = hx("#D9783E")
    hi = hx("#F0A060")
    deep = hx("#5A2410")
    for y in range(16):
        for x in range(16):
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
            if x + y > 23:
                c = deep
            put(im, x, y, c)
    # 油亮反光与脆皮颗粒
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([gloss, hi, deep]))
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        put(im, x, y, deep)
        put(im, x + 1, y, gloss)
    save(im, "schweinshaxe_meat_top.png")


# ---------------------------------------------------------------- 脆皮(侧)
def meat_side():
    im = new()
    rng = random.Random(2930)
    top = hx("#9A4424")
    bot = hx("#5A2410")
    hi = hx("#C96A3A")
    gloss = hx("#E08A50")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.12
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.0)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            if y <= 2:
                c = lerp(c, hi, 0.5)
            put(im, x, y, c)
    for _ in range(14):
        x, y = rng.randrange(2, 14), rng.randrange(16)
        put(im, x, y, rng.choice([hi, gloss, bot]))
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.0)
        put(im, 1, y, lerp(c, hi, 0.35))
        put(im, 14, y, lerp(c, bot, 0.4))
    save(im, "schweinshaxe_meat_side.png")


# ---------------------------------------------------------------- 骨头
def bone():
    im = new()
    top = hx("#F0E4C8")
    bot = hx("#C9BB98")
    hi = hx("#FBF3E0")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.5)
            elif x >= 12:
                cc = lerp(c, bot, 0.4)
            put(im, x, y, cc)
    save(im, "schweinshaxe_bone.png")


# ---------------------------------------------------------------- 酸菜/土豆泥
def kraut_top():
    """浅黄酸菜丝/土豆泥：不规则边缘 + 细丝纹理。"""
    im = new()
    rng = random.Random(2931)
    base = hx("#E4D49E")
    light = hx("#F2E7BE")
    shade = hx("#C9B87A")
    deep = hx("#A89660")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            edge = 7.4 + 0.7 * math.sin(ang * 4.0 + 0.8) + 0.4 * math.sin(ang * 7.0)
            if r > edge:
                continue
            c = base
            if dx + dy < -2:
                c = light
            elif dx + dy > 4:
                c = shade
            put(im, x, y, c)
    # 酸菜细丝
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([light, shade, deep]))
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        if im.getpixel((x, y))[3]:
            put(im, x, y, light)
            put(im, x + 1, y, shade)
    save(im, "schweinshaxe_kraut_top.png")


# ---------------------------------------------------------------- 肉汁碟
def gravy_top():
    """小碟俯视：白瓷沿 + 深色肉汁。"""
    im = new()
    white = hx("#FAF8F3")
    edge = hx("#D8D4CA")
    gravy = hx("#4A2412")
    gravy_l = hx("#7A4526")
    gravy_hi = hx("#9A5A32")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.6:
                continue
            if r > 6.5:
                c = white if r < 7.1 else edge
            else:
                c = gravy
                if dx + dy < -2:
                    c = gravy_l
                elif dx + dy > 3:
                    c = gravy_hi
            put(im, x, y, c)
    save(im, "schweinshaxe_gravy_top.png")


def dish_side():
    im = new()
    top = hx("#FBF9F4")
    bot = hx("#DCD7CC")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "schweinshaxe_dish_side.png")


if __name__ == "__main__":
    meat_top()
    meat_side()
    bone()
    kraut_top()
    gravy_top()
    dish_side()
