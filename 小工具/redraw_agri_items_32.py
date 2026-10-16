"""农业类物品贴图重画：16x16 → 32x32，1px 黑描边，按实物造型重绘。

产出文件（全部落在 textures/item/）：
  作物: tomato lettuce rice onion garlic chili soybean lablab_bean ormosia
        celery rye pineapple peach ginger tofu
  原料: rice_flour pie_crust raw_pasta pork_paste yeast beans_can fermented_fish
  菜品: cooked_rice pasta rye_bread
  树苗: chinese_plum_sapling gardenia_sapling peach_sapling
"""
from PIL import Image
import os, math

W = 32
ITEM_DIR = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item"))
PREVIEW = "/tmp/agri_preview"

OL = (30, 24, 18, 255)  # 1px 深黑描边


def canvas():
    return Image.new("RGBA", (W, W), (0, 0, 0, 0))


def save(img, name):
    img.save(os.path.join(ITEM_DIR, name + ".png"))
    img.resize((W * 4, W * 4), Image.NEAREST).save(os.path.join(PREVIEW, name + ".png"))
    print(name)


def outline(img, col=OL):
    """给所有非透明像素补 1px 外描边（只画在透明处）。"""
    px = img.load()
    border = []
    for y in range(W):
        for x in range(W):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < W and px[nx, ny][3] > 0:
                        border.append((x, y)); break
    for (x, y) in border:
        px[x, y] = col


def fill_ellipse(img, cx, cy, rx, ry, col):
    px = img.load()
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            if 0 <= x < W and 0 <= y < W and ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                px[x, y] = col


def blob(img, pts, r, col):
    """一串点各画一个圆，连成连续形状。"""
    for (cx, cy) in pts:
        fill_ellipse(img, cx, cy, r, r, col)


def disc_grid(img, cx, cy, rx, ry):
    """生成椭圆内的像素点集，便于逐像素上色。"""
    px = img.load()
    cells = []
    for y in range(int(cy - ry), int(cy + ry) + 1):
        for x in range(int(cx - rx), int(cx + rx) + 1):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                cells.append((x, y))
    return cells


def shade_ellipse(img, cx, cy, rx, ry, base, dark, light):
    """左上光：按位置插值明暗。"""
    px = img.load()
    for (x, y) in disc_grid(img, cx, cy, rx, ry):
        # 归一化：-1(左上) ~ +1(右下)
        t = ((x - cx) / rx + (y - cy) / ry) / 2
        if t < -0.25:
            px[x, y] = light
        elif t > 0.35:
            px[x, y] = dark
        else:
            px[x, y] = base


os.makedirs(PREVIEW, exist_ok=True)


# ===================== 作物 =====================

def tomato():
    img = canvas(); px = img.load()
    # 果体：近圆微扁
    shade_ellipse(img, 16, 19, 11, 9.5,
                  (205, 48, 32, 255), (160, 30, 20, 255), (240, 95, 60, 255))
    # 萼片：五片放射
    for (x, y) in [(16, 9), (15, 9), (17, 9), (16, 8),
                   (13, 10), (12, 10), (14, 10), (18, 10), (19, 10), (20, 10),
                   (14, 9), (18, 9), (13, 11), (19, 11)]:
        px[x, y] = (62, 125, 48, 255)
    # 中央小柄
    px[16, 8] = (48, 95, 38, 255); px[16, 7] = (48, 95, 38, 255)
    outline(img)
    save(img, "tomato")


def lettuce():
    img = canvas(); px = img.load()
    # 外叶团：不规则大叶缘
    shade_ellipse(img, 16, 18, 12, 10,
                  (70, 130, 50, 255), (48, 95, 35, 255), (95, 160, 68, 255))
    # 外叶波浪凸点
    for (x, y) in [(5, 14), (4, 18), (6, 23), (10, 27), (15, 28), (21, 27),
                   (26, 23), (28, 18), (27, 13), (24, 8), (19, 6), (13, 6), (8, 9)]:
        px[x, y] = (70, 130, 50, 255)
    # 中层
    shade_ellipse(img, 15, 16, 8, 6.5,
                  (95, 165, 62, 255), (70, 130, 45, 255), (125, 190, 85, 255))
    # 内心浅黄绿
    fill_ellipse(img, 15, 15, 4.5, 3.5, (190, 222, 132, 255))
    # 叶脉
    for (x, y) in [(15, 13), (15, 14), (14, 15), (16, 15), (13, 16), (17, 16)]:
        px[x, y] = (95, 165, 62, 255)
    outline(img)
    save(img, "lettuce")


def rice():
    """稻穗：穗头朝右下垂，谷粒密集。"""
    img = canvas(); px = img.load()
    STEM = (115, 155, 65, 255); LEAF = (95, 140, 55, 255)
    G = (232, 225, 195, 255); GD = (195, 185, 150, 255); GL = (248, 244, 228, 255)
    # 弯茎：左下向右上弯
    stalk = [(11, 29), (12, 27), (12, 25), (13, 23), (13, 21), (13, 19), (14, 17)]
    for (x, y) in stalk:
        px[x, y] = STEM; px[x + 1, y] = STEM
    # 长叶两片
    for (x, y) in [(10, 27), (9, 25), (8, 23), (7, 22), (6, 21),
                   (13, 28), (14, 26), (15, 25), (16, 24)]:
        px[x, y] = LEAF
    # 穗头：从茎顶端向右下弯的谷粒串（两层）
    grains = [(14, 15), (15, 14), (16, 13), (17, 12), (18, 11), (19, 10), (20, 9),
              (13, 13), (14, 12), (15, 11), (16, 10), (17, 9), (18, 8), (19, 7),
              (12, 15), (13, 14), (14, 13), (15, 12), (16, 11), (17, 10), (18, 9),
              (11, 12), (12, 11), (13, 10), (14, 9), (15, 8), (16, 7)]
    for (x, y) in grains:
        px[x, y] = G
    # 每粒谷加暗/亮
    for (x, y) in grains:
        if x + 1 < W and px[x + 1, y] == G and (x + y) % 2 == 0:
            px[x + 1, y] = GD
    for (x, y) in [(13, 12), (15, 10), (17, 9), (12, 14), (14, 12)]:
        px[x, y] = GL
    # 穗尖芒
    for (x, y) in [(20, 6), (21, 5), (21, 6), (22, 5)]:
        px[x, y] = GD
    outline(img)
    save(img, "rice")


def onion():
    """洋葱：尖顶圆身鳞茎 + 纵纹。"""
    img = canvas(); px = img.load()
    SK = (198, 122, 55, 255); SKD = (158, 88, 36, 255); SKL = (225, 158, 85, 255)
    # 球体
    shade_ellipse(img, 16, 20, 10.5, 9, SK, SKD, SKL)
    # 顶部收窄成颈
    fill_ellipse(img, 16, 12, 4, 3, SK)
    # 纵纹：弧线从顶到底
    for xoff, col in [(-6, SKD), (-3, SKL), (0, SKD), (3, SKL), (6, SKD)]:
        for y in range(13, 28):
            # 中段外鼓
            x = 16 + xoff + int(1.2 * math.sin((y - 13) / 15 * math.pi)) * (1 if xoff >= 0 else -1)
            d = ((x - 16) / 10.5) ** 2 + ((y - 20) / 9) ** 2
            if d <= 0.95 and px[x, y] == SK:
                px[x, y] = col
    # 芽
    for (x, y) in [(15, 9), (16, 9), (16, 8), (15, 8), (15, 10), (16, 10), (17, 10)]:
        px[x, y] = (95, 140, 60, 255)
    # 根痕
    for (x, y) in [(14, 28), (15, 28), (16, 28), (17, 28), (15, 29), (16, 29)]:
        px[x, y] = SKD
    outline(img)
    save(img, "onion")


def garlic():
    """大蒜：分瓣鳞茎，顶留干茎。"""
    img = canvas(); px = img.load()
    SK = (232, 222, 208, 255); SKD = (188, 172, 155, 255); SKL = (248, 242, 230, 255)
    # 球体上尖下宽
    shade_ellipse(img, 16, 19, 10, 8.5, SK, SKD, SKL)
    fill_ellipse(img, 16, 12, 4.5, 3, SK)
    # 分瓣沟（5 条）
    for xoff in (-6, -3, 0, 3, 6):
        for y in range(13, 26):
            x = 16 + xoff
            d = ((x - 16) / 10) ** 2 + ((y - 19) / 8.5) ** 2
            if d <= 0.95 and px[x, y] == SK:
                px[x, y] = SKD
    # 瓣脊高光
    for xoff in (-4, 2, 5):
        for y in range(14, 24):
            x = 16 + xoff
            if px[x, y] == SK: px[x, y] = SKL
    # 干茎
    for (x, y) in [(15, 8), (16, 8), (16, 7), (15, 9), (16, 9), (15, 10), (16, 10), (17, 9)]:
        px[x, y] = (125, 140, 95, 255)
    # 平底
    for x in range(10, 23):
        if px[x, 26] == SK: px[x, 26] = SKD
        if px[x, 27] == SK: px[x, 27] = SKD
    outline(img)
    save(img, "garlic")


def chili():
    """辣椒：弯角，蒂部朝左上。"""
    img = canvas(); px = img.load()
    B = (205, 40, 28, 255); BD = (162, 26, 18, 255); BL = (238, 85, 58, 255)
    # 弯月形：中心线一串圆
    path = [(14, 10), (16, 11), (18, 13), (20, 15), (21, 18), (21, 21), (20, 24), (18, 26), (15, 27)]
    for i, (cx, cy) in enumerate(path):
        r = 3.4 - i * 0.18
        fill_ellipse(img, cx, cy, r, r, B)
    # 明暗：内弧暗、外弧亮
    for (x, y) in [(20, 15), (21, 18), (21, 21), (20, 24), (18, 25), (17, 26)]:
        if px[x, y] == B: px[x, y] = BD
    for (x, y) in [(15, 10), (17, 12), (19, 14), (20, 17), (20, 20), (19, 22)]:
        if px[x, y] == B: px[x, y] = BL
    # 萼片+柄
    for (x, y) in [(13, 8), (14, 8), (15, 8), (12, 9), (13, 9), (14, 9), (15, 9),
                   (13, 10), (14, 10), (12, 7), (11, 7), (11, 8)]:
        px[x, y] = (70, 125, 45, 255)
    outline(img)
    save(img, "chili")


def soybean():
    """大豆荚：两段带豆荚 + 一片叶。"""
    img = canvas(); px = img.load()
    POD = (118, 165, 62, 255); POD_D = (88, 132, 45, 255)
    BEAN = (228, 205, 120, 255)
    LEAF = (60, 125, 48, 255)
    # 荚1：斜
    blob(img, [(10, 21), (13, 19), (16, 17), (19, 15), (22, 13)], 2.4, POD)
    # 荚2：更弯
    blob(img, [(12, 27), (15, 26), (18, 24), (21, 22), (24, 19)], 2.4, POD)
    # 豆粒鼓点
    for (x, y) in [(12, 20), (15, 18), (18, 16), (15, 26), (18, 24), (21, 22)]:
        px[x, y] = BEAN
    # 荚尖
    for (x, y) in [(23, 12), (25, 18)]:
        px[x, y] = POD_D
    # 叶
    blob(img, [(9, 15), (8, 13), (9, 11), (11, 10)], 1.6, LEAF)
    outline(img)
    save(img, "soybean")


def lablab_bean():
    """扁豆荚：紫边浅绿扁荚，两片交叉。"""
    img = canvas(); px = img.load()
    POD = (150, 185, 80, 255); POD_D = (115, 148, 58, 255)
    EDGE = (148, 88, 138, 255)
    # 荚1 左斜
    pod1 = [(9, 18), (12, 16), (15, 14), (18, 12), (21, 10)]
    for (x, y) in pod1: fill_ellipse(img, x, y, 2.6, 1.8, POD)
    # 荚2 右斜
    pod2 = [(10, 25), (14, 24), (18, 23), (22, 21), (25, 19)]
    for (x, y) in pod2: fill_ellipse(img, x, y, 2.6, 1.8, POD)
    # 荚背紫边
    for (x, y) in [(9, 17), (12, 15), (15, 13), (18, 11), (10, 24), (14, 23), (18, 22), (22, 20)]:
        px[x, y] = EDGE
    for (x, y) in [(21, 9), (25, 18)]:
        px[x, y] = POD_D
    outline(img)
    save(img, "lablab_bean")


def ormosia():
    """海红豆：三粒红黑相思豆 + 一段开裂种荚。"""
    img = canvas(); px = img.load()
    RED = (195, 38, 30, 255); RD = (148, 25, 20, 255); RL = (228, 80, 55, 255)
    POD = (140, 95, 60, 255); POD_D = (108, 70, 42, 255)
    # 开裂荚（右上弧）
    blob(img, [(20, 7), (21, 8), (22, 9), (23, 10), (24, 11), (24, 12), (25, 13)], 1.7, POD)
    for (x, y) in [(19, 8), (20, 9), (21, 10), (22, 11), (23, 12)]:
        px[x, y] = POD_D
    # 三粒豆
    for (cx, cy, rx, ry) in [(11, 13, 4.5, 3.8), (13, 22, 4.5, 3.8), (19, 18, 4.2, 3.5)]:
        shade_ellipse(img, cx, cy, rx, ry, RED, RD, RL)
    # 黑斑（一端）
    for (x, y) in [(8, 12), (9, 11), (10, 21), (10, 22), (9, 21), (16, 17), (17, 16)]:
        px[x, y] = (45, 30, 26, 255)
    outline(img)
    save(img, "ormosia")


def celery():
    """芹菜：细茎束，底部收拢，顶端小叶。"""
    img = canvas(); px = img.load()
    ST = (150, 195, 80, 255); STD = (110, 155, 55, 255)
    LF = (60, 130, 50, 255); LFL = (95, 170, 70, 255)

    def line(x0, y0, x1, y1):
        steps = max(abs(x1 - x0), abs(y1 - y0)) * 2 + 1
        for i in range(steps + 1):
            t = i / steps
            x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
            px[x, y] = ST

    # 五根细茎
    line(13, 29, 10, 14)
    line(14, 29, 13, 12)
    line(15, 29, 15, 12)
    line(16, 29, 17, 12)
    line(17, 28, 21, 14)
    # 茎节
    for (x, y) in [(12, 24), (11, 20), (14, 21), (15, 17), (17, 22), (19, 17)]:
        if px[x, y] == ST: px[x, y] = STD
    # 小叶（每茎末端 2-3 片）
    leaves = [(10, 13), (9, 11), (10, 10), (11, 12),
              (13, 11), (12, 9), (14, 9), (13, 8),
              (15, 11), (14, 10), (16, 9), (15, 8),
              (17, 11), (18, 9), (19, 9), (18, 8),
              (21, 13), (22, 11), (21, 10), (23, 12)]
    for (x, y) in leaves:
        px[x, y] = LF
    for (x, y) in [(9, 11), (13, 8), (18, 8), (22, 11)]:
        px[x, y] = LFL
    outline(img)
    save(img, "celery")


def rye():
    """黑麦穗：细长穗，籽粒稀疏交替，长芒。"""
    img = canvas(); px = img.load()
    G = (188, 158, 88, 255); GD = (148, 120, 62, 255)
    AWN = (212, 188, 128, 255)
    ST = (150, 135, 70, 255)
    # 茎
    for y in range(16, 30): px[16, y] = ST
    for y in range(24, 30): px[15, y] = ST
    # 穗粒：单列交替，籽粒间有空隙更像麦穗
    for i, y in enumerate(range(9, 19)):
        if i % 2 == 0:
            px[15, y] = G; px[17, y] = G
    # 穗轴
    for y in range(9, 19): px[16, y] = GD
    # 顶芒
    for (x, y) in [(16, 7), (15, 6), (17, 6), (16, 5), (15, 4), (17, 4),
                   (14, 5), (18, 5), (13, 4), (19, 4), (14, 7), (18, 7)]:
        px[x, y] = AWN
    # 侧芒（穗两侧斜出）
    for (x, y) in [(14, 10), (13, 9), (18, 10), (19, 9), (14, 14), (13, 13), (18, 14), (19, 13)]:
        px[x, y] = AWN
    # 底叶
    for (x, y) in [(13, 28), (12, 27), (13, 27), (18, 28), (19, 27), (18, 27)]:
        px[x, y] = ST
    outline(img)
    save(img, "rye")


def pineapple():
    img = canvas(); px = img.load()
    B = (222, 162, 42, 255); BD = (178, 126, 28, 255); BL = (242, 192, 72, 255)
    LF = (55, 130, 55, 255); LFD = (38, 95, 40, 255)
    # 果身
    shade_ellipse(img, 16, 20, 8.5, 9, B, BD, BL)
    # 菱纹（网格）
    for y in range(13, 28):
        for x in range(9, 24):
            if px[x, y] == B and (x + y) % 3 == 0:
                px[x, y] = BD
    for y in range(13, 28):
        for x in range(9, 24):
            if px[x, y] == B and (x - y) % 4 == 0:
                px[x, y] = BL
    # 叶冠：5 片尖叶
    for (x, y) in [(16, 10), (16, 8), (16, 6), (16, 4),
                   (14, 10), (13, 8), (12, 6), (11, 5),
                   (18, 10), (19, 8), (20, 6), (21, 5),
                   (13, 10), (19, 10), (15, 9), (17, 9)]:
        px[x, y] = LF
    for (x, y) in [(16, 3), (11, 4), (21, 4), (12, 5), (20, 5)]:
        px[x, y] = LFD
    outline(img)
    save(img, "pineapple")


def peach():
    img = canvas(); px = img.load()
    B = (246, 150, 108, 255); BD = (215, 105, 72, 255); BL = (252, 186, 140, 255)
    shade_ellipse(img, 16, 19, 10.5, 10, B, BD, BL)
    # 顶凹缝
    for (x, y) in [(16, 9), (16, 10), (15, 10), (17, 10), (16, 11), (15, 12), (17, 12)]:
        px[x, y] = BD
    # 红晕
    for (x, y) in [(21, 15), (22, 16), (21, 16), (22, 17), (20, 16), (23, 17), (21, 17), (20, 17)]:
        px[x, y] = BD
    # 叶
    for (x, y) in [(13, 8), (12, 7), (14, 7), (13, 7), (12, 6), (14, 6), (15, 7)]:
        px[x, y] = (75, 140, 55, 255)
    outline(img)
    save(img, "peach")


def ginger():
    """姜块：主块+分叉指块，土黄。"""
    img = canvas(); px = img.load()
    B = (212, 175, 110, 255); BD = (172, 138, 82, 255); BL = (232, 200, 140, 255)
    # 主块
    shade_ellipse(img, 14, 19, 8, 6, B, BD, BL)
    # 指块
    shade_ellipse(img, 22, 15, 4.5, 3.5, B, BD, BL)
    shade_ellipse(img, 8, 14, 4, 3, B, BD, BL)
    shade_ellipse(img, 19, 23, 4, 3, B, BD, BL)
    shade_ellipse(img, 12, 11, 3.2, 2.6, B, BD, BL)
    # 节环
    for (x, y) in [(11, 18), (14, 17), (17, 18), (21, 14), (8, 13), (19, 22), (12, 10)]:
        px[x, y] = BD
    outline(img)
    save(img, "ginger")


def tofu():
    """豆腐：正立方块，顶面浅梯形透视，三个面分色。"""
    img = canvas(); px = img.load()
    TOP = (250, 246, 235, 255); SIDE = (228, 220, 200, 255); SIDE_D = (198, 188, 166, 255)
    # 顶面：梯形（后缘 y=7 宽 12，前缘 y=12 宽 18，居中）
    for y in range(7, 13):
        t = (y - 7) / 5
        half = 6 + t * 3
        cx = 16
        for x in range(int(cx - half), int(cx + half)):
            px[x, y] = TOP
    # 正面（下接顶面前缘 y=13 起）
    for y in range(13, 25):
        for x in range(7, 25):
            px[x, y] = SIDE
    # 右侧面暗
    for y in range(13, 25):
        for x in range(20, 25):
            px[x, y] = SIDE_D
    # 顶面高光
    for (x, y) in [(12, 9), (13, 9), (12, 10), (16, 8), (18, 10)]:
        px[x, y] = (255, 255, 250, 255)
    outline(img)
    save(img, "tofu")


# ===================== 原料 =====================

def rice_flour():
    """米粉袋：布袋+袋口露出白粉。"""
    img = canvas(); px = img.load()
    BAG = (200, 170, 120, 255); BAG_D = (162, 132, 88, 255)
    PD = (245, 242, 230, 255)
    # 袋身：下宽上窄梯形
    for y in range(14, 28):
        half = 8 - (y - 14) // 5
        for x in range(16 - half, 16 + half):
            px[x, y] = BAG
    # 袋口露粉
    for x in range(10, 23): px[x, 13] = BAG_D
    for x in range(11, 22): px[x, 12] = PD
    for x in range(12, 21): px[x, 11] = PD
    # 扎绳
    for x in range(11, 22): px[x, 14] = (120, 85, 50, 255)
    # 袋面褶皱
    for (x, y) in [(12, 18), (14, 21), (19, 19), (16, 23)]:
        px[x, y] = BAG_D
    # 撒粉
    for (x, y) in [(7, 28), (9, 29), (24, 28), (23, 29), (10, 29)]:
        px[x, y] = PD
    outline(img)
    save(img, "rice_flour")


def pie_crust():
    """派皮：圆面皮 + 捏褶边。"""
    img = canvas(); px = img.load()
    C = (230, 200, 150, 255); CD = (195, 160, 110, 255)
    shade_ellipse(img, 16, 18, 11, 9.5, C, CD, (245, 220, 175, 255))
    # 捏褶（边缘点）
    for (x, y) in [(8, 12), (12, 9), (16, 8), (20, 9), (24, 12), (26, 17),
                   (24, 22), (20, 26), (16, 27), (12, 26), (8, 22), (6, 17)]:
        px[x, y] = CD
    # 面粉斑
    for (x, y) in [(14, 16), (15, 16), (17, 18), (14, 19), (18, 15)]:
        px[x, y] = (246, 236, 206, 255)
    outline(img)
    save(img, "pie_crust")


def raw_pasta():
    """生意面：一捆直面，中间扎绳。"""
    img = canvas(); px = img.load()
    P = (235, 210, 140, 255); PD = (200, 175, 110, 255)
    TIE = (160, 90, 50, 255)
    xs = [9, 11, 13, 15, 17, 19, 21, 23]
    tops = [7, 6, 8, 5, 7, 6, 8, 7]
    bots = [28, 29, 27, 29, 28, 29, 27, 28]
    for i, x in enumerate(xs):
        for y in range(tops[i], bots[i]):
            px[x, y] = P if i % 2 == 0 else PD
    # 扎绳两股
    for x in range(8, 25):
        px[x, 16] = TIE; px[x, 17] = TIE
    outline(img)
    save(img, "raw_pasta")


def pork_paste():
    """猪肉糜：粉色肉泥带脂肪纹。"""
    img = canvas(); px = img.load()
    M = (225, 130, 130, 255); MD = (188, 92, 92, 255); FAT = (245, 205, 195, 255)
    shade_ellipse(img, 16, 19, 11, 8, M, MD, (240, 165, 160, 255))
    # 顶部两坨凸起（手捏）
    fill_ellipse(img, 14, 12, 4, 2.5, M)
    fill_ellipse(img, 20, 13, 3.5, 2.2, M)
    # 脂肪纹
    for (x, y) in [(11, 16), (12, 17), (13, 16), (18, 15), (19, 16), (20, 17),
                   (14, 21), (15, 22), (19, 21), (20, 22), (16, 18), (10, 20)]:
        px[x, y] = FAT
    for (x, y) in [(13, 11), (19, 12)]:
        px[x, y] = MD
    outline(img)
    save(img, "pork_paste")


def yeast():
    """鲜酵母：两块方饼。"""
    img = canvas(); px = img.load()
    B = (205, 180, 135, 255); BD = (168, 143, 102, 255); BL = (226, 206, 162, 255)
    # 块1
    for y in range(10, 20):
        for x in range(7, 17): px[x, y] = B
    # 块2（叠在前右下）
    for y in range(14, 24):
        for x in range(14, 25): px[x, y] = B
    # 气孔
    for (x, y) in [(9, 12), (12, 14), (14, 11), (16, 16), (19, 15), (22, 17), (17, 21)]:
        px[x, y] = BD
    for (x, y) in [(8, 11), (11, 11), (15, 15), (21, 15)]:
        px[x, y] = BL
    # 碎屑
    for (x, y) in [(9, 24), (25, 25), (6, 21), (13, 26)]:
        px[x, y] = B
    outline(img)
    save(img, "yeast")


def beans_can():
    """豆罐头：金属罐 + 红标签带。"""
    img = canvas(); px = img.load()
    T = (185, 190, 195, 255); TD = (142, 147, 152, 255); TL = (216, 221, 226, 255)
    LB = (205, 68, 45, 255); LBD = (168, 50, 33, 255); BN = (240, 225, 190, 255)
    # 罐身
    for y in range(10, 27):
        for x in range(9, 24): px[x, y] = T
    for y in range(10, 27): px[22, y] = TD; px[23, y] = TD; px[10, y] = TL
    # 顶盖椭圆
    fill_ellipse(img, 16, 9, 7, 2.2, TL)
    for x in range(11, 22): px[x, 9] = TD
    # 标签
    for y in range(14, 22):
        for x in range(9, 24): px[x, y] = LB
    for x in range(9, 24): px[x, 14] = LBD; px[x, 21] = LBD
    # 标签上的豆图
    for (x, y) in [(12, 16), (13, 16), (13, 17), (18, 16), (19, 16), (18, 17),
                   (15, 19), (16, 19), (20, 19), (11, 19)]:
        px[x, y] = BN
    for x in range(9, 24): px[x, 26] = TD
    outline(img)
    save(img, "beans_can")


def fermented_fish():
    """发酵鱼：椭圆碟上一条咸鱼。"""
    img = canvas(); px = img.load()
    F = (150, 140, 110, 255); FD = (112, 102, 78, 255)
    PL = (200, 195, 185, 255); PLD = (160, 155, 145, 255)
    # 碟
    shade_ellipse(img, 16, 23, 11.5, 3.5, PL, PLD, (225, 220, 210, 255))
    # 鱼身
    shade_ellipse(img, 15, 17, 7.5, 3.2, F, FD, (180, 170, 138, 255))
    # 尾鳍
    for (x, y) in [(22, 15), (23, 14), (23, 16), (24, 13), (24, 17), (22, 16), (23, 15), (24, 15), (24, 16)]:
        px[x, y] = F
    # 眼
    px[10, 16] = (30, 25, 20, 255)
    # 腌制斑
    for (x, y) in [(14, 16), (17, 15), (15, 19), (19, 17)]:
        px[x, y] = FD
    outline(img)
    save(img, "fermented_fish")


# ===================== 菜品 =====================

def cooked_rice():
    """米饭：红碗，饭粒蓬松冒尖。"""
    img = canvas(); px = img.load()
    BOWL = (205, 60, 55, 255); BOWL_D = (158, 40, 38, 255); RIM = (235, 230, 225, 255)
    R = (250, 248, 240, 255); RD = (222, 216, 200, 255)
    # 饭堆：圆顶冒尖
    shade_ellipse(img, 16, 11, 9.5, 5.5, R, RD, (255, 253, 248, 255))
    # 饭粒凸点
    for (x, y) in [(11, 10), (14, 8), (17, 8), (20, 10), (12, 12), (15, 11),
                   (18, 11), (21, 12), (13, 9), (19, 9), (16, 7)]:
        px[x, y] = R
    for (x, y) in [(12, 11), (15, 10), (18, 10), (14, 12), (17, 12), (20, 11)]:
        px[x, y] = RD
    # 碗沿
    for x in range(7, 26): px[x, 16] = RIM
    for (x, y) in [(6, 15), (26, 15), (7, 15), (25, 15)]:
        px[x, y] = RIM
    # 碗身下收
    for y in range(17, 25):
        half = int(9.5 * (1 - (y - 17) / 12))
        for x in range(16 - half, 16 + half):
            px[x, y] = BOWL
    for y in range(17, 25):
        for x in range(19, 25):
            if px[x, y] == BOWL: px[x, y] = BOWL_D
    # 足
    for x in range(13, 20):
        px[x, 25] = BOWL_D; px[x, 26] = BOWL_D
    outline(img)
    save(img, "cooked_rice")


def pasta():
    """意面：浅盘一团拌酱面。"""
    img = canvas(); px = img.load()
    PL = (228, 224, 214, 255); PLD = (186, 181, 170, 255)
    N = (238, 208, 128, 255); ND = (205, 175, 100, 255)
    S = (205, 72, 42, 255)
    # 盘
    shade_ellipse(img, 16, 21, 12.5, 5.5, PL, PLD, (245, 242, 235, 255))
    fill_ellipse(img, 16, 20, 10, 4, PLD)
    # 面条：三层交错弧
    for (x, y) in [(10, 16), (12, 15), (14, 16), (16, 15), (18, 16), (20, 15), (22, 16),
                   (9, 18), (11, 17), (13, 18), (15, 17), (17, 18), (19, 17), (21, 18), (23, 18),
                   (10, 20), (12, 19), (14, 20), (16, 19), (18, 20), (20, 19), (22, 20),
                   (11, 21), (13, 21), (15, 21), (17, 21), (19, 21), (21, 21)]:
        px[x, y] = N
    # 缝
    for (x, y) in [(11, 16), (14, 15), (17, 16), (20, 15), (12, 18), (15, 18), (18, 17), (21, 17), (13, 20), (16, 20), (19, 20)]:
        if px[x, y] == N: px[x, y] = ND
    # 酱
    for (x, y) in [(13, 16), (16, 15), (19, 17), (12, 18), (17, 19), (15, 17), (20, 19)]:
        px[x, y] = S
    outline(img)
    save(img, "pasta")


def rye_bread():
    """黑麦面包：椭圆切片，深棕皮。"""
    img = canvas(); px = img.load()
    CR = (135, 85, 45, 255); CB = (180, 130, 75, 255); CL = (205, 160, 105, 255)
    shade_ellipse(img, 16, 18, 11.5, 8.5, CR, (105, 62, 32, 255), (165, 115, 65, 255))
    fill_ellipse(img, 16, 17, 9, 6.5, CB)
    # 割包裂口
    for (x, y) in [(13, 11), (14, 12), (15, 12), (16, 11), (17, 12), (18, 12), (19, 11)]:
        px[x, y] = CL
    # 麸皮点
    for (x, y) in [(11, 17), (20, 15), (14, 20), (18, 19), (12, 14), (21, 18)]:
        px[x, y] = (150, 100, 50, 255)
    outline(img)
    save(img, "rye_bread")


# ===================== 树苗 =====================

def _sapling(name, leaf, leaf_d, trunk, dots=None, dot_col=None):
    img = canvas(); px = img.load()
    # 土堆
    for (x, y) in [(13, 28), (14, 28), (15, 28), (16, 28), (17, 28), (18, 28),
                   (14, 29), (15, 29), (16, 29), (17, 29), (12, 29), (19, 29)]:
        px[x, y] = (95, 70, 45, 255)
    # 干
    for (x, y) in [(16, 27), (16, 26), (16, 25), (15, 24), (15, 23), (15, 22), (15, 21), (15, 20), (16, 19)]:
        px[x, y] = trunk
    # 叶团
    fill_ellipse(img, 16, 13, 7.5, 5.5, leaf)
    fill_ellipse(img, 10, 16, 4, 3, leaf)
    fill_ellipse(img, 22, 16, 4, 3, leaf)
    # 暗
    for (x, y) in [(13, 16), (14, 17), (18, 17), (19, 16), (16, 18), (11, 18), (21, 18)]:
        if px[x, y] == leaf: px[x, y] = leaf_d
    # 亮
    hi = tuple(min(255, c + 35) for c in leaf[:3]) + (255,)
    for (x, y) in [(13, 10), (16, 9), (19, 11), (11, 14), (14, 12)]:
        if px[x, y] == leaf: px[x, y] = hi
    if dots and dot_col:
        for (x, y) in dots: px[x, y] = dot_col
    outline(img)
    save(img, name)


def chinese_plum_sapling():
    _sapling("chinese_plum_sapling", (78, 140, 60, 255), (54, 105, 42, 255), (110, 80, 55, 255),
             dots=[(13, 10), (18, 11), (15, 14), (21, 14), (11, 15), (17, 9)],
             dot_col=(240, 160, 175, 255))


def gardenia_sapling():
    _sapling("gardenia_sapling", (60, 125, 55, 255), (42, 95, 40, 255), (100, 75, 50, 255),
             dots=[(14, 9), (19, 11), (12, 14), (17, 15), (20, 16)],
             dot_col=(245, 245, 235, 255))


def peach_sapling():
    _sapling("peach_sapling", (70, 135, 55, 255), (48, 102, 40, 255), (105, 78, 52, 255),
             dots=[(12, 12), (19, 13), (16, 16), (14, 10)],
             dot_col=(245, 150, 110, 255))


if __name__ == "__main__":
    for fn in [tomato, lettuce, rice, onion, garlic, chili, soybean, lablab_bean,
               ormosia, celery, rye, pineapple, peach, ginger, tofu,
               rice_flour, pie_crust, raw_pasta, pork_paste, yeast, beans_can,
               fermented_fish, cooked_rice, pasta, rye_bread,
               chinese_plum_sapling, gardenia_sapling, peach_sapling]:
        fn()
    print("done ->", ITEM_DIR)
