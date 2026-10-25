# -*- coding: utf-8 -*-
"""天河物品贴图 — 沿用主炮的「原画降采样 + 纯黑描边」管线。

原画：zjsnrwiki 文件 Equip_L_238.png（512×512），天河双发陆攻，
3/4 侧俯视 —— 深绿机体、浅灰蓝机腹、双发动机短舱、
黄色翼根前缘识别带、翼尖与后机身白框橙芯方标。

两态规范与同级鱼雷机（seiun_kai_bomber / level_bomber_squadron）一致：
满油态 = 本体 + 底部鱼雷；空膛态 = 本体逐像素一致 + 底部黄色警告三角。
底部元素直接从现成贴图逐像素复制，不手画。

天河是双发轰炸机，原画剪影很宽（439×236）；机身区限高 20 行，
给底部鱼雷留位置，等比缩完约 30×16。
"""
from PIL import Image

SRC = "/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_238.png"
BASE = "src/main/resources/assets/piranport/textures/item"
W = H = 32

# 原画里签名水印是很低的 alpha，先滤掉再裁切，否则降采样会留下灰霾
ALPHA_CUTOFF = 96


def mask_exhaust_glow(im):
    """原画两台发动机后方画了红色排气尾焰（插画效果，不是机体）。

    不清掉的话降采样后会在机翼/机尾留下两块红斑，和同级飞机图标不一致。
    只在这两条尾焰带里删"偏红"像素，翼尖/机身的橙色方标在带外，不会被误伤。
    """
    px = im.load()
    for x0, y0, x1, y1 in [(200, 215, 330, 255), (80, 300, 250, 340)]:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = px[x, y]
                if r > 140 and r > g + 25 and r > b + 25:
                    px[x, y] = (0, 0, 0, 0)


def load_cutout():
    im = Image.open(SRC).convert("RGBA")
    mask_exhaust_glow(im)
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][3] < ALPHA_CUTOFF:
                px[x, y] = (0, 0, 0, 0)
    return im.crop(im.getbbox())


def fit_plane():
    """把原画等比缩进 30×22 的机身区（底部 4 行留给鱼雷/警告三角）。"""
    crop = load_cutout()
    max_w, max_h = 30, 20
    scale = min(max_w / crop.width, max_h / crop.height)
    size = (round(crop.width * scale), round(crop.height * scale))
    small = crop.resize(size, Image.LANCZOS)

    # LANCZOS 会在边缘留下半透明像素：二值化 alpha，得到干净的像素画剪影
    px = small.load()
    for y in range(small.height):
        for x in range(small.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 128 else (0, 0, 0, 0)

    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    canvas.alpha_composite(small, ((W - size[0]) // 2, 3))
    return canvas


def outline(im):
    """最外圈八邻域补纯黑描边，不改动已有像素。"""
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


def paste_element(im, name, region):
    """逐像素复制同级贴图的底部元素（鱼雷 / 警告三角）。"""
    src = Image.open(f"{BASE}/{name}.png").convert("RGBA")
    x0, y0, x1, y1 = region
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p = src.getpixel((x, y))
            if p[3] > 0:
                im.putpixel((x, y), p)


def main():
    plane = outline(fit_plane())

    fed = plane.copy()
    paste_element(fed, "level_bomber_squadron", (10, 26, 22, 30))
    fed.save(f"{BASE}/tenga.png")

    empty = plane.copy()
    paste_element(empty, "level_bomber_squadron_empty", (12, 24, 20, 31))
    empty.save(f"{BASE}/tenga_empty.png")

    # 自检：两态本体在 y<24 区域逐像素一致
    diff = [(x, y) for y in range(24) for x in range(W)
            if fed.getpixel((x, y)) != empty.getpixel((x, y))]
    print("body diff:", diff)
    print("wrote tenga.png / tenga_empty.png")


if __name__ == "__main__":
    main()
