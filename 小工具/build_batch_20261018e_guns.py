#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""批量生成 2026-10-18（第五批）五门主炮的 3D 贴图与模型。

  L_224 美国双联18英寸炮：灰蓝宽炮塔、两管、屋顶测距盒
  L_26  日本46厘米三连装炮：灰色三管、白色炮衣、后顶设备盒
  L_433 苏联三联460毫米炮：棱角灰塔、两舷测距翼、屋顶设备、高座圈
  L_289 英国双联20英寸主炮：浅灰圆塔、卡其炮衣、后部鳍板装填机构
  L_382 美国双联20英寸主炮：蓝灰低长炮塔、两管、侧盒
"""
import json
import os
from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_DIR = os.path.join(ASSETS, "textures/item")
NS = "piranport:item/"


def _n(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1


def _c(v):
    return max(0, min(255, int(round(v))))


def panel(base, seed, gd=5, rivet=4, grid=8):
    im = Image.new("RGBA", (32, 32)); r, g, b = base
    for y in range(32):
        for x in range(32):
            d = _n(x, y, seed) * 2.2
            if x % grid == 0 or y % grid == 0: d -= gd
            if x % grid == 2 and y % grid == 3: d -= rivet
            if y < 2: d += 5
            if y >= 30: d -= 6
            im.putpixel((x, y), (_c(r + d), _c(g + d), _c(b + d), 255))
    return im


def blast(base, seed):
    im = Image.new("RGBA", (32, 32)); r, g, b = base
    for y in range(32):
        for x in range(32):
            d = _n(x, y, seed) * 4
            if x % 5 == 0: d -= 12
            if x % 5 == 1: d += 4
            im.putpixel((x, y), (_c(r + d), _c(g + d), _c(b + d), 255))
    return im


def barrel(base, seed):
    im = Image.new("RGBA", (32, 32)); r, g, b = base
    for y in range(32):
        s = 1.12 - y * .008
        for x in range(32):
            d = _n(x, y, seed) * 3
            if x % 7 == 0: d -= 6
            if 13 <= x <= 18: d += 5
            im.putpixel((x, y), (_c(r * s + d), _c(g * s + d), _c(b * s + d), 255))
    return im


def muzzle(loaded):
    if loaded:
        im = Image.new("RGBA", (32, 32), (196, 158, 72, 255))
        for y in range(32):
            for x in range(32):
                if x < 3 or y < 3 or x >= 29 or y >= 29:
                    im.putpixel((x, y), (120, 94, 42, 255))
        return im
    im = Image.new("RGBA", (32, 32), (20, 20, 22, 255))
    for y in range(32):
        for x in range(32):
            if x < 2 or y < 2 or x >= 30 or y >= 30:
                im.putpixel((x, y), (44, 46, 48, 255))
    return im


SETS = {
    "us_twin_18inch_gun": dict(hull=(115, 130, 150), dark=(65, 78, 95), roof=(125, 140, 160),
                              detail=(90, 105, 125), barrel=(145, 150, 158)),
    "japanese_triple_460mm_gun": dict(hull=(140, 145, 150), dark=(80, 85, 90), roof=(155, 160, 165),
                                     detail=(110, 115, 120), barrel=(120, 125, 130), blast=(205, 200, 190)),
    "soviet_triple_460mm_gun": dict(hull=(115, 120, 116), dark=(65, 70, 68), roof=(125, 130, 126),
                                   detail=(88, 94, 90), barrel=(110, 115, 112)),
    "british_twin_20inch_gun": dict(hull=(185, 188, 190), dark=(100, 105, 108), roof=(195, 198, 200),
                                   detail=(150, 155, 158), barrel=(160, 165, 168), blast=(195, 180, 120)),
    "us_twin_20inch_gun": dict(hull=(110, 125, 140), dark=(62, 75, 88), roof=(120, 135, 150),
                              detail=(85, 100, 115), barrel=(140, 145, 152)),
}


def sprites():
    for i, (g, c) in enumerate(SETS.items()):
        s = 251 + i * 13
        sv = lambda im, n: im.save(os.path.join(TEX_DIR, n))
        sv(panel(c["hull"], s), f"{g}_hull.png")
        sv(panel(c["dark"], s + 3), f"{g}_hull_dark.png")
        sv(panel(c["roof"], s + 5, gd=4, rivet=3, grid=10), f"{g}_roof.png")
        sv(panel(c["detail"], s + 7), f"{g}_detail.png")
        sv(barrel(c["barrel"], s + 9), f"{g}_barrel.png")
        if "blast" in c:
            sv(blast(c["blast"], s + 11), f"{g}_blast.png")
        sv(muzzle(False), f"{g}_muzzle_empty.png")
        sv(muzzle(True), f"{g}_muzzle_loaded.png")
        print("textures", g)


def uv(f, x1, y1, z1, x2, y2, z2, seed):
    w, h = x2 - x1, y2 - y1
    if f in ("east", "west"): w = z2 - z1
    elif f in ("up", "down"): w, h = x2 - x1, z2 - z1
    ou = (seed * 3.7 + len(f) * 1.3) % (max(0, 16 - w) + 1e-6)
    ov = (seed * 5.1 + len(f) * 2.9) % (max(0, 16 - h) + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


class M:
    def __init__(self): self.e = []

    def b(self, n, x1, y1, z1, x2, y2, z2, t, rot=None):
        fj = {}; seed = len(self.e) + 1
        for f in ("north", "south", "west", "east", "up", "down"):
            fj[f] = {"texture": "#" + t, "uv": uv(f, x1, y1, z1, x2, y2, z2, seed)}
        el = {"name": n, "from": [x1, y1, z1], "to": [x2, y2, z2], "faces": fj}
        if rot: el["rotation"] = rot
        self.e.append(el)

    def d(self, n, cx, cy, z1, z2, half, t):
        self.b(n, cx - half, cy - half, z1, cx + half, cy + half, z2, t,
               rot={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})

    def ring(self, n, r, y1, y2, t, cz=7.5):
        for i, (fx, fz) in enumerate(((1, .35), (.96, .6), (.85, .8), (.7, .92), (.5, 1))):
            self.b(f"{n}_{i}", 8 - r * fx, y1, cz - r * fz, 8 + r * fx, y2, cz + r * fz, t)

    def tex(self, g, extra=()):
        d = {k: NS + g + "_" + k for k in
             ["hull", "hull_dark", "roof", "barrel", "detail",
              "muzzle_empty", "muzzle_loaded"] + list(extra)}
        d["particle"] = NS + g + "_hull"
        return d

    def out(self, g, extra=()):
        return {"parent": "minecraft:block/block", "gui_light": "front",
                "ambientocclusion": False, "textures": self.tex(g, extra), "elements": self.e}


DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1], "scale": [.65, .65, .65]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1], "scale": [.65, .65, .65]},
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.6, .6, .6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.6, .6, .6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def entry():
    return {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}


def us18():
    m = M()
    m.ring("ring", 4.4, 0, .9, "hull_dark")
    m.b("body", 3.5, .9, 2.8, 12.5, 3.5, 12.0, "hull")
    m.b("roof", 4.0, 3.5, 3.2, 12.0, 3.9, 12.0, "roof")
    m.b("slope_f", 4.1, 3.3, 11.2, 11.9, 3.7, 12.1, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 3.5, 11.9]})
    m.b("rf_box", 8.8, 3.9, 3.8, 10.6, 4.25, 5.2, "detail")
    m.b("side_box", 12.1, 3.1, 5.4, 13.1, 3.7, 7.0, "detail")
    for t, cx in (("l", 6.2), ("r", 9.8)):
        m.d("s_" + t, cx, 2.7, 11.4, 13.0, .4, "hull_dark")
        m.d("b_" + t, cx, 2.7, 13.0, 26.2, .27, "barrel")
        m.d("r_" + t, cx, 2.7, 15.8, 16.2, .33, "barrel")
        m.d("mr_" + t, cx, 2.7, 25.9, 26.4, .35, "barrel")
        m.d("mu_" + t, cx, 2.7, 26.4, 26.5, .21, "muzzle_empty")
    return m


def jp460():
    m = M()
    m.ring("ring", 4.4, 0, 1.0, "hull_dark")
    m.b("body", 3.5, 1.0, 2.7, 12.5, 3.6, 11.9, "hull")
    m.b("roof", 4.0, 3.6, 3.1, 12.0, 4.0, 11.8, "roof")
    m.b("slope_f", 4.1, 3.4, 11.0, 11.9, 3.8, 12.0, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 3.6, 11.7]})
    m.b("rear_box", 8.7, 4.0, 3.6, 10.6, 4.45, 5.0, "detail")
    for t, cx in (("l", 5.8), ("m", 8.0), ("r", 10.2)):
        m.b("bl_l_" + t, cx - .4, 2.2, 11.5, cx + .4, 2.5, 12.2, "blast")
        m.b("bl_m_" + t, cx - .56, 2.5, 11.3, cx + .56, 3.3, 12.3, "blast")
        m.b("bl_t_" + t, cx - .44, 3.3, 11.5, cx + .44, 3.6, 12.2, "blast")
        m.d("s_" + t, cx, 2.7, 11.5, 13.2, .32, "hull_dark")
        m.d("b_" + t, cx, 2.7, 13.2, 26.0, .24, "barrel")
        m.d("r_" + t, cx, 2.7, 15.6, 16.0, .3, "barrel")
        m.d("mr_" + t, cx, 2.7, 25.7, 26.2, .32, "barrel")
        m.d("mu_" + t, cx, 2.7, 26.2, 26.3, .19, "muzzle_empty")
    return m


def soviet460():
    m = M()
    m.ring("ring", 4.5, 0, 1.2, "hull_dark")
    m.b("body", 3.4, 1.2, 2.7, 12.6, 3.4, 12.1, "hull")
    m.b("upper", 3.7, 3.4, 3.0, 12.3, 4.0, 11.9, "hull")
    m.b("roof", 4.1, 4.0, 3.3, 11.9, 4.3, 11.7, "roof")
    m.b("slope_f", 4.2, 3.8, 11.2, 11.8, 4.15, 12.1, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 4.0, 11.7]})
    m.b("wing_p", 2.6, 3.4, 4.8, 3.9, 4.1, 6.8, "hull")
    m.b("wing_s", 12.1, 3.4, 4.8, 13.4, 4.1, 6.8, "hull")
    m.b("top_box", 7.4, 4.3, 4.0, 8.6, 4.75, 5.4, "detail")
    m.b("top_cyl", 7.6, 4.75, 4.2, 8.4, 4.95, 5.2, "hull_dark")
    for t, cx in (("l", 5.7), ("m", 8.0), ("r", 10.3)):
        m.d("s_" + t, cx, 2.6, 11.5, 13.0, .36, "hull_dark")
        m.d("b_" + t, cx, 2.6, 13.0, 26.2, .24, "barrel")
        m.d("r_" + t, cx, 2.6, 15.8, 16.2, .3, "barrel")
        m.d("mr_" + t, cx, 2.6, 25.9, 26.4, .32, "barrel")
        m.d("mu_" + t, cx, 2.6, 26.4, 26.5, .19, "muzzle_empty")
    return m


def brit20():
    m = M()
    m.ring("ring", 4.4, 0, 1.0, "hull_dark")
    m.b("body", 4.0, 1.0, 3.0, 12.0, 3.0, 11.5, "hull")
    m.b("upper", 4.2, 3.0, 3.3, 11.8, 3.9, 11.2, "hull")
    m.b("roof", 4.5, 3.9, 3.6, 11.5, 4.2, 10.9, "roof")
    m.b("rear_mass", 5.0, 1.2, 2.2, 11.0, 4.1, 3.4, "hull")
    m.b("fin_p", 4.3, 2.2, 2.4, 5.0, 3.2, 3.3, "hull_dark")
    m.b("fin_s", 11.0, 2.2, 2.4, 11.7, 3.2, 3.3, "hull_dark")
    m.b("rear_detail", 6.0, 3.4, 2.4, 10.0, 3.9, 3.1, "detail")
    for t, cx in (("l", 6.3), ("r", 9.7)):
        m.b("bl_l_" + t, cx - .4, 2.3, 11.3, cx + .4, 2.6, 12.1, "blast")
        m.b("bl_m_" + t, cx - .56, 2.6, 11.1, cx + .56, 3.4, 12.2, "blast")
        m.b("bl_t_" + t, cx - .44, 3.4, 11.3, cx + .44, 3.7, 12.1, "blast")
        m.d("s_" + t, cx, 2.9, 11.5, 13.2, .38, "hull_dark")
        m.d("b_" + t, cx, 2.9, 13.2, 26.2, .28, "barrel")
        m.d("r_" + t, cx, 2.9, 15.8, 16.2, .34, "barrel")
        m.d("mr_" + t, cx, 2.9, 25.9, 26.4, .36, "barrel")
        m.d("mu_" + t, cx, 2.9, 26.4, 26.5, .22, "muzzle_empty")
    return m


def us20():
    m = M()
    m.ring("ring", 4.5, 0, .9, "hull_dark")
    m.b("body", 3.4, .9, 2.6, 12.6, 3.4, 12.2, "hull")
    m.b("roof", 3.9, 3.4, 3.0, 12.1, 3.8, 12.1, "roof")
    m.b("slope_f", 4.0, 3.2, 11.3, 12.0, 3.6, 12.2, "roof",
        rot={"angle": -22.5, "axis": "x", "origin": [8, 3.4, 12.0]})
    m.b("side_box_p", 2.9, 3.0, 5.0, 3.9, 3.6, 6.6, "detail")
    m.b("side_box_s", 12.1, 3.0, 5.0, 13.1, 3.6, 6.6, "detail")
    for t, cx in (("l", 6.3), ("r", 9.7)):
        m.d("s_" + t, cx, 2.6, 11.5, 13.0, .38, "hull_dark")
        m.d("b_" + t, cx, 2.6, 13.0, 26.2, .27, "barrel")
        m.d("r_" + t, cx, 2.6, 15.8, 16.2, .33, "barrel")
        m.d("mr_" + t, cx, 2.6, 25.9, 26.4, .35, "barrel")
        m.d("mu_" + t, cx, 2.6, 26.4, 26.5, .21, "muzzle_empty")
    return m


def dump(name, data):
    p = os.path.join(ITEM_DIR, name)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2); f.write("\n")
    print("wrote", p)


def dump_set(g, model, extra=()):
    dump(f"{g}_3d.json", model.out(g, extra))
    dump(f"{g}_3d_loaded.json", {"parent": f"piranport:item/{g}_3d",
                                 "textures": {"muzzle_empty": NS + g + "_muzzle_loaded"}})
    dump(f"{g}.json", entry())


if __name__ == "__main__":
    sprites()
    dump_set("us_twin_18inch_gun", us18())
    dump_set("japanese_triple_460mm_gun", jp460(), extra=("blast",))
    dump_set("soviet_triple_460mm_gun", soviet460())
    dump_set("british_twin_20inch_gun", brit20(), extra=("blast",))
    dump_set("us_twin_20inch_gun", us20())
