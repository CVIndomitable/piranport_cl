# -*- coding: utf-8 -*-
"""Ju 87C 斯图卡实体贴图 + 三视图预览。

造型重点：倒海鸥翼（三段高度近似）、固定起落架轮罩、黄色发动机罩、
三叶螺旋桨、长条座舱罩。涂装是蓝灰 + 绿斑块的德军迷彩。
贴图与 Ju87cModel.java 的 BOXES 一一对应。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/ju87c.png"

BLUE = (108, 124, 146, 255)
BLUE_D = (74, 90, 112, 255)
BLUE_L = (132, 148, 166, 255)
PALE = (160, 176, 190, 255)      # 机腹浅蓝灰
GREEN = (92, 122, 96, 255)
GREEN_D = (58, 86, 64, 255)
YELLOW = (227, 208, 121, 255)    # 发动机罩
GLASS = (128, 160, 184, 255)
FRAME = (42, 50, 58, 255)
DARK = (38, 44, 50, 255)
BLACK = (22, 24, 26, 255)
CLEAR = (0, 0, 0, 0)

# (x, y, z, w, h, d, texOffsU, texOffsV)
BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.0, -3.0, -2.5, 2.0, 0.5, 4.0, 30, 0),      # 座舱罩
    (-0.5, -2.0, -6.8, 1.0, 1.0, 1.0, 44, 0),      # 桨毂
    (-1.5, -3.0, -6.7, 3.0, 3.0, 0.0, 48, 8),      # 三叶螺旋桨
    (-0.5, 0.0, -5.2, 1.0, 0.8, 1.5, 50, 0),       # 机腹滑油散热器
    # 倒海鸥翼三段：翼根高 → 折点低 → 外翼再抬高（左右对称共用 UV）
    (-2.5, -1.5, -2.6, 5.0, 0.1, 5.0, 0, 16),
    (-4.5, -0.9, -2.5, 2.0, 0.1, 5.0, 22, 16),
    (2.5, -0.9, -2.5, 2.0, 0.1, 5.0, 22, 16),
    (-7.5, -1.5, -2.5, 3.0, 0.1, 5.0, 38, 16),
    (4.5, -1.5, -2.5, 3.0, 0.1, 5.0, 38, 16),
    (-2.5, -1.6, 4.2, 5.0, 0.1, 2.0, 0, 22),       # 水平尾翼
    (0.0, -5.4, 4.5, 0.0, 3.0, 2.0, 56, 0),        # 垂尾
    # 固定起落架轮罩（左右共用 UV）
    (-3.2, -0.5, -2.5, 0.7, 1.2, 2.4, 56, 6),
    (2.5, -0.5, -2.5, 0.7, 1.2, 2.4, 56, 6),
    # 轮子
    (-3.1, 0.7, -2.0, 0.5, 0.4, 0.8, 60, 40),
    (2.6, 0.7, -2.0, 0.5, 0.4, 0.8, 60, 40),
]


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), BLUE)
    for cx, cy, rx, ry, c in [
        (8, 6, 9, 5, GREEN), (24, 10, 8, 6, GREEN_D), (40, 6, 8, 5, BLUE_D),
        (12, 20, 9, 4, GREEN), (34, 18, 8, 4, BLUE_L), (52, 14, 8, 5, BLUE_D),
        (20, 28, 9, 5, GREEN_D), (44, 30, 9, 5, BLUE_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机身：机腹浅蓝灰 + 黄色发动机罩 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], YELLOW)
    # 机头侧面 3 格黄罩（WEST 机头在 U 大端，EAST 在小端）
    fill(img, (9, 12, 3, 2.5), YELLOW)
    fill(img, (14, 12, 3, 2.5), YELLOW)
    # 机背最靠机头的两行刷黄
    fill(img, (rf["down"][0], rf["down"][1] + rf["down"][3] - 2, 2, 2), YELLOW)
    fill(img, rf["south"], BLUE_D)

    # ---- 机翼：下表面浅蓝灰；上表面蓝灰/绿迷彩已由底色给出 ----
    for box in BOXES[5:10]:
        r = face_regions(box)
        fill(img, r["up"], PALE)

    # ---- 水平尾翼下表面 ----
    rt = face_regions(BOXES[10])
    fill(img, rt["up"], PALE)

    # ---- 座舱罩 ----
    rc = face_regions(BOXES[1])
    fill(img, rc["down"], GLASS)
    fill(img, rc["up"], GLASS)
    for fy in (1, 3):
        fill(img, (rc["down"][0], rc["down"][1] + fy, rc["down"][2], 1), FRAME)
        fill(img, (rc["up"][0], rc["up"][1] + fy, rc["up"][2], 1), FRAME)
    fill(img, (30, 4, 4, 0.5), GLASS)
    fill(img, (34, 4, 4, 0.5), GLASS)
    for fx in (30, 32, 34, 36, 38, 40):
        fill(img, (fx, 4, 1, 0.5), FRAME)
    fill(img, rc["north"], FRAME)
    fill(img, rc["south"], GLASS)

    # ---- 桨毂：深灰，前缘一圈黄 ----
    rs = face_regions(BOXES[2])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], DARK)
    fill(img, rs["north"], YELLOW)

    # ---- 三叶螺旋桨：透明底 + Y 形桨叶 ----
    for x0 in (48, 51):          # north / south 两个 3×3 面都要画
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BLACK)
        # 去掉右臂，形成三叶桨
        img.putpixel((x0 + 2, 9), CLEAR)

    # ---- 滑油散热器：灰罩 + 深色进气口 ----
    rno = face_regions(BOXES[4])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rno[key], DARK)
    fill(img, rno["north"], BLACK)

    # ---- 垂尾：前缘受光 ----
    rv = face_regions(BOXES[11])
    fill(img, (rv["west"][0] + 1, rv["west"][1], 1, rv["west"][3]), BLUE_L)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLUE_L)

    # ---- 轮罩：绿；轮子：黑 ----
    rsp = face_regions(BOXES[12])
    for key in ("down", "up", "west", "east", "north", "south"):
        fill(img, rsp[key], GREEN_D)
    fill(img, (60, 40, 4, 4), BLACK)

    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_ju87c_entity" if "--preview" in sys.argv else None,
         span=17.0)
    write_model("Ju87cModel", "ju87c", BOXES, """Ju 87C 斯图卡实体模型。

<p>造型重点：倒海鸥翼用三段不同高度的翼面近似、固定起落架轮罩、
黄色发动机罩、三叶螺旋桨、长条座舱罩。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
