#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""罗宋汤方块模型 — 六面贴图绘制
原画: zjsnrwiki 文件:Cookbook_3.png（带底碟的宽口白瓷汤盘，深红甜菜汤 +
      中央酸奶油、左侧绿色香草）
画风参考: 农夫乐事(FarmersDelight) / 镇守府(ChinjufuMod) 食物贴图
  - 16×16、暖色低对比、柔和渐变、无纯黑描边
  - 圆形盘/碗靠贴图透明角 + 模型 render_type: cutout 实现
输出到: src/main/resources/assets/piranport/textures/block/borscht_*.png
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
    """两个颜色按 t(0-1) 线性插值，保持 alpha。"""
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def save(im, name):
    path = os.path.join(OUT_DIR, name)
    im.save(path)
    print("saved", path)


# ---------------------------------------------------------------- 底碟(顶)
def saucer_top():
    """白瓷底碟俯视：圆盘 + 一圈浅灰碟沿，去掉四角成圆形。"""
    im = new()
    white = hx("#FDFDFE")
    warm = hx("#F4F2F6")
    rim = hx("#EBE8EF")
    edge = hx("#D6D2DB")
    shadow = hx("#F0EEF3")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r > 7.75:
                continue  # 圆角切角，配合 cutout 呈圆盘
            if r > 7.2:
                c = edge
            elif r > 6.4:
                c = rim
            elif r > 5.7:
                c = warm
            else:
                c = white
            # 原画光源在左上，右下轻压一层暖灰
            if y >= 9 and r <= 6.4:
                c = shadow if c == white else c
            put(im, x, y, c)
    # 左上碟沿高光
    for p in ((3, 4), (4, 3), (5, 3), (3, 5)):
        put(im, p[0], p[1], hx("#FFFFFF"))
    save(im, "borscht_saucer_top.png")


# ---------------------------------------------------------------- 底碟(侧)
def saucer_side():
    """碟侧：竖直柔和渐变，左右各留两列透明让方盒四角读作圆碟。"""
    im = new()
    top = hx("#FAF9FC")
    bot = hx("#CFCBD5")
    for x in range(3, 13):
        # 中间略亮，两侧收暗，模拟圆柱受光
        band = 1.0 - min(abs(x - 7.5) / 7.5, 1.0) * 0.25
        for y in range(16):
            t = (y / 15.0) ** 1.1
            c = lerp(top, bot, t)
            c = tuple(int(v * band) if i < 3 else v for i, v in enumerate(c))
            put(im, x, y, c)
    # 切角旁补一列中间调，避免透明边生硬
    for y in range(16):
        t = (y / 15.0) ** 1.1
        c = lerp(top, bot, t)
        put(im, 2, y, lerp(c, hx("#E7E3EB"), 0.45))
        put(im, 13, y, lerp(c, hx("#E7E3EB"), 0.45))
    save(im, "borscht_saucer_side.png")


# ---------------------------------------------------------------- 碗体(侧)
def bowl_side():
    """白瓷碗壁：上亮下暗的柔和渐变 + 左侧竖向高光、右侧轻阴影；
    左右各两列透明，让方形 box 的竖棱在等轴视角下读作圆碗。"""
    im = new()
    top = hx("#FFFFFF")
    bot = hx("#DCD8E0")
    hi = hx("#FFFFFF")
    shade = hx("#E7E3EA")
    for y in range(16):
        t = (y / 15.0) ** 0.9
        base = lerp(top, bot, t)
        for x in range(3, 13):
            c = base
            # 左侧柔和反光带（原画碗壁高光）
            wa = max(0.0, 1.0 - abs(x - 4.2) / 2.6)
            if wa > 0 and 2 <= y <= 13:
                c = lerp(c, hi, 0.5 * wa)
            # 右侧柔和背光
            wb = max(0.0, 1.0 - abs(x - 11.5) / 2.8)
            if wb > 0:
                c = lerp(c, shade, 0.55 * wb)
            # 碗底收边压暗
            if y >= 14:
                c = lerp(c, hx("#CFCAD4"), 0.35)
            put(im, x, y, c)
        # 切角旁一列中间调
        edge = lerp(base, hx("#E4E0E8"), 0.5)
        put(im, 2, y, edge)
        put(im, 13, y, edge)
    save(im, "borscht_bowl_side.png")


# ---------------------------------------------------------------- 碗口(顶)
def bowl_top():
    """碗口俯视：外圈白瓷 + 中央镂空（透明），透过镂空看到下方汤面。"""
    im = new()
    edge = hx("#D6D2DB")
    rim = hx("#EBE8EE")
    face = hx("#FAF9FC")
    lip = hx("#FFFFFF")
    inner = hx("#D8D3DD")  # 内沿阴影，衬出碗壁厚度
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r < 6.0:
                continue  # 中央透明：露出汤面
            if r > 7.85:
                continue  # 圆角
            if r > 7.35:
                c = edge
            elif r > 6.7:
                c = rim
            elif r > 6.25:
                c = face
            else:
                c = inner
            # 左上碗沿高光
            if 6.6 < r <= 7.3 and x + y < 13:
                c = lip
            put(im, x, y, c)
    save(im, "borscht_bowl_top.png")


# ---------------------------------------------------------------- 汤面
def soup():
    """甜菜红汤面：深红渐变 + 橙红汤汁漩涡 + 半沉的甜菜/胡萝卜块。"""
    im = new()
    deep = hx("#7E0607")
    base = hx("#9C0C0A")
    bright = hx("#B4261D")
    glow = hx("#C63A2A")
    chunk = hx("#CE6A4A")
    chunk_hi = hx("#DE8A62")
    chunk_dk = hx("#A94A32")
    fleck = hx("#E97A52")
    for y in range(16):
        for x in range(16):
            # 左上受光更亮，右下更深
            t = ((x + y) / 30.0) * 0.8
            c = lerp(base, deep, t)
            # 两道横向光泽汤纹
            if y in (4, 5) and 2 <= x <= 13 and not (x in (8, 9)):
                c = lerp(c, bright, 0.5)
            elif y in (9, 10) and 3 <= x <= 12:
                c = lerp(c, bright, 0.35)
            # 中央偏左的柔和高光（原画汤面反光）
            d = math.hypot(x - 5.5, y - 5.0)
            if d < 4.0:
                c = lerp(c, glow, (4.0 - d) * 0.10)
            put(im, x, y, c)
    # 汤料块：偏居汤面边缘、2×2 左右的柔和块，避免"撒芝麻"式碎点
    blocks = [
        (2, 4, 2, 2), (11, 3, 2, 2), (2, 9, 2, 2), (10, 10, 2, 2),
        (6, 12, 2, 2), (12, 7, 1, 2), (4, 13, 1, 1), (13, 12, 1, 1),
    ]
    for bx, by, w, h in blocks:
        for yy in range(by, by + h):
            for xx in range(bx, bx + w):
                edge = xx == bx + w - 1 or yy == by + h - 1
                c = chunk_dk if edge else chunk
                if xx == bx and yy == by:
                    c = chunk_hi
                put(im, xx, yy, c)
    # 零星高光点（汤面反光，不是汤料）
    for p in ((5, 6), (7, 8), (12, 5), (4, 3), (9, 13)):
        put(im, p[0], p[1], fleck)
    save(im, "borscht_soup.png")


# ---------------------------------------------------------------- 酸奶油(顶)
def cream_top():
    """酸奶油顶面：奶白团，左上高光、右下暖灰阴影。"""
    im = new()
    cream = hx("#F7F1E3")
    hi = hx("#FFFCF4")
    shadow = hx("#DED5BF")
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot((x + 0.5 - cx) * 1.05, (y + 0.5 - cy) * 1.0)
            if r > 7.6:
                continue
            if r > 6.9:
                c = shadow
            elif (x < 7 and y < 7) or r < 2.4:
                c = hi
            else:
                c = cream
            put(im, x, y, c)
    save(im, "borscht_cream_top.png")


# ---------------------------------------------------------------- 酸奶油(侧)
def cream_side():
    im = new()
    top = hx("#FBF6EA")
    bot = hx("#DDD4BF")
    for y in range(16):
        c = lerp(top, bot, (y / 15.0) ** 1.1)
        for x in range(16):
            cc = c
            if x <= 2:
                cc = lerp(c, hx("#FFFCF4"), 0.45)
            elif x >= 13:
                cc = lerp(c, hx("#D2C8B1"), 0.5)
            put(im, x, y, cc)
    save(im, "borscht_cream_side.png")


# ---------------------------------------------------------------- 香草叶(贴花)
def herb():
    """透明底上的两片欧芹叶：大叶朝左下、小叶朝右上，叶脉提亮。"""
    im = new()
    leaf = hx("#5E8C25")
    leaf_hi = hx("#83AE3E")
    leaf_dk = hx("#43681A")
    vein = hx("#9CC253")

    def draw_leaf(cx, cy, length, width, angle_deg, colors):
        """按旋转后的叶形（两端收尖）逐像素填充。"""
        ang = math.radians(angle_deg)
        ca, sa = math.cos(ang), math.sin(ang)
        for y in range(16):
            for x in range(16):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                u = dx * ca + dy * sa     # 沿叶长
                v = -dx * sa + dy * ca    # 垂直叶长
                t = (u + length / 2) / length
                if not (0.04 < t < 0.96):
                    continue
                # 两端收尖的宽度包络
                env = math.sin(math.pi * t) ** 0.72
                if abs(v) > width * env:
                    continue
                if abs(v) > width * env * 0.72:
                    c = colors[2]
                elif abs(v) < width * env * 0.20 and t > 0.12:
                    c = colors[3]      # 叶脉
                elif u < 0:
                    c = colors[1]
                else:
                    c = colors[0]
                put(im, x, y, c)

        return

    # 大叶：从左下伸向右上（原画香草压在汤面左侧）
    draw_leaf(6.6, 7.6, 11.5, 4.1, -38.0, (leaf, leaf_hi, leaf_dk, vein))
    # 小叶：交叉叠在大叶上方
    draw_leaf(10.2, 5.2, 7.0, 3.0, 30.0, (leaf, leaf_hi, leaf_dk, vein))
    # 第三片小叶子：朝右下，增加原画那种成簇感
    draw_leaf(10.8, 10.2, 5.2, 2.2, 118.0, (leaf, leaf_hi, leaf_dk, vein))
    # 叶柄
    for p in ((2, 11), (3, 11), (3, 12), (4, 12)):
        put(im, p[0], p[1], leaf_dk)
    save(im, "borscht_herb.png")


if __name__ == "__main__":
    saucer_top()
    saucer_side()
    bowl_side()
    bowl_top()
    soup()
    cream_top()
    cream_side()
    herb()
