#!/usr/bin/env python3
"""重画「海喷火」（Supermarine Seafire）贴图。

构图依据 zjsnrwiki 原画（Equip_L_57.png）：英制舰载战斗机侧视 ——
机头三叶螺旋桨、深色框架座舱盖、椭圆机翼向右下展开（翼面带皇家海军
蓝/红/白三色圆徽）、右端平尾。区别于旧 F4F 野猫的宽机头平直翼构图。

画风对齐 f4_f_wildcat / c1_recon：纯黑最外侧描边 + 冷灰铝合金色阶，
机身占 y=6..21，底座留空一格后绘制（系列统一的图标托架）。
输出 32x32 RGBA，同时产出（装填）与（空膛）两版。
"""
from PIL import Image

W = H = 32

# ---- 调色板（取自现有飞机贴图，保持系列一致）----
OUTLINE = (0, 0, 0, 255)
HI      = (232, 236, 240, 255)   # E8ECF0 机身顶部高光
LIGHT   = (198, 206, 214, 255)   # C6CED6 受光铝合金
MID     = (164, 174, 184, 255)   # A4AEB8 机身主色
SHADE   = (126, 136, 148, 255)   # 7E8894 机身背光/下缘
DARK    = (86, 95, 106, 255)     # 565F6A 机腹阴影
GLASS   = (58, 90, 116, 255)     # 3A5A74 座舱盖防眩玻璃
CANOPY  = (44, 52, 60, 255)      # 2C343C 座舱框架
BLUE    = (38, 59, 138, 255)     # 263B8A 圆徽外环（皇家海军蓝）
WHITE   = (238, 240, 242, 255)   # EEF0F2 圆徽中环
RED     = (170, 38, 34, 255)     # AA2622 圆徽内点
PROP    = (52, 58, 66, 255)      # 343A42 螺旋桨

# ---- 底座（系列统一的图标托架，13x5 @ y=26..31）----
PED_TOP = (138, 146, 154, 255)   # 8A929A
PED_MID = (104, 112, 122, 255)   # 68707A
PED_LOW = (72, 79, 88, 255)      # 484F58


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


def line(im, x0, y0, x1, y1, c):
    """Bresenham 直线，用于机翼前后缘与尾翼斜边。"""
    dx, dy = abs(x1 - x0), abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        px(im, x0, y0, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


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


def draw_wings(im):
    """椭圆机翼：后缘自翼根向右下展开，翼尖椭圆收尖。

    原画中机翼是翼展方向向右下斜伸的，这里用后缘斜线 + 前缘近水平线
    围出椭圆翼型；翼展方向 8px 用以容纳外侧圆徽。
    """
    line(im, 13, 15, 24, 18, MID)      # 后缘（自翼根斜下到翼尖）
    line(im, 12, 11, 23, 13, LIGHT)    # 前缘（近水平）
    # 逐列填充翼面，使前缘到后缘之间成为实心
    for x in range(12, 25):
        t = (x - 12) / 12.0
        top = int(round(11 + t * 2))
        bot = int(round(15 + t * 3))
        for y in range(top, bot + 1):
            px(im, x, y, MID)
        # 翼根与翼中段加一道受光带，翼尖压暗
        px(im, x, top, LIGHT)
        px(im, x, bot, SHADE)
    # 翼尖椭圆收尖：右侧两列收窄
    px(im, 25, 15, MID)
    px(im, 25, 16, SHADE)
    px(im, 26, 16, MID)


def draw_roundel(im, cx, cy):
    """皇家海军 FAA 圆徽：外蓝环 / 中白环 / 内红点。

    切角后落在翼面上（翼面仅 5px 高），因此只画 5x5 的蓝白红同心方块：
    四角用 WHITE 而非翼面色，避免在深蓝上留下突兀的灰点。
    """
    rect(im, cx - 2, cy - 2, cx + 2, cy + 2, BLUE)
    rect(im, cx - 1, cy - 1, cx + 1, cy + 1, WHITE)
    px(im, cx, cy, RED)


def draw_seafire(loaded: bool):
    """画一架侧视海喷火，机头朝右。

    loaded=False 时空膛（螺旋桨静止、桨叶十字可见），
    loaded=True 时螺旋桨高速旋转（模糊桨盘），以示「已启动」。
    """
    im = new_canvas()

    # ---- 1) 机翼 wing：向右下展开的椭圆翼（先画翼，机身压在上层）----
    draw_wings(im)
    draw_roundel(im, 21, 17)

    # ---- 2) 机身 fuselage：机头略尖、机尾收细的水平长条 ----
    rect(im, 9, 12, 22, 15, MID)
    rect(im, 9, 12, 22, 12, LIGHT)     # 背部受光
    rect(im, 9, 15, 22, 15, SHADE)     # 腹部背光
    # 机头整流罩：向右收尖
    px(im, 23, 12, LIGHT)
    px(im, 23, 13, MID)
    px(im, 23, 14, MID)
    px(im, 23, 15, SHADE)
    # 机尾：向左收细成尾锥
    rect(im, 6, 13, 8, 14, MID)
    rect(im, 6, 13, 8, 13, LIGHT)
    px(im, 5, 13, SHADE)
    px(im, 5, 14, SHADE)

    # ---- 3) 座舱盖 canopy：机身中段前的深色框架玻璃 ----
    rect(im, 17, 10, 20, 12, CANOPY)
    rect(im, 18, 10, 19, 11, GLASS)
    px(im, 20, 10, CANOPY)
    px(im, 17, 10, SHADE)

    # ---- 4) 尾翼 tailplane：机尾上下的小型平尾 ----
    rect(im, 6, 11, 8, 11, LIGHT)
    rect(im, 6, 16, 8, 16, SHADE)
    # 垂直安定面
    rect(im, 6, 11, 6, 16, MID)

    # ---- 5) 螺旋桨 propeller：机头前方的桨叶/桨盘 ----
    rect(im, 24, 10, 24, 14, PROP)     # 桨毂/桨叶根部（两态共有）
    if loaded:
        # 高速旋转：桨盘用高光 + 两道桨叶残影表现，而非纯亮柱
        rect(im, 25, 9, 25, 16, HI)
        px(im, 26, 10, HI)
        px(im, 26, 14, HI)
        px(im, 25, 12, MID)
    else:
        # 静止：三叶十字
        rect(im, 24, 16, 25, 16, PROP)
        px(im, 25, 12, PROP)
        px(im, 25, 15, PROP)

    # ---- 6) 图标底座（与系列其它飞机贴图一致：13x6 @ y=26..31）----
    rect(im, 9, 25, 21, 25, PED_LOW)   # 顶缘留一行暗带，与机身分开
    rect(im, 9, 26, 21, 27, PED_TOP)
    rect(im, 9, 28, 21, 29, PED_MID)
    rect(im, 9, 30, 21, 31, PED_LOW)

    return outline_silhouette(im)


def main():
    base = "src/main/resources/assets/piranport/textures/item"
    draw_seafire(loaded=False).save(f"{base}/seafire_empty.png")
    draw_seafire(loaded=True).save(f"{base}/seafire.png")
    print("wrote seafire.png / seafire_empty.png")


if __name__ == "__main__":
    main()
