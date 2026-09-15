#!/usr/bin/env python3
"""生成弹药类物品贴图（航弹、深水炸弹）。

画风对齐现有弹药贴图（item/torpedo_533_mm.png、item/small_he_shell.png）：
  - 32x32 画布，背景全透明，无抗锯齿（alpha 只有 0 和 255）
  - 轮廓为 1px 纯黑 #000000
  - 弹体为顶部受光的圆柱渐变
  - 黄铜件取色与炮弹的铜弹带 / 引信一致

    python3 tools/make_ammo_textures.py
"""
import os
from PIL import Image

W = H = 32
BLACK = (0, 0, 0, 255)


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


# 钢质弹体渐变，自上而下（水平圆柱，光从上方来）
STEEL = ['5e5e5e', '7c7c7c', 'a7a7a7', 'c9c9c9', 'a7a7a7',
         '8b8b8b', '5e5e5e', '454545', '2f2f2f']
# 黄铜，取色自 small_he_shell 的引信 / 铜弹带
BRASS = ['a27828', 'c99a20', 'e0aa29', 'ffdd64', 'e0aa29',
         'c99a20', 'a27828', '7a5a1c', '5e4416']
# 尾鳍：平板，上缘亮下缘暗；整体比弹体暗部亮一档，避免尾部糊成一团黑
FIN = ['c0c0c0', 'c0c0c0', 'b0b0b0', 'b0b0b0', 'a7a7a7', 'a7a7a7', 'a7a7a7',
       '8b8b8b', '8b8b8b', '8b8b8b', '8b8b8b', '7c7c7c', '7c7c7c',
       '6a6a6a', '6a6a6a', '6a6a6a', '5e5e5e', '5e5e5e', '5e5e5e']


def ramp(table, n):
    """取 n 级渐变；表里没有正好 n 级时按比例重采样。"""
    if n == len(table):
        return [hx(c) for c in table]
    return [hx(table[round(i * (len(table) - 1) / max(1, n - 1))]) for i in range(n)]


def render(layers):
    """layers: 有序的 (x, half, colors)，colors 自 -half 到 +half 逐行上色。
    按顺序逐像素绘制，后画的只覆盖重合部分——尾鳍这类"先画、被弹体压住一半"
    的部件必须靠这个顺序保留露出来的部分，所以不能按 x 去重。
    返回 32x32 贴图：外圈描 1px 纯黑边，最后整体居中。"""
    px = {}
    for cx, half, colors in layers:
        for i, y in enumerate(range(-half, half + 1)):
            px[(cx, y)] = colors[i]
    return finish(px)


def put_row(px, cy, half, colors):
    """垂直部件用：在 y=cy 这行铺一条自 x=-half 到 +half 的横向渐变。"""
    for i, x in enumerate(range(-half, half + 1)):
        px[(x, cy)] = colors[i]


def finish(px):
    """描 1px 纯黑外框并居中到 32x32 画布。"""
    # 描边：只在最外圈上色（每列上下端 + 每行左右端）
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


# ---------------------------------------------------------------- 航空炸弹
# 弹径 9 像素：锥形弹头 -> 圆柱弹体 -> 收敛尾锥 -> 尾鳍 + 尾杆
TAIL_NUB = 2        # 尾鳍后还露出的细尾杆列数


def aerial_bomb():
    profile = [1, 2, 3, 4] + [4] * 12 + [4, 3, 2, 1, 1, 1, 1]
    fins = [4, 6, 8, 9, 9, 9, 9, 9]
    band_x0, band_w = 6, 2

    layers = []
    # 尾鳍先放，弹体压在上面
    fin_x0 = len(profile) - len(fins) - TAIL_NUB
    for i, fh in enumerate(fins):
        layers.append((fin_x0 + i, fh, ramp(FIN, 2 * fh + 1)))
    for cx, half in enumerate(profile):
        colors = ramp(BRASS, 2 * half + 1) if band_x0 <= cx < band_x0 + band_w \
            else ramp(STEEL, 2 * half + 1)
        layers.append((cx, half, colors))
    return render(layers)


# ---------------------------------------------------------------- 深水炸弹
# 横放的钢桶：两端倒角封头 + 桶身两道加强箍 + 一端静水压引信
def depth_charge():
    """横躺的钢桶，桶身两道加强箍。
    正侧视看不见端面，所以靠"两端一段暗边 + 箍的明暗"来读成桶，而不是管子。"""
    half = 6                                   # 桶身半高 -> 桶径 13 像素
    caps = [4, 5, half]                        # 两端倒角
    profile = caps + [half] * 16 + caps[::-1]
    cap_cols = set(range(len(caps))) | set(range(len(profile) - len(caps), len(profile)))
    hoops = {6, 7, 14, 15}                     # 加强箍：比桶身亮，凸起才看得出来

    # 都用同一套"顶部偏上最亮、向下渐暗"的圆柱规律，只是整体明度分三档
    cap_grad = ['2f2f2f', '3a3a3a', '454545', '5e5e5e', '454545', '3a3a3a', '2f2f2f',
                '262626', '1a1a1a']
    hoop_grad = ['8b8b8b', 'a7a7a7', 'c9c9c9', 'e0e0e0', 'c9c9c9', 'a7a7a7', '8b8b8b',
                 '7c7c7c', '6a6a6a']

    layers = []
    for cx, h in enumerate(profile):
        if cx in hoops:
            grad = hoop_grad
        elif cx in cap_cols:
            grad = cap_grad
        else:
            grad = STEEL
        layers.append((cx, h, ramp(grad, 2 * h + 1)))
    return render(layers)


# -------------------------------------------------------------- 航空机枪弹
def fighter_ammo():
    """竖放的黄铜子弹：构图照炮弹来（弹头朝上、圆柱横向渐变），
    但整体矮一截、细一档，和炮弹摆一起明显小一圈。"""
    half = 3
    # 行半宽，自上而下：弹尖锥 -> 弹头 -> 弹壳口 -> 弹壳 -> 底缘 -> 底面
    profile = [0, 1, 2] + [half] * 3 + [half] * 12 + [half, half - 1]
    seam_row, case_end, rim_row = 6, 18, 19

    NOSE = ['5e4416', '7a5a1c', 'a27828', 'c99a20', 'e0aa29', 'ffdd64', 'e0aa29',
            'c99a20', 'a27828', '7a5a1c', '5e4416']          # 弹头：铜被甲，比弹壳暗一档
    CASE = ['7a5a1c', 'a27828', 'c99a20', 'e0aa29', 'ffdd64', 'ffe88a', 'ffdd64',
            'e0aa29', 'c99a20', 'a27828', '7a5a1c']          # 弹壳：黄铜，最亮
    RIM = ['a27828', 'c99a20', 'e0aa29', 'ffdd64', 'ffe88a', 'ffe88a', 'ffdd64',
           'e0aa29', 'c99a20', 'a27828', '7a5a1c']           # 底缘：凸起，回亮
    BASE = ['3a2a0e', '5e4416', '7a5a1c', '8b661c', '7a5a1c', '5e4416', '3a2a0e']

    px = {}
    for cy, h in enumerate(profile):
        if cy < seam_row:
            colors = ramp(NOSE, 2 * h + 1)
        elif cy == seam_row:
            colors = [hx('8b661c')] * (2 * h + 1)            # 弹壳口的一圈阴影
        elif cy <= case_end:
            colors = ramp(CASE, 2 * h + 1)
        elif cy == rim_row:
            colors = ramp(RIM, 2 * h + 1)
        else:
            colors = ramp(BASE, 2 * h + 1)
        put_row(px, cy, h, colors)
    return finish(px)


# ------------------------------------------------------------ 飞行中的弹丸
def projectile_bullet():
    """机枪弹丸飞出去时渲染用的贴图（PROJECTILE_BULLET 隐藏物品）。
    保持 16x16 的实心方块、不描边：飞行时只看得见一个金点，加形状反而会糊。
    取色与 fighter_ammo 同族的金色。"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    gold = hx('ffdd64')
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), gold)
    return img


TEXTURES = {
    'aerial_bomb': aerial_bomb,
    'depth_charge': depth_charge,
    'fighter_ammo': fighter_ammo,
    'projectile_bullet': projectile_bullet,
}

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src/main/resources/assets/piranport/textures/item")


def main():
    imgs = {}
    for name, fn in TEXTURES.items():
        imgs[name] = fn()
        imgs[name].save(f"{OUT}/{name}.png")
        print(f"wrote {OUT}/{name}.png")

    # 放大预览 + 背包 16x16 实际观感
    scale = 9
    names = list(TEXTURES)
    canvas = Image.new("RGB", (len(names) * W * scale * 2 + 48, H * scale + 16), (30, 30, 32))
    for i, name in enumerate(names):
        img = imgs[name]
        x = 8 + i * (W * scale * 2 + 16)
        big = img.resize((W * scale, H * scale), Image.NEAREST)
        canvas.paste(big, (x, 8), big)
        inv = img.resize((16, 16), Image.LANCZOS).resize((W * scale, H * scale), Image.NEAREST)
        canvas.paste(inv, (x + W * scale, 8), inv)
    canvas.save("/tmp/ammo_preview.png")
    print("wrote /tmp/ammo_preview.png（每个物件：左原图放大，右背包 16x16 观感）")


if __name__ == "__main__":
    main()
