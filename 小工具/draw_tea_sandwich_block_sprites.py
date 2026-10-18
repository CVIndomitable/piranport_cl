#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""红茶三明治方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_28.png（双层金架下午茶点架，上下两层
      摆着白面包手指三明治（绿色黄瓜/红色夹心），左侧一套红茶杯碟）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆盘/杯口靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/tea_sandwich_*.png
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


# ---------------------------------------------------------------- 点心架盘(顶)
def plate_top():
    """白瓷盘：盘沿一圈红色小点花边，四角透明。"""
    im = new()
    white = hx("#FAF9F5")
    hi = hx("#FFFFFF")
    shade = hx("#E9E6DE")
    edge = hx("#D6D2C9")
    red = hx("#B83A32")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.7:
                continue
            if r > 7.15:
                c = edge
            elif r > 6.6:
                # 盘沿红点花边
                ang = math.atan2(dy, dx)
                step = int(((ang + math.pi) / (2 * math.pi)) * 20)
                c = red if step % 2 == 0 else white
            elif r > 5.6:
                c = shade
            else:
                c = white
            if dx < -1 and dy < -1 and r > 6.6:
                c = hi
            put(im, x, y, c)
    save(im, "tea_sandwich_plate_top.png")


# ---------------------------------------------------------------- 点心架盘(侧)
def plate_side():
    im = new()
    top = hx("#FBF9F4")
    bot = hx("#DAD5CA")
    gold = hx("#C9A544")
    for x in range(1, 15):
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            if y >= 13:
                c = lerp(c, gold, 0.45)
            put(im, x, y, c)
    save(im, "tea_sandwich_plate_side.png")


# ---------------------------------------------------------------- 金架
def gold():
    im = new()
    base = hx("#C9A544")
    hi = hx("#E8CC7A")
    dark = hx("#9A7628")
    deep = hx("#7A5A1C")
    for y in range(16):
        c = lerp(hi, base, min(1.0, y / 6.0)) if y < 6 else lerp(base, dark, (y - 6) / 9.0)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.5)
            elif x >= 12:
                cc = lerp(c, deep, 0.5)
            put(im, x, y, cc)
    save(im, "tea_sandwich_gold.png")


# ---------------------------------------------------------------- 三明治
def bread_top():
    im = new()
    base = hx("#F4EEDC")
    hi = hx("#FFFDF4")
    crust = hx("#E2D6B8")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 12:
                c = hi
            elif x + y > 21:
                c = crust
            # 面包边
            if x <= 1 or x >= 14 or y <= 1 or y >= 14:
                c = lerp(c, crust, 0.55)
            put(im, x, y, c)
    save(im, "tea_sandwich_top.png")


def bread_cut():
    """三明治切面：白面包夹绿色黄瓜与红色夹心。"""
    im = new()
    bread = hx("#F6F0DE")
    bread_d = hx("#E6DCC4")
    green = hx("#6E9A3A")
    green_d = hx("#557A2A")
    red = hx("#C94A3A")
    red_d = hx("#A83228")
    for y in range(16):
        for x in range(16):
            c = bread
            if 4 <= y <= 6:
                c = green if y != 6 else green_d
            elif 7 <= y <= 9:
                c = red if y != 9 else red_d
            elif y >= 13:
                c = bread_d
            # 两端面包边
            if x <= 1 or x >= 14:
                c = bread_d
            put(im, x, y, c)
    save(im, "tea_sandwich_cut.png")


def bread_crust():
    im = new()
    top = hx("#F2EBD8")
    bot = hx("#DFD3B8")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            put(im, x, y, c)
    save(im, "tea_sandwich_crust.png")


# ---------------------------------------------------------------- 茶杯
def cup_side():
    im = new()
    top = hx("#FBF9F4")
    bot = hx("#DCD7CC")
    gold = hx("#C9A544")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 1:
                cc = lerp(c, gold, 0.45)
            put(im, x, y, cc)
    save(im, "tea_sandwich_cup_side.png")


def cup_top():
    """杯口：白瓷沿 + 中央深红茶水。"""
    im = new()
    white = hx("#FAF8F3")
    edge = hx("#D8D4CA")
    tea = hx("#7A2A1A")
    tea_l = hx("#A04A2E")
    tea_hi = hx("#C87A4A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue
            if r > 6.4:
                c = white if r < 7.0 else edge
            else:
                c = tea
                if dx + dy < -2:
                    c = tea_l
                elif dx + dy > 3:
                    c = tea_hi
            put(im, x, y, c)
    save(im, "tea_sandwich_cup_top.png")


def cup_handle():
    im = new()
    top = hx("#FBF9F4")
    bot = hx("#DCD7CC")
    gold = hx("#C9A544")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if y <= 3:
                cc = lerp(c, gold, 0.4)
            put(im, x, y, cc)
    save(im, "tea_sandwich_cup_handle.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    gold()
    bread_top()
    bread_cut()
    bread_crust()
    cup_side()
    cup_top()
    cup_handle()
