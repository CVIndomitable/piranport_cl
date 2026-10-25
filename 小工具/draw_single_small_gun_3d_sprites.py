#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制日本12厘米单装炮 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_2.png：
  - 炮盾是灰蓝色（受光面约 RGB(161,189,198)），顶盖略亮；
  - 背光面与下缘更深（约 RGB(96,120,130)），座圈与瞄准具近黑；
  - 炮管是中性深灰（约 RGB(107,109,110)），顶缘有高光。
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


def panel_texture(base, seed, grid=16, grid_dark=4, amp=2.4,
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
    save(panel_texture((161, 189, 198), seed=11, grid=16, grid_dark=4, amp=2.6),
         "single_small_gun_hull.png")


def roof():
    save(panel_texture((176, 201, 210), seed=29, grid=16, grid_dark=3, amp=2.2),
         "single_small_gun_roof.png")


def hull_dark():
    save(panel_texture((96, 120, 130), seed=23, grid=16, grid_dark=4, amp=2.4),
         "single_small_gun_hull_dark.png")


def detail():
    save(panel_texture((130, 152, 160), seed=37, grid=16, grid_dark=4, amp=2.2),
         "single_small_gun_detail.png")


def dark():
    """座圈 / 瞄准具：原画里最暗的蓝黑金属。"""
    save(panel_texture((64, 72, 76), seed=43, grid=16, grid_dark=5, amp=2.0),
         "single_small_gun_dark.png")


def barrel():
    """炮管：沿 v 的圆柱明暗 + 细焊缝，中性深灰。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(32):
        shade = 132 - y * 1.7  # 132 -> 79，上亮下暗
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 5
            if 12 <= x <= 19:
                d += 6  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v), _clamp(v + 1), _clamp(v + 3), 255))
    save(img, "single_small_gun_barrel.png")


def port():
    """炮口暗板：近黑色，带一圈浅色反光。"""
    img = Image.new("RGBA", (SIZE, SIZE), (26, 29, 32, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (86, 98, 106, 255))
    save(img, "single_small_gun_port.png")


def muzzle_empty():
    """空膛炮口：整图纯色（炮口面用整图 uv）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (18, 18, 20, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (56, 58, 60, 255))
    save(img, "single_small_gun_muzzle_empty.png")


def muzzle_loaded():
    """装填态炮口：铜色弹底。"""
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    save(img, "single_small_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull()
    roof()
    hull_dark()
    detail()
    dark()
    barrel()
    port()
    muzzle_empty()
    muzzle_loaded()
