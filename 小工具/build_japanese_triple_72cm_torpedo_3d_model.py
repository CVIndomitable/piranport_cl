#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成日本72厘米三连装鱼雷的 3D 手持模型（按 Equip_L_225 原画从零建模）。

只依据原画本身的形体：
  - 3 根粗大的鱼雷管并排，管口朝前；管身浅蓝灰、带分段接缝，
    雷头前有一圈白色识别亮带，最前面是近黑的圆头（空膛态换成暗内膛）；
  - 管尾有厚实的雷尾块，后段上方横着一块深色支架；
  - 后方是安装座：大圆盘底座 + 圆鼓立柱 + 托架；
  - 托架上有向前上斜伸的支撑臂、跨过管束的横杆，以及右侧的小控制箱，
    箱旁立着手轮（水平轮盘 + 立柱）。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，长面 uv 夹到 16，纹理不会被拉伸；
  - 圆管/圆鼓用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 装填/空膛的差异只在雷头/管口端盖；
  - 模型中心在 x=8，雷头朝 +Z，物品 display 旋转 180° 后朝角色前方。
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_ROOT = os.path.join(ASSETS, "textures")

NS = "piranport:item/"
ID = "japanese_triple_72cm_torpedo"
PREFIX = ID + "_"
TEX = {
    "hull": NS + PREFIX + "hull",
    "hull_dark": NS + PREFIX + "hull_dark",
    "ring": NS + PREFIX + "ring",
    "tube": NS + PREFIX + "tube",
    "detail": NS + PREFIX + "detail",
    "cap": NS + PREFIX + "cap_empty",
    "cap_loaded": NS + PREFIX + "cap_loaded",
}

VALID_ANGLES = (-45.0, -22.5, 0.0, 22.5, 45.0)

elements = []

# 三根管：X 向等距，管心 y 一致
TUBE_XS = (5.1, 8.0, 10.9)
TUBE_Y = 4.35
TUBE_Z0 = -3.2      # 管身起点（雷尾块在前，管身在 -3.2 之后）
TUBE_Z1 = 12.4      # 管身终点（白带/雷头在前）
NOSE_Z = (12.8, 13.6, 14.35)   # 雷头三段收口的位置


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面的实际尺寸取 uv，长边夹到 16（32px 贴图 / 每单位 2px）。"""
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    w, h = min(w, 16.0), min(h, 16.0)
    max_u = 16.0 - w
    max_v = 16.0 - h
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
        full_uv_faces=()):
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


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex):
    """轴向沿 Z 的八棱柱：轴对齐方盒 + 绕 Z 转 45° 的方盒。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def cylinder_y(name, cx, cz, y1, y2, inner, outer, tex):
    """轴向沿 Y 的八棱柱（立柱、轮盘）。"""
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


# ================= 安装座 =================
cylinder_y("base_disc", 8.0, -1.6, 0.0, 0.45, 4.5, 5.3, "hull_dark")
cylinder_y("pedestal", 8.0, -3.45, 0.45, 2.0, 1.35, 1.95, "hull_dark")
box("cradle", 4.3, 2.0, -5.4, 11.7, 2.75, -0.8, "hull_dark")

# ================= 三根鱼雷管 =================
for tag, cx in zip("abc", TUBE_XS):
    # 管身 + 管尾块
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1, 1.27, 1.45, "tube")
    box(f"breech_{tag}", cx - 1.25, 3.1, -5.1, cx + 1.25, 5.6, -3.4, "hull")
    box(f"breech_cap_{tag}", cx - 1.05, 3.3, -5.45, cx + 1.05, 5.4, -5.1,
        "detail")
    octagon_z(f"breech_collar_{tag}", cx, TUBE_Y, -3.5, -3.15, 1.32, 1.52,
              "detail")
    # 管身分段接缝（细箍）
    for i, z in enumerate((0.3, 4.4, 8.0)):
        octagon_z(f"seam_{tag}_{i}", cx, TUBE_Y, z, z + 0.30, 1.30, 1.50,
                  "detail")
    # 雷头后的白色识别亮带（紧贴雷头）
    octagon_z(f"white_band_{tag}", cx, TUBE_Y, 11.6, 12.4, 1.30, 1.50, "ring")
    # 雷头：三段收口的近黑圆头（空膛态整段换成暗内膛唇口）
    octagon_z(f"nose_collar_{tag}", cx, TUBE_Y, TUBE_Z1, NOSE_Z[0],
              1.32, 1.50, "detail")
    for i, (z0, z1, r_in, r_out) in enumerate((
            (NOSE_Z[0], NOSE_Z[1], 1.28, 1.45),
            (NOSE_Z[1], NOSE_Z[2], 0.98, 1.12),
            (NOSE_Z[2], NOSE_Z[2] + 0.50, 0.62, 0.72))):
        octagon_z(f"nose_{tag}_{i}", cx, TUBE_Y, z0, z1, r_in, r_out, "cap")

# ================= 后部支架 =================
box("rear_bracket", 5.6, 5.85, -5.0, 10.4, 7.5, -2.6, "hull_dark")
box("rear_bracket_top", 5.8, 7.5, -4.8, 10.2, 7.7, -2.8, "detail")

# 支撑臂：从托架向前上斜伸到管束两侧（绕 X 转 22.5° 做出倾斜）
# 放在管束外侧，否则会埋进最左/最右那根管子里
for tag, x1, x2 in (("l", 2.9, 3.7), ("r", 12.3, 13.1)):
    box(f"arm_{tag}", x1, 2.1, -2.9, x2, 5.9, -2.2, "detail",
        rotation={"angle": 22.5, "axis": "x",
                  "origin": [(x1 + x2) / 2, 4.0, -2.55]})
box("arm_c", 7.6, 2.1, -2.9, 8.4, 3.2, -2.2, "detail")
# 跨过管束的横杆 + 两端立柱
box("cross_bar", 3.4, 6.0, -1.9, 12.6, 6.3, -1.55, "detail")
box("post_l", 3.4, 5.4, -1.9, 3.7, 6.3, -1.55, "detail")
box("post_r", 12.3, 5.4, -1.9, 12.6, 6.3, -1.55, "detail")

# ================= 手轮（右侧立柱 + 水平轮盘） =================
# 立在小控制箱顶面、后段支架之后，既避开管子也避开支撑臂，正面能看见
box("wheel_post", 12.75, 2.3, -4.75, 13.05, 4.1, -4.45, "detail")
cylinder_y("wheel", 12.9, -4.6, 4.1, 4.28, 0.62, 0.78, "ring")
cylinder_y("wheel_hub", 12.9, -4.6, 4.28, 4.44, 0.22, 0.32, "detail")

# ================= 右侧小控制箱 =================
box("control_box", 10.6, 0.5, -5.6, 13.6, 2.1, -2.9, "hull")
box("control_lid", 10.45, 2.1, -5.75, 13.75, 2.3, -2.75, "detail")
box("control_knob", 11.0, 2.3, -4.8, 12.2, 2.7, -3.7, "hull_dark")

DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0], "translation": [0, 2, -1],
        "scale": [0.58, 0.58, 0.58],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0], "translation": [0, 2, -1],
        "scale": [0.58, 0.58, 0.58],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0], "translation": [0, 0, 0],
        "scale": [0.52, 0.52, 0.52],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0], "translation": [0, 0, 0],
        "scale": [0.52, 0.52, 0.52],
    },
    "ground": {
        "rotation": [0, 0, 0], "translation": [0, 2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7],
             "scale": [1, 1, 1]},
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
        "detail": TEX["detail"],
        "cap": TEX["cap"],
        "cap_loaded": TEX["cap_loaded"],
    },
    "elements": elements,
}

item_entry = {
    "parent": "builtin/entity",
    "gui_light": "front",
    "display": DISPLAY,
}

loaded_3d = {
    "parent": "piranport:item/" + ID + "_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def validate():
    """写出前自检：from<to、rotation 合法、uv 在 0..16、贴图文件存在。"""
    for el in elements:
        name = el["name"]
        for i, axis in enumerate("xyz"):
            if not el["from"][i] < el["to"][i]:
                raise ValueError(f"{name}: from.{axis} 必须小于 to.{axis}")
            for key in ("from", "to"):
                if not -16.0 <= el[key][i] <= 32.0:
                    raise ValueError(f"{name}: {key}.{axis} 超出 -16..32")
        rot = el.get("rotation")
        if rot and float(rot["angle"]) not in VALID_ANGLES:
            raise ValueError(f"{name}: rotation {rot['angle']} 非法")
        for face, data in el["faces"].items():
            u1, v1, u2, v2 = data["uv"]
            if not (0 <= u1 <= u2 <= 16 and 0 <= v1 <= v2 <= 16):
                raise ValueError(f"{name}.{face}: uv {data['uv']} 越界")
    for ref in TEX.values():
        rel = ref.split(":", 1)[1] + ".png"
        path = os.path.join(TEX_ROOT, rel)
        if not os.path.exists(path):
            raise FileNotFoundError(f"缺少贴图: {path}")
    print(f"校验通过: {len(elements)} 个 element, {len(TEX)} 张贴图")


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    validate()
    dump(ID + "_3d.json", base)
    dump(ID + "_3d_loaded.json", loaded_3d)
    dump(ID + ".json", item_entry)
