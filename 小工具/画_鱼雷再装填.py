#!/usr/bin/env python3
"""重画「鱼雷再装填」（torpedo_reload）物品贴图 —— 按原画构图手绘像素。

原画: https://www.zjsnrwiki.com/wiki/鱼雷再装填系统
      Equip_L_466.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
      内容 bbox x33-479 y133-376 = 446×243 —— 深蓝灰再装填系统：
      左侧鱼雷头探出 + 中部圆角舰体甲板 + 后方烟囱圆柱塔 +
      右侧长条网格再装填轨道架 + 穿架绑带支腿。

为什么手绘而不是降采样（与 画_标准型对空雷达.py 的管线分岔）:
  原画是四件套复杂场景（塔/网格架/甲板/鱼雷）且整体深蓝灰单色系，
  32px 直接面积平均会糊成一块深色斜条（试过 BOX/LANCZOS/USM 三版，内部结构全丢）。
  多结构+细网格类素材必须先解出几何模板再逐像素落点。

构图对齐原画（446×243 → 画布 x0-31 y13-31，贴底摆放）:
  鱼雷 = 左缘三条亮/暗交替条纹（W/O 相间即三根雷的速写）；
  舰体 = x3-14 圆角块，甲板 y22-24 带三舷窗，右侧 y23-24 接两根连接管；
  塔   = x14-18 圆柱 y15-21（顶环+中段箍带+明暗三调），基座 y20-21 加宽；
  轨道架 = x19-30，顶面 y18-21 双排网格格，前面板 y22-26 竖向板缝；
  绑带支腿 = x27-29 纵贯架面下行至 y30，带三个观察孔（原画右侧特征）。

配色全部取自原画区域采样:
  塔 #9a9dab/#80838d/#6a6d7a，架顶 #414249 格线 #323348，前面板 #47484f/#303245，
  甲板 #6f717d/#585a6d，舰体 #484a5e，雷头 #e2e1ed 雷身 #43455a，支腿 #222235。

两态: 鱼雷再装填是被动部件不是武器（单模型 torpedo_reload.json），
      无装填/空膛两态，只出单张贴图。
"""
from PIL import Image
import numpy as np
import os

OUT_MOD = '/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/'
OUT_ART = '/Users/lianran/apps/皮兰港实验/美术素材/Item（图标）/舰船装备/'

CANVAS = 32

PALETTE = {
    'B': (61, 62, 69, 255),      # #3d3e45 塔顶小炮座
    'r': (59, 59, 60, 255),      # #3b3b3c 塔顶环/箍带
    'H': (154, 157, 171, 255),   # #9a9dab 塔受光面
    'T': (128, 131, 141, 255),   # #80838d 塔身中调
    't': (106, 109, 122, 255),   # #6a6d7a 塔背光面
    'R': (65, 66, 73, 255),      # #414249 轨道架顶面
    'g': (50, 51, 72, 255),      # #323348 顶面格线
    'F': (71, 72, 79, 255),      # #47484f 前面板
    'f': (48, 50, 69, 255),      # #303245 前面板板缝/架底沿
    'D': (111, 113, 125, 255),   # #6f717d 甲板亮面
    'd': (88, 90, 109, 255),     # #585a6d 甲板暗面/舰体上缘
    'h': (72, 74, 94, 255),      # #484a5e 舰体侧暗面
    'p': (48, 53, 60, 255),      # 舷窗
    'W': (226, 225, 237, 255),   # #e2e1ed 鱼雷头亮面
    'O': (67, 69, 90, 255),      # #43455a 鱼雷身/连接管
    'L': (34, 34, 53, 255),      # #222235 绑带支腿
    'o': (71, 72, 79, 255),      # 支腿观察孔
}

# 32×32 逐像素落点（'.' = 透明）。y0-12 留白，本体贴底 y13-31。
ROWS = [
    '................................',  # 0
    '................................',  # 1
    '................................',  # 2
    '................................',  # 3
    '................................',  # 4
    '................................',  # 5
    '................................',  # 6
    '................................',  # 7
    '................................',  # 8
    '................................',  # 9
    '................................',  # 10
    '................................',  # 11
    '................................',  # 12
    '................B...............',  # 13 塔顶观瞄柱
    '...............BBB..............',  # 14 炮座
    '..............rrrrr.............',  # 15 塔顶环
    '..............HTTtt.............',  # 16 塔身
    '..............HTTtt.............',  # 17 塔身
    '..............rrrrrRRRgRRRgLLLR.',  # 18 箍带 + 架顶格排1（绑带压格）
    '..............HTTttggggggggLLLg.',  # 19 格排间隙（绑带续）
    '.............tHTTttrRRRgRRRLLLR.',  # 20 塔基座 + 架顶格排2
    '.............tHTTttrgggggggLLLg.',  # 21 塔基座 + 架顶格间隙
    '...DDDDDDDDDDDDDDDDFFFFfFFFLLLF.',  # 22 甲板亮面接架前脸
    '...DDDpDDpDDpDDOOOOFFFFfFFFLLLF.',  # 23 甲板舷窗 + 连接管
    '...ddddddddddddOOOOFFFFfFFFLLLF.',  # 24 甲板暗面 + 连接管
    'WWWddddddddddddhhhhFFFFfFFFLLLF.',  # 25 雷1亮 + 舰体上缘 + 管影
    'OOOhhhhhhhhhhhhhhhhFFFFfFFFLLLF.',  # 26 雷1暗 + 舰体(x3-18) + 架底沿
    'WWWhhhhhhhhhhhh............LoL..',  # 27 雷2亮 + 舰体 + 支腿孔1
    'OOOhhhhhhhhhhh.............LLL..',  # 28 雷2暗 + 舰体收 + 支腿
    'WWWhhhhhhhhhhh.............LoL..',  # 29 雷3亮 + 舰体收 + 支腿孔2
    'OOOhhhhhhhhhh..............LLL..',  # 30 雷3暗 + 舰体底 + 支腿
    '................................',  # 31 描边行（程序补）
]


def build():
    assert len(ROWS) == 32, len(ROWS)
    arr = np.zeros((CANVAS, CANVAS, 4), dtype=np.float64)
    for y, row in enumerate(ROWS):
        row = row.replace(' ', '')  # 排版空格不进画布
        assert len(row) == 32, f'y{y} len={len(row)}: {row}'
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            assert ch in PALETTE, f'未知色号 {ch} at ({x},{y})'
            arr[y, x] = PALETTE[ch]

    # 最外圈 8 邻域补纯黑描边（padded 邻域扩张，不回绕画布）
    solid = arr[:, :, 3] > 8
    p = np.pad(solid, 1, constant_values=False)
    grow = np.zeros_like(solid)
    for dy in (0, 1, 2):
        for dx in (0, 1, 2):
            grow |= p[dy:dy + solid.shape[0], dx:dx + solid.shape[1]]
    edge = grow & ~solid
    arr[edge] = (0, 0, 0, 255)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    im = build()
    im.save(OUT_MOD + 'torpedo_reload.png')
    im.save(OUT_ART + 'torpedo_reload.png')   # 美术素材库是画风源头，成品同步入库存档
    print('saved torpedo_reload.png')

    # 预览：白底，文件名带任务前缀防并发线程覆盖
    from PIL import ImageDraw
    bg = Image.new('RGBA', (32, 32), (255, 255, 255, 255))
    bg.alpha_composite(im)
    out = bg.convert('RGB').resize((512, 512), Image.NEAREST)
    d = ImageDraw.Draw(out)
    d.rectangle([0, 0, 40, 40], fill=(255, 0, 0))
    d.rectangle([471, 0, 511, 40], fill=(0, 200, 0))
    d.rectangle([0, 471, 40, 511], fill=(0, 0, 255))
    out.save('/Users/lianran/apps/皮兰港实验/测试版/小工具/画_鱼雷再装填_preview.png')
    print('preview saved')
