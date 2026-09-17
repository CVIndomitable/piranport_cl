#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 3D 物品模型（models/item/*.json）离线渲染成预览图，用于快速检查形状 / 朝向 / 贴图 UV。

因为不想每次改模型都启动一次 Minecraft，这里复刻了 MC 的模型变换：
  - 旋转组合顺序 R = Rx·Ry·Rz（Z 先转，再 Y，最后 X）—— 与 JOML rotationXYZ 一致
    （已用 JOML 1.10.5 实测校正，见 tools/ 下同目录的说明）
  - ItemRenderer 会先 translate(-0.5,-0.5,-0.5)，即把 0..16 的中心移到原点
  - display 里的 translation 单位是 1/16 方块，与模型坐标同一尺度

用法：
  python3 tools/preview_item_model.py <模型json> <display上下文> <输出png> [贴图覆盖路径]
例：
  python3 tools/preview_item_model.py \
      src/main/resources/assets/piranport/models/item/japanese_127mm_twin_gun.json \
      gui /tmp/preview_gui.png
"""

import json
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_SIZE = 640
SUPER = 2  # 超采样倍率，抑制锯齿


def euler_matrix(rot):
    """MC(JOML) 的 rotationXYZ：先绕 Z、再绕 Y、最后绕 X。返回 3x3。"""
    rx, ry, rz = [np.radians(a) for a in rot]

    def rx_m(a):
        c, s = np.cos(a), np.sin(a)
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])

    def ry_m(a):
        c, s = np.cos(a), np.sin(a)
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])

    def rz_m(a):
        c, s = np.cos(a), np.sin(a)
        return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])

    return rx_m(rx) @ ry_m(ry) @ rz_m(rz)


# 元素的面 -> (法线, 四个角在 from/to 里的索引)
# 角的顺序：逆时针（从面外侧看）
FACE_CORNERS = {
    "north": ((0, 0, -1), [(1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)]),
    "south": ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
    "west": ((-1, 0, 0), [(0, 0, 1), (0, 0, 0), (0, 1, 0), (0, 1, 1)]),
    "east": ((1, 0, 0), [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)]),
    "up": ((0, 1, 0), [(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)]),
    "down": ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]),
}


def face_quad(frm, to, face):
    """返回该面的 4 个模型空间角点（顺序与 UV 的左上/左下/右下/右上对应）。"""
    _, idx = FACE_CORNERS[face]
    pts = []
    for i in idx:
        pts.append(np.array([frm[k] if i[k] == 0 else to[k] for k in range(3)], dtype=float))
    return pts


def uv_quad(uv):
    """uv = [u1,v1,u2,v2]（0..16）。返回与 face_quad 顺序对应的 UV 角点。"""
    u1, v1, u2, v2 = uv
    return [
        np.array([u1, v1]),
        np.array([u1, v2]),
        np.array([u2, v2]),
        np.array([u2, v1]),
    ]


def load_texture(path):
    return np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)


def sample(tex, uv):
    """uv 单位 0..16 -> 像素双线性采样。"""
    h, w = tex.shape[:2]
    x = np.clip(uv[..., 0] / 16.0 * w - 0.5, 0, w - 1)
    y = np.clip(uv[..., 1] / 16.0 * h - 0.5, 0, h - 1)
    x0 = np.floor(x).astype(int)
    y0 = np.floor(y).astype(int)
    x1 = np.clip(x0 + 1, 0, w - 1)
    y1 = np.clip(y0 + 1, 0, h - 1)
    fx = (x - x0)[..., None]
    fy = (y - y0)[..., None]
    c00 = tex[y0, x0].astype(float)
    c10 = tex[y0, x1].astype(float)
    c01 = tex[y1, x0].astype(float)
    c11 = tex[y1, x1].astype(float)
    top = c00 * (1 - fx) + c10 * fx
    bot = c01 * (1 - fx) + c11 * fx
    return (top * (1 - fy) + bot * fy).astype(np.uint8)


def solve_affine(src, dst):
    """求 2x3 仿射矩阵 A，使 dst ≈ A·[src;1]。src/dst 为 3 个点。"""
    M = np.hstack([src, np.ones((3, 1))])
    return np.linalg.solve(M, dst).T  # 2x3


def render(model_path, context, out_path, tex_override=None):
    model = json.load(open(model_path, encoding="utf-8"))
    disp = model.get("display", {}).get(context, {})
    rot = disp.get("rotation", [0, 0, 0])
    trans = np.array(disp.get("translation", [0, 0, 0]), dtype=float)
    scale = np.array(disp.get("scale", [1, 1, 1]), dtype=float)

    tex_path = tex_override
    if tex_path is None:
        tex_ref = model["textures"]["0"].split(":")[-1]
        tex_path = os.path.join(ROOT, "src/main/resources/assets/piranport/textures", tex_ref + ".png")
    tex = load_texture(tex_path)

    R = euler_matrix(rot)
    S = np.diag(scale)

    # 收集所有（可见）面
    quads = []
    for el in model["elements"]:
        frm, to = el["from"], el["to"]
        for face, fdef in el["faces"].items():
            n = np.array(FACE_CORNERS[face][0], dtype=float)
            n_view = R @ (S @ n)
            if n_view[2] <= 1e-6:
                continue  # 背面剔除（正交投影下相机沿 -Z 看）
            pts = []
            for p in face_quad(frm, to, face):
                v = R @ (S @ (p - 8.0)) + trans
                pts.append(v)
            uv = uv_quad(fdef["uv"])
            # 简易光照：跟 MC 的 gui_light=side 类似，顶面最亮、侧面次之、底面最暗
            shade = 0.74 + 0.26 * max(0.0, float(n_view[1]))
            quads.append((pts, uv, shade))

    # 计算屏幕投影范围
    allz = [p[2] for pts, _, _ in quads for p in pts]
    zmin, zmax = min(allz), max(allz)
    pts2 = [p[:2] for q in quads for p in q[0]]
    xs = [p[0] for p in pts2]
    ys = [p[1] for p in pts2]
    span = max(max(xs) - min(xs), max(ys) - min(ys), 1e-3)
    W = OUT_SIZE * SUPER
    k = W * 0.78 / span
    cx = (max(xs) + min(xs)) / 2
    cy = (max(ys) + min(ys)) / 2

    def to_screen(p):
        return np.array([W / 2 + (p[0] - cx) * k, W / 2 - (p[1] - cy) * k])

    color = np.zeros((W, W, 3), dtype=float)
    alpha = np.zeros((W, W), dtype=bool)
    zbuf = np.full((W, W), -1e9)

    for pts, uv, shade in quads:
        scr = np.array([to_screen(p) for p in pts])
        depth = (pts[0][2] + pts[1][2] + pts[2][2] + pts[3][2]) / 4.0
        # 屏幕 -> UV 的仿射（正交投影下是精确的）
        A = solve_affine(scr[:3], uv[:3])
        x0, y0 = np.floor(scr.min(axis=0)).astype(int)
        x1, y1 = np.ceil(scr.max(axis=0)).astype(int)
        x0, y0 = max(x0, 0), max(y0, 0)
        x1, y1 = min(x1, W - 1), min(y1, W - 1)
        if x1 <= x0 or y1 <= y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1), np.arange(y0, y1 + 1))
        sxy = np.stack([gx.ravel(), gy.ravel(), np.ones(gx.size)])
        uvs = A @ sxy  # 2xN
        # 重心法判断像素是否落在四边形内
        inside = point_in_quad(np.stack([gx.ravel(), gy.ravel()], axis=1), scr)
        if not inside.any():
            continue
        cols = sample(tex, uvs.T)
        rgb = np.clip(cols[:, :3].astype(float) * shade, 0, 255)
        # 用四边形重心处的深度做简单遮挡（够用：模型各面基本互不共面）
        sel = inside
        py = (gy.ravel()[sel]).astype(int)
        px = (gx.ravel()[sel]).astype(int)
        better = depth > zbuf[py, px]
        py, px = py[better], px[better]
        rgb = rgb[sel][better]
        color[py, px] = rgb
        alpha[py, px] = True
        zbuf[py, px] = depth

    out = np.zeros((W, W, 4), dtype=np.uint8)
    out[..., :3] = color.astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    img = Image.fromarray(out, "RGBA").resize((OUT_SIZE, OUT_SIZE), Image.LANCZOS)
    img.save(out_path)
    print(context, "->", out_path)


def point_in_quad(pts, quad):
    """凸四边形内外判断（叉积同号）。"""
    inside = np.ones(len(pts), dtype=bool)
    n = len(quad)
    sign = None
    for i in range(n):
        a = quad[i]
        b = quad[(i + 1) % n]
        cross = (b[0] - a[0]) * (pts[:, 1] - a[1]) - (b[1] - a[1]) * (pts[:, 0] - a[0])
        if sign is None:
            # 用四边形中心确定正方向
            c = quad.mean(axis=0)
            s = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
            sign = 1 if s > 0 else -1
        inside &= (cross * sign) >= -0.5
    return inside


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(1)
    render(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
