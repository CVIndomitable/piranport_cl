#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成日本12厘米单装炮的 3D 手持模型（按 Equip_L_2 原画从零建模）。

只依据原画本身的形体，不套用其他炮塔的布局：
  - 圆润的八角形炮盾：中段最宽、上下收，四周大倒角；
  - 顶盖是一整块略亮的平面，肩部再收一层；
  - 前脸偏左是一道竖长的深色炮口槽，炮管从槽的上部伸出；
  - 右后顶上一组深色瞄准具：方盒 + 前伸圆镜 + 右侧横杆；
  - 座圈是近黑色的矮圆鼓，带一圈法兰。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 八角形用「中心带 + 前后条 + 左右条 + 四条 45° 倒角板」拼出；
  - 圆鼓座圈用多层嵌套方块拼出阶梯圆，不做 45° 并集（会冒出尖角）；
  - 炮管用绕 Z 轴 45° 的菱形截面，低分辨率下读作圆管；
  - 模型坐标以 (8, 7.8) 为炮塔中心，炮口朝 +Z，物品 display 旋转 180° 后朝前。

产出：
  models/item/single_small_gun.json            builtin/entity 物品入口（只放 display）
  models/item/single_small_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/single_small_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "single_small_gun_hull",
    "roof": NS + "single_small_gun_roof",
    "hull_dark": NS + "single_small_gun_hull_dark",
    "detail": NS + "single_small_gun_detail",
    "dark": NS + "single_small_gun_dark",
    "barrel": NS + "single_small_gun_barrel",
    "port": NS + "single_small_gun_port",
    "muzzle_empty": NS + "single_small_gun_muzzle_empty",
    "muzzle_loaded": NS + "single_small_gun_muzzle_loaded",
}

CX, CZ = 8.0, 7.8
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


def corner_slab(name, mx, mz, length, thickness, y1, y2, tex, angle):
    """绕 Y 转 ±45° 的斜板，用来补八角形的四个倒角。

    WHY：左右两对角的倒角线方向相反，旋转角必须跟着翻；
    统一用 +45 会把其中两个角转到另一条对角线上，模型边缘冒出方角。
    """
    box(name, mx - thickness / 2, y1, mz - length / 2,
        mx + thickness / 2, y2, mz + length / 2, tex,
        rotation={"angle": angle, "axis": "y",
                  "origin": [mx, (y1 + y2) / 2, mz]})


def octagon(prefix, y1, y2, hw, hd, chamfer, tex):
    """八角形柱体层：中心带 + 前后条 + 左右条 + 四条 45° 倒角板。"""
    # 中心带：整宽，前后各缩进倒角
    box(f"{prefix}_mid", CX - hw, y1, CZ - hd + chamfer,
        CX + hw, y2, CZ + hd - chamfer, tex)
    # 前后两条：宽度缩进倒角
    box(f"{prefix}_front", CX - hw + chamfer, y1, CZ + hd - chamfer,
        CX + hw - chamfer, y2, CZ + hd, tex)
    box(f"{prefix}_back", CX - hw + chamfer, y1, CZ - hd,
        CX + hw - chamfer, y2, CZ - hd + chamfer, tex)
    # 左右两条
    box(f"{prefix}_left", CX - hw, y1, CZ - hd + chamfer,
        CX - hw + chamfer, y2, CZ + hd - chamfer, tex)
    box(f"{prefix}_right", CX + hw - chamfer, y1, CZ - hd + chamfer,
        CX + hw, y2, CZ + hd - chamfer, tex)
    # 四个倒角：中心沿外法线内推半个板厚，外表面正好落在倒角线上
    for tag, sx, sz in (("fr", 1, 1), ("fl", -1, 1),
                        ("br", 1, -1), ("bl", -1, -1)):
        mx = CX + sx * (hw - chamfer / 2)
        mz = CZ + sz * (hd - chamfer / 2)
        # 斜板两端各多探 0.06、厚度加大到 0.60：让倒角与相邻条块充分交叠，
        # 否则层与层之间会露出背后一层的深色小三角。
        thickness = 0.60
        shift = (thickness / 2) / (2 ** 0.5)
        # fr / bl 的倒角线沿 (1,-1)，fl / br 沿 (1,1)：旋转角随之取 -45 / +45。
        angle = -45 if sx * sz > 0 else 45
        corner_slab(f"{prefix}_chamfer_{tag}", mx - sx * shift, mz - sz * shift,
                    chamfer * (2 ** 0.5) + 0.12, thickness, y1, y2, tex, angle)


# ================= 座圈：近黑色矮圆鼓 =================
round_footprint("ring_flange", CX, CZ, 3.20, 0.0, 0.42, "dark")
round_footprint("ring_drum", CX, CZ, 2.85, 0.42, 1.05, "dark")
round_footprint("ring_lip", CX, CZ, 2.95, 1.05, 1.30, "hull_dark")

# ================= 八角形炮盾：中段最宽，向上分多层收成圆顶 =================
octagon("body_lower", 1.25, 3.10, 3.55, 3.45, 1.00, "hull")
octagon("body_bulge", 3.10, 5.00, 4.20, 4.05, 1.30, "hull")
octagon("body_upper", 5.00, 5.80, 4.05, 3.90, 1.20, "hull")
octagon("body_upper2", 5.80, 6.45, 3.85, 3.70, 1.15, "hull")
octagon("body_upper3", 6.45, 6.95, 3.55, 3.40, 1.10, "hull")
octagon("body_shoulder", 6.95, 7.25, 3.15, 3.00, 1.00, "hull")
octagon("body_top", 7.25, 7.50, 2.75, 2.60, 0.90, "roof")

# ================= 前脸偏左的竖长炮口槽（落在最宽的中段前脸） =================
# 用四条边框围出一个真正的开口，中央的深色板略缩在框后，读作凹进去的炮口槽。
PORT_CX = 6.30
FRAME_X1, FRAME_X2 = PORT_CX - 1.00, PORT_CX + 1.00
FRAME_Y1, FRAME_Y2 = 3.10, 5.00
BORDER = 0.22
box("mantlet_left", FRAME_X1, FRAME_Y1, 11.80,
    FRAME_X1 + BORDER, FRAME_Y2, 12.02, "hull_dark")
box("mantlet_right", FRAME_X2 - BORDER, FRAME_Y1, 11.80,
    FRAME_X2, FRAME_Y2, 12.02, "hull_dark")
box("mantlet_bottom", FRAME_X1 + BORDER, FRAME_Y1, 11.80,
    FRAME_X2 - BORDER, FRAME_Y1 + BORDER, 12.02, "hull_dark")
box("mantlet_top", FRAME_X1 + BORDER, FRAME_Y2 - BORDER, 11.80,
    FRAME_X2 - BORDER, FRAME_Y2, 12.02, "hull_dark")
box("mantlet_top_edge", FRAME_X1 + BORDER, FRAME_Y2 - 0.10, 11.80,
    FRAME_X2 - BORDER, FRAME_Y2, 12.04, "detail")
box("port_recess", FRAME_X1 + BORDER, FRAME_Y1 + BORDER, 11.82,
    FRAME_X2 - BORDER, FRAME_Y2 - BORDER, 11.92, "port")

# ================= 炮管 =================
BARREL_Y = 4.30
diamond("barrel_sleeve", PORT_CX, BARREL_Y, 11.60, 12.85, 0.48, "hull_dark")
diamond("barrel_step", PORT_CX, BARREL_Y, 12.85, 13.60, 0.36, "barrel")
diamond("barrel", PORT_CX, BARREL_Y, 13.60, 23.85, 0.28, "barrel")
diamond("barrel_ring_a", PORT_CX, BARREL_Y, 15.15, 15.55, 0.34, "barrel")
diamond("barrel_ring_b", PORT_CX, BARREL_Y, 18.25, 18.65, 0.34, "barrel")
diamond("muzzle_ring", PORT_CX, BARREL_Y, 23.60, 24.10, 0.34, "barrel")
# 炮口内膛：装填/空膛差异只在这一个小面
diamond("muzzle", PORT_CX, BARREL_Y, 24.10, 24.20, 0.22, "barrel",
        faces={"south": "muzzle_empty"}, full_uv_faces=("south",))

# ================= 右后顶上的深色瞄准具 =================
box("gear_neck", 10.55, 6.10, 3.95, 12.35, 6.60, 5.45, "hull_dark")
box("gear_box", 10.40, 6.60, 3.35, 12.95, 7.55, 5.30, "dark")
box("gear_box_top", 10.50, 7.55, 3.45, 12.85, 7.67, 5.20, "detail")
box("gear_box_stripe", 10.50, 6.75, 5.30, 12.85, 7.42, 5.38, "hull_dark")
diamond("gear_scope", 11.65, 7.08, 5.30, 5.95, 0.46, "dark")
box("gear_arm", 12.95, 6.85, 3.95, 14.10, 7.17, 4.40, "dark")
box("gear_arm_post", 13.55, 7.17, 3.90, 14.10, 7.67, 4.45, "dark")
box("gear_arm_foot", 13.20, 6.65, 4.15, 13.55, 7.17, 4.50, "hull_dark")

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
        "roof": TEX["roof"],
        "hull_dark": TEX["hull_dark"],
        "detail": TEX["detail"],
        "dark": TEX["dark"],
        "barrel": TEX["barrel"],
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
    "parent": "piranport:item/single_small_gun_3d",
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
    dump("single_small_gun_3d.json", base)
    dump("single_small_gun_3d_loaded.json", loaded_3d)
    dump("single_small_gun.json", item_entry)
