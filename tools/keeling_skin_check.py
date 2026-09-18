#!/usr/bin/env python3
"""基林改皮肤（skin_22）校验图：把 64×64 皮肤按真实 UV 展开成
「正面 / 背面」两张平面拼图，便于逐像素比对。

与 keeling_skin_tools.py 的 3D 预览互补：3D 预览看整体观感，
本图看**每个 texel 的落点**——排查"某块颜色跑到对面去了"这类
UV 方向错误时用这张图，不要靠 3D 预览猜。

运行：cd tools && python3 keeling_skin_check.py
输出：build/offline-renders/keeling_skin_check.png
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SKIN = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_22.png"
OUT = ROOT / "build/offline-renders/keeling_skin_check.png"

# 面片矩形 (x, y, w, h) in 64 space
F = {
    "head_top": (8, 0, 8, 8), "head_bottom": (16, 0, 8, 8),
    "head_left": (0, 8, 8, 8), "head_front": (8, 8, 8, 8),
    "head_right": (16, 8, 8, 8), "head_back": (24, 8, 8, 8),
    "head_top_ov": (40, 0, 8, 8), "head_bottom_ov": (48, 0, 8, 8),
    "head_left_ov": (32, 8, 8, 8), "head_front_ov": (40, 8, 8, 8),
    "head_right_ov": (48, 8, 8, 8), "head_back_ov": (56, 8, 8, 8),
    "body_top": (20, 16, 8, 4), "body_bottom": (28, 16, 8, 4),
    "body_left": (16, 20, 4, 12), "body_front": (20, 20, 8, 12),
    "body_right": (28, 20, 4, 12), "body_back": (32, 20, 8, 12),
    "body_top_ov": (20, 32, 8, 4), "body_bottom_ov": (28, 32, 8, 4),
    "body_left_ov": (16, 36, 4, 12), "body_front_ov": (20, 36, 8, 12),
    "body_right_ov": (28, 36, 4, 12), "body_back_ov": (32, 36, 8, 12),
    "rarm_top": (44, 16, 4, 4), "rarm_bottom": (48, 16, 4, 4),
    "rarm_left": (40, 20, 4, 12), "rarm_front": (44, 20, 4, 12),
    "rarm_right": (48, 20, 4, 12), "rarm_back": (52, 20, 4, 12),
    "rarm_top_ov": (44, 32, 4, 4), "rarm_bottom_ov": (48, 32, 4, 4),
    "rarm_left_ov": (40, 36, 4, 12), "rarm_front_ov": (44, 36, 4, 12),
    "rarm_right_ov": (48, 36, 4, 12), "rarm_back_ov": (52, 36, 4, 12),
    "larm_top": (36, 48, 4, 4), "larm_bottom": (40, 48, 4, 4),
    "larm_left": (32, 52, 4, 12), "larm_front": (36, 52, 4, 12),
    "larm_right": (40, 52, 4, 12), "larm_back": (44, 52, 4, 12),
    "larm_top_ov": (52, 48, 4, 4), "larm_bottom_ov": (56, 48, 4, 4),
    "larm_left_ov": (48, 52, 4, 12), "larm_front_ov": (52, 52, 4, 12),
    "larm_right_ov": (56, 52, 4, 12), "larm_back_ov": (60, 52, 4, 12),
    "rleg_top": (4, 16, 4, 4), "rleg_bottom": (8, 16, 4, 4),
    "rleg_left": (0, 20, 4, 12), "rleg_front": (4, 20, 4, 12),
    "rleg_right": (8, 20, 4, 12), "rleg_back": (12, 20, 4, 12),
    "rleg_top_ov": (4, 32, 4, 4), "rleg_bottom_ov": (8, 32, 4, 4),
    "rleg_left_ov": (0, 36, 4, 12), "rleg_front_ov": (4, 36, 4, 12),
    "rleg_right_ov": (8, 36, 4, 12), "rleg_back_ov": (12, 36, 4, 12),
    "lleg_top": (20, 48, 4, 4), "lleg_bottom": (24, 48, 4, 4),
    "lleg_left": (16, 52, 4, 12), "lleg_front": (20, 52, 4, 12),
    "lleg_right": (24, 52, 4, 12), "lleg_back": (28, 52, 4, 12),
    "lleg_top_ov": (4, 48, 4, 4), "lleg_bottom_ov": (8, 48, 4, 4),
    "lleg_left_ov": (0, 52, 4, 12), "lleg_front_ov": (4, 52, 4, 12),
    "lleg_right_ov": (8, 52, 4, 12), "lleg_back_ov": (12, 52, 4, 12),
}

S = 14          # 每个 texel 的像素尺寸


def tile(skin, face, flip=False):
    x, y, w, h = F[face]
    t = skin.crop((x, y, x + w, y + h))
    if flip:
        t = t.transpose(Image.FLIP_LEFT_RIGHT)
    return t.resize((w * S, h * S), Image.NEAREST)


def compose(skin, parts, cols):
    """parts: list of (face, flip) 或 ('gap', w) —— 按网格拼成一张图。"""
    rows = []
    row = []
    for item in parts:
        if item[0] == "gap":
            row.append(Image.new("RGBA", (item[1] * S, 1), (0, 0, 0, 0)))
        else:
            row.append(tile(skin, item[0], item[1] if len(item) > 1 else False))
        if len(row) == cols:
            rows.append(row)
            row = []
    if row:
        rows.append(row)
    W = max(sum(im.width for im in r) for r in rows) + 40
    H = sum(max(im.height for im in r) for r in rows) + 40
    out = Image.new("RGBA", (W, H), (238, 240, 244, 255))
    oy = 20
    for r in rows:
        ox = 20
        for im in r:
            out.alpha_composite(im, (ox, oy))
            ox += im.width
        oy += max(im.height for im in r)
    return out


def main() -> None:
    skin = Image.open(SKIN).convert("RGBA")
    # 正面：左臂 | 左腿 | 躯干正面 | 右腿 | 头正面 | 右臂
    front = compose(skin, [
        ("larm_front",), ("lleg_front",), ("body_front",), ("rleg_front",),
        ("head_front",), ("rarm_front",),
    ], 6)
    # 背面
    back = compose(skin, [
        ("larm_back",), ("lleg_back",), ("body_back",), ("rleg_back",),
        ("head_back",), ("rarm_back",),
    ], 6)

    canvas = Image.new("RGBA", (front.width, front.height + back.height + 60), (238, 240, 244, 255))
    d = ImageDraw.Draw(canvas)
    d.text((20, 6), "FRONT  (larm | lleg | body | rleg | head | rarm)  — 每个 texel 已放大", fill=(20, 30, 40, 255))
    canvas.alpha_composite(front, (0, 20))
    d.text((20, front.height + 34), "BACK", fill=(20, 30, 40, 255))
    canvas.alpha_composite(back, (0, front.height + 48))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(OUT)
    print(OUT, canvas.size)


if __name__ == "__main__":
    main()
