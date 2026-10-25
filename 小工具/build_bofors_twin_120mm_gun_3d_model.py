#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「博福斯双联120毫米炮」的 3D 手持模型（按 Equip_L_455 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 半敞开式炮房：左右侧壁 + 后壁 + 顶盖，正面完全敞开，露出暗橄榄色内舱；
  - 内舱里两只炮闩与炮座，炮管从开口伸到舱外；
  - 圆脊顶盖比侧壁略亮，前缘压一条深色檐口，顶上还有一个小的瞄准/通风盒；
  - 两舷有黑色栏杆与前方弹架，右舷一块带把手的检修盖板；
  - 座圈是蓝灰矮圆鼓，底部一圈近黑法兰。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆；
  - 炮管用绕 Z 轴 45° 的菱形截面，低分辨率下读作圆管；
  - 模型坐标以 (8, 7.8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/bofors_twin_120mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/bofors_twin_120mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/bofors_twin_120mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "bofors_twin_120mm_gun_hull",
    "roof": NS + "bofors_twin_120mm_gun_roof",
    "hull_dark": NS + "bofors_twin_120mm_gun_hull_dark",
    "detail": NS + "bofors_twin_120mm_gun_detail",
    "interior": NS + "bofors_twin_120mm_gun_interior",
    "breech": NS + "bofors_twin_120mm_gun_breech",
    "dark": NS + "bofors_twin_120mm_gun_dark",
    "barrel": NS + "bofors_twin_120mm_gun_barrel",
    "muzzle_empty": NS + "bofors_twin_120mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "bofors_twin_120mm_gun_muzzle_loaded",
}

CX, CZ = 8.0, 7.8
elements = []


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面尺寸给 uv 窗口（uv 单位=模型单位；贴图 32px 时 16uv=整图）。"""
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    max_u = max(0.0, 16.0 - w)
    max_v = max(0.0, 16.0 - h)
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


def diamond(name, cx, cy, z1, z2, half, tex, faces=None, full_uv_faces=()):
    """绕 Z 轴转 45° 的菱形截面（炮管 / 手轮）。"""
    box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
        faces=faces, full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z",
                  "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 座圈：蓝灰矮圆鼓 + 近黑法兰 =================
round_footprint("base_flange", CX, CZ, 4.35, 0.0, 0.35, "dark")
round_footprint("base_drum", CX, CZ, 4.05, 0.35, 0.95, "hull_dark")
round_footprint("base_lip", CX, CZ, 4.15, 0.95, 1.10, "detail")

# ================= 半敞开炮房：地板 / 侧壁 / 后壁 =================
box("floor", 3.95, 1.10, 4.10, 12.05, 1.45, 12.00, "interior")
box("wall_left", 3.50, 1.10, 3.60, 4.20, 6.00, 12.00, "hull")
box("wall_right", 11.80, 1.10, 3.60, 12.50, 6.00, 12.00, "hull")
box("wall_rear", 4.20, 1.10, 3.60, 11.80, 6.00, 4.30, "hull")

# 侧壁顶沿：浅色压条
box("cap_left", 3.45, 6.00, 3.55, 4.25, 6.15, 12.05, "detail")
box("cap_right", 11.75, 6.00, 3.55, 12.55, 6.15, 12.05, "detail")
box("cap_rear", 4.20, 6.00, 3.55, 11.80, 6.15, 4.35, "detail")

# 正面开口的边框：左右竖梃 + 下沿
box("front_lip_left", 3.50, 1.45, 11.90, 4.20, 6.00, 12.05, "hull_dark")
box("front_lip_right", 11.80, 1.45, 11.90, 12.50, 6.00, 12.05, "hull_dark")
box("front_lip_bottom", 4.20, 1.10, 11.90, 11.80, 1.45, 12.05, "hull_dark")

# ================= 内舱：暗橄榄色背板 / 侧板 / 顶板 =================
box("interior_back", 4.20, 1.45, 4.30, 11.80, 5.85, 4.50, "interior")
box("interior_left", 4.20, 1.45, 4.50, 4.32, 5.85, 11.90, "interior")
box("interior_right", 11.68, 1.45, 4.50, 11.80, 5.85, 11.90, "interior")
box("interior_ceiling", 4.20, 5.70, 4.50, 11.80, 5.85, 9.80, "interior")

# ================= 两只炮闩与炮座 =================
for tag, x1, x2 in (("l", 6.55, 7.25), ("r", 8.75, 9.45)):
    box(f"breech_{tag}", x1, 2.80, 6.00, x2, 4.35, 7.80, "breech")
    box(f"breech_top_{tag}", x1 + 0.05, 4.35, 6.10, x2 - 0.05, 4.44, 7.70, "detail")
box("cradle", 6.30, 4.10, 8.20, 9.70, 4.60, 10.30, "hull_dark")
box("trunnion_left", 6.10, 4.25, 8.60, 6.40, 4.65, 9.40, "hull_dark")
box("trunnion_right", 9.60, 4.25, 8.60, 9.90, 4.65, 9.40, "hull_dark")

# ================= 圆脊顶盖 =================
box("roof_lower", 3.40, 6.00, 3.60, 12.60, 6.25, 9.80, "roof")
box("roof_upper", 3.80, 6.25, 3.80, 12.20, 6.50, 9.70, "roof")
box("roof_top", 4.60, 6.50, 4.00, 11.40, 6.63, 9.60, "roof")
# 两舷圆肩：把平顶的两侧收成斜面，读作原画的圆弧顶盖
box("roof_slope_left", 3.25, 5.85, 3.65, 3.75, 6.45, 9.75, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.50, 6.15, 6.70]})
box("roof_slope_right", 12.25, 5.85, 3.65, 12.75, 6.45, 9.75, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.50, 6.15, 6.70]})
box("roof_front_lip", 3.40, 5.95, 9.70, 12.60, 6.30, 9.92, "hull_dark")
box("roof_rear_overhang", 3.50, 5.90, 3.35, 12.50, 6.25, 3.70, "roof")
# 顶盖后部的瞄准 / 通风盒
box("roof_box", 6.80, 6.50, 4.80, 9.20, 7.00, 6.40, "hull")
box("roof_box_top", 6.90, 7.00, 4.90, 9.10, 7.10, 6.30, "roof")

# ================= 两舷检修盖板 =================
box("hatch_right", 12.50, 3.60, 6.60, 12.62, 4.60, 7.80, "detail")
box("hatch_handle_right", 12.62, 4.00, 6.85, 12.68, 4.15, 7.05, "dark")
box("hatch_left", 3.38, 3.60, 6.60, 3.50, 4.60, 7.80, "detail")
box("hatch_handle_left", 3.32, 4.00, 6.85, 3.38, 4.15, 7.05, "dark")

# ================= 黑色栏杆与前方弹架 =================
for tag, x1, x2 in (("r", 12.50, 12.64), ("l", 3.36, 3.50)):
    # 后段上下两道横杆
    for y in (5.30, 5.75):
        box(f"rail_{tag}_{int(y * 100)}", x1, y, 4.20, x2, y + 0.10, 9.00, "dark")
    for z in (4.40, 6.20, 8.00, 8.90):
        box(f"rail_post_{tag}_{int(z * 10)}", x1, 4.90, z - 0.06, x2, 5.90, z + 0.06, "dark")
    # 前方弹架：三道横杆 + 立柱
    for y in (3.20, 3.75, 4.30):
        box(f"rack_{tag}_{int(y * 100)}", x1, y, 9.40, x2, y + 0.10, 11.60, "dark")
    for z in (9.50, 10.50, 11.50):
        box(f"rack_post_{tag}_{int(z * 10)}", x1, 3.10, z - 0.06, x2, 4.60, z + 0.06, "dark")

# ================= 正面开口下角的手轮 =================
for tag, cx in (("l", 4.55), ("r", 11.45)):
    diamond(f"handwheel_{tag}", cx, 2.35, 12.05, 12.12, 0.42, "detail")
    diamond(f"handwheel_hub_{tag}", cx, 2.35, 12.12, 12.17, 0.22, "dark")

# ================= 两门炮管 =================
BARREL_Y = 4.50
for tag, bx in (("l", 6.90), ("r", 9.10)):
    diamond(f"barrel_sleeve_{tag}", bx, BARREL_Y, 6.00, 8.20, 0.45, "hull_dark")
    diamond(f"barrel_step_{tag}", bx, BARREL_Y, 8.20, 9.00, 0.34, "barrel")
    diamond(f"barrel_{tag}", bx, BARREL_Y, 9.00, 23.60, 0.24, "barrel")
    diamond(f"barrel_ring_a_{tag}", bx, BARREL_Y, 11.50, 11.90, 0.30, "barrel")
    diamond(f"barrel_ring_b_{tag}", bx, BARREL_Y, 15.50, 15.90, 0.30, "barrel")
    diamond(f"muzzle_bell_{tag}", bx, BARREL_Y, 23.20, 24.00, 0.40, "barrel")
    # 炮口内膛：装填/空膛差异只在这两个小面
    diamond(f"muzzle_{tag}", bx, BARREL_Y, 24.00, 24.10, 0.26, "barrel",
            faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    # 炮管顶部的瞄准肋条
    box(f"barrel_rib_{tag}", bx - 0.06, BARREL_Y + 0.26, 8.60,
        bx + 0.06, BARREL_Y + 0.33, 14.00, "detail")

# 手持时绕 Y 转 180°，让炮口朝角色前方
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.65, 0.65, 0.65],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.65, 0.65, 0.65],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.6, 0.6, 0.6],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.6, 0.6, 0.6],
    },
    "ground": {
        "rotation": [0, 0, 0],
        "translation": [0, 2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "fixed": {
        "rotation": [0, 180, 0],
        "scale": [1, 1, 1],
    },
    "head": {
        "rotation": [0, 180, 0],
        "translation": [0, 13, 7],
        "scale": [1, 1, 1],
    },
}

base = {
    "parent": "minecraft:block/block",
    "gui_light": "front",
    "ambientocclusion": False,
    "textures": {
        "particle": TEX["hull"],
        "hull": TEX["hull"],
        "roof": TEX["roof"],
        "hull_dark": TEX["hull_dark"],
        "detail": TEX["detail"],
        "interior": TEX["interior"],
        "breech": TEX["breech"],
        "dark": TEX["dark"],
        "barrel": TEX["barrel"],
        "muzzle_empty": TEX["muzzle_empty"],
        "muzzle_loaded": TEX["muzzle_loaded"],
    },
    "elements": elements,
}

item_entry = {
    "parent": "builtin/entity",
    "gui_light": "front",
    "display": DISPLAY,
}

loaded_3d = {
    "parent": "piranport:item/bofors_twin_120mm_gun_3d",
    "textures": {
        "muzzle_empty": TEX["muzzle_loaded"],
    },
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("bofors_twin_120mm_gun_3d.json", base)
    dump("bofors_twin_120mm_gun_3d_loaded.json", loaded_3d)
    dump("bofors_twin_120mm_gun.json", item_entry)
