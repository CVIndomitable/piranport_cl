#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""吐司面包方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_1.png（白瓷盘 + 整条烤吐司 + 一片草莓果酱吐司）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
输出到: src/main/resources/assets/piranport/textures/block/toast_bread_*.png
"""
import os
import random
from PIL import Image

OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport/textures/block",
)

def hx(s):
    """#RRGGBB -> (r,g,b,a)"""
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)

def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c if len(c) == 4 else (*c, 255))

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def save(im, name):
    path = os.path.join(OUT_DIR, name)
    im.save(path)
    print("saved", path)

# ---------------------------------------------------------------- 盘面(顶)
def plate_top():
    im = new()
    white = hx("#FFFFFF")
    warm = hx("#F7F8FB")
    rim = hx("#EBECF1")
    edge = hx("#E3E4EA")
    shadow = hx("#F1F2F6")  # 盘面下半柔和阴影
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if r > 7.4:
                continue  # 圆角切角，配合 render_type cutout 呈圆形盘
            if r > 6.4:
                c = rim
            elif r > 5.6:
                c = warm
            else:
                c = white
            # 下半轻压一层阴影，模拟原画盘子受光
            if y >= 9 and r <= 6.4:
                c = shadow if c == white else c
            # 最外一圈描边用浅灰(非纯黑，乐事画风)
            if r > 7.0:
                c = edge
            put(im, x, y, c)
    # 盘沿高光：左上弧两点
    put(im, 3, 4, hx("#FFFFFF"))
    put(im, 4, 3, hx("#FFFFFF"))
    save(im, "toast_bread_plate_top.png")

# ---------------------------------------------------------------- 盘沿(侧)
def plate_side():
    im = new()
    rows = ["#FAFBFC", "#F4F5F8", "#F0F1F5", "#EEF0F4",
            "#ECEEF2", "#EAECF1", "#E8EAEF", "#E6E8ED",
            "#E4E6EC", "#E2E4EA", "#E0E2E8", "#DDDFE6",
            "#DADCE4", "#D7D9E1", "#D3D5DE", "#CFD1DB"]
    for y, h in enumerate(rows):
        for x in range(16):
            # 两端(圆盘切角处)透明，俯仰视角都呈圆盘
            if x == 0 or x == 15:
                continue
            put(im, x, y, hx(h))
    save(im, "toast_bread_plate_side.png")

# ---------------------------------------------------------------- 吐司顶(烤层)
def loaf_top():
    im = new()
    rng = random.Random(20260924)
    base = hx("#B87038")     # 焦糖中间调
    dark = hx("#7A4420")     # 边缘/背面
    crev = hx("#5A3016")     # 四瓣之间沟壑
    light = hx("#D08848")    # 瓣脊亮部
    bright = hx("#E8A860")   # 高光
    gloss = hx("#F0C88C")    # 镜面亮点
    # 底: 上下缘压暗( crust 往下卷 )
    for y in range(16):
        for x in range(16):
            c = base
            if y <= 1:
                c = dark
            elif y >= 14:
                c = hx("#8A4C24")
            elif y == 2 or y == 13:
                c = hx("#A86430")
            # 左右外缘略暗
            if x == 0 or x == 15:
                c = dark
            elif x == 1 or x == 14:
                if c == base:
                    c = hx("#A86430")
            put(im, x, y, c)
    # 四条竖瓣: 中心提亮(瓣沿长轴排布，对应原画四凸起)
    centers = [1, 5, 9, 13]
    for cx in centers:
        for y in range(3, 14):
            # 瓣中脊
            for dx, col in ((0, light), (1, light) if cx == 1 else (-1, light)):
                x = cx + (dx if cx in (1, 13) else dx)
                # 保持在瓣带内
                if abs(x - cx) <= 1:
                    if y in (6, 7, 8):
                        put(im, x, y, bright)
                    else:
                        put(im, x, y, col)
    # 沟壑: 瓣界 u=3,7,11
    for gx in (3, 7, 11):
        for y in range(3, 13):
            put(im, gx, y, crev)
            # 沟壑旁补一刀中间调避免过硬
            put(im, gx + 1, y, hx("#9A5A2C") if y % 2 == 0 else base)
    # 随机镜面点缀(乐事式小高光)
    for _ in range(6):
        x = rng.choice([1, 2, 5, 6, 9, 10, 13])
        y = rng.randint(5, 10)
        put(im, x, y, gloss)
    save(im, "toast_bread_loaf_top.png")

# ---------------------------------------------------------------- 吐司侧(瓤)
def loaf_side():
    im = new()
    rng = random.Random(114514)
    crust_edge = hx("#9A5A2C")  # 顶缘焦边
    crust = hx("#D8A858")       # 薄薄一圈金边
    crumb = hx("#F4E6BE")       # 奶白瓤
    line = hx("#E2CE9E")        # 横向纹理线
    line2 = hx("#D8C28E")
    shade = hx("#ECDCB1")       # 两侧轻压
    bottom = hx("#D6C28E")      # 底影
    for y in range(16):
        for x in range(16):
            c = crumb
            if y == 0:
                c = crust_edge
            elif y == 1:
                c = crust
            elif y >= 14:
                c = bottom
            # 横向瓤纹(原画的水平纹路)，带一点起伏
            if y in (5, 9, 12) and (x + (y % 3)) % 4 != 3:
                c = line
            if y in (7, 11) and x % 5 == 2:
                c = line2
            # 两侧轻阴影
            if x in (0, 15):
                c = shade if y > 1 else c
            # 随机撒一点瓤色差，镇守府式细噪点
            if rng.random() < 0.08 and 2 <= y <= 13:
                c = hx("#EFE0B0")
            put(im, x, y, c)
    save(im, "toast_bread_loaf_side.png")

# ---------------------------------------------------------------- 果酱片顶
def slice_top():
    im = new()
    rng = random.Random(1919810)
    crust_o = hx("#C08848")   # 外圈脆边
    crust = hx("#D8A860")     # 脆边
    inner = hx("#EFE0BC")     # 脆边内过渡
    crumb = hx("#FAF0D8")     # 奶白瓤
    jam = hx("#D03428")       # 果酱底
    jam_d = hx("#A02020")     # 果酱深块
    jam_m = hx("#E04838")     # 果酱亮块
    jam_l = hx("#F06850")     # 果酱高光
    gloss = hx("#F8A090")     # 水光
    white = hx("#FFF0E8")     # 糖粒
    # 底: 脆边框 + 奶白瓤; 四角切圆(render_type cutout)
    for y in range(16):
        for x in range(16):
            if (x < 2 and y < 2) or (x > 13 and y < 2) \
               or (x < 2 and y > 13) or (x > 13 and y > 13):
                continue  # 圆角
            d_edge = min(x, y, 15 - x, 15 - y)
            if d_edge == 0:
                c = crust_o
            elif d_edge == 1:
                c = crust
            elif d_edge == 2:
                c = inner
            else:
                c = crumb
                # 瓤面轻噪
                if rng.random() < 0.10:
                    c = hx("#F4E8C8")
            put(im, x, y, c)
    # 果酱团: 不规则椭圆覆盖中部偏左(按原画构图)
    jam_px = []
    for y in range(2, 14):
        for x in range(2, 14):
            # 椭圆 + 边缘抖动做出不规则涂抹感
            ex = (x - 7.3) / 5.2
            ey = (y - 7.6) / 4.6
            wob = (rng.random() - 0.5) * 0.22
            if ex * ex + ey * ey + wob < 1.0:
                put(im, x, y, jam)
                jam_px.append((x, y))
    # 深浅块面
    rng.shuffle(jam_px)
    for i, (x, y) in enumerate(jam_px):
        if i % 7 == 0:
            put(im, x, y, jam_d)
        elif i % 5 == 0:
            put(im, x, y, jam_m)
    # 高光与水光
    for _ in range(5):
        x, y = rng.choice(jam_px)
        put(im, x, y, jam_l)
    for x, y in [(6, 5), (8, 6), (5, 8)]:
        if (x, y) in jam_px:
            put(im, x, y, gloss)
    # 糖粒点缀
    for _ in range(3):
        x, y = rng.choice(jam_px)
        put(im, x, y, white)
    save(im, "toast_bread_slice_top.png")

# ---------------------------------------------------------------- 果酱片侧
def slice_side():
    im = new()
    crust_o = hx("#A87438")
    crust = hx("#C89048")
    crumb = hx("#F0E2B8")
    crumb_d = hx("#E0CE98")
    bottom = hx("#C9B478")
    for y in range(16):
        for x in range(16):
            if y <= 1:
                c = crust if y == 1 else crust_o
            elif y >= 14:
                c = bottom
            elif y >= 12:
                c = crumb_d
            else:
                c = crumb
            # 两端轻微内收暗角
            if x in (0, 15) and y > 1:
                c = crumb_d
            put(im, x, y, c)
    save(im, "toast_bread_slice_side.png")

if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    plate_top()
    plate_side()
    loaf_top()
    loaf_side()
    slice_top()
    slice_side()
    print("done: 6 textures")
