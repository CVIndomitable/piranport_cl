#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制德国三联283毫米炮 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_27.png：
  - 炮塔侧面中灰约 RGB(124,127,129)；
  - 顶盖受光更亮约 RGB(154,157,158)，所以 roof 比 hull 浅一档；
  - 炮管与套筒中灰、带冷色高光。
贴图保持 32x32、每模型单位 2px，逐面 uv 由建模脚本负责，纹理不被拉伸。
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


def panel_texture(base, seed, grid=8, grid_dark=5, rivet_dark=4,
                  amp=2.2, top_light=5, bottom_dark=6):
    """带细板缝和少量铆点的装甲面。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if (x % grid == 0) or (y % grid == 0):
                d -= grid_dark
            if x % grid == 2 and y % grid == 3:
                d -= rivet_dark
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
    save(panel_texture((140, 144, 147), seed=11, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "german_triple_283mm_gun_hull.png")


def hull_dark():
    save(panel_texture((96, 100, 103), seed=23, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "german_triple_283mm_gun_hull_dark.png")


def roof():
    """受光顶盖：比侧面亮一档，板缝更稀。"""
    save(panel_texture((168, 171, 173), seed=29, grid=10, grid_dark=4,
                       rivet_dark=3, amp=2.0),
         "german_triple_283mm_gun_roof.png")


def detail():
    save(panel_texture((124, 128, 131), seed=37, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "german_triple_283mm_gun_detail.png")


def barrel():
    """炮管：沿 v 的圆柱明暗 + 沿 u 的细焊缝，冷色金属。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 170 - y * 1.5  # 170 -> 124
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 6
            if 13 <= x <= 18:
                d += 5  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 3), _clamp(v), _clamp(v + 3), 255))
    save(img, "german_triple_283mm_gun_barrel.png")


def port():
    """炮口暗板：近黑色，带一圈浅色反光。"""
    img = Image.new("RGBA", (SIZE, SIZE), (20, 21, 23, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (78, 82, 86, 255))
    save(img, "german_triple_283mm_gun_port.png")


def muzzle_empty():
    """空膛炮口：整图纯色（炮口面用整图 uv）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (20, 20, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (44, 46, 48, 255))
    save(img, "german_triple_283mm_gun_muzzle_empty.png")


def muzzle_loaded():
    """装填态炮口：铜色弹底。"""
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    save(img, "german_triple_283mm_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    roof()
    detail()
    barrel()
    port()
    muzzle_empty()
    muzzle_loaded()
