#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成「61厘米三连装鱼雷」的 3D 手持模型（按 Equip_L_90 原画从零建模）。

原画形体（只按这张原画，不套用其他发射器的零件布局）：
  - 一座低矮宽大的八角形装甲炮座：短壁板 + 一圈外凸腰箍 + 收分斜肩 + 平顶甲板，
    甲板比斜肩高出一小步，形成一圈压边；
  - 甲板四周一圈细栏杆（16 根立柱 + 8 段上横杆 + 两道斜拉杆），板面有两道长条
    舱盖槽、几颗圆头螺栓、一个矮圆包，靠管口一侧还有一个大圆盖；
  - 炮座正前方壁板上是一扇带框装甲舱门；
  - 三根鱼雷管横贯炮座上部，正好从壁板顶沿与甲板压边之间伸出：
    +Z 一端是黑管口（空膛黑膛 / 装填金属封盖），-Z 一端两道管箍 + 细长抛光钢雷头。
  - 比例取自原画：炮座宽度 ≈ 3 个管径，壁板高度 ≈ 1.7 个管径，整体低矮。

技术要点：
  - 八角形用「两个正交方盒 + 四个 45° 方盒」拼出切角八棱柱：正交方盒给 ±X/±Z
    的平面，45° 方盒补四个斜角面；斜角/正交方盒在 y 端各内缩 0.006，
    避免共面 z-fighting（0.006 单位 = 0.012px，肉眼不可见）；
  - 鱼雷管/圆盖用「轴对齐方盒 + 45° 绕 Z 方盒」叠成八棱柱；
  - 逐面写 uv，贴图 32x32、每模型单位 2px，长面 uv 夹到 16；
  - 装填/空膛的差异只在管口端面贴图（cap_empty / cap_loaded）；
  - 模型中心在 x=8，管口朝 +Z，物品 display 旋转 180° 后朝角色前方。

产出：
  models/item/triple_61cm_torpedo.json               builtin/entity 物品入口（只放 display）
  models/item/triple_61cm_torpedo_3d.json            空膛 3D 发射器
  models/item/triple_61cm_torpedo_3d_loaded.json     装填 3D 发射器
"""
import json
import math
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
PREFIX = "triple_61cm_torpedo_"
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
CZ = 8.4          # 炮座中心（略偏后，让管口一侧留出观察距离）
R_WALL = 6.6      # 炮座壁板内切圆半径（宽度 13.2 = 3 个管径）
R_BAND = 6.72     # 腰箍外凸
R_BEVEL = 5.90    # 斜肩
R_PLATE = 5.45    # 甲板
Y_PLINTH = 0.50
Y_WALL_TOP = 4.60
Y_BEVEL_TOP = 5.35
Y_PLATE_TOP = 5.70
FRONT_Z = CZ + R_WALL     # 正前方（+Z）平面 z = 15.0
BACK_Z = CZ - R_WALL      # 后方平面 z = 1.8

TUBE_Y = 3.95             # 管轴高度：管顶刚好藏在甲板压边之下
TUBE_R = 1.14
TUBE_XS = (5.70, 8.00, 10.30)
TUBE_FRONT = 15.75        # 管口桩起点
TUBE_CAP = 18.00          # 管口端面（黑膛/封盖）
TUBE_BACK = -2.00         # 管身尾端（雷头从这里往后接）


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
    # 确定性错位，避免所有面采同一块纹理
    ou = (seed * 3.7 + len(face) * 1.3) % (max_u + 1e-6)
    ov = (seed * 5.1 + len(face) * 2.9) % (max_v + 1e-6)
    return [round(ou, 3), round(ov, 3), round(ou + w, 3), round(ov + h, 3)]


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None, rotation=None,
        full_uv_faces=()):
    """添加一个盒子。

    faces: 覆盖单面材质，如 {"south": "cap"}。
    rotation: 原版元素旋转（单轴，角度仅 ±45/±22.5/0）。
    full_uv_faces: 这些面改用整图 uv（管口端面「整张就是一张圆盘」）。
    """
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
    inset：各盒在 y 端的内缩量，避免顶/底面共面 z-fighting。
    """
    k = 0.41421356                     # tan22.5°，切角八边形内缩比
    box(f"{name}_x", cx - radius, y1, cz - k * radius,
        cx + radius, y2, cz + k * radius, tex, faces=faces)
    box(f"{name}_z", cx - k * radius, y1 + inset, cz - radius,
        cx + k * radius, y2 - inset, cz + radius, tex, faces=faces)
    # 四个斜角：45° 方盒的外侧面正好落在 x±z = radius*sqrt(2) 上
    uc = 0.72 * radius                 # 斜角方盒在 u=(x±z)/√2 上的中心距离
    half_u = 0.28 * radius             # 沿 u 的半厚（内侧埋进正交方盒，端面不露）
    half_v = k * radius                # 沿 v 的半宽，正好顶到相邻两个顶点
    for sx, sz, ang in ((1, 1, 45), (-1, 1, -45), (-1, -1, 45), (1, -1, -45)):
        px, pz = cx + sx * uc / math.sqrt(2.0), cz + sz * uc / math.sqrt(2.0)
        box(f"{name}_c{sx}{sz}", px - half_v, y1 + 2 * inset, pz - half_u,
            px + half_v, y2 - 2 * inset, pz + half_u, tex, faces=faces,
            rotation={"angle": ang, "axis": "y", "origin": [px, (y1 + y2) / 2, pz]})


def octagon_z(name, cx, cy, z1, z2, radius, tex, faces=None,
              full_uv_faces=()):
    """沿 Z 的八棱柱（鱼雷管/管口桩）：轴对齐方盒 + 45° 绕 Z 方盒。"""
    half = radius / math.sqrt(2.0)   # 两个方盒半宽相同，并集才是正八边形
    box(f"{name}_box", cx - half, cy - half, z1, cx + half, cy + half,
        z2, tex, faces=faces, full_uv_faces=full_uv_faces)
    box(f"{name}_dia", cx - half, cy - half, z1 + 0.003,
        cx + half, cy + half, z2 - 0.003, tex, faces=faces,
        full_uv_faces=full_uv_faces,
        rotation={"angle": 45, "axis": "z", "origin": [cx, cy, (z1 + z2) / 2]})


def stud_z(name, cx, cy, z, radius=0.26, depth=0.14, tex="ring"):
    """贴在 +Z 壁板上的圆头螺栓。"""
    octagon_z(name, cx, cy, z, z + depth, radius, tex)


def stud_x(name, cy, cz, x, radius=0.26, depth=0.14, tex="ring"):
    """贴在 ±X 壁板上的圆头螺栓（沿 X 的扁八棱柱）。"""
    half = radius / math.sqrt(2.0)
    box(f"{name}_box", x, cy - half, cz - half, x + depth, cy + half, cz + half,
        tex)
    box(f"{name}_dia", x + 0.003, cy - half, cz - half, x + depth, cy + half,
        cz + half, tex,
        rotation={"angle": 45, "axis": "x",
                  "origin": [x + depth / 2, cy, cz]})


def stud_y(name, cx, cz, y, radius=0.22, height=0.14, tex="ring"):
    """甲板上的圆头螺栓（沿 Y 的扁八棱柱）。"""
    octagon_y(name, cx, cz, y, y + height, radius, tex, inset=0.004)


# ================= 炮座本体：下裙 / 壁板 / 腰箍 / 斜肩 / 甲板 =================
octagon_y("plinth", CX, CZ, 0.0, Y_PLINTH, R_BAND, "hull_dark", inset=0.006)
octagon_y("wall", CX, CZ, Y_PLINTH, Y_WALL_TOP, R_WALL, "hull", inset=0.006)
octagon_y("waist_band", CX, CZ, 2.55, 2.85, R_BAND, "hull_dark", inset=0.004)
octagon_y("bevel", CX, CZ, Y_WALL_TOP, Y_BEVEL_TOP, R_BEVEL, "hull", inset=0.006)
octagon_y("top_plate", CX, CZ, Y_BEVEL_TOP, Y_PLATE_TOP, R_PLATE, "plate",
          inset=0.006)

# ================= 三根鱼雷管 =================
for tag, cx in zip("abc", TUBE_XS):
    # 管身：从炮座后方一直贯到正前方
    octagon_z(f"tube_{tag}", cx, TUBE_Y, TUBE_BACK, TUBE_FRONT, TUBE_R, "tube")
    # 管口一侧：管箍 + 管口桩（端面贴 cap，空膛/装填只差这一张图）
    octagon_z(f"band_front_{tag}", cx, TUBE_Y, 15.10, 15.75, TUBE_R * 1.09, "ring")
    octagon_z(f"muzzle_{tag}", cx, TUBE_Y, TUBE_FRONT, TUBE_CAP, TUBE_R * 0.95,
              "ring", faces={"south": "cap"}, full_uv_faces=("south",))
    # 雷头一侧：两道管箍 + 逐段收细的抛光钢雷头（原画最左端的细长雷头）
    octagon_z(f"band_rear_{tag}", cx, TUBE_Y, TUBE_BACK - 0.60, TUBE_BACK,
              TUBE_R * 1.09, "ring")
    octagon_z(f"band_rear2_{tag}", cx, TUBE_Y, TUBE_BACK - 1.15, TUBE_BACK - 0.60,
              TUBE_R * 1.03, "ring")
    octagon_z(f"warhead_neck_{tag}", cx, TUBE_Y, TUBE_BACK - 1.50,
              TUBE_BACK - 1.15, TUBE_R * 0.82, "warhead")
    octagon_z(f"warhead_body_{tag}", cx, TUBE_Y, TUBE_BACK - 2.00,
              TUBE_BACK - 1.50, TUBE_R * 0.77, "warhead")
    octagon_z(f"warhead_tip1_{tag}", cx, TUBE_Y, TUBE_BACK - 2.45,
              TUBE_BACK - 2.00, TUBE_R * 0.58, "warhead")
    octagon_z(f"warhead_tip2_{tag}", cx, TUBE_Y, TUBE_BACK - 2.75,
              TUBE_BACK - 2.45, TUBE_R * 0.30, "warhead")

# ================= 甲板栏杆 =================
RAIL_R = 5.15              # 栏杆所在的内切圆半径（甲板 r=5.45 之内）
RAIL_H = 1.05              # 栏杆高度
RAIL_TOP = Y_PLATE_TOP + RAIL_H
POST = 0.075               # 立柱半宽
BAR = 0.055                # 横杆半厚
rail_c = RAIL_R / 0.92388  # 顶点所在圆半径
verts = []
for i in range(8):
    a = math.radians(22.5 + 45 * i)
    verts.append((CX + rail_c * math.cos(a), CZ + rail_c * math.sin(a)))

# 8 个顶点立柱 + 8 个平面中点立柱（原画栏杆立柱很密）
posts = list(verts)
for i in range(8):
    ax, az = verts[i]
    bx, bz = verts[(i + 1) % 8]
    posts.append(((ax + bx) / 2, (az + bz) / 2))
for i, (vx, vz) in enumerate(posts):
    box(f"rail_post_{i}", vx - POST, Y_PLATE_TOP, vz - POST,
        vx + POST, RAIL_TOP, vz + POST, "detail")

# 8 段上横杆：4 段沿 ±X/±Z 的平面，4 段沿四个斜平面
flat_half = rail_c * math.sin(math.radians(22.5))   # 平面半长 = 2.135
for i in range(8):
    ax, az = verts[i]
    bx, bz = verts[(i + 1) % 8]
    mx, mz = (ax + bx) / 2, (az + bz) / 2
    if abs(ax - bx) < 1e-6:      # 沿 Z 方向的平面
        box(f"rail_bar_z{i}", mx - BAR, RAIL_TOP - 2 * BAR, mz - flat_half - 0.02,
            mx + BAR, RAIL_TOP, mz + flat_half + 0.02, "detail")
    elif abs(az - bz) < 1e-6:    # 沿 X 方向的平面
        box(f"rail_bar_x{i}", mx - flat_half - 0.02, RAIL_TOP - 2 * BAR, mz - BAR,
            mx + flat_half + 0.02, RAIL_TOP, mz + BAR, "detail")
    else:                        # 斜平面：绕 Y 转 ±45°，长轴沿平面方向
        # 旋转 +45° 时局部 x 轴指向 (1,-1)/√2，所以平面方向 (1,-1) 用 +45
        ang = 45 if (bx - ax) * (bz - az) < 0 else -45
        box(f"rail_bar_d{i}", mx - flat_half - 0.02, RAIL_TOP - 2 * BAR, mz - BAR,
            mx + flat_half + 0.02, RAIL_TOP, mz + BAR, "detail",
            rotation={"angle": ang, "axis": "y",
                      "origin": [mx, RAIL_TOP - BAR, mz]})

# 两道斜拉杆（原画后侧栏杆上的斜撑）
box("rail_brace_back", CX - 1.5, Y_PLATE_TOP + 0.5, CZ - RAIL_R - 0.04,
    CX + 1.5, Y_PLATE_TOP + 0.58, CZ - RAIL_R + 0.04, "detail",
    rotation={"angle": 22.5, "axis": "z", "origin": [CX, Y_PLATE_TOP + 0.54, CZ - RAIL_R]})
box("rail_brace_left", CX - RAIL_R - 0.04, Y_PLATE_TOP + 0.5, CZ - 1.5,
    CX - RAIL_R + 0.04, Y_PLATE_TOP + 0.58, CZ + 1.5, "detail",
    rotation={"angle": 22.5, "axis": "x", "origin": [CX - RAIL_R, Y_PLATE_TOP + 0.54, CZ]})


# ================= 甲板细节：舱盖槽 / 大圆盖 / 圆头螺栓 =================
def hatch(name, x1, z1, x2, z2, rot=None):
    """一道舱盖槽：外圈细框 + 内圈暗槽。"""
    box(f"{name}_rim", x1 - 0.14, Y_PLATE_TOP, z1 - 0.14,
        x2 + 0.14, Y_PLATE_TOP + 0.04, z2 + 0.14, "detail", rotation=rot)
    box(f"{name}_slot", x1, Y_PLATE_TOP + 0.02, z1,
        x2, Y_PLATE_TOP + 0.07, z2, "hull_dark", rotation=rot)


hatch("hatch_a", 5.35, 8.30, 8.25, 9.20)
hatch("hatch_b", 6.30, 10.30, 8.80, 11.00,
      rot={"angle": 22.5, "axis": "y", "origin": [7.55, Y_PLATE_TOP, 10.65]})

# 大圆盖（原画甲板上靠管口一侧的圆盖）
octagon_y("deck_cap", 10.30, 11.30, Y_PLATE_TOP, Y_PLATE_TOP + 0.28, 1.10,
          "ring", inset=0.004)
octagon_y("deck_cap_top", 10.30, 11.30, Y_PLATE_TOP + 0.28, Y_PLATE_TOP + 0.35,
          0.80, "plate", inset=0.004)

# 圆头螺栓 + 一个矮圆包（原画甲板上的圆钉）
for i, (bx, bz) in enumerate(((5.85, 10.55), (9.05, 8.95), (10.40, 7.15),
                              (6.10, 6.95), (8.00, 12.10))):
    stud_y(f"deck_bolt_{i}", bx, bz, Y_PLATE_TOP)
octagon_y("deck_dome", 6.90, 11.55, Y_PLATE_TOP, Y_PLATE_TOP + 0.24, 0.40,
          "detail", inset=0.004)

# ================= 正前方壁板：装甲舱门 + 螺栓 =================
box("door_frame", 6.35, 0.80, FRONT_Z - 0.10, 9.65, 3.30, FRONT_Z + 0.02,
    "detail")
box("door_plate", 6.55, 0.98, FRONT_Z + 0.02, 9.45, 3.12, FRONT_Z + 0.16,
    "hull_dark")
box("door_bar", 7.30, 1.85, FRONT_Z + 0.16, 8.70, 2.35, FRONT_Z + 0.24, "detail")
for i, (bx, by) in enumerate(((5.80, 3.85), (10.20, 3.85), (5.80, 1.20),
                              (10.20, 1.20))):
    stud_z(f"front_bolt_{i}", bx, by, FRONT_Z - 0.02)

# ================= 侧壁 / 后壁的圆头螺栓（原画每块壁板上都有圆钉） =================
for i, (by, bz) in enumerate(((3.70, 7.40), (3.70, 9.40), (1.35, 8.40))):
    stud_x(f"east_bolt_{i}", by, bz, CX + R_WALL - 0.02)
    stud_x(f"west_bolt_{i}", by, bz, CX - R_WALL + 0.02 - 0.14)
for i, bx in enumerate((6.10, 9.90)):
    # 后壁（-Z）用沿 Z 的螺栓，朝 -Z，所以贴在里面一侧
    octagon_z(f"back_bolt_{i}", bx, 3.70, BACK_Z - 0.12, BACK_Z + 0.02, 0.26,
              "ring")

# ================= 甲板上的一道 45° 斜压条（原画甲板的对角棱线） =================
box("deck_strip", 6.10, Y_PLATE_TOP, 10.05, 10.90, Y_PLATE_TOP + 0.07, 10.30,
    "detail",
    rotation={"angle": -45, "axis": "y", "origin": [8.50, Y_PLATE_TOP, 10.175]})

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
    "parent": "piranport:item/triple_61cm_torpedo_3d",
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
    dump("triple_61cm_torpedo_3d.json", base)
    dump("triple_61cm_torpedo_3d_loaded.json", loaded_3d)
    dump("triple_61cm_torpedo.json", item_entry)
