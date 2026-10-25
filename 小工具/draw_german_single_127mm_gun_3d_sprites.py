#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制「德国单装127毫米炮」3D 手持模型用的 32x32 材质（原画 Equip_L_5）。"""
import os
from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")
SIZE = 32


def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=16, grid_dark=4, amp=2.4, top_light=4, bottom_dark=5):
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
    save(panel_texture((127, 122, 122), seed=11), "german_single_127mm_gun_hull.png")


def roof():
    save(panel_texture((169, 168, 168), seed=29, grid=10, amp=2.0),
         "german_single_127mm_gun_roof.png")


def hull_dark():
    save(panel_texture((92, 88, 88), seed=23), "german_single_127mm_gun_hull_dark.png")


def detail():
    save(panel_texture((150, 148, 145), seed=37), "german_single_127mm_gun_detail.png")


def interior():
    save(panel_texture((60, 59, 59), seed=43, grid=8, amp=3.0),
         "german_single_127mm_gun_interior.png")


def barrel():
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(32):
        shade = 168 - y * 1.6
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 5
            if 12 <= x <= 19:
                d += 6
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v), _clamp(v), _clamp(v + 2), 255))
    save(img, "german_single_127mm_gun_barrel.png")


def muzzle_empty():
    img = Image.new("RGBA", (SIZE, SIZE), (18, 18, 20, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (56, 58, 60, 255))
    save(img, "german_single_127mm_gun_muzzle_empty.png")


def muzzle_loaded():
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    save(img, "german_single_127mm_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull(); roof(); hull_dark(); detail(); interior()
    barrel(); muzzle_empty(); muzzle_loaded()
