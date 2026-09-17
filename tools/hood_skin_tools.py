#!/usr/bin/env python3
"""胡德（skin_23）经典 Steve 体型（4px 手臂）玩家皮肤生成工具。

分辨率与画风基准 = 大凤（skin_4）：64×64。头部 12 个面片**原样照搬** skin_4 的
结构（只做配色替换：粉发 → 金发、粉瞳 → 蓝瞳），保证游戏内的眼睛/脸型与大凤
完全一致；身体部分按胡德立绘（L_NORMAL_1001）画，沿用同一套像素密度与色阶做法。

立绘特征：
  - 金色长直发（及腰），左侧头顶紫花/紫晶发饰
  - 蓝色披肩（肩甲式短斗篷）+ 金色滚边
  - 白色高领衬衫，胸前金纽扣与金链饰
  - 蓝色格纹（tartan）百褶裙
  - 白色过膝长筒袜 + 蓝色竖条纹，袜口灰色松紧带
  - 白/蓝鞋 + 红色鞋底与鞋跟

体型说明：ShipGirlRenderer 对 skin_23 走默认 PlayerModel（classic，4px 手臂），
所以本脚本按经典 Steve 布局出图，不能改用 Alex/slim 的 3px 手臂。

输出：
  src/main/resources/assets/piranport/textures/skin/skin_23.png
  src/main/resources/assets/piranport/textures/item/skin_core_23.png
  build/offline-renders/hood_skin_preview.png
  build/offline-renders/hood_skin_sheet.png

运行：cd tools && python3 hood_skin_tools.py

UV 约定（与原版一致，render_model 按此投影）：
  正面面片局部 x=0 是角色左侧（-x），模型朝 -z。
  WEST 面片（-x）局部 x=0 为后脑，EAST 面片（+x）局部 x=0 为面部。
  UP 面片局部 y=0 为后脑、y=7 为面部；DOWN 面片局部 y=0 为面部。
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
# 画风基准：大凤（skin_4）的头部结构被本脚本直接照搬，只做配色替换
TAIHOU_SKIN = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_4.png"
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_23.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_23.png"
PREVIEW_OUT = ROOT / "build/offline-renders/hood_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/hood_skin_sheet.png"
BASE_SIZE = 64          # 标准皮肤 UV 空间，与大凤一致
SCALE = 1               # 1:1 输出 64×64
SIZE = BASE_SIZE * SCALE


# ---------------------------------------------------------------- 调色板

P = {
    "none": (0, 0, 0, 0),
    # 金色长发 —— 6 个色阶（发梢深 → 高光浅）
    "hair_deep": (170, 122, 66, 255),
    "hair_deep2": (186, 136, 78, 255),
    "hair_shadow": (200, 156, 94, 255),
    "hair": (222, 180, 116, 255),
    "hair_mid": (240, 208, 152, 255),
    "hair_light": (250, 228, 178, 255),
    # 皮肤 —— 4 个色阶（沿用大凤的肤色，胡德同样是白皙暖调）
    "skin_light": (254, 243, 229, 255),
    "skin": (252, 231, 216, 255),
    "skin_shadow": (249, 205, 191, 255),
    "skin_deep": (252, 196, 182, 255),
    "blush": (247, 186, 176, 255),
    # 五官（蓝瞳，与大凤同结构不同色）
    "lash": (110, 78, 62, 255),
    "lash_light": (198, 172, 158, 255),
    "eye_deep": (74, 138, 214, 255),
    "eye_light": (176, 222, 250, 255),
    "eye_white": (250, 250, 252, 255),
    # 蓝色制服 / 披肩
    "navy": (44, 58, 92, 255),
    "cape": (78, 108, 168, 255),
    "cape_light": (110, 146, 204, 255),
    "cape_deep": (52, 76, 128, 255),
    # 白衬衫
    "white": (250, 250, 252, 255),
    "white_sh": (214, 222, 236, 255),
    "white_deep": (186, 198, 218, 255),
    # 金饰
    "gold": (228, 190, 96, 255),
    "gold_deep": (176, 134, 46, 255),
    # 蓝格纹裙
    "plaid": (58, 96, 158, 255),
    "plaid_light": (104, 146, 204, 255),
    "plaid_deep": (40, 68, 118, 255),
    # 紫花 / 紫晶发饰
    "violet": (168, 122, 216, 255),
    "violet_deep": (116, 72, 168, 255),
    "violet_light": (214, 184, 242, 255),
    # 白色过膝袜
    "stocking": (250, 250, 253, 255),
    "stocking_sh": (216, 222, 232, 255),
    "stocking_top": (172, 180, 196, 255),
    "stripe": (74, 122, 196, 255),
    # 鞋
    "shoe": (246, 248, 251, 255),
    "shoe_sh": (208, 216, 228, 255),
    "shoe_red": (198, 48, 58, 255),
    "shoe_red_deep": (150, 30, 40, 255),
}

# 大凤头部用到的 14 种颜色 → 胡德配色（结构一模一样，只换色）。
# 一律引用上面的调色板，避免同一份颜色在两处各写一遍后走样。
TAIHOU_TO_HOOD = {
    # 粉发 → 金发
    (230, 116, 142): P["hair_deep"][:3],
    (235, 118, 149): P["hair_deep2"][:3],
    (238, 136, 163): P["hair_shadow"][:3],
    (242, 144, 167): P["hair"][:3],
    (248, 164, 174): P["hair_mid"][:3],
    # 皮肤：保留大凤原肤色
    (252, 196, 182): P["skin_deep"][:3],
    (249, 205, 191): P["skin_shadow"][:3],
    (252, 231, 216): P["skin"][:3],
    (254, 243, 229): P["skin_light"][:3],
    # 睫毛 / 眼睑
    (97, 51, 51): P["lash"][:3],
    (184, 157, 167): P["lash_light"][:3],
    # 粉瞳 → 蓝瞳
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
    # 右臂 classic 4×12×4
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
    # 左臂 classic 4×12×4
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


def shade(color, factor: float):
    r, g, b, a = color
    return (min(255, int(r * factor)), min(255, int(g * factor)), min(255, int(b * factor)), a)


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
                nc = TAIHOU_TO_HOOD.get(c[:3], c[:3]) + (255,)
                s.px(key, x, y, nc)


def detail_ornament(s: Sheet) -> None:
    """胡德特征：左侧头顶的紫花 / 紫晶发饰。

    立绘里发饰在观众视角的右上方 = 角色自己的左侧 = -x = head_left 面片。
    只压在帽层（ov）上，不动大凤的眼睛结构。
    """
    lo = "head_left_ov"          # 局部 x：0 后脑 → 7 面部
    # 花瓣主体（前沿偏上）
    s.rect(lo, 4, 1, 3, 2, P["violet_deep"])
    s.rect(lo, 4, 1, 2, 1, P["violet"])
    s.px(lo, 5, 0, P["violet"])
    s.px(lo, 4, 0, P["violet_light"])
    s.px(lo, 6, 1, P["violet_light"])
    s.px(lo, 5, 2, P["violet"])
    s.px(lo, 4, 3, P["violet_deep"])
    s.px(lo, 6, 0, P["violet_deep"])
    # 顶部面片左上角呼应一点紫色，从俯视也认得出
    s.px("head_top_ov", 1, 2, P["violet"])
    s.px("head_top_ov", 1, 3, P["violet_deep"])
    s.px("head_top_ov", 2, 2, P["violet_light"])
    # 正面帽层左上角露出的花尖
    s.px("head_front_ov", 0, 1, P["violet"])
    s.px("head_front_ov", 0, 2, P["violet_deep"])


# ---------------------------------------------------------------- 躯干


def draw_plaid(s: Sheet, face: str, x: int, y: int, w: int, h: int, phase: int = 0) -> None:
    """蓝格纹（tartan）：蓝底 + 深蓝竖条 × 亮蓝横条，交叉处最亮。

    只有 8×3 像素可用，画不出真 tartan，取"竖深横亮"两向交叉即可读作格纹。
    """
    s.rect(face, x, y, w, h, P["plaid"])
    for i in range(x, x + w):                 # 深蓝竖条 = 褶裥
        if (i + phase) % 4 == 2:
            s.rect(face, i, y, 1, h, P["plaid_deep"])
    s.rect(face, x, y, w, 1, P["plaid_light"])           # 顶部一条亮蓝横带


def draw_body(s: Sheet) -> None:
    f, b = "body_front", "body_back"
    # ---- 正面 ----
    # 白高领 + 蓝色披肩盖肩
    s.rect(f, 0, 0, 8, 2, P["cape"])
    s.rect(f, 2, 0, 4, 2, P["white"])
    s.px(f, 3, 0, P["white_sh"])
    s.px(f, 4, 0, P["white_sh"])
    s.rect(f, 0, 0, 1, 2, P["cape_deep"])
    s.rect(f, 7, 0, 1, 2, P["cape_deep"])
    # 披肩合拢，只在中缝留一线白衬衫
    s.rect(f, 0, 2, 8, 1, P["cape"])
    s.rect(f, 3, 2, 2, 1, P["white"])
    # 白衬衫（先铺满，再压披肩金边，否则会被后面的矩形覆盖掉）
    s.rect(f, 0, 3, 8, 6, P["white"])
    s.rect(f, 0, 3, 1, 6, P["white_sh"])
    s.rect(f, 7, 3, 1, 6, P["white_sh"])
    s.rect(f, 0, 3, 2, 1, P["gold"])          # 披肩下缘金边
    s.rect(f, 6, 3, 2, 1, P["gold"])
    s.px(f, 1, 3, P["gold_deep"])
    s.px(f, 6, 3, P["gold_deep"])
    # 胸前金饰：只留一粒小挂坠，别和腰封金扣连成一条金线
    s.px(f, 4, 4, P["gold"])
    s.px(f, 4, 5, P["gold_deep"])
    # 蓝色腰封 + 金扣
    s.rect(f, 0, 8, 8, 1, P["cape_deep"])
    s.rect(f, 1, 8, 6, 1, P["cape"])
    s.rect(f, 3, 8, 2, 1, P["gold"])
    # 蓝格纹裙
    draw_plaid(s, f, 0, 9, 8, 3)
    s.rect(f, 0, 11, 8, 1, P["plaid_deep"])

    # ---- 背面 ----
    s.rect(b, 0, 0, 8, 3, P["cape"])
    s.rect(b, 0, 3, 8, 1, P["gold"])
    s.rect(b, 0, 0, 8, 1, P["cape_light"])
    s.rect(b, 0, 4, 8, 4, P["white"])
    s.px(b, 1, 5, P["white_sh"])
    s.px(b, 1, 6, P["white_sh"])
    s.px(b, 6, 5, P["white_sh"])
    s.px(b, 6, 6, P["white_sh"])
    s.rect(b, 0, 8, 8, 1, P["cape_deep"])
    draw_plaid(s, b, 0, 9, 8, 3)

    # ---- 侧面 ----
    for face in ("body_left", "body_right"):
        s.rect(face, 0, 0, 4, 3, P["cape"])
        s.rect(face, 0, 3, 4, 1, P["gold"])
        s.rect(face, 0, 4, 4, 4, P["white"])
        s.rect(face, 0, 8, 4, 1, P["cape_deep"])
        draw_plaid(s, face, 0, 9, 4, 3)
    s.rect("body_top", 0, 0, 8, 4, P["cape"])
    s.rect("body_top", 2, 1, 4, 3, P["skin"])        # 颈部开口
    s.rect("body_bottom", 0, 0, 8, 4, P["plaid_deep"])


def draw_body_overlay(s: Sheet) -> None:
    # 正面外衣层：垂到胸前的金色发绺 + 腰封金属细节
    fo = "body_front_ov"
    s.fill(fo, P["none"])
    # 垂到胸前的一小绺头发：只盖到锁骨附近，长了会看成背带
    for x in (0, 7):
        s.rect(fo, x, 0, 1, 2, P["hair"])
        s.px(fo, x, 2, P["hair_shadow"])
        s.px(fo, x, 3, P["hair_deep"])
    s.px(fo, 1, 1, P["hair_light"])
    s.px(fo, 6, 1, P["hair_light"])

    # 背面外衣层：金色长发披到臀部。用纵向发丝而不是横向色带，
    # 否则背面会糊成一块均匀的棕色板子。
    bo = "body_back_ov"
    s.fill(bo, P["none"])
    cols = [P["hair_deep"], P["hair_shadow"], P["hair"], P["hair_mid"],
            P["hair_mid"], P["hair"], P["hair_shadow"], P["hair_deep"]]
    for x, c in enumerate(cols):
        s.rect(bo, x, 0, 1, 12, c)
    s.rect(bo, 3, 0, 1, 12, P["hair_light"])        # 中央高光带
    s.rect(bo, 4, 0, 1, 12, shade(P["hair_light"], 0.95))
    # 发梢：底部逐级压暗，末端两角收圆
    for x, c in enumerate(cols):
        s.rect(bo, x, 8, 1, 4, shade(c, 0.9))
    s.rect(bo, 1, 10, 6, 2, shade(P["hair_deep"], 0.94))
    s.rect(bo, 0, 11, 8, 1, P["hair_deep"])
    s.px(bo, 0, 11, P["none"])
    s.px(bo, 7, 11, P["none"])

    for face, outer_x in (("body_left_ov", 0), ("body_right_ov", 3)):
        s.fill(face, P["none"])
        tones = ([P["hair"], P["hair_mid"], P["hair"], P["hair_shadow"]]
                 if outer_x == 0 else
                 [P["hair_shadow"], P["hair"], P["hair_mid"], P["hair"]])
        for x, c in enumerate(tones):
            s.rect(face, x, 0, 1, 12, c)
        s.rect(face, outer_x, 1, 1, 8, P["hair_light"])
        s.rect(face, 0, 9, 4, 3, shade(P["hair_shadow"], 0.9))

    s.fill("body_top_ov", P["none"])
    s.rect("body_top_ov", 0, 0, 8, 3, P["hair"])
    s.rect("body_top_ov", 1, 0, 6, 3, P["hair_mid"])
    s.rect("body_top_ov", 3, 0, 2, 3, P["hair_light"])
    s.fill("body_bottom_ov", P["none"])


# ---------------------------------------------------------------- 四肢


def mirror_x(x: int, w: int, span: int, flip: bool) -> int:
    """左右两侧共用一套设计时，把局部 x 翻到对面去。

    左右肢体的贴图面片朝向是镜像的（正面面片 x0 对左腿是外侧、对右腿是内侧），
    所以同一份像素坐标直接套用到两侧会得到不对称的花纹，必须翻转。
    """
    return span - x - w if flip else x


def draw_arms(s: Sheet) -> None:
    """蓝披肩盖肩 → 金边 → 白衬衫袖 → 蓝金臂环 → 白袖口 → 手。"""
    for flip, (front, back, side_in, side_out) in (
            (False, ("rarm_front", "rarm_back", "rarm_left", "rarm_right")),
            (True, ("larm_front", "larm_back", "larm_left", "larm_right"))):
        # 内侧/后侧压暗，撑出圆柱感
        for f in (side_in, back):
            s.fill(f, P["white_sh"])
        s.fill(side_out, P["white"])
        s.fill(front, P["white"])
        # 正面靠外侧的转角压暗（两侧都要在外侧，所以要翻转）
        s.rect(front, mirror_x(3, 1, 4, flip), 3, 1, 7, P["white_sh"])

        # 披肩盖住肩头（两侧都对），金边收口
        s.rect(front, 0, 0, 4, 2, P["cape"])
        s.rect(side_out, 0, 0, 4, 2, P["cape"])
        s.rect(side_in, 0, 0, 4, 2, P["cape_deep"])
        s.rect(back, 0, 0, 4, 2, P["cape_deep"])
        for f in (front, back, side_in, side_out):
            s.rect(f, 0, 2, 4, 1, P["gold"] if f in (front, side_out) else P["gold_deep"])
        # 上臂蓝金臂环
        for f in (front, side_out):
            s.rect(f, 0, 6, 4, 1, P["cape"])
            s.rect(f, 0, 7, 4, 1, P["gold"])
        for f in (back, side_in):
            s.rect(f, 0, 6, 4, 1, P["cape_deep"])
            s.rect(f, 0, 7, 4, 1, P["gold_deep"])
        # 白袖口 + 手
        for f in (front, back, side_in, side_out):
            s.rect(f, 0, 9, 4, 1, P["white_sh"])
        s.rect(front, 0, 10, 4, 2, P["skin"])
        s.rect(side_out, 0, 10, 4, 2, P["skin"])
        s.rect(side_in, 0, 10, 4, 2, P["skin_shadow"])
        s.rect(back, 0, 10, 4, 2, P["skin_shadow"])
        s.rect(front, 0, 11, 4, 1, P["skin_shadow"])

    # 外衣层：披肩下的白色蕾丝荷叶边 + 垂到手臂上的发绺
    for flip, (front, back, side_in, side_out) in (
            (False, ("rarm_front_ov", "rarm_back_ov", "rarm_left_ov", "rarm_right_ov")),
            (True, ("larm_front_ov", "larm_back_ov", "larm_left_ov", "larm_right_ov"))):
        for f in (front, back, side_in, side_out):
            s.fill(f, P["none"])
        for x in (0, 2):
            s.px(front, mirror_x(x, 1, 4, flip), 3, P["white"])
            s.px(side_out, mirror_x(x, 1, 4, flip), 3, P["white"])
        for x in (1, 3):
            s.px(back, mirror_x(x, 1, 4, flip), 3, P["white_sh"])
            s.px(side_in, mirror_x(x, 1, 4, flip), 3, P["white_sh"])
        # 后侧垂下的发绺（手臂背面也能看到头发）；面片只有 4px 宽，
        # 超过 2px 就会把整条手臂糊成头发，所以这里只占外侧两列
        s.rect(back, 1, 0, 2, 6, P["hair"])
        s.rect(back, 1, 6, 2, 1, P["hair_deep"])
        s.rect(side_out, mirror_x(2, 2, 4, flip), 1, 2, 3, P["hair"])
        s.rect(side_out, mirror_x(2, 2, 4, flip), 4, 2, 1, P["hair_deep"])
        s.rect(side_out, mirror_x(1, 3, 4, flip), 0, 3, 1, P["hair_mid"])

    s.fill("rarm_top", P["skin_shadow"])
    s.fill("larm_top", P["skin_shadow"])
    s.fill("rarm_bottom", P["skin_deep"])
    s.fill("larm_bottom", P["skin_deep"])
    for f in ("rarm_top_ov", "rarm_bottom_ov", "larm_top_ov", "larm_bottom_ov"):
        s.fill(f, P["none"])


def draw_legs(s: Sheet) -> None:
    """白色过膝袜 + 蓝色竖条纹 + 红底鞋。

    花纹在两侧必须镜像，否则两条腿会一条条纹靠内、一条靠外。
    """
    for flip, (front, back, side_in, side_out) in (
            (False, ("rleg_front", "rleg_back", "rleg_left", "rleg_right")),
            (True, ("lleg_front", "lleg_back", "lleg_left", "lleg_right"))):
        # 大腿露出部分
        for f in (front, side_out):
            s.fill(f, P["skin"])
        for f in (back, side_in):
            s.fill(f, P["skin_shadow"])
        # 袜子本体（r2 袜口 → r9）
        for f in (front, back, side_in, side_out):
            s.rect(f, 0, 2, 4, 8, P["stocking"])
        s.rect(back, 0, 2, 4, 8, P["stocking_sh"])       # 背面整体压暗
        s.rect(side_in, 0, 2, 4, 8, P["stocking_sh"])
        s.rect(front, mirror_x(3, 1, 4, flip), 3, 1, 7, P["stocking_sh"])
        # 袜口：两条腿紧贴着，整条横贯的深色带会连成一根腰带，
        # 所以只用比袜子略深一档的灰蓝，并且只在外侧留一列白，断开视觉连续
        for f in (front, back, side_in, side_out):
            s.rect(f, 0, 2, 4, 1, P["stocking_top"])
            s.px(f, mirror_x(2, 1, 4, flip), 2, P["stocking"])
        # 蓝色竖条纹：正面靠外一条 + 外侧一条，正面再补一小段斜线
        s.rect(front, mirror_x(2, 1, 4, flip), 3, 1, 7, P["stripe"])
        s.rect(side_out, 1, 3, 1, 7, P["stripe"])
        # 鞋：r10 白鞋面（外侧压一道蓝） / r11 红鞋底
        for f in (front, back, side_in, side_out):
            s.rect(f, 0, 10, 4, 1, P["shoe"] if f in (front, side_out) else P["shoe_sh"])
            s.rect(f, 0, 11, 4, 1, P["shoe_red"])
        s.px(front, mirror_x(2, 1, 4, flip), 10, P["cape"])
        s.px(side_out, 2, 10, P["cape"])

    for f in ("rleg_top", "lleg_top"):
        s.fill(f, P["skin_shadow"])
    for f in ("rleg_bottom", "lleg_bottom"):
        s.fill(f, P["shoe_red_deep"])

    # 外衣层：蓝格纹裙摆盖住大腿上段
    for flip, (front, back, side_in, side_out) in (
            (False, ("rleg_front_ov", "rleg_back_ov", "rleg_left_ov", "rleg_right_ov")),
            (True, ("lleg_front_ov", "lleg_back_ov", "lleg_left_ov", "lleg_right_ov"))):
        # 只盖到大腿根，让 base 层 r2 的灰色袜口露出来，
        # 否则袜子直接从裙摆下面开始，整条腿会糊成一根白柱子
        for f in (front, back, side_in, side_out):
            s.fill(f, P["none"])
            s.rect(f, 0, 0, 4, 2, P["plaid"])
            s.rect(f, 0, 1, 4, 1, P["plaid_light"])
            s.rect(f, mirror_x(2, 1, 4, flip), 0, 1, 2, P["plaid_deep"])
            s.rect(f, 0, 2, 4, 1, P["none"])
        for f in (front, back):
            s.px(f, 0, 1, P["plaid_deep"])
            s.px(f, 3, 1, P["plaid_deep"])


def create_skin() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), P["none"])
    s = Sheet(img)
    copy_head(s)                 # 头部：1:1 照搬大凤结构，只换金发/蓝瞳
    detail_ornament(s)           # 胡德特征：紫花发饰
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

FACE_LIGHT = {
    "front": 1.03, "right": 0.88, "left": 0.94,
    "back": 0.82, "top": 1.06, "bottom": 0.68,
}


def face_point(box, face: str, u: float, v: float, tw: int, th: int):
    """局部 (u, v) → 3D 坐标。约定与原版 UV 展开一致（y0 是底面，y1 是顶面）。"""
    x0, y0, z0, x1, y1, z1 = box
    fu, fv = u / tw, v / th
    y = y1 - (y1 - y0) * fv                   # v=0 在顶面
    if face == "front":                       # NORTH (-z)：u 随 +x
        return (x0 + (x1 - x0) * fu, y, z0)
    if face == "back":                        # SOUTH (+z)：u=0 在 +x
        return (x1 - (x1 - x0) * fu, y, z1)
    if face == "left":                        # WEST (-x)：u=0 在后脑
        return (x0, y, z1 - (z1 - z0) * fu)
    if face == "right":                       # EAST (+x)：u=0 在面部
        return (x1, y, z0 + (z1 - z0) * fu)
    if face == "top":                         # UP：u 随 +x，v=0 在后脑
        return (x0 + (x1 - x0) * fu, y1, z1 - (z1 - z0) * fv)
    if face == "bottom":                      # DOWN：u 随 +x，v=0 在面部
        return (x0 + (x1 - x0) * fu, y0, z0 + (z1 - z0) * fv)
    raise ValueError(face)


def inflate(box, amount: float):
    x0, y0, z0, x1, y1, z1 = box
    return (x0 - amount, y0 - amount, z0 - amount, x1 + amount, y1 + amount, z1 + amount)


def project(p, yaw_deg: float, pitch_deg: float, scale: float, cx: float, cy: float):
    """相机在 yaw=0 时位于 -z 正前方，pitch>0 表示俯视。"""
    x, y, z = p
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    sx = -math.cos(yaw) * x - math.sin(yaw) * z
    sy = (-math.sin(yaw) * math.sin(pitch) * x + math.cos(pitch) * y
          + math.cos(yaw) * math.sin(pitch) * z)
    depth = (-math.sin(yaw) * math.cos(pitch) * x - math.sin(pitch) * y
             + math.cos(yaw) * math.cos(pitch) * z)
    return (cx + sx * scale, cy - sy * scale, depth)


def render_model(skin: Image.Image, yaw: float, pitch: float, size) -> Image.Image:
    w, h = size
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    polys = []
    ts = max(1, skin.width // BASE_SIZE)
    scale = min(w / 20.0, h / 36.0)
    cx, cy = w / 2, h * 0.94

    for _, box, base_map, ov_map in PARTS:
        for mapping, amount in ((base_map, 0.0), (ov_map, 0.4)):
            used = inflate(box, amount) if amount else box
            for face, key in mapping.items():
                ox, oy, tw, th = FACES[key]
                for yy in range(th * ts):
                    for xx in range(tw * ts):
                        color = skin.getpixel((ox * ts + xx, oy * ts + yy))
                        if color[3] == 0:
                            continue
                        u0, v0 = xx / ts, yy / ts
                        u1, v1 = (xx + 1) / ts, (yy + 1) / ts
                        pts = [
                            face_point(used, face, u0, v0, tw, th),
                            face_point(used, face, u1, v0, tw, th),
                            face_point(used, face, u1, v1, tw, th),
                            face_point(used, face, u0, v1, tw, th),
                        ]
                        proj = [project(pt, yaw, pitch, scale, cx, cy) for pt in pts]
                        depth = sum(pt[2] for pt in proj) / 4
                        fill = shade(color, FACE_LIGHT[face])
                        polys.append((depth, [(pt[0], pt[1]) for pt in proj], fill))

    draw = ImageDraw.Draw(out, "RGBA")
    for _, poly, color in sorted(polys, key=lambda item: item[0], reverse=True):
        draw.polygon(poly, fill=color)
    return out


def create_icon(skin: Image.Image) -> Image.Image:
    ts = max(1, skin.width // BASE_SIZE)
    head = Image.new("RGBA", (8 * ts, 8 * ts), (0, 0, 0, 0))
    head.alpha_composite(skin.crop((8 * ts, 8 * ts, 16 * ts, 16 * ts)))
    head.alpha_composite(skin.crop((40 * ts, 8 * ts, 48 * ts, 16 * ts)))
    icon = head.resize((16, 16), Image.Resampling.NEAREST)
    draw = ImageDraw.Draw(icon)
    draw.rectangle((0, 0, 15, 15), outline=(120, 156, 214, 255))
    icon.putpixel((1, 1), P["violet"])
    icon.putpixel((14, 1), P["gold"])
    icon.putpixel((1, 14), P["cape"])
    icon.putpixel((14, 14), P["plaid"])
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
        f"skin_23.png {skin.width}x{skin.height} (classic 4px arm, 与大凤 skin_4 同分辨率)",
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
    draw.text((200, 668), "胡德 Hood — 64×64，头部结构照搬大凤 skin_4，只换金发/蓝瞳", fill=(38, 48, 58, 255))
    draw.text((200, 692), "金色长发+紫花发饰 / 蓝披肩金边 / 白衬衫金纽扣 / 蓝格纹裙 / 白过膝袜蓝条纹", fill=(75, 88, 98, 255))
    draw.text((200, 716), "离线软件预览（与游戏内渲染接近，光照为近似值）", fill=(75, 88, 98, 255))
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
