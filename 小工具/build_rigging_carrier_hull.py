#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""航母舰体变体（GeckoLib Bedrock）。

航母有飞机时右舷让位给飞行甲板（carrier_deck），所以舰体去掉右侧机械臂，
避免右舷炮座平台 / 立柱和甲板桁架穿插。其余几何与通用舰体完全一致，
骨骼命名保持不变，接线时可以复用同一套挂点与动画驱动。

只读导入 build_rigging_hull_models 的 BedrockModel / 通用舰体 / idle 动画，
不修改共享脚本。

坐标（与通用舰体同一套，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot；
    +Y 朝上；-Z 朝前；+X 左舷、-X 右舷。

贴图复用 general_hull.png：两条机械臂的 cube 尺寸完全相同（左右镜像），
删掉右臂不会移除任何唯一的 (材质, 宽, 高) UV 区域，因此图集打包结果
逐像素不变。脚本每次落盘前都会和磁盘上的 general_hull.png 做像素比对。

用法：
    python3 build_rigging_carrier_hull.py             # 写 geo/animation
    python3 build_rigging_carrier_hull.py --dry-run   # 只打印统计与校验
    python3 build_rigging_carrier_hull.py --preview   # 额外渲染预览图
"""
import argparse
import copy
import json
import os

from PIL import Image

import build_rigging_hull_models as builder

IDENTIFIER = "geometry.carrier_hull"
REMOVED_BONE = "arm_right"


# ---------------------------------------------------------------- 模型
def build_carrier_hull():
    """通用舰体去掉右臂骨骼。

    WHY 用「先建再删」而不是重抄一遍 cube：通用舰体会继续微调甲板/桅杆，
    复制一份坐标必然漂移；这里只做减法，保证左臂、基座、烟囱始终同源。
    """
    m = builder.build_general_hull()
    m.identifier = IDENTIFIER
    removed = [b for b in m.bones if b.name == REMOVED_BONE]
    if not removed:
        raise RuntimeError(f"共享舰体里找不到骨骼 {REMOVED_BONE}，右臂可能已改名")
    m.bones = [b for b in m.bones if b.name != REMOVED_BONE]
    # _by_name 只服务 cube()，落盘前已经用完，这里同步清掉避免残留
    m._by_name.pop(REMOVED_BONE, None)
    return m


# ---------------------------------------------------------------- 动画
def build_animation():
    """复用通用舰体 idle，只删掉 arm_right 的关键帧条目。

    WHY 不重写关键帧：航母舰体只少了一条臂，其余骨骼必须和通用舰体同拍，
    否则两套模型切换时基座、桅杆的呼吸节奏会肉眼可见地不一致。
    """
    anim = copy.deepcopy(builder.build_animation())
    bones = anim["animations"]["idle"]["bones"]
    if REMOVED_BONE not in bones:
        raise RuntimeError(f"共享 idle 动画里找不到 {REMOVED_BONE} 的关键帧")
    bones.pop(REMOVED_BONE)
    return anim


# ---------------------------------------------------------------- 校验
def verify_texture_reuse(model, repo_root):
    """确认删掉右臂后图集仍与 general_hull.png 逐像素一致。"""
    tex_path = os.path.join(
        repo_root, "src/main/resources/assets/piranport",
        "textures/rigging/hull/general_hull.png")
    if not os.path.exists(tex_path):
        print("! 找不到 general_hull.png，跳过贴图比对")
        return False
    built, placed, used = model.build_texture()
    on_disk = Image.open(tex_path).convert("RGBA")
    built = built.convert("RGBA")
    if built.size != on_disk.size:
        print(f"! 贴图尺寸不一致：生成 {built.size} vs 磁盘 {on_disk.size}")
        return False
    if built.tobytes() != on_disk.tobytes():
        print("! 图集与 general_hull.png 不一致，不能复用贴图")
        return False
    print(f"贴图复用校验：与 general_hull.png 逐像素一致（{len(placed)} 个 UV 区域）")
    return True


def verify_geo_matches_general(model, repo_root):
    """确认生成的 geo 就是通用舰体去掉 arm_right，其他 cube 的 UV 完全不变。"""
    general_path = os.path.join(
        repo_root, "src/main/resources/assets/piranport",
        "geo/rigging/hull/general_hull.geo.json")
    if not os.path.exists(general_path):
        print("! 找不到 general_hull.geo.json，跳过几何比对")
        return False
    with open(general_path, "r", encoding="utf-8") as f:
        general = json.load(f)["minecraft:geometry"][0]
    expected = copy.deepcopy(
        [b for b in general["bones"] if b["name"] != REMOVED_BONE])

    _, placed, _ = model.build_texture()
    actual = model.geo_dict(placed)["minecraft:geometry"][0]["bones"]
    if actual != expected:
        print("! 生成的骨骼/cube/UV 与「通用舰体 - arm_right」不一致")
        return False
    print("几何比对：与「通用舰体 - arm_right」完全一致（含 UV）")
    return True


# ---------------------------------------------------------------- 落盘
def write_assets(model, repo_root, dry_run=False):
    geo = model.geo_dict(model.build_texture()[1])
    anim = build_animation()

    base = os.path.join(repo_root, "src/main/resources/assets/piranport")
    geo_path = os.path.join(base, "geo/rigging/hull/carrier_hull.geo.json")
    anim_path = os.path.join(base,
                             "animations/rigging/hull/carrier_hull.animation.json")

    cubes, bones, mats = model.stats()
    print(f"identifier: {model.identifier}")
    print(f"cubes={cubes} bones={bones} materials={len(mats)}")
    print("bones:", ", ".join(b.name for b in model.bones))
    print("materials:", ", ".join(mats))
    verify_texture_reuse(model, repo_root)
    verify_geo_matches_general(model, repo_root)

    if dry_run:
        print("dry-run: 不写文件")
        return

    os.makedirs(os.path.dirname(geo_path), exist_ok=True)
    os.makedirs(os.path.dirname(anim_path), exist_ok=True)
    with open(geo_path, "w", encoding="utf-8") as f:
        json.dump(geo, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    with open(anim_path, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    print("wrote", geo_path)
    print("wrote", anim_path)
    print("贴图沿用 general_hull.png，未新建贴图")


# ---------------------------------------------------------------- 预览
def _load_faces(geo_path, tex_h_px, v_offset_px=0):
    """读取一个 geo 的面，并把 UV 映射到纵向拼接后的图集。

    rigging_geo_preview 一次只吃一张贴图；把 hull / deck 两张 64×64 竖着
    拼成 64×128，再把 deck 的 v 偏移 +64 像素，就能让两个模型共用一个
    z-buffer 正常互相遮挡，而不是两张图叠加前后关系全错。
    """
    import rigging_geo_preview as pv
    geo = pv.load_geometry(geo_path)
    faces, _ = pv.gather_faces(geo, (64, 64))
    out = []
    for f in faces:
        u, v, w, h = f["uv"]  # 归一化到 64×64 贴图
        out.append(dict(
            f,
            uv=(u,
                (v * 64 + v_offset_px) / tex_h_px,
                w,
                h * 64 / tex_h_px),
        ))
    return out


def render_previews(repo_root):
    import numpy as np
    import rigging_geo_preview as pv

    base = os.path.join(repo_root, "src/main/resources/assets/piranport")
    hull_geo = os.path.join(base, "geo/rigging/hull/carrier_hull.geo.json")
    deck_geo = os.path.join(base, "geo/rigging/hull/carrier_deck.geo.json")
    hull_tex_path = os.path.join(base, "textures/rigging/hull/general_hull.png")
    deck_tex_path = os.path.join(base, "textures/rigging/hull/carrier_deck.png")
    out_dir = os.path.join(repo_root, "小工具/_preview")
    os.makedirs(out_dir, exist_ok=True)

    hull_tex = Image.open(hull_tex_path).convert("RGBA")
    deck_tex = Image.open(deck_tex_path).convert("RGBA")

    # 单舰体：玩家参考 + 共享偏移，检查右臂确实消失
    hull_faces, _ = pv.gather_faces(pv.load_geometry(hull_geo), hull_tex.size)
    img = pv.render_view(hull_faces, np.asarray(hull_tex), "iso",
                         size=720, ss=3, show_player=True, shared_offset=True)
    p = os.path.join(out_dir, "preview_rigging_carrier_hull.png")
    img.convert("RGB").save(p)
    print("preview:", p)

    # 舰体 + 甲板：纵向拼接 64×128 图集，共用一个深度缓冲
    combined_tex = Image.new("RGBA", (64, 128), (0, 0, 0, 0))
    combined_tex.paste(hull_tex, (0, 0))
    combined_tex.paste(deck_tex, (0, 64))
    faces = _load_faces(hull_geo, 128, 0) + _load_faces(deck_geo, 128, 64)
    combined = np.asarray(combined_tex)
    for view, suffix in (("iso", "deck_iso"), ("right", "deck_right")):
        img = pv.render_view(faces, combined, view, size=720, ss=3,
                             show_player=True, shared_offset=True)
        p = os.path.join(out_dir, f"preview_rigging_carrier_hull_{suffix}.png")
        img.convert("RGB").save(p)
        print("preview:", p)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--preview", action="store_true", help="渲染预览图")
    args = ap.parse_args()
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    model = build_carrier_hull()
    write_assets(model, repo_root, dry_run=args.dry_run)
    if args.preview and not args.dry_run:
        render_previews(repo_root)


if __name__ == "__main__":
    main()
