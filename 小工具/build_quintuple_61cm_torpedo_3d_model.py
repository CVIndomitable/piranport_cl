#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「61厘米五连装鱼雷」的 3D 手持模型（按 Equip_L_148 原画从零建模）。

只依据原画本身的形体，不套用其他鱼雷发射器的布局：
  - 一座大型圆弧装甲炮座：平面是带大倒角的圆角矩形，立面分三层微微内收，
    顶部一圈外挑檐口 + 略内缩的甲板，甲板四周立着一圈栏杆（立柱 + 上横杆）；
  - 炮座正面上方是一块带 X 形加强筋的大舱板，两侧各一块小方窗板；
  - 炮座侧面（两舷）各有一列梯级把手 + 一扇带合页的舱门；
  - 5 根鱼雷管从炮座正面下沿向前伸出，管身两道管箍，管口是张开的深色圆口；
  - 炮座下方是圆形座板 + 圆鼓底座；管束下方有一副托架和两侧的边梁。

技术要点：
  - 逐面写 uv，贴图 32x32、每模型单位 2px，长面 uv 一律夹到 16 以内，纹理不被拉伸；
  - 圆管/圆鼓用「轴对齐方盒 + 45° 旋转方盒」叠成八棱柱；
  - 圆角平面用「中段整宽盒 + 中段整深盒 + 四角 45° 菱形」并集近似倒角；
  - 装填/空膛的差异只在管口端盖（cap → cap_loaded）；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/quintuple_61cm_torpedo.json              builtin/entity 物品入口（只放 display）
  models/item/quintuple_61cm_torpedo_3d.json           空膛 3D 发射器
  models/item/quintuple_61cm_torpedo_3d_loaded.json    装填 3D 发射器（只覆盖 cap 贴图）
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_ROOT = os.path.join(ASSETS, "textures/item")

NS = "piranport:item/"
ID = "quintuple_61cm_torpedo"
PREFIX = ID + "_"
TEX = {
    "hull": NS + PREFIX + "hull",
    "hull_dark": NS + PREFIX + "hull_dark",
    "ring": NS + PREFIX + "ring",
    "tube": NS + PREFIX + "tube",
    "detail": NS + PREFIX + "detail",
    "cap": NS + PREFIX + "cap_empty",       # 空膛态：近黑管口
    "cap_loaded": NS + PREFIX + "cap_loaded",
}

elements = []

# ---------------------------------------------------------------- 尺寸常量
CX = 8.0                    # 模型横向中心，管口朝 +Z
CHAMFER = 1.5               # 炮座平面倒角半径

BODY_X1, BODY_X2 = 2.2, 13.8
BODY_Z1, BODY_Z2 = 3.0, 12.9     # 炮座前后：正面 z=12.9，鱼雷管从这里伸出

TUBE_Y = 4.6
TUBE_R_IN = 0.58            # 八棱柱内接盒半宽
TUBE_R_OUT = 0.72           # 八棱柱对角半宽（旋转盒决定外轮廓）
TUBE_XS = (4.4, 6.2, 8.0, 9.8, 11.6)
TUBE_Z0 = 8.0               # 尾端藏在炮座里
TUBE_Z1 = 20.0              # 管口

DECK_Y = 8.7                # 甲板面高度
RAIL_TOP = 10.4             # 两舷栏杆顶
RAIL_FRONT = 9.7            # 正面栏杆较矮（原画正面是低矮围栏）


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面的真实尺寸取 uv 子块，并夹到 16 以内，避免纹理被拉伸。"""
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


def octagon_z(name, cx, cy, z1, z2, inner, outer, tex, faces=None):
    """z 轴向八棱柱：方盒 + 45° 旋转盒（对角半宽 outer）。"""
    box(f"{name}_box", cx - inner, cy - inner, z1, cx + inner, cy + inner,
        z2, tex, faces=faces)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.002, cx + half, cy + half,
        z2 - 0.002, tex, faces=faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def octagon_y(name, cx, cz, y1, y2, inner, outer, tex):
    """y 轴向八棱柱：圆鼓/底座用。"""
    box(f"{name}_box", cx - inner, y1, cz - inner, cx + inner, y2, cz + inner,
        tex)
    half = outer / math.sqrt(2.0)
    box(f"{name}_dia", cx - half, y1 + 0.002, cz - half, cx + half, y2 - 0.002,
        cz + half, tex,
        rotation={"angle": 45, "axis": "y",
                  "origin": [cx, (y1 + y2) / 2, cz]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    """扁圆盘：多层递减椭圆的并集，读作圆座板。"""
    steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
             (0.70, 0.92), (0.50, 1.00))
    for i, (fx, fz) in enumerate(steps):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


def chamfered_plan(name, x1, x2, z1, z2, r, y1, y2, tex, full_uv_up=False):
    """圆角平面棱柱：中段整宽 + 中段整深 + 四角 45° 菱形，并集近似倒角。"""
    box(f"{name}_mid_x", x1, y1, z1 + r, x2, y2, z2 - r, tex,
        full_uv_faces=("up",) if full_uv_up else ())
    box(f"{name}_mid_z", x1 + r, y1, z1, x2 - r, y2, z2, tex,
        full_uv_faces=("up",) if full_uv_up else ())
    half = r / math.sqrt(2.0)
    for tag, cx, cz in (("fl", x1 + r, z2 - r), ("fr", x2 - r, z2 - r),
                        ("bl", x1 + r, z1 + r), ("br", x2 - r, z1 + r)):
        box(f"{name}_c{tag}", cx - half, y1, cz - half, cx + half, y2, cz + half,
            tex, full_uv_faces=("up",) if full_uv_up else (),
            rotation={"angle": 45, "axis": "y",
                      "origin": [cx, (y1 + y2) / 2, cz]})


# ================================================== 底座：圆座板 + 圆鼓
round_footprint("base_plate", CX, 8.4, 5.6, 0.0, 0.36, "hull_dark")
round_footprint("base_rim", CX, 8.4, 4.95, 0.36, 0.52, "ring")
octagon_y("base_drum_lo", CX, 8.4, 0.52, 1.45, 3.45, 4.05, "hull_dark")
octagon_y("base_drum_hi", CX, 8.4, 1.45, 2.35, 3.15, 3.75, "hull_dark")

# ================================================== 炮座本体（三层内收 + 檐口 + 甲板）
# 最下层承担鱼雷管出口，正面在 z=12.9
chamfered_plan("body_lo", BODY_X1, BODY_X2, BODY_Z1, BODY_Z2, CHAMFER,
               2.0, 5.45, "hull")
# 中段微微内收，正面退后 0.15：正好在鱼雷管上方形成一道挑檐
chamfered_plan("body_mid", BODY_X1 + 0.15, BODY_X2 - 0.15,
               BODY_Z1 + 0.15, BODY_Z2 - 0.15, CHAMFER,
               5.45, 7.3, "hull")
# 外挑檐口
chamfered_plan("body_eave", BODY_X1 - 0.1, BODY_X2 + 0.1,
               BODY_Z1 - 0.1, BODY_Z2 + 0.1, CHAMFER,
               7.3, 8.15, "hull")
# 甲板面（略内缩，顶面整幅 uv 让拼板纹连续）
chamfered_plan("deck", 2.4, 13.6, 3.2, 12.7, 2.0,
               8.15, DECK_Y, "hull", full_uv_up=True)

# ================================================== 正面舱板 / X 加强筋 / 方窗
box("front_panel", 5.9, 5.85, 12.9, 10.1, 7.05, 13.04, "hull")
box("front_panel_top", 5.66, 7.05, 12.9, 10.34, 7.3, 13.08, "detail")
box("front_panel_bot", 5.66, 5.6, 12.9, 10.34, 5.85, 13.08, "detail")
box("front_panel_l", 5.66, 5.6, 12.9, 5.9, 7.3, 13.08, "detail")
box("front_panel_r", 10.1, 5.6, 12.9, 10.34, 7.3, 13.08, "detail")
for tag, ang in (("a", 22.5), ("b", -22.5)):
    box(f"front_x_{tag}", 6.7, 6.29, 13.04, 9.3, 6.61, 13.12, "detail",
        rotation={"angle": ang, "axis": "z", "origin": [CX, 6.45, 13.08]})
box("front_hatch_l", 4.6, 6.15, 12.9, 5.75, 6.95, 13.0, "detail")
box("front_hatch_r", 10.25, 6.15, 12.9, 11.4, 6.95, 13.0, "detail")

# 甲板前部的小舱盖 + 舱口围板
box("deck_hatch", 6.9, DECK_Y, 9.3, 9.1, 9.15, 10.9, "hull")
box("deck_hatch_rim", 6.75, 9.15, 9.15, 9.25, 9.3, 11.05, "detail")

# ================================================== 栏杆：立柱 + 上横杆
POST = 0.16
RAIL_XW1, RAIL_XW2 = 2.62, 2.78          # 左舷立柱截面
RAIL_XE1, RAIL_XE2 = 13.22, 13.38        # 右舷立柱截面
RAIL_ZB1, RAIL_ZB2 = 3.42, 3.58          # 后端立柱截面
RAIL_ZF1, RAIL_ZF2 = 12.32, 12.48        # 正面立柱截面

# 两舷上横杆
for tag, x1, x2 in (("w", RAIL_XW1, RAIL_XW2), ("e", RAIL_XE1, RAIL_XE2)):
    box(f"rail_side_{tag}", x1, RAIL_TOP - POST, 3.42, x2, RAIL_TOP, 12.48,
        "ring")
# 后端上横杆
box("rail_back", RAIL_XW1, RAIL_TOP - POST, RAIL_ZB1,
    RAIL_XE2, RAIL_TOP, RAIL_ZB2, "ring")
# 正面横杆更低一档，读作原画里低矮的前围栏
box("rail_front", RAIL_XW1, RAIL_FRONT - POST, RAIL_ZF1,
    RAIL_XE2, RAIL_FRONT, RAIL_ZF2, "ring")

# 立柱：四角由两舷立柱兼任，正/背面只加中间三根
for i, z in enumerate((3.42, 5.6, 8.0, 10.4, 12.32)):
    box(f"post_w_{i}", RAIL_XW1, DECK_Y, z, RAIL_XW2, RAIL_TOP, z + POST, "ring")
    box(f"post_e_{i}", RAIL_XE1, DECK_Y, z, RAIL_XE2, RAIL_TOP, z + POST, "ring")
for i, x in enumerate((5.0, 8.0, 11.0)):
    box(f"post_b_{i}", x, DECK_Y, RAIL_ZB1, x + POST, RAIL_TOP, RAIL_ZB2, "ring")
    box(f"post_f_{i}", x, DECK_Y, RAIL_ZF1, x + POST, RAIL_FRONT, RAIL_ZF2,
        "ring")

# ================================================== 两舷细节：梯级把手 + 舱门 + 竖向加强肋
for tag, ox, sgn in (("w", BODY_X1, -1), ("e", BODY_X2, 1)):
    def _side(depth):
        """按舷别生成贴在侧面外表面上的薄盒 x 区间。"""
        return sorted((ox, ox + depth * sgn))

    # 竖向装甲肋：只在平直侧面范围（倒角区避开）
    for i, z in enumerate((5.35, 10.4)):
        xa, xb = _side(0.14)
        box(f"rib_{tag}_{i}", xa, 2.2, z, xb, 5.35, z + 0.18, "detail")
    # 梯级把手：五道横向短杆，靠后
    for i in range(5):
        y = 2.75 + i * 0.55
        xa, xb = _side(0.24)
        box(f"rung_{tag}_{i}", xa, y, 5.75, xb, y + 0.22, 7.15, "detail")
    # 舱门：带合页的整块薄板
    xa, xb = _side(0.15)
    box(f"door_{tag}", xa, 2.35, 8.2, xb, 5.15, 9.7, "detail")

# ================================================== 鱼雷管（5 根）+ 托架
for tag, cx in zip("abcde", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_Z0, TUBE_Z1,
              TUBE_R_IN, TUBE_R_OUT, "tube")
    for i, z in enumerate((14.2, 17.4)):
        octagon_z(f"tube_ring_{tag}_{i}", cx, TUBE_Y, z, z + 0.34,
                  TUBE_R_IN + 0.05, TUBE_R_OUT + 0.06, "ring")
    # 管口：外圈管箍 + 端盖（空膛态是近黑圆口，装填态换浅金属封盖）
    octagon_z(f"mouth_{tag}", cx, TUBE_Y, TUBE_Z1 - 0.42, TUBE_Z1,
              TUBE_R_IN + 0.07, TUBE_R_OUT + 0.08, "ring")
    octagon_z(f"cap_{tag}", cx, TUBE_Y, TUBE_Z1, TUBE_Z1 + 0.32,
              TUBE_R_IN - 0.02, TUBE_R_OUT - 0.03, "cap")

# 管束下方的托架 + 两侧边梁 + 前端横梁
box("cradle", 3.05, 2.3, 12.4, 12.95, 4.05, 17.5, "hull_dark")
for tag, x1, x2 in (("w", 2.55, 3.0), ("e", 13.0, 13.45)):
    box(f"cradle_beam_{tag}", x1, 2.5, 12.4, x2, 4.15, 18.1, "hull_dark")
box("cradle_cross", 2.65, 2.65, 18.1, 13.35, 3.25, 18.65, "detail")
box("cradle_ram", 4.4, 3.05, 17.5, 11.6, 3.8, 18.3, "detail")

# ================================================== 输出
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
    "parent": f"piranport:item/{ID}_3d",
    "textures": {"cap": TEX["cap_loaded"]},
}


def validate(data):
    """建模自检：from < to、旋转角度合法、uv 全部落在 0..16、贴图存在。"""
    valid_angles = (-45.0, -22.5, 0.0, 22.5, 45.0)
    for el in data.get("elements", []):
        name = el.get("name", "?")
        for i in range(3):
            assert el["from"][i] < el["to"][i], f"{name}: from/to 非法 {i}"
        rot = el.get("rotation")
        if rot:
            assert rot["axis"] in ("x", "y", "z"), f"{name}: 旋转轴非法"
            assert float(rot["angle"]) in valid_angles, \
                f"{name}: 旋转角度 {rot['angle']} 非法"
        for face, entry in el["faces"].items():
            uv = entry.get("uv")
            if uv:
                assert all(0.0 <= v <= 16.0 for v in uv), f"{name}.{face}: uv 越界 {uv}"
                assert uv[0] < uv[2] and uv[1] < uv[3], f"{name}.{face}: uv 退化 {uv}"
    for key, ref in data["textures"].items():
        if key == "particle" or not ref.startswith("piranport:"):
            continue
        rel = ref.split(":", 1)[1]
        if rel.startswith("item/"):
            rel = rel[len("item/"):]
        path = os.path.join(TEX_ROOT, rel + ".png")
        assert os.path.exists(path), f"贴图缺失: {path}"


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    validate(base)
    print(f"自检通过：{len(elements)} 个 element")
    dump(f"{ID}_3d.json", base)
    dump(f"{ID}_3d_loaded.json", loaded_3d)
    dump(f"{ID}.json", item_entry)
