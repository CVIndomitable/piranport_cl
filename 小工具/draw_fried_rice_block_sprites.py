#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""什锦叉烧炒饭方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_12.png（白色方形浅盘，盘沿有蓝色花纹，
      中间一座炒饭：米粒混着叉烧丁、蛋碎、青豆和葱花）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 饭堆圆角靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/fried_rice_*.png
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


# ---------------------------------------------------------------- 方盘(顶)
def plate_top():
    """白色方形浅盘：盘沿高光 + 蓝白花纹，四角切一像素。"""
    im = new()
    white = hx("#F6F5F1")
    hi = hx("#FFFFFF")
    rim = hx("#EDEBE4")
    shade = hx("#E2DED4")
    edge = hx("#D3CFC6")
    blue = hx("#2B4EA0")
    blue_lt = hx("#4E72C4")
    for y in range(16):
        for x in range(16):
            if (x in (0, 15)) and (y in (0, 15)):
                continue
            c = white
            if x in (0, 15) or y in (0, 15):
                c = edge
            elif x in (1, 14) or y in (1, 14):
                c = hi
            elif x in (2, 13) or y in (2, 13):
                c = rim
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    # 蓝白花纹：左后沿 + 右前沿各一段波浪
    for x in range(3, 9):
        put(im, x, 2 + (1 if x % 2 else 0), blue)
        put(im, x, 1, blue_lt if x % 2 else hx("#FFFFFF"))
    for x in range(8, 14):
        put(im, x, 13 - (1 if x % 2 else 0), blue)
    save(im, "fried_rice_plate_top.png")


# ---------------------------------------------------------------- 方盘(侧)
def plate_side():
    im = new()
    top = hx("#FAF9F5")
    bot = hx("#D8D4CA")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "fried_rice_plate_side.png")


# ---------------------------------------------------------------- 炒饭(顶)
def rice_top():
    """什锦炒饭顶面：米粒底 + 叉烧丁/蛋碎/青豆/葱花/深色料。"""
    im = new()
    rng = random.Random(1212)
    base = hx("#F3EEE0")
    gan = hx("#FFF9EC")
    shade = hx("#E3DCC8")
    char = hx("#B54A32")
    char_d = hx("#8E2E1E")
    egg = hx("#E8C24E")
    egg_hi = hx("#F5DE8A")
    pea = hx("#5E8C2A")
    pea_hi = hx("#7AAE3E")
    scall = hx("#4E7A22")
    dark = hx("#4A3A2A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.6:
                continue  # 圆形饭堆
            c = base
            if dx + dy < -2:
                c = gan
            elif dx + dy > 4:
                c = shade
            if r > 6.7:
                c = lerp(c, shade, 0.4)
            put(im, x, y, c)
    # 米粒颗粒
    for _ in range(30):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([gan, shade, gan, base]))
    # 叉烧丁
    for bx, by in ((4, 4), (9, 3), (7, 8), (11, 9), (3, 9), (10, 11)):
        for dy in (0, 1):
            for dx in (0, 1):
                x, y = bx + dx, by + dy
                if 0 <= x < 16 and 0 <= y < 16 and im.getpixel((x, y))[3]:
                    put(im, x, y, char if (dx + dy) == 0 else char_d)
    # 蛋碎
    for bx, by in ((6, 4), (10, 6), (5, 7), (8, 12), (12, 6)):
        if im.getpixel((bx, by))[3]:
            put(im, bx, by, egg)
            put(im, bx + 1, by, egg_hi)
    # 青豆与葱花
    for bx, by in ((5, 5), (9, 6), (6, 10), (11, 8), (4, 11), (8, 5)):
        if im.getpixel((bx, by))[3]:
            put(im, bx, by, pea if rng.random() < 0.6 else pea_hi)
    for bx, by in ((7, 6), (10, 9), (5, 9), (9, 12)):
        if im.getpixel((bx, by))[3]:
            put(im, bx, by, scall)
    # 深色料
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, dark)
    save(im, "fried_rice_rice_top.png")


# ---------------------------------------------------------------- 炒饭(侧)
def rice_side():
    im = new()
    rng = random.Random(1213)
    top = hx("#EDE5D2")
    bot = hx("#CFC4AC")
    specks = [hx("#B54A32"), hx("#E8C24E"), hx("#5E8C2A"), hx("#8E2E1E")]
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            put(im, x, y, c)
        # 左右各两列透明，让方盒侧壁读作圆饭堆
        put(im, 1, y, lerp(c, hx("#D8CEB6"), 0.5))
        put(im, 14, y, lerp(c, hx("#D8CEB6"), 0.5))
    for _ in range(22):
        x, y = rng.randrange(2, 14), rng.randrange(16)
        put(im, x, y, rng.choice(specks))
    save(im, "fried_rice_rice_side.png")


# ---------------------------------------------------------------- 配料小块
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
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hi, shade]))
    save(im, name)


if __name__ == "__main__":
    plate_top()
    plate_side()
    rice_top()
    rice_side()
    chunk_texture("fried_rice_char_siu.png", hx("#B54A32"), hx("#D06A4A"), hx("#8E2E1E"), 1214)
    chunk_texture("fried_rice_egg.png", hx("#E8C24E"), hx("#F5DE8A"), hx("#C99A2E"), 1215)
    chunk_texture("fried_rice_pea.png", hx("#5E8C2A"), hx("#7AAE3E"), hx("#3E6A1E"), 1216)
    chunk_texture("fried_rice_scallion.png", hx("#4E7A22"), hx("#6D9A34"), hx("#35571A"), 1217)
    chunk_texture("fried_rice_dark.png", hx("#4A3A2A"), hx("#6B5640"), hx("#2E2318"), 1218)
