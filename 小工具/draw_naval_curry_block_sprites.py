#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""海军咖喱方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_9.png（白色椭圆盘：左前方深棕咖喱酱
      带肉/土豆/胡萝卜块，右后方一座白米饭）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 椭圆盘/酱汁/饭堆的圆形轮廓靠贴图透明角 + render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/naval_curry_*.png
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


def rounded_mask(radius=3.0):
    m = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            qx = min(max(xc, radius), 16 - radius)
            qy = min(max(yc, radius), 16 - radius)
            m[y][x] = math.hypot(xc - qx, yc - qy) <= radius
    return m


# ---------------------------------------------------------------- 椭圆盘(顶)
def plate_top():
    """白色椭圆餐盘：盘沿浅灰，右下柔和阴影。"""
    im = new()
    white = hx("#FDFDFE")
    warm = hx("#F6F5F2")
    rim = hx("#EDECE8")
    edge = hx("#D9D7D2")
    shadow = hx("#F1F0ED")
    cx, cy = 7.5, 7.5
    rx, ry = 7.85, 7.05
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            e = (dx / rx) ** 2 + (dy / ry) ** 2
            if e > 1.0:
                continue
            if e > 0.90:
                c = edge
            elif e > 0.78:
                c = rim
            elif e > 0.66:
                c = warm
            else:
                c = white
            if e <= 0.78 and dx + dy > 3.0:
                c = shadow if c == white else c
            put(im, x, y, c)
    for p in ((4, 3), (5, 3), (4, 4)):
        put(im, p[0], p[1], hx("#FFFFFF"))
    save(im, "naval_curry_plate_top.png")


# ---------------------------------------------------------------- 椭圆盘(侧)
def plate_side():
    im = new()
    top = hx("#FCFBF8")
    bot = hx("#DAD7D2")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.15
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.15)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.15)
        put(im, 2, y, lerp(c, hx("#E7E4DF"), 0.5))
        put(im, 13, y, lerp(c, hx("#E7E4DF"), 0.5))
    save(im, "naval_curry_plate_side.png")


# ---------------------------------------------------------------- 咖喱酱
def curry_sauce():
    """深棕咖喱酱：不规则酱汁边缘 + 油亮旋纹 + 隐约的块状料。"""
    im = new()
    rng = random.Random(909)
    base = hx("#6B3A18")
    light = hx("#8A5226")
    gloss = hx("#A96A36")
    dark = hx("#4A240C")
    deep = hx("#361806")
    speck_p = hx("#D8C08A")
    speck_o = hx("#C9772E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            edge = 7.45 + 0.55 * math.sin(ang * 3.0 + 0.7) + 0.30 * math.sin(ang * 5.0)
            if r > edge:
                continue  # 不规则酱汁边缘
            c = base
            if dx + dy < -2:
                c = light
            elif dx + dy > 4:
                c = dark
            if r > edge - 0.9:
                c = deep  # 边缘一圈更深的酱色
            put(im, x, y, c)
    # 油亮旋纹
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([gloss, gloss, light]))
    # 隐约的土豆/胡萝卜碎料
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([speck_p, speck_o, speck_p]))
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, deep)
    save(im, "naval_curry_sauce.png")


def curry_sauce_side():
    im = new()
    top = hx("#7A4520")
    bot = hx("#3E1E09")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "naval_curry_sauce_side.png")


# ---------------------------------------------------------------- 米饭(顶)
def rice_top():
    """白米饭顶面：米粒颗粒与柔和起伏，四角透明收圆。"""
    im = new()
    mask = rounded_mask(3.2)
    rng = random.Random(910)
    base = hx("#F7F4EB")
    hi = hx("#FFFDF7")
    shade = hx("#E5DFD0")
    beige = hx("#EFE9DA")
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 22:
                c = beige
            put(im, x, y, c)
    # 米粒：成对的亮面/阴影短颗粒，密度高一些才读得出米饭
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        if mask[y][x]:
            put(im, x, y, rng.choice([hi, shade, beige, shade]))
    for _ in range(14):
        x, y = rng.randrange(15), rng.randrange(15)
        if mask[y][x]:
            put(im, x, y, hi)
            put(im, x + 1, y, shade)
    # 几处略深的饭堆凹陷
    for _ in range(6):
        x, y = rng.randrange(15), rng.randrange(15)
        if mask[y][x] and mask[y][x + 1]:
            put(im, x, y, shade)
            put(im, x + 1, y, beige)
    save(im, "naval_curry_rice_top.png")


def rice_side():
    im = new()
    top = hx("#FBF8F1")
    bot = hx("#DDD6C6")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hx("#FFFFFF"), 0.4)
            elif y >= 13:
                cc = lerp(c, hx("#CFC7B6"), 0.4)
            put(im, x, y, cc)
    save(im, "naval_curry_rice_side.png")


# ---------------------------------------------------------------- 咖喱块料
def chunk_texture(name, base, hi, shade, seed):
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
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, shade, base]))
    save(im, name)


if __name__ == "__main__":
    plate_top()
    plate_side()
    curry_sauce()
    curry_sauce_side()
    rice_top()
    rice_side()
    chunk_texture("naval_curry_potato.png", hx("#E8DDB8"), hx("#F5EDD2"), hx("#C9B98D"), 911)
    chunk_texture("naval_curry_carrot.png", hx("#D9822B"), hx("#EBA04A"), hx("#B5601A"), 912)
    chunk_texture("naval_curry_meat.png", hx("#6B3A22"), hx("#8A5232"), hx("#4A2414"), 913)
