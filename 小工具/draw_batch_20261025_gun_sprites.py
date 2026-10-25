#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量绘制 2026-10-25 批次火炮 3D 手持模型材质（每门 32x32 一套）。

每门炮的配色从对应 zjsnrwiki 原画的受光/背光面上取样，保留：
hull / roof / hull_dark / detail / barrel / interior / muzzle_empty / muzzle_loaded。
英国单装6英寸炮额外有 canvas（炮根帆布套）。
"""
import os

from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")
SIZE = 32

# id -> (hull, roof, hull_dark, detail, barrel_base, interior)
PALETTES = {
    "us_single_5inch_gun": ((109, 129, 147), (140, 160, 176), (79, 96, 112),
                            (132, 152, 168), (150, 155, 165), (62, 68, 74)),
    "us_twin_5inch_gun": ((205, 205, 205), (235, 235, 235), (166, 166, 166),
                          (228, 228, 228), (188, 188, 188), (92, 92, 92)),
    "us_twin_5inch_dp_gun": ((100, 121, 126), (167, 188, 187), (81, 96, 98),
                             (150, 170, 170), (140, 150, 150), (70, 80, 80)),
    "twin_5inch_l54_dp_gun": ((102, 121, 136), (170, 199, 218), (67, 86, 101),
                              (150, 175, 195), (150, 160, 170), (70, 80, 90)),
    "japanese_single_140mm_gun": ((84, 110, 93), (149, 177, 159), (50, 66, 55),
                                  (130, 160, 140), (122, 142, 128), (55, 70, 60)),
    "british_single_6inch_gun": ((167, 182, 204), (213, 222, 238), (138, 154, 179),
                                 (195, 210, 230), (160, 170, 185), (82, 92, 108)),
    "british_twin_6inch_gun": ((136, 156, 164), (165, 188, 198), (84, 98, 104),
                               (160, 180, 188), (140, 150, 155), (70, 80, 85)),
    "japanese_twin_152mm_gun": ((106, 118, 112), (162, 177, 173), (87, 96, 90),
                                (140, 155, 148), (122, 132, 127), (55, 65, 60)),
    "british_triple_6inch_gun": ((87, 112, 128), (189, 222, 236), (53, 80, 98),
                                 (140, 175, 195), (130, 150, 165), (55, 70, 85)),
    "us_triple_6inch_gun": ((79, 94, 116), (118, 133, 155), (63, 75, 95),
                            (110, 125, 145), (120, 130, 145), (50, 60, 75)),
    "mk16_triple_6inch_gun_flashless": ((114, 142, 167), (205, 235, 245), (84, 99, 123),
                                        (170, 200, 215), (140, 160, 175), (65, 80, 95)),
}


def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=16, grid_dark=4, amp=2.2, top_light=4, bottom_dark=5):
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


def barrel_texture(base):
    r, g, b = base
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(32):
        shade = -20 + y * 0.6
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 5
            if 12 <= x <= 19:
                d += 6
            img.putpixel((x, y), (_clamp(r + shade + d), _clamp(g + shade + d),
                                  _clamp(b + shade + 2 + d), 255))
    return img


def solid_rim(inner, rim):
    img = Image.new("RGBA", (SIZE, SIZE), inner + (255,))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), rim + (255,))
    return img


def loaded_muzzle():
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    return img


def save(img, name):
    img.save(os.path.join(TEX_DIR, name))


def generate(gun_id, palette):
    hull, roof, dark, detail, barrel, interior = palette
    save(panel_texture(hull, 11), gun_id + "_hull.png")
    save(panel_texture(roof, 29, grid=10, amp=2.0), gun_id + "_roof.png")
    save(panel_texture(dark, 23), gun_id + "_hull_dark.png")
    save(panel_texture(detail, 37), gun_id + "_detail.png")
    save(panel_texture(interior, 43, grid=8, amp=3.0), gun_id + "_interior.png")
    save(barrel_texture(barrel), gun_id + "_barrel.png")
    save(solid_rim((18, 18, 20), (56, 58, 60)), gun_id + "_muzzle_empty.png")
    save(loaded_muzzle(), gun_id + "_muzzle_loaded.png")
    if gun_id == "british_single_6inch_gun":
        save(panel_texture((218, 205, 175), 61, grid=16, amp=2.4), gun_id + "_canvas.png")


if __name__ == "__main__":
    for gun_id, palette in PALETTES.items():
        generate(gun_id, palette)
    print("done", len(PALETTES), "guns")
