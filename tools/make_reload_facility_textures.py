#!/usr/bin/env python3
"""装填设施（龙门吊）贴图生成器。

装填设施是给舰娘吊装鱼雷/导弹的 3x3x3 龙门吊，结构分三种材质：
- frame：黄黑警示条纹的钢结构（吊臂横梁、端柱、设备箱）
- deck ：甲板防滑钢板（立柱底座、行走导轨、作业台面）
- rail ：深灰导轨/钢缆（纵向导轨、小车、吊索、吊具）
"""
import random
from PIL import Image

SIZE = 16
rng = random.Random(20260918)

# 钢结构：冷灰蓝，接近工业钢
STEEL   = (0x6B, 0x70, 0x78)
STEEL_H = (0x86, 0x8C, 0x94)
STEEL_D = (0x4E, 0x53, 0x5A)
STEEL_X = (0x33, 0x37, 0x3C)

# 警示漆：项目制式黄 + 近黑
WARN_Y  = (0xC8, 0x9B, 0x2E)
WARN_YH = (0xE0, 0xB4, 0x46)
WARN_YX = (0x1E, 0x1E, 0x20)

# 甲板：暖灰防滑板
DECK    = (0x5C, 0x5E, 0x60)
DECK_H  = (0x74, 0x76, 0x78)
DECK_D  = (0x44, 0x46, 0x49)

# 导轨 / 钢缆
RAIL    = (0x3A, 0x3E, 0x44)
RAIL_H  = (0x54, 0x59, 0x60)
RAIL_X  = (0x22, 0x25, 0x29)

# 吊具上的弹体提示色（鱼雷/导弹的金属弹身）
AMMO    = (0x8E, 0x74, 0x3C)
AMMO_H  = (0xA8, 0x8C, 0x50)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def grain(c, amp=2):
    return shade(c, rng.randint(-amp, amp))


def new_img(color):
    return Image.new("RGB", (SIZE, SIZE), color)


def make_frame():
    """钢结构：冷灰钢板 + 一道窄黄警示带。

    之前整张贴满 45° 黄黑斜条，贴到每一面后像迷彩，完全盖掉了结构轮廓，
    所以改成"干净钢板 + 一条警示带"：钢板上只保留轻微的轧制纹和铆钉，
    黄带放在贴图中段，包到立柱上时正好形成一圈腰线。
    """
    img = new_img(STEEL)

    # 1. 钢板的竖向轧制纹（细、低对比，只为说明是金属而非水泥）
    for y in range(SIZE):
        for x in range(SIZE):
            v = x % 4
            c = STEEL_H if v == 0 else (STEEL_D if v == 2 else STEEL)
            img.putpixel((x, y), grain(c, 1))

    # 2. 中段警示带 y=6..9：黄底 + 斜向黑条，出画一圈腰线
    for y in range(6, 10):
        for x in range(SIZE):
            c = WARN_YX if ((x + y) // 4) % 2 == 0 else WARN_Y
            img.putpixel((x, y), grain(c, 2))
    # 黄带的上下压边（钣金折边，让带子有厚度）
    for x in range(SIZE):
        img.putpixel((x, 5), grain(STEEL_X, 1))
        img.putpixel((x, 10), grain(STEEL_X, 1))

    # 3. 上下边框（钢结构横梁的厚度感）
    for x in range(SIZE):
        img.putpixel((x, 0), grain(STEEL_H, 2))
        img.putpixel((x, SIZE - 1), grain(STEEL_X, 2))

    # 4. 铆钉：避开警示带，只在钢板区域打
    for y in (2, 13):
        for x in range(2, SIZE - 1, 5):
            img.putpixel((x, y), grain(STEEL_X, 1))

    return img


def make_deck():
    """甲板防滑钢：方格纹止滑板 + 四周包边。"""
    img = new_img(DECK)

    # 1. 止滑纹理：每 4x4 一个凸点花型（菱形凸起）
    for ty in range(4):
        for tx in range(4):
            cx, cy = tx * 4 + 2, ty * 4 + 2
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if abs(dx) + abs(dy) > 1:
                        continue
                    x, y = cx + dx, cy + dy
                    if 0 <= x < SIZE and 0 <= y < SIZE:
                        c = DECK_H if dx + dy <= 0 else DECK_D
                        img.putpixel((x, y), grain(c, 2))

    # 2. 板缝（每 8px 一道，模拟钢板拼接）
    for i in range(0, SIZE, 8):
        for j in range(SIZE):
            img.putpixel((i, j), grain(shade(DECK, -18), 2))
            img.putpixel((j, i), grain(shade(DECK, -18), 2))

    # 3. 包边（顶部受光、底部背光）
    for x in range(SIZE):
        img.putpixel((x, 0), grain(DECK_H, 2))
        img.putpixel((x, SIZE - 1), grain(shade(DECK, -22), 2))

    return img


def make_rail():
    """导轨/钢缆：中央高光纵向钢轨 + 两侧暗槽。"""
    img = new_img(RAIL)

    # 1. 纵向钢轨：中间两道亮条，两侧深槽
    for y in range(SIZE):
        for x in range(SIZE):
            d = abs(x - 7.5)
            if d < 1.2:
                c = RAIL_H
            elif d < 2.6:
                c = shade(RAIL, 14)
            elif d < 4.2:
                c = RAIL
            else:
                c = RAIL_X
            img.putpixel((x, y), grain(c, 2))

    # 2. 每隔 5px 一道横向加强肋
    for y in range(2, SIZE, 5):
        for x in range(SIZE):
            img.putpixel((x, y), grain(shade(img.getpixel((x, y)), -12), 2))

    # 3. 顶底端面
    for x in range(SIZE):
        img.putpixel((x, 0), grain(RAIL_H, 2))
        img.putpixel((x, SIZE - 1), grain(RAIL_X, 2))

    return img


def make_hoist():
    """吊具：钢制夹爪 + 弹体环槽，用于横梁上的可动吊具。"""
    img = new_img(RAIL)

    # 1. 钢制吊具本体
    for y in range(5, 11):
        for x in range(2, 14):
            c = STEEL_H if y < 8 else STEEL_D
            img.putpixel((x, y), grain(c, 2))
    # 2. 吊具顶部横梁（连接钢缆）
    for y in range(0, 5):
        for x in range(6, 10):
            img.putpixel((x, y), grain(RAIL_H, 2))
    # 3. 弹体（吊装中的鱼雷/导弹筒身）
    for y in range(6, 10):
        for x in range(4, 12):
            c = AMMO_H if y == 6 else AMMO
            img.putpixel((x, y), grain(c, 2))
    # 4. 夹爪（左右两片压住弹体）
    for y in range(5, 11):
        img.putpixel((2, y), grain(STEEL_X, 1))
        img.putpixel((13, y), grain(STEEL_X, 1))

    return img


def main():
    out = "src/main/resources/assets/piranport/textures/block"
    items = [("reload_facility_frame", make_frame),
             ("reload_facility_deck", make_deck),
             ("reload_facility_rail", make_rail),
             ("reload_facility_hoist", make_hoist)]

    imgs = {}
    for name, fn in items:
        fn().save(f"{out}/{name}.png")
        print(f"wrote {out}/{name}.png")
        imgs[name] = Image.open(f"{out}/{name}.png").convert("RGB")

    # 放大预览拼图
    scale = 12
    w = SIZE * scale * len(items) + 16 * (len(items) - 1)
    canvas = Image.new("RGB", (w, SIZE * scale), (30, 30, 32))
    for i, (name, _) in enumerate(items):
        big = imgs[name].resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        canvas.paste(big, (i * (SIZE * scale + 16), 0))
    canvas.save("/tmp/reload_facility_textures.png")
    print("wrote /tmp/reload_facility_textures.png")


if __name__ == "__main__":
    main()
