# -*- coding: utf-8 -*-
"""彩云舰侦贴图重画 — 按 zjsnrwiki 原画 Equip_L_142 像素化到 32×32。

原画（512×512，不透明剪影 x39-477 / y129-381）：中岛 C6N1 彩云，3/4 侧俯视 ——
机头朝右偏下（金属灰发动机罩 + 一像素竖线螺旋桨，同级贴图规范），
长条温室状座舱罩沿机身中段，深绿机身，近翼向左下展开、远翼向右上展开，
垂尾立于左上，两翼各一道橙色识别条 + 白框橙芯方徽（原画的日之丸位）。

画风对齐 美术素材/Item（图标）/飞机 同级贴图：
32×32 RGBA、纯黑最外侧八邻域描边、色板取自原画本身。
两态：saiun_recon.png 本体；saiun_recon_empty.png 本体完全一致，
仅底部加黄色叹号（同 type0_recon / seiun_bomber 的空膛规范）。
"""
from PIL import Image

W = H = 32

# ---- 调色板（取样自原画 Equip_L_142.png）----
OUTLINE = (0, 0, 0, 255)          # 纯黑最外侧描边
GREEN_D = (47, 78, 61, 255)       # 2F4E3D 机身暗面
GREEN_M = (62, 102, 80, 255)      # 3E6650 机身主色
GREEN_L = (101, 147, 122, 255)    # 65937A 机翼顶面受光
GREEN_H = (133, 176, 150, 255)    # 85B096 前缘高光带
COWL    = (72, 78, 76, 255)       # 484E4C 发动机罩暗灰
COWL_L  = (112, 120, 116, 255)    # 707874 发动机罩亮面
CANOPY  = (148, 156, 154, 255)    # 949C9A 座舱玻璃
FRAME   = (60, 66, 66, 255)       # 3C4242 座舱框架
ORANGE  = (232, 108, 44, 255)     # E86C2C 橙色识别条
WHITE   = (238, 240, 236, 255)    # EEF0EC 方徽底
RED     = (206, 68, 60, 255)      # CE443C 方徽芯
YELLOW  = (255, 248, 0, 255)      # FFF800 空膛叹号


def px(im, x, y, c):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), c)


def rect(im, x0, y0, x1, y1, c):
    """含端点的实心矩形。"""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)


def poly(im, pts, c):
    """扫描线填充凸多边形（含边界）。"""
    ys = [p[1] for p in pts]
    for y in range(min(ys), max(ys) + 1):
        xs = []
        n = len(pts)
        for i in range(n):
            x1, y1 = pts[i]
            x2, y2 = pts[(i + 1) % n]
            if y1 == y2:
                continue
            if min(y1, y2) <= y <= max(y1, y2):
                t = (y - y1) / (y2 - y1)
                xs.append(x1 + t * (x2 - x1))
        if not xs:
            continue
        xs.sort()
        # 逐条跨距填充，保证斜边实心
        for k in range(0, len(xs) - 1, 2):
            for x in range(round(xs[k]), round(xs[k + 1]) + 1):
                px(im, x, y, c)


def outline_silhouette(im):
    """给已有像素的最外圈补纯黑描边（八邻域），不改动已有像素本身。"""
    solid = {(x, y) for y in range(H) for x in range(W) if im.getpixel((x, y))[3] > 0}
    edge = set()
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H and (nx, ny) not in solid:
                    edge.add((nx, ny))
    for (x, y) in edge:
        px(im, x, y, OUTLINE)
    return im


def draw_saiun(empty: bool):
    """画一架 3/4 侧俯视的彩云，机头朝右。

    坐标构图（左上为原点，对应原画 ÷16）：
      垂尾顶 (6,8)   尾锥 (7,12)   机头 (27,18)
      远翼（右上）翼尖 (30,10)，近翼（左下）翼尖 (3,23)
      座舱罩 (12,13)-(22,16) 长条温室
      发动机罩 (23,15)-(27,20)，螺旋桨 = x30 一像素竖线
    empty=True 时底部加黄色叹号，本体两版完全一致。
    """
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # 绘制顺序：远翼 → 水平尾翼/垂尾 → 近翼 → 机身（压住翼根，形成前后关系）
    #           → 座舱罩 → 发动机罩 → 桨毂/桨叶 → 天线 → 最外侧描边

    # ---- 1) 远翼（右上）：顶面受光，根部粗、翼尖收窄 ----
    poly(im, [(18, 13), (24, 11), (30, 9), (30, 12), (24, 15), (19, 16)], GREEN_L)
    poly(im, [(18, 13), (24, 11), (30, 9), (30, 10), (24, 12), (18, 14)], GREEN_H)
    # 橙色识别条（远翼中段，原画机翼后缘前的橙带）
    poly(im, [(24, 13), (26, 12), (26, 14), (24, 15)], ORANGE)
    # 白框红芯方徽
    rect(im, 27, 11, 28, 12, WHITE)
    px(im, 28, 11, RED)

    # ---- 2) 水平尾翼（左上，垂尾根部向左伸出）----
    poly(im, [(2, 12), (9, 12), (10, 15), (3, 15)], GREEN_L)
    poly(im, [(2, 12), (9, 12), (9, 13), (2, 13)], GREEN_H)

    # ---- 3) 垂直尾翼（立于尾部上方，原画左上角剪影）----
    poly(im, [(5, 8), (7, 8), (9, 13), (5, 13)], GREEN_M)
    poly(im, [(5, 8), (7, 8), (8, 11), (5, 11)], GREEN_L)

    # ---- 4) 近翼（左下）：比机身亮两档，靠色差从机身分出 ----
    poly(im, [(11, 16), (4, 21), (3, 25), (7, 25), (15, 19)], GREEN_L)
    poly(im, [(11, 16), (4, 21), (3, 24), (4, 24), (12, 17)], GREEN_H)
    # 橙色识别条（近翼根部后缘，原画最醒目的一道）
    poly(im, [(9, 19), (12, 17), (13, 18), (10, 21)], ORANGE)
    # 白框红芯方徽
    rect(im, 6, 22, 7, 23, WHITE)
    px(im, 6, 22, RED)

    # ---- 5) 机身：尾锥细、机头粗的斜置长条，压在两翼根部之上 ----
    poly(im, [(6, 11), (24, 15), (25, 20), (10, 18), (6, 13)], GREEN_M)
    # 背脊受光一条
    poly(im, [(6, 11), (24, 15), (24, 16), (7, 13)], GREEN_D)
    # 机腹压暗，和近翼拉开一档
    poly(im, [(10, 17), (25, 19), (25, 20), (11, 18)], GREEN_D)
    # 机身尾段白框方徽（原画垂尾下方那枚）
    rect(im, 9, 14, 10, 15, WHITE)
    px(im, 9, 14, RED)

    # ---- 6) 长条温室座舱罩：深色框上沿 + 玻璃格 + 竖楞 ----
    # 罩体两行玻璃，沿机身斜率从 (12,13) 走到 (21,16)
    poly(im, [(12, 13), (21, 16), (21, 18), (12, 15)], CANOPY)
    # 上沿框架：斜着一行深色，读出「罩在机身上」的体积
    for bx, by in [(12, 13), (13, 13), (14, 14), (15, 14), (16, 14),
                   (17, 15), (18, 15), (19, 15), (20, 16), (21, 16)]:
        px(im, bx, by, FRAME)
    # 竖楞分格（每 2px 一格，温室感）
    for bx, by in [(14, 14), (14, 15), (16, 15), (16, 16),
                   (18, 16), (18, 17), (20, 16), (20, 17)]:
        px(im, bx, by, FRAME)
    # 罩体高光：玻璃中段点两粒浅色
    px(im, 15, 15, WHITE)
    px(im, 19, 16, WHITE)

    # ---- 7) 发动机罩：深灰圆筒，亮面在上，与座舱罩明确分界 ----
    poly(im, [(23, 15), (27, 16), (27, 21), (23, 21), (22, 18)], COWL)
    poly(im, [(23, 15), (27, 16), (27, 18), (23, 17)], COWL_L)
    # 罩体前端环线（深浅交界）
    for by in range(16, 21):
        px(im, 27, by, FRAME)

    # ---- 8) 天线杆（座舱前缘的小黑刺，原画有）----
    px(im, 21, 14, OUTLINE)
    px(im, 22, 13, OUTLINE)

    im = outline_silhouette(im)

    # ---- 9) 螺旋桨：机头处一像素竖线（同级贴图规范）----
    # 贴图尺度下桨叶侧视成线：x=30 一根 1px 黑线，y11-13 压在远翼翼尖上
    # （原画中桨叶就在翼面前方），y14-23 悬在发动机罩轮廓（x28）右侧，
    # 与罩体轮廓之间空出 x29 一列透明隙，读出「桨在罩前转动」的前后关系。
    # 在描边之后画，保证线宽始终 1px，不被描边加粗、也不与罩体轮廓粘连。
    for py_ in range(11, 24):
        px(im, 30, py_, OUTLINE)

    # ---- 10) 空膛态：本体不动，仅底部加黄色警告三角（同级贴图规范，⚠）----
    # 像素级复刻 seiun_bomber_empty / zero_model52_empty 的三角：
    # 黑描边三角 + 黄填充 + 中央黑色感叹号（竖笔 y27-28、间隙 y29、点 y30）
    if empty:
        tri = {
            24: [16],
            25: [15, 16, 17],
            26: [15, 16, 17],
            27: [14, 15, 16, 17, 18],
            28: [13, 14, 15, 16, 17, 18, 19],
            29: [13, 14, 15, 16, 17, 18, 19],
            30: [12, 13, 14, 15, 16, 17, 18, 19, 20],
            31: [12, 13, 14, 15, 16, 17, 18, 19, 20],
        }
        # 三角外轮廓（每行两端 + 底行整行）
        border = set()
        for y, xs in tri.items():
            border.add((xs[0], y))
            border.add((xs[-1], y))
            if y == 31:
                for x in xs:
                    border.add((x, y))
        # 顶部 apex 与两侧斜边已含在两端点中；内部填黄
        for y, xs in tri.items():
            for x in xs:
                px(im, x, y, YELLOW)
        for (x, y) in border:
            px(im, x, y, OUTLINE)
        # 中央黑色感叹号：竖笔 + 间隙 + 点
        px(im, 16, 27, OUTLINE)
        px(im, 16, 28, OUTLINE)
        # y29 保持黄色（号与点之间的间隙）
        px(im, 16, 30, OUTLINE)

    return im


def main():
    base = "src/main/resources/assets/piranport/textures/item"
    draw_saiun(empty=False).save(f"{base}/saiun_recon.png")
    draw_saiun(empty=True).save(f"{base}/saiun_recon_empty.png")
    print("wrote saiun_recon.png / saiun_recon_empty.png")


if __name__ == "__main__":
    main()
