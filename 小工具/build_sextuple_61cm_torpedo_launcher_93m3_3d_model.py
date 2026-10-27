#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成六连61厘米鱼雷发射器（93M3）的 3D 手持模型（按 Equip_L_678 原画从零建模）。

只依据原画本身的形体：
  - 六根等长裸鱼雷管并排，管口朝前，管身每根三道细箍、箍间还有小卡榫；
  - 管束上方从管尾到中段各有一根细导轨（原画里管顶那条细黑线）；
  - 管束下方是一块浅色托板 + 深色机械台面，台面上有两部辐条手轮、一个操纵箱
    和一部竖直摇柄；托板底下是圆形座板 + 圆鼓底座（裸管发射器的典型座架）。

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
PREFIX = "sextuple_61cm_torpedo_launcher_93m3_"
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

# 六根管：间距 1.75，总宽 6*1.75 + 管径 1.76 ≈ 12.3 单位，画布宽度只用到 2.7~13.3
TUBE_XS = tuple(round(8.0 + (i - 2.5) * 1.75, 3) for i in range(6))
TUBE_Y = 4.6
TUBE_Z0 = 3.0    # 管尾
TUBE_Z1 = 24.5   # 管口
TUBE_IN = 0.62   # 轴对齐方盒半宽
TUBE_OUT = 0.88  # 外接圆半径


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
        full_uv_faces=(), uv_faces=None):
    faces_json = {}
    seed = len(elements) + 1
    for face in ("north", "south", "west", "east", "up", "down"):
        face_tex = (faces or {}).get(face, tex)
        entry = {"texture": "#" + face_tex}
        if uv_faces and face in uv_faces:
            entry["uv"] = list(uv_faces[face])
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


# 圆管逐面 uv：管身长边取满 [0,16] 走完整个明暗渐变，顶/底面各取一行单色。
# WHY：管的长侧面 u 是管长、v 是管径，只有 v 走满渐变才像圆柱；
# 而顶面 u 是管径、v 是管长，若也取满就会顺着管长出现一条渐变亮带。
TUBE_UV = {
    "east": [0, 0, 16, 16], "west": [0, 0, 16, 16],
    "up": [0, 0, 1, 1], "down": [0, 15, 1, 16],
    "north": [0, 0, 1.2, 1.2], "south": [0, 0, 1.2, 1.2],
}


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex, faces=None,
              uv_faces=None):
    """沿 z 的八棱柱（圆管/管箍/端盖都走这里）。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces, uv_faces=uv_faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces, uv_faces=uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, outer, tex):
    """沿 y 的八棱柱（圆鼓底座）。"""
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """多层收缩的方形叠出近似圆形的座板。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


def handwheel(name, cx, cy, cz, radius, tex="detail"):
    """平放的辐条手轮：八棱轮缘 + 两根交叉辐条 + 轮毂。"""
    box(f"{name}_rim_box", cx - radius * 0.72, cy, cz - radius * 0.72,
        cx + radius * 0.72, cy + 0.22, cz + radius * 0.72, tex)
    half = radius / math.sqrt(2.0)
    box(f"{name}_rim_dia", cx - half, cy + 0.004, cz - half,
        cx + half, cy + 0.218, cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, cy + 0.11, cz]})
    # 辐条必须短于轮缘：超出轮缘会画成十字架而不是手轮
    box(f"{name}_spoke_x", cx - radius * 0.64, cy + 0.22, cz - 0.06,
        cx + radius * 0.64, cy + 0.3, cz + 0.06, tex)
    box(f"{name}_spoke_z", cx - 0.06, cy + 0.22, cz - radius * 0.64,
        cx + 0.06, cy + 0.3, cz + radius * 0.64, tex)
    box(f"{name}_hub", cx - 0.18, cy + 0.22, cz - 0.18,
        cx + 0.18, cy + 0.38, cz + 0.18, tex)


# ================= 底座 =================
round_footprint("base_plate", 8.0, 12.0, 3.1, 0.0, 0.35, "hull_dark")
cylinder_y("base_drum", 8.0, 12.0, 0.35, 1.7, 1.3, 2.0, "hull_dark")

# ================= 托板与机械台面 =================
# 托板比管束宽出一截（原画里台面就是从管束两侧露出来的），
# 两侧露出的窄台正好放手轮，避免手轮整个藏在管束投影底下从任何角度都看不见。
box("tray", 1.2, 1.7, 8.2, 14.8, 2.35, 17.0, "hull")
box("tray_lip_front", 1.2, 2.35, 17.0, 14.8, 2.78, 17.3, "hull")
box("tray_lip_rear", 1.2, 2.35, 7.9, 14.8, 2.78, 8.2, "hull")
box("tray_lip_left", 1.2, 2.35, 8.2, 1.55, 2.78, 17.0, "hull")
box("tray_lip_right", 14.45, 2.35, 8.2, 14.8, 2.78, 17.0, "hull")
box("deck", 1.65, 2.35, 8.65, 14.35, 3.0, 16.55, "hull_dark")
# 管束下方的托架：填住台面与管底之间的缝，让六根管看着有支撑
box("cradle", 3.0, 3.0, 9.2, 13.0, 3.72, 16.0, "hull_dark")

# 操纵箱（藏在管束下方，只从正面低角度能看到，作为台面的机械体积感）
box("control_box", 6.9, 3.0, 15.2, 8.4, 3.62, 16.45, "detail")
box("control_face", 7.1, 3.12, 16.45, 8.2, 3.5, 16.57, "hull_dark")

# 两部辐条手轮（原画：一对高低不同的手轮，这里左右各一，转炮/俯仰各一）
handwheel("wheel_a", 2.35, 3.0, 12.5, 0.72)
handwheel("wheel_b", 13.65, 3.0, 12.5, 0.72)

# 右侧台面边缘的摇柄（原画台面前端那根立起来的把手）
box("crank_post", 13.35, 3.0, 15.9, 13.62, 3.95, 16.17, "detail")
box("crank_grip", 13.05, 3.9, 15.78, 13.92, 4.12, 15.98, "detail")

# ================= 六根鱼雷管 =================
for i, cx in enumerate(TUBE_XS):
    tag = "abcdef"[i]
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, TUBE_IN, TUBE_OUT,
              "tube", uv_faces=TUBE_UV)
    octagon_z(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.18, TUBE_Z0 + 0.05,
              0.60, 0.84, "hull_dark")
    # 三道细箍（原画管身每隔一段就有一道亮箍）
    for k, z in enumerate((9.6, 16.4, 21.4)):
        octagon_z(f"tube_ring_{tag}_{k}", cx, TUBE_Y, z, z + 0.30,
                  0.66, 0.96, "ring")
    # 管口一圈加厚 + 端盖（空膛/装填只换端盖贴图）
    octagon_z(f"muzzle_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.5, TUBE_Z1,
              0.66, 0.94, "ring")
    # 端盖正面取满整张贴图：空膛贴图的浅色管口圈画在贴图四周，
    # 只取中间一小块会把圈裁掉，六根管口就成了一片死黑。
    octagon_z(f"cap_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.28,
              TUBE_IN, TUBE_OUT, "cap",
              uv_faces={"south": [0, 0, 16, 16]})
    # 管顶小卡榫（原画里箍与箍之间的凸起）
    for k, z in enumerate((11.6, 18.4)):
        box(f"lug_{tag}_{k}", cx - 0.24, TUBE_Y + TUBE_OUT - 0.12, z,
            cx + 0.24, TUBE_Y + TUBE_OUT + 0.26, z + 0.72, "detail")
    # 管顶细导轨（原画管身上那条细线）
    box(f"rail_{tag}", cx + 0.16, TUBE_Y + TUBE_OUT - 0.05, TUBE_Z0 + 1.4,
        cx + 0.34, TUBE_Y + TUBE_OUT + 0.12, 20.6, "detail")

# 管尾上方的横向卡榫轨（把六根管连成一组）
box("rear_clamp", TUBE_XS[0] - 0.5, TUBE_Y + TUBE_OUT - 0.18, 4.6,
    TUBE_XS[-1] + 0.5, TUBE_Y + TUBE_OUT + 0.22, 5.05, "detail")
for i, cx in enumerate(TUBE_XS):
    box(f"rear_clamp_tab_{i}", cx - 0.22, TUBE_Y + TUBE_OUT + 0.22, 4.7,
        cx + 0.22, TUBE_Y + TUBE_OUT + 0.52, 5.0, "detail")

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
    "parent": "piranport:item/sextuple_61cm_torpedo_launcher_93m3_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("sextuple_61cm_torpedo_launcher_93m3_3d.json", base)
    dump("sextuple_61cm_torpedo_launcher_93m3_3d_loaded.json", loaded_3d)
    dump("sextuple_61cm_torpedo_launcher_93m3.json", item_entry)
