#!/usr/bin/env python3
"""重画「标准型对海雷达」（standard_surface_radar）物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/标准型对海雷达
      Equip_L_80.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x139-331 y150-381 = 193×232 —— 军绿色三块矩形天线板 + 圆柱基座，竖构图。

管线与 画_德国双联380毫米炮.py 一致:
  裁内容 bbox → 预乘 alpha 面积平均降采样（高 28 → 23×28）→
  量化 48 色 → 画布居中摆放 y0=2 → 最外圈 8 邻域补纯黑描边。
  雷达单态，无空膛叹号。
"""
from PIL import Image
import numpy as np
import os
import urllib.request

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_80.png'
SRC_URL = 'https://0v0.zjsnrwiki.com/images/6/67/Equip_L_80.png'  # 仅缓存缺失时用一次
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/舰船装备/'

CANVAS = 32
ART_H = 28          # 竖构图按高等比：28 → 23 宽，四周留描边余量
N_COLORS = 48       # 量化档位：范例贴图普遍 30~110 色
Y0 = 2              # 本体顶行，底行 29，描边可到 30，顶描边 1（y0=0 留呼吸）


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
    """裁内容 bbox 后按高定尺等比面积平均降采样（预乘 alpha 避免边缘黑边）。"""
    a = np.asarray(img, dtype=np.float64) / 255.0
    op = a[:, :, 3] > 8 / 255.0
    ys, xs = np.where(op)
    a = a[ys.min():ys.max() + 1, xs.min():xs.max() + 1]

    h_src, w_src = a.shape[:2]
    w_out = max(1, round(ART_H * w_src / h_src))

    # 预乘后再采样：半透明边缘像素的 RGB 才不会把透明区的脏色平均进来
    premult = a.copy()
    premult[:, :, :3] *= premult[:, :, 3:4]

    def box_resize(ch2d):
        return np.asarray(
            Image.fromarray(ch2d.astype(np.float32), mode='F').resize((w_out, ART_H), Image.BOX)
        )

    out = np.zeros((ART_H, w_out, 4), dtype=np.float64)
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
    """不像素最外圈外侧补一圈纯黑描边（画布边缘会自然裁掉，与范例一致）。"""
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
    x0 = (CANVAS - art.shape[1]) // 2
    canvas[Y0:Y0 + art.shape[0], x0:x0 + art.shape[1]] = art
    canvas = add_outline(canvas)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    name = 'standard_surface_radar.png'
    im = build()
    im.save(OUT_MOD + name)
    im.save(OUT_ART + name)   # 美术素材库是画风源头，成品同步入库存档
    print('saved', name, im.size)
