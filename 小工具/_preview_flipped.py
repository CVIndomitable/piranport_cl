#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""临时检查工具：把模型整体绕 y 轴转 180°，用标准预览器看正面/另一舷。

物品模型预览器只画 up/south/east 三个面；正面（+Z）与左舷（-X）
在等轴测里背对相机。这里把几何整体旋转 180° 再渲染，就能看到背面细节。
"""
import json
import os
import subprocess
import sys

SRC = sys.argv[1]
OUT = sys.argv[2]

with open(SRC, encoding='utf-8') as f:
    model = json.load(f)


def rot(p):
    # 绕 (8, y, 8) 的 y 轴转 180°：x -> 16-x, z -> 16-z
    return [round(16 - p[0], 3), p[1], round(16 - p[2], 3)]


for el in model['elements']:
    f, t = el['from'], el['to']
    el['from'] = [min(a, b) for a, b in zip(rot(f), rot(t))]
    el['to'] = [max(a, b) for a, b in zip(rot(f), rot(t))]
    r = el.get('rotation')
    if r:
        r['origin'] = rot(r['origin'])
        if r['axis'] == 'y':
            r['angle'] = -r['angle']
        elif r['axis'] == 'z':
            r['angle'] = -r['angle']

tmp = '/tmp/_flip_model.json'
with open(tmp, 'w', encoding='utf-8') as f:
    json.dump(model, f, ensure_ascii=False)

here = os.path.dirname(os.path.abspath(__file__))
subprocess.run([
    'python3', os.path.join(here, 'render_block_model_preview.py'),
    tmp, OUT, '--size', '640',
    '--assets-root', 'src/main/resources/assets',
], check=True)
