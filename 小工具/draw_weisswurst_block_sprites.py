#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""白香肠面包圈方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_8.png（巴伐利亚蓝白菱纹椭圆盘上：
      深棕碱水面包圈 + 两根白香肠 + 一颗肉丸 + 欧芹叶）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 椭圆盘靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/weisswurst_*.png
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


# ---------------------------------------------------------------- 椭圆盘(顶)
def plate_top():
    """巴伐利亚蓝白菱纹椭圆盘俯视：白瓷盘沿一圈深蓝菱纹，四角透明。"""
    im = new()
    white = hx("#F5F4EF")
    cream = hx("#EFEDE6")
    rim = hx("#F7F6F1")
    edge = hx("#D9D6CE")
    blue = hx("#002080")
    blue_dark = hx("#001A66")
    shadow = hx("#EAE7E0")
    cx = cy = 7.5
    rx, ry = 7.8, 6.7
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            e = (dx / rx) ** 2 + (dy / ry) ** 2
            if e > 1.0:
                continue  # 椭圆外透明
            theta = math.atan2(dy / ry, dx / rx)
            if e > 0.80:
                # 盘沿：蓝白相间的菱纹（原画蓝白菱形花边）
                step = int(((theta + math.pi) / (2 * math.pi)) * 24)
                c = blue if step % 2 == 0 else rim
                if e > 0.965:
                    c = edge
            elif e > 0.72:
                c = rim
            elif e > 0.72:
                c = cream
            else:
                c = white
            # 右下盘面轻压阴影
            if e <= 0.80 and dx + dy > 3.5:
                c = shadow if c == white else c
            put(im, x, y, c)
    # 左上盘沿高光
    for p in ((4, 3), (5, 3), (4, 4)):
        put(im, p[0], p[1], hx("#FFFFFF"))
    # 蓝菱纹里补一点深蓝分层，避免读成噪点
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[:3] == blue[:3]:
                if x % 2 == 0 and y % 2 == 0:
                    put(im, x, y, blue_dark)
    save(im, "weisswurst_plate_top.png")


# ---------------------------------------------------------------- 椭圆盘(侧)
def plate_side():
    im = new()
    top = hx("#FBFAF6")
    bot = hx("#DCD9D1")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.16
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.15)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.15)
        put(im, 2, y, lerp(c, hx("#E7E4DC"), 0.5))
        put(im, 13, y, lerp(c, hx("#E7E4DC"), 0.5))
    save(im, "weisswurst_plate_side.png")


# ---------------------------------------------------------------- 面包圈(顶)
def pretzel_top():
    """碱水面包圈顶面：金棕外壳 + 深色裂纹 + 粗盐粒。"""
    im = new()
    rng = random.Random(817)
    base = hx("#A75F2B")
    light = hx("#C5813F")
    gloss = hx("#D89A55")
    dark = hx("#7E4219")
    salt = hx("#F7F2E6")
    for y in range(16):
        for x in range(16):
            # 斜向受光
            c = lerp(base, light, max(0.0, (14 - (x + y)) / 34.0))
            put(im, x, y, c)
    # 裂纹/褶皱
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, dark)
        if rng.random() < 0.5:
            put(im, x, min(15, y + 1), lerp(dark, base, 0.5))
    # 高光与盐粒
    for _ in range(6):
        put(im, rng.randrange(16), rng.randrange(16), gloss)
    for _ in range(10):
        put(im, rng.randrange(16), rng.randrange(16), salt)
    save(im, "weisswurst_pretzel_top.png")


# ---------------------------------------------------------------- 面包圈(侧)
def pretzel_side():
    im = new()
    rng = random.Random(818)
    top = hx("#B26C33")
    bot = hx("#74390F")
    hi = hx("#C98444")
    dark = hx("#5E2C0C")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.0)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.5)
            elif x <= 2:
                cc = lerp(c, hi, 0.3)
            put(im, x, y, cc)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(4, 14)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "weisswurst_pretzel_side.png")


# ---------------------------------------------------------------- 白香肠(顶)
def sausage_top():
    """白香肠顶面：横向胶囊形，两端透明收圆；奶白带灰调与粉色肉感。"""
    im = new()
    base = hx("#E7E0D4")
    hi = hx("#F6F2E8")
    shade = hx("#CFC6B5")
    pink = hx("#EBD9CC")
    cy = 7.5
    r = 2.5
    x0, x1 = 2.5, 13.5
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            qx = min(max(xc, x0), x1)
            d = math.hypot(xc - qx, yc - cy)
            if d > r:
                continue  # 胶囊外透明 → 香肠两端在俯视下收圆
            if d > r - 0.7:
                c = shade
            elif yc < cy - 0.8:
                c = hi
            elif yc > cy + 0.9:
                c = shade
            else:
                c = base
            # 中段一条淡淡的粉色肉纹
            if 4 <= x <= 11 and abs(yc - cy) < 0.6:
                c = lerp(c, pink, 0.45)
            put(im, x, y, c)
    save(im, "weisswurst_sausage_top.png")


# ---------------------------------------------------------------- 白香肠(侧)
def sausage_side():
    im = new()
    top = hx("#F2EDE2")
    bot = hx("#D6CDBD")
    hi = hx("#FBF8F0")
    shade = hx("#C9C0AF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(2, 14):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.5)
            elif y >= 13:
                cc = lerp(c, shade, 0.5)
            put(im, x, y, cc)
        # 两端切角，让方盒端头读作圆头香肠
        put(im, 1, y, lerp(c, shade, 0.55))
        put(im, 14, y, lerp(c, shade, 0.55))
    save(im, "weisswurst_sausage_side.png")


# ---------------------------------------------------------------- 肉丸(顶)
def meatball_top():
    im = new()
    rng = random.Random(88)
    base = hx("#7B4B2C")
    dark = hx("#5C341D")
    light = hx("#A16A3E")
    hi = hx("#B98050")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.4:
                c = dark
            elif dx < -1 and dy < -1:
                c = hi
            else:
                c = base
            put(im, x, y, c)
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        if im.getpixel((x, y))[3]:
            put(im, x, y, rng.choice([light, dark]))
    save(im, "weisswurst_meatball_top.png")


# ---------------------------------------------------------------- 肉丸(侧)
def meatball_side():
    im = new()
    top = hx("#8A5533")
    bot = hx("#5A321C")
    hi = hx("#A97247")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.0)
        for x in range(16):
            cc = c
            if x <= 3:
                cc = lerp(c, hi, 0.4)
            elif x >= 13:
                cc = lerp(c, bot, 0.4)
            put(im, x, y, cc)
    save(im, "weisswurst_meatball_side.png")


# ---------------------------------------------------------------- 欧芹叶
def parsley():
    """透明底上的三片欧芹叶（点缀在面包圈与香肠之间）。"""
    im = new()
    leaf = hx("#4E7A22")
    leaf_hi = hx("#6D9A34")
    leaf_dk = hx("#35571A")
    vein = hx("#8FB553")

    def draw_leaf(cx, cy, length, width, angle_deg):
        ang = math.radians(angle_deg)
        ca, sa = math.cos(ang), math.sin(ang)
        for y in range(16):
            for x in range(16):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                u = dx * ca + dy * sa
                v = -dx * sa + dy * ca
                t = (u + length / 2) / length
                if not (0.05 < t < 0.95):
                    continue
                env = math.sin(math.pi * t) ** 0.7
                if abs(v) > width * env:
                    continue
                if abs(v) > width * env * 0.72:
                    c = leaf_dk
                elif abs(v) < width * env * 0.22 and t > 0.12:
                    c = vein
                elif u < 0:
                    c = leaf_hi
                else:
                    c = leaf
                put(im, x, y, c)

    draw_leaf(7.2, 8.2, 11.0, 3.6, -42.0)
    draw_leaf(9.8, 6.4, 6.8, 2.8, 26.0)
    draw_leaf(10.2, 10.4, 5.4, 2.4, 116.0)
    # 叶柄
    for p in ((2, 12), (3, 12), (3, 13), (4, 13)):
        put(im, p[0], p[1], leaf_dk)
    save(im, "weisswurst_parsley.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    pretzel_top()
    pretzel_side()
    sausage_top()
    sausage_side()
    meatball_top()
    meatball_side()
    parsley()
