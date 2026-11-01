#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""腰部鱼雷挂架（GeckoLib Bedrock）构建器。

复用 build_rigging_hull_models.py 的 BedrockModel / 调色板 / 贴图打包（只读导入），
不复制第二套坐标与贴图逻辑。

坐标约定（与通用舰装架完全一致，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot；
    +Y 朝上；-Z 朝前、+Z 朝后；+X 左舷、-X 右舷。

模型只做「挂架」：左右各一块从髋侧外伸的托板 + 一块矮后挡板，
不画发射管本体、不做环状抱箍。托板顶面与挂点齐平，发射器底座压在上面、
后缘靠住挡板；托板向前伸出托住底座前段。

配套挂点（建议新增 WAIST_LEFT / WAIST_RIGHT，见下方 WAIST_MOUNTS）：
发射器「底座在托板上、管口朝前」= 绕 X 翻 180°（pitch 180）。
因为 item 原点在底座角上而不是中心，左右两侧的 x 原点不对称。

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
# x 左舷、y 向下、z 向后（与模型 +Z 同向）。
# pitch=180 让 item 的 +Y 仍然朝上、+Z（管口）朝前，底座落在 y=0 平面上。
# 左右 x 原点不对称是因为 item 原点在底座角上：同一个旋转下发射器永远
# 从原点向 +X 伸出，所以右舷只能把原点放到外舷角（-9.36）才能落在髋外侧。
LAUNCHER_SCALE = 0.36
WAIST_MOUNTS = {
    "left": dict(x=3.6, y=5.0, z=2.0, yaw=0.0, pitch=180.0, roll=0.0),
    "right": dict(x=-9.36, y=5.0, z=2.0, yaw=0.0, pitch=180.0, roll=0.0),
}

# 托板（模型空间）：x 4..8、厚 1（顶面 y -5）、z -3..2，向体前伸到 z=-3。
SADDLE_X0, SADDLE_X1 = 4, 8
SADDLE_TOP_Y = -5
SADDLE_Z0, SADDLE_Z1 = -3, 2
# 后挡板：压在托板后沿上，前脸 z=1，正好接住发射器后缘（z≈0.99）。
BACKSTOP_Z0, BACKSTOP_Z1 = 1, 3
BACKSTOP_TOP_Y = -4

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
    """左/右腰侧伸出支架 + 一条深色腰带。几何共 6 块，贴图 64×64 1:1。"""
    m = BedrockModel(IDENTIFIER, texture_size=64,
                     # 模型中心大致在 body pivot 下方 5 单位、体前 0.5 单位
                     visible_bounds=(2, 2, (0, -0.3, 0.0)), texel_scale=1)
    m.bone("root")
    m.bone("waist_belt", parent="root", pivot=(0, -5, 2))
    m.bone("rack_left", parent="waist_belt", pivot=(4, -5, 1.5))
    m.bone("rack_right", parent="waist_belt", pivot=(-4, -5, 1.5))

    # ---- 腰带（2 块）：横在腰后，托板从这条带子上生根 ----
    m.cube("waist_belt", (-4, -5.5, 2), (8, 1, 1), "metal_dark")
    m.cube("waist_belt", (-1, -5.5, 3), (2, 1, 1), "red_accent")

    # ---- 左右伸出支架（每侧 2 块，全部镜像 X）----
    # 1) 托板：顶面 y=-5 与挂点齐平，向前伸到 z=-3；顶面炮座环纹给底座一个"座"
    # 2) 后挡板：压在托板后沿，向上 1 格，接住发射器后缘并连到腰带上
    for side, sign in (("left", 1), ("right", -1)):
        bone = f"rack_{side}"
        _add(m, bone, (SADDLE_X0, SADDLE_TOP_Y - 1, SADDLE_Z0),
             (SADDLE_X1 - SADDLE_X0, 1, SADDLE_Z1 - SADDLE_Z0),
             "armor_light", {"up": "platform_ring"}, sign)
        _add(m, bone, (SADDLE_X0, SADDLE_TOP_Y, BACKSTOP_Z0),
             (SADDLE_X1 - SADDLE_X0, BACKSTOP_TOP_Y - SADDLE_TOP_Y,
              BACKSTOP_Z1 - BACKSTOP_Z0),
             "armor_mid", {"up": "trim_gold"}, sign)
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
    """把发射器 3D 物模型按挂点姿态换算成模型空间的轴对齐盒。

    pitch 180 是 90° 的整数倍，盒子旋转后仍是轴对齐，不需要矩阵分解。
    玩家空间映射：(x, y, z) -> (x, -y, -z)（+Y 上、+Z 管口朝前），
    再按 模型 y = -玩家 y、模型 z = 玩家 z 换到舰装空间：
        模型 = 挂点 + (s*x, s*y, -s*z)
    发射器因此从 item 原点向 +X 伸出，右舷要靠挂点 x 落到外舷角。
    """
    elements = _launcher_elements(repo_root)
    if not elements:
        return []
    s = LAUNCHER_SCALE
    mnt = WAIST_MOUNTS[side]
    mx, my, mz = mnt["x"], -mnt["y"], mnt["z"]
    boxes = []
    for e in elements:
        x0, y0, z0 = e["from"]
        x1, y1, z1 = e["to"]
        pts = []
        for xi in (x0, x1):
            for yi in (y0, y1):
                for zi in (z0, z1):
                    pts.append((mx + s * xi, my + s * yi, mz - s * zi))
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
    """检查发射器底座是否压在托板上、后缘靠住挡板、且不穿躯干。"""
    print(f"托板: x {SADDLE_X0}..{SADDLE_X1}, 顶面 y {SADDLE_TOP_Y}, "
          f"z {SADDLE_Z0}..{SADDLE_Z1}；后挡板前脸 z {BACKSTOP_Z0}")
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
        bottom_ok = abs(lo[1] - SADDLE_TOP_Y) < 1e-3
        rear_ok = abs(bz1 - BACKSTOP_Z0) < 0.15
        body_ok = lo[0] >= 4 - 1e-6 if side == "left" else hi[0] <= -4 + 1e-6
        print(f"  {side}: 发射器 x {lo[0]:.2f}..{hi[0]:.2f}, "
              f"y {lo[1]:.2f}..{hi[1]:.2f}, z {lo[2]:.2f}..{hi[2]:.2f}")
        print(f"    底座压在托板 y={SADDLE_TOP_Y}: {'OK' if bottom_ok else 'NG'}; "
              f"后缘靠挡板 z={BACKSTOP_Z0}: {'OK' if rear_ok else 'NG'}; "
              f"不穿躯干(|x|>=4): {'OK' if body_ok else 'NG'}; "
              f"托板承托底座 {ratio * 100:.0f}%")


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
    # 叠发射器物模型，确认底座压在托板上、管口朝前
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
