# -*- coding: utf-8 -*-
"""彩云舰侦贴图 — 按 zjsnrwiki 原画 Equip_L_142 像素化到 32×32，只加最外侧黑描边。
原画：中岛 C6N1 彩云，俯视，细长机身 + 大展弦比机翼，深绿迷彩，红日之丸。"""
from PIL import Image, ImageDraw

W = H = 32

# 调色板（取自原画，略提饱和以适应 32px）
GREEN_L = (108, 152, 118)
GREEN_M = (78, 122, 96)
GREEN_D = (48, 84, 62)
GRAY_D  = (70, 76, 74)      # 座舱/金属细节
RED     = (206, 68, 60)     # 日之丸
BLACK   = (0, 0, 0)
CREAM   = (214, 208, 194)   # 机腹浅色/螺旋桨

img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# 机翼：从机身向两侧后掠，翼展约 30px，弦长中段 5px
# 右翼
wing = [(14,11),(30,14),(30,18),(14,16)]
d.polygon(wing, fill=GREEN_M)
d.polygon([(14,11),(24,12),(30,14),(30,15),(14,13)], fill=GREEN_L)
# 左翼（镜像）
d.polygon([(W-1-x, y) for x, y in wing], fill=GREEN_M)
d.polygon([(W-1-x, y) for x, y in [(14,11),(24,12),(30,14),(30,15),(14,13)]], fill=GREEN_L)

# 机身：细长，宽 4px，从机首 y=3 到机尾 y=25
d.polygon([(14,3),(17,3),(18,25),(13,25)], fill=GREEN_M)
d.polygon([(14,3),(16,3),(16,25),(14,25)], fill=GREEN_L)

# 水平尾翼
d.polygon([(14,22),(8,23),(8,24),(14,25)], fill=GREEN_M)
d.polygon([(W-1-14,22),(W-1-8,23),(W-1-8,24),(W-1-14,25)], fill=GREEN_M)
d.polygon([(14,22),(10,23),(10,24),(14,25)], fill=GREEN_L)
d.polygon([(W-1-14,22),(W-1-10,23),(W-1-10,24),(W-1-14,25)], fill=GREEN_L)

# 垂尾（机尾深绿）+ 座舱（机身中前段金属灰）
d.rectangle([14,20,17,24], fill=GREEN_D)
d.rectangle([14,9,17,12], fill=GRAY_D)


# 日之丸：机翼中段各一个红点
d.rectangle([19,15,20,16], fill=RED)
d.rectangle([W-1-20,15,W-1-19,16], fill=RED)

# 螺旋桨盘
d.rectangle([15,1,16,1], fill=CREAM)
d.rectangle([13,0,18,0], fill=CREAM)

# 最外侧黑描边：所有不透明像素的 4 邻接透明位补黑
px = img.load()
opaque = {(x, y) for y in range(H) for x in range(W) if px[x, y][3] > 0}
for (x, y) in opaque:
    for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
        nx, ny = x+dx, y+dy
        if 0 <= nx < W and 0 <= ny < H and px[nx, ny][3] == 0:
            px[nx, ny] = BLACK + (255,)

OUT='src/main/resources/assets/piranport/textures/item/'
img.save(OUT+'saiun_recon.png')

# 空弹版：与已加燃料版构图完全一致，仅把红色日之丸换成亮黄（无弹药指示）
empty = img.copy()
ep = empty.load()
for y in range(H):
    for x in range(W):
        if ep[x, y][:3] == RED:
            ep[x, y] = (255, 248, 0, 255)
empty.save(OUT+'saiun_recon_empty.png')
print('saved')
