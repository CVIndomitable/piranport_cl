#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重画「日本15.2厘米单装炮」（japanese_single_152mm_gun）物品贴图。

原画: https://www.zjsnrwiki.com/wiki/文件:Equip_L_38.png
      Equip_L_38.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x28-486 y207-363 = 459×157 —— 绿色圆筒炮盾 + 炮管朝右，
      炮根有一段浅色帆布套。

为什么重画：此前这门炮的物品模型借用了 single_small_gun 的 12cm 图标，
构图和原画完全不符。

管线与项目内其他火炮图标一致：
  裁内容 bbox → 预乘 alpha 面积平均降采样（长边 30 → 30×10）→ 轻度 unsharp →
  量化 48 色 → 画布底部对齐 BODY_BOTTOM → 最外圈 8 邻域补纯黑描边。

两态（2026-10-23 起的项目约定）：
  - 装填态：本体 + 底部 13×6 弹药图标（x9-21/y26-31，从现有 loaded 图标逐像素取）；
  - 空膛态：本体 + 标准黄色爆发标记（9×8，x12-20/y24-31）。
"""
from PIL import Image, ImageFilter
import numpy as np
import os

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_38.png'
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/火炮/'
REF_AMMO_ICON = OUT_MOD + 'bofors_twin_120mm_gun.png'          # 13×6 弹药图标来源
REF_BURST_ICON = OUT_MOD + 'us_twin_20inch_gun_empty.png'       # 9×8 爆发标记来源

CANVAS = 32
ART_LONG = 30      # 长边 30，四周留 1px 描边余量
BODY_BOTTOM = 23   # 本体底边行号（与既有主炮图标一致）
N_COLORS = 48

AMMO_X, AMMO_Y, AMMO_W, AMMO_H = 9, 26, 13, 6
BURST_X, BURST_Y, BURST_W, BURST_H = 12, 24, 9, 8


def load_original():
    return Image.open(SRC).convert('RGBA')


def downsample_art(img):
    """裁 bbox 后按长边 30 等比面积平均降采样（预乘 alpha 避免边缘黑边）。"""
    a = np.asarray(img, dtype=np.float64) / 255.0
    op = a[:, :, 3] > 8 / 255.0
    ys, xs = np.where(op)
    a = a[ys.min():ys.max() + 1, xs.min():xs.max() + 1]

    h_src, w_src = a.shape[:2]
    if w_src >= h_src:
        w_out, h_out = ART_LONG, max(1, round(ART_LONG * h_src / w_src))
    else:
        h_out, w_out = ART_LONG, max(1, round(ART_LONG * w_src / h_src))

    premult = a.copy()
    premult[:, :, :3] *= premult[:, :, 3:4]

    def box_resize(ch2d):
        return np.asarray(
            Image.fromarray(ch2d.astype(np.float32), mode='F').resize((w_out, h_out), Image.BOX)
        )

    out = np.zeros((h_out, w_out, 4), dtype=np.float64)
    for c in range(4):
        out[:, :, c] = box_resize(premult[:, :, c])
    alpha = out[:, :, 3:4]
    rgb = np.where(alpha > 1e-6, out[:, :, :3] / np.maximum(alpha, 1e-6), 0.0)
    out[:, :, :3] = np.clip(rgb, 0.0, 1.0)
    return out * 255.0


def quantize(arr):
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
    solid = arr[:, :, 3] > 8
    grow = np.zeros_like(solid)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            grow |= np.roll(np.roll(solid, dy, 0), dx, 1)
    edge = grow & ~solid
    arr[edge] = (0, 0, 0, 255)
    return arr


def stamp(canvas, block, x0, y0):
    """只覆盖标准件的非透明像素。"""
    for j in range(block.shape[0]):
        for i in range(block.shape[1]):
            r, g, b, al = block[j, i]
            if al:
                canvas[y0 + j, x0 + i] = (r, g, b, al)
    return canvas


def build(with_burst):
    art = downsample_art(load_original())
    art = Image.fromarray(np.clip(art, 0, 255).astype(np.uint8), 'RGBA')
    art = art.filter(ImageFilter.UnsharpMask(radius=1, percent=60, threshold=0))
    art = quantize(np.array(art))

    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    h, w = art.shape[:2]
    x0 = (CANVAS - w) // 2
    y0 = BODY_BOTTOM - h + 1
    canvas[y0:y0 + h, x0:x0 + w] = art
    canvas = add_outline(canvas)

    if with_burst:
        ref = np.array(Image.open(REF_BURST_ICON).convert('RGBA'))
        canvas = stamp(canvas, ref[BURST_Y:BURST_Y + BURST_H,
                                  BURST_X:BURST_X + BURST_W], BURST_X, BURST_Y)
    else:
        ref = np.array(Image.open(REF_AMMO_ICON).convert('RGBA'))
        canvas = stamp(canvas, ref[AMMO_Y:AMMO_Y + AMMO_H,
                                  AMMO_X:AMMO_X + AMMO_W], AMMO_X, AMMO_Y)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    for name, with_burst in [('japanese_single_152mm_gun.png', False),
                             ('japanese_single_152mm_gun_empty.png', True)]:
        im = build(with_burst)
        im.save(OUT_MOD + name)
        os.makedirs(OUT_ART, exist_ok=True)
        im.save(OUT_ART + name)
        print('saved', name, im.size)
