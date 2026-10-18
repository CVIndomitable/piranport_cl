#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""炸鱼薯条方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_6.png（揉皱的报纸/油纸垫底，白纸托着
      一大块金黄炸鱼柳、一堆粗薯条、一片柠檬和一小枝欧芹）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 报纸不规则边缘靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/fish_chips_*.png
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


# ---------------------------------------------------------------- 报纸/油纸(顶)
def paper_top():
    """揉皱的报纸：暖白纸底 + 灰色印刷字行 + 折痕阴影，边缘不规则。"""
    im = new()
    rng = random.Random(606)
    base = hx("#E9E6E0")
    light = hx("#F5F3EE")
    shade = hx("#D2CEC7")
    crease = hx("#C3BFB7")
    ink = hx("#9B9A95")
    ink_dark = hx("#807F7A")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            # 不规则边缘：椭圆 + 角度扰动
            ang = math.atan2(dy, dx)
            edge = 7.5 + 0.55 * math.sin(ang * 3.0 + 1.2) + 0.35 * math.sin(ang * 5.0)
            if math.hypot(dx, dy) > edge:
                continue
            c = base
            if dx + dy < -2:
                c = light
            elif dx + dy > 4:
                c = shade
            put(im, x, y, c)
    # 折痕：几条斜向阴影/亮线
    for i in range(3):
        x0 = rng.randrange(2, 12)
        for t in range(0, 12):
            x = x0 + t
            y = 2 + i * 5 + t // 4
            if x < 16 and y < 16 and im.getpixel((x, y))[3]:
                put(im, x, y, crease if i % 2 else light)
    # 报纸印刷字行：密集短横线 + 一条粗标题栏
    for row in range(2, 15):
        for x in range(1, 15):
            if not im.getpixel((x, row))[3]:
                continue
            if row == 4:
                if x % 5 != 4:
                    put(im, x, row, ink_dark)  # 标题栏
            elif x % 4 != 3 and rng.random() < 0.72:
                put(im, x, row, ink if rng.random() < 0.85 else ink_dark)
    # 一点红色油印痕迹（原画报纸右下角）
    for p in ((11, 12), (12, 12), (12, 13)):
        if im.getpixel(p)[3]:
            put(im, p[0], p[1], hx("#B98F86"))
    save(im, "fish_chips_paper_top.png")


# ---------------------------------------------------------------- 报纸(侧)
def paper_side():
    im = new()
    top = hx("#F2F0EA")
    bot = hx("#CFCBC3")
    for x in range(2, 14):
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.12
        for y in range(16):
            c = lerp(top, bot, (y / 15.0) ** 1.1)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        put(im, 1, y, lerp(c, hx("#E3DFD8"), 0.5))
        put(im, 14, y, lerp(c, hx("#E3DFD8"), 0.5))
    save(im, "fish_chips_paper_side.png")


# ---------------------------------------------------------------- 内层白纸(顶)
def inner_paper_top():
    im = new()
    base = hx("#FAF8F3")
    light = hx("#FFFFFF")
    shade = hx("#E7E4DD")
    crease = hx("#DEDAD2")
    for y in range(16):
        for x in range(16):
            c = base
            if x + y < 11:
                c = light
            elif x + y > 21:
                c = shade
            put(im, x, y, c)
    # 两道折痕
    for t in range(16):
        put(im, t, 4 + t // 5, crease)
        put(im, 11 - t // 6, t, crease)
    save(im, "fish_chips_inner_paper_top.png")


# ---------------------------------------------------------------- 内层白纸(侧)
def inner_paper_side():
    im = new()
    top = hx("#FBF9F5")
    bot = hx("#E0DCD4")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            put(im, x, y, c)
    save(im, "fish_chips_inner_paper_side.png")


# ---------------------------------------------------------------- 炸鱼(顶)
def fish_top():
    """金黄面糊外壳：深浅金棕斑块 + 酥脆裂纹。"""
    im = new()
    rng = random.Random(607)
    base = hx("#D9A23E")
    light = hx("#EFC463")
    hi = hx("#F9DE8C")
    dark = hx("#A96C1E")
    brown = hx("#8A4E12")
    for y in range(16):
        for x in range(16):
            t = (math.sin(x * 0.8 + y * 0.5) + 1.0) * 0.5
            c = lerp(base, light, t * 0.5)
            if y <= 1:
                c = lerp(c, light, 0.5)
            elif y >= 14:
                c = lerp(c, dark, 0.45)
            # 面糊外缘压深一圈，给鱼柳一个清楚轮廓
            if x <= 0 or x >= 15 or y <= 0 or y >= 15:
                c = lerp(c, brown, 0.55)
            put(im, x, y, c)
    # 面糊裂纹与鼓包
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        put(im, x, y, rng.choice([dark, brown, hi]))
    for _ in range(8):
        x, y = rng.randrange(15), rng.randrange(15)
        put(im, x, y, dark)
        put(im, x + 1, y, light)
    save(im, "fish_chips_fish_top.png")


# ---------------------------------------------------------------- 炸鱼(侧)
def fish_side():
    im = new()
    rng = random.Random(608)
    top = hx("#DCA744")
    bot = hx("#9A5C18")
    hi = hx("#F0C96B")
    dark = hx("#7A420E")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.5)
            put(im, x, y, cc)
    for _ in range(12):
        x, y = rng.randrange(16), rng.randrange(3, 13)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "fish_chips_fish_side.png")


# ---------------------------------------------------------------- 薯条(顶)
def chip_top():
    """粗切薯条顶面：金黄外壳，中段一条更亮的切面。"""
    im = new()
    rng = random.Random(609)
    base = hx("#E3AF48")
    light = hx("#F3CE6E")
    hi = hx("#FBE79C")
    dark = hx("#B87A24")
    for y in range(16):
        for x in range(16):
            c = base
            if 5 <= y <= 10:
                c = light
            elif y < 3 or y > 12:
                c = dark
            if y in (7, 8):
                c = lerp(c, hi, 0.5)
            # 两端略深
            if x <= 1 or x >= 14:
                c = lerp(c, dark, 0.4)
            put(im, x, y, c)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(3, 13)
        put(im, x, y, rng.choice([hi, dark]))
    save(im, "fish_chips_chip_top.png")


# ---------------------------------------------------------------- 薯条(侧)
def chip_side():
    im = new()
    top = hx("#EDBC59")
    bot = hx("#A96A1C")
    hi = hx("#F7D883")
    dark = hx("#83490E")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.05)
        for x in range(16):
            cc = c
            if y <= 2:
                cc = lerp(c, hi, 0.45)
            elif y >= 13:
                cc = lerp(c, dark, 0.5)
            put(im, x, y, cc)
    save(im, "fish_chips_chip_side.png")


# ---------------------------------------------------------------- 柠檬片(顶)
def lemon_top():
    """柠檬切片：黄色果皮圈 + 白瓤 + 八瓣果肉。"""
    im = new()
    rind = hx("#E8C23A")
    rind_d = hx("#C99A1E")
    pith = hx("#F8F2CC")
    flesh = hx("#F6E27A")
    flesh_d = hx("#EBD05C")
    seg_line = hx("#FFFBDD")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.8:
                c = rind_d
            elif r > 5.9:
                c = rind
            elif r > 5.1:
                c = pith
            else:
                c = flesh
                frac = (math.degrees(math.atan2(dy, dx)) % 360) / 45.0
                frac -= int(frac)
                if min(frac, 1 - frac) < 0.14:
                    c = seg_line
                elif frac < 0.26 or frac > 0.74:
                    c = flesh_d
            put(im, x, y, c)
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, p[0], p[1], hx("#FDF8D2"))
    save(im, "fish_chips_lemon_top.png")


# ---------------------------------------------------------------- 柠檬片(侧)
def lemon_side():
    im = new()
    rind = hx("#E8C23A")
    rind_d = hx("#C99A1E")
    pith = hx("#F8F2CC")
    for y in range(16):
        c = rind if 3 <= y <= 12 else rind_d
        for x in range(16):
            cc = c
            if x <= 3:
                cc = pith if 4 <= y <= 11 else rind_d
            put(im, x, y, cc)
    save(im, "fish_chips_lemon_side.png")


# ---------------------------------------------------------------- 欧芹
def parsley():
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

    draw_leaf(6.8, 8.4, 10.0, 3.2, -40.0)
    draw_leaf(9.6, 6.8, 6.0, 2.5, 30.0)
    draw_leaf(10.0, 10.4, 4.8, 2.1, 120.0)
    for p in ((2, 12), (3, 12), (3, 13)):
        put(im, p[0], p[1], leaf_dk)
    save(im, "fish_chips_parsley.png")


if __name__ == "__main__":
    paper_top()
    paper_side()
    inner_paper_top()
    inner_paper_side()
    fish_top()
    fish_side()
    chip_top()
    chip_side()
    lemon_top()
    lemon_side()
    parsley()
