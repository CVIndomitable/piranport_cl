# -*- coding: utf-8 -*-
"""飞龙（Westland Wyvern）实体贴图 + 三视图预览。

造型重点：很长的机头、红色桨毂、深蓝灰机翼、银色机身、
皇家海军圆徽、翼下火箭。贴图与 WyvernModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/wyvern.png"

SILVER = (176, 176, 176, 255)
SILVER_D = (130, 134, 138, 255)
SILVER_L = (204, 208, 208, 255)
WING = (68, 70, 88, 255)
WING_D = (46, 48, 64, 255)
WING_L = (92, 96, 116, 255)
PALE = (160, 170, 182, 255)
RED = (190, 66, 66, 255)
RED_D = (140, 40, 42, 255)
BLUE = (42, 70, 150, 255)
WHITE = (242, 244, 240, 255)
GLASS = (170, 192, 210, 255)
FRAME = (52, 58, 66, 255)
DARK = (42, 46, 54, 255)
BLACK = (22, 24, 28, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -7.0, 2.0, 2.5, 14.0, 0, 0),      # 机身（长机头）
    (-1.0, -3.1, -2.5, 2.0, 0.6, 4.0, 32, 0),      # 座舱罩
    (-0.5, -2.0, -8.0, 1.0, 1.0, 1.0, 46, 0),      # 红色桨毂
    (-1.5, -3.0, -7.9, 3.0, 3.0, 0.0, 46, 6),      # 四叶螺旋桨
    (-0.7, 0.0, -6.0, 1.4, 0.7, 1.8, 50, 10),      # 机腹散热器
    (-2.5, -1.2, -2.8, 5.0, 0.1, 5.0, 0, 17),      # 中翼
    (-7.5, -1.2, -2.6, 5.0, 0.1, 5.0, 22, 17),     # 左外翼
    (2.5, -1.2, -2.6, 5.0, 0.1, 5.0, 22, 17),      # 右外翼（共用 UV）
    (-2.5, -1.6, 4.4, 5.0, 0.1, 2.0, 0, 24),       # 水平尾翼
    (0.0, -5.6, 4.7, 0.0, 3.0, 2.0, 56, 0),        # 垂尾
    # 翼下火箭（左右各两枚，共用 UV）
    (-5.2, 0.0, -1.6, 0.3, 0.3, 1.6, 44, 12),
    (-4.3, 0.0, -1.6, 0.3, 0.3, 1.6, 44, 12),
    (3.9, 0.0, -1.6, 0.3, 0.3, 1.6, 44, 12),
    (4.8, 0.0, -1.6, 0.3, 0.3, 1.6, 44, 12),
    (-0.1, 0.0, 6.2, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def roundel4(img, x0, y0):
    px = {(0, 1): BLUE, (0, 2): BLUE, (1, 0): BLUE, (1, 1): WHITE, (1, 2): WHITE,
          (1, 3): BLUE, (2, 0): BLUE, (2, 1): WHITE, (2, 2): RED, (2, 3): BLUE,
          (3, 1): BLUE, (3, 2): BLUE}
    for (dx, dy), c in px.items():
        img.putpixel((x0 + dx, y0 + dy), c)


def roundel3(img, x0, y0):
    for dy in range(3):
        for dx in range(3):
            img.putpixel((x0 + dx, y0 + dy), RED if (dx, dy) == (1, 1) else BLUE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), SILVER)
    for cx, cy, rx, ry, c in [
        (10, 8, 9, 5, SILVER_L), (28, 6, 8, 5, SILVER_D), (44, 10, 8, 5, SILVER_L),
        (16, 20, 9, 4, SILVER_D), (36, 22, 8, 4, SILVER_L), (52, 16, 8, 5, SILVER_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机翼：上表面深蓝灰 + 圆徽，下表面浅色 ----
    for box in BOXES[5:8]:
        r = face_regions(box)
        fill(img, r["down"], WING)
        fill(img, r["up"], PALE)
    # 上表面浅色面板
    rc = face_regions(BOXES[5])
    fill(img, (rc["down"][0] + 1, rc["down"][1] + 1, 3, 2), WING_L)
    ro = face_regions(BOXES[6])
    fill(img, (ro["down"][0], ro["down"][1] + 1, 5, 1), WING_D)
    roundel4(img, 28, 18)

    # ---- 水平尾翼 ----
    rt = face_regions(BOXES[8])
    fill(img, rt["down"], WING)
    fill(img, rt["up"], PALE)

    # ---- 机身：机腹浅色、侧面圆徽 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 15, 14, 1), PALE)
    fill(img, (16, 15, 14, 1), PALE)
    fill(img, rf["north"], SILVER_D)
    fill(img, rf["south"], SILVER_D)
    roundel3(img, 3, 14)
    roundel3(img, 27, 14)

    # ---- 座舱罩 ----
    rcan = face_regions(BOXES[1])
    fill(img, rcan["down"], GLASS)
    fill(img, rcan["up"], GLASS)
    for fy in (1, 3):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (32, 4, 4, 0.6), GLASS)
    fill(img, (36, 4, 4, 0.6), GLASS)
    for fx in (32, 34, 36, 38, 40):
        fill(img, (fx, 4, 1, 0.6), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], GLASS)

    # ---- 红色桨毂 ----
    rs = face_regions(BOXES[2])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], RED)
    fill(img, rs["north"], RED_D)

    # ---- 四叶螺旋桨 ----
    for x0 in (46, 49):
        fill(img, (x0, 6, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 6 + dy), BLACK)

    # ---- 机腹散热器 ----
    rr = face_regions(BOXES[4])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rr[key], SILVER_D)
    fill(img, rr["north"], DARK)

    # ---- 垂尾：前缘受光 ----
    rv = face_regions(BOXES[9])
    fill(img, rv["west"], WING)
    fill(img, rv["east"], WING)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), WING_L)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), WING_L)

    # ---- 翼下火箭：白色 ----
    rk = face_regions(BOXES[10])
    for key in ("down", "up", "west", "east", "north", "south"):
        fill(img, rk[key], WHITE)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_wyvern_entity" if "--preview" in sys.argv else None,
         span=17.0)
    write_model("WyvernModel", "wyvern", BOXES, """飞龙（Westland Wyvern）实体模型。

<p>长机头 + 红色桨毂 + 四叶桨、深蓝灰机翼、银色机身、翼下火箭。
机头朝 -Z，y 轴向下；机翼/平尾用 0.1 格厚度避免上下异色时 z-fighting。""")


if __name__ == "__main__":
    main()
