#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成五联鱼雷发射器（53-65）的 3D 手持模型（按 Equip_L_747 原画从零建模）。

只依据原画本身的形体：
  - 五根深钢蓝灰鱼雷管并排，管口朝前，每根带三道细管箍与管口箍；
  - 管尾上方散布大量小阀门、接线盒、小方盒，越靠管尾越密集；
  - 后段下方是一块横向平板托架，托架下是一个外翻底缘的圆鼓底座；
  - 圆鼓两侧各有两根向外斜撑的支腿，鼓前方有一个小方箱与一块舱口板；
  - 没有大型控制室（区别于 53-56）。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 鱼雷管按「顶亮、侧中、底暗」取 tube 贴图的不同 v 段，八棱柱因此有圆筒感；
  - 圆管/圆鼓用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 管口端盖整张贴图铺满，空膛/装填的差异只在这一层；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "quintuple_torpedo_launcher_53_65_"
TEX = {
    "hull": NS + PREFIX + "hull",
    "hull_dark": NS + PREFIX + "hull_dark",
    "ring": NS + PREFIX + "ring",
    "tube": NS + PREFIX + "tube",
    "detail": NS + PREFIX + "detail",
    "cap": NS + PREFIX + "cap_empty",
    "cap_loaded": NS + PREFIX + "cap_loaded",
}

elements = []

# 管间距 2.1，整体以 x=8 为中心
TUBE_XS = (3.8, 5.9, 8.0, 10.1, 12.2)
TUBE_Y = 4.4
TUBE_Z0 = 1.6
TUBE_Z1 = 24.2

DRUM_X = 8.0
DRUM_Z = 12.0

# 鱼雷管各面取 tube 贴图的哪一段：贴图自上而下由亮到暗，
# 上/下与两侧取不同 v，八棱柱在游戏里就有圆筒的受光层次。
TUBE_UV = {
    "up": [0, 1, 16, 5],
    "down": [0, 9, 16, 13],
    "north": [0, 4.5, 16, 8.5],
    "south": [0, 4.5, 16, 8.5],
    "east": [0, 4.5, 16, 8.5],
    "west": [0, 4.5, 16, 8.5],
}


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
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
        full_uv_faces=(), uvs=None):
    faces_json = {}
    seed = len(elements) + 1
    for face in ("north", "south", "west", "east", "up", "down"):
        face_tex = (faces or {}).get(face, tex)
        entry = {"texture": "#" + face_tex}
        if uvs and face in uvs:
            entry["uv"] = list(uvs[face])
        elif face in full_uv_faces:
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


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex, faces=None,
              full_uv_faces=(), uvs=None):
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces, full_uv_faces=full_uv_faces, uvs=uvs)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces, full_uv_faces=full_uv_faces, uvs=uvs,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, outer, tex):
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 圆鼓底座 =================
# 原画里的底座是个外翻底缘的圆鼓：底缘 → 鼓身 → 顶法兰，自下而上略收
round_footprint("base_rim", DRUM_X, DRUM_Z, 2.5, 0.0, 0.35, "hull_dark")
cylinder_y("base_drum", DRUM_X, DRUM_Z, 0.35, 1.95, 2.05, 2.9, "hull_dark")
cylinder_y("base_flange", DRUM_X, DRUM_Z, 1.95, 2.11, 2.2, 3.1, "hull")

# 两侧外撑支腿：竖盒绕 z 轴转 ±45°，顶端顶住托架底面、底端向外撇。
# 旋转后棱角会再抬高 (半宽+半高)/√2，所以高度收到 2.2 以内，避免顶穿平板。
for side, sign, zc in (("port", 1, 10.0), ("port", 1, 13.0),
                       ("stbd", -1, 10.0), ("stbd", -1, 13.0)):
    cx = 5.5 if sign > 0 else 10.5
    box(f"leg_{side}_{int(zc)}", cx - 0.22, 0.3, zc, cx + 0.22, 2.2,
        zc + 0.6, "hull_dark",
        rotation={"angle": 45 * sign, "axis": "z",
                  "origin": [cx, 1.25, zc + 0.3]})

# 鼓前方的舱口板与独立小方箱（原画中贴在鼓前左下）
box("drum_hatch", 8.55, 0.72, DRUM_Z + 1.88, 9.7, 1.52, DRUM_Z + 2.2, "hull")
box("front_box", 4.95, 0.2, 13.35, 5.9, 1.02, 14.3, "hull_dark")
box("front_box_lid", 5.01, 1.02, 13.41, 5.84, 1.14, 14.24, "detail")

# ================= 横向托架平板 =================
# 平板只盖住后段，前面让圆鼓露出一截，轮廓才不会读成一块方板
box("cradle_plate", 2.7, 2.12, 5.4, 13.3, 2.52, 13.0, "hull")
box("cradle_lip", 2.95, 1.98, 13.0, 13.05, 2.24, 13.42, "hull_dark")
box("cradle_rear_bar", 3.0, 2.52, 5.55, 13.0, 2.86, 6.15, "detail")

# ================= 五根鱼雷管 =================
for tag, cx in zip("abcde", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 0.66, 1.02, "tube",
              uvs=TUBE_UV)
    # 管尾端圈
    octagon_z(f"tube_tail_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.25, TUBE_Z0 + 0.15,
              0.68, 1.05, "hull_dark")
    # 三道细管箍
    for i, z in enumerate((8.0, 12.6, 17.2)):
        octagon_z(f"tube_band_{tag}_{i}", cx, TUBE_Y, z, z + 0.32,
                  0.71, 1.10, "ring")
    # 管口箍与端盖
    octagon_z(f"tube_muzzle_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.75, TUBE_Z1 - 0.35,
              0.73, 1.13, "ring")
    octagon_z(f"cap_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.32,
              0.66, 1.02, "cap",
              full_uv_faces=("north", "south", "west", "east", "up", "down"))
    # 管下托座（坐在托架平板上）
    for i, z in enumerate((5.9, 9.0, 12.0)):
        box(f"tube_pad_{tag}_{i}", cx - 0.56, 2.52, z, cx + 0.56, 3.72,
            z + 0.5, "detail")

# ================= 管尾上方的小阀门/接线盒 =================
# 大件：每根管顶各一个「底座 + 偏心顶盖」的阀门盒，越靠管尾越大
big_fittings = (
    (0, 6.9, 0.66, 0.46, 0.14),
    (1, 5.5, 0.76, 0.52, -0.16),
    (2, 4.35, 0.88, 0.60, 0.12),
    (3, 3.0, 0.92, 0.62, -0.14),
    (4, 4.05, 0.82, 0.56, 0.18),
)
for idx, z, half, height, cap_off in big_fittings:
    cx = TUBE_XS[idx]
    y0 = 5.02
    box(f"valve_{idx}_body", cx - half, y0, z, cx + half, y0 + height,
        z + half * 2.0, "detail")
    box(f"valve_{idx}_cap", cx - half * 0.58 + cap_off, y0 + height,
        z + 0.14, cx + half * 0.58 + cap_off, y0 + height + 0.34,
        z + half * 2.0 - 0.14, "hull_dark")

# 第二排：更靠前的两个小阀
for idx, z, half in ((2, 7.9, 0.48), (4, 6.7, 0.46)):
    cx = TUBE_XS[idx]
    box(f"valve2_{idx}_body", cx - half, 5.0, z, cx + half, 5.36,
        z + half * 1.8, "detail")
    box(f"valve2_{idx}_cap", cx - half * 0.5, 5.36, z + 0.12,
        cx + half * 0.5, 5.58, z + half * 1.8 - 0.12, "hull_dark")

# 管间沟槽里的小接线盒，错落摆放
for i, (cx, z, size, h) in enumerate((
        (6.95, 6.2, 0.40, 0.32), (9.05, 5.0, 0.36, 0.28),
        (11.15, 6.3, 0.38, 0.30), (6.95, 9.8, 0.32, 0.26),
        (11.15, 9.2, 0.34, 0.28), (9.05, 8.4, 0.30, 0.24))):
    box(f"junction_{i}", cx - size / 2, 4.45, z, cx + size / 2, 4.45 + h,
        z + size, "detail")

# 管顶更靠前的零星小件（照原画散布到中段）
for i, (idx, z, size) in enumerate((
        (0, 10.6, 0.42), (1, 12.4, 0.36), (2, 13.9, 0.44),
        (3, 12.0, 0.40), (4, 14.6, 0.36), (1, 16.4, 0.30),
        (3, 15.2, 0.32), (0, 13.2, 0.28))):
    cx = TUBE_XS[idx]
    box(f"stud_{i}", cx - size / 2, 5.05, z, cx + size / 2, 5.05 + size * 0.72,
        z + size, "ring")

# 外侧（右舷）挂在管子侧面的小支盒
for i, (z, h) in enumerate(((5.9, 0.44), (7.6, 0.36), (9.9, 0.32))):
    box(f"side_box_{i}", 12.9, 4.62, z, 13.42, 4.62 + h, z + 0.58, "detail")

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
    "parent": "piranport:item/quintuple_torpedo_launcher_53_65_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("quintuple_torpedo_launcher_53_65_3d.json", base)
    dump("quintuple_torpedo_launcher_53_65_3d_loaded.json", loaded_3d)
    dump("quintuple_torpedo_launcher_53_65.json", item_entry)
