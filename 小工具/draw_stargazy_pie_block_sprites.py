#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""仰望星空派方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_13.png（深灰波浪边派盘，金黄酥皮上
      压着几颗深橙五角星，几条沙丁鱼的头/尾从馅里竖出来）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆派盘/酥皮圆边靠贴图透明角 + render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/stargazy_*.png
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


# ---------------------------------------------------------------- 派盘(顶)
def dish_top():
    """深灰金属派盘俯视：一圈波浪压边，中央镂空。"""
    im = new()
    edge = hx("#1E1E22")
    dark = hx("#2E2E34")
    mid = hx("#3E3E45")
    hi = hx("#55555D")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r < 7.0:
                continue  # 中央镂空：露出上方酥皮边缘
            if r > 7.85:
                continue
            ang = math.atan2(dy, dx)
            # 波浪压边：按角度交替明暗
            flute = int(((ang + math.pi) / (2 * math.pi)) * 24)
            c = mid if flute % 2 == 0 else dark
            if r > 7.55:
                c = edge
            elif r < 7.15:
                c = hi
            if dx < -1 and dy < -1 and r > 7.2:
                c = hx("#6A6A73")
            put(im, x, y, c)
    save(im, "stargazy_dish_top.png")


# ---------------------------------------------------------------- 派盘(侧)
def dish_side():
    im = new()
    top = hx("#4C4C54")
    bot = hx("#202026")
    hi = hx("#64646E")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.2
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        put(im, 2, y, lerp(c, hi, 0.35))
        put(im, 13, y, lerp(c, bot, 0.45))
    for x in range(3, 13):
        put(im, x, 0, hx("#70707A"))
    save(im, "stargazy_dish_side.png")


STAR = [
    "..#..",
    "#####",
    ".###.",
    ".#.#.",
    "#...#",
]


def stamp_star(im, ox, oy, fill, hi):
    for j, row in enumerate(STAR):
        for i, ch in enumerate(row):
            if ch == "#":
                x, y = ox + i, oy + j
                if 0 <= x < 16 and 0 <= y < 16 and im.getpixel((x, y))[3]:
                    put(im, x, y, hi if (i == 2 and j == 1) else fill)


# ---------------------------------------------------------------- 酥皮(顶)
def crust_top():
    """金黄酥皮顶面：波浪圆边 + 六颗深橙五角星，星间有烘焙纹理。"""
    im = new()
    rng = random.Random(1313)
    base = hx("#E8B44F")
    light = hx("#F4CE79")
    hi = hx("#FBE2A0")
    dark = hx("#C08A30")
    deep = hx("#9A671E")
    star = hx("#C0561D")
    star_hi = hx("#D97A2E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            # 波浪形酥皮边缘
            edge = 7.85 + 0.35 * math.sin(ang * 8.0)
            if r > edge:
                continue
            c = lerp(base, light, max(0.0, (11 - (x + y)) / 26.0))
            if r > edge - 0.9:
                c = lerp(c, deep, 0.45)  # 酥皮卷边压深
            put(im, x, y, c)
    # 四颗五角星错开摆放，避免 16px 下糊成一团
    for sx, sy in ((2, 3), (9, 2), (11, 9), (4, 10)):
        stamp_star(im, sx, sy, star, star_hi)
    # 烘焙斑点
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([dark, hi, light]))
    save(im, "stargazy_crust_top.png")


# ---------------------------------------------------------------- 酥皮(侧)
def crust_side():
    im = new()
    top = hx("#E3AD4A")
    bot = hx("#A86A22")
    hi = hx("#F2C56C")
    dark = hx("#8A5218")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(3, 13):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.5)
            put(im, x, y, cc)
        put(im, 2, y, lerp(c, dark, 0.45))
        put(im, 13, y, lerp(c, dark, 0.45))
    save(im, "stargazy_crust_side.png")


# ---------------------------------------------------------------- 沙丁鱼
def fish_side():
    """银色鱼身：背部深蓝灰、腹部银白，带鳞片与侧线。"""
    im = new()
    rng = random.Random(1314)
    back = hx("#6E7A80")
    mid = hx("#AEB7BA")
    belly = hx("#DDE3E4")
    line = hx("#5A666C")
    scale = hx("#C6CFD2")
    for y in range(16):
        for x in range(16):
            if x <= 2:
                c = back
            elif x <= 5:
                c = mid
            elif x <= 12:
                c = lerp(mid, belly, (x - 5) / 7.0 * 0.7)
            else:
                c = belly
            if x >= 13:
                c = back
            # 侧线
            if x == 6:
                c = line
            put(im, x, y, c)
    for _ in range(18):
        x, y = rng.randrange(4, 13), rng.randrange(16)
        put(im, x, y, scale)
    save(im, "stargazy_fish_side.png")


def fish_head():
    """鱼头端面：银色圆头 + 黑眼。"""
    im = new()
    mid = hx("#AEB7BA")
    hi = hx("#DDE3E4")
    dark = hx("#6E7A80")
    eye = hx("#1A1E20")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 7.5, y + 0.5 - 7.5
            if math.hypot(dx, dy) > 7.3:
                continue
            c = mid if x < 9 else hi
            if y <= 3:
                c = hi
            elif y >= 12:
                c = dark
            put(im, x, y, c)
    for p in ((5, 5), (6, 5), (5, 6), (6, 6)):
        put(im, p[0], p[1], eye)
    for x in (7, 8, 9):
        put(im, x, 10, hx("#7A868C"))  # 嘴线
    save(im, "stargazy_fish_head.png")


def fish_tail():
    """鱼尾端面：银色 + 深色分叉。"""
    im = new()
    mid = hx("#AEB7BA")
    hi = hx("#DDE3E4")
    dark = hx("#5A666C")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 7.5, y + 0.5 - 7.5
            if math.hypot(dx, dy) > 7.3:
                continue
            c = mid if y < 9 else hi
            if x <= 3 or x >= 12:
                c = dark
            put(im, x, y, c)
    # 分叉尾
    for i in range(6):
        put(im, 7 - i, 4 + i, dark)
        put(im, 8 + i, 4 + i, dark)
    save(im, "stargazy_fish_tail.png")


if __name__ == "__main__":
    dish_top()
    dish_side()
    crust_top()
    crust_side()
    fish_side()
    fish_head()
    fish_tail()
