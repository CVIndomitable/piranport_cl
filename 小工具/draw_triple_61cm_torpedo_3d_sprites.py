#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制「61厘米三连装鱼雷」3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_90.png：
  - 炮座顶面是最亮的中性灰（采样约 RGB(167,166,168)）；
  - 炮座侧壁明显更暗（采样 81~121，偏冷蓝灰）；
  - 鱼雷管是中性钢灰，受光面到 190 上下；
  - 炮口内膛近黑（采样 28~32），外圈有一道亮钢边；
  - 雷头是抛光钢，比管身亮一档。
贴图保持 32x32、每模型单位 2px，逐面 uv 由建模脚本负责。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32
PREFIX = "triple_61cm_torpedo_"


def _noise(x, y, seed):
    """确定性噪点，范围 -1..1。"""
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel(base, seed, grid=8, grid_dark=5, rivet_dark=4,
          amp=2.2, top_light=5, bottom_dark=6):
    """带细板缝和少量铆点的装甲面（原画炮座侧壁的拼板感）。"""
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
    """炮座侧壁：中性灰、冷调，拼板缝比顶面明显。"""
    save(panel((122, 126, 132), seed=211, grid=8, grid_dark=6,
               rivet_dark=5, amp=2.4), "hull.png")


def hull_dark():
    """炮座下裙/阴影面：原画侧壁下缘近 RGB(81,83,88)。"""
    save(panel((78, 82, 89), seed=217, grid=8, grid_dark=5,
               rivet_dark=4, amp=2.2), "hull_dark.png")


def plate():
    """炮座顶面：原画最亮的一档（167,166,168），板缝细、铆点浅。"""
    save(panel((158, 158, 161), seed=223, grid=11, grid_dark=3,
               rivet_dark=3, amp=1.6, top_light=3, bottom_dark=3), "plate.png")


def ring():
    """管箍/加强箍：比管身亮的钢，横向一道亮边让箍看起来是环。"""
    img = panel((146, 152, 160), seed=227, grid=16, grid_dark=3,
                rivet_dark=0, amp=1.4)
    for x in range(SIZE):
        for y in (4, 5, 6):
            px = img.getpixel((x, y))
            img.putpixel((x, y), (_clamp(px[0] + 14), _clamp(px[1] + 14), _clamp(px[2] + 15), 255))
    save(img, "ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗（上亮下暗），靠高光条读出圆管。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 152 - y * 2.4  # 152 -> 78
        for x in range(SIZE):
            d = _noise(x, y, 229) * 2.6
            if x % 11 == 0:
                d -= 6   # 纵向管缝
            if 8 <= x <= 12:
                d += 6   # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 4), _clamp(v - 1), _clamp(v + 4), 255))
    save(img, "tube.png")


def detail():
    """栏杆、支架、螺栓等小件：中暗钢。"""
    save(panel((96, 101, 109), seed=233, grid=6, grid_dark=5,
               rivet_dark=3, amp=2.0), "detail.png")


def warhead():
    """雷头（装填态露在炮座外的鱼雷前段）：抛光钢，比管身亮。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 206 - y * 1.5  # 上亮下暗，保持圆柱感
        for x in range(SIZE):
            d = _noise(x, y, 239) * 1.8
            if 6 <= x <= 10:
                d += 8   # 高光带
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 3), _clamp(v), _clamp(v + 5), 255))
    save(img, "warhead.png")


def cap_loaded():
    """装填态炮口：金属封盖。整面拍在炮口端面上，所以画成内切圆盘。"""
    img = Image.new("RGBA", (SIZE, SIZE), (86, 90, 96, 255))
    cx = cy = SIZE / 2
    for y in range(SIZE):
        for x in range(SIZE):
            r = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r < 12.5:
                # 盖面：中心略暗，越靠外越亮，读作球面金属盖
                v = _clamp(150 + r * 1.8 + _noise(x, y, 241) * 2.4)
                col = (_clamp(v - 3), _clamp(v), _clamp(v + 5), 255)
            elif r < 14.5:
                col = (196, 199, 204, 255)   # 亮钢边圈
            else:
                col = (70, 74, 80, 255)      # 端面阴影
            img.putpixel((x, y), col)
    # 盖中一道搬运把手缝
    for x in range(9, 23):
        img.putpixel((x, 16), (108, 112, 118, 255))
    save(img, "cap_loaded.png")


def cap_empty():
    """空膛态炮口：近黑内膛 + 一圈亮钢口沿（原画黑管口）。"""
    img = Image.new("RGBA", (SIZE, SIZE), (58, 62, 68, 255))
    cx = cy = SIZE / 2
    for y in range(SIZE):
        for x in range(SIZE):
            r = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r < 11.5:
                v = 26 + 10 * (r / 12.0) ** 2          # 内膛近黑、靠壁稍亮
                col = (_clamp(v), _clamp(v + 2), _clamp(v + 5), 255)
            elif r < 14.0:
                col = (176, 180, 186, 255)             # 口沿亮钢圈
            else:
                col = (64, 68, 74, 255)
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
