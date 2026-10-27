#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「61厘米三连装氧气鱼雷」的 3D 手持模型（按 Equip_L_149 原画从零建模）。

原画形体（只按这张原画，不套用其他发射器的零件布局）：
  - 与常规型同为低矮宽大的八角形装甲炮座，但这型甲板更大更平、压边更高，
    壁板上有一条斜向加强条；
  - 甲板栏杆立柱更密（16 根），除上横杆外还有一道中横杆；
  - 甲板右前方有一个很大的圆盖（原画最显眼的地标），旁边两道长条舱盖槽；
  - 三根鱼雷管更粗，+Z 一端是黑管口（空膛黑膛 / 装填金属封盖），
    -Z 一端是又粗又钝的抛光钢雷头（氧气雷的识别点），只一道管箍。

技术要点：
  - 八角形用「两个正交方盒 + 四个 45° 方盒」拼出切角八棱柱：正交方盒给 ±X/±Z
    的平面，45° 方盒补四个斜角面；各盒在 y 端内缩 0.006 避免共面 z-fighting；
  - 鱼雷管/圆盖用「轴对齐方盒 + 45° 绕 Z 方盒」叠成八棱柱；
  - 逐面写 uv，贴图 32x32、每模型单位 2px，长面 uv 夹到 16；
  - 装填/空膛的差异只在管口端面贴图（cap_empty / cap_loaded）；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/triple_61cm_oxygen_torpedo.json               builtin/entity 物品入口
  models/item/triple_61cm_oxygen_torpedo_3d.json            空膛 3D 发射器
  models/item/triple_61cm_oxygen_torpedo_3d_loaded.json     装填 3D 发射器
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "triple_61cm_oxygen_torpedo_"
TEX = {
    "hull": NS + PREFIX + "hull",
    "hull_dark": NS + PREFIX + "hull_dark",
    "plate": NS + PREFIX + "plate",
    "ring": NS + PREFIX + "ring",
    "tube": NS + PREFIX + "tube",
    "detail": NS + PREFIX + "detail",
    "warhead": NS + PREFIX + "warhead",
    "cap": NS + PREFIX + "cap_empty",
    "cap_loaded": NS + PREFIX + "cap_loaded",
}

elements = []

# ============ 主要尺寸（单位 = 2px，模型中心 x=8） ============
CX = 8.0
CZ = 8.60         # 炮座中心
R_WALL = 6.90     # 壁板内切圆半径（宽 13.8，比常规型略大）
R_BAND = 7.02     # 腰箍外凸
R_BEVEL = 6.15    # 斜肩
R_PLATE = 5.75    # 甲板（比常规型更大更平）
Y_PLINTH = 0.50
Y_WALL_TOP = 4.95
Y_BEVEL_TOP = 5.75
Y_PLATE_TOP = 6.12
FRONT_Z = CZ + R_WALL     # +Z 平面 z = 15.5
BACK_Z = CZ - R_WALL      # -Z 平面 z = 1.7

TUBE_Y = 4.25             # 管轴高度：管顶刚好藏在甲板压边之下
TUBE_R = 1.20             # 比常规型更粗
TUBE_XS = (5.55, 8.00, 10.45)
TUBE_FRONT = 16.05        # 管口桩起点
TUBE_CAP = 18.20          # 管口端面
TUBE_BACK = -2.20         # 管身尾端


def _uv_for(face, x1, y1, z1, x2, y2, z2, seed):
    """按面尺寸给 uv 窗口（uv 单位=模型单位；贴图 32px 时 16uv=整图）。

    面宽高与 uv 窗口一致，保证每模型单位恒定 2 像素，纹理不被拉伸。
    超过 16 单位的长侧面把窗口夹到 16（uv 不能越界），接受轻微拉伸。
    """
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
    """添加一个盒子（faces 覆盖单面材质，full_uv_faces 用整图 uv）。"""
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


def octagon_y(name, cx, cz, y1, y2, radius, tex, faces=None, inset=0.0):
    """切角八棱柱（平面朝 ±X/±Z，斜角面朝四个对角）。

    两个正交方盒给出 ±X、±Z 四块平面，四个 45° 方盒补斜角面。
    """
    k = 0.41421356                     # tan22.5°
    box(f"{name}_x", cx - radius, y1, cz - k * radius,
        cx + radius, y2, cz + k * radius, tex, faces=faces)
    box(f"{name}_z", cx - k * radius, y1 + inset, cz - radius,
        cx + k * radius, y2 - inset, cz + radius, tex, faces=faces)
    uc = 0.72 * radius                 # 斜角方盒在 u=(x±z)/√2 上的中心距离
    half_u = 0.28 * radius             # 沿 u 的半厚（内侧埋进正交方盒）
    half_v = k * radius                # 沿 v 的半宽，顶到相邻两个顶点
    for sx, sz, ang in ((1, 1, 45), (-1, 1, -45), (-1, -1, 45), (1, -1, -45)):
        px, pz = cx + sx * uc / math.sqrt(2.0), cz + sz * uc / math.sqrt(2.0)
        box(f"{name}_c{sx}{sz}", px - half_v, y1 + 2 * inset, pz - half_u,
            px + half_v, y2 - 2 * inset, pz + half_u, tex, faces=faces,
            rotation={"angle": ang, "axis": "y", "origin": [px, (y1 + y2) / 2, pz]})


def octagon_z(name, cx, cy, z1, z2, radius, tex, faces=None,
              full_uv_faces=()):
    """沿 Z 的八棱柱（鱼雷管/管口桩）：轴对齐方盒 + 45° 绕 Z 方盒。"""
    half = radius / math.sqrt(2.0)
    box(f"{name}_box", cx - half, cy - half, z1, cx + half, cy + half,
        z2, tex, faces=faces, full_uv_faces=full_uv_faces)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.003,
        cx + half, cy + half, z2 - 0.003, tex, faces=faces,
        full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def stud_z(name, cx, cy, z, radius=0.28, depth=0.14, tex="ring"):
    """贴在 +Z 壁板上的圆头螺栓。"""
    octagon_z(name, cx, cy, z, z + depth, radius, tex)


def stud_x(name, cy, cz, x, radius=0.28, depth=0.14, tex="ring"):
    """贴在 ±X 壁板上的圆头螺栓（沿 X 的扁八棱柱）。"""
    half = radius / math.sqrt(2.0)
    box(f"{name}_box", x, cy - half, cz - half, x + depth, cy + half, cz + half,
        tex)
    box(f"{name}_dia", x + 0.003, cy - half, cz - half, x + depth, cy + half,
        cz + half, tex,
        rotation={"angle": 45, "axis": "x",
                  "origin": [x + depth / 2, cy, cz]})


def stud_y(name, cx, cz, y, radius=0.24, height=0.14, tex="ring"):
    """甲板上的圆头螺栓（沿 Y 的扁八棱柱）。"""
    octagon_y(name, cx, cz, y, y + height, radius, tex, inset=0.004)


# ================= 炮座本体 =================
octagon_y("plinth", CX, CZ, 0.0, Y_PLINTH, R_BAND, "hull_dark", inset=0.006)
octagon_y("wall", CX, CZ, Y_PLINTH, Y_WALL_TOP, R_WALL, "hull", inset=0.006)
octagon_y("waist_band", CX, CZ, 2.75, 3.05, R_BAND, "hull_dark", inset=0.004)
octagon_y("bevel", CX, CZ, Y_WALL_TOP, Y_BEVEL_TOP, R_BEVEL, "hull", inset=0.006)
octagon_y("top_plate", CX, CZ, Y_BEVEL_TOP, Y_PLATE_TOP, R_PLATE, "plate",
          inset=0.006)

# ================= 三根氧气鱼雷管 =================
for tag, cx in zip("abc", TUBE_XS):
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_BACK, TUBE_FRONT, TUBE_R, "tube")
    # 管口一侧：宽管箍 + 管口桩（端面贴 cap）
    octagon_z(f"band_front_{tag}", cx, TUBE_Y, 15.20, 15.95, TUBE_R * 1.10, "ring")
    octagon_z(f"muzzle_{tag}", cx, TUBE_Y, TUBE_FRONT, TUBE_CAP, TUBE_R * 0.94,
              "ring", faces={"south": "cap"}, full_uv_faces=("south",))
    # 雷头一侧：一道粗管箍 + 又粗又钝的抛光钢雷头
    octagon_z(f"band_rear_{tag}", cx, TUBE_Y, TUBE_BACK - 0.70, TUBE_BACK,
              TUBE_R * 1.12, "ring")
    octagon_z(f"warhead_neck_{tag}", cx, TUBE_Y, TUBE_BACK - 1.30,
              TUBE_BACK - 0.70, TUBE_R * 1.06, "warhead")
    octagon_z(f"warhead_body_{tag}", cx, TUBE_Y, TUBE_BACK - 2.00,
              TUBE_BACK - 1.30, TUBE_R * 0.94, "warhead")
    octagon_z(f"warhead_mid_{tag}", cx, TUBE_Y, TUBE_BACK - 2.60,
              TUBE_BACK - 2.00, TUBE_R * 0.72, "warhead")
    octagon_z(f"warhead_tip_{tag}", cx, TUBE_Y, TUBE_BACK - 3.00,
              TUBE_BACK - 2.60, TUBE_R * 0.38, "warhead")

# ================= 甲板栏杆（立柱更密，另有中横杆） =================
RAIL_R = 5.45              # 内切圆半径（甲板 r=5.75 之内）
RAIL_TOP = Y_PLATE_TOP + 1.15
MID_TOP = Y_PLATE_TOP + 0.58
POST = 0.075
BAR = 0.055
rail_c = RAIL_R / 0.92388
verts = []
for i in range(8):
    a = math.radians(22.5 + 45 * i)
    verts.append((CX + rail_c * math.cos(a), CZ + rail_c * math.sin(a)))

posts = list(verts)
for i in range(8):
    ax, az = verts[i]
    bx, bz = verts[(i + 1) % 8]
    posts.append(((ax + bx) / 2, (az + bz) / 2))
for i, (vx, vz) in enumerate(posts):
    box(f"rail_post_{i}", vx - POST, Y_PLATE_TOP, vz - POST,
        vx + POST, RAIL_TOP, vz + POST, "detail")

flat_half = rail_c * math.sin(math.radians(22.5))


def rail_bar(i, y_top, tag):
    """一段横杆：按两个顶点连线的方向决定轴向 / 45° 旋转方向。"""
    ax, az = verts[i]
    bx, bz = verts[(i + 1) % 8]
    mx, mz = (ax + bx) / 2, (az + bz) / 2
    if abs(ax - bx) < 1e-6:
        box(f"{tag}_z{i}", mx - BAR, y_top - 2 * BAR, mz - flat_half - 0.02,
            mx + BAR, y_top, mz + flat_half + 0.02, "detail")
    elif abs(az - bz) < 1e-6:
        box(f"{tag}_x{i}", mx - flat_half - 0.02, y_top - 2 * BAR, mz - BAR,
            mx + flat_half + 0.02, y_top, mz + BAR, "detail")
    else:
        # 旋转 +45° 时局部 x 轴指向 (1,-1)/√2
        ang = 45 if (bx - ax) * (bz - az) < 0 else -45
        box(f"{tag}_d{i}", mx - flat_half - 0.02, y_top - 2 * BAR, mz - BAR,
            mx + flat_half + 0.02, y_top, mz + BAR, "detail",
            rotation={"angle": ang, "axis": "y", "origin": [mx, y_top - BAR, mz]})


for i in range(8):
    rail_bar(i, RAIL_TOP, "rail_top")
    rail_bar(i, MID_TOP, "rail_mid")

# 三道斜拉杆（原画栏杆上的斜撑）
box("rail_brace_back", CX - 1.5, Y_PLATE_TOP + 0.55, CZ - RAIL_R - 0.04,
    CX + 1.5, Y_PLATE_TOP + 0.63, CZ - RAIL_R + 0.04, "detail",
    rotation={"angle": 22.5, "axis": "z", "origin": [CX, Y_PLATE_TOP + 0.59, CZ - RAIL_R]})
box("rail_brace_left", CX - RAIL_R - 0.04, Y_PLATE_TOP + 0.55, CZ - 1.5,
    CX - RAIL_R + 0.04, Y_PLATE_TOP + 0.63, CZ + 1.5, "detail",
    rotation={"angle": 22.5, "axis": "x", "origin": [CX - RAIL_R, Y_PLATE_TOP + 0.59, CZ]})
box("rail_brace_right", CX + RAIL_R - 0.04, Y_PLATE_TOP + 0.55, CZ - 1.5,
    CX + RAIL_R + 0.04, Y_PLATE_TOP + 0.63, CZ + 1.5, "detail",
    rotation={"angle": -22.5, "axis": "x", "origin": [CX + RAIL_R, Y_PLATE_TOP + 0.59, CZ]})

# ================= 甲板细节 =================
def hatch(name, x1, z1, x2, z2, rot=None):
    """一道舱盖槽：外圈细框 + 内圈暗槽。"""
    box(f"{name}_rim", x1 - 0.14, Y_PLATE_TOP, z1 - 0.14,
        x2 + 0.14, Y_PLATE_TOP + 0.04, z2 + 0.14, "detail", rotation=rot)
    box(f"{name}_slot", x1, Y_PLATE_TOP + 0.02, z1,
        x2, Y_PLATE_TOP + 0.07, z2, "hull_dark", rotation=rot)


hatch("hatch_a", 5.55, 8.55, 8.55, 9.45)
hatch("hatch_b", 6.45, 10.55, 9.05, 11.25,
      rot={"angle": 22.5, "axis": "y", "origin": [7.75, Y_PLATE_TOP, 10.90]})

# 大圆盖（原画甲板右前方最醒目的地标）
octagon_y("deck_cap", 10.55, 11.60, Y_PLATE_TOP, Y_PLATE_TOP + 0.34, 1.35,
          "ring", inset=0.004)
octagon_y("deck_cap_top", 10.55, 11.60, Y_PLATE_TOP + 0.34, Y_PLATE_TOP + 0.42,
          0.98, "plate", inset=0.004)
octagon_y("deck_cap_knob", 10.55, 11.60, Y_PLATE_TOP + 0.42, Y_PLATE_TOP + 0.50,
          0.45, "detail", inset=0.004)

# 圆头螺栓 + 矮圆包
for i, (bx, bz) in enumerate(((5.90, 10.85), (9.20, 9.15), (10.70, 7.25),
                              (6.15, 6.95), (8.05, 12.35))):
    stud_y(f"deck_bolt_{i}", bx, bz, Y_PLATE_TOP)
octagon_y("deck_dome", 6.95, 11.75, Y_PLATE_TOP, Y_PLATE_TOP + 0.24, 0.42,
          "detail", inset=0.004)

# ================= 壁板细节：前壁舱门 + 斜加强条 + 圆头螺栓 =================
box("door_frame", 6.45, 0.85, FRONT_Z - 0.10, 9.75, 3.45, FRONT_Z + 0.02,
    "detail")
box("door_plate", 6.65, 1.03, FRONT_Z + 0.02, 9.55, 3.27, FRONT_Z + 0.16,
    "hull_dark")
box("door_bar", 7.40, 1.95, FRONT_Z + 0.16, 8.80, 2.45, FRONT_Z + 0.24, "detail")
for i, (bx, by) in enumerate(((5.95, 4.05), (10.05, 4.05), (5.95, 1.25),
                              (10.05, 1.25))):
    stud_z(f"front_bolt_{i}", bx, by, FRONT_Z - 0.02)

# 斜加强条（原画前壁的斜向棱线）
box("wall_strip", 5.90, 0.95, FRONT_Z - 0.02, 9.05, 1.35, FRONT_Z + 0.06,
    "detail",
    rotation={"angle": 22.5, "axis": "z", "origin": [7.45, 1.15, FRONT_Z]})

for i, (by, bz) in enumerate(((4.05, 7.50), (4.05, 9.70), (1.35, 8.60))):
    stud_x(f"east_bolt_{i}", by, bz, CX + R_WALL - 0.02)
    stud_x(f"west_bolt_{i}", by, bz, CX - R_WALL + 0.02 - 0.14)
for i, bx in enumerate((6.20, 9.80)):
    octagon_z(f"back_bolt_{i}", bx, 4.05, BACK_Z - 0.12, BACK_Z + 0.02, 0.28,
              "ring")

# 手持时绕 Y 转 180°，让管口朝角色前方
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.56, 0.56, 0.56],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.56, 0.56, 0.56],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.50, 0.50, 0.50],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.50, 0.50, 0.50],
    },
    # 掉落物/展示框/头戴走的是 2D 图标，沿用原版 item/generated 的变换
    "ground": {
        "rotation": [0, 0, 0],
        "translation": [0, 2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "fixed": {
        "rotation": [0, 180, 0],
        "scale": [1, 1, 1],
    },
    "head": {
        "rotation": [0, 180, 0],
        "translation": [0, 13, 7],
        "scale": [1, 1, 1],
    },
}

base = {
    "parent": "minecraft:block/block",
    "gui_light": "front",
    "ambientocclusion": False,
    "textures": {
        "particle": TEX["hull"],
        "hull": TEX["hull"],
        "hull_dark": TEX["hull_dark"],
        "plate": TEX["plate"],
        "ring": TEX["ring"],
        "tube": TEX["tube"],
        "detail": TEX["detail"],
        "warhead": TEX["warhead"],
        "cap": TEX["cap"],
        "cap_loaded": TEX["cap_loaded"],
    },
    "elements": elements,
}

# 物品入口：builtin/entity 让原版把渲染交给 BEWLR
item_entry = {
    "parent": "builtin/entity",
    "gui_light": "front",
    "display": DISPLAY,
}

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
loaded_3d = {
    "parent": "piranport:item/triple_61cm_oxygen_torpedo_3d",
    "textures": {
        "cap": TEX["cap_loaded"],
    },
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("triple_61cm_oxygen_torpedo_3d.json", base)
    dump("triple_61cm_oxygen_torpedo_3d_loaded.json", loaded_3d)
    dump("triple_61cm_oxygen_torpedo.json", item_entry)
