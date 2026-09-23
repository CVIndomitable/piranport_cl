# -*- coding: utf-8 -*-
"""
重画 XA2J（轰炸）的 32x32 双态贴图。
参考原画: 战舰少女R Equip_L_458.png（zjsnrwiki）

构图对齐原画（侧面视图，机头朝左）:
  长圆筒机身横贯画面，机头左端带两叶螺旋桨盘（十字形）；
  机身中段挂一盏发动机短舱，短舱后侧连一片内翼；
  机身下缘伸出细长后掠的外翼，外翼前缘再挂一台短舱；
  机身右上伸出高耸的垂尾与水平尾翼，尾翼上有 "46" 编号与红带；
  机腹下缘一道深色阴影，机头下方有小前起落架。

双态约定（见 docs/策划决策/航空/04-飞机双态贴图.md 方案 B）:
  xa2j_bomber.png       —— 有燃料（装填）
  xa2j_bomber_empty.png —— 无燃料（空膛）
"""
from PIL import Image

W = H = 32
T = (0, 0, 0, 0)          # 透明

# ---------- 调色板 ----------
# 原画是喷绘线稿：整体是偏暖的灰橄榄（取样约 #A88F60 / #96814F），
# 只有尾翼红带与少量蒙皮色块是饱和色。所以这里走"低饱和暖灰"阶梯，
# 靠明度分层而不是靠色相拉开，才和原画的调子一致。
OUTLINE = (0, 0, 0, 255)         # 纯黑描边（原画像素化约定：只加最外层）
HILIGHT = (232, 218, 190, 255)   # 桨毂/短舱顶部最亮点
TOP     = (196, 176, 132, 255)   # 机身背部最亮高光
LIGHT   = (156, 138, 100, 255)   # 机身上部受光
MID_L   = (134, 116, 84, 255)    # 机身上部到主体的过渡
MID     = (116, 100, 70, 255)    # 机身主体暖橄榄
MID_D   = (95, 81, 56, 255)      # 机身主体到阴影的过渡
DARK    = (78, 66, 44, 255)      # 机身下部
DARK_D  = (58, 49, 33, 255)      # 机身下缘再压一档
DEEP    = (44, 38, 26, 255)      # 机腹深阴影
GLASS   = (168, 190, 200, 255)   # 座舱玻璃
GLASS_D = (92, 116, 128, 255)    # 座舱玻璃暗部
CANOPY  = (216, 232, 238, 255)   # 玻璃高光
METAL   = (140, 138, 124, 255)   # 发动机短舱整流罩
METAL_L = (170, 168, 154, 255)   # 整流罩受光面
METAL_D = (86, 86, 78, 255)      # 短舱暗部
PROP    = (48, 44, 40, 255)      # 螺旋桨叶片
PROP_L  = (66, 61, 56, 255)      # 螺旋桨叶片受光侧
HUB     = (200, 196, 180, 255)   # 桨毂
RED     = (168, 44, 38, 255)     # 尾翼识别红带
RED_D   = (110, 28, 24, 255)     # 红带暗部


def new_canvas():
    return Image.new("RGBA", (W, H), T)


def px(im, x, y, c):
    if 0 <= x < W and 0 <= y < H:
        im.putpixel((x, y), c)


def rect(im, x0, y0, x1, y1, c):
    """含端点的实心矩形。"""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)


def outline_silhouette(im):
    """给已有像素的最外圈补纯黑描边（八邻域），不改动已有像素本身。"""
    solid = {(x, y) for y in range(H) for x in range(W) if im.getpixel((x, y))[3] > 0}
    edge = []
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < W and 0 <= ny < H):
                    continue
                if (nx, ny) not in solid:
                    edge.append((nx, ny))
    for (x, y) in edge:
        px(im, x, y, OUTLINE)
    return im


def draw_xa2j(loaded: bool):
    """画一台 XA2J 侧面像。loaded=True 为装填态，False 为空膛态。

    机身轴线约在 y=19，机头 x=4 起，机尾 x=27 收细。
    各部件自后向前堆叠，最后统一描边，保证只有最外层有黑线。
    """
    im = new_canvas()

    # ---------- 1. 垂尾 + 水平尾翼（画面右上，先画，被机身压住根部）----------
    # 垂尾：自机尾向上耸起，略向后倾
    for i, y in enumerate(range(4, 15)):
        x = 22 + i * 3 // 10                    # 越往上越靠后
        rect(im, x, y, x + 3, y, MID)
        px(im, x, y, LIGHT)                     # 前缘受光
        px(im, x + 2, y, MID_D)                 # 中间过渡
        px(im, x + 3, y, DARK)                  # 后缘背光
    # 垂尾识别红带 + "46" 编号位置（原画尾部有一道红带与白色编号）
    rect(im, 24, 6, 26, 7, RED)
    rect(im, 24, 7, 26, 7, RED_D)               # 红带下缘压暗，避免一条死红
    px(im, 25, 9, LIGHT)
    px(im, 26, 9, LIGHT)
    px(im, 25, 10, LIGHT)
    px(im, 26, 10, LIGHT)
    # 水平尾翼：自垂尾根部向后下方伸出
    for i, x in enumerate(range(20, 30)):
        y = 13 + i * 2 // 10
        rect(im, x, y, x, y + 1, DARK)
        px(im, x, y, MID)
    px(im, 29, 14, DEEP)
    px(im, 29, 15, DEEP)

    # ---------- 2. 外侧长翼（机腹下缘伸出，后掠收窄）----------
    # 后掠角约 25 度，翼根厚翼尖薄，翼面比机身暗一档以拉开层次
    for i, x in enumerate(range(8, 24)):
        y = 22 + i * 5 // 16
        th = 3 - i * 2 // 16
        rect(im, x, y, x, y + th, DARK)
        px(im, x, y, MID)                       # 翼前缘
        px(im, x, y + 1, MID_D)                 # 翼面中段
        px(im, x, y + th, DEEP)                 # 翼后缘
    # 翼尖收口
    rect(im, 23, 26, 24, 27, DEEP)

    # ---------- 3. 发动机短舱（机翼前缘挂载）----------
    # 外侧翼短舱（画面右下），整流罩呈胶囊形
    rect(im, 19, 25, 22, 28, METAL)
    rect(im, 19, 25, 22, 25, METAL_L)           # 罩体上缘受光
    rect(im, 19, 26, 22, 26, LIGHT)             # 受光面到主体的过渡
    rect(im, 19, 28, 22, 28, METAL_D)           # 罩体下缘暗部
    px(im, 23, 26, METAL_D)
    px(im, 23, 27, METAL_D)
    # 内侧短舱（贴近机腹）
    rect(im, 12, 22, 15, 25, METAL)
    rect(im, 12, 22, 15, 22, METAL_L)
    rect(im, 12, 24, 15, 24, MID_D)
    rect(im, 12, 25, 15, 25, METAL_D)

    # ---------- 4. 机身（横贯画面的长圆筒）----------
    # 机身按上/中/下三条色带分层，才能从侧面看出圆筒的圆柱感
    rect(im, 4, 15, 27, 21, MID)                # 主体
    rect(im, 4, 15, 27, 16, LIGHT)              # 背部受光带
    rect(im, 5, 15, 24, 15, TOP)                # 背部最亮点
    rect(im, 4, 17, 27, 17, MID_L)              # 上机身到主体的过渡
    rect(im, 4, 19, 27, 20, DARK)               # 腹部过渡带
    rect(im, 5, 20, 26, 20, DARK_D)             # 腹部再压一档
    rect(im, 5, 21, 26, 21, DEEP)               # 机腹最深阴影
    # 机头圆钝收口（画成圆角而不是直角）
    px(im, 3, 17, MID)
    px(im, 3, 18, MID)
    px(im, 3, 19, DARK)
    px(im, 4, 17, MID_L)
    px(im, 4, 21, DEEP)
    # 机尾收细
    px(im, 28, 16, MID)
    px(im, 28, 17, MID_L)
    px(im, 28, 18, DARK)
    px(im, 27, 21, DEEP)
    # 座舱（机头后上方的玻璃罩）
    rect(im, 7, 13, 11, 15, GLASS)
    rect(im, 7, 13, 11, 13, CANOPY)
    rect(im, 7, 14, 7, 15, GLASS_D)             # 玻璃前框背光，让座舱有厚度
    rect(im, 10, 14, 11, 15, GLASS_D)
    px(im, 6, 14, GLASS_D)
    px(im, 6, 15, LIGHT)

    # ---------- 5. 两叶螺旋桨（机头左端，十字桨盘）----------
    px(im, 2, 13, PROP)
    px(im, 2, 14, PROP_L)                       # 叶片受光侧
    px(im, 2, 15, PROP)
    px(im, 2, 16, PROP)
    px(im, 2, 17, HUB)                          # 桨毂
    px(im, 2, 18, PROP)
    px(im, 2, 19, PROP)
    px(im, 2, 20, PROP_L)
    px(im, 2, 21, PROP)
    px(im, 1, 17, PROP)
    px(im, 1, 18, PROP_L)
    px(im, 3, 14, PROP)
    px(im, 3, 15, PROP_L)

    # ---------- 6. 前起落架（机头下方细杆）----------
    px(im, 6, 22, METAL_D)
    px(im, 6, 23, METAL_D)
    px(im, 5, 24, PROP)
    px(im, 6, 24, PROP)
    px(im, 7, 24, PROP)

    # ---------- 7. 弹舱挂弹（装填态独占）----------
    if loaded:
        # 机腹弹舱比空膛态明显鼓出一块，槽位净空，与后续描边不粘连。
        # 弹体用金属灰而非机身暖色，才能从机腹深阴影里跳出来。
        rect(im, 14, 23, 18, 23, METAL)
        rect(im, 15, 22, 17, 22, METAL_L)       # 弹体上缘受光
        px(im, 14, 23, METAL_D)
        px(im, 18, 23, METAL_D)
        px(im, 15, 24, METAL_D)                 # 弹体下缘暗部（与描边隔开一格）
        px(im, 17, 24, METAL_D)

    outline_silhouette(im)
    return im


def main():
    base = "src/main/resources/assets/piranport/textures/item/xa2j_bomber"

    # 空膛先写（对应 models/item/xa2j_bomber.json 的 layer0）
    empty = draw_xa2j(loaded=False)
    path_empty = base + "_empty.png"
    empty.save(path_empty)
    print("saved", path_empty)

    # 装填后写（对应 models/item/xa2j_bomber_fueled.json 的 layer0）
    full = draw_xa2j(loaded=True)
    path_full = base + ".png"
    full.save(path_full)
    print("saved", path_full)

    # 导出放大预览便于肉眼验收
    full.resize((512, 512), Image.NEAREST).save("/tmp/xa2j_new_full.png")
    empty.resize((512, 512), Image.NEAREST).save("/tmp/xa2j_new_empty.png")


if __name__ == "__main__":
    main()
