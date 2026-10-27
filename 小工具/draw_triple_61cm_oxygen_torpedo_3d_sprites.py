#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制「61厘米三连装氧气鱼雷」3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_149.png：
  - 顶面同样是亮中性灰（167,166,168），但侧壁更偏冷、亮一档（86~133）；
  - 氧气雷的雷头是抛光钢，原画高光到 225，明显比常规型雷头亮；
  - 炮口内膛近黑（62），口沿一圈亮钢（130）；
  - 顶面右前方的大圆盖是黄铜感较弱的暗钢（原画约 150 的圆面 + 深色边）。
与常规型共用同一套贴图名，但每个文件独立绘制，便于两件物品各自微调配色。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32
PREFIX = "triple_61cm_oxygen_torpedo_"


def _noise(x, y, seed):
    """确定性噪点，范围 -1..1。"""
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel(base, seed, grid=8, grid_dark=5, rivet_dark=4,
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
    path = os.path.join(TEX_DIR, PREFIX + name)
    img.save(path)
    print("wrote", path)


def hull():
    """炮座侧壁：冷调中灰，原画该型侧壁比常规型亮一档（121,121,126）。"""
    save(panel((128, 132, 139), seed=307, grid=8, grid_dark=6,
               rivet_dark=5, amp=2.4), "hull.png")


def hull_dark():
    """炮座下裙/背光面（原画约 86,90,95）。"""
    save(panel((84, 88, 96), seed=311, grid=8, grid_dark=5,
               rivet_dark=4, amp=2.2), "hull_dark.png")


def plate():
    """炮座顶面：原画最亮的一档（167,166,168），板缝更细。"""
    save(panel((162, 162, 165), seed=313, grid=11, grid_dark=3,
               rivet_dark=3, amp=1.5, top_light=3, bottom_dark=3), "plate.png")


def ring():
    """管箍/圆盖边圈：亮钢，横向一道亮边。"""
    img = panel((154, 159, 167), seed=317, grid=16, grid_dark=3,
                rivet_dark=0, amp=1.3)
    for x in range(SIZE):
        for y in (4, 5, 6):
            px = img.getpixel((x, y))
            img.putpixel((x, y), (_clamp(px[0] + 16), _clamp(px[1] + 16), _clamp(px[2] + 17), 255))
    save(img, "ring.png")


def tube():
    """氧气鱼雷管：冷调钢，高光更集中（原画管面高光到 225）。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 160 - y * 2.6  # 160 -> 76
        for x in range(SIZE):
            d = _noise(x, y, 319) * 2.6
            if x % 11 == 0:
                d -= 6
            if 9 <= x <= 12:
                d += 10  # 收窄的高光条
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 3), _clamp(v), _clamp(v + 6), 255))
    save(img, "tube.png")


def detail():
    """栏杆、支架、螺栓等小件。"""
    save(panel((100, 105, 113), seed=323, grid=6, grid_dark=5,
               rivet_dark=3, amp=2.0), "detail.png")


def warhead():
    """雷头：大而钝的抛光钢头，原画高光到 225，是氧气型最大的视觉差异。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 218 - y * 1.7
        for x in range(SIZE):
            d = _noise(x, y, 327) * 1.7
            if 5 <= x <= 9:
                d += 10
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 4), _clamp(v - 1), _clamp(v + 6), 255))
    save(img, "warhead.png")


def cap_loaded():
    """装填态炮口：金属封盖，比常规型更亮（氧气型原画管口偏银）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (92, 97, 104, 255))
    cx = cy = SIZE / 2
    for y in range(SIZE):
        for x in range(SIZE):
            r = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r < 12.5:
                v = _clamp(164 + r * 1.6 + _noise(x, y, 331) * 2.2)
                col = (_clamp(v - 3), _clamp(v), _clamp(v + 6), 255)
            elif r < 14.5:
                col = (206, 210, 216, 255)   # 亮钢边圈
            else:
                col = (76, 80, 87, 255)
            img.putpixel((x, y), col)
    for x in range(9, 23):
        img.putpixel((x, 16), (118, 123, 130, 255))
    save(img, "cap_loaded.png")


def cap_empty():
    """空膛态炮口：近黑内膛 + 亮钢口沿。"""
    img = Image.new("RGBA", (SIZE, SIZE), (62, 67, 74, 255))
    cx = cy = SIZE / 2
    for y in range(SIZE):
        for x in range(SIZE):
            r = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r < 11.5:
                v = 22 + 12 * (r / 12.0) ** 2
                col = (_clamp(v), _clamp(v + 3), _clamp(v + 7), 255)
            elif r < 14.0:
                col = (168, 173, 180, 255)
            else:
                col = (68, 73, 80, 255)
            img.putpixel((x, y), col)
    save(img, "cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    plate()
    ring()
    tube()
    detail()
    warhead()
    cap_loaded()
    cap_empty()
