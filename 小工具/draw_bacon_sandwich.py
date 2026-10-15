"""画培根三明治 32x32 贴图 v2
两片方吐司水平叠放（非对角切），夹心从侧面露出：培根 + 生菜。
参考 Farmer's Delight 的 bacon_sandwich 构图。
"""
from PIL import Image
import os

W = 32
OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources",
    "assets", "piranport", "textures", "item", "bacon_sandwich.png"))

CRUST = (200, 150, 80, 255)
CRUST_D = (165, 112, 55, 255)
CRUMB = (245, 215, 150, 255)
CRUMB_L = (255, 233, 178, 255)
OUTLINE = (35, 25, 18, 255)
BACON_M = (175, 58, 48, 255)
BACON_F = (242, 205, 185, 255)
LETTUCE = (122, 186, 82, 255)
LETTUCE_D = (86, 146, 56, 255)
TOMATO = (220, 90, 70, 255)

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
px = img.load()


def toast_slice(x0, y0, w, h, crust, crumb, highlight):
    """一个方吐司片，顶部带圆弧"""
    for y in range(y0, y0 + h):
        rel = y - y0
        inset = 0
        if rel == 0:
            inset = 3
        elif rel == 1:
            inset = 1
        for x in range(x0 + inset, x0 + w - inset):
            px[x, y] = crust
    # 瓤
    for y in range(y0 + 1, y0 + h - 1):
        for x in range(x0 + 1, x0 + w - 1):
            if px[x, y] == crust:
                px[x, y] = crumb
    # 顶部高光
    if highlight:
        for y in range(y0 + 1, y0 + 4):
            for x in range(x0 + 3, x0 + w - 3):
                if px[x, y] == crumb:
                    px[x, y] = CRUMB_L


# ===== 下片吐司 =====
toast_slice(5, 19, 22, 9, CRUST_D, CRUMB, False)

# ===== 夹心层：y=11..19 =====
# 生菜：顶缘锯齿
for x in range(4, 28):
    for y in range(12, 14):
        px[x, y] = LETTUCE
for x in range(4, 28, 3):
    px[x, 11] = LETTUCE
    if x + 1 < W:
        px[x + 1, 11] = LETTUCE_D

# 培根两条（略缩短，两端不外露太多）
for x in range(5, 27):
    px[x, 14] = BACON_M
    px[x, 15] = BACON_M if x % 4 < 2 else BACON_F
    px[x, 16] = BACON_F if x % 4 < 2 else BACON_M
    px[x, 17] = BACON_M
    px[x, 18] = BACON_M

# 番茄片：左侧露一小块
for y in range(15, 19):
    for x in range(3, 5):
        px[x, y] = TOMATO

# ===== 上片吐司 =====
toast_slice(5, 3, 22, 10, CRUST, CRUMB, True)

# ===== 黑描边 =====
for y in range(W):
    for x in range(W):
        if px[x, y][3] == 0:
            continue
        # 是边界像素则描边
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, ny = x + dx, y + dy
            if nx < 0 or ny < 0 or nx >= W or ny >= W or px[nx, ny][3] == 0:
                px[x, y] = OUTLINE
                break

img.save(OUT)
img.resize((256, 256), Image.NEAREST).save("/tmp/preview_bacon_sandwich.png")
print("saved", OUT)
