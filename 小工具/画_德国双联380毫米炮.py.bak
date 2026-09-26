#!/usr/bin/env python3
"""重画「德国双联380毫米炮」贴图。

构图依据 zjsnrwiki 原画：德式双联装炮塔 —— 两门炮管并排、方形/斜角装甲炮室、
下方座圈（barbette）。区别于旧「法国四联380毫米炮」的四管并排构图。

画风对齐 medium_gun / large_gun：纯黑最外侧描边 + 灰蓝装甲色阶。
输出 32x32 RGBA，同时产出（装填）与（空膛）两版。
"""
from PIL import Image

W = H = 32

# ---- 调色板（取自现有火炮贴图，保持系列一致）----
OUTLINE = (0, 0, 0, 255)
HI      = (214, 218, 220, 255)   # D6DADC 高光
TOP     = (181, 190, 196, 255)   # 炮塔顶面
LIGHT   = (133, 152, 164, 255)   # 8598A4 受光面
MID     = (106, 128, 145, 255)   # 6A8091 侧面
DARK    = (67, 81, 91, 255)      # 43515B 背光面
DEEP    = (39, 49, 56, 255)      # 273138 炮口/腔体
BRASS   = (189, 159, 90, 255)    # BD9F5A 装填弹体黄铜
BRASS_D = (115, 93, 44, 255)     # 735D2C 黄铜暗部


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


def draw_turret(loaded: bool):
    """画一座德式双联装炮塔。

    loaded=False 时空膛（炮口为深腔、无弹），loaded=True 时炮口露出黄铜弹体。
    结构自下而上：座圈 -> 炮室（顶面斜切）-> 两根并列粗炮管。
    单像素坐标；整体略偏右下以贴近原画构图。
    """
    im = new_canvas()

    # ---- 1) 座圈 barbette：炮塔下方的梯形基座 ----
    rect(im, 11, 25, 19, 26, MID)
    rect(im, 10, 26, 20, 27, LIGHT)
    rect(im, 9, 27, 21, 28, MID)
    rect(im, 8, 28, 22, 29, DARK)

    # ---- 2) 炮塔顶面（斜切装甲顶，俯视可见的一条亮带）----
    rect(im, 11, 13, 20, 15, TOP)
    # 前面板与背面板的分界
    rect(im, 15, 13, 16, 15, LIGHT)

    # ---- 3) 炮室 gun house：正面装甲板 ----
    rect(im, 10, 16, 21, 25, MID)
    # 左缘受光
    rect(im, 10, 16, 10, 25, LIGHT)
    # 右缘背光
    rect(im, 21, 16, 21, 25, DARK)
    # 中缝（左右两片装甲的分界）
    rect(im, 15, 16, 16, 25, DARK)
    # 底缘压暗，避免与座圈糊在一起
    rect(im, 10, 24, 21, 25, DARK)
    # 装甲板上的观察窗/铆钉高光点
    px(im, 12, 18, HI)
    px(im, 19, 18, HI)

    # ---- 4) 两根并列粗炮管（德式双联：左右各一门）----
    # 炮管比炮室窄，靠炮室顶面留出左右各 1px 的肩部，读出「双联」而非实心块。
    for cx in (13, 18):                  # 左炮管中心 13，右炮管中心 18
        rect(im, cx - 1, 5, cx + 1, 14, MID)       # 管身
        rect(im, cx - 1, 5, cx - 1, 14, LIGHT)     # 左管壁受光
        rect(im, cx + 1, 5, cx + 1, 14, DARK)      # 右管壁背光
        # 炮口：装填时露黄铜弹体（与管壁同宽），空膛时为深色腔体
        if loaded:
            rect(im, cx - 1, 3, cx + 1, 4, BRASS)
            px(im, cx, 3, HI)                       # 弹顶高光，区分「有弹」
            rect(im, cx - 1, 5, cx + 1, 5, BRASS_D)  # 弹体与管口的接缝
        else:
            rect(im, cx - 1, 3, cx + 1, 4, DEEP)
            px(im, cx, 4, OUTLINE)                   # 空膛更深的腔底

    # ---- 5) 炮管根部炮盾（炮室与炮管衔接的凸台）----
    rect(im, 12, 13, 14, 14, LIGHT)
    rect(im, 17, 13, 19, 14, LIGHT)

    return outline_silhouette(im)


def main():
    base = "src/main/resources/assets/piranport/textures/item"
    draw_turret(loaded=False).save(f"{base}/german_twin_380mm_gun_empty.png")
    draw_turret(loaded=True).save(f"{base}/german_twin_380mm_gun.png")
    print("wrote german_twin_380mm_gun.png / _empty.png")


if __name__ == "__main__":
    main()
