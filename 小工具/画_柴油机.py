#!/usr/bin/env python3
"""重画「柴油机」（diesel_engine）物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/柴油机
      Equip_L_158.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox 约 x85-430 y130-375 ≈ 345×245 —— 横置多缸柴油机本体
      （顶部一排摇臂/气门罩 + 中部检修走廊栏杆 + 一排圆形检修盖
        + 底部曲轴箱检查孔 + 左端附件/滤清器 + 右端扶梯）。

管线与 画_高温高压锅炉.py 一致，微调三处:
  裁内容 bbox → 预乘 alpha 面积平均降采样（宽 30 → 等比约 30×21，横构图）
  → 量化 24 色（48 色会糊）→ 中灰轴对比度 +0.5（线条拉分明）
  → 画布水平居中、底边 y30 → 最外圈 8 邻域补纯黑描边。

两态: 柴油机是被动引擎装备不是武器，无装填/空膛两态，只出单张贴图。
"""
from PIL import Image
import numpy as np
import os

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_158.png'
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/舰船装备/'

CANVAS = 32
ART_W = 30         # 345×245 横构图：宽 30 → 等比约 30×21，四周留描边
ART_BOTTOM = 30    # 本体底边行号：y31 留一行给描边
N_COLORS = 24      # 量化档位：24 档让灰阶落块更利落，48 反而糊成一片
CONTRAST = 0.5     # 以中灰为轴的对比度拉伸：原画线条降采样后发灰，拉回来才分明


def load_original():
    """本地缓存优先（缓存缺失时报错——下载见模块 docstring）。"""
    if not os.path.exists(SRC):
        raise FileNotFoundError(f'原画缓存缺失: {SRC}')
    return Image.open(SRC).convert('RGBA')


def downsample_art(img):
    """裁内容 bbox 后按宽等比面积平均降采样（预乘 alpha 避免边缘黑边）。"""
    a = np.asarray(img, dtype=np.float64) / 255.0
    op = a[:, :, 3] > 8 / 255.0
    ys, xs = np.where(op)
    a = a[ys.min():ys.max() + 1, xs.min():xs.max() + 1]

    h_src, w_src = a.shape[:2]
    h_out = max(1, round(ART_W * h_src / w_src))

    # 预乘后再采样：半透明边缘像素的 RGB 才不会把透明区的脏色平均进来
    premult = a.copy()
    premult[:, :, :3] *= premult[:, :, 3:4]

    def box_resize(ch2d):
        return np.asarray(
            Image.fromarray(ch2d.astype(np.float32), mode='F').resize((ART_W, h_out), Image.BOX)
        )

    out = np.zeros((h_out, ART_W, 4), dtype=np.float64)
    for c in range(4):
        out[:, :, c] = box_resize(premult[:, :, c])
    alpha = out[:, :, 3:4]
    rgb = np.where(alpha > 1e-6, out[:, :, :3] / np.maximum(alpha, 1e-6), 0.0)
    out[:, :, :3] = np.clip(rgb, 0.0, 1.0)
    return out * 255.0


def quantize(arr):
    """量化颜色到 N_COLORS 档，贴齐范例 30~110 色的颗粒质感；alpha 保留软边。"""
    h, w = arr.shape[:2]
    flat = arr.reshape(-1, 4)
    op = flat[:, 3] > 8
    rgb_u8 = np.clip(flat[op, :3], 0, 255).astype(np.uint8)
    q = Image.fromarray(rgb_u8.reshape(-1, 1, 3), 'RGB').quantize(
        colors=N_COLORS, method=Image.MEDIANCUT
    )
    pal = np.asarray(q.convert('RGB'), dtype=np.float64).reshape(-1, 3)
    flat[op, :3] = pal
    return flat.reshape(h, w, 4)


def add_outline(arr):
    """不透明像素最外圈外侧补一圈纯黑描边（画布边缘会自然裁掉，与范例一致）。"""
    solid = arr[:, :, 3] > 8
    grow = np.zeros_like(solid)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            grow |= np.roll(np.roll(solid, dy, 0), dx, 1)
    edge = grow & ~solid
    arr[edge] = (0, 0, 0, 255)
    return arr


def boost_contrast(arr):
    """对量化后的 RGB 以中灰为轴做对比度拉伸，让发灰的线条重新分明。"""
    rgb = arr[:, :, :3]
    op = arr[:, :, 3] > 8
    rgb[op] = np.clip(128.0 + (rgb[op] - 128.0) * (1 + CONTRAST), 0, 255)
    arr[:, :, :3] = rgb
    return arr


def build():
    art = boost_contrast(quantize(downsample_art(load_original())))
    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    h, w = art.shape[:2]
    y0 = ART_BOTTOM - h + 1
    x0 = (CANVAS - w) // 2   # 横构图水平居中
    canvas[y0:y0 + h, x0:x0 + w] = art
    canvas = add_outline(canvas)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    im = build()
    im.save(OUT_MOD + 'diesel_engine.png')
    im.save(OUT_ART + 'diesel_engine.png')   # 美术素材库是画风源头，成品同步入库存档
    print('saved diesel_engine.png')
