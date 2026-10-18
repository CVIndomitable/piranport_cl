#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""甜豆花 / 咸豆花方块模型 — 共用一个白瓷碗的贴图绘制
原画: zjsnrwiki 文件:Cookbook_25.png（甜豆花：琥珀糖浆 + 红豆 + 黄绿蜜豆）
      文件:Cookbook_26.png（咸豆花：酱色汤汁 + 深色紫菜/榨菜 + 葱花）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 碗口圆沿/豆花圆面靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/douhua_*.png
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


# ---------------------------------------------------------------- 碗口(顶)
def bowl_top():
    """白瓷碗口：一圈白沿，中央镂空露豆花。"""
    im = new()
    white = hx("#F7F5F1")
    hi = hx("#FFFFFF")
    rim = hx("#EDEAE3")
    lip = hx("#E2DED5")
    edge = hx("#D5D1C8")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r < 6.2:
                continue
            if r > 7.85:
                continue
            if r > 7.4:
                c = edge
            elif r > 6.9:
                c = white
            elif r > 6.5:
                c = rim
            else:
                c = lip
            if dx < -1 and dy < -1 and r > 6.8:
                c = hi
            put(im, x, y, c)
    save(im, "douhua_bowl_top.png")


# ---------------------------------------------------------------- 碗壁(侧)
def bowl_side():
    im = new()
    top = hx("#FBFAF6")
    bot = hx("#DDD8CE")
    hi = hx("#FFFFFF")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.12
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        put(im, 2, y, lerp(c, hi, 0.4))
        put(im, 13, y, lerp(c, hi, 0.2))
    save(im, "douhua_bowl_side.png")


# ---------------------------------------------------------------- 豆花侧面
def curd_side():
    im = new()
    top = hx("#F4EFE3")
    bot = hx("#E0D8C6")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(2, 14):
            put(im, x, y, c)
        put(im, 1, y, lerp(c, top, 0.4))
        put(im, 14, y, lerp(c, bot, 0.3))
    save(im, "douhua_curd_side.png")


# ---------------------------------------------------------------- 甜豆花面
def sweet_top():
    """甜豆花：嫩白豆花 + 琥珀糖浆 + 红豆 + 黄绿蜜豆。"""
    im = new()
    rng = random.Random(2525)
    base = hx("#F2EDE0")
    hi = hx("#FFFDF6")
    shade = hx("#E2DBC8")
    syrup = hx("#D9A64A")
    syrup_hi = hx("#E8C27A")
    bean = hx("#7A2A22")
    bean_hi = hx("#9A3A2E")
    melon = hx("#C9C25A")
    melon_d = hx("#A8A83E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.7:
                continue
            c = base
            if dx + dy < -2:
                c = hi
            elif dx + dy > 4:
                c = shade
            # 糖浆纹：斜向的琥珀色带
            if (x + y * 2) % 7 in (0, 1) and r < 7.2:
                c = syrup if (x + y) % 2 else syrup_hi
            put(im, x, y, c)
    # 红豆堆（右半）
    for bx, by in ((9, 6), (10, 7), (9, 8), (11, 8), (10, 9), (9, 10), (11, 10), (10, 11)):
        put(im, bx, by, bean if (bx + by) % 2 else bean_hi)
    # 黄绿蜜豆堆（右下）
    for bx, by in ((9, 11), (10, 12), (11, 11), (10, 13)):
        put(im, bx, by, melon if (bx + by) % 2 else melon_d)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, shade, syrup_hi]))
    save(im, "douhua_sweet_top.png")


# ---------------------------------------------------------------- 咸豆花面
def savory_top():
    """咸豆花：嫩白豆花 + 酱色汤汁 + 深色紫菜/榨菜 + 葱花。"""
    im = new()
    rng = random.Random(2626)
    base = hx("#F2EDE0")
    hi = hx("#FFFDF6")
    shade = hx("#E2DBC8")
    sauce = hx("#A8662E")
    sauce_hi = hx("#C98A4A")
    dark = hx("#3A3A2A")
    dark_l = hx("#2A2A1E")
    scall = hx("#5E8C2A")
    scall_hi = hx("#7AAE3E")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.7:
                continue
            c = base
            if dx + dy < -2:
                c = hi
            elif dx + dy > 4:
                c = shade
            # 酱色汤汁纹
            if (x * 2 + y) % 8 in (0, 1, 2) and r < 7.2:
                c = sauce if (x + y) % 2 else sauce_hi
            put(im, x, y, c)
    # 深色紫菜/榨菜（中偏左）
    for bx, by in ((6, 7), (7, 8), (6, 9), (8, 8), (7, 10), (5, 8)):
        put(im, bx, by, dark if (bx + by) % 2 else dark_l)
    # 葱花
    for bx, by in ((9, 5), (10, 7), (9, 9), (11, 6), (10, 11), (8, 12)):
        put(im, bx, by, scall if (bx + by) % 2 else scall_hi)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, shade, sauce_hi]))
    save(im, "douhua_savory_top.png")


# ---------------------------------------------------------------- 配料小块
def chunk(name, base, hi, shade, seed):
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
    bowl_top()
    bowl_side()
    curd_side()
    sweet_top()
    savory_top()
    chunk("douhua_red_bean.png", hx("#7A2A22"), hx("#9A3A2E"), hx("#561A14"), 2526)
    chunk("douhua_melon.png", hx("#C9C25A"), hx("#DCDA7A"), hx("#A8A83E"), 2527)
    chunk("douhua_dark.png", hx("#3A3A2A"), hx("#4E4E38"), hx("#2A2A1E"), 2627)
    chunk("douhua_scallion.png", hx("#5E8C2A"), hx("#7AAE3E"), hx("#3E6A1E"), 2628)
    chunk("douhua_brown.png", hx("#A8662E"), hx("#C98A4A"), hx("#7A4418"), 2629)
