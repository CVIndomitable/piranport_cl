#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-18（第三批）四门主炮的 3D 贴图与模型。

  L_29  德国双联406毫米炮：深蓝灰、双管、屋顶潜望镜、两舷测距翼、座圈前盾
  L_31  英国三联16英寸炮：蓝灰方正炮塔、三管、后右屋顶测距盒
  L_147 英国三联16英寸炮(改)：继承 L_31 几何，改深蓝顶/白侧/绿块
  L_35  美国三联16英寸炮(MK6)：蓝灰、三管带红色环带与卡其炮衣、顶部 MK6 指挥仪盒
"""
import json
import os

from PIL import Image

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_DIR = os.path.join(ASSETS, "textures/item")
NS = "piranport:item/"
SIZE = 32


# ---------------- 贴图 ----------------
def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel(base, seed, grid=8, grid_dark=5, rivet=4, amp=2.2):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if x % grid == 0 or y % grid == 0:
                d -= grid_dark
            if x % grid == 2 and y % grid == 3:
                d -= rivet
            if y < 2:
                d += 5
            if y >= SIZE - 2:
                d -= 6
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def blast(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * 4
            if x % 5 == 0:
                d -= 12
            if x % 5 == 1:
                d += 4
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def barrel_tex(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        shade = 1.12 - y * 0.008
        for x in range(SIZE):
            d = _noise(x, y, seed) * 3
            if x % 7 == 0:
                d -= 6
            if 13 <= x <= 18:
                d += 5
            img.putpixel((x, y), (_clamp(r * shade + d), _clamp(g * shade + d),
                                  _clamp(b * shade + d), 255))
    return img


def band_tex(seed):
    img = Image.new("RGBA", (SIZE, SIZE), (176, 66, 54, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * 4
            if y % 6 == 0:
                d -= 18
            img.putpixel((x, y), (_clamp(176 + d), _clamp(66 + d), _clamp(54 + d), 255))
    return img


def muzzle(loaded):
    if loaded:
        img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
        for y in range(SIZE):
            for x in range(SIZE):
                if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                    img.putpixel((x, y), (120, 94, 42, 255))
                elif 10 <= x < 22 and 10 <= y < 16:
                    img.putpixel((x, y), (232, 201, 110, 255))
        return img
    img = Image.new("RGBA", (SIZE, SIZE), (20, 20, 22, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), (44, 46, 48, 255))
    return img


SETS = {
    "german_twin_406mm_gun": dict(
        hull=(95, 100, 110), hull_dark=(58, 62, 70), roof=(108, 113, 122),
        detail=(78, 84, 94), barrel=(92, 97, 106)),
    "british_triple_16inch_gun": dict(
        hull=(105, 125, 140), hull_dark=(64, 79, 91), roof=(122, 142, 157),
        detail=(84, 99, 114), barrel=(110, 115, 122)),
    "british_triple_16inch_gun_kai": dict(
        hull=(188, 188, 182), hull_dark=(32, 42, 66), roof=(42, 58, 88),
        detail=(98, 112, 86), barrel=(108, 113, 122)),
    "us_triple_16inch_gun_mk6": dict(
        hull=(105, 120, 135), hull_dark=(64, 77, 89), roof=(118, 133, 148),
        detail=(84, 99, 114), barrel=(152, 157, 162),
        blast=(192, 178, 142), band=True),
}


def gen_sprites():
    for i, (gun, c) in enumerate(SETS.items()):
        seed = 131 + i * 13
        save = lambda img, n: img.save(os.path.join(TEX_DIR, n))
        save(panel(c["hull"], seed), f"{gun}_hull.png")
        save(panel(c["hull_dark"], seed + 3), f"{gun}_hull_dark.png")
        save(panel(c["roof"], seed + 5, grid=10, grid_dark=4, rivet=3), f"{gun}_roof.png")
        save(panel(c["detail"], seed + 7), f"{gun}_detail.png")
        save(barrel_tex(c["barrel"], seed + 9), f"{gun}_barrel.png")
        if "blast" in c:
            save(blast(c["blast"], seed + 11), f"{gun}_blast.png")
        if c.get("band"):
            save(band_tex(seed + 13), f"{gun}_band.png")
        save(muzzle(False), f"{gun}_muzzle_empty.png")
        save(muzzle(True), f"{gun}_muzzle_loaded.png")
        print("textures", gun)


# ---------------- 模型 ----------------
def uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"):
        w = z2 - z1
    elif face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    ou = (seed * 3.7 + len(face) * 1.3) % (max(0.0, 16.0 - w) + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max(0.0, 16.0 - h) + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


class Model:
    def __init__(self):
        self.elements = []

    def box(self, name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
            full_uv_faces=()):
        fj, seed = {}, len(self.elements) + 1
        for face in ("north", "south", "west", "east", "up", "down"):
            entry = {"texture": "#" + (faces or {}).get(face, tex)}
            entry["uv"] = [0, 0, 16, 16] if face in full_uv_faces else \
                uv_for(face, x1, y1, z1, x2, y2, z2, seed)
            fj[face] = entry
        el = {"name": name, "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
              "to": [round(x2, 3), round(y2, 3), round(z2, 3)], "faces": fj}
        if rotation:
            el["rotation"] = rotation
        self.elements.append(el)

    def diamond(self, name, cx, cy, z1, z2, half, tex, faces=None,
                full_uv_faces=()):
        self.box(name, cx - half, cy - half, z1, cx + half, cy + half, z2, tex,
                 faces=faces, full_uv_faces=full_uv_faces,
                 rotation={"angle": 45, "axis": "z",
                           "origin": [cx, cy, (z1 + z2) / 2]})

    def round_footprint(self, name, cx, cz, radius, y1, y2, tex):
        for i, (fx, fz) in enumerate(((1, .35), (.96, .6), (.85, .8),
                                      (.7, .92), (.5, 1))):
            self.box(f"{name}_{i}", cx - radius * fx, y1, cz - radius * fz,
                     cx + radius * fx, y2, cz + radius * fz, tex)

    def textures(self, gun, extra=()):
        keys = ["hull", "hull_dark", "roof", "barrel", "detail",
                "muzzle_empty", "muzzle_loaded"] + list(extra)
        d = {k: NS + gun + "_" + k for k in keys}
        d["particle"] = NS + gun + "_hull"
        return d

    def base(self, gun, extra=()):
        return {"parent": "minecraft:block/block", "gui_light": "front",
                "ambientocclusion": False, "textures": self.textures(gun, extra),
                "elements": self.elements}


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1], "scale": [.65, .65, .65]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1], "scale": [.65, .65, .65]},
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.6, .6, .6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.6, .6, .6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def item_entry():
    return {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}


def german406():
    m = Model()
    m.round_footprint("ring", 8, 7.5, 4.4, 0, .9, "hull_dark")
    m.box("front_shield", 4.6, .9, 10.8, 11.4, 1.6, 12.2, "hull")
    m.box("front_step", 5.6, .5, 11.7, 7.0, .95, 12.3, "detail")
    m.box("lower", 3.5, 1.3, 2.8, 12.5, 2.7, 11.8, "hull")
    m.box("upper", 3.8, 2.7, 3.1, 12.2, 3.9, 11.6, "hull")
    m.box("roof", 4.1, 3.9, 3.3, 11.9, 4.25, 11.5, "roof")
    m.box("roof_slope_front", 4.2, 3.7, 11.0, 11.8, 4.05, 12.0, "hull",
          rotation={"angle": -22.5, "axis": "x", "origin": [8, 3.9, 11.5]})
    m.box("slope_port", 3.7, 3.5, 3.3, 4.2, 4.1, 11.5, "hull",
          rotation={"angle": -22.5, "axis": "z", "origin": [3.95, 3.8, 7.4]})
    m.box("slope_stbd", 11.8, 3.5, 3.3, 12.3, 4.1, 11.5, "hull",
          rotation={"angle": 22.5, "axis": "z", "origin": [12.05, 3.8, 7.4]})
    m.box("wing_port", 2.7, 3.5, 5.0, 4.0, 4.2, 6.8, "hull_dark")
    m.box("wing_stbd", 12.0, 3.5, 5.0, 13.3, 4.2, 6.8, "hull_dark")
    m.box("wing_top_port", 2.8, 4.2, 5.1, 3.9, 4.32, 6.7, "detail")
    m.box("wing_top_stbd", 12.1, 4.2, 5.1, 13.2, 4.32, 6.7, "detail")
    m.box("periscope", 7.6, 4.25, 4.2, 8.1, 4.65, 4.6, "detail")
    m.box("periscope_cap", 7.5, 4.65, 4.1, 8.2, 4.77, 4.7, "hull_dark")
    m.box("front_plate", 4.4, 1.3, 11.5, 11.6, 3.4, 12.0, "hull")
    for tag, cx in (("l", 6.4), ("r", 9.6)):
        m.diamond(f"port_{tag}", cx, 2.8, 11.95, 12.07, 0.5, "hull_dark")
        m.diamond(f"sleeve_{tag}", cx, 2.8, 11.4, 13.0, 0.42, "hull_dark")
        m.diamond(f"barrel_{tag}", cx, 2.8, 13.0, 26.0, 0.26, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.8, 15.5, 15.9, 0.32, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.8, 25.7, 26.2, 0.34, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.8, 26.2, 26.3, 0.21, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def brit_triple16():
    m = Model()
    m.round_footprint("ring", 8, 7.5, 4.5, 0, 1.1, "hull_dark")
    m.box("lower", 3.6, 1.1, 2.9, 12.4, 2.7, 11.9, "hull")
    m.box("upper", 3.9, 2.7, 3.2, 12.1, 3.8, 11.7, "hull")
    m.box("front_slope", 4.0, 2.4, 11.6, 12.0, 3.6, 12.3, "hull",
          rotation={"angle": -22.5, "axis": "x", "origin": [8, 2.9, 12.1]})
    m.box("roof", 4.0, 3.8, 3.2, 12.0, 4.2, 11.8, "roof")
    m.box("roof_front", 4.1, 3.6, 11.0, 11.9, 4.0, 12.0, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8, 3.8, 11.7]})
    m.box("rf_box", 8.9, 4.2, 4.0, 10.8, 4.6, 5.6, "detail")
    m.box("rf_cap", 9.0, 4.6, 4.1, 10.7, 4.72, 5.5, "hull_dark")
    m.box("side_box", 12.1, 3.1, 4.5, 13.1, 3.75, 6.0, "hull")
    for tag, cx in (("l", 5.7), ("m", 8.0), ("r", 10.3)):
        m.diamond(f"port_{tag}", cx, 2.7, 12.18, 12.3, 0.38, "hull_dark")
        m.diamond(f"sleeve_{tag}", cx, 2.7, 11.7, 13.0, 0.34, "hull_dark")
        m.diamond(f"barrel_{tag}", cx, 2.7, 13.0, 26.0, 0.22, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.7, 15.4, 15.8, 0.28, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.7, 25.7, 26.2, 0.3, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.7, 26.2, 26.3, 0.18, "barrel",
                  faces={"south": "muzzle_empty"}, full_uv_faces=("south",))
    return m


def us_mk6():
    m = Model()
    m.round_footprint("ring", 8, 7.5, 4.5, 0, 1.0, "hull_dark")
    m.box("body", 3.5, 1.0, 2.8, 12.5, 3.6, 11.9, "hull")
    m.box("roof", 4.0, 3.6, 3.2, 12.0, 4.3, 11.9, "roof")
    m.box("roof_front", 4.1, 3.4, 11.1, 11.9, 4.05, 12.1, "roof",
          rotation={"angle": -22.5, "axis": "x", "origin": [8, 3.6, 11.8]})
    m.box("mk6_box", 8.8, 4.3, 3.9, 10.9, 4.95, 5.7, "detail")
    m.box("mk6_cap", 8.9, 4.95, 4.0, 10.8, 5.07, 5.6, "hull_dark")
    m.box("mk6_face", 10.9, 4.4, 4.0, 11.0, 4.85, 5.6, "hull_dark")
    m.box("side_box", 12.1, 3.2, 4.6, 13.2, 3.9, 6.2, "detail")
    m.box("front_plate", 4.2, 1.0, 11.6, 11.8, 3.4, 12.1, "hull")
    for tag, cx in (("l", 5.8), ("m", 8.0), ("r", 10.2)):
        m.box(f"blast_low_{tag}", cx - 0.38, 2.2, 11.5, cx + 0.38, 2.5, 12.2, "blast")
        m.box(f"blast_mid_{tag}", cx - 0.54, 2.5, 11.3, cx + 0.54, 3.3, 12.3, "blast")
        m.box(f"blast_top_{tag}", cx - 0.42, 3.3, 11.5, cx + 0.42, 3.6, 12.2, "blast")
        m.diamond(f"band_{tag}", cx, 2.7, 13.4, 13.7, 0.34, "band")
        m.diamond(f"barrel_{tag}", cx, 2.7, 13.7, 26.0, 0.24, "barrel")
        m.diamond(f"ring_{tag}", cx, 2.7, 15.9, 16.3, 0.3, "barrel")
        m.diamond(f"muzzle_ring_{tag}", cx, 2.7, 25.7, 26.2, 0.32, "barrel")
        m.diamond(f"muzzle_{tag}", cx, 2.7, 26.2, 26.3, 0.19, "barrel",
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
    gen_sprites()
    dump_set("german_twin_406mm_gun", german406())
    dump_set("british_triple_16inch_gun", brit_triple16())
    dump_set("us_triple_16inch_gun_mk6", us_mk6(), extra=("blast", "band"))
    dump("british_triple_16inch_gun_kai_3d.json", {
        "parent": "piranport:item/british_triple_16inch_gun_3d",
        "textures": Model().textures("british_triple_16inch_gun_kai")})
    dump("british_triple_16inch_gun_kai_3d_loaded.json", {
        "parent": "piranport:item/british_triple_16inch_gun_kai_3d",
        "textures": {"muzzle_empty": NS + "british_triple_16inch_gun_kai_muzzle_loaded"}})
    dump("british_triple_16inch_gun_kai.json", item_entry())
