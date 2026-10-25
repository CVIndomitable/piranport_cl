# -*- coding: utf-8 -*-
"""BTD-1 毁灭者实体贴图 + 三视图预览。

造型重点：单发大直径星形发动机罩、气泡座舱罩、低置直翼；
深蓝涂装 + 白色星徽。贴图与 BtdDestroyerModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/btd_destroyer.png"

BLUE = (48, 80, 128, 255)
BLUE_D = (32, 56, 96, 255)
BLUE_M = (64, 104, 152, 255)
BLUE_L = (84, 124, 172, 255)
PALE = (150, 168, 184, 255)
SILVER = (150, 158, 166, 255)
SILVER_D = (92, 100, 110, 255)
DARK = (38, 44, 52, 255)
WHITE = (230, 238, 248, 255)
GLASS = (140, 165, 190, 255)
FRAME = (40, 50, 62, 255)
BLACK = (20, 22, 26, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.1, -2.8, -7.0, 2.2, 2.8, 1.8, 30, 0),      # 星形发动机罩
    (-1.0, -3.2, -2.8, 2.0, 0.7, 4.5, 40, 0),      # 气泡座舱罩
    (-0.5, -2.05, -7.4, 1.0, 1.0, 1.0, 54, 0),     # 桨毂
    (-1.5, -3.0, -7.3, 3.0, 3.0, 0.0, 44, 8),      # 四叶螺旋桨
    (-0.6, 0.0, -5.6, 1.2, 0.6, 1.5, 56, 4),       # 机腹进气口
    # 低置直翼：中翼 + 左右外翼（外翼共用 UV）
    (-2.5, -1.2, -2.6, 5.0, 0.1, 5.0, 0, 16),
    (-7.5, -1.2, -2.5, 5.0, 0.1, 5.0, 22, 16),
    (2.5, -1.2, -2.5, 5.0, 0.1, 5.0, 22, 16),
    (-2.5, -1.6, 4.2, 5.0, 0.1, 2.0, 0, 22),       # 水平尾翼
    (0.0, -5.6, 4.5, 0.0, 3.0, 2.0, 44, 12),       # 垂尾
    (-0.1, 0.0, 5.8, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def star(img, x0, y0):
    """3×3 白星徽：白点 + 蓝芯，避免正十字被读成德军十字。"""
    for dx, dy in ((1, 0), (0, 1), (2, 1), (1, 2)):
        img.putpixel((x0 + dx, y0 + dy), WHITE)
    img.putpixel((x0 + 1, y0 + 1), BLUE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), BLUE)
    for cx, cy, rx, ry, c in [
        (8, 6, 9, 5, BLUE_M), (24, 8, 8, 5, BLUE_D), (40, 10, 8, 5, BLUE_L),
        (12, 20, 9, 4, BLUE_D), (34, 22, 8, 4, BLUE_M), (52, 18, 8, 5, BLUE_L),
        (18, 30, 9, 5, BLUE_M), (44, 32, 9, 5, BLUE_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机身：机腹浅色、尾端深蓝 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], BLUE_D)
    fill(img, rf["south"], BLUE_D)
    star(img, 2, 12)
    star(img, 26, 12)

    # ---- 机翼：下表面浅色、翼尖星徽 ----
    for box in BOXES[6:9]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
    star(img, 28, 18)

    # ---- 水平尾翼下表面 ----
    rt = face_regions(BOXES[9])
    fill(img, rt["up"], PALE)

    # ---- 发动机罩：银灰圆筒，正面深色进气口 ----
    rc = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rc[key], SILVER)
    fill(img, rc["north"], DARK)
    fill(img, rc["south"], SILVER_D)
    fill(img, (rc["west"][0], rc["west"][1] + 2, rc["west"][2], 1), SILVER_D)
    fill(img, (rc["east"][0], rc["east"][1] + 2, rc["east"][2], 1), SILVER_D)

    # ---- 气泡座舱罩 ----
    rcan = face_regions(BOXES[2])
    fill(img, rcan["down"], GLASS)
    fill(img, rcan["up"], GLASS)
    for fy in (1, 3):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (40, 4, 4.5, 0.7), GLASS)
    fill(img, (44.5, 4, 4.5, 0.7), GLASS)
    for fx in (40, 42, 44, 46, 48, 50):
        fill(img, (fx, 4, 1, 0.7), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], GLASS)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], SILVER)
    fill(img, rs["north"], DARK)

    # ---- 四叶螺旋桨 ----
    for x0 in (44, 47):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BLACK)

    # ---- 机腹进气口 ----
    ri = face_regions(BOXES[5])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, ri[key], DARK)
    fill(img, ri["north"], BLACK)

    # ---- 垂尾：前缘受光 ----
    rv = face_regions(BOXES[10])
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), BLUE_L)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLUE_L)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_btd_entity" if "--preview" in sys.argv else None,
         span=17.0)
    write_model("BtdDestroyerModel", "btd_destroyer", BOXES, """BTD-1 毁灭者实体模型。

<p>单发大直径星形发动机罩、气泡座舱罩、低置直翼，深蓝涂装 + 白色星徽。
机头朝 -Z，y 轴向下；机翼/平尾用 0.1 格厚度避免上下异色时 z-fighting。""")


if __name__ == "__main__":
    main()
