#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量绘制 2026-10-18（第二批）四门主炮 3D 手持模型用的 32x32 材质。

  italian_triple_381mm_gun        浅灰侧板 + 蓝色顶盖（L_166）
  italian_triple_381mm_gun_kai    蓝色船体 + 蓝绿顶盖改色（L_267）
  us_twin_16inch_gun              浅灰顶/壳 + 深灰侧板（L_122）
  japanese_twin_410mm_gun         灰色炮塔 + 浅色炮衣 + 侧面刻度带（L_25）
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
                  amp=2.2, top_light=5, bottom_dark=6):
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


def blast_texture(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * 4.0
            if x % 5 == 0:
                d -= 12
            if x % 5 == 1:
                d += 4
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def barrel_texture(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        shade = 1.12 - y * 0.008
        for x in range(SIZE):
            d = _noise(x, y, seed) * 3.0
            if x % 7 == 0:
                d -= 6
            if 13 <= x <= 18:
                d += 5
            img.putpixel((x, y), (_clamp(r * shade + d), _clamp(g * shade + d),
                                  _clamp(b * shade + d), 255))
    return img


def scale_texture(seed):
    """侧面刻度带：深灰底 + 等距浅色刻度线。"""
    img = Image.new("RGBA", (SIZE, SIZE), (58, 58, 60, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x % 3 == 0:
                img.putpixel((x, y), (150, 148, 142, 255))
            elif y % 8 == 0:
                img.putpixel((x, y), (95, 95, 97, 255))
    return img


def muzzle_texture(loaded):
    if loaded:
        img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
        for y in range(SIZE):
            for x in range(SIZE):
                if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                    img.putpixel((x, y), (120, 94, 42, 255))
                elif 10 <= x < 22 and 10 <= y < 16:
                    img.putpixel((x, y), (232, 201, 110, 255))
        return img
    img = Image.new("RGBA", (SIZE, SIZE), (20, 20, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (44, 46, 48, 255))
    return img


SETS = {
    "italian_triple_381mm_gun": dict(
        hull=(160, 163, 168), hull_dark=(95, 100, 110), roof=(70, 110, 170),
        detail=(130, 134, 140), barrel=(150, 154, 160)),
    "italian_triple_381mm_gun_kai": dict(
        hull=(65, 105, 180), hull_dark=(35, 60, 110), roof=(115, 130, 95),
        detail=(50, 80, 140), barrel=(110, 120, 140)),
    "us_twin_16inch_gun": dict(
        hull=(190, 192, 195), hull_dark=(55, 57, 62), roof=(205, 207, 210),
        detail=(120, 123, 128), barrel=(150, 152, 158)),
    "japanese_twin_410mm_gun": dict(
        hull=(125, 125, 125), hull_dark=(70, 70, 72), roof=(145, 145, 145),
        detail=(95, 95, 97), barrel=(105, 105, 107), blast=(215, 210, 200)),
}


def save(img, name):
    path = os.path.join(TEX_DIR, name)
    img.save(path)
    print("wrote", path)


if __name__ == "__main__":
    for i, (gun, c) in enumerate(SETS.items()):
        seed = 71 + i * 13
        save(panel_texture(c["hull"], seed), f"{gun}_hull.png")
        save(panel_texture(c["hull_dark"], seed + 3), f"{gun}_hull_dark.png")
        save(panel_texture(c["roof"], seed + 5, grid=10, grid_dark=4, rivet_dark=3),
             f"{gun}_roof.png")
        save(panel_texture(c["detail"], seed + 7), f"{gun}_detail.png")
        save(barrel_texture(c["barrel"], seed + 9), f"{gun}_barrel.png")
        if "blast" in c:
            save(blast_texture(c["blast"], seed + 11), f"{gun}_blast.png")
        if gun == "japanese_twin_410mm_gun":
            save(scale_texture(seed + 13), f"{gun}_scale.png")
        save(muzzle_texture(False), f"{gun}_muzzle_empty.png")
        save(muzzle_texture(True), f"{gun}_muzzle_loaded.png")
