#!/usr/bin/env python3
"""生成航空炸弹系列物品贴图（航空炸弹 / 小型航弹 / 中型航弹）。

画风对齐现有弹药贴图（item/torpedo_533_mm.png、item/small_he_shell.png）：
  - 32x32 画布，背景全透明，无抗锯齿（alpha 只有 0 和 255）
  - 轮廓为 1px 纯黑 #000000
  - 弹体为顶部受光的圆柱渐变（5~9 级灰阶）
  - 弹头后一道黄铜色识别环，取色与炮弹的铜环 / 引信一致

三张图按尺寸区分：小型 < 标准 < 中型（弹径 7 / 9 / 11 像素）。

    python3 tools/make_aerial_bomb_textures.py
"""
import os
from PIL import Image

W = H = 32
BLACK = (0, 0, 0, 255)
TAIL_NUB = 2        # 尾鳍后还露出的细尾杆列数

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src/main/resources/assets/piranport/textures/item")


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


# 弹体渐变，自上而下（水平圆柱，光从上方来）
RAMPS = {
    3: ['8b8b8b', 'a7a7a7', '5e5e5e'],
    5: ['7c7c7c', 'c9c9c9', 'a7a7a7', '5e5e5e', '3f3f3f'],
    7: ['5e5e5e', '8b8b8b', 'c9c9c9', 'a7a7a7', '8b8b8b', '5e5e5e', '3f3f3f'],
    9: ['5e5e5e', '7c7c7c', 'a7a7a7', 'c9c9c9', 'a7a7a7', '8b8b8b', '5e5e5e', '454545', '2f2f2f'],
}
# 黄铜环，取色自 small_he_shell 的引信 / 铜弹带
BRASS = {
    3: ['d8a210', 'ffdd64', 'a27828'],
    5: ['d8a210', 'ffdd64', 'e0aa29', 'a27828', '7a5a1c'],
    7: ['a27828', 'd8a210', 'ffdd64', 'e0aa29', 'd8a210', 'a27828', '7a5a1c'],
    9: ['a27828', 'c99a20', 'e0aa29', 'ffdd64', 'e0aa29', 'c99a20', 'a27828', '7a5a1c', '5e4416'],
}
# 尾鳍：平板，上缘亮下缘暗；整体比弹体暗部亮一档，避免尾部糊成一团黑
FIN = {
    7:  ['b0b0b0', 'a7a7a7', '8b8b8b', '8b8b8b', '7c7c7c', '6a6a6a', '5e5e5e'],
    9:  ['b0b0b0', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '7c7c7c', '6a6a6a', '5e5e5e', '5e5e5e'],
    11: ['b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '5e5e5e', '5e5e5e'],
    13: ['c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '5e5e5e', '5e5e5e'],
    15: ['c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e'],
    17: ['c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e', '5e5e5e'],
    19: ['c0c0c0', 'c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e', '5e5e5e'],
    21: ['c0c0c0', 'c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', 'a7a7a7', 'a7a7a7', '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c', '6a6a6a', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e', '5e5e5e', '5e5e5e'],
}


def ramp_for(table, n):
    """取 n 级渐变；表里没有正好 n 级时按比例从最近的档位重采样。"""
    if n in table:
        return [hx(c) for c in table[n]]
    best = min(sorted(table), key=lambda k: abs(k - n))
    src = table[best]
    return [hx(src[round(i * (len(src) - 1) / max(1, n - 1))]) for i in range(n)]


def build(profile, fins, band_x0, band_w):
    """profile : 每列的弹体半高（列坐标自左向右，第一项是弹尖）
       fins    : 每列的尾鳍半高；最后一列与弹体末端对齐，其后留 TAIL_NUB 列尾杆"""
    px = {}

    def column(cx, half, colors):
        for i, y in enumerate(range(-half, half + 1)):
            px[(cx, y)] = colors[i]

    # 1. 尾鳍先画，弹体压在上面
    fin_x0 = len(profile) - len(fins) - TAIL_NUB
    for i, fh in enumerate(fins):
        column(fin_x0 + i, fh, ramp_for(FIN, 2 * fh + 1))

    # 2. 弹体
    for cx, half in enumerate(profile):
        column(cx, half, ramp_for(RAMPS, 2 * half + 1))

    # 3. 黄铜识别环
    for cx in range(band_x0, band_x0 + band_w):
        if 0 <= cx < len(profile):
            column(cx, profile[cx], ramp_for(BRASS, 2 * profile[cx] + 1))

    # 4. 描边：只在最外圈上色（每列上下端 + 每行左右端）
    filled = set(px)
    row_extent = {}
    for (cx, cy) in filled:
        lo, hi = row_extent.get(cy, (99, -99))
        row_extent[cy] = (min(lo, cx), max(hi, cx))
    out = {}
    for (cx, cy), c in px.items():
        edge = ((cx, cy + 1) not in filled or (cx, cy - 1) not in filled
                or cx == row_extent[cy][0] or cx == row_extent[cy][1])
        out[(cx, cy)] = BLACK if edge else c

    # 5. 居中
    xs = [cx for cx, _ in out]
    ys = [cy for _, cy in out]
    dx = round((W - 1) / 2 - (min(xs) + max(xs)) / 2)
    dy = round((H - 1) / 2 - (min(ys) + max(ys)) / 2)
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    p = img.load()
    for (cx, cy), c in out.items():
        nx, ny = cx + dx, cy + dy
        if 0 <= nx < W and 0 <= ny < H:
            p[nx, ny] = c
    return img


VARIANTS = {
    # 小型航弹 —— 细而短
    'aerial_bomb_small': dict(
        profile=[1, 2, 3] + [3] * 10 + [3, 2, 1, 1, 1],
        fins=[3, 5, 6, 7, 7, 7, 7], band_x0=5, band_w=2),
    # 航空炸弹 —— 标准型
    'aerial_bomb': dict(
        profile=[1, 2, 3, 4] + [4] * 12 + [4, 3, 2, 1, 1, 1, 1],
        fins=[4, 6, 8, 9, 9, 9, 9, 9], band_x0=6, band_w=2),
    # 中型航弹 —— 长而粗
    'aerial_bomb_medium': dict(
        profile=[1, 2, 3, 4, 5] + [5] * 13 + [5, 4, 3, 2, 1, 1, 1, 1],
        fins=[5, 7, 9, 10, 10, 10, 10, 10], band_x0=7, band_w=3),
}


def main():
    imgs = {}
    for name, kw in VARIANTS.items():
        build(**kw).save(f"{OUT}/{name}.png")
        print(f"wrote {OUT}/{name}.png")
        imgs[name] = Image.open(f"{OUT}/{name}.png").convert("RGBA")

    # 放大预览 + 背包 16x16 实际观感
    scale = 10
    names = list(VARIANTS)
    canvas = Image.new("RGB", (len(names) * W * scale + 32, H * scale + 16), (30, 30, 32))
    for i, name in enumerate(names):
        big = imgs[name].resize((W * scale, H * scale), Image.NEAREST)
        canvas.paste(big, (8 + i * (W * scale + 8), 8), big)
    canvas.save("/tmp/aerial_bomb_preview.png")
    print("wrote /tmp/aerial_bomb_preview.png")


if __name__ == "__main__":
    main()
