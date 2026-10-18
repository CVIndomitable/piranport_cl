#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""皇家海军咸牛肉方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_14.png（棕木桶，两道深灰铁箍，
      桶口露出带脂肪纹理的暗红咸牛肉块）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/salted_beef_*.png
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


# ---------------------------------------------------------------- 桶身(侧)
def barrel_side():
    """橡木桶板：竖向木板条纹 + 木纹 + 上下略暗。"""
    im = new()
    rng = random.Random(1414)
    planks = ["#7A4A2A", "#8A5632", "#74451F", "#81502C"]
    seam = hx("#4A2A14")
    hi = hx("#9A6238")
    dark = hx("#5C3418")
    for y in range(16):
        for x in range(16):
            p = x // 4
            c = hx(planks[p % len(planks)])
            # 木板接缝
            if x % 4 == 3:
                c = seam
            elif x % 4 == 0:
                c = hi
            # 桶身上下收暗
            if y <= 1:
                c = lerp(c, hi, 0.35)
            elif y >= 14:
                c = lerp(c, dark, 0.45)
            put(im, x, y, c)
    # 木纹短线
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([dark, hi]))
    save(im, "salted_beef_barrel_side.png")


def barrel_side_with_hoop(name, rows):
    """带铁箍的木桶侧贴图：在指定行画一道深灰金属箍。"""
    barrel_side()
    src = Image.open(os.path.join(OUT_DIR, "salted_beef_barrel_side.png")).convert("RGBA")
    im = src.copy()
    top = hx("#5A5A64")
    mid = hx("#44444C")
    bot = hx("#2C2C33")
    hi = hx("#72727E")
    for i, y in enumerate(rows):
        t = i / max(1, len(rows) - 1)
        c = lerp(top, mid, t) if t < 0.5 else lerp(mid, bot, (t - 0.5) * 2)
        for x in range(16):
            put(im, x, y, hi if i == 0 else c)
    save(im, name)


# ---------------------------------------------------------------- 桶口(顶)
def barrel_top():
    """桶口俯视：木沿一圈，中央暗色开口露出咸牛肉。"""
    im = new()
    rng = random.Random(1415)
    wood = hx("#7A4A2A")
    wood_hi = hx("#96603A")
    wood_dk = hx("#4A2A14")
    inner = hx("#2A1710")
    inner_l = hx("#3A2014")
    meat = hx("#8A3A2A")
    fat = hx("#D8B49A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.7:
                continue
            if r > 6.0:
                # 木沿：竖纹
                c = wood_hi if (x % 4 == 0) else wood
                if r > 7.2:
                    c = wood_dk
            else:
                c = inner
                if r > 4.4:
                    c = inner_l
                # 开口里的肉块/脂肪点
                if rng.random() < 0.35:
                    c = meat if rng.random() < 0.7 else fat
            put(im, x, y, c)
    save(im, "salted_beef_barrel_top.png")


# ---------------------------------------------------------------- 咸牛肉(顶)
def meat_top():
    """咸牛肉顶面：暗红瘦肉 + 米白脂肪纹 + 盐粒。"""
    im = new()
    rng = random.Random(1416)
    base = hx("#8A3A2A")
    dark = hx("#5E2418")
    deep = hx("#45180F")
    fat = hx("#D8B49A")
    fat_hi = hx("#EBD3C0")
    salt = hx("#F2E9DA")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 11:
                c = lerp(base, hx("#A04A34"), 0.5)
            elif x + y > 22:
                c = dark
            put(im, x, y, c)
    # 脂肪纹路（斜向短线）
    for _ in range(9):
        x, y = rng.randrange(13), rng.randrange(13)
        for i in range(3):
            put(im, x + i, y + i // 2, fat if i < 2 else fat_hi)
    # 深色肉缘与盐粒
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([deep, dark]))
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, salt)
    save(im, "salted_beef_meat_top.png")


# ---------------------------------------------------------------- 咸牛肉(侧)
def meat_side():
    im = new()
    rng = random.Random(1417)
    top = hx("#8A3A2A")
    bot = hx("#4E1C12")
    fat = hx("#D8B49A")
    dark = hx("#3E140C")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.0)
        for x in range(16):
            cc = c
            if x <= 2:
                cc = lerp(c, fat, 0.25)
            elif x >= 13:
                cc = lerp(c, dark, 0.4)
            put(im, x, y, cc)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([fat, dark]))
    save(im, "salted_beef_meat_side.png")


if __name__ == "__main__":
    barrel_side()
    barrel_side_with_hoop("salted_beef_barrel_side_hoop_bottom.png", list(range(12, 16)))
    barrel_side_with_hoop("salted_beef_barrel_side_hoop_top.png", list(range(0, 4)))
    barrel_top()
    meat_top()
    meat_side()
