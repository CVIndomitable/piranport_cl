#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制「博福斯双联120毫米炮」3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_455.png：
  - 炮房外壳是中性蓝灰（受光面约 RGB(141,146,155)），顶盖略亮；
  - 敞开的炮房内部是暗橄榄色（约 RGB(105,106,91)），炮闩是更深的金属灰；
  - 炮管是蓝灰金属（约 RGB(118,121,127)），顶缘有高光；
  - 栏杆、底座法兰近黑。
贴图保持 32x32，逐面 uv 由建模脚本负责，纹理不被拉伸。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32


def _noise(x, y, seed):
    """确定性噪点，范围 -1..1。"""
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=16, grid_dark=4, amp=2.2,
                  top_light=4, bottom_dark=5):
    """带板缝与轻微噪点的装甲面。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if (x % grid == 0) or (y % grid == 0):
                d -= grid_dark
            if y < 2:
                d += top_light
            if y >= SIZE - 2:
                d -= bottom_dark
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def save(img, name):
    path = os.path.join(TEX_DIR, name)
    img.save(path)
    print("wrote", path)


def hull():
    save(panel_texture((141, 146, 155), seed=11, grid=16, grid_dark=4, amp=2.2),
         "bofors_twin_120mm_gun_hull.png")


def roof():
    save(panel_texture((167, 169, 168), seed=29, grid=16, grid_dark=3, amp=2.0),
         "bofors_twin_120mm_gun_roof.png")


def hull_dark():
    save(panel_texture((108, 112, 120), seed=23, grid=16, grid_dark=4, amp=2.2),
         "bofors_twin_120mm_gun_hull_dark.png")


def detail():
    save(panel_texture((160, 163, 166), seed=37, grid=16, grid_dark=4, amp=2.0),
         "bofors_twin_120mm_gun_detail.png")


def interior():
    """炮房内部：暗橄榄色，板缝更暗，少量油污噪点。"""
    save(panel_texture((105, 106, 91), seed=43, grid=16, grid_dark=6, amp=3.0),
         "bofors_twin_120mm_gun_interior.png")


def breech():
    """炮闩：比内部更冷的深金属灰。"""
    save(panel_texture((88, 92, 86), seed=47, grid=16, grid_dark=5, amp=2.6),
         "bofors_twin_120mm_gun_breech.png")


def dark():
    """栏杆 / 底座法兰：近黑。"""
    save(panel_texture((42, 45, 52), seed=53, grid=16, grid_dark=4, amp=2.0),
         "bofors_twin_120mm_gun_dark.png")


def barrel():
    """炮管：沿 v 的圆柱明暗 + 细焊缝，蓝灰金属。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(32):
        shade = 150 - y * 1.8  # 150 -> 94，上亮下暗
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 5
            if 12 <= x <= 19:
                d += 6  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 2), _clamp(v), _clamp(v + 5), 255))
    save(img, "bofors_twin_120mm_gun_barrel.png")


def muzzle_empty():
    """空膛炮口：整图纯色（炮口面用整图 uv）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (18, 18, 20, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (56, 58, 60, 255))
    save(img, "bofors_twin_120mm_gun_muzzle_empty.png")


def muzzle_loaded():
    """装填态炮口：铜色弹底。"""
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    save(img, "bofors_twin_120mm_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull()
    roof()
    hull_dark()
    detail()
    interior()
    breech()
    dark()
    barrel()
    muzzle_empty()
    muzzle_loaded()
