# -*- coding: utf-8 -*-
"""
重画皮肤核心 24（大黄蜂）的 64x64 玩家皮肤贴图。
参考原画: 战舰少女R L_NORMAL_31.png（zjsnrwiki，已缓存 IndomitableCache/zjsnrwiki原画/）

头部（脸）：**直接复制大凤 skin_4 的 12 个头面，只做配色替换**
（画风基准同 draw_firefly_skin.py 的做法）——粉发→大黄蜂金发、粉瞳→蓝瞳、
肤色/睫毛原样保留；叠层空闲格画红黄白条纹飞行帽带（护目镜带环头），
不覆盖大凤发型（程序化自检：统计每面画到的格数并校验未侵占发型）。

身体/手臂/腿为本脚本自绘（对齐原画 L_NORMAL_31）：
黑色挂脖领 + 白短上衣 + 露脐 + 珊瑚红丝带 + 棕腰带银扣 + 藏青百褶裙红裙边
+ 白袖口红蓝条纹 + 裸腿 + 黑长靴红鞋底金靴扣。

贴图按 Minecraft 64x64 玩家皮肤标准分区写入（每个面一张位图，含外层叠层区）。
同时生成 16x16 物品图标 skin_core_24.png（同脚本任务内输出）。
"""
from pathlib import Path

from PIL import Image

# 画风基准：大凤（skin_4）的头部被本脚本直接照搬，只换配色
TAIHOU_SKIN = Path(__file__).resolve().parents[1] / \
    "src/main/resources/assets/piranport/textures/skin/skin_4.png"

# 大凤头部全部 14 色 → 大黄蜂配色（结构一模一样，只换色）。
# 键来自 skin_4 头部区域的实际取色，精确匹配。
TAIHOU_TO_HORNET = {
    # 粉发 → 大黄蜂金发（5 个色阶一一对应，保留大凤头发的明暗分布；
    # 主色 (255,244,156) 取自原画发色）
    (230, 116, 142): (240, 182, 96),
    (235, 118, 149): (248, 204, 116),
    (238, 136, 163): (252, 224, 136),
    (242, 144, 167): (255, 244, 156),   # = HAIR 主色
    (248, 164, 174): (255, 251, 208),
    # 皮肤：沿用大凤原肤色（大凤同为白皙暖调），恒等映射
    (254, 243, 229): (254, 243, 229),
    (252, 231, 216): (252, 231, 216),
    (249, 205, 191): (249, 205, 191),
    (252, 196, 182): (252, 196, 182),
    # 睫毛 / 眼睑：原样保留
    (97, 51, 51): (97, 51, 51),
    (184, 157, 167): (184, 157, 167),
    # 粉瞳 → 大黄蜂亮蓝瞳（原画蓝瞳取色）
    (82, 173, 203): (70, 140, 215),
    (152, 238, 244): (150, 225, 255),
    # 眼高光
    (247, 247, 247): (247, 247, 247),
}

W = H = 64
T = (0, 0, 0, 0)          # 透明

# ---------- 调色板（取自原画配色） ----------
OUT    = (10, 10, 12, 255)       # 纯黑描边
HAIR   = (255, 244, 156, 255)    # 金发主色（原画取色）
HAIR_D = (252, 224, 136, 255)    # 头发暗部
SKIN   = (255, 216, 206, 255)    # 身体肤色（原画偏粉调）
SKIN_D = (238, 186, 178, 255)    # 皮肤阴影
WHITE  = (255, 252, 250, 255)    # 白短上衣
WHITE_D= (208, 198, 206, 255)    # 上衣阴影（原画灰紫调）
BLACK  = (37, 32, 44, 255)       # 挂脖黑领（原画取色）
RED    = (242, 90, 104, 255)     # 珊瑚红丝带/裙边（原画取色）
RED_D  = (190, 60, 76, 255)      # 红暗部
NAVY   = (67, 73, 95, 255)       # 藏青百褶裙（原画取色）
NAVY_D = (38, 40, 49, 255)       # 裙暗部/褶
BROWN  = (134, 86, 50, 255)      # 棕腰带
BROWN_D= (96, 60, 34, 255)       # 腰带暗部/小包
SLIVER = (206, 208, 216, 255)    # 银色带扣
BOOT   = (30, 27, 27, 255)       # 黑长靴（原画取色）
BOOT_L = (58, 56, 58, 255)       # 靴高光
BOOT_D = (16, 14, 15, 255)       # 靴暗部
SOLE   = (146, 58, 58, 255)      # 红鞋底（原画 103,57,56 提亮）
GOLD   = (240, 190, 70, 255)     # 靴扣金
CAP_Y  = (255, 220, 90, 255)     # 护目镜带黄条
CAP_W  = WHITE                    # 护目镜带白条（红用 RED）


class Face:
    """一个面位图；坐标相对该面左上角。"""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[T] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return T

    def is_opaque(self, x, y):
        return self.get(x, y)[3] > 0

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def paste(self, canvas, ox, oy):
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if c[3] > 0:
                    canvas[oy + y][ox + x] = c


def outline_face(f, c=OUT):
    """在 face 内所有「不透明像素的邻居是空洞」的一侧补上纯黑描边。
    项目既定像素化约定：构图对齐原画，只在最外层加黑描边。
    注意：不透明判断必须用**初始快照**——若边扫边改，新描的黑边会变成
    下一轮的"源"一路泛滥，把整面刷黑（躯干叠层曾因此整层变黑）。"""
    orig = [[f.px[y][x][3] > 0 for x in range(f.w)] for y in range(f.h)]

    def opaque(x, y):
        return 0 <= x < f.w and 0 <= y < f.h and orig[y][x]

    mark = set()
    for y in range(f.h):
        for x in range(f.w):
            if not orig[y][x]:
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < f.w and 0 <= ny < f.h and not orig[ny][nx]:
                    mark.add((nx, ny))

    for x, y in mark:
        f.set(x, y, c)


def bake(faces):
    """把各面位图贴到 64x64 皮肤画布上。"""
    canvas = [[T] * W for _ in range(H)]
    for f, ox, oy in faces:
        f.paste(canvas, ox, oy)
    return canvas


# =====================================================================
# 头部 —— 8x8x8，展开于 (0,0)
#   top(8,0) bottom(16,0) right(0,8) front(8,8) left(16,8) back(24,8)
#   叠层: top(40,0) bottom(48,0) right(32,8) front(40,8) left(48,8) back(56,8)
#   面朝向：right 面 x7 为前缘，left 面 x0 为前缘（盒状展开相邻边）
# =====================================================================


def _copy_head_face(src, box):
    """从大凤皮肤裁一个 8x8 头面并做配色替换；未映射颜色保留原值并告警。"""
    f = Face(8, 8)
    unmapped = set()
    for y in range(8):
        for x in range(8):
            c = src.getpixel((box[0] + x, box[1] + y))
            if c[3] == 0:
                continue
            rgb = c[:3]
            if rgb in TAIHOU_TO_HORNET:
                t = TAIHOU_TO_HORNET[rgb]
                f.set(x, y, (t[0], t[1], t[2], 255))
            else:
                unmapped.add(rgb)
                f.set(x, y, c)
    if unmapped:
        # 出现映射表外的颜色说明 skin_4 被改过，打印出来提醒补映射
        print("警告: skin_4 头部出现未映射颜色", unmapped)
    f.do_outline = False   # 照搬大凤的脸，不加黑描边，保持原样
    return f


def build_head():
    """头部 = 大凤 skin_4 的 12 个面原样复制 + 换色；
    叠层空闲格画红黄白条纹飞行帽带（环头一圈，程序化自检格数）。"""
    src = Image.open(TAIHOU_SKIN).convert('RGBA')

    # 基底 6 面
    top = _copy_head_face(src, (8, 0))
    bot = _copy_head_face(src, (16, 0))
    right = _copy_head_face(src, (0, 8))
    front = _copy_head_face(src, (8, 8))
    left = _copy_head_face(src, (16, 8))
    back = _copy_head_face(src, (24, 8))
    # 叠层 6 面（大凤的刘海/发饰造型，换色后即大黄蜂金发叠层）
    l_top = _copy_head_face(src, (40, 0))
    l_bot = _copy_head_face(src, (48, 0))
    l_right = _copy_head_face(src, (32, 8))
    l_front = _copy_head_face(src, (40, 8))
    l_left = _copy_head_face(src, (48, 8))
    l_back = _copy_head_face(src, (56, 8))

    # ---- 大黄蜂标志：红黄白条纹飞行帽带（原画头顶护目镜带）----
    # 只画在叠层**空闲格**（不覆盖大凤发型）；带子沿头侧面 y0 一圈 +
    # 顶面后缘(y0)/前缘(y7)/两侧列，形成 2px 厚的环带。
    # 程序化自检：逐面统计画到的格数，缺格直接报错。
    band_colors = [RED, CAP_Y, CAP_W, CAP_Y]
    drawn_counts = {'front': 0, 'right': 0, 'back': 0, 'left': 0, 'top': 0}
    state = {'i': 0}

    def stripe_if_free(face, x, y, key):
        """只在叠层空闲格画带纹——刘海等发型格（原不透明）直接跳过，
        不可能侵占（下方格数自检兜底 skin_4 几何被改动的情况）。"""
        if not face.is_opaque(x, y):
            face.set(x, y, band_colors[state['i'] % 4])
            state['i'] += 1
            drawn_counts[key] += 1

    # 四个侧面的顶行 = 环头一圈（顺序 front→right→back→left 保证条纹连续）
    for x in range(8):
        stripe_if_free(l_front, x, 0, 'front')
    for x in range(8):
        stripe_if_free(l_right, x, 0, 'right')
    for x in range(8):
        stripe_if_free(l_back, x, 0, 'back')
    for x in range(8):
        stripe_if_free(l_left, x, 0, 'left')
    # 顶面：后缘整行 + 前缘两角 + 两侧列（与侧面带拼成 2px 厚）
    for x in range(8):
        stripe_if_free(l_top, x, 0, 'top')          # 后缘（接 back y0）
    for x in (0, 1, 6, 7):
        stripe_if_free(l_top, x, 7, 'top')          # 前缘两角（接 front y0）
    for y in range(1, 7):
        stripe_if_free(l_top, 0, y, 'top')          # 左侧列（接 right 面）
        stripe_if_free(l_top, 7, y, 'top')          # 右侧列（接 left 面）

    # 自检阈值：侧/后/左顶行应整行 8 格；前脸刘海占位只画两角 ≥4；
    # 顶面 ≥20 格。低于阈值说明 skin_4 叠层几何变了，必须人工复查。
    assert drawn_counts['right'] == 8, drawn_counts
    assert drawn_counts['back'] == 8, drawn_counts
    assert drawn_counts['left'] == 8, drawn_counts
    assert drawn_counts['front'] >= 4, drawn_counts
    assert drawn_counts['top'] >= 20, drawn_counts
    print("帽带自检 OK:", drawn_counts, "共", sum(drawn_counts.values()), "格")

    return [
        (top, 8, 0), (bot, 16, 0),
        (right, 0, 8), (front, 8, 8), (left, 16, 8), (back, 24, 8),
        (l_top, 40, 0), (l_bot, 48, 0),
        (l_right, 32, 8), (l_front, 40, 8), (l_left, 48, 8), (l_back, 56, 8),
    ]


# =====================================================================
# 身体 —— 8x12x4
#   顶层 (20,16) 底层 (28,16) 右(16,20) 前(20,20) 左(28,20) 后(32,20)
#   叠层 顶层(20,32) 底层(28,32) 右(16,36) 前(20,36) 左(28,36) 后(32,36)
# 布局: y0 黑挂脖领 | y1-5 白短上衣 | y6-7 露脐 | y8 腰带 | y9-10 裙 | y11 红裙边
# =====================================================================


def build_body():
    # 顶面：裸肩为主，前缘黑领、后缘黑领结（挂脖式）
    top = Face(8, 4)
    top.rect(0, 0, 8, 4, SKIN)
    top.rect(0, 0, 8, 1, BLACK)          # 前缘领口
    top.rect(0, 3, 8, 1, BLACK)          # 后缘系带

    bot = Face(8, 4)
    bot.rect(0, 0, 8, 4, NAVY_D)         # 裙底

    front = Face(8, 12)
    front.rect(0, 0, 8, 1, BLACK)        # y0 挂脖黑领
    front.set(1, 0, RED)                 # 丝带结
    front.set(2, 0, RED)
    front.rect(0, 1, 8, 5, WHITE)        # y1-5 白短上衣
    front.rect(6, 1, 2, 5, WHITE_D)      # 右侧衣褶阴影
    front.rect(1, 1, 1, 5, BLACK)        # 左襟黑边
    front.rect(0, 6, 8, 1, SKIN)         # y6 露脐
    front.rect(0, 7, 8, 1, SKIN_D)       # y7 腰影
    front.rect(0, 8, 8, 1, BROWN)        # y8 腰带
    front.set(3, 8, SLIVER)              # 银扣
    front.rect(0, 9, 8, 2, NAVY)         # y9-10 百褶裙
    for x in (1, 3, 5, 7):
        front.rect(x, 9, 1, 2, NAVY_D)   # 褶
    front.rect(0, 11, 8, 1, RED)         # y11 红裙边

    back = Face(8, 12)
    back.rect(0, 0, 8, 1, BLACK)
    back.rect(0, 1, 8, 5, WHITE)
    back.rect(6, 1, 2, 5, WHITE_D)
    back.rect(0, 6, 8, 1, SKIN)
    back.rect(0, 7, 8, 1, SKIN_D)
    back.rect(0, 8, 8, 1, BROWN)
    back.rect(0, 9, 8, 2, NAVY)
    for x in (1, 3, 5, 7):
        back.rect(x, 9, 1, 2, NAVY_D)
    back.rect(0, 11, 8, 1, RED)

    right = Face(4, 12)
    right.rect(0, 0, 4, 1, BLACK)
    right.rect(0, 1, 4, 5, WHITE)
    right.rect(2, 1, 2, 5, WHITE_D)
    right.rect(0, 6, 4, 1, SKIN)
    right.rect(0, 7, 4, 1, SKIN_D)
    right.rect(0, 8, 4, 1, BROWN)
    right.rect(0, 9, 4, 2, NAVY)
    right.rect(1, 9, 1, 2, NAVY_D)
    right.rect(3, 9, 1, 2, NAVY_D)
    right.rect(0, 11, 4, 1, RED)

    left = Face(4, 12)
    left.rect(0, 0, 4, 1, BLACK)
    left.rect(0, 1, 4, 5, WHITE)
    left.rect(2, 1, 2, 5, WHITE_D)
    left.rect(0, 6, 4, 1, SKIN)
    left.rect(0, 7, 4, 1, SKIN_D)
    left.rect(0, 8, 4, 1, BROWN)
    left.rect(0, 9, 4, 2, NAVY)
    left.rect(1, 9, 1, 2, NAVY_D)
    left.rect(3, 9, 1, 2, NAVY_D)
    left.rect(0, 11, 4, 1, RED)

    # ---- 叠层：珊瑚红丝带尾（左襟垂下）+ 腰侧小包（原画腰带上挂包）----
    ov_front = Face(8, 12)
    ov_front.set(0, 1, RED)              # 丝带尾：沿左襟垂下
    ov_front.set(0, 2, RED)
    ov_front.set(1, 3, RED)
    ov_front.set(1, 4, RED_D)
    ov_front.set(7, 9, BROWN)            # 小包前缘（角色左胯）
    ov_front.set(7, 10, BROWN_D)

    ov_back = Face(8, 12)

    ov_right = Face(4, 12)

    ov_left = Face(4, 12)                # 角色左胯小包（原画腰带挂包）
    ov_left.set(0, 9, BROWN)
    ov_left.set(1, 9, BROWN)
    ov_left.set(0, 10, BROWN)
    ov_left.set(1, 10, BROWN_D)

    return [
        (top, 20, 16), (bot, 28, 16),
        (right, 16, 20), (front, 20, 20), (left, 28, 20), (back, 32, 20),
        (Face(8, 4), 20, 32), (Face(8, 4), 28, 32),
        (ov_right, 16, 36), (ov_front, 20, 36),
        (ov_left, 28, 36), (ov_back, 32, 36),
    ]


# =====================================================================
# 手臂 —— 4x12x4（右臂用 (40,16) 区，左臂用 (32,48) 区）
# 裸臂；腕部白袖口 + 蓝/红条纹（原画双腕袖口），其下裸手
# =====================================================================


def build_arm(mirror, x0, y0):
    """mirror=True 表示左臂。
    展开顺序（盒状）：[侧(+0)][前(+4)][侧(+8)][后(+12)]；
    +0 侧恒为角色右手侧：右臂 = 外侧，左臂 = 内侧。
    袖口三行：y8 白、y9 藏青条、y10 红条 → y11 裸手。"""

    top = Face(4, 4)
    top.rect(0, 0, 4, 4, SKIN)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, SKIN_D)

    front = Face(4, 12)
    front.rect(0, 0, 4, 8, SKIN)         # 裸臂 y0-7
    front.rect(0, 8, 4, 1, WHITE)        # 袖口
    front.rect(0, 9, 4, 1, NAVY)
    front.rect(0, 10, 4, 1, RED)
    front.rect(0, 11, 4, 1, SKIN)        # 裸手
    front.set(3, 11, SKIN_D)             # 指缝阴影

    back = Face(4, 12)
    back.rect(0, 0, 4, 8, SKIN)
    back.rect(0, 8, 4, 1, WHITE)
    back.rect(0, 9, 4, 1, NAVY)
    back.rect(0, 10, 4, 1, RED)
    back.rect(0, 11, 4, 1, SKIN)

    outer = Face(4, 12)   # 外侧（正常光照）
    outer.rect(0, 0, 4, 8, SKIN)
    outer.rect(0, 8, 4, 1, WHITE)
    outer.rect(0, 9, 4, 1, NAVY)
    outer.rect(0, 10, 4, 1, RED)
    outer.rect(0, 11, 4, 1, SKIN)

    inner = Face(4, 12)    # 内侧（贴身体，整体压暗）
    inner.rect(0, 0, 4, 8, SKIN_D)
    inner.rect(0, 8, 4, 1, WHITE_D)
    inner.rect(0, 9, 4, 1, NAVY)
    inner.rect(0, 10, 4, 1, RED)
    inner.rect(0, 11, 4, 1, SKIN_D)

    side_a = outer if not mirror else inner    # +0 侧
    side_b = inner if not mirror else outer    # +8 侧

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (side_a, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (side_b, x0 + 8, y0 + 4), (back, x0 + 12, y0 + 4),
    ]


# =====================================================================
# 腿 —— 4x12x4（右腿 (0,16) 区，左腿 (16,48) 区）
# 裸腿 + 黑色高筒靴（y6 起，原画过膝靴），红鞋底、金靴扣（外侧）
# =====================================================================


def build_leg(x0, y0, gold_col):
    """gold_col: 金靴扣所在列（右腿 0 / 左腿 3，均在外侧）。"""
    top = Face(4, 4)
    top.rect(0, 0, 4, 4, SKIN)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, SOLE)           # 红鞋底

    front = Face(4, 12)
    front.rect(0, 0, 4, 1, SKIN_D)       # 裙影
    front.rect(0, 1, 4, 5, SKIN)         # 裸腿 y1-5
    front.rect(0, 6, 4, 5, BOOT)         # 高筒靴 y6-10
    front.set(1, 7, BOOT_L)              # 靴面高光
    front.set(gold_col, 7, GOLD)         # 金靴扣（外侧）
    front.rect(0, 11, 4, 1, SOLE)        # 红鞋底

    back = Face(4, 12)
    back.rect(0, 0, 4, 1, SKIN_D)
    back.rect(0, 1, 4, 5, SKIN)
    back.rect(0, 6, 4, 5, BOOT)
    back.set(1, 7, BOOT_L)
    back.rect(0, 11, 4, 1, SOLE)

    right = Face(4, 12)
    right.rect(0, 0, 4, 1, SKIN_D)
    right.rect(0, 1, 4, 5, SKIN)
    right.rect(0, 6, 4, 5, BOOT)
    right.rect(0, 11, 4, 1, SOLE)

    left = Face(4, 12)
    left.rect(0, 0, 4, 1, SKIN_D)
    left.rect(0, 1, 4, 5, SKIN)
    left.rect(0, 6, 4, 5, BOOT)
    left.rect(0, 11, 4, 1, SOLE)

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (right, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (left, x0 + 8, y0 + 4), (back, x0 + 12, y0 + 4),
    ]


# =====================================================================
# 16x16 物品图标 —— 大黄蜂头像（条纹帽带 + 金发蓝瞳白上衣）
# =====================================================================


def build_icon():
    im = Image.new('RGBA', (16, 16), (232, 235, 244, 255))
    px = im.load()

    def put(x, y, c):
        px[x, y] = (c[0], c[1], c[2], 255)

    # y0: 红黄白条纹帽带
    strip = [RED, CAP_Y, CAP_W, CAP_Y]
    for x in range(16):
        put(x, 0, strip[x % 4])
    # 头发：顶 rows1-2 + 两侧列 + 刘海尖
    for y in (1, 2):
        for x in range(2, 14):
            put(x, y, HAIR)
    for x in (2, 13):
        for y in range(3, 9):
            put(x, y, HAIR)
    for x in (3, 12):
        for y in range(3, 7):
            put(x, y, HAIR)
    for x in (4, 7, 10, 11):             # 刘海尖垂到 y3
        put(x, 3, HAIR)
    put(5, 3, HAIR_D)
    put(8, 3, HAIR_D)
    # 脸
    for y in range(3, 12):
        for x in range(4, 12):
            put(x, y, (254, 243, 229))
    for y in range(4, 12):
        put(4, y, (252, 231, 216))
        put(11, y, (252, 231, 216))
    # 腮红
    put(4, 9, (250, 186, 172))
    put(11, 9, (250, 186, 172))
    # 蓝瞳（2x2，下排深蓝）
    for x in (5, 6, 9, 10):
        put(x, 7, (70, 140, 215))
        put(x, 8, (70, 140, 215))
    put(5, 7, (255, 255, 255))
    put(9, 7, (255, 255, 255))
    put(6, 8, (150, 225, 255))
    put(10, 8, (150, 225, 255))
    # 嘴
    put(7, 10, (198, 116, 110))
    put(8, 10, (198, 116, 110))
    # 脖子 + 黑领 + 红丝带 + 白上衣
    for x in (6, 7, 8, 9):
        put(x, 12, (252, 231, 216))
        put(x, 13, (252, 231, 216))
    for x in range(3, 13):
        put(x, 14, BLACK)
    put(5, 14, RED)
    put(6, 14, RED)
    for x in range(2, 14):
        put(x, 15, WHITE)
    return im


def main():
    faces = []
    faces += build_head()
    faces += build_body()
    faces += build_arm(False, 40, 16)    # 右臂
    faces += build_arm(True, 32, 48)     # 左臂
    faces += build_leg(0, 16, gold_col=0)    # 右腿：金扣在外侧列
    faces += build_leg(16, 48, gold_col=3)   # 左腿：金扣在外侧列

    for f, _, _ in faces:
        if getattr(f, 'do_outline', True):
            outline_face(f)

    canvas = bake(faces)
    out = Image.new('RGBA', (W, H), T)
    for y in range(H):
        for x in range(W):
            out.putpixel((x, y), canvas[y][x])

    path = 'src/main/resources/assets/piranport/textures/skin/skin_24.png'
    out.save(path)
    print('saved', path)

    # 物品图标（同任务输出）
    icon_path = 'src/main/resources/assets/piranport/textures/item/skin_core_24.png'
    build_icon().save(icon_path)
    print('saved', icon_path)

    # 导出放大预览
    out.resize((512, 512), Image.NEAREST).save('/tmp/skin24_new_big.png')
    # 3D 视角预览（正/背/侧三视图拼合）
    compose_view(out).save('/tmp/skin24_new_view.png')
    build_icon().resize((256, 256), Image.NEAREST).save('/tmp/skin24_icon_big.png')


def compose_view(im):
    """把 64x64 皮肤按玩家模型拼出正/背/右侧三视图，便于肉眼验收。
    模型 16x32 像素（classic 4px 臂），4 倍放大。
    侧视图：角色面朝右，+0 侧面 = 角色右手侧（前缘 x3）。"""
    S = 4
    fw = 16 * S                       # 正/背面宽 16px
    sw = 8 * S                        # 侧面宽 8px（头宽）
    canvas = Image.new('RGBA', (fw + 4 + fw + 4 + sw, 32 * S), (40, 40, 48, 255))

    def blit(box, dx, dy):
        crop = im.crop(box)
        crop = crop.resize((crop.width * S, crop.height * S), Image.NEAREST)
        canvas.alpha_composite(crop, (dx, dy))

    # ---- 正面（角色右手在观察者左） ----
    ox = 0
    blit((8, 8, 16, 16), ox + 4 * S, 0)            # 头前
    blit((44, 20, 48, 32), ox + 0, 8 * S)          # 右臂前
    blit((20, 20, 28, 32), ox + 4 * S, 8 * S)      # 躯干前
    blit((36, 52, 40, 64), ox + 12 * S, 8 * S)     # 左臂前
    blit((4, 20, 8, 32), ox + 4 * S, 20 * S)       # 右腿前
    blit((20, 52, 24, 64), ox + 8 * S, 20 * S)     # 左腿前

    # ---- 背面 ----
    ox = fw + 4
    blit((24, 8, 32, 16), ox + 4 * S, 0)           # 头后
    blit((52, 20, 56, 32), ox + 0, 8 * S)          # 右臂后 (+12)
    blit((32, 20, 40, 32), ox + 4 * S, 8 * S)      # 躯干后
    blit((44, 52, 48, 64), ox + 12 * S, 8 * S)     # 左臂后 (+12)
    blit((12, 20, 16, 32), ox + 4 * S, 20 * S)     # 右腿后
    blit((28, 52, 32, 64), ox + 8 * S, 20 * S)     # 左腿后

    # ---- 右侧面（角色面朝右：头/躯干/右臂外侧/右腿） ----
    ox = fw + 4 + fw + 4
    blit((0, 8, 8, 16), ox + 0, 0)                 # 头右面（前缘 x7 靠右）
    blit((16, 20, 20, 32), ox + 0, 8 * S)          # 躯干右面
    blit((48, 20, 52, 32), ox + 4 * S, 8 * S)      # 右臂外侧 (+8)
    blit((8, 20, 12, 32), ox + 0, 20 * S)          # 右腿外侧 (+8)

    return canvas


if __name__ == '__main__':
    main()
