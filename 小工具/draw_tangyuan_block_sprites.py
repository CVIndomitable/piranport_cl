#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""汤圆方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_31.png（白瓷底盘上放一只青花瓷碗，
      金边碗口，清汤里浮着八九颗白白胖胖的汤圆）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆盘/碗口/汤圆圆顶靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/tangyuan_*.png
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


# ---------------------------------------------------------------- 底盘(顶)
def plate_top():
    im = new()
    white = hx("#FAF9F6")
    hi = hx("#FFFFFF")
    shade = hx("#E9E6DF")
    edge = hx("#D6D2C9")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.8:
                continue
            if r > 7.2:
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
    save(im, "tangyuan_plate_top.png")


def plate_side():
    im = new()
    top = hx("#FCFBF8")
    bot = hx("#DAD6CE")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tangyuan_plate_side.png")


# ---------------------------------------------------------------- 青花碗(侧)
def bowl_pattern():
    """青花瓷碗外壁：深蓝底 + 白色菱格花纹 + 金边。"""
    im = new()
    navy = hx("#2A3560")
    navy_d = hx("#1E2748")
    blue = hx("#4A5A8A")
    pale = hx("#D8DCE8")
    gold = hx("#C9A544")
    for y in range(16):
        for x in range(16):
            c = navy
            if y >= 13:
                c = navy_d
            # 菱格 + 小花：斜线网格 + 中心点
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                c = blue
            if (x * 3 + y * 2) % 7 == 0:
                c = pale
            if y in (0, 1):
                c = gold
            put(im, x, y, c)
    save(im, "tangyuan_bowl_pattern.png")


# ---------------------------------------------------------------- 碗口(顶)
def bowl_top():
    """碗口俯视：白瓷沿 + 细金线，中央镂空露汤面。"""
    im = new()
    white = hx("#F7F5F0")
    hi = hx("#FFFFFF")
    edge = hx("#D8D3C9")
    gold = hx("#C9A544")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < 6.0:
                continue
            if r > 7.8:
                continue
            if r > 7.4:
                c = edge
            elif r > 7.0:
                c = white
            elif r > 6.5:
                c = gold
            else:
                c = white
            if x + y < 11 and r > 6.5:
                c = hi
            put(im, x, y, c)
    save(im, "tangyuan_bowl_top.png")


# ---------------------------------------------------------------- 汤面
def broth():
    im = new()
    base = hx("#DAD5C8")
    light = hx("#EDE9DE")
    shade = hx("#C9C3B4")
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
                c = shade
            put(im, x, y, c)
    save(im, "tangyuan_broth.png")


# ---------------------------------------------------------------- 汤圆
def ball_top():
    im = new()
    base = hx("#F8F5EE")
    hi = hx("#FFFFFF")
    shade = hx("#E4DED2")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            c = base
            if dx < -1 and dy < -1:
                c = hi
            elif r > 6.2:
                c = shade
            put(im, x, y, c)
    save(im, "tangyuan_top.png")


def ball_side():
    im = new()
    top = hx("#F7F4ED")
    bot = hx("#E0DAD0")
    hi = hx("#FFFFFF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(3, 13):
            cc = c
            if x <= 5:
                cc = lerp(c, hi, 0.35)
            put(im, x, y, cc)
        put(im, 2, y, lerp(c, bot, 0.3))
        put(im, 13, y, lerp(c, bot, 0.3))
    save(im, "tangyuan_side.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    bowl_pattern()
    bowl_top()
    broth()
    ball_top()
    ball_side()
