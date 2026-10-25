#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制四联533毫米鱼雷 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_88.png：
  - 鱼雷管是冷调蓝灰（原画灰面中位约 RGB(73,81,83)，受光面更亮）；
  - 管箍/顶部导轨比管身浅一档；
  - 雷头是暗红色（原画红面中位约 RGB(91,36,32)，高光到 (126,67,61)）；
  - 空膛态雷头用全透明贴图，露出后面的黑管口。
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
                  amp=2.2, top_light=5, bottom_dark=6):
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
    save(panel_texture((118, 124, 128), seed=61, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "quad_533mm_torpedo_hull.png")


def hull_dark():
    save(panel_texture((78, 84, 88), seed=67, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.2),
         "quad_533mm_torpedo_hull_dark.png")


def ring():
    """管箍/导轨：比管身浅一档的受光金属。"""
    save(panel_texture((158, 163, 166), seed=71, grid=10, grid_dark=4,
                       rivet_dark=3, amp=2.0),
         "quad_533mm_torpedo_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗 + 少量纵向焊缝，冷调蓝灰。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 168 - y * 1.6  # 168 -> 118，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 79) * 3.0
            if x % 9 == 0:
                d -= 5
            if 12 <= x <= 17:
                d += 4  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 6), _clamp(v), _clamp(v + 4), 255))
    save(img, "quad_533mm_torpedo_tube.png")


def warhead():
    """雷头：暗红漆面，顶部受光、下缘压暗。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        k = 1.18 - y * 0.014  # 1.18 -> 0.75
        for x in range(SIZE):
            d = _noise(x, y, 83) * 3.0
            if 11 <= x <= 20 and 6 <= y <= 10:
                d += 8  # 一道横向高光
            img.putpixel((x, y), (_clamp(122 * k + d), _clamp(44 * k + d * 0.5),
                                  _clamp(38 * k + d * 0.5), 255))
    save(img, "quad_533mm_torpedo_warhead.png")


def warhead_empty():
    """空膛态：全透明。BEWLR 用 translucent item sheet，alpha=0 的面不可见，
    正好露出后面的 tube_end 黑管口。"""
    save(Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0)),
         "quad_533mm_torpedo_warhead_empty.png")


def tube_end():
    """空管口：近黑内膛 + 一圈略亮管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (18, 20, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (72, 78, 82, 255))
    save(img, "quad_533mm_torpedo_tube_end.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    warhead()
    warhead_empty()
    tube_end()
