#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制六连61厘米鱼雷发射器（93M3）3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_678.png：
  - 六根裸鱼雷管是偏冷的浅蓝灰（受光面约 RGB(200,220,222)），管底压到 150 出头；
  - 管箍比管身亮一档，是细窄的亮金属圈；
  - 下方托板/护板是浅灰蓝，与管身接近但更平；
  - 托板上的机械台面与圆鼓底座是深石板色（原画里几乎是黑灰）；
  - 卡榫、辐条手轮等小件用中灰金属。
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
    """带细板缝和少量铆点的金属面。"""
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
    """托板/护板：浅灰蓝平金属，板缝比管身明显。"""
    save(panel_texture((180, 198, 200), seed=211, grid=8, grid_dark=7,
                       rivet_dark=4, amp=1.8),
         "sextuple_61cm_torpedo_launcher_93m3_hull.png")


def hull_dark():
    """机械台面/圆鼓底座：原画里最深的一块石板灰。"""
    save(panel_texture((72, 86, 90), seed=223, grid=8, grid_dark=6,
                       rivet_dark=4, amp=2.4, top_light=6, bottom_dark=3),
         "sextuple_61cm_torpedo_launcher_93m3_hull_dark.png")


def ring():
    """管箍：本体最亮的一档亮金属。"""
    save(panel_texture((206, 224, 226), seed=227, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.5),
         "sextuple_61cm_torpedo_launcher_93m3_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，冷调浅蓝灰，管顶亮、管底暗。

    WHY 渐变只铺满 v 的 0..16 行再重复：模型里管的侧面 uv 恰好取满 [0,16]，
    正好走完整个明暗渐变；顶/底面各取一行单色（uv 的 0..1 / 15..16）。
    若把渐变按 32 行铺，侧面只能看到 198->172 的半截，管身会显得发白发平。
    """
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 198 - (y % 16) * 3.2  # 198 -> 150，上下两半一致
        for x in range(SIZE):
            d = _noise(x, y, 229) * 2.4
            if x % 11 == 0:
                d -= 5          # 管身接缝
            if 12 <= x <= 19:
                d += 5          # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 6), _clamp(v), _clamp(v + 2), 255))
    save(img, "sextuple_61cm_torpedo_launcher_93m3_tube.png")


def detail():
    """卡榫、辐条手轮、操纵箱等小件的中灰金属。"""
    save(panel_texture((124, 138, 141), seed=233, grid=6, grid_dark=6,
                       rivet_dark=3, amp=2.2),
         "sextuple_61cm_torpedo_launcher_93m3_detail.png")


def cap_loaded():
    """装填态管口：亮金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (196, 214, 216, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 239) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 44
            if 15 <= x <= 16:
                d -= 26  # 中缝
            v = _clamp(196 + d)
            img.putpixel((x, y), (_clamp(v - 5), _clamp(v), _clamp(v + 2), 255))
    save(img, "sextuple_61cm_torpedo_launcher_93m3_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈浅色管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (16, 20, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (176, 194, 196, 255))
    save(img, "sextuple_61cm_torpedo_launcher_93m3_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
