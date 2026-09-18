#!/usr/bin/env python3
"""基林改（skin_22）经典 Steve 体型（4px 手臂）玩家皮肤生成工具。

分辨率与画风基准 = 大凤（skin_4）：64×64。头部 12 个面片**原样照搬** skin_4 的
结构（只做配色替换：粉发 → 金发、粉瞳 → 蓝瞳），保证游戏内的眼睛/脸型与大凤
完全一致；身体部分按基林改立绘（L_NORMAL_1093，zjsnrwiki）绘制，沿用同一套
像素密度与色阶做法。

立绘特征（L_NORMAL_1093 观察所得）：
  - 蓬松金色短发（及肩外翘），**左鬓一束细麻花辫**（观众视角左侧）
  - 蓝色大眼；白皙肤色
  - **白色水手帽**（宽檐、正前方红色斜纹小帽徽）
  - 藏青水手领（背后方形披领）+ 红色领巾；左肩金色五角星臂章
  - 白色短泡泡袖衬衫 + 海军蓝背带；**露脐**（腰腹露肤）
  - 红色短裙（白色褶边、两侧各一颗蓝色五角星）+ 棕色腰带（金色圆扣）
  - 白色过膝长筒袜，袜口白花边 + **蓝色五角星**；白袜蓝条纹
  - 白/红玛丽珍鞋（红色鞋头与鞋跟、蓝色鞋带扣）

配色取向：基林改是**红色主调**（红裙 + 藏青领），与萤火虫（深蓝水手背心裙 +
红黑围巾 + 黑过膝袜）几乎处处相反，两者不可混用。

体型说明：ShipGirlRenderer 对 skin_22 走默认 PlayerModel（classic，4px 手臂），
所以本脚本按经典 Steve 布局出图，不能改用 Alex/slim 的 3px 手臂。

输出：
  src/main/resources/assets/piranport/textures/skin/skin_22.png
  src/main/resources/assets/piranport/textures/item/skin_core_22.png
  build/offline-renders/keeling_skin_preview.png
  build/offline-renders/keeling_skin_sheet.png

运行：cd tools && python3 keeling_skin_tools.py

UV 约定（与原版一致，render_model 按此投影）：
  正面面片局部 x=0 是角色左侧（-x），模型朝 -z。
  WEST 面片（-x）局部 x=0 为后脑，EAST 面片（+x）局部 x=0 为面部。
  UP 面片局部 y=0 为后脑、y=7 为面部；DOWN 面片局部 y=0 为面部。
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
# 画风基准：大凤（skin_4）的头部结构被本脚本直接照搬，只做配色替换
TAIHOU_SKIN = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_4.png"
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_22.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_22.png"
PREVIEW_OUT = ROOT / "build/offline-renders/keeling_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/keeling_skin_sheet.png"
BASE_SIZE = 64          # 标准皮肤 UV 空间，与大凤一致
SCALE = 1               # 1:1 输出 64×64
SIZE = BASE_SIZE * SCALE


# ---------------------------------------------------------------- 调色板

P = {
    "none": (0, 0, 0, 0),
    # 金色短发 —— 6 个色阶（发梢深 → 高光浅）；基林改是亮金，比胡德更偏柠檬黄
    "hair_deep": (196, 152, 62, 255),
    "hair_deep2": (214, 172, 84, 255),
    "hair_shadow": (232, 196, 110, 255),
    "hair": (246, 218, 132, 255),
    "hair_mid": (252, 234, 168, 255),
    "hair_light": (255, 246, 208, 255),
    # 皮肤 —— 沿用大凤的肤色（基林改同为白皙暖调）
    "skin_light": (254, 243, 229, 255),
    "skin": (252, 231, 216, 255),
    "skin_shadow": (249, 205, 191, 255),
    "skin_deep": (252, 196, 182, 255),
    "blush": (247, 186, 176, 255),
    # 五官（蓝瞳，与大凤同结构不同色）
    "lash": (110, 78, 62, 255),
    "lash_light": (198, 172, 158, 255),
    "eye_deep": (58, 116, 206, 255),
    "eye_light": (170, 220, 250, 255),
    "eye_white": (250, 250, 252, 255),
    # 白水手帽 / 白衬衫 / 白过膝袜。
    # 立绘实测上衣/袜子就是纯白 #ffffff，蓝通道不能高于红绿，
    # 否则在游戏光照下整张图会往青色偏（第一版发青的主因之一）。
    "white": (255, 255, 255, 255),
    "white_sh": (238, 240, 242, 255),
    "white_deep": (214, 218, 224, 255),
    # 藏青（水手领 / 背带）。第一版 rgb(42,56,96) 蓝通道过高，
    # 和红裙并置时整体偏冷偏紫；立绘的水手领是**偏灰的深藏青**。
    "navy": (48, 58, 82, 255),
    "navy_light": (74, 88, 118, 255),
    "navy_deep": (30, 38, 56, 255),
    # 红（短裙 / 领巾 / 鞋）。立绘实测裙红 = #da2f1f = rgb(218,47,31)：
    # 蓝通道极低（31）且偏橙。第一版用 rgb(198,44,52)（蓝通道 52）偏品红，
    # 是"整体发青"最主要的原因——红一旦偏洋红，画面就压不住青。
    "red": (218, 47, 31, 255),
    "red_light": (238, 84, 56, 255),
    "red_deep": (168, 30, 22, 255),
    # 金（腰带扣 / 五角星 / 帽徽）
    "gold": (240, 200, 96, 255),
    "gold_deep": (190, 144, 44, 255),
    # 棕色腰带。第一版 rgb(108,78,50) 蓝通道偏高显得发紫，
    # 立绘腰带是偏暖的中棕。
    "belt": (122, 84, 48, 255),
    "belt_deep": (88, 58, 32, 255),
    # 蓝（星星 / 鞋带扣）。立绘的星是**亮天蓝**而不是深靛蓝，
    # 深靛蓝在红裙上会显脏、也加重整体冷感。
    "star_blue": (74, 154, 232, 255),
    "star_deep": (44, 106, 190, 255),
}

# 大凤头部用到的全部颜色 → 基林改配色（结构一模一样，只换色）。
# 一律引用上面的调色板，避免同一份颜色在两处各写一遍后走样。
TAIHOU_TO_KEELING = {
    # 粉发 → 金发（6 个发色阶一一对应，保留大凤头发上的明暗分布）
    (230, 116, 142): P["hair_deep"][:3],
    (235, 118, 149): P["hair_deep2"][:3],
    (238, 136, 163): P["hair_shadow"][:3],
    (242, 144, 167): P["hair"][:3],
    (248, 164, 174): P["hair_mid"][:3],
    # 皮肤：保留大凤原肤色，逐值恒等映射
    (252, 196, 182): P["skin_deep"][:3],
    (249, 205, 191): P["skin_shadow"][:3],
    (252, 231, 216): P["skin"][:3],
    (254, 243, 229): P["skin_light"][:3],
    # 睫毛 / 眼睑
    (97, 51, 51): P["lash"][:3],
    (184, 157, 167): P["lash_light"][:3],
    # 粉瞳 → 蓝瞳（基林改是明亮宝蓝瞳）
    (82, 173, 203): P["eye_deep"][:3],
    (152, 238, 244): P["eye_light"][:3],
    # 高光
    (247, 247, 247): P["eye_white"][:3],
}


# ---------------------------------------------------------------- UV 表

# (起点 x, 起点 y, 宽, 高)，全部为 64 空间；classic 体型，手臂宽 4
FACES = {
    # 头部 8×8×8
    "head_top": (8, 0, 8, 8),
    "head_bottom": (16, 0, 8, 8),
    "head_left": (0, 8, 8, 8),          # -x 角色左侧：x0 后脑 → x7 面部
    "head_front": (8, 8, 8, 8),
    "head_right": (16, 8, 8, 8),        # +x 角色右侧：x0 面部 → x7 后脑
    "head_back": (24, 8, 8, 8),
    "head_top_ov": (40, 0, 8, 8),
    "head_bottom_ov": (48, 0, 8, 8),
    "head_left_ov": (32, 8, 8, 8),
    "head_front_ov": (40, 8, 8, 8),
    "head_right_ov": (48, 8, 8, 8),
    "head_back_ov": (56, 8, 8, 8),
    # 躯干 8×12×4
    "body_top": (20, 16, 8, 4),
    "body_bottom": (28, 16, 8, 4),
    "body_left": (16, 20, 4, 12),       # -x：x0 后 → x3 前
    "body_front": (20, 20, 8, 12),
    "body_right": (28, 20, 4, 12),      # +x：x0 前 → x3 后
    "body_back": (32, 20, 8, 12),
    "body_top_ov": (20, 32, 8, 4),
    "body_bottom_ov": (28, 32, 8, 4),
    "body_left_ov": (16, 36, 4, 12),
    "body_front_ov": (20, 36, 8, 12),
    "body_right_ov": (28, 36, 4, 12),
    "body_back_ov": (32, 36, 8, 12),
    # 右臂 classic 4×12×4（玩家右臂 = +x）
    "rarm_top": (44, 16, 4, 4),
    "rarm_bottom": (48, 16, 4, 4),
    "rarm_left": (40, 20, 4, 12),       # 内侧：x0 后 → x3 前
    "rarm_front": (44, 20, 4, 12),
    "rarm_right": (48, 20, 4, 12),      # 外侧：x0 前 → x3 后
    "rarm_back": (52, 20, 4, 12),
    "rarm_top_ov": (44, 32, 4, 4),
    "rarm_bottom_ov": (48, 32, 4, 4),
    "rarm_left_ov": (40, 36, 4, 12),
    "rarm_front_ov": (44, 36, 4, 12),
    "rarm_right_ov": (48, 36, 4, 12),
    "rarm_back_ov": (52, 36, 4, 12),
    # 左臂 classic 4×12×4（玩家左臂 = -x）
    "larm_top": (36, 48, 4, 4),
    "larm_bottom": (40, 48, 4, 4),
    "larm_left": (32, 52, 4, 12),       # 外侧：x0 后 → x3 前
    "larm_front": (36, 52, 4, 12),
    "larm_right": (40, 52, 4, 12),      # 内侧：x0 前 → x3 后
    "larm_back": (44, 52, 4, 12),
    "larm_top_ov": (52, 48, 4, 4),
    "larm_bottom_ov": (56, 48, 4, 4),
    "larm_left_ov": (48, 52, 4, 12),
    "larm_front_ov": (52, 52, 4, 12),
    "larm_right_ov": (56, 52, 4, 12),
    "larm_back_ov": (60, 52, 4, 12),
    # 右腿 4×12×4
    "rleg_top": (4, 16, 4, 4),
    "rleg_bottom": (8, 16, 4, 4),
    "rleg_left": (0, 20, 4, 12),        # 内侧：x0 后 → x3 前
    "rleg_front": (4, 20, 4, 12),
    "rleg_right": (8, 20, 4, 12),       # 外侧：x0 前 → x3 后
    "rleg_back": (12, 20, 4, 12),
    "rleg_top_ov": (4, 32, 4, 4),
    "rleg_bottom_ov": (8, 32, 4, 4),
    "rleg_left_ov": (0, 36, 4, 12),
    "rleg_front_ov": (4, 36, 4, 12),
    "rleg_right_ov": (8, 36, 4, 12),
    "rleg_back_ov": (12, 36, 4, 12),
    # 左腿 4×12×4
    "lleg_top": (20, 48, 4, 4),
    "lleg_bottom": (24, 48, 4, 4),
    "lleg_left": (16, 52, 4, 12),       # 外侧：x0 后 → x3 前
    "lleg_front": (20, 52, 4, 12),
    "lleg_right": (24, 52, 4, 12),      # 内侧：x0 前 → x3 后
    "lleg_back": (28, 52, 4, 12),
    "lleg_top_ov": (4, 48, 4, 4),
    "lleg_bottom_ov": (8, 48, 4, 4),
    "lleg_left_ov": (0, 52, 4, 12),
    "lleg_front_ov": (4, 52, 4, 12),
    "lleg_right_ov": (8, 52, 4, 12),
    "lleg_back_ov": (12, 52, 4, 12),
}

HEAD_KEYS = [k for k in FACES if k.startswith("head_")]


class Sheet:
    """按 FACES 表在贴图上按局部坐标作画。"""

    def __init__(self, img: Image.Image, scale: int = SCALE) -> None:
        self.img = img
        self.scale = scale

    def px(self, face: str, x: int, y: int, color) -> None:
        ox, oy, w, h = FACES[face]
        if not (0 <= x < w * self.scale and 0 <= y < h * self.scale):
            return
        self.img.putpixel((ox * self.scale + x, oy * self.scale + y), color)

    def rect(self, face: str, x: int, y: int, w: int, h: int, color) -> None:
        for i in range(w):
            for j in range(h):
                self.px(face, x + i, y + j, color)

    def fill(self, face: str, color) -> None:
        ox, oy, w, h = FACES[face]
        self.rect(face, 0, 0, w * self.scale, h * self.scale, color)

    def sym(self, face: str, x: int, y: int, w: int, h: int, color) -> None:
        """左右对称画一矩形（镜像到 x' = 面片宽 - x - w）。"""
        fw = FACES[face][2] * self.scale
        self.rect(face, x, y, w, h, color)
        self.rect(face, fw - x - w, y, w, h, color)

    def sympx(self, face: str, x: int, y: int, color) -> None:
        fw = FACES[face][2] * self.scale
        self.px(face, x, y, color)
        self.px(face, fw - 1 - x, y, color)

    def get(self, face: str, x: int, y: int):
        ox, oy, w, h = FACES[face]
        return self.img.getpixel((ox * self.scale + x, oy * self.scale + y))


# ---------------------------------------------------------------- 头部：照搬大凤

def copy_head(s: Sheet) -> None:
    """把 skin_4（大凤）的头部 6 个面片 + 6 个帽层面片原样搬过来，只替换配色。

    大凤皮肤是 64×64，本皮肤同样是 64×64，所以是严格 1:1 像素对像素搬运，
    游戏内眼睛的每一个像素都和大凤一致，只把粉发换成金发、粉瞳换成蓝瞳。
    """
    src = Image.open(TAIHOU_SKIN).convert("RGBA")
    for key in HEAD_KEYS:                      # 先清空，避免残留底图内容
        s.fill(key, P["none"])
    for key in HEAD_KEYS:
        ox, oy, w, h = FACES[key]
        for y in range(h):
            for x in range(w):
                c = src.getpixel((ox + x, oy + y))
                if c[3] == 0:
                    continue
                nc = TAIHOU_TO_KEELING.get(c[:3], c[:3]) + (255,)
                s.px(key, x, y, nc)


def detail_ornament(s: Sheet) -> None:
    """基林改特征：白水手帽 + 左鬓麻花辫。

    水手帽画在帽层（*_ov）。因为帽檐是 8×8 的平面，无法做出真正的斜面，
    处理办法是**顶部帽层铺满白**（俯视即整顶帽子），**四个侧面帽层只保留
    上缘 2 行**（游戏里看着就是一圈压在发际线上的宽檐），帽墙本身不画，
    这样侧视不会出现"帽子把头包起来"的塑料感。

    麻花辫：立绘中垂在**观众视角左侧** = 角色右侧 = +x = head_right 面片，
    所以辫子画在 head_right(_ov) 的下半段，斜向后下方收尾。
    """
    white, deep = P["white"], P["white_deep"]
    red, gold = P["red"], P["gold"]
    hair_d, hair_s = P["hair_deep"], P["hair_shadow"]

    # ---- 帽檐：四个侧面帽层各保留上缘 2 行
    for side in ("left", "front", "right", "back"):
        fo = f"head_{side}_ov"
        s.rect(fo, 0, 0, 8, 2, white)
        s.rect(fo, 0, 2, 8, 1, deep)          # 檐口阴影线，勾出帽檐厚度

    # ---- 帽顶：整片白 + 中心浅阴影（俯视是个圆盘）
    s.fill("head_top_ov", white)
    s.rect("head_top_ov", 2, 2, 4, 4, P["white_sh"])

    # ---- 正面：红色帽徽（立绘正前方那道斜红纹）
    s.rect("head_front_ov", 3, 0, 2, 1, red)
    s.px("head_front_ov", 5, 0, gold)

    # ---- 侧面：帽带尾端在右侧（角色右侧）垂下红白细条
    s.px("head_right_ov", 6, 2, red)
    s.px("head_right_ov", 6, 3, white)
    s.px("head_left_ov", 1, 2, red)
    s.px("head_left_ov", 1, 3, white)

    # ---- 左鬓麻花辫（角色右侧，+x，head_right / head_right_ov）
    # 辫子从耳前一路垂到下颌，用「深-浅」两色交错表现编发
    for i, y in enumerate(range(3, 8)):
        c = hair_d if i % 2 == 0 else hair_s
        s.px("head_right_ov", 6, y, c)
        s.px("head_right_ov", 7, y, hair_d)
    s.px("head_right_ov", 7, 7, P["hair_deep2"])       # 发梢
    s.px("head_right_ov", 6, 7, P["red"])              # 辫子末端的小红绳


# ---------------------------------------------------------------- 躯干

def draw_body(s: Sheet) -> None:
    """躯干（8 宽 × 12 高 × 4 厚）：白衬衫露脐 + 红短裙。

    高度分层（正面局部 y，0 = 领口，11 = 裙摆底）。这里的关键比例是
    **红裙必须占满 y5-9 共 5 行**，白褶边只留 y10-11 两行；第一版把白铺到
    y4 导致红只剩腰带一条，游戏里完全看不出是红裙。
      y 0    藏青水手领 + 红色领巾
      y 1-3  白衬衫 + 藏青背带 + 露脐
      y 4    白衬衫下摆荷叶边（短一截，露出腰腹）
      y 5    棕腰带 + 金色圆扣
      y 6-9  红短裙裙身（两侧蓝色五角星）
      y 10-11 白褶边裙摆
    """
    bf, bb = "body_front", "body_back"
    bl, br = "body_left", "body_right"
    bt, bbo = "body_top", "body_bottom"

    # ---- 底色：上白（衬衫）下红（裙）；红从 y5 起，一直铺到底
    for face in (bf, bb, bl, br):
        s.fill(face, P["white"])
    for face in (bf, bb, bl, br):
        s.rect(face, 0, 5, FACES[face][2], 7, P["red"])

    # ---- 正面：藏青水手领 + 红色领巾
    s.rect(bf, 0, 0, 8, 1, P["navy"])
    s.rect(bf, 2, 0, 4, 1, P["red"])                  # 领巾结
    # 领角：只在外侧两列压深，中间的领巾不被吃掉
    s.px(bf, 0, 0, P["navy_deep"])
    s.px(bf, 7, 0, P["navy_deep"])
    # 领巾垂下的三角（延伸到衬衫第一行）
    s.px(bf, 3, 1, P["red_deep"])
    s.px(bf, 4, 1, P["red_deep"])
    s.px(bf, 3, 2, P["red"])
    s.px(bf, 4, 2, P["red"])

    # ---- 正面：白衬衫 + 海军蓝背带（两条竖带，从肩垂到裙腰）
    s.rect(bf, 0, 1, 8, 1, P["white"])
    for x in (1, 2, 5, 6):
        s.px(bf, x, 1, P["navy"])
    # 背带继续往下（y2-4）
    s.px(bf, 1, 2, P["navy"])
    s.px(bf, 2, 2, P["navy"])
    s.px(bf, 5, 2, P["navy"])
    s.px(bf, 6, 2, P["navy"])
    s.px(bf, 1, 3, P["navy"])
    s.px(bf, 2, 3, P["navy"])
    s.px(bf, 5, 3, P["navy"])
    s.px(bf, 6, 3, P["navy"])

    # ---- 正面：露脐（y3 中间两列露肤，形成腰腹留白）
    s.rect(bf, 3, 3, 2, 1, P["skin"])
    # 衬衫下摆的荷叶边（y4 压一行浅白，做出「短上衣+露腰」的分界）
    s.rect(bf, 0, 4, 8, 1, P["white_sh"])
    s.px(bf, 3, 4, P["white"])
    s.px(bf, 4, 4, P["white"])

    # ---- 正面：左肩金色五角星臂章（观众视角右 = 角色左 = 局部 x7 一侧）
    s.px(bf, 6, 1, P["gold"])
    s.px(bf, 5, 1, P["gold"])
    s.px(bf, 7, 1, P["gold_deep"])

    # ---- 正面：棕腰带 + 金色圆扣（y5）
    s.rect(bf, 0, 5, 8, 1, P["belt"])
    s.rect(bf, 3, 5, 2, 1, P["gold"])
    s.px(bf, 3, 5, P["gold_deep"])
    s.px(bf, 0, 5, P["belt_deep"])
    s.px(bf, 7, 5, P["belt_deep"])

    # ---- 正面：红短裙两侧的蓝色五角星（立绘裙面左右各一颗）
    # 3×3 的星形：上排三点（左右肩+顶）、中排一点（星腰）、下排两点（脚）
    star = [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)]
    for base_x in (0, 5):
        for (dx, dy) in star:
            x, y = base_x + dx, 7 + dy
            if 0 <= x < 8 and 0 <= y < 12:
                s.px(bf, x, y, P["star_blue"])
    # 星脚下再加一点深蓝，让星形在红底上更扎实
    s.px(bf, 1, 9, P["star_deep"])
    s.px(bf, 6, 9, P["star_deep"])
    # 裙身明暗：两侧压深，中间留亮，让红裙有圆柱感
    s.rect(bf, 0, 6, 1, 4, P["red_deep"])
    s.rect(bf, 7, 6, 1, 4, P["red_deep"])
    s.rect(bf, 3, 6, 2, 1, P["red_light"])

    # ---- 正面：白褶边裙摆（y10-11）
    s.rect(bf, 0, 10, 8, 1, P["white"])
    for x in range(0, 8, 2):
        s.px(bf, x, 11, P["white"])
        s.px(bf, x + 1, 11, P["white_sh"])

    # ---- 背面：藏青方披领（水手服标志性的背后方片）+ 红裙
    s.rect(bb, 0, 0, 8, 1, P["white"])
    s.rect(bb, 1, 0, 6, 1, P["navy"])
    s.rect(bb, 2, 1, 4, 1, P["navy"])                 # 披领下缘的方形轮廓
    s.px(bb, 1, 1, P["navy_deep"])
    s.px(bb, 6, 1, P["navy_deep"])
    # 背面背带
    s.px(bb, 1, 2, P["navy"])
    s.px(bb, 6, 2, P["navy"])
    s.px(bb, 1, 3, P["navy"])
    s.px(bb, 6, 3, P["navy"])
    # 背面腰带 + 裙摆白褶边
    s.rect(bb, 0, 5, 8, 1, P["belt"])
    s.rect(bb, 0, 10, 8, 1, P["white"])
    for x in range(1, 8, 2):
        s.px(bb, x, 11, P["white_sh"])

    # ---- 侧面：白衬衫 + 红裙（y6-9 已是红的，只需补腰带和裙摆）
    for face in (bl, br):
        s.rect(face, 0, 4, 4, 1, P["white_sh"])
        s.rect(face, 0, 5, 4, 1, P["belt"])
        s.rect(face, 0, 10, 4, 1, P["white"])
        s.rect(face, 0, 6, 4, 4, P["red"])
        s.rect(face, 0, 11, 4, 1, P["white_sh"])

    # ---- 顶面：肩线（白衬衫肩 + 藏青背带）
    s.rect(bt, 0, 0, 8, 4, P["white"])
    for x in (1, 2, 5, 6):
        s.rect(bt, x, 0, 1, 4, P["navy"])
    s.px(bt, 0, 0, P["white_deep"])
    s.px(bt, 7, 0, P["white_deep"])
    # 底面不可见，铺裙内衬红即可
    s.fill(bbo, P["red_deep"])


def draw_body_overlay(s: Sheet) -> None:
    """躯干帽层（第二层皮肤）：只放立体装饰，避免整体加厚导致穿模。"""
    fo, bo = "body_front_ov", "body_back_ov"
    # 左肩星章做出一点点厚度（帽层加一颗，正面可见）
    s.px(fo, 6, 1, P["gold"])
    # 裙摆白边做出立体感（帽层只在最下一行补白）
    s.rect(fo, 0, 11, 8, 1, P["white"])
    s.rect(bo, 0, 11, 8, 1, P["white"])
    # 腰带扣的高光
    s.px(fo, 3, 5, P["gold"])


# ---------------------------------------------------------------- 四肢

def draw_arms(s: Sheet) -> None:
    """裸露肩臂 + 白色手套（腕口红边）。

    立绘里手臂**大部分是裸露的肤色**：肩头是一小块白色泡泡袖肩章（只盖住
    y0-1 两行），肘下才是白色手套。第一版把 y0-11 整段画成白色，手臂在
    游戏里会变成两根比躯干还粗的白柱子，完全不是基林改的样子。
    y 0 = 肩，11 = 手腕。分层：
      y 0-1  白色泡泡袖肩章（顶部两行）
      y 2-6  裸露上臂 + 肘
      y 7-8  手套束口（红边）
      y 9-11 白色手套主体
    """
    for side, faces in (("r", ("rarm_front", "rarm_back", "rarm_left", "rarm_right")),
                        ("l", ("larm_front", "larm_back", "larm_left", "larm_right"))):
        for face in faces:
            fw = FACES[face][2]
            # y 0-1：白色泡泡袖肩章
            s.rect(face, 0, 0, fw, 2, P["white"])
            for y in range(0, 2):
                for x in range(fw):
                    if (x + y) % 3 == 0:
                        s.px(face, x, y, P["white_sh"])
            s.rect(face, 0, 1, fw, 1, P["white_deep"])       # 袖口滚边
            # y 2-6：裸露上臂
            s.rect(face, 0, 2, fw, 5, P["skin"])
            s.rect(face, 0, 2, fw, 1, P["skin_shadow"])       # 肘部阴影
            # y 7-8：手套束口。
            # 注意：**手套束口不能做成整行红**。躯干的红裙正好也在 y7-9，
            # 手臂与躯干同高同深，整行红会连成一条横贯双臂的红杠（第一版
            # 预览里那两道"红肩章"就是这么来的）。这里只在束口行放一个
            # 红点做扣子，其余用白，切断视觉上的连续。
            s.rect(face, 0, 7, fw, 1, P["white_deep"])
            s.px(face, fw // 2, 7, P["red_deep"])
            s.rect(face, 0, 8, fw, 1, P["white_deep"])
            # y 9-11：白色手套
            s.rect(face, 0, 9, fw, 3, P["white_sh"])
            s.rect(face, 0, 9, 1, 3, P["white_deep"])
        # 顶面 = 肩头：裸露肤色，被背带压出一道藏青
        s.fill(f"{side}arm_top", P["skin"])
        s.rect(f"{side}arm_top", 1, 0, 2, 4, P["navy"])
        s.fill(f"{side}arm_bottom", P["white_sh"])


def draw_legs(s: Sheet) -> None:
    """腿部：**双腿都是白色过膝长筒袜**，袜口白花边 + 蓝色五角星；白/红玛丽珍鞋。

    立绘中两条腿都穿白过膝袜（袜口各一颗蓝星），这是基林改与萤火虫
    （左腿黑袜 / 右腿裸足）最容易混淆、必须搞对的一点。
    """
    for faces in (("rleg_front", "rleg_back", "rleg_left", "rleg_right"),
                  ("lleg_front", "lleg_back", "lleg_left", "lleg_right")):
        for face in faces:
            fw = FACES[face][2]
            s.fill(face, P["white"])                  # 白袜底色
            # y 0-1：大腿根部露肤（裙下）
            s.rect(face, 0, 0, fw, 2, P["skin"])
            s.rect(face, 0, 0, fw, 1, P["skin_shadow"])
            # y 2：袜口白花边（比袜身更亮的白 + 一列阴影）
            s.rect(face, 0, 2, fw, 1, P["white_deep"])
            # y 3-5：蓝色五角星（袜口正下方，与裙面同一套 3×3 星形）
            s.px(face, 1, 3, P["star_blue"])
            s.px(face, 0, 4, P["star_blue"])
            s.px(face, 1, 4, P["star_blue"])
            s.px(face, 2, 4, P["star_blue"])
            s.px(face, 1, 5, P["star_blue"])
            # 袜身竖向明暗，模拟圆筒腿
            s.rect(face, 0, 3, 1, 6, P["white_deep"])
            s.rect(face, fw - 1, 3, 1, 6, P["white_sh"])
            # y 9-11：玛丽珍鞋（红鞋头 / 红鞋跟 + 蓝鞋带扣）
            s.rect(face, 0, 9, fw, 3, P["white"])
            s.rect(face, 0, 9, fw, 1, P["red_deep"])  # 鞋口红线
            s.rect(face, 0, 10, fw, 2, P["red"])      # 红鞋身
            s.rect(face, 0, 11, fw, 1, P["red_deep"])  # 鞋底
            s.px(face, fw // 2, 10, P["star_blue"])   # 蓝色鞋带扣
        # 顶面 = 大腿根部：露肤；底面 = 鞋底
        s.fill(faces[0].split("_")[0] + "_top", P["skin"])
        s.fill(faces[0].split("_")[0] + "_bottom", P["belt_deep"])

    # 正面鞋头加一点白，做出「白鞋红头」的玛丽珍分色
    for face in ("rleg_front", "lleg_front"):
        s.rect(face, 1, 9, 2, 1, P["white"])


def create_skin() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), P["none"])
    s = Sheet(img)
    copy_head(s)                 # 头部：1:1 照搬大凤结构，只换金发/蓝瞳
    detail_ornament(s)           # 基林改特征：白水手帽 + 左鬓麻花辫
    draw_body(s)
    draw_body_overlay(s)
    draw_arms(s)
    draw_legs(s)
    return img


# ---------------------------------------------------------------- 预览渲染

# 3D 盒体 (x0, y0, z0, x1, y1, z1)，y 轴向上，z0 为正面（模型朝向 -z）
# classic 体型：手臂宽 4
PARTS = [
    ("head", (-4.0, 24.0, -4.0, 4.0, 32.0, 4.0),
     {"top": "head_top", "bottom": "head_bottom", "left": "head_left",
      "front": "head_front", "right": "head_right", "back": "head_back"},
     {"top": "head_top_ov", "bottom": "head_bottom_ov", "left": "head_left_ov",
      "front": "head_front_ov", "right": "head_right_ov", "back": "head_back_ov"}),
    ("body", (-4.0, 12.0, -2.0, 4.0, 24.0, 2.0),
     {"top": "body_top", "bottom": "body_bottom", "left": "body_left",
      "front": "body_front", "right": "body_right", "back": "body_back"},
     {"top": "body_top_ov", "bottom": "body_bottom_ov", "left": "body_left_ov",
      "front": "body_front_ov", "right": "body_right_ov", "back": "body_back_ov"}),
    ("rarm", (4.0, 12.0, -2.0, 8.0, 24.0, 2.0),
     {"top": "rarm_top", "bottom": "rarm_bottom", "left": "rarm_left",
      "front": "rarm_front", "right": "rarm_right", "back": "rarm_back"},
     {"top": "rarm_top_ov", "bottom": "rarm_bottom_ov", "left": "rarm_left_ov",
      "front": "rarm_front_ov", "right": "rarm_right_ov", "back": "rarm_back_ov"}),
    ("larm", (-8.0, 12.0, -2.0, -4.0, 24.0, 2.0),
     {"top": "larm_top", "bottom": "larm_bottom", "left": "larm_left",
      "front": "larm_front", "right": "larm_right", "back": "larm_back"},
     {"top": "larm_top_ov", "bottom": "larm_bottom_ov", "left": "larm_left_ov",
      "front": "larm_front_ov", "right": "larm_right_ov", "back": "larm_back_ov"}),
    ("rleg", (0.0, 0.0, -2.0, 4.0, 12.0, 2.0),
     {"top": "rleg_top", "bottom": "rleg_bottom", "left": "rleg_left",
      "front": "rleg_front", "right": "rleg_right", "back": "rleg_back"},
     {"top": "rleg_top_ov", "bottom": "rleg_bottom_ov", "left": "rleg_left_ov",
      "front": "rleg_front_ov", "right": "rleg_right_ov", "back": "rleg_back_ov"}),
    ("lleg", (-4.0, 0.0, -2.0, 0.0, 12.0, 2.0),
     {"top": "lleg_top", "bottom": "lleg_bottom", "left": "lleg_left",
      "front": "lleg_front", "right": "lleg_right", "back": "lleg_back"},
     {"top": "lleg_top_ov", "bottom": "lleg_bottom_ov", "left": "lleg_left_ov",
      "front": "lleg_front_ov", "right": "lleg_right_ov", "back": "lleg_back_ov"}),
]


def inflate(box, amount: float):
    x0, y0, z0, x1, y1, z1 = box
    return (x0 - amount, y0 - amount, z0 - amount, x1 + amount, y1 + amount, z1 + amount)


# 逐面片固定光照系数，与 hood_skin_tools / unicorn_skin_tools 保持一致。
# 注意：**不要**改用「正面有光、侧面按法线衰减」的连续公式——那会让左右侧面
# 掉到 0.72 左右，白色帽子和白袜在预览图里直接变成灰色，看起来像没画。
# 这里最暗的背面也只压到 0.82，白色仍然读得出是白色。
FACE_LIGHT = {
    "front": 1.03, "right": 0.88, "left": 0.94,
    "back": 0.82, "top": 1.06, "bottom": 0.68,
}


def render_model(skin: Image.Image, yaw: float, pitch: float, size) -> Image.Image:
    """离线软渲染：把 64×64 皮肤按 6 个盒体投影出来，供人工核对配色与分区。

    只用于预览图，不参与游戏渲染。逐面片做正交投影 + 简单明暗，
    yaw 为绕 Y 轴旋转（度），pitch 为俯仰。
    """
    from math import cos, radians, sin

    W, H = size
    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pix = canvas.load()
    tex = skin.load()
    ry, rp = radians(yaw), radians(pitch)

    faces_def = {
        "front": (0, 0, 1),
        "back": (0, 0, -1),
        "right": (1, 0, 0),
        "left": (-1, 0, 0),
        "top": (0, 1, 0),
        "bottom": (0, -1, 0),
    }

    def rot(v):
        x, y, z = v
        x, z = x * cos(ry) + z * sin(ry), -x * sin(ry) + z * cos(ry)
        y, z = y * cos(rp) - z * sin(rp), y * sin(rp) + z * cos(rp)
        return (x, y, z)

    def to_screen(v, s=11.0):
        return (W / 2 + v[0] * s, H / 2 + 18 - v[1] * s)

    quads = []
    for name, box, fmap, omap in PARTS:
        x0, y0, z0, x1, y1, z1 = box
        for fname, (nx, ny, nz) in faces_def.items():
            fkey = fmap[fname]
            ox, oy, fw, fh = FACES[fkey]
            # 面片四角（局部 uv → 世界坐标）
            if fname in ("front", "back"):
                a = (x0, y1, z1 if fname == "front" else z0)
                b = (x1, y1, z1 if fname == "front" else z0)
                c = (x1, y0, z1 if fname == "front" else z0)
                d = (x0, y0, z1 if fname == "front" else z0)
            elif fname in ("left", "right"):
                xx = x0 if fname == "left" else x1
                a = (xx, y1, z1)
                b = (xx, y1, z0)
                c = (xx, y0, z0)
                d = (xx, y0, z1)
            else:
                yy = y1 if fname == "top" else y0
                a = (x0, yy, z1)
                b = (x1, yy, z1)
                c = (x1, yy, z0)
                d = (x0, yy, z0)
            rn = rot((nx, ny, nz))
            if rn[2] < 0.25:          # 背面剔除（相机在 +z）
                continue
            depth = sum(rot(v)[2] for v in (a, b, c, d)) / 4
            quads.append((depth, fkey, (a, b, c, d), fname))
    quads.sort(key=lambda t: t[0])

    for depth, fkey, corners, fname in quads:
        ox, oy, fw, fh = FACES[fkey]
        k = FACE_LIGHT[fname]         # 固定面光，见 FACE_LIGHT 注释
        pa = to_screen(rot(corners[0]))
        pb = to_screen(rot(corners[1]))
        pd = to_screen(rot(corners[3]))
        # 用双线性把 8×8（或 8×12 / 4×12）面片贴到平行四边形上
        for j in range(fh):
            for i in range(fw):
                u0, v0 = (i + .5) / fw, (j + .5) / fh
                ux = pa[0] + (pb[0] - pa[0]) * u0
                uy = pa[1] + (pb[1] - pa[1]) * u0
                vx = pd[0] + (pb[0] - pa[0]) * u0
                vy = pd[1] + (pb[1] - pa[1]) * u0
                sx = ux + (vx - ux) * v0
                sy = uy + (vy - uy) * v0
                c = tex[ox + i, oy + j]
                if c[3] == 0:
                    continue
                col = (min(255, int(c[0] * k)), min(255, int(c[1] * k)),
                       min(255, int(c[2] * k)), c[3])
                # 一个 texel 铺成约 11×11 的小块
                for dy in range(-5, 6):
                    for dx in range(-5, 6):
                        px_, py_ = int(sx + dx), int(sy + dy)
                        if 0 <= px_ < W and 0 <= py_ < H:
                            pix[px_, py_] = col
    return canvas


def create_icon(skin: Image.Image) -> Image.Image:
    ts = max(1, skin.width // BASE_SIZE)
    head = Image.new("RGBA", (8 * ts, 8 * ts), (0, 0, 0, 0))
    head.alpha_composite(skin.crop((8 * ts, 8 * ts, 16 * ts, 16 * ts)))
    head.alpha_composite(skin.crop((40 * ts, 8 * ts, 48 * ts, 16 * ts)))
    icon = head.resize((16, 16), Image.Resampling.NEAREST)
    draw = ImageDraw.Draw(icon)
    draw.rectangle((0, 0, 15, 15), outline=(120, 156, 214, 255))
    icon.putpixel((1, 1), P["red"])
    icon.putpixel((14, 1), P["navy"])
    icon.putpixel((1, 14), P["white"])
    icon.putpixel((14, 14), P["star_blue"])
    return icon


def label(draw, text: str, x: int, y: int) -> None:
    draw.rectangle((x - 4, y - 2, x + len(text) * 7 + 6, y + 13), fill=(245, 248, 250, 235))
    draw.text((x, y), text, fill=(32, 45, 58, 255))


def write_sheet(skin: Image.Image) -> None:
    ds = 8
    sheet = Image.new("RGBA", (skin.width * ds, skin.height * ds + 24), (242, 242, 244, 255))
    sheet.alpha_composite(skin.resize((skin.width * ds, skin.height * ds), Image.Resampling.NEAREST))
    ImageDraw.Draw(sheet).text(
        (6, skin.height * ds + 6),
        f"skin_22.png {skin.width}x{skin.height} (classic 4px arm, 与大凤 skin_4 同分辨率)",
        fill=(30, 30, 30, 255))
    SHEET_OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(SHEET_OUT)


def write_preview(skin: Image.Image, icon: Image.Image) -> None:
    PREVIEW_OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (1080, 760), (228, 233, 238, 255))
    draw = ImageDraw.Draw(canvas)
    views = [
        ("front three-quarter", -32, 10, (20, 60)),
        ("front", 0, 6, (390, 60)),
        ("back three-quarter", 148, 10, (760, 60)),
    ]
    for title, yaw, pitch, pos in views:
        rendered = render_model(skin, yaw, pitch, (300, 580))
        canvas.alpha_composite(rendered, pos)
        label(draw, title, pos[0] + 20, pos[1] + 556)
    draw.rectangle((32, 656, 176, 736), fill=(248, 250, 252, 255), outline=(164, 177, 188, 255))
    draw.text((46, 664), "skin core icon", fill=(40, 50, 60, 255))
    canvas.alpha_composite(icon.resize((64, 64), Image.Resampling.NEAREST), (72, 660))
    draw.text((200, 668), "基林改 Keeling Kai — 64x64，头部结构照搬大凤 skin_4，只换金发/蓝瞳", fill=(38, 48, 58, 255))
    label(draw, "参考立绘 L_NORMAL_1093（zjsnrwiki）", 200, 690)
    draw.text((200, 716), "白水手帽 / 藏青领+红领巾 / 白衬衫露脐 / 红短裙蓝星 / 白过膝袜蓝星 / 白红玛丽珍鞋",
              fill=(75, 88, 98, 255))
    draw.text((200, 740), "离线软件预览（与游戏内渲染接近，光照为近似值）", fill=(75, 88, 98, 255))
    canvas.save(PREVIEW_OUT)


def main() -> None:
    skin = create_skin()
    icon = create_icon(skin)
    SKIN_OUT.parent.mkdir(parents=True, exist_ok=True)
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    skin.save(SKIN_OUT)
    icon.save(ICON_OUT)
    write_sheet(skin)
    write_preview(skin, icon)
    print(SKIN_OUT)
    print(ICON_OUT)
    print(PREVIEW_OUT)
    print(SHEET_OUT)


if __name__ == "__main__":
    main()
