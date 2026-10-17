#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制日本12.7厘米连装炮 3D 手持模型用的 32x32 材质。

技法对齐 us_mk16dp_twin_6inch_gun 那套手建模型贴图：
低对比像素噪点 + 边缘一像素暗棱，明暗交给引擎的面光照。
主色仍按原画保持灰白（mk16dp 原画是蓝灰，所以那边偏蓝）。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32


def _noise(x, y, seed):
    """确定性噪点，范围 -1..1；重复生成贴图结果一致。"""
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def make(base, amp=4.0, seed=0, top_light=0, bottom_dark=0,
         edge_dark=0, seams=(), seam_dark=0):
    """生成一张带噪点的装甲面贴图。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if y < 2:
                d += top_light
            if y >= SIZE - 2:
                d -= bottom_dark
            if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1:
                d -= edge_dark
            for sx in seams:
                if x == sx and (y + sx) % 11 != 4:
                    d -= seam_dark
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def save(img, name):
    path = os.path.join(TEX_DIR, name)
    img.save(path)
    print("wrote", path)


def hull():
    save(make((198, 201, 193), amp=7.0, seed=11,
              top_light=8, bottom_dark=10, edge_dark=12,
              seams=(7, 15, 24), seam_dark=8),
         "japanese_127mm_twin_gun_hull.png")


def hull_dark():
    save(make((118, 122, 114), amp=6.0, seed=23,
              top_light=7, bottom_dark=8, edge_dark=10,
              seams=(12,), seam_dark=7),
         "japanese_127mm_twin_gun_hull_dark.png")


def detail():
    save(make((162, 166, 157), amp=6.0, seed=37,
              top_light=8, bottom_dark=8, edge_dark=10),
         "japanese_127mm_twin_gun_detail.png")


def barrel():
    """炮管：沿 v 的圆柱明暗（上亮下暗）+ 沿 u 的轻微拉丝噪点。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        base = 210 - y * 3.2  # 210 -> 111
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if 12 <= x <= 19:
                d += 7  # 正对相机的一线高光
            v = _clamp(base + d)
            img.putpixel((x, y), (v, _clamp(v + 3), _clamp(v - 6), 255))
    save(img, "japanese_127mm_twin_gun_barrel.png")


def muzzle_empty():
    """空膛炮口：一圈圈压暗的方形膛腔。"""
    img = Image.new("RGBA", (SIZE, SIZE), (52, 52, 46, 255))
    for y in range(4, SIZE - 4):
        for x in range(4, SIZE - 4):
            img.putpixel((x, y), (34, 34, 29, 255))
    for y in range(9, SIZE - 9):
        for x in range(9, SIZE - 9):
            img.putpixel((x, y), (19, 19, 16, 255))
    save(img, "japanese_127mm_twin_gun_muzzle_empty.png")


def muzzle_loaded():
    """装填态炮口：铜色弹底 + 高光。"""
    img = Image.new("RGBA", (SIZE, SIZE), (92, 74, 36, 255))
    for y in range(4, SIZE - 4):
        for x in range(4, SIZE - 4):
            img.putpixel((x, y), (176, 142, 62, 255))
    for y in range(9, SIZE - 9):
        for x in range(9, SIZE - 9):
            img.putpixel((x, y), (204, 166, 78, 255))
    for y in range(13, SIZE - 13):
        for x in range(13, SIZE - 13):
            img.putpixel((x, y), (234, 204, 110, 255))
    save(img, "japanese_127mm_twin_gun_muzzle_loaded.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    detail()
    barrel()
    muzzle_empty()
    muzzle_loaded()
