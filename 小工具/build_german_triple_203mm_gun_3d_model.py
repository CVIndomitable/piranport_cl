#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成德国三联203毫米炮的 3D 手持模型（按 Equip_L_260 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 宽而扁的银色炮塔，侧面上半部被一整圈深蓝灰屋顶斜面覆盖，
    形成「浅色侧板 + 深色倒角 + 深色平顶」的三段式侧影；
  - 前脸是浅色直立装甲，三管等距，每根炮管根部有一块竖长水滴形炮罩；
  - 右前脸有一扇带浅色边框的黑色观察窗，左前角另有一扇小观察窗；
  - 顶盖只有两根小零件：左后天线杆、右后蘑菇形通风口；
  - 座圈是大直径圆鼓，炮塔前部明显悬出座圈。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 模型坐标以 (8,8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/german_triple_203mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/german_triple_203mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/german_triple_203mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "german_triple_203mm_gun_hull",
    "hull_dark": NS + "german_triple_203mm_gun_hull_dark",
    "roof": NS + "german_triple_203mm_gun_roof",
    "barrel": NS + "german_triple_203mm_gun_barrel",
    "detail": NS + "german_triple_203mm_gun_detail",
    "port": NS + "german_triple_203mm_gun_port",
    "muzzle_empty": NS + "german_triple_203mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "german_triple_203mm_gun_muzzle_loaded",
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
    """绕 Z 轴转 45° 的炮管：方形截面变成菱形截面。"""
    box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
        faces=faces, full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆（比 45° 方块并集干净，不会出尖角）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 座圈：大直径圆鼓，中心略靠后 =================
BASE_Z = 7.6
round_footprint("ring_flange", 8.0, BASE_Z, 4.55, 0.0, 0.35, "hull_dark")
round_footprint("ring_drum", 8.0, BASE_Z, 4.25, 0.35, 0.9, "hull")
round_footprint("ring_lip", 8.0, BASE_Z, 4.35, 0.9, 1.15, "hull_dark")

# ================= 炮塔主体：阶梯八边形平面 =================
# 下层浅色侧板（中段最宽，前后收窄）
box("lower_mid", 3.5, 1.15, 4.0, 12.5, 2.9, 11.0, "hull")
box("lower_front", 4.4, 1.15, 4.0, 11.6, 2.9, 12.6, "hull")
box("lower_rear", 4.4, 1.15, 2.3, 11.6, 2.9, 11.0, "hull")
# 上层（略收进，给屋顶斜面留出位置）
box("upper_mid", 3.7, 2.7, 4.2, 12.3, 3.98, 10.8, "hull")
box("upper_front", 4.5, 2.7, 4.2, 11.5, 3.98, 12.2, "hull")
box("upper_rear", 4.5, 2.7, 2.6, 11.5, 3.98, 10.8, "hull")

# ================= 深色屋顶：平顶 + 四周大斜面 =================
box("roof_top", 4.35, 4.1, 3.45, 11.65, 4.48, 11.15, "roof")
# 前斜面（从平顶向前下方压到前装甲上沿）
box("roof_slope_front", 4.4, 3.95, 10.7, 11.6, 4.3, 11.95, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.1, 11.2]})
# 后斜面
box("roof_slope_rear", 4.4, 3.95, 2.75, 11.6, 4.3, 3.95, "roof",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 4.1, 3.4]})
# 两舷大斜面：原画里侧面最醒目的深色倒角，从屋顶一直包到侧板上沿
box("roof_slope_port", 3.5, 2.95, 3.55, 4.0, 4.45, 11.05, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.75, 3.7, 7.3]})
box("roof_slope_stbd", 12.0, 2.95, 3.55, 12.5, 4.45, 11.05, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.25, 3.7, 7.3]})

# ================= 前脸与后脸 =================
# 浅色直立前装甲，三根炮管从它上面穿出
box("front_plate", 4.4, 1.15, 12.25, 11.6, 3.35, 12.7, "hull")
box("front_plate_edge", 4.5, 3.35, 12.3, 11.5, 3.45, 12.72, "hull_dark")
# 后脸（原画后部是平整的浅色板，只有上沿一圈深色倒角）
box("rear_plate", 4.45, 1.15, 2.28, 11.55, 3.55, 2.55, "hull")
box("rear_plate_edge", 4.5, 3.55, 2.3, 11.5, 3.75, 2.58, "hull_dark")

# ================= 三根炮管与水滴形炮罩 =================
BARREL_Y = 2.85
for tag, cx in (("l", 5.6), ("m", 8.0), ("r", 10.4)):
    # 竖长水滴形炮罩：五层逐渐收分，下窄、中宽、上收
    box(f"fairing_a_{tag}", cx - 0.36, 1.75, 12.55, cx + 0.36, 2.2, 12.98, "detail")
    box(f"fairing_b_{tag}", cx - 0.48, 2.2, 12.52, cx + 0.48, 2.65, 12.98, "detail")
    box(f"fairing_c_{tag}", cx - 0.52, 2.65, 12.5, cx + 0.52, 3.1, 12.98, "detail")
    box(f"fairing_d_{tag}", cx - 0.46, 3.1, 12.52, cx + 0.46, 3.5, 12.98, "detail")
    box(f"fairing_e_{tag}", cx - 0.32, 3.5, 12.55, cx + 0.32, 3.85, 12.98, "detail")
    # 炮根短套筒 + 细长炮管 + 两道加强环
    diamond(f"barrel_sleeve_{tag}", cx, BARREL_Y, 11.9, 13.5, 0.38, "barrel")
    diamond(f"barrel_{tag}", cx, BARREL_Y, 13.5, 25.6, 0.26, "barrel")
    diamond(f"barrel_ring_a_{tag}", cx, BARREL_Y, 16.6, 17.0, 0.32, "barrel")
    diamond(f"barrel_ring_b_{tag}", cx, BARREL_Y, 19.6, 20.0, 0.32, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, BARREL_Y, 25.6, 26.1, 0.34, "barrel")
    # 炮口内膛：装填/空膛差异只在这三个小面
    diamond(f"muzzle_{tag}", cx, BARREL_Y, 26.1, 26.2, 0.22, "barrel",
            faces={"south": "muzzle_empty"}, full_uv_faces=("south",))

# ================= 观察窗 =================
# 右前脸：浅色边框 + 近黑色窗
box("sight_r_frame", 10.85, 2.55, 12.7, 11.55, 3.2, 12.78, "detail")
box("sight_r_glass", 10.95, 2.65, 12.78, 11.45, 3.1, 12.84, "port")
# 左前角：小窗开在收窄的侧向台阶面上
box("sight_l_frame", 4.33, 2.6, 11.25, 4.4, 3.2, 11.85, "detail")
box("sight_l_glass", 4.27, 2.7, 11.35, 4.33, 3.1, 11.75, "port")

# ================= 顶盖小零件 =================
box("antenna", 5.35, 4.46, 4.25, 5.47, 6.7, 4.37, "hull_dark")
box("vent_base", 10.35, 4.46, 4.5, 10.85, 4.68, 5.0, "detail")
box("vent_cap", 10.25, 4.68, 4.4, 10.95, 4.8, 5.1, "hull_dark")

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
    "parent": "piranport:item/german_triple_203mm_gun_3d",
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
    dump("german_triple_203mm_gun_3d.json", base)
    dump("german_triple_203mm_gun_3d_loaded.json", loaded_3d)
    dump("german_triple_203mm_gun.json", item_entry)
