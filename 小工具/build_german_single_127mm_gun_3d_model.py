#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「德国单装127毫米炮」的 3D 手持模型（按 Equip_L_5 原画从零建模）。

形体要点（只依据原画）：
  - 方正、带斜顶的敞背炮座，前角切角，后部一个大的深色开口；
  - 前脸是垂直面，中央深色炮口，两侧各一块浅色凸起方板；
  - 顶盖近白灰，后缘有一道小台阶；
  - 单根细长炮管，深色炮根套筒。

产出：models/item/german_single_127mm_gun{,_3d,_3d_loaded}.json
"""
import json
import os

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
NS = "piranport:item/"
ID = "german_single_127mm_gun"
TEX = {k: NS + ID + "_" + v for k, v in {
    "hull": "hull", "roof": "roof", "hull_dark": "hull_dark", "detail": "detail",
    "interior": "interior", "barrel": "barrel", "muzzle_empty": "muzzle_empty",
    "muzzle_loaded": "muzzle_loaded"}.items()}

elements = []


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    ou = (seed * 3.7 + len(face) * 1.3) % (max(0.0, 16.0 - w) + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max(0.0, 16.0 - h) + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None, full_uv_faces=()):
    fj = {}
    seed = len(elements) + 1
    for face in ("north", "south", "west", "east", "up", "down"):
        ft = (faces or {}).get(face, tex)
        e = {"texture": "#" + ft}
        e["uv"] = [0, 0, 16, 16] if face in full_uv_faces else _uv_for(face, x1, y1, z1, x2, y2, z2, seed)
        fj[face] = e
    el = {"name": name, "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
          "to": [round(x2, 3), round(y2, 3), round(z2, 3)], "faces": fj}
    if rotation:
        el["rotation"] = rotation
    elements.append(el)


def diamond(name, cx, cy, z1, z2, half, tex, faces=None, full_uv_faces=()):
    box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex, faces=faces,
        full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def round_footprint(name, cx, cz, radius, y1, y2, tex):
    for i, (fx, fz) in enumerate(((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
                                  (0.70, 0.92), (0.50, 1.00))):
        box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
            cx + radius * fx, y2, cz + radius * fz, tex)


def chamfer(name, cx, cz, half_len, thickness, y1, y2, angle, tex):
    box(name, cx - thickness / 2, y1, cz - half_len / 2,
        cx + thickness / 2, y2, cz + half_len / 2, tex,
        rotation={"angle": angle, "axis": "y", "origin": [cx, (y1 + y2) / 2, cz]})


CX, CZ = 8.0, 7.8

# 座圈
round_footprint("base_flange", CX, CZ, 3.65, 0.0, 0.35, "hull_dark")
round_footprint("base_drum", CX, CZ, 3.40, 0.35, 0.90, "hull_dark")
round_footprint("base_lip", CX, CZ, 3.50, 0.90, 1.05, "detail")

# 方正炮座
box("body_lower", 3.90, 1.05, 3.90, 12.10, 3.50, 11.60, "hull")
box("body_upper", 4.10, 3.50, 4.10, 11.90, 4.20, 11.50, "hull")
# 前角切角
chamfer("chamfer_front_right", 11.55, 11.05, 1.70, 0.55, 1.05, 4.20, -45, "hull")
chamfer("chamfer_front_left", 4.45, 11.05, 1.70, 0.55, 1.05, 4.20, 45, "hull")

# 顶盖 + 斜前檐 + 后部台阶
box("roof", 4.00, 4.20, 4.00, 12.00, 4.48, 11.45, "roof")
box("roof_front_slope", 4.10, 4.05, 10.75, 11.90, 4.45, 11.55, "roof",
    rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.20, 11.15]})
box("roof_step", 4.45, 4.48, 4.10, 11.55, 4.68, 4.85, "roof")
box("roof_step_edge", 4.55, 4.68, 4.15, 11.45, 4.76, 4.80, "detail")

# 后部大开口（深色内腔 + 浅色边框）
box("rear_frame_left", 4.55, 1.35, 3.86, 4.85, 3.55, 3.98, "detail")
box("rear_frame_right", 11.15, 1.35, 3.86, 11.45, 3.55, 3.98, "detail")
box("rear_frame_top", 4.85, 3.35, 3.86, 11.15, 3.55, 3.98, "detail")
box("rear_frame_bottom", 4.85, 1.35, 3.86, 11.15, 1.55, 3.98, "detail")
box("rear_interior", 4.85, 1.55, 3.90, 11.15, 3.35, 3.96, "interior")

# 前脸：炮口 + 两块凸起方板
box("port_outer", 7.00, 1.95, 11.55, 9.00, 3.80, 11.78, "detail")
box("port_inner", 7.25, 2.15, 11.78, 8.75, 3.60, 11.86, "interior")
box("pad_left", 5.30, 2.90, 11.60, 6.40, 3.70, 11.78, "detail")
box("pad_right", 9.60, 2.90, 11.60, 10.70, 3.70, 11.78, "detail")

# 炮管
BY = 2.85
diamond("barrel_sleeve", CX, BY, 10.75, 11.80, 0.45, "hull_dark")
diamond("barrel_step", CX, BY, 11.80, 12.50, 0.34, "barrel")
diamond("barrel", CX, BY, 12.50, 23.60, 0.24, "barrel")
diamond("barrel_ring", CX, BY, 15.00, 15.40, 0.30, "barrel")
diamond("muzzle_ring", CX, BY, 23.35, 23.95, 0.32, "barrel")
diamond("muzzle", CX, BY, 23.95, 24.05, 0.24, "barrel",
        faces={"south": "muzzle_empty"}, full_uv_faces=("south",))

DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                              "scale": [0.65, 0.65, 0.65]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                             "scale": [0.65, 0.65, 0.65]},
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                              "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                             "scale": [0.6, 0.6, 0.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

base = {
    "parent": "minecraft:block/block", "gui_light": "front", "ambientocclusion": False,
    "textures": {"particle": TEX["hull"], **TEX},
    "elements": elements,
}
item_entry = {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}
loaded_3d = {"parent": "piranport:item/" + ID + "_3d",
             "textures": {"muzzle_empty": TEX["muzzle_loaded"]}}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump(ID + "_3d.json", base)
    dump(ID + "_3d_loaded.json", loaded_3d)
    dump(ID + ".json", item_entry)
