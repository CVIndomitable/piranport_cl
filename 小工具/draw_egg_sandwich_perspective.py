"""画鸡蛋三明治 32x32 贴图 v3（3/4 俯视透视，对齐 american_burger）
构图：方形白吐司，俯视能看到顶面；鸡蛋沙拉夹心从四边探出；
底部吐司只露出一条窄边（透视收缩）。
"""
from PIL import Image
import os

W = 32
OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item", "egg_sandwich.png"))

# 面包（白吐司）
CRUST = (196, 143, 74, 255)      # 面包边（顶面烤色）
CRUST_D = (158, 104, 48, 255)    # 面包边暗部/侧面
CRUMB = (246, 218, 158, 255)     # 吐司瓤
CRUMB_L = (255, 236, 186, 255)   # 瓤高光
CRUMB_PORE = (226, 186, 120, 255)  # 气孔
OUTLINE = (35, 25, 18, 255)

# 鸡蛋沙拉
EGG_Y = (248, 205, 88, 255)
EGG_Y_L = (255, 229, 138, 255)
EGG_Y_D = (210, 160, 50, 255)
EGG_SIDE = (228, 182, 72, 255)   # 侧面探出部稍暗
EGG_WHITE = (250, 244, 226, 255)
YOLK = (243, 172, 48, 255)

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
px = img.load()


def rr(x0, y0, x1, y1, r):
    """圆角矩形 mask（含端点），r=圆角半径"""
    m = [[False] * W for _ in range(W)]
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cx = min(max(x, x0 + r), x1 - r)
            cy = min(max(y, y0 + r), y1 - r)
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 1:
                m[y][x] = True
    return m


# 形状布局（俯视）：
#   上片吐司顶面：y4..17，占视觉主体
#   沙拉层探出：y13..21（比吐司宽，四角和两边超出）
#   下片吐司：y19..27，只露窄边
bottom = rr(6, 19, 25, 27, 3)
egg    = rr(2, 13, 29, 21, 4)
top    = rr(3, 4, 28, 18, 3)


# ---------- 绘制 ----------
# 1) 下片吐司：整体皮色侧面 + 内部瓤窄条
for y in range(W):
    for x in range(W):
        if bottom[y][x]:
            px[x, y] = CRUST_D
for y in range(21, 26):
    for x in range(8, 23):
        if bottom[y][x] and bottom[y - 1][x] and bottom[y + 1][x]:
            px[x, y] = CRUMB

# 2) 鸡蛋沙拉层
for y in range(W):
    for x in range(W):
        if egg[y][x]:
            px[x, y] = EGG_Y
# 上缘亮、下缘暗
for y in range(13, 21):
    for x in range(W):
        if egg[y][x]:
            if not egg[y - 1][x]:
                px[x, y] = EGG_Y_L
            if y + 1 >= W or not egg[y + 1][x]:
                px[x, y] = EGG_Y_D
# 探出吐司外的侧面区稍暗
for y in range(W):
    for x in range(W):
        if egg[y][x] and not top[y][x] and not bottom[y][x] and px[x, y] == EGG_Y:
            px[x, y] = EGG_SIDE
# 蛋黄块 / 蛋白块
for (x, y) in [(6, 16), (10, 17), (14, 16), (18, 17), (22, 16), (26, 17),
               (8, 18), (12, 19), (16, 18), (20, 19), (24, 18)]:
    if egg[y][x]:
        px[x, y] = YOLK
for (x, y) in [(7, 15), (12, 17), (17, 15), (21, 15), (25, 16),
               (9, 18), (15, 19), (19, 18), (23, 19)]:
    if egg[y][x]:
        px[x, y] = EGG_WHITE

# 3) 上片吐司：侧面皮色 + 顶面瓤
for y in range(W):
    for x in range(W):
        if top[y][x]:
            px[x, y] = CRUST
# 顶面瓤：y4..15（前缘 y16..18 留侧面皮色）
for y in range(4, 16):
    for x in range(W):
        if top[y][x]:
            px[x, y] = CRUMB
# 后缘高光带
for y in range(4, 9):
    for x in range(W):
        if top[y][x] and px[x, y] == CRUMB:
            px[x, y] = CRUMB_L
# 气孔点缀
for (x, y) in [(9, 9), (14, 7), (19, 10), (12, 12), (22, 8), (8, 11),
               (16, 13), (24, 11)]:
    if top[y][x] and px[x, y] in (CRUMB, CRUMB_L):
        px[x, y] = CRUMB_PORE
# 前缘侧面皮色加深
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
img.resize((256, 256), Image.NEAREST).save("/tmp/preview_egg_sandwich.png")
print("saved", OUT)
