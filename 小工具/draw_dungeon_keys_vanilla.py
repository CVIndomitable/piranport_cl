#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 Blockbench 原版物品贴图规范重画副本钥匙（16x16）。

规范：
- 先用中间调画形状，再加明显更深的描边；
- 光源统一来自左上角，描边也按光源分「左上略亮 / 右下更暗」；
- 每张底图 5-7 色有限色板：描边、暗部、中间调、亮部、金属高光（+1 反光/划痕）；
- 禁止抗锯齿 / 噪点 / 自动渐变，每个像素都是手绘色块；
- 数字层改成小金属挂牌，背景保持全透明。

输出：
  dungeon_key.png + dungeon_key_1..7.png   8 张底图
  dungeon_key_num_1..5.png                 5 张数字挂牌叠层
  小工具/_preview/preview_dungeon_keys_vanilla.png  10 倍拼图预览
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/piranport/textures/item"
PREVIEW_DIR = ROOT / "小工具/_preview"
PREVIEW = PREVIEW_DIR / "preview_dungeon_keys_vanilla.png"

# ---------------------------------------------------------------------------
# 底图：手绘角色网格（光源左上）
#   a 右下最暗描边   b 左上略亮描边   s 暗部   m 中间调   l 亮部
#   h 金属高光       r 反光/划痕      . 透明
# 构图与原版一致：圆环在上左，杆向右下，齿在右下；数字挂牌区（右上）留空。
# ---------------------------------------------------------------------------
KEY_GRID = [
    "................",  # 0
    "................",  # 1
    "................",  # 2
    "....bbb.........",  # 3  环顶描边
    "...bhhls........",  # 4  环顶高光，右侧转入背光
    "..bh...ma.......",  # 5  左臂高光 / 右臂中间调
    "..bh...ma.......",  # 6
    "..bl...la.......",  # 7  孔内右下沿受光
    "...saaaaa.......",  # 8  环底描边（左下略亮）
    "....ssmmhl......",  # 9  环颈（环下暗部）接杆顶受光
    "........bha.bla.",  # 10 杆左上高光 / 齿顶受光（中间是齿口）
    ".........bhmmra.",  # 11 齿块主体（右下一点磨痕）
    "..........bhrma.",  # 12 杆身细划痕
    "...........bla..",  # 13 右侧咬口，分出上下两齿
    "............bma.",  # 14
    ".............bma",  # 15 齿尖
]

# 每张底图 7 色：a/b 描边、s 暗部、m 中间调、l 亮部、h 高光、r 反光划痕
PALETTES = {
    # 通用钥匙：亮金，比第三章的黄铜更黄更亮
    "dungeon_key": {
        "a": "#3a2606", "b": "#7d5514", "s": "#a87c1e", "m": "#e3b53c",
        "l": "#f7d862", "h": "#fff4b0", "r": "#efe0b0",
    },
    # 第一章：橙 / 赤铜
    "dungeon_key_1": {
        "a": "#3a1b09", "b": "#7a3a16", "s": "#a85a26", "m": "#d9813f",
        "l": "#f0a566", "h": "#ffd2a8", "r": "#f5c49a",
    },
    # 第二章：银白
    "dungeon_key_2": {
        "a": "#2a2f37", "b": "#58626e", "s": "#8792a1", "m": "#b4bec9",
        "l": "#d8e0e8", "h": "#ffffff", "r": "#cfe2f5",
    },
    # 第三章：黄铜 / 暖金，比通用钥匙更暗更橙
    "dungeon_key_3": {
        "a": "#33230a", "b": "#6b4a12", "s": "#96681c", "m": "#c9912e",
        "l": "#e2b04a", "h": "#f8d88a", "r": "#e8c070",
    },
    # 第四章：青
    "dungeon_key_4": {
        "a": "#0a3136", "b": "#12606b", "s": "#1e8c98", "m": "#43c4c9",
        "l": "#7ce6e3", "h": "#d2fffd", "r": "#aaf0f2",
    },
    # 第五章：绿
    "dungeon_key_5": {
        "a": "#0a3016", "b": "#136a31", "s": "#1e9847", "m": "#37ca66",
        "l": "#73e795", "h": "#c8ffd6", "r": "#a4f2bc",
    },
    # 第六章：暗紫
    "dungeon_key_6": {
        "a": "#1a141c", "b": "#3a2d3d", "s": "#56455b", "m": "#71607a",
        "l": "#90809a", "h": "#bcadc6", "r": "#a894b0",
    },
    # 第七章：亮紫
    "dungeon_key_7": {
        "a": "#290d38", "b": "#521b73", "s": "#7a2ea6", "m": "#a75fde",
        "l": "#c98ef2", "h": "#f0daff", "r": "#d9b0ff",
    },
}

# ---------------------------------------------------------------------------
# 数字层：小金属挂牌（6x7，固定位置，不压住圆环主轮廓）
#   a 下右最暗描边   b 上左略亮描边   s 暗部   m 中间调   l 亮部
#   h 上左倒角高光   d 压印数字（深色内凹）
# ---------------------------------------------------------------------------
PLATE_X, PLATE_Y = 10, 0
PLATE_W, PLATE_H = 6, 7
DIGIT_X, DIGIT_Y = 12, 1  # 3x5 数字在挂牌内的位置

DIGIT_FONT = {
    1: [".#.",
        "##.",
        ".#.",
        ".#.",
        "###"],
    2: ["###",
        "..#",
        "###",
        "#..",
        "###"],
    3: ["###",
        "..#",
        "###",
        "..#",
        "###"],
    4: ["#.#",
        "#.#",
        "###",
        "..#",
        "..#"],
    5: ["###",
        "#..",
        "###",
        "..#",
        "###"],
}

PLATE_PALETTE = {
    "a": "#171a20",  # 右下描边（最暗）
    "b": "#4b5261",  # 左上描边（略亮）
    "s": "#626b7a",  # 暗部
    "m": "#7d8696",  # 中间调
    "l": "#9ba5b5",  # 亮部
    "h": "#c6cfdd",  # 左上倒角高光
    "d": "#262b34",  # 压印数字（深色内凹）
}


def _rgb(hex_color: str):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def render_key(palette):
    """把角色网格转成 16x16 RGBA。"""
    assert len(KEY_GRID) == 16, "底图必须是 16 行"
    px = {}
    for y, row in enumerate(KEY_GRID):
        assert len(row) == 16, f"第 {y} 行不是 16 列: {row!r}"
        for x, role in enumerate(row):
            if role == ".":
                continue
            px[(x, y)] = _rgb(palette[role])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        img.putpixel((x, y), c)
    return img


def render_num(digit: int):
    """画 6x7 金属挂牌 + 3x5 压印数字，其余全透明。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    put = img.putpixel
    x0, y0 = PLATE_X, PLATE_Y
    x1, y1 = x0 + PLATE_W - 1, y0 + PLATE_H - 1  # 右下角坐标

    # 上描边：受光面，由左往右由亮转暗（左上描边略亮）
    for i, role in enumerate(["b", "h", "h", "l", "m", "a"]):
        put((x0 + i, y0), _rgb(PLATE_PALETTE[role]))
    # 左 / 右描边：左上略亮，右下最暗
    for y in range(y0 + 1, y1):
        put((x0, y), _rgb(PLATE_PALETTE["b"]))
        put((x1, y), _rgb(PLATE_PALETTE["a"]))
    # 下描边：最暗
    for x in range(x0, x1 + 1):
        put((x, y1), _rgb(PLATE_PALETTE["a"]))

    # 左侧一条倒角高光，自上而下由亮转暗
    for i, role in enumerate(["h", "l", "l", "m", "m"]):
        put((x0 + 1, y0 + 1 + i), _rgb(PLATE_PALETTE[role]))

    # 数字：深色压印；字形负空间按左上亮、右下暗铺台面
    glyph = DIGIT_FONT[digit]
    for r in range(5):
        for c in range(3):
            x, y = DIGIT_X + c, DIGIT_Y + r
            if glyph[r][c] == "#":
                put((x, y), _rgb(PLATE_PALETTE["d"]))
            else:
                v = r + c
                role = "l" if v <= 2 else ("m" if v <= 4 else "s")
                put((x, y), _rgb(PLATE_PALETTE[role]))
    return img


def all_pixels(img):
    px = img.load()
    return [px[x, y] for y in range(16) for x in range(16)]


def validate(img, label, expect_bg_transparent=True):
    """自检：16x16 RGBA、无半透明、透明区干净。"""
    assert img.size == (16, 16), f"{label}: 尺寸 {img.size}"
    assert img.mode == "RGBA", f"{label}: 模式 {img.mode}"
    alphas = {c[3] for c in all_pixels(img)}
    assert alphas <= {0, 255}, f"{label}: 出现半透明抗锯齿像素 {alphas}"
    if expect_bg_transparent:
        assert img.getpixel((0, 0))[3] == 0, f"{label}: 左上角不是透明"
        assert img.getpixel((0, 15))[3] == 0, f"{label}: 左下角不是透明"


def palette_of(img):
    return sorted({c for c in all_pixels(img) if c[3] == 255})


def make_preview(keys, nums, composites):
    scale = 10
    cell = 16 * scale
    gap, label_h = 10, 14
    cols = max(len(keys), len(nums), len(composites))
    w = cols * (cell + gap) + gap
    h = 3 * (cell + gap + label_h) + gap
    canvas = Image.new("RGBA", (w, h), (30, 30, 34, 255))
    draw = ImageDraw.Draw(canvas)

    def checker(px):
        tile = 8
        for yy in range(0, cell, tile):
            for xx in range(0, cell, tile):
                shade = 58 if ((xx // tile) + (yy // tile)) % 2 == 0 else 44
                px.rectangle([xx, yy, xx + tile - 1, yy + tile - 1], fill=(shade, shade, shade, 255))

    # 标题用 ASCII：PIL 默认字体不含中文字形
    rows = [
        ([("key", n, im) for n, im in keys], "base keys (8)"),
        ([("num", n, im) for n, im in nums], "number plates (5)"),
        ([("cmp", n, im) for n, im in composites], "composite: chapter key = base + number"),
    ]
    for ri, (items, title) in enumerate(rows):
        base_y = gap + ri * (cell + gap + label_h) + label_h
        draw.text((gap, base_y - 12), title, fill=(220, 220, 226, 255))
        for ci, (kind, label, im) in enumerate(items):
            cx = gap + ci * (cell + gap)
            bg = Image.new("RGBA", (cell, cell), (44, 44, 44, 255))
            checker(ImageDraw.Draw(bg))
            big = im.resize((cell, cell), Image.NEAREST)
            bg.alpha_composite(big)
            canvas.paste(bg, (cx, base_y))
            draw.text((cx, base_y + cell + 1), label, fill=(200, 205, 215, 255))
    return canvas


def main():
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    keys = []
    for name, pal in PALETTES.items():
        img = render_key(pal)
        validate(img, name)
        cols = palette_of(img)
        assert 5 <= len(cols) <= 7, f"{name}: 色板 {len(cols)} 色（要求 5-7）"
        img.save(TEX / f"{name}.png")
        keys.append((name.replace("dungeon_key", "key"), img))
        print(f"{name}.png  色板 {len(cols)}: " + " ".join("#%02x%02x%02x" % c[:3] for c in cols))

    nums = []
    for digit in range(1, 6):
        name = f"dungeon_key_num_{digit}"
        img = render_num(digit)
        validate(img, name)
        # 背景必须全透明：挂牌外不允许有任何像素
        for y in range(16):
            for x in range(16):
                inside = PLATE_X <= x < PLATE_X + PLATE_W and PLATE_Y <= y < PLATE_Y + PLATE_H
                if not inside:
                    assert img.getpixel((x, y))[3] == 0, f"{name}: ({x},{y}) 挂牌外不透明"
        img.save(TEX / f"{name}.png")
        nums.append((str(digit), img))
        print(f"{name}.png  色板 {len(palette_of(img))}")

    # 组合预览：第 i 章配数字 ((i-1) % 5) + 1，与原组合图一致
    cards = []
    for i in range(1, 8):
        base = render_key(PALETTES[f"dungeon_key_{i}"])
        num = render_num(((i - 1) % 5) + 1)
        cards.append((f"key_{i}+{((i - 1) % 5) + 1}", Image.alpha_composite(base, num)))

    preview = make_preview(keys, nums, cards)
    preview.save(PREVIEW)
    print(f"\n预览: {PREVIEW}")


if __name__ == "__main__":
    main()
