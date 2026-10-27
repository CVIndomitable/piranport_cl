#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 CLT四联鱼雷发射器零式 的 3D 手持模型（按 Equip_L_704 原画从零建模）。

只依据原画本身的形体：
  - 方正的石板灰装甲发射箱：顶板亮、前脸与侧面深，边缘有包边；
  - 四根鱼雷管从箱体前脸伸出，管身有一圈圈细密的环向接缝；
  - 箱体 +X 侧面靠上有一排四个小舷窗，箱体后下方是底座；
  - 箱体后方是一片立板，板上四个 C 形大卡箍分别扣住一根鱼雷管尾部。

注意：原画左上角那盏探照灯是独立物体，按用户要求**不建模**。

坐标约定：
  - 管口朝 +Z，模型中心 x=8；每个模型单位 2px，长面 uv 夹到 16；
  - 装填/空膛的差异只在管口端盖（cap 贴图）。

产出：
  models/item/clt_quad_torpedo_launcher_type0_3d.json         空膛 3D 模型
  models/item/clt_quad_torpedo_launcher_type0_3d_loaded.json  装填 3D 模型
  models/item/clt_quad_torpedo_launcher_type0.json            builtin/entity 物品入口
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "clt_quad_torpedo_launcher_type0_"
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

# 四根鱼雷管：中心 x=8，等距 3.0
TUBE_XS = (3.50, 6.50, 9.50, 12.50)
TUBE_Y = 4.00
BOX_X0, BOX_X1 = 1.80, 14.20
BOX_Z0, BOX_Z1 = 0.00, 8.00
TUBE_REAR = -1.20
TUBE_FRONT = 18.50


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面尺寸给 uv 窗口（uv 单位=模型单位；贴图 32px 时 16uv=整图）。

    面宽高与 uv 窗口一致，保证每模型单位恒定 2 像素，纹理不被拉伸。
    超过 16 单位的长侧面把窗口夹到 16（uv 不能越界），接受轻微拉伸。
    """
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    w, h = min(w, 16.0), min(h, 16.0)
    max_u = 16.0 - w
    max_v = 16.0 - h
    # 确定性错位，避免所有面采同一块纹理
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
        full_uv_faces=()):
    """添加一个盒子。

    faces: 覆盖单面材质，如 {"south": "cap"}。
    rotation: 原版元素旋转（单轴，角度仅 ±45/±22.5/0）。
    full_uv_faces: 这些面改用整图 uv（用于纯色小面）。
    """
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


def octagon(name, cx, cy, z1, z2, inner, outer, tex, faces=None):
    """八棱柱：轴对齐方盒（半宽 inner）+ 45° 旋转方盒（外接半径 outer）。

    鱼雷管与管箍都用它近似圆柱。
    """
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


# ================= 底座与箱体 =================
box("base_foot", 1.90, 0.00, 0.30, 14.10, 0.32, 8.00, "detail")
box("base_block", 2.40, 0.32, 0.50, 13.60, 1.20, 7.80, "hull_dark")
box("hull_main", BOX_X0, 1.20, BOX_Z0, BOX_X1, 6.20, 7.80, "hull_dark")
box("hull_rim", 1.50, 6.20, -0.20, 14.50, 6.85, 8.10, "hull_dark")
box("top_plate", 1.40, 6.85, -0.30, 14.60, 7.55, 7.80, "hull")
box("top_panel", 2.00, 7.55, 0.20, 14.00, 7.80, 7.20, "hull")

# 前脸（+Z）：整体深色，下方再向管束方向伸出一级楔形底板
box("front_plate", 2.00, 1.40, BOX_Z1, 14.00, 6.20, BOX_Z1 + 0.30,
    "hull_dark")
box("front_wedge_a", 2.60, 1.30, BOX_Z1 + 0.30, 13.40, 4.10,
    BOX_Z1 + 1.10, "hull_dark")
box("front_wedge_b", 3.10, 1.50, BOX_Z1 + 1.10, 12.90, 3.60,
    BOX_Z1 + 1.80, "hull_dark")
# 前左立边的一道亮包边（原画最左侧的浅色竖条）
box("corner_strip", 1.36, 1.20, 7.70, 1.70, 7.55, 8.05, "detail")

# ================= +X 侧面：一排小舷窗 =================
for i, z in enumerate((1.20, 2.55, 3.90, 5.25)):
    box(f"port_frame_{i}", BOX_X1, 4.55, z, BOX_X1 + 0.18, 5.25,
        z + 0.70, "detail")
    box(f"port_glass_{i}", BOX_X1 + 0.18, 4.65, z + 0.10,
        BOX_X1 + 0.24, 5.15, z + 0.60, "hull_dark")

# ================= 后方卡箍立板与四个 C 形卡箍 =================
box("clamp_plate", 1.60, 1.20, -0.55, 14.40, 6.80, 0.00, "hull_dark")
box("clamp_beam", 2.40, 2.30, -1.15, 13.60, 2.60, -0.55, "detail")
for tag, cx in zip("abcd", TUBE_XS):
    box(f"clamp_arm_{tag}", cx - 0.16, 3.00, -1.15, cx + 0.16, 5.10,
        -0.55, "detail")
    box(f"clamp_low_{tag}", cx - 0.95, 3.00, -1.15, cx + 0.95, 3.32,
        -0.25, "detail")
    box(f"clamp_up_{tag}", cx - 0.95, 4.78, -1.15, cx + 0.95, 5.10,
        -0.25, "detail")
    box(f"clamp_tip_{tag}", cx - 0.92, 3.32, -1.15, cx - 0.62, 4.78,
        -0.25, "detail")
    box(f"clamp_tip2_{tag}", cx + 0.62, 3.32, -1.15, cx + 0.92, 4.78,
        -0.25, "detail")

# ================= 四根鱼雷管 =================
for tag, cx in zip("abcd", TUBE_XS):
    # 尾段（箱体之后、卡箍之内）
    octagon(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_REAR, BOX_Z0 + 0.05,
            0.70, 0.95, "tube")
    octagon(f"tube_tail_{tag}", cx, TUBE_Y, TUBE_REAR - 0.28, TUBE_REAR,
            0.56, 0.76, "hull_dark")
    # 管口段：一圈圈环向接缝（原画最明显的节奏）
    octagon(f"tube_front_{tag}", cx, TUBE_Y, BOX_Z1 - 0.05, TUBE_FRONT,
            0.70, 0.95, "tube")
    for i, z in enumerate((9.60, 11.80, 14.00, 16.20)):
        octagon(f"tube_seam_{tag}_{i}", cx, TUBE_Y, z, z + 0.24,
                0.74, 1.00, "ring")
    # 管口：亮管箍 + 端盖（空膛态换成近黑内膛）
    octagon(f"tube_muzzle_{tag}", cx, TUBE_Y, TUBE_FRONT - 0.65, TUBE_FRONT,
            0.74, 1.02, "ring")
    octagon(f"tube_cap_{tag}", cx, TUBE_Y, TUBE_FRONT, TUBE_FRONT + 0.35,
            0.70, 0.95, "cap")

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
    "parent": "piranport:item/clt_quad_torpedo_launcher_type0_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("clt_quad_torpedo_launcher_type0_3d.json", base)
    dump("clt_quad_torpedo_launcher_type0_3d_loaded.json", loaded_3d)
    dump("clt_quad_torpedo_launcher_type0.json", item_entry)
