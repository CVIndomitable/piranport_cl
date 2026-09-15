#!/usr/bin/env python3
"""16x16 Minecraft 风格石磨贴图生成器：top / side / bottom 三张贴图。

石磨造型参考传统石磨：
- 顶面：方形石座 + 圆形上扇磨盘，中央磨心 + 左右双磨眼(进料孔)，
       磨齿从磨心向盘缘放射
- 侧面：上扇(细竖纹) + 木摇柄 + 两扇接缝 + 下扇底座(粗竖纹)
- 底面：底座同心圆环刻痕
"""
import math
import random
from PIL import Image

SIZE = 16
rng = random.Random(20240915)

# 颜色基调（暖灰，贴近原版石头系）
BASE  = (0x7B, 0x7B, 0x7A)  # 标准石头
LIGHT = (0x90, 0x90, 0x8F)  # 受光面
DARK  = (0x63, 0x63, 0x64)  # 阴影
DEEP  = (0x4A, 0x4A, 0x4C)  # 深沟 / 接缝
BLACK = (0x18, 0x18, 0x1A)  # 磨眼内芯
WOOD  = (0x8B, 0x5A, 0x2B)  # 木柄(橡木色)
WOOD_D= (0x6E, 0x45, 0x21)
WOOD_H= (0x9B, 0x6E, 0x36)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def grain(c, amp=2):
    return shade(c, rng.randint(-amp, amp))


def in_circle(x, y, cx, cy, r):
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def new_img(color):
    return Image.new("RGB", (SIZE, SIZE), color)


def make_top():
    img = new_img(BASE)

    # --- 1. 方形石座（4x4 瓷砖 + 暗接缝） ---
    for ty in range(4):
        for tx in range(4):
            for y in range(ty * 4, ty * 4 + 4):
                for x in range(tx * 4, tx * 4 + 4):
                    c = grain(BASE, 2)
                    if x % 4 == 3 or y % 4 == 3:
                        c = shade(c, -14)
                    img.putpixel((x, y), c)

    # --- 2. 圆形上扇磨盘（亮灰） ---
    for y in range(SIZE):
        for x in range(SIZE):
            if in_circle(x + 0.5, y + 0.5, 7.5, 7.5, 6.75):
                img.putpixel((x, y), grain(LIGHT, 3))

    # --- 3. 磨盘外沿一圈阴影，形成立体感 ---
    for y in range(SIZE):
        for x in range(SIZE):
            if in_circle(x + 0.5, y + 0.5, 7.5, 7.5, 6.75) \
               and not in_circle(x + 0.5, y + 0.5, 7.5, 7.5, 5.9):
                img.putpixel((x, y), grain(DARK, 2))

    # --- 4. 磨心（中心中轴点） ---
    for y in range(SIZE):
        for x in range(SIZE):
            if in_circle(x + 0.5, y + 0.5, 7.5, 7.5, 1.3):
                img.putpixel((x, y), grain(DEEP, 2))

    # --- 5. 双磨眼（磨心左右两个进料深孔） ---
    for (ex, ey) in ((5.5, 7.0), (9.5, 7.0)):
        for y in range(SIZE):
            for x in range(SIZE):
                if in_circle(x + 0.5, y + 0.5, ex, ey, 1.8):
                    if in_circle(x + 0.5, y + 0.5, ex, ey, 1.3):
                        img.putpixel((x, y), BLACK)
                    else:
                        img.putpixel((x, y), grain(DEEP, 2))

    # --- 6. 放射状磨齿（从磨心两侧向盘缘，避开磨眼） ---
    for i in range(10):
        a = math.radians(i * 36)
        dx, dy = math.sin(a), -math.cos(a)   # 顶视图绕中心旋转
        for r in range(2, 6):
            x = 7.5 + r * dx
            y = 7.5 + r * dy
            # 磨齿是短刻痕：每条约 2px 长、间隔 1px
            j = r % 3
            if j == 1:
                xi, yi = int(round(x)), int(round(y))
                if 0 <= xi < SIZE and 0 <= yi < SIZE \
                   and in_circle(xi + 0.5, yi + 0.5, 7.5, 7.5, 6.0) \
                   and not in_circle(xi + 0.5, yi + 0.5, 5.5, 7.0, 2.3) \
                   and not in_circle(xi + 0.5, yi + 0.5, 9.5, 7.0, 2.3):
                    img.putpixel((xi, yi), grain(DEEP, 2))

    return img


def make_side():
    img = new_img(BASE)

    # --- 1. 顶边高光 ---
    for x in range(SIZE):
        img.putpixel((x, 0), grain(shade(LIGHT, 14), 2))

    # --- 2. 上扇磨盘侧壁（细竖纹，亮灰） ---
    for y in range(1, 6):
        for x in range(SIZE):
            c = LIGHT if x % 2 == 0 else shade(LIGHT, -18)
            img.putpixel((x, y), grain(c, 2))

    # --- 3. 木摇柄（左侧伸出，穿入上扇） ---
    for y in range(3, 5):
        for x in range(0, 4):
            if x < 3:
                img.putpixel((x, y), grain(WOOD, 2))       # 柄身
            else:
                img.putpixel((x, y), grain(WOOD_D, 2))     # 根部阴影(衔接)
    # 柄的顶面高光、底面阴影，一点立体感
    for x in range(3):
        img.putpixel((x, 3), grain(WOOD_H, 2) if x == 0 else grain(WOOD, 2))
        img.putpixel((x, 4), grain(WOOD_D, 2))
    # 柄端面高光
    img.putpixel((0, 3), WOOD_H)
    img.putpixel((0, 4), shade(WOOD, -6))

    # --- 4. 两扇接缝（暗色横槽） ---
    for y in range(6, 8):
        for x in range(SIZE):
            img.putpixel((x, y), grain(DEEP if y == 6 else shade(DEEP, 12), 2))

    # --- 5. 下扇底座（粗竖纹，暗灰） ---
    for y in range(8, 15):
        for x in range(SIZE):
            c = shade(BASE, -6) if (x // 2) % 2 == 0 else shade(BASE, -22)
            img.putpixel((x, y), grain(c, 2))

    # --- 6. 底边阴影 ---
    for x in range(SIZE):
        img.putpixel((x, 15), shade(DARK, -12))

    return img


def make_bottom():
    img = new_img(shade(BASE, -16))

    # 同心圆环刻痕（从外到内）
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5)
            if abs(d - 5.5) < 0.5:
                img.putpixel((x, y), grain(shade(BASE, -2), 2))
            elif abs(d - 3.2) < 0.5:
                img.putpixel((x, y), grain(DARK, 2))
            elif abs(d - 1.2) < 0.5:
                img.putpixel((x, y), grain(DEEP, 2))

    # 轻微砖缝，增加质感
    for ty in range(4):
        for tx in range(4):
            for y in range(ty * 4 + 1, ty * 4 + 3):
                for x in range(tx * 4 + 1, tx * 4 + 3):
                    if (x % 4 == 3 or y % 4 == 3) and rng.random() < 0.35:
                        img.putpixel((x, y), shade(img.getpixel((x, y)), -8))

    return img


def main():
    out = "src/main/resources/assets/piranport/textures/block"
    imgs = {}
    for name, fn in [("stone_mill_top", make_top),
                     ("stone_mill_side", make_side),
                     ("stone_mill_bottom", make_bottom)]:
        fn().save(f"{out}/{name}.png")
        print(f"wrote {out}/{name}.png")
        imgs[name] = Image.open(f"{out}/{name}.png").convert("RGBA")

    # 放大预览拼图
    scale = 16
    canvas = Image.new("RGB", (SIZE * scale * 3 + 32, SIZE * scale), (30, 30, 32))
    for i, name in enumerate(["stone_mill_top", "stone_mill_side", "stone_mill_bottom"]):
        big = imgs[name].resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        canvas.paste(big, (i * (SIZE * scale + 16), 0))
    canvas.save("/tmp/stone_mill_preview.png")
    print("wrote /tmp/stone_mill_preview.png")


if __name__ == "__main__":
    main()
