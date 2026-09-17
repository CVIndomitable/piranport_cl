#!/usr/bin/env python3
"""16x16 Minecraft 风格灶台（中式砖砌灶）贴图生成器。

产物（写入 assets/piranport/textures/block/）：
- stove_side.png        灶身砖墙：土砖错缝砌法 + 底部积灰
- stove_front.png       灶门那一面（无火）：砖墙 + 券口门洞 + 冷灰
- stove_front_on.png    同上，有火：门内余烬，4 帧循环（附 .mcmeta）
- stove_top.png         灶台顶面（无火）：抹平的泥面 + 灶眼 + 熏黑
- stove_top_on.png      同上，有火：灶眼内余烬，4 帧循环（附 .mcmeta）

UV 约定（与 models/block/stove.json 严格对应）：
模型正面是 north 面（z 最小）。MC 的 north 面 u 随 -x、v 随 -y，即贴图
u = 16 - x、v = 16 - y。所以：
    灶门洞  x 4..12, y 2..9   ->  贴图 u 4..12, v 7..14
    灶身    x 1..15, y 0..15  ->  贴图 u 1..15, v 1..16
灶门内壁用的是灶身 north 面的那块贴图，所以余烬直接画在 stove_front*
的门洞区域里，靠门框的四块几何体把它框出来，不需要额外贴图。

动态贴图按原版 blast_furnace_front_on 的做法：PNG 竖向堆叠 N 帧，
配一个 .mcmeta 声明 frametime。模型 UV 仍然写 [0,16]，MC 会只取第一帧
的尺寸来映射，所以帧数变化不影响模型。

配色取自原版 mud_bricks / packed_mud 的土砖色阶，余烬色取自 campfire。
"""
import math
import os
import random

from PIL import Image

SIZE = 16
FRAMES = 4
FRAMETIME = 5
OUT_DIR = "src/main/resources/assets/piranport/textures/block"

# --- 土砖色阶（源自原版 mud_bricks，由暗到亮排序）---
MB_DEEP = (0x4A, 0x3A, 0x34)     # 砖缝阴影
MB_MORTAR = (0x5E, 0x48, 0x41)   # 原版 mud_bricks 的砖缝色
MB_DARK = (0x7E, 0x5D, 0x48)
MB_MID = (0x8C, 0x67, 0x4F)
MB_BASE = (0x95, 0x71, 0x50)     # 原版 mud_bricks 主色
MB_LIGHT = (0x9D, 0x78, 0x5C)
MB_HI = (0xAB, 0x86, 0x61)

# --- 烟熏 / 冷灰 / 炭火 ---
SOOT = (0x2B, 0x22, 0x1C)
SOOT_DEEP = (0x19, 0x15, 0x12)
ASH = (0x3A, 0x2F, 0x1E)
ASH_COLD = (0x4E, 0x49, 0x45)    # 熄火后的灰白炉灰
ASH_COLD_D = (0x33, 0x30, 0x2E)
EMBER_LO = (0x9C, 0x50, 0x14)
EMBER = (0xE8, 0x8A, 0x1C)
EMBER_HI = (0xFF, 0xD8, 0x3D)

BRICK_LADDER = [MB_DEEP, MB_MORTAR, MB_DARK, MB_MID, MB_BASE, MB_LIGHT, MB_HI]


def step_brick(color, delta):
    i = BRICK_LADDER.index(color)
    return BRICK_LADDER[max(0, min(len(BRICK_LADDER) - 1, i + delta))]


def cloud(seed, cells):
    """低频噪声云图（0..1），模拟原版贴图里成片的斑驳而非逐像素噪点。"""
    r = random.Random(seed)
    small = Image.new("L", (cells, cells))
    small.putdata([r.randint(0, 255) for _ in range(cells * cells)])
    big = small.resize((SIZE, SIZE), Image.BICUBIC)
    return [v / 255.0 for v in big.getdata()]


def brick_wall(seed, soot_bottom=0.0):
    """错缝土砖墙，砌法照抄原版 mud_bricks：

    砖高 6px、砖缝 2px（共 2 层砖），砖宽 8px、竖缝 1px，上下层错开半块。
    每块砖内部还有一道竖向明暗（中间亮、上下压暗），这是原版土砖看起来
    "有厚度"而不是一块块色片的关键。砖缝只比砖面暗一到两档且断续，
    用最暗档画连续深缝会让每块砖像独立贴片。
    """
    rng = random.Random(seed)
    grain = cloud(seed + 1, 5)     # 大色块
    fine = cloud(seed + 2, 8)      # 细颗粒
    img = Image.new("RGB", (SIZE, SIZE), MB_BASE)
    for y in range(SIZE):
        row = y // 8               # 0 或 1
        yl = y % 8                 # 该层内的行号：0..5 砖面，6..7 砖缝
        seams = (8,) if row == 0 else (4, 12)
        for x in range(SIZE):
            i = y * SIZE + x
            if yl >= 6 or x in seams:
                c = MB_DARK
                if fine[i] < 0.35:
                    c = MB_MORTAR
                elif fine[i] < 0.18:
                    c = MB_DEEP
                if yl == 7:        # 砖缝中线再压深一档，做出凹槽
                    c = step_brick(c, -1)
            else:
                # 每块砖一个基准色，做出砖块之间的色差
                off = random.Random(seed * 131 + row * 17 + x // 8).choice([-1, 0, 0, 0, 1])
                c = step_brick(MB_BASE, off)
                if yl in (0, 5):
                    c = step_brick(c, -1)      # 砖的上下沿压暗
                elif yl in (2, 3):
                    c = step_brick(c, 1)       # 砖腹受光
                if grain[i] > 0.76:
                    c = step_brick(c, 1)
                elif grain[i] < 0.24:
                    c = step_brick(c, -1)
                if fine[i] > 0.90:
                    c = step_brick(c, 1)
                elif fine[i] < 0.10:
                    c = step_brick(c, -1)
            # 灶脚积灰：越靠下越暗
            if soot_bottom > 0 and y >= 13:
                depth = (y - 12) / 3.0 * soot_bottom
                if rng.random() < depth * 0.55:
                    c = SOOT if rng.random() < 0.3 else MB_DARK
            img.putpixel((x, y), c)
    return img


def make_side():
    """灶身侧面：干净的砖墙（顶部一小圈会被灶台顶盖住）。"""
    return brick_wall(6001, soot_bottom=0.5)


def _fire_cavity(img, lit, frame, seed):
    """门洞内的炉膛：上段烟熏、下段炉灰；lit 时炉灰里透出余烬。

    冷灰用低频云图分色而不是逐像素掷点 —— 掷点会得到棋盘格噪声。
    """
    rng = random.Random(seed + frame * 977)
    ash = cloud(seed + 5, 4)
    for v in range(7, 15):
        for u in range(4, 12):
            i = v * SIZE + u
            if v <= 10:
                # 炉膛上部：烟熏内壁，越往上越黑
                depth = (v - 6) / 4.0
                if rng.random() < depth * 0.75:
                    c = SOOT_DEEP
                elif rng.random() < 0.5:
                    c = SOOT
                else:
                    c = ASH
            elif v == 11:
                c = SOOT if rng.random() < 0.45 else ASH
            elif not lit:
                c = ASH_COLD if ash[i] > 0.42 else ASH_COLD_D
            else:
                # 炉膛下部：余烬，越靠下越亮；每帧重新掷点形成跳动
                glow = (v - 11) / 3.0
                r = rng.random()
                if r < 0.10 * glow + 0.05:
                    c = EMBER_HI
                elif r < 0.40 * glow + 0.20:
                    c = EMBER
                elif r < 0.70 * glow + 0.35:
                    c = EMBER_LO
                else:
                    c = ASH if rng.random() < 0.5 else SOOT
            img.putpixel((u, v), c)


def _door_frame(img, rng):
    """券口：门洞上方一道券砖，两侧立颊砖。"""
    for u in range(3, 13):
        img.putpixel((u, 6), MB_DEEP if u in (3, 12) else step_brick(MB_MORTAR, 1))
    for v in range(7, 15):
        img.putpixel((3, v), MB_DEEP)
        img.putpixel((12, v), MB_DEEP)


def _door_smoke(img, rng, soot):
    """门楣以上的烟炱：按离门楣的距离连续压暗，越靠上越淡。"""
    for x in range(SIZE):
        reach = 1.0 + soot[x] * 4.5
        for y in range(0, 6):
            dist = 6 - y
            if dist > reach:
                continue
            base = img.getpixel((x, y))
            if base not in BRICK_LADDER:
                continue
            img.putpixel((x, y), step_brick(base, -2 if dist < reach * 0.5 else -1))
    for x in (2, 13):
        for y in range(4, 9):
            base = img.getpixel((x, y))
            if base in BRICK_LADDER:
                img.putpixel((x, y), step_brick(base, -2))


def make_front(lit, frame=0):
    """灶门那一面。v = 16 - y，所以门洞（方块 y 2..9）落在贴图 v 7..14，
    v 越大越靠下 —— 余烬画在 v 12..14，烟灰堆在 v 7..11。"""
    img = brick_wall(8001, soot_bottom=0.7)
    rng = random.Random(8002)
    soot = cloud(8003, 4)
    _fire_cavity(img, lit, frame, 8010)
    _door_frame(img, rng)
    _door_smoke(img, rng, soot)
    return img


def _stove_eye(img, lit, frame, seed):
    """顶面的灶眼：中心是灶膛。lit 时余烬会随帧跳动。

    冷灰同样用低频云图分色，边缘压到烟黑做出"这是个坑"的立体感。
    """
    rng = random.Random(seed + frame * 613)
    ash = cloud(seed + 5, 3)
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x + 0.5 - 8.0, y + 0.5 - 8.0)
            if d > 4.4:
                continue
            i = y * SIZE + x
            if lit:
                if d <= 1.9 and rng.random() < 0.5:
                    c = EMBER_LO if rng.random() < 0.7 else EMBER
                elif d <= 2.9:
                    c = ASH
                else:
                    c = SOOT_DEEP
            elif d > 3.4:
                c = SOOT_DEEP if ash[i] < 0.6 else ASH_COLD_D
            else:
                c = ASH_COLD if ash[i] > 0.42 else ASH_COLD_D
                if ash[i] < 0.12:
                    c = SOOT
            img.putpixel((x, y), c)


def make_top(lit=False, frame=0):
    """灶台顶面：抹平的泥面 + 中央灶眼 + 熏黑。

    灶眼开口是 x/z 3..13（四角各切 1x1 成八边形），贴图中心 (8,8) 即灶眼中心；
    露出来的只有灶眼那一块，外围一圈被模型的灶沿环盖住。
    """
    rng = random.Random(7001)
    plaster = cloud(7002, 5)
    soot = cloud(7003, 4)
    img = Image.new("RGB", (SIZE, SIZE), MB_BASE)
    for y in range(SIZE):
        for x in range(SIZE):
            i = y * SIZE + x
            d = math.hypot(x + 0.5 - 8.0, y + 0.5 - 8.0)

            if d <= 4.4:
                # 灶眼内部交给 _stove_eye 按帧画
                c = ASH
            elif d <= 5.2:
                # 灶眼边沿：被烤黑的泥面
                c = SOOT if soot[i] < 0.6 else ASH
            else:
                # 抹平的泥面：大色块 + 轻微的抹刀痕
                c = MB_LIGHT if plaster[i] > 0.76 else MB_BASE
                if plaster[i] < 0.24:
                    c = MB_MID
                if d <= 6.4:
                    # 灶眼外围的熏黑：整片压暗成泥灰色，而不是撒黑点 ——
                    # 撒点会让台面从上方看像一排破洞。
                    heat = 1.0 - (d - 5.2) / 1.2
                    c = step_brick(c, -2 if heat > 0.5 else -1)
                    if rng.random() < heat * 0.28:
                        c = MB_DARK
                elif rng.random() < 0.04:
                    c = MB_HI if rng.random() < 0.5 else MB_MID
            img.putpixel((x, y), c)
    _stove_eye(img, lit, frame, 7010)
    return img


def stack(frames):
    """把 N 张 16x16 竖向拼成 16x(16N) 的动态贴图。"""
    if len(frames) == 1:
        return frames[0]
    sheet = Image.new("RGB", (SIZE, SIZE * len(frames)))
    for k, f in enumerate(frames):
        sheet.paste(f, (0, k * SIZE))
    return sheet


def write(path, img, animated=False):
    img.save(path)
    meta = path + ".mcmeta"
    if animated:
        with open(meta, "w") as f:
            f.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % FRAMETIME)
    elif os.path.exists(meta):
        os.remove(meta)
    print("wrote", path, img.size, "(animated)" if animated else "")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, img, anim in [
        ("stove_side", make_side(), False),
        ("stove_front", make_front(lit=False), False),
        ("stove_front_on", stack([make_front(True, k) for k in range(FRAMES)]), True),
        ("stove_top", make_top(lit=False), False),
        ("stove_top_on", stack([make_top(True, k) for k in range(FRAMES)]), True),
    ]:
        write(os.path.join(OUT_DIR, name + ".png"), img, anim)


if __name__ == "__main__":
    main()
