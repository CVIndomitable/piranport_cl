#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制七联鱼雷发射器 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_560.png：
  - 发射箱是冷调蓝灰装甲板，受光面约 RGB(128,132,144)；
  - 鱼雷管比箱体再暗一档，带横向加强箍；
  - 帆布防水罩是米色（约 RGB(212,204,192)），有褶皱暗纹和几处污渍；
  - 底座、下缘暗带用深蓝灰；
  - 装填态管口是金属封盖，空膛态换成近黑内膛 + 金属管壁。
贴图保持 32x32、每模型单位 2px，逐面 uv 由建模脚本负责，纹理不被拉伸。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32
PREFIX = "septuple_torpedo_launcher_"


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
    save(panel_texture((128, 132, 144), seed=311, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         PREFIX + "hull.png")


def hull_dark():
    save(panel_texture((76, 84, 100), seed=317, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         PREFIX + "hull_dark.png")


def ring():
    """管箍/顶面镶板：最亮的一档金属。"""
    save(panel_texture((152, 158, 172), seed=323, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         PREFIX + "ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，冷调蓝灰。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 165 - y * 2.4  # 165 -> 91，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 331) * 2.5
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 4  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 7), _clamp(v - 2), _clamp(v + 5), 255))
    save(img, PREFIX + "tube.png")


def detail():
    """底座台阶、小件的中灰金属。"""
    save(panel_texture((100, 108, 122), seed=337, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         PREFIX + "detail.png")


def canvas():
    """米色防水帆布：布纹噪点 + 斜向褶皱暗痕 + 几处淡污渍。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 341) * 7.0
            # 斜向褶皱：几条宽窄不一的暗带，模拟布的垂坠
            fold = (x * 3 + y * 5) % 17
            if fold in (0, 1):
                d -= 14
            elif fold in (7,):
                d += 8
            # 布纹：隔行轻微明暗差
            if y % 3 == 0:
                d -= 2.5
            # 几处污渍/加固缝
            if 6 <= x <= 9 and 18 <= y <= 24:
                d -= 9
            if 22 <= x <= 25 and 4 <= y <= 9:
                d -= 6
            img.putpixel((x, y), (_clamp(212 + d), _clamp(204 + d * 0.95),
                                  _clamp(192 + d * 0.9), 255))
    save(img, PREFIX + "canvas.png")


def cap_loaded():
    """装填态管口：金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (152, 160, 174, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 347) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 44
            if 15 <= x <= 16:
                d -= 26  # 中缝
            v = _clamp(152 + d)
            img.putpixel((x, y), (_clamp(v - 7), _clamp(v - 2), _clamp(v + 5), 255))
    save(img, PREFIX + "cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈金属管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (18, 21, 26, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (112, 124, 138, 255))
    save(img, PREFIX + "cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    canvas()
    cap_loaded()
    cap_empty()
