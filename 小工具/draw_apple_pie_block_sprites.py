#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""苹果派方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_5.png（深灰金属派盘，金黄色苹果馅 +
      编织格纹酥皮，缝隙里露出苹果块）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆派盘/圆馅料靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/apple_pie_*.png
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
def tin_top():
    """深灰金属派盘顶面：外圈圆盘 + 浅灰金属高光，四角透明。"""
    im = new()
    edge = hx("#26262A")
    rim = hx("#43434A")
    rim_hi = hx("#5A5A62")
    inner = hx("#34343A")
    inner_hi = hx("#45454C")
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
                c = rim
            else:
                c = inner
            # 左上金属反光
            if dx < -1 and dy < -1:
                c = rim_hi if r > 6.6 else inner_hi
            put(im, x, y, c)
    # 盘沿一圈细高光（左上弧）
    for p in ((3, 4), (4, 3), (5, 3), (3, 5)):
        put(im, p[0], p[1], hx("#74747C"))
    save(im, "apple_pie_tin_top.png")


# ---------------------------------------------------------------- 派盘(侧)
def tin_side():
    im = new()
    top = hx("#4A4A51")
    bot = hx("#24242A")
    hi = hx("#60606A")
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
    # 上沿金属反光
    for x in range(3, 13):
        put(im, x, 0, hx("#6C6C76"))
    save(im, "apple_pie_tin_side.png")


# ---------------------------------------------------------------- 苹果馅
def filling():
    """金黄苹果馅：圆形馅面（四角透明），可见苹果块与糖浆光泽。"""
    im = new()
    rng = random.Random(505)
    base = hx("#E2A238")
    light = hx("#F5C75A")
    gold = hx("#FFE38A")
    syrup = hx("#C47A1E")
    deep = hx("#A85E12")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.9:
                continue
            c = lerp(base, light, max(0.0, (10 - (x + y)) / 28.0))
            if r > 7.1:
                c = lerp(c, deep, 0.5)  # 贴盘一圈略深
            put(im, x, y, c)
    # 苹果块：2×2 左右的方块，带亮面与暗边
    chunks = [(3, 4), (9, 3), (6, 7), (11, 8), (4, 10), (9, 11), (7, 3), (12, 4), (2, 7)]
    for bx, by in chunks:
        for dy in (0, 1):
            for dx in (0, 1):
                x, y = bx + dx, by + dy
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) > 7.7:
                    continue
                c = light if (dx + dy) == 0 else base
                if dx == 1 and dy == 1:
                    c = syrup
                put(im, x, y, c)
    # 糖浆高光
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([gold, syrup]))
    save(im, "apple_pie_filling.png")


# ---------------------------------------------------------------- 酥皮条(顶)
def crust_top():
    """格纹酥皮条顶面：烤至金黄的派皮，亮面 + 焦边。"""
    im = new()
    rng = random.Random(506)
    base = hx("#E0A552")
    light = hx("#F0C06B")
    hi = hx("#F8D68C")
    dark = hx("#B97A32")
    baked = hx("#C98B3E")
    for y in range(16):
        for x in range(16):
            # 条带中部亮、两侧略暗
            t = 1.0 - min(abs(y - 7.5) / 8.0, 1.0) * 0.35
            c = lerp(dark, base, t)
            if 2 <= y <= 13:
                c = lerp(c, light, max(0.0, (12 - (x + y)) / 26.0))
            put(im, x, y, c)
    # 烘焙斑与高光
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([baked, hi, dark]))
    # 两端收口压暗
    for y in range(16):
        put(im, 0, y, lerp(im.getpixel((0, y)), dark, 0.55))
        put(im, 15, y, lerp(im.getpixel((15, y)), dark, 0.55))
    save(im, "apple_pie_crust_top.png")


# ---------------------------------------------------------------- 酥皮条(侧)
def crust_side():
    im = new()
    rng = random.Random(507)
    top = hx("#DFA24F")
    bot = hx("#A9692A")
    hi = hx("#EDBA63")
    dark = hx("#8A4F1D")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.45)
            put(im, x, y, cc)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(3, 13)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "apple_pie_crust_side.png")


if __name__ == "__main__":
    tin_top()
    tin_side()
    filling()
    crust_top()
    crust_side()
