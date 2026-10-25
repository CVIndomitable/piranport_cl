#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成五联533毫米鱼雷的 3D 手持模型（按 Equip_L_89 原画从零建模）。

只依据原画本身的形体，不套用四联型的布局：
  - 五根钢蓝灰鱼雷管平铺，前后端都是平整管口；
  - 管束中后部立着一个巨大的锥顶圆鼓（原画最醒目的识别点），鼓身有多道加强箍；
  - 管束左半段每根管顶有一排竖直小导板；
  - 下方是宽大的圆形座板 + 圆鼓底座 + 托架；
  - 前侧偏右有一组小型瞄准具，镜筒朝前伸出。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆管/圆鼓用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 圆鼓锥顶用 3 层递减半径的八棱柱拼出；
  - 装填/空膛的差异只在管口端盖：装填态金属封盖，空膛态近黑管口；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/quintuple_533mm_torpedo.json               builtin/entity 物品入口（只放 display）
  models/item/quintuple_533mm_torpedo_3d.json            空膛 3D 发射器
  models/item/quintuple_533mm_torpedo_3d_loaded.json     装填 3D 发射器
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "quintuple_533mm_torpedo_"
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
TUBE_Y = 4.6
TUBE_Z0 = 1.5      # 管尾
TUBE_Z1 = 25.0     # 管口


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


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex, faces=None):
    """沿 Z 的八棱柱（鱼雷管）：轴对齐方盒 + 45° 绕 Z 方盒。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, outer, tex):
    """沿 Y 的八棱柱（圆鼓/底座）：轴对齐方盒 + 45° 绕 Y 方盒。"""
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆（比 45° 方块并集干净，不会出尖角）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 底座：圆形座板 + 圆鼓 + 托架 =================
round_footprint("base_plate", 8.0, 15.5, 4.6, 0.0, 0.35, "hull_dark")
cylinder_y("base_drum", 8.0, 15.5, 0.35, 2.2, 2.0, 2.8, "hull_dark")
box("cradle", 2.8, 2.2, 8.0, 13.2, 4.0, 20.0, "hull_dark")

# ================= 五根鱼雷管 =================
for tag, cx in zip("abcde", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 0.72, 1.02, "tube")
    # 管尾封板 + 前后管箍
    octagon_z(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.15, TUBE_Z0 + 0.05,
              0.70, 1.00, "hull_dark")
    for i, z in enumerate((2.0, 22.6)):
        octagon_z(f"tube_ring_{tag}_{i}", cx, TUBE_Y, z, z + 0.30,
                  0.74, 1.06, "ring")
    # 管口：两段收圆的金属封盖（空膛态换成近黑管口）
    octagon_z(f"cap_base_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.45,
              0.72, 1.02, "cap")
    octagon_z(f"cap_tip_{tag}", cx, TUBE_Y, TUBE_Z1 + 0.45, TUBE_Z1 + 0.75,
              0.55, 0.78, "cap")
    # 管顶竖直小导板（原画左半段的一排小鳍）
    for i, z in enumerate((4.5, 6.5, 8.5, 10.5)):
        box(f"fin_{tag}_{i}", cx - 0.10, 5.5, z, cx + 0.10, 6.35, z + 0.18,
            "detail")

# ================= 锥顶圆鼓（原画识别点） =================
DRUM_Z = 16.0
cylinder_y("drum_body", 8.0, DRUM_Z, 5.0, 9.0, 2.4, 3.35, "hull")
for i, y in enumerate((5.9, 7.1, 8.3)):
    cylinder_y(f"drum_rib_{i}", 8.0, DRUM_Z, y, y + 0.35, 2.45, 3.45, "ring")
# 三层递减半径拼出锥顶，最后加小顶盖
cylinder_y("drum_cone_0", 8.0, DRUM_Z, 9.0, 9.6, 2.15, 3.0, "hull")
cylinder_y("drum_cone_1", 8.0, DRUM_Z, 9.6, 10.05, 1.65, 2.3, "hull")
cylinder_y("drum_cone_2", 8.0, DRUM_Z, 10.05, 10.45, 1.15, 1.6, "hull")
cylinder_y("drum_cap", 8.0, DRUM_Z, 10.45, 10.8, 0.65, 0.9, "hull")
cylinder_y("drum_knob", 8.0, DRUM_Z, 10.8, 10.95, 0.32, 0.45, "hull_dark")

# ================= 前侧瞄准具（镜筒朝前） =================
box("director_body", 9.6, 2.4, 19.0, 12.6, 4.6, 21.2, "hull_dark")
box("director_bracket", 10.7, 3.1, 18.6, 11.5, 3.9, 19.0, "hull")
octagon_z("director_scope", 11.1, 3.5, 21.2, 22.6, 0.40, 0.55, "detail")
octagon_z("director_scope_ring", 11.1, 3.5, 22.6, 22.8, 0.42, 0.60, "ring")

# ================= 近侧顶部的细瞄准杆 =================
box("sight_rod", 12.95, 6.0, 3.0, 13.07, 6.12, 21.5, "detail")
for i, z in enumerate((5.0, 10.0, 15.0, 20.0)):
    box(f"sight_rod_post_{i}", 12.95, 5.6, z, 13.07, 6.0, z + 0.12, "detail")

# 手持时绕 Y 转 180°，让管口朝角色前方
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.55, 0.55, 0.55],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.55, 0.55, 0.55],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.50, 0.50, 0.50],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.50, 0.50, 0.50],
    },
    # 掉落物/展示框/头戴走的是 2D 图标，沿用原版 item/generated 的变换
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
        "hull_dark": TEX["hull_dark"],
        "ring": TEX["ring"],
        "tube": TEX["tube"],
        "detail": TEX["detail"],
        "cap": TEX["cap"],
        "cap_loaded": TEX["cap_loaded"],
    },
    "elements": elements,
}

# 物品入口：builtin/entity 让原版把渲染交给 BEWLR
item_entry = {
    "parent": "builtin/entity",
    "gui_light": "front",
    "display": DISPLAY,
}

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
loaded_3d = {
    "parent": "piranport:item/quintuple_533mm_torpedo_3d",
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
    dump("quintuple_533mm_torpedo_3d.json", base)
    dump("quintuple_533mm_torpedo_3d_loaded.json", loaded_3d)
    dump("quintuple_533mm_torpedo.json", item_entry)
