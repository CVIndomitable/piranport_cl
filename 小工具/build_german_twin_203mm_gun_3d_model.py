#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成德国双联203毫米炮的 3D 手持模型（按 Equip_L_18 原画重新建模）。

建模依据（原画 Equip_L_18.png）：
  - 宽而扁的德式方形炮塔，四边上部有大倒角，屋顶是略窄的平顶；
  - 前脸整体后倾约 22.5°，下部有一圈外凸的下颚板；
  - 两个圆形炮口开在前脸，炮根是明显加粗的阶梯套筒，炮管细长；
  - 屋顶只有两个不对称的测距仪：左后一个圆角矩形盒，右舷后部一个外凸盒；
  - 座圈是带凸缘的三层收分鼓座，炮塔悬出座圈。

技术要点（同 build_japanese_127mm_twin_gun_3d_model.py）：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 旋转只用 ±22.5°/45°；炮管用 45° 菱形截面近似圆管；
  - 没有照搬日本炮的吊篮、护栏、天线等原画里不存在的零件。

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


# ================= 座圈（带凸缘的鼓座） =================
# WHY 不用「方块 + 45° 方块」拼八角：加法元素只能并集，转角会冒出四片尖角。
# 原画座圈大部分被炮塔遮住，三层收分的方鼓座已经能读出圆鼓轮廓。
box("ring_flange", 3.3, 0.0, 3.3, 12.7, 0.45, 12.7, "hull_dark")
box("ring_drum", 3.8, 0.45, 3.8, 12.2, 0.95, 12.2, "hull_dark")
box("ring_lip", 3.55, 0.95, 3.55, 12.45, 1.2, 12.45, "hull")

# ================= 炮塔主体 =================
# 下缘外凸的座圈裙板
box("hull_skirt", 3.3, 1.2, 2.4, 12.7, 2.0, 12.1, "hull")
box("hull_lower", 3.5, 2.0, 2.7, 12.5, 3.1, 12.3, "hull")
box("hull_mid", 3.6, 3.1, 3.0, 12.4, 4.45, 12.0, "hull")

# 两舷上部内倾倒角（原画最明显的特征：上部收进、屋顶变窄）
box("hull_slope_port", 3.46, 2.9, 3.2, 3.74, 4.8, 11.8, "hull",
    rotation={"angle": -22.5, "axis": "z", "origin": [3.6, 3.85, 7.5]})
box("hull_slope_stbd", 12.26, 2.9, 3.2, 12.54, 4.8, 11.8, "hull",
    rotation={"angle": 22.5, "axis": "z", "origin": [12.4, 3.85, 7.5]})

# 屋顶：比炮塔窄一圈，前缘与后缘各有一道倒角
box("roof", 4.15, 4.45, 3.35, 11.85, 4.75, 11.55, "hull")
box("roof_front_bevel", 4.1, 4.35, 11.3, 11.9, 4.6, 11.95, "hull",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.45, 11.6]})
box("roof_rear_bevel", 4.1, 4.35, 2.95, 11.9, 4.6, 3.6, "hull",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 4.45, 3.3]})

# ================= 前脸 =================
# 下部下颚板（原画前脸下缘外凸的一条厚板）
box("chin_plate", 3.75, 1.2, 11.9, 12.25, 2.15, 12.55, "hull_dark")
# 后倾约 22.5° 的主装甲板
box("front_slope", 3.8, 2.05, 11.85, 12.2, 4.6, 12.4, "hull",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 2.05, 12.4]})

# 两个炮口暗板：贴在前脸斜面上，深色方块读作炮口开口
for tag, cx in (("l", 6.5), ("r", 9.5)):
    box(f"gunport_{tag}", cx - 0.95, 2.45, 12.30, cx + 0.95, 4.15, 12.44,
        "hull_dark",
        rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 2.05, 12.4]})

# ================= 后部 =================
box("rear_plate", 3.9, 1.4, 2.25, 12.1, 4.45, 2.7, "hull_dark")
box("rear_top_bevel", 4.0, 4.25, 2.5, 12.0, 4.55, 3.15, "hull",
    rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 4.4, 2.8]})
# 右舷后部的小检修口（原画右下侧那块凸起）
box("side_hatch", 12.46, 1.6, 8.7, 12.62, 2.5, 9.6, "detail")
box("side_hatch_hinge", 12.58, 2.0, 8.75, 12.7, 2.12, 9.55, "hull_dark")

# ================= 屋顶测距仪（原画左右不对称） =================
# 左后：低矮的圆角矩形盒
box("rangefinder_port", 4.55, 4.75, 4.1, 6.75, 5.5, 6.15, "detail")
box("rangefinder_port_cap", 4.7, 5.5, 4.25, 6.6, 5.7, 6.0, "hull_dark")
box("rangefinder_port_bevel", 4.6, 5.35, 5.75, 6.7, 5.55, 6.2, "hull_dark",
    rotation={"angle": -22.5, "axis": "x", "origin": [5.65, 5.45, 5.95]})
# 右舷后部：从侧舷凸出来的方形盒
box("rangefinder_stbd", 11.25, 4.05, 7.5, 12.9, 4.95, 9.7, "detail")
box("rangefinder_stbd_cap", 11.35, 4.95, 7.65, 12.8, 5.15, 9.55, "hull_dark")

# ================= 双联菱形炮管 =================
for tag, cx in (("l", 6.5), ("r", 9.5)):
    # 炮根阶梯套筒：原画里靠炮盾的一段呈锥形加粗，逐级收细
    diamond(f"barrel_sleeve_{tag}", cx, 3.35, 11.6, 13.2, 0.72, "barrel")
    diamond(f"barrel_sleeve_mid_{tag}", cx, 3.35, 13.2, 14.4, 0.6, "barrel")
    diamond(f"barrel_sleeve_front_{tag}", cx, 3.35, 14.4, 15.4, 0.5, "barrel")
    diamond(f"barrel_{tag}", cx, 3.35, 15.4, 25.4, 0.4, "barrel")
    diamond(f"muzzle_ring_{tag}", cx, 3.35, 25.4, 26.0, 0.5, "barrel")
    # 炮口内膛：装填/空膛差异只在这两个小面
    diamond(f"muzzle_{tag}", cx, 3.35, 26.0, 26.1, 0.3, "barrel",
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
