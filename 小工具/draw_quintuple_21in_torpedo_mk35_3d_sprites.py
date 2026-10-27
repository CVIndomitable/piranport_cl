#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制21寸五联装鱼雷（MK35） 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_707.png：
  - 鱼雷管是浅冰蓝（原画受光面接近 RGB(185,200,210)）；
  - 管箍比管身亮一档，整体是中性灰金属；
  - 底座、卡榫用深蓝灰；
  - 装填态管口是金属封盖，空膛态换成近黑管口。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32


def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=8, grid_dark=5, rivet_dark=4,
                  amp=2.0, top_light=4, bottom_dark=5):
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
    save(panel_texture((128, 131, 133), seed=161, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quintuple_21in_torpedo_mk35_hull.png")


def hull_dark():
    save(panel_texture((72, 75, 77), seed=167, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quintuple_21in_torpedo_mk35_hull_dark.png")


def ring():
    save(panel_texture((168, 171, 173), seed=173, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "quintuple_21in_torpedo_mk35_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，浅冰蓝。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 192 - y * 1.9  # 192 -> 131
        for x in range(SIZE):
            d = _noise(x, y, 179) * 2.5
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 4
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 2), _clamp(v - 1), _clamp(v + 2), 255))
    save(img, "quintuple_21in_torpedo_mk35_tube.png")


def detail():
    save(panel_texture((104, 107, 110), seed=181, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "quintuple_21in_torpedo_mk35_detail.png")


def band():
    """鼓身近黑环带。"""
    save(panel_texture((30, 32, 33), seed=191, grid=10, grid_dark=2,
                       rivet_dark=2, amp=1.6),
         "quintuple_21in_torpedo_mk35_band.png")


def cap_loaded():
    img = Image.new("RGBA", (SIZE, SIZE), (172, 175, 177, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 193) * 2.2
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 50
            if 15 <= x <= 16:
                d -= 28
            v = _clamp(172 + d)
            img.putpixel((x, y), (_clamp(v - 2), _clamp(v - 1), _clamp(v + 2), 255))
    save(img, "quintuple_21in_torpedo_mk35_cap_loaded.png")


def cap_empty():
    img = Image.new("RGBA", (SIZE, SIZE), (18, 23, 28, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (150, 153, 155, 255))
    save(img, "quintuple_21in_torpedo_mk35_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    band()
    cap_loaded()
    cap_empty()
