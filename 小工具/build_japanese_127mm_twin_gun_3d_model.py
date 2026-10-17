#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成日本12.7厘米连装炮的 3D 手持模型（参考车万女仆的建模风格）。

风格要点（对照 TLM 的 Blockbench 模型）：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 用小尺寸元素堆细节（栏杆、吊篮、天线、舱门），而不是几个大盒子；
  - 用 ±22.5/45° 旋转元素做斜面装甲和菱形截面炮管。

物品走 Japanese127mmTwinGunRenderer（BEWLR）：
  - GUI/地面/展示框等上下文画 32x32 图标；
  - 第一/第三人称渲染本模型。

产出：
  models/item/japanese_127mm_twin_gun.json            builtin/entity 物品入口（只放 display）
  models/item/japanese_127mm_twin_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/japanese_127mm_twin_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）

WHY 用 builtin/entity：原版只在 BakedModel.isCustomRenderer() 为 true 时
才把渲染交给 BEWLR；普通模型即使注册了 IClientItemExtensions 也不会走。
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "japanese_127mm_twin_gun_hull",
    "hull_dark": NS + "japanese_127mm_twin_gun_hull_dark",
    "barrel": NS + "japanese_127mm_twin_gun_barrel",
    "detail": NS + "japanese_127mm_twin_gun_detail",
    "muzzle_empty": NS + "japanese_127mm_twin_gun_muzzle_empty",
    "muzzle_loaded": NS + "japanese_127mm_twin_gun_muzzle_loaded",
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


# ================= 座圈 =================
box("ring_square", 5.0, 0.0, 5.0, 11.0, 0.55, 11.0, "hull_dark")
box("ring_diamond", 5.6, 0.0, 5.6, 10.4, 0.55, 10.4, "hull_dark",
    rotation={"angle": 45, "axis": "y", "origin": [8.0, 0.275, 8.0]})
box("ring_band", 4.6, 0.55, 4.6, 11.4, 1.0, 11.4, "hull_dark")
box("ring_lip", 4.5, 1.0, 4.5, 11.5, 1.2, 11.5, "hull")

# ================= 炮塔主体 =================
box("hull_lower", 4.0, 1.2, 2.6, 12.0, 3.0, 12.6, "hull")
# 两舷向内倾的装甲板，做出上窄下宽的倒角
box("hull_slope_port", 3.86, 1.2, 3.0, 4.14, 3.5, 12.2, "hull",
    rotation={"angle": -22.5, "axis": "z", "origin": [4.0, 2.35, 7.6]})
box("hull_slope_stbd", 11.86, 1.2, 3.0, 12.14, 3.5, 12.2, "hull",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.0, 2.35, 7.6]})
box("hull_upper", 4.4, 3.0, 3.0, 11.6, 4.7, 12.0, "hull")
box("hull_top_step", 4.9, 4.7, 3.5, 11.1, 5.0, 11.5, "hull")

# ================= 前部炮盾 =================
box("shield_base", 5.2, 2.2, 12.2, 10.8, 4.5, 13.6, "hull")
# 前脸向后倾的斜面
box("shield_slope", 5.3, 4.35, 12.3, 10.7, 4.65, 13.5, "hull",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.4, 13.0]})
box("mantlet_port", 5.3, 2.8, 13.2, 7.3, 4.6, 14.5, "detail")
box("mantlet_stbd", 8.7, 2.8, 13.2, 10.7, 4.6, 14.5, "detail")
box("mantlet_center", 7.3, 2.8, 13.3, 8.7, 4.6, 14.2, "detail")
box("shield_trim", 5.1, 4.5, 13.9, 10.9, 4.75, 14.2, "hull_dark")

# ================= 后部配重与吊篮 =================
box("rear_block", 4.8, 1.8, 1.4, 11.2, 4.4, 2.65, "hull_dark")
# 吊篮：底板 + 四立柱 + 顶框，细杆堆出女仆模组那种结构感
box("basket_floor", 5.6, 2.0, 0.7, 10.4, 2.25, 1.5, "detail")
for tag, px in (("l", 5.6), ("r", 10.15)):
    box(f"basket_post_front_{tag}", px, 2.0, 0.7, px + 0.25, 3.9, 0.95, "detail")
    box(f"basket_post_back_{tag}", px, 2.0, 1.25, px + 0.25, 3.9, 1.5, "detail")
box("basket_rail_back", 5.6, 3.65, 0.7, 10.4, 3.9, 0.95, "detail")
box("basket_rail_front", 5.6, 3.65, 1.25, 10.4, 3.9, 1.5, "detail")

# ================= 两舷舱门 =================
box("door_port", 3.8, 1.6, 5.6, 4.02, 3.2, 8.2, "detail")
box("door_stbd", 11.98, 1.6, 5.6, 12.2, 3.2, 8.2, "detail")
box("door_handle_port", 3.72, 2.3, 6.7, 3.82, 2.6, 7.1, "hull_dark")
box("door_handle_stbd", 12.18, 2.3, 6.7, 12.28, 2.6, 7.1, "hull_dark")

# ================= 顶部设备 =================
box("rangefinder", 9.2, 5.0, 5.2, 10.8, 6.0, 6.8, "detail")
box("rangefinder_cap", 9.4, 6.0, 5.4, 10.6, 6.3, 6.6, "hull_dark")
box("hatch", 5.5, 5.0, 8.3, 7.1, 5.35, 9.9, "detail")
box("vent", 4.9, 5.0, 4.0, 5.7, 5.5, 4.8, "hull_dark")
box("periscope", 7.9, 5.0, 4.2, 8.4, 5.8, 4.7, "hull_dark")
box("antenna", 4.6, 5.0, 3.8, 4.8, 7.4, 4.0, "hull_dark")

# 顶部边缘护栏：四根横杆 + 四根角柱
box("rail_front", 5.0, 5.05, 11.25, 11.0, 5.25, 11.45, "detail")
box("rail_back", 5.0, 5.05, 3.55, 11.0, 5.25, 3.75, "detail")
box("rail_port", 4.85, 5.05, 3.6, 5.05, 5.25, 11.4, "detail")
box("rail_stbd", 10.95, 5.05, 3.6, 11.15, 5.25, 11.4, "detail")
for tag, rx, rz in (("fl", 4.85, 11.2), ("fr", 10.95, 11.2),
                    ("bl", 4.85, 3.55), ("br", 10.95, 3.55)):
    box(f"rail_post_{tag}", rx, 5.0, rz, rx + 0.2, 5.65, rz + 0.2, "hull_dark")

# ================= 双联菱形炮管 =================
for tag, cx in (("l", 6.3), ("r", 9.7)):
    diamond(f"barrel_{tag}", cx, 3.9, 13.8, 23.6, 0.7, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, 3.9, 23.6, 24.4, 0.82, "barrel")
    # 炮口内膛：装填/空膛差异只在这两个小面
    diamond(f"muzzle_{tag}", cx, 3.9, 24.4, 24.5, 0.4, "barrel",
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
        "barrel": TEX["barrel"],
        "detail": TEX["detail"],
        "muzzle_empty": TEX["muzzle_empty"],
        "muzzle_loaded": TEX["muzzle_loaded"],
    },
    "elements": elements,
}

# 物品入口：builtin/entity 让原版把渲染交给 BEWLR
item_entry = {
    "parent": "builtin/entity",
    "display": DISPLAY,
}

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
loaded_3d = {
    "parent": "piranport:item/japanese_127mm_twin_gun_3d",
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
    dump("japanese_127mm_twin_gun_3d.json", base)
    dump("japanese_127mm_twin_gun_3d_loaded.json", loaded_3d)
    dump("japanese_127mm_twin_gun.json", item_entry)
