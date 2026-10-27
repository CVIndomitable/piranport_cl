#!/usr/bin/env python3
"""重画「CLT四联鱼雷发射器零式」物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/文件:Equip_L_704.png
      Equip_L_704.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      主内容 bbox x63-448 y211-419 = 386×209 —— 方正的多面体装甲发射箱，
      四根鱼雷管向右前方伸出；箱体左侧一排小舷窗，后方有一排 C 形卡箍。

注意：原画左上角 (x113-193 y75-183) 另有一盏独立探照灯，不是发射器本体。
本模组的立体模型明确不建模探照灯，图标也一并裁掉，避免图标多出一个
模型里不存在的部件、同时把发射器本体压小。

管线与其他火炮/装备图标一致：
  裁内容 bbox → 预乘 alpha 面积平均降采样（满宽 32 → 32×17）→
  量化 48 色 → 画布底部对齐 ART_BOTTOM → 最外圈 8 邻域补纯黑描边。

两态: 本体逐像素一致；装填态无底部图标，空膛态在 (12,24) 盖项目标准
  BURST 空膛叹号（9×8，与项目内其他 _empty 图标逐像素一致）。
"""
from PIL import Image
import numpy as np
import os

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_704.png'
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/鱼雷/'

# 探照灯与本体之间只有空白，y=200 一刀切干净（原画探照灯 y75-183）
CROP_TOP = 200

CANVAS = 32
ART_W = 32        # 386×209 横向构图，按满宽等比降采样 → 32×17
ART_BOTTOM = 23   # 本体底边行号：下方留 y24 描边 + y24..31 叹号
BURST_X, BURST_Y = 12, 24   # 空膛爆发标记位置（全项目统一）
N_COLORS = 48     # 量化档位：范例贴图普遍 30~110 色

# 空膛爆发标记 9×8：黄色爆发形（下宽上尖），与其他 _empty 图标逐像素一致。
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
    """只读本地缓存。原画缓存缺失时直接报错，不联网下载。"""
    if not os.path.exists(SRC):
        raise FileNotFoundError(f'缺少原画缓存，请先手动放到: {SRC}')
    return Image.open(SRC).convert('RGBA')


def downsample_art(img):
    """裁内容 bbox 后按满宽等比面积平均降采样（预乘 alpha 避免边缘黑边）。"""
    a = np.asarray(img, dtype=np.float64) / 255.0
    a[:CROP_TOP, :, 3] = 0.0   # 去掉左上角探照灯
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
    # 装填态 = 原画本体；空膛态 = 同本体 + 标准叹号
    for name, with_burst in [('clt_quad_torpedo_launcher_type0.png', False),
                             ('clt_quad_torpedo_launcher_type0_empty.png', True)]:
        im = build(with_burst)
        im.save(OUT_MOD + name)
        os.makedirs(OUT_ART, exist_ok=True)
        im.save(OUT_ART + name)   # 美术素材库是画风源头，成品同步入库存档
        print('saved', name, im.size)
