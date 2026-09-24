#!/usr/bin/env python3
"""重画「法国四联380毫米炮」（french_quad_380mm_gun）物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/文件:Equip_L_102.png
      Equip_L_102.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x34-475 y165-384 = 442×220 —— 银白法式四联炮塔，
      四管朝左下，下方圆柱炮座，2:1 扁构图。

管线与 画_德国双联380毫米炮.py 一致:
  裁内容 bbox → 预乘 alpha 面积平均降采样（满宽 32 → 32×16）→
  量化 48 色 → 画布贴底摆放（同 chinese_twin 扁炮排版）→
  最外圈 8 邻域补纯黑描边。

两态: 本体逐像素一致；装填态无底部图标，空膛态在 (12,24) 盖项目标准
  BURST 空膛叹号（9×8，与 chinese_twin / german_twin 标准件逐像素一致），
  盖在描边之后（先 finish 再盖，叹号叠在本体底部上）。
"""
from PIL import Image
import numpy as np
import os
import urllib.request

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_102.png'
SRC_URL = 'https://0v0.zjsnrwiki.com/images/c/ce/Equip_L_102.png'  # 仅缓存缺失时用一次
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/火炮/'

CANVAS = 32
ART_W = 32        # 442×220 扁构图，按满宽等比降采样 → 32×16
ART_BOTTOM = 31   # 贴底摆放（同 chinese_twin_140mm 扁炮），叹号盖在本体下部
BURST_X, BURST_Y = 12, 24   # 空膛爆发标记位置（全项目统一）
N_COLORS = 48     # 量化档位：范例贴图普遍 30~110 色

# 空膛爆发标记 9×8：黄色爆发形（下宽上尖），提取自 f2h_banshee_empty.png（x=12-20, y=24-31）
# 与 gen_chinese_twin_140mm_gun_texture.py / 画_德国双联380毫米炮.py 的标准件逐像素一致。
T = (0, 0, 0, 0)
K = (0, 0, 0, 255)
YY = (255, 248, 0, 255)
BURST = [
    [T, T, T, T, K, T, T, T, T],
    [T, T, T, K, YY, K, T, T, T],
    [T, T, T, K, YY, K, T, T, T],
    [T, T, K, YY, K, YY, K, T, T],
    [T, K, YY, YY, K, YY, YY, K, T],
    [T, K, YY, YY, YY, YY, YY, K, T],
    [K, YY, YY, YY, K, YY, YY, YY, K],
    [K, K, K, K, K, K, K, K, K],
]


def load_original():
    """本地缓存优先；缓存缺失才下载一次并写回缓存。"""
    if not os.path.exists(SRC):
        os.makedirs(os.path.dirname(SRC), exist_ok=True)
        urllib.request.urlretrieve(SRC_URL, SRC)
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
    """不透明像素最外圈外侧补一圈纯黑描边（画布外不补，边缘自然裁切）。

    不能用 np.roll：本体贴到画布底行时，向下的描边会回绕到 row 0。
    """
    solid = arr[:, :, 3] > 8
    grow = solid.copy()
    h, w = solid.shape
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            ys, ye = max(0, dy), min(h, h + dy)
            xs, xe = max(0, dx), min(w, w + dx)
            grow[ys:ye, xs:xe] |= solid[ys - dy:ye - dy, xs - dx:xe - dx]
    edge = grow & ~solid
    arr[edge] = (0, 0, 0, 255)
    return arr


def stamp(canvas, patch, x0, y0):
    """把标准叹号盖到画布上，只覆盖其非透明像素（本体其余部分不动）。"""
    for j, row in enumerate(patch):
        for i, (r, g, b, al) in enumerate(row):
            if al:
                canvas[y0 + j, x0 + i] = (r, g, b, al)
    return canvas


def build(with_burst):
    art = quantize(downsample_art(load_original()))
    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    y0 = ART_BOTTOM - art.shape[0] + 1
    canvas[y0:y0 + art.shape[0], :art.shape[1]] = art
    canvas = add_outline(canvas)
    if with_burst:
        canvas = stamp(canvas, BURST, BURST_X, BURST_Y)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    for name, with_burst in [('french_quad_380mm_gun.png', False),
                             ('french_quad_380mm_gun_empty.png', True)]:
        im = build(with_burst)
        im.save(OUT_MOD + name)
        im.save(OUT_ART + name)   # 美术素材库是画风源头，成品同步入库存档
        print('saved', name)
