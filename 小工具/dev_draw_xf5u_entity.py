# -*- coding: utf-8 -*-
"""XF5U 飞行圆饼实体贴图 + 三视图预览。

造型重点：扁平圆盘机身、两侧发动机短舱 + 大直径四叶桨、双垂尾、
中央气泡座舱、美军星徽。贴图与 Xf5uModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/xf5u.png"

BLUE = (150, 176, 196, 255)
BLUE_D = (112, 138, 162, 255)
BLUE_L = (182, 204, 218, 255)
PALE = (196, 212, 222, 255)
SILVER = (190, 198, 204, 255)
WHITE = (242, 245, 246, 255)
NAVY = (38, 60, 118, 255)
FRAME = (48, 60, 76, 255)
DARK = (40, 48, 60, 255)
BLACK = (22, 26, 32, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-3.5, -2.2, -4.0, 7.0, 2.2, 8.0, 0, 0),       # 圆盘机身
    (-1.0, -3.0, -1.5, 2.0, 0.8, 3.0, 32, 0),      # 气泡座舱罩
    (-3.4, -1.8, -4.5, 1.6, 1.6, 3.0, 44, 0),      # 左发动机短舱
    (1.8, -1.8, -4.5, 1.6, 1.6, 3.0, 44, 0),       # 右发动机短舱
    (-4.4, -2.6, -4.7, 3.0, 3.0, 0.0, 44, 6),      # 左四叶桨
    (1.1, -2.6, -4.7, 3.0, 3.0, 0.0, 50, 6),       # 右四叶桨
    (-3.1, -1.7, -4.9, 1.0, 1.0, 1.0, 54, 0),      # 左桨毂
    (2.1, -1.7, -4.9, 1.0, 1.0, 1.0, 54, 0),       # 右桨毂
    (-1.8, -4.5, 3.0, 0.0, 2.3, 2.0, 38, 12),      # 左垂尾
    (1.8, -4.5, 3.0, 0.0, 2.3, 2.0, 38, 12),       # 右垂尾
    (-2.5, -2.0, 4.0, 5.0, 0.1, 1.5, 0, 12),       # 水平尾翼
]


def insignia(img, x0, y0):
    """3×3 美军星徽：蓝菱形 + 白芯。"""
    for dx, dy in ((1, 0), (0, 1), (2, 1), (1, 2)):
        img.putpixel((x0 + dx, y0 + dy), NAVY)
    img.putpixel((x0 + 1, y0 + 1), WHITE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), BLUE)
    for cx, cy, rx, ry, c in [
        (8, 4, 8, 4, BLUE_L), (22, 6, 7, 4, BLUE_D), (38, 4, 8, 4, BLUE_L),
        (12, 24, 9, 5, BLUE_D), (34, 26, 8, 5, BLUE_L), (52, 22, 8, 5, BLUE_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 圆盘机身：上表面浅蓝 + 星徽，下表面更浅 ----
    rb = face_regions(BOXES[0])
    fill(img, rb["up"], PALE)
    fill(img, rb["down"], BLUE_L)
    fill(img, (rb["down"][0], rb["down"][1] + 3, rb["down"][2], 1), BLUE_D)
    fill(img, (rb["down"][0] + 2, rb["down"][1], 1, rb["down"][3]), BLUE_D)
    insignia(img, 9, 2)
    insignia(img, 12, 2)
    fill(img, rb["north"], BLUE_D)
    fill(img, rb["south"], BLUE_D)
    # 侧面舷窗/面板
    fill(img, (0, 9, 8, 1), BLUE_D)
    fill(img, (14, 9, 14, 1), BLUE_D)

    # ---- 座舱罩 ----
    rcan = face_regions(BOXES[1])
    fill(img, rcan["down"], BLUE_L)
    fill(img, rcan["up"], BLUE_L)
    for fy in (1,):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (32, 3, 3, 0.8), BLUE_L)
    fill(img, (35, 3, 3, 0.8), BLUE_L)
    for fx in (32, 34, 36, 38, 40):
        fill(img, (fx, 3, 1, 0.8), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], BLUE_L)

    # ---- 发动机短舱：银灰 + 深色进气口 ----
    rn = face_regions(BOXES[2])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rn[key], SILVER)
    fill(img, rn["north"], DARK)
    fill(img, (rn["west"][0], rn["west"][1] + 1, rn["west"][2], 1), DARK)
    fill(img, (rn["east"][0], rn["east"][1] + 1, rn["east"][2], 1), DARK)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[6])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], SILVER)
    fill(img, rs["north"], WHITE)

    # ---- 四叶螺旋桨 ----
    for x0 in (44, 50):
        fill(img, (x0, 6, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 6 + dy), BLACK)

    # ---- 双垂尾 + 平尾 ----
    for box in BOXES[8:10]:
        rv = face_regions(box)
        fill(img, rv["west"], BLUE)
        fill(img, rv["east"], BLUE)
        fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), BLUE_L)
        fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLUE_L)
    rt = face_regions(BOXES[10])
    fill(img, rt["down"], BLUE)
    fill(img, rt["up"], PALE)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_xf5u_entity" if "--preview" in sys.argv else None,
         span=15.0)
    write_model("Xf5uModel", "xf5u", BOXES, """XF5U 飞行圆饼实体模型。

<p>扁平圆盘机身、两侧发动机短舱 + 大直径四叶桨、双垂尾、中央气泡座舱、
美军星徽。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
