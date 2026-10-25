#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制四联533毫米鱼雷（ur高速）3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_668.png：
  - 四根鱼雷管是亮银灰（原画受光面接近 RGB(200,205,208)）；
  - 方形炮架比管身略深一档，顶部控制箱再深一点；
  - 管尾机械卡榫用中灰金属；
  - 装填态管口是亮金属封盖，空膛态换成近黑管口。
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
    save(panel_texture((168, 172, 175), seed=91, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quad_533mm_torpedo_ur_high_speed_hull.png")


def hull_dark():
    save(panel_texture((112, 117, 120), seed=97, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         "quad_533mm_torpedo_ur_high_speed_hull_dark.png")


def ring():
    """管箍/控制箱顶面：最亮的一档金属。"""
    save(panel_texture((200, 205, 208), seed=101, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "quad_533mm_torpedo_ur_high_speed_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，亮银灰、冷调。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 205 - y * 1.5  # 205 -> 160，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 103) * 2.5
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 4  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 5), _clamp(v), _clamp(v + 3), 255))
    save(img, "quad_533mm_torpedo_ur_high_speed_tube.png")


def detail():
    """管尾卡榫等小件的中灰金属。"""
    save(panel_texture((145, 150, 153), seed=107, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         "quad_533mm_torpedo_ur_high_speed_detail.png")


def cap_loaded():
    """装填态管口：亮金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (188, 193, 196, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 109) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 42
            if 15 <= x <= 16:
                d -= 24  # 中缝
            v = _clamp(188 + d)
            img.putpixel((x, y), (_clamp(v - 4), _clamp(v), _clamp(v + 3), 255))
    save(img, "quad_533mm_torpedo_ur_high_speed_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈金属管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (16, 18, 20, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (150, 155, 158, 255))
    save(img, "quad_533mm_torpedo_ur_high_speed_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
