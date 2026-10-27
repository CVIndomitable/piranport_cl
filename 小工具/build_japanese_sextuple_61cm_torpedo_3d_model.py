#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成日本六联61厘米鱼雷的 3D 手持模型（按 Equip_L_357 原画从零建模）。

只依据原画本身的形体：
  - 一座狭长的浅蓝灰装甲发射箱，长轴与鱼雷管平行（管从箱体两端伸出）；
  - 箱顶沿四周一圈细栏杆，顶面有两道纵向凸梁、三个圆形舱盖和两个矩形舱口；
  - 箱体侧面（+X 面）有一扇拱顶装甲门、一个方形舷窗、三排卡榫，
    底部还有一根长扶手；
  - 六根鱼雷管并排，管口朝 +Z：前端是深色管口 + 亮管箍，后端是收圆的尾盖；
  - 管束两端各有一组连接相邻管的横向卡箍，前端下方一圈 U 形护杆。

坐标约定：
  - 管口朝 +Z，模型中心 x=8；每个模型单位 2px，长面 uv 夹到 16；
  - 装填/空膛的差异只在管口端盖（cap 贴图）。

产出：
  models/item/japanese_sextuple_61cm_torpedo_3d.json         空膛 3D 模型
  models/item/japanese_sextuple_61cm_torpedo_3d_loaded.json  装填 3D 模型
  models/item/japanese_sextuple_61cm_torpedo.json            builtin/entity 物品入口
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "japanese_sextuple_61cm_torpedo_"
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

# 六根鱼雷管：中心 x=8，等距 1.76，管外径 1.32
TUBE_XS = (3.60, 5.36, 7.12, 8.88, 10.64, 12.40)
TUBE_Y = 3.00
BOX_X0, BOX_X1 = 2.20, 13.80   # 箱体横向范围
BOX_Z0, BOX_Z1 = 5.80, 17.20   # 箱体沿管轴范围
BOX_TOP = 5.55                 # 顶板下沿
RAIL_BASE = 5.95               # 栏杆立柱起点（顶面）
TUBE_REAR = -1.60              # 管尾
TUBE_FRONT = 24.00             # 管口


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

    鱼雷管、管箍都用它近似圆柱。z 两端各错开 0.002，避免端面 z-fighting。
    """
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, outer, tex):
    """水平圆盘（顶面朝上）：舱盖用。"""
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


# ================= 装甲箱体（低矮的装甲罩，不是高房子） =================
box("hull_skirt", 2.40, 0.55, 5.60, 13.60, 2.00, 17.40, "hull_dark")
box("hull_main", 2.20, 2.00, BOX_Z0, BOX_X1, 5.00, BOX_Z1, "hull")
box("hull_rim", 2.00, 5.00, 5.60, 14.00, BOX_TOP, 17.40, "hull_dark")
box("top_deck", 2.30, BOX_TOP, 5.90, 13.70, RAIL_BASE, 17.10, "hull")
box("top_panel", 2.90, RAIL_BASE, 6.50, 13.10, 6.10, 16.50, "hull")
box("keel", 2.60, 0.25, 6.20, 13.40, 0.57, 16.80, "detail")

# ================= 顶面细节 =================
# 两道纵向凸梁（原画顶面最显眼的两条细轨）
for i, x in enumerate((6.00, 9.40)):
    box(f"top_beam_{i}", x, RAIL_BASE, 7.00, x + 0.34, 6.22, 16.20, "detail")
# 三个圆形舱盖，沿 +X 侧边缘错开排列
for i, (x, z) in enumerate(((11.10, 6.70), (11.70, 8.40), (12.20, 10.10))):
    cylinder_y(f"top_hatch_{i}", x, z, RAIL_BASE, 6.16, 0.30, 0.44, "ring")
    box(f"top_hatch_pin_{i}", x - 0.10, 6.16, z - 0.10,
        x + 0.10, 6.22, z + 0.10, "hull_dark")
# 两个矩形舱口
for i, z in enumerate((11.90, 14.30)):
    box(f"top_lock_{i}", 10.40, RAIL_BASE, z, 11.70, 6.42, z + 1.10, "hull")
    box(f"top_lock_inner_{i}", 10.55, 6.42, z + 0.15,
        11.55, 6.48, z + 0.95, "hull_dark")

# ================= 顶面栏杆 =================
POSTS = []
for x in (2.35, 13.65):
    for z in (5.95, 8.60, 13.00, 17.05):
        POSTS.append((x, z))
for z in (5.95, 17.05):
    POSTS.append((8.00, z))
for i, (x, z) in enumerate(POSTS):
    box(f"rail_post_{i}", x - 0.08, RAIL_BASE, z - 0.08,
        x + 0.08, 7.00, z + 0.08, "ring")
# 两道水平栏杆（上下），沿四条边各一段
for i, (x1, x2) in enumerate(((2.35, 2.35), (13.65, 13.65))):
    for j, y in enumerate((6.40, 6.86)):
        box(f"rail_side_{i}_{j}", x1 - 0.07, y, 5.95, x2 + 0.07, y + 0.12,
            17.05, "ring")
for i, z in enumerate((5.95, 17.05)):
    for j, y in enumerate((6.40, 6.86)):
        box(f"rail_end_{i}_{j}", 2.35, y, z - 0.07, 13.65, y + 0.12,
            z + 0.07, "ring")

# ================= +X 侧面（装甲门 / 舷窗 / 卡榫） =================
# 门放在箱体前半段：原画里门与卡榫都在可见侧面，这里也避开管束遮挡。
FX = BOX_X1          # 侧面基准 x=13.8
box("door_back", FX - 0.02, 0.90, 9.40, FX + 0.18, 4.10, 12.20, "hull_dark")
box("door_panel", FX + 0.18, 1.05, 9.65, FX + 0.30, 3.60, 11.95, "ring")
box("door_arch_a", FX + 0.18, 3.60, 9.80, FX + 0.30, 3.85, 11.80, "ring")
box("door_arch_b", FX + 0.18, 3.85, 10.00, FX + 0.30, 4.02, 11.60, "ring")
box("door_handle", FX + 0.30, 2.00, 11.00, FX + 0.38, 2.80, 11.15,
    "detail")

# 方形舷窗（原画在门斜上方）
box("port_frame", FX - 0.02, 3.95, 7.60, FX + 0.20, 4.60, 8.30, "detail")
box("port_glass", FX + 0.20, 4.05, 7.70, FX + 0.26, 4.50, 8.20, "hull_dark")

# 三排卡榫：每排两只横向把手 + 内端锁扣
for row, y in enumerate((1.65, 2.35, 3.05)):
    for col, z in enumerate((12.85, 14.35)):
        box(f"latch_{row}_{col}", FX + 0.02, y, z, FX + 0.24, y + 0.16,
            z + 0.85, "ring")
        box(f"latch_knob_{row}_{col}", FX + 0.02, y - 0.08, z + 0.70,
            FX + 0.28, y + 0.24, z + 0.92, "detail")
# 底部一根长扶手 + 两端小立柱（装在更窄的裙板上）
box("side_rail", 13.62, 1.15, 6.20, 13.78, 1.30, 15.00, "ring")
for i, z in enumerate((6.20, 14.86)):
    box(f"side_rail_post_{i}", 13.62, 0.65, z, 13.78, 1.30,
        z + 0.14, "ring")
# -X 侧面一根同高度的短扶手（原画远侧也有一根）
box("far_rail", 2.02, 1.18, 6.40, 2.18, 1.32, 16.20, "ring")

# ================= 六根鱼雷管 =================
for tag, cx in zip("abcdef", TUBE_XS):
    # 管尾段（箱体之前）与收圆尾盖
    octagon(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_REAR, BOX_Z0 + 0.05,
            0.48, 0.66, "tube")
    octagon(f"tube_tail_{tag}", cx, TUBE_Y, TUBE_REAR - 0.40, TUBE_REAR,
            0.30, 0.42, "hull_dark")
    octagon(f"tube_tail_ring_{tag}", cx, TUBE_Y, TUBE_REAR - 0.46,
            TUBE_REAR - 0.32, 0.34, 0.48, "ring")
    # 管尾一道管箍
    octagon(f"tube_rear_band_{tag}", cx, TUBE_Y, 1.70, 2.02, 0.52, 0.76,
            "ring")
    # 管口段（箱体之后）
    octagon(f"tube_front_{tag}", cx, TUBE_Y, BOX_Z1 - 0.05, TUBE_FRONT,
            0.48, 0.66, "tube")
    octagon(f"tube_front_band_{tag}", cx, TUBE_Y, 18.10, 18.42, 0.52, 0.76,
            "ring")
    # 管口：亮管箍 + 端盖（空膛态换成近黑内膛）
    octagon(f"tube_muzzle_{tag}", cx, TUBE_Y, TUBE_FRONT - 0.65, TUBE_FRONT,
            0.52, 0.78, "ring")
    octagon(f"tube_cap_{tag}", cx, TUBE_Y, TUBE_FRONT, TUBE_FRONT + 0.35,
            0.48, 0.66, "cap")

# 相邻管之间的横向卡箍：管束两端各一组
for i in range(5):
    x1 = TUBE_XS[i] + 0.66
    x2 = TUBE_XS[i + 1] - 0.66
    box(f"clamp_rear_{i}", x1, TUBE_Y - 0.20, 4.70, x2, TUBE_Y + 0.20,
        5.60, "detail")
    box(f"clamp_front_{i}", x1, TUBE_Y - 0.20, 18.70, x2, TUBE_Y + 0.20,
        19.60, "detail")

# 管口下方一圈 U 形护杆
box("guard_rail_port", 2.90, 0.85, 17.50, 3.06, 1.00, 23.90, "ring")
box("guard_rail_stbd", 12.94, 0.85, 17.50, 13.10, 1.00, 23.90, "ring")
box("guard_rail_front", 2.90, 0.85, 23.74, 13.10, 1.00, 23.90, "ring")
box("guard_rail_rear", 2.90, 0.85, 17.50, 13.10, 1.00, 17.66, "ring")

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
    "parent": "piranport:item/japanese_sextuple_61cm_torpedo_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("japanese_sextuple_61cm_torpedo_3d.json", base)
    dump("japanese_sextuple_61cm_torpedo_3d_loaded.json", loaded_3d)
    dump("japanese_sextuple_61cm_torpedo.json", item_entry)
