# -*- coding: utf-8 -*-
"""F4U-7 海盗实体贴图 + 三视图预览。

造型重点：倒海鸥翼（三段高度）、长机头大星形发动机、气泡座舱、
法国三色圆徽与苏伊士黄黑识别带、三色垂尾。
贴图与 F4u7Model.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/f4u7.png"

BLUE = (96, 112, 128, 255)
BLUE_D = (62, 78, 96, 255)
BLUE_L = (128, 144, 160, 255)
PALE = (160, 176, 192, 255)
YELLOW = (240, 224, 64, 255)
BLACK = (26, 28, 32, 255)
WHITE = (238, 242, 244, 255)
RED = (196, 62, 54, 255)
RGB_BLUE = (42, 66, 148, 255)
GLASS = (140, 168, 190, 255)
FRAME = (44, 54, 66, 255)
DARK = (40, 46, 56, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.2, -2.9, -6.8, 2.4, 2.9, 1.8, 30, 0),      # 星形发动机罩
    (-1.0, -3.2, -2.5, 2.0, 0.7, 3.5, 40, 0),      # 气泡座舱罩
    (-0.5, -2.0, -7.3, 1.0, 1.0, 1.0, 52, 0),      # 桨毂
    (-1.5, -3.0, -7.2, 3.0, 3.0, 0.0, 44, 8),      # 四叶螺旋桨
    # 倒海鸥翼三段
    (-2.0, -1.4, -2.8, 4.0, 0.1, 5.0, 0, 16),
    (-4.5, -0.8, -2.7, 2.5, 0.1, 5.0, 20, 16),
    (2.0, -0.8, -2.7, 2.5, 0.1, 5.0, 20, 16),
    (-7.5, -1.4, -2.7, 3.0, 0.1, 5.0, 37, 16),
    (4.5, -1.4, -2.7, 3.0, 0.1, 5.0, 37, 16),
    (-2.5, -1.6, 4.2, 5.0, 0.1, 2.0, 0, 22),       # 水平尾翼
    (0.0, -5.6, 4.5, 0.0, 3.0, 2.0, 56, 4),        # 垂尾
    # 翼下火箭 ×4（共用 UV）
    (-6.5, -0.5, -1.8, 0.3, 0.3, 1.4, 52, 12),
    (-5.5, -0.5, -1.8, 0.3, 0.3, 1.4, 52, 12),
    (5.2, -0.5, -1.8, 0.3, 0.3, 1.4, 52, 12),
    (6.2, -0.5, -1.8, 0.3, 0.3, 1.4, 52, 12),
    (-0.1, 0.0, 5.6, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
]


def roundel3(img, x0, y0):
    """3×3 法国圆徽：蓝底 + 白环 + 红芯（避免画成棋盘格）。"""
    for dy in range(3):
        for dx in range(3):
            c = RGB_BLUE
            if dx == 1 and dy == 1:
                c = RED
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

    # ---- 机翼：下表面浅色；上表面蓝灰 + 苏伊士黄黑带 + 圆徽 ----
    for box in BOXES[5:10]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
    # 中翼一条黄带 + 黑边
    rc = face_regions(BOXES[5])
    fill(img, (rc["down"][0] + 1, rc["down"][1], 1, rc["down"][3]), BLACK)
    fill(img, (rc["down"][0] + 2, rc["down"][1], 1, rc["down"][3]), YELLOW)
    # 外翼两段黄黑带
    rm = face_regions(BOXES[6])
    fill(img, (rm["down"][0], rm["down"][1], 1, rm["down"][3]), YELLOW)
    fill(img, (rm["down"][0] + 1, rm["down"][1], 1, rm["down"][3]), BLACK)
    rt = face_regions(BOXES[7])
    fill(img, (rt["down"][0], rt["down"][1], 1, rt["down"][3]), YELLOW)
    fill(img, (rt["down"][0] + 1, rt["down"][1], 1, rt["down"][3]), BLACK)
    roundel3(img, 42, 18)

    # ---- 水平尾翼 ----
    rtail = face_regions(BOXES[9])
    fill(img, rtail["up"], PALE)

    # ---- 机身：机腹浅色 + 后段黄黑识别带 + 圆徽 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], BLUE_D)
    fill(img, rf["south"], BLUE_D)
    # 尾部环绕的黄黑带（WEST 机尾 U 小端，EAST 机尾 U 大端）
    fill(img, (0, 12, 1, 2.5), YELLOW)
    fill(img, (1, 12, 1, 2.5), BLACK)
    fill(img, (27, 12, 1, 2.5), BLACK)
    fill(img, (28, 12, 1, 2.5), YELLOW)
    roundel3(img, 4, 12)
    roundel3(img, 25, 12)

    # ---- 发动机罩 ----
    rcv = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rcv[key], BLUE)
    fill(img, rcv["north"], DARK)
    fill(img, rcv["south"], BLUE_D)
    fill(img, (rcv["west"][0], rcv["west"][1] + 2, rcv["west"][2], 1), BLUE_L)
    fill(img, (rcv["east"][0], rcv["east"][1] + 2, rcv["east"][2], 1), BLUE_L)

    # ---- 气泡座舱罩 ----
    rcan = face_regions(BOXES[2])
    fill(img, rcan["down"], GLASS)
    fill(img, rcan["up"], GLASS)
    for fy in (1, 3):
        fill(img, (rcan["down"][0], rcan["down"][1] + fy, rcan["down"][2], 1), FRAME)
        fill(img, (rcan["up"][0], rcan["up"][1] + fy, rcan["up"][2], 1), FRAME)
    fill(img, (40, 4, 3.5, 0.7), GLASS)
    fill(img, (43.5, 4, 3.5, 0.7), GLASS)
    for fx in (40, 42, 44, 46, 48, 50):
        fill(img, (fx, 4, 1, 0.7), FRAME)
    fill(img, rcan["north"], FRAME)
    fill(img, rcan["south"], GLASS)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], DARK)
    fill(img, rs["north"], BLUE_L)

    # ---- 四叶螺旋桨：黄尖 ----
    for x0 in (44, 47):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BLACK)
        for dx, dy in ((1, 0), (0, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), YELLOW)

    # ---- 垂尾：蓝红三色（2 格宽，白色省略）----
    rv = face_regions(BOXES[10])
    fill(img, (rv["west"][0], rv["west"][1], 1, rv["west"][3]), RGB_BLUE)
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), RED)
    fill(img, (rv["east"][0], rv["east"][1], 1, rv["east"][3]), RGB_BLUE)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), RED)

    # ---- 翼下火箭：白 ----
    rr = face_regions(BOXES[11])
    for key in ("down", "up", "west", "east", "north", "south"):
        fill(img, rr[key], WHITE)

    fill(img, (60, 40, 4, 4), BLACK)
    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_f4u7_entity" if "--preview" in sys.argv else None,
         span=17.0)
    write_model("F4u7Model", "f4u7", BOXES, """F4U-7 海盗实体模型。

<p>倒海鸥翼用三段高度近似、长机头大星形发动机、气泡座舱、
法国三色圆徽与苏伊士黄黑识别带。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
