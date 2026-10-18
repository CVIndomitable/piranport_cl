#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-18（第二批）主炮 3D 手持模型。

  L_166 意大利三联381毫米炮：浅灰侧板 + 蓝色顶盖 + 右舷带圆帽测距仪
  L_267 意大利三联381毫米炮改：继承 381 几何，整体改蓝/蓝绿贴图
  L_122 美国双联16英寸炮：浅灰顶壳 + 深灰侧板 + 左后测距仪盒（改型共用）
  L_25  日本41厘米连装炮：灰色长炮塔 + 浅色炮衣 + 侧面刻度带 + 后顶潜望镜
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
        element = {"name": name,
                   "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
                   "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
                   "faces": faces_json}
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

    def textures(self, gun, extra=()):
        keys = ["hull", "hull_dark", "roof", "barrel", "detail",
                "muzzle_empty", "muzzle_loaded"] + list(extra)
        return {k: NS + gun + "_" + k for k in keys} | {"particle": NS + gun + "_hull"}

    def base(self, gun, extra=()):
        return {"parent": "minecraft:block/block", "gui_light": "front",
                "ambientocclusion": False,
                "textures": self.textures(gun, extra),
                "elements": self.elements}


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


def italian381():
    m = Model()
    m.round_footprint("ring", 8.0, 7.5, 4.6, 0.0, 0.5, "hull")
    m.round_footprint("ring_drum", 8.0, 7.5, 4.35, 0.5, 1.2, "hull")
    m.round_footprint("ring_band", 8.0, 7.5, 4.42, 1.2, 1.55, "roof")
    m.box("lower", 3.5, 1.55, 2.8, 12.5, 2.8, 11.8, "hull")
    m.box("upper", 3.9, 2.8, 3.1, 12.1, 3.75, 11.5, "hull")
    m.box("roof_top", 4.0, 3.75, 3.2, 12.0, 4.1, 12.0, "roof")
    m.box("roof_slope_front", 4.1, 3.55, 11.3, 11.9, 3.9, 12.3, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.75, 11.95]})
    m.box("roof_slope_port", 3.8, 3.4, 3.3, 4.3, 4.0, 11.7, "hull",
          rotation={"angle": -22.5, "axis": "z", "origin": [4.05, 3.7, 7.5]})
    m.box("roof_slope_stbd", 11.7, 3.4, 3.3, 12.2, 4.0, 11.7, "hull",
          rotation={"angle": 22.5, "axis": "z", "origin": [11.95, 3.7, 7.5]})
    m.box("front_plate", 4.2, 1.55, 11.9, 11.8, 3.3, 12.3, "hull")
    # 后顶两个瞄准具
    for tag, cx in (("l", 6.2), ("r", 9.8)):
        m.box(f"sight_{tag}", cx - 0.18, 4.1, 3.85, cx + 0.18, 4.5, 4.25, "hull_dark")
        m.box(f"sight_cap_{tag}", cx - 0.26, 4.5, 3.75, cx + 0.26, 4.62, 4.35, "detail")
    # 右舷测距仪盒 + 顶部圆帽
    m.box("rf_box", 12.1, 3.3, 5.2, 13.3, 4.0, 7.0, "hull")
    m.box("rf_cap", 12.2, 4.0, 5.3, 13.2, 4.12, 6.9, "detail")
    m.box("rf_cylinder", 12.85, 4.0, 5.7, 13.15, 4.28, 6.1, "hull_dark")
    for tag, cx in (("l", 5.9), ("m", 8.0), ("r", 10.1)):
        m.diamond(f"sleeve_{tag}", cx, 2.8, 11.4, 12.8, 0.5, "detail")
        m.diamond(f"sleeve2_{tag}", cx, 2.8, 12.8, 14.0, 0.4, "detail")
        m.diamond(f"barrel_{tag}", cx, 2.8, 14.0, 26.0, 0.24, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.8, 16.5, 16.9, 0.3, "barrel")
        m.diamond(f"ring2_{tag}", cx, 2.8, 19.0, 19.4, 0.3, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.8, 25.7, 26.2, 0.32, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.8, 26.2, 26.3, 0.2, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def us16():
    m = Model()
    m.round_footprint("ring", 8.0, 7.5, 4.4, 0.0, 0.9, "hull_dark")
    m.box("body", 3.5, 0.9, 2.9, 12.5, 3.5, 11.9, "hull")
    # 两侧深灰侧板，原画侧面明显压暗
    m.box("side_port", 3.38, 1.2, 3.5, 3.53, 3.3, 11.3, "hull_dark")
    m.box("side_stbd", 12.47, 1.2, 3.5, 12.62, 3.3, 11.3, "hull_dark")
    m.box("roof_top", 4.0, 3.5, 3.2, 12.0, 3.95, 12.0, "roof")
    m.box("roof_slope_front", 4.1, 3.3, 11.3, 11.9, 3.7, 12.3, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.5, 11.95]})
    m.box("front_plate", 4.2, 0.9, 11.9, 11.8, 3.3, 12.3, "hull")
    # 左后屋顶小测距仪盒、右后侧盒、右舷深色观察窗
    m.box("rf_small", 3.3, 3.95, 3.5, 4.3, 4.4, 4.7, "detail")
    m.box("rf_small_cap", 3.4, 4.4, 3.6, 4.2, 4.52, 4.6, "hull_dark")
    m.box("side_box", 12.1, 3.0, 4.0, 13.2, 3.65, 5.4, "detail")
    m.box("side_oval", 12.5, 1.5, 7.0, 12.62, 2.6, 8.8, "hull_dark")
    for tag, cx in (("l", 6.2), ("r", 9.8)):
        m.diamond(f"sleeve_{tag}", cx, 2.7, 11.3, 12.8, 0.45, "detail")
        m.diamond(f"sleeve2_{tag}", cx, 2.7, 12.8, 13.8, 0.35, "detail")
        m.diamond(f"barrel_{tag}", cx, 2.7, 13.8, 26.0, 0.28, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.7, 16.2, 16.6, 0.34, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.7, 25.7, 26.2, 0.36, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.7, 26.2, 26.3, 0.22, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def japanese410():
    m = Model()
    m.round_footprint("ring", 8.0, 7.5, 4.4, 0.0, 1.0, "hull_dark")
    m.box("lower", 3.4, 1.0, 2.6, 12.6, 2.6, 11.8, "hull")
    m.box("upper", 3.7, 2.6, 2.9, 12.3, 3.7, 11.6, "hull")
    m.box("roof_top", 4.1, 3.7, 3.3, 11.9, 4.05, 11.4, "roof")
    m.box("roof_slope_front", 4.2, 3.5, 10.9, 11.8, 3.85, 11.95, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 3.7, 11.5]})
    m.box("roof_slope_port", 3.7, 3.4, 3.4, 4.2, 3.95, 11.5, "hull",
          rotation={"angle": -22.5, "axis": "z", "origin": [3.95, 3.65, 7.45]})
    m.box("roof_slope_stbd", 11.8, 3.4, 3.4, 12.3, 3.95, 11.5, "hull",
          rotation={"angle": 22.5, "axis": "z", "origin": [12.05, 3.65, 7.45]})
    m.box("front_plate", 4.2, 1.0, 11.85, 11.8, 3.3, 12.3, "hull")
    # 侧面刻度带（原画最醒目的识别点）
    m.box("scale_band", 3.32, 1.9, 4.5, 3.44, 2.8, 9.6, "scale")
    m.box("side_hatch", 12.5, 1.6, 7.0, 12.62, 2.4, 8.0, "detail")
    # 后顶潜望镜
    m.box("periscope", 10.4, 4.05, 4.2, 10.8, 4.6, 4.6, "hull_dark")
    m.box("periscope_cap", 10.3, 4.6, 4.1, 10.9, 4.72, 4.7, "detail")
    for tag, cx in (("l", 6.2), ("r", 9.8)):
        m.box(f"blast_low_{tag}", cx - 0.4, 2.2, 11.5, cx + 0.4, 2.5, 12.2, "blast")
        m.box(f"blast_mid_{tag}", cx - 0.56, 2.5, 11.3, cx + 0.56, 3.4, 12.3, "blast")
        m.box(f"blast_top_{tag}", cx - 0.44, 3.4, 11.5, cx + 0.44, 3.7, 12.2, "blast")
        m.diamond(f"sleeve_{tag}", cx, 2.8, 11.7, 13.2, 0.38, "barrel")
        m.diamond(f"barrel_{tag}", cx, 2.8, 13.2, 26.0, 0.26, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.8, 15.8, 16.2, 0.32, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.8, 25.7, 26.2, 0.34, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.8, 26.2, 26.3, 0.2, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


def dump_set(gun, model, extra=()):
    dump(f"{gun}_3d.json", model.base(gun, extra))
    dump(f"{gun}_3d_loaded.json", {
        "parent": f"piranport:item/{gun}_3d",
        "textures": {"muzzle_empty": NS + gun + "_muzzle_loaded"}})
    dump(f"{gun}.json", item_entry())


if __name__ == "__main__":
    dump_set("italian_triple_381mm_gun", italian381())
    dump_set("us_twin_16inch_gun", us16())
    dump("us_twin_16inch_gun_kai.json", item_entry())
    dump_set("japanese_twin_410mm_gun", japanese410(), extra=("blast", "scale"))
    dump("italian_triple_381mm_gun_kai_3d.json", {
        "parent": "piranport:item/italian_triple_381mm_gun_3d",
        "textures": Model().textures("italian_triple_381mm_gun_kai")})
    dump("italian_triple_381mm_gun_kai_3d_loaded.json", {
        "parent": "piranport:item/italian_triple_381mm_gun_kai_3d",
        "textures": {"muzzle_empty": NS + "italian_triple_381mm_gun_kai_muzzle_loaded"}})
    dump("italian_triple_381mm_gun_kai.json", item_entry())
