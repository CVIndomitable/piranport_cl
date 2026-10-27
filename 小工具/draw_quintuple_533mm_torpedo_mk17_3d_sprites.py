#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制五联533毫米鱼雷(MK17) 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_194.png：
  - 鱼雷管是浅冰蓝（原画受光面接近 RGB(185,200,210)）；
  - 圆鼓比管身再亮一档，鼓身有一道近黑环带；
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
    save(panel_texture((150, 168, 180), seed=161, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quintuple_533mm_torpedo_mk17_hull.png")


def hull_dark():
    save(panel_texture((70, 85, 98), seed=167, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quintuple_533mm_torpedo_mk17_hull_dark.png")


def ring():
    save(panel_texture((185, 198, 208), seed=173, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "quintuple_533mm_torpedo_mk17_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，浅冰蓝。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 185 - y * 1.7  # 185 -> 131
        for x in range(SIZE):
            d = _noise(x, y, 179) * 2.5
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 4
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 12), _clamp(v - 3), _clamp(v + 5), 255))
    save(img, "quintuple_533mm_torpedo_mk17_tube.png")


def detail():
    save(panel_texture((100, 118, 132), seed=181, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "quintuple_533mm_torpedo_mk17_detail.png")


def band():
    """鼓身近黑环带。"""
    save(panel_texture((24, 29, 34), seed=191, grid=10, grid_dark=2,
                       rivet_dark=2, amp=1.6),
         "quintuple_533mm_torpedo_mk17_band.png")


def cap_loaded():
    img = Image.new("RGBA", (SIZE, SIZE), (170, 185, 195, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 193) * 2.2
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 50
            if 15 <= x <= 16:
                d -= 28
            v = _clamp(170 + d)
            img.putpixel((x, y), (_clamp(v - 10), _clamp(v - 3), _clamp(v + 4), 255))
    save(img, "quintuple_533mm_torpedo_mk17_cap_loaded.png")


def cap_empty():
    img = Image.new("RGBA", (SIZE, SIZE), (18, 23, 28, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 136, 150, 255))
    save(img, "quintuple_533mm_torpedo_mk17_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    band()
    cap_loaded()
    cap_empty()
