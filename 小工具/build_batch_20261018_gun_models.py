#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-18 六门主炮的 3D 手持模型（四套几何 + 两套改色）。

几何全部按各自原画从零搭：
  L_24  日本35.6厘米连装炮：深棕灰炮塔、屋顶长条测距仪、两管白色帆布炮衣
  L_34  美国三联14英寸炮：低长浅蓝灰炮塔、三管、后部两舷测距仪盒
  L_32  英国四联14英寸炮：蓝色方正炮塔、四管、后部两舷小盒
  L_30  英国双联15英寸炮：灰蓝炮塔、屋顶阶梯测距仪、两管卡其炮衣
改色（只换贴图，几何直接继承）：
  L_452 英国四联14英寸主炮（284）→ 四联14几何 + 灰绿贴图
  L_120 英国双联15英寸炮(改)      → 双联15几何 + 亮蓝贴图
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
NS = "piranport:item/"


def uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    max_u = max(0.0, 16.0 - w)
    max_v = max(0.0, 16.0 - h)
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


class Model:
    def __init__(self):
        self.elements = []

    def box(self, name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
            full_uv_faces=()):
        faces_json = {}
        seed = len(self.elements) + 1
        for face in ("north", "south", "west", "east", "up", "down"):
            face_tex = (faces or {}).get(face, tex)
            entry = {"texture": "#" + face_tex}
            if face in full_uv_faces:
                entry["uv"] = [0, 0, 16, 16]
            else:
                entry["uv"] = uv_for(face, x1, y1, z1, x2, y2, z2, seed)
            faces_json[face] = entry
        element = {
            "name": name,
            "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
            "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
            "faces": faces_json,
        }
        if rotation:
            element["rotation"] = rotation
        self.elements.append(element)

    def diamond(self, name, cx, cy, z1, z2, half, tex, faces=None,
                full_uv_faces=()):
        self.box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
                 faces=faces, full_uv_faces=full_uv_faces,
                 rotation={"angle": 45, "axis": "z",
                           "origin": [cx, cy, (z1 + z2) / 2]})

    def round_footprint(self, name, cx, cz, radius, y1, y2, tex):
        steps = ((1.00, 0.35), (0.96, 0.60), (0.85, 0.80),
                 (0.70, 0.92), (0.50, 1.00))
        for i, (fx, fz) in enumerate(steps):
            self.box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
                     cx + radius * fx, y2, cz + radius * fz, tex)

    def textures(self, gun, blast=False):
        d = {
            "particle": NS + gun + "_hull",
            "hull": NS + gun + "_hull",
            "hull_dark": NS + gun + "_hull_dark",
            "roof": NS + gun + "_roof",
            "barrel": NS + gun + "_barrel",
            "detail": NS + gun + "_detail",
            "muzzle_empty": NS + gun + "_muzzle_empty",
            "muzzle_loaded": NS + gun + "_muzzle_loaded",
        }
        if blast:
            d["blast"] = NS + gun + "_blast"
        return d

    def base(self, gun, blast=False):
        return {
            "parent": "minecraft:block/block",
            "gui_light": "front",
            "ambientocclusion": False,
            "textures": self.textures(gun, blast),
            "elements": self.elements,
        }


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                              "scale": [0.65, 0.65, 0.65]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                             "scale": [0.65, 0.65, 0.65]},
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                              "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                             "scale": [0.6, 0.6, 0.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0],
               "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7],
             "scale": [1, 1, 1]},
}


def item_entry():
    return {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}


# ================= 日本35.6厘米连装炮 =================
def jp356():
    m = Model()
    m.round_footprint("barbette", 8.0, 7.6, 4.4, 0.0, 1.5, "hull_dark")
    m.round_footprint("barbette_lip", 8.0, 7.6, 4.55, 1.5, 1.7, "hull_dark")
    m.box("lower_mid", 3.6, 1.7, 3.2, 12.4, 2.6, 11.6, "hull")
    m.box("lower_front", 4.2, 1.7, 3.8, 11.8, 2.6, 12.2, "hull")
    m.box("mid", 3.8, 2.6, 3.4, 12.2, 3.7, 11.4, "hull")
    m.box("upper_front", 4.4, 2.6, 4.0, 11.6, 3.7, 12.0, "hull")
    m.box("front_plate", 4.2, 1.7, 11.9, 11.8, 3.3, 12.35, "hull")
    m.box("roof_top", 4.2, 3.7, 3.6, 11.8, 4.15, 11.4, "roof")
    m.box("roof_slope_front", 4.3, 3.5, 10.9, 11.7, 3.9, 11.95, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.7, 11.4]})
    m.box("roof_slope_port", 3.8, 3.4, 3.6, 4.3, 4.05, 11.3, "roof",
          rotation={"angle": -22.5, "axis": "z", "origin": [4.0, 3.65, 7.45]})
    m.box("roof_slope_stbd", 11.7, 3.4, 3.6, 12.2, 4.05, 11.3, "roof",
          rotation={"angle": 22.5, "axis": "z", "origin": [12.0, 3.65, 7.45]})
    # 屋顶长条测距仪：横贯全宽，两端方帽
    m.box("rf_bar", 2.7, 4.55, 3.9, 13.3, 4.95, 5.1, "hull_dark")
    m.box("rf_cap_port", 2.4, 4.45, 3.8, 2.85, 5.05, 5.2, "hull_dark")
    m.box("rf_cap_stbd", 13.15, 4.45, 3.8, 13.6, 5.05, 5.2, "hull_dark")
    m.box("rf_support_port", 4.8, 4.15, 4.3, 5.2, 4.55, 4.8, "hull_dark")
    m.box("rf_support_stbd", 10.8, 4.15, 4.3, 11.2, 4.55, 4.8, "hull_dark")
    for tag, cx in (("l", 6.4), ("r", 9.6)):
        # 白色帆布炮衣：上中下三段收分
        m.box(f"blast_low_{tag}", cx - 0.45, 1.9, 11.6, cx + 0.45, 2.2, 12.4, "blast")
        m.box(f"blast_mid_{tag}", cx - 0.62, 2.2, 11.4, cx + 0.62, 3.4, 12.6, "blast")
        m.box(f"blast_top_{tag}", cx - 0.5, 3.4, 11.6, cx + 0.5, 3.7, 12.4, "blast")
        m.diamond(f"barrel_sleeve_{tag}", cx, 2.8, 11.8, 13.2, 0.42, "barrel")
        m.diamond(f"barrel_{tag}", cx, 2.8, 13.2, 25.7, 0.28, "barrel")
        m.diamond(f"barrel_ring_a_{tag}", cx, 2.8, 15.0, 15.4, 0.35, "barrel")
        m.diamond(f"barrel_ring_b_{tag}", cx, 2.8, 17.5, 17.9, 0.35, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.8, 25.7, 26.2, 0.36, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.8, 26.2, 26.3, 0.22, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


# ================= 美国三联14英寸炮 =================
def us14():
    m = Model()
    m.round_footprint("ring", 8.0, 7.7, 4.4, 0.0, 0.55, "hull_dark")
    m.round_footprint("ring_lip", 8.0, 7.7, 4.5, 0.55, 1.0, "detail")
    m.box("lower_mid", 3.6, 1.0, 2.8, 12.4, 2.4, 12.4, "hull")
    m.box("lower_front", 4.2, 1.0, 3.4, 11.8, 2.4, 12.9, "hull")
    m.box("lower_rear", 4.2, 1.0, 2.4, 11.8, 2.4, 11.8, "hull")
    m.box("upper_mid", 3.9, 2.4, 3.0, 12.1, 3.4, 12.0, "hull")
    m.box("upper_front", 4.4, 2.4, 3.6, 11.6, 3.4, 12.6, "hull")
    m.box("roof_top", 4.1, 3.4, 2.9, 11.9, 3.85, 12.0, "roof")
    m.box("roof_slope_front", 4.2, 3.2, 11.5, 11.8, 3.65, 12.85, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.4, 12.0]})
    m.box("roof_slope_rear", 4.3, 3.2, 2.5, 11.7, 3.65, 3.2, "roof",
          rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.4, 2.9]})
    m.box("roof_slope_port", 3.7, 3.1, 3.0, 4.2, 3.75, 12.0, "roof",
          rotation={"angle": -22.5, "axis": "z", "origin": [3.95, 3.4, 7.5]})
    m.box("roof_slope_stbd", 11.8, 3.1, 3.0, 12.3, 3.75, 12.0, "roof",
          rotation={"angle": 22.5, "axis": "z", "origin": [12.05, 3.4, 7.5]})
    m.box("rf_port", 2.9, 3.0, 3.6, 3.9, 3.65, 4.9, "hull")
    m.box("rf_stbd", 12.1, 3.0, 3.6, 13.1, 3.65, 4.9, "hull")
    m.box("front_plate", 4.2, 1.0, 12.3, 11.8, 3.1, 12.7, "hull")
    for tag, cx in (("l", 5.8), ("m", 8.0), ("r", 10.2)):
        m.diamond(f"port_{tag}", cx, 2.5, 12.62, 12.74, 0.44, "hull_dark")
        m.diamond(f"barrel_sleeve_{tag}", cx, 2.5, 11.9, 13.4, 0.38, "hull_dark")
        m.diamond(f"barrel_{tag}", cx, 2.5, 13.4, 26.0, 0.22, "barrel")
        m.diamond(f"ring_a_{tag}", cx, 2.5, 15.8, 16.2, 0.29, "barrel")
        m.diamond(f"ring_b_{tag}", cx, 2.5, 18.3, 18.7, 0.29, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.5, 25.7, 26.2, 0.3, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.5, 26.2, 26.3, 0.18, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


# ================= 英国四联14英寸炮 =================
def brit_quad14():
    m = Model()
    m.round_footprint("ring", 8.0, 7.5, 4.5, 0.0, 0.6, "hull_dark")
    m.round_footprint("ring_lip", 8.0, 7.5, 4.6, 0.6, 1.1, "hull")
    m.box("lower_mid", 3.6, 1.1, 2.9, 12.4, 2.6, 11.9, "hull")
    m.box("lower_front", 4.0, 1.1, 3.4, 12.0, 2.6, 12.3, "hull")
    m.box("upper_mid", 3.9, 2.6, 3.2, 12.1, 3.7, 11.7, "hull")
    m.box("upper_front", 4.3, 2.6, 3.7, 11.7, 3.7, 12.1, "hull")
    m.box("front_slope", 4.0, 2.3, 11.6, 12.0, 3.5, 12.3, "hull",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 2.8, 12.1]})
    m.box("roof_top", 4.1, 3.7, 3.2, 11.9, 4.1, 11.7, "roof")
    m.box("roof_slope_front", 4.2, 3.5, 10.9, 11.8, 3.9, 12.0, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.7, 11.6]})
    m.box("roof_slope_port", 3.8, 3.4, 3.3, 4.3, 4.0, 11.6, "roof",
          rotation={"angle": -22.5, "axis": "z", "origin": [4.05, 3.65, 7.45]})
    m.box("roof_slope_stbd", 11.7, 3.4, 3.3, 12.2, 4.0, 11.6, "roof",
          rotation={"angle": 22.5, "axis": "z", "origin": [11.95, 3.65, 7.45]})
    m.box("box_port", 2.9, 3.15, 3.9, 3.9, 3.8, 5.3, "hull")
    m.box("box_stbd", 12.1, 3.15, 3.9, 13.1, 3.8, 5.3, "hull")
    for tag, cx in (("a", 5.45), ("b", 7.15), ("c", 8.85), ("d", 10.55)):
        m.diamond(f"port_{tag}", cx, 2.7, 12.18, 12.3, 0.34, "hull_dark")
        m.diamond(f"sleeve_{tag}", cx, 2.7, 11.7, 13.0, 0.3, "hull_dark")
        m.diamond(f"barrel_{tag}", cx, 2.7, 13.0, 25.5, 0.18, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.7, 15.3, 15.7, 0.24, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.7, 25.5, 26.0, 0.26, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.7, 26.0, 26.1, 0.16, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


# ================= 英国双联15英寸炮 =================
def brit_twin15():
    m = Model()
    m.round_footprint("ring", 8.0, 7.6, 4.5, 0.0, 0.6, "hull_dark")
    m.round_footprint("ring_lip", 8.0, 7.6, 4.6, 0.6, 1.15, "hull")
    m.box("lower_mid", 3.6, 1.15, 2.9, 12.4, 2.7, 11.9, "hull")
    m.box("lower_front", 4.0, 1.15, 3.4, 12.0, 2.7, 12.3, "hull")
    m.box("upper_mid", 3.9, 2.7, 3.2, 12.1, 3.8, 11.7, "hull")
    m.box("upper_front", 4.3, 2.7, 3.7, 11.7, 3.8, 12.1, "hull")
    m.box("front_slope", 4.0, 2.3, 11.6, 12.0, 3.6, 12.3, "hull",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 2.9, 12.1]})
    m.box("roof_top", 4.0, 3.8, 3.2, 12.0, 4.2, 11.7, "roof")
    m.box("roof_slope_front", 4.1, 3.6, 10.9, 11.9, 4.0, 12.0, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.8, 11.6]})
    m.box("roof_slope_port", 3.7, 3.5, 3.3, 4.2, 4.1, 11.6, "roof",
          rotation={"angle": -22.5, "axis": "z", "origin": [3.95, 3.75, 7.45]})
    m.box("roof_slope_stbd", 11.8, 3.5, 3.3, 12.3, 4.1, 11.6, "roof",
          rotation={"angle": 22.5, "axis": "z", "origin": [12.05, 3.75, 7.45]})
    # 屋顶阶梯测距仪
    m.box("rf_base", 4.3, 4.2, 3.6, 11.7, 4.5, 5.0, "hull_dark")
    m.box("rf_center", 5.0, 4.5, 3.7, 11.0, 4.75, 4.9, "hull")
    m.box("rf_cap_port", 4.1, 4.35, 3.5, 4.5, 4.6, 5.1, "hull_dark")
    m.box("rf_cap_stbd", 11.5, 4.35, 3.5, 11.9, 4.6, 5.1, "hull_dark")
    m.box("side_hatch", 12.02, 2.5, 7.4, 12.2, 3.3, 8.2, "detail")
    for tag, cx in (("l", 6.2), ("r", 9.8)):
        m.box(f"blast_low_{tag}", cx - 0.42, 2.25, 11.6, cx + 0.42, 2.55, 12.3, "blast")
        m.box(f"blast_mid_{tag}", cx - 0.58, 2.55, 11.4, cx + 0.58, 3.45, 12.35, "blast")
        m.box(f"blast_top_{tag}", cx - 0.46, 3.45, 11.6, cx + 0.46, 3.75, 12.3, "blast")
        m.diamond(f"sleeve_{tag}", cx, 2.9, 11.8, 13.2, 0.4, "barrel")
        m.diamond(f"barrel_{tag}", cx, 2.9, 13.2, 25.8, 0.28, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.9, 15.6, 16.0, 0.35, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.9, 25.8, 26.3, 0.36, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.9, 26.3, 26.4, 0.22, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


def dump_set(gun, model, blast=False):
    dump(f"{gun}_3d.json", model.base(gun, blast))
    dump(f"{gun}_3d_loaded.json", {
        "parent": f"piranport:item/{gun}_3d",
        "textures": {"muzzle_empty": NS + gun + "_muzzle_loaded"},
    })
    dump(f"{gun}.json", item_entry())


def dump_recolor(gun, parent_gun, blast=False):
    keys = ["hull", "hull_dark", "roof", "barrel", "detail",
            "muzzle_empty", "muzzle_loaded"]
    if blast:
        keys.append("blast")
    dump(f"{gun}_3d.json", {
        "parent": f"piranport:item/{parent_gun}_3d",
        "textures": {k: NS + gun + "_" + k for k in keys},
    })
    dump(f"{gun}_3d_loaded.json", {
        "parent": f"piranport:item/{gun}_3d",
        "textures": {"muzzle_empty": NS + gun + "_muzzle_loaded"},
    })
    dump(f"{gun}.json", item_entry())


if __name__ == "__main__":
    dump_set("japanese_twin_356mm_gun", jp356(), blast=True)
    dump_set("us_triple_14inch_gun", us14())
    # 改型与原版共用一套贴图/模型，物品入口单独一份
    dump("us_triple_14inch_gun_kai.json", item_entry())
    dump_set("british_quad_14inch_gun", brit_quad14())
    dump_set("british_twin_15inch_gun", brit_twin15(), blast=True)
    dump_recolor("british_quad_14inch_gun_284", "british_quad_14inch_gun")
    dump_recolor("british_twin_15inch_gun_kai", "british_twin_15inch_gun", blast=True)
