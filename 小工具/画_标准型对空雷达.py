#!/usr/bin/env python3
"""重画「标准型对空雷达」（standard_air_radar）物品贴图 —— 按原画构图手绘像素。

原画: https://www.zjsnrwiki.com/wiki/标准型对空雷达
      Equip_L_79.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x54-415 y26-486 = 362×461 竖构图 —— 矩形网状对空天线：
      桁架边框 + 水平张线 + 3 列菱形振子 + 螺栓横梁 + 金字塔底座。

为什么手绘而不是降采样（与画_德国双联380毫米炮.py 的管线分岔）:
  原画天线网眼/菱形振子在 32px 下直接面积平均会糊成色块（试过，顶框还被
  量化带偏成棕色）。细网格类素材必须先解出几何模板再逐像素落点。

配色全部取自原画区域采样:
  网面暗底 #1d201f / 张线 #303534 / 桁架 #464c4a-#717b77 /
  横梁高光 #8b9992-#c2ccc8 / 底座 #a8b4af。

两态: 雷达是被动装备不是武器，无装填/空膛两态，只出单张贴图。
"""
from PIL import Image
import numpy as np
import os

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_79.png'
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/舰船装备/'

CANVAS = 32

T = (0, 0, 0, 0)
# 网面
MESH = (29, 32, 31, 255)      # #1d201f 暗网底
WIRE = (48, 53, 52, 255)      # #303534 水平张线
TRUSS = (70, 76, 74, 255)     # #464c4a 桁架中灰
TRUSS_HI = (113, 123, 119, 255)  # #717b77 桁架高光
FRAME = (76, 84, 81, 255)     # #4c5451 外框
BEAM_HI = (194, 204, 200, 255)  # #c2ccc8 横梁高光
BEAM_MID = (139, 153, 146, 255)  # #8b9992 横梁中调
BEAM_LO = (58, 64, 62, 255)   # #3a403e 横梁暗部/螺栓
BASE = (168, 180, 175, 255)   # #a8b4af 底座亮面
BASE_SH = (120, 132, 127, 255)  # 底座暗面
DIP = (140, 152, 147, 255)    # 菱形振子主体
DIP_HI = (186, 198, 193, 255)  # 振子高光

# 布局（32×32，四周留 1 行/列给黑描边）
# 阵面 x4-27 y3-21；桁架边 x4-6 / x25-27；网面 x7-24 y4-20
# 横梁 y22-23 满阵宽；立柱 y24-25；金字塔底座 y26-30


def draw():
    c = np.zeros((CANVAS, CANVAS, 4), dtype=np.uint8)

    def px(x, y, col):
        c[y, x] = col

    def rect(x0, y0, x1, y1, col):
        c[y0:y1 + 1, x0:x1 + 1] = col

    # ---- 网面暗底 ----
    rect(7, 4, 24, 20, MESH)

    # ---- 水平张线（每 4px 一根，菱形振子以线为中心骑在上面——对齐原画）----
    wires = (6, 10, 14, 18)
    for y in wires:
        rect(7, y, 24, y, WIRE)

    # ---- 菱形振子：3 列 x 4 行，5 像素对称菱形，中心压在张线上 ----
    for cx in (11, 15, 19):
        for cy in wires:
            px(cx, cy - 1, DIP_HI)      # 上尖（网底行，不会被张线吞掉）
            px(cx - 1, cy, DIP)         # 左右翼压住张线
            px(cx, cy, DIP_HI)
            px(cx + 1, cy, DIP)
            px(cx, cy + 1, DIP)         # 下尖同色收口

    # ---- 桁架边框（左右各 3 列：外框 + 内侧之字斜纹）----
    for x0 in (4, 25):
        rect(x0, 3, x0 + 2, 21, TRUSS)
    # 外侧立柱高光
    rect(4, 3, 4, 21, TRUSS_HI)
    rect(27, 3, 27, 21, TRUSS_HI)
    # 之字斜纹（原画桁架的交叉腹杆，1px 斜线交替）
    for i, y in enumerate(range(4, 21)):
        if i % 2 == 0:
            px(5, y, FRAME)
            px(26, y, FRAME)
        else:
            px(6, y, FRAME)
            px(25, y, FRAME)

    # ---- 顶框横杆 ----
    rect(4, 3, 27, 3, FRAME)
    rect(5, 3, 26, 3, TRUSS_HI)

    # ---- 底部横梁（原画：满宽浅色梁 + 一排圆螺栓）----
    rect(4, 22, 27, 22, BEAM_HI)
    rect(4, 23, 27, 23, BEAM_MID)
    for x in (7, 12, 17, 22):
        px(x, 22, BEAM_LO)

    # ---- 中央立柱 ----
    rect(15, 24, 16, 25, BASE)
    px(15, 24, BEAM_HI)

    # ---- 金字塔底座（原画：上窄下宽，左亮右暗两面）----
    rows = [(14, 17), (13, 18), (12, 19), (11, 20), (10, 21)]
    for y, (x0, x1) in zip(range(26, 31), rows):
        mid = (x0 + x1) // 2
        rect(x0, y, mid, y, BASE)       # 左侧亮面
        rect(mid + 1, y, x1, y, BASE_SH)  # 右侧暗面

    # ---- 最外圈 8 邻域补纯黑描边（画布边缘自然裁切，与范例一致）----
    solid = c[:, :, 3] > 8
    grow = np.zeros_like(solid)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            grow |= np.roll(np.roll(solid, dy, 0), dx, 1)
    edge = grow & ~solid
    c[edge] = (0, 0, 0, 255)
    return Image.fromarray(c, 'RGBA')


if __name__ == '__main__':
    # 原画缓存自检：确保对照的原画还在
    assert os.path.exists(SRC), f'原画缓存缺失: {SRC}'
    im = draw()
    im.save(OUT_MOD + 'standard_air_radar.png')
    im.save(OUT_ART + 'standard_air_radar.png')   # 美术素材库是画风源头，成品同步入库存档
    print('saved standard_air_radar.png')
