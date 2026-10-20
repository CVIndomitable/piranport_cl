"""生成 17 座自动近防炮的物品图标（32×32）。

按现有火炮图标的 3/4 视角画：炮塔顶面 + 正面 + 侧面三层色，炮管朝左上斜出，
联装数决定炮管组宽度（整块画再分隔，避免外描边钻进管间变成梯子）。
28 联喷进炮画成火箭发射箱。最后统一加纯黑外描边。
"""
from PIL import Image, ImageDraw

OUT = "/Users/lianran/apps/皮兰港实验/测试版/src/main/resources/assets/piranport/textures/item"
BLACK = (0, 0, 0, 255)
DARK = (56, 70, 74, 255)
MID = (100, 120, 124, 255)
STEEL = (146, 166, 168, 255)
LIGHT = (196, 216, 216, 255)
WHITE = (232, 246, 246, 255)
BRASS = (170, 136, 70, 255)

W = H = 32


def hull(d, scale):
    """底座 + 炮塔；scale 越大炮塔越宽（大口径/多联装）。"""
    # 底座
    d.polygon([(7, 25), (29, 25), (28, 28), (8, 28)], fill=DARK)
    d.line([(8, 25), (28, 25)], fill=MID)
    # 顶面
    x0 = 10 - (1 if scale else 0)
    x1 = 22 + (2 if scale else 0)
    d.polygon([(x0, 16), (x1, 13), (28, 17), (17, 20)], fill=STEEL)
    d.line([(x0, 16), (x1, 13)], fill=LIGHT)
    d.line([(x1, 13), (28, 17)], fill=STEEL)
    # 正面
    d.polygon([(17, 20), (28, 17), (28, 23), (17, 26)], fill=MID)
    d.line([(17, 26), (28, 23)], fill=DARK)
    # 左侧面
    d.polygon([(x0, 16), (17, 20), (17, 26), (x0, 22)], fill=DARK)
    d.line([(x0, 16), (x0, 22)], fill=MID)
    # 顶部高光
    d.point((13, 15), fill=WHITE)
    d.point((14, 15), fill=WHITE)


def barrel_slab(d, ox, oy, length, width, muzzle_ticks):
    """一排炮管画成整块斜管束：上沿高光、下沿阴影，炮口端用短深色刻线提示联装数。

    WHY 不逐根留缝：1 像素的缝会被外描边填黑，联装越多越像梯子；
    32×32 下靠整体轮廓 + 炮口刻线表达「几联装」比逐根画更清楚。
    """
    # 管束主体
    for k in range(width):
        d.line([(ox + k, oy), (ox + k - length, oy - length)], fill=MID)
    # 上沿高光（两根）、下沿阴影
    d.line([(ox, oy), (ox - length, oy - length)], fill=LIGHT)
    d.line([(ox + 1, oy), (ox + 1 - length, oy - length)], fill=LIGHT)
    d.line([(ox + width - 1, oy), (ox + width - 1 - length, oy - length)], fill=DARK)
    # 炮口：整排压一道深色，再按联装数点出管口
    d.line([(ox - length, oy - length), (ox + width - 1 - length, oy + width - 1 - length)], fill=DARK)
    for t in range(muzzle_ticks):
        k = 1 + t * 2
        if k < width:
            d.point((ox + k - length, oy + k - length), fill=BLACK)


def rocket_box(d):
    d.rectangle([9, 8, 24, 20], fill=MID)
    d.polygon([(9, 8), (24, 8), (26, 11), (11, 11)], fill=STEEL)
    d.line([(9, 8), (24, 8)], fill=LIGHT)
    for row in range(3):
        for col in range(4):
            cx, cy = 11 + col * 3, 10 + row * 3
            d.rectangle([cx, cy, cx + 1, cy + 1], fill=DARK)
            d.point((cx, cy), fill=BRASS)


def gun_icon(n, caliber, rocket=False):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    scale = caliber >= 45 or n >= 6
    hull(d, scale)
    if rocket:
        rocket_box(d)
    else:
        rows = 1 if n <= 4 else (2 if n <= 12 else 3)
        per_row = (n + rows - 1) // rows
        length = 11 if caliber <= 40 else 13
        for row in range(rows):
            count = min(per_row, n - row * per_row)
            barrel_slab(d, 14 - row, 16 - row * 3, length, count * 2, min(count, 4))
    # 外描边
    px = img.load()
    out = img.copy()
    op = out.load()
    for y in range(H):
        for x in range(W):
            if px[x, y][3]:
                continue
            if any(0 <= x + dx < W and 0 <= y + dy < H and px[x + dx, y + dy][3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                op[x, y] = BLACK
    return out


SPECS = {
    "us_twin_oerlikon_20mm": (2, 20),
    "german_twin_37mm_aa": (2, 37),
    "german_quad_20mm_aa": (4, 20),
    "us_twin_bofors_40mm": (2, 40),
    "quad_pom_pom_40mm": (4, 40),
    "up_mark_i": (10, 28),
    "french_quad_37mm_aa": (4, 37),
    "us_quad_bofors_40mm": (4, 40),
    "octuple_pom_pom_40mm": (8, 40),
    "japanese_28_tube_aa_rocket": (28, 28),
    "sm24_zif_twin_57mm": (2, 57),
    "ak230": (2, 30),
    "british_sextuple_bofors_40mm": (6, 40),
    "sm20_zif_quad_45mm": (4, 45),
    "bofors_m1951_twin_57mm": (2, 57),
    "zif75_quad_57mm": (4, 57),
    "us_twin_3inch_aa": (2, 76),
}

for name, (n, cal) in SPECS.items():
    gun_icon(n, cal, rocket=(name == "japanese_28_tube_aa_rocket")).save(f"{OUT}/{name}.png")
print("done")
