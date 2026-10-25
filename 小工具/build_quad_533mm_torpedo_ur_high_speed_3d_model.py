#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成四联533毫米鱼雷（ur高速）的 3D 手持模型（按 Equip_L_668 原画从零建模）。

只依据原画本身的形体，不套用基础型的布局：
  - 四根亮银灰鱼雷管平铺，管口平整（没有外露的红色雷头）；
  - 每根管尾上方有一排机械卡榫/止动块；
  - 管束中部偏前是一整块方形炮架，炮架顶部有凸起的控制箱，
    前下方还有一级矮台阶（原画最醒目的识别点）；
  - 炮架后方的远侧管顶上有一个圆形绞盘/转盘；
  - 没有独立的圆鼓底座，炮架本身就是承力结构。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆管用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱，读作圆柱；
  - 装填/空膛的差异只在管口端盖：装填态亮金属封盖，空膛态近黑管口；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/quad_533mm_torpedo_ur_high_speed_3d.json         空膛 3D 发射器
  models/item/quad_533mm_torpedo_ur_high_speed_3d_loaded.json  装填 3D 发射器
（物品入口 quad_533mm_torpedo_ur_high_speed.json 已是 builtin/entity，沿用）
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "quad_533mm_torpedo_ur_high_speed_"
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

TUBE_XS = (3.95, 6.65, 9.35, 12.05)
TUBE_Y = 3.1
TUBE_Z0 = 2.6      # 管尾
TUBE_Z1 = 24.0     # 管口


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

    z 两端各错开 0.002，避免两个方盒的端面共面 z-fighting。
    """
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


# ================= 四根亮银灰鱼雷管 =================
for tag, cx in zip("abcd", TUBE_XS):
    octagon(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 0.82, 1.18, "tube")
    # 管尾封板
    octagon(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.15, TUBE_Z0 + 0.05,
            0.80, 1.16, "hull_dark")
    # 管身中后段一道细接缝
    octagon(f"tube_seam_{tag}", cx, TUBE_Y, 20.4, 20.65, 0.84, 1.20, "hull_dark")
    # 管口：两段收圆的亮金属封盖（空膛态换成近黑管口）
    octagon(f"cap_base_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.45,
            0.82, 1.18, "cap")
    octagon(f"cap_tip_{tag}", cx, TUBE_Y, TUBE_Z1 + 0.45, TUBE_Z1 + 0.75,
            0.64, 0.92, "cap")
    # 管尾上方一排机械卡榫（原画识别点）
    for i, z in enumerate((4.2, 5.5, 6.8)):
        box(f"stop_{tag}_{i}", cx - 0.45, 4.0, z, cx + 0.45, 4.4, z + 0.6,
            "detail")
        offset = -0.15 if i % 2 == 0 else 0.15
        box(f"stop_cap_{tag}_{i}", cx - 0.25 + offset, 4.4, z + 0.1,
            cx + 0.25 + offset, 4.75, z + 0.5, "detail")

# ================= 方形炮架（中部偏前） =================
box("mount_main", 2.7, 1.4, 13.5, 13.3, 5.0, 19.5, "hull")
box("mount_top_rim", 2.7, 5.0, 13.5, 13.3, 5.25, 19.5, "ring")
box("mount_rear_plate", 2.9, 1.55, 13.25, 13.1, 4.85, 13.5, "hull_dark")
box("mount_front_plate", 2.9, 1.55, 19.5, 13.1, 4.85, 19.75, "hull_dark")
box("mount_side_port", 2.45, 2.9, 15.2, 2.75, 3.5, 18.2, "detail")
box("mount_side_stbd", 13.25, 2.9, 15.2, 13.55, 3.5, 18.2, "detail")

# 炮架顶部的凸起控制箱
box("control_box", 4.3, 5.25, 13.8, 11.7, 6.9, 17.6, "hull")
box("control_face", 4.4, 5.35, 17.6, 11.6, 6.8, 17.85, "hull_dark")
box("control_top", 4.2, 6.9, 13.7, 11.8, 7.15, 17.7, "ring")

# 前下方的矮台阶
box("front_step", 4.3, 1.0, 19.75, 11.7, 2.9, 21.2, "hull")
box("front_step_face", 4.4, 1.1, 21.2, 11.6, 2.8, 21.4, "hull_dark")

# 炮架后方远侧管顶的圆形绞盘
octagon("capstan_base", 12.05, 4.55, 11.9, 12.55, 0.62, 0.75, "ring")
octagon("capstan_top", 12.05, 4.55, 12.55, 12.75, 0.40, 0.52, "hull_dark")

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
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

loaded_3d = {
    "parent": "piranport:item/quad_533mm_torpedo_ur_high_speed_3d",
    "textures": {
        "cap": TEX["cap_loaded"],
    },
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("quad_533mm_torpedo_ur_high_speed_3d.json", base)
    dump("quad_533mm_torpedo_ur_high_speed_3d_loaded.json", loaded_3d)
