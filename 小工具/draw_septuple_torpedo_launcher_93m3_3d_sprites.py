#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制七联鱼雷发射器（93M3）3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_718.png（比裸管发射器更蓝、更沉）：
  - 装甲箱顶盖/正面是蓝灰（受光面约 RGB(156,174,190)），侧面压暗一档；
  - 箱体侧板比顶盖再深，原画里左侧面明显更暗；
  - 七根鱼雷管是中等亮度的蓝灰，管箍更亮；
  - 舱门、把手、铆点用中蓝灰金属。
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
                  amp=2.0, top_light=4, bottom_dark=5, blue_shift=0):
    """带细板缝和少量铆点的装甲面；blue_shift 让暗部更偏冷蓝。"""
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
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d),
                                  _clamp(b + d + blue_shift), 255))
    return img


def save(img, name):
    path = os.path.join(TEX_DIR, name)
    img.save(path)
    print("wrote", path)


def hull():
    """箱体顶盖与正面：蓝灰装甲，板缝细而密（原画顶盖分成几块大板）。"""
    save(panel_texture((152, 170, 186), seed=311, grid=8, grid_dark=6,
                       rivet_dark=5, amp=1.8),
         "septuple_torpedo_launcher_93m3_hull.png")


def hull_dark():
    """箱体侧板/底座：更沉的一档蓝灰。"""
    save(panel_texture((100, 114, 130), seed=313, grid=8, grid_dark=6,
                       rivet_dark=4, amp=2.0, top_light=3, bottom_dark=4,
                       blue_shift=3),
         "septuple_torpedo_launcher_93m3_hull_dark.png")


def ring():
    """管箍与铆点：最亮的一档亮金属。"""
    save(panel_texture((176, 190, 206), seed=317, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6),
         "septuple_torpedo_launcher_93m3_ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，蓝灰冷调。

    WHY 渐变只铺满 v 的 0..16 行再重复：模型里管的侧面 uv 恰好取满 [0,16]，
    正好走完整个明暗渐变；顶/底面各取一行单色（uv 的 0..1 / 15..16）。
    """
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 172 - (y % 16) * 3.0  # 172 -> 127，上下两半一致
        for x in range(SIZE):
            d = _noise(x, y, 331) * 2.4
            if x % 11 == 0:
                d -= 5          # 管身接缝
            if 12 <= x <= 19:
                d += 5          # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 4), _clamp(v), _clamp(v + 5), 255))
    save(img, "septuple_torpedo_launcher_93m3_tube.png")


def detail():
    """舱门、把手、铰链等小件的中蓝灰金属。"""
    save(panel_texture((116, 130, 146), seed=337, grid=6, grid_dark=6,
                       rivet_dark=3, amp=2.2, blue_shift=2),
         "septuple_torpedo_launcher_93m3_detail.png")


def cap_loaded():
    """装填态管口：亮金属封盖，外圈略暗、中心一道细缝。"""
    img = Image.new("RGBA", (SIZE, SIZE), (182, 198, 214, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 347) * 2.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 42
            if 15 <= x <= 16:
                d -= 24  # 中缝
            v = _clamp(182 + d)
            img.putpixel((x, y), (_clamp(v - 5), _clamp(v), _clamp(v + 6), 255))
    save(img, "septuple_torpedo_launcher_93m3_cap_loaded.png")


def cap_empty():
    """空膛态管口：近黑内膛 + 一圈蓝灰管壁。"""
    img = Image.new("RGBA", (SIZE, SIZE), (12, 16, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (166, 180, 196, 255))
    save(img, "septuple_torpedo_launcher_93m3_cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
