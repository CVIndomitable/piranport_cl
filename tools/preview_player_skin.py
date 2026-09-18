#!/usr/bin/env python3
"""玩家皮肤 3D 预览器：按 Minecraft PlayerModel（classic / 4px 手臂）的
真实骨骼与 UV 展开，把 64×64 皮肤渲染成「正面 / 背面 / 侧面」三视图。

不启动游戏就能检查皮肤在模型上的实际观感，尤其是：
  - 头顶 (UP) / 后脑 (BACK) 面片的接缝
  - overlay 层（帽子层 / 衣服层）是否正确贴合
  - 肢体正面贴脸的像素是否落在正确的一侧（UV 镜像问题）

渲染方式：正交投影 + z-buffer 逐面片光栅化，每面乘原版亮度系数
（顶 1.0 / 南北 0.8 / 东西 0.6 / 底 0.5），并做超采样抗锯齿。

用法：
    python3 tools/preview_player_skin.py <皮肤png> [输出png]

Author: PiranPort Dev Team
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SUPER = 4          # 超采样倍率
TEXEL = 0.5        # 1 texel = 0.5 模型单位（玩家模型高 32 texel * 0.5 = 16 单位高? 见下）
# 说明：MC 玩家模型是 64×64 皮肤贴到「像素单位」的立方体上。
# 头 8×8×8、躯干 8×12×4、四肢 4×12×4（单位 = 皮肤 texel）。
# 这里直接用 texel 作为世界单位，最后统一缩放。


# ---------------------------------------------------------------- 几何

class Box:
    """一个立方体部件：origin 是最小角坐标（texel 单位，y 向上），size 是尺寸。

    `base` 是不含膨胀的原始尺寸 —— UV 展开必须用 base，因为原版
    ModelPart 的 overlay 层虽然几何上膨胀了，贴图 UV 仍然是按原始尺寸展开的。
    """

    def __init__(self, name, ox, oy, oz, sx, sy, sz, uv, inflate=0.0):
        self.name = name
        self.base = (sx, sy, sz)
        self.ox, self.oy, self.oz = ox - inflate, oy - inflate, oz - inflate
        self.sx, self.sy, self.sz = sx + 2 * inflate, sy + 2 * inflate, sz + 2 * inflate
        self.uv = uv

    def corners(self):
        x0, y0, z0 = self.ox, self.oy, self.oz
        x1, y1, z1 = x0 + self.sx, y0 + self.sy, z0 + self.sz
        return x0, y0, z0, x1, y1, z1


def texel_rect(box_uv, face):
    """按原版 ModelPart 的 UV 布局，给出某个面的 texel 矩形 (u, v, w, h)。"""
    u, v = box_uv
    w, h, d = None, None, None
    return u, v, face


def face_uv(u, v, w, h, d, face):
    """原版 Box UV 展开（照抄 ModelPart.Cube 的构造函数）。

    参数 (u,v) 是展开图左上角，w/h/d 是盒子尺寸。
    返回 {face: (x, y, width, height)}，坐标系是「贴图左上角为原点」。
    """
    return {
        # DOWN (y-) / UP (y+) 并排在展开图顶部
        "down": (u + d,          v,      w, d),
        "up":   (u + d + w,      v,      w, d),
        # 四个侧面在第二行
        "east":  (u,             v + d,  d, h),   # +x
        "north": (u + d,         v + d,  w, h),   # -z
        "west":  (u + d + w,     v + d,  d, h),   # -x
        "south": (u + d + w + d, v + d,  w, h),   # +z
    }[face]


# 面部 → 该面四个角的 3D 坐标顺序，必须与贴图上的顺时针顺序一致。
# 顶点顺序：从贴图 (u,v) 左上角开始，依次 左上 → 左下 → 右下 → 右上（tile 内）。
FACE_CORNERS = {
    # north (-z)：面朝观察者的正面。贴图 tile 内 x 向右 = 世界 +x? 不对，
    # 原版 north 面在贴图上 x 向右对应世界的 -x（从正面看角色，其左手在观察者右侧）。
    "north": lambda x0, y0, z0, x1, y1, z1: [
        (x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
    # south (+z)：背面
    "south": lambda x0, y0, z0, x1, y1, z1: [
        (x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
    # east (+x)
    "east": lambda x0, y0, z0, x1, y1, z1: [
        (x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)],
    # west (-x)
    "west": lambda x0, y0, z0, x1, y1, z1: [
        (x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
    # up (+y)
    "up": lambda x0, y0, z0, x1, y1, z1: [
        (x0, y1, z1), (x0, y1, z0), (x1, y1, z0), (x1, y1, z1)],
    # down (-y)
    "down": lambda x0, y0, z0, x1, y1, z1: [
        (x0, y0, z0), (x0, y0, z1), (x1, y0, z1), (x1, y0, z0)],
}

FACE_SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8,
              "east": 0.6, "west": 0.6}
FACE_NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1),
               "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}


def make_parts():
    """构建经典（Steve）玩家模型的全部部件，坐标 y 向上、-z 为正面朝向。

    overlay 层（hat/jacket/sleeve/pant）原版是 0.25 膨胀的独立模型，
    在原版里靠 `zOffset` 做轻微外推以避免 z-fighting；这里统一用 0.4 膨胀，
    并把它们排到基础层之后渲染，靠 z-buffer 自然覆盖。
    """
    P = []
    INF = 0.4
    # 头：8×8×8，中心在 x,z 原点，y 从 24 到 32
    P.append(Box("head", -4, 24, -4, 8, 8, 8, (0, 0)))
    P.append(Box("hat", -4, 24, -4, 8, 8, 8, (32, 0), inflate=INF))
    # 躯干：8×12×4，y 从 12 到 24
    P.append(Box("body", -4, 12, -2, 8, 12, 4, (16, 16)))
    P.append(Box("jacket", -4, 12, -2, 8, 12, 4, (16, 32), inflate=INF))
    # 右臂（角色右手 = 世界 -x 侧）：4×12×4，y 12..24
    P.append(Box("right_arm", -8, 12, -2, 4, 12, 4, (40, 16)))
    P.append(Box("right_sleeve", -8, 12, -2, 4, 12, 4, (40, 32), inflate=INF))
    # 左臂（+x 侧）
    P.append(Box("left_arm", 4, 12, -2, 4, 12, 4, (32, 48)))
    P.append(Box("left_sleeve", 4, 12, -2, 4, 12, 4, (48, 48), inflate=INF))
    # 右腿（-x 侧）：4×12×4，y 0..12
    P.append(Box("right_leg", -4, 0, -2, 4, 12, 4, (0, 16)))
    P.append(Box("right_pant", -4, 0, -2, 4, 12, 4, (0, 32), inflate=INF))
    # 左腿（+x 侧）
    P.append(Box("left_leg", 0, 0, -2, 4, 12, 4, (16, 48)))
    P.append(Box("left_pant", 0, 0, -2, 4, 12, 4, (0, 48), inflate=INF))
    return P


# ---------------------------------------------------------------- 相机

def rotate(p, yaw, pitch):
    x, y, z = p
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    x, z = x * cy + z * sy, -x * sy + z * cy
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def project(p, scale, cx, cy):
    """正交投影：相机沿 +z 看，屏幕 y 向下。"""
    return cx + p[0] * scale, cy - p[1] * scale, p[2]


# ---------------------------------------------------------------- 渲染

def render(skin, yaw, pitch, out_size, scale=1.0):
    """渲染一个视角。返回 RGBA 图。"""
    W = H = out_size
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    buf = [(0.0, 0.0, 0.0, 0.0)] * (W * H)
    zbuf = [-1e18] * (W * H)

    # 模型 y 范围 0..32、x 范围 -8..8。留 12% 边距后按高度铺满。
    sx = W / 40.0 * scale
    cx, cy = W / 2.0, H / 2.0 + 16.0 * sx    # y=16 是模型竖直中心

    parts = make_parts()
    for box in parts:
        x0, y0, z0, x1, y1, z1 = box.corners()
        # UV 展开用「未膨胀」的原始尺寸（与原版 ModelPart 一致）
        uw, uh, ud = box.base
        u, v = box.uv
        for face in FACE_UV_KEYS:
            fuv = face_uv(u, v, uw, uh, ud, face)
            corners = FACE_CORNERS[face](x0, y0, z0, x1, y1, z1)
            shade = FACE_SHADE[face]
            rasterize_face(buf, zbuf, W, H, skin, fuv, corners,
                           yaw, pitch, sx, cx, cy, shade)
    img.putdata([_clip(c) for c in buf])
    # 汉考克整套配色是「黑披风 + 黑裙 + 黑丝袜 + 黑手套」，暗部连成一片，
    # 纯色渲染会看不出四肢轮廓。这里给 alpha 边缘描一圈浅灰，便于分辨部件。
    return outline(img)


def outline(img, color=(120, 124, 138, 255)):
    """给不透明区域的外轮廓描边，让深色部件之间能看出分界。"""
    W, H = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    for y in range(H):
        for x in range(W):
            if src[x, y][3] != 0:
                continue
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if 0 <= nx < W and 0 <= ny < H and src[nx, ny][3] != 0:
                    dst[x, y] = color
                    break
    return out


FACE_UV_KEYS = ["down", "up", "east", "north", "west", "south"]


def _clip(c):
    return tuple(min(255, max(0, int(round(v)))) for v in c)


def rasterize_face(buf, zbuf, W, H, skin, fuv, corners3d, yaw, pitch,
                   scale, cx, cy, shade):
    """把一个面片光栅化进缓冲区。"""
    fu, fv, fw, fh = fuv
    if fw <= 0 or fh <= 0:
        return
    # 四个角的屏幕坐标
    proj = []
    for c in corners3d:
        r = rotate(c, yaw, pitch)
        px_, py_, pz = project(r, scale, cx, cy)
        proj.append((px_, py_, pz))

    # 用包围盒遍历像素
    minx = max(0, int(math.floor(min(p[0] for p in proj))))
    maxx = min(W - 1, int(math.ceil(max(p[0] for p in proj))))
    miny = max(0, int(math.floor(min(p[1] for p in proj))))
    maxy = min(H - 1, int(math.ceil(max(p[1] for p in proj))))
    if minx > maxx or miny > maxy:
        return

    # 面片在屏幕上可能退化成线段；用重心坐标做仿射插值
    a, b, c, d = proj
    for py in range(miny, maxy + 1):
        for px_ in range(minx, maxx + 1):
            s = px_ + 0.5
            t = py + 0.5
            # 四边形 (a,b,c,d) 是平行四边形（正交投影保持平行四边形）
            # 用 a->b 和 a->d 两个基向量解 (alpha, beta)
            bx, by, _ = b[0] - a[0], b[1] - a[1], 0
            dx, dy, _ = d[0] - a[0], d[1] - a[1], 0
            det = bx * dy - by * dx
            if abs(det) < 1e-9:
                continue
            ex, ey = s - a[0], t - a[1]
            alpha = (ex * dy - ey * dx) / det
            beta = (bx * ey - by * ex) / det
            # 留一点边缘容差，避免相邻面片之间出现 1px 接缝
            eps = 0.004
            if not (-eps <= alpha <= 1 + eps and -eps <= beta <= 1 + eps):
                continue
            alpha = min(1.0, max(0.0, alpha))
            beta = min(1.0, max(0.0, beta))
            wz = a[2] + alpha * (b[2] - a[2]) + beta * (d[2] - a[2])
            idx = py * W + px_
            if wz <= zbuf[idx]:
                continue
            # 贴图坐标：alpha 沿贴图的宽度方向（tile 内水平），beta 沿高度
            tu = fu + alpha * fw
            tv = fv + beta * fh
            tx = min(skin.width - 1, max(0, int(tu)))
            ty = min(skin.height - 1, max(0, int(tv)))
            r, g, bl, al = skin.getpixel((tx, ty))
            if al == 0:
                continue
            zbuf[idx] = wz
            fr, fg, fb, fa = buf[idx]
            na = al / 255.0
            buf[idx] = (r * shade * na + fr * (1 - na),
                        g * shade * na + fg * (1 - na),
                        bl * shade * na + fb * (1 - na),
                        max(fa, al))


# ---------------------------------------------------------------- 主流程

def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else \
        ROOT / "src/main/resources/assets/piranport/textures/skin/skin_20.png"
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else \
        ROOT / "build/offline-renders/skin_3d.png"
    skin = Image.open(src).convert("RGBA")
    if skin.size != (64, 64):
        raise SystemExit(f"需要 64×64 皮肤，实际 {skin.size}")

    views = [("front", 180, 0), ("back", 0, 0), ("left", 270, 0),
             ("right", 90, 0), ("iso", 150, 12)]
    S = 260
    sheet = Image.new("RGBA", (S * len(views), S + 20), (36, 36, 46, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(sheet)
    for i, (name, yaw, pitch) in enumerate(views):
        v = render(skin, yaw, pitch, S)
        sheet.alpha_composite(v, (i * S, 0))
        d.text((i * S + 6, S + 3), f"{name} (yaw={yaw})", fill=(220, 220, 230, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(f"已生成 {out}")


if __name__ == "__main__":
    main()
