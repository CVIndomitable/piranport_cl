#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成五连鱼雷发射器（53-56）的 3D 手持模型（按 Equip_L_672 原画从零建模）。

只依据原画本身的形体：
  - 五根深钢蓝灰鱼雷管，管口朝前，带两道管箍；
  - 管尾上方排列着多层方形机械件（装填/止动机构）；
  - 中前部顶着一个带斜面和椭圆舷窗的方形控制室，屋顶有一个小圆柱观测镜；
  - 下方是圆形座板 + 圆鼓底座 + 托架，前侧挂着一部小梯子；
  - 管束前下方还有一个独立的小配电箱。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆管/圆鼓用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 装填/空膛的差异只在管口端盖；
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
PREFIX = "quintuple_torpedo_launcher_53_56_"
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

TUBE_XS = (3.8, 5.9, 8.0, 10.1, 12.2)
TUBE_Y = 4.4
TUBE_Z0 = 1.5
TUBE_Z1 = 25.0


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


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex, faces=None):
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
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


# ================= 底座 =================
round_footprint("base_plate", 8.0, 14.0, 3.6, 0.0, 0.35, "hull_dark")
cylinder_y("base_drum", 8.0, 14.0, 0.35, 2.0, 1.6, 2.3, "hull_dark")
box("cradle", 2.8, 2.0, 10.0, 13.2, 3.8, 18.0, "hull_dark")

# ================= 五根鱼雷管 =================
for tag, cx in zip("abcde", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 0.70, 1.00, "tube")
    octagon_z(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.15, TUBE_Z0 + 0.05,
              0.68, 0.98, "hull_dark")
    for i, z in enumerate((10.5, 21.0)):
        octagon_z(f"tube_ring_{tag}_{i}", cx, TUBE_Y, z, z + 0.35,
                  0.76, 1.08, "ring")
    octagon_z(f"muzzle_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.45, TUBE_Z1,
              0.74, 1.04, "ring")
    octagon_z(f"cap_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.30,
              0.70, 1.00, "cap")
    # 管尾上方的多层方形机械件
    for i, z in enumerate((3.8, 5.4, 7.0)):
        box(f"fitting_{tag}_{i}", cx - 0.70, 4.9, z, cx + 0.70, 5.35,
            z + 0.90, "detail")
        offset = -0.20 if i % 2 == 0 else 0.20
        box(f"fitting_cap_{tag}_{i}", cx - 0.35 + offset, 5.35, z + 0.15,
            cx + 0.35 + offset, 5.65, z + 0.70, "detail")

# ================= 方形控制室 =================
box("house_main", 8.3, 5.8, 11.5, 13.1, 8.8, 17.0, "hull")
box("house_side_port", 8.3, 6.0, 11.8, 8.6, 8.6, 16.7, "hull_dark")
box("house_side_stbd", 12.8, 6.0, 11.8, 13.1, 8.6, 16.7, "hull_dark")
box("house_face", 8.5, 6.0, 17.0, 12.9, 8.6, 17.25, "hull")
# 前脸椭圆舷窗用八角形近似
octagon_z("house_porthole", 10.2, 7.3, 17.25, 17.4, 0.32, 0.50, "ring")
# 屋顶观测镜
cylinder_y("house_sight_base", 12.0, 13.6, 8.8, 9.15, 0.28, 0.38, "hull_dark")
cylinder_y("house_sight", 12.0, 13.6, 9.15, 9.75, 0.18, 0.26, "detail")

# 前下方小配电箱
box("front_box", 5.2, 2.8, 17.5, 6.8, 4.2, 18.6, "hull_dark")

# 前侧小梯子
for x in (7.35, 8.45):
    box(f"ladder_rail_{x}", x, 0.5, 18.3, x + 0.14, 3.2, 18.44, "detail")
for i, y in enumerate((1.0, 1.6, 2.2)):
    box(f"ladder_rung_{i}", 7.35, y, 18.34, 8.59, y + 0.12, 18.40, "detail")

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
    "parent": "piranport:item/quintuple_torpedo_launcher_53_56_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("quintuple_torpedo_launcher_53_56_3d.json", base)
    dump("quintuple_torpedo_launcher_53_56_3d_loaded.json", loaded_3d)
    dump("quintuple_torpedo_launcher_53_56.json", item_entry)
