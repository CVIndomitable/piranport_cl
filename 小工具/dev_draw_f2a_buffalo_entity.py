# -*- coding: utf-8 -*-
"""F2A 水牛实体贴图 + 三视图预览。

造型重点：蓝灰机身、黄色机翼（战前美军黄翼涂装）、银色发动机罩、
三星徽、三叶螺旋桨。贴图与 F2aBuffaloModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/f2a_buffalo.png"

BLUE = (92, 112, 136, 255)
BLUE_D = (58, 76, 98, 255)
BLUE_L = (122, 142, 164, 255)
YELLOW = (228, 188, 70, 255)
YELLOW_D = (184, 144, 46, 255)
PALE = (164, 176, 186, 255)
SILVER = (194, 200, 204, 255)
WHITE = (240, 240, 238, 255)
NAVY = (36, 56, 110, 255)
GLASS = (146, 172, 194, 255)
FRAME = (48, 58, 70, 255)
DARK = (40, 46, 54, 255)
BLACK = (22, 24, 28, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.1, -2.7, -6.8, 2.2, 2.6, 1.6, 30, 0),      # 发动机罩
    (-1.0, -3.2, -2.5, 2.0, 0.6, 3.0, 40, 0),      # 座舱罩
    (-0.5, -2.0, -7.3, 1.0, 1.0, 1.0, 50, 0),      # 桨毂
    (-1.5, -3.0, -7.2, 3.0, 3.0, 0.0, 44, 6),      # 三叶螺旋桨
    (-2.25, -1.2, -2.4, 4.5, 0.1, 4.0, 0, 16),     # 中翼
    (-6.75, -1.2, -2.3, 4.5, 0.1, 4.0, 19, 16),    # 左外翼
    (2.25, -1.2, -2.3, 4.5, 0.1, 4.0, 19, 16),     # 右外翼（共用 UV）
    (-2.25, -1.4, 4.2, 4.5, 0.1, 2.0, 0, 21),      # 水平尾翼
    (0.0, -5.4, 4.5, 0.0, 2.8, 2.0, 38, 16),       # 垂尾
    (-0.1, 0.0, 5.4, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def insignia(img, x0, y0):
    """3×3 美军星徽：蓝菱形 + 白芯（4×4 会盖满半块翼面）。"""
    for dx, dy in ((1, 0), (0, 1), (2, 1), (1, 2)):
        img.putpixel((x0 + dx, y0 + dy), NAVY)
    img.putpixel((x0 + 1, y0 + 1), WHITE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), BLUE)
    for cx, cy, rx, ry, c in [
        (8, 6, 9, 5, BLUE_L), (24, 8, 8, 5, BLUE_D), (40, 10, 8, 5, BLUE_L),
        (12, 20, 9, 4, BLUE_D), (34, 22, 8, 4, BLUE_L), (52, 18, 8, 5, BLUE_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机翼：上表面黄色，下表面浅灰 ----
    for box in BOXES[5:8]:
        r = face_regions(box)
        fill(img, r["down"], YELLOW)
        fill(img, r["up"], PALE)
    # 外翼星徽（左右共用 UV）
    insignia(img, 24, 18)

    # ---- 水平尾翼 / 垂尾：蓝灰 ----
    rt = face_regions(BOXES[8])
    fill(img, rt["down"], BLUE)
    fill(img, rt["up"], PALE)
    rv = face_regions(BOXES[9])
    fill(img, rv["west"], BLUE)
    fill(img, rv["east"], BLUE)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), BLUE_L)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLUE_L)

    # ---- 机身：机腹浅灰 + 侧面星徽 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], BLUE_D)
    fill(img, rf["south"], BLUE_D)
    # 机身侧面小星徽（3×3 蓝底白芯）
    for y0, x0 in ((13, 3), (13, 26)):
        for dy in range(3):
            for dx in range(3):
                c = WHITE if (dx, dy) == (1, 1) else NAVY
                img.putpixel((x0 + dx, y0 + dy), c)

    # ---- 发动机罩：银环 + 深色正面 ----
    rc = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rc[key], SILVER)
    fill(img, rc["north"], DARK)
    fill(img, rc["south"], SILVER)

    # ---- 座舱罩 ----
    rcan = face_regions(BOXES[2])
    fill(img, rcan["down"], GLASS)
    fill(img, rcan["up"], GLASS)
    for fy in (1,):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (40, 3, 3, 0.6), GLASS)
    fill(img, (43, 3, 3, 0.6), GLASS)
    for fx in (40, 42, 44, 46, 48):
        fill(img, (fx, 3, 1, 0.6), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], GLASS)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], SILVER)
    fill(img, rs["north"], WHITE)

    # ---- 三叶螺旋桨：银白 Y ----
    for x0 in (44, 47):
        fill(img, (x0, 6, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (1, 2)):
            img.putpixel((x0 + dx, 6 + dy), SILVER)
        img.putpixel((x0 + 1, 6 + 1), DARK)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_f2a_entity" if "--preview" in sys.argv else None,
         span=15.0)
    write_model("F2aBuffaloModel", "f2a_buffalo", BOXES, """F2A 水牛实体模型。

<p>蓝灰机身、黄色机翼（战前美军黄翼涂装）、银色发动机罩、三星徽、
三叶螺旋桨。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
