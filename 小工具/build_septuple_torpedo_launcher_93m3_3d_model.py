#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成七联鱼雷发射器（93M3）的 3D 手持模型（按 Equip_L_718 原画从零建模）。

只依据原画本身的形体：
  - 一具深蓝灰装甲发射箱：箱体是切角八棱多面体（方形体 + 四块 45° 旋转体填角），
    顶上是略微外挑的舱盖，盖面上有一圈铆点、一道浅色细带、一个小圆舱口和一个
    带把手的圆盖；正面左右两块切角面上各有一扇带观察窗的小舱门，左舷还有一块
    铰接检修门；
  - 七根鱼雷管从箱体正前方伸出，按 4+3 两排错位排列，每根带三道管箍，
    根部有一圈深色束带把两排箍在一起；
  - 箱体下方是矮底座。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 切角八棱体：方形体 A/B 两块 + 四个 45° 旋转小方块填角（并集恰好切掉四角），
    见 octa_layer；
  - 鱼雷管同样是方形 + 45° 旋转形叠成的八棱柱；
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
PREFIX = "septuple_torpedo_launcher_93m3_"
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

# 箱体：中心 x=8（模型中心），重心在 z≈4.4 的后半段，前面留给七根管
BOX_CX, BOX_CZ = 8.0, 4.4
BODY_D, BODY_C = 4.2, 1.4      # 箱身半宽与切角尺寸
LID_D, LID_C = 4.45, 1.5       # 舱盖（外挑一圈）
TOP_D, TOP_C = 3.95, 1.2       # 盖顶板
BODY_Y0, BODY_Y1 = 1.2, 7.0
LID_Y0, LID_Y1 = 7.0, 8.55
TOP_Y1 = 8.9                   # 盖顶板顶面

# 七根管：上排 4 根、下排 3 根，间距 1.75，管束总宽约 9 单位（x 4.5~11.5）
UPPER_XS = tuple(round(8.0 + (i - 1.5) * 1.75, 3) for i in range(4))
LOWER_XS = tuple(round(8.0 + (i - 1) * 1.75, 3) for i in range(3))
UPPER_Y, LOWER_Y = 5.95, 4.25
TUBE_Z0 = 6.0    # 管尾（藏进箱体里）
TUBE_Z1 = 23.0   # 管口
TUBE_IN = 0.66
TUBE_OUT = 0.88


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
    """沿 z 的八棱柱（鱼雷管/管箍/端盖）。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces, uv_faces=uv_faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces, uv_faces=uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def octa_layer(name, cx, cz, d, c, y1, y2, tex, faces=None):
    """切角八棱体的一层：两块方形体 + 四个 45° 旋转方块填掉四个角。

    WHY 这么做：模型 JSON 里盒子只能相加（并集），没法"切角"。把
      横带 A（|z|<=d-c，x 到 ±d）、竖带 B（|x|<=d-c，z 到 ±d）
    拼起来正好差四个 c×c 的角，再用四块半边长 c/√2、中心在 (d-c, d-c) 的
    45° 方块把角补齐 —— 它们的斜边恰好落在 x+z=2d-c 这条切角线上。
    三块体的顶/底面互相错开 0.02，避免共面打架。
    """
    box(f"{name}_a", cx - d, y1, cz - (d - c), cx + d, y2, cz + (d - c),
        tex, faces=faces)
    box(f"{name}_b", cx - (d - c), y1 + 0.02, cz - d, cx + (d - c), y2 - 0.02,
        cz + d, tex, faces=faces)
    half = c / math.sqrt(2.0)
    for i, (sx, sz) in enumerate(((1, 1), (-1, 1), (1, -1), (-1, -1))):
        ox = cx + sx * (d - c)
        oz = cz + sz * (d - c)
        box(f"{name}_corner_{i}", ox - half, y1 + 0.04, oz - half,
            ox + half, y2 - 0.04, oz + half, tex, faces=faces,
            rotation={"angle": 45, "axis": "y",
                      "origin": [ox, (y1 + y2) / 2, oz]})


def disc_y(name, cx, cy, cz, radius, height, tex, faces=None):
    """平放在顶面上的小圆盘（铆点、小舱口）：八棱薄片。"""
    box(f"{name}_box", cx - radius * 0.72, cy, cz - radius * 0.72,
        cx + radius * 0.72, cy + height, cz + radius * 0.72, tex, faces=faces)
    half = radius / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy + 0.004, cz - half,
        cx + half, cy + height - 0.004, cz + half, tex, faces=faces,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, cy + height / 2, cz]})


def panel_on_chamfer(name, cx, cz, length, thick, y1, y2, angle, tex,
                     faces=None):
    """贴在箱体切角面上的薄板（45° 舱门）：绕自身中心绕 y 转 angle。"""
    half_l = length / 2.0
    box(f"{name}_box", cx - half_l, y1, cz - thick, cx + half_l, y2, cz + thick,
        tex, faces=faces,
        rotation={"angle": angle, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


# ================= 矮底座 =================
box("base_plinth", 4.9, 0.0, 1.5, 11.1, 0.55, 7.5, "hull_dark")
box("base_step", 5.5, 0.55, 2.1, 10.5, 1.2, 6.9, "hull_dark")

# ================= 箱体分层 =================
# 箱身正面用亮一档的 hull，侧面/后面压成 hull_dark（原画左侧面明显更暗）
BODY_FACES = {"south": "hull", "east": "hull_dark", "west": "hull_dark",
              "north": "hull_dark", "up": "hull_dark", "down": "hull_dark"}
octa_layer("body", BOX_CX, BOX_CZ, BODY_D, BODY_C, BODY_Y0, BODY_Y1,
           "hull_dark", faces=BODY_FACES)
# 舱盖：外挑一圈，四周都是亮面，形成原画那种"盖沿"
octa_layer("lid", BOX_CX, BOX_CZ, LID_D, LID_C, LID_Y0, LID_Y1, "hull")
# 盖顶板：比盖沿再收一点，做出顶面的一级台阶
octa_layer("top_panel", BOX_CX, BOX_CZ, TOP_D, TOP_C, LID_Y1, TOP_Y1, "hull")

# ================= 盖顶细节 =================
# 一圈铆点（都在盖顶板的实体内，别落到被切掉的角外）
for i, (rx, rz) in enumerate(((5.8, 2.2), (8.0, 1.8), (10.2, 2.2),
                              (5.8, 6.6), (10.2, 6.6), (8.0, 7.0))):
    disc_y(f"rivet_{i}", rx, TOP_Y1, rz, 0.26, 0.14, "detail")
# 斜贯盖面的浅色细带（原画那条铜色装饰线，这里用亮金属表现）
box("top_stripe", 4.6, TOP_Y1, 3.3, 11.4, TOP_Y1 + 0.06, 3.6, "detail")
# 小圆舱口 + 十字把手
disc_y("top_hatch", 6.85, TOP_Y1, 5.55, 0.52, 0.16, "ring")
box("top_hatch_bar_x", 6.3, TOP_Y1 + 0.16, 5.48, 7.4, TOP_Y1 + 0.3, 5.62,
    "detail")
box("top_hatch_bar_z", 6.78, TOP_Y1 + 0.16, 5.05, 6.92, TOP_Y1 + 0.3, 6.05,
    "detail")
# 带把手的圆盖
disc_y("top_cap", 9.5, TOP_Y1, 4.6, 0.6, 0.16, "ring")
box("top_cap_handle_a", 9.15, TOP_Y1 + 0.16, 4.42, 9.42, TOP_Y1 + 0.42, 4.78,
    "detail")
box("top_cap_handle_b", 9.15, TOP_Y1 + 0.32, 4.42, 9.86, TOP_Y1 + 0.44, 4.78,
    "detail")

# ================= 正面切角上的舱门 =================
# 正前方被七根管占满，原画里那两扇带观察窗的小舱门落在左右切角面上。
for tag, sx, angle in (("l", -1, -45), ("r", 1, 45)):
    px = BOX_CX + sx * (BODY_D - BODY_C / 2.0)
    pz = BOX_CZ + (BODY_D - BODY_C / 2.0)
    panel_on_chamfer(f"chamfer_door_{tag}", px, pz, 1.55, 0.16, 3.4, 5.7,
                     angle, "detail")
    # 观察窗：沿斜舱门面方向错开两小块亮面，厚度比舱门多 0.05 好浮在门面上
    ux, uz = sx / math.sqrt(2.0), -1 / math.sqrt(2.0)
    for j, t in (("a", -0.42), ("b", 0.42)):
        panel_on_chamfer(f"chamfer_window_{tag}_{j}",
                         px + t * ux, pz + t * uz, 0.4, 0.21, 4.1, 4.7,
                         angle, "cap_loaded")

# ================= 侧面细节 =================
# 左舷铰接检修门（原画左侧那块带合页的大舱门）
X_LEFT = BOX_CX - BODY_D             # 左侧板（x=3.8）
box("side_door", X_LEFT - 0.2, 2.5, 2.9, X_LEFT, 5.6, 6.3, "detail")
for i, z in enumerate((3.35, 5.85)):
    box(f"side_hinge_{i}", X_LEFT - 0.26, 3.0, z, X_LEFT - 0.2, 3.5,
        z + 0.45, "detail")
box("side_door_handle", X_LEFT - 0.32, 4.0, 5.3, X_LEFT - 0.2, 4.5, 5.55,
    "detail")
# 右舷两块加强板
X_RIGHT = BOX_CX + BODY_D            # 右侧板（x=12.2）
for i, z in enumerate((2.6, 5.4)):
    box(f"side_plate_{i}", X_RIGHT, 2.8, z, X_RIGHT + 0.18, 5.2, z + 1.6,
        "detail")

# ================= 七根鱼雷管 =================
TUBES = [(cx, UPPER_Y, f"u{i}") for i, cx in enumerate(UPPER_XS)] + \
        [(cx, LOWER_Y, f"l{i}") for i, cx in enumerate(LOWER_XS)]
for cx, cy, tag in TUBES:
    octagon_z(f"tube_{tag}", cx, cy, TUBE_Z0, TUBE_Z1, TUBE_IN, TUBE_OUT,
              "tube", uv_faces=TUBE_UV)
    octagon_z(f"tube_rear_{tag}", cx, cy, TUBE_Z0 - 0.15, TUBE_Z0 + 0.05,
              0.64, 0.84, "hull_dark")
    for k, z in enumerate((11.6, 16.2, 20.4)):
        octagon_z(f"tube_ring_{tag}_{k}", cx, cy, z, z + 0.30,
                  0.70, 0.97, "ring")
    octagon_z(f"muzzle_{tag}", cx, cy, TUBE_Z1 - 0.5, TUBE_Z1,
              0.70, 0.94, "ring")
    # 端盖正面取满整张贴图：浅色管口圈画在贴图四周，只取中间一小块会把圈裁掉
    octagon_z(f"cap_{tag}", cx, cy, TUBE_Z1, TUBE_Z1 + 0.28,
              TUBE_IN, TUBE_OUT, "cap",
              uv_faces={"south": [0, 0, 16, 16]})

# 上排 4 根管顶的小卡榫（原画管身上零星的凸起）
for i, cx in enumerate(UPPER_XS):
    for k, z in enumerate((13.4, 18.6)):
        box(f"lug_u{i}_{k}", cx - 0.22, UPPER_Y + TUBE_OUT - 0.12, z,
            cx + 0.22, UPPER_Y + TUBE_OUT + 0.22, z + 0.66, "detail")

# 管束根部的束带（把 4+3 两排从箱体里伸出的部分箍成一组）
box("tube_clamp", 4.75, 3.35, 9.0, 11.25, 6.85, 9.6, "hull_dark")

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
    "parent": "piranport:item/septuple_torpedo_launcher_93m3_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("septuple_torpedo_launcher_93m3_3d.json", base)
    dump("septuple_torpedo_launcher_93m3_3d_loaded.json", loaded_3d)
    dump("septuple_torpedo_launcher_93m3.json", item_entry)
