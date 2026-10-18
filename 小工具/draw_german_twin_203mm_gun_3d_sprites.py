#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制德国双联203毫米炮 3D 手持模型用的 32x32 材质。

画风对齐日本12.7厘米连装炮那套物品模型：
  - 32x32 贴图，模型逐面 uv 按尺寸取样（每模型单位 2px），纹理不变形；
  - 细网格板缝 + 铆钉 + 低对比噪点，明暗主要交给引擎面光照。
主色取原画（Equip_L_18.png）的德式深灰钢：原画炮塔均值约 RGB(80,79,79)，
但直接搬会糊成黑块，这里按面片分开提亮，保留深浅层次。
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


def panel_texture(base, seed, grid=8, grid_dark=10, rivet_dark=20,
                  amp=3.0, top_light=5, bottom_dark=7):
    """带网格板缝和铆钉的装甲面。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            on_grid = (x % grid == 0) or (y % grid == 0)
            if on_grid:
                d -= grid_dark
            # 网格交点旁的铆钉
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
    save(panel_texture((132, 136, 134), seed=11, grid=8, grid_dark=5,
                       rivet_dark=5, amp=2.0),
         "german_twin_203mm_gun_hull.png")


def hull_dark():
    save(panel_texture((82, 86, 84), seed=23, grid=8, grid_dark=5,
                       rivet_dark=5, amp=2.0),
         "german_twin_203mm_gun_hull_dark.png")


def detail():
    save(panel_texture((106, 110, 108), seed=37, grid=8, grid_dark=5,
                       rivet_dark=6, amp=2.0),
         "german_twin_203mm_gun_detail.png")


def barrel():
    """炮管：沿 v 的圆柱明暗 + 沿 u 的加强环，金属拉丝噪点。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 178 - y * 1.8  # 178 -> 122
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 7  # 沿炮管方向的细焊缝
            if 13 <= x <= 18:
                d += 5  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (v, _clamp(v + 2), _clamp(v - 4), 255))
    save(img, "german_twin_203mm_gun_barrel.png")


def muzzle_empty():
    """空膛炮口：整图纯色（炮口面用整图 uv）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (20, 20, 18, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (40, 40, 36, 255))
    save(img, "german_twin_203mm_gun_muzzle_empty.png")


def muzzle_loaded():
    """装填态炮口：铜色弹底。"""
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    save(img, "german_twin_203mm_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    detail()
    barrel()
    muzzle_empty()
    muzzle_loaded()
