# -*- coding: utf-8 -*-
"""烈风（A7M）实体贴图 + 三视图预览。

造型重点：深绿涂装、黄绿前缘/机腹、大面积低置翼、气泡座舱、
四叶深棕螺旋桨、翼上橙白方标。贴图与 ReppuModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/reppu.png"

GREEN = (72, 112, 82, 255)
GREEN_D = (48, 82, 58, 255)
GREEN_L = (98, 138, 104, 255)
PALE = (188, 198, 150, 255)
ORANGE = (220, 140, 62, 255)
WHITE = (244, 242, 234, 255)
BROWN = (96, 70, 50, 255)
BROWN_D = (64, 46, 34, 255)
GLASS = (120, 150, 130, 255)
FRAME = (42, 54, 44, 255)
DARK = (38, 48, 40, 255)
BLACK = (22, 26, 22, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.2, -2.8, -6.8, 2.4, 2.8, 1.6, 30, 0),      # 发动机罩
    (-1.0, -3.2, -2.8, 2.0, 0.7, 4.0, 40, 0),      # 气泡座舱罩
    (-0.5, -2.0, -7.3, 1.0, 1.0, 1.0, 52, 0),      # 桨毂
    (-1.5, -3.0, -7.2, 3.0, 3.0, 0.0, 44, 8),      # 四叶螺旋桨
    (-2.5, -1.2, -2.8, 5.0, 0.1, 5.0, 0, 16),      # 中翼
    (-7.0, -1.2, -2.7, 4.5, 0.1, 5.0, 22, 16),     # 左外翼
    (2.5, -1.2, -2.7, 4.5, 0.1, 5.0, 22, 16),      # 右外翼（共用 UV）
    (-2.5, -1.5, 4.2, 5.0, 0.1, 2.0, 0, 22),       # 水平尾翼
    (0.0, -5.4, 4.5, 0.0, 2.9, 2.0, 41, 16),       # 垂尾
    (-0.1, 0.0, 5.4, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def square_mark3(img, x0, y0):
    """3×3 白边 + 2×2 橙芯（日机识别标）。"""
    for dy in range(3):
        for dx in range(3):
            img.putpixel((x0 + dx, y0 + dy),
                         ORANGE if dx >= 1 and dy >= 1 else WHITE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), GREEN)
    for cx, cy, rx, ry, c in [
        (8, 6, 9, 5, GREEN_L), (24, 8, 8, 5, GREEN_D), (40, 10, 8, 5, GREEN_L),
        (12, 20, 9, 4, GREEN_D), (34, 22, 8, 4, GREEN_L), (52, 18, 8, 5, GREEN_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机翼：上表面绿 + 黄绿前缘；下表面黄绿 ----
    for box in BOXES[5:8]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
        fill(img, (r["down"][0], r["down"][1] + r["down"][3] - 1,
                   r["down"][2], 1), PALE)
    square_mark3(img, 28, 17)

    # ---- 水平尾翼 ----
    rt = face_regions(BOXES[8])
    fill(img, rt["down"], GREEN)
    fill(img, rt["up"], PALE)

    # ---- 机身：机腹黄绿 + 侧面方标 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], GREEN_D)
    fill(img, rf["south"], GREEN_D)
    square_mark3(img, 1, 12)
    square_mark3(img, 27, 12)

    # ---- 发动机罩 ----
    rc = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rc[key], GREEN)
    fill(img, rc["north"], DARK)
    fill(img, rc["south"], GREEN_D)
    fill(img, (rc["west"][0], rc["west"][1] + 2, rc["west"][2], 1), GREEN_D)
    fill(img, (rc["east"][0], rc["east"][1] + 2, rc["east"][2], 1), GREEN_D)

    # ---- 座舱罩 ----
    rcan = face_regions(BOXES[2])
    fill(img, rcan["down"], GLASS)
    fill(img, rcan["up"], GLASS)
    for fy in (1, 3):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (40, 4, 4, 0.7), GLASS)
    fill(img, (44, 4, 4, 0.7), GLASS)
    for fx in (40, 42, 44, 46, 48, 50):
        fill(img, (fx, 4, 1, 0.7), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], GLASS)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], BROWN_D)
    fill(img, rs["north"], BROWN)

    # ---- 四叶螺旋桨 ----
    for x0 in (44, 47):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BROWN)

    # ---- 垂尾：黄绿前缘 ----
    rv = face_regions(BOXES[9])
    fill(img, rv["west"], GREEN)
    fill(img, rv["east"], GREEN)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), PALE)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), PALE)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_reppu_entity" if "--preview" in sys.argv else None,
         span=16.0)
    write_model("ReppuModel", "reppu", BOXES, """烈风（A7M）实体模型。

<p>深绿涂装、黄绿前缘/机腹、大面积低置翼、气泡座舱、四叶深棕螺旋桨、
翼上橙白方标。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
