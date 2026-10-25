# -*- coding: utf-8 -*-
"""流星（横须贺 D4Y）实体模型贴图 + 离线三视图预览。

模型几何与 RyuseiModel.java 一一对应（1 单位 = 1/16 格，机头朝 -Z，
贴图 64×64）。职责与 dev_draw_barracuda_entity.py 相同：
  1. 画 textures/entity/ryusei.png（深绿机体、白框橙芯方标、
     长条温室座舱罩、四叶深棕螺旋桨、垂尾黄色编号）
  2. 软件光栅化三视图，先核对 UV 对位

用法：
  python3 小工具/dev_draw_ryusei_entity.py            # 只写贴图
  python3 小工具/dev_draw_ryusei_entity.py --preview  # 写贴图 + 预览
"""
import math
import sys

import numpy as np
from PIL import Image

TEX = 64
OUT = "src/main/resources/assets/piranport/textures/entity/ryusei.png"

# ---- 调色板（取样自 zjsnrwiki 原画 Equip_L_71）----
GREEN_BASE = (50, 66, 62, 255)
GREEN_DARK = (34, 46, 42, 255)
GREEN_MID = (66, 84, 78, 255)
GREEN_LIGHT = (84, 102, 96, 255)
GREEN_HI = (106, 124, 116, 255)
BLACK = (22, 26, 28, 255)
ORANGE = (223, 146, 71, 255)
WHITE = (246, 249, 247, 255)
BROWN = (88, 39, 37, 255)
BROWN_DARK = (56, 25, 23, 255)
BROWN_LIGHT = (124, 58, 50, 255)
GLASS = (74, 104, 112, 255)
FRAME = (24, 32, 38, 255)
YELLOW = (224, 192, 64, 255)
SILVER = (148, 158, 160, 255)
SILVER_DARK = (92, 102, 106, 255)
DARK = (38, 44, 46, 255)
CLEAR = (0, 0, 0, 0)

# (x, y, z, w, h, d, texOffsU, texOffsV)
# y 向下：0 = 机腹底、负数向上；机头 z=-6。
BOXES = [
    # 机身
    (-1.0, -2.5, -6.0, 2.0, 2.5, 12.0, 0, 0),
    # 长条温室座舱罩
    (-1.0, -3.0, -3.0, 2.0, 0.8, 6.0, 30, 0),
    # 桨毂
    (-0.5, -2.0, -6.8, 1.0, 1.0, 1.0, 48, 0),
    # 四叶螺旋桨（0 厚度圆盘，贴图画十字桨叶、四角透明）
    (-1.5, -3.0, -6.7, 3.0, 3.0, 0.0, 48, 8),
    # 机腹滑油散热器
    (-0.5, 0.0, -5.2, 1.0, 1.0, 2.0, 56, 0),
    # 中央翼 / 左外翼 / 右外翼（0 厚度；低置）
    (-2.5, -1.0, -2.2, 5.0, 0.0, 5.0, 0, 16),
    (-6.5, -1.0, -2.0, 4.0, 0.0, 5.0, 22, 16),
    (2.5, -1.0, -2.0, 4.0, 0.0, 5.0, 42, 16),
    # 水平尾翼（机身中低位置）
    (-2.5, -1.4, 4.2, 5.0, 0.0, 2.0, 0, 22),
    # 垂尾（0 厚度）
    (0.0, -5.8, 4.5, 0.0, 3.3, 2.0, 55, 6),
    # 尾轮
    (-0.1, 0.0, 5.2, 0.2, 0.5, 0.5, 60, 40),
    # 天线杆
    (-0.05, -3.4, 3.2, 0.1, 0.9, 0.1, 60, 40),
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


def base_green(img):
    """流星是整体深绿涂装，只有很收敛的面板明暗，不做梭鱼那种迷彩斑块。"""
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), GREEN_BASE)
    for cx, cy, rx, ry, c in [
        (12, 10, 10, 7, GREEN_MID), (40, 8, 9, 6, GREEN_DARK),
        (24, 30, 12, 8, GREEN_DARK), (54, 34, 8, 7, GREEN_MID),
        (10, 48, 10, 8, GREEN_MID), (42, 52, 12, 8, GREEN_DARK),
    ]:
        blob(img, cx, cy, rx, ry, c)
    # 机翼/尾翼前缘一条受光带，读出厚度
    for box in BOXES[5:9]:
        r = face_regions(box)
        fill(img, (r["down"][0], r["down"][1] + r["down"][3] - 1,
                   r["down"][2], 1), GREEN_LIGHT)
        fill(img, (r["up"][0], r["up"][1], r["up"][2], 1), GREEN_LIGHT)


def square_mark2(img, x0, y0):
    """2×2 橙标。

    贴图分辨率只有 1 格 = 1 像素：机翼弦长 5 格、翼根到翼尖 4 格，
    2×2 已经是原画里方标的比例；再大（4×4）会盖满整块翼面。
    机身侧面只有 2.5 行高，用 1×1 才不至于把机尾糊成橙块。
    """
    for dy in range(2):
        for dx in range(2):
            img.putpixel((x0 + dx, y0 + dy), ORANGE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    base_green(img)

    # ---- 机翼：翼尖白框橙芯方标（顶/底各一对）----
    for x0, y0 in [(28, 18), (32, 17), (48, 18), (52, 17)]:
        square_mark2(img, x0, y0)
    # 中央翼前缘一小段橙黄识别线（原画近翼根部那道）
    r = face_regions(BOXES[5])
    fill(img, (r["down"][0], r["down"][1] + r["down"][3] - 1, 2, 1), ORANGE)

    # ---- 机身侧面：排气管、后段方标、尾部白条纹 ----
    # WEST 的 U 从机尾(+Z)走向机头(-Z)；EAST 反过来
    for u in (9, 10, 11):
        fill(img, (u, 12, 1, 1), DARK)       # 左侧排气管
    for u in (14, 15, 16):
        fill(img, (u, 12, 1, 1), DARK)       # 右侧排气管
    img.putpixel((3, 13), ORANGE)
    img.putpixel((23, 13), ORANGE)
    img.putpixel((0, 12), WHITE)             # 尾部白条纹（1 像素示意）
    img.putpixel((25, 12), WHITE)
    # 机头端面/尾端面
    r = face_regions(BOXES[0])
    fill(img, r["north"], GREEN_DARK)
    fill(img, r["south"], GREEN_DARK)

    # ---- 温室座舱罩：顶面玻璃 + 横向框架；侧面玻璃 + 竖向框架 ----
    r = face_regions(BOXES[1])
    fill(img, r["down"], GLASS)
    fill(img, r["up"], GLASS)
    for fy in (1, 3, 5):
        fill(img, (r["down"][0], r["down"][1] + fy, r["down"][2], 1), FRAME)
        fill(img, (r["up"][0], r["up"][1] + fy, r["up"][2], 1), FRAME)
    fill(img, (30, 6, 6, 0.8), GLASS)
    fill(img, (38, 6, 6, 0.8), GLASS)
    for fx in (30, 32, 34, 38, 40, 42):
        fill(img, (fx, 6, 1, 0.8), FRAME)
    fill(img, r["north"], FRAME)
    fill(img, r["south"], GLASS)

    # ---- 桨毂：深棕，根部压暗 ----
    r = face_regions(BOXES[2])
    for key in ("down", "up", "west", "east"):
        fill(img, r[key], BROWN)
    fill(img, r["north"], BROWN_DARK)
    fill(img, r["south"], BROWN_DARK)
    fill(img, (49, 0, 1, 1), BROWN_LIGHT)

    # ---- 四叶螺旋桨：透明底 + 十字桨叶 ----
    for x0 in (48, 51):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BROWN)
        img.putpixel((x0 + 1, 8 + 0), BROWN_DARK)
        img.putpixel((x0 + 1, 8 + 2), BROWN_DARK)

    # ---- 机腹滑油散热器：银灰罩 + 正面进气口 ----
    r = face_regions(BOXES[4])
    for key in ("down", "up", "west", "east", "south"):
        fill(img, r[key], SILVER)
    fill(img, r["north"], SILVER_DARK)

    # ---- 垂尾：前缘白线 + 黄色编号 ----
    # 垂尾只有 2 格宽，白带/黄色编号都会占掉半块垂尾；这个尺度下省略

    # ---- 尾轮 / 天线：深色 ----
    fill(img, (60, 40, 4, 4), DARK)

    return img


# ---------------------------------------------------------------- 预览

def face_quads(box):
    """返回每个面的 4 个模型坐标顶点 + 贴图矩形。"""
    x, y, z, w, h, d, _, _ = box
    x0, y0, z0 = x, y, z
    x1, y1, z1 = x + w, y + h, z + d
    r = face_regions(box)
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
                if tarr[ty, tx, 3] < 128:
                    continue  # entityCutoutNoCull：透明像素不参与渲染
                canvas[py, px] = tarr[ty, tx]

    Image.fromarray(canvas).save(path)
    print("preview:", path)


def main():
    img = draw_texture()
    img.save(OUT)
    print("wrote", OUT)
    if "--preview" in sys.argv:
        render_preview(img, "/tmp/preview_ryusei_entity.png")
        render_preview(img, "/tmp/preview_ryusei_entity_side.png",
                       eye=(-14.0, -1.0, 0.0), target=(0.0, -2.0, 0.0), span=17.0)
        render_preview(img, "/tmp/preview_ryusei_entity_top.png",
                       eye=(-2.0, 16.0, -4.0), target=(0.0, -2.0, 0.0), span=19.0)


if __name__ == "__main__":
    main()
