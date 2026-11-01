#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Bedrock geo（GeckoLib）离线预览器。

用途：不启动游戏就能检查舰装 geo + 贴图的立体效果，支持正/背/侧/等轴测多视角。
与 render_block_model_preview.py 的区别：Bedrock cube 的每个面有独立的 uv / uv_size，
而且有骨骼层级、pivot、rotation，不能按 Java 方块模型的三面可见法偷懒。

坐标约定（与 RiggingAttachedLayer 一致）：
    1 单位 = 1/16 格；+Y 朝上；-Z 朝前；+X 为左舷。
    geo 里 authored 的就是这个空间（渲染时游戏再做 X 翻转，本模型左右对称，预览不受影响）。

用法：
    python3 rigging_geo_preview.py <geo.json> <texture.png> <out.png> \
        [--views iso,front,back,left] [--show-player] [--size 720] [--ss 3]

多视角时输出 <stem>_<view>.png；单视角时直接写 out。
"""
import argparse
import json
import math
import os

import numpy as np
from PIL import Image

# GeckoLib 的 Bedrock 材质采样方向：每个面给出四个顶点，顺序为
# 贴图左上(TL)、右上(TR)、右下(BR)、左下(BL)。角标 (ix, iy, iz)：
# 0 = 该轴最小值，1 = 该轴最大值。
# 这套顺序是从 GeckoLib 4.9.3 的 BakedModelFactory$VertexSet / GeoQuad.build
# 反编译确认的，不要按「从外面看自然朝向」想当然。
FACE_CORNERS = {
    "north": [(0, 1, 0), (1, 1, 0), (1, 0, 0), (0, 0, 0)],
    "south": [(1, 1, 1), (0, 1, 1), (0, 0, 1), (1, 0, 1)],
    "east":  [(1, 1, 0), (1, 1, 1), (1, 0, 1), (1, 0, 0)],
    "west":  [(0, 1, 1), (0, 1, 0), (0, 0, 0), (0, 0, 1)],
    "up":    [(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)],
    "down":  [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)],
}
FACE_NORMALS = {
    "north": (0, 0, -1), "south": (0, 0, 1),
    "east": (1, 0, 0), "west": (-1, 0, 0),
    "up": (0, 1, 0), "down": (0, -1, 0),
}
# 固定光照（不随相机变），和 MC 的面明暗一致
FACE_SHADE = {
    "up": 1.00, "down": 0.50,
    "north": 0.80, "south": 0.80,
    "east": 0.62, "west": 0.62,
}


def _shade_for_normal(n):
    """按世界法线的主轴取光，旋转过的骨骼/方块也能得到正确明暗。"""
    ax = int(np.argmax(np.abs(n)))
    if ax == 1:
        return FACE_SHADE["up"] if n[1] > 0 else FACE_SHADE["down"]
    if ax == 2:
        return FACE_SHADE["south"]
    return FACE_SHADE["east"]


def _rot_matrix(rot_deg):
    """Bedrock 骨骼/方块旋转：按 Z→Y→X 顺序应用（GeckoLib optionalRotateZYX）。

    返回 3x3 行主序矩阵，等价于 Rx @ Ry @ Rz。
    """
    rx, ry, rz = (math.radians(float(v)) for v in rot_deg)
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], dtype=float)
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=float)
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], dtype=float)
    return Rx @ Ry @ Rz


def _affine(rot_deg, pivot):
    """绕 pivot 旋转的 4x4 矩阵（行主序，np 乘法用 p @ M）。"""
    T = np.eye(4)
    T[:3, 3] = -np.asarray(pivot, dtype=float)
    R = np.eye(4)
    if rot_deg is not None and any(abs(float(v)) > 1e-9 for v in rot_deg):
        R[:3, :3] = _rot_matrix(rot_deg)
    Ti = np.eye(4)
    Ti[:3, 3] = np.asarray(pivot, dtype=float)
    return Ti @ R @ T


class Camera:
    """正交相机：eye/target 只用来确定方向，不做透视除法。"""

    def __init__(self, eye, target, up=(0, 1, 0)):
        self.target = np.asarray(target, dtype=float)
        fwd = self.target - np.asarray(eye, dtype=float)
        fwd /= np.linalg.norm(fwd)
        up = np.asarray(up, dtype=float)
        right = np.cross(fwd, up)
        if np.linalg.norm(right) < 1e-8:
            # 正上方/正下方视角时换一个 up
            right = np.cross(fwd, np.array([0.0, 0.0, -1.0]))
        right /= np.linalg.norm(right)
        up2 = np.cross(right, fwd)
        self.forward = fwd
        self.right = right
        self.up = up2

    def project(self, p):
        q = np.asarray(p, dtype=float) - self.target
        return np.array([q @ self.right, q @ self.up, q @ self.forward])


VIEWS = {
    # 模型 -Z 朝前：front 从 -Z 看；left 从 +X（左舷）看。
    "iso":   dict(eye=(14.0, 9.0, -17.0), target=(0.0, 2.0, 1.5)),
    "front": dict(eye=(0.0, 2.0, -24.0), target=(0.0, 2.0, 1.5)),
    "back":  dict(eye=(0.0, 2.0, 26.0), target=(0.0, 2.0, 1.5)),
    "left":  dict(eye=(26.0, 2.0, 1.5), target=(0.0, 2.0, 1.5)),
    "right": dict(eye=(-26.0, 2.0, 1.5), target=(0.0, 2.0, 1.5)),
    "top":   dict(eye=(0.0, 28.0, 1.5), target=(0.0, 2.0, 1.5),
                  up=(0.0, 0.0, -1.0)),
}

# 参考用玩家上身，按任务给的定义换算到舰装模型空间：
# 原点 = 躯干 body pivot；玩家模型 +Y 朝下、-Z 朝前（玩家模型的脸在 -Z 面），
# 经 Axis.XP(180) 后变成模型 +Y 朝上、+Z 朝后。
#
# 注意：这里**没有**叠加 RiggingSocketTransforms.applyBase 里给武器挂点用的
# translate(0, 0.28, 0.34) 格。船体层是否复用那套共享偏移属于接线决策，
# 详见 build_rigging_hull_models.py 顶部说明；如果直接照搬，烟囱会整体埋进头/躯干。
PLAYER_GHOST = {
    # name: (origin, size, rgb)
    "torso": ((-4, -12, -2), (8, 12, 4), (104, 146, 184)),
    "head": ((-4, 0, -4), (8, 8, 8), (128, 170, 206)),
    "arm_left": ((4, -12, -2), (4, 12, 4), (104, 146, 184)),
    "arm_right": ((-8, -12, -2), (4, 12, 4), (104, 146, 184)),
}


def load_geometry(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    geoms = data.get("minecraft:geometry")
    if isinstance(geoms, list):
        if not geoms:
            raise ValueError("minecraft:geometry 为空")
        return geoms[0]
    return data


def box_uv_regions(size, uv):
    """Bedrock box uv（"uv": [u,v]）的经典十字展开。

    返回 {face: (u, v, w, h)}；w/h 是像素尺寸，可带小数。
    """
    w, h, d = (float(v) for v in size)
    u, v = (float(x) for x in uv)
    return {
        "west":  (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east":  (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
        "up":    (u + d, v, w, d),
        "down":  (u + d + w, v, w, d),
    }


def gather_faces(geo, tex_size):
    """遍历骨骼层级，算出每个面的世界顶点、UV 像素矩形、法线。

    返回 (faces, corners_for_bounds)。corners_for_bounds 是每个 cube 的 8 个角，
    用于自动取景。
    """
    tex_w, tex_h = tex_size
    bones = {b["name"]: b for b in geo.get("bones", [])}
    transformed = {}  # bone name -> 4x4
    faces = []
    bounds_points = []

    def bone_matrix(name):
        if name in transformed:
            return transformed[name]
        b = bones[name]
        parent = b.get("parent")
        parent_m = bone_matrix(parent) if parent else np.eye(4)
        pivot = b.get("pivot", [0, 0, 0])
        m = parent_m @ _affine(b.get("rotation"), pivot)
        transformed[name] = m
        return m

    for b in geo.get("bones", []):
        m = bone_matrix(b["name"])
        for cube in b.get("cubes", []):
            ox, oy, oz = (float(v) for v in cube["origin"])
            sx, sy, sz = (float(v) for v in cube["size"])
            inflate = float(cube.get("inflate", 0.0))
            origin = np.array([ox - inflate, oy - inflate, oz - inflate])
            size = np.array([sx + 2 * inflate, sy + 2 * inflate, sz + 2 * inflate])
            maxp = origin + size

            cube_m = m
            if cube.get("rotation"):
                cube_m = m @ _affine(cube["rotation"], cube.get("pivot", [0, 0, 0]))

            corners = []
            for ix in (0, 1):
                for iy in (0, 1):
                    for iz in (0, 1):
                        p = np.array([maxp[0] if ix else origin[0],
                                      maxp[1] if iy else origin[1],
                                      maxp[2] if iz else origin[2], 1.0])
                        corners.append((cube_m @ p)[:3])
            bounds_points.extend(corners)

            # UV：每面独立 或 box uv
            uvunion = cube.get("uv")
            if isinstance(uvunion, dict):
                per_face = {}
                for fname, fv in uvunion.items():
                    if fname not in FACE_CORNERS:
                        continue
                    u, v = (float(x) for x in fv["uv"])
                    w, h = (float(x) for x in fv["uv_size"])
                    per_face[fname] = (u, v, w, h)
            elif isinstance(uvunion, list):
                per_face = box_uv_regions(size, uvunion)
            else:
                per_face = {}

            for fname, (u, v, w, h) in per_face.items():
                verts = []
                for (ix, iy, iz) in FACE_CORNERS[fname]:
                    p = np.array([maxp[0] if ix else origin[0],
                                  maxp[1] if iy else origin[1],
                                  maxp[2] if iz else origin[2], 1.0])
                    verts.append((cube_m @ p)[:3])
                n = np.array(FACE_NORMALS[fname], dtype=float)
                # 旋转后的法线只取方向，用于背面剔除/取光
                n3 = cube_m[:3, :3] @ n
                faces.append(dict(
                    name=fname,
                    verts=verts,
                    uv=(u / tex_w, v / tex_h, w / tex_w, h / tex_h),
                    normal=n3,
                    shade=_shade_for_normal(n3),
                ))
    return faces, bounds_points


def _ghost_faces():
    """把上身占位盒拆成面，返回与 gather_faces 同结构（UV 无意义）。"""
    faces = []
    for _, (org, size, color) in PLAYER_GHOST.items():
        o = np.array(org, dtype=float)
        s = np.array(size, dtype=float)
        mx = o + s
        for fname, corners in FACE_CORNERS.items():
            verts = []
            for (ix, iy, iz) in corners:
                verts.append(np.array([mx[0] if ix else o[0],
                                       mx[1] if iy else o[1],
                                       mx[2] if iz else o[2]], dtype=float))
            faces.append(dict(name=fname, verts=verts, uv=None,
                              normal=np.array(FACE_NORMALS[fname], dtype=float),
                              shade=FACE_SHADE[fname], color=color))
    return faces


def background(size, top=(246, 247, 249), bottom=(224, 227, 232)):
    grad = np.linspace(0, 1, size, dtype=np.float32)[:, None, None]
    top = np.array(top, dtype=np.float32)
    bottom = np.array(bottom, dtype=np.float32)
    img = top * (1 - grad) + bottom * grad
    img = np.repeat(img, size, axis=1)
    out = np.zeros((size, size, 4), dtype=np.uint8)
    out[..., :3] = np.clip(img, 0, 255).astype(np.uint8)
    out[..., 3] = 255
    return out


def _tri_project(verts, cam):
    return [cam.project(v) for v in verts]


def _fit_scale(projected_points, size, margin=0.88):
    """根据投影点自动取景，返回 (scale, cx, cy)。projected: Nx3 (x,y,depth)。"""
    pts = np.asarray(projected_points)
    if pts.size == 0:
        return 1.0, size / 2, size / 2
    minx, maxx = pts[:, 0].min(), pts[:, 0].max()
    miny, maxy = pts[:, 1].min(), pts[:, 1].max()
    extent = max(maxx - minx, maxy - miny, 1e-6)
    scale = size * margin / extent
    cx = size / 2 - (minx + maxx) / 2 * scale
    cy = size / 2 + (miny + maxy) / 2 * scale
    return scale, cx, cy


def render_view(faces, tex, view_name, size=720, ss=3, show_player=False):
    """渲染一个视角，返回 PIL 图像。

    faces 来自 gather_faces；tex 是 numpy 数组 (H,W,4)。
    """
    cam = Camera(**VIEWS[view_name])
    S = int(size * ss)
    tex_h, tex_w = tex.shape[:2]

    all_points = []
    for f in faces:
        all_points.extend(_tri_project(f["verts"], cam))
    ghost_faces = _ghost_faces() if show_player else []
    ghost_points = []
    for f in ghost_faces:
        ghost_points.extend(_tri_project(f["verts"], cam))
    scale, cx, cy = _fit_scale(all_points + ghost_points, S)

    canvas = background(S).astype(np.float32)
    zbuf = np.full((S, S), np.inf, dtype=np.float32)

    def to_screen(p):
        return np.array([cx + p[0] * scale, cy - p[1] * scale])

    def raster(face, color_mode, depth_write=True, alpha=1.0):
        """color_mode: 'tex' 或 'ghost'。"""
        # 背面剔除：法线朝向相机（法线 · forward < 0）才可见
        if face["normal"] @ cam.forward >= -1e-9:
            return
        sp = [to_screen(p) for p in _tri_project(face["verts"], cam)]
        dp = [p[2] for p in _tri_project(face["verts"], cam)]
        u0, v0, uw, vh = face["uv"] if face["uv"] else (0, 0, 1, 1)
        uvs = [(u0, v0), (u0 + uw, v0), (u0 + uw, v0 + vh), (u0, v0 + vh)]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = tri
            sa, sb, sc = sp[a], sp[b], sp[c]
            denom = (sb[1] - sc[1]) * (sa[0] - sc[0]) + (sc[0] - sb[0]) * (sa[1] - sc[1])
            if abs(denom) < 1e-9:
                continue
            minx = max(0, int(math.floor(min(sa[0], sb[0], sc[0]))))
            maxx = min(S - 1, int(math.ceil(max(sa[0], sb[0], sc[0]))))
            miny = max(0, int(math.floor(min(sa[1], sb[1], sc[1]))))
            maxy = min(S - 1, int(math.ceil(max(sa[1], sb[1], sc[1]))))
            if minx > maxx or miny > maxy:
                continue
            xs = np.arange(minx, maxx + 1, dtype=np.float32)
            ys = np.arange(miny, maxy + 1, dtype=np.float32)
            px, py = np.meshgrid(xs, ys)

            w0 = ((sb[1] - sc[1]) * (px - sc[0]) + (sc[0] - sb[0]) * (py - sc[1])) / denom
            w1 = ((sc[1] - sa[1]) * (px - sc[0]) + (sa[0] - sc[0]) * (py - sc[1])) / denom
            w2 = 1.0 - w0 - w1
            mask = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            if not mask.any():
                continue

            depth = w0 * dp[a] + w1 * dp[b] + w2 * dp[c]
            zb = zbuf[miny:maxy + 1, minx:maxx + 1]
            mask &= depth < zb
            if not mask.any():
                continue

            if color_mode == "tex":
                uu = w0 * uvs[a][0] + w1 * uvs[b][0] + w2 * uvs[c][0]
                vv = w0 * uvs[a][1] + w1 * uvs[b][1] + w2 * uvs[c][1]
                tx = np.clip((uu * tex_w).astype(np.int32), 0, tex_w - 1)
                ty = np.clip((vv * tex_h).astype(np.int32), 0, tex_h - 1)
                rgba = tex[ty, tx].astype(np.float32)
                mask &= rgba[..., 3] >= 128
                if not mask.any():
                    continue
                rgb = rgba[..., :3] * face["shade"]
                region = canvas[miny:maxy + 1, minx:maxx + 1]
                region[mask, :3] = rgb[mask]
                region[mask, 3] = 255
            else:
                # 参考玩家：半透明色块，只染色不写深度
                color = np.array(face.get("color", (116, 158, 196)),
                                 dtype=np.float32) * face["shade"]
                region = canvas[miny:maxy + 1, minx:maxx + 1]
                m3 = mask[..., None]
                region[..., :3] = np.where(m3, region[..., :3] * (1 - alpha) + color * alpha,
                                           region[..., :3])
            if depth_write:
                zb[mask] = depth[mask]

    # 先画舰装（不透明），再叠玩家参考
    for f in faces:
        raster(f, "tex")
    for f in ghost_faces:
        raster(f, "ghost", depth_write=False, alpha=0.24)

    img = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), "RGBA")
    img = img.resize((size, size), Image.LANCZOS)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("geo")
    ap.add_argument("texture")
    ap.add_argument("out")
    ap.add_argument("--views", default="iso",
                    help="逗号分隔：iso,front,back,left,right,top；多视角输出 <stem>_<view>.png")
    ap.add_argument("--size", type=int, default=720, help="输出边长(像素)")
    ap.add_argument("--ss", type=int, default=3, help="超采样倍数")
    ap.add_argument("--show-player", action="store_true", help="叠加玩家上身参考")
    args = ap.parse_args()

    tex_img = Image.open(args.texture).convert("RGBA")
    tex = np.asarray(tex_img)
    geo = load_geometry(args.geo)
    faces, _ = gather_faces(geo, tex_img.size)
    if not faces:
        raise SystemExit("没有解析到任何面；检查 geo 里 cubes/uv 是否合法")

    views = [v.strip() for v in args.views.split(",") if v.strip()]
    stem, ext = os.path.splitext(args.out)
    if not ext:
        ext = ".png"
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)

    for v in views:
        if v not in VIEWS:
            raise SystemExit(f"未知视角 {v}；可选 {', '.join(VIEWS)}")
        img = render_view(faces, tex, v, size=args.size, ss=args.ss,
                          show_player=args.show_player)
        path = args.out if len(views) == 1 else f"{stem}_{v}{ext}"
        img.convert("RGB").save(path)
        print("preview:", path)


if __name__ == "__main__":
    main()
