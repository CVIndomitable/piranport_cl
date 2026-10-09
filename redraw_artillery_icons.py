#!/usr/bin/env python3
"""批量重画火炮物品贴图（32×32）。

管线（依据记忆 draw-weapons-and-planes-from-original-art，画风参考
`美术素材/Item（图标）/火炮/`）：

    原画(512×512) --裁 alpha bbox--> 等比面积平均降采样(长边 LONG) --alpha 阈值-->
    居中放到 32×32 --> 自适应量化 --> 外轮廓补一圈黑描边 --> 落地

为什么不是旧做法：旧脚本把整张 512×512 直接缩到 32×32，炮身只占画面一小块；
又用 RGB 跳变阈值去描「内部结构线」，把炮身整块涂黑，结果是一坨黑块。
正确做法是先裁掉透明边再等比缩小（炮身铺满画布），描边只加**最外侧一圈**，
炮身内部保留原画的明暗层次。

两态：装填态=本体（无图标）；空膛态=本体 + 底部中央标准警告三角（9×7，
x12-20 / y25-31，从 `美术素材/Item（图标）/火炮/french_quad_380mm_gun_empty.png`
原样抠取，全项目同一枚）。除该三角外两态逐像素一致。

用法：python3 redraw_artillery_icons.py [--write]
不带 --write 时只出预览，不覆盖模组贴图。
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

CACHE = Path("/Users/lianran/IndomitableCache/zjsnrwiki原画")
REPO = Path(__file__).resolve().parent
TEX = REPO / "src/main/resources/assets/piranport/textures/item"
ART_REF = Path("/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/火炮")

CANVAS = 32
LONG = 30          # 降采样后内容长边；留 1px 给外圈描边，合计正好 32
ALPHA_THR = 110    # 稍低于 128，保住细炮管
N_COLORS = 48      # 参考图实测约 48 色
SHARPEN = 60       # unsharp 强度%：找回降采样糊掉的面板线/双炮管缝隙（再高描边会发粗）

# 标准空膛警告三角：french_quad_380mm_gun_empty.png 的 (12,25)-(21,32) 区域
MARKER_SRC = ART_REF / "french_quad_380mm_gun_empty.png"
MARKER_BOX = (12, 25, 21, 32)   # 9 宽 × 7 高

# (原画编号, 物品 id, 中文名)
GUNS = [
    ("Equip_L_455", "bofors_twin_120mm_gun",    "博福斯双联120毫米炮"),
    ("Equip_L_11",  "us_single_5inch_gun",      "美国单装5英寸炮"),
    ("Equip_L_41",  "us_twin_5inch_gun",        "美国双联5英寸炮"),
    ("Equip_L_10",  "us_twin_5inch_dp_gun",     "美国双联5英寸平高两用炮"),
    ("Equip_L_456", "twin_5inch_l54_dp_gun",    "5英寸L54双联高平两用炮"),
    ("Equip_L_14",  "japanese_single_140mm_gun","日本14厘米单装炮"),
    ("Equip_L_172", "british_single_6inch_gun", "英国单装6英寸炮"),
]


def load_marker() -> np.ndarray:
    """抠出标准警告三角（含黑描边）的 RGBA 数组。"""
    im = Image.open(MARKER_SRC).convert("RGBA")
    return np.array(im.crop(MARKER_BOX))


def body_from_art(path: Path) -> np.ndarray:
    """原画 → 32×32 本体（含外圈黑描边），返回 RGBA 数组。"""
    src = Image.open(path).convert("RGBA")
    bbox = src.split()[3].getbbox()
    if bbox is None:
        raise ValueError(f"{path.name} 全透明，没有内容")
    crop = src.crop(bbox)

    # 等比缩到长边 LONG；BOX = 面积平均，等价于按覆盖面积取均值
    scale = LONG / max(crop.size)
    w = max(1, round(crop.width * scale))
    h = max(1, round(crop.height * scale))

    # 原画背景是「黑色 + alpha=0」，直接平均会把边缘颜色往黑里带（发灰发脏）。
    # 先预乘 alpha 再缩，缩完除回来，边缘就能保留原色。
    src_arr = np.array(crop, dtype=np.float32)
    src_arr[..., :3] *= src_arr[..., 3:4] / 255.0
    small = np.array(
        Image.fromarray(src_arr.astype(np.uint8), "RGBA").resize((w, h), Image.Resampling.BOX),
        dtype=np.float32,
    )
    alpha = small[..., 3:4]
    with np.errstate(divide="ignore", invalid="ignore"):
        small[..., :3] = np.where(alpha > 0, small[..., :3] / (alpha / 255.0), 0.0)
    np.clip(small[..., :3], 0, 255, out=small[..., :3])

    # 1:15 的降采样会把面板线、双炮管缝隙这类高频细节糊掉；轻度 unsharp 找回来，
    # 否则结果比参考图标（美术素材/Item（图标）/火炮/）明显发糊。
    sharp = Image.fromarray(small[..., :3].astype(np.uint8), "RGB").filter(
        ImageFilter.UnsharpMask(radius=1, percent=SHARPEN, threshold=0)
    )
    small[..., :3] = np.clip(np.array(sharp, dtype=np.float32), 0, 255)

    # 抗锯齿边缘二值化，否则描边轮廓会漏
    small[..., 3] = np.where(small[..., 3] >= ALPHA_THR, 255.0, 0.0)

    # 只对不透明像素做自适应量化（黑色描边留给下一步，避免被量化带偏）
    op = small[..., 3] > 0
    if op.any():
        rgb = small[..., :3].astype(np.uint8)
        q = Image.fromarray(rgb).quantize(colors=N_COLORS, method=Image.Quantize.MEDIANCUT)
        small[..., :3] = np.array(q.convert("RGB"), dtype=np.float32)

    # 居中贴到画布
    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float32)
    ox, oy = (CANVAS - w) // 2, (CANVAS - h) // 2
    canvas[oy:oy + h, ox:ox + w] = small

    # 外圈描边：透明像素只要有 8 邻域不透明就涂黑（避免斜向漏线）
    opaque = canvas[..., 3] > 0
    near = np.zeros_like(opaque)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dx == 0 and dy == 0:
                continue
            near |= np.roll(np.roll(opaque, dy, 0), dx, 1)
    canvas[near & ~opaque] = (0.0, 0.0, 0.0, 255.0)

    return canvas.astype(np.uint8)


def add_marker(body: np.ndarray, marker: np.ndarray) -> np.ndarray:
    """本体 + 底部中央警告三角 = 空膛态。"""
    out = body.copy()
    x1, y1 = MARKER_BOX[0], MARKER_BOX[1]
    h, w = marker.shape[:2]
    region = out[y1:y1 + h, x1:x1 + w]
    # 只在三角自身不透明处覆盖，保留三角周围的炮身像素
    m = marker[..., 3] > 0
    region[m] = marker[m]
    return out


def ascii_preview(arr: np.ndarray) -> str:
    lines = []
    for y in range(arr.shape[0]):
        row = []
        for x in range(arr.shape[1]):
            r, g, b, a = arr[y, x]
            if a < 128:
                row.append(".")
            elif r < 40 and g < 40 and b < 40:
                row.append("#")
            else:
                row.append("0123456789abcdef"[(int(r) + int(g) + int(b)) // 3 // 16])
        lines.append("".join(row))
    return "\n".join(lines)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--write", action="store_true", help="写入模组 textures/item/")
    args = ap.parse_args()

    marker = load_marker()

    tiles = []
    for ref, item_id, cn in GUNS:
        art = CACHE / f"{ref}.png"
        if not art.exists():
            print(f"✗ 缺原画: {art}")
            return 1

        body = body_from_art(art)
        empty = add_marker(body, marker)

        # 自检：两态差异必须只落在三角区域内
        diff = np.argwhere(np.any(body != empty, axis=2))
        x1, y1, x2, y2 = MARKER_BOX
        stray = [tuple(p) for p in diff if not (y1 <= p[0] < y2 and x1 <= p[1] < x2)]
        op = int((body[..., 3] > 0).sum())
        black = int(((body[..., 3] > 0) & (body[..., :3].max(axis=2) < 40)).sum())
        print(f"{cn:14s} {item_id:26s} 不透明{op:4d}px 黑{black:4d}px({100*black/op:4.1f}%) 两态差异{len(diff)}px 越界{len(stray)}")

        Image.fromarray(body, "RGBA").save(REPO / f"preview_art_{item_id}.png")
        Image.fromarray(empty, "RGBA").save(REPO / f"preview_art_{item_id}_empty.png")
        tiles.append((item_id, body, empty))

        if args.write:
            Image.fromarray(body, "RGBA").save(TEX / f"{item_id}.png")
            Image.fromarray(empty, "RGBA").save(TEX / f"{item_id}_empty.png")

    # 汇总对比图：上排全部装填态，下排全部空膛态
    S = 8
    n = len(tiles)
    row_h = CANVAS * S + 18
    sheet = Image.new("RGBA", (CANVAS * S * n, row_h * 2), (255, 255, 255, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(sheet)
    for i, (name, body, empty) in enumerate(tiles):
        cx = i * CANVAS * S
        for j, arr in enumerate((body, empty)):
            yy = j * row_h + 16
            im = Image.fromarray(arr, "RGBA").resize((CANVAS * S, CANVAS * S), Image.NEAREST)
            sheet.paste(im, (cx, yy), im)
        d.text((cx + 4, 2), f"{i+1}.{name}", fill=(0, 0, 0, 255))
    sheet.save(REPO / "preview_art_batch.png")

    print("\n预览: preview_art_batch.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
