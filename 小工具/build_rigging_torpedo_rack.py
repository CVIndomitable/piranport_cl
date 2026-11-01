#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""腰部鱼雷挂架（GeckoLib Bedrock）构建器。

复用 build_rigging_hull_models.py 的 BedrockModel / 调色板 / 贴图打包（只读导入），
不复制第二套坐标与贴图逻辑。

坐标约定（与通用舰装架完全一致，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot；
    +Y 朝上；-Z 朝前、+Z 朝后；+X 左舷、-X 右舷。

模型只做「挂架」：髋侧背板 + 腰侧短臂 + 方形抱箍环，不画发射管本体。
抱箍环在 Y-Z 平面上围出 6 高 × 4 深的开口，套住以
scale 0.36、yaw ±90、roll 90 挂上去的鱼雷发射器物模型。

配套挂点（建议新增 WAIST_LEFT / WAIST_RIGHT，见下方 WAIST_MOUNTS）：
发射器物模型的 item 原点不居中，左右两侧的 z 偏移必须各自补偿，
否则右侧发射器会跑到髋前/肩侧。

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

# 配套挂点，数值直接对应 RiggingSocketTransforms.mountPose 的玩家空间参数：
# x 左舷、y 向下、z 向后（与模型 +Z 同向）。左右不对称是因为发射器
# 物模型的原点不在几何中心，用纯旋转镜像不了：
#   左：yaw 90 / roll 90  → 发射器向髋后展开（模型 z +0.2..+3.9）
#   右：yaw -90 / roll 90 → 镜像后整体落在髋前，z 原点要前移 3.6 单位拉回髋后。
LAUNCHER_SCALE = 0.36
WAIST_MOUNTS = {
    "left": dict(x=4.3, y=5.1, z=0.2, yaw=90.0, pitch=0.0, roll=90.0),
    "right": dict(x=-4.3, y=5.1, z=3.8, yaw=-90.0, pitch=0.0, roll=90.0),
}

# 抱箍环（模型空间）：
#   x 7..9，厚 2；外框 y -12..-4、z -1..5；内开口 y -11..-5、z 0..4。
RING_X = 7
RING_W = 2
RING_Y0 = -12
RING_Y1 = -4
RING_Z0 = -1
RING_Z1 = 5

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
    """左/右腰侧挂架 + 一条深色腰带。几何共 16 块，贴图 64×64 1:1。"""
    m = BedrockModel(IDENTIFIER, texture_size=64,
                     # 模型中心大致在 body pivot 下方 8 单位、后方 2 单位
                     visible_bounds=(2, 2, (0, -0.35, 0.05)), texel_scale=1)
    m.bone("root")
    m.bone("waist_belt", parent="root", pivot=(0, -5, 2))
    m.bone("rack_left", parent="waist_belt", pivot=(4, -5, 1.5))
    m.bone("rack_right", parent="waist_belt", pivot=(-4, -5, 1.5))

    # ---- 腰带（2 块）：横在腰后，把左右挂架的受力点连起来 ----
    # 深色带只到髋上方，不压大腿；红色只留 2×1 的小结，避免大面积红。
    m.cube("waist_belt", (-4, -5.5, 2), (8, 1, 1), "metal_dark")
    m.cube("waist_belt", (-1, -5.5, 3), (2, 1, 1), "red_accent")

    # ---- 左右挂架（每侧 7 块，全部镜像 X）----
    # 1) 髋侧背板：贴住躯干外侧面，正面那条红线是 1064 红色挂带的暗示
    # 2) 腰侧短臂：从背板外伸到抱箍上沿
    # 3) 抱箍环：上下两根横梁 + 前后两根立柱，围出 6×4 的开口
    # 4) 抱箍轴销：环正面中心的黄铜销
    for side, sign in (("left", 1), ("right", -1)):
        bone = f"rack_{side}"
        _add(m, bone, (4, -7, -0.5), (1, 3, 3), "armor_mid",
             {"north": "red_accent"}, sign)
        _add(m, bone, (5, -5, 0.5), (2, 1, 2), "armor_light",
             {"north": "trim_gold"}, sign)
        _add(m, bone, (RING_X, -5, RING_Z0), (RING_W, 1, RING_Z1 - RING_Z0),
             "armor_light", None, sign)
        _add(m, bone, (RING_X, RING_Y0, RING_Z0), (RING_W, 1, RING_Z1 - RING_Z0),
             "armor_mid", None, sign)
        _add(m, bone, (RING_X, RING_Y0, RING_Z0), (RING_W, RING_Y1 - RING_Y0, 1),
             "armor_mid", None, sign)
        _add(m, bone, (RING_X, RING_Y0, RING_Z1 - 1), (RING_W, RING_Y1 - RING_Y0, 1),
             "armor_mid", None, sign)
        _add(m, bone, (RING_X + 0.5, -8.5, RING_Z0 - 1), (1, 1, 1),
             "trim_gold", None, sign)
    return m


def build_animation():
    """idle：腰带轻浮 + 抱箍微摆。

    武器不在这个模型里，幅度必须比通用舰装架还小，否则抱箍和发射器会脱开。
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
                            "1.0": [0, 0.14, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0, -0.1, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "rack_left": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.22, 0, 0.12],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.15, 0, -0.08],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "rack_right": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.22, 0, -0.12],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.15, 0, 0.08],
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
    """把发射器 3D 物模型按挂点姿态换算成模型空间的轴对齐盒。

    yaw 90 / roll 90 都是 90° 的整数倍，盒子旋转后仍是轴对齐，不需要矩阵分解。
    映射（玩家空间）：(x, y, z) -> 左：(z, x, y)；右：(-z, x, -y)。
    再按 模型 y = -玩家 y、模型 z = 玩家 z 换到舰装空间。
    """
    elements = _launcher_elements(repo_root)
    if not elements:
        return []
    s = LAUNCHER_SCALE
    mnt = WAIST_MOUNTS[side]
    mx, my, mz = mnt["x"], -mnt["y"], mnt["z"]
    sign = 1 if side == "left" else -1
    boxes = []
    for e in elements:
        x0, y0, z0 = e["from"]
        x1, y1, z1 = e["to"]
        # 先按挂点映射四个对角，再取 min/max（镜像时 from/to 会交换）
        pts = []
        for xi in (x0, x1):
            for yi in (y0, y1):
                for zi in (z0, z1):
                    pts.append((
                        mx + sign * s * zi,
                        my - s * xi,
                        mz + sign * s * yi,
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
    """打印发射器在抱箍平面附近的截面，确认不会穿模/脱出。"""
    inner_y = (RING_Y0 + 1, RING_Y1 - 1)   # -11..-5
    inner_z = (RING_Z0 + 1, RING_Z1 - 1)   # 0..4
    print(f"抱箍内开口: x {RING_X}..{RING_X + RING_W}, y {inner_y[0]}..{inner_y[1]}, "
          f"z {inner_z[0]}..{inner_z[1]}")
    for side, plane in (("left", 1), ("right", -1)):
        boxes = launcher_model_boxes(repo_root, side)
        if not boxes:
            print(f"  {side}: 找不到 {LAUNCHER_PROXY_ITEM}_3d.json")
            continue
        lo, hi = RING_X, RING_X + RING_W
        near = []
        for b in boxes:
            # 右舷镜像到左舷坐标系再判断，左右环都在 |x| 7..9
            x0, x1 = (b[0][0], b[1][0]) if plane > 0 else (-b[1][0], -b[0][0])
            if x1 > lo and x0 < hi:
                near.append(b)
        ys = [v for b in near for v in (b[0][1], b[1][1])]
        zs = [v for b in near for v in (b[0][2], b[1][2])]
        fits = (min(ys) >= inner_y[0] and max(ys) <= inner_y[1]
                and min(zs) >= inner_z[0] and max(zs) <= inner_z[1])
        print(f"  {side}: 环平面附近 {len(near)} 盒，"
              f"y {min(ys):.2f}..{max(ys):.2f}, z {min(zs):.2f}..{max(zs):.2f} "
              f"{'在开口内 OK' if fits else '超出开口，需调挂点'}")


# ---------------------------------------------------------------- 预览
def _player_ghost_with_launcher(repo_root):
    """返回临时覆盖的 PLAYER_GHOST：玩家上身 + 左右发射器占位。

    发射器用暖色区分，只画 24% 透明度、不写深度，用来核对抱箍位置。
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
    # 叠发射器物模型，确认抱箍套住发射器
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
