#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成意大利三联305毫米炮的 3D 手持模型（按 Equip_L_37 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 圆润的椭圆形炮塔，前宽后窄，顶盖是一整块明显受光的浅灰曲面；
  - 炮塔侧面与顶盖之间是连续的大圆角肩部，没有明显棱线；
  - 三根炮管从带浅色边框的拱形炮口伸出，炮根套筒是深灰近黑色；
  - 两舷各有一个外凸的矩形测距仪盒；
  - 座圈是整张原画里最暗的大圆鼓，左侧下沿另有一块外凸的小台阶。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 炮口用「矩形 + 45° 菱形」叠成拱形，外面再套一圈浅色边框；
  - 模型坐标以 (8,8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/italian_triple_305mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/italian_triple_305mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/italian_triple_305mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "italian_triple_305mm_gun_hull",
    "hull_dark": NS + "italian_triple_305mm_gun_hull_dark",
    "roof": NS + "italian_triple_305mm_gun_roof",
    "barrel": NS + "italian_triple_305mm_gun_barrel",
    "detail": NS + "italian_triple_305mm_gun_detail",
    "port": NS + "italian_triple_305mm_gun_port",
    "muzzle_empty": NS + "italian_triple_305mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "italian_triple_305mm_gun_muzzle_loaded",
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
    """绕 Z 轴转 45° 的菱形截面（炮管/拱形炮口）。"""
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


# ================= 座圈：原画里最暗的大圆鼓 =================
round_footprint("ring_flange", 8.0, 7.5, 4.65, 0.0, 0.5, "hull_dark")
round_footprint("ring_drum", 8.0, 7.5, 4.35, 0.5, 1.05, "hull_dark")
round_footprint("ring_lip", 8.0, 7.5, 4.45, 1.05, 1.25, "detail")
# 左侧下沿外凸的小台阶（原画座圈左下方那块方形凸起）
box("base_step", 3.05, 0.55, 8.5, 3.75, 1.2, 9.6, "detail")

# ================= 炮塔主体：前宽后窄的圆角轮廓 =================
box("lower_mid", 3.5, 1.25, 3.4, 12.5, 2.8, 11.8, "hull")
box("lower_front", 4.2, 1.25, 3.8, 11.8, 2.8, 12.4, "hull")
box("lower_rear", 4.4, 1.25, 2.9, 11.6, 2.8, 11.4, "hull")
box("upper_mid", 3.8, 2.8, 3.6, 12.2, 3.9, 11.6, "hull")
box("upper_front", 4.4, 2.8, 4.0, 11.6, 3.9, 12.2, "hull")
box("upper_rear", 4.6, 2.8, 3.1, 11.4, 3.9, 11.6, "hull")

# ================= 圆顶屋盖：浅色曲面 + 大圆角肩部 =================
box("roof_top", 4.45, 3.9, 3.8, 11.55, 4.2, 11.55, "roof")
box("roof_front", 4.9, 3.8, 4.5, 11.1, 4.1, 12.05, "roof")
box("roof_rear", 5.0, 3.8, 3.2, 11.0, 4.1, 10.4, "roof")
box("roof_slope_front", 4.4, 3.55, 11.25, 11.6, 3.95, 12.25, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.75, 11.8]})
box("roof_slope_rear", 4.5, 3.55, 2.95, 11.5, 3.95, 3.85, "roof",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.75, 3.4]})
box("roof_slope_port", 3.75, 3.35, 3.7, 4.3, 4.1, 11.55, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [4.0, 3.65, 7.6]})
box("roof_slope_stbd", 11.7, 3.35, 3.7, 12.25, 4.1, 11.55, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.0, 3.65, 7.6]})

# ================= 两舷外凸的测距仪盒 =================
for tag, x1, x2 in (("port", 2.85, 4.15), ("stbd", 11.85, 13.15)):
    box(f"rangefinder_{tag}", x1, 3.35, 6.1, x2, 4.05, 8.1, "hull")
    box(f"rangefinder_top_{tag}", x1 + 0.1, 4.05, 6.2, x2 - 0.1, 4.18, 8.0, "roof")
    if tag == "port":
        box("rangefinder_tip_port", x1, 3.45, 6.2, x1 + 0.18, 3.95, 8.0, "hull_dark")
    else:
        box("rangefinder_tip_stbd", x2 - 0.18, 3.45, 6.2, x2, 3.95, 8.0, "hull_dark")

# ================= 前脸与三处拱形炮口 =================
box("front_plate", 4.3, 1.25, 12.1, 11.7, 3.5, 12.5, "hull")
box("front_plate_edge", 4.4, 3.5, 12.15, 11.6, 3.65, 12.52, "hull_dark")

BARREL_Y = 2.75
GUN_XS = (("l", 5.9), ("m", 8.0), ("r", 10.1))
for tag, cx in GUN_XS:
    # 浅色边框：矩形 + 菱形拼出拱形轮廓
    box(f"port_frame_rect_{tag}", cx - 0.62, 2.0, 12.5, cx + 0.62, 2.95, 12.58, "detail")
    diamond(f"port_frame_arch_{tag}", cx, 2.95, 12.5, 12.58, 0.62, "detail")
    # 近黑色开口
    box(f"port_rect_{tag}", cx - 0.48, 2.15, 12.58, cx + 0.48, 2.95, 12.64, "port")
    diamond(f"port_arch_{tag}", cx, 2.95, 12.58, 12.64, 0.48, "port")

# ================= 三根炮管与深色套筒 =================
for tag, cx in GUN_XS:
    diamond(f"barrel_sleeve_{tag}", cx, BARREL_Y, 11.9, 13.6, 0.4, "hull_dark")
    diamond(f"barrel_step_{tag}", cx, BARREL_Y, 13.6, 14.4, 0.32, "barrel")
    diamond(f"barrel_{tag}", cx, BARREL_Y, 14.4, 26.2, 0.21, "barrel")
    diamond(f"barrel_ring_a_{tag}", cx, BARREL_Y, 16.8, 17.2, 0.28, "barrel")
    diamond(f"barrel_ring_b_{tag}", cx, BARREL_Y, 19.3, 19.7, 0.28, "barrel")
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
    "parent": "piranport:item/italian_triple_305mm_gun_3d",
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
    dump("italian_triple_305mm_gun_3d.json", base)
    dump("italian_triple_305mm_gun_3d_loaded.json", loaded_3d)
    dump("italian_triple_305mm_gun.json", item_entry)
