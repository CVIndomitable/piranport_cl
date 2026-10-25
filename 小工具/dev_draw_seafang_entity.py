# -*- coding: utf-8 -*-
"""海毒牙（Seafang）实体贴图 + 三视图预览。

造型重点：浅蓝灰椭圆翼、长机头、红色桨毂、英式圆徽、
机翼/后机身黄黑识别带。贴图与 SeafangModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/seafang.png"

BLUE = (140, 160, 182, 255)
BLUE_D = (98, 118, 146, 255)
BLUE_L = (170, 188, 206, 255)
PALE = (184, 198, 210, 255)
YELLOW = (240, 224, 64, 255)
BLACK = (26, 28, 32, 255)
WHITE = (242, 244, 242, 255)
RED = (200, 120, 120, 255)
RED_D = (156, 78, 82, 255)
RGB_BLUE = (42, 66, 148, 255)
FRAME = (48, 58, 72, 255)
DARK = (40, 46, 58, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.1, -2.7, -6.8, 2.2, 2.6, 1.6, 30, 0),      # 发动机罩
    (-1.0, -3.2, -2.6, 2.0, 0.7, 3.5, 39, 0),      # 气泡座舱罩
    (-0.5, -2.0, -7.3, 1.0, 1.0, 1.0, 52, 0),      # 红色桨毂
    (-1.5, -3.0, -7.2, 3.0, 3.0, 0.0, 44, 8),      # 四叶螺旋桨
    (-2.0, -1.2, -2.5, 4.0, 0.1, 4.5, 0, 16),      # 中翼
    (-6.0, -1.2, -2.4, 4.0, 0.1, 4.5, 19, 16),     # 左外翼
    (2.0, -1.2, -2.4, 4.0, 0.1, 4.5, 19, 16),      # 右外翼（共用 UV）
    (-2.0, -1.4, 4.2, 4.0, 0.1, 2.0, 0, 21),       # 水平尾翼
    (0.0, -5.4, 4.5, 0.0, 2.9, 2.0, 38, 16),       # 垂尾
    (-0.1, 0.0, 5.4, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def roundel3(img, x0, y0):
    """3×3 英式圆徽：蓝底 + 白环 + 红芯（避免画成棋盘格）。"""
    for dy in range(3):
        for dx in range(3):
            c = RGB_BLUE
            if dx == 1 and dy == 1:
                c = RED_D
            elif dx == 1 and dy == 0:
                c = WHITE
            img.putpixel((x0 + dx, y0 + dy), c)


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

    # ---- 机翼：下表面浅色 + 中翼黄黑带 + 外翼圆徽 ----
    for box in BOXES[5:8]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
    rc = face_regions(BOXES[5])
    for i, c in enumerate((BLACK, YELLOW, BLACK, YELLOW)):
        fill(img, (rc["down"][0] + i, rc["down"][1], 1, rc["down"][3]), c)
    roundel3(img, 24, 17)

    # ---- 水平尾翼 ----
    rt = face_regions(BOXES[8])
    fill(img, rt["down"], BLUE)
    fill(img, rt["up"], PALE)

    # ---- 机身：机腹浅色 + 后段黄黑带 + 圆徽 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], BLUE_D)
    fill(img, rf["south"], BLUE_D)
    fill(img, (0, 12, 1, 2.5), YELLOW)
    fill(img, (1, 12, 1, 2.5), BLACK)
    fill(img, (27, 12, 1, 2.5), BLACK)
    fill(img, (28, 12, 1, 2.5), YELLOW)
    roundel3(img, 3, 12)
    roundel3(img, 25, 12)

    # ---- 发动机罩 ----
    rcv = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rcv[key], BLUE)
    fill(img, rcv["north"], DARK)
    fill(img, rcv["south"], BLUE_D)
    fill(img, (rcv["west"][0], rcv["west"][1] + 2, rcv["west"][2], 1), BLUE_L)
    fill(img, (rcv["east"][0], rcv["east"][1] + 2, rcv["east"][2], 1), BLUE_L)

    # ---- 座舱罩 ----
    rcan = face_regions(BOXES[2])
    fill(img, rcan["down"], BLUE_L)
    fill(img, rcan["up"], BLUE_L)
    for fy in (1,):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (39, 4, 3.5, 0.7), BLUE_L)
    fill(img, (42.5, 4, 3.5, 0.7), BLUE_L)
    for fx in (39, 41, 43, 45, 47):
        fill(img, (fx, 4, 1, 0.7), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], BLUE_L)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], RED)
    fill(img, rs["north"], RED_D)

    # ---- 四叶螺旋桨 ----
    for x0 in (44, 47):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BLACK)

    # ---- 垂尾：前缘受光 ----
    rv = face_regions(BOXES[9])
    fill(img, rv["west"], BLUE)
    fill(img, rv["east"], BLUE)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), BLUE_L)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLUE_L)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_seafang_entity" if "--preview" in sys.argv else None,
         span=15.0)
    write_model("SeafangModel", "seafang", BOXES, """海毒牙（Seafang）实体模型。

<p>浅蓝灰椭圆翼、长机头、红色桨毂、英式圆徽、黄黑识别带。
机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
