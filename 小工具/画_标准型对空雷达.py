#!/usr/bin/env python3
"""重画「标准型对空雷达」（standard_air_radar）物品贴图 —— 从原画直接降采样。

原画: https://www.zjsnrwiki.com/wiki/标准型对空雷达
      Equip_L_79.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x54-415 y26-486 = 362×461 竖构图 —— 矩形网状对空天线：
      桁架边框 + 水平张线 + 3 列菱形振子 + 螺栓横梁 + 金字塔底座。
      原画是轻微透视视角（顶框左低右高、底座见棱面），降采样原样保留。

画风: 与主炮一致的「降采样 + 量化 48 色 + 纯黑描边」管线
      （画_德国双联380毫米炮.py），不参考旧占位贴图的抛物面构图，
      也不手绘扁平正视图——手绘版已试过，丢掉原画透视，弃用。

管线: 裁内容 bbox → 预乘 alpha 面积平均降采样（宽 22 → 22×28 竖构图）→
      量化 48 色 → 画布水平居中、底边 y30 → 最外圈 8 邻域补纯黑描边。

两态: 雷达是被动装备不是武器，无装填/空膛两态，只出单张贴图。

预览: 写 小工具/_preview/preview_画_标准型对空雷达_<说明>.png
      （统一 preview_<任务名> 命名，防并发线程互相覆盖；不用 /tmp）。
"""
from PIL import Image
import numpy as np
import os
import hashlib

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_79.png'
OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/舰船装备/'
PREVIEW_DIR = '/Users/lianran/apps/皮兰港实验/测试版/小工具/_preview/'

CANVAS = 32
ART_W = 22         # 362×461 竖构图：宽 22 → 等比 22×28，给描边留出四周
ART_BOTTOM = 30    # 本体底边行号：y31 留一行给描边
N_COLORS = 48      # 量化档位：与 german_twin_380毫米炮一致


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


def build():
    art = quantize(downsample_art(load_original()))
    canvas = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    h, w = art.shape[:2]
    y0 = ART_BOTTOM - h + 1
    x0 = (CANVAS - w) // 2   # 竖构图水平居中，左右对称
    canvas[y0:y0 + h, x0:x0 + w] = art
    canvas = add_outline(canvas)
    return Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), 'RGBA')


def save_preview(im, note):
    """预览落盘：统一命名 preview_<任务名>.png（并发线程间天然不冲突的命名空间）。"""
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    path = f'{PREVIEW_DIR}preview_画_标准型对空雷达_{note}.png'
    im.save(path)
    print('preview', path, 'src_md5', hashlib.md5(open(SRC, 'rb').read()).hexdigest()[:8])
    return path


if __name__ == '__main__':
    im = build()
    im.save(OUT_MOD + 'standard_air_radar.png')
    im.save(OUT_ART + 'standard_air_radar.png')   # 美术素材库是画风源头，成品同步入库存档
    save_preview(im.resize((256, 256), Image.NEAREST), 'x8')
    print('saved standard_air_radar.png')
