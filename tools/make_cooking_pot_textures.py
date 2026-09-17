#!/usr/bin/env python3
"""16x16 Minecraft 风格厨锅（中式大黑锅）贴图生成器。

产物（写入 assets/piranport/textures/block/）：
- cooking_pot_side.png   锅身外壁：黑铸铁 + 底部积炭变暗 + 锅沿卷边
- cooking_pot_top.png    锅口顶面：亮铁锅沿 + 深色锅膛（俯视"看进锅里"的感觉）
- cooking_pot_stand.png  双耳：暗铁

UV 约定（与 models/block/cooking_pot.json 严格对应）：
锅身外壁全部用 MC 的默认面 UV（BlockElement.uvsByFace），按 u=16-x（南北面）
/ u=z（西面）/ u=16-z（东面）、v=16-y 采样，所以贴图 v 轴自上而下就是方块的
y 由高到低，且刚好 1 像素 = 1 格。锅是半高的（只占 y 0..7），所以实际被采样的
只有 v 9..15 这一段，v 0..8 是空行、填成沿口色即可：
    v=9  ↔ y=7（锅沿顶棱）      v=15 ↔ y=1（锅底）
锅口顶面按 u=x, v=z 采样，贴图中心 (8,8) 即锅心，半径 6px 即锅沿外缘；
模型里锅沿是一圈真正架高 1 格的环，环内露出的就是锅膛膛底。

配色取自原版 cauldron_side / cauldron_top 的铸铁色阶并整体向暗端偏移，
以呈现"大黑锅"观感，同时不脱离原版灰阶家族。
"""
import math
import random
from PIL import Image

SIZE = 16
OUT_DIR = "src/main/resources/assets/piranport/textures/block"

# --- 铸铁色阶（源自原版 cauldron 系贴图，由暗到亮排序）---
BLACK = (0x27, 0x27, 0x2B)
DEEP = (0x2D, 0x2D, 0x32)
DARK = (0x34, 0x34, 0x38)
LO = (0x3F, 0x3E, 0x42)
MID = (0x49, 0x48, 0x48)
BASE = (0x4F, 0x4F, 0x4F)
HI = (0x59, 0x58, 0x58)
RIM = (0x5D, 0x5D, 0x5D)
RIM_HI = (0x6B, 0x6B, 0x6F)

LADDER = [BLACK, DEEP, DARK, LO, MID, BASE, HI, RIM, RIM_HI]


def step(color, delta):
    """在色阶梯子上移动 delta 级；不引入原版调色板之外的新颜色。"""
    i = LADDER.index(color)
    return LADDER[max(0, min(len(LADDER) - 1, i + delta))]


def cloud(seed, cells=4):
    """低频噪声云图（0..1）。原版铸铁贴图的斑驳是成片的大色块，
    不是逐像素噪点，所以先在小网格上取随机值再双三次放大。"""
    r = random.Random(seed)
    small = Image.new("L", (cells, cells))
    small.putdata([r.randint(0, 255) for _ in range(cells * cells)])
    big = small.resize((SIZE, SIZE), Image.BICUBIC)
    return [v / 255.0 for v in big.getdata()]


def make_side():
    """锅身外壁。v=4..13 是实际采样区，其余行做合理延伸防止边缘渗色。"""
    # 竖向明暗基线：只有锅沿最上一格提亮，往下迅速压暗（大黑锅的观感）。
    # 提亮范围刻意收窄，否则整个锅口段会变成一圈浅灰"盘子"。
    # v 0..8 不被任何面采样，只是填上沿口色防止边缘渗色。
    profile = {
        9: HI,       # y=7 锅沿顶棱
        10: BASE,    # y=6 锅口段
        11: MID,
        12: LO,      # y=4 锅腹段
        13: DARK,
        14: DEEP,    # y=2 锅底段
        15: BLACK,   # y=1 锅底
    }
    soot = cloud(1001, 4)    # 积炭斑
    grain = cloud(1002, 6)   # 铸铁颗粒
    rng = random.Random(2001)

    img = Image.new("RGB", (SIZE, SIZE), DEEP)
    for y in range(SIZE):
        for x in range(SIZE):
            i = y * SIZE + x
            if y in profile:
                c = profile[y]
            else:
                c = RIM          # v 0..8：锅沿以上，不被采样

            # 积炭：越靠近锅底越明显，成片而非逐像素。
            # 斜率压得平缓，否则整个锅底会糊成一块没有信息的死黑。
            if y >= 11:
                strength = (y - 10) / 8.0
                if soot[i] > 1.0 - strength * 0.85:
                    c = step(c, -1)
                    if soot[i] > 0.95:
                        c = step(c, -1)

            # 铸铁颗粒：低幅度抖动，让平面不至于死板
            if grain[i] > 0.72:
                c = step(c, 1)
            elif grain[i] < 0.24:
                c = step(c, -1)

            # 零星亮斑，模拟铸铁表面反光颗粒
            if rng.random() < 0.04:
                c = step(c, 1)
            img.putpixel((x, y), c)

    # 锅沿下方的卷边接缝：v=10（即紧贴锅沿那一格）压一道暗线做出卷口厚度
    for x in range(SIZE):
        img.putpixel((x, 10), step(MID, -1))
    return img


def make_top():
    """锅口顶面：锅沿环 + 锅膛。UV 用 u=x, v=z，(8,8) 为锅心。

    锅身是八边形（半宽 6、切角 2），所以外沿必须跟着八边形走而不是画圆：
    否则八边形的四个切角会落在"沿外"区域、被填成浅灰，俯视时整个锅口
    看起来像一块方盘子。内圈仍按圆形分层，因为锅膛本身是圆的。
    """
    R, CUT = 6.0, 2.0
    l1 = 2 * R - CUT            # 八边形的 L1 边界：|dx|+|dz| <= l1
    edge = cloud(3001, 5)
    inner = cloud(3002, 3)
    rng = random.Random(3003)

    def oct_radius(dx, dz):
        """八边形度量：边界处恒为 R。"""
        return max(abs(dx), abs(dz), (abs(dx) + abs(dz)) * R / l1)

    img = Image.new("RGB", (SIZE, SIZE), BLACK)
    for y in range(SIZE):
        for x in range(SIZE):
            i = y * SIZE + x
            dx, dz = x + 0.5 - 8.0, y + 0.5 - 8.0
            # 半径加抖动，避免出现完美的机器图形（保持原版手绘感）
            rho = oct_radius(dx, dz) + (edge[i] - 0.5) * 0.45
            d = math.hypot(dx, dz) + (edge[i] - 0.5) * 0.7

            if rho > R:
                c = BLACK                # 八边形之外（被切掉，不渲染）
            elif rho > R - 0.85:
                # 锅沿顶面（模型里对应架高的那一圈环）。刻意比锅身暗一档：
                # MC 顶面的亮度系数是 1.0，侧面只有 0.8/0.6，同色也会发白。
                c = HI if dx + dz < 0 else MID
            elif rho > R - 1.5:
                c = DARK                 # 沿口内壁，撑出锅口的厚度
            elif d > 3.4:
                # 锅膛膛底。中间调只要宽过一格，锅口就会读成一块浅色平板
                # 而不是"看进去的黑洞"，所以这里直接压到暗端。
                c = DEEP
            else:
                c = BLACK                # 锅底（最深）

            # 锅膛里的炭黑斑：幅度很小，只是打散大色块
            if d <= 3.4 and inner[i] < 0.35:
                c = step(c, -1)
            if R - 0.85 < rho <= R and rng.random() < 0.15:
                c = step(c, 1)           # 锅沿上的铁锈亮点
            img.putpixel((x, y), c)
    return img


def make_stand():
    """双耳：均匀的暗铸铁，比锅身再暗一档且几乎没有花纹。"""
    grain = cloud(4001, 4)
    rng = random.Random(4002)

    img = Image.new("RGB", (SIZE, SIZE), DARK)
    for y in range(SIZE):
        for x in range(SIZE):
            i = y * SIZE + x
            if grain[i] > 0.86:
                c = BASE
            elif grain[i] > 0.72:
                c = MID
            elif grain[i] < 0.14:
                c = BLACK
            elif grain[i] < 0.30:
                c = DEEP
            else:
                c = DARK
            if rng.random() < 0.05:
                c = step(c, 1)
            img.putpixel((x, y), c)
    # 耳/足是细长铁条，上下压暗做出圆角感
    for x in range(SIZE):
        img.putpixel((x, 0), DEEP)
        img.putpixel((x, 15), DEEP)
    return img


def make_bottom():
    """锅底与各级台阶的下表面：烟熏黑铁，几乎无花纹（多为掠射角看到）。"""
    grain = cloud(5001, 3)
    rng = random.Random(5002)

    img = Image.new("RGB", (SIZE, SIZE), BLACK)
    for y in range(SIZE):
        for x in range(SIZE):
            i = y * SIZE + x
            c = DEEP if grain[i] > 0.62 else BLACK
            if grain[i] < 0.16:
                c = BLACK
            if rng.random() < 0.04:
                c = DARK
            img.putpixel((x, y), c)
    return img


def main():
    import os

    os.makedirs(OUT_DIR, exist_ok=True)
    for name, img in [
        ("cooking_pot_side", make_side()),
        ("cooking_pot_top", make_top()),
        ("cooking_pot_bottom", make_bottom()),
        ("cooking_pot_stand", make_stand()),
    ]:
        path = os.path.join(OUT_DIR, name + ".png")
        img.save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
