#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制五联533毫米鱼雷 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_89.png：
  - 五根鱼雷管是钢蓝灰（原画管面中位约 RGB(86,98,106)，受光面到 (150,166,178)）；
  - 锥顶圆鼓比管身浅一档，横向有一圈圈加强箍；
  - 底座、瞄准具用更深的蓝灰；
  - 装填态管口是金属封盖，空膛态换成近黑管口。
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
    save(panel_texture((138, 146, 152), seed=121, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "quintuple_533mm_torpedo_hull.png")


def hull_dark():
    save(panel_texture((74, 84, 92), seed=127, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "quintuple_533mm_torpedo_hull_dark.png")


def ring():
    """圆鼓加强箍/管箍：比管身亮一档的钢蓝金属。"""
    save(panel_texture((135, 150, 162), seed=131, grid=10, grid_dark=4,
                       rivet_dark=3, amp=2.0),
         "quintuple_533mm_torpedo_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，钢蓝灰、冷调。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 150 - y * 2.3  # 150 -> 78，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 137) * 2.8
            if x % 9 == 0:
                d -= 5
            if 12 <= x <= 17:
                d += 4  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 10), _clamp(v - 2), _clamp(v + 4), 255))
    save(img, "quintuple_533mm_torpedo_tube.png")


def detail():
    """瞄准具/小支架等中灰蓝金属。"""
    save(panel_texture((105, 118, 128), seed=139, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "quintuple_533mm_torpedo_detail.png")


def cap_loaded():
    """装填态管口：金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (150, 164, 174, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 149) * 2.2
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 46
            if 15 <= x <= 16:
                d -= 26  # 中缝
            v = _clamp(150 + d)
            img.putpixel((x, y), (_clamp(v - 8), _clamp(v - 2), _clamp(v + 4), 255))
    save(img, "quintuple_533mm_torpedo_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈蓝灰管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (14, 17, 20, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (100, 112, 122, 255))
    save(img, "quintuple_533mm_torpedo_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
