#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成德国双联203毫米炮的 3D 手持模型（对齐日本12.7厘米炮的建模风格）。

风格要点（同 build_japanese_127mm_twin_gun_3d_model.py）：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 用小尺寸元素堆细节（舱门、测距仪、扶手、通风格栅），而不是几个大盒子；
  - 用 ±22.5° 旋转元素做内倾装甲，45° 菱形截面炮管。
  - 与日本炮的区别：炮塔更宽更扁、前脸更陡、双管间距更窄，取原画
    Equip_L_18.png 的德式方形炮塔轮廓。

物品走 GermanTwin203mmGunRenderer（BEWLR）：
  - GUI/地面/展示框等上下文画 32x32 图标；
  - 第一/第三人称渲染本模型。

产出：
  models/item/german_twin_203mm_gun.json            builtin/entity 物品入口（只放 display）
  models/item/german_twin_203mm_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/german_twin_203mm_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "german_twin_203mm_gun_hull",
    "hull_dark": NS + "german_twin_203mm_gun_hull_dark",
    "barrel": NS + "german_twin_203mm_gun_barrel",
    "detail": NS + "german_twin_203mm_gun_detail",
    "muzzle_empty": NS + "german_twin_203mm_gun_muzzle_empty",
    "muzzle_loaded": NS + "german_twin_203mm_gun_muzzle_loaded",
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


# ================= 座圈（比日本炮更宽更扁） =================
box("ring_square", 4.2, 0.0, 4.2, 11.8, 0.5, 11.8, "hull_dark")
box("ring_diamond", 4.8, 0.0, 4.8, 11.2, 0.5, 11.2, "hull_dark",
    rotation={"angle": 45, "axis": "y", "origin": [8.0, 0.25, 8.0]})
box("ring_band", 3.8, 0.5, 3.8, 12.2, 0.95, 12.2, "hull_dark")
box("ring_lip", 3.7, 0.95, 3.7, 12.3, 1.15, 12.3, "hull")

# ================= 炮塔主体 =================
box("hull_lower", 3.4, 1.15, 2.4, 12.6, 2.9, 12.4, "hull")
# 两舷向内倾的装甲板，做出上窄下宽的倒角
box("hull_slope_port", 3.26, 1.15, 2.8, 3.54, 3.4, 12.0, "hull",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.4, 2.3, 7.4]})
box("hull_slope_stbd", 12.46, 1.15, 2.8, 12.74, 3.4, 12.0, "hull",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.6, 2.3, 7.4]})
box("hull_upper", 3.8, 2.9, 2.8, 12.2, 4.5, 12.0, "hull")
box("hull_top_step", 4.2, 4.5, 3.2, 11.8, 4.8, 11.4, "hull")

# ================= 前部炮盾（德式陡前脸） =================
box("shield_base", 4.2, 2.2, 12.3, 11.8, 4.5, 13.5, "hull")
box("shield_slope", 4.3, 4.35, 12.4, 11.7, 4.65, 13.4, "hull",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.4, 12.9]})
box("mantlet_center", 6.0, 2.6, 13.3, 10.0, 4.3, 14.2, "detail")
box("mantlet_port", 5.7, 2.9, 13.6, 6.9, 4.2, 14.5, "detail")
box("mantlet_stbd", 9.1, 2.9, 13.6, 10.3, 4.2, 14.5, "detail")
box("shield_trim", 4.1, 4.45, 13.8, 11.9, 4.7, 14.1, "hull_dark")

# ================= 后部配重 =================
box("rear_block", 4.2, 1.6, 1.1, 11.8, 4.2, 2.5, "hull_dark")
box("rear_step", 4.6, 4.2, 1.6, 11.4, 4.5, 2.5, "hull")
box("rear_hatch", 6.0, 2.0, 0.85, 10.0, 3.4, 1.15, "detail")
box("rear_handle", 7.2, 2.6, 0.72, 8.8, 2.9, 0.88, "hull_dark")

# ================= 两舷舱门与扶手 =================
box("door_port", 3.16, 1.5, 5.4, 3.4, 2.9, 8.0, "detail")
box("door_stbd", 12.6, 1.5, 5.4, 12.84, 2.9, 8.0, "detail")
box("door_handle_port", 3.08, 2.1, 6.5, 3.18, 2.4, 6.9, "hull_dark")
box("door_handle_stbd", 12.82, 2.1, 6.5, 12.92, 2.4, 6.9, "hull_dark")
# 侧面抓握扶手：横杆 + 两端短柱
for tag, x in (("port", 3.1), ("stbd", 12.9)):
    box(f"step_rail_{tag}", x, 1.9, 8.6, x + 0.16, 2.05, 10.4, "hull_dark")

# ================= 顶部设备（原画的左右测距仪是德式识别点） =================
box("rangefinder", 4.3, 4.8, 6.4, 6.1, 5.7, 8.4, "detail")
box("rangefinder_cap", 4.4, 5.7, 6.6, 6.0, 6.0, 8.2, "hull_dark")
box("side_rangefinder", 11.9, 3.6, 8.2, 12.9, 4.6, 10.2, "detail")
box("side_rangefinder_cap", 12.0, 4.6, 8.4, 12.8, 4.85, 10.0, "hull_dark")
box("hatch", 5.4, 4.8, 9.0, 7.0, 5.1, 10.4, "detail")
box("hatch_handle", 6.0, 5.1, 9.3, 6.4, 5.2, 9.7, "hull_dark")
box("vent", 9.4, 4.8, 4.0, 10.2, 5.3, 4.8, "hull_dark")
box("periscope", 8.2, 4.8, 4.2, 8.6, 5.6, 4.6, "hull_dark")
box("antenna", 4.0, 4.8, 3.4, 4.2, 6.8, 3.6, "hull_dark")

# 后缘护栏：横杆 + 两根立柱
box("rail_back", 5.2, 4.85, 3.55, 10.8, 5.05, 3.75, "detail")
for tag, rx in (("l", 5.2), ("r", 10.6)):
    box(f"rail_post_{tag}", rx, 4.8, 3.55, rx + 0.2, 5.45, 3.75, "hull_dark")

# ================= 双联菱形炮管 =================
for tag, cx in (("l", 6.6), ("r", 9.4)):
    # 炮根套筒（原画里靠近炮盾的一段明显加粗）
    diamond(f"barrel_sleeve_{tag}", cx, 3.6, 13.8, 16.2, 0.66, "barrel")
    diamond(f"barrel_{tag}", cx, 3.6, 16.2, 25.0, 0.5, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, 3.6, 25.0, 25.8, 0.62, "barrel")
    # 炮口内膛：装填/空膛差异只在这两个小面
    diamond(f"muzzle_{tag}", cx, 3.6, 25.8, 25.9, 0.34, "barrel",
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
    "gui_light": "front",
    "display": DISPLAY,
}

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
loaded_3d = {
    "parent": "piranport:item/german_twin_203mm_gun_3d",
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
    dump("german_twin_203mm_gun_3d.json", base)
    dump("german_twin_203mm_gun_3d_loaded.json", loaded_3d)
    dump("german_twin_203mm_gun.json", item_entry)
