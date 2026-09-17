#!/usr/bin/env python3
"""独角兽（skin_18）Alex/艾丽克斯（slim，3px 手臂）体型玩家皮肤生成工具。

立绘特征（L_NORMAL_127）：
  - 淡蓝白色极长发，垂至膝盖
  - 头顶花环（粉/白/黄/红花 + 绿叶）
  - 白色荷叶边吊带连衣裙，胸前淡蓝装饰
  - 彩色花环披带斜跨裙摆
  - 白色长筒袜 + 绿色交叉缎带，白色高跟鞋

输出：
  src/main/resources/assets/piranport/textures/skin/skin_18.png
  src/main/resources/assets/piranport/textures/item/skin_core_18.png
  build/offline-renders/unicorn_skin_preview.png
  build/offline-renders/unicorn_skin_sheet.png

运行：cd tools && python3 unicorn_skin_tools.py

UV 约定（与 Minecraft 原版一致，已在 render_model 中按此投影验证）：
  正面面片局部 x=0 是角色左侧（-x）；模型朝向 -z。
  WEST 面片（-x）局部 x=0 为后脑，EAST 面片（+x）局部 x=0 为面部。
  UP 面片局部 v=0 为后脑、v=7 为面部；DOWN 面片局部 v=0 为面部。
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_18.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_18.png"
PREVIEW_OUT = ROOT / "build/offline-renders/unicorn_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/unicorn_skin_sheet.png"
BASE_SIZE = 64          # 基础绘制分辨率（标准皮肤 UV 空间）
SCALE = 2               # 输出为 128×128 高清皮肤
SIZE = BASE_SIZE * SCALE


# ---------------------------------------------------------------- 调色板

P = {
    "none": (0, 0, 0, 0),
    # 淡蓝白发
    "hair": (232, 241, 252, 255),
    "hair_light": (251, 253, 255, 255),
    "hair_mid": (210, 226, 245, 255),
    "hair_shadow": (180, 203, 232, 255),
    "hair_deep": (146, 176, 212, 255),
    # 皮肤
    "skin": (253, 229, 212, 255),
    "skin_shadow": (233, 198, 180, 255),
    "skin_deep": (211, 171, 156, 255),
    "blush": (248, 172, 182, 255),
    # 五官
    "lash": (76, 92, 122, 255),
    "eye_deep": (54, 116, 182, 255),
    "eye_blue": (108, 182, 234, 255),
    "eye_light": (198, 238, 255, 255),
    "mouth": (206, 112, 124, 255),
    # 白裙
    "white": (253, 254, 255, 255),
    "white_sh": (226, 235, 245, 255),
    "white_deep": (198, 214, 232, 255),
    "blue_acc": (188, 219, 247, 255),
    "blue_deep": (146, 190, 236, 255),
    # 绿色缎带 / 叶
    "green": (150, 205, 152, 255),
    "green_deep": (100, 164, 110, 255),
    # 花朵
    "pink": (247, 160, 192, 255),
    "yellow": (251, 217, 124, 255),
    "red": (233, 104, 122, 255),
    "orange": (248, 172, 98, 255),
    "lilac": (204, 174, 232, 255),
    # 鞋
    "shoe": (252, 252, 254, 255),
    "shoe_sh": (214, 224, 236, 255),
    "shoe_deep": (178, 190, 210, 255),
}

FLOWERS = ["pink", "white", "yellow", "red", "orange", "lilac"]


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


class Sheet:
    """按 FACES 表在贴图上按局部坐标作画。scale=1 时即 64 空间。"""

    def __init__(self, img: Image.Image, scale: int = 1) -> None:
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


# ---------------------------------------------------------------- 64 基础层


def draw_head(s: Sheet) -> None:
    W = s.scale

    # 头顶：发旋与高光（v=0 后脑，v=7 刘海）
    s.rect("head_top", 0, 0, 8 * W, 8 * W, P["hair"])
    s.rect("head_top", 0, 0, 8 * W, W, P["hair_shadow"])
    s.rect("head_top", 0, 0, W, 8 * W, P["hair_mid"])
    s.rect("head_top", 7 * W, 0, W, 8 * W, P["hair_mid"])
    s.rect("head_top", 3 * W, 3 * W, 2 * W, 3 * W, P["hair_light"])
    s.sym("head_top", 2 * W, 4 * W, W, W, P["hair_shadow"])
    s.sym("head_top", 2 * W, 6 * W, W, W, P["hair_light"])

    # 头底（几乎不可见）
    s.rect("head_bottom", 0, 0, 8 * W, 8 * W, P["hair_deep"])

    # 正面：刘海 3 行 + 面部 5 行
    s.rect("head_front", 0, 0, 8 * W, 3 * W, P["hair"])
    s.rect("head_front", 2 * W, 2 * W, 4 * W, W, P["hair_light"])
    s.rect("head_front", 0, 3 * W, 8 * W, 5 * W, P["skin"])
    s.rect("head_front", 0, 2 * W, W, 6 * W, P["hair_mid"])
    s.rect("head_front", 7 * W, 2 * W, W, 6 * W, P["hair_mid"])
    s.sympx("head_front", W, 3 * W, P["hair"])

    # 侧面：左侧 x0 后脑 → x7 面部；右侧镜像
    for face, front_is_right in (("head_left", True), ("head_right", False)):
        s.rect(face, 0, 0, 8 * W, 8 * W, P["hair"])
        back = 0 if front_is_right else 7
        s.rect(face, back * W, 0, W, 8 * W, P["hair_shadow"])
        fx = 6 if front_is_right else 1
        edge = 7 if front_is_right else 0
        # 前额角
        s.rect(face, fx * W, 2 * W, W, 2 * W, P["hair"])
        # 脸颊与下颌露出
        s.rect(face, fx * W, 4 * W, W, 4 * W, P["skin"])
        s.rect(face, edge * W, 4 * W, W, 4 * W, P["skin"])
        # 与正面 x0/x7 列对齐的鬓发
        s.rect(face, edge * W, 0, W, 2 * W, P["hair"])
        s.rect(face, edge * W, 2 * W, W, 2 * W, P["hair_mid"])
        s.rect(face, edge * W, 7 * W, W, W, P["skin_shadow"])
        s.px(face, (fx if front_is_right else 5) * W, 2 * W, P["hair_light"])
        s.px(face, 4 * W if front_is_right else 3 * W, 3 * W, P["hair_light"])

    # 后脑：垂直发丝
    s.rect("head_back", 0, 0, 8 * W, 8 * W, P["hair"])
    s.rect("head_back", 0, 0, W, 8 * W, P["hair_shadow"])
    s.rect("head_back", 7 * W, 0, W, 8 * W, P["hair_shadow"])
    s.rect("head_back", 0, 0, 8 * W, W, P["hair_shadow"])
    s.sym("head_back", W, W, W, 5 * W, P["hair_mid"])
    s.sym("head_back", 3 * W, W, W, 3 * W, P["hair_light"])


def draw_body(s: Sheet) -> None:
    W = s.scale

    # 躯干正反面底色
    for face in ("body_front", "body_back"):
        s.rect(face, 0, 0, 8 * W, 12 * W, P["white"])
    for face in ("body_left", "body_right"):
        s.rect(face, 0, 0, 4 * W, 12 * W, P["white_sh"])
    s.rect("body_top", 0, 0, 8 * W, 4 * W, P["skin"])
    s.rect("body_bottom", 0, 0, 8 * W, 4 * W, P["white_deep"])

    # 正面：颈肩 + 细肩带
    s.rect("body_front", 0, 0, 8 * W, W, P["skin"])
    s.sym("body_front", 2 * W, 0, W, W, P["white"])
    # 胸衣上缘与淡蓝装饰
    s.rect("body_front", 0, W, 8 * W, W, P["white"])
    s.rect("body_front", 2 * W, 2 * W, 4 * W, W, P["blue_acc"])
    s.rect("body_front", 3 * W, 3 * W, 2 * W, W, P["white"])
    s.rect("body_front", 3 * W, 4 * W, 2 * W, 2 * W, P["white_sh"])
    s.rect("body_front", 2 * W, 3 * W, W, 5 * W, P["white_sh"])
    s.rect("body_front", 5 * W, 3 * W, W, 5 * W, P["white_sh"])
    # 腰部淡蓝腰带
    s.rect("body_front", 0, 7 * W, 8 * W, W, P["blue_acc"])
    s.sym("body_front", 0, 7 * W, W, W, P["blue_deep"])

    # 背面：颈 + 裙身
    s.rect("body_back", 0, 0, 8 * W, W, P["skin"])
    s.rect("body_back", 2 * W, 2 * W, 4 * W, 3 * W, P["white_sh"])
    s.rect("body_back", 0, 7 * W, 8 * W, W, P["blue_acc"])
    # 侧面：淡蓝腰带
    for face in ("body_left", "body_right"):
        s.rect(face, 0, 7 * W, 4 * W, W, P["blue_acc"])

    # 裙摆下缘（正面/背面/侧面）
    for face in ("body_front", "body_back"):
        s.rect(face, 0, 10 * W, 8 * W, 2 * W, P["white_sh"])
        s.rect(face, 0, 11 * W, 8 * W, W, P["white_deep"])
    for face in ("body_left", "body_right"):
        s.rect(face, 0, 10 * W, 4 * W, 2 * W, P["white_deep"])


def draw_arms(s: Sheet) -> None:
    W = s.scale

    # 手臂：裸露 + 腕部蕾丝袖口（正面 x0 内侧 / x2 外侧，左右镜像）
    for front, back, side_in, side_out, top, bottom in (
        ("rarm_front", "rarm_back", "rarm_left", "rarm_right", "rarm_top", "rarm_bottom"),
        ("larm_front", "larm_back", "larm_left", "larm_right", "larm_top", "larm_bottom"),
    ):
        s.rect(front, 0, 0, 3 * W, 12 * W, P["skin"])
        s.rect(back, 0, 0, 3 * W, 12 * W, P["skin_shadow"])
        s.rect(side_in, 0, 0, 4 * W, 12 * W, P["skin_shadow"])
        s.rect(side_out, 0, 0, 4 * W, 12 * W, P["skin"])
        s.rect(top, 0, 0, 3 * W, 4 * W, P["skin"])
        s.rect(bottom, 0, 0, 3 * W, 4 * W, P["skin_deep"])
        # 腕部袖口
        for f in (front, back, side_in, side_out):
            fw, fh = FACES[f][2] * W, FACES[f][3] * W
            s.rect(f, 0, 8 * W, fw, 2 * W, P["white"])
            s.rect(f, 0, 9 * W, fw, W, P["white_sh"])
        s.rect(front, 0, 8 * W, 3 * W, W, P["white"])
        s.rect(front, 0, 10 * W, 3 * W, 2 * W, P["skin_shadow"])


def draw_legs(s: Sheet) -> None:
    W = s.scale

    for front, back, side_in, side_out, top, bottom in (
        ("rleg_front", "rleg_back", "rleg_left", "rleg_right", "rleg_top", "rleg_bottom"),
        ("lleg_front", "lleg_back", "lleg_left", "lleg_right", "lleg_top", "lleg_bottom"),
    ):
        # 腿底：大腿裸露（裙摆覆盖上半段，袜子在下面）
        s.rect(front, 0, 0, 4 * W, 12 * W, P["skin"])
        s.rect(back, 0, 0, 4 * W, 12 * W, P["skin_shadow"])
        s.rect(side_in, 0, 0, 4 * W, 12 * W, P["skin_shadow"])
        s.rect(side_out, 0, 0, 4 * W, 12 * W, P["skin"])
        s.rect(top, 0, 0, 4 * W, 4 * W, P["skin_shadow"])
        s.rect(bottom, 0, 0, 4 * W, 4 * W, P["shoe_deep"])
        # 白色长筒袜：r5 袜口 → r9
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * W
            s.rect(f, 0, 5 * W, fw, 5 * W, P["white_sh"] if f is back else P["white"])
            s.rect(f, 5 * W, fw, 0, 0, P["white"])
        # 袜口绿缎带
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * W
            s.rect(f, 0, 5 * W, fw, W, P["green"])
        # 绿色交叉缎带（正面）
        s.rect(front, 0, 7 * W, 4 * W, W, P["green_deep"])
        s.rect(front, 0, 9 * W, 4 * W, W, P["green"])
        # 白鞋
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * W
            s.rect(f, 0, 10 * W, fw, 2 * W, P["shoe"])
            s.rect(f, 0, 11 * W, fw, W, P["shoe_deep"])


def draw_head_overlay(s: Sheet) -> None:
    """帽层：花环 + 长发体积。"""
    W = s.scale

    # 正面花环：第 0 行整圈花，第 1 行叶片与垂花，第 2 行偶尔一朵
    s.rect("head_front_ov", 0, 0, 8 * W, 8 * W, P["none"])
    for i in range(8):
        s.rect("head_front_ov", i * W, 0, W, W, P[FLOWERS[i % len(FLOWERS)]])
    s.rect("head_front_ov", 3 * W, 0, W, W, P["red"])
    s.rect("head_front_ov", 4 * W, 0, W, W, P["lilac"])
    s.rect("head_front_ov", 0, W, 8 * W, W, P["none"])
    s.sym("head_front_ov", 0, W, W, W, P["green"])
    s.sym("head_front_ov", 2 * W, W, W, W, P["green_deep"])
    s.sym("head_front_ov", 3 * W, W, W, W, P["green"])
    s.sympx("head_front_ov", W, 2 * W, P["pink"])
    s.sympx("head_front_ov", 2 * W, 2 * W, P["yellow"])
    s.sympx("head_front_ov", 3 * W, 2 * W, P["green"])

    # 侧面与后脑：花环 + 垂发
    for face, front_is_right in (("head_left_ov", True), ("head_right_ov", False)):
        s.rect(face, 0, 0, 8 * W, 8 * W, P["none"])
        for i in range(8):
            s.rect(face, i * W, 0, W, W, P[FLOWERS[(i + 2) % len(FLOWERS)]])
        s.sym(face, 1 * W, W, W, W, P["green"])
        s.sym(face, 5 * W, W, W, W, P["green_deep"])
        s.rect(face, 0, 2 * W, 8 * W, 6 * W, P["hair"])
        s.rect(face, 0 if front_is_right else 6 * W, 2 * W, W, 6 * W, P["hair_light"])
        s.rect(face, 3 * W, 3 * W, 2 * W, 5 * W, P["hair_mid"])
    s.rect("head_back_ov", 0, 0, 8 * W, 8 * W, P["hair"])
    for i in range(8):
        s.rect("head_back_ov", i * W, 0, W, W, P[FLOWERS[(i + 4) % len(FLOWERS)]])
    s.sym("head_back_ov", 1 * W, W, 2 * W, 2 * W, P["hair_light"])
    s.sym("head_back_ov", W, 3 * W, W, 5 * W, P["hair_mid"])
    s.rect("head_back_ov", 3 * W, W, 2 * W, 7 * W, P["hair_light"])

    # 头顶：俯视花环环带，中心露出头发
    s.rect("head_top_ov", 0, 0, 8 * W, 8 * W, P["none"])
    for i in range(8):
        c = P[FLOWERS[(i + 1) % len(FLOWERS)]]
        s.rect("head_top_ov", i * W, 0, W, W, c)
        s.rect("head_top_ov", i * W, 7 * W, W, W, c)
        s.rect("head_top_ov", 0, i * W, W, W, P[FLOWERS[(i + 3) % len(FLOWERS)]])
        s.rect("head_top_ov", 7 * W, i * W, W, W, P[FLOWERS[(i + 3) % len(FLOWERS)]])
    s.rect("head_bottom_ov", 0, 0, 8 * W, 8 * W, P["none"])


def draw_body_overlay(s: Sheet) -> None:
    """外衣层：长发披背 + 裙摆荷叶边。"""
    W = s.scale

    # 正面：领口与裙摆荷叶边
    s.rect("body_front_ov", 0, 0, 8 * W, 12 * W, P["none"])
    s.rect("body_front_ov", 3 * W, 0, 2 * W, W, P["white"])
    s.sym("body_front_ov", 2 * W, 0, W, W, P["white_sh"])
    s.rect("body_front_ov", 0, 8 * W, 8 * W, W, P["white"])
    s.rect("body_front_ov", 0, 9 * W, 8 * W, W, P["none"])
    for i in range(8):
        s.rect("body_front_ov", i * W, 9 * W, W, W, P[FLOWERS[i % len(FLOWERS)]])
    s.rect("body_front_ov", 0, 10 * W, 8 * W, 2 * W, P["white_sh"])
    s.sym("body_front_ov", 1 * W, 10 * W, W, 2 * W, P["white"])
    s.sym("body_front_ov", 3 * W, 11 * W, W, W, P["white_deep"])

    # 背面：长发（垂到臀部）+ 裙摆
    s.rect("body_back_ov", 0, 0, 8 * W, 12 * W, P["none"])
    s.rect("body_back_ov", 0, 0, 8 * W, 8 * W, P["hair"])
    s.sym("body_back_ov", W, 0, 2 * W, 8 * W, P["hair_light"])
    s.sym("body_back_ov", 3 * W, 0, W, 8 * W, P["hair_mid"])
    s.rect("body_back_ov", 3 * W, 0, 2 * W, 8 * W, P["hair_light"])
    s.rect("body_back_ov", 0, 7 * W, 8 * W, W, P["hair_shadow"])
    s.rect("body_back_ov", 0, 9 * W, 8 * W, W, P["white"])
    for i in range(8):
        s.rect("body_back_ov", i * W, 10 * W, W, W, P[FLOWERS[(i + 3) % len(FLOWERS)]])
    s.rect("body_back_ov", 0, 11 * W, 8 * W, W, P["white_sh"])

    # 侧面：前半裙摆 + 后半长发
    for face, front_is_right in (("body_left_ov", True), ("body_right_ov", False)):
        s.rect(face, 0, 0, 4 * W, 12 * W, P["none"])
        hair_x = 0 if front_is_right else 0
        s.rect(face, hair_x, 0, 4 * W, 5 * W, P["hair"])
        s.rect(face, 0, 5 * W, 4 * W, 3 * W, P["hair_shadow"])
        s.rect(face, 0, 8 * W, 4 * W, W, P["white"])
        s.rect(face, 0, 9 * W, 4 * W, 3 * W, P["white_sh"])

    # 顶面：长发覆盖肩部
    s.rect("body_top_ov", 0, 0, 8 * W, 4 * W, P["none"])
    s.rect("body_top_ov", 0, 0, 8 * W, 3 * W, P["hair"])
    s.sym("body_top_ov", 3 * W, 0, W, 3 * W, P["hair_light"])
    s.rect("body_bottom_ov", 0, 0, 8 * W, 4 * W, P["none"])


def draw_arm_overlay(s: Sheet) -> None:
    W = s.scale
    for faces, front_is_right in (
        (("rarm_front_ov", "rarm_back_ov", "rarm_left_ov", "rarm_right_ov"), True),
        (("larm_front_ov", "larm_back_ov", "larm_left_ov", "larm_right_ov"), False),
    ):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2] * W
            s.rect(f, 0, 0, fw, 12 * W, P["none"])
            # 肩部荷叶边
            s.rect(f, 0, 0, fw, W, P["white"])
            s.rect(f, 0, W, fw, W, P["white_sh"])
            # 腕部蕾丝
            s.rect(f, 0, 8 * W, fw, W, P["white"])
            s.rect(f, 0, 9 * W, fw, W, P["white_sh"])
        # 外侧垂下的长发
        outer = side_out
        s.rect(outer, 0, 2 * W, 4 * W, 5 * W, P["hair"])
        s.rect(back, 0, 2 * W, 3 * W, 5 * W, P["hair_shadow"])
        s.rect(outer, 0, 6 * W, 4 * W, W, P["hair_shadow"])
    for f, _ in (("rarm_top_ov", 0), ("rarm_bottom_ov", 0), ("larm_top_ov", 0), ("larm_bottom_ov", 0)):
        fw = FACES[f][2] * W
        s.rect(f, 0, 0, fw, 4 * W, P["none"])


def draw_leg_overlay(s: Sheet) -> None:
    W = s.scale
    for front, back, side_in, side_out in (
        ("rleg_front_ov", "rleg_back_ov", "rleg_left_ov", "rleg_right_ov"),
        ("lleg_front_ov", "lleg_back_ov", "lleg_left_ov", "lleg_right_ov"),
    ):
        s.rect(front, 0, 0, 4 * W, 12 * W, P["none"])
        s.rect(back, 0, 0, 4 * W, 12 * W, P["none"])
        s.rect(side_in, 0, 0, 4 * W, 12 * W, P["none"])
        s.rect(side_out, 0, 0, 4 * W, 12 * W, P["none"])
        # 裙摆荷叶边盖住大腿上段
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * W
            s.rect(f, 0, 0, fw, 3 * W, P["white"])
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * W
            s.rect(f, 3 * W, fw, 0, 0, P["white"])
            s.rect(f, 0, 3 * W, fw, W, P["white_sh"])
        # 裙摆下缘的花环
        for i in range(4):
            s.rect(front, i * W, 2 * W, W, W, P[FLOWERS[(i + 1) % len(FLOWERS)]])
            s.rect(back, i * W, 2 * W, W, W, P[FLOWERS[(i + 4) % len(FLOWERS)]])
        s.rect(side_out, 0, 2 * W, 4 * W, W, P["white_sh"])


def draw_base(img: Image.Image) -> None:
    s = Sheet(img, 1)
    draw_head(s)
    draw_body(s)
    draw_arms(s)
    draw_legs(s)
    draw_head_overlay(s)
    draw_body_overlay(s)
    draw_arm_overlay(s)
    draw_leg_overlay(s)


# ---------------------------------------------------------------- 128 高清层


def side_x(front_is_right: bool, d: int) -> int:
    """侧面面片：把「距面部边缘 d 像素」换算成局部 x（面片宽 16）。"""
    return 15 - d if front_is_right else d


def side_rect(s: Sheet, face: str, front_is_right: bool, d: int, y: int, w: int, h: int, color) -> None:
    s.rect(face, side_x(front_is_right, d + w - 1), y, w, h, color)


def hd_face(s: Sheet) -> None:
    """16×16 面部：刘海 6 行 + 动漫大眼 5 行 + 腮红与嘴。"""
    f = "head_front"
    # 底色
    s.rect(f, 0, 0, 16, 16, P["skin"])
    # 两侧鬓发：太阳穴以上为发，颊部以下转为脸颊侧影
    s.rect(f, 0, 0, 2, 9, P["hair_mid"])
    s.rect(f, 14, 0, 2, 9, P["hair_mid"])
    s.rect(f, 0, 9, 2, 7, P["skin_shadow"])
    s.rect(f, 14, 9, 2, 7, P["skin_shadow"])
    # 刘海：5 行 + 发丝尖
    s.rect(f, 0, 0, 16, 6, P["hair"])
    s.rect(f, 0, 0, 16, 1, P["hair_shadow"])
    s.rect(f, 6, 1, 4, 3, P["hair_light"])
    s.rect(f, 1, 3, 4, 3, P["hair_light"])
    s.rect(f, 11, 3, 4, 3, P["hair_light"])
    s.rect(f, 3, 5, 3, 2, P["hair"])
    s.rect(f, 10, 5, 3, 2, P["hair"])
    s.rect(f, 0, 5, 2, 2, P["hair_mid"])
    s.rect(f, 14, 5, 2, 2, P["hair_mid"])
    s.px(f, 7, 6, P["hair"])
    s.px(f, 8, 6, P["hair"])

    # 眼睛：左眼 x2..6，右眼 x9..13，鼻梁留白 x7..8，共 4 行
    for x0 in (2, 9):
        outer = x0 if x0 == 2 else x0 + 4
        s.rect(f, x0, 8, 5, 1, P["lash"])              # 上眼线
        s.px(f, outer, 7, P["lash"])                   # 外眼角上挑
        s.rect(f, x0, 9, 5, 1, P["eye_deep"])
        s.rect(f, x0, 10, 5, 1, P["eye_blue"])
        s.rect(f, x0 + 1, 10, 2, 1, P["eye_light"])    # 高光
        s.rect(f, x0, 11, 5, 1, P["eye_deep"])
        s.rect(f, x0 + 1, 11, 3, 1, P["eye_blue"])
        s.px(f, x0, 12, P["skin_shadow"])
        s.px(f, x0 + 4, 12, P["skin_shadow"])

    # 腮红、嘴、下颌
    s.sym(f, 1, 12, 2, 2, (*P["blush"][:3], 165))
    s.sym(f, 2, 14, 1, 1, (*P["blush"][:3], 110))
    s.rect(f, 7, 13, 2, 1, (222, 142, 150, 255))
    s.rect(f, 3, 15, 10, 1, P["skin_shadow"])

    # 侧面：后脑厚发 + 前颊露肤，前缘与正面 x0/x1 列严格对齐
    for face, front_is_right in (("head_left", True), ("head_right", False)):
        s.rect(face, 0, 0, 16, 16, P["hair"])
        side_rect(s, face, front_is_right, 0, 0, 2, 9, P["hair_mid"])       # 与正面鬓发一致
        side_rect(s, face, front_is_right, 0, 9, 2, 7, P["skin_shadow"])
        side_rect(s, face, front_is_right, 2, 6, 4, 10, P["skin"])          # 脸颊与下颌
        side_rect(s, face, front_is_right, 2, 5, 4, 1, P["hair"])
        side_rect(s, face, front_is_right, 2, 14, 4, 2, P["skin_shadow"])
        side_rect(s, face, front_is_right, 6, 0, 10, 2, P["hair_light"])    # 侧发高光
        side_rect(s, face, front_is_right, 6, 13, 10, 3, P["hair_shadow"])
        side_rect(s, face, front_is_right, 10, 4, 3, 12, P["hair_mid"])

    # 后脑：垂直发丝分层（发缝 + 高光发股）
    s.rect("head_back", 0, 0, 16, 16, P["hair"])
    for x in (0, 4, 8, 12):
        s.rect("head_back", x, 0, 1, 16, P["hair_shadow"])
        s.rect("head_back", x + 1, 0, 2, 16, P["hair_light"])
    s.rect("head_back", 0, 0, 16, 1, P["hair_shadow"])
    s.rect("head_back", 0, 14, 16, 2, P["hair_shadow"])

    # 头顶：发旋
    s.rect("head_top", 6, 6, 4, 6, P["hair_light"])
    s.sym("head_top", 5, 9, 1, 2, P["hair_shadow"])
    s.rect("head_top", 0, 0, 16, 1, P["hair_shadow"])


def hd_crown(s: Sheet) -> None:
    """花环：只在头顶与刘海顶端 1 格高度，避免变成厚帽子。"""
    petals = [P["pink"], P["white"], P["yellow"], P["red"], P["lilac"], P["orange"]]

    def flower(face: str, x: int, y: int, color) -> None:
        s.rect(face, x, y, 2, 2, color)
        s.px(face, x + 1, y + 1, P["hair_light"] if color == P["white"] else color)

    # 正面：一整圈花 + 少量叶片 + 两朵垂花
    f = "head_front_ov"
    s.rect(f, 0, 0, 16, 16, P["none"])
    for i in range(8):
        flower(f, i * 2, 0, petals[i % len(petals)])
    s.sym(f, 3, 2, 1, 1, P["green"])
    s.sym(f, 5, 2, 1, 1, P["green_deep"])
    flower(f, 2, 3, P["pink"])
    flower(f, 12, 3, P["yellow"])

    # 侧面：花环 + 垂到肩上的长发
    for face, front_is_right in (("head_left_ov", True), ("head_right_ov", False)):
        s.rect(face, 0, 0, 16, 16, P["none"])
        for i in range(8):
            flower(face, i * 2, 0, petals[(i + 2) % len(petals)])
        side_rect(s, face, front_is_right, 6, 2, 6, 14, P["hair"])
        side_rect(s, face, front_is_right, 6, 2, 2, 14, P["hair_light"])
        side_rect(s, face, front_is_right, 8, 6, 8, 10, P["hair_shadow"])
        side_rect(s, face, front_is_right, 0, 2, 6, 14, P["hair_mid"])
        side_rect(s, face, front_is_right, 0, 13, 6, 3, P["hair_shadow"])

    # 后脑：花环 + 长发
    s.rect("head_back_ov", 0, 0, 16, 16, P["none"])
    for i in range(8):
        flower("head_back_ov", i * 2, 0, petals[(i + 4) % len(petals)])
    s.rect("head_back_ov", 0, 2, 16, 14, P["hair"])
    s.rect("head_back_ov", 0, 2, 2, 14, P["hair_shadow"])
    s.sym("head_back_ov", 3, 2, 3, 14, P["hair_light"])
    s.rect("head_back_ov", 7, 2, 2, 14, P["hair_light"])
    s.rect("head_back_ov", 0, 14, 16, 2, P["hair_shadow"])

    # 头顶俯视：花环只沿周长画 1 像素，避免俯视时变成彩色盒盖
    t = "head_top_ov"
    s.rect(t, 0, 0, 16, 16, P["none"])
    for i in range(16):
        c = petals[(i // 2) % len(petals)]
        s.px(t, i, 0, c)
        s.px(t, i, 15, c)
        s.px(t, 0, i, c)
        s.px(t, 15, i, c)


def hd_dress(s: Sheet) -> None:
    """裙装细节：领口、淡蓝胸衣、腰封、裙摆褶皱、斜跨花环。"""
    f = "body_front"
    # 颈肩 + 细肩带
    s.rect(f, 0, 0, 16, 2, P["skin"])
    s.sym(f, 5, 0, 2, 3, P["white"])
    s.sym(f, 6, 0, 1, 3, P["white_sh"])
    # 胸衣上缘与淡蓝装饰带
    s.rect(f, 0, 2, 16, 2, P["white"])
    s.rect(f, 0, 3, 16, 1, P["white_sh"])
    s.rect(f, 0, 4, 16, 3, P["blue_acc"])
    s.rect(f, 2, 4, 12, 1, P["blue_deep"])
    s.rect(f, 0, 7, 16, 1, P["blue_deep"])
    # 胸口小花点缀
    for x, c in ((2, P["pink"]), (12, P["pink"]), (5, P["yellow"]), (9, P["yellow"])):
        s.rect(f, x, 4, 2, 2, c)
    # 白色胸衣 + 胸前小蝴蝶结
    s.rect(f, 0, 8, 16, 6, P["white"])
    s.sym(f, 0, 8, 1, 6, P["white_sh"])
    s.rect(f, 4, 9, 3, 2, P["blue_acc"])
    s.rect(f, 9, 9, 3, 2, P["blue_acc"])
    s.rect(f, 4, 9, 1, 2, P["blue_deep"])
    s.rect(f, 11, 9, 1, 2, P["blue_deep"])
    s.rect(f, 7, 9, 2, 2, P["blue_deep"])
    s.rect(f, 7, 11, 2, 2, P["blue_acc"])
    # 腰封
    s.rect(f, 0, 14, 16, 1, P["white"])
    s.rect(f, 0, 15, 16, 2, P["blue_acc"])
    s.rect(f, 0, 17, 16, 1, P["blue_deep"])
    # 裙摆
    s.rect(f, 0, 18, 16, 6, P["white"])
    s.sym(f, 2, 18, 2, 6, P["white_sh"])
    s.rect(f, 0, 23, 16, 1, P["white_deep"])
    # 斜跨花环（自左下摆斜向右上，只落在裙摆内）
    for i in range(5):
        x, y = 1 + i * 2, 22 - i
        s.rect(f, x, y, 2, 2, P[FLOWERS[i % len(FLOWERS)]])
        s.px(f, x, y + 2, P["green_deep"])

    b = "body_back"
    s.rect(b, 0, 0, 16, 2, P["skin"])
    s.rect(b, 0, 2, 16, 13, P["white"])
    s.rect(b, 0, 15, 16, 3, P["blue_acc"])
    s.rect(b, 0, 18, 16, 6, P["white_sh"])
    s.rect(b, 0, 23, 16, 1, P["white_deep"])

    for face in ("body_left", "body_right"):
        s.rect(face, 0, 2, 8, 13, P["white"])
        s.rect(face, 0, 15, 8, 3, P["blue_acc"])
        s.rect(face, 0, 18, 8, 6, P["white_sh"])
        s.rect(face, 0, 23, 8, 1, P["white_deep"])

    # 裙摆荷叶边（外衣层）
    fo = "body_front_ov"
    s.rect(fo, 0, 0, 16, 24, P["none"])
    s.rect(fo, 0, 17, 16, 1, P["white"])
    s.rect(fo, 0, 18, 16, 1, P["white_sh"])
    for i in range(8):
        s.rect(fo, i * 2, 19, 2, 2, P[FLOWERS[i % len(FLOWERS)]])
    s.rect(fo, 0, 21, 16, 3, P["white_sh"])
    s.sym(fo, 2, 21, 2, 3, P["white"])
    s.sym(fo, 6, 22, 2, 2, P["white_deep"])

    # 背外衣层：长发披到臀部（发缝 + 高光发股）
    bo = "body_back_ov"
    s.rect(bo, 0, 0, 16, 24, P["none"])
    s.rect(bo, 0, 0, 16, 18, P["hair"])
    for x in (0, 4, 8, 12):
        s.rect(bo, x, 0, 1, 18, P["hair_shadow"])
        s.rect(bo, x + 1, 0, 2, 18, P["hair_light"])
    s.rect(bo, 0, 0, 16, 2, P["hair_shadow"])
    s.rect(bo, 0, 16, 16, 2, P["hair_shadow"])
    s.rect(bo, 0, 20, 16, 1, P["white"])
    for i in range(8):
        s.rect(bo, i * 2, 21, 2, 2, P[FLOWERS[(i + 3) % len(FLOWERS)]])
    s.rect(bo, 0, 23, 16, 1, P["white_sh"])

    for face, front_is_right in (("body_left_ov", True), ("body_right_ov", False)):
        s.rect(face, 0, 0, 8, 24, P["none"])
        s.rect(face, 0, 0, 8, 12, P["hair"])
        side_rect(s, face, front_is_right, 0, 0, 2, 12, P["hair_light"])
        s.rect(face, 0, 10, 8, 2, P["hair_shadow"])
        s.rect(face, 0, 17, 8, 1, P["white"])
        s.rect(face, 0, 20, 8, 4, P["white_sh"])

    s.rect("body_top_ov", 0, 0, 16, 8, P["none"])
    s.rect("body_top_ov", 0, 0, 16, 6, P["hair"])
    s.sym("body_top_ov", 6, 0, 2, 6, P["hair_light"])
    s.rect("body_bottom_ov", 0, 0, 16, 8, P["none"])


def hd_arms(s: Sheet) -> None:
    for faces in (
        ("rarm_front", "rarm_back", "rarm_left", "rarm_right"),
        ("larm_front", "larm_back", "larm_left", "larm_right"),
    ):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2] * 2
            s.rect(f, 0, 0, fw, 24, P["skin"] if f is not back and f is not side_in else P["skin_shadow"])
            s.rect(f, 0, 18, fw, 2, P["white"])
            s.rect(f, 0, 19, fw, 1, P["white_sh"])
            s.rect(f, 0, 20, fw, 4, P["skin"])
        s.rect(front, 0, 0, 6, 2, P["skin"])
        s.rect(front, 0, 20, 6, 4, P["skin_shadow"])
        s.rect(side_out, 0, 6, 8, 12, P["skin_shadow"])

    for faces in (
        ("rarm_front_ov", "rarm_back_ov", "rarm_left_ov", "rarm_right_ov"),
        ("larm_front_ov", "larm_back_ov", "larm_left_ov", "larm_right_ov"),
    ):
        front, back, side_in, side_out = faces
        for f in faces:
            fw = FACES[f][2] * 2
            s.rect(f, 0, 0, fw, 24, P["none"])
            s.rect(f, 0, 0, fw, 3, P["white"])
            s.rect(f, 0, 3, fw, 1, P["white_sh"])
            s.px(f, 0, 3, P["none"])
            s.px(f, fw - 1, 3, P["none"])
            s.rect(f, 0, 16, fw, 2, P["white"])
            s.rect(f, 0, 18, fw, 1, P["white_sh"])
        # 外侧与后侧垂下的长发
        s.rect(side_out, 0, 5, 8, 9, P["hair"])
        s.rect(side_out, 0, 5, 8, 1, P["hair_light"])
        s.rect(side_out, 0, 12, 8, 2, P["hair_shadow"])
        s.rect(back, 0, 4, 6, 10, P["hair"])
        s.rect(back, 0, 12, 6, 2, P["hair_shadow"])
        for f in ("rarm_top_ov", "rarm_bottom_ov", "larm_top_ov", "larm_bottom_ov"):
            s.rect(f, 0, 0, FACES[f][2] * 2, 8, P["none"])


def hd_legs(s: Sheet) -> None:
    for front, back, side_in, side_out in (
        ("rleg_front", "rleg_back", "rleg_left", "rleg_right"),
        ("lleg_front", "lleg_back", "lleg_left", "lleg_right"),
    ):
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * 2
            tone = P["skin_shadow"] if f in (back, side_in) else P["skin"]
            s.rect(f, 0, 0, fw, 24, tone)
            # 白色长筒袜（袜口 r11 → r19）
            s.rect(f, 0, 11, fw, 9, P["white"])
            s.rect(f, 0, 11, fw, 1, P["green"])
            s.rect(f, 0, 12, fw, 1, P["green_deep"])
            # 袜身两侧淡蓝阴影
            s.px(f, 0, 15, P["white_sh"])
            s.px(f, 0, 17, P["white_sh"])
            s.px(f, fw - 1, 15, P["white_sh"])
            s.px(f, fw - 1, 17, P["white_sh"])
            # 白鞋
            s.rect(f, 0, 20, fw, 3, P["shoe"])
            s.rect(f, 0, 23, fw, 1, P["shoe_deep"])
            s.rect(f, 0, 20, fw, 1, P["shoe_sh"])
        # 正面/外侧的绿色交叉缎带：一道贯穿袜身的大 X（1 像素线）
        for f in (front, side_out, side_in):
            fw = FACES[f][2] * 2
            xs = [0, 1, 3, 4, 6, 7] if fw == 8 else [0, 1, 2, 3]
            for i, xa in enumerate(xs):
                s.px(f, xa, 13 + i, P["green"])
                s.px(f, fw - 1 - xa, 13 + i, P["green"])
        # 鞋面缎带
        s.rect(front, 2, 21, 4, 1, P["green"])
        s.rect(back, 2, 21, 4, 1, P["shoe_sh"])

    for front, back, side_in, side_out in (
        ("rleg_front_ov", "rleg_back_ov", "rleg_left_ov", "rleg_right_ov"),
        ("lleg_front_ov", "lleg_back_ov", "lleg_left_ov", "lleg_right_ov"),
    ):
        for f in (front, back, side_in, side_out):
            fw = FACES[f][2] * 2
            s.rect(f, 0, 0, fw, 24, P["none"])
            # 裙摆荷叶边盖住大腿上段
            s.rect(f, 0, 0, fw, 8, P["white"])
            s.rect(f, 0, 7, fw, 1, P["white_deep"])
            s.rect(f, 0, 4, fw, 1, P["white_sh"])
        for i in range(4):
            s.rect(front, i * 2, 5, 2, 2, P[FLOWERS[(i + 1) % len(FLOWERS)]])
            s.rect(back, i * 2, 5, 2, 2, P[FLOWERS[(i + 4) % len(FLOWERS)]])
        s.rect(side_out, 0, 5, 8, 2, P[FLOWERS[2]])
        s.rect(side_in, 0, 5, 8, 2, P[FLOWERS[5]])


def enhance(img: Image.Image) -> Image.Image:
    out = img.resize((SIZE, SIZE), Image.Resampling.NEAREST)
    s = Sheet(out, SCALE)
    hd_face(s)
    hd_crown(s)
    hd_dress(s)
    hd_arms(s)
    hd_legs(s)
    return out


def create_skin() -> Image.Image:
    img = Image.new("RGBA", (BASE_SIZE, BASE_SIZE), P["none"])
    draw_base(img)
    return enhance(img)


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
    sy = -math.sin(yaw) * math.sin(pitch) * x + math.cos(pitch) * y + math.cos(yaw) * math.sin(pitch) * z
    depth = -math.sin(yaw) * math.cos(pitch) * x - math.sin(pitch) * y + math.cos(yaw) * math.cos(pitch) * z
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
    head = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    head.alpha_composite(skin.crop((16, 16, 32, 32)))
    head.alpha_composite(skin.crop((80, 16, 96, 32)))
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
    ds = 4
    sheet = Image.new("RGBA", (skin.width * ds, skin.height * ds + 24), (242, 242, 244, 255))
    sheet.alpha_composite(skin.resize((skin.width * ds, skin.height * ds), Image.Resampling.NEAREST))
    ImageDraw.Draw(sheet).text((6, skin.height * ds + 6),
                               f"skin_18.png {skin.width}x{skin.height} (Alex/slim 3px arm)",
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
    draw.text((200, 668), "独角兽 Unicorn — 淡蓝白长发 / 花环 / 白色荷叶边连衣裙 / 绿缎带长筒袜", fill=(38, 48, 58, 255))
    draw.text((200, 692), "Alex (slim) 体型 128×128 玩家皮肤，对应 skin_18 / 皮肤核心·独角兽", fill=(75, 88, 98, 255))
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
