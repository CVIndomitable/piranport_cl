# -*- coding: utf-8 -*-
"""九六式舰战实体贴图 + 三视图预览。

造型重点：银色机身、深色星形发动机罩、红色垂尾/平尾、开放式座舱、
固定起落架轮罩、两叶螺旋桨、翼上橙白方标。
贴图与 Type96FighterModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/type96_fighter.png"

SILVER = (190, 195, 195, 255)
SILVER_D = (146, 152, 154, 255)
SILVER_L = (214, 218, 216, 255)
PALE = (176, 186, 190, 255)
RED = (196, 38, 38, 255)
RED_D = (140, 24, 28, 255)
ORANGE = (222, 142, 68, 255)
WHITE = (246, 244, 238, 255)
DARK = (44, 50, 56, 255)
BLACK = (24, 26, 28, 255)
GLASS = (140, 170, 190, 255)
FRAME = (52, 58, 64, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.1, -2.7, -6.8, 2.2, 2.6, 1.6, 30, 0),      # 星形发动机罩
    (-0.7, -3.0, -1.5, 1.4, 0.4, 1.2, 40, 0),      # 开放式座舱风挡
    (-0.5, -2.0, -7.3, 1.0, 1.0, 1.0, 48, 0),      # 桨毂
    (-1.5, -3.0, -7.2, 3.0, 3.0, 0.0, 44, 8),      # 两叶螺旋桨
    (-2.0, -1.2, -2.4, 4.0, 0.1, 4.0, 0, 16),      # 中翼
    (-6.0, -1.2, -2.3, 4.0, 0.1, 4.0, 18, 16),     # 左外翼
    (2.0, -1.2, -2.3, 4.0, 0.1, 4.0, 18, 16),      # 右外翼（共用 UV）
    (-2.0, -1.4, 4.2, 4.0, 0.1, 2.0, 0, 21),       # 水平尾翼
    (0.0, -5.2, 4.5, 0.0, 2.8, 2.0, 36, 16),       # 垂尾
    # 固定起落架轮罩 + 轮子
    (-2.7, -0.4, -1.6, 0.6, 1.2, 2.0, 52, 12),
    (2.1, -0.4, -1.6, 0.6, 1.2, 2.0, 52, 12),
    (-2.65, 0.8, -1.2, 0.5, 0.4, 0.7, 60, 40),
    (2.15, 0.8, -1.2, 0.5, 0.4, 0.7, 60, 40),
    (-0.1, 0.0, 5.4, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def square_mark(img, x0, y0):
    """2×2 橙标（翼上识别标）。"""
    for dy in range(2):
        for dx in range(2):
            img.putpixel((x0 + dx, y0 + dy), ORANGE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), SILVER)
    for cx, cy, rx, ry, c in [
        (8, 6, 9, 5, SILVER_L), (24, 8, 8, 5, SILVER_D), (40, 10, 8, 5, SILVER_L),
        (12, 20, 9, 4, SILVER_D), (34, 22, 8, 4, SILVER_L), (52, 18, 8, 5, SILVER_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机翼：下表面浅色 + 翼上橙标 ----
    for box in BOXES[5:8]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
    square_mark(img, 23, 18)

    # ---- 水平尾翼 / 垂尾：红色 ----
    rt = face_regions(BOXES[8])
    fill(img, rt["down"], RED)
    fill(img, rt["up"], RED_D)
    rv = face_regions(BOXES[9])
    fill(img, rv["west"], RED)
    fill(img, rv["east"], RED)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), RED_D)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), RED_D)

    # ---- 机身：机腹浅色 + 侧面橙标 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], SILVER_D)
    fill(img, rf["south"], RED_D)
    square_mark(img, 2, 13)
    square_mark(img, 26, 13)

    # ---- 发动机罩 ----
    rc = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rc[key], DARK)
    fill(img, rc["north"], BLACK)
    fill(img, rc["south"], DARK)

    # ---- 风挡 ----
    rw = face_regions(BOXES[2])
    fill(img, rw["down"], GLASS)
    fill(img, rw["up"], GLASS)
    fill(img, rw["north"], FRAME)
    fill(img, rw["south"], FRAME)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], DARK)
    fill(img, rs["north"], SILVER_D)

    # ---- 两叶螺旋桨：1px 竖线 ----
    for x0 in (44, 47):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dy in range(3):
            img.putpixel((x0 + 1, 8 + dy), BLACK)

    # ---- 轮罩：深灰；轮子黑 ----
    rsp = face_regions(BOXES[10])
    for key in ("down", "up", "west", "east", "north", "south"):
        fill(img, rsp[key], DARK)
    fill(img, (60, 40, 4, 4), BLACK)

    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_type96_entity" if "--preview" in sys.argv else None,
         span=14.0)
    write_model("Type96FighterModel", "type96_fighter", BOXES, """九六式舰战实体模型。

<p>银色机身、深色星形发动机罩、红色垂尾/平尾、开放式座舱、
固定起落架轮罩、两叶螺旋桨。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
