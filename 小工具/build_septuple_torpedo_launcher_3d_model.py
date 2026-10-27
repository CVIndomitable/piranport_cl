#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成七联鱼雷发射器的 3D 手持模型（按 Equip_L_560 原画从零建模）。

只依据原画本身的形体：
  - 后方是一个大型矩形发射箱，箱体下部有一圈深色下缘，下方是两级矮底座；
  - 箱体前脸伸出一束 7 根鱼雷管，按 4 下 3 上两排排列；
    每根管身有 5 道横向加强箍，管口带管箍和端盖（空膛/装填差别只在端盖）；
  - 箱体后上方披着米色帆布防水罩：包住后脸、左端和顶面后半段，
    下缘垂到接近底座，顶面用几片斜置薄板做出褶皱；
  - 箱顶是一块带镶边的凸起面板，前脸下方有一个小台阶。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，长面 uv 夹到 16，纹理不会被拉伸；
  - 圆管用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 装填/空膛的差异只在管口端盖；
  - 模型中心在 x=8，管口朝 +Z，帆布罩在 -Z，物品 display 旋转 180° 后朝角色前方。
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_ROOT = os.path.join(ASSETS, "textures")

NS = "piranport:item/"
ID = "septuple_torpedo_launcher"
PREFIX = ID + "_"
TEX = {
    "hull": NS + PREFIX + "hull",
    "hull_dark": NS + PREFIX + "hull_dark",
    "ring": NS + PREFIX + "ring",
    "tube": NS + PREFIX + "tube",
    "detail": NS + PREFIX + "detail",
    "canvas": NS + PREFIX + "canvas",
    "cap": NS + PREFIX + "cap_empty",
    "cap_loaded": NS + PREFIX + "cap_loaded",
}

VALID_ANGLES = (-45.0, -22.5, 0.0, 22.5, 45.0)

elements = []

# 7 根管：下排 4 根、上排 3 根，X 向错位排布，两排错开半个管距
BOTTOM_XS = (4.1, 6.7, 9.3, 11.9)
TOP_XS = (5.4, 8.0, 10.6)
BOTTOM_Y = 3.1
TOP_Y = 5.45
TUBE_Z0 = 1.0
# 原画里管束长度约等于箱体宽度，不能拉太长
BOTTOM_Z1 = 13.8
TOP_Z1 = 13.2
HOOP_ZS = (3.2, 5.2, 7.2, 9.2, 11.2)


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面的实际尺寸取 uv，长边夹到 16（32px 贴图 / 每单位 2px）。"""
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    w, h = min(w, 16.0), min(h, 16.0)
    max_u = 16.0 - w
    max_v = 16.0 - h
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
        full_uv_faces=()):
    faces_json = {}
    seed = len(elements) + 1
    for face in ("north", "south", "west", "east", "up", "down"):
        face_tex = (faces or {}).get(face, tex)
        entry = {"texture": "#" + face_tex}
        if face in full_uv_faces:
            entry["uv"] = [0, 0, 16, 16]
        else:
            entry["uv"] = _uv_for(face, x1, y1, z1, x2, y2, z2, seed)
        faces_json[face] = entry
    element = {
        "name": name,
        "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
        "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
        "faces": faces_json,
    }
    if rotation:
        element["rotation"] = rotation
    elements.append(element)


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex):
    """轴向沿 Z 的八棱柱：轴对齐方盒 + 绕 Z 转 45° 的方盒。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用几层不同长宽比的方盒叠出圆角矩形足迹（等轴测下近似圆盘）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 底座 =================
box("base_plate", 3.6, 0.0, -5.4, 12.4, 0.4, 2.2, "detail")
box("base_step", 3.0, 0.4, -5.8, 13.0, 1.1, 2.6, "hull_dark")

# ================= 发射箱 =================
# 下缘深色带略微外凸，避免与箱体共面闪烁
box("box_lower", 2.14, 1.05, -6.64, 13.86, 2.7, 1.98, "hull_dark")
box("box_main", 2.2, 1.1, -6.6, 13.8, 7.4, 1.9, "hull")
# 顶部凸起镶板只做帆布罩露出的前半段，后半段被帆布盖住（避免共面/穿插）
box("box_top", 2.35, 7.4, -0.95, 13.65, 7.7, 1.6, "hull")
box("box_top_panel", 3.1, 7.7, -0.85, 12.9, 7.9, 1.0, "ring")
# 前脸：内凹深灰导向板 + 一圈浅色斜面边框（原画前脸有倒角框）
box("front_plate", 2.6, 1.8, 1.9, 13.4, 7.0, 2.3, "detail")
box("front_frame_t", 2.45, 6.9, 1.9, 13.55, 7.25, 2.38, "hull")
box("front_frame_b", 2.45, 1.55, 1.9, 13.55, 1.95, 2.38, "hull")
box("front_frame_l", 2.45, 1.55, 1.9, 2.95, 7.25, 2.38, "hull")
box("front_frame_r", 13.05, 1.55, 1.9, 13.55, 7.25, 2.38, "hull")
# 前脸下方的小台阶（原画箱体底部前伸的一块）
box("front_step", 6.6, 0.7, 2.0, 9.4, 1.6, 3.0, "hull_dark")

# ================= 7 根鱼雷管 =================
for tag, cx, cy, z1 in (
        *[(f"b{i}", x, BOTTOM_Y, BOTTOM_Z1) for i, x in enumerate(BOTTOM_XS)],
        *[(f"t{i}", x, TOP_Y, TOP_Z1) for i, x in enumerate(TOP_XS)]):
    octagon_z(f"tube_{tag}", cx, cy, TUBE_Z0, z1, 0.88, 1.03, "tube")
    octagon_z(f"tube_rear_{tag}", cx, cy, TUBE_Z0 - 0.10, TUBE_Z0 + 0.15,
              0.86, 1.01, "hull_dark")
    for i, z in enumerate(HOOP_ZS):
        octagon_z(f"hoop_{tag}_{i}", cx, cy, z, z + 0.36, 0.92, 1.09, "ring")
    octagon_z(f"muzzle_{tag}", cx, cy, z1 - 0.55, z1, 0.92, 1.09, "ring")
    octagon_z(f"cap_{tag}", cx, cy, z1, z1 + 0.30, 0.84, 0.99, "cap")

# ================= 帆布防水罩（-Z 后方） =================
box("canvas_rear", 1.5, 2.4, -7.5, 14.5, 7.9, -6.55, "canvas")
box("canvas_top", 1.8, 7.42, -7.25, 14.2, 8.05, -1.0, "canvas")
box("canvas_left", 1.35, 2.8, -7.4, 2.25, 8.0, -1.4, "canvas")
box("canvas_right", 13.75, 3.2, -7.2, 14.3, 7.9, -3.6, "canvas")
box("canvas_rear_low_a", 1.5, 1.0, -7.4, 4.4, 2.5, -6.6, "canvas")
box("canvas_rear_low_b", 4.2, 1.5, -7.2, 6.2, 2.5, -6.6, "canvas")
box("canvas_fold_l", 1.25, 1.2, -7.7, 2.7, 3.4, -6.9, "canvas")
# 顶面三道斜置薄板：做出布料褶皱的起伏
box("canvas_ridge_0", 1.9, 7.98, -6.3, 14.1, 8.14, -5.4, "canvas",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 8.06, -5.85]})
box("canvas_ridge_1", 1.9, 7.98, -3.9, 14.1, 8.14, -3.0, "canvas",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 8.06, -3.45]})
box("canvas_ridge_2", 1.9, 8.0, -1.25, 14.1, 8.16, -0.65, "canvas",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 8.08, -0.95]})

DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0], "translation": [0, 2, -1],
        "scale": [0.58, 0.58, 0.58],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0], "translation": [0, 2, -1],
        "scale": [0.58, 0.58, 0.58],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0], "translation": [0, 0, 0],
        "scale": [0.52, 0.52, 0.52],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0], "translation": [0, 0, 0],
        "scale": [0.52, 0.52, 0.52],
    },
    "ground": {
        "rotation": [0, 0, 0], "translation": [0, 2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7],
             "scale": [1, 1, 1]},
}

base = {
    "parent": "minecraft:block/block",
    "gui_light": "front",
    "ambientocclusion": False,
    "textures": {
        "particle": TEX["hull"],
        "hull": TEX["hull"],
        "hull_dark": TEX["hull_dark"],
        "ring": TEX["ring"],
        "tube": TEX["tube"],
        "detail": TEX["detail"],
        "canvas": TEX["canvas"],
        "cap": TEX["cap"],
        "cap_loaded": TEX["cap_loaded"],
    },
    "elements": elements,
}

item_entry = {
    "parent": "builtin/entity",
    "gui_light": "front",
    "display": DISPLAY,
}

loaded_3d = {
    "parent": "piranport:item/" + ID + "_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def validate():
    """写出前自检：from<to、rotation 合法、uv 在 0..16、贴图文件存在。"""
    for el in elements:
        name = el["name"]
        for i, axis in enumerate("xyz"):
            if not el["from"][i] < el["to"][i]:
                raise ValueError(f"{name}: from.{axis} 必须小于 to.{axis}")
            for key in ("from", "to"):
                if not -16.0 <= el[key][i] <= 32.0:
                    raise ValueError(f"{name}: {key}.{axis} 超出 -16..32")
        rot = el.get("rotation")
        if rot and float(rot["angle"]) not in VALID_ANGLES:
            raise ValueError(f"{name}: rotation {rot['angle']} 非法")
        for face, data in el["faces"].items():
            u1, v1, u2, v2 = data["uv"]
            if not (0 <= u1 <= u2 <= 16 and 0 <= v1 <= v2 <= 16):
                raise ValueError(f"{name}.{face}: uv {data['uv']} 越界")
    for ref in TEX.values():
        rel = ref.split(":", 1)[1] + ".png"
        path = os.path.join(TEX_ROOT, rel)
        if not os.path.exists(path):
            raise FileNotFoundError(f"缺少贴图: {path}")
    print(f"校验通过: {len(elements)} 个 element, {len(TEX)} 张贴图")


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    validate()
    dump(ID + "_3d.json", base)
    dump(ID + "_3d_loaded.json", loaded_3d)
    dump(ID + ".json", item_entry)
