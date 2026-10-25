# -*- coding: utf-8 -*-
"""舰载机物品图标通用管线：原画降采样到 32×32 + 纯黑描边 + 底部状态元素。

原有的 dev_draw_barracuda_icon.py / dev_draw_ryusei_icon.py 是自包含的一次性
脚本；从流星之后的批量机型开始复用本模块，每架飞机的脚本只写参数：

    from aircraft_icon_pipeline import build
    build("/path/Equip_L_64.png", "ju87c", "payload")

kind:
  - "payload"：俯冲/鱼雷机，满油态底部放航空炸弹/鱼雷图标
  - "fighter"：战斗机，满油态底部放燃油表
空膛态一律是"本体逐像素一致 + 底部黄色警告三角"。
"""
from PIL import Image

W = H = 32
ALPHA_CUTOFF = 96
BASE = "src/main/resources/assets/piranport/textures/item"


def clear_reddish(im, rects):
    """清除原画里画在机体后方的红色排气/尾焰等插画效果。"""
    px = im.load()
    for x0, y0, x1, y1 in rects:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = px[x, y]
                if r > 140 and r > g + 25 and r > b + 25:
                    px[x, y] = (0, 0, 0, 0)


def _cutout(src, masks):
    im = Image.open(src).convert("RGBA")
    clear_reddish(im, masks)
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][3] < ALPHA_CUTOFF:
                px[x, y] = (0, 0, 0, 0)
    return im.crop(im.getbbox())


def _outline(im):
    px = im.load()
    solid = {(x, y) for y in range(H) for x in range(W) if px[x, y][3] > 0}
    edge = set()
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H and (nx, ny) not in solid:
                    edge.add((nx, ny))
    for (x, y) in edge:
        px[x, y] = (0, 0, 0, 255)
    return im


def _paste(im, name, region):
    src = Image.open(f"{BASE}/{name}.png").convert("RGBA")
    x0, y0, x1, y1 = region
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p = src.getpixel((x, y))
            if p[3] > 0:
                im.putpixel((x, y), p)


def build(src, name, kind, max_w=30, max_h=20, masks=()):
    """生成 <name>.png（满油/满弹）与 <name>_empty.png（空膛）。"""
    crop = _cutout(src, masks)
    scale = min(max_w / crop.width, max_h / crop.height)
    size = (max(1, round(crop.width * scale)), max(1, round(crop.height * scale)))
    small = crop.resize(size, Image.LANCZOS)

    # LANCZOS 边缘的半透明像素二值化，得到干净的像素画剪影
    px = small.load()
    for y in range(small.height):
        for x in range(small.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 128 else (0, 0, 0, 0)

    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    canvas.alpha_composite(small, ((W - size[0]) // 2, 3))
    plane = _outline(canvas)

    fed = plane.copy()
    if kind == "fighter":
        _paste(fed, "seafire", (9, 26, 20, 31))          # 燃油表
    else:
        _paste(fed, "level_bomber_squadron", (10, 26, 22, 30))  # 炸弹/鱼雷
    fed.save(f"{BASE}/{name}.png")

    empty = plane.copy()
    _paste(empty, "level_bomber_squadron_empty", (12, 24, 20, 31))  # 警告三角
    empty.save(f"{BASE}/{name}_empty.png")

    diff = sum(1 for y in range(24) for x in range(W)
               if fed.getpixel((x, y)) != empty.getpixel((x, y)))
    print(f"wrote {name}.png / {name}_empty.png  (body diff rows<24: {diff})")
