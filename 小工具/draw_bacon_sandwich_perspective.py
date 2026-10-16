"""画培根三明治 32x32 贴图 v3（3/4 俯视透视，对齐 american_burger）
构图：方形白吐司俯视顶面；生菜+培根+番茄夹心从四边探出；
底部吐司只露窄边。
"""
from PIL import Image
import os

W = 32
OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item", "bacon_sandwich.png"))

CRUST = (196, 143, 74, 255)
CRUST_D = (158, 104, 48, 255)
CRUMB = (246, 218, 158, 255)
CRUMB_L = (255, 236, 186, 255)
CRUMB_PORE = (226, 186, 120, 255)
OUTLINE = (35, 25, 18, 255)

BACON = (196, 68, 52, 255)       # 培根瘦肉（提亮）
BACON_F = (248, 214, 194, 255)   # 培根肥边
BACON_D = (150, 44, 34, 255)     # 培根暗
LETTUCE = (120, 186, 80, 255)
LETTUCE_L = (152, 210, 108, 255)
LETTUCE_D = (84, 144, 54, 255)
TOMATO = (214, 82, 60, 255)
TOMATO_D = (170, 54, 40, 255)

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
px = img.load()


def rr(x0, y0, x1, y1, r):
    m = [[False] * W for _ in range(W)]
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cx = min(max(x, x0 + r), x1 - r)
            cy = min(max(y, y0 + r), y1 - r)
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 1:
                m[y][x] = True
    return m


# 布局同 egg_sandwich
bottom = rr(6, 19, 25, 27, 3)
fill   = rr(2, 13, 29, 21, 4)   # 馅料层外包络
top    = rr(3, 4, 28, 18, 3)


# ---------- 绘制 ----------
# 1) 下片吐司
for y in range(W):
    for x in range(W):
        if bottom[y][x]:
            px[x, y] = CRUST_D
for y in range(21, 26):
    for x in range(8, 23):
        if bottom[y][x] and bottom[y - 1][x] and bottom[y + 1][x]:
            px[x, y] = CRUMB

# 2) 馅料层：先铺生菜底，再叠培根条，番茄从左侧探出
# 生菜层：填充馅料区
for y in range(W):
    for x in range(W):
        if fill[y][x]:
            px[x, y] = LETTUCE
# 生菜上缘亮、下缘暗
for y in range(13, 21):
    for x in range(W):
        if fill[y][x]:
            if not fill[y - 1][x]:
                px[x, y] = LETTUCE_L
            if y + 1 >= W or not fill[y + 1][x]:
                px[x, y] = LETTUCE_D
# 生菜叶缘波浪锯齿（顶部探出处）
for x in range(2, 30, 3):
    if fill[13][x]:
        px[x, 13] = LETTUCE_D
        if x + 1 < W and fill[13][x + 1]:
            px[x + 1, 13] = LETTUCE_L

# 培根：铺在馅料层中段（y15..18），两条培根各=红+白脂肪条纹
# 第一条培根 y15..16：红底 + y16 中央白条
for x in range(4, 28):
    if fill[15][x]:
        px[x, 15] = BACON
    if fill[16][x]:
        px[x, 16] = BACON_F
# 第二条培根 y17..18：红底 + y18 中央白条
for x in range(4, 28):
    if fill[17][x]:
        px[x, 17] = BACON
    if fill[18][x]:
        px[x, 18] = BACON_F
# 培根两端收成生菜角
for ry in (15, 16, 17, 18):
    for x in (4, 5, 26, 27):
        if fill[ry][x]:
            px[x, ry] = LETTUCE if (x + ry) % 2 else LETTUCE_D

# 番茄片：左下缘探出半圆
tomato = rr(1, 15, 6, 21, 2)
for y in range(15, 22):
    for x in range(1, 7):
        if tomato[y][x]:
            px[x, y] = TOMATO
# 番茄内圈暗心
for y in range(16, 20):
    for x in range(2, 5):
        if tomato[y][x]:
            px[x, y] = TOMATO_D

# 3) 上片吐司
for y in range(W):
    for x in range(W):
        if top[y][x]:
            px[x, y] = CRUST
for y in range(4, 16):
    for x in range(W):
        if top[y][x]:
            px[x, y] = CRUMB
for y in range(4, 9):
    for x in range(W):
        if top[y][x] and px[x, y] == CRUMB:
            px[x, y] = CRUMB_L
for (x, y) in [(9, 9), (14, 7), (19, 10), (12, 12), (22, 8), (8, 11),
               (16, 13), (24, 11)]:
    if top[y][x] and px[x, y] in (CRUMB, CRUMB_L):
        px[x, y] = CRUMB_PORE
for y in range(16, 19):
    for x in range(W):
        if top[y][x]:
            px[x, y] = CRUST_D

# ---------- 描边 ----------
sil = [[(px[x, y][3] > 0) for x in range(W)] for y in range(W)]
for y in range(W):
    for x in range(W):
        if not sil[y][x]:
            continue
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if nx < 0 or ny < 0 or nx >= W or ny >= W or not sil[ny][nx]:
                px[x, y] = OUTLINE
                break

img.save(OUT)
img.resize((256, 256), Image.NEAREST).save("/tmp/preview_bacon_sandwich.png")
print("saved", OUT)
