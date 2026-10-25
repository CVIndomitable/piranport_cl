# -*- coding: utf-8 -*-
"""Java 实体飞机模型的离线预览公共模块。

- face_regions / fill / blob：按 ModelPart.Cube 展开规则画 64×64 贴图
- render_preview：深度缓冲软件光栅化（0 厚度/0.1 厚度翼面都能正确分前后）
- save：写贴图并跑三视图

每架飞机的 dev_draw_<id>_entity.py 只定义调色板、BOXES 和 draw_texture，
然后调用 preview.save(img, BOXES, OUT, "/tmp/preview_<id>").

注意：MC 模型 y 向下 —— DOWN 面在 minY（视觉朝上），UP 面在 maxY（视觉朝下）。
"""
import math

import numpy as np
from PIL import Image

TEX = 64


def face_regions(box):
    x, y, z, w, h, d, u, v = box
    return {
        "down": (u + d, v, w, d),
        "up": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def fill(img, rect, color):
    u, v, w, h = rect
    x0, y0 = int(round(u)), int(round(v))
    x1, y1 = int(round(u + w)), int(round(v + h))
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < TEX and 0 <= y < TEX:
                img.putpixel((x, y), color)


def blob(img, cx, cy, rx, ry, color):
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            if 0 <= x < TEX and 0 <= y < TEX:
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    img.putpixel((x, y), color)


def clear(img, rect):
    fill(img, rect, (0, 0, 0, 0))


def face_quads(box):
    x, y, z, w, h, d, _, _ = box
    x0, y0, z0 = x, y, z
    x1, y1, z1 = x + w, y + h, z + d
    r = face_regions(box)
    return [
        ("down", [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], r["down"]),
        ("up", [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], r["up"]),
        ("west", [(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], r["west"]),
        ("east", [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], r["east"]),
        ("north", [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], r["north"]),
        ("south", [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)], r["south"]),
    ]


def render_preview(tex, boxes, path, eye=(-2.6, 3.4, -4.2), target=(0.0, -2.0, 0.0),
                   size=560, span=18.0):
    eye = np.array(eye, dtype=float)
    target = np.array(target, dtype=float)
    forward = target - eye
    forward /= np.linalg.norm(forward)
    world_up = np.array([0.0, 1.0, 0.0])
    right = np.cross(forward, world_up)
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)

    def project(p):
        p = np.array([p[0], -p[1], p[2]], dtype=float)
        p -= target
        return np.array([np.dot(p, right), np.dot(p, up), np.dot(p, forward)])

    tris = []
    for box in boxes:
        for name, verts, rect in face_quads(box):
            v3 = [project(v) for v in verts]
            n = np.cross(v3[1] - v3[0], v3[2] - v3[0])
            if np.linalg.norm(n) < 1e-9:
                continue
            u, v, tw, th = rect
            uvs = [(u, v), (u + tw, v), (u + tw, v + th), (u, v + th)]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                tris.append((v3[a], v3[b], v3[c], uvs[a], uvs[b], uvs[c]))

    canvas = np.zeros((size, size, 4), dtype=np.uint8)
    canvas[:, :, 3] = 255
    canvas[:, :, 0:3] = 235
    zbuf = np.full((size, size), np.inf)
    scale = size / span
    cx = cy = size / 2
    tarr = np.asarray(tex)

    def to_screen(p):
        return np.array([cx + p[0] * scale, cy - p[1] * scale])

    for a, b, c, ua, ub, uc in tris:
        sa, sb, sc = to_screen(a), to_screen(b), to_screen(c)
        minx = max(0, int(math.floor(min(sa[0], sb[0], sc[0]))))
        maxx = min(size - 1, int(math.ceil(max(sa[0], sb[0], sc[0]))))
        miny = max(0, int(math.floor(min(sa[1], sb[1], sc[1]))))
        maxy = min(size - 1, int(math.ceil(max(sa[1], sb[1], sc[1]))))
        denom = (sb[1] - sc[1]) * (sa[0] - sc[0]) + (sc[0] - sb[0]) * (sa[1] - sc[1])
        if abs(denom) < 1e-9:
            continue
        for py in range(miny, maxy + 1):
            for px in range(minx, maxx + 1):
                w0 = ((sb[1] - sc[1]) * (px - sc[0]) + (sc[0] - sb[0]) * (py - sc[1])) / denom
                w1 = ((sc[1] - sa[1]) * (px - sc[0]) + (sa[0] - sc[0]) * (py - sc[1])) / denom
                w2 = 1 - w0 - w1
                if w0 < -0.02 or w1 < -0.02 or w2 < -0.02:
                    continue
                depth = w0 * a[2] + w1 * b[2] + w2 * c[2]
                if depth >= zbuf[py, px]:
                    continue
                uu = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
                vv = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
                tx = min(TEX - 1, max(0, int(uu)))
                ty = min(TEX - 1, max(0, int(vv)))
                if tarr[ty, tx, 3] < 128:
                    continue
                zbuf[py, px] = depth
                canvas[py, px] = tarr[ty, tx]

    Image.fromarray(canvas).save(path)
    print("preview:", path)


def save(img, boxes, out, preview_base=None, span=18.0):
    img.save(out)
    print("wrote", out)
    if preview_base:
        render_preview(img, boxes, preview_base + ".png", span=span)
        render_preview(img, boxes, preview_base + "_side.png",
                       eye=(-16.0, -1.0, 0.0), target=(0.0, -2.0, 0.0), span=span)
        render_preview(img, boxes, preview_base + "_top.png",
                       eye=(-2.0, 18.0, -4.0), target=(0.0, -2.0, 0.0), span=span + 1)
