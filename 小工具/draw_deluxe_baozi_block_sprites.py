#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""金镶玉包子方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_24.png（圆形竹蒸笼里摆着三个白胖包子，
      包顶有放射褶和一点粉色）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 蒸笼圆沿/包子圆顶靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/deluxe_baozi_*.png
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


# ---------------------------------------------------------------- 蒸笼侧
def basket_side():
    """竹蒸笼外壁：竖向竹片 + 上下深色箍带。"""
    im = new()
    base = hx("#D3A95E")
    hi = hx("#E6C47E")
    dark = hx("#B68B42")
    band = hx("#8A6A32")
    band_d = hx("#6E5224")
    for y in range(16):
        for x in range(4, 12):
            c = base
            if x % 4 == 0:
                c = hi
            elif x % 4 == 3:
                c = dark
            if y <= 1:
                c = lerp(c, hi, 0.45)
            elif y >= 13:
                c = lerp(c, band, 0.55)
            put(im, x, y, c)
        # 左右切角，让方盒侧壁读作圆蒸笼
        cc = base
        put(im, 2, y, lerp(cc, base, 0.4))
        put(im, 3, y, lerp(cc, dark, 0.3))
        put(im, 12, y, lerp(cc, dark, 0.3))
        put(im, 13, y, lerp(cc, base, 0.4))
    # 两道深色箍带
    for x in range(3, 13):
        put(im, x, 11, band)
        put(im, x, 12, band_d)
        put(im, x, 3, band)
    save(im, "deluxe_baozi_basket_side.png")


# ---------------------------------------------------------------- 蒸笼沿(顶)
def basket_top():
    """蒸笼沿俯视：竹圈顶面，中央镂空（露出笼底与包子）。"""
    im = new()
    base = hx("#D8AF63")
    hi = hx("#EACB87")
    dark = hx("#B0863C")
    edge = hx("#8A6A32")
    inner = hx("#C49A50")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < 6.5:
                continue  # 中央镂空
            if r > 7.85:
                continue
            if r > 7.4:
                c = edge
            elif x % 4 == 0:
                c = hi
            elif x % 4 == 3:
                c = dark
            else:
                c = base
            if r < 6.9:
                c = inner  # 内沿阴影
            put(im, x, y, c)
    save(im, "deluxe_baozi_basket_top.png")


# ---------------------------------------------------------------- 笼底
def basket_inner():
    """笼底竹编：深色交错竹条。"""
    im = new()
    base = hx("#6B5230")
    light = hx("#7E6238")
    dark = hx("#5A4526")
    for y in range(16):
        for x in range(16):
            c = base
            if x % 3 == 0 or y % 3 == 0:
                c = light
            if x % 5 == 2 and y % 5 == 2:
                c = dark
            put(im, x, y, c)
    save(im, "deluxe_baozi_basket_inner.png")


# ---------------------------------------------------------------- 包子(顶)
def bun_top():
    """包子顶：白面团 + 中心放射褶 + 一点粉红。"""
    im = new()
    base = hx("#F5F1E6")
    hi = hx("#FFFFFF")
    shade = hx("#E2DCCB")
    pleat = hx("#D3CBB6")
    pink = hx("#E8B0A0")
    pink_d = hx("#D98A7A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.6:
                continue  # 圆形包顶
            c = base
            if dx < -1 and dy < -1:
                c = hi
            elif r > 6.4:
                c = shade
            ang = math.atan2(dy, dx)
            # 放射褶：每 45° 一条浅阴影
            frac = ((math.degrees(ang) + 360) % 45) / 45.0
            if min(frac, 1 - frac) < 0.16 and r > 1.5:
                c = pleat
            put(im, x, y, c)
    # 中心粉色点
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, p[0], p[1], pink if (p[0] + p[1]) % 2 == 0 else pink_d)
    save(im, "deluxe_baozi_bun_top.png")


# ---------------------------------------------------------------- 包子(侧)
def bun_side():
    im = new()
    top = hx("#F8F4EA")
    bot = hx("#DDD6C4")
    pleat = hx("#E7E0CF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(3, 13):
            cc = c
            if x % 4 == 1:
                cc = lerp(c, pleat, 0.7)
            put(im, x, y, cc)
        # 左右切角，让包子侧壁读作圆弧
        put(im, 2, y, lerp(c, pleat, 0.35))
        put(im, 13, y, lerp(c, pleat, 0.35))
    save(im, "deluxe_baozi_bun_side.png")


if __name__ == "__main__":
    basket_side()
    basket_top()
    basket_inner()
    bun_top()
    bun_side()
