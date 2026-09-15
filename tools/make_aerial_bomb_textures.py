#!/usr/bin/env python3
"""生成航空炸弹物品贴图（item/aerial_bomb.png）。

航弹只有一种（小型/中型已在 AmmoItems 中统一，仅留 legacy 空壳兼容老存档），
所以只出一张贴图；aerial_bomb_small / aerial_bomb_medium 的模型直接复用它。

画风对齐现有弹药贴图（item/torpedo_533_mm.png、item/small_he_shell.png）：
  - 32x32 画布，背景全透明，无抗锯齿（alpha 只有 0 和 255）
  - 轮廓为 1px 纯黑 #000000
  - 弹体为顶部受光的圆柱渐变（9 级灰阶）
  - 弹头后一道黄铜识别环，取色与炮弹的铜弹带 / 引信一致

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
BODY_RAMP = ['5e5e5e', '7c7c7c', 'a7a7a7', 'c9c9c9', 'a7a7a7',
             '8b8b8b', '5e5e5e', '454545', '2f2f2f']
# 黄铜识别环，取色自 small_he_shell 的引信 / 铜弹带
BRASS_RAMP = ['a27828', 'c99a20', 'e0aa29', 'ffdd64', 'e0aa29',
              'c99a20', 'a27828', '7a5a1c', '5e4416']
# 尾鳍：平板，上缘亮下缘暗；整体比弹体暗部亮一档，避免尾部糊成一团黑
FIN_RAMP = ['c0c0c0', 'c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', 'a7a7a7',
            '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c',
            '6a6a6a', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e', '5e5e5e']


def ramp(table, n):
    """取 n 级渐变；表里没有正好 n 级时按比例重采样。"""
    if n == len(table):
        return [hx(c) for c in table]
    return [hx(table[round(i * (len(table) - 1) / max(1, n - 1))]) for i in range(n)]


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
        column(fin_x0 + i, fh, ramp(FIN_RAMP, 2 * fh + 1))

    # 2. 弹体
    for cx, half in enumerate(profile):
        column(cx, half, ramp(BODY_RAMP, 2 * half + 1))

    # 3. 黄铜识别环
    for cx in range(band_x0, band_x0 + band_w):
        if 0 <= cx < len(profile):
            column(cx, profile[cx], ramp(BRASS_RAMP, 2 * profile[cx] + 1))

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


# 弹径 9 像素的标准航弹：锥形弹头 -> 圆柱弹体 -> 收敛尾锥 -> 尾鳍 + 尾杆
BOMB = dict(
    profile=[1, 2, 3, 4] + [4] * 12 + [4, 3, 2, 1, 1, 1, 1],
    fins=[4, 6, 8, 9, 9, 9, 9, 9],
    band_x0=6, band_w=2)


def main():
    name = "aerial_bomb"
    build(**BOMB).save(f"{OUT}/{name}.png")
    print(f"wrote {OUT}/{name}.png")

    # 放大预览 + 背包 16x16 实际观感
    img = Image.open(f"{OUT}/{name}.png").convert("RGBA")
    scale = 10
    canvas = Image.new("RGB", (W * scale * 2 + 24, H * scale + 16), (30, 30, 32))
    big = img.resize((W * scale, H * scale), Image.NEAREST)
    canvas.paste(big, (8, 8), big)
    inv = img.resize((16, 16), Image.LANCZOS).resize((W * scale, H * scale), Image.NEAREST)
    canvas.paste(inv, (W * scale + 16, 8), inv)
    canvas.save("/tmp/aerial_bomb_preview.png")
    print("wrote /tmp/aerial_bomb_preview.png（左：原图放大，右：背包 16x16 观感）")


if __name__ == "__main__":
    main()
