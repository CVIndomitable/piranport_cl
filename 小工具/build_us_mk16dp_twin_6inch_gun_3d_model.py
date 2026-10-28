#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""美国 MK16DP 双联 6 英寸高射炮的手持 3D 模型与材质（原画 Equip_L_350）。

按 2026-10-25 批次火炮的同一套 BEWLR 输出格式：
  models/item/<id>.json            builtin/entity 物品入口（只放 display）
  models/item/<id>_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/<id>_3d_loaded.json  装填 3D 炮塔（只覆盖炮口材质）
  textures/item/<id>_*.png         32x32 一套材质

形体按原画从零建模：深蓝灰的圆鼓炮座 + 正面弧形炮盾（顶缘锯齿、竖向棱条）
+ 两侧平坦舷板（带检修舱门）+ 微凸顶盖（后部潜望镜/测距罩）+ 一对紧挨的
6 英寸炮管。炮盾只用 ±22.5° 的面片拼弧——原版方块模型只接受 22.5° 倍数
的 rotation，写 10° 之类会让整个模型加载失败（表现为紫黑方块）。

坐标空间与同批火炮一致：16 格方块空间，正面朝 +Z，炮管伸到 z≈24；
display 里的 [0,180,0] 与缩放负责把它摆成手持姿态。
"""
import json
import math
import os

from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")
TEX_DIR = os.path.join(ASSETS, "textures/item")
NS = "piranport:item/"
SIZE = 32

GID = "us_mk16dp_twin_6inch_gun"
CX, CZ = 8.0, 7.6          # 炮塔中心（z 向前为正）
CROWN_R = 4.30             # 炮盾外弧半径

# 原画取样：主色 (96,113,120)，暗部 (38,52,59)，受光面偏亮
PALETTE = {
    "hull": (96, 113, 120),
    "roof": (142, 160, 168),
    "hull_dark": (52, 64, 72),
    "detail": (120, 138, 146),
    "barrel": (95, 112, 120),
    "interior": (38, 50, 57),
}

# 与 british_twin_6inch_gun / japanese_twin_152mm_gun 等双联 6 英寸炮一致
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                              "scale": [0.6175, 0.6175, 0.6175]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, -1],
                             "scale": [0.6175, 0.6175, 0.6175]},
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                              "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0, 0],
                             "scale": [0.6, 0.6, 0.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

elements = []


# ---------------------------------------------------------------- 材质绘制

def _noise(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 127.5 - 1.0


def _clamp(v):
    return max(0, min(255, int(round(v))))


def panel_texture(base, seed, grid=16, grid_dark=4, amp=2.2, top_light=4, bottom_dark=5):
    img = Image.new("RGBA", (SIZE, SIZE))
    r, g, b = base
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, seed) * amp
            if (x % grid == 0) or (y % grid == 0):
                d -= grid_dark
            if y < 2:
                d += top_light
            if y >= SIZE - 2:
                d -= bottom_dark
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def crown_texture(base=(104, 122, 130)):
    """炮盾：原画那圈带棱条的弧形护盾，竖纹（uv 的 u 方向）间隔亮暗。"""
    r, g, b = base
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            d = _noise(x, y, 71) * 2.0
            stripe = x % 4
            if stripe == 0:
                d -= 14                      # 棱沟
            elif stripe == 1:
                d += 8                       # 受光棱
            if y < 2:
                d += 6
            img.putpixel((x, y), (_clamp(r + d), _clamp(g + d), _clamp(b + d), 255))
    return img


def barrel_texture(base):
    r, g, b = base
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        shade = -20 + y * 0.6
        for x in range(SIZE):
            d = _noise(x, y, 51) * 3.0
            if x % 7 == 0:
                d -= 5
            if 12 <= x <= 19:
                d += 6
            img.putpixel((x, y), (_clamp(r + shade + d), _clamp(g + shade + d),
                                  _clamp(b + shade + 2 + d), 255))
    return img


def solid_rim(inner, rim):
    img = Image.new("RGBA", (SIZE, SIZE), inner + (255,))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 2 or y < 2 or x >= SIZE - 2 or y >= SIZE - 2:
                img.putpixel((x, y), rim + (255,))
    return img


def loaded_muzzle():
    img = Image.new("RGBA", (SIZE, SIZE), (196, 158, 72, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            if x < 3 or y < 3 or x >= SIZE - 3 or y >= SIZE - 3:
                img.putpixel((x, y), (120, 94, 42, 255))
            elif 10 <= x < 22 and 10 <= y < 16:
                img.putpixel((x, y), (232, 201, 110, 255))
    return img


def write_textures():
    save = lambda img, name: img.save(os.path.join(TEX_DIR, GID + "_" + name + ".png"))
    save(panel_texture(PALETTE["hull"], 11), "hull")
    save(panel_texture(PALETTE["roof"], 29, grid=10, amp=2.0), "roof")
    save(panel_texture(PALETTE["hull_dark"], 23), "hull_dark")
    save(panel_texture(PALETTE["detail"], 37), "detail")
    save(panel_texture(PALETTE["interior"], 43, grid=8, amp=3.0), "interior")
    save(barrel_texture(PALETTE["barrel"]), "barrel")
    save(crown_texture(), "crown")
    save(solid_rim((18, 18, 20), (56, 58, 60)), "muzzle_empty")
    save(loaded_muzzle(), "muzzle_loaded")


# ---------------------------------------------------------------- 模型拼装

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
    # from/to 一律存成 min/max：斜切面片的 corner 参数可能让 x1>x2，
    # 原版按 from→to 顺序生成顶点，倒序会让法线翻转、面被背面剔除。
    fr = [round(min(x1, x2), 3), round(min(y1, y2), 3), round(min(z1, z2), 3)]
    to = [round(max(x1, x2), 3), round(max(y1, y2), 3), round(max(z1, z2), 3)]
    el = {"name": name, "from": fr, "to": to, "faces": fj}
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


def octagon(prefix, y1, y2, hw, hd, c, tex):
    box(f"{prefix}_mid", CX - hw, y1, CZ - hd + c, CX + hw, y2, CZ + hd - c, tex)
    box(f"{prefix}_front", CX - hw + c, y1, CZ + hd - c, CX + hw - c, y2, CZ + hd, tex)
    box(f"{prefix}_back", CX - hw + c, y1, CZ - hd, CX + hw - c, y2, CZ - hd + c, tex)
    box(f"{prefix}_left", CX - hw, y1, CZ - hd + c, CX - hw + c, y2, CZ + hd - c, tex)
    box(f"{prefix}_right", CX + hw - c, y1, CZ - hd + c, CX + hw, y2, CZ + hd - c, tex)
    for tag, sx, sz in (("fr", 1, 1), ("fl", -1, 1), ("br", 1, -1), ("bl", -1, -1)):
        mx = CX + sx * (hw - c / 2)
        mz = CZ + sz * (hd - c / 2)
        thickness = 0.55
        shift = (thickness / 2) / (2 ** 0.5)
        angle = -45 if sx * sz > 0 else 45
        box(f"{prefix}_chamfer_{tag}", mx - sx * shift, y1, mz - sz * shift,
            mx + sx * shift, y2, mz + sz * shift, tex,
            rotation={"angle": angle, "axis": "y", "origin": [mx, (y1 + y2) / 2, mz]})


def barrels(items, sleeve_extra=0.28):
    """套筒/台阶/主管/加强环/炮口；炮口端面单独给 muzzle_empty 贴图。"""
    for i, (bx, by, z0, z1, half) in enumerate(items):
        tag = str(i)
        sleeve_end = z0 + 1.30
        step_end = sleeve_end + 0.75
        bell = z1 - 0.70
        diamond(f"barrel_sleeve_{tag}", bx, by, z0, sleeve_end, half + sleeve_extra, "hull_dark")
        diamond(f"barrel_step_{tag}", bx, by, sleeve_end, step_end, half + 0.08, "barrel")
        diamond(f"barrel_{tag}", bx, by, step_end, bell, half * 0.92, "barrel")
        mid = step_end + (bell - step_end) * 0.45
        diamond(f"barrel_ring_{tag}", bx, by, mid, mid + 0.35, half * 0.92 + 0.05, "barrel")
        diamond(f"muzzle_bell_{tag}", bx, by, bell, z1, half + 0.08, "barrel")
        diamond(f"muzzle_{tag}", bx, by, z1, z1 + 0.10, half - 0.02, "barrel",
                faces={"south": "muzzle_empty"}, full_uv_faces=("south",))


def crown_segment(angle, y1, y2, width=1.90, thickness=0.45, tex="crown", tag=""):
    """沿半径 CROWN_R 的弧形炮盾面片。

    角度必须落在 -45..45 且为 22.5 的倍数——原版方块模型只接受这几个值，
    写 ±67.5 或 10 会让整个模型加载失败；更靠后的侧面由平贴板补。
    """
    assert angle in (-45.0, -22.5, 0.0, 22.5, 45.0)
    rad = math.radians(angle)
    mx = round(CX + CROWN_R * math.sin(rad), 3)
    mz = round(CZ + CROWN_R * math.cos(rad), 3)
    box(f"crown{tag}_{int(angle * 10)}", mx - width / 2, y1, mz - thickness / 2,
        mx + width / 2, y2, mz + thickness / 2, tex,
        rotation={"angle": angle, "axis": "y", "origin": [mx, (y1 + y2) / 2, mz]})


def build():
    gid = GID

    # 圆鼓炮座（原画里的深色座圈，略比炮房宽）
    round_footprint("base_flange", CX, CZ, 4.15, 0.0, 0.35, "hull_dark")
    round_footprint("base_drum", CX, CZ, 3.90, 0.35, 0.95, "hull_dark")
    round_footprint("base_lip", CX, CZ, 4.02, 0.95, 1.08, "detail")

    # 炮房主体：上下两段八棱鼓包，外侧再贴平舷板（原画侧面是整块平板）
    octagon("body_lower", 1.05, 3.20, 3.55, 3.20, 1.10, "hull")
    octagon("body_upper", 3.20, 4.60, 3.40, 3.05, 1.00, "hull")
    for tag, x1, x2 in (("r", 11.60, 11.90), ("l", 4.10, 4.40)):
        box(f"side_slab_{tag}", x1, 1.35, 3.95, x2, 4.55, 9.45, "hull")
        box(f"side_bevel_{tag}", x1 + (0.15 if tag == "r" else 0.0), 1.05, 4.15,
            x2 - (0.0 if tag == "r" else 0.15), 1.35, 9.25, "hull_dark")
        box(f"side_cap_{tag}", x1 - (0.05 if tag == "r" else 0.0), 4.55, 4.05,
            x2 + (0.0 if tag == "r" else 0.05), 4.70, 9.35, "detail")

    # 两舷检修舱门（原画右侧后部的大舱门 + 左侧小舱门）
    box("hatch_r_frame", 11.88, 2.00, 6.35, 11.96, 3.55, 8.15, "hull_dark")
    box("hatch_r_panel", 11.96, 2.12, 6.50, 12.04, 3.43, 8.00, "detail")
    box("hatch_r_window", 12.04, 2.55, 6.95, 12.10, 3.00, 7.55, "roof")
    box("hatch_l_frame", 4.05, 2.10, 5.60, 4.13, 3.40, 7.40, "hull_dark")
    box("hatch_l_panel", 3.97, 2.22, 5.75, 4.05, 3.28, 7.25, "detail")

    # 正面炮盾：-45..45 五片面片拼弧（rotation 只能是 22.5 的倍数且不超过 45）。
    # 中央三片只做炮口以上的部分，下面就是炮管穿出的暗色开口（原画炮盾中央有缺口）。
    for ang in (-45.0, 45.0):
        crown_segment(ang, 1.45, 5.35)
        crown_segment(ang, 5.35, 5.65, width=1.10, thickness=0.40, tag="_tooth")
    for ang in (-22.5, 0.0, 22.5):
        crown_segment(ang, 3.85, 5.35)
        crown_segment(ang, 5.35, 5.65, width=1.10, thickness=0.40, tag="_tooth")

    # 炮盾侧面平贴板：把 ±45° 面片接到舷板，弧面只到 ±45° 剩下的由它补
    for tag, x1, x2 in (("r", 11.85, 12.15), ("l", 3.85, 4.15)):
        box(f"crown_side_{tag}", x1, 1.45, 7.60, x2, 5.35, 10.35, "crown")
        box(f"crown_side_rim_{tag}", x1, 5.35, 7.70, x2, 5.57, 10.25, "detail")

    # 炮盾中央的缺口：下缘先垫一段船体装甲，再是炮管穿出的暗色开口
    box("port_sill", 5.80, 1.05, 11.30, 10.20, 2.00, 11.55, "hull")
    box("gun_port", 5.80, 2.00, 11.10, 10.20, 3.85, 11.50, "interior")
    box("gun_port_lip", 5.65, 3.85, 11.15, 10.35, 4.00, 11.55, "detail")

    # 顶盖：比炮盾顶缘低，向后略微收窄（原画顶盖是整块浅色板）
    box("roof", 4.60, 4.60, 4.35, 11.40, 4.86, 10.75, "roof")
    box("roof_rear_step", 4.85, 4.30, 4.10, 11.15, 4.60, 4.55, "roof")
    box("roof_front_lip", 4.70, 4.55, 10.67, 11.30, 4.90, 10.92, "detail")
    box("roof_under_shield", 5.90, 4.60, 10.75, 10.10, 4.92, 11.55, "detail")

    # 顶部附件：后部潜望镜、左侧测距罩、右前小通风口
    box("periscope_base", 7.70, 4.86, 6.95, 8.30, 5.02, 7.60, "detail")
    box("periscope_stalk", 7.90, 5.02, 7.18, 8.10, 5.70, 7.38, "hull_dark")
    box("periscope_head", 7.78, 5.70, 7.05, 8.22, 5.94, 7.55, "hull")
    box("rangefinder", 5.35, 4.86, 5.10, 6.55, 5.30, 6.95, "hull")
    box("rangefinder_top", 5.45, 5.30, 5.20, 6.45, 5.42, 6.85, "roof")
    box("vent", 9.95, 4.86, 7.55, 10.55, 5.06, 8.40, "detail")

    # 后部舱门
    box("rear_frame_l", 5.45, 1.40, 4.34, 5.70, 4.10, 4.48, "detail")
    box("rear_frame_r", 10.30, 1.40, 4.34, 10.55, 4.10, 4.48, "detail")
    box("rear_interior", 5.70, 1.40, 4.38, 10.30, 4.10, 4.46, "interior")

    # 一对紧挨的 6 英寸炮管
    barrels([(7.30, 3.05, 10.55, 24.20, 0.22),
             (8.70, 3.05, 10.55, 24.20, 0.22)])

    tex = {k: NS + gid + "_" + k for k in
           ("hull", "roof", "hull_dark", "detail", "barrel", "interior",
            "crown", "muzzle_empty", "muzzle_loaded")}
    base_model = {
        "parent": "minecraft:block/block", "gui_light": "front", "ambientocclusion": False,
        "textures": {"particle": tex["hull"], **tex}, "elements": elements,
    }
    item_entry = {"parent": "builtin/entity", "gui_light": "front", "display": DISPLAY}
    loaded = {"parent": "piranport:item/" + gid + "_3d",
              "textures": {"muzzle_empty": tex["muzzle_loaded"]}}
    # 物品入口的 _loaded 变体，与同批火炮一样是图标模型（当前没有代码引用，纯惯例占位）
    item_loaded = {"parent": "minecraft:item/generated",
                   "textures": {"layer0": NS + gid}}
    for name, data in ((gid + "_3d.json", base_model), (gid + "_3d_loaded.json", loaded),
                       (gid + ".json", item_entry), (gid + "_loaded.json", item_loaded)):
        with open(os.path.join(ITEM_DIR, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("wrote", gid, len(elements), "elements")


if __name__ == "__main__":
    write_textures()
    build()
