#!/usr/bin/env python3
"""重画「景云改」轰炸机贴图。

构图依据 zjsnrwiki 原画（文件:Equip_L_211.png）：双发、平直翼陆基水平轰炸机，
3/4 斜俯视 —— 机翼沿「左上—右下」对角线展开，机头朝右上角，双发动机舱
前伸到机翼前缘之外，圆头 + 多格座舱罩，机身涂装为暗橄榄绿，机翼前缘有一条
偏亮的浅绿高光带，翼尖发白，左翼一枚红白圆形国籍标。

画风对齐同级飞机贴图：16x16 RGBA、纯黑最外侧八邻域描边、不超过 8 色。
色板取自原画本身（暗橄榄绿机身 / 浅绿前缘 / 米白翼尖 / 红白标），
不用旧占位贴图，也不用 xa2j_bomber 那套紫灰海军迷彩（那是另一条线的涂装）。
输出（空膛）与（装填）两版。
"""
from PIL import Image

W = H = 16

# ---- 调色板（取样自 zjsnrwiki 原画 /tmp/ref_crop.png）----
OUTLINE = (0, 0, 0, 255)          # 纯黑最外侧描边
SHADOW  = (42, 54, 46, 255)       # 2A362E 背光面/机身下缘
BODY    = (60, 76, 66, 255)       # 3C4C42 机身暗橄榄绿（主色）
WING    = (81, 107, 86, 255)      # 516B56 机翼顶面
LIT     = (99, 126, 113, 255)     # 637E71 受光面
EDGE    = (146, 186, 162, 255)    # 92BAA2 机翼前缘高光带
TIP     = (222, 230, 220, 255)    # DEE6DC 翼尖米白
RED     = (192, 58, 58, 255)      # C03A3A 左翼国籍标红
PALE    = (238, 240, 236, 255)    # EEF0EC 国籍标白芯 / 座舱罩高光


def new_canvas():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), c)


def rect(im, x0, y0, x1, y1, c):
    """含端点的实心矩形。"""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)


def outline_silhouette(im):
    """给已有像素的最外圈补纯黑描边（八邻域），不改动已有像素本身。"""
    solid = {(x, y) for y in range(H) for x in range(W) if im.getpixel((x, y))[3] > 0}
    edge = []
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < W and 0 <= ny < H):
                    continue
                if (nx, ny) not in solid:
                    edge.append((nx, ny))
    for (x, y) in edge:
        px(im, x, y, OUTLINE)
    return im


def draw_bomber(fueled: bool):
    """画一架斜置 3/4 俯视的景云改。

    坐标构图（左上为原点）：
      机翼   沿 (2,4)-(5,6) 到 (11,11)-(14,9) 的对角线展开
      机身   从 (5,6) 的圆机头沿对角线走到 (11,11) 的尾锥
      尾翼   机身尾部右下的水平尾翼，加一侧垂直安定面
      发动机 两台前伸到机翼前缘之外的短舱（原画的双发特征）
    fueled=False 时座舱罩压暗（无人/未加注），fueled=True 时亮起。
    """
    im = new_canvas()

    # ---- 1) 机翼：左上翼 + 右下翼，斜置贯通 ----
    rect(im, 2, 4, 5, 5, WING)         # 左上翼外段
    rect(im, 3, 4, 6, 6, WING)         # 左上翼根部
    rect(im, 9, 9, 13, 10, WING)       # 右下翼
    rect(im, 10, 10, 14, 11, WING)     # 右下翼外段

    # ---- 2) 机翼前缘高光带（原画最醒目的浅绿亮带）----
    rect(im, 2, 4, 5, 4, EDGE)         # 左上翼前缘
    rect(im, 10, 10, 14, 10, EDGE)     # 右下翼前缘

    # ---- 3) 翼尖：米白 ----
    rect(im, 2, 4, 2, 5, TIP)
    rect(im, 14, 10, 14, 11, TIP)

    # ---- 4) 左翼国籍标：红环白芯（原画红白圆形标）----
    rect(im, 4, 5, 5, 5, RED)
    px(im, 5, 5, PALE)

    # ---- 5) 机身：暗橄榄绿，比机翼暗一档 ----
    rect(im, 5, 6, 7, 7, BODY)         # 机头段
    rect(im, 6, 6, 8, 8, BODY)
    rect(im, 8, 8, 10, 10, BODY)       # 尾段

    # ---- 6) 座舱罩：圆机头之后的玻璃格 ----
    rect(im, 6, 6, 7, 6, PALE)         # 罩体
    if not fueled:
        rect(im, 6, 6, 6, 6, OUTLINE)  # 未加注时压暗一格，区分两版

    # ---- 7) 发动机短舱：两台，前伸到机翼前缘之外 ----
    # 左发：沿翼展向前上探出，读出「双发」而非单发
    rect(im, 4, 3, 5, 4, LIT)
    rect(im, 4, 3, 4, 4, EDGE)
    # 右发：对称地探到右下翼前缘之外
    rect(im, 12, 11, 13, 12, LIT)
    px(im, 13, 12, SHADOW)

    # ---- 8) 尾翼：机身尾部右下的水平尾翼 + 垂直安定面 ----
    rect(im, 10, 11, 13, 12, WING)     # 水平尾翼
    rect(im, 10, 11, 13, 11, EDGE)     # 尾翼前缘受光
    rect(im, 12, 12, 13, 13, BODY)     # 垂直安定面/方向舵

    # ---- 9) 机身下缘压暗，避免与机翼糊成一块 ----
    rect(im, 8, 10, 10, 10, SHADOW)
    rect(im, 6, 8, 8, 8, SHADOW)

    return outline_silhouette(im)


def main():
    base = "src/main/resources/assets/piranport/textures/item"
    draw_bomber(fueled=False).save(f"{base}/seiun_kai_bomber_empty.png")
    draw_bomber(fueled=True).save(f"{base}/seiun_kai_bomber.png")
    print("wrote seiun_kai_bomber.png / _empty.png")


if __name__ == "__main__":
    main()
