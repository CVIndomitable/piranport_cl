#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成意大利三联320毫米炮的 3D 手持模型（按 Equip_L_167 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 近白色的椭圆炮塔，顶盖是强受光的曲面，炮塔下沿向外翻出；
  - 三块深灰近黑的方形炮罩并排立在前脸，炮管从炮罩中伸出；
  - 左舷一条外凸的测距仪臂，右舷后上方一片小鳍；
  - 座圈是宽大的浅灰圆鼓，炮塔与座圈之间有一圈近黑束带；
  - 座圈左下侧有一根带盖的小圆柱。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 炮罩顶端用 -22.5° 斜板压出原画的折角；
  - 模型坐标以 (8,8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/italian_triple_320mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/italian_triple_320mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/italian_triple_320mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "italian_triple_320mm_gun_hull",
    "hull_dark": NS + "italian_triple_320mm_gun_hull_dark",
    "roof": NS + "italian_triple_320mm_gun_roof",
    "barrel": NS + "italian_triple_320mm_gun_barrel",
    "detail": NS + "italian_triple_320mm_gun_detail",
    "mantlet": NS + "italian_triple_320mm_gun_mantlet",
    "muzzle_empty": NS + "italian_triple_320mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "italian_triple_320mm_gun_muzzle_loaded",
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
    """绕 Z 轴转 45° 的菱形截面（炮管）。"""
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


# ================= 座圈：浅灰圆鼓 + 近黑束带 =================
round_footprint("ring_drum", 8.0, 7.6, 4.7, 0.0, 0.7, "hull")
round_footprint("ring_band", 8.0, 7.6, 4.55, 0.7, 1.2, "hull_dark")
round_footprint("ring_lip", 8.0, 7.6, 4.62, 1.2, 1.4, "detail")
# 座圈左下侧带盖小圆柱
box("base_pin", 3.05, 0.9, 7.7, 3.45, 1.45, 8.1, "detail")
box("base_pin_cap", 2.95, 1.45, 7.6, 3.55, 1.6, 8.2, "hull_dark")

# ================= 炮塔主体：下沿外翻的椭圆轮廓 =================
box("flare_mid", 3.3, 1.4, 3.2, 12.7, 2.2, 12.0, "hull")
box("flare_front", 4.0, 1.4, 3.6, 12.0, 2.2, 12.6, "hull")
box("flare_rear", 4.2, 1.4, 2.8, 11.8, 2.2, 11.6, "hull")
box("body_mid", 3.7, 2.2, 3.5, 12.3, 3.55, 11.6, "hull")
box("body_front", 4.3, 2.2, 3.9, 11.7, 3.55, 12.3, "hull")
box("body_rear", 4.5, 2.2, 3.0, 11.5, 3.55, 11.6, "hull")

# ================= 近白圆顶：平顶 + 大斜面 =================
box("roof_top", 4.5, 3.55, 3.7, 11.5, 4.12, 11.3, "roof")
box("roof_front", 5.0, 3.45, 4.4, 11.0, 4.0, 11.9, "roof")
box("roof_rear", 5.1, 3.45, 3.2, 10.9, 4.0, 10.0, "roof")
box("roof_slope_front", 4.4, 3.25, 11.0, 11.6, 3.75, 12.05, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.5, 11.5]})
box("roof_slope_rear", 4.5, 3.25, 2.9, 11.5, 3.75, 3.9, "roof",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.5, 3.4]})
box("roof_slope_port", 3.65, 3.05, 3.5, 4.2, 4.0, 11.55, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.9, 3.45, 7.5]})
box("roof_slope_stbd", 11.8, 3.05, 3.5, 12.35, 4.0, 11.55, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.1, 3.45, 7.5]})

# ================= 左舷测距仪臂与右舷小鳍 =================
box("rangefinder_arm", 2.5, 3.0, 5.8, 3.9, 3.75, 7.8, "hull")
box("rangefinder_arm_top", 2.6, 3.75, 5.9, 3.8, 3.9, 7.7, "roof")
box("rangefinder_arm_tip", 2.5, 3.1, 6.0, 2.68, 3.65, 7.6, "hull_dark")
box("fin_stbd", 12.0, 3.55, 5.0, 13.15, 4.1, 6.3, "hull")
box("fin_stbd_top", 12.1, 4.1, 5.1, 13.05, 4.22, 6.2, "roof")

# ================= 前脸与三块方形炮罩 =================
box("front_plate", 4.3, 1.4, 12.25, 11.7, 3.5, 12.62, "hull")

BARREL_Y = 2.6
GUN_XS = (("l", 5.8), ("m", 8.0), ("r", 10.2))
for tag, cx in GUN_XS:
    # 深灰方形炮罩：主体 + 顶部折角
    box(f"mantlet_{tag}", cx - 0.62, 1.95, 12.1, cx + 0.62, 3.4, 13.05, "mantlet")
    box(f"mantlet_top_{tag}", cx - 0.6, 3.2, 12.05, cx + 0.6, 3.55, 13.0, "mantlet",
        rotation={"angle": -22.5, "axis": "x", "origin": [cx, 3.3, 12.6]})

# ================= 三根炮管 =================
for tag, cx in GUN_XS:
    diamond(f"barrel_sleeve_{tag}", cx, BARREL_Y, 11.8, 13.0, 0.36, "mantlet")
    diamond(f"barrel_{tag}", cx, BARREL_Y, 13.0, 26.2, 0.22, "barrel")
    diamond(f"barrel_ring_a_{tag}", cx, BARREL_Y, 15.5, 15.9, 0.28, "barrel")
    diamond(f"barrel_ring_b_{tag}", cx, BARREL_Y, 18.0, 18.4, 0.28, "barrel")
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
        "mantlet": TEX["mantlet"],
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
    "parent": "piranport:item/italian_triple_320mm_gun_3d",
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
    dump("italian_triple_320mm_gun_3d.json", base)
    dump("italian_triple_320mm_gun_3d_loaded.json", loaded_3d)
    dump("italian_triple_320mm_gun.json", item_entry)
