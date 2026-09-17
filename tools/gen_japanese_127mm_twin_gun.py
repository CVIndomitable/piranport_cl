#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
日本12.7厘米连装炮 —— 贴图 + 方块模型(3D item model) 生成器。

为什么用脚本而不是手写 JSON：
  - 3D 物品模型需要 元素坐标 与 UV 严格对应，手写极易错位；
  - 贴图是程序化绘制的，UV 分区必须和贴图分区同步，脚本保证二者永不脱节。

模型坐标系约定（1 单位 = 1/16 方块）：
  - 炮口朝 **-X**（这是 MC 手持物品的"前方"轴，见 FILE 末尾说明）
  - +Y 为炮塔上方
  - Z 为炮塔左右宽度方向

输出：
  src/main/resources/assets/piranport/textures/item/japanese_127mm_twin_gun.png
  src/main/resources/assets/piranport/models/item/japanese_127mm_twin_gun.json

用法：python3 tools/gen_japanese_127mm_twin_gun.py
"""

import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_PATH = os.path.join(
    ROOT, "src/main/resources/assets/piranport/textures/item/japanese_127mm_twin_gun.png"
)
MODEL_PATH = os.path.join(
    ROOT, "src/main/resources/assets/piranport/models/item/japanese_127mm_twin_gun.json"
)

TEX_SIZE = 64          # 贴图边长（像素）
SS = 4                 # 超采样倍率：先按 4 倍画再缩回，得到平滑边缘（YSM 那种柔和画风）
UNIT = 16.0 / TEX_SIZE  # 像素 -> 模型 UV 单位（0..16）


# --------------------------------------------------------------------------
# 贴图分区：每个材质占一块矩形（单位：64x64 像素坐标）
# --------------------------------------------------------------------------
REGIONS = {
    # 炮塔侧板：米白色船体漆，带细微竖向渐变 + 板缝铆钉
    "hull":       (0, 0, 28, 28),
    # 炮塔顶板：更亮，中央有一个圆形舱盖
    "hull_top":   (32, 0, 60, 28),
    # 炮室正面：白色面板，中间开矩形炮管口，口里两个黑洞
    "shield":      (0, 32, 28, 60),
    # 炮管：中灰色圆柱感（中间高光、两侧压暗）
    "barrel":     (32, 32, 44, 44),
    # 炮口：深色圆环
    "muzzle":     (44, 32, 56, 44),
    # 底座/摇架等金属件：深灰，带轻微噪点
    "metal":      (32, 48, 60, 60),
    # 扶手栏杆：浅灰细杆
    "rail":       (0, 60, 28, 64),
}

# --------------------------------------------------------------------------
# 模型元素：(名字, 材质, from, to, 额外面覆盖)
# 额外面覆盖用 ("muzzle",) 表示该元素的西面(-X)单独用炮口贴图
# --------------------------------------------------------------------------
# 相邻元素故意留 0.1 的重叠：完全贴合会在共面处产生 z-fighting（闪烁）。
ELEMENTS = [
    # 底座转台 —— 深灰金属，托住整个炮塔（做得浅，别抢戏）
    ("base",   "metal",  (8.4, 5.0, 5.6), (13.6, 6.0, 10.4), {}),
    # 座圈 —— 略宽一圈，形成"炮塔坐在环上"的层次
    ("ring",   "metal",  (7.8, 5.9, 4.6), (14.2, 7.0, 11.4), {}),
    # 炮塔主体 —— 米白低矮宽体（真炮塔比高度宽得多）。
    # 这是"封闭炮塔"，正面就是炮室面板、炮管直接从面板开口穿出，没有外加炮盾，
    # 所以西面用带炮管开口的贴图，其余面用白漆。
    ("body",   "hull",   (7.2, 6.9, 4.2), (14.6, 10.2, 11.8),
     {"up": "hull", "down": "metal"}),
    # 顶盖 —— 四周内收 0.6，压出斜切边的观感（MC 不支持梯形，用阶梯近似）
    ("roof",   "hull",   (7.8, 10.1, 5.0), (14.0, 11.1, 11.0), {"up": "hull_top"}),
    # 顶部舱盖
    ("hatch",  "hull",   (9.8, 11.0, 7.0), (12.4, 11.7, 9.0), {"up": "hull_top"}),
    # 炮室前脸 —— 比塔身略窄的一块薄板，往前凸 0.3，做出"正面有开口面板"的层次
    ("face",   "hull",   (6.9, 7.1, 4.8), (7.3, 10.0, 11.2), {"west": "shield"}),
    # 两根炮管 —— 细长，并列朝 -X 伸出，长度与塔身相当，尾端插进炮室里
    ("barrel_l", "barrel", (0.6, 7.8, 6.9), (8.0, 8.6, 7.7), {"west": "muzzle"}),
    ("barrel_r", "barrel", (0.6, 7.8, 8.3), (8.0, 8.6, 9.1), {"west": "muzzle"}),
    # 炮口套环 —— 炮管最前端的加粗环
    ("muzzle_l", "metal", (0.55, 7.65, 6.75), (1.2, 8.75, 7.85), {}),
    ("muzzle_r", "metal", (0.55, 7.65, 8.15), (1.2, 8.75, 9.25), {}),
    # 扶手栏杆 —— 顶盖四周一圈细杆，舰装味。
    # 四根杆都刻意避开顶盖的六个面（各让开 0.01），否则贴合处会 z-fighting 闪烁。
    ("rail_f", "rail", (7.3, 11.0, 4.55), (7.79, 11.35, 11.45), {}),
    ("rail_b", "rail", (14.01, 11.0, 4.55), (14.5, 11.35, 11.45), {}),
    ("rail_l", "rail", (7.3, 11.0, 4.55), (14.5, 11.35, 4.99), {}),
    ("rail_r", "rail", (7.3, 11.0, 11.01), (14.5, 11.35, 11.45), {}),
]


# --------------------------------------------------------------------------
# 贴图绘制
# --------------------------------------------------------------------------
def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def draw_texture():
    """程序化绘制 64x64 贴图。先画 256x256 再缩回，得到柔和边缘。"""
    W = TEX_SIZE * SS
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def rect(region, color):
        x0, y0, x1, y1 = [v * SS for v in REGIONS[region]]
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=color)

    def box(region):
        return [v * SS for v in REGIONS[region]]

    # ---- 炮塔侧板：竖向渐变 + 板缝 + 铆钉 ----
    x0, y0, x1, y1 = box("hull")
    top_c, bot_c = (242, 245, 244), (216, 222, 221)
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        d.line([(x0, y), (x1 - 1, y)], fill=lerp(top_c, bot_c, t))
    # 板缝（水平两道）—— 用深一点的灰而非纯黑，避免 MC 像素画的硬描边感
    seam = (170, 178, 178)
    for frac in (0.34, 0.70):
        yy = int(y0 + (y1 - y0) * frac)
        d.line([(x0, yy), (x1 - 1, yy)], fill=seam, width=max(1, SS // 2))
    # 铆钉
    for cx in range(x0 + 4 * SS, x1 - 2 * SS, 7 * SS):
        for yy in (int(y0 + (y1 - y0) * 0.16), int(y0 + (y1 - y0) * 0.86)):
            r = max(1, int(0.7 * SS))
            d.ellipse([cx - r, yy - r, cx + r, yy + r], fill=(178, 186, 186))

    # ---- 炮塔顶板：更亮 + 中央舱盖圆 ----
    x0, y0, x1, y1 = box("hull_top")
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        d.line([(x0, y), (x1 - 1, y)], fill=lerp((238, 241, 240), (214, 220, 219), t))
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    r = int(7 * SS)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(212, 218, 217))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(176, 184, 184), width=max(1, SS // 2))
    r2 = int(4 * SS)
    d.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], fill=(226, 231, 230))

    # ---- 炮室正面：白面板 + 中央矩形炮管开口 + 两个圆孔 ----
    x0, y0, x1, y1 = box("shield")
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        d.line([(x0, y), (x1 - 1, y)], fill=lerp((232, 235, 234), (190, 197, 197), t))
    # 面板外框（炮室棱线）
    d.rectangle([x0, y0, x1 - 1, y1 - 1], outline=(154, 162, 162), width=max(1, SS // 2))
    # 中央开口：凹进去的矩形，比面板暗一档
    ox0 = x0 + int((x1 - x0) * 0.04)
    ox1 = x1 - int((x1 - x0) * 0.04)
    oy0 = y0 + int((y1 - y0) * 0.30)
    oy1 = y1 - int((y1 - y0) * 0.16)
    d.rectangle([ox0, oy0, ox1 - 1, oy1 - 1], fill=(146, 153, 156))
    # 开口内的两个炮管圆孔
    for frac in (0.33, 0.67):
        cx = int(ox0 + (ox1 - ox0) * frac)
        cyy = (oy0 + oy1) // 2
        r = int((oy1 - oy0) * 0.30)
        d.ellipse([cx - r, cyy - r, cx + r, cyy + r], fill=(52, 56, 58))

    # ---- 炮管：中间高光的圆柱感 ----
    x0, y0, x1, y1 = box("barrel")
    for x in range(x0, x1):
        t = (x - x0) / max(1, (x1 - x0 - 1))
        # t 从 0->1，做成"暗-亮-暗"的圆柱明暗
        shade = 1.0 - abs(t - 0.42) * 1.7
        shade = max(0.0, min(1.0, shade))
        col = lerp((118, 126, 130), (186, 194, 197), shade)
        d.line([(x, y0), (x, y1 - 1)], fill=col)

    # ---- 炮口：深色圆环 + 中心黑洞 ----
    x0, y0, x1, y1 = box("muzzle")
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=(132, 139, 143))
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    r = int(4.4 * SS)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(78, 84, 87))
    r2 = int(2.4 * SS)
    d.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], fill=(34, 37, 39))

    # ---- 金属件：深灰 + 噪点 ----
    x0, y0, x1, y1 = box("metal")
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        d.line([(x0, y), (x1 - 1, y)], fill=lerp((142, 149, 152), (110, 117, 120), t))
    import random

    rnd = random.Random(20260917)
    for _ in range(int((x1 - x0) * (y1 - y0) / (11 * SS * SS))):
        px = rnd.randrange(x0, x1)
        py = rnd.randrange(y0, y1)
        d.point((px, py), fill=(96, 102, 105))

    # ---- 栏杆：浅灰 ----
    rect("rail", (198, 205, 205))

    img = img.resize((TEX_SIZE, TEX_SIZE), Image.LANCZOS)
    return img


# --------------------------------------------------------------------------
# 模型 JSON 生成
# --------------------------------------------------------------------------
def region_uv(name):
    """把贴图分区换算成模型 UV（0..16 单位）。"""
    x0, y0, x1, y1 = REGIONS[name]
    return [x0 * UNIT, y0 * UNIT, x1 * UNIT, y1 * UNIT]


def build_model():
    elements = []
    for name, mat, frm, to, overrides in ELEMENTS:
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            tex = overrides.get(face, mat)
            faces[face] = {"uv": region_uv(tex), "texture": "#0"}
        elements.append(
            {
                "name": name,
                "from": list(frm),
                "to": list(to),
                "faces": faces,
            }
        )

    model = {
        "credit": "日本12.7厘米连装炮 — 由 tools/gen_japanese_127mm_twin_gun.py 生成，勿手改",
        "texture_size": [TEX_SIZE, TEX_SIZE],
        "gui_light": "side",
        "textures": {"0": "piranport:item/japanese_127mm_twin_gun", "particle": "piranport:item/japanese_127mm_twin_gun"},
        "elements": elements,
        # 显式写 display 而不 parent item/handheld：
        # item/handheld 的父链上有 builtin/generated，会再生成一张平面 sprite 叠在模型上。
        # 这里直接复制 handheld 的手持姿势并针对"炮口朝前"重算旋转。
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, -90, -80],
                "translation": [0, 4.0, 0.5],
                "scale": [0.85, 0.85, 0.85],
            },
            "thirdperson_lefthand": {
                "rotation": [0, 90, 80],
                "translation": [0, 4.0, 0.5],
                "scale": [0.85, 0.85, 0.85],
            },
            "firstperson_righthand": {
                "rotation": [0, -80, 0],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.68, 0.68, 0.68],
            },
            "firstperson_lefthand": {
                "rotation": [0, 80, 0],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.68, 0.68, 0.68],
            },
            "gui": {
                "rotation": [20, 35, 0],
                "translation": [0, 0, 0],
                "scale": [0.62, 0.62, 0.62],
            },
            "ground": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 0],
                "scale": [0.25, 0.25, 0.25],
            },
            "fixed": {
                "rotation": [0, 0, 0],
                "translation": [0, 0, 0],
                "scale": [0.5, 0.5, 0.5],
            },
            "head": {
                "rotation": [0, 180, 0],
                "translation": [0, 13, 7],
                "scale": [1, 1, 1],
            },
        },
    }
    return model


def main():
    os.makedirs(os.path.dirname(TEX_PATH), exist_ok=True)
    draw_texture().save(TEX_PATH)
    with open(MODEL_PATH, "w", encoding="utf-8") as f:
        json.dump(build_model(), f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("贴图 ->", TEX_PATH)
    print("模型 ->", MODEL_PATH)


if __name__ == "__main__":
    main()
