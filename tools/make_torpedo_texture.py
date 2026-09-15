#!/usr/bin/env python3
"""生成鱼雷实体贴图（torpedo.png）+ 离线模型预览图。

实体模型见 src/main/java/com/piranport/client/model/TorpedoModel.java，
本脚本的 BOXES 表是贴图 UV 布局的唯一来源，Java 侧的 texOffs 必须与之一致。

轴向约定（与 Java 模型一致）：雷头朝 -Z，雷尾螺旋桨朝 +Z，原点在雷体正中，
单位是 MC 模型的像素（1px = 1/16 格）。

  - 64x64 画布，背景全透明，无抗锯齿（alpha 只有 0 和 255）
  - 每个盒子的 UV 区域按 MC 标准展开（顶/底/西/北/东/南 六面）
  - 顶面平涂最亮、底面平涂最暗，四个侧面自上而下做亮→暗渐变，
    这样方盒子拼出来的雷体看起来像一根受顶光的圆柱

    python3 tools/make_torpedo_texture.py
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TEXTURE_OUT = ROOT / "src/main/resources/assets/piranport/textures/entity/torpedo.png"
PREVIEW_OUT = ROOT / "build/offline-renders/torpedo_preview.png"

TEX_W = TEX_H = 64

# 顶光因子：>=1 提亮，<=1 压暗
SHADE_UP = 1.28
SHADE_DOWN = 0.55
GRAD_TOP = 1.28
GRAD_BOTTOM = 0.55

# 取色对齐现有 533mm 鱼雷物品贴图（铜色雷头 + 钢质雷体）
PALETTE = {
    "copper": (171, 67, 49),
    "steel": (124, 124, 124),
    "steel_dark": (94, 94, 94),
    "steel_light": (167, 167, 167),
}

# name, uv(u,v), size(w,h,d), 模型坐标(x,y,z 为最小角), 配色
BOXES = [
    ("noseA",    (20, 11), (2, 2, 1),  (-1.0, -1.0, -10.0), "copper"),
    ("noseB",    (11, 11), (3, 3, 1),  (-1.5, -1.5, -9.0),  "copper"),
    ("noseC",    (0, 11),  (4, 4, 1),  (-2.0, -2.0, -8.0),  "copper"),
    ("warhead",  (21, 0),  (4, 4, 4),  (-2.0, -2.0, -7.0),  "copper"),
    ("midbody",  (0, 0),   (4, 4, 6),  (-2.0, -2.0, -3.0),  "steel"),
    ("afterbody", (38, 0), (4, 4, 4),  (-2.0, -2.0, 3.0),   "steel_dark"),
    ("tailCone", (27, 11), (3, 3, 2),  (-1.5, -1.5, 7.0),   "steel_dark"),
    ("tailEnd",  (38, 11), (2, 2, 1),  (-1.0, -1.0, 9.0),   "steel"),
    ("finUp",    (45, 11), (1, 2, 3),  (-0.5, 1.5, 5.0),    "steel_light"),
    ("finDown",  (45, 11), (1, 2, 3),  (-0.5, -3.5, 5.0),   "steel_light"),
    ("finLeft",  (54, 11), (2, 1, 3),  (-3.5, -0.5, 5.0),   "steel_light"),
    ("finRight", (54, 11), (2, 1, 3),  (1.5, -0.5, 5.0),    "steel_light"),
    ("hub",      (0, 17),  (1, 1, 1),  (-0.5, -0.5, 10.0),  "steel_light"),
    ("bladeH",   (5, 17),  (5, 1, 1),  (-2.5, -0.5, 10.0),  "steel_light"),
    ("bladeV",   (18, 17), (1, 5, 1),  (-0.5, -2.5, 10.0),  "steel_light"),
]


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, round(channel * factor))) for channel in color) + (255,)


def gradient(color: tuple[int, int, int], rows: int) -> list[tuple[int, int, int, int]]:
    """自上而下的亮→暗渐变；只有一行时取中间调。"""
    if rows <= 1:
        return [shade(color, 1.0)]
    return [shade(color, GRAD_TOP + (GRAD_BOTTOM - GRAD_TOP) * i / (rows - 1))
            for i in range(rows)]


def face_rects(u: int, v: int, w: int, h: int, d: int) -> dict[str, tuple[int, int, int, int]]:
    """MC 标准盒子 UV 展开：上排 顶/底，下排 西/北/东/南。"""
    return {
        "up":    (u + d, v, w, d),
        "down":  (u + d + w, v, w, d),
        "west":  (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east":  (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def paint_box(px: dict[tuple[int, int], tuple[int, int, int, int]], u: int, v: int,
              w: int, h: int, d: int, color: tuple[int, int, int]) -> None:
    for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
        if face == "up":
            fill = [shade(color, SHADE_UP)] * fh
        elif face == "down":
            fill = [shade(color, SHADE_DOWN)] * fh
        else:
            fill = gradient(color, fh)
        for row in range(fh):
            for col in range(fw):
                px[(fx + col, fy + row)] = fill[row]


def create_texture() -> Image.Image:
    px: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for name, (u, v), (w, h, d), _, palette in BOXES:
        paint_box(px, u, v, w, h, d, PALETTE[palette])
        assert u + 2 * (w + d) <= TEX_W and v + d + h <= TEX_H, f"{name} 超出贴图范围"
    image = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    for (x, y), color in px.items():
        image.putpixel((x, y), color)
    return image


# ==================== 离线预览（仅用于人工核对形状与 UV） ====================

# MC 的实体模型不吃方向光，明暗全部来自贴图本身，所以预览不再叠一层面光照，
# 直接取该面 UV 的平均色——这样预览能如实反映 UV 有没有对错面。
FACE_VERTS = (
    ("up",    (3, 2, 6, 7)),      # +Y
    ("down",  (0, 1, 5, 4)),      # -Y
    ("west",  (0, 3, 7, 4)),      # -X
    ("east",  (1, 2, 6, 5)),      # +X
    ("north", (0, 1, 2, 3)),      # -Z
    ("south", (4, 5, 6, 7)),      # +Z
)


def face_color(texture: Image.Image, u: int, v: int, w: int, h: int, d: int,
               face: str) -> tuple[int, int, int, int]:
    fx, fy, fw, fh = face_rects(u, v, w, h, d)[face]
    total = [0, 0, 0]
    count = 0
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            pixel = texture.getpixel((x, y))
            if pixel[3] == 0:
                continue
            total = [total[i] + pixel[i] for i in range(3)]
            count += 1
    if count == 0:
        return (255, 0, 255, 255)
    return tuple(round(channel / count) for channel in total) + (255,)


def project(point: tuple[float, float, float], yaw: float, pitch: float, scale: float,
            cx: float, cy: float) -> tuple[float, float, float]:
    x, y, z = point
    yaw_r, pitch_r = math.radians(yaw), math.radians(pitch)
    xr = math.cos(yaw_r) * x + math.sin(yaw_r) * z
    zr = -math.sin(yaw_r) * x + math.cos(yaw_r) * z
    yr = math.cos(pitch_r) * y - math.sin(pitch_r) * zr
    depth = math.sin(pitch_r) * y + math.cos(pitch_r) * zr
    return cx + xr * scale, cy - yr * scale, depth


def render_view(texture: Image.Image, yaw: float, pitch: float,
                size: tuple[int, int]) -> Image.Image:
    supersample = 3
    width, height = size
    out = Image.new("RGBA", (width * supersample, height * supersample), (238, 241, 244, 255))
    draw = ImageDraw.Draw(out, "RGBA")
    scale = min(width / 26.0, height / 26.0) * supersample
    cx, cy = width * supersample / 2, height * supersample / 2

    polygons = []
    for _, (u, v), (w, h, d), (bx, by, bz), _ in BOXES:
        corners = (
            (bx, by, bz), (bx + w, by, bz), (bx + w, by + h, bz), (bx, by + h, bz),
            (bx, by, bz + d), (bx + w, by, bz + d), (bx + w, by + h, bz + d),
            (bx, by + h, bz + d),
        )
        verts = [project(p, yaw, pitch, scale, cx, cy) for p in corners]
        for face, quad in FACE_VERTS:
            color = face_color(texture, u, v, w, h, d, face)[:3]
            depth = sum(verts[i][2] for i in quad) / 4
            polygons.append((depth, [verts[i][:2] for i in quad], color))
    for _, points, color in sorted(polygons, key=lambda item: item[0], reverse=True):
        draw.polygon(points, fill=color)
    return out.resize(size, Image.Resampling.LANCZOS)


def write_preview(texture: Image.Image) -> None:
    PREVIEW_OUT.parent.mkdir(parents=True, exist_ok=True)
    panel = 380
    canvas = Image.new("RGBA", (panel * 4 + 40, panel + 400), (238, 241, 244, 255))
    draw = ImageDraw.Draw(canvas, "RGBA")
    draw.text((16, 10), "TORPEDO - OFFLINE PREVIEW (no animation, painter's algorithm)",
              fill=(31, 42, 54, 255))
    views = (("side", 90.0, 0.0, 10), ("front 3/4", 128.0, 14.0, panel + 10),
             ("top", 90.0, 82.0, panel * 2 + 10), ("nose", 200.0, 8.0, panel * 3 + 10))
    for name, yaw, pitch, x in views:
        canvas.alpha_composite(render_view(texture, yaw, pitch, (panel - 20, panel - 20)), (x, 44))
        draw.text((x + 8, panel + 20), name, fill=(60, 72, 84, 255))
    canvas.alpha_composite(
        texture.resize((TEX_W * 4, TEX_H * 4), Image.Resampling.NEAREST), (16, panel + 46))
    canvas.save(PREVIEW_OUT)


def main() -> None:
    texture = create_texture()
    TEXTURE_OUT.parent.mkdir(parents=True, exist_ok=True)
    texture.save(TEXTURE_OUT)
    write_preview(texture)
    for name, (u, v), (w, h, d), _, _ in BOXES:
        print(f"{name:10s} texOffs({u:2d}, {v:2d})  size {w}x{h}x{d}"
              f"  rect {2 * (w + d)}x{d + h}")
    print(TEXTURE_OUT)
    print(PREVIEW_OUT)


if __name__ == "__main__":
    main()
