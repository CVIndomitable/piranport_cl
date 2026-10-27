#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制 61厘米四连装鱼雷 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_91.png：
  - 炮座是深蓝灰装甲（原画受光面接近 RGB(101,104,118)，侧面更暗）；
  - 鱼雷管比炮座再亮一档的冷蓝灰，管身有横向加强箍；
  - 栏杆/卡榫是浅一档的灰金属；
  - 装填态雷头是浅薰衣草白（原画雷头 RGB 约 205,205,220），
    空膛态换成近黑管口 + 一圈亮金属管唇。
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
                  amp=2.0, top_light=4, bottom_dark=5):
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
    """炮座装甲：原画顶面 #656876、侧面 #56586b。"""
    save(panel_texture((100, 103, 118), seed=211, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quad_61cm_torpedo_hull.png")


def hull_dark():
    """裙板、板缝、舱口内凹面：原画暗部 #2c2d39 ~ #414254。"""
    save(panel_texture((58, 60, 74), seed=217, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quad_61cm_torpedo_hull_dark.png")


def ring():
    """甲板口盖、管箍：亮一档的冷灰金属。"""
    save(panel_texture((132, 136, 152), seed=223, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "quad_61cm_torpedo_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，冷蓝灰、管顶亮管底暗。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 118 - y * 1.6  # 118 -> 69
        for x in range(SIZE):
            d = _noise(x, y, 227) * 2.5
            if x % 10 == 0:
                d -= 4          # 管身横向加强箍
            if 13 <= x <= 18:
                d += 5          # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 6), _clamp(v - 3), _clamp(v + 8), 255))
    save(img, "quad_61cm_torpedo_tube.png")


def detail():
    """栏杆、管尾卡榫等小件：偏亮的灰金属（原画栏杆约 #5a5c66，受光更高）。"""
    save(panel_texture((104, 108, 122), seed=229, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "quad_61cm_torpedo_detail.png")


def cap_loaded():
    """装填态雷头：浅薰衣草白，中段一道高光、根部一圈暗箍。"""
    img = Image.new("RGBA", (SIZE, SIZE), (198, 199, 212, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 233) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 46
            if 13 <= x <= 18:
                d += 12         # 雷头中段高光
            v = _clamp(198 + d)
            img.putpixel((x, y), (_clamp(v - 3), _clamp(v - 2), _clamp(v + 9), 255))
    save(img, "quad_61cm_torpedo_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈亮金属管唇。"""
    img = Image.new("RGBA", (SIZE, SIZE), (16, 17, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (122, 126, 140, 255))
    save(img, "quad_61cm_torpedo_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
