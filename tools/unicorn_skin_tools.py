#!/usr/bin/env python3
"""独角兽（skin_18）Alex/艾丽克斯（slim，3px 手臂）体型玩家皮肤生成工具。

分辨率与画风基准 = 大凤（skin_4）：64×64。头部 12 个面片**原样照搬** skin_4 的
结构（只做配色替换），保证游戏内的眼睛/脸型与大凤完全一致；身体部分按独角兽
立绘（L_NORMAL_127）画，沿用同一套像素密度与色阶做法。

立绘特征：
  - 淡蓝白色长发、头顶花环（粉/白/黄/红花 + 绿叶）
  - 白色荷叶边吊带连衣裙，胸前淡蓝装饰 + 蝴蝶结
  - 彩色花环斜跨裙摆
  - 白色长筒袜 + 绿色交叉缎带，白色高跟鞋

输出：
  src/main/resources/assets/piranport/textures/skin/skin_18.png
  src/main/resources/assets/piranport/textures/item/skin_core_18.png
  build/offline-renders/unicorn_skin_preview.png
  build/offline-renders/unicorn_skin_sheet.png

运行：cd tools && python3 unicorn_skin_tools.py

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
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_18.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_18.png"
PREVIEW_OUT = ROOT / "build/offline-renders/unicorn_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/unicorn_skin_sheet.png"
BASE_SIZE = 64          # 标准皮肤 UV 空间，与大凤一致
SCALE = 1               # 1:1 输出 64×64
SIZE = BASE_SIZE * SCALE


# ---------------------------------------------------------------- 调色板

P = {
    "none": (0, 0, 0, 0),
    # 淡蓝白发 —— 5 个色阶（发梢深 → 头顶亮）
    "hair_deep": (124, 158, 206, 255),
    "hair_shadow": (162, 192, 228, 255),
    "hair": (202, 224, 246, 255),
    "hair_mid": (228, 240, 253, 255),
    "hair_light": (250, 253, 255, 255),
    # 皮肤 —— 4 个色阶
    "skin_light": (255, 241, 232, 255),
    "skin": (253, 224, 208, 255),
    "skin_shadow": (240, 198, 183, 255),
    "skin_deep": (220, 170, 156, 255),
    "blush": (250, 156, 172, 255),
    # 五官
    "lash": (104, 86, 98, 255),
    "eye_deep": (72, 146, 214, 255),
    "eye_light": (162, 224, 250, 255),
    "mouth": (214, 116, 130, 255),
    # 白色连衣裙
    "white": (253, 253, 255, 255),
    "white_sh": (214, 228, 244, 255),
    "white_deep": (182, 203, 228, 255),
    "mint": (206, 240, 244, 255),
    "blue_acc": (166, 208, 246, 255),
    "blue_deep": (114, 166, 226, 255),
    # 深色收边
    "navy": (62, 76, 112, 255),
    # 绿色缎带 / 叶（light 用于袜子上的细绑带，避免整条腿发绿）
    "green": (128, 198, 138, 255),
    "green_deep": (82, 146, 98, 255),
    "green_light": (176, 222, 186, 255),
    # 花朵
    "pink": (250, 138, 180, 255),
    "yellow": (252, 214, 98, 255),
    "red": (232, 82, 108, 255),
    "orange": (250, 158, 74, 255),
    "lilac": (196, 158, 232, 255),
    # 鞋
    "shoe": (250, 250, 253, 255),
    "shoe_sh": (208, 220, 236, 255),
    "shoe_deep": (164, 178, 206, 255),
}

FLOWERS = ["pink", "white", "yellow", "red", "orange", "lilac"]

# 大凤头部用到的 14 种颜色 → 独角兽配色（结构一模一样，只换色）
# 明暗落差按原比例略放大，否则淡蓝发会糊成一片
TAIHOU_TO_UNICORN = {
    (230, 116, 142): (128, 162, 208),
    (235, 118, 149): (146, 178, 218),
    (238, 136, 163): (168, 196, 232),
    (242, 144, 167): (192, 216, 242),
    (248, 164, 174): (216, 234, 250),
    (252, 196, 182): (240, 198, 183),
    (249, 205, 191): (252, 219, 202),
    (252, 231, 216): (253, 224, 208),
    (254, 243, 229): (255, 241, 232),
    (97, 51, 51): (104, 86, 98),
    (184, 157, 167): (168, 186, 208),
    (82, 173, 203): (72, 146, 214),
    (152, 238, 244): (162, 224, 250),
    (247, 247, 247): (250, 252, 255),
}


# ---------------------------------------------------------------- UV 表

# (起点 x, 起点 y, 宽, 高)，全部为 64 空间
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
    # 右臂 slim 3×12×4
    "rarm_top": (44, 16, 3, 4),
    "rarm_bottom": (47, 16, 3, 4),
    "rarm_left": (40, 20, 4, 12),       # 内侧：x0 后 → x3 前
    "rarm_front": (44, 20, 3, 12),
    "rarm_right": (47, 20, 4, 12),      # 外侧：x0 前 → x3 后
    "rarm_back": (51, 20, 3, 12),
    "rarm_top_ov": (44, 32, 3, 4),
    "rarm_bottom_ov": (47, 32, 3, 4),
    "rarm_left_ov": (40, 36, 4, 12),
    "rarm_front_ov": (44, 36, 3, 12),
    "rarm_right_ov": (47, 36, 4, 12),
    "rarm_back_ov": (51, 36, 3, 12),
    # 左臂 slim 3×12×4
    "larm_top": (36, 48, 3, 4),
    "larm_bottom": (39, 48, 3, 4),
    "larm_left": (32, 52, 4, 12),       # 外侧：x0 后 → x3 前
    "larm_front": (36, 52, 3, 12),
    "larm_right": (39, 52, 4, 12),      # 内侧：x0 前 → x3 后
    "larm_back": (43, 52, 3, 12),
    "larm_top_ov": (52, 48, 3, 4),
    "larm_bottom_ov": (55, 48, 3, 4),
    "larm_left_ov": (48, 52, 4, 12),
    "larm_front_ov": (52, 52, 3, 12),
    "larm_right_ov": (55, 52, 4, 12),
    "larm_back_ov": (59, 52, 3, 12),
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
    游戏内眼睛的每一个像素都和大凤一致。
    """
    src = Image.open(TAIHOU_SKIN).convert("RGBA")
    for key in HEAD_KEYS:                      # 先清空，避免残留底图内容
        ox, oy, w, h = FACES[key]
        s.rect(key, 0, 0, w * s.scale, h * s.scale, P["none"])
    for key in HEAD_KEYS:
        ox, oy, w, h = FACES[key]
        for y in range(h):
            for x in range(w):
                c = src.getpixel((ox + x, oy + y))
                if c[3] == 0:
                    continue
                nc = TAIHOU_TO_UNICORN.get(c[:3], c[:3]) + (255,)
                s.px(key, x, y, nc)


def detail_crown(s: Sheet) -> None:
    """独角兽特征：花环。压在大凤帽层顶部一圈，不碰眼睛所在的第 3 行以下。"""
    petals = [P["pink"], P["white"], P["yellow"], P["red"], P["lilac"], P["orange"]]

    for face, off in (("head_front_ov", 0), ("head_left_ov", 2),
                      ("head_right_ov", 4), ("head_back_ov", 6)):
        for i in range(8):
            s.px(face, i, 0, petals[(i + off) % len(petals)])
    # 正面：花环下缘的叶片
    s.sympx("head_front_ov", 1, 1, P["green"])
    s.sympx("head_front_ov", 6, 1, P["green_deep"])
    # 侧面：垂到肩上的白花
    s.px("head_left_ov", 6, 1, P["white"])
    s.px("head_right_ov", 1, 1, P["white"])

    # 头顶俯视：花环只沿周长画 1 像素
    for i in range(8):
        c = petals[(i + 1) % len(petals)]
        s.px("head_top_ov", i, 0, c)
        s.px("head_top_ov", i, 7, c)
        s.px("head_top_ov", 0, i, petals[(i + 3) % len(petals)])
        s.px("head_top_ov", 7, i, petals[(i + 3) % len(petals)])


# ---------------------------------------------------------------- 躯干


def draw_body(s: Sheet) -> None:
    f, b = "body_front", "body_back"
    # 颈肩 + 细肩带
    s.rect(f, 0, 0, 8, 1, P["skin"])
    s.sym(f, 2, 0, 1, 1, P["white"])
    s.rect(f, 0, 1, 8, 1, P["white"])
    s.sym(f, 0, 1, 1, 1, P["white_sh"])
    # 淡蓝胸衣带 + 小花
    s.rect(f, 0, 2, 8, 2, P["blue_acc"])
    s.rect(f, 0, 2, 8, 1, P["blue_deep"])
    s.sym(f, 1, 3, 1, 1, P["pink"])
    s.sym(f, 3, 3, 1, 1, P["yellow"])
    # 白胸衣 + 蝴蝶结
    s.rect(f, 0, 4, 8, 3, P["white"])
    s.sym(f, 0, 4, 1, 3, P["white_sh"])
    s.px(f, 2, 4, P["blue_acc"])
    s.px(f, 5, 4, P["blue_acc"])
    s.rect(f, 3, 4, 2, 2, P["blue_deep"])
    s.px(f, 3, 5, P["blue_acc"])
    s.px(f, 4, 5, P["blue_acc"])
    s.rect(f, 0, 6, 8, 1, P["white_deep"])
    # 腰封
    s.rect(f, 0, 7, 8, 1, P["blue_acc"])
    s.rect(f, 0, 7, 8, 1, P["mint"])
    s.sym(f, 0, 7, 1, 1, P["blue_deep"])
    # 裙摆：褶皱 + 斜跨花环
    s.rect(f, 0, 8, 8, 4, P["white"])
    for x in (1, 3, 5):
        s.rect(f, x, 8, 1, 3, P["white_sh"])
    for i in range(4):
        s.px(f, i * 2, 11 - i, P[FLOWERS[i % len(FLOWERS)]])
    s.px(f, 1, 11, P["green_deep"])
    s.px(f, 3, 10, P["green"])
    s.px(f, 5, 9, P["green_deep"])
    s.rect(f, 0, 11, 8, 1, P["white_deep"])

    # 背面
    s.rect(b, 0, 0, 8, 1, P["skin"])
    s.rect(b, 0, 1, 8, 7, P["white"])
    s.sym(b, 0, 1, 1, 8, P["white_sh"])
    s.rect(b, 0, 7, 8, 1, P["blue_acc"])
    s.rect(b, 0, 8, 8, 4, P["white_sh"])
    s.rect(b, 0, 11, 8, 1, P["white_deep"])

    for face in ("body_left", "body_right"):
        s.rect(face, 0, 1, 4, 7, P["white"])
        s.rect(face, 0, 7, 4, 1, P["blue_acc"])
        s.rect(face, 0, 8, 4, 4, P["white_sh"])
    s.rect("body_top", 0, 0, 8, 4, P["skin"])
    s.rect("body_bottom", 0, 0, 8, 4, P["white_deep"])


def draw_body_overlay(s: Sheet) -> None:
    # 正面：裙摆荷叶边 + 花环
    fo = "body_front_ov"
    s.rect(fo, 0, 0, 8, 12, P["none"])
    s.rect(fo, 0, 8, 8, 1, P["white"])
    for i in range(4):
        s.px(fo, i * 2, 9, P[FLOWERS[i % len(FLOWERS)]])
    s.rect(fo, 0, 10, 8, 2, P["white_sh"])
    s.sym(fo, 2, 10, 1, 2, P["white"])

    # 背面：长发披到臀部 + 裙摆
    bo = "body_back_ov"
    s.rect(bo, 0, 0, 8, 12, P["none"])
    s.rect(bo, 0, 0, 8, 2, P["hair_mid"])
    s.rect(bo, 0, 2, 8, 3, P["hair"])
    s.rect(bo, 0, 5, 8, 3, P["hair_shadow"])
    s.rect(bo, 0, 8, 8, 1, P["hair_deep"])
    s.sym(bo, 1, 0, 1, 8, P["hair_light"])
    s.rect(bo, 3, 0, 2, 8, P["hair_light"])
    s.rect(bo, 0, 9, 8, 1, P["white"])
    for i in range(4):
        s.px(bo, i * 2, 10, P[FLOWERS[(i + 3) % len(FLOWERS)]])
    s.rect(bo, 0, 11, 8, 1, P["white_sh"])

    for face, front_is_right in (("body_left_ov", True), ("body_right_ov", False)):
        s.rect(face, 0, 0, 4, 12, P["none"])
        s.rect(face, 0, 0, 4, 1, P["hair_mid"])
        s.rect(face, 0, 1, 4, 3, P["hair"])
        s.rect(face, 0, 4, 4, 2, P["hair_shadow"])
        s.rect(face, 0, 6, 4, 1, P["hair_deep"])
        s.rect(face, 0, 8, 4, 1, P["white"])
        s.rect(face, 0, 10, 4, 2, P["white_sh"])

    s.rect("body_top_ov", 0, 0, 8, 4, P["none"])
    s.rect("body_top_ov", 0, 0, 8, 3, P["hair"])
    s.sym("body_top_ov", 3, 0, 1, 3, P["hair_light"])
    s.rect("body_bottom_ov", 0, 0, 8, 4, P["none"])


# ---------------------------------------------------------------- 四肢


def draw_arms(s: Sheet) -> None:
    for faces in (("rarm_front", "rarm_back", "rarm_left", "rarm_right"),
                  ("larm_front", "larm_back", "larm_left", "larm_right")):
        front, back, side_in, side_out = faces
        # 内侧深 / 外侧亮，撑出圆柱感
        s.rect(side_in, 0, 0, 4, 12, P["skin_shadow"])
        s.rect(side_out, 0, 0, 4, 12, P["skin_light"])
        s.rect(back, 0, 0, 3, 12, P["skin_shadow"])
        s.rect(front, 0, 0, 3, 12, P["skin"])
        s.rect(front, 2, 2, 1, 8, P["skin_light"])
        for f in faces:
            fw = FACES[f][2]
            s.rect(f, 0, 8, fw, 2, P["white"])          # 腕部蕾丝袖口
            s.rect(f, 0, 8, fw, 1, P["white_sh"])
            s.rect(f, 0, 10, fw, 2, P["skin_shadow"] if f is not side_out else P["skin"])

    for faces in (("rarm_front_ov", "rarm_back_ov", "rarm_left_ov", "rarm_right_ov"),
                  ("larm_front_ov", "larm_back_ov", "larm_left_ov", "larm_right_ov")):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2]
            s.rect(f, 0, 0, fw, 12, P["none"])
            s.rect(f, 0, 0, fw, 1, P["white"])          # 肩部细荷叶边
            s.px(f, 0, 1, P["white_sh"])
            s.px(f, fw - 1, 1, P["white_sh"])
            s.rect(f, 0, 8, fw, 1, P["white"])          # 腕部蕾丝
        # 外侧与后侧垂下的长发
        s.rect(side_out, 0, 1, 4, 2, P["hair_mid"])
        s.rect(side_out, 0, 3, 4, 3, P["hair"])
        s.rect(side_out, 0, 6, 4, 1, P["hair_shadow"])
        s.rect(back, 0, 1, 3, 4, P["hair"])
        s.rect(back, 0, 5, 3, 1, P["hair_shadow"])

    for f in ("rarm_top_ov", "rarm_bottom_ov", "larm_top_ov", "larm_bottom_ov"):
        s.rect(f, 0, 0, FACES[f][2], FACES[f][3], P["none"])
    s.rect("rarm_top", 0, 0, 3, 4, P["skin"])
    s.rect("larm_top", 0, 0, 3, 4, P["skin"])
    s.rect("rarm_bottom", 0, 0, 3, 4, P["skin_deep"])
    s.rect("larm_bottom", 0, 0, 3, 4, P["skin_deep"])


def draw_legs(s: Sheet) -> None:
    for faces in (("rleg_front", "rleg_back", "rleg_left", "rleg_right"),
                  ("lleg_front", "lleg_back", "lleg_left", "lleg_right")):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2]
            tone = P["skin_shadow"] if f in (back, side_in) else P["skin"]
            s.rect(f, 0, 0, fw, 12, tone)
        s.rect(side_out, 0, 0, 1, 12, P["skin_light"])
        # 白色长筒袜（袜口 r4 → r9）
        for f in faces:
            fw = FACES[f][2]
            s.rect(f, 0, 4, fw, 6, P["white"])
            s.rect(f, 0, 4, fw, 1, P["green"])
            s.rect(f, fw - 1, 5, 1, 5, P["white_sh"])
        # 绿色交叉缎带：细绑带用浅绿，避免整条腿发绿
        for f in (front, side_out):
            fw = FACES[f][2]
            for y, xs in ((5, (0, fw - 1)), (6, (1, fw - 2)),
                          (8, (1, fw - 2)), (9, (0, fw - 1))):
                for x in xs:
                    s.px(f, x, y, P["green_light"])
        s.rect(front, 1, 6, 2, 1, P["green_light"])
        s.px(front, 1, 7, P["green_deep"])
        s.px(front, 2, 7, P["green_deep"])
        # 白鞋
        for f in faces:
            fw = FACES[f][2]
            s.rect(f, 0, 10, fw, 2, P["shoe"])
            s.rect(f, 0, 10, fw, 1, P["shoe_sh"])
            s.rect(f, 0, 11, fw, 1, P["shoe_deep"])
        s.rect(front, 1, 10, 2, 1, P["green"])
        s.rect("rleg_top", 0, 0, 4, 4, P["skin_shadow"])
        s.rect("lleg_top", 0, 0, 4, 4, P["skin_shadow"])
        s.rect("rleg_bottom", 0, 0, 4, 4, P["shoe_deep"])
        s.rect("lleg_bottom", 0, 0, 4, 4, P["shoe_deep"])

    # 外衣层：裙摆荷叶边盖住大腿上段
    for faces in (("rleg_front_ov", "rleg_back_ov", "rleg_left_ov", "rleg_right_ov"),
                  ("lleg_front_ov", "lleg_back_ov", "lleg_left_ov", "lleg_right_ov")):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2]
            s.rect(f, 0, 0, fw, 12, P["none"])
            s.rect(f, 0, 0, fw, 3, P["white"])
            s.rect(f, 0, 2, fw, 1, P["white_sh"])
        for i in range(4):
            s.px(front, i, 1, P[FLOWERS[(i + 1) % len(FLOWERS)]])
            s.px(back, i, 1, P[FLOWERS[(i + 4) % len(FLOWERS)]])
        s.rect(side_out, 0, 1, 4, 1, P[FLOWERS[2]])
        s.rect(side_in, 0, 1, 4, 1, P[FLOWERS[5]])
        s.rect(side_out, 2, 0, 2, 3, P["white_sh"])
        s.rect(side_in, 0, 0, 2, 3, P["white_sh"])
        s.rect(front, 0, 2, 4, 1, P["white_deep"])
        s.rect(back, 0, 2, 4, 1, P["white_deep"])


def create_skin() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), P["none"])
    s = Sheet(img)
    copy_head(s)                 # 头部：1:1 照搬大凤结构
    detail_crown(s)              # 独角兽特征：花环
    draw_body(s)
    draw_body_overlay(s)
    draw_arms(s)
    draw_legs(s)
    return img


# ---------------------------------------------------------------- 预览渲染

# 3D 盒体 (x0, y0, z0, x1, y1, z1)，y 轴向上，z0 为正面（模型朝向 -z）
# Alex/slim 体型：手臂宽 3
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
    ("rarm", (4.0, 12.0, -2.0, 7.0, 24.0, 2.0),
     {"top": "rarm_top", "bottom": "rarm_bottom", "left": "rarm_left",
      "front": "rarm_front", "right": "rarm_right", "back": "rarm_back"},
     {"top": "rarm_top_ov", "bottom": "rarm_bottom_ov", "left": "rarm_left_ov",
      "front": "rarm_front_ov", "right": "rarm_right_ov", "back": "rarm_back_ov"}),
    ("larm", (-7.0, 12.0, -2.0, -4.0, 24.0, 2.0),
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


def shade(color, factor: float):
    r, g, b, a = color
    return (min(255, int(r * factor)), min(255, int(g * factor)), min(255, int(b * factor)), a)


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
    draw.rectangle((0, 0, 15, 15), outline=(150, 190, 230, 255))
    icon.putpixel((1, 1), P["pink"])
    icon.putpixel((14, 1), P["yellow"])
    icon.putpixel((1, 14), P["green"])
    icon.putpixel((14, 14), P["blue_acc"])
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
        f"skin_18.png {skin.width}x{skin.height} (Alex/slim 3px arm, 与大凤 skin_4 同分辨率)",
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
    draw.text((200, 668), "独角兽 Unicorn — 64×64，头部结构照搬大凤 skin_4，只换配色", fill=(38, 48, 58, 255))
    draw.text((200, 692), "淡蓝白长发 / 花环 / 白色荷叶边连衣裙 / 绿缎带长筒袜（Alex slim 体型）", fill=(75, 88, 98, 255))
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
