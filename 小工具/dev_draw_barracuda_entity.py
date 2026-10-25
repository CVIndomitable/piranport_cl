# -*- coding: utf-8 -*-
"""梭鱼（Barracuda）实体模型贴图 + 离线三视图预览。

模型几何与 BarracudaModel.java 一一对应（1 单位 = 1/16 格，机头朝 -Z，
贴图 64×64）。这里同时承担两个职责：
  1. 画 textures/entity/barracuda.png（绿/蓝绿迷彩、皇家海军圆徽、
     温室座舱罩、银白桨毂、机腹散热器）
  2. 用软件光栅化把同一个 box 列表渲染成 3/4 俯视图，先核对 UV 对位
     （比启动游戏快，改贴图不用反复 runClient）

用法：
  python3 小工具/dev_draw_barracuda_entity.py            # 只写贴图
  python3 小工具/dev_draw_barracuda_entity.py --preview  # 写贴图 + 预览
"""
import math
import sys

import numpy as np
from PIL import Image

TEX = 64
OUT = "src/main/resources/assets/piranport/textures/entity/barracuda.png"

# ---- 调色板（取样自 zjsnrwiki 原画 Equip_L_73）----
GREEN_MID = (74, 138, 76, 255)
GREEN_DARK = (45, 95, 50, 255)
GREEN_LIGHT = (107, 181, 103, 255)
TEAL_MID = (70, 145, 160, 255)
TEAL_DARK = (46, 108, 124, 255)
TEAL_LIGHT = (112, 192, 208, 255)
COWL = (66, 132, 148, 255)
COWL_DARK = (40, 72, 88, 255)
GLASS = (150, 200, 205, 255)
FRAME = (44, 60, 58, 255)
SILVER = (178, 190, 200, 255)
SILVER_DARK = (112, 132, 148, 255)
NAVY = (32, 52, 70, 255)
DARK = (46, 52, 56, 255)
RB = (42, 94, 228, 255)      # 圆徽蓝
RW = (248, 252, 251, 255)    # 圆徽白
RR = (241, 70, 35, 255)      # 圆徽红

# (x, y, z, w, h, d, texOffsU, texOffsV)
# y 向下：0 = 机腹底、负数向上；机头 z=-6。
BOXES = [
    # 机身
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),
    # 温室座舱罩
    (-1.0, -3.0, -2.6, 2.0, 0.8, 5.0, 30, 0),
    # 桨毂
    (-0.5, -2.0, -6.8, 1.0, 1.0, 1.0, 45, 0),
    # 机腹散热器
    (-1.0, 0.0, -5.8, 2.0, 1.0, 2.0, 45, 4),
    # 中央翼 / 左外翼 / 右外翼（0 厚度；弦长 6 给圆徽留出迷彩边）
    (-2.5, -2.3, -3.5, 5.0, 0.0, 6.0, 0, 16),
    (-7.5, -2.3, -3.3, 5.0, 0.0, 6.0, 24, 16),
    (2.5, -2.3, -3.3, 5.0, 0.0, 6.0, 44, 16),
    # 高置水平尾翼
    (-3.0, -4.9, 4.4, 6.0, 0.0, 2.0, 0, 21),
    # 垂尾（0 厚度，立在 x=0 平面）
    (0.0, -6.8, 4.4, 0.0, 4.3, 2.0, 55, 0),
    # 尾翼撑杆 ×2
    (-1.8, -4.9, 5.4, 0.4, 2.4, 0.4, 60, 40),
    (1.4, -4.9, 5.4, 0.4, 2.4, 0.4, 60, 40),
    # 排气管 ×2
    (-1.15, -1.6, -4.6, 0.3, 0.6, 1.4, 60, 40),
    (0.85, -1.6, -4.6, 0.3, 0.6, 1.4, 60, 40),
    # 天线杆
    (-0.1, -3.4, 3.4, 0.2, 0.9, 0.2, 60, 40),
]


# ---------------------------------------------------------------- 贴图

def face_regions(box):
    """按 ModelPart.Cube 的展开规则返回 6 个面的贴图矩形 (u,v,w,h)。

    注意：MC 模型 y 向下，所以名字叫 DOWN 的面在视觉上朝上。
    """
    x, y, z, w, h, d, u, v = box
    return {
        "down": (u + d, v, w, d),              # 视觉顶面
        "up": (u + d + w, v, w, d),            # 视觉底面
        "west": (u, v + d, d, h),              # x- 侧面
        "north": (u + d, v + d, w, h),         # z- 端面（机头）
        "east": (u + d + w, v + d, d, h),      # x+ 侧面
        "south": (u + d + w + d, v + d, w, h),  # z+ 端面（机尾）
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


def camo(img):
    """整张图先铺迷彩，再往具体面区域上画结构细节。"""
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), GREEN_MID)

    # 蓝绿大色块（原画机翼/机身的大片海蓝绿迷彩）
    for cx, cy, rx, ry, c in [
        (10, 7, 9, 6, TEAL_MID), (33, 6, 8, 5, TEAL_DARK),
        (52, 9, 8, 6, TEAL_MID), (21, 20, 10, 6, TEAL_LIGHT),
        (47, 24, 9, 6, TEAL_MID), (9, 34, 8, 6, TEAL_MID),
        (36, 42, 12, 8, TEAL_DARK), (57, 38, 8, 8, TEAL_MID),
        (14, 55, 10, 8, TEAL_MID), (44, 55, 10, 8, TEAL_LIGHT),
    ]:
        blob(img, cx, cy, rx, ry, c)

    # 深绿/浅绿碎块，打散大色块边界
    for cx, cy, rx, ry, c in [
        (24, 9, 6, 4, GREEN_DARK), (58, 18, 6, 5, GREEN_DARK),
        (5, 22, 6, 5, GREEN_LIGHT), (30, 31, 7, 5, GREEN_DARK),
        (49, 34, 6, 4, GREEN_LIGHT), (20, 45, 6, 5, GREEN_LIGHT),
        (56, 50, 6, 5, GREEN_DARK),
    ]:
        blob(img, cx, cy, rx, ry, c)


def roundel4(img, x0, y0):
    """4×4 圆徽：蓝环 + 白芯 + 红点。"""
    px = {
        (0, 1): RB, (0, 2): RB,
        (1, 0): RB, (1, 1): RW, (1, 2): RW, (1, 3): RB,
        (2, 0): RB, (2, 1): RW, (2, 2): RR, (2, 3): RB,
        (3, 1): RB, (3, 2): RB,
    }
    for (dx, dy), c in px.items():
        img.putpixel((x0 + dx, y0 + dy), c)


def roundel3(img, x0, y0):
    """3×3 圆徽：蓝环 + 红芯（机身侧面太窄，省掉白环）。"""
    for dy in range(3):
        for dx in range(3):
            img.putpixel((x0 + dx, y0 + dy), RR if (dx, dy) == (1, 1) else RB)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    camo(img)

    # ---- 机翼：前缘亮色带 + 翼尖圆徽（顶/底各一对）----
    # down = 视觉顶面（顶行是后缘，底行是前缘）；up = 视觉底面（顶行是前缘）
    for box in BOXES[4:7]:
        r = face_regions(box)
        # 顶面前缘 = 最底一行；底面（从下往上看）前缘 = 最顶一行
        fill(img, (r["down"][0], r["down"][1] + r["down"][3] - 1,
                   r["down"][2], 1), TEAL_LIGHT)
        fill(img, (r["up"][0], r["up"][1], r["up"][2], 1), TEAL_LIGHT)
    for x0, y0 in [(31, 17), (36, 17), (51, 17), (56, 17)]:
        roundel4(img, x0, y0)

    # ---- 机身侧面：前半段一条海蓝绿 + 后段圆徽 ----
    # WEST 的 U 从机尾(+Z)走向机头(-Z)；EAST 反过来，所以两条色带坐标不同
    fill(img, (5, 12, 4, 2.5), TEAL_MID)
    fill(img, (17, 12, 4, 2.5), TEAL_MID)
    roundel3(img, 1, 12)
    roundel3(img, 23, 12)

    # ---- 温室座舱罩：顶面玻璃 + 横向框架；侧面玻璃 + 竖向框架 ----
    r = face_regions(BOXES[1])
    fill(img, r["down"], GLASS)
    fill(img, r["up"], GLASS)
    for fy in (1, 3):
        fill(img, (r["down"][0], r["down"][1] + fy, r["down"][2], 1), FRAME)
        fill(img, (r["up"][0], r["up"][1] + fy, r["up"][2], 1), FRAME)
    for rx_ in (30, 37):
        fill(img, (rx_, 5, 5, 0.8), GLASS)
    for fx in (30, 32, 34, 37, 39, 41):
        fill(img, (fx, 5, 1, 0.8), FRAME)
    fill(img, r["north"], FRAME)
    fill(img, r["south"], GLASS)

    # ---- 桨毂：银白，根部压暗 ----
    r = face_regions(BOXES[2])
    for key in ("down", "up", "west", "north", "east"):
        fill(img, r[key], SILVER)
    fill(img, r["south"], SILVER_DARK)

    # ---- 机腹散热器：青灰罩 + 正面进气口 ----
    r = face_regions(BOXES[3])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, r[key], COWL)
    fill(img, r["north"], NAVY)
    fill(img, (45, 6, 2, 1), COWL_DARK)
    fill(img, (51, 6, 2, 1), COWL_DARK)

    # ---- 发动机罩正面 / 机身前后端面 ----
    r = face_regions(BOXES[0])
    fill(img, r["north"], COWL)
    fill(img, r["south"], GREEN_DARK)
    # 机头侧面一小段青灰罩（排气管前方）：WEST 机头在 U 大端，EAST 在 U 小端
    fill(img, (9, 12, 3, 2.5), COWL)
    fill(img, (14, 12, 3, 2.5), COWL)

    # ---- 尾翼面：垂尾加一条浅色识别带 ----
    r = face_regions(BOXES[8])
    fill(img, (r["west"][0], 4, 2, 1), TEAL_MID)
    fill(img, (r["east"][0], 4, 2, 1), TEAL_MID)

    # ---- 撑杆 / 排气管 / 天线：统一深色 ----
    fill(img, (60, 40, 4, 4), DARK)

    return img


# ---------------------------------------------------------------- 预览

def face_quads(box):
    """返回每个可见面的 4 个模型坐标顶点 + 贴图矩形。"""
    x, y, z, w, h, d, _, _ = box
    x0, y0, z0 = x, y, z
    x1, y1, z1 = x + w, y + h, z + d
    r = face_regions(box)
    # 顶点顺序与贴图矩形 (u0,v0)-(u1,v1) 的左上/右上/右下/左下对应
    return [
        ("down", [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)], r["down"]),
        ("up", [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], r["up"]),
        ("west", [(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], r["west"]),
        ("east", [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], r["east"]),
        ("north", [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], r["north"]),
        ("south", [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)], r["south"]),
    ]


def render_preview(tex, path, eye=(-2.6, 3.4, -4.2), target=(0.0, -2.0, 0.0),
                   size=560, span=18.0):
    # 机头 -Z；默认相机放在左前上方，复刻原画的 3/4 俯视
    eye = np.array(eye, dtype=float)
    target = np.array(target, dtype=float)
    forward = target - eye
    forward /= np.linalg.norm(forward)
    world_up = np.array([0.0, 1.0, 0.0])
    right = np.cross(forward, world_up)
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)

    def project(p):
        p = np.array([p[0], -p[1], p[2]], dtype=float)  # 模型 y 向下 → 世界 y 向上
        p -= target
        return np.array([np.dot(p, right), np.dot(p, up), np.dot(p, forward)])

    tris = []
    for box in BOXES:
        for name, verts, rect in face_quads(box):
            v3 = [project(v) for v in verts]
            # 0 厚度的面会有两条退化边，交给下面的三角形面积判断跳过；
            # 不做背面剔除，靠画家算法让近面覆盖远面。
            n = np.cross(v3[1] - v3[0], v3[2] - v3[0])
            if np.linalg.norm(n) < 1e-9:
                continue
            u, v, tw, th = rect
            uvs = [(u, v), (u + tw, v), (u + tw, v + th), (u, v + th)]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                tris.append((v3[a], v3[b], v3[c], uvs[a], uvs[b], uvs[c]))

    tris.sort(key=lambda t: -((t[0][2] + t[1][2] + t[2][2]) / 3.0))

    canvas = np.zeros((size, size, 4), dtype=np.uint8)
    canvas[:, :, 3] = 255
    canvas[:, :, 0:3] = 235

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
                uu = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
                vv = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
                tx = min(TEX - 1, max(0, int(uu)))
                ty = min(TEX - 1, max(0, int(vv)))
                canvas[py, px] = tarr[ty, tx]

    Image.fromarray(canvas).save(path)
    print("preview:", path)


def main():
    img = draw_texture()
    img.save(OUT)
    print("wrote", OUT)
    if "--preview" in sys.argv:
        render_preview(img, "/tmp/preview_barracuda_entity.png")
        render_preview(img, "/tmp/preview_barracuda_entity_side.png",
                       eye=(-14.0, -1.0, 0.0), target=(0.0, -2.0, 0.0), span=17.0)
        render_preview(img, "/tmp/preview_barracuda_entity_top.png",
                       eye=(-2.0, 16.0, -4.0), target=(0.0, -2.0, 0.0), span=19.0)


if __name__ == "__main__":
    main()
