# -*- coding: utf-8 -*-
"""天袭者（A-1 Skyraider + 南瓜头）实体贴图 + 三视图预览。

原画是万圣节版：座舱位置顶着一个戴女巫帽的南瓜头。南瓜按"原版 carved
pumpkin"的思路画成方形橙块 + 黑刻脸，不追求原画的圆润插画感。
贴图与 SkyraiderModel.java 的 BOXES 同源。
"""
import sys

from PIL import Image

from aircraft_model_preview import TEX, blob, fill, save, face_regions
from java_model import write_model

OUT = "src/main/resources/assets/piranport/textures/entity/skyraider.png"

SILVER = (172, 176, 176, 255)
SILVER_D = (130, 134, 136, 255)
PALE = (158, 168, 178, 255)
ORANGE = (228, 138, 52, 255)
ORANGE_D = (186, 102, 36, 255)
BLACK = (28, 28, 30, 255)
DARK = (48, 48, 52, 255)
PURPLE = (92, 66, 110, 255)
GREEN = (86, 116, 74, 255)
FRAME = (50, 54, 60, 255)
GLASS = (150, 180, 200, 255)
WHITE = (235, 235, 232, 255)
CLEAR = (0, 0, 0, 0)

BOXES = [
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),      # 机身
    (-1.1, -2.7, -7.0, 2.2, 2.6, 1.6, 30, 0),      # 星形发动机罩
    (-0.5, -2.0, -7.4, 1.0, 1.0, 1.0, 44, 0),      # 桨毂
    (-1.5, -3.0, -7.3, 3.0, 3.0, 0.0, 38, 0),      # 四叶螺旋桨
    (-2.0, -6.5, -1.8, 4.0, 4.0, 4.0, 32, 8),      # 南瓜头
    (-0.15, -6.9, -0.15, 0.3, 0.4, 0.3, 48, 8),    # 瓜蒂
    (-2.0, -7.2, -1.0, 4.0, 0.3, 2.0, 48, 10),     # 女巫帽帽檐
    (-1.4, -8.4, -0.6, 2.8, 1.2, 1.2, 48, 13),     # 帽身
    (-0.8, -9.2, -0.2, 1.6, 0.8, 0.4, 56, 13),     # 帽尖
    (-2.5, -1.2, -2.8, 5.0, 0.1, 5.0, 0, 17),      # 中翼
    (-7.5, -1.2, -2.6, 5.0, 0.1, 5.0, 22, 17),     # 左外翼
    (2.5, -1.2, -2.6, 5.0, 0.1, 5.0, 22, 17),      # 右外翼（共用 UV）
    (-2.5, -1.6, 4.2, 5.0, 0.1, 2.0, 0, 23),       # 水平尾翼
    (0.0, -5.6, 4.5, 0.0, 3.0, 2.0, 60, 8),        # 垂尾
    # 主起落架 + 轮子
    (-1.9, -0.2, -1.2, 0.3, 1.2, 0.3, 60, 40),
    (1.6, -0.2, -1.2, 0.3, 1.2, 0.3, 60, 40),
    (-2.1, 0.9, -1.4, 0.7, 0.7, 0.9, 60, 40),
    (1.4, 0.9, -1.4, 0.7, 0.7, 0.9, 60, 40),
    (-0.1, 0.0, 5.6, 0.2, 0.4, 0.4, 60, 40),       # 尾轮
    # 翼下挂弹
    (-4.8, -0.6, -1.4, 0.6, 0.6, 2.0, 52, 20),
    (4.2, -0.6, -1.4, 0.6, 0.6, 2.0, 52, 20),
]


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), SILVER)
    for cx, cy, rx, ry, c in [
        (10, 6, 9, 5, ORANGE), (26, 10, 8, 5, SILVER_D), (46, 6, 8, 5, ORANGE),
        (14, 20, 9, 4, ORANGE_D), (34, 24, 8, 4, SILVER_D), (54, 34, 8, 5, ORANGE_D),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # ---- 机翼：上表面银灰/橙黑，下表面浅灰 ----
    for box in BOXES[9:12]:
        r = face_regions(box)
        fill(img, r["down"], SILVER)
        fill(img, r["up"], PALE)
    ro = face_regions(BOXES[10])
    # 外翼一大块橙色 + 黑色前缘条
    fill(img, (ro["down"][0], ro["down"][1], 5, 3), ORANGE)
    fill(img, (ro["down"][0], ro["down"][1] + ro["down"][3] - 1, 5, 1), BLACK)
    # 翼上橙色星
    for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
        img.putpixel((28 + dx, 18 + dy), ORANGE_D)

    # ---- 水平尾翼 ----
    rt = face_regions(BOXES[12])
    fill(img, rt["down"], SILVER)
    fill(img, rt["up"], PALE)

    # ---- 机身：机腹浅色 + 侧面橙黑装饰 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 13, 12, 1.5), PALE)
    fill(img, (14, 13, 12, 1.5), PALE)
    fill(img, rf["north"], SILVER_D)
    fill(img, rf["south"], DARK)
    fill(img, (1, 12, 3, 2), ORANGE)
    fill(img, (24, 12, 3, 2), ORANGE)
    fill(img, (0, 12, 1, 2.5), BLACK)
    fill(img, (27, 12, 1, 2.5), BLACK)

    # ---- 发动机罩 ----
    rc = face_regions(BOXES[1])
    for key in ("down", "up", "west", "east"):
        fill(img, rc[key], SILVER)
    fill(img, rc["north"], DARK)
    fill(img, rc["south"], SILVER_D)
    fill(img, (rc["west"][0], rc["west"][1] + 2, rc["west"][2], 1), BLACK)
    fill(img, (rc["east"][0], rc["east"][1] + 2, rc["east"][2], 1), BLACK)

    # ---- 桨毂 ----
    rs = face_regions(BOXES[2])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rs[key], DARK)
    fill(img, rs["north"], ORANGE)

    # ---- 四叶螺旋桨 ----
    for x0 in (38, 41):
        fill(img, (x0, 0, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 0 + dy), BLACK)

    # ---- 南瓜头：橙色 + 深橙竖棱 + 正面刻脸 ----
    rp = face_regions(BOXES[4])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rp[key], ORANGE)
    # 竖棱
    for key in ("west", "east"):
        for i in (0, 2):
            fill(img, (rp[key][0] + i, rp[key][1], 1, rp[key][3]), ORANGE_D)
    fill(img, rp["north"], ORANGE)
    # 4×4 刻脸：两只竖眼 + 鼻子 + 嘴
    face = {
        (0, 0): BLACK, (3, 0): BLACK,
        (0, 1): BLACK, (3, 1): BLACK,
        (1, 2): BLACK, (2, 2): BLACK,
        (0, 3): BLACK, (1, 3): BLACK, (2, 3): BLACK, (3, 3): BLACK,
    }
    for (dx, dy), c in face.items():
        img.putpixel((int(rp["north"][0]) + dx, int(rp["north"][1]) + dy), c)

    # ---- 瓜蒂 ----
    rstem = face_regions(BOXES[5])
    for key in ("down", "up", "west", "east", "north", "south"):
        fill(img, rstem[key], GREEN)

    # ---- 女巫帽：黑 + 紫色帽带 ----
    for box in BOXES[6:9]:
        rb = face_regions(box)
        for key in ("down", "up", "west", "east", "north", "south"):
            fill(img, rb[key], BLACK)
    rbrim = face_regions(BOXES[6])
    fill(img, (rbrim["west"][0], rbrim["west"][1], rbrim["west"][2], 1), PURPLE)
    fill(img, (rbrim["east"][0], rbrim["east"][1], rbrim["east"][2], 1), PURPLE)

    # ---- 垂尾：橙色前缘 + 黑色后缘 ----
    rv = face_regions(BOXES[13])
    fill(img, rv["west"], ORANGE)
    fill(img, rv["east"], ORANGE)
    fill(img, (rv["west"][0], rv["west"][1], 1, rv["west"][3]), BLACK)
    fill(img, (rv["east"][0] + 1, rv["east"][1], 1, rv["east"][3]), BLACK)

    # ---- 挂弹：银灰 + 橙头 ----
    rbomb = face_regions(BOXES[19])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, rbomb[key], SILVER_D)
    fill(img, rbomb["north"], ORANGE)

    # ---- 起落架/轮子 ----
    fill(img, (60, 40, 4, 4), DARK)

    return img


def main():
    img = draw_texture()
    save(img, BOXES, OUT, "/tmp/preview_skyraider_entity" if "--preview" in sys.argv else None,
         span=18.0)
    write_model("SkyraiderModel", "skyraider", BOXES, """天袭者（A-1 Skyraider + 南瓜头）实体模型。

<p>原画是万圣节版：座舱位置顶着一个戴女巫帽的南瓜头。南瓜按原版 carved
pumpkin 的思路画成方形橙块 + 黑刻脸。机头朝 -Z，y 轴向下。""")


if __name__ == "__main__":
    main()
