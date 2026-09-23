# -*- coding: utf-8 -*-
"""
重画皮肤核心 22（萤火虫）的 64x64 玩家皮肤贴图。
参考原画: 战舰少女R L_NORMAL_82.png（zjsnrwiki）
构图对齐原画: 金色/橙色头发 + 银白羽翼发饰、苍白皮肤、绿色眼睛、
深蓝灰海军制服 + 橙色滚边 + 金色纽扣。

贴图按 Minecraft 64x64 玩家皮肤标准分区写入（每个面一张位图，含外层叠层区）。
"""
from PIL import Image

W = H = 64
T = (0, 0, 0, 0)          # 透明

# ---------- 调色板 ----------
OUT   = (10, 10, 14, 255)        # 纯黑描边
HAIR  = (232, 176, 66, 255)      # 金色头发
HAIR_D= (186, 132, 40, 255)      # 头发暗部
HAIR_L= (252, 226, 150, 255)     # 头发亮部
SIL   = (222, 226, 238, 255)     # 银白发饰
SIL_D = (152, 160, 178, 255)
SKIN  = (252, 226, 211, 255)     # 苍白皮肤
SKIN_D= (226, 176, 156, 255)
EYE   = (58, 150, 84, 255)       # 绿色眼睛
EYE_D = (22, 58, 36, 255)
COAT  = (28, 34, 62, 255)        # 深蓝灰军装
COAT_D= (16, 20, 40, 255)        # 暗部/衣褶
COAT_L= (66, 82, 138, 255)       # 亮部
ORNG  = (216, 118, 34, 255)      # 橙色滚边
GOLD  = (238, 194, 92, 255)      # 金色纽扣
WHITE = (244, 248, 255, 255)     # 衬衫
PANTS = (26, 34, 68, 255)        # 长裤
PANTS_D=(14, 18, 38, 255)
BOOT  = (44, 32, 28, 255)        # 深色长靴
BOOT_D= (26, 18, 16, 255)


class Face:
    """一个 64x64 面位图；坐标相对该面左上角。"""

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

    def frame(self, x, y, w, h, c):
        """只描最外圈的边（矩形轮廓）。"""
        for xx in range(x, x + w):
            self.set(xx, y, c)
            self.set(xx, y + h - 1, c)
        for yy in range(y, y + h):
            self.set(x, yy, c)
            self.set(x + w - 1, yy, c)

    def outline(self, c=OUT):
        """把整张面最外缘描一圈纯黑（原画像素化约定：只加最外侧黑描边）。"""
        for xx in range(self.w):
            for yy in range(self.h):
                if not self.is_opaque(xx, yy):
                    continue
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    nx, ny = xx + dx, yy + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h:
                        if not self.is_opaque(nx, ny):
                            pass  # 洞口位置留给贴图外天然透明，不描里面
        # 只描最外圈边框
        for xx in range(self.w):
            for yy in range(self.h):
                if self.is_opaque(xx, yy):
                    if xx == 0 or yy == 0 or xx == self.w - 1 or yy == self.h - 1:
                        self.set(xx, yy, c)

    def paste(self, canvas, ox, oy):
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if c[3] > 0:
                    canvas[oy + y][ox + x] = c


def edge_outline(f, c=OUT):
    """给 face 中每个不透明像素若四邻有不透明像素则保持不变；
    真正的黑描边在绘制阶段手工指定，这里只在轮廓与透明交界处补黑边。"""
    src = [row[:] for row in f.px]
    for y in range(f.h):
        for x in range(f.w):
            if src[y][x][3] == 0:
                continue
            # 若右/下侧为透明，则在该像素的右/下画黑边（把轮廓推向透明一侧）
            for dx, dy in ((1, 0), (0, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < f.w and 0 <= ny < f.h and src[ny][nx][3] == 0:
                    pass


def outline_face(f, c=OUT):
    """在 face 内所有「不透明像素的邻居是空洞」的一侧补上纯黑描边。
    这是项目既定的像素化约定：构图对齐原画，只在最外层加黑描边。"""
    src = [row[:] for row in f.px]

    def opaque(x, y):
        if 0 <= x < f.w and 0 <= y < f.h:
            return src[y][x][3] > 0
        return False

    for y in range(f.h):
        for x in range(f.w):
            if not opaque(x, y):
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                # 只处理面内邻格；面外的边缘由贴图本身的边界体现
                if 0 <= nx < f.w and 0 <= ny < f.h and not opaque(nx, ny):
                    src[ny][nx] = c
    for y in range(f.h):
        for x in range(f.w):
            if src[y][x][3] > 0:
                f.set(x, y, src[y][x])


def bake(faces):
    """把各面位图贴到 64x64 皮肤画布上，超出叠层区的像素直接丢弃。"""
    canvas = [[T] * W for _ in range(H)]
    for f, ox, oy in faces:
        f.paste(canvas, ox, oy)
    return canvas


def to_topleft_origin(x, z_offset=0.0, u_origin=0.0):
    return x


# =====================================================================
# 头部 —— 8x8x8，展开于 (0,0)
#   top(8,0) bottom(16,0) right(0,8) front(8,8) left(16,8) back(24,8)
#   叠层: top(40,0) bottom(48,0) right(32,8) front(40,8) left(48,8) back(56,8)
# =====================================================================


def build_head():
    top = Face(8, 8)
    # 头顶：金黄头发，中央一道分缝亮线
    top.rect(0, 0, 8, 8, HAIR)
    top.rect(1, 0, 6, 1, HAIR_L)
    top.rect(3, 3, 2, 1, HAIR_D)

    bot = Face(8, 8)
    bot.rect(0, 0, 8, 8, HAIR_D)          # 下巴底面/脖颈窝：暗色
    bot.rect(2, 2, 4, 4, SKIN_D)

    front = Face(8, 8)
    # 底色：脸
    front.rect(0, 0, 8, 8, SKIN)
    # 额头碎发（刘海）：金黄，中间略长
    front.rect(0, 0, 8, 1, HAIR)
    front.rect(0, 1, 1, 3, HAIR)
    front.rect(7, 1, 1, 3, HAIR)
    front.set(2, 1, HAIR)
    front.set(5, 1, HAIR)
    # 绿色眼睛 2x1，中间留一格做鼻梁
    front.rect(1, 2, 2, 1, EYE)
    front.rect(5, 2, 2, 1, EYE)
    front.set(1, 3, EYE_D)
    front.set(6, 3, EYE_D)
    # 腮红/阴影 → 下颊
    front.rect(2, 5, 4, 1, SKIN_D)

    back = Face(8, 8)
    back.rect(0, 0, 8, 8, HAIR)
    back.rect(0, 1, 8, 2, HAIR_D)         # 后脑发暗部
    back.rect(2, 4, 4, 4, HAIR)

    right = Face(8, 8)
    right.rect(0, 0, 8, 8, HAIR)
    right.rect(3, 1, 5, 6, SKIN)          # 侧面脸（后侧留发）
    right.rect(2, 1, 1, 6, HAIR)
    right.set(4, 2, EYE)                  # 侧视眼睛（贴前缘，不跨面重复）
    right.rect(3, 6, 4, 1, SKIN_D)

    left = Face(8, 8)
    left.rect(0, 0, 8, 8, HAIR)
    left.rect(0, 1, 5, 6, SKIN)
    left.rect(5, 1, 1, 6, HAIR)
    left.set(3, 2, EYE)
    left.rect(1, 6, 4, 1, SKIN_D)

    # ---- 叠层（帽子/配饰区）：银白羽翼发饰 ----
    l_top = Face(8, 8)                    # 头顶叠层
    l_top.set(0, 0, T)
    l_top.rect(0, 0, 2, 1, HAIR_L)
    l_top.rect(2, 0, 4, 1, SIL)           # 银色头饰正面
    l_top.set(2, 0, T)
    l_top.set(5, 0, T)

    l_front = Face(8, 8)
    # 额前银白发饰 + 两侧羽翼
    l_front.rect(1, 0, 6, 1, SIL)
    l_front.set(1, 0, T)
    l_front.set(6, 0, T)
    l_front.rect(0, 1, 1, 1, SIL_D)
    l_front.rect(7, 1, 1, 1, SIL_D)
    l_front.rect(2, 1, 4, 1, GOLD)
    l_front.set(3, 1, SIL)
    l_front.set(4, 1, SIL)

    l_right = Face(8, 8)
    l_right.rect(0, 0, 1, 1, SIL)
    l_right.rect(3, 0, 4, 1, SIL)         # 侧翼
    l_right.set(3, 0, T)
    l_right.set(6, 0, SIL_D)

    l_left = Face(8, 8)
    l_left.rect(7, 0, 1, 1, SIL)
    l_left.rect(1, 0, 4, 1, SIL)
    l_left.set(4, 0, T)
    l_left.set(1, 0, SIL_D)

    l_back = Face(8, 8)
    l_back.rect(2, 0, 4, 1, SIL)
    l_back.set(2, 0, T)
    l_back.set(5, 0, T)
    l_back.set(3, 0, SIL_D)
    l_back.set(4, 0, SIL_D)

    return [
        (top, 8, 0), (bot, 16, 0),
        (right, 0, 8), (front, 8, 8), (left, 16, 8), (back, 24, 8),
        (l_top, 40, 0), (Face(8, 8), 48, 0),          # 叠层底=空
        (l_right, 32, 8), (l_front, 40, 8), (l_left, 48, 8), (l_back, 56, 8),
    ]


# =====================================================================
# 身体 —— 8x12x4
#   顶层 (20,16) 底层 (28,16) 右(16,20) 前(20,20) 左(28,20) 后(32,20)
#   叠层 顶层(20,32) 底层(28,32) 右(16,36) 前(20,36) 左(28,36) 后(32,36)
# =====================================================================


def build_body():
    top = Face(8, 4)
    # 肩/脖颈处：颈部露肤，两侧肩章
    top.rect(0, 0, 8, 4, COAT)
    top.rect(2, 0, 4, 2, SKIN_D)
    top.rect(3, 0, 2, 1, SKIN)
    top.rect(0, 3, 2, 1, GOLD)
    top.rect(6, 3, 2, 1, GOLD)

    bot = Face(8, 4)
    bot.rect(0, 0, 8, 4, COAT_D)
    bot.rect(2, 1, 4, 2, PANTS_D)

    front = Face(8, 12)
    # 上衣主体
    front.rect(0, 0, 8, 12, COAT)
    # 双排扣：中央橙色滚边 + 左右金色纽扣
    front.rect(3, 1, 2, 10, COAT_L)
    front.rect(3, 1, 2, 1, ORNG)
    for yy in (3, 5, 7, 9):
        front.set(2, yy, GOLD)
        front.set(5, yy, GOLD)
    # 白色内衬领口
    front.rect(2, 0, 4, 1, WHITE)
    front.set(3, 1, WHITE)
    front.set(4, 1, WHITE)
    # 腰带
    front.rect(0, 10, 8, 1, COAT_D)
    front.set(3, 10, GOLD)
    front.set(4, 10, GOLD)
    # 下摆橙色滚边
    front.rect(0, 11, 8, 1, ORNG)

    back = Face(8, 12)
    back.rect(0, 0, 8, 12, COAT)
    back.rect(0, 0, 8, 1, COAT_D)         # 后领
    back.rect(3, 4, 2, 7, COAT_D)         # 背部中缝
    back.rect(1, 2, 6, 1, COAT_L)         # 肩胛高光
    back.rect(0, 11, 8, 1, ORNG)          # 下摆滚边

    right = Face(4, 12)
    right.rect(0, 0, 4, 12, COAT)
    right.rect(0, 0, 1, 12, COAT_L)
    right.rect(0, 10, 4, 1, ORNG)

    left = Face(4, 12)
    left.rect(0, 0, 4, 12, COAT)
    left.rect(3, 0, 1, 12, COAT_L)
    left.rect(0, 10, 4, 1, ORNG)

    return [
        (top, 20, 16), (bot, 28, 16),
        (right, 16, 20), (front, 20, 20), (left, 28, 20), (back, 32, 20),
        (Face(8, 4), 20, 32), (Face(8, 4), 28, 32),
        (Face(4, 12), 16, 36), (Face(8, 12), 20, 36),
        (Face(4, 12), 28, 36), (Face(8, 12), 32, 36),
    ]


# =====================================================================
# 手臂 —— 4x12x4（右臂用 (40,16) 区，左臂用 (32,48) 区）
# =====================================================================


def build_arm(mirror, x0, y0, sleeve_len=4):
    """mirror=True 表示左臂（左右镜像）。"""
    def mx(x, w=4):
        return (w - 1 - x) if mirror else x

    top = Face(4, 4)
    top.rect(0, 0, 4, 4, COAT)
    top.set(1, 1, COAT_L)
    top.set(2, 2, COAT_L)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, SKIN_D)
    bot.set(1, 1, SKIN)
    bot.set(2, 2, SKIN)

    front = Face(4, 12)
    # 袖口以上为军装
    front.rect(0, 0, 4, sleeve_len, COAT)
    front.rect(0, sleeve_len - 1, 4, 1, ORNG)   # 袖口橙色滚边
    # 裸露小臂
    front.rect(0, sleeve_len, 4, 12 - sleeve_len, SKIN)
    front.rect(0, 8, 4, 1, SKIN_D)              # 臂弯阴影
    front.set(mx(0), 1, GOLD)                   # 肩纽扣
    front.rect(0, 0, 4, 1, COAT_L)

    back = Face(4, 12)
    back.rect(0, 0, 4, sleeve_len, COAT)
    back.rect(0, sleeve_len - 1, 4, 1, ORNG)
    back.rect(0, sleeve_len, 4, 12 - sleeve_len, SKIN)
    back.rect(0, 8, 4, 1, SKIN_D)

    right = Face(4, 12)   # 外侧
    right.rect(0, 0, 4, sleeve_len, COAT)
    right.rect(0, sleeve_len - 1, 4, 1, ORNG)
    right.rect(0, sleeve_len, 4, 12 - sleeve_len, SKIN)
    right.rect(0, 3, 1, 2, GOLD)

    left = Face(4, 12)    # 内侧
    left.rect(0, 0, 4, sleeve_len, COAT_D)
    left.rect(0, sleeve_len - 1, 4, 1, ORNG)
    left.rect(0, sleeve_len, 4, 12 - sleeve_len, SKIN_D)

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (right, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (left, x0 + 12, y0 + 4), (back, x0 + 8, y0 + 4),
    ]


# =====================================================================
# 腿 —— 4x12x4（右腿 (0,16) 区，左腿 (16,48) 区）
# =====================================================================


def build_leg(x0, y0, boot=True):
    top = Face(4, 4)
    top.rect(0, 0, 4, 4, PANTS)

    bot = Face(4, 4)
    bot.rect(0, 0, 4, 4, BOOT)
    bot.rect(1, 1, 2, 2, BOOT_D)

    front = Face(4, 12)
    front.rect(0, 0, 4, 3, PANTS)          # 大腿
    front.rect(0, 3, 4, 1, COAT_D)         # 衣摆/裙边
    front.rect(0, 4, 4, 4, PANTS)          # 小腿
    front.rect(0, 8, 4, 4, BOOT)           # 长靴
    front.rect(0, 8, 4, 1, BOOT_D)         # 靴口
    front.set(2, 10, BOOT_D)               # 靴面光泽

    back = Face(4, 12)
    back.rect(0, 0, 4, 3, PANTS)
    back.rect(0, 3, 4, 1, COAT_D)
    back.rect(0, 4, 4, 4, PANTS)
    back.rect(0, 8, 4, 4, BOOT)
    back.rect(0, 8, 4, 1, BOOT_D)
    back.set(1, 10, BOOT_D)

    right = Face(4, 12)
    right.rect(0, 0, 4, 8, PANTS)
    right.rect(0, 0, 1, 8, PANTS_D)
    right.rect(0, 8, 4, 4, BOOT)
    right.rect(0, 8, 4, 1, BOOT_D)

    left = Face(4, 12)
    left.rect(0, 0, 4, 8, PANTS)
    left.rect(3, 0, 1, 8, PANTS_D)
    left.rect(0, 8, 4, 4, BOOT)
    left.rect(0, 8, 4, 1, BOOT_D)

    return [
        (top, x0 + 4, y0), (bot, x0 + 8, y0),
        (right, x0, y0 + 4), (front, x0 + 4, y0 + 4),
        (left, x0 + 12, y0 + 4), (back, x0 + 8, y0 + 4),
    ]


# =====================================================================


def main():
    faces = []
    faces += build_head()
    faces += build_body()
    faces += build_arm(False, 40, 16)    # 右臂
    faces += build_arm(True, 32, 48)     # 左臂
    faces += build_leg(0, 16)            # 右腿
    faces += build_leg(16, 48)           # 左腿

    for f, _, _ in faces:
        outline_face(f)

    canvas = bake(faces)
    out = Image.new('RGBA', (W, H), T)
    for y in range(H):
        for x in range(W):
            out.putpixel((x, y), canvas[y][x])

    # 验收：所有绘制像素必须在合法皮肤区域内
    path = 'src/main/resources/assets/piranport/textures/skin/skin_22.png'
    out.save(path)
    print('saved', path)

    # 导出放大预览
    out.resize((512, 512), Image.NEAREST).save('/tmp/skin22_new_big.png')
    # 3D 视角预览（正/背/侧三视图拼合）
    compose_view(out).save('/tmp/skin22_new_view.png')


def compose_view(im):
    """把 64x64 皮肤按玩家模型拼出正/背/侧三视图，便于肉眼验收。"""
    vw, vh = 4 * 8, 12 * 4
    view = Image.new('RGBA', (vw * 3 + 4, vh), (40, 40, 48, 255))

    def blit(face_box, dest, w, h, scale=4):
        fw, fh = face_box[2] - face_box[0], face_box[3] - face_box[1]
        crop = im.crop(face_box).resize((fw * scale, fh * scale), Image.NEAREST)
        view.alpha_composite(crop, dest)

    # 正面
    ox = 0
    blit((8, 8, 16, 16), (ox + 4 * 4, 0), 8, 8)             # 头
    blit((20, 20, 28, 32), (ox + 4 * 4, 8 * 4), 8, 12)      # 躯干
    blit((44, 20, 48, 32), (ox, 8 * 4), 4, 12)              # 右臂
    blit((36, 52, 40, 64), (ox + 12 * 4, 8 * 4), 4, 12)     # 左臂
    blit((4, 20, 8, 32), (ox + 4 * 4, 20 * 4), 4, 12)       # 右腿
    blit((20, 52, 24, 64), (ox + 8 * 4, 20 * 4), 4, 12)     # 左腿

    # 背面
    ox = vw + 2
    blit((24, 8, 32, 16), (ox + 4 * 4, 0), 8, 8)
    blit((32, 20, 40, 32), (ox + 4 * 4, 8 * 4), 8, 12)
    blit((40, 20, 44, 32), (ox, 8 * 4), 4, 12)
    blit((32, 52, 36, 64), (ox + 12 * 4, 8 * 4), 4, 12)
    blit((8, 20, 12, 32), (ox + 4 * 4, 20 * 4), 4, 12)
    blit((16, 52, 20, 64), (ox + 8 * 4, 20 * 4), 4, 12)

    # 侧面（右）
    ox = (vw + 2) * 2
    blit((0, 8, 8, 16), (ox + 4 * 4, 0), 8, 8)
    blit((16, 20, 20, 32), (ox + 4 * 4, 8 * 4), 4, 12)
    blit((0, 20, 4, 32), (ox, 8 * 4), 4, 12)
    blit((0, 52, 4, 64), (ox + 4 * 4, 20 * 4), 4, 12)

    return view


if __name__ == '__main__':
    main()
