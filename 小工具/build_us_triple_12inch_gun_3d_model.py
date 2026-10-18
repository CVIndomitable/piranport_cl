#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成美国三联12英寸炮的 3D 手持模型（按 Equip_L_33 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 方正的蓝灰迷彩炮塔：大面积平顶、前脸后倾、前角切角、后部略收窄；
  - 侧面有浅灰垂直迷彩条，顶盖是比侧面略亮的蓝灰色；
  - 三个半圆形炮口，炮根是深灰粗套筒，炮管细长并带加强环；
  - 右后舷外凸一个矩形测距仪盒，左后屋顶有一个小台阶盒；
  - 座圈是深蓝灰的矮圆鼓。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 炮口用「矩形 + 45° 菱形」叠成半圆拱；
  - 模型坐标以 (8,8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/us_triple_12inch_gun.json            builtin/entity 物品入口（只放 display）
  models/item/us_triple_12inch_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/us_triple_12inch_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "us_triple_12inch_gun_hull",
    "hull_dark": NS + "us_triple_12inch_gun_hull_dark",
    "roof": NS + "us_triple_12inch_gun_roof",
    "barrel": NS + "us_triple_12inch_gun_barrel",
    "detail": NS + "us_triple_12inch_gun_detail",
    "patch": NS + "us_triple_12inch_gun_patch",
    "port": NS + "us_triple_12inch_gun_port",
    "muzzle_empty": NS + "us_triple_12inch_gun_muzzle_empty",
    "muzzle_loaded": NS + "us_triple_12inch_gun_muzzle_loaded",
}

elements = []


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面尺寸给 uv 窗口（uv 单位=模型单位；贴图 32px 时 16uv=整图）。

    面宽高与 uv 窗口一致，保证每模型单位恒定 2 像素，纹理不被拉伸。
    """
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    max_u = max(0.0, 16.0 - w)
    max_v = max(0.0, 16.0 - h)
    # 确定性错位，避免所有面采同一块纹理
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
        full_uv_faces=()):
    """添加一个盒子。

    faces: 覆盖单面材质，如 {"south": "muzzle_empty"}。
    rotation: 原版元素旋转（单轴，角度仅 ±45/±22.5/0）。
    full_uv_faces: 这些面改用整图 uv（用于炮口这种纯色小面）。
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


def diamond(name, cx, cy, z1, z2, half, tex, faces=None, full_uv_faces=()):
    """绕 Z 轴转 45° 的菱形截面（炮管/半圆炮口）。"""
    box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
        faces=faces, full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z",
                  "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆（比 45° 方块并集干净，不会出尖角）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 座圈：深蓝灰矮圆鼓 =================
round_footprint("ring_flange", 8.0, 7.5, 4.55, 0.0, 0.4, "hull_dark")
round_footprint("ring_drum", 8.0, 7.5, 4.3, 0.4, 0.95, "hull_dark")
round_footprint("ring_lip", 8.0, 7.5, 4.4, 0.95, 1.15, "detail")

# ================= 炮塔主体：方正蓝灰炮塔 =================
box("lower_mid", 3.4, 1.15, 3.4, 12.6, 2.8, 11.6, "hull")
box("lower_front", 4.1, 1.15, 3.8, 11.9, 2.8, 12.3, "hull")
box("lower_rear", 4.3, 1.15, 2.9, 11.7, 2.8, 11.4, "hull")
box("upper_mid", 3.7, 2.8, 3.6, 12.3, 3.9, 11.5, "hull")
box("upper_front", 4.3, 2.8, 4.0, 11.7, 3.9, 12.2, "hull")
box("upper_rear", 4.5, 2.8, 3.1, 11.5, 3.9, 11.5, "hull")

# ================= 蓝灰顶盖：大平顶 + 倒角 =================
box("roof_top", 3.95, 3.9, 3.55, 12.05, 4.25, 11.75, "roof")
box("roof_slope_front", 4.0, 3.7, 11.2, 12.0, 4.02, 12.25, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.9, 11.75]})
box("roof_slope_rear", 4.4, 3.7, 3.0, 11.6, 4.0, 3.95, "roof",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.9, 3.5]})
box("roof_slope_port", 3.55, 3.4, 3.6, 4.1, 4.1, 11.55, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.8, 3.7, 7.55]})
box("roof_slope_stbd", 11.9, 3.4, 3.6, 12.45, 4.1, 11.55, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.2, 3.7, 7.55]})

# ================= 侧舷浅灰迷彩条 =================
box("camo_stbd", 12.28, 1.15, 7.6, 12.44, 3.85, 10.0, "patch")
box("camo_front", 10.9, 1.15, 12.62, 11.7, 3.55, 12.68, "patch")

# ================= 右后测距仪盒与左后屋顶台阶 =================
box("rangefinder", 12.45, 3.3, 5.2, 13.75, 4.0, 6.9, "hull")
box("rangefinder_top", 12.55, 4.0, 5.3, 13.65, 4.12, 6.8, "roof")
box("rangefinder_face", 13.75, 3.4, 5.35, 13.88, 3.9, 6.75, "hull_dark")
box("roof_step", 4.15, 4.25, 3.8, 5.5, 4.62, 4.95, "hull")
box("roof_step_top", 4.25, 4.62, 3.9, 5.4, 4.74, 4.85, "roof")

# ================= 前脸与半圆炮口 =================
box("front_plate", 4.1, 1.15, 12.2, 11.9, 3.55, 12.62, "hull")
box("front_plate_edge", 4.2, 3.55, 12.25, 11.8, 3.7, 12.64, "hull_dark")

BARREL_Y = 2.75
GUN_XS = (("l", 5.7), ("m", 8.0), ("r", 10.3))
for tag, cx in GUN_XS:
    # 半圆拱：下半矩形 + 上半菱形，近黑色
    box(f"port_rect_{tag}", cx - 0.46, 2.1, 12.62, cx + 0.46, 2.85, 12.68, "port")
    diamond(f"port_arch_{tag}", cx, 2.85, 12.62, 12.68, 0.46, "port")
    # 浅色包边（比开口大一圈，压在前脸上）
    box(f"port_frame_rect_{tag}", cx - 0.58, 1.98, 12.58, cx + 0.58, 2.85, 12.63, "detail")
    diamond(f"port_frame_arch_{tag}", cx, 2.85, 12.58, 12.63, 0.58, "detail")

# ================= 三根炮管与深色套筒 =================
for tag, cx in GUN_XS:
    diamond(f"barrel_sleeve_{tag}", cx, BARREL_Y, 11.8, 13.3, 0.44, "hull_dark")
    diamond(f"barrel_step_{tag}", cx, BARREL_Y, 13.3, 14.1, 0.34, "barrel")
    diamond(f"barrel_{tag}", cx, BARREL_Y, 14.1, 26.2, 0.22, "barrel")
    diamond(f"barrel_ring_a_{tag}", cx, BARREL_Y, 16.7, 17.1, 0.29, "barrel")
    diamond(f"barrel_ring_b_{tag}", cx, BARREL_Y, 19.2, 19.6, 0.29, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, BARREL_Y, 25.9, 26.4, 0.3, "barrel")
    # 炮口内膛：装填/空膛差异只在这三个小面
    diamond(f"muzzle_{tag}", cx, BARREL_Y, 26.4, 26.5, 0.18, "barrel",
            faces={"south": "muzzle_empty"}, full_uv_faces=("south",))

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
        "roof": TEX["roof"],
        "barrel": TEX["barrel"],
        "detail": TEX["detail"],
        "patch": TEX["patch"],
        "port": TEX["port"],
        "muzzle_empty": TEX["muzzle_empty"],
        "muzzle_loaded": TEX["muzzle_loaded"],
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
    "parent": "piranport:item/us_triple_12inch_gun_3d",
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
    dump("us_triple_12inch_gun_3d.json", base)
    dump("us_triple_12inch_gun_3d_loaded.json", loaded_3d)
    dump("us_triple_12inch_gun.json", item_entry)
