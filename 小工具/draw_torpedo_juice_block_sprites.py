#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""鱼雷果汁方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_15.png（玻璃高杯装橙汁 + 杯口斜靠的青柠片）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 玻璃靠"边缘上色 + 中央透明"表现，配合 render_type: cutout
输出到: src/main/resources/assets/piranport/textures/block/torpedo_juice_*.png
"""
import math
import os
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
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def save(im, name):
    path = os.path.join(OUT_DIR, name)
    im.save(path)
    print("saved", path)


# ---------------------------------------------------------------- 玻璃杯壁(侧)
def glass_side():
    """玻璃壁：半透明淡蓝白的圆角矩形轮廓（SDF 描边），中央更透以透视内胆液体。

    模型用 render_type: translucent，所以这里的 alpha 直接生效；
    圆角轮廓让方形 box 的竖直棱看起来是杯沿，而非玻璃箱的直角。
    """
    im = new()
    wall_o = hx("#BEE1EEB4")   # 外沿：稍实，读作玻璃的一圈厚度
    wall = hx("#D8EFF8A0")     # 杯壁主体
    wall_i = hx("#E8F7FCA0")   # 内沿：更透
    rim = hx("#CBE8F2A8")      # 杯底横向收边
    gloss = hx("#FFFFFFBA")    # 竖向镜面高光
    x0, y0, x1, y1 = 0.5, 0.5, 15.5, 15.5
    r = 3.2
    for y in range(16):
        yc = y + 0.5
        for x in range(16):
            xc = x + 0.5
            qx = min(max(xc, x0 + r), x1 - r)
            qy = min(max(yc, y0 + r), y1 - r)
            d = math.hypot(xc - qx, yc - qy) - r
            if d > 0.4:
                continue  # 圆角外切掉
            if d < -1.1:
                continue  # 中央留空 → 透视内胆液体
            side = xc < 3.6 or xc > 12.4
            if side:
                c = wall_o if d > -0.5 else (wall if d > -0.85 else wall_i)
            elif yc > 13.0:
                c = rim  # 只留杯底收边；不画上沿，避免整面读成"相框"
            else:
                continue
            # 左侧竖向镜面高光 —— 玻璃杯的辨识特征
            if xc < 3.2 and 4 <= y <= 11:
                c = gloss
            put(im, x, y, c)
    save(im, "torpedo_juice_glass_side.png")


# ---------------------------------------------------------------- 玻璃杯口(顶)
def glass_top():
    """俯视杯口：一圈半透明玻璃环，环心透明 → 透出下方液面。"""
    im = new()
    ring_o = hx("#BEE1EEB4")
    ring = hx("#D8EFF8A0")
    ring_i = hx("#E8F7FCA0")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 7.4:
                continue
            if r > 6.6:
                c = ring_o
            elif r > 5.6:
                c = ring
            else:
                continue  # 中央透明：露出液面
            if r <= 6.0:
                c = ring_i
            put(im, x, y, c)
    save(im, "torpedo_juice_glass_top.png")


# ---------------------------------------------------------------- 液体(侧)
def liquid_side():
    im = new()
    top = hx("#FFCB52")
    bot = hx("#DE8818")
    gloss = hx("#FFE9A8")
    for y in range(16):
        base = lerp(top, bot, (y / 15.0) ** 0.85)
        for x in range(16):
            c = base
            if y == 0:
                c = hx("#FFE08A")   # 液面反光
            if x == 4 and 2 <= y <= 13:
                c = gloss            # 竖向高光
            elif x == 5 and 3 <= y <= 12:
                c = hx("#FFDA7C")
            if x in (0, 15):
                c = lerp(c, bot, 0.35)  # 贴壁处压暗
            put(im, x, y, c)
    save(im, "torpedo_juice_liquid_side.png")


# ---------------------------------------------------------------- 液体(顶)
def liquid_top():
    im = new()
    rim = hx("#FFDE86")
    surf = hx("#F9B434")
    deep = hx("#EE9E24")
    ripple = hx("#FFC95E")
    hi = hx("#FFF3C4")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.2:
                continue
            c = surf if r > 3.4 else deep
            if r > 6.3:
                c = rim          # 贴壁一圈亮环，读作液面受光
            # 两道横向波纹，避免纯同心圆的"蛋黄感"
            if (5 <= y <= 6 or 10 <= y <= 11) and 3 <= x <= 12 and (x + y) % 3 != 0:
                c = ripple
            put(im, x, y, c)
    for p in ((4, 4), (5, 4), (4, 5), (10, 11), (11, 11), (10, 12)):
        put(im, p[0], p[1], hi)
    save(im, "torpedo_juice_liquid_top.png")


# ---------------------------------------------------------------- 青柠切面(正/背)
def lime_top():
    im = new()
    rind_d = hx("#6E9A2C")
    rind = hx("#93BE44")
    pith = hx("#F2FBC4")
    flesh = hx("#E6F794")
    flesh_d = hx("#D2E878")
    seg_line = hx("#FBFFD8")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.8:
                c = rind_d
            elif r > 5.7:
                c = rind
            elif r > 4.9:
                c = pith
            else:
                c = flesh
                # 8 瓣放射：瓣界与瓣沿各分一档颜色
                frac = (math.degrees(math.atan2(dy, dx)) % 360) / 45.0
                frac -= int(frac)
                if min(frac, 1 - frac) < 0.15:
                    c = seg_line
                elif frac < 0.26 or frac > 0.74:
                    c = flesh_d
            put(im, x, y, c)
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, p[0], p[1], hx("#F8FECC"))
    save(im, "torpedo_juice_lime_top.png")


# ---------------------------------------------------------------- 青柠薄边
def lime_side():
    im = new()
    rind_d = hx("#628A26")
    rind = hx("#8AB63E")
    flesh = hx("#E2F58C")
    flesh_l = hx("#F0FBB4")
    flesh_d = hx("#CFE470")
    for y in range(16):
        for x in range(16):
            c = flesh
            if y == 0:
                c = rind_d
            elif y == 1:
                c = rind
            elif y >= 14:
                c = flesh_l
            if y in (6, 10) and x % 5 != 0:
                c = flesh_d
            put(im, x, y, c)
    save(im, "torpedo_juice_lime_side.png")


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    glass_side()
    glass_top()
    liquid_side()
    liquid_top()
    lime_top()
    lime_side()
    print("done: 6 textures")
