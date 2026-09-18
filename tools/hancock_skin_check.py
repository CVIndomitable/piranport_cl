#!/usr/bin/env python3
"""汉考克皮肤（skin_20）校验图：把 64×64 皮肤按真实 UV 展开成
「正面 / 背面」两张平面拼图，便于逐像素比对。

与 hancock_skin_tools.py 的 3D 预览互补：3D 预览看整体观感，
本图看**每个 texel 的落点**——排查"某块颜色跑到对面去了"这类
UV 方向错误时用这张图，不要靠 3D 预览猜。

运行：cd tools && python3 hancock_skin_check.py
输出：build/offline-renders/hancock_skin_check.png
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SKIN = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_20.png"
OUT = ROOT / "build/offline-renders/hancock_skin_check.png"

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


def assemble(skin, order, checker=False):
    """把若干面片排成一行（正面那几个面按「左臂-左腿-躯干-右腿-右臂」摆放）。

    checker=True 时给每个面片垫棋盘底色 —— overlay 层大面积透明，
    不垫底就分不清「透明」和「画了深色」。
    """
    tiles = [tile(skin, f) for f in order]
    if checker:
        tiles = [checker_tile(t) for t in tiles]
    W = sum(t.width for t in tiles) + 8 * (len(tiles) + 1)
    H = max(t.height for t in tiles) + 34
    canvas = Image.new("RGBA", (W, H), (34, 34, 44, 255))
    d = ImageDraw.Draw(canvas)
    x = 8
    for f, t in zip(order, tiles):
        canvas.alpha_composite(t, (x, 8))
        d.text((x, 8 + t.height + 4), f, fill=(215, 215, 225, 255))
        x += t.width + 8
    return canvas


# overlay 层要用棋盘垫底才看得清——overlay 大面积透明，
# 直接叠在纯色背景上会分不清「透明」和「画了深色」
def checker_tile(t, size=8):
    bg = Image.new("RGBA", t.size, (86, 86, 100, 255))
    d = ImageDraw.Draw(bg)
    for yy in range(0, t.height, size):
        for xx in range(0, t.width, size):
            if (xx // size + yy // size) % 2:
                d.rectangle([xx, yy, xx + size - 1, yy + size - 1],
                            fill=(112, 112, 128, 255))
    bg.alpha_composite(t)
    return bg


def build():
    skin = Image.open(SKIN).convert("RGBA")
    if skin.size != (64, 64):
        raise SystemExit(f"需要 64×64 皮肤，实际 {skin.size}")

    # 正面：从左到右是 左臂 / 左腿 / 头 / 躯干 / 右腿 / 右臂
    front = assemble(skin, [
        "larm_front", "lleg_front", "head_front", "body_front",
        "rleg_front", "rarm_front",
    ])
    # 背面同理（注意背面面片的左右与正面相反）
    back = assemble(skin, [
        "rarm_back", "rleg_back", "head_back", "body_back",
        "lleg_back", "larm_back",
    ])
    # 侧面：头左右 + 躯干左右 + 腿左右
    side = assemble(skin, [
        "head_left", "head_right", "body_left", "body_right",
        "rleg_left", "rleg_right", "lleg_left", "lleg_right",
    ])
    # overlay 层（第二层）：确认透明度与贴脸情况。用棋盘垫底以便分辨透明像素。
    ov = assemble(skin, [
        "head_front_ov", "head_top_ov", "head_back_ov",
        "body_front_ov", "body_back_ov", "body_left_ov", "body_right_ov",
        "rleg_front_ov", "lleg_front_ov", "rarm_front_ov", "larm_front_ov",
    ], checker=True)

    W = max(front.width, back.width, side.width, ov.width)
    H = front.height + back.height + side.height + ov.height + 6
    out = Image.new("RGBA", (W, H), (26, 26, 34, 255))
    y = 0
    d = ImageDraw.Draw(out)
    for name, im in (("正面", front), ("背面", back), ("侧面", side), ("overlay(棋盘垫底)", ov)):
        d.text((4, y + 2), name, fill=(255, 220, 140, 255))
        out.alpha_composite(im, (0, y))
        y += im.height
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT)
    print(f"已生成 {OUT}")


if __name__ == "__main__":
    build()
