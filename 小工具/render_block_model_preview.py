#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""MC 方块模型离线预览渲染器（等轴测）

用途：不启动游戏就能看模型 JSON 的立体效果，快速迭代食物方块等小模型。
支持：elements 的 from/to、每面贴图（整图映射，即不写 uv 的用法）、
      rotation（单轴 origin/axis/angle）、render_type cutout 的 alpha 直通。

用法：
    python3 render_block_model_preview.py <模型.json> <输出.png> [--scale 16] [--bg 色]

注意：只画朝向相机的三个面（up / south / east），足够等轴测预览。
"""
import argparse
import json
import math
import os
import sys

import numpy as np
from PIL import Image

ISO = 0.8660254  # cos30，等轴测 x 轴压缩系数


def project(p, scale, ox, oy):
    """世界坐标(0-16) -> 屏幕像素。相机方向 (1,1,1)。"""
    x, y, z = p
    sx = (x - z) * ISO
    sy = (x + z) * 0.5 - y
    return (ox + sx * scale, oy + sy * scale)


VALID_ANGLES = (-45.0, -22.5, 0.0, 22.5, 45.0)


def check_rotation(rot, element_name):
    """原版只接受 ±22.5 的倍数；越界会让整个模型加载失败（游戏里表现为紫黑方块）。"""
    if not rot:
        return
    if float(rot["angle"]) not in VALID_ANGLES:
        raise ValueError(
            f"element '{element_name}': rotation angle {rot['angle']} 非法，"
            f"原版仅允许 {VALID_ANGLES}")


def rot_point(p, rot):
    if not rot:
        return p
    ang = math.radians(rot["angle"])
    ox, oy, oz = rot["origin"]
    dx, dy, dz = p[0] - ox, p[1] - oy, p[2] - oz
    ca, sa = math.cos(ang), math.sin(ang)
    if rot["axis"] == "x":
        dy, dz = dy * ca - dz * sa, dy * sa + dz * ca
    elif rot["axis"] == "y":
        dx, dz = dx * ca + dz * sa, -dx * sa + dz * ca
    else:
        dx, dy = dx * ca - dy * sa, dx * sa + dy * ca
    return (ox + dx, oy + dy, oz + dz)


# 每个可见面的 4 角，按贴图左上/右上/右下/左下顺序给出世界坐标的角标
# 角标含义 (ix, iy, iz)：0 = min, 1 = max
VISIBLE_FACES = {
    "up":    ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], 1.00),
    "south": ([(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)], 0.80),
    "east":  ([(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)], 0.60),
}


def persp_coeffs(src_quad, dst_quad):
    """求 PIL PERSPECTIVE 系数：把输出坐标映射回源贴图坐标。"""
    m = []
    for (X, Y), (x, y) in zip(dst_quad, src_quad):
        m.append([X, Y, 1, 0, 0, 0, -x * X, -x * Y])
        m.append([0, 0, 0, X, Y, 1, -y * X, -y * Y])
    A = np.array(m, dtype=np.float64)
    b = np.array([c for pt in src_quad for c in pt], dtype=np.float64)
    return np.linalg.solve(A, b)


def shade(img, light):
    """按光照系数压暗一张 RGBA 贴图。"""
    a = np.asarray(img, dtype=np.float32).copy()
    a[..., :3] *= light
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def render(model_path, out_path, assets_root, size=512, supersample=4,
           bg=(238, 238, 240, 255)):
    with open(model_path, "r", encoding="utf-8") as f:
        model = json.load(f)
    tex = model.get("textures", {})

    S = int(size * supersample)
    # 模型投影后约 28 单位宽 × 29 单位高，留 32 单位见方使画面收边
    scale = S / 32.0
    # 投影内容 y 中心 ≈ 1.5 单位，把该点对到画布中心
    ox, oy = S * 0.5, S * 0.5 - 1.5 * scale

    def teximg(ref):
        # ref 形如 "piranport:block/xxx"；相对 <assets>/<ns>/textures/ 解析
        ns, _, path = ref.partition(":")
        p = os.path.join(assets_root, ns, "textures", path + ".png")
        return Image.open(p).convert("RGBA")

    quads = []  # (depth, 目标四角, 贴图, 光照)
    for el in model.get("elements", []):
        fx, fy, fz = el["from"]
        tx, ty, tz = el["to"]
        rot = el.get("rotation")
        check_rotation(rot, el.get("name", "?"))
        corners = {}
        for ix in (0, 1):
            for iy in (0, 1):
                for iz in (0, 1):
                    p = (tx if ix else fx, ty if iy else fy, tz if iz else fz)
                    corners[(ix, iy, iz)] = rot_point(p, rot)
        for fname, (idx_list, light) in VISIBLE_FACES.items():
            face = el.get("faces", {}).get(fname)
            if not face:
                continue
            tref = face.get("texture", "")
            if not tref.startswith("#"):
                continue
            ref = tex.get(tref[1:])
            if not ref:
                continue
            pts = [corners[i] for i in idx_list]
            depth = sum((q[0] + q[1] + q[2]) / 3.0 for q in pts)
            quads.append((depth, pts, ref, light))

    quads.sort(key=lambda q: q[0])  # 远的先画

    canvas = Image.new("RGBA", (S, S), bg)
    for depth, pts, ref, light in quads:
        img = teximg(ref)
        tw, th = img.size
        src_quad = [(0, 0), (tw, 0), (tw, th), (0, th)]
        dst_quad = [project(p, scale, ox, oy) for p in pts]
        coeffs = persp_coeffs(src_quad, dst_quad)
        warped = img.transform((S, S), Image.PERSPECTIVE, coeffs, Image.BICUBIC)
        warped = shade(warped, light)
        canvas.alpha_composite(warped)

    canvas = canvas.resize((int(S / supersample), int(S / supersample)), Image.LANCZOS)
    canvas.convert("RGB").save(out_path)
    print("saved", out_path)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("model")
    ap.add_argument("out")
    ap.add_argument("--size", type=int, default=512, help="输出边长(像素)")
    ap.add_argument("--ss", type=int, default=4, help="超采样倍数")
    ap.add_argument("--assets-root", default="src/main/resources/assets",
                    help="assets 根目录（用于解析 piranport:block/xxx 贴图）")
    args = ap.parse_args()
    render(args.model, args.out, os.path.abspath(args.assets_root),
           size=args.size, supersample=args.ss)
