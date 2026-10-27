#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 61厘米四连装鱼雷 的 3D 手持模型（按 Equip_L_91 原画从零建模）。

只依据原画本身的形体：
  - 一座大而方的深蓝灰装甲炮座，顶面内收成下沉甲板，四周一圈细栏杆；
  - 甲板上有三个敞口矩形舱围、一个圆形舱口和三个圆形舱盖凸起；
  - 炮座前脸下方有一块竖直椭圆凸台（原画的深色舱门/配重）；
  - 4 根鱼雷管沿舰宽方向平铺成一排，从炮座前脸穿出：
    前方伸出带浅色金属雷头的鱼雷，后方露出 4 个近黑管口。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆管用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱，读作圆柱；
  - 装填/空膛的差异只在雷头端盖：装填态浅色雷头，空膛态近黑管口；
    几何完全一致，_3d_loaded 只覆盖 cap → cap_loaded；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/quad_61cm_torpedo_3d.json          空膛 3D 发射器
  models/item/quad_61cm_torpedo_3d_loaded.json   装填 3D 发射器
  models/item/quad_61cm_torpedo.json             物品入口（builtin/entity）
  models/item/quad_61cm_oxygen_torpedo.json      氧气型物品入口（共用同一套）
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "quad_61cm_torpedo_"
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

# 4 根鱼雷管沿 x 平铺；炮座宽 10.5，管间距 2.25（管外径 1.7，留出可见缝）
TUBE_XS = (4.63, 6.88, 9.13, 11.38)
TUBE_Y = 4.55
TUBE_R_IN, TUBE_R_HALF = 0.62, 0.67
TUBE_Z0 = 2.60      # 管尾（炮座后方露出约 3.8）
TUBE_Z1 = 23.50     # 管口
HEAD_Z1 = 25.32     # 雷头尖端


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


def octagon_z(name, cx, cy, z1, z2, inner, half, tex, faces=None):
    """八棱柱（轴向 +Z）：轴对齐方盒 + 45° 旋转方盒叠成圆柱近似。

    inner = 方盒半宽（跨平面半径），half = 旋转方盒半宽（对角半径）。
    WHY half 必须比 inner 大 0.02 以上：两个方盒的顶/底面同为水平面，
    取值相同时会整面共面 z-fighting，模型在游戏里会闪烁花斑；
    略大一点让旋转面稳稳压在方盒面之上，多出的 0.02 在 2px/单位下不可见。
    """
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, half, tex):
    """八棱柱（轴向 +Y）：甲板上的圆形舱口/舱盖凸起。

    旋转方盒的顶面抬 0.004（同理避免与方盒顶面共面 z-fighting），
    从上方看是「方盒四角 + 旋转八棱」组成的圆盖。
    """
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half,
        y2 + 0.004, cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


def coaming(name, x1, z1, x2, z2, y, t=0.16, h=0.16, tex="hull_dark"):
    """甲板上的敞口矩形舱围：四条细挡边围成一圈（原画最显眼的甲板结构）。"""
    box(f"{name}_n", x1, y, z1, x2, y + h, z1 + t, tex)
    box(f"{name}_s", x1, y, z2 - t, x2, y + h, z2, tex)
    box(f"{name}_w", x1, y, z1 + t, x1 + t, y + h, z2 - t, tex)
    box(f"{name}_e", x2 - t, y, z1 + t, x2, y + h, z2 - t, tex)


def railing(x1, z1, x2, z2, y_base, y_top, tex="detail",
            post_x=(), post_z=(), t=0.14):
    """甲板四周的细栏杆：立柱 + 上/中两道横杆（原画栏杆是独立细杆）。"""
    for i, x in enumerate(post_x):
        box(f"rail_post_n{i}", x, y_base, z1, x + t, y_top, z1 + t, tex)
        box(f"rail_post_s{i}", x, y_base, z2 - t, x + t, y_top, z2, tex)
    for i, z in enumerate(post_z):
        box(f"rail_post_w{i}", x1, y_base, z, x1 + t, y_top, z + t, tex)
        box(f"rail_post_e{i}", x2 - t, y_base, z, x2, y_top, z + t, tex)
    mid = y_base + (y_top - y_base) * 0.52
    for i, y in enumerate((mid, y_top - t)):
        box(f"rail_n{i}", x1, y, z1, x2, y + t, z1 + t, tex)
        box(f"rail_s{i}", x1, y, z2 - t, x2, y + t, z2, tex)
        box(f"rail_w{i}", x1, y, z1 + t, x1 + t, y + t, z2 - t, tex)
        box(f"rail_e{i}", x2 - t, y, z1 + t, x2, y + t, z2 - t, tex)


# ================= 装甲炮座 =================
box("base_foot", 4.60, 0.00, 7.40, 11.40, 0.18, 17.40, "hull_dark")
box("base_skirt", 2.55, 0.15, 6.20, 13.45, 0.95, 18.80, "hull_dark")
box("hull_main", 2.75, 0.90, 6.40, 13.25, 5.30, 18.60, "hull")
box("hull_upper", 2.95, 5.25, 6.60, 13.05, 5.85, 18.40, "hull")
box("deck_rim", 3.05, 5.80, 6.45, 12.95, 6.05, 18.25, "hull_dark")
box("deck_plate", 3.25, 6.00, 6.70, 12.75, 6.30, 18.00, "hull")
box("front_plate", 3.00, 1.05, 18.60, 13.00, 5.20, 18.74, "hull")
box("rear_plate", 3.00, 1.05, 6.26, 13.00, 5.20, 6.40, "hull")
# 鱼雷管穿出前脸处的深色凹带（原画管根处的暗部）
box("tube_recess", 3.38, 3.66, 18.74, 12.62, 5.44, 18.92, "hull_dark")
# 侧舷加强板和立式托架
box("side_panel_w", 2.60, 1.40, 7.60, 2.78, 2.70, 17.30, "hull_dark")
box("side_panel_e", 13.22, 1.40, 7.60, 13.40, 2.70, 17.30, "hull_dark")
box("side_brace_w", 2.58, 3.30, 14.00, 2.78, 5.65, 16.60, "hull_dark")
box("side_brace_e", 13.22, 3.30, 14.00, 13.42, 5.65, 16.60, "hull_dark")
# 下裙板一圈横向板缝
box("rubbing_band_n", 2.70, 2.35, 6.36, 13.30, 2.60, 6.44, "hull_dark")
box("rubbing_band_s", 2.70, 2.35, 18.56, 13.30, 2.60, 18.64, "hull_dark")
box("rubbing_band_w", 2.70, 2.35, 6.44, 2.78, 2.60, 18.56, "hull_dark")
box("rubbing_band_e", 13.22, 2.35, 6.44, 13.30, 2.60, 18.56, "hull_dark")
# 四角立式角柱（原画炮座四角有清晰竖缝）
for tag, (x, z) in {"wn": (2.72, 6.40), "ws": (2.72, 18.44),
                    "en": (13.12, 6.40), "es": (13.12, 18.44)}.items():
    box(f"corner_post_{tag}", x, 0.95, z, x + 0.16, 5.35, z + 0.16, "hull_dark")
# 前脸下方的竖直椭圆凸台
box("front_pad", 8.70, 1.45, 18.74, 10.30, 3.15, 18.86, "hull_dark")
box("front_pad_inner", 8.95, 1.70, 18.86, 10.05, 2.90, 18.92, "detail")

# ================= 四根鱼雷管 =================
for tag, cx in zip("abcd", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1,
              TUBE_R_IN, TUBE_R_HALF, "tube")
    # 管尾：一圈金属端箍 + 近黑管口
    octagon_z(f"tube_rear_ring_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z0 + 0.35,
              0.66, 0.70, "detail")
    octagon_z(f"tube_rear_bore_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.02,
              TUBE_Z0 + 0.10, 0.46, 0.49, "hull_dark")
    octagon_z(f"tube_band_rear_{tag}", cx, TUBE_Y, 3.60, 3.95,
              0.70, 0.74, "ring")
    # 管口前段两道加强箍
    for i, z in enumerate((19.40, 20.90)):
        octagon_z(f"tube_band_{tag}_{i}", cx, TUBE_Y, z, z + 0.35,
                  0.70, 0.74, "ring")
    octagon_z(f"tube_muzzle_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.35, TUBE_Z1,
              0.72, 0.76, "ring")
    # 雷头：三级收圆 + 尖端（装填态浅色雷头 / 空膛态近黑管口，几何共用）
    octagon_z(f"head_{tag}_1", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.65,
              0.64, 0.68, "cap")
    octagon_z(f"head_{tag}_2", cx, TUBE_Y, TUBE_Z1 + 0.65, TUBE_Z1 + 1.25,
              0.45, 0.49, "cap")
    octagon_z(f"head_{tag}_3", cx, TUBE_Y, TUBE_Z1 + 1.25, TUBE_Z1 + 1.65,
              0.28, 0.32, "cap")
    octagon_z(f"head_{tag}_tip", cx, TUBE_Y, TUBE_Z1 + 1.65, HEAD_Z1,
              0.13, 0.16, "cap")

# ================= 甲板细节 =================
# 三个敞口矩形舱围
coaming("deck_coaming_a", 4.35, 8.40, 7.35, 10.20, 6.30)
coaming("deck_coaming_b", 8.50, 10.00, 11.90, 11.60, 6.30)
coaming("deck_coaming_c", 4.60, 12.60, 6.90, 14.00, 6.30)
# 圆形舱口（前左）+ 内圈把手
cylinder_y("deck_hatch", 9.90, 8.30, 6.30, 6.50, 0.64, 0.73, "ring")
cylinder_y("deck_hatch_inner", 9.90, 8.30, 6.50, 6.62, 0.26, 0.30, "hull_dark")
# 甲板中部三个圆形舱盖凸起
for i, (cx, cz) in enumerate(((8.30, 12.90), (10.05, 13.95), (11.35, 13.10))):
    cylinder_y(f"deck_bump_{i}", cx, cz, 6.30, 6.46, 0.40, 0.46, "ring")
# 中心小旋钮
cylinder_y("deck_knob", 8.05, 11.35, 6.30, 6.46, 0.17, 0.20, "detail")

# 四周栏杆
railing(3.50, 6.95, 12.50, 17.75, 6.30, 7.50,
        post_x=(3.95, 5.95, 8.00, 10.05, 12.05),
        post_z=(9.55, 12.35, 15.15))

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
    "parent": "piranport:item/quad_61cm_torpedo_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("quad_61cm_torpedo_3d.json", base)
    dump("quad_61cm_torpedo_3d_loaded.json", loaded_3d)
    # 常规型与氧气型共用同一套素材与同一份入口模型
    dump("quad_61cm_torpedo.json", item_entry)
    dump("quad_61cm_oxygen_torpedo.json", item_entry)
