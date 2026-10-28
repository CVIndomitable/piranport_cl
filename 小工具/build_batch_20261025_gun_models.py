#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-25 批次 11 门火炮的 3D 手持模型。

每门炮都按对应 zjsnrwiki 原画从零建模，共用同一套 BEWLR 输出格式：
  models/item/<id>.json            builtin/entity 物品入口（只放 display）
  models/item/<id>_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/<id>_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）

形体用「座圈 + 八角/方正炮房 + 圆顶/平顶 + 前脸炮口 + 炮管」拼装；
单装/双联/三联只在炮管位置、炮口宽度和顶部附件上区分，细节按各自原画补。
"""
import json
import os

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
NS = "piranport:item/"
CX, CZ = 8.0, 7.8

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


def chamfer(name, cx, cz, length, thickness, y1, y2, angle, tex):
    box(name, cx - thickness / 2, y1, cz - length / 2,
        cx + thickness / 2, y2, cz + length / 2, tex,
        rotation={"angle": angle, "axis": "y", "origin": [cx, (y1 + y2) / 2, cz]})


def octagon(prefix, y1, y2, hw, hd, c, tex):
    box(f"{prefix}_mid", CX - hw, y1, CZ - hd + c, CX + hw, y2, CZ + hd - c, tex)
    box(f"{prefix}_front", CX - hw + c, y1, CZ + hd - c, CX + hw - c, y2, CZ + hd, tex)
    box(f"{prefix}_back", CX - hw + c, y1, CZ - hd, CX + hw - c, y2, CZ - hd + c, tex)
    box(f"{prefix}_left", CX - hw, y1, CZ - hd + c, CX - hw + c, y2, CZ + hd - c, tex)
    box(f"{prefix}_right", CX + hw - c, y1, CZ - hd + c, CX + hw, y2, CZ + hd - c, tex)
    for tag, sx, sz in (("fr", 1, 1), ("fl", -1, 1), ("br", 1, -1), ("bl", -1, -1)):
        mx = CX + sx * (hw - c / 2)
        mz = CZ + sz * (hd - c / 2)
        thickness = 0.60
        shift = (thickness / 2) / (2 ** 0.5)
        angle = -45 if sx * sz > 0 else 45
        chamfer(f"{prefix}_chamfer_{tag}", mx - sx * shift, mz - sz * shift,
                c * (2 ** 0.5) + 0.12, thickness, y1, y2, angle, tex)


def barrels(items, tex_prefix, sleeve_extra=0.20):
    """items: [(x, y, z_root, z_end, half)]，统一出套筒/台阶/主管/加强环/炮口。"""
    for i, (bx, by, z0, z1, half) in enumerate(items):
        tag = str(i)
        sleeve_end = z0 + 1.25
        step_end = sleeve_end + 0.75
        bell = z1 - 0.70
        diamond(f"barrel_sleeve_{tag}", bx, by, z0, sleeve_end, half + sleeve_extra, "hull_dark")
        diamond(f"barrel_step_{tag}", bx, by, sleeve_end, step_end, half + 0.10, "barrel")
        diamond(f"barrel_{tag}", bx, by, step_end, bell, half, "barrel")
        mid = step_end + (bell - step_end) * 0.45
        diamond(f"barrel_ring_{tag}", bx, by, mid, mid + 0.35, half + 0.06, "barrel")
        diamond(f"muzzle_bell_{tag}", bx, by, bell, z1, half + 0.10, "barrel")
        diamond(f"muzzle_{tag}", bx, by, z1, z1 + 0.10, half - 0.02, "barrel",
                faces={"south": "muzzle_empty"}, full_uv_faces=("south",))


def base(radius, dark="hull_dark"):
    round_footprint("base_flange", CX, CZ, radius, 0.0, 0.35, "dark")
    round_footprint("base_drum", CX, CZ, radius * 0.93, 0.35, 0.95, dark)
    round_footprint("base_lip", CX, CZ, radius * 0.96, 0.95, 1.05, "detail")


def front_port(x1, x2, y1, y2, z, frame=True):
    if frame:
        box("port_frame", x1 - 0.20, y1 - 0.20, z - 0.10, x2 + 0.20, y2 + 0.20, z + 0.10, "detail")
    box("port_inner", x1, y1, z + 0.10, x2, y2, z + 0.18, "interior")


def roof_flat(x1, x2, z1, z2, y, tex="roof"):
    box("roof", x1, y, z1, x2, y + 0.25, z2, tex)


def roof_steps(x1, x2, z1, z2, y):
    box("roof_lower", x1, y, z1, x2, y + 0.22, z2, "roof")
    box("roof_upper", x1 + 0.35, y + 0.22, z1 + 0.18, x2 - 0.35, y + 0.42, z2 - 0.18, "roof")
    box("roof_top", x1 + 1.0, y + 0.42, z1 + 0.35, x2 - 1.0, y + 0.55, z2 - 0.35, "roof")


def rear_recess(width, y1, y2, z=3.90, frame=True):
    x1, x2 = CX - width / 2, CX + width / 2
    if frame:
        box("rear_frame_left", x1 - 0.25, y1 - 0.15, z - 0.10, x1, y2 + 0.15, z + 0.02, "detail")
        box("rear_frame_right", x2, y1 - 0.15, z - 0.10, x2 + 0.25, y2 + 0.15, z + 0.02, "detail")
        box("rear_frame_top", x1, y2, z - 0.10, x2, y2 + 0.15, z + 0.02, "detail")
        box("rear_frame_bottom", x1, y1 - 0.15, z - 0.10, x2, y1, z + 0.02, "detail")
    box("rear_interior", x1, y1, z - 0.02, x2, y2, z + 0.04, "interior")


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


def write(gid):
    tex = {k: NS + gid + "_" + k for k in
           ("hull", "roof", "hull_dark", "detail", "barrel", "interior",
            "muzzle_empty", "muzzle_loaded")}
    # base() 的底座法兰面用独立语义键 dark，实际复用 hull_dark 贴图；
    # 必须在这里显式登记，否则模型里 #dark 指向未定义纹理，渲染成 missing texture
    tex["dark"] = NS + gid + "_hull_dark"
    if gid == "british_single_6inch_gun":
        tex["canvas"] = NS + gid + "_canvas"
    if gid == "soviet_improved_triple_180mm_gun":
        tex["stripe"] = NS + gid + "_stripe"
    base_model = {
        "parent": "minecraft:block/block", "gui_light": "front", "ambientocclusion": False,
        "textures": {"particle": tex["hull"], **tex}, "elements": elements,
    }
    item_entry = {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}
    loaded = {"parent": "piranport:item/" + gid + "_3d",
              "textures": {"muzzle_empty": tex["muzzle_loaded"]}}
    for name, data in ((gid + "_3d.json", base_model), (gid + "_3d_loaded.json", loaded),
                       (gid + ".json", item_entry)):
        with open(os.path.join(ITEM_DIR, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("wrote", gid, len(elements), "elements")


# ================= 1. 美国单装5英寸炮（L_11） =================
def build_us_single_5inch_gun():
    gid = "us_single_5inch_gun"
    base(3.8)
    octagon("body_lower", 1.05, 2.80, 4.00, 3.90, 1.10, "hull")
    octagon("body_bulge", 2.80, 4.40, 4.30, 4.15, 1.30, "hull")
    octagon("body_upper", 4.40, 5.10, 4.00, 3.85, 1.20, "hull")
    octagon("body_shoulder", 5.10, 5.40, 3.30, 3.20, 1.00, "hull")
    octagon("body_top", 5.40, 5.60, 2.80, 2.70, 0.90, "roof")
    front_port(7.10, 8.90, 2.05, 3.65, 11.92)
    barrels([(CX, 2.80, 10.80, 24.00, 0.24)], "us_single_5inch_gun")
    # 左后顶的测距仪罩 + 右舷检修盖
    box("rangefinder", 4.90, 5.60, 5.70, 6.50, 6.20, 7.40, "hull")
    box("rangefinder_top", 5.00, 6.20, 5.80, 6.40, 6.32, 7.30, "roof")
    box("hatch", 12.20, 2.65, 6.40, 12.34, 3.65, 7.70, "detail")
    box("hatch_handle", 12.34, 3.05, 6.70, 12.40, 3.25, 7.40, "dark")
    rear_recess(7.2, 1.60, 4.80, 3.86, False)
    write(gid)


# ================= 2. 美国双联5英寸炮（L_41） =================
def build_us_twin_5inch_gun():
    gid = "us_twin_5inch_gun"
    base(3.7)
    octagon("body_lower", 1.05, 3.00, 4.10, 3.95, 1.15, "hull")
    octagon("body_bulge", 3.00, 4.50, 4.40, 4.20, 1.35, "hull")
    octagon("body_upper", 4.50, 5.00, 4.10, 3.90, 1.20, "hull")
    octagon("body_top", 5.00, 5.20, 3.30, 3.10, 1.00, "roof")
    front_port(5.80, 10.20, 2.05, 3.55, 11.98)
    barrels([(6.85, 2.70, 10.70, 24.00, 0.23),
             (9.15, 2.70, 10.70, 24.00, 0.23)], gid)
    # 后部小尾舱
    box("bustle", 6.40, 4.00, 3.10, 9.60, 4.85, 4.10, "hull")
    box("bustle_top", 6.50, 4.85, 3.20, 9.50, 4.97, 4.00, "roof")
    rear_recess(6.8, 1.55, 4.60, 3.62, True)
    write(gid)


# ================= 3. 美国双联5英寸平高两用炮（L_10） =================
def build_us_twin_5inch_dp_gun():
    gid = "us_twin_5inch_dp_gun"
    base(4.0)
    box("floor", 3.90, 1.05, 4.20, 12.10, 1.40, 12.00, "interior")
    box("wall_left", 3.50, 1.05, 3.70, 4.20, 5.20, 12.00, "hull")
    box("wall_right", 11.80, 1.05, 3.70, 12.50, 5.20, 12.00, "hull")
    box("wall_rear", 4.20, 1.05, 3.70, 11.80, 5.20, 4.40, "hull")
    box("cap_left", 3.45, 5.20, 3.65, 4.25, 5.35, 12.05, "detail")
    box("cap_right", 11.75, 5.20, 3.65, 12.55, 5.35, 12.05, "detail")
    box("front_lip_left", 3.50, 1.40, 11.90, 4.20, 5.20, 12.05, "hull_dark")
    box("front_lip_right", 11.80, 1.40, 11.90, 12.50, 5.20, 12.05, "hull_dark")
    box("front_lip_bottom", 4.20, 1.05, 11.90, 11.80, 1.40, 12.05, "hull_dark")
    box("interior_back", 4.20, 1.40, 4.40, 11.80, 5.10, 4.58, "interior")
    box("roof", 3.60, 5.20, 3.70, 12.40, 5.50, 9.70, "roof")
    box("roof_front_lip", 3.60, 5.15, 9.60, 12.40, 5.45, 9.82, "hull_dark")
    box("roof_vent", 7.00, 5.50, 5.20, 9.00, 5.95, 6.60, "hull")
    barrels([(6.85, 3.90, 6.00, 24.00, 0.23),
             (9.15, 3.90, 6.00, 24.00, 0.23)], gid)
    for tag, x1, x2 in (("r", 12.50, 12.64), ("l", 3.36, 3.50)):
        for y in (4.55, 5.00):
            box(f"rail_{tag}_{int(y*100)}", x1, y, 4.30, x2, y + 0.10, 9.00, "dark")
        for z in (4.50, 6.30, 8.10, 8.90):
            box(f"rail_post_{tag}_{int(z*10)}", x1, 4.15, z - 0.06, x2, 5.10, z + 0.06, "dark")
        for y in (3.05, 3.60, 4.10):
            box(f"rack_{tag}_{int(y*100)}", x1, y, 9.40, x2, y + 0.10, 11.50, "dark")
        for z in (9.50, 10.50, 11.40):
            box(f"rack_post_{tag}_{int(z*10)}", x1, 2.95, z - 0.06, x2, 4.20, z + 0.06, "dark")
    write(gid)


# ================= 4. 5英寸L54双联高平两用炮（L_456） =================
def build_twin_5inch_l54_dp_gun():
    gid = "twin_5inch_l54_dp_gun"
    base(3.9)
    octagon("body_lower", 1.05, 3.00, 4.20, 4.00, 1.20, "hull")
    octagon("body_bulge", 3.00, 4.60, 4.50, 4.20, 1.40, "hull")
    octagon("body_upper", 4.60, 5.30, 4.20, 3.90, 1.25, "hull")
    octagon("body_top", 5.30, 5.55, 3.30, 3.10, 1.00, "roof")
    front_port(5.60, 10.40, 2.15, 3.75, 11.98)
    barrels([(6.85, 2.90, 10.70, 24.00, 0.23),
             (9.15, 2.90, 10.70, 24.00, 0.23)], gid)
    # 顶部双筒测距仪
    for tag, x1, x2 in (("l", 5.60, 6.80), ("r", 9.20, 10.40)):
        box(f"sight_{tag}", x1, 5.55, 5.40, x2, 6.15, 6.90, "hull")
        box(f"sight_top_{tag}", x1 + 0.10, 6.15, 5.50, x2 - 0.10, 6.27, 6.80, "roof")
    rear_recess(7.0, 1.60, 4.80, 3.68, False)
    write(gid)


# ================= 5. 日本14厘米单装炮（L_14） =================
def build_japanese_single_140mm_gun():
    gid = "japanese_single_140mm_gun"
    base(3.5)
    box("body_lower", 4.00, 1.05, 4.00, 12.00, 3.20, 11.50, "hull")
    box("body_upper", 4.20, 3.20, 4.20, 11.80, 4.00, 11.40, "hull")
    chamfer("chamfer_fr", 11.45, 10.95, 1.60, 0.55, 1.05, 4.00, -45, "hull")
    chamfer("chamfer_fl", 4.55, 10.95, 1.60, 0.55, 1.05, 4.00, 45, "hull")
    box("roof", 4.10, 4.00, 4.10, 11.90, 4.28, 11.30, "roof")
    box("roof_slope", 4.20, 3.85, 10.60, 11.80, 4.25, 11.42, "roof",
        rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.00, 11.00]})
    front_port(7.15, 8.85, 2.00, 3.60, 11.50)
    barrels([(CX, 2.75, 10.60, 24.00, 0.24)], gid)
    box("side_box_l", 3.85, 2.30, 6.30, 4.05, 3.10, 7.60, "detail")
    box("side_box_r", 11.95, 2.30, 6.30, 12.15, 3.10, 7.60, "detail")
    rear_recess(6.6, 1.50, 3.60, 3.88, True)
    write(gid)


# ================= 6. 英国单装6英寸炮（L_172） =================
def build_british_single_6inch_gun():
    gid = "british_single_6inch_gun"
    base(3.6)
    box("body_lower", 3.90, 1.05, 3.90, 12.10, 3.60, 11.60, "hull")
    box("body_upper", 4.10, 3.60, 4.10, 11.90, 4.30, 11.50, "hull")
    chamfer("chamfer_fr", 11.55, 11.05, 1.70, 0.60, 1.05, 4.30, -45, "hull")
    chamfer("chamfer_fl", 4.45, 11.05, 1.70, 0.60, 1.05, 4.30, 45, "hull")
    box("roof", 4.00, 4.30, 4.00, 12.00, 4.58, 11.40, "roof")
    box("roof_slope", 4.10, 4.15, 10.70, 11.90, 4.55, 11.52, "roof",
        rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.30, 11.10]})
    # 炮根帆布套
    box("canvas", 6.85, 1.95, 11.45, 9.15, 3.75, 12.15, "canvas")
    box("canvas_inner", 7.15, 2.20, 12.15, 8.85, 3.50, 12.25, "interior")
    barrels([(CX, 2.85, 10.60, 24.00, 0.24)], gid)
    box("hatch", 12.10, 2.80, 6.40, 12.24, 3.80, 7.70, "detail")
    box("hatch_handle", 12.24, 3.20, 6.70, 12.30, 3.40, 7.40, "dark")
    rear_recess(6.6, 1.55, 3.90, 3.88, False)
    write(gid)


# ================= 7. 英国双联6英寸炮（L_20） =================
def build_british_twin_6inch_gun():
    gid = "british_twin_6inch_gun"
    base(3.4)
    octagon("body_lower", 1.05, 2.60, 3.70, 3.55, 1.00, "hull")
    octagon("body_bulge", 2.60, 3.80, 4.00, 3.80, 1.15, "hull")
    octagon("body_upper", 3.80, 4.40, 3.70, 3.50, 1.00, "hull")
    octagon("body_top", 4.40, 4.60, 2.90, 2.70, 0.80, "roof")
    front_port(5.90, 10.10, 1.95, 3.20, 11.60)
    barrels([(6.85, 2.45, 10.40, 23.00, 0.22),
             (9.15, 2.45, 10.40, 23.00, 0.22)], gid)
    box("periscope", 7.70, 4.60, 5.10, 8.30, 5.20, 5.70, "hull")
    box("periscope_top", 7.78, 5.20, 5.18, 8.22, 5.28, 5.62, "detail")
    rear_recess(6.0, 1.40, 3.40, 3.68, False)
    write(gid)


# ================= 8. 日本15.2厘米连装炮（L_15） =================
def build_japanese_twin_152mm_gun():
    gid = "japanese_twin_152mm_gun"
    base(3.6)
    box("body_lower", 3.90, 1.05, 3.90, 12.10, 3.30, 11.60, "hull")
    box("body_upper", 4.10, 3.30, 4.10, 11.90, 4.30, 11.50, "hull")
    chamfer("chamfer_fr", 11.55, 11.05, 1.70, 0.55, 1.05, 4.30, -45, "hull")
    chamfer("chamfer_fl", 4.45, 11.05, 1.70, 0.55, 1.05, 4.30, 45, "hull")
    box("roof", 4.00, 4.30, 4.00, 12.00, 4.58, 11.40, "roof")
    box("roof_step", 4.40, 4.58, 4.10, 11.60, 4.76, 5.20, "roof")
    front_port(5.80, 10.20, 2.00, 3.55, 11.52)
    barrels([(6.85, 2.75, 10.60, 24.00, 0.23),
             (9.15, 2.75, 10.60, 24.00, 0.23)], gid)
    box("periscope", 7.65, 4.76, 5.30, 8.35, 5.35, 5.95, "hull")
    box("blister_l", 3.80, 2.35, 6.20, 4.02, 3.15, 7.70, "detail")
    box("blister_r", 11.98, 2.35, 6.20, 12.20, 3.15, 7.70, "detail")
    rear_recess(6.8, 1.45, 3.80, 3.88, True)
    write(gid)


# ================= 9. 英国三联6英寸炮（L_118） =================
def build_british_triple_6inch_gun():
    gid = "british_triple_6inch_gun"
    base(4.2)
    octagon("body_lower", 1.05, 3.20, 4.40, 4.20, 1.30, "hull")
    octagon("body_bulge", 3.20, 4.90, 4.80, 4.50, 1.50, "hull")
    octagon("body_upper", 4.90, 5.60, 4.40, 4.10, 1.30, "hull")
    octagon("body_top", 5.60, 5.90, 3.40, 3.10, 1.00, "roof")
    front_port(5.20, 10.80, 2.10, 3.80, 12.18)
    barrels([(6.20, 2.95, 10.90, 24.40, 0.23),
             (8.00, 2.95, 10.90, 24.40, 0.23),
             (9.80, 2.95, 10.90, 24.40, 0.23)], gid)
    for tag, x1 in (("l", 12.35), ("r", 3.55)):
        box(f"side_hatch_{tag}", x1 - (0.14 if tag == "l" else 0.0), 2.70, 6.30,
            x1 + (0.0 if tag == "l" else 0.14), 3.70, 7.70, "detail")
    rear_recess(7.6, 1.55, 4.90, 3.58, False)
    write(gid)


# ================= 10. 美国三联6英寸炮（L_21） =================
def build_us_triple_6inch_gun():
    gid = "us_triple_6inch_gun"
    base(4.0)
    octagon("body_lower", 1.05, 2.80, 4.40, 4.00, 1.20, "hull")
    octagon("body_upper", 2.80, 4.00, 4.20, 3.80, 1.10, "hull")
    box("roof_step_lower", 3.90, 4.00, 3.90, 12.10, 4.30, 11.20, "roof")
    box("roof_step_upper", 4.60, 4.30, 4.30, 11.40, 4.55, 10.60, "roof")
    front_port(5.20, 10.80, 2.00, 3.50, 11.78)
    barrels([(6.20, 2.65, 10.60, 24.00, 0.23),
             (8.00, 2.65, 10.60, 24.00, 0.23),
             (9.80, 2.65, 10.60, 24.00, 0.23)], gid)
    rear_recess(7.4, 1.45, 3.60, 3.58, False)
    write(gid)


# ================= 11. MK16三联6英寸消焰炮（L_311） =================
def build_mk16_triple_6inch_gun_flashless():
    gid = "mk16_triple_6inch_gun_flashless"
    base(4.2)
    octagon("body_lower", 1.05, 3.00, 4.40, 4.20, 1.30, "hull")
    octagon("body_bulge", 3.00, 4.70, 4.70, 4.40, 1.45, "hull")
    octagon("body_upper", 4.70, 5.40, 4.30, 4.00, 1.30, "hull")
    octagon("body_top", 5.40, 5.70, 3.30, 3.00, 1.00, "roof")
    front_port(5.20, 10.80, 2.10, 3.75, 12.18)
    barrels([(6.20, 2.90, 10.90, 24.40, 0.23),
             (8.00, 2.90, 10.90, 24.40, 0.23),
             (9.80, 2.90, 10.90, 24.40, 0.23)], gid, sleeve_extra=0.30)
    for tag, x1 in (("l", 12.35), ("r", 3.55)):
        box(f"side_plate_{tag}", x1 - (0.14 if tag == "l" else 0.0), 2.80, 6.20,
            x1 + (0.0 if tag == "l" else 0.14), 3.90, 7.80, "detail")
    rear_recess(7.6, 1.55, 4.70, 3.58, False)
    write(gid)


# ================= 13. 改进型6英寸三联主炮（L_516） =================
def build_improved_triple_6inch_gun():
    gid = "improved_triple_6inch_gun"
    base(4.0, dark="hull_dark")
    box("body_lower", 3.80, 1.05, 3.90, 12.20, 3.40, 11.70, "hull")
    box("body_upper", 4.00, 3.40, 4.10, 12.00, 4.20, 11.60, "hull")
    box("roof", 4.00, 4.20, 4.00, 12.00, 4.50, 11.50, "roof")
    box("roof_slope", 4.10, 4.05, 10.80, 11.90, 4.45, 11.60, "roof",
        rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 4.20, 11.20]})
    box("side_plate_l", 3.72, 1.60, 5.60, 3.85, 3.30, 8.20, "detail")
    box("side_plate_r", 12.15, 1.60, 5.60, 12.28, 3.30, 8.20, "detail")
    front_port(5.20, 10.80, 2.00, 3.55, 11.72)
    barrels([(6.20, 2.70, 10.70, 24.00, 0.23),
             (8.00, 2.70, 10.70, 24.00, 0.23),
             (9.80, 2.70, 10.70, 24.00, 0.23)], gid)
    rear_recess(7.4, 1.40, 3.70, 3.88, False)
    write(gid)


# ================= 14. 152毫米三联高平炮(46)（L_493） =================
def build_triple_152mm_dp_gun_46():
    gid = "triple_152mm_dp_gun_46"
    base(4.2)
    octagon("body_lower", 1.05, 3.20, 4.40, 4.20, 1.30, "hull")
    octagon("body_bulge", 3.20, 4.80, 4.70, 4.40, 1.45, "hull")
    octagon("body_upper", 4.80, 5.40, 4.30, 4.00, 1.30, "hull")
    octagon("body_top", 5.40, 5.70, 3.40, 3.10, 1.00, "roof")
    front_port(5.20, 10.80, 2.10, 3.80, 12.08)
    barrels([(6.20, 3.00, 10.90, 24.20, 0.23),
             (8.00, 3.00, 10.90, 24.20, 0.23),
             (9.80, 3.00, 10.90, 24.20, 0.23)], gid)
    # 顶部环形扶手：四段直杆 + 四根立柱
    for tag, x1, z1, x2, z2 in (("f", 4.90, 10.75, 11.10, 10.87),
                                ("b", 4.90, 4.73, 11.10, 4.85),
                                ("l", 4.73, 5.00, 4.85, 10.60),
                                ("r", 11.15, 5.00, 11.27, 10.60)):
        box(f"ring_{tag}", x1, 5.70, z1, x2, 5.84, z2, "dark")
    for tag, x, z in (("fl", 4.90, 10.75), ("fr", 11.10, 10.75),
                      ("bl", 4.90, 4.73), ("br", 11.10, 4.73)):
        box(f"ring_post_{tag}", x - 0.06, 5.55, z - 0.06, x + 0.06, 5.84, z + 0.06, "dark")
    rear_recess(7.4, 1.50, 4.80, 3.60, False)
    write(gid)


# ================= 15. 瑞典三联152毫米炮（L_270） =================
def build_swedish_triple_152mm_gun():
    gid = "swedish_triple_152mm_gun"
    base(3.8)
    box("body_lower", 3.90, 1.05, 3.90, 12.10, 3.30, 11.60, "hull")
    box("body_upper", 4.10, 3.30, 4.10, 11.90, 4.20, 11.50, "hull")
    chamfer("chamfer_fr", 11.55, 11.05, 1.70, 0.55, 1.05, 4.20, -45, "hull")
    chamfer("chamfer_fl", 4.45, 11.05, 1.70, 0.55, 1.05, 4.20, 45, "hull")
    box("roof", 4.00, 4.20, 4.00, 12.00, 4.48, 11.40, "roof")
    box("roof_step", 4.40, 4.48, 4.10, 11.60, 4.72, 5.80, "roof")
    front_port(5.20, 10.80, 1.95, 3.55, 11.52)
    barrels([(6.20, 2.65, 10.60, 24.00, 0.23),
             (8.00, 2.65, 10.60, 24.00, 0.23),
             (9.80, 2.65, 10.60, 24.00, 0.23)], gid)
    box("rangefinder", 6.80, 4.72, 4.80, 8.50, 5.20, 6.10, "hull")
    box("rangefinder_top", 6.90, 5.20, 4.90, 8.40, 5.30, 6.00, "detail")
    rear_recess(7.4, 1.45, 3.70, 3.88, False)
    write(gid)


# ================= 16. 苏联三联180毫米炮（L_177） =================
def build_soviet_triple_180mm_gun():
    gid = "soviet_triple_180mm_gun"
    base(4.4)
    octagon("body_lower", 1.05, 3.40, 4.60, 4.40, 1.40, "hull")
    octagon("body_bulge", 3.40, 5.00, 4.90, 4.60, 1.55, "hull")
    octagon("body_upper", 5.00, 5.70, 4.50, 4.20, 1.35, "hull")
    octagon("body_top", 5.70, 6.00, 3.50, 3.20, 1.00, "roof")
    front_port(5.20, 10.80, 2.20, 4.00, 12.28)
    barrels([(6.20, 3.20, 11.10, 24.60, 0.25),
             (8.00, 3.20, 11.10, 24.60, 0.25),
             (9.80, 3.20, 11.10, 24.60, 0.25)], gid)
    box("rf_box", 4.60, 5.85, 5.20, 6.40, 6.35, 6.70, "hull")
    box("rf_top", 4.70, 6.35, 5.30, 6.30, 6.45, 6.60, "roof")
    for tag, x1, x2 in (("r", 12.60, 12.74), ("l", 3.26, 3.40)):
        for y in (4.30, 4.80):
            box(f"handrail_{tag}_{int(y*100)}", x1, y, 4.80, x2, y + 0.09, 9.20, "detail")
        for z in (5.00, 6.80, 8.60):
            box(f"handrail_post_{tag}_{int(z*10)}", x1, 3.95, z - 0.05, x2, 4.90, z + 0.05, "detail")
    rear_recess(7.8, 1.55, 5.00, 3.58, False)
    write(gid)


# ================= 17. 苏联改良型三联180毫米炮（L_301） =================
def build_soviet_improved_triple_180mm_gun():
    gid = "soviet_improved_triple_180mm_gun"
    base(4.3)
    octagon("body_lower", 1.05, 3.30, 4.50, 4.30, 1.35, "hull")
    octagon("body_bulge", 3.30, 4.90, 4.80, 4.50, 1.50, "hull")
    octagon("body_upper", 4.90, 5.60, 4.40, 4.10, 1.30, "hull")
    octagon("body_top", 5.60, 5.90, 3.40, 3.10, 1.00, "roof")
    front_port(5.20, 10.80, 2.15, 3.95, 12.18)
    barrels([(6.20, 3.10, 11.00, 24.40, 0.24),
             (8.00, 3.10, 11.00, 24.40, 0.24),
             (9.80, 3.10, 11.00, 24.40, 0.24)], gid)
    # 右舷红色识别条
    box("red_stripe", 12.68, 1.40, 5.90, 12.80, 4.80, 7.20, "stripe")
    box("side_hatch", 12.55, 2.60, 8.00, 12.68, 3.70, 9.20, "detail")
    rear_recess(7.6, 1.50, 4.80, 3.56, False)
    write(gid)


# ================= 18. 英国双联8英寸炮（L_119） =================
def build_british_twin_8inch_gun():
    gid = "british_twin_8inch_gun"
    base(4.2)
    box("body_lower", 3.60, 1.05, 3.80, 12.40, 2.80, 11.80, "hull")
    box("body_upper", 3.90, 2.80, 4.00, 12.10, 3.60, 11.70, "hull")
    chamfer("chamfer_fr", 11.85, 11.20, 1.70, 0.55, 1.05, 3.60, -45, "hull")
    chamfer("chamfer_fl", 4.15, 11.20, 1.70, 0.55, 1.05, 3.60, 45, "hull")
    box("roof", 3.90, 3.60, 4.00, 12.10, 3.88, 11.60, "roof")
    box("roof_slope", 4.00, 3.45, 10.95, 12.00, 3.85, 11.72, "roof",
        rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.60, 11.35]})
    front_port(5.80, 10.20, 1.75, 3.10, 11.82)
    barrels([(6.75, 2.40, 10.80, 24.00, 0.24),
             (9.25, 2.40, 10.80, 24.00, 0.24)], gid)
    box("side_plate_l", 3.52, 1.40, 5.80, 3.64, 2.60, 8.40, "detail")
    box("side_plate_r", 12.36, 1.40, 5.80, 12.48, 2.60, 8.40, "detail")
    rear_recess(7.2, 1.35, 3.20, 3.78, False)
    write(gid)


# ================= 19. 日本20.3厘米连装炮（L_16） =================
def build_japanese_twin_203mm_gun():
    gid = "japanese_twin_203mm_gun"
    base(3.8)
    box("body_lower", 3.90, 1.05, 3.90, 12.10, 3.30, 11.60, "hull")
    box("body_upper", 4.10, 3.30, 4.10, 11.90, 4.20, 11.50, "hull")
    chamfer("chamfer_fr", 11.55, 11.05, 1.70, 0.55, 1.05, 4.20, -45, "hull")
    chamfer("chamfer_fl", 4.45, 11.05, 1.70, 0.55, 1.05, 4.20, 45, "hull")
    box("roof", 4.00, 4.20, 4.00, 12.00, 4.48, 11.40, "roof")
    box("roof_step", 4.40, 4.48, 4.10, 11.60, 4.72, 5.90, "roof")
    front_port(5.80, 10.20, 2.00, 3.55, 11.52)
    barrels([(6.80, 2.70, 10.60, 24.20, 0.24),
             (9.20, 2.70, 10.60, 24.20, 0.24)], gid)
    box("periscope_base", 7.60, 4.72, 5.20, 8.40, 5.05, 5.90, "hull")
    box("periscope", 7.90, 5.05, 5.45, 8.10, 5.75, 5.65, "dark")
    box("blister_l", 3.80, 2.30, 6.30, 4.02, 3.10, 7.70, "detail")
    box("blister_r", 11.98, 2.30, 6.30, 12.20, 3.10, 7.70, "detail")
    rear_recess(6.8, 1.45, 3.80, 3.88, True)
    write(gid)


if __name__ == "__main__":
    for fn in (build_us_single_5inch_gun, build_us_twin_5inch_gun,
               build_us_twin_5inch_dp_gun, build_twin_5inch_l54_dp_gun,
               build_japanese_single_140mm_gun, build_british_single_6inch_gun,
               build_british_twin_6inch_gun, build_japanese_twin_152mm_gun,
               build_british_triple_6inch_gun, build_us_triple_6inch_gun,
               build_mk16_triple_6inch_gun_flashless,
               build_improved_triple_6inch_gun, build_triple_152mm_dp_gun_46,
               build_swedish_triple_152mm_gun, build_soviet_triple_180mm_gun,
               build_soviet_improved_triple_180mm_gun, build_british_twin_8inch_gun,
               build_japanese_twin_203mm_gun):
        elements.clear()
        fn()
