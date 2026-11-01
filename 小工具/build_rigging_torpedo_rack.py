#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""腰部鱼雷挂架（GeckoLib Bedrock）构建器。

复用 build_rigging_hull_models.py 的 BedrockModel / 调色板 / 贴图打包（只读导入），
不复制第二套坐标与贴图逻辑。

坐标约定（与通用舰装架完全一致，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot；
    +Y 朝上；-Z 朝前、+Z 朝后；+X 左舷、-X 右舷。

模型只做「挂架」：腰部横梁 + 左右吊带 + 大腿根托板，不画发射管本体。
托板顶面 player y=11（大腿根）；模型 z -5..1 对应 player z -1..+5，
即托板中心前移到 player z=+2.0。腰带留在腰上，用 1 块上接头 + 1 条吊带
把托板吊下来，不悬空。

配套挂点（WAIST_LEFT / WAIST_RIGHT，接线侧已在 mountPose 里）：
- 模型空间 +Z 朝后、玩家空间 +Z 朝前，所以挂点 z = -模型 z。
- 发射器底座下沉补偿 8*0.36 = 2.88 单位 → mount y = 托板顶面 player y - 2.88。
- yaw 180 让 3D 发射器管口朝前、底座落在托板上。

用法：
    python3 build_rigging_torpedo_rack.py            # 写 geo/png/animation
    python3 build_rigging_torpedo_rack.py --dry-run  # 只打印统计
    python3 build_rigging_torpedo_rack.py --preview  # 写资产 + 预览图
"""
import argparse
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_rigging_hull_models import BedrockModel  # noqa: E402

IDENTIFIER = "geometry.waist_torpedo_rack"
ASSET_STEM = "waist_torpedo_rack"

# 配套挂点（RiggingSocketTransforms.mountPose 的玩家空间参数）：
# x 左舷、y 向下、z 向前。托板顶面 player y=11 → mount y = 11.0 - 2.88 = 8.12；
# 托板 z 中心 模型 -2.0 → 挂点 z = +2.0（朝前）。
LAUNCHER_SCALE = 0.36
WAIST_MOUNTS = {
    "left": dict(x=6.0, y=11.0, z=2.0, yaw=180.0, pitch=0.0, roll=0.0),
    "right": dict(x=-6.0, y=11.0, z=2.0, yaw=180.0, pitch=0.0, roll=0.0),
}
# item 物模型的底座中心（x 中心 8、z 中心 (2.8+20.3)/2）；预览时用它把底座对到挂点
LAUNCHER_ITEM_CENTER_X = 8.0
LAUNCHER_ITEM_CENTER_Z = 11.55

# 托板（模型空间）：x 4..8、顶面 y -11（player y 11）、z -5..1（player z -1..+5）
SADDLE_X0, SADDLE_X1 = 4, 8
SADDLE_TOP_Y = -11
SADDLE_Z0, SADDLE_Z1 = -5, 1
# 吊带/上接头：上接头从腰部横梁前脸前伸 1 格，吊带垂直落到托板后沿 z=1；
# x 7..8 落在托板宽度内，吊带底端与托板后脸是面接触而不是只擦一个角。
STRAP_X = 7
LINK_Z0, LINK_Z1 = 1, 2

# 预检查用：确认环内开口能装下推荐的发射器物模型
LAUNCHER_PROXY_ITEM = "quintuple_61cm_torpedo"


def _add(m, bone, origin, size, material, face_materials=None, sign=1):
    """加一个立方体；sign=-1 时沿 X 轴镜像（面材质 east/west 互换）。"""
    x, y, z = origin
    faces = dict(face_materials or {})
    if sign < 0:
        x = -(x + size[0])
        faces = {
            ("west" if f == "east" else "east" if f == "west" else f): mat
            for f, mat in faces.items()
        }
    m.cube(bone, (x, y, z), size, material, faces)


def build_waist_torpedo_rack():
    """腰部横梁 + 左右吊带 + 大腿根托板。几何共 8 块，贴图 64×64 1:1。"""
    m = BedrockModel(IDENTIFIER, texture_size=64,
                     # 模型中心大致在 body pivot 下方 8.5 单位、托板靠前
                     visible_bounds=(2, 2, (0, -0.45, 0.1)), texel_scale=1)
    m.bone("root")
    m.bone("waist_belt", parent="root", pivot=(0, -5, 2))
    m.bone("rack_left", parent="waist_belt", pivot=(7.5, -5.5, 1.5))
    m.bone("rack_right", parent="waist_belt", pivot=(-7.5, -5.5, 1.5))

    # ---- 腰部横梁（1 块）+ 红结（1 块）：留在腰上，两端伸到髋外侧接吊带 ----
    m.cube("waist_belt", (-8, -5.5, 2), (16, 1, 1), "metal_dark")
    m.cube("waist_belt", (-1, -5.5, 3), (2, 1, 1), "red_accent")

    # ---- 每侧 3 块：上接头 + 吊带 + 托板（全部镜像 X）----
    # 1) 上接头：从横梁前脸往前 1 格，给吊带一个可见挂点
    # 2) 吊带：垂直落到大腿根，贴在托板后沿
    # 3) 托板：顶面 y=-11 与挂点齐平，向前伸到模型 z=-5；顶面炮座环纹当"座"
    for side, sign in (("left", 1), ("right", -1)):
        bone = f"rack_{side}"
        _add(m, bone, (STRAP_X, -5.5, LINK_Z0), (1, 1, 1), "armor_mid",
             {"north": "trim_gold"}, sign)
        _add(m, bone, (STRAP_X, -11.5, LINK_Z0), (1, 6, 1), "armor_mid", None, sign)
        _add(m, bone, (SADDLE_X0, SADDLE_TOP_Y - 1, SADDLE_Z0),
             (SADDLE_X1 - SADDLE_X0, 1, SADDLE_Z1 - SADDLE_Z0),
             "armor_light", {"up": "platform_ring"}, sign)
    return m


def build_animation():
    """idle：腰带轻浮 + 托板微摆。

    发射器不在这个模型里，幅度必须比通用舰装架还小，
    否则托板和发射器底座会肉眼可见地脱开。
    """
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    "waist_belt": {
                        "position": {
                            "0.0": [0, 0, 0],
                            "1.0": [0, 0.08, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0, -0.06, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "rack_left": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.12, 0, 0.08],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.09, 0, -0.05],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "rack_right": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.12, 0, -0.08],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.09, 0, 0.05],
                            "4.0": [0, 0, 0],
                        }
                    },
                },
            }
        },
    }


# ---------------------------------------------------------------- 落盘
def asset_paths(repo_root):
    base = os.path.join(repo_root, "src/main/resources/assets/piranport")
    return (
        os.path.join(base, f"geo/rigging/hull/{ASSET_STEM}.geo.json"),
        os.path.join(base, f"textures/rigging/hull/{ASSET_STEM}.png"),
        os.path.join(base, f"animations/rigging/hull/{ASSET_STEM}.animation.json"),
    )


def write_assets(model, repo_root, dry_run=False):
    img, placed, used = model.build_texture()
    geo = model.geo_dict(placed)
    anim = build_animation()
    geo_path, tex_path, anim_path = asset_paths(repo_root)

    cubes, bones, mats = model.stats()
    print(f"cubes={cubes} bones={bones} materials={len(mats)}")
    print("materials:", ", ".join(mats))
    print(f"atlas regions={len(placed)} used_px={used}/4096 "
          f"({used / 4096 * 100:.1f}%)")

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


# ---------------------------------------------------------------- 发射器占位
def _launcher_elements(repo_root):
    path = os.path.join(repo_root, "src/main/resources/assets/piranport/models/item",
                        f"{LAUNCHER_PROXY_ITEM}_3d.json")
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f).get("elements", [])


def launcher_model_boxes(repo_root, side):
    """按接线后的目标摆放，把五联装物模型换算成模型空间的轴对齐盒。

    这是预览代理，不是物品渲染管线的逐字模拟：接线用 yaw 180 + 底座补偿
    8*0.36，让底座中心落在挂点 x 上、管口（item +Z）朝模型 -Z。
    item 原点在底座角上，所以先减掉底座中心 (8, 11.55)：
        模型 = (挂点x - s*(x-8), -挂点y + s*y, -挂点z - s*(z-11.55))
    旋转全是 0°/180°，盒子仍是轴对齐，不用矩阵分解。
    """
    elements = _launcher_elements(repo_root)
    if not elements:
        return []
    s = LAUNCHER_SCALE
    mnt = WAIST_MOUNTS[side]
    mx, my, mz = mnt["x"], -mnt["y"], -mnt["z"]
    boxes = []
    for e in elements:
        x0, y0, z0 = e["from"]
        x1, y1, z1 = e["to"]
        pts = []
        for xi in (x0, x1):
            for yi in (y0, y1):
                for zi in (z0, z1):
                    pts.append((
                        mx - s * (xi - LAUNCHER_ITEM_CENTER_X),
                        my + s * yi,
                        mz - s * (zi - LAUNCHER_ITEM_CENTER_Z),
                    ))
        boxes.append((
            tuple(min(p[i] for p in pts) for i in range(3)),
            tuple(max(p[i] for p in pts) for i in range(3)),
        ))
    return boxes


def _cube_signature(cube, mirror=False):
    """cube 的可比较签名；mirror=True 时按 X 轴镜像（east/west 互换）。"""
    ox, oy, oz = cube.origin
    if mirror:
        ox = -(ox + cube.size[0])
        faces = {("west" if f == "east" else "east" if f == "west" else f): m
                 for f, m in cube.face_materials.items()}
    else:
        faces = dict(cube.face_materials)
    return (round(ox, 3), round(oy, 3), round(oz, 3), cube.size,
            cube.material, tuple(sorted(faces.items())))


def report_symmetry(model):
    """确认左右挂架严格镜像（几何 + 面材质，不靠肉眼）。"""
    bones = {b.name: b for b in model.bones}
    left = {_cube_signature(c, mirror=True) for c in bones["rack_left"].cubes}
    right = {_cube_signature(c) for c in bones["rack_right"].cubes}
    print(f"左右镜像: {'OK' if left == right else '不一致'} "
          f"({len(left)} / {len(right)} 块)")


def report_launcher_fit(repo_root):
    """检查发射器底座是否压在托板上、管口朝前、且不穿躯干。"""
    z_center_model = (SADDLE_Z0 + SADDLE_Z1) / 2.0
    print(f"托板: 模型 x {SADDLE_X0}..{SADDLE_X1}, 顶面 y {SADDLE_TOP_Y}, "
          f"z {SADDLE_Z0}..{SADDLE_Z1} "
          f"→ player y={-SADDLE_TOP_Y:.1f}, z 中心={-z_center_model:+.1f}")
    for side in ("left", "right"):
        boxes = launcher_model_boxes(repo_root, side)
        if not boxes:
            print(f"  {side}: 找不到 {LAUNCHER_PROXY_ITEM}_3d.json")
            continue
        sx0, sx1 = ((SADDLE_X0, SADDLE_X1) if side == "left"
                    else (-SADDLE_X1, -SADDLE_X0))
        lo = tuple(min(b[0][i] for b in boxes) for i in range(3))
        hi = tuple(max(b[1][i] for b in boxes) for i in range(3))
        # 底座 = item y 最小的一层盒子（底板），用它算与托板的重叠面积
        base = [b for b in boxes if b[0][1] <= lo[1] + 0.25]
        bx0 = min(b[0][0] for b in base)
        bx1 = max(b[1][0] for b in base)
        bz0 = min(b[0][2] for b in base)
        bz1 = max(b[1][2] for b in base)
        ox = max(0.0, min(bx1, sx1) - max(bx0, sx0))
        oz = max(0.0, min(bz1, SADDLE_Z1) - max(bz0, SADDLE_Z0))
        ratio = ox * oz / max((bx1 - bx0) * (bz1 - bz0), 1e-9)
        inner = lo[0] if side == "left" else -hi[0]   # 到躯干中心一侧的最近距离
        bottom_ok = abs(lo[1] - SADDLE_TOP_Y) < 1e-3
        print(f"  {side}: 发射器 x {lo[0]:.2f}..{hi[0]:.2f}, "
              f"y {lo[1]:.2f}..{hi[1]:.2f}, z {lo[2]:.2f}..{hi[2]:.2f}")
        print(f"    底座压托板 y={SADDLE_TOP_Y}: {'OK' if bottom_ok else 'NG'}; "
              f"管口 z={lo[2]:.2f}（负=朝前）; 后缘 z={bz1:.2f} 贴吊带 z={LINK_Z0}; "
              f"内缘 |x|={inner:.2f}（躯干面 4.0）; 承托 {ratio * 100:.0f}%")


# ---------------------------------------------------------------- 预览
def _player_ghost_with_launcher(repo_root):
    """返回临时覆盖的 PLAYER_GHOST：玩家上身 + 左右发射器占位。

    发射器用暖色区分，只画 24% 透明度、不写深度，用来核对托板位置。
    """
    ghost = {}
    for side in ("left", "right"):
        for i, (lo, hi) in enumerate(launcher_model_boxes(repo_root, side)):
            ghost[f"launcher_{side}_{i}"] = (
                tuple(lo),
                tuple(hi[j] - lo[j] for j in range(3)),
                (196, 132, 72),
            )
    return ghost


def write_previews(repo_root, out_dir):
    import numpy as np
    import rigging_geo_preview as pv

    os.makedirs(out_dir, exist_ok=True)
    geo_path, tex_path, _ = asset_paths(repo_root)
    geo = pv.load_geometry(geo_path)
    tex = np.asarray(Image.open(tex_path).convert("RGBA"))
    faces, _ = pv.gather_faces(geo, (64, 64))

    def render(name, view, show_player=False, shared_offset=False, launcher=False):
        old = pv.PLAYER_GHOST
        extra = {}
        if launcher:
            extra = _player_ghost_with_launcher(repo_root)
        if show_player or launcher:
            merged = dict(old)
            merged.update(extra)
            pv.PLAYER_GHOST = merged
        try:
            image = pv.render_view(faces, tex, view, size=720, ss=3,
                                   show_player=show_player or launcher,
                                   shared_offset=shared_offset)
        finally:
            pv.PLAYER_GHOST = old
        path = os.path.join(out_dir, name)
        image.convert("RGB").save(path)
        print("preview:", path)

    prefix = "preview_rigging_torpedo_rack"
    render(f"{prefix}.png", "iso")
    render(f"{prefix}_front.png", "front")
    render(f"{prefix}_back.png", "back")
    render(f"{prefix}_left.png", "left")
    render(f"{prefix}_right.png", "right")
    # 作者坐标系（与通用舰装架 geo 对比时看这张）
    render(f"{prefix}_player_nooffset_iso.png", "iso", show_player=True)
    # 正面：确认挂架只占髋外侧，不挡大腿正前方
    render(f"{prefix}_player_front.png", "front", show_player=True)
    # applyBase 的共享偏移 (0, 0.28, 0.34) 格之后的位置
    render(f"{prefix}_player_iso.png", "iso", show_player=True, shared_offset=True)
    # 叠发射器物模型，确认底座压在托板上、管口朝前、托板在大腿根
    render(f"{prefix}_launcher_iso.png", "iso", launcher=True)
    render(f"{prefix}_launcher_left.png", "left", launcher=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--preview", action="store_true", help="写资产并渲染预览图")
    args = ap.parse_args()

    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    model = build_waist_torpedo_rack()
    write_assets(model, repo_root, dry_run=args.dry_run)
    report_symmetry(model)
    report_launcher_fit(repo_root)

    if args.preview and not args.dry_run:
        out_dir = os.path.join(repo_root, "小工具/_preview")
        write_previews(repo_root, out_dir)


if __name__ == "__main__":
    main()
