#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制日本六联61厘米鱼雷 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_357.png：
  - 装甲箱顶板 / 侧面是浅蓝灰（原画中间调约 #839bb2，亮面到 #a9bcce）；
  - 箱体背光面与底部裙板深一档（#5f6e7e）；
  - 栏杆、管箍、门框是最亮的一档冷银蓝（#c8d6e5）；
  - 鱼雷管比箱体更亮，受光面接近 #c8d6e5；
  - 装填态管口是亮金属封盖，空膛态换成近黑内膛。
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
            if x % grid == 2 and y % 3 == 1:
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
    """箱体亮面：浅蓝灰，顶板与受光侧面。"""
    save(panel_texture((137, 155, 174), seed=211, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "japanese_sextuple_61cm_torpedo_hull.png")


def hull_dark():
    """箱体背光面 / 底部裙板：深蓝灰。"""
    save(panel_texture((85, 100, 116), seed=217, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "japanese_sextuple_61cm_torpedo_hull_dark.png")


def ring():
    """栏杆、管箍、门框：最亮的冷银蓝。"""
    save(panel_texture((198, 212, 227), seed=223, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.5),
         "japanese_sextuple_61cm_torpedo_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，浅冰蓝、顶部接近原画高光。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 208 - y * 1.8  # 208 -> 152，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 227) * 2.5
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 5  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 14), _clamp(v - 3), _clamp(v + 8), 255))
    save(img, "japanese_sextuple_61cm_torpedo_tube.png")


def detail():
    """舱口、卡榫等小件的中蓝灰金属。"""
    save(panel_texture((108, 124, 140), seed=229, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "japanese_sextuple_61cm_torpedo_detail.png")


def cap_loaded():
    """装填态管口：亮金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (188, 201, 214, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 233) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 44
            if 15 <= x <= 16:
                d -= 26  # 中缝
            v = _clamp(188 + d)
            img.putpixel((x, y), (_clamp(v - 12), _clamp(v - 3), _clamp(v + 6), 255))
    save(img, "japanese_sextuple_61cm_torpedo_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈金属管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (15, 19, 24, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (146, 159, 173, 255))
    save(img, "japanese_sextuple_61cm_torpedo_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
