#!/usr/bin/env python3
"""深海驱逐 (No.901 敌驱逐舰) 实体贴图生成器.

立绘依据: 立绘/No901_敌驱逐舰.png

从立绘提取的识别锚点 (YSM 规范 §10):
  1. 银灰长直发 + 刘海
  2. 右眼 = 粉色发光机械眼; 左眼被黑色护目镜遮挡
  3. 白皮肤 + 黑白色比基尼式胸甲
  4. 口鼻部黑色带齿面罩
  5. 腿部 / 缠腰的黑色六边形鳞甲束
  6. 舰装核心 —— 一条蓝灰色「黑鱼」(鲸): 弧形身体盘绕左肩, 分叉尾鳍,
     前端黑色机械炮舱并伸出粉色电路炮管
  7. 身后大型倾斜黑盾, 内嵌粉色电路纹路
  8. 两侧炮舱, 及带粉色发光眼的黑色舱体

UV 布局由 tools/deepocean_uv_layout.py 唯一定义, 与
DeepOceanDestroyerModel 的 texOffs 一致 —— 改布局只改那一份.

运行: python3 tools/make_deepocean_destroyer_texture.py
输出: src/main/resources/assets/piranport/textures/entity/deep_ocean/destroyer_model.png
      build/offline-renders/deepocean_destroyer_texture.png
"""
from __future__ import annotations

import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from deepocean_uv_layout import BY_NAME, LAYOUT, validate  # noqa: E402

TEXTURE_OUT = ROOT / "src/main/resources/assets/piranport/textures/entity/deep_ocean/destroyer_model.png"
SHEET_OUT = ROOT / "build/offline-renders/deepocean_destroyer_texture.png"

SIZE = 128
rng = random.Random(901)

# ---------------------------------------------------------------- 调色板
P = {
    "skin":      (247, 231, 224, 255),
    "skin_sh":   (226, 199, 193, 255),
    "skin_hi":   (255, 246, 242, 255),
    "blush":     (238, 176, 178, 255),

    "hair_hi":   (240, 238, 244, 255),
    "hair":      (198, 194, 206, 255),
    "hair_sh":   (150, 146, 160, 255),
    "hair_deep": (104, 100, 116, 255),

    "white":     (243, 241, 240, 255),
    "white_sh":  (202, 198, 201, 255),

    "black":      (46, 42, 50, 255),
    "black_hi":   (76, 70, 82, 255),
    "black_sh":   (32, 29, 36, 255),
    "black_deep": (20, 18, 24, 255),
    "panel":      (58, 53, 62, 255),

    "metal":    (110, 106, 118, 255),
    "metal_hi": (158, 154, 168, 255),

    "fish":       (118, 132, 148, 255),
    "fish_hi":    (158, 172, 188, 255),
    "fish_sh":    (84, 98, 116, 255),
    "fish_deep":  (58, 70, 86, 255),
    "fish_belly": (188, 198, 208, 255),

    "glow":     (255, 106, 214, 255),
    "glow_hot": (255, 190, 242, 255),
    "glow_dk":  (196, 42, 154, 255),
    "eye_pink": (255, 128, 222, 255),

    "eye_line": (36, 30, 44, 255),
    "teeth":    (238, 233, 228, 255),
    "teeth_sh": (196, 190, 186, 255),
}


def box(d, r, color):
    """填一个 UV 矩形 (x0,y0,x1,y1) 开区间."""
    x0, y0, x1, y1 = r
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=P[color] if isinstance(color, str) else color)


def dot(d, x, y, color):
    d.point((x, y), fill=P[color] if isinstance(color, str) else color)


def line(d, r, color):
    x0, y0, x1, y1 = r
    d.line([x0, y0, x1 - 1, y1 - 1], fill=P[color])


def face_of(name):
    """取某 box 的正面 UV 矩形 —— 画脸/图案用."""
    return BY_NAME[name].front


def side_of(name):
    return BY_NAME[name].right


def top_of(name):
    return BY_NAME[name].top


# ---------------------------------------------------------------- 人物
def draw_body(d):
    b = BY_NAME["body"]
    box(d, b.rect, "skin")
    box(d, b.back, "black")            # 背面 = 黑色背甲
    box(d, b.left, "black")
    box(d, b.right, "black")
    # 正面: 白皮肤 + 黑色比基尼胸甲
    f = b.front
    box(d, f, "skin")
    box(d, (f[0], f[1], f[2], f[1] + 1), "black")               # 上缘
    box(d, (f[0], f[3] - 2, f[2], f[3]), "black")               # 下缘
    dot(d, f[0] + 2, f[1] + 2, "skin_sh")


def draw_waist(d):
    b = BY_NAME["waist"]
    box(d, b.rect, "skin")
    box(d, b.back, "black")
    box(d, b.left, "black")
    box(d, b.right, "black")
    box(d, b.front, "white")
    box(d, (b.front[0], b.front[3] - 1, b.front[2], b.front[3]), "black")


def draw_hips(d):
    b = BY_NAME["hips"]
    box(d, b.rect, "black")
    box(d, top_of("hips"), "black_hi")


def draw_head(d):
    b = BY_NAME["head"]
    box(d, b.rect, "skin")
    box(d, b.left, "skin_sh")
    box(d, b.right, "skin_sh")

    # 正面画脸: 宽 5, 高 5
    f = b.front
    fx, fy = f[0], f[1]
    box(d, f, "skin")

    # 角色右眼 (画面左) —— 粉色发光机械眼
    box(d, (fx + 1, fy + 1, fx + 3, fy + 3), "eye_pink")
    box(d, (fx + 1, fy + 2, fx + 3, fy + 3), "glow_hot")
    dot(d, fx + 1, fy + 1, "glow")
    # 上眼线
    box(d, (fx + 1, fy, fx + 3, fy + 1), "eye_line")

    # 角色左眼 (画面右) —— 黑色护目镜遮挡
    box(d, (fx + 3, fy, fx + 5, fy + 3), "black")
    dot(d, fx + 4, fy + 1, "glow_dk")

    # 口鼻部黑色带齿面罩
    box(d, (fx, fy + 3, fx + 5, fy + 4), "black")
    for i in range(0, 5, 2):
        dot(d, fx + i, fy + 3, "teeth")
    box(d, (fx, fy + 4, fx + 5, fy + 5), "black_sh")
    for i in range(1, 5, 2):
        dot(d, fx + i, fy + 4, "teeth")

    # 额发阴影
    box(d, (fx, fy, fx + 5, fy + 1), "skin_sh")


def draw_chin(d):
    b = BY_NAME["chin"]
    box(d, b.rect, "skin")
    box(d, (b.front[0], b.front[1], b.front[2], b.front[1] + 1), "black")


def draw_hair(d):
    # 头顶
    b = BY_NAME["hair_top"]
    box(d, b.rect, "hair")
    box(d, b.top, "hair_hi")
    for (x, y) in _scatter(b.rect, 0.22):
        dot(d, x, y, "hair_sh")

    # 刘海
    b = BY_NAME["bangs"]
    box(d, b.rect, "hair")
    f = b.front
    for x in range(f[0], f[2], 2):
        box(d, (x, f[1], x + 1, f[3]), "hair_hi")
    box(d, (f[0], f[3] - 1, f[2], f[3]), "hair_deep")

    # 后发 (大体块, 发量)
    b = BY_NAME["hair_back"]
    box(d, b.rect, "hair")
    box(d, b.back, "hair_deep")
    for (x, y) in _scatter(b.rect, 0.18):
        dot(d, x, y, "hair_hi")

    # 侧发束 (发梢渐变, 规范 §5.2 长短节奏)
    for nm, hi_side in (("hair_sideL", "right"), ("hair_sideR", "left")):
        b = BY_NAME[nm]
        box(d, b.rect, "hair")
        box(d, getattr(b, hi_side), "hair_sh")
        f = b.front
        box(d, (f[0], f[3] - 2, f[2], f[3]), "hair_deep")


def _scatter(rect, ratio):
    x0, y0, x1, y1 = rect
    out = []
    for y in range(y0, y1):
        for x in range(x0, x1):
            if rng.random() < ratio:
                out.append((x, y))
    return out


def draw_arms(d):
    for nm in ("armL_upper", "armR_upper"):
        b = BY_NAME[nm]
        box(d, b.rect, "skin")
        box(d, b.left, "skin_sh")
        f = b.front
        box(d, (f[0], f[3] - 3, f[2], f[3]), "white")   # 手套袖口
        box(d, (f[0], f[3] - 3, f[2], f[3] - 2), "black")
    for nm in ("armL_lower", "armR_lower"):
        b = BY_NAME[nm]
        box(d, b.rect, "white")
        box(d, b.left, "white_sh")
        f = b.front
        box(d, (f[0], f[1], f[2], f[1] + 1), "black")


def hex_scales(d, rect, base="black", hi="black_hi", lo="black_deep", step=3):
    """六边形鳞甲纹样 —— 立绘腿部/触手装甲的标志纹理."""
    box(d, rect, base)
    x0, y0, x1, y1 = rect
    for row, y in enumerate(range(y0, y1, step)):
        off = 0 if row % 2 == 0 else step // 2
        for x in range(x0 + off, x1, step):
            d.arc([x - 1, y, x + 1, min(y + step - 1, y1 - 1)], 200, 340, fill=P[hi])
            dot(d, x, min(y + step - 1, y1 - 1), lo)


def draw_legs(d):
    for nm in ("legL_upper", "legL_lower", "legR_upper", "legR_lower"):
        hex_scales(d, BY_NAME[nm].rect)


def draw_tentacles(d):
    for nm in ("tentacle_a", "tentacle_b", "tentacle_c", "tentacle_d"):
        hex_scales(d, BY_NAME[nm].rect)


# ---------------------------------------------------------------- 发光件
def draw_glow_parts(d):
    b = BY_NAME["chest_core"]
    box(d, b.rect, "glow_dk")
    box(d, b.front, "glow")
    dot(d, b.front[0], b.front[1], "glow_hot")

    b = BY_NAME["face_glow"]
    box(d, b.rect, "glow_dk")
    box(d, b.front, "glow")
    box(d, (b.front[0], b.front[1], b.front[2], b.front[3] - 1), "glow_hot")

    b = BY_NAME["visor"]
    box(d, b.rect, "black")
    dot(d, b.front[0], b.front[1], "glow_dk")

    b = BY_NAME["eye_L"]
    box(d, b.rect, "glow")
    dot(d, (b.front[0] + b.front[2]) // 2, b.front[1], "glow_hot")

    for nm in ("gun_glow_a", "gun_glow_b"):
        b = BY_NAME[nm]
        box(d, b.rect, "black_deep")
        f = b.front
        d.line([f[0], f[1], f[0], f[3] - 1], fill=P["glow"])
        d.line([f[2] - 1, f[1], f[2] - 1, f[3] - 1], fill=P["glow_dk"])
        for y in range(f[1], f[3]):
            dot(d, f[0], y, "glow")

    b = BY_NAME["pod_eye"]
    box(d, b.rect, "glow")
    box(d, b.front, "glow_hot")


def draw_circuit(d, rect, base="black_deep", trace="glow_dk", hot="glow"):
    """粉色电路纹路 —— 立绘盾内部/炮管的标志纹样."""
    box(d, rect, base)
    x0, y0, x1, y1 = rect
    spine = (x0 + x1) // 2
    d.line([spine, y0, spine, y1 - 1], fill=P[hot])
    for y in range(y0 + 1, y1, 3):
        d.line([x0, y, x1 - 1, y], fill=P[trace])
        dot(d, x0, y, hot)
        dot(d, x1 - 1, y, hot)


def draw_shield(d):
    for nm in ("shield_upper", "shield_left", "shield_right", "shield_lower"):
        b = BY_NAME[nm]
        box(d, b.rect, "black")
        f = b.front
        # 铆钉 + 边缘高光 (明度分层, 规范 §8.3)
        d.line([f[0], f[1], f[2] - 1, f[1]], fill=P["panel"])
        for x in range(f[0] + 1, f[2], 4):
            dot(d, x, f[1] + 1, "metal_hi")
        for x in range(f[0], f[2], 3):
            d.line([x, f[3] - 1, x, f[3] - 1], fill=P["black_sh"])

    b = BY_NAME["shield_inner"]
    box(d, b.rect, "black_deep")
    box(d, b.front, "black_sh")


def draw_circuit_parts(d):
    b = BY_NAME["circuit_spoke"]
    draw_circuit(d, b.front)
    b = BY_NAME["circuit_arcA"]
    box(d, b.rect, "black_deep")
    f = b.front
    d.line([f[0], f[1], f[2] - 1, f[1]], fill=P["glow"])
    d.line([f[0], f[3] - 1, f[2] - 1, f[3] - 1], fill=P["glow_dk"])
    b = BY_NAME["circuit_arcB"]
    box(d, b.rect, "black_deep")
    f = b.front
    d.line([f[0], f[1], f[2] - 1, f[1]], fill=P["glow"])


def draw_side_rigging(d):
    for side in ("L", "R"):
        b = BY_NAME[f"rig_{side}_body"]
        box(d, b.rect, "black")
        box(d, b.front, "panel")
        f = b.front
        for x in range(f[0] + 1, f[2], 3):
            dot(d, x, f[1] + 1, "metal_hi")

        b = BY_NAME[f"rig_{side}_gun"]
        box(d, b.rect, "metal")
        line(d, b.front, "metal_hi")

        b = BY_NAME[f"rig_{side}_pod"]
        box(d, b.rect, "black")
        dot(d, b.front[0], b.front[1], "black_hi")
        # 圆形舱体的弧形高光
        d.arc([b.front[0], b.front[1], b.front[2] - 1, b.front[3] - 1], 0, 360,
              fill=P["black_hi"])

        b = BY_NAME[f"rig_{side}_mount"]
        box(d, b.rect, "black")
        box(d, b.back, "black_deep")


# ---------------------------------------------------------------- 黑鱼
def draw_black_fish(d):
    # 鱼身: 背深腹浅 + 鳞纹
    b = BY_NAME["fish_body"]
    box(d, b.rect, "fish")
    f = b.front
    box(d, (f[0], f[1], f[2], f[1] + 1), "fish_deep")            # 背脊
    box(d, (f[0], f[3] - 1, f[2], f[3]), "fish_belly")           # 腹部
    for y in range(f[1] + 1, f[3] - 1, 2):
        d.line([f[0], y, f[2] - 1, y], fill=P["fish_sh"])
    for x in range(f[0] + 1, f[2] - 1, 2):
        d.line([x, f[1] + 1, x, f[3] - 2], fill=P["fish_hi"])
    box(d, b.back, "fish_deep")
    box(d, b.left, "fish_deep")
    box(d, b.right, "fish_belly")

    # 胸鳍
    b = BY_NAME["fish_fin"]
    box(d, b.rect, "fish")
    for y in range(b.front[1], b.front[3], 1):
        d.line([b.front[0], y, b.front[2] - 1, y], fill=P["fish_sh"])
    d.line([b.front[0], b.front[1], b.front[2] - 1, b.front[1]], fill=P["fish_hi"])

    # 机械炮舱
    b = BY_NAME["fish_mount"]
    box(d, b.rect, "black")
    box(d, b.front, "panel")
    f = b.front
    d.arc([f[0], f[1], f[2] - 1, f[3] - 1], 0, 360, fill=P["black_hi"])
    box(d, (f[0] + 1, f[1] + 1, f[2] - 1, f[3] - 1), "black_deep")
    draw_circuit(d, (f[0] + 1, f[1] + 1, f[2] - 1, f[3] - 1))
    for x in range(f[0] + 1, f[2], 3):
        dot(d, x, f[1], "metal_hi")

    # 炮管 (长条, 带粉色电路)
    b = BY_NAME["fish_gun"]
    box(d, b.rect, "black_deep")
    f = b.front
    d.line([f[0], (f[1] + f[3]) // 2, f[2] - 1, (f[1] + f[3]) // 2], fill=P["glow"])
    for y in range(f[1] + 1, f[3], 2):
        dot(d, f[0], y, "glow_dk")
        dot(d, f[2] - 1, y, "glow")

    # 尾柄 + 分叉尾鳍
    b = BY_NAME["tail_stem"]
    box(d, b.rect, "fish")
    box(d, b.front, "fish_sh")
    b = BY_NAME["tail_fluke"]
    box(d, b.rect, "fish")
    f = b.front
    d.line([f[0], f[1], f[2] - 1, f[1]], fill=P["fish_hi"])
    d.line([f[0], f[3] - 1, f[2] - 1, f[3] - 1], fill=P["fish_deep"])
    box(d, ((f[0] + f[2]) // 2 - 1, f[1], (f[0] + f[2]) // 2 + 1, f[3]), "fish_deep")


# ---------------------------------------------------------------- 主流程
def build() -> Image.Image:
    errs = validate()
    if errs:
        raise SystemExit("UV 布局有问题, 先修 tools/deepocean_uv_layout.py:\n  "
                         + "\n  ".join(sorted(set(errs))[:10]))

    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    draw_body(d)
    draw_waist(d)
    draw_hips(d)
    draw_head(d)
    draw_chin(d)
    draw_hair(d)
    draw_arms(d)
    draw_legs(d)
    draw_tentacles(d)
    draw_glow_parts(d)
    draw_shield(d)
    draw_circuit_parts(d)
    draw_side_rigging(d)
    draw_black_fish(d)

    return img


def build_sheet(img: Image.Image) -> Image.Image:
    scale = 5
    out = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    d = ImageDraw.Draw(out)
    for b in LAYOUT:
        x0, y0, x1, y1 = [v * scale for v in b.rect]
        d.rectangle([x0, y0, x1 - 1, y1 - 1], outline=(90, 160, 255, 200))
    for i in range(0, SIZE + 1, 16):
        d.line([i * scale, 0, i * scale, SIZE * scale], fill=(255, 90, 90, 70))
        d.line([0, i * scale, SIZE * scale, i * scale], fill=(255, 90, 90, 70))
    return out


def main():
    img = build()
    TEXTURE_OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEXTURE_OUT)
    SHEET_OUT.parent.mkdir(parents=True, exist_ok=True)
    build_sheet(img).save(SHEET_OUT)
    print(f"wrote {TEXTURE_OUT}")
    print(f"wrote {SHEET_OUT}")


if __name__ == "__main__":
    main()
