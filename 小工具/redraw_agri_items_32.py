"""农业类物品贴图重画：16x16 → 32x32，菜品式黑色描边 + 轻微透视。

产出文件（全部落在 textures/item/）：
  作物: tomato lettuce rice onion garlic chili soybean lablab_bean ormosia
        celery rye pineapple peach ginger tofu
  原料: rice_flour pie_crust raw_pasta pork_paste yeast beans_can fermented_fish
  菜品: cooked_rice pasta rye_bread
  树苗: chinese_plum_sapling gardenia_sapling peach_sapling
"""
from PIL import Image
import os

W = 32
ITEM_DIR = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item"))
PREVIEW = "/tmp/agri_preview"  # 每张画完先存预览，确认后再覆盖

OL = (28, 22, 18, 255)  # 统一深黑描边（菜品画风）


def canvas():
    return Image.new("RGBA", (W, W), (0, 0, 0, 0))


def ellipse(img, cx, cy, rx, ry, fill, outline=OL, owidth=1):
    """以 (cx,cy) 为中心画填充椭圆+外描边。"""
    px = img.load()
    for y in range(int(cy - ry - owidth - 1), int(cy + ry + owidth + 2)):
        for x in range(int(cx - rx - owidth - 1), int(cx + rx + owidth + 2)):
            if 0 <= x < W and 0 <= y < W:
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if d <= 1.0:
                    px[x, y] = fill
    # 描边：椭圆边缘外扩一圈
    for y in range(int(cy - ry - owidth - 1), int(cy + ry + owidth + 2)):
        for x in range(int(cx - rx - owidth - 1), int(cx + rx + owidth + 2)):
            if 0 <= x < W and 0 <= y < W:
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if 1.0 < d <= ((rx + owidth) / rx) ** 2 + 0.2:
                    if px[x, y][3] == 0:
                        px[x, y] = outline


def save(img, name):
    path = os.path.join(ITEM_DIR, name + ".png")
    img.save(path)
    img.resize((W * 4, W * 4), Image.NEAREST).save(
        os.path.join(PREVIEW, name + ".png"))
    print(name)


os.makedirs(PREVIEW, exist_ok=True)


# ============ 作物 ============

def tomato():
    img = canvas(); px = img.load()
    BODY = (205, 48, 32, 255); BODY_D = (160, 32, 22, 255); BODY_L = (240, 90, 60, 255)
    LEAF = (66, 130, 50, 255); LEAF_D = (45, 95, 35, 255)
    # 果实主体：略扁圆（透视：下缘压平）
    ellipse(img, 16, 19, 11.5, 9.5, BODY)
    # 暗部：右下弧
    for y in range(8, 30):
        for x in range(4, 28):
            d = ((x - 16) / 11.5) ** 2 + ((y - 19) / 9.5) ** 2
            if 0.55 < d <= 1.0 and x > 16 and px[x, y] == BODY:
                px[x, y] = BODY_D
    # 高光左上方块
    for (x, y) in [(10, 14), (11, 14), (10, 15), (11, 15), (12, 15), (9, 15), (10, 16)]:
        px[x, y] = BODY_L
    # 顶部萼片：五角星叶
    for (x, y) in [(16, 9), (15, 9), (17, 9), (16, 8),
                   (13, 10), (12, 11), (14, 10), (18, 10), (19, 10), (20, 11),
                   (14, 9), (18, 9), (12, 12), (20, 12)]:
        px[x, y] = LEAF
    for (x, y) in [(16, 7), (11, 11), (21, 11)]:
        px[x, y] = LEAF_D
    save(img, "tomato")


def lettuce():
    """生菜球：外叶松散波浪边 + 浅绿内心。"""
    img = canvas(); px = img.load()
    L_OUT = (58, 120, 42, 255); L_MID = (95, 168, 62, 255); L_IN = (190, 222, 132, 255)
    # 外叶：圆团 + 波浪边缘（在椭圆外圈补凸点）
    ellipse(img, 16, 18, 11.5, 9.5, L_OUT)
    for (x, y) in [(6, 14), (5, 18), (6, 22), (9, 26), (13, 27), (18, 27),
                   (23, 26), (26, 22), (27, 18), (26, 13), (23, 9), (18, 7), (13, 7), (9, 9)]:
        px[x, y] = L_OUT
        px[x + 1 if x < 16 else x, y] = L_OUT
    # 中层叶
    ellipse(img, 15, 16, 8.5, 7.0, L_MID)
    # 内心
    ellipse(img, 15, 15, 4.5, 3.8, L_IN)
    # 叶脉
    for (x, y) in [(15, 13), (15, 14), (14, 15), (16, 15), (13, 16), (17, 16)]:
        px[x, y] = L_MID
    save(img, "lettuce")


def rice():
    """稻穗：弯茎 + 顶部下垂谷穗 + 长叶。"""
    img = canvas(); px = img.load()
    STEM = (115, 155, 65, 255); LEAF = (95, 140, 55, 255)
    GRAIN = (235, 228, 200, 255); GRAIN_D = (200, 190, 155, 255); GRAIN_L = (248, 244, 225, 255)
    # 弯茎
    for (x, y) in [(13, 29), (14, 27), (14, 25), (15, 23), (15, 21), (15, 19), (14, 17)]:
        px[x, y] = STEM; px[x, y + 1] = STEM
    # 长叶两片（向左下、右下）
    for (x, y) in [(12, 27), (11, 25), (10, 23), (9, 22), (8, 21), (7, 21),
                   (16, 27), (17, 25), (18, 24), (19, 23)]:
        px[x, y] = LEAF
    # 穗头：顶部下垂弧线上密集排两层谷粒
    grains = [(13, 15), (14, 14), (15, 13), (16, 12), (17, 11), (18, 10), (19, 9), (20, 8),
              (12, 13), (13, 12), (14, 11), (15, 10), (16, 9), (17, 8), (18, 7), (19, 6),
              (12, 15), (13, 14), (14, 13), (15, 12), (16, 11), (17, 10), (18, 9), (19, 8),
              (11, 12), (12, 11), (13, 10), (14, 9), (15, 8), (16, 7), (17, 6)]
    for (x, y) in grains:
        px[x, y] = GRAIN
    # 暗面
    for (x, y) in [(15, 13), (16, 12), (17, 11), (18, 10), (19, 9), (20, 8), (16, 10), (17, 9), (18, 8), (19, 7)]:
        if px[x, y] == GRAIN: px[x, y] = GRAIN_D
    # 亮面
    for (x, y) in [(12, 13), (13, 12), (14, 11), (15, 10), (13, 15), (14, 14)]:
        if px[x, y] == GRAIN: px[x, y] = GRAIN_L
    # 顶部芒（穗尖向外散）
    for (x, y) in [(20, 5), (21, 5), (21, 6), (22, 5), (22, 7), (19, 5)]:
        px[x, y] = GRAIN_D
    save(img, "rice")


def onion():
    """洋葱：尖顶圆底鳞茎，棕黄皮纵向条纹。"""
    img = canvas(); px = img.load()
    SKIN = (196, 120, 55, 255); SKIN_D = (160, 90, 38, 255); SKIN_L = (222, 155, 85, 255)
    # 球茎：上大下略收，顶收窄成芽点
    ellipse(img, 16, 19, 10.5, 9.0, SKIN)
    # 顶部收尖（盖一个小椭圆补形）
    ellipse(img, 16, 11, 4.0, 2.5, SKIN)
    # 纵向皮纹：从顶到底的弧线（用窄椭圆近似）
    for xoff, col in [(-6, SKIN_D), (-3, SKIN_L), (0, SKIN_D), (3, SKIN_L), (6, SKIN_D)]:
        for y in range(12, 27):
            # 弧线：中段稍偏
            x = 16 + xoff + int(1.5 * ((y - 19) / 8.0) ** 2) * (1 if xoff >= 0 else -1)
            d = ((x - 16) / 10.5) ** 2 + ((y - 19) / 9.0) ** 2
            if d <= 0.95 and px[x, y] == SKIN:
                px[x, y] = col
    # 顶部芽
    for (x, y) in [(16, 9), (15, 9), (16, 8), (15, 8), (17, 9), (16, 10), (15, 10)]:
        px[x, y] = (95, 140, 60, 255)
    # 底部根痕
    for (x, y) in [(14, 27), (15, 27), (16, 27), (17, 27), (15, 28), (16, 28)]:
        px[x, y] = SKIN_D
    save(img, "onion")


def garlic():
    """大蒜：分瓣鳞茎，外皮紫白纵纹。"""
    img = canvas(); px = img.load()
    SKIN = (232, 222, 208, 255); SKIN_D = (190, 175, 158, 255); SKIN_L = (248, 242, 230, 255)
    # 鳞茎（上尖下宽）
    ellipse(img, 16, 18, 10.0, 8.5, SKIN)
    ellipse(img, 16, 11, 4.5, 3.0, SKIN)
    # 分瓣纵沟：五条凹线
    for xoff in (-6, -3, 0, 3, 6):
        for y in range(12, 26):
            x = 16 + xoff - int(1.0 * ((y - 18) / 8.0) ** 2) * (1 if xoff >= 0 else -1)
            d = ((x - 16) / 10.0) ** 2 + ((y - 18) / 8.5) ** 2
            if d <= 0.95 and px[x, y] == SKIN:
                px[x, y] = SKIN_D
    # 瓣间亮脊
    for xoff in (-4, 2, 5):
        for y in range(13, 24):
            x = 16 + xoff
            if px[x, y] == SKIN:
                px[x, y] = SKIN_L
    # 顶部干茎
    for (x, y) in [(15, 9), (16, 9), (16, 8), (16, 7), (15, 7), (15, 10), (16, 10), (17, 10)]:
        px[x, y] = (125, 140, 95, 255)
    # 底部平切痕
    for x in range(10, 23):
        if px[x, 25] == SKIN: px[x, 25] = SKIN_D
        if px[x, 26] == SKIN: px[x, 26] = SKIN_D
    save(img, "garlic")


def chili():
    img = canvas(); px = img.load()
    BODY = (205, 40, 28, 255); BODY_D = (165, 28, 20, 255); BODY_L = (240, 80, 55, 255)
    CALYX = (70, 125, 45, 255)
    # 弯月形椒身：沿弧排椭圆
    body = [(15, 11), (18, 12), (20, 14), (21, 17), (21, 20), (20, 23), (18, 25), (15, 26)]
    for i, (cx, cy) in enumerate(body):
        r = 3.2 - i * 0.22
        ellipse(img, cx, cy, r, r, BODY)
    # 暗面（右/内侧）
    for (x, y) in [(20, 15), (21, 18), (21, 20), (20, 22), (19, 24), (17, 25)]:
        if px[x, y] == BODY: px[x, y] = BODY_D
    # 高光
    for (x, y) in [(15, 10), (17, 12), (19, 13), (20, 16), (20, 19), (19, 21)]:
        if px[x, y] == BODY: px[x, y] = BODY_L
    # 萼片+柄
    for (x, y) in [(14, 9), (15, 9), (16, 9), (13, 10), (14, 10), (15, 10), (16, 10), (14, 11), (13, 8), (12, 8)]:
        px[x, y] = CALYX
    save(img, "chili")


def soybean():
    """豆荚：两颗带荚，淡黄豆粒鼓出。"""
    img = canvas(); px = img.load()
    POD = (110, 160, 60, 255); POD_D = (85, 130, 45, 255)
    BEAN = (228, 205, 120, 255); BEAN_D = (195, 172, 95, 255)
    # 荚1（斜）
    pod1 = [(11, 20), (13, 18), (15, 16), (17, 14), (19, 12)]
    for (cx, cy) in pod1: ellipse(img, cx, cy, 2.6, 2.0, POD)
    # 荚2（更弯）
    pod2 = [(13, 26), (16, 25), (19, 23), (21, 21), (23, 18)]
    for (cx, cy) in pod2: ellipse(img, cx, cy, 2.6, 2.0, POD)
    # 豆粒鼓起
    for (x, y) in [(12, 19), (14, 17), (16, 15), (15, 25), (18, 23), (20, 21)]:
        px[x, y] = BEAN
    for (x, y) in [(13, 19), (17, 15), (16, 25), (19, 23)]:
        px[x, y] = BEAN_D
    # 荚尖
    for (x, y) in [(19, 11), (20, 11), (23, 17), (24, 17)]:
        px[x, y] = POD_D
    save(img, "soybean")


def lablab_bean():
    """扁豆荚：紫边绿荚，扁宽带弧。"""
    img = canvas(); px = img.load()
    POD = (150, 185, 80, 255); POD_D = (115, 150, 58, 255)
    EDGE = (150, 90, 140, 255)  # 紫边
    # 三个扁荚扇形排开
    pods = [[(10, 18), (13, 16), (16, 14), (19, 12)],
            [(11, 24), (15, 23), (19, 21), (22, 19)],
            [(14, 28), (18, 27), (21, 25)]]
    for pod in pods:
        for (cx, cy) in pod:
            ellipse(img, cx, cy, 2.4, 1.7, POD)
    # 紫边描在荚背
    for (x, y) in [(10, 17), (13, 15), (16, 13), (19, 11), (11, 23), (15, 22), (19, 20), (22, 18), (14, 27), (18, 26), (21, 24)]:
        px[x, y] = EDGE
    # 荚尖
    for (x, y) in [(20, 11), (23, 18), (22, 24)]:
        px[x, y] = POD_D
    save(img, "lablab_bean")


def ormosia():
    """海红豆：三粒红种 + 一段开裂种荚。"""
    img = canvas(); px = img.load()
    RED = (195, 40, 32, 255); RED_D = (150, 28, 22, 255); RED_L = (225, 75, 55, 255)
    POD = (140, 95, 60, 255); POD_D = (110, 72, 45, 255)
    # 开裂种荚（右上弧形两瓣）
    for (x, y) in [(19, 7), (20, 8), (21, 9), (22, 10), (23, 11), (24, 12),
                   (20, 9), (21, 10), (22, 11), (23, 12), (24, 13)]:
        px[x, y] = POD
    for (x, y) in [(18, 8), (19, 9), (20, 10), (21, 11), (22, 12)]:
        px[x, y] = POD_D
    # 三粒红豆（相思豆椭圆）
    for (cx, cy, rx, ry) in [(11, 12, 4.5, 3.8), (12, 21, 4.5, 3.8), (19, 17, 4.2, 3.5)]:
        ellipse(img, cx, cy, rx, ry, RED)
    # 黑斑（一端）
    for (x, y) in [(8, 11), (9, 10), (9, 11), (9, 20), (9, 21), (10, 20), (16, 16), (16, 17), (17, 16)]:
        px[x, y] = (45, 32, 28, 255)
    # 高光
    for (x, y) in [(12, 10), (13, 20), (20, 15), (10, 11), (11, 19), (18, 16)]:
        if px[x, y] == RED: px[x, y] = RED_L
    for (x, y) in [(14, 13), (10, 22), (21, 18)]:
        if px[x, y] == RED: px[x, y] = RED_D
    save(img, "ormosia")


def celery():
    """芹菜：带叶茎束，底部束拢、顶部散叶。"""
    img = canvas(); px = img.load()
    STALK = (150, 195, 80, 255); STALK_D = (110, 155, 55, 255)
    LEAF = (60, 130, 50, 255); LEAF_L = (95, 170, 70, 255)

    def stalk(x0, y0, x1, y1):
        """两像素宽的实心茎段。"""
        steps = max(abs(x1 - x0), abs(y1 - y0)) * 2 + 1
        for i in range(steps + 1):
            t = i / steps
            x = round(x0 + (x1 - x0) * t)
            y = round(y0 + (y1 - y0) * t)
            px[x, y] = STALK
            px[x + 1, y] = STALK

    # 四根茎：底部束拢、向上扇开
    stalk(12, 29, 10, 15)   # 左
    stalk(14, 29, 14, 13)   # 中左
    stalk(16, 29, 17, 13)   # 中右
    stalk(17, 28, 21, 15)   # 右
    # 茎节横纹
    for (x, y) in [(12, 25), (11, 21), (14, 23), (14, 18), (15, 20), (16, 24), (18, 22), (19, 18)]:
        if px[x, y] == STALK: px[x, y] = STALK_D
    # 顶部散叶（每茎末端一小簇）
    leaves = [(9, 13), (8, 11), (10, 12), (9, 10), (11, 13),
              (14, 11), (13, 9), (15, 9), (14, 8), (16, 10),
              (18, 11), (17, 9), (19, 9), (18, 8), (20, 10),
              (21, 13), (22, 11), (23, 12), (22, 14), (21, 10),
              (12, 7), (16, 6), (20, 7)]
    for (x, y) in leaves:
        px[x, y] = LEAF
    for (x, y) in [(8, 11), (14, 8), (18, 8), (22, 11), (16, 6)]:
        px[x, y] = LEAF_L
    save(img, "celery")


def rye():
    """黑麦穗：细长穗+长芒，黄褐色。"""
    img = canvas(); px = img.load()
    GRAIN = (190, 160, 90, 255); GRAIN_D = (150, 122, 65, 255)
    AWN = (215, 190, 130, 255)
    STEM = (150, 135, 70, 255)
    # 茎
    for y in range(16, 30): px[16, y] = STEM
    # 穗粒交错排列
    for i, y in enumerate(range(8, 20, 2)):
        px[14, y] = GRAIN; px[18, y] = GRAIN
        px[15, y + 1] = GRAIN; px[17, y + 1] = GRAIN
    for (x, y) in [(14, 8), (15, 9), (17, 9), (18, 8)]:
        pass  # 顶部已覆盖
    # 芒：穗顶向上散开
    for (x, y) in [(14, 6), (13, 5), (12, 4), (16, 6), (16, 5), (16, 4),
                   (18, 6), (19, 5), (20, 4), (15, 7), (17, 7)]:
        px[x, y] = AWN
    # 侧芒
    for (x, y) in [(13, 10), (12, 9), (19, 10), (20, 9), (13, 14), (12, 13), (19, 14), (20, 13)]:
        px[x, y] = AWN
    # 叶
    for (x, y) in [(13, 28), (12, 27), (13, 27), (19, 28), (20, 27), (19, 27)]:
        px[x, y] = STEM
    save(img, "rye")


def pineapple():
    img = canvas(); px = img.load()
    BODY = (220, 160, 40, 255); BODY_D = (180, 128, 30, 255); BODY_L = (240, 190, 70, 255)
    LEAF = (55, 130, 55, 255); LEAF_D = (38, 95, 40, 255)
    # 果身椭圆
    ellipse(img, 16, 20, 8.5, 9.0, BODY)
    # 菱形网纹
    for y in range(13, 28):
        for x in range(9, 24):
            if px[x, y] == BODY and (x + y) % 4 == 0:
                px[x, y] = BODY_D
    for y in range(13, 28):
        for x in range(9, 24):
            if px[x, y] == BODY and (x - y) % 5 == 0:
                px[x, y] = BODY_L
    # 顶叶冠：五片尖叶
    for (x, y) in [(16, 10), (16, 8), (16, 6), (15, 9), (14, 7), (13, 5),
                   (17, 9), (18, 7), (19, 5), (15, 11), (17, 11), (13, 9), (19, 9), (12, 7), (20, 7)]:
        px[x, y] = LEAF
    for (x, y) in [(16, 5), (13, 4), (19, 4), (12, 6), (20, 6)]:
        px[x, y] = LEAF_D
    save(img, "pineapple")


def peach():
    img = canvas(); px = img.load()
    BODY = (245, 150, 110, 255); BODY_D = (215, 105, 75, 255); BODY_L = (252, 185, 140, 255)
    # 桃形：圆底+顶凹
    ellipse(img, 16, 18, 10.5, 10.0, BODY)
    # 顶部缝合线凹口
    for (x, y) in [(16, 8), (16, 9), (15, 9), (17, 9), (16, 10), (15, 11), (17, 11)]:
        px[x, y] = BODY_D
    # 腮红（右侧红晕）
    for (x, y) in [(21, 15), (22, 16), (21, 16), (22, 17), (20, 16), (23, 17), (21, 17), (20, 17)]:
        px[x, y] = BODY_D
    # 高光
    for (x, y) in [(11, 13), (12, 13), (11, 14), (12, 14), (10, 14)]:
        px[x, y] = BODY_L
    # 叶
    for (x, y) in [(13, 7), (12, 6), (14, 6), (13, 6), (12, 7), (14, 5), (15, 6)]:
        px[x, y] = (75, 140, 55, 255)
    save(img, "peach")


def ginger():
    """姜块：不规则分叉块茎，土黄色。"""
    img = canvas(); px = img.load()
    BODY = (212, 175, 110, 255); BODY_D = (175, 140, 85, 255); BODY_L = (230, 200, 140, 255)
    # 主块
    ellipse(img, 15, 19, 8.0, 6.0, BODY)
    # 分叉指块
    ellipse(img, 22, 15, 4.5, 3.5, BODY)
    ellipse(img, 9, 14, 4.0, 3.0, BODY)
    ellipse(img, 20, 23, 4.0, 3.0, BODY)
    ellipse(img, 12, 11, 3.2, 2.6, BODY)
    # 节环纹
    for (x, y) in [(11, 18), (14, 17), (17, 18), (21, 14), (9, 13), (19, 22), (12, 10)]:
        px[x, y] = BODY_D
    # 亮面
    for (x, y) in [(13, 15), (16, 16), (19, 13), (10, 12), (21, 17)]:
        if px[x, y] == BODY: px[x, y] = BODY_L
    save(img, "ginger")


def tofu():
    """豆腐：白方块带轻微透视（顶面可见）。"""
    img = canvas(); px = img.load()
    TOP = (250, 246, 235, 255); SIDE = (228, 220, 200, 255); SIDE_D = (200, 190, 168, 255)
    # 顶面（菱形透视）：后缘窄前缘宽
    for y in range(8, 14):
        inset = y - 8
        for x in range(6 + inset, 27 - inset):
            px[x, y] = TOP
    # 正面
    for y in range(14, 25):
        for x in range(6, 27):
            px[x, y] = SIDE
    # 右侧面稍暗
    for y in range(14, 25):
        for x in range(21, 27):
            if px[x, y] == SIDE: px[x, y] = SIDE_D
    # 底缘描边
    for x in range(6, 27):
        px[x, 25] = OL
    for y in range(8, 26):
        px[5, y] = OL; px[27, y] = OL if y >= 14 else OL
    for x in range(6, 27):
        pass
    # 顶面轮廓
    px[6, 8] = OL; px[26, 8] = OL
    for x in range(6, 27): px[x, 7] = OL
    # 顶面高光点
    for (x, y) in [(10, 10), (11, 10), (10, 11), (14, 9), (18, 10)]:
        px[x, y] = (255, 255, 250, 255)
    save(img, "tofu")


# ============ 原料 ============

def rice_flour():
    """米粉：布袋装粉，袋口系绳。"""
    img = canvas(); px = img.load()
    BAG = (200, 170, 120, 255); BAG_D = (165, 135, 90, 255)
    POWDER = (245, 242, 230, 255)
    # 袋身：下宽上收
    for y in range(14, 28):
        half = 8 - (y - 14) // 5
        for x in range(16 - half, 16 + half):
            px[x, y] = BAG
    # 袋口褶皱+露出粉
    for x in range(10, 23):
        px[x, 13] = BAG_D
        px[x, 12] = POWDER
    for x in range(11, 22): px[x, 11] = POWDER
    # 系带
    for x in range(12, 21): px[x, 14] = (120, 85, 50, 255)
    # 撒落的粉
    for (x, y) in [(8, 28), (9, 29), (24, 28), (23, 29), (10, 29), (22, 29)]:
        px[x, y] = POWDER
    save(img, "rice_flour")


def pie_crust():
    """派皮：圆形生面皮，边缘捏褶。"""
    img = canvas(); px = img.load()
    CRUST = (230, 200, 150, 255); CRUST_D = (195, 160, 110, 255)
    ellipse(img, 16, 17, 11.0, 9.5, CRUST)
    # 捏褶边缘
    for (x, y) in [(8, 12), (12, 9), (16, 8), (20, 9), (24, 12), (26, 17), (24, 22), (20, 25), (16, 26), (12, 25), (8, 22), (6, 17)]:
        px[x, y] = CRUST_D
    # 中央面粉斑
    for (x, y) in [(14, 16), (15, 16), (17, 18), (14, 19), (18, 15)]:
        px[x, y] = (245, 235, 205, 255)
    save(img, "pie_crust")


def raw_pasta():
    """生意面：一捆直面用绳扎着。"""
    img = canvas(); px = img.load()
    PASTA = (235, 210, 140, 255); PASTA_D = (200, 175, 110, 255)
    TIE = (160, 90, 50, 255)
    # 面束：一簇竖条，顶底参差
    xs = [9, 11, 13, 15, 17, 19, 21, 23]
    tops = [7, 6, 8, 5, 7, 6, 8, 7]
    bots = [28, 29, 27, 29, 28, 29, 27, 28]
    for i, x in enumerate(xs):
        for y in range(tops[i], bots[i]):
            px[x, y] = PASTA
            if i % 2: px[x, y] = PASTA_D
    # 扎绳
    for x in range(8, 25):
        px[x, 17] = TIE; px[x, 18] = TIE
    save(img, "raw_pasta")


def pork_paste():
    """猪肉糜：一碗/一坨粉色肉糜带脂肪纹。"""
    img = canvas(); px = img.load()
    MEAT = (225, 130, 130, 255); MEAT_D = (190, 95, 95, 255); FAT = (245, 205, 195, 255)
    ellipse(img, 16, 19, 11.0, 8.0, MEAT)
    # 脂肪大理石纹
    for (x, y) in [(11, 16), (12, 17), (13, 16), (18, 15), (19, 16), (20, 17),
                   (14, 21), (15, 22), (19, 21), (20, 22), (16, 18), (10, 20)]:
        px[x, y] = FAT
    # 顶部凸起（手捏状）
    ellipse(img, 14, 12, 4.0, 2.5, MEAT)
    ellipse(img, 20, 13, 3.5, 2.2, MEAT)
    for (x, y) in [(13, 11), (19, 12)]:
        px[x, y] = MEAT_D
    save(img, "pork_paste")


def yeast():
    """酵母：小方块鲜酵母/酒酿饼。"""
    img = canvas(); px = img.load()
    BODY = (205, 180, 135, 255); BODY_D = (170, 145, 105, 255)
    # 两块方饼
    for y in range(11, 21):
        for x in range(7, 17): px[x, y] = BODY
    for y in range(14, 24):
        for x in range(15, 25): px[x, y] = BODY
    # 顶面高光与气孔
    for (x, y) in [(9, 13), (12, 14), (14, 12), (17, 16), (20, 15), (22, 17), (18, 20)]:
        px[x, y] = BODY_D
    for (x, y) in [(8, 12), (11, 12), (16, 15), (21, 16)]:
        px[x, y] = (225, 205, 160, 255)
    # 碎屑
    for (x, y) in [(10, 24), (24, 25), (6, 22), (14, 26)]:
        px[x, y] = BODY
    save(img, "yeast")


def beans_can():
    """豆子罐头：金属罐+红色标签带，标签上印豆子。"""
    img = canvas(); px = img.load()
    TIN = (185, 190, 195, 255); TIN_D = (145, 150, 155, 255); TIN_L = (215, 220, 225, 255)
    LABEL = (205, 70, 45, 255); LABEL_D = (170, 52, 35, 255); BEAN = (240, 225, 190, 255)
    # 罐身
    for y in range(10, 27):
        for x in range(9, 24):
            px[x, y] = TIN
    # 右侧明暗交界
    for y in range(10, 27):
        px[22, y] = TIN_D; px[23, y] = TIN_D
    for y in range(10, 27):
        px[10, y] = TIN_L
    # 顶盖椭圆（透视）
    ellipse(img, 16, 9, 7.0, 2.2, TIN_L)
    for x in range(11, 22): px[x, 9] = TIN_D
    # 标签带
    for y in range(14, 22):
        for x in range(9, 24):
            px[x, y] = LABEL
    for x in range(9, 24):
        px[x, 14] = LABEL_D; px[x, 21] = LABEL_D
    # 标签上的豆粒图
    for (x, y) in [(12, 16), (13, 16), (13, 17), (18, 16), (19, 16), (18, 17),
                   (15, 19), (16, 19), (20, 19), (11, 19)]:
        px[x, y] = BEAN
    # 罐底棱
    for x in range(9, 24): px[x, 26] = TIN_D
    save(img, "beans_can")


def fermented_fish():
    """发酵鱼/臭鱼：小碟上一条鱼。"""
    img = canvas(); px = img.load()
    FISH = (150, 140, 110, 255); FISH_D = (115, 105, 80, 255)
    PLATE = (200, 195, 185, 255)
    # 碟
    ellipse(img, 16, 23, 11.5, 3.5, PLATE)
    # 鱼身
    ellipse(img, 15, 17, 7.5, 3.2, FISH)
    # 鱼尾三角
    for (x, y) in [(22, 15), (23, 14), (23, 16), (24, 13), (24, 17), (22, 16), (23, 15), (24, 15), (24, 16)]:
        px[x, y] = FISH
    # 鱼眼
    px[10, 16] = (30, 25, 20, 255)
    # 暗纹/腌制斑
    for (x, y) in [(14, 16), (17, 15), (15, 19), (19, 17)]:
        px[x, y] = FISH_D
    save(img, "fermented_fish")


# ============ 菜品 ============

def cooked_rice():
    """米饭：红碗装白米饭，饭粒蓬松高出碗口。"""
    img = canvas(); px = img.load()
    BOWL = (205, 60, 55, 255); BOWL_D = (160, 42, 40, 255); RIM = (235, 230, 225, 255)
    RICE = (250, 248, 240, 255); RICE_D = (222, 216, 200, 255)
    # 饭堆：高出碗口的圆顶
    ellipse(img, 16, 12, 9.5, 5.5, RICE)
    # 饭粒凹凸（堆叠感）
    for (x, y) in [(11, 10), (14, 8), (17, 8), (20, 10), (12, 12), (15, 11),
                   (18, 11), (21, 12), (13, 9), (19, 9), (16, 7)]:
        px[x, y] = RICE
    for (x, y) in [(12, 11), (15, 10), (18, 10), (14, 12), (17, 12), (20, 11)]:
        px[x, y] = RICE_D
    # 碗沿（饭下沿一圈白）
    for x in range(7, 26):
        px[x, 16] = RIM
    for (x, y) in [(6, 15), (26, 15), (7, 15), (25, 15)]:
        px[x, y] = RIM
    # 碗身下收
    for y in range(17, 25):
        half = int(9.5 * (1 - (y - 17) / 12))
        for x in range(16 - half, 16 + half):
            px[x, y] = BOWL
    # 右侧暗面
    for y in range(17, 25):
        for x in range(19, 25):
            if px[x, y] == BOWL: px[x, y] = BOWL_D
    # 碗足
    for x in range(13, 20):
        px[x, 25] = BOWL_D; px[x, 26] = BOWL_D
    save(img, "cooked_rice")


def pasta():
    """成品意面：浅盘上一团拌了番茄酱的面。"""
    img = canvas(); px = img.load()
    PLATE = (228, 224, 214, 255); PLATE_D = (188, 183, 172, 255)
    NOODLE = (238, 208, 128, 255); NOODLE_D = (205, 175, 100, 255)
    SAUCE = (205, 72, 42, 255)
    # 盘底（大椭圆）
    ellipse(img, 16, 21, 12.5, 5.5, PLATE)
    # 盘心凹
    ellipse(img, 16, 20, 10.0, 4.0, PLATE_D)
    # 面条团：交错弧线堆在盘心
    noodle_rows = [
        (13, [(10, 16), (12, 15), (14, 16), (16, 15), (18, 16), (20, 15), (22, 16)]),
        (16, [(9, 18), (11, 17), (13, 18), (15, 17), (17, 18), (19, 17), (21, 18), (23, 18)]),
        (18, [(10, 20), (12, 19), (14, 20), (16, 19), (18, 20), (20, 19), (22, 20)]),
        (19, [(11, 21), (13, 21), (15, 21), (17, 21), (19, 21), (21, 21)]),
    ]
    for _, pts in noodle_rows:
        for (x, y) in pts:
            px[x, y] = NOODLE
    # 面条缝隙
    for (x, y) in [(11, 16), (14, 15), (17, 16), (20, 15), (12, 18), (15, 18), (18, 17), (21, 17), (13, 20), (16, 20), (19, 20)]:
        if px[x, y] == NOODLE: px[x, y] = NOODLE_D
    # 酱块点
    for (x, y) in [(13, 16), (16, 15), (19, 17), (12, 18), (17, 19), (15, 17), (20, 19)]:
        px[x, y] = SAUCE
    # 盘沿亮边
    for (x, y) in [(7, 18), (6, 20), (26, 20), (25, 17), (9, 14), (23, 14)]:
        if px[x, y] == PLATE: px[x, y] = PLATE_D
    save(img, "pasta")


def rye_bread():
    """黑麦面包：椭圆切片面包，深棕色。"""
    img = canvas(); px = img.load()
    CRUST = (135, 85, 45, 255); CRUMB = (180, 130, 75, 255); CRUMB_L = (205, 160, 105, 255)
    ellipse(img, 16, 18, 11.5, 8.5, CRUST)
    ellipse(img, 16, 17, 9.0, 6.5, CRUMB)
    # 顶部割包裂口
    for (x, y) in [(13, 11), (14, 12), (15, 12), (16, 11), (17, 12), (18, 12), (19, 11)]:
        px[x, y] = CRUMB_L
    # 麸皮点
    for (x, y) in [(11, 17), (20, 15), (14, 20), (18, 19), (12, 14), (21, 18)]:
        px[x, y] = (150, 100, 50, 255)
    save(img, "rye_bread")


# ============ 树苗 ============

def _sapling(name, leaf, leaf_d, trunk, dots=None, dot_col=None):
    """通用树苗：土堆 + 细干 + 双层叶团 + 可选花/果点。"""
    img = canvas(); px = img.load()
    # 土
    for (x, y) in [(13, 28), (14, 28), (15, 28), (16, 28), (17, 28), (18, 28),
                   (14, 29), (15, 29), (16, 29), (17, 29), (12, 29), (19, 29)]:
        px[x, y] = (95, 70, 45, 255)
    # 干（略斜向上）
    for (x, y) in [(16, 27), (16, 26), (16, 25), (15, 24), (15, 23), (15, 22),
                   (15, 21), (15, 20), (16, 19)]:
        px[x, y] = trunk
    # 主叶团（上）+ 两侧小枝
    ellipse(img, 16, 13, 7.5, 5.5, leaf)
    ellipse(img, 10, 16, 4.0, 3.0, leaf)
    ellipse(img, 22, 16, 4.0, 3.0, leaf)
    # 叶脉暗部
    for (x, y) in [(13, 16), (14, 17), (18, 17), (19, 16), (16, 18), (11, 18), (21, 18)]:
        if px[x, y] == leaf: px[x, y] = leaf_d
    # 叶高光
    hi = tuple(min(255, c + 35) for c in leaf[:3]) + (255,)
    for (x, y) in [(13, 10), (16, 9), (19, 11), (11, 14), (14, 12)]:
        if px[x, y] == leaf: px[x, y] = hi
    # 花/果点
    if dots and dot_col:
        for (x, y) in dots:
            px[x, y] = dot_col
    save(img, name)


def chinese_plum_sapling():
    """梅树苗：粉白花点。"""
    _sapling("chinese_plum_sapling", (78, 140, 60, 255), (54, 105, 42, 255), (110, 80, 55, 255),
             dots=[(13, 10), (18, 11), (15, 14), (21, 14), (11, 15), (17, 9)],
             dot_col=(240, 160, 175, 255))


def gardenia_sapling():
    """栀子苗：白花点。"""
    _sapling("gardenia_sapling", (60, 125, 55, 255), (42, 95, 40, 255), (100, 75, 50, 255),
             dots=[(14, 9), (19, 11), (12, 14), (17, 15), (20, 16)],
             dot_col=(245, 245, 235, 255))


def peach_sapling():
    """桃树苗：桃果点。"""
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
