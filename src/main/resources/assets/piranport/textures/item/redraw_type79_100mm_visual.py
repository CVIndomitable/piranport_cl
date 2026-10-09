#!/usr/bin/env python3
"""79式双联100毫米主炮 - 纯视觉判断版本
完全抛弃算法阈值，手动标记需要结构线的关键位置
"""
from PIL import Image
import numpy as np

# 1. 加载原画（512×512）
cache_path = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_2.png'
orig = Image.open(cache_path).convert('RGBA')
orig_arr = np.array(orig)

# 2. 重采样到32×32
base = orig.resize((32, 32), Image.Resampling.LANCZOS)
base_arr = np.array(base)

# 3. 色块量化
rgb = base_arr[:, :, :3]
rgb_quant = (rgb // 20) * 20
base_arr[:, :, :3] = rgb_quant

# 4. 外轮廓：透明边界
h, w = 32, 32
alpha = base_arr[:, :, 3]
edges = set()

for y in range(h):
    for x in range(w):
        if alpha[y, x] < 128:
            continue
        # 检查8邻域
        for dy, dx in [(-1,0), (1,0), (0,-1), (0,1), (-1,-1), (-1,1), (1,-1), (1,1)]:
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w:
                if alpha[ny, nx] < 128:
                    edges.add((y, x))
                    break
            else:
                edges.add((y, x))
                break

# 5. 手动标记内部结构线（视觉判断）
# 观察原画关键结构：
# - 双炮管中间分界线（x=15-16，垂直线）
# - 炮管与炮塔的水平分界（y=18-20）
# - 炮塔前后面板交界（某些垂直或斜线）

internal_lines = set()

# 双炮管中间分界（最重要的结构线）
for y in range(6, 18):
    if alpha[y, 15] > 128 and alpha[y, 16] > 128:
        c1 = base_arr[y, 15, :3]
        c2 = base_arr[y, 16, :3]
        # 左右两根炮管的颜色差异
        if np.sum(np.abs(c1.astype(int) - c2.astype(int))) > 40:
            internal_lines.add((y, 15))

# 炮管与炮塔的水平分界
for x in range(10, 22):
    if alpha[18, x] > 128 and alpha[19, x] > 128:
        c1 = base_arr[18, x, :3]
        c2 = base_arr[19, x, :3]
        if np.sum(np.abs(c1.astype(int) - c2.astype(int))) > 50:
            internal_lines.add((18, x))

# 炮塔底部主要水平线
for x in range(8, 24):
    if alpha[24, x] > 128 and alpha[25, x] > 128:
        c1 = base_arr[24, x, :3]
        c2 = base_arr[25, x, :3]
        if np.sum(np.abs(c1.astype(int) - c2.astype(int))) > 50:
            internal_lines.add((24, x))

# 6. 合并描边
all_edges = edges | internal_lines

# 7. 应用黑色描边
outlined = base_arr.copy()
for y, x in all_edges:
    outlined[y, x] = [0, 0, 0, 255]

# 8. 保存装填完成态
result_loaded = Image.fromarray(outlined)
result_loaded.save('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/type79_twin_100mm_gun.png')

print(f"✓ 装填完成态：{len(all_edges)} 个描边像素")
print(f"  - 外轮廓：{len(edges)} 像素")
print(f"  - 内部结构线：{len(internal_lines)} 像素")

# 9. 空膛态：提取警告三角
ref_empty = Image.open('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/xtb2d_empty.png').convert('RGBA')
ref_empty_arr = np.array(ref_empty)

empty_arr = outlined.copy()
# 逐像素复制黄色警告三角（底部）
for y in range(32):
    for x in range(32):
        ref_pixel = ref_empty_arr[y, x]
        # 黄色判定：R > 200, G > 200, B < 100, alpha > 200
        if ref_pixel[0] > 200 and ref_pixel[1] > 200 and ref_pixel[2] < 100 and ref_pixel[3] > 200:
            empty_arr[y, x] = ref_pixel

result_empty = Image.fromarray(empty_arr)
result_empty.save('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/type79_twin_100mm_gun_empty.png')

print("✓ 空膛态已生成（逐像素提取警告三角）")
