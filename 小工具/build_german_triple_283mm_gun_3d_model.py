#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成德国三联283毫米炮的 3D 手持模型（按 Equip_L_27 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 紧凑的圆顶炮塔，前脸和屋顶都由大块斜面组成，顶盖比侧面亮一档；
  - 三根炮管靠得很近，炮根是逐级收细的粗套筒，管身带两道加强环；
  - 后部两舷各伸出一条粗大的矩形测距仪臂（原画最醒目的识别点）；
  - 右后方屋面上有一根小圆柱形潜望镜/通风口；
  - 座圈是宽大的圆鼓，炮塔前低后高，座圈在炮塔下沿露出一圈。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 炮口暗板用 45° 菱形贴在前脸上，读作圆形炮口；
  - 模型坐标以 (8,8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/german_triple_283mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/german_triple_283mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/german_triple_283mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "german_triple_283mm_gun_hull",
    "hull_dark": NS + "german_triple_283mm_gun_hull_dark",
    "roof": NS + "german_triple_283mm_gun_roof",
    "barrel": NS + "german_triple_283mm_gun_barrel",
    "detail": NS + "german_triple_283mm_gun_detail",
    "port": NS + "german_triple_283mm_gun_port",
    "muzzle_empty": NS + "german_triple_283mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "german_triple_283mm_gun_muzzle_loaded",
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


def diamond(name, cx, cy, z1, z2, half, tex, faces=None, full_uv_faces=(),
            angle=45):
    """绕 Z 轴旋转的菱形截面（炮管/炮口暗板）。"""
    box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
        faces=faces, full_uv_faces=full_uv_faces,
        rotation={"angle": angle, "axis": "z",
                  "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆（比 45° 方块并集干净，不会出尖角）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 座圈：宽大圆鼓 =================
round_footprint("ring_flange", 8.0, 7.6, 4.5, 0.0, 0.35, "hull_dark")
round_footprint("ring_drum", 8.0, 7.6, 4.2, 0.35, 0.9, "hull")
round_footprint("ring_lip", 8.0, 7.6, 4.3, 0.9, 1.15, "detail")

# ================= 炮塔主体：前窄后宽的圆顶轮廓 =================
box("lower_mid", 3.8, 1.15, 3.6, 12.2, 2.8, 11.4, "hull")
box("lower_front", 4.5, 1.15, 3.9, 11.5, 2.8, 12.2, "hull")
box("lower_rear", 4.5, 1.15, 2.7, 11.5, 2.8, 11.4, "hull")
box("upper_mid", 4.0, 2.8, 3.4, 12.0, 3.85, 11.2, "hull")
box("upper_front", 4.7, 2.8, 3.8, 11.3, 3.85, 12.0, "hull")
box("upper_rear", 4.7, 2.8, 2.8, 11.3, 3.85, 11.2, "hull")

# ================= 圆顶屋盖：浅色平顶 + 四周斜面 =================
box("roof_top", 4.9, 3.85, 3.7, 11.1, 4.25, 10.9, "roof")
box("roof_slope_front", 4.8, 3.65, 10.3, 11.2, 3.98, 11.6, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.85, 10.9]})
box("roof_slope_rear", 4.8, 3.65, 3.0, 11.2, 3.98, 4.3, "roof",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.85, 3.7]})
box("roof_slope_port", 4.0, 3.3, 3.6, 4.5, 4.05, 11.15, "roof",
    rotation={"angle": -22.5, "axis": "z", "origin": [4.25, 3.65, 7.35]})
box("roof_slope_stbd", 11.5, 3.3, 3.6, 12.0, 4.05, 11.15, "roof",
    rotation={"angle": 22.5, "axis": "z", "origin": [11.75, 3.65, 7.35]})

# ================= 后部两舷的测距仪臂（原画识别点） =================
for tag, x1, x2 in (("port", 2.2, 4.3), ("stbd", 11.7, 13.8)):
    box(f"wing_{tag}", x1, 3.45, 4.9, x2, 4.25, 7.2, "hull")
    # 臂顶盖一道浅色倒角，外端面压深一档
    box(f"wing_top_{tag}", x1 + 0.15, 4.25, 5.0, x2 - 0.15, 4.4, 7.1, "roof")
    if tag == "port":
        box("wing_tip_port", x1, 3.55, 5.0, x1 + 0.2, 4.15, 7.1, "hull_dark")
    else:
        box("wing_tip_stbd", x2 - 0.2, 3.55, 5.0, x2, 4.15, 7.1, "hull_dark")

# ================= 前脸与炮口暗板 =================
box("front_plate", 4.5, 1.15, 11.9, 11.5, 3.35, 12.4, "hull")
box("front_plate_edge", 4.6, 3.35, 11.95, 11.4, 3.5, 12.42, "hull_dark")

BARREL_Y = 2.75
GUN_XS = (("l", 5.8), ("m", 8.0), ("r", 10.2))
for tag, cx in GUN_XS:
    # 菱形暗板贴在前脸上，读作圆形炮口
    diamond(f"gunport_{tag}", cx, BARREL_Y, 12.2, 12.34, 0.85, "port")

# ================= 三根炮管与阶梯套筒 =================
for tag, cx in GUN_XS:
    diamond(f"barrel_sleeve_{tag}", cx, BARREL_Y, 11.2, 12.9, 0.68, "barrel")
    diamond(f"barrel_sleeve_mid_{tag}", cx, BARREL_Y, 12.9, 14.2, 0.55, "barrel")
    diamond(f"barrel_sleeve_front_{tag}", cx, BARREL_Y, 14.2, 15.3, 0.42, "barrel")
    diamond(f"barrel_{tag}", cx, BARREL_Y, 15.3, 25.8, 0.22, "barrel")
    diamond(f"barrel_ring_a_{tag}", cx, BARREL_Y, 17.4, 17.8, 0.3, "barrel")
    diamond(f"barrel_ring_b_{tag}", cx, BARREL_Y, 19.9, 20.3, 0.3, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, BARREL_Y, 25.8, 26.3, 0.32, "barrel")
    # 炮口内膛：装填/空膛差异只在这三个小面
    diamond(f"muzzle_{tag}", cx, BARREL_Y, 26.3, 26.4, 0.2, "barrel",
            faces={"south": "muzzle_empty"}, full_uv_faces=("south",))

# ================= 顶盖潜望镜/通风口 =================
box("periscope_base", 9.3, 4.25, 6.2, 9.7, 4.6, 6.6, "detail")
box("periscope_cap", 9.2, 4.6, 6.1, 9.8, 4.75, 6.7, "hull_dark")

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
    "parent": "piranport:item/german_triple_283mm_gun_3d",
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
    dump("german_triple_283mm_gun_3d.json", base)
    dump("german_triple_283mm_gun_3d_loaded.json", loaded_3d)
    dump("german_triple_283mm_gun.json", item_entry)
