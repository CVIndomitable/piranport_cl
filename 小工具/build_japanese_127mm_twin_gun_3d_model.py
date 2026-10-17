#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成日本12.7厘米连装炮的 3D 手持模型（items/elements 版）。

参考 us_mk16dp_twin_6inch_gun 的建模/显示先例：
  - 炮口朝 +Z，display 里 thirdperson/firstperson 用 yaw=90 转向前方
  - gui 用 [30, 225, 0] 等轴测
本模型不逐面写 uv，整张贴图铺满每个面，靠分盒材质和几何细节出效果。

物品走 Japanese127mmTwinGunRenderer（BEWLR）渲染：
  - GUI/地面/展示框等上下文画 32x32 图标平面
  - 第一/第三人称渲染这份 3D 模型
所以这里只定义手持 display，GUI 不写（保持 identity，图标才正）。

产出：
  models/item/japanese_127mm_twin_gun.json            builtin/entity 物品入口（只放 display）
  models/item/japanese_127mm_twin_gun_3d.json         空膛 3D 炮塔（standalone 烘焙）
  models/item/japanese_127mm_twin_gun_3d_loaded.json  装填 3D 炮塔（覆盖炮口材质）

WHY 用 builtin/entity：原版只在 BakedModel.isCustomRenderer() 为 true 时
才把渲染交给 BEWLR；普通模型即使注册了 IClientItemExtensions 也不会走。
"""
import json
import os

ASSETS = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src/main/resources/assets/piranport")
ITEM_DIR = os.path.join(ASSETS, "models/item")

NS = "piranport:item/"
TEX = {
    "hull": NS + "japanese_127mm_twin_gun_hull",
    "hull_dark": NS + "japanese_127mm_twin_gun_hull_dark",
    "barrel": NS + "japanese_127mm_twin_gun_barrel",
    "detail": NS + "japanese_127mm_twin_gun_detail",
    "muzzle_empty": NS + "japanese_127mm_twin_gun_muzzle_empty",
    "muzzle_loaded": NS + "japanese_127mm_twin_gun_muzzle_loaded",
}

elements = []


def box(name, x1, y1, z1, x2, y2, z2, tex, faces=None):
    """添加一个盒子；faces 可覆盖单面材质，如 {"south": "muzzle_empty"}。"""
    faces_json = {}
    for face in ("north", "south", "west", "east", "up", "down"):
        face_tex = (faces or {}).get(face, tex)
        faces_json[face] = {"texture": "#" + face_tex}
    elements.append({
        "name": name,
        "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
        "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
        "faces": faces_json,
    })


# ---------- 座圈 ----------
box("ring_base", 4.8, 0.0, 4.8, 11.2, 0.6, 11.2, "hull_dark")
box("ring_top", 4.3, 0.6, 4.3, 11.7, 1.0, 11.7, "hull_dark")

# ---------- 炮塔主体（三段阶梯近似原画的上窄下宽倒角） ----------
box("hull_low", 4.0, 1.0, 2.8, 12.0, 2.9, 12.4, "hull")
box("hull_mid", 4.2, 2.9, 3.1, 11.8, 4.3, 12.1, "hull")
box("hull_top", 4.6, 4.3, 3.5, 11.4, 4.8, 11.6, "hull")
# 顶盖：中央略抬高，四周留出一圈顶板轮廓
box("roof_plate", 5.0, 4.8, 3.9, 11.0, 5.0, 11.2, "hull")

# ---------- 前部炮盾（下厚上薄、向前凸出） ----------
box("shield_low", 5.3, 1.9, 11.9, 10.7, 3.9, 13.2, "hull")
box("shield_up", 5.5, 3.9, 11.8, 10.5, 4.9, 13.0, "detail")

# ---------- 后部配重 ----------
box("rear_counterweight", 5.2, 1.8, 2.0, 10.8, 4.0, 2.9, "hull_dark")
box("rear_deck", 5.8, 4.0, 2.1, 10.2, 4.5, 2.8, "detail")

# ---------- 两舷凸出的储物盒 ----------
box("port_box", 3.75, 1.5, 5.5, 4.0, 2.8, 7.8, "detail")
box("starboard_box", 12.0, 1.5, 5.5, 12.25, 2.8, 7.8, "detail")

# ---------- 顶部设备 ----------
box("rangefinder", 8.9, 4.8, 4.6, 10.5, 5.4, 6.0, "detail")
box("rangefinder_cap", 9.1, 5.4, 4.8, 10.3, 5.7, 5.8, "hull_dark")
box("hatch", 5.8, 4.8, 8.2, 7.2, 5.0, 9.6, "detail")
box("vent", 5.0, 4.8, 3.9, 5.8, 5.3, 4.8, "hull_dark")
box("periscope", 7.6, 4.8, 3.7, 8.1, 5.4, 4.2, "hull_dark")

# ---------- 双联装炮管（中心 x=6.4 / 9.6，y=3.7） ----------
for tag, cx in (("l", 6.4), ("r", 9.6)):
    box(f"sleeve_{tag}", cx - 0.85, 2.9, 11.2, cx + 0.85, 4.5, 13.6, "barrel")
    box(f"barrel_{tag}", cx - 0.6, 3.05, 13.0, cx + 0.6, 4.35, 18.6, "barrel")
    box(f"muzzle_ring_{tag}", cx - 0.75, 2.9, 18.6, cx + 0.75, 4.5, 19.4, "barrel")
    # 炮口内膛单独一个小面：空膛/装填的差异只在这里
    box(f"muzzle_{tag}", cx - 0.32, 3.4, 19.4, cx + 0.32, 4.0, 19.5,
        "barrel", faces={"south": "muzzle_empty"})

# 炮口朝 +Z。手持时绕 Y 转 180° 让炮口朝角色前方（-Z）。
# 单位：rotation 度、translation 1/16 像素、scale 倍率。
DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.5, 0.5, 0.5],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 2, -1],
        "scale": [0.5, 0.5, 0.5],
    },
    "firstperson_righthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.45, 0.45, 0.45],
    },
    "firstperson_lefthand": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, 0],
        "scale": [0.45, 0.45, 0.45],
    },
    # 掉落物/展示框/头戴走的是 2D 图标，这里沿用原版 item/generated 的变换
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
        "barrel": TEX["barrel"],
        "detail": TEX["detail"],
        "muzzle_empty": TEX["muzzle_empty"],
        "muzzle_loaded": TEX["muzzle_loaded"],
    },
    "elements": elements,
}

# 物品入口：builtin/entity 让原版把渲染交给 BEWLR；
# display 定义手持/掉落物等上下文，只有 2D 图标会用到（3D 也沿用同一 pose）。
item_entry = {
    "parent": "builtin/entity",
    "display": DISPLAY,
}

# 空膛/装填两份 standalone 3D 模型，BEWLR 按 LOADED_AMMO 组件选择
loaded_3d = {
    "parent": "piranport:item/japanese_127mm_twin_gun_3d",
    "textures": {
        "muzzle_empty": TEX["muzzle_loaded"],
    },
}


def dump(name, data):
    path = os.path.join(ITEM_DIR, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    dump("japanese_127mm_twin_gun_3d.json", base)
    dump("japanese_127mm_twin_gun_3d_loaded.json", loaded_3d)
    dump("japanese_127mm_twin_gun.json", item_entry)
