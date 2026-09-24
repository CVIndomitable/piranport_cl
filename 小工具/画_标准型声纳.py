# -*- coding: utf-8 -*-
"""标准型声纳贴图重画 — 按 zjsnrwiki 原画 Equip_L_83 像素化到 32×32。

原画: https://www.zjsnrwiki.com/wiki/标准型声纳
      Equip_L_83.png（512×512，缓存于 ~/IndomitableCache/zjsnrwiki原画/）
构图: 灰色声纳盒（顶面右上红按钮）+ 挂式耳机（左上耳罩、右侧耳罩没入盒后）
      + 线缆左侧小圈、盒底大圈 — 全部对齐原画，不改构图
管线: 裁内容包围盒 → LANCZOS 降采样 → alpha 二值化去脏边 → 量化 12 色
输出: src/main/resources/assets/piranport/textures/item/standard_sonar.png (32×32)
"""

from PIL import Image

SRC = '/Users/lianran/IndomitableCache/zjsnrwiki原画/Equip_L_83.png'
DST = ('/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/'
       'assets/piranport/textures/item/standard_sonar.png')

# 原画内容包围盒（alpha 非空区域），留一点余量
CROP = (126, 185, 382, 384)
ALPHA_THRESHOLD = 110  # 半透明脏边一刀切，避免 32×32 下发虚
COLORS = 12            # 量化色数，贴近原画调色板又不糊


def build():
    im = Image.open(SRC).convert('RGBA')
    crop = im.crop(CROP)

    # 等比缩进 30×30，居中留 1px 呼吸边
    scale = min(30 / crop.width, 30 / crop.height)
    nw, nh = round(crop.width * scale), round(crop.height * scale)
    small = crop.resize((nw, nh), Image.LANCZOS)

    # alpha 二值化（LANCZOS 会拉出半透明边，32×32 下必须切干净）
    px = small.load()
    for y in range(nh):
        for x in range(nw):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255 if a >= ALPHA_THRESHOLD else 0)

    # 量化去 LANCZOS 杂色，再套回二值 alpha
    pal = small.convert('P', palette=Image.ADAPTIVE, colors=COLORS).convert('RGBA')
    p2 = pal.load()
    for y in range(nh):
        for x in range(nw):
            r, g, b, _ = p2[x, y]
            p2[x, y] = (r, g, b, px[x, y][3])

    canvas = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    canvas.paste(pal, ((32 - nw) // 2, (32 - nh) // 2))
    return canvas


def main():
    im = build()
    im.save(DST)
    # 预览：白底 + 彩色角标（防读图缓存混淆）
    from PIL import ImageDraw
    bg = Image.new('RGBA', (32, 32), (255, 255, 255, 255))
    bg.alpha_composite(im)
    out = bg.convert('RGB').resize((512, 512), Image.NEAREST)
    d = ImageDraw.Draw(out)
    d.rectangle([0, 0, 40, 40], fill=(255, 0, 0))
    d.rectangle([471, 0, 511, 40], fill=(0, 200, 0))
    d.rectangle([0, 471, 40, 511], fill=(0, 0, 255))
    out.save('/tmp/standard_sonar_preview.png')
    print('saved', DST)


if __name__ == '__main__':
    main()
