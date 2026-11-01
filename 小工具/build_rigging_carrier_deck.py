#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""航母飞行甲板舰装模型（GeckoLib Bedrock）。

只读复用 build_rigging_hull_models 的 BedrockModel / 自动 UV 打包 / 调色板，
不修改共享脚本；木色与新材质在本脚本里增量登记。

坐标（与通用舰装架同一套，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot（applyBase 之后、XP(180) 之前）；
    +Y 朝上；-Z 朝前；+X 左舷、-X 右舷。

程序化占位在玩家空间（+Y 朝下、+Z 朝前）给出，换算到模型空间：
    model_x = player_x
    model_y = -player_y
    model_z = -player_z
甲板面 player y 0.8..1.2 → model y -1.2..-0.8，取顶面 -0.8、厚 1 单位；
DECK_1/2 挂点 player y 0.4 → model y -0.4，正好落在甲面上方 0.4 单位。

甲板整体挂在右臂外侧（双臂外移后右臂外缘约 x=-11.5）：
    内缘 x=-11.5（贴右臂外缘）、外缘 x=-18.5、中心 x=-15.0，宽 7、长 11。
    甲板向前伸：z=-9.0..+2.0（中心 z=-3.5），后缘只到身体中心附近；
    顶面 y=-0.8 平放。后段短支架贴右臂外缘，前伸部分用内侧纵向龙骨
    + 阶梯吊臂托住，不向躯干拉长梁。

贴图 64×64、1:1（1 texel = 1/16 格）：所有面的 uv_size 等于面尺寸，
跑道中线与两条弹射轨道直接在甲板顶面 UV 区域内逐像素画，不额外堆几何。

用法：
    python3 build_rigging_carrier_deck.py           # 写 geo/png/animation
    python3 build_rigging_carrier_deck.py --dry-run # 只打印统计与坐标核对
"""
import argparse
import json
import os

from PIL import Image

import build_rigging_hull_models as builder

# ---------------------------------------------------------------- 调色板扩展
# 只在运行时扩充共享构建器的字典，不改共享文件。
# 木色取 29 号原画的浅暖木地板；标线用米白与低饱和黄，避免纯白/纯黄刺眼。
builder.PALETTE.update({
    "wood": (218, 158, 112, 255),
    "wood_light": (238, 182, 138, 255),
    "wood_dark": (180, 120, 84, 255),
    "wood_seam": (126, 82, 54, 255),
    "marking_white": (236, 233, 221, 255),
    "marking_yellow": (228, 190, 86, 255),
})

# 材质 -> (填充色, 描边色, 内缝色)；通用绘制器按这个规格画面板缝。
builder.MATERIALS.update({
    # 顶面会被 paint_deck_top 覆盖，这里的规格只是让打包器接受该材质
    "deck_top": ("wood", "wood_seam", "wood_dark"),
    "deck_wood": ("wood", "wood_seam", "wood_dark"),
    "deck_edge": ("wood_dark", "wood_seam", "wood_seam"),
    "deck_under": ("dark", "shadow", "dark"),
    "metal_gray": ("mid", "dark", "base"),
})

# ---------------------------------------------------------------- 尺寸常量
# 甲板面（模型空间）。挂在右臂外侧，宽 7、长 11，整体向前伸。
DECK_X0, DECK_X1 = -18.5, -11.5    # 内缘贴右臂外缘 x=-11.5，外缘 -18.5
# z=-9.0..+2.0：大部分在身体前方，后缘只到身体中心附近（中心 z=-3.5）。
DECK_Z0, DECK_Z1 = -9.0, 2.0
DECK_LENGTH = int(DECK_Z1 - DECK_Z0)
DECK_TOP_Y = -0.8                  # 甲板顶面（player 0.8），平放
DECK_THICKNESS = 1

# DECK_1/2 挂点换算到模型空间（x 不变，y/z 取反）
# DECK_1 靠后、DECK_2 靠前；两机足迹 z=-8.72..1.72，前后各留 0.28。
DECK_MOUNTS = {
    "DECK_1": (-15.0, -0.4, -1.0),
    "DECK_2": (-15.0, -0.4, -6.0),
}
MOUNT_CLEARANCE = 0.4              # 飞机挂点应在甲面上方约 0.4 单位


# ---------------------------------------------------------------- 甲板顶面绘制
def paint_deck_top(px, x0, y0, w, h):
    """逐像素画木甲板 + 白色中线 + 两条黄色弹射轨道。

    Bedrock up 面的 UV 方向（见 rigging_geo_preview.FACE_CORNERS）：
        u 轴 = 模型 x 增大（-9 → -2）
        v 轴 = 模型 -z（甲板后端 +z → 前端 -z）
    所以跑道方向是纹理的竖向：中线与轨道都画成竖线。

    只在 7 宽的顶面上生效；其他宽度退回纯木板，避免以后改尺寸时画花。
    """
    P = builder.PALETTE
    # 纵向木板：逐列换色，两舷边缘压深，作为甲板收边
    tones = ["wood_dark", "wood", "wood_light", "wood",
             "wood_light", "wood", "wood_dark"]
    for x in range(w):
        tone = tones[x] if w == 7 else "wood"
        for y in range(h):
            px[x0 + x, y0 + y] = P[tone]

    # 木板横向接头：每 3 行一道，错开边界
    for y in range(1, h - 1, 3):
        for x in range(1, w - 1):
            px[x0 + x, y0 + y] = P["wood_seam"]

    # 四周 1px 深木收边，让甲板轮廓在等轴测里不糊
    for x in range(w):
        px[x0 + x, y0] = P["wood_seam"]
        px[x0 + x, y0 + h - 1] = P["wood_seam"]
    for y in range(h):
        px[x0, y0 + y] = P["wood_seam"]
        px[x0 + w - 1, y0 + y] = P["wood_seam"]

    if w != 7 or h < 5:
        return

    # 中线：u=3（模型 x=-5.5），白色实线
    for y in range(1, h - 1):
        px[x0 + 3, y0 + y] = P["marking_white"]

    # 两条弹射轨道：u=1 / u=5，黄色虚线（2px 实、1px 空）
    for x in (1, 5):
        for y in range(1, h - 1):
            if (y - 1) % 3 != 2:
                px[x0 + x, y0 + y] = P["marking_yellow"]


# ---------------------------------------------------------------- 模型
def build_carrier_deck():
    """右舷飞行甲板：挂在右臂外侧，木甲板面 + 外舷收边 + 短支架 + 小舰岛。

    几何总数 9 块，远低于 20 块上限：
      deck 骨骼  2 块（甲板本体、外舷加厚条）
      mount 骨骼 4 块（后段短支架、内侧纵向龙骨、两段阶梯吊臂）
      island 骨骼 3 块（基座、舰桥、桅杆）
    """
    m = builder.BedrockModel(
        "geometry.carrier_deck", texture_size=64,
        visible_bounds=(2, 2, (-0.95, 0.1, 0)),
        texel_scale=1,  # 严格 1:1，1 texel = 1/16 格
    )
    m.bone("root")
    # 支撑桁架跟随躯干，不参与待机浮动
    m.bone("mount", parent="root", pivot=(0, 0, 0))
    # 甲板整体做极轻微浮动；pivot 取甲板中心
    m.bone("deck", parent="root", pivot=(-15.0, -1.3, -3.5))
    # 舰岛挂在甲板上，叠一点独立摆动
    m.bone("island", parent="deck", pivot=(-18.5, -0.8, 1.0))

    # ---- 甲板本体：顶面 7×11 的跑道贴图，外舷侧面用金属收边 ----
    m.cube("deck", (DECK_X0, DECK_TOP_Y - DECK_THICKNESS, DECK_Z0),
           (7, DECK_THICKNESS, DECK_LENGTH), "deck_edge", {
               "up": "deck_top",
               "down": "deck_under",
               "west": "metal_gray",   # west = 模型 -X，外舷侧
           })
    # 外舷下方的一层加厚条，从侧面看像舰体舷台
    m.cube("deck", (-18.9, -2.8, DECK_Z0), (1, 1, DECK_LENGTH), "metal_dark")

    # ---- 后段短支架 + 前伸吊臂（金属灰），不向躯干拉梁 ----
    # 右臂外缘只有 x=-11.5 的立柱（z≈1..2）真正能受力：
    #   1. 后段短支架：x -13.5..-11.5、z 0..2，东端面贴住右臂立柱；
    #   2. 内侧纵向龙骨：沿甲板内缘 z -9..2，顶面贴甲板底面，把前段托住；
    #   3. 两段阶梯吊臂：从后段支架向前、向外逐级下探，托住龙骨前段。
    # 三段都是轴对齐块，用台阶模拟斜撑（不引入非 90° 旋转）。
    m.cube("mount", (-13.5, -3.0, 0.0), (2, 2, 2), "metal_gray")
    m.cube("mount", (-13.0, -2.75, -9.0), (1, 1, 11), "metal_dark")
    m.cube("mount", (-14.0, -3.2, -4.0), (2, 2, 4), "metal_gray")
    m.cube("mount", (-14.5, -3.4, -8.0), (2, 2, 4), "metal_gray")

    # ---- 右舷小舰岛：1 宽 × 2 深，放在甲板后段外舷 ----
    # 舰岛 x=-19.0..-18.0、z=0..2（甲板最后 2 单位），一半搭甲板、一半探出舷外。
    # DECK_1 飞机足迹最外到 x=-17.72，中间留 0.28 单位，不压飞机。
    m.cube("island", (-19.0, -0.8, 0.0), (1, 2, 2), "armor_light", {
        "west": "armor_mid",
        "up": "armor_white",
    })
    m.cube("island", (-19.0, 1.2, 0.5), (1, 1, 1), "armor_white", {
        "west": "armor_mid",
    })
    m.cube("island", (-19.0, 2.2, 0.5), (1, 1, 1), "metal_dark")

    return m


# ---------------------------------------------------------------- 动画
def build_animation():
    """idle：整块甲板极轻浮动 + 舰岛一点点摆头，不提供 fire。

    甲板承载 DECK_1/2 的飞机（飞机由挂点单独渲染），浮动幅度必须远小于
    1 单位，否则飞机和甲板会肉眼可见地脱开。这里最大 0.06 单位。
    """
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    "deck": {
                        "position": {
                            "0.0": [0, 0, 0],
                            "1.0": [0, 0.06, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0, -0.04, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "island": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0, 0.5, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0, -0.4, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                },
            }
        },
    }


# ---------------------------------------------------------------- 核对
def print_coordinate_check():
    """打印甲板面与 DECK_1/2 挂点的坐标核对结果。"""
    print(f"deck surface y={DECK_TOP_Y}  x={DECK_X0}..{DECK_X1}  "
          f"z={DECK_Z0}..{DECK_Z1} (model space)")
    for name, (mx, my, mz) in DECK_MOUNTS.items():
        on_deck = (DECK_X0 <= mx <= DECK_X1 and DECK_Z0 <= mz <= DECK_Z1)
        gap = my - DECK_TOP_Y
        print(f"{name}: model=({mx}, {my}, {mz})  "
              f"gap_above_deck={gap:+.2f}  on_deck={on_deck}")
    # 飞机是 16px 的平面物品模型、挂点 scale 0.34 → 半边长约 2.72 单位。
    # 只用于提示主线程：前后两架之间以及后缘是否压到甲板边。
    half = 8 * 0.34
    print(f"aircraft sprite half-size ≈ {half:.2f} units (16px × 0.34)")
    for name, (mx, my, mz) in DECK_MOUNTS.items():
        print(f"  {name} footprint x={mx - half:.2f}..{mx + half:.2f} "
              f"z={mz - half:.2f}..{mz + half:.2f} "
              f"(deck x={DECK_X0}..{DECK_X1}, z={DECK_Z0}..{DECK_Z1})")
    print(f"deck center z={(DECK_Z0 + DECK_Z1) / 2:+.1f} "
          f"(甲板向前伸，后缘只到身体中心附近)")
    # 舰岛 x=-19.0..-18.0、z=0..2，半格搭在甲板外缘；与 DECK_1 足迹最外沿比较
    island_x0, island_x1 = -19.0, -18.0
    sprite_min_x = DECK_MOUNTS["DECK_1"][0] - half
    print(f"island x={island_x0}..{island_x1} z=0..2  "
          f"DECK_1 sprite min_x={sprite_min_x:.2f}  "
          f"clearance={sprite_min_x - island_x1:+.2f}")
    print("support: rear bracket x=-13.5..-11.5 z=0..2 (贴右臂立柱); "
          "keel x=-13.0..-12.0 z=-9..2; "
          "boom x=-14.5..-12.0 z=-8..0 (阶梯吊臂)")


# ---------------------------------------------------------------- 落盘
def write_assets(model, repo_root, dry_run=False):
    img, placed, used = model.build_texture()

    # 甲板顶面二次绘制：木纹 + 中线 + 弹射轨道
    deck_key = ("deck_top", 7, DECK_LENGTH)
    if deck_key not in placed:
        raise RuntimeError(f"找不到甲板顶面 UV 区域 {deck_key}，检查甲板尺寸")
    u, v = placed[deck_key]
    paint_deck_top(img.load(), u, v, *deck_key[1:])

    geo = model.geo_dict(placed)
    anim = build_animation()

    base = os.path.join(repo_root, "src/main/resources/assets/piranport")
    geo_path = os.path.join(base, "geo/rigging/hull/carrier_deck.geo.json")
    tex_path = os.path.join(base, "textures/rigging/hull/carrier_deck.png")
    anim_path = os.path.join(base,
                             "animations/rigging/hull/carrier_deck.animation.json")

    cubes, bones, mats = model.stats()
    print(f"cubes={cubes} bones={bones} materials={len(mats)}")
    print("materials:", ", ".join(mats))
    print(f"atlas regions={len(placed)} used_px={used}/4096 "
          f"({used / 4096 * 100:.1f}%)")
    print(f"palette opaque={len(builder.PALETTE)}")
    print(f"deck top uv={placed[deck_key]} size=(7, {DECK_LENGTH})")
    print_coordinate_check()

    if dry_run:
        print("dry-run: 不写文件")
        return

    for p in (geo_path, tex_path, anim_path):
        os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(geo_path, "w", encoding="utf-8") as f:
        json.dump(geo, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    img.save(tex_path)
    with open(anim_path, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    print("wrote", geo_path)
    print("wrote", tex_path)
    print("wrote", anim_path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    write_assets(build_carrier_deck(), repo_root, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
