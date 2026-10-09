#!/usr/bin/env python3
"""德国单装127毫米炮 - 纯手动指定结构线版本"""
from PIL import Image
import numpy as np

# 1. 加载并重采样
cache_path = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_5.png'
orig = Image.open(cache_path).convert('RGBA')
base = orig.resize((32, 32), Image.Resampling.LANCZOS)
base_arr = np.array(base)

# 2. 色块量化
rgb = base_arr[:, :, :3]
rgb_quant = (rgb // 20) * 20
base_arr[:, :, :3] = rgb_quant

# 3. 外轮廓
h, w = 32, 32
alpha = base_arr[:, :, 3]
edges = set()

for y in range(h):
    for x in range(w):
        if alpha[y, x] < 128:
            continue
        for dy, dx in [(-1,0), (1,0), (0,-1), (0,1), (-1,-1), (-1,1), (1,-1), (1,1)]:
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w:
                if alpha[ny, nx] < 128:
                    edges.add((y, x))
                    break
            else:
                edges.add((y, x))
                break

# 4. 手动指定内部结构线（看着原画标记）
internal_lines = set()

# 炮管左侧开口深色边缘（垂直线，x=8-9之间）
for y in range(10, 20):
    if alpha[y, 8] > 128:
        internal_lines.add((y, 8))

# 炮管底部与护盾的水平分界（y=20-21）
for x in range(9, 20):
    if alpha[20, x] > 128:
        internal_lines.add((20, x))

# 5. 合并并应用描边
all_edges = edges | internal_lines
outlined = base_arr.copy()
for y, x in all_edges:
    outlined[y, x] = [0, 0, 0, 255]

# 6. 保存装填完成态
result_loaded = Image.fromarray(outlined)
result_loaded.save('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/german_single_127mm_gun.png')

print(f"✓ 装填完成态：{len(all_edges)} 个描边像素")
print(f"  - 外轮廓：{len(edges)} 像素")
print(f"  - 内部结构线：{len(internal_lines)} 像素（手动指定）")

# 7. 空膛态
ref_empty = Image.open('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/xtb2d_empty.png').convert('RGBA')
ref_empty_arr = np.array(ref_empty)

empty_arr = outlined.copy()
for y in range(32):
    for x in range(32):
        ref_pixel = ref_empty_arr[y, x]
        if ref_pixel[0] > 200 and ref_pixel[1] > 200 and ref_pixel[2] < 100 and ref_pixel[3] > 200:
            empty_arr[y, x] = ref_pixel

result_empty = Image.fromarray(empty_arr)
result_empty.save('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item/german_single_127mm_gun_empty.png')

print("✓ 空膛态已生成")
