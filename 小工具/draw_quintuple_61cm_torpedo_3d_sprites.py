#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""绘制「61厘米五连装鱼雷」3D 手持模型用的 32x32 材质。

配色取自原画 Equip_L_148.png（整幅是中性灰的单色素描）：
  - 炮座装甲板是最亮的一档浅灰（受光面接近 RGB(219,219,219)）；
  - 鱼雷管比炮座暗一档的钢灰，管身上浅下深；
  - 栏杆、管箍、包边用近白的高光金属灰；
  - 底座、托架、阴影结构用深灰；
  - 面板、舱门、把手用中间灰；
  - 装填态管口是浅金属封盖，空膛态换成近黑管口。

产出（全部落在 textures/item/，文件名严格按物品 ID）：
  <id>_hull / _hull_dark / _ring / _tube / _detail / _cap_loaded / _cap_empty
"""
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
TEX_DIR = os.path.join(ASSETS, "textures/item")

SIZE = 32
ID = "quintuple_61cm_torpedo"


def _noise(x, y, seed):
    """确定性伪随机噪声：同一张贴图每次重跑结果一致，便于回退比对。"""
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=8, grid_dark=5, rivet_dark=4,
                  amp=2.0, top_light=4, bottom_dark=5):
    """装甲板：网格接缝 + 铆钉点 + 上下边缘明暗，模拟钢板拼装。"""
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


def save(img, suffix):
    path = os.path.join(TEX_DIR, f"{ID}_{suffix}.png")
    img.save(path)
    print("wrote", path)


def hull():
    save(panel_texture((198, 201, 204), seed=211, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0), "hull")


def hull_dark():
    save(panel_texture((78, 82, 86), seed=223, grid=8, grid_dark=5,
                       rivet_dark=4, amp=2.0), "hull_dark")


def ring():
    save(panel_texture((208, 212, 216), seed=227, grid=10, grid_dark=3,
                       rivet_dark=2, amp=1.6), "ring")


def tube():
    """鱼雷管：沿 v 的圆柱明暗（上浅下深），中性钢灰，带一道接缝。"""
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = 176 - y * 1.4  # 176 -> 133
        for x in range(SIZE):
            d = _noise(x, y, 229) * 2.4
            if x % 12 == 0:
                d -= 4
            if 10 <= x <= 16:
                d += 5          # 受光带
            v = _clamp(shade + d)
            img.putpixel((x, y), (v, _clamp(v + 2), _clamp(v + 5), 255))
    save(img, "tube")


def detail():
    save(panel_texture((112, 116, 120), seed=233, grid=6, grid_dark=6,
                       rivet_dark=3, amp=2.2), "detail")


def cap_loaded():
    """装填态管口：浅金属封盖，中心一道竖缝，边缘压暗。"""
    img = Image.new("RGBA", (SIZE, SIZE), (186, 190, 194, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 239) * 2.2
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                d -= 52
            if 15 <= x <= 16:
                d -= 26
            v = _clamp(186 + d)
            img.putpixel((x, y), (v, _clamp(v + 2), _clamp(v + 5), 255))
    save(img, "cap_loaded")


def cap_empty():
    """空膛态：近黑管口，只留一圈浅灰管壁，读作张开的深色圆口。"""
    img = Image.new("RGBA", (SIZE, SIZE), (14, 16, 18, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 241) * 3.0
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (146, 150, 154, 255))
            else:
                img.putpixel((x, y), (_clamp(14 + d), _clamp(16 + d),
                                      _clamp(18 + d), 255))
    save(img, "cap_empty")


if __name__ == "__main__":
    hull()
    hull_dark()
    ring()
    tube()
    detail()
    cap_loaded()
    cap_empty()
