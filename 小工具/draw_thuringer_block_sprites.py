#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""图林根肠啤酒方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_7.png（白色椭圆盘：两根焦褐烤肠 +
      三根带红绿香料的浅色肠 + 土豆泥 + 西兰花 + 番茄酱挤线；
      盘右前方一杯带把手、满泡沫的金色啤酒）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 椭圆盘靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/thuringer_*.png
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


def rounded_rect_mask(radius=3.0):
    """16×16 圆角矩形遮罩，用于长条食物的两端收圆。"""
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
    """白色椭圆餐盘：盘沿一圈浅灰，右下柔和阴影。"""
    im = new()
    white = hx("#FDFDFE")
    warm = hx("#F5F4F2")
    rim = hx("#ECEAE7")
    edge = hx("#D8D5D1")
    shadow = hx("#F0EFEC")
    cx, cy = 7.5, 7.5
    rx, ry = 7.85, 7.0
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
    save(im, "thuringer_plate_top.png")


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
    save(im, "thuringer_plate_side.png")


# ---------------------------------------------------------------- 焦褐烤肠(顶)
def sausage_top():
    im = new()
    mask = rounded_rect_mask(3.0)
    rng = random.Random(707)
    base = hx("#96522A")
    dark = hx("#6B3517")
    char = hx("#4A2410")
    light = hx("#B06A36")
    fat = hx("#C98A52")
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            # 沿肠身方向（x）的明暗起伏
            t = (math.sin(x * 0.9 + y * 0.35) + 1.0) * 0.5
            c = lerp(base, light, t * 0.45)
            if y <= 2:
                c = lerp(c, light, 0.5)
            elif y >= 13:
                c = lerp(c, dark, 0.55)
            put(im, x, y, c)
    # 顺肠身的油亮亮线
    for y in (4, 8, 11):
        for x in range(3, 13):
            if mask[y][x]:
                put(im, x, y, fat if x % 2 else lerp(fat, light, 0.5))
    # 焦痕：不规则的短斑，避免读成木条
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(2, 14)
        if mask[y][x]:
            put(im, x, y, char if rng.random() < 0.6 else dark)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(2, 14)
        if mask[y][x]:
            put(im, x, y, light)
    save(im, "thuringer_sausage_top.png")


# ---------------------------------------------------------------- 焦褐烤肠(侧)
def sausage_side():
    im = new()
    top = hx("#7E411C")
    bot = hx("#4A220F")
    hi = hx("#A9602F")
    dark = hx("#331706")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(2, 14):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.5)
            put(im, x, y, cc)
        put(im, 1, y, lerp(c, dark, 0.45))
        put(im, 14, y, lerp(c, dark, 0.45))
    save(im, "thuringer_sausage_side.png")


# ---------------------------------------------------------------- 浅色香料肠(顶)
def pale_sausage_top():
    im = new()
    mask = rounded_rect_mask(3.0)
    rng = random.Random(708)
    base = hx("#EDE6D6")
    hi = hx("#FBF7EE")
    shade = hx("#D6CCB6")
    red = hx("#C0392B")
    green = hx("#6B8E23")
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            c = base
            if y <= 2:
                c = hi
            elif y >= 13:
                c = shade
            put(im, x, y, c)
    # 红绿香料碎粒（原画浅色肠上的彩点）
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(2, 14)
        if mask[y][x]:
            put(im, x, y, red if rng.random() < 0.65 else green)
    save(im, "thuringer_pale_sausage_top.png")


# ---------------------------------------------------------------- 浅色香料肠(侧)
def pale_sausage_side():
    im = new()
    top = hx("#F3EDE0")
    bot = hx("#D3C8B2")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(2, 14):
            cc = c
            if y <= 2:
                cc = lerp(c, hx("#FCF9F2"), 0.5)
            elif y >= 13:
                cc = lerp(c, hx("#C4B9A2"), 0.5)
            put(im, x, y, cc)
        put(im, 1, y, lerp(c, hx("#C4B9A2"), 0.45))
        put(im, 14, y, lerp(c, hx("#C4B9A2"), 0.45))
    save(im, "thuringer_pale_sausage_side.png")


# ---------------------------------------------------------------- 土豆泥(顶/侧)
def potato_top():
    im = new()
    rng = random.Random(77)
    base = hx("#E8D48A")
    hi = hx("#F5E7AC")
    shade = hx("#C9B069")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 7.5, y + 0.5 - 7.5
            if math.hypot(dx, dy) > 7.4:
                continue
            c = base
            if dx < -0.5 and dy < -0.5:
                c = hi
            elif math.hypot(dx, dy) > 6.4:
                c = shade
            put(im, x, y, c)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([hi, shade]))
    save(im, "thuringer_potato_top.png")


def potato_side():
    im = new()
    top = hx("#EEDD9C")
    bot = hx("#C2A961")
    hi = hx("#F7ECC0")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.4)
            elif x >= 13:
                cc = lerp(c, bot, 0.35)
            put(im, x, y, cc)
    save(im, "thuringer_potato_side.png")


# ---------------------------------------------------------------- 西兰花(顶/侧)
def broccoli_top():
    im = new()
    rng = random.Random(78)
    dark = hx("#3E6A1E")
    base = hx("#4E7A28")
    mid = hx("#63912F")
    hi = hx("#7FB344")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 7.5, y + 0.5 - 7.5
            if math.hypot(dx, dy) > 7.4:
                continue
            # 花球分瓣的高光分布
            n = math.sin(x * 1.7) * math.cos(y * 1.3)
            c = mid if n > 0 else base
            if dx < -1 and dy < -1:
                c = hi
            if dx * dx + dy * dy > 52:
                c = dark
            put(im, x, y, c)
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([dark, hi, mid]))
    save(im, "thuringer_broccoli_top.png")


def broccoli_side():
    im = new()
    top = hx("#5C8B2E")
    bot = hx("#39601B")
    hi = hx("#78A83C")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.45)
            elif x >= 13:
                cc = lerp(c, bot, 0.4)
            put(im, x, y, cc)
    save(im, "thuringer_broccoli_side.png")


# ---------------------------------------------------------------- 番茄酱挤线
def ketchup():
    """透明底：盘沿前一波浪形番茄酱线。"""
    im = new()
    red = hx("#C0231E")
    hi = hx("#DE4A38")
    dark = hx("#8C130F")
    for x in range(1, 15):
        y = int(8 + 2.2 * math.sin(x * 0.9))
        put(im, x, y, red)
        put(im, x, y + 1, red)
        if x % 3 == 0:
            put(im, x, y - 1, hi)
            put(im, x, y + 2, dark)
    save(im, "thuringer_ketchup.png")


# ---------------------------------------------------------------- 啤酒(侧/顶)
def beer_side():
    im = new()
    top = hx("#F0C05A")
    bot = hx("#C98A1E")
    hi = hx("#FFE39A")
    deep = hx("#B47614")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 0.85)
        for x in range(16):
            cc = c
            # 玻璃反光：左侧竖向高光带
            if 3 <= x <= 5:
                cc = lerp(c, hi, 0.55)
            elif x >= 12:
                cc = lerp(c, deep, 0.45)
            put(im, x, y, cc)
    # 顶部一圈浅色泡沫边
    for x in range(16):
        put(im, x, 0, hx("#F6E2A8"))
    save(im, "thuringer_beer_side.png")


def beer_top():
    im = new()
    c = hx("#E8B44A")
    for y in range(16):
        for x in range(16):
            put(im, x, y, c)
    save(im, "thuringer_beer_top.png")


# ---------------------------------------------------------------- 泡沫(顶/侧)
def foam_top():
    im = new()
    rng = random.Random(79)
    base = hx("#F7F4EC")
    shade = hx("#E5DFD0")
    hi = hx("#FFFFFF")
    for y in range(16):
        for x in range(16):
            put(im, x, y, base)
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([shade, shade, hi]))
    save(im, "thuringer_foam_top.png")


def foam_side():
    im = new()
    rng = random.Random(80)
    top = hx("#FBF9F3")
    bot = hx("#E2DCCD")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.2)
        for x in range(16):
            put(im, x, y, c)
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([hx("#FFFFFF"), hx("#D8D1C0")]))
    save(im, "thuringer_foam_side.png")


# ---------------------------------------------------------------- 杯把
def mug_handle():
    im = new()
    top = hx("#F2F5F6")
    bot = hx("#D5DADD")
    hi = hx("#FFFFFF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if x <= 4:
                cc = lerp(c, hi, 0.5)
            elif x >= 12:
                cc = lerp(c, hx("#C3C9CC"), 0.5)
            put(im, x, y, cc)
    save(im, "thuringer_mug_handle.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    sausage_top()
    sausage_side()
    pale_sausage_top()
    pale_sausage_side()
    potato_top()
    potato_side()
    broccoli_top()
    broccoli_side()
    ketchup()
    beer_side()
    beer_top()
    foam_top()
    foam_side()
    mug_handle()
