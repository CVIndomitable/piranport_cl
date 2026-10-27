#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制日本72厘米三连装鱼雷 3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_225.png：
  - 三根粗大鱼雷管是浅蓝灰（管身受光面约 RGB(178,182,190)）；
  - 管头前有一圈接近纯白的识别亮带；
  - 雷头是近黑的圆头（装填态），空膛态换成带金属唇口的暗内膛；
  - 后部安装座、托架是深蓝灰（约 RGB(72,78,88)）；
  - 支撑臂/框架是中灰金属。
贴图保持 32x32、每模型单位 2px，逐面 uv 由建模脚本负责，纹理不被拉伸。
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32
PREFIX = "japanese_triple_72cm_torpedo_"


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
    """雷尾块/箱体：与管身同色系的浅灰装甲。"""
    save(panel_texture((168, 174, 184), seed=411, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         PREFIX + "hull.png")


def hull_dark():
    """安装座、托架、底座：深蓝灰。"""
    save(panel_texture((72, 78, 88), seed=417, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0),
         PREFIX + "hull_dark.png")


def ring():
    """雷头后方的白色识别亮带。"""
    save(panel_texture((232, 234, 240), seed=423, grid=11, grid_dark=3,
                       rivet_dark=2, amp=1.6, top_light=2, bottom_dark=6),
         PREFIX + "ring.png")


def tube():
    """鱼雷管：沿 v 的圆柱明暗，浅蓝灰。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 205 - y * 2.4  # 205 -> 131，管顶亮、管底暗
        for x in range(SIZE):
            d = _noise(x, y, 431) * 2.5
            # 管身分段的横向接缝：每隔 7px 一道暗线
            if y % 7 == 0:
                d -= 10
            if x % 10 == 0:
                d -= 4
            if 13 <= x <= 18:
                d += 4  # 正面高光
            v = _clamp(shade + d)
            img.putpixel((x, y), (_clamp(v - 6), _clamp(v - 1), _clamp(v + 6), 255))
    save(img, PREFIX + "tube.png")


def detail():
    """管箍、支撑臂、手轮等中灰金属。"""
    save(panel_texture((110, 118, 128), seed=437, grid=6, grid_dark=5,
                       rivet_dark=3, amp=2.2),
         PREFIX + "detail.png")


def cap_loaded():
    """装填态雷头：整段近黑，只在受光面上方留一道冷调高光。
    不做亮边——雷头是分三段收口的八棱柱，亮边会在每级台阶上画出「画框」。
    """
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 20 + _noise(x, y, 443) * 3.0
            # 圆头的受光面：上方两三条高光带
            if 4 <= y <= 9:
                v += 16 - abs(x - 15) * 1.2
            elif 10 <= y <= 12:
                v += 6
            # 边缘略压暗，让圆头收边
            if x < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                v -= 4
            c = _clamp(v)
            img.putpixel((x, y), (c, c + 1, c + 4, 255))
    save(img, PREFIX + "cap_loaded.png")


def cap_empty():
    """空膛态管口：中灰金属封盖，与装填态的黑雷头拉开明度差。
    不做亮边：雷头是分段收口的，亮边会在每级台阶上画出「画框」。
    """
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 112 + _noise(x, y, 449) * 3.5
            if y < 6:
                v += 12
            if y >= SIZE - 6:
                v -= 10
            if 15 <= x <= 16:
                v -= 14  # 中缝
            c = _clamp(v)
            img.putpixel((x, y), (c - 3, c - 1, c + 4, 255))
    save(img, PREFIX + "cap_empty.png")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
