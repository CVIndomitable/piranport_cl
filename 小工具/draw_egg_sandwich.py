"""画鸡蛋三明治 32x32 贴图
鸡蛋沙拉三明治：双层吐司 + 黄色鸡蛋沙拉酱（蛋黄碎块点缀）。
"""
from PIL import Image
import os

W = 32
OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item", "egg_sandwich.png"))

CRUST = (200, 150, 80, 255)
CRUST_D = (165, 112, 55, 255)
CRUMB = (245, 215, 150, 255)
CRUMB_L = (255, 233, 178, 255)
OUTLINE = (35, 25, 18, 255)
EGG_Y = (248, 208, 92, 255)      # 鸡蛋沙拉主色
EGG_Y_L = (255, 228, 140, 255)   # 亮
EGG_Y_D = (214, 165, 55, 255)    # 暗
EGG_WHITE = (252, 246, 230, 255) # 蛋白块
YOLK = (245, 175, 50, 255)       # 蛋黄浓块

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
px = img.load()


def toast_slice(x0, y0, w, h, crust, crumb, highlight):
    for y in range(y0, y0 + h):
        rel = y - y0
        inset = 0
        if rel == 0:
            inset = 3
        elif rel == 1:
            inset = 1
        for x in range(x0 + inset, x0 + w - inset):
            px[x, y] = crust
    for y in range(y0 + 1, y0 + h - 1):
        for x in range(x0 + 1, x0 + w - 1):
            if px[x, y] == crust:
                px[x, y] = crumb
    if highlight:
        for y in range(y0 + 1, y0 + 4):
            for x in range(x0 + 3, x0 + w - 3):
                if px[x, y] == crumb:
                    px[x, y] = CRUMB_L


# 下片吐司
toast_slice(5, 19, 22, 9, CRUST_D, CRUMB, False)

# ===== 鸡蛋沙拉夹心层 y=12..19 =====
# 主酱体（波浪状填满，上下略有起伏）
for x in range(4, 28):
    for y in range(12, 19):
        px[x, y] = EGG_Y
# 顶部不规则波浪
for x in range(4, 28):
    if x % 6 < 3:
        px[x, 11] = EGG_Y
        if x + 1 < W:
            px[x + 1, 11] = EGG_Y_L
    else:
        px[x, 11] = EGG_Y_D
# 沙拉碎块点缀：蛋黄浓块 + 蛋白块
for (x, y) in [(7, 14), (12, 16), (17, 13), (22, 15), (9, 17), (20, 17), (14, 14), (25, 13)]:
    px[x, y] = YOLK
for (x, y) in [(6, 16), (11, 13), (16, 17), (21, 14), (24, 16), (8, 15), (18, 15), (13, 17)]:
    px[x, y] = EGG_WHITE
# 夹心层两侧溢出不规则
px[3, 14] = EGG_Y; px[3, 15] = EGG_Y_D; px[4, 15] = EGG_Y_L
px[28, 13] = EGG_Y; px[28, 14] = EGG_Y; px[27, 15] = EGG_Y_D

# 上片吐司
toast_slice(5, 3, 22, 10, CRUST, CRUMB, True)

# 黑描边
for y in range(W):
    for x in range(W):
        if px[x, y][3] == 0:
            continue
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, ny = x + dx, y + dy
            if nx < 0 or ny < 0 or nx >= W or ny >= W or px[nx, ny][3] == 0:
                px[x, y] = OUTLINE
                break

img.save(OUT)
img.resize((256, 256), Image.NEAREST).save("/tmp/preview_egg_sandwich.png")
print("saved", OUT)
