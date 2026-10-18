#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量绘制 2026-10-18 六门主炮 3D 手持模型用的 32x32 材质。

覆盖四套基础模型 + 两套改色：
  japanese_twin_356mm_gun        深棕灰日式双联（白色炮衣）
  us_triple_14inch_gun           浅蓝灰美式三联
  british_quad_14inch_gun        蓝色英式四联
  british_quad_14inch_gun_284    284 指挥仪式灰绿改色
  british_twin_15inch_gun        灰蓝英式双联（卡其炮衣）
  british_twin_15inch_gun_kai    亮蓝改色（卡其炮衣）

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
    """帆布炮衣：竖向褶皱纹，无板缝。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * 4.0
            if x % 5 == 0:
                d -= 12  # 帆布褶
            if x % 5 == 1:
                d += 4
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def barrel_texture(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        shade = 1.12 - y * 0.008  # 顶部亮、底部暗
        for x in range(SIZE):
            d = _noise(x, y, seed) * 3.0
            if x % 7 == 0:
                d -= 6
            if 13 <= x <= 18:
                d += 5
            img.putpixel((x, y), (_clamp(r * shade + d), _clamp(g * shade + d),
                                  _clamp(b * shade + d), 255))
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
    "japanese_twin_356mm_gun": dict(
        hull=(95, 85, 86), hull_dark=(58, 51, 52), roof=(112, 102, 103),
        detail=(80, 73, 74), barrel=(72, 68, 69), blast=(228, 222, 205)),
    "us_triple_14inch_gun": dict(
        hull=(150, 162, 175), hull_dark=(88, 98, 108), roof=(168, 178, 188),
        detail=(118, 130, 142), barrel=(108, 114, 122)),
    "british_quad_14inch_gun": dict(
        hull=(92, 130, 165), hull_dark=(54, 76, 98), roof=(108, 148, 182),
        detail=(74, 104, 134), barrel=(112, 116, 122)),
    "british_quad_14inch_gun_284": dict(
        hull=(108, 118, 116), hull_dark=(66, 74, 72), roof=(126, 136, 133),
        detail=(88, 98, 96), barrel=(104, 108, 108)),
    "british_twin_15inch_gun": dict(
        hull=(120, 130, 145), hull_dark=(70, 78, 90), roof=(138, 148, 160),
        detail=(96, 106, 118), barrel=(118, 123, 130), blast=(188, 172, 112)),
    "british_twin_15inch_gun_kai": dict(
        hull=(55, 85, 180), hull_dark=(30, 50, 115), roof=(72, 108, 205),
        detail=(42, 66, 145), barrel=(125, 130, 138), blast=(198, 178, 108)),
}


def save(img, name):
    path = os.path.join(TEX_DIR, name)
    img.save(path)
    print("wrote", path)


if __name__ == "__main__":
    for i, (gun, c) in enumerate(SETS.items()):
        seed = 11 + i * 13
        save(panel_texture(c["hull"], seed), f"{gun}_hull.png")
        save(panel_texture(c["hull_dark"], seed + 3), f"{gun}_hull_dark.png")
        save(panel_texture(c["roof"], seed + 5, grid=10, grid_dark=4, rivet_dark=3),
             f"{gun}_roof.png")
        save(panel_texture(c["detail"], seed + 7), f"{gun}_detail.png")
        save(barrel_texture(c["barrel"], seed + 9), f"{gun}_barrel.png")
        if "blast" in c:
            save(blast_texture(c["blast"], seed + 11), f"{gun}_blast.png")
        save(muzzle_texture(False), f"{gun}_muzzle_empty.png")
        save(muzzle_texture(True), f"{gun}_muzzle_loaded.png")
