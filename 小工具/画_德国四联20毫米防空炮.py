#!/usr/bin/env python3
"""重画「德国四联20毫米防空炮」（german_quad_20mm_aa）物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/文件:Equip_L_45.png
      Equip_L_45.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x114-395 y180-390 = 282×211 —— 双联×2 炮管朝右 + 浅蓝防盾 + 圆柱基座，横构图。

管线与 画_美国三联16英寸炮MK6.py 一致（横构图按宽定尺）:
  裁内容 bbox → 预乘 alpha 面积平均降采样（满宽 32 → 32×24）→
  量化 48 色 → 画布纵向居中 → 最外圈 8 邻域补纯黑描边。
  近防炮单态，无装填/空膛两态。
"""
from PIL import Image
import numpy as np
import os
import urllib.request

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_45.png'
SRC_URL = 'https://0v0.zjsnrwiki.com/images/3/30/Equip_L_45.png'  # 仅缓存缺失时用一次
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/火炮/'

CANVAS = 32
ART_W = 32        # 282×211 横构图，按满宽等比降采样 → 32×24
N_COLORS = 48     # 量化档位：范例贴图普遍 30~110 色


def load_original():
    """本地缓存优先；缓存缺失才下载一次并写回缓存。"""
    if not os.path.exists(SRC):
        os.makedirs(os.path.dirname(SRC), exist_ok=True)
        req = urllib.request.Request(SRC_URL, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=30) as r:
            data = r.read()
        with open(SRC, 'wb') as f:
            f.write(data)
        print(f'已下载原画缓存: {SRC}')
    return Image.open(SRC).convert('RGBA')


def downsample_art(img):
    """裁内容 bbox 后按满宽等比面积平均降采样（预乘 alpha 避免边缘黑边）。"""
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
    """不透明像素最外圈外侧补一圈纯黑描边（左右贴边会被自然裁掉，与范例一致）。"""
    solid = arr[:, :, 3] > 8
    grow = np.zeros_like(solid)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            grow |= np.roll(np.roll(solid, dy, 0), dx, 1)
    edge = grow & ~solid
    arr[edge] = (0, 0, 0, 255)
    return arr


def build():
    art = quantize(downsample_art(load_original()))
    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    y0 = (CANVAS - art.shape[0]) // 2   # 纵向居中，上下各留描边余量
    canvas[y0:y0 + art.shape[0], :art.shape[1]] = art
    canvas = add_outline(canvas)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    name = 'german_quad_20mm_aa.png'
    im = build()
    im.save(OUT_MOD + name)
    os.makedirs(OUT_ART, exist_ok=True)
    im.save(OUT_ART + name)   # 美术素材库是画风源头，成品同步入库存档
    print('saved', name, im.size)
