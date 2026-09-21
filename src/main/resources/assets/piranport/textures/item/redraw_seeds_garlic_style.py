"""
按大蒜种子画风重画所有种子贴图。

大蒜种子画风要素（参考 garlic_seeds.png 实测）：
  - 32×32 画布，主体只占中间一条带（bbox 约 x:5..25, y:9..21），四周留白
  - 每个形体 1px 纯黑描边 (#000000)
  - 主体用 3 色阶填充：亮面占大部分、中间色窄过渡带、暗面仅最外一圈
  - 2~3 个形体横向错落，相邻形体互相咬合成「一坨带分瓣轮廓」的剪影，
    而不是彼此分离的小点

本脚本按上述「结构」重画每种种子，但保留各种子原本的色相身份，
使彼此仍可辨识。籽粒形状按作物各自特征给出（椭圆 / 长粒 / 水滴 / 扁圆）。
"""
from PIL import Image, ImageDraw

S = 32

# 每种种子：色阶（亮, 中, 暗）+ 形体列表
# 形体 = (cx, cy, rx, ry, shape)  shape: 'ellipse' | 'oval' | 'teardrop'
#
# 横向半径按大蒜比例收紧过一轮：大蒜 bbox 宽 21px，
# 早期版本铺到 29px 会明显比参考「胖一圈」。
SEEDS = {
    # 芹菜籽：细长椭圆，偏黄绿
    'celery_seeds': dict(
        colors=('#f6ff9c', '#dce9a0', '#7d8a4a'),
        forms=[(10.0, 15.5, 2.8, 2.8, 'ellipse'),
               (17.0, 14.0, 3.1, 3.0, 'ellipse'),
               (23.5, 16.5, 2.6, 2.6, 'ellipse')]),

    # 辣椒籽：扁圆偏方，米黄
    'chili_seeds': dict(
        colors=('#ffe9a0', '#e3cd88', '#8f7a33'),
        forms=[(10.5, 15.0, 3.0, 3.6, 'ellipse'),
               (17.5, 14.5, 3.1, 3.7, 'ellipse'),
               (24.0, 16.0, 2.7, 3.3, 'ellipse')]),

    # 扁豆籽：肾形，紫褐
    'lablab_bean_seeds': dict(
        colors=('#cbbcd3', '#a396ac', '#4d4750'),
        forms=[(10.0, 15.5, 3.0, 3.6, 'ellipse'),
               (17.5, 14.5, 3.1, 3.7, 'ellipse'),
               (24.5, 16.0, 2.8, 3.4, 'ellipse')]),

    # 生菜籽：细长，深绿
    'lettuce_seeds': dict(
        colors=('#bfd679', '#98a962', '#48512e'),
        forms=[(9.5, 15.5, 2.5, 3.6, 'oval'),
               (16.5, 14.0, 2.6, 3.8, 'oval'),
               (23.5, 16.5, 2.3, 3.4, 'oval')]),

    # 洋葱籽：水滴/三角，琥珀
    'onion_seeds': dict(
        colors=('#f9cd7c', '#d8ad68', '#6f5a33'),
        forms=[(10.0, 16.0, 2.6, 3.6, 'teardrop'),
               (17.0, 14.5, 2.8, 3.8, 'teardrop'),
               (24.0, 16.5, 2.5, 3.4, 'teardrop')]),

    # 红豆籽：圆润饱满，砖红
    'ormosia_seeds': dict(
        colors=('#f68e74', '#cc7a64', '#5d352c'),
        forms=[(10.0, 15.5, 3.0, 3.6, 'ellipse'),
               (17.5, 14.5, 3.1, 3.8, 'ellipse'),
               (24.5, 16.0, 2.8, 3.4, 'ellipse')]),

    # 菠萝种：粗短块状，橙黄
    'pineapple_seed': dict(
        colors=('#ffdc59', '#edb44c', '#7a5624'),
        forms=[(10.0, 15.5, 3.0, 3.8, 'ellipse'),
               (17.5, 14.5, 3.2, 3.9, 'ellipse'),
               (24.5, 16.0, 2.7, 3.4, 'ellipse')]),

    # 稻种：细长米粒，浅黄（4 粒，比其他种子多一粒以免同系混淆）
    'rice_seeds': dict(
        colors=('#e8e2a8', '#cfc274', '#7c7233'),
        forms=[(8.5, 16.0, 2.2, 3.7, 'oval'),
               (14.5, 14.5, 2.3, 3.9, 'oval'),
               (20.5, 16.5, 2.2, 3.5, 'oval'),
               (26.0, 15.0, 1.8, 3.1, 'oval')]),

    # 黑麦种：窄长灰褐
    'rye_seeds': dict(
        colors=('#d3c8bf', '#b0a8a2', '#504b48'),
        forms=[(9.5, 15.5, 2.3, 3.7, 'oval'),
               (16.5, 14.5, 2.5, 3.9, 'oval'),
               (23.5, 16.0, 2.2, 3.5, 'oval')]),

    # 大豆籽：饱满圆球，土黄
    'soybean_seeds': dict(
        colors=('#e8d292', '#c9a751', '#6c5222'),
        forms=[(10.0, 15.0, 3.0, 3.7, 'ellipse'),
               (17.5, 14.0, 3.2, 3.9, 'ellipse'),
               (24.5, 15.8, 2.8, 3.4, 'ellipse')]),

    # 番茄籽：扁水滴，米赭
    'tomato_seeds': dict(
        colors=('#ffeec0', '#e0c091', '#766146'),
        forms=[(10.0, 15.5, 2.6, 3.2, 'teardrop'),
               (17.0, 14.5, 2.8, 3.4, 'teardrop'),
               (24.0, 16.0, 2.5, 3.0, 'teardrop')]),
}


def form_mask(shape, cx, cy, rx, ry):
    """在 4 倍超采样画布上画形体，降采样得到抗锯齿后的布尔轮廓。"""
    k = 4
    m = Image.new('L', (S * k, S * k), 0)
    d = ImageDraw.Draw(m)
    X, Y, RX, RY = cx * k, cy * k, rx * k, ry * k

    if shape in ('ellipse', 'oval'):
        d.ellipse([X - RX, Y - RY, X + RX, Y + RY], fill=255)
    elif shape == 'teardrop':
        # 上宽下尖：椭圆主体 + 底部收成尖角
        d.ellipse([X - RX, Y - RY, X + RX, Y + RY * 0.25], fill=255)
        d.polygon([(X - RX, Y + RY * 0.1), (X + RX, Y + RY * 0.1), (X, Y + RY)], fill=255)

    return m.resize((S, S), Image.LANCZOS).point(lambda v: 255 if v >= 128 else 0)


def erode(im):
    """4 邻域腐蚀 1px。"""
    out = Image.new('L', (S, S), 0)
    o, p = out.load(), im.load()
    for y in range(S):
        for x in range(S):
            if p[x, y] and all(
                0 <= x + dx < S and 0 <= y + dy < S and p[x + dx, y + dy]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
            ):
                o[x, y] = 255
    return out


def shaded(mask, px, light, mid, dark):
    """
    三色阶填充。大蒜实测配比是「亮面占主体、中间色只做窄过渡」，
    所以亮面取内缩 1px 以内；若按内缩 3px 取亮面，
    中小形体几乎全被中间色填满，整体发暗、失去大蒜那种奶白感。
    """
    m1 = erode(mask)   # 内缩 1px
    m2 = erode(m1)     # 内缩 2px

    p0, p1, p2 = mask.load(), m1.load(), m2.load()
    for y in range(S):
        for x in range(S):
            if not p0[x, y]:
                continue
            if p1[x, y]:
                # 形体够大（内缩 2px 仍在）才铺中间色，
                # 太小的籽粒直接用亮面，避免整颗变暗。
                px[x, y] = mid if p2[x, y] else light
            else:
                px[x, y] = dark


def outline(mask, px):
    """形体外部一圈补纯黑描边；已被其他形体占用的像素不动。"""
    p = mask.load()
    for y in range(S):
        for x in range(S):
            if p[x, y]:
                continue
            near = any(
                0 <= x + dx < S and 0 <= y + dy < S and p[x + dx, y + dy]
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
            )
            if near and px[x, y][3] == 0:
                px[x, y] = (0, 0, 0, 255)


def build(spec):
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = img.load()

    def rgb(h):
        return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)

    light, mid, dark = (rgb(c) for c in spec['colors'])

    # 按 cy 从后往前画，让靠前的形体压住靠后的，形成层叠剪影
    masks = []
    for cx, cy, rx, ry, shape in sorted(spec['forms'], key=lambda f: f[1]):
        m = form_mask(shape, cx, cy, rx, ry)
        masks.append(m)
        shaded(m, px, light, mid, dark)

    for m in masks:
        outline(m, px)
    return img


for name, spec in SEEDS.items():
    build(spec).save(name + '.png')
    print('wrote', name + '.png')
