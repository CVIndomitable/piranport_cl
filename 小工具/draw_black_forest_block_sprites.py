#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""黑森林蛋糕方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_17.png（白色方盘上的方形巧克力蛋糕，
      顶面四坨奶油、四颗红樱桃带长梗）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆盘/圆形奶油靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/black_forest_*.png
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
    """白瓷方盘俯视：圆角方形 + 浅灰碟沿，四角透明配合 cutout。"""
    im = new()
    white = hx("#FDFDFE")
    warm = hx("#F4F2F6")
    rim = hx("#EAE7EE")
    edge = hx("#D7D3DB")
    shadow = hx("#F0EEF3")
    x0, y0, x1, y1 = 1.0, 1.0, 15.0, 15.0
    r = 2.3
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            qx = min(max(xc, x0 + r), x1 - r)
            qy = min(max(yc, y0 + r), y1 - r)
            d = math.hypot(xc - qx, yc - qy) - r
            if d > 0.3:
                continue  # 圆角切角
            if d > -0.7:
                c = edge
            elif d > -1.5:
                c = rim
            elif d > -2.5:
                c = warm
            else:
                c = white
            # 右下轻压阴影
            if xc + yc > 18 and d <= -2.5:
                c = shadow if c == white else c
            put(im, x, y, c)
    # 左上盘沿高光
    for p in ((3, 3), (4, 3), (3, 4), (4, 4), (5, 4)):
        put(im, p[0], p[1], hx("#FFFFFF"))
    save(im, "black_forest_plate_top.png")


# ---------------------------------------------------------------- 方盘(侧)
def plate_side():
    """盘侧：竖直柔和渐变，左右各两列透明对应方盘圆角。"""
    im = new()
    top = hx("#FBFAFC")
    bot = hx("#DCD8E2")
    for x in range(3, 13):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.18
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        put(im, 2, y, lerp(c, hx("#E8E5EC"), 0.45))
        put(im, 13, y, lerp(c, hx("#E8E5EC"), 0.45))
    save(im, "black_forest_plate_side.png")


# ---------------------------------------------------------------- 蛋糕顶
def cake_top():
    """巧克力蛋糕顶面：深可可底色 + 浅色可可粉/蛋糕屑颗粒 + 左上柔光。"""
    im = new()
    rng = random.Random(20261017)
    base = hx("#41251E")
    dark = hx("#2C1813")
    light = hx("#5C382E")
    lighter = hx("#70483A")
    cocoa = hx("#6A4133")
    for y in range(16):
        for x in range(16):
            # 左上略亮的整体渐变
            c = lerp(base, light, max(0.0, (13 - (x + y)) / 40.0))
            put(im, x, y, c)
    # 可可粉/碎屑颗粒
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([light, lighter, dark, cocoa]))
    # 少数成对的浅色碎屑，读作蛋糕屑而非噪点
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        put(im, x, y, lighter)
        put(im, x + 1, y, light)
    # 四角压暗，让方蛋糕边缘收得住
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                put(im, x, y, lerp(im.getpixel((x, y)), dark, 0.45))
    save(im, "black_forest_cake_top.png")


# ---------------------------------------------------------------- 蛋糕侧
def cake_side():
    """巧克力蛋糕侧面：上亮下暗的可可渐变 + 横向蛋糕屑纹。"""
    im = new()
    rng = random.Random(1717)
    top = hx("#4C2D25")
    bot = hx("#2A1712")
    crumb = hx("#6B4437")
    crumb2 = hx("#7C5344")
    dark = hx("#20110D")
    for y in range(16):
        base = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            put(im, x, y, base)
    # 横向碎屑纹（原画侧面的粗糙颗粒感）
    for _ in range(30):
        x = rng.randrange(16)
        y = rng.randrange(3, 15)
        put(im, x, y, rng.choice([crumb, crumb, crumb2, dark]))
    # 底部收暗一档
    for x in range(16):
        put(im, x, 15, lerp(im.getpixel((x, 15)), dark, 0.5))
        put(im, x, 0, lerp(im.getpixel((x, 0)), hx("#61402F"), 0.35))
    save(im, "black_forest_cake_side.png")


# ---------------------------------------------------------------- 奶油(顶)
def cream_top():
    """裱花奶油顶面：圆形白色奶油团，同心旋纹 + 左上高光。"""
    im = new()
    cream = hx("#F6EFE1")
    hi = hx("#FFFCF4")
    shadow = hx("#E3D8C4")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.5:
                continue  # 圆形奶油
            ang = math.atan2(dy, dx)
            # 旋纹：半径叠加角度调制，呈螺旋裱花
            band = int(r + 1.6 * math.sin(ang * 3.0)) % 3
            c = cream if band == 0 else (hi if band == 1 else shadow)
            # 左上受光
            if dx < -1 and dy < -1:
                c = lerp(c, hi, 0.5)
            # 外圈轻压
            if r > 6.6:
                c = lerp(c, shadow, 0.5)
            put(im, x, y, c)
    # 中心小尖
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, p[0], p[1], hi)
    save(im, "black_forest_cream_top.png")


# ---------------------------------------------------------------- 奶油(侧)
def cream_side():
    im = new()
    top = hx("#FBF6EB")
    bot = hx("#E4D9C4")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(3, 13):
            cc = c
            if x <= 5:
                cc = lerp(c, hx("#FFFCF4"), 0.45)
            elif x >= 10:
                cc = lerp(c, hx("#D8CCB6"), 0.5)
            put(im, x, y, cc)
        # 左右切角，让裱花奶油在方盒上读作圆形
        put(im, 2, y, lerp(c, hx("#E8DDC8"), 0.5))
        put(im, 13, y, lerp(c, hx("#E8DDC8"), 0.5))
    save(im, "black_forest_cream_side.png")


# ---------------------------------------------------------------- 樱桃(顶)
def cherry_top():
    """樱桃顶面：深红圆球 + 左上高光点。"""
    im = new()
    base = hx("#B30318")
    dark = hx("#87020F")
    hi = hx("#E4566A")
    spec = hx("#FFE9E6")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.4:
                c = dark
            elif dx < -1.5 and dy < -1.5:
                c = hi
            else:
                c = base
            put(im, x, y, c)
    for p in ((4, 4), (5, 4), (4, 5)):
        put(im, p[0], p[1], spec)
    save(im, "black_forest_cherry_top.png")


# ---------------------------------------------------------------- 樱桃(侧)
def cherry_side():
    im = new()
    top = hx("#C00920")
    mid = hx("#A50214")
    bot = hx("#76010D")
    hi = hx("#E45A6C")
    for y in range(16):
        c = lerp(top, mid, min(1.0, y / 8.0)) if y < 8 else lerp(mid, bot, (y - 8) / 7.0)
        for x in range(16):
            cc = c
            if 3 <= x <= 5:
                cc = lerp(c, hi, 0.5)
            elif x >= 11:
                cc = lerp(c, bot, 0.45)
            put(im, x, y, cc)
    save(im, "black_forest_cherry_side.png")


# ---------------------------------------------------------------- 樱桃梗
def stem():
    im = new()
    base = hx("#566328")
    hi = hx("#6F7D3A")
    dark = hx("#3C481D")
    for y in range(16):
        for x in range(16):
            if x <= 3:
                c = hi
            elif x >= 12:
                c = dark
            else:
                c = base
            put(im, x, y, c)
    save(im, "black_forest_stem.png")


if __name__ == "__main__":
    plate_top()
    plate_side()
    cake_top()
    cake_side()
    cream_top()
    cream_side()
    cherry_top()
    cherry_side()
    stem()
