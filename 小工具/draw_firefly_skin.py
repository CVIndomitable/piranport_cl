# -*- coding: utf-8 -*-
"""
重画皮肤核心 22（萤火虫）的 64x64 玩家皮肤贴图。
参考原画: 战舰少女R L_NORMAL_82.png（zjsnrwiki）

头部（脸）：**直接复制大凤 skin_4 的 12 个头面，只做配色替换**
（画风基准同 keeling_skin_tools.py 的做法）——粉发→金发、粉瞳→蓝瞳、
肤色/睫毛原样保留；叠层透明处补萤火虫的红黑条纹发带。
身体/手臂/腿为本脚本自绘：深蓝水手服 + 白泡泡袖 + 红滚边百褶裙 +
红黑条纹围巾 + 黑长靴红边 + 腰间萤火光。

贴图按 Minecraft 64x64 玩家皮肤标准分区写入（每个面一张位图，含外层叠层区）。
"""
from pathlib import Path

from PIL import Image

# 画风基准：大凤（skin_4）的头部被本脚本直接照搬，只换配色
TAIHOU_SKIN = Path(__file__).resolve().parents[1] / \
    "src/main/resources/assets/piranport/textures/skin/skin_4.png"

# 大凤头部全部 14 色 → 萤火虫配色（结构一模一样，只换色）。
# 键来自 skin_4 头部区域的实际取色，精确匹配。
TAIHOU_TO_FIREFLY = {
    # 粉发 → 金发（5 个色阶一一对应，保留大凤头发的明暗分布）
    (230, 116, 142): (200, 156, 66),
    (235, 118, 149): (214, 172, 84),
    (238, 136, 163): (232, 196, 110),
    (242, 144, 167): (243, 216, 140),   # = HAIR
    (248, 164, 174): (252, 234, 168),
    # 皮肤：沿用大凤原肤色（萤火虫同为白皙暖调），恒等映射
    (254, 243, 229): (254, 243, 229),
    (252, 231, 216): (252, 231, 216),
    (249, 205, 191): (249, 205, 191),
    (252, 196, 182): (252, 196, 182),
    # 睫毛 / 眼睑：原样保留
    (97, 51, 51): (97, 51, 51),
    (184, 157, 167): (184, 157, 167),
    # 粉瞳 → 蓝瞳（萤火虫是明亮蓝瞳）
    (82, 173, 203): (92, 130, 210),
    (152, 238, 244): (170, 220, 255),
    # 眼高光
    (247, 247, 247): (247, 247, 247),
}

W = H = 64
T = (0, 0, 0, 0)          # 透明

# ---------- 调色板（取自原画配色） ----------
OUT    = (10, 10, 12, 255)       # 纯黑描边
HAIR   = (243, 216, 140, 255)    # 金 Blonde 发（浅金）
HAIR_D = (214, 176, 100, 255)    # 头发暗部
HAIR_L = (255, 240, 190, 255)    # 头发亮部
SKIN   = (255, 230, 214, 255)    # 苍白皮肤
SKIN_D = (238, 196, 176, 255)    # 皮肤阴影
BLUSH  = (250, 186, 172, 255)    # 腮红
EYE    = (92, 130, 210, 255)     # 蓝色眼睛
EYE_D  = (52, 74, 150, 255)      # 眼睛暗部
WHITE  = (248, 248, 250, 255)    # 白衬衫
WHITE_D= (220, 222, 232, 255)    # 白衬衫阴影
NAVY   = (56, 66, 116, 255)      # 深蓝水手服
NAVY_D = (40, 48, 92, 255)       # 深蓝暗部/裙褶暗
NAVY_L = (78, 90, 148, 255)      # 深蓝亮部
RED    = (201, 54, 58, 255)      # 红滚边/领巾
RED_D  = (150, 36, 44, 255)      # 红暗部
STRIPE = (32, 30, 34, 255)       # 条纹中的黑（发带/围巾）
BOOT   = (46, 38, 40, 255)       # 黑长靴
BOOT_D = (30, 25, 27, 255)       # 长靴暗部
GREEN  = (120, 235, 120, 255)    # 萤火光
GREEN_L= (210, 255, 190, 255)    # 萤火光核心
MOUTH  = (198, 116, 110, 255)    # 张口笑的嘴


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
            if rgb in TAIHOU_TO_FIREFLY:
                t = TAIHOU_TO_FIREFLY[rgb]
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
    """头部 = 大凤 skin_4 的 12 个面原样复制 + 换色；叠层透明处补条纹发带。"""
    src = Image.open(TAIHOU_SKIN).convert('RGBA')

    # 基底 6 面
    top = _copy_head_face(src, (8, 0))
    bot = _copy_head_face(src, (16, 0))
    right = _copy_head_face(src, (0, 8))
    front = _copy_head_face(src, (8, 8))
    left = _copy_head_face(src, (16, 8))
    back = _copy_head_face(src, (24, 8))
    # 叠层 6 面（大凤的刘海/发饰造型，换色后即萤火虫金发叠层）
    l_top = _copy_head_face(src, (40, 0))
    l_bot = _copy_head_face(src, (48, 0))
    l_right = _copy_head_face(src, (32, 8))
    l_front = _copy_head_face(src, (40, 8))
    l_left = _copy_head_face(src, (48, 8))
    l_back = _copy_head_face(src, (56, 8))

    # ---- 萤火虫标志：红黑条纹发带（只画在叠层透明处，不覆盖大凤造型）----
    def stripe_if_free(f, x, y, by='y'):
        if not f.is_opaque(x, y):
            idx = y if by == 'y' else x
            f.set(x, y, RED if idx % 2 == 0 else STRIPE)

    # 头顶两条竖起的条纹飘带（x1、x6 列）
    for y in range(8):
        stripe_if_free(l_top, 1, y)
        stripe_if_free(l_top, 6, y)
    # 额前发带（叠层前脸 y0 横过）
    for x in range(8):
        stripe_if_free(l_front, x, 0, by='x')
    # 后脑发带结（叠层后脸 y0 中段）
    for x in range(2, 6):
        stripe_if_free(l_back, x, 0, by='x')
    # 马尾根部发圈（叠层两侧后缘 y1）
    for x in (0, 1):
        stripe_if_free(l_right, x, 1, by='x')
    for x in (6, 7):
        stripe_if_free(l_left, x, 1, by='x')

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
# =====================================================================


def build_body():
    top = Face(8, 4)
    top.rect(0, 0, 8, 4, NAVY)
    top.rect(1, 0, 6, 1, WHITE)          # 前缘白领/衬衫

    bot = Face(8, 4)
    bot.rect(0, 0, 8, 4, NAVY_D)         # 裙底

    front = Face(8, 12)
    front.rect(0, 0, 8, 12, NAVY)
    # 白衬衫领口 + 红领巾尖（y2，y0-1 被围巾叠层盖住）
    front.rect(2, 2, 4, 1, WHITE)
    front.rect(3, 2, 2, 1, RED)
    # 髋部：白滚边 + 红滚边
    front.rect(0, 8, 8, 1, WHITE)
    front.rect(0, 9, 8, 1, RED)
    # 百褶裙摆 y10-11
    front.rect(0, 10, 8, 2, NAVY_D)
    for x in (0, 2, 4, 6):
        front.rect(x, 10, 1, 2, NAVY)

    back = Face(8, 12)
    back.rect(0, 0, 8, 12, NAVY)
    # 水手方领
    back.rect(1, 2, 6, 1, WHITE)
    back.rect(1, 3, 6, 1, WHITE)
    back.rect(2, 3, 4, 1, RED)
    back.rect(0, 8, 8, 1, WHITE)
    back.rect(0, 9, 8, 1, RED)
    back.rect(0, 10, 8, 2, NAVY_D)
    for x in (0, 2, 4, 6):
        back.rect(x, 10, 1, 2, NAVY)

    right = Face(4, 12)
    right.rect(0, 0, 4, 12, NAVY)
    right.rect(0, 8, 4, 1, WHITE)
    right.rect(0, 9, 4, 1, RED)
    right.rect(0, 10, 4, 2, NAVY_D)
    right.set(0, 10, NAVY)
    right.set(2, 10, NAVY)
    right.set(0, 11, NAVY)
    right.set(2, 11, NAVY)

    left = Face(4, 12)
    left.rect(0, 0, 4, 12, NAVY)
    left.rect(0, 8, 4, 1, WHITE)
    left.rect(0, 9, 4, 1, RED)
    left.rect(0, 10, 4, 2, NAVY_D)
    left.set(1, 10, NAVY)
    left.set(3, 10, NAVY)
    left.set(1, 11, NAVY)
    left.set(3, 11, NAVY)

    # ---- 叠层：红黑条纹围巾（y0-1 环绕）+ 腰间萤火光 ----
    ov_front = Face(8, 12)
    for y in (0, 1):
        for x in range(8):
            # 斜纹：红黑相间条纹围巾
            c = RED if (x + y) % 2 == 0 else STRIPE
            ov_front.set(x, y, c)
    # 萤火光点（前右髋部，对应原画腰间绿色光 pouch）
    ov_front.set(5, 6, GREEN)
    ov_front.rect(6, 6, 2, 2, GREEN)
    ov_front.set(6, 6, GREEN_L)
    ov_front.set(4, 7, GREEN)

    ov_back = Face(8, 12)
    for y in (0, 1):
        for x in range(8):
            ov_back.set(x, y, RED if (x + y) % 2 == 0 else STRIPE)

    ov_right = Face(4, 12)
    for y in (0, 1):
        for x in range(4):
            ov_right.set(x, y, RED if (x + y) % 2 == 0 else STRIPE)

    ov_left = Face(4, 12)
    for y in (0, 1):
        for x in range(4):
            ov_left.set(x, y, RED if (x + y) % 2 == 0 else STRIPE)

    return [
        (top, 20, 16), (bot, 28, 16),
        (right, 16, 20), (front, 20, 20), (left, 28, 20), (back, 32, 20),
        (Face(8, 4), 20, 32), (Face(8, 4), 28, 32),
        (ov_right, 16, 36), (ov_front, 20, 36),
        (ov_left, 28, 36), (ov_back, 32, 36),
    ]


# =====================================================================
# 手臂 —— 4x12x4（右臂用 (40,16) 区，左臂用 (32,48) 区）
# 白色泡泡袖至 y4，其下裸露手臂
# =====================================================================


def build_arm(mirror, x0, y0):
    """mirror=True 表示左臂。
    展开顺序（盒状）：[侧(+0)][前(+4)][侧(+8)][后(+12)]；
    +0 侧恒为角色右手侧：右臂 = 外侧，左臂 = 内侧。"""

    top = Face(4, 4)
    top.rect(0, 0, 4, 4, WHITE)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, SKIN_D)

    front = Face(4, 12)
    front.rect(0, 0, 4, 1, NAVY)             # 肩部背带线
    front.rect(0, 1, 4, 3, WHITE)            # 泡泡袖主体 y1-4
    front.rect(0, 4, 4, 1, WHITE_D)          # 袖口收边
    front.rect(0, 5, 4, 7, SKIN)             # 裸露手臂
    front.rect(0, 8, 4, 1, SKIN_D)           # 肘弯阴影
    front.set(3, 11, SKIN_D)                 # 指缝阴影

    back = Face(4, 12)
    back.rect(0, 0, 4, 1, NAVY)
    back.rect(0, 1, 4, 3, WHITE)
    back.rect(0, 4, 4, 1, WHITE_D)
    back.rect(0, 5, 4, 7, SKIN)
    back.rect(0, 8, 4, 1, SKIN_D)

    outer = Face(4, 12)   # 外侧（正常光照）
    outer.rect(0, 0, 4, 1, NAVY)
    outer.rect(0, 1, 4, 3, WHITE)
    outer.rect(0, 4, 4, 1, WHITE_D)
    outer.rect(0, 5, 4, 7, SKIN)

    inner = Face(4, 12)    # 内侧（贴身体，整体压暗）
    inner.rect(0, 0, 4, 1, NAVY)
    inner.rect(0, 1, 4, 3, WHITE_D)
    inner.rect(0, 4, 4, 1, WHITE_D)
    inner.rect(0, 5, 4, 7, SKIN_D)

    side_a = outer if not mirror else inner    # +0 侧
    side_b = inner if not mirror else outer    # +8 侧

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (side_a, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (side_b, x0 + 8, y0 + 4), (back, x0 + 12, y0 + 4),
    ]


# =====================================================================
# 腿 —— 4x12x4（右腿 (0,16) 区，左腿 (16,48) 区）
# 裸腿 + 黑色高筒靴（y6 起），靴口红边、前缘红细线
# =====================================================================


def build_leg(x0, y0, boot_red_col):
    """boot_red_col: 靴前缘红细线所在列（右腿 0 / 左腿 3，均在外侧）。"""
    top = Face(4, 4)
    top.rect(0, 0, 4, 4, SKIN)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, BOOT_D)
    bot.rect(1, 1, 2, 2, BOOT)

    front = Face(4, 12)
    front.rect(0, 0, 4, 6, SKIN)             # 裸腿（裙下大腿至膝）
    front.rect(0, 5, 4, 1, SKIN_D)           # 膝下阴影
    front.rect(0, 6, 4, 1, RED)              # 靴口红边
    front.rect(0, 7, 4, 5, BOOT)             # 高筒靴
    front.set(boot_red_col, 7, RED)          # 靴前缘红细线（外侧）
    for yy in range(8, 12):
        front.set(boot_red_col, yy, RED)
    front.rect(0, 11, 4, 1, BOOT_D)          # 鞋底

    back = Face(4, 12)
    back.rect(0, 0, 4, 6, SKIN)
    back.rect(0, 5, 4, 1, SKIN_D)
    back.rect(0, 6, 4, 1, RED)
    back.rect(0, 7, 4, 5, BOOT)
    back.rect(0, 11, 4, 1, BOOT_D)

    right = Face(4, 12)
    right.rect(0, 0, 4, 6, SKIN)
    right.rect(0, 6, 4, 1, RED)
    right.rect(0, 7, 4, 5, BOOT)
    right.rect(0, 11, 4, 1, BOOT_D)

    left = Face(4, 12)
    left.rect(0, 0, 4, 6, SKIN)
    left.rect(0, 6, 4, 1, RED)
    left.rect(0, 7, 4, 5, BOOT)
    left.rect(0, 11, 4, 1, BOOT_D)

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (right, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (left, x0 + 8, y0 + 4), (back, x0 + 12, y0 + 4),
    ]


# =====================================================================


def main():
    faces = []
    faces += build_head()
    faces += build_body()
    faces += build_arm(False, 40, 16)    # 右臂
    faces += build_arm(True, 32, 48)     # 左臂
    faces += build_leg(0, 16, boot_red_col=0)    # 右腿：红细线在前脸外侧列
    faces += build_leg(16, 48, boot_red_col=3)   # 左腿：红细线在前脸外侧列

    for f, _, _ in faces:
        if getattr(f, 'do_outline', True):
            outline_face(f)

    canvas = bake(faces)
    out = Image.new('RGBA', (W, H), T)
    for y in range(H):
        for x in range(W):
            out.putpixel((x, y), canvas[y][x])

    path = 'src/main/resources/assets/piranport/textures/skin/skin_22.png'
    out.save(path)
    print('saved', path)

    # 导出放大预览
    out.resize((512, 512), Image.NEAREST).save('/tmp/skin22_new_big.png')
    # 3D 视角预览（正/背/侧三视图拼合）
    compose_view(out).save('/tmp/skin22_new_view.png')


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
