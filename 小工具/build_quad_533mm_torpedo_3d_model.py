#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成四联533毫米鱼雷的 3D 手持模型（按 Equip_L_88 原画从零建模）。

只依据原画本身的形体，不套用其他主炮/鱼雷发射器的布局：
  - 四根等长的并联鱼雷管，管口朝前，管身带多道管箍；
  - 每根管口探出暗红色圆头鱼雷（装填态），空膛时只留黑色管口；
  - 炮架是中央圆鼓底座 + 两舷托架，底座在四管下方露出；
  - 四管上方有一圈矩形顶部导轨与立柱（原画最醒目的识别点）。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，纹理不会被拉伸；
  - 圆管用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱，读作圆柱；
  - 装填/空膛的差异只在雷头：空膛用全透明雷头贴图，露出后面的黑管口；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/quad_533mm_torpedo.json            builtin/entity 物品入口（只放 display）
  models/item/quad_533mm_torpedo_3d.json         空膛 3D 发射器（standalone 烘焙）
  models/item/quad_533mm_torpedo_3d_loaded.json  装填 3D 发射器（换雷头材质）
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "quad_533mm_torpedo_hull",
    "hull_dark": NS + "quad_533mm_torpedo_hull_dark",
    "ring": NS + "quad_533mm_torpedo_ring",
    "tube": NS + "quad_533mm_torpedo_tube",
    "warhead": NS + "quad_533mm_torpedo_warhead",
    "warhead_empty": NS + "quad_533mm_torpedo_warhead_empty",
    "tube_end": NS + "quad_533mm_torpedo_tube_end",
}

elements = []

TUBE_XS = (3.95, 6.65, 9.35, 12.05)
TUBE_Y = 5.1
TUBE_Z0 = 3.6      # 管尾
TUBE_Z1 = 23.2     # 管口（雷头从这里探出）


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面尺寸给 uv 窗口（uv 单位=模型单位；贴图 32px 时 16uv=整图）。

    面宽高与 uv 窗口一致，保证每模型单位恒定 2 像素，纹理不被拉伸。
    """
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    # 20 单位长的鱼雷管侧面超过一张 16uv 贴图，uv 不能越界（会采样到别的贴图），
    # 这里把窗口夹到 16 并接受轻微拉伸；其余短面仍保持 2px/单位。
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

    faces: 覆盖单面材质，如 {"south": "tube_end"}。
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


def octagon(name, cx, cy, z1, z2, inner, outer, tex, faces=None):
    """八棱柱：轴对齐方盒（半宽 inner）+ 45° 旋转方盒（外接半径 outer）。

    z 两端各错开 0.002，避免两个方盒的端面共面 z-fighting。
    """
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """用 5 层嵌套长方体拼出阶梯圆（比 45° 方块并集干净，不会出尖角）。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


# ================= 底座：中央圆鼓 + 两舷托架 =================
round_footprint("base_drum", 8.0, 14.0, 3.9, 0.0, 1.5, "hull_dark")
round_footprint("base_upper", 8.0, 14.0, 3.5, 1.5, 2.3, "hull")
round_footprint("base_column", 8.0, 14.0, 2.4, 2.3, 3.9, "hull_dark")

box("cradle", 3.0, 3.7, 10.0, 13.0, 4.5, 17.6, "hull")
box("cradle_lip_front", 3.1, 4.5, 16.8, 12.9, 4.8, 17.7, "ring")
box("cradle_lip_rear", 3.1, 4.5, 9.9, 12.9, 4.8, 10.8, "ring")

# 两舷托架：把四管夹在中央（原画里管束两侧的立板）
box("cheek_port", 2.6, 4.3, 11.2, 3.3, 6.0, 16.8, "hull_dark")
box("cheek_stbd", 12.7, 4.3, 11.2, 13.4, 6.0, 16.8, "hull_dark")

# ================= 四根并联鱼雷管 =================
for tag, cx in zip("abcd", TUBE_XS):
    # 管身
    octagon(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 0.82, 1.18, "tube")
    # 管尾封板
    octagon(f"tube_rear_{tag}", cx, TUBE_Y, TUBE_Z0 - 0.1, TUBE_Z0 + 0.05,
            0.80, 1.16, "hull_dark")
    # 管箍
    for i, z in enumerate((7.2, 12.8, 18.4)):
        octagon(f"tube_ring_{tag}_{i}", cx, TUBE_Y, z, z + 0.55,
                0.90, 1.30, "ring")
    # 管口：外圈管壁 + 内圈黑膛；黑膛在空膛态露出来
    octagon(f"tube_mouth_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.35, TUBE_Z1,
            0.88, 1.24, "ring")
    octagon(f"tube_end_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.02, TUBE_Z1 + 0.04,
            0.82, 1.18, "tube_end")
    # 装填态雷头：三段收细的暗红圆头（空膛时整组贴图透明）
    octagon(f"warhead_base_{tag}", cx, TUBE_Y, TUBE_Z1 + 0.04,
            TUBE_Z1 + 0.90, 0.78, 1.12, "warhead")
    octagon(f"warhead_mid_{tag}", cx, TUBE_Y, TUBE_Z1 + 0.90,
            TUBE_Z1 + 1.85, 0.60, 0.86, "warhead")
    octagon(f"warhead_tip_{tag}", cx, TUBE_Y, TUBE_Z1 + 1.85,
            TUBE_Z1 + 2.55, 0.36, 0.52, "warhead")

# ================= 顶部矩形导轨框（原画识别点） =================
for side, x in (("port", 3.05), ("stbd", 12.55)):
    box(f"rail_{side}", x, 6.5, 5.4, x + 0.4, 6.9, 22.0, "ring")
    for i, z in enumerate((6.4, 12.4, 17.9)):
        box(f"rail_post_{side}_{i}", x, 5.6, z, x + 0.4, 6.5, z + 0.4,
            "hull_dark")
for i, z in enumerate((6.2, 12.2, 17.7)):
    box(f"rail_cross_{i}", 3.05, 6.45, z, 12.95, 6.85, z + 0.45, "hull_dark")

# 后部小观瞄具
box("sight_base", 7.55, 6.9, 8.1, 8.45, 7.15, 9.0, "hull_dark")
box("sight_head", 7.7, 7.15, 8.25, 8.3, 7.55, 8.85, "hull")

# 手持时绕 Y 转 180°，让管口朝角色前方
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.60, 0.60, 0.60],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.60, 0.60, 0.60],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.55, 0.55, 0.55],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.55, 0.55, 0.55],
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
        "ring": TEX["ring"],
        "tube": TEX["tube"],
        "warhead": TEX["warhead_empty"],
        "warhead_empty": TEX["warhead_empty"],
        "tube_end": TEX["tube_end"],
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
    "parent": "piranport:item/quad_533mm_torpedo_3d",
    "textures": {
        "warhead": TEX["warhead"],
    },
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("quad_533mm_torpedo_3d.json", base)
    dump("quad_533mm_torpedo_3d_loaded.json", loaded_3d)
    dump("quad_533mm_torpedo.json", item_entry)
    # 高速型是同一套发射器，共用模型与渲染器
    dump("quad_533mm_torpedo_ur_high_speed.json", item_entry)
