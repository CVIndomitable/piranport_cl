#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制 61厘米鱼雷发射器9 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_574.png：
  - 炮座是浅灰白装甲（甲板受光面接近 RGB(203,218,230)，前脸 #818b93）；
  - 鱼雷管是亮银灰，比炮座再亮一档；
  - 管箍/吊环/栏杆是高光金属（原画管箍约 #e7f2f6）；
  - 装填态管口是亮金属封盖，空膛态换成近黑管口 + 亮金属管唇。
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
    """炮座装甲：原画甲板 #cbdae6、前脸/侧面 #818b93。"""
    save(panel_texture((176, 186, 196), seed=241, grid=8, grid_dark=6,
                       rivet_dark=4, amp=2.0),
         "torpedo_launcher_61cm_type9_hull.png")


def hull_dark():
    """裙板、板缝、舱门内凹面：原画暗部 #575d61 再压一档。"""
    save(panel_texture((112, 120, 128), seed=251, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "torpedo_launcher_61cm_type9_hull_dark.png")


def ring():
    """甲板面板、舱口盖：原画最亮一档 #dee7f2。"""
    save(panel_texture((206, 216, 226), seed=257, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "torpedo_launcher_61cm_type9_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，亮银灰、冷调（原画管身 #cdd8dd）。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 208 - y * 1.7  # 208 -> 155
        for x in range(SIZE):
            d = _noise(x, y, 263) * 2.5
            if x % 10 == 0:
                d -= 5          # 管身横向接缝
            if 13 <= x <= 18:
                d += 6          # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 6), _clamp(v), _clamp(v + 6), 255))
    save(img, "torpedo_launcher_61cm_type9_tube.png")


def detail():
    """栏杆、吊环、把手等小件：中亮灰金属。"""
    save(panel_texture((158, 169, 178), seed=269, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "torpedo_launcher_61cm_type9_detail.png")


def cap_loaded():
    """装填态管口封盖：亮金属，中心一道细缝高光。"""
    img = Image.new("RGBA", (SIZE, SIZE), (216, 224, 231, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 271) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 44
            if 15 <= x <= 16:
                d += 14         # 封盖中缝高光
            v = _clamp(216 + d)
            img.putpixel((x, y), (_clamp(v - 5), _clamp(v), _clamp(v + 5), 255))
    save(img, "torpedo_launcher_61cm_type9_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈亮金属管唇。"""
    img = Image.new("RGBA", (SIZE, SIZE), (20, 22, 26, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (176, 187, 196, 255))
    save(img, "torpedo_launcher_61cm_type9_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
