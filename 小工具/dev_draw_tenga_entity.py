# -*- coding: utf-8 -*-
"""天河（双发陆攻）实体模型贴图 + 离线三视图预览。

模型几何与 TengaModel.java 一一对应（1 单位 = 1/16 格，机头朝 -Z，
贴图 64×64）。职责与 dev_draw_barracuda_entity.py 相同：
  1. 画 textures/entity/tenga.png（深绿机背、浅灰蓝机腹、双发动机短舱、
     黄色翼根前缘识别带、白框橙芯方标、玻璃机鼻）
  2. 软件光栅化三视图，先核对 UV 对位

用法：
  python3 小工具/dev_draw_tenga_entity.py            # 只写贴图
  python3 小工具/dev_draw_tenga_entity.py --preview  # 写贴图 + 预览
"""
import math
import sys

import numpy as np
from PIL import Image

TEX = 64
OUT = "src/main/resources/assets/piranport/textures/entity/tenga.png"

# ---- 调色板（取样自 zjsnrwiki 原画 Equip_L_238）----
GREEN_BASE = (88, 116, 100, 255)
GREEN_DARK = (54, 74, 62, 255)
GREEN_MID = (102, 130, 114, 255)
GREEN_LIGHT = (122, 150, 132, 255)
GREEN_HI = (146, 174, 156, 255)
PALE = (146, 162, 174, 255)          # 机腹/翼下浅灰蓝
PALE_DARK = (116, 132, 144, 255)
YELLOW = (240, 240, 64, 255)         # 翼根前缘识别带
WHITE = (248, 244, 238, 255)
ORANGE = (223, 146, 71, 255)
GLASS = (150, 185, 220, 255)
GLASS_DARK = (108, 142, 178, 255)
FRAME = (45, 58, 66, 255)
DARK = (40, 46, 44, 255)
BLACK = (24, 27, 27, 255)
SILVER = (170, 180, 186, 255)
CLEAR = (0, 0, 0, 0)

# (x, y, z, w, h, d, texOffsU, texOffsV)
# y 向下：0 = 机腹底、负数向上；机头 z=-7。
BOXES = [
    # 机身（双发陆攻，比单发机长一截）
    (-1.0, -2.6, -7.0, 2.0, 2.6, 14.0, 0, 0),
    # 玻璃机鼻
    (-1.0, -2.2, -7.8, 2.0, 2.0, 1.0, 48, 0),
    # 长条座舱罩
    (-1.0, -3.3, -6.4, 2.0, 0.7, 5.0, 32, 0),
    # 左/右发动机短舱（共用 UV）
    (-3.5, -2.2, -4.2, 2.0, 2.0, 4.0, 32, 8),
    (1.5, -2.2, -4.2, 2.0, 2.0, 4.0, 32, 8),
    # 左/右四叶螺旋桨（0 厚度圆盘，透明角）
    (-4.0, -2.7, -4.4, 3.0, 3.0, 0.0, 44, 8),
    (1.0, -2.7, -4.4, 3.0, 3.0, 0.0, 50, 8),
    # 左/右桨毂
    (-3.0, -1.7, -4.6, 1.0, 1.0, 1.0, 56, 8),
    (2.0, -1.7, -4.6, 1.0, 1.0, 1.0, 56, 8),
    # 中央翼 / 左外翼 / 右外翼（0 厚度；低置；两侧外翼共用 UV）
    (-3.0, -1.2, -3.6, 6.0, 0.1, 6.0, 0, 17),
    (-9.0, -1.2, -3.4, 6.0, 0.1, 6.0, 26, 17),
    (3.0, -1.2, -3.4, 6.0, 0.1, 6.0, 26, 26),
    # 水平尾翼
    (-3.0, -1.5, 5.2, 6.0, 0.1, 2.0, 0, 24),
    # 垂尾（0 厚度）
    (0.0, -5.6, 5.2, 0.0, 3.0, 2.0, 57, 0),
    # 尾轮
    (-0.1, 0.0, 6.0, 0.2, 0.4, 0.4, 60, 40),
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
    """机背深绿 + 很收敛的面板明暗；机身下表面随后改刷浅灰蓝。"""
    for y in range(TEX):
        for x in range(TEX):
            img.putpixel((x, y), GREEN_BASE)
    for cx, cy, rx, ry, c in [
        (10, 8, 10, 6, GREEN_MID), (30, 6, 9, 5, GREEN_DARK),
        (44, 12, 8, 6, GREEN_MID), (16, 20, 9, 4, GREEN_DARK),
        (34, 24, 8, 4, GREEN_LIGHT), (52, 20, 8, 5, GREEN_MID),
    ]:
        blob(img, cx, cy, rx, ry, c)


def square_mark4(img, x0, y0):
    """4×4 白框橙芯方标（原画翼尖识别标）。"""
    for dy in range(4):
        for dx in range(4):
            img.putpixel((x0 + dx, y0 + dy),
                         ORANGE if 1 <= dx <= 2 and 1 <= dy <= 2 else WHITE)


def square_mark2(img, x0, y0):
    """2×2 橙标。

    1 像素 = 1 模型单位：外翼面 6×6、机身侧面 2.6 高，2×2 才是原画
    方标占弦长 1/3 的比例；4×4 会白花花盖掉大半翼面。
    """
    for dy in range(2):
        for dx in range(2):
            img.putpixel((x0 + dx, y0 + dy), ORANGE)


def draw_texture():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    base_green(img)

    # ---- 机身：机腹、机腹侧边转浅灰蓝 ----
    rf = face_regions(BOXES[0])
    fill(img, rf["up"], PALE)
    fill(img, (0, 16, 14, 1), PALE)          # WEST 下缘
    fill(img, (16, 16, 14, 1), PALE)         # EAST 下缘
    fill(img, rf["north"], GREEN_DARK)       # 机头端面（被玻璃机鼻挡住）
    fill(img, rf["south"], GREEN_DARK)

    # ---- 主机翼：上表面深绿，下表面浅灰蓝 ----
    for box in BOXES[9:12]:
        r = face_regions(box)
        fill(img, r["up"], PALE)
        # 前缘黄色识别带：DOWN 面的最底行是前缘
        fill(img, (r["down"][0], r["down"][1] + r["down"][3] - 1,
                   r["down"][2], 1), YELLOW)
    # 外翼前缘只在靠近翼根的一半刷黄（原画识别带长度）。
    # 左右外翼各有独立 UV：左翼 U 从翼尖(32)到翼根(38)，右翼反过来。
    rl = face_regions(BOXES[10])
    rr = face_regions(BOXES[11])
    fill(img, (rl["down"][0], rl["down"][1] + rl["down"][3] - 1, 3, 1), GREEN_LIGHT)
    fill(img, (rr["down"][0] + 3, rr["down"][1] + rr["down"][3] - 1, 3, 1), GREEN_LIGHT)
    # 左右外翼上方的橙标
    square_mark2(img, 34, 19)
    square_mark2(img, 34, 28)

    # ---- 发动机短舱：顶面绿、底面浅灰蓝、前缘进气口深色 ----
    rn = face_regions(BOXES[3])
    fill(img, rn["up"], PALE)
    fill(img, rn["north"], DARK)
    fill(img, rn["south"], DARK)
    fill(img, (rn["down"][0], rn["down"][1], 1, rn["down"][3]), YELLOW)
    # 短舱侧面一条浅色面板线，读出圆筒
    fill(img, (rn["west"][0], rn["west"][1] + 1, rn["west"][2], 1), GREEN_DARK)
    fill(img, (rn["east"][0], rn["east"][1] + 1, rn["east"][2], 1), GREEN_DARK)

    # ---- 水平尾翼：下表面浅灰蓝 ----
    rt = face_regions(BOXES[12])
    fill(img, rt["up"], PALE)

    # ---- 垂尾：前缘受光 ----
    rf2 = face_regions(BOXES[13])
    fill(img, (rf2["west"][0] + 1, rf2["west"][1], 1, rf2["west"][3]), GREEN_LIGHT)
    fill(img, (rf2["east"][0] + 1, rf2["east"][1], 1, rf2["east"][3]), GREEN_LIGHT)

    # ---- 机身侧面：后段白框橙芯方标 + 舷窗 + 尾部白条纹 ----
    # WEST 的 U 从机尾(+Z)走向机头(-Z)；EAST 反过来
    square_mark2(img, 2, 14)
    square_mark2(img, 28, 14)
    img.putpixel((5, 14), GLASS)
    img.putpixel((6, 14), GLASS)
    img.putpixel((23, 14), GLASS)
    img.putpixel((24, 14), GLASS)

    # ---- 玻璃机鼻 ----
    rno = face_regions(BOXES[1])
    for key in ("down", "west", "east", "north", "south"):
        fill(img, rno[key], GLASS)
    fill(img, rno["up"], DARK)               # 机鼻下巴
    fill(img, (rno["north"][0], rno["north"][1], rno["north"][2], 1), FRAME)
    fill(img, (rno["north"][0], rno["north"][1] + 1, 1, 1), FRAME)

    # ---- 长条座舱罩 ----
    rc = face_regions(BOXES[2])
    fill(img, rc["down"], GLASS)
    fill(img, rc["up"], GLASS_DARK)
    for fy in (1, 3):
        fill(img, (rc["down"][0], rc["down"][1] + fy, rc["down"][2], 1), FRAME)
        fill(img, (rc["up"][0], rc["up"][1] + fy, rc["up"][2], 1), FRAME)
    fill(img, (32, 5, 5, 0.7), GLASS)
    fill(img, (39, 5, 5, 0.7), GLASS)
    for fx in (32, 34, 36, 39, 41, 43):
        fill(img, (fx, 5, 1, 0.7), FRAME)
    fill(img, rc["north"], FRAME)
    fill(img, rc["south"], GLASS_DARK)

    # ---- 桨毂：深色，顶部一点高光 ----
    rs = face_regions(BOXES[7])
    for key in ("down", "up", "west", "north", "east", "south"):
        fill(img, rs[key], DARK)
    fill(img, rs["down"], SILVER)

    # ---- 四叶螺旋桨：透明底 + 黑色十字桨叶 ----
    for x0 in (44, 50):
        fill(img, (x0, 8, 3, 3), CLEAR)
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            img.putpixel((x0 + dx, 8 + dy), BLACK)

    # ---- 尾轮：深色 ----
    fill(img, (60, 40, 4, 4), DARK)

    return img


# ---------------------------------------------------------------- 预览

def face_quads(box):
    x, y, z, w, h, d, _, _ = box
    x0, y0, z0 = x, y, z
    x1, y1, z1 = x + w, y + h, z + d
    r = face_regions(box)
    return [
        # DOWN 面在 minY（视觉朝上）；UP 面在 maxY（视觉朝下）
        ("down", [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], r["down"]),
        ("up", [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], r["up"]),
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
        p = np.array([p[0], -p[1], p[2]], dtype=float)
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
    # 逐像素深度缓冲：画家算法对上下表面只差 0.1 的机翼会翻车
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


def main():
    img = draw_texture()
    img.save(OUT)
    print("wrote", OUT)
    if "--preview" in sys.argv:
        render_preview(img, "/tmp/preview_tenga_entity.png", span=22.0)
        render_preview(img, "/tmp/preview_tenga_entity_side.png",
                       eye=(-16.0, -1.0, 0.0), target=(0.0, -2.0, 0.0), span=18.0)
        render_preview(img, "/tmp/preview_tenga_entity_top.png",
                       eye=(-2.0, 18.0, -4.0), target=(0.0, -2.0, 0.0), span=23.0)
        render_preview(img, "/tmp/preview_tenga_entity_below.png",
                       eye=(-3.0, -10.0, -3.0), target=(0.0, -1.0, 0.0), span=22.0)


if __name__ == "__main__":
    main()
