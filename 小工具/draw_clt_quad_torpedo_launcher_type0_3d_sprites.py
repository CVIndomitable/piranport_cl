#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制 CLT四联鱼雷发射器零式 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_704.png：
  - 箱体顶板是中性石板灰（#939da7），比侧面亮；
  - 箱体前脸、侧面是深灰（#4a5055），底部围板更深；
  - 舷窗框与 C 形卡箍用中灰（#7a8287 附近）；
  - 鱼雷管是比箱体更浅的灰（#8a949e），带细密的环向接缝；
  - 装填态管口是浅灰金属封盖，空膛态换成近黑内膛。
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
    """箱体顶板：中性石板灰，最亮的一档大面积面。"""
    save(panel_texture((147, 157, 167), seed=311, grid=8, grid_dark=4,
                       rivet_dark=3, amp=1.8),
         "clt_quad_torpedo_launcher_type0_hull.png")


def hull_dark():
    """箱体前脸 / 侧面：深石板灰。"""
    save(panel_texture((72, 78, 84), seed=317, grid=8, grid_dark=4,
                       rivet_dark=3, amp=1.8),
         "clt_quad_torpedo_launcher_type0_hull_dark.png")


def ring():
    """顶板包边、管口管箍：亮灰金属。"""
    save(panel_texture((172, 180, 188), seed=323, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.4),
         "clt_quad_torpedo_launcher_type0_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，中性灰、管顶亮。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 168 - y * 1.7  # 168 -> 114，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 327) * 2.4
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 5  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 2), _clamp(v), _clamp(v + 4), 255))
    save(img, "clt_quad_torpedo_launcher_type0_tube.png")


def detail():
    """舷窗框、C 形卡箍等小件：中灰金属。"""
    save(panel_texture((115, 122, 128), seed=329, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.0),
         "clt_quad_torpedo_launcher_type0_detail.png")


def cap_loaded():
    """装填态管口：浅灰金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (156, 164, 171, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 331) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 40
            if 15 <= x <= 16:
                d -= 22  # 中缝
            v = _clamp(156 + d)
            img.putpixel((x, y), (_clamp(v - 2), _clamp(v), _clamp(v + 4), 255))
    save(img, "clt_quad_torpedo_launcher_type0_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈金属管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (13, 15, 17, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (128, 135, 142, 255))
    save(img, "clt_quad_torpedo_launcher_type0_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
