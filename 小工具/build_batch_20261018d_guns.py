#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-18（第四批）四门主炮的 3D 贴图与模型。

  L_36  美国三联16英寸炮(MK7)：蓝绿灰、三管卡其炮衣、座圈三角撑
  L_351 德国SKC/40型42厘米三联主炮：浅银灰、长白色炮衣
  L_307 英国单装18英寸舰炮：浅灰圆润单装、后部装填机构与侧鳍
  L_279 英国18英寸双联主炮：灰蓝双联、屋顶测距条、卡其炮衣
"""
import json
import os

from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_DIR = os.path.join(ASSETS, "textures/item")
NS = "piranport:item/"
SIZE = 32


def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _c(v):
    return max(0, min(255, int(round(v))))


def panel(base, seed, grid=8, gd=5, rivet=4, amp=2.2):
    img = Image.new("RGBA", (SIZE, SIZE)); r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if x % grid == 0 or y % grid == 0: d -= gd
            if x % grid == 2 and y % grid == 3: d -= rivet
            if y < 2: d += 5
            if y >= SIZE - 2: d -= 6
            img.putpixel((x, y), (_c(r + d), _c(g + d), _c(b + d), 255))
    return img


def blast(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE)); r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * 4
            if x % 5 == 0: d -= 12
            if x % 5 == 1: d += 4
            img.putpixel((x, y), (_c(r + d), _c(g + d), _c(b + d), 255))
    return img


def barrel_tex(base, seed):
    img = Image.new("RGBA", (SIZE, SIZE)); r, g, b = base
    for y in range(SIZE):
        shade = 1.12 - y * 0.008
        for x in range(SIZE):
            d = _noise(x, y, seed) * 3
            if x % 7 == 0: d -= 6
            if 13 <= x <= 18: d += 5
            img.putpixel((x, y), (_c(r * shade + d), _c(g * shade + d),
                                  _c(b * shade + d), 255))
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
    "us_triple_16inch_gun_mk7": dict(hull=(110, 130, 135), hull_dark=(65, 80, 85),
                                     roof=(120, 140, 145), detail=(85, 100, 105),
                                     barrel=(150, 155, 160), blast=(190, 180, 150)),
    "german_skc40_triple_420mm_gun": dict(hull=(170, 175, 178), hull_dark=(95, 100, 105),
                                          roof=(180, 185, 188), detail=(135, 140, 145),
                                          barrel=(140, 145, 150), blast=(225, 225, 220)),
    "british_single_18inch_gun": dict(hull=(195, 200, 200), hull_dark=(110, 115, 118),
                                      roof=(205, 210, 210), detail=(150, 155, 158),
                                      barrel=(160, 165, 168)),
    "british_twin_18inch_gun": dict(hull=(105, 115, 130), hull_dark=(62, 72, 85),
                                    roof=(115, 125, 140), detail=(82, 92, 105),
                                    barrel=(125, 130, 138), blast=(190, 175, 120)),
}


def gen_sprites():
    for i, (gun, c) in enumerate(SETS.items()):
        seed = 191 + i * 13
        save = lambda img, n: img.save(os.path.join(TEX_DIR, n))
        save(panel(c["hull"], seed), f"{gun}_hull.png")
        save(panel(c["hull_dark"], seed + 3), f"{gun}_hull_dark.png")
        save(panel(c["roof"], seed + 5, grid=10, gd=4, rivet=3), f"{gun}_roof.png")
        save(panel(c["detail"], seed + 7), f"{gun}_detail.png")
        save(barrel_tex(c["barrel"], seed + 9), f"{gun}_barrel.png")
        if "blast" in c:
            save(blast(c["blast"], seed + 11), f"{gun}_blast.png")
        save(muzzle(False), f"{gun}_muzzle_empty.png")
        save(muzzle(True), f"{gun}_muzzle_loaded.png")
        print("textures", gun)


def uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    w, h = x2 - x1, y2 - y1
    if face in ("east", "west"): w = z2 - z1
    elif face in ("up", "down"): w, h = x2 - x1, z2 - z1
    ou = (seed * 3.7 + len(face) * 1.3) % (max(0, 16 - w) + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max(0, 16 - h) + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


class M:
    def __init__(self): self.e = []

    def b(self, n, x1, y1, z1, x2, y2, z2, t, rot=None):
        fj = {}; seed = len(self.e) + 1
        for f in ("north", "south", "west", "east", "up", "down"):
            fj[f] = {"texture": "#" + t, "uv": uv_for(f, x1, y1, z1, x2, y2, z2, seed)}
        el = {"name": n, "from": [x1, y1, z1], "to": [x2, y2, z2], "faces": fj}
        if rot: el["rotation"] = rot
        self.e.append(el)

    def d(self, n, cx, cy, z1, z2, half, t, full=False):
        self.b(n, cx - half, cy - half, z1, cx + half, cy + half, z2, t,
               rot={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})

    def ring(self, n, cx, cz, r, y1, y2, t):
        for i, (fx, fz) in enumerate(((1, .35), (.96, .6), (.85, .8), (.7, .92), (.5, 1))):
            self.b(f"{n}_{i}", cx - r * fx, y1, cz - r * fz, cx + r * fx, y2, cz + r * fz, t)

    def tex(self, gun, extra=()):
        d = {k: NS + gun + "_" + k for k in
             ["hull", "hull_dark", "roof", "barrel", "detail",
              "muzzle_empty", "muzzle_loaded"] + list(extra)}
        d["particle"] = NS + gun + "_hull"
        return d

    def out(self, gun, extra=()):
        return {"parent": "minecraft:block/block", "gui_light": "front",
                "ambientocclusion": False, "textures": self.tex(gun, extra),
                "elements": self.e}


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


def mk7():
    m = M()
    m.ring("ring", 8, 7.5, 4.4, 0, .9, "hull_dark")
    for n, x1, z1, x2, z2 in (("fin_f", 6.5, 11.8, 9.5, 12.6), ("fin_b", 6.5, 2.4, 9.5, 3.2),
                              ("fin_p", 3.0, 6.5, 3.8, 9.5), ("fin_s", 12.2, 6.5, 13.0, 9.5)):
        m.b(n, x1, .3, z1, x2, 1.0, z2, "hull")
    m.b("body", 3.5, .9, 2.8, 12.5, 3.5, 12.0, "hull")
    m.b("upper", 3.8, 3.5, 3.1, 12.2, 3.95, 11.9, "hull")
    m.b("roof", 4.0, 3.95, 3.2, 12.0, 4.25, 12.0, "roof")
    m.b("slope_f", 4.1, 3.75, 11.2, 11.9, 4.1, 12.1, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 3.95, 11.9]})
    m.b("rf_box", 8.6, 4.25, 3.8, 10.4, 4.6, 5.2, "detail")
    m.b("side_box", 12.1, 3.2, 5.5, 13.1, 3.8, 7.0, "detail")
    for tag, cx in (("l", 5.8), ("m", 8.0), ("r", 10.2)):
        m.b(f"bl_l_{tag}", cx - .4, 2.15, 11.5, cx + .4, 2.5, 12.2, "blast")
        m.b(f"bl_m_{tag}", cx - .56, 2.5, 11.3, cx + .56, 3.3, 12.3, "blast")
        m.b(f"bl_t_{tag}", cx - .44, 3.3, 11.5, cx + .44, 3.6, 12.2, "blast")
        m.d(f"s_{tag}", cx, 2.6, 11.6, 13.2, .32, "hull_dark")
        m.d(f"b_{tag}", cx, 2.6, 13.2, 26.0, .24, "barrel")
        m.d(f"r_{tag}", cx, 2.6, 15.6, 16.0, .3, "barrel")
        m.d(f"mr_{tag}", cx, 2.6, 25.7, 26.2, .32, "barrel")
        m.d(f"mu_{tag}", cx, 2.6, 26.2, 26.3, .19, "muzzle_empty")
    return m


def skc40():
    m = M()
    m.ring("ring", 8, 7.5, 4.3, 0, .9, "hull_dark")
    m.b("body", 3.6, .9, 2.9, 12.4, 3.0, 11.9, "hull")
    m.b("slope_f", 3.9, 2.2, 11.3, 12.1, 3.2, 12.1, "hull",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 2.6, 11.9]})
    m.b("roof", 4.0, 3.0, 3.2, 12.0, 3.65, 11.8, "roof")
    m.b("periscope", 9.2, 3.65, 4.2, 9.6, 4.05, 4.6, "hull_dark")
    m.b("peri_cap", 9.1, 4.05, 4.1, 9.7, 4.17, 4.7, "detail")
    m.b("rear_box", 2.9, 2.8, 4.8, 4.0, 3.5, 6.4, "hull")
    m.b("rear_box_top", 3.0, 3.5, 4.9, 3.9, 3.62, 6.3, "detail")
    for tag, cx in (("l", 5.7), ("m", 8.0), ("r", 10.3)):
        for i, (z1, z2, half) in enumerate(((11.4, 12.6, .5), (12.6, 13.6, .42),
                                            (13.6, 14.4, .34), (14.4, 15.0, .28))):
            m.d(f"bl_{tag}_{i}", cx, 2.4, z1, z2, half, "blast")
        m.d(f"b_{tag}", cx, 2.4, 15.0, 26.5, .22, "barrel")
        m.d(f"r_{tag}", cx, 2.4, 17.2, 17.6, .28, "barrel")
        m.d(f"mr_{tag}", cx, 2.4, 26.1, 26.6, .3, "barrel")
        m.d(f"mu_{tag}", cx, 2.4, 26.6, 26.7, .18, "muzzle_empty")
    return m


def single18():
    m = M()
    m.ring("ring", 8, 7.5, 4.3, 0, 1.0, "hull_dark")
    m.b("lower", 4.0, 1.0, 3.4, 12.0, 2.8, 11.2, "hull")
    m.b("upper", 4.3, 2.8, 3.7, 11.7, 3.8, 10.9, "hull")
    m.b("roof", 4.6, 3.8, 4.0, 11.4, 4.15, 10.6, "roof")
    m.b("front_round", 4.8, 1.0, 10.9, 11.2, 3.6, 11.6, "hull")
    m.b("rear_mass", 5.2, 1.2, 2.2, 10.8, 4.2, 3.6, "hull")
    m.b("rear_fin_p", 4.4, 2.2, 2.4, 5.2, 3.2, 3.4, "hull_dark")
    m.b("rear_fin_s", 10.8, 2.2, 2.4, 11.6, 3.2, 3.4, "hull_dark")
    m.b("rear_detail", 6.0, 3.4, 2.4, 10.0, 3.9, 3.2, "detail")
    m.d("sleeve", 8.0, 2.8, 11.2, 12.8, .45, "hull_dark")
    m.d("barrel", 8.0, 2.8, 12.8, 26.5, .26, "barrel")
    m.d("ring", 8.0, 2.8, 15.4, 15.8, .33, "barrel")
    m.d("muzzle_ring", 8.0, 2.8, 26.0, 26.5, .35, "barrel")
    m.d("muzzle", 8.0, 2.8, 26.5, 26.6, .2, "muzzle_empty")
    return m


def twin18():
    m = M()
    m.ring("ring", 8, 7.5, 4.5, 0, 1.1, "hull_dark")
    m.b("body", 3.6, 1.1, 2.9, 12.4, 3.7, 11.9, "hull")
    m.b("roof", 4.0, 3.7, 3.2, 12.0, 4.1, 11.8, "roof")
    m.b("slope_f", 4.1, 3.5, 11.0, 11.9, 3.9, 12.0, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 3.7, 11.7]})
    m.b("rf_base", 4.3, 4.1, 3.6, 11.7, 4.4, 5.0, "hull_dark")
    m.b("rf_center", 5.0, 4.4, 3.7, 11.0, 4.65, 4.9, "hull")
    m.b("rf_cap_p", 4.1, 4.25, 3.5, 4.5, 4.5, 5.1, "hull_dark")
    m.b("rf_cap_s", 11.5, 4.25, 3.5, 11.9, 4.5, 5.1, "hull_dark")
    for tag, cx in (("l", 6.3), ("r", 9.7)):
        m.b(f"bl_l_{tag}", cx - .4, 2.3, 11.5, cx + .4, 2.6, 12.2, "blast")
        m.b(f"bl_m_{tag}", cx - .56, 2.6, 11.3, cx + .56, 3.4, 12.3, "blast")
        m.b(f"bl_t_{tag}", cx - .44, 3.4, 11.5, cx + .44, 3.7, 12.2, "blast")
        m.d(f"s_{tag}", cx, 2.9, 11.7, 13.2, .38, "hull_dark")
        m.d(f"b_{tag}", cx, 2.9, 13.2, 26.0, .28, "barrel")
        m.d(f"r_{tag}", cx, 2.9, 15.8, 16.2, .34, "barrel")
        m.d(f"mr_{tag}", cx, 2.9, 25.7, 26.2, .36, "barrel")
        m.d(f"mu_{tag}", cx, 2.9, 26.2, 26.3, .22, "muzzle_empty")
    return m


def dump(name, data):
    p = os.path.join(ITEM_DIR, name)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2); f.write("\n")
    print("wrote", p)


def dump_set(gun, model, extra=()):
    dump(f"{gun}_3d.json", model.out(gun, extra))
    dump(f"{gun}_3d_loaded.json", {"parent": f"piranport:item/{gun}_3d",
                                   "textures": {"muzzle_empty": NS + gun + "_muzzle_loaded"}})
    dump(f"{gun}.json", item_entry())


if __name__ == "__main__":
    gen_sprites()
    dump_set("us_triple_16inch_gun_mk7", mk7(), extra=("blast",))
    dump_set("german_skc40_triple_420mm_gun", skc40(), extra=("blast",))
    dump_set("british_single_18inch_gun", single18())
    dump_set("british_twin_18inch_gun", twin18(), extra=("blast",))
