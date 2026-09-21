"""
按辣椒种子（chili_seeds）的画风重画各种子贴图。

辣椒种子的画法要点（从 chili_seeds.png 逐像素还原）：
- 32×32 画布，实际作画包围盒约 17×19，四周留白，主体偏左上
- 主体是 **2~4 枚水滴形（teardrop）籽粒**：圆头在一端，另一端收成**尖尾**
- 籽粒整体拉长，长轴带倾角，不是圆球
- 每枚籽粒 **3 层色阶**：亮面（迎光侧）→ 主色 → 暗面（背光侧）
- 深色描边，用主色的深色调（约 0.55 倍明度），不用纯黑
- 迎光侧的圆头附近有一小块高光

用法：改 SPECIES 表里的配色，跑一次即可输出全部贴图。
"""

import os
from PIL import Image

W = H = 32


def shade(rgb, f):
    """按系数 f 调整明暗，f<1 变暗，f>1 变亮。"""
    return tuple(max(0, min(255, int(c * f))) for c in rgb)


def draw_seed(px, x0, y0, w, h, tail, light='left', base=(200, 176, 90),
              outline=None):
    """
    画一枚水滴形籽粒。

    (x0, y0) 绘图区左上角，(w, h) 绘图区尺寸。
    tail: 'left'/'right'/'down-left'/'down-right' —— 尖尾朝向
    light: 'left'/'right' —— 迎光侧，决定亮面与高光位置
    """
    if outline is None:
        outline = shade(base, 0.55)
    hi = shade(base, 1.24)
    mid = base
    lo = shade(base, 0.72)
    spec = shade(base, 1.45)

    cx_t = w / 2.0           # 圆头中心（横向）
    cy_t = h * 0.34          # 圆头中心（纵向）
    rad = min(w, h * 0.9) / 2.0

    # 尖尾落点
    tail_pt = {
        'left':       (-0.05 * w, h * 0.80),
        'right':      (1.05 * w,  h * 0.80),
        'down-left':  (0.20 * w,  h * 1.02),
        'down-right': (0.80 * w,  h * 1.02),
    }[tail]

    # 逐行求该行水平跨度：圆头 + 向尖尾线性收窄
    for y in range(h):
        # 该行在"圆头→尖尾"这条轴上的参数 t
        denom = (tail_pt[1] - cy_t)
        t = (y - cy_t) / denom if abs(denom) > 1e-6 else 0.0
        t = max(0.0, min(1.0, t * 0.5 + 0.5)) if abs(denom) > 1e-6 else 0.5
        # 重新映射：y=圆头处 t→0，y=尖尾处 t→1
        t = (y - cy_t) / denom if abs(denom) > 1e-6 else 0.5
        t = float(max(-0.6, min(1.5, t)))

        # 轴心横坐标：从圆头中心线性移到尖尾
        ax = cx_t + (tail_pt[0] - cx_t) * max(0.0, t)

        # 半径：圆头处最大，往尖尾收窄到 0.5
        if t <= 0.0:
            r = rad * (1.0 - (t ** 2) * 0.6)      # 圆头往上仍略带弧
        else:
            r = max(0.5, rad * (1.0 - t ** 1.35))
        if r < 0.5:
            continue

        x_from = int(round(ax - r))
        x_to = int(round(ax + r))
        for dx in range(x_from, x_to + 1):
            X, Y = x0 + dx, y0 + y
            if not (0 <= X < W and 0 <= Y < H):
                continue
            if dx == x_from or dx == x_to or abs(dx - ax) > r - 0.6:
                px[X, Y] = outline + (255,)
                continue
            # 明暗：迎光侧亮，背光侧暗
            f = (dx - (ax - r)) / max(1e-6, 2 * r)   # 0 左 → 1 右
            lit = f if light == 'right' else (1.0 - f)
            if lit > 0.70:
                c = hi
            elif lit < 0.32:
                c = lo
            else:
                c = mid
            px[X, Y] = c + (255,)

    # 高光：圆头迎光侧的一小片
    hx = x0 + (int(cx_t + rad * 0.30) if light == 'right' else int(cx_t - rad * 0.65))
    hy = y0 + int(cy_t - rad * 0.45)
    for dx in range(2):
        for dy in range(3):
            X, Y = hx + dx, hy + dy
            if 0 <= X < W and 0 <= Y < H and px[X, Y][3] > 0:
                px[X, Y] = spec + (255,)


def new_canvas():
    return Image.new('RGBA', (W, H), (0, 0, 0, 0))


# 排布：(x, y, w, h, tail, light)
LAYOUTS = {
    # 3 枚：大 + 中 + 小，错落分布 —— 最贴近辣椒种子
    'cluster3': [(10, 4, 10, 13, 'down-right', 'left'),
                 (2, 11, 9, 12, 'left',       'right'),
                 (15, 15, 9, 12, 'down-right', 'left')],
    # 4 枚：更密（番茄籽偏小偏多）
    'cluster4': [(11, 3, 9, 12, 'down-right', 'left'),
                 (2, 10, 8, 11, 'left',       'right'),
                 (15, 14, 8, 11, 'down-right', 'left'),
                 (6, 21, 7, 9,  'down-right', 'right')],
    # 3 枚细长型（麦类/生菜）
    'tall3':    [(11, 2, 8, 14, 'down-right', 'left'),
                 (2, 9, 7, 14,  'left',       'right'),
                 (16, 12, 8, 13, 'down-right', 'left')],
}


def build(name, base, layout, outline=None):
    im = new_canvas()
    px = im.load()
    for (x, y, w, h, tail, light) in LAYOUTS[layout]:
        draw_seed(px, x, y, w, h, tail, light, base, outline)
    im.save(name + '.png')
    print(f'  ✓ {name}.png  {W}x{H}  base={base}')


if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    # 各种子主色：取自各作物本体配色，整体向辣椒种子的金褐色调靠拢
    build('celery_seeds',      (198, 174, 108), 'cluster3')  # 芹菜籽：浅黄褐
    build('lablab_bean_seeds', (196, 174, 148), 'cluster3')  # 扁豆：灰米色
    build('lettuce_seeds',     (174, 160, 108), 'tall3')     # 生菜籽：土黄
    build('onion_seeds',       (172, 142,  86), 'cluster3')  # 洋葱籽：深褐
    build('ormosia_seeds',     (170,  98,  80), 'cluster3')  # 红豆：暗红
    build('pineapple_seed',    (190, 166, 104), 'cluster3')  # 菠萝籽：黄褐
    build('rye_seeds',         (168, 144, 110), 'tall3')     # 黑麦籽：灰褐
    build('tomato_seeds',      (208, 192, 146), 'cluster4')  # 番茄籽：浅米黄
