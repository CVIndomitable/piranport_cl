#!/usr/bin/env python3
"""汉考克（skin_20）经典 Steve 体型（4px 手臂）玩家皮肤生成工具 —— 2026-09 重画版。

分辨率与画风基准 = 大凤（skin_4）：64×64。头部 12 个面片沿用 skin_4 的**脸部结构
骨架**（保证眼睛/脸型与整套皮肤同源），但配色全部重做：粉发 → 银白长发、蓝瞳 →
金瞳，并叠加汉考克自己的发饰与服装领口。

立绘参考：L_NORMAL_374（zjsnrwiki，2048×2048）。

立绘特征（观察所得）：
  - 银白色微卷长发，及腰；头顶两侧各一枚黑色小尖角（恶魔角发饰）
  - 金色/琥珀色竖瞳，眼神冷淡；肤色白皙偏冷
  - 黑色长披风（内衬银灰蓝），领口由银色圆形扣针固定
  - 胸前深酒红色心形胸衣，白色荷叶边领饰（jabot）
  - 黑色高领紧身衣 + 红色竖条镶边、胸前交叉系带
  - 黑色过肘长手套；黑色过膝长袜 + 黑色高跟鞋（银灰鞋头/鞋跟）
  - 黑色短裙，白色多层荷叶边衬裙
  - 小腿外侧有银色金属护胫（greeve）

体型说明：ShipGirlRenderer 对 skin_20 走默认 PlayerModel（classic，4px 手臂），
所以本脚本按经典 Steve 布局出图，不能改用 Alex/slim 的 3px 手臂。

输出：
  src/main/resources/assets/piranport/textures/skin/skin_20.png
  src/main/resources/assets/piranport/textures/item/skin_core_20.png
  build/offline-renders/hancock_skin_preview.png
  build/offline-renders/hancock_skin_sheet.png

运行：cd tools && python3 hancock_skin_tools.py

UV 约定（与原版一致，render_model 按此投影）：
  正面面片局部 x=0 是角色左侧（-x），模型朝 -z。
  WEST 面片（-x）局部 x=0 为后脑，EAST 面片（+x）局部 x=0 为面部。
  UP 面片局部 y=0 为后脑、y=7 为面部；DOWN 面片局部 y=0 为面部。
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
# 画风基准：大凤（skin_4）的脸部结构被本脚本照搬，只做配色替换
TAIHOU_SKIN = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_4.png"
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_20.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_20.png"
PREVIEW_OUT = ROOT / "build/offline-renders/hancock_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/hancock_skin_sheet.png"
BASE_SIZE = 64          # 标准皮肤 UV 空间，与大凤一致
SCALE = 1               # 1:1 输出 64×64
SIZE = BASE_SIZE * SCALE


# ---------------------------------------------------------------- 调色板

P = {
    "none": (0, 0, 0, 0),
    # 银白长发 —— 5 个色阶（发根深 → 头顶高光）；偏冷的灰白
    "hair_deep": (150, 144, 140, 255),
    "hair_shadow": (186, 180, 175, 255),
    "hair": (216, 211, 205, 255),
    "hair_mid": (235, 231, 226, 255),
    "hair_light": (250, 249, 247, 255),
    # 皮肤 —— 白皙偏冷
    "skin_light": (252, 240, 234, 255),
    "skin": (247, 226, 217, 255),
    "skin_shadow": (233, 203, 194, 255),
    "skin_deep": (214, 180, 173, 255),
    "blush": (232, 178, 172, 255),
    # 五官：金色/琥珀竖瞳
    "lash": (72, 58, 52, 255),
    "lash_light": (168, 152, 146, 255),
    "eye_deep": (198, 132, 26, 255),
    "eye_light": (246, 208, 104, 255),
    "eye_white": (252, 250, 248, 255),
    # 黑色长披风 / 裙
    "black": (32, 30, 38, 255),
    "black_light": (56, 53, 64, 255),
    "black_deep": (18, 17, 22, 255),
    # 披风内衬（银灰蓝）
    "lining": (140, 152, 176, 255),
    "lining_light": (178, 188, 208, 255),
    # 深酒红胸衣 / 红色镶边
    "crimson": (122, 32, 40, 255),
    "crimson_light": (156, 50, 56, 255),
    "crimson_deep": (86, 20, 28, 255),
    # 白色荷叶边（jabot / 衬裙）
    "white": (248, 247, 246, 255),
    "white_shadow": (222, 221, 226, 255),
    "white_deep": (196, 196, 206, 255),
    # 银灰金属（扣针 / 护胫 / 鞋头）
    "silver": (176, 178, 188, 255),
    "silver_light": (224, 226, 234, 255),
    "silver_deep": (128, 130, 142, 255),
    "gold": (206, 172, 72, 255),
}


# ---------------------------------------------------------------- 工具

def rect(img, box, color):
    """在 (x0,y0,x1,y1) 半开区间上填色，越界自动裁剪。"""
    x0, y0, x1, y1 = box
    img.paste(color, (x0, y0, max(x0, x1), max(y0, y1)))


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def copy_region(dst, src, box):
    x0, y0, x1, y1 = box
    dst.paste(src.crop(box), (x0, y0))


# ---------------------------------------------------------------- 头部

def build_head(skin, base):
    """头部：照搬 skin_4 的脸部结构，整体重制配色（银发 / 金瞳 / 恶魔角）。"""
    # 1) 先把大凤头部的全部 12 个面片原样拷贝过来（含 64 空间的上半部分）
    HEAD_BOXES = [
        (8, 0, 24, 16),      # 头顶 + 底
        (0, 8, 32, 16),      # 四个侧面
        (40, 0, 56, 16),     # 头层 top/bottom
        (32, 8, 64, 16),     # 头层四个侧面
    ]
    for box in HEAD_BOXES:
        copy_region(skin, base, box)

    # 2) 把大凤的粉色系像素按色相映射替换成汉考克的银白/冷色系。
    #    这里按「面片语义」逐块重绘，比逐像素猜色更可控。
    # --- 头顶 (8,0)-(16,8)：全部发丝。y=7 一行是正面发际线，做深一档，
    #     使正面看过去有一条明确的刘海分界（照搬 skin_4 的 e6748e 处理）
    rect(skin, (8, 0, 16, 8), P["hair"])
    for x, c in enumerate([P["hair_shadow"], P["hair_mid"], P["hair_shadow"],
                           P["hair_deep"], P["hair_mid"], P["hair_shadow"],
                           P["hair_deep"], P["hair_deep"]]):
        px(skin, 8 + x, 7, c)
    # 顶部挑染高光（立绘上头顶偏右有一束更亮）
    rect(skin, (11, 1, 14, 3), P["hair_mid"])
    rect(skin, (12, 2, 14, 4), P["hair_light"])
    px(skin, 9, 4, P["hair_shadow"])
    px(skin, 10, 5, P["hair_shadow"])
    # --- 头底 (16,0)-(24,8)：被脖子挡住，不可见，填发色即可
    rect(skin, (16, 0, 24, 8), P["hair_shadow"])
    # --- 头左面 (0,8)-(8,16)：太阳穴一侧，大部分被发丝覆盖
    rect(skin, (0, 8, 8, 16), P["hair"])
    rect(skin, (0, 12, 8, 16), P["hair_shadow"])   # 鬓角垂下
    # --- 头右面 (16,8)-(24,16)
    rect(skin, (16, 8, 24, 16), P["hair"])
    rect(skin, (16, 12, 24, 16), P["hair_shadow"])
    # --- 头后面 (24,8)-(32,16)：长发
    rect(skin, (24, 8, 32, 16), P["hair_shadow"])
    rect(skin, (25, 9, 31, 15), P["hair"])
    rect(skin, (26, 12, 28, 16), P["hair_deep"])
    rect(skin, (30, 13, 32, 16), P["hair_deep"])

    # 3) 正面脸 (8,8)-(16,16)。
    #    逐行照搬 skin_4 的脸部「结构」（刘海覆盖 → 睫毛 → 虹膜 → 脸颊，
    #    眼睛占 x=9..10 / 13..14、y=11..12），只把配色换成汉考克的银发金瞳。
    rect(skin, (8, 8, 16, 16), P["skin"])          # 先铺底：整张脸都是肤色
    #    第 0~2 行：刘海全覆盖（y=8..11 上半）
    rect(skin, (8, 8, 16, 11), P["hair"])
    # 正中间一撮挑染更亮（立绘的分缝）
    px(skin, 11, 10, P["hair_light"])
    px(skin, 12, 10, P["hair_mid"])
    # 第 3 行（y=11）：睫毛 + 眼白起点
    px(skin, 8, 11, P["hair_shadow"])    # 鬓发贴脸
    px(skin, 9, 11, P["lash"])
    px(skin, 10, 11, P["lash"])
    px(skin, 15, 11, P["hair_shadow"])
    px(skin, 13, 11, P["lash"])
    px(skin, 14, 11, P["lash"])
    # 第 4 行（y=12）：睫毛 → 虹膜 → 眼白（内侧留白，与大凤一致）
    px(skin, 8, 12, P["lash_light"])
    px(skin, 9, 12, P["eye_deep"])
    px(skin, 10, 12, P["eye_light"])
    px(skin, 11, 12, P["eye_white"])
    px(skin, 12, 12, P["eye_white"])
    px(skin, 13, 12, P["eye_light"])
    px(skin, 14, 12, P["eye_deep"])
    px(skin, 15, 12, P["lash_light"])
    #    第 5 行（y=13）：眼白 + 虹膜下半
    px(skin, 8, 13, P["skin_shadow"])
    px(skin, 9, 13, P["eye_light"])
    px(skin, 10, 13, P["eye_deep"])
    px(skin, 11, 13, P["skin"])       # 鼻梁，保持肤色而不是眼白
    px(skin, 12, 13, P["skin"])
    px(skin, 13, 13, P["eye_deep"])
    px(skin, 14, 13, P["eye_light"])
    px(skin, 15, 13, P["skin_shadow"])
    # 第 6~7 行（y=14..15）：脸颊与下巴
    px(skin, 8, 14, P["skin_shadow"])
    px(skin, 15, 14, P["skin_shadow"])
    px(skin, 9, 15, P["skin_shadow"])
    px(skin, 14, 15, P["skin_shadow"])
    px(skin, 11, 15, P["skin_shadow"])
    px(skin, 12, 15, P["skin_shadow"])

    # 5) 头层 overlay 正面 (40,8)-(48,16)：头顶两侧的黑色小尖角。
    #    注意 UV 方向：该面片局部 y=0 是**头顶**、y=7 是下巴（与 head_top 一致），
    #    局部 x=0 是角色左侧。所以「角」要画在 y=0 附近，不是 y=7。
    rect(skin, (40, 8, 48, 16), P["none"])
    # 角色左角（局部 x=0..1，头顶 y=0..1）
    px(skin, 40, 8, P["black"])
    px(skin, 41, 8, P["black"])
    px(skin, 40, 9, P["black_light"])
    # 角色右角（局部 x=6..7）
    px(skin, 46, 8, P["black"])
    px(skin, 47, 8, P["black"])
    px(skin, 47, 9, P["black_light"])
    # 额头碎发（overlay 层投在刘海上，局部 y=1..2）
    rect(skin, (42, 9, 46, 10), P["hair_light"])

    # 6) 其余头层 overlay 面片：清空（避免大凤的发饰残留）
    rect(skin, (32, 8, 40, 16), P["none"])
    rect(skin, (48, 8, 64, 16), P["none"])
    rect(skin, (40, 0, 56, 8), P["none"])


# ---------------------------------------------------------------- 身体

def build_body(skin, base):
    """身体/手臂/腿：按立绘重画（黑披风 + 酒红胸衣 + 黑裙 + 白荷叶边 + 黑丝袜）。"""
    # ===== 躯干 (20,16)-(32,32) =====
    # 躯干顶 (20,16)-(28,20)：肩部，披风覆盖
    rect(skin, (20, 16, 28, 20), P["black"])
    rect(skin, (21, 16, 27, 17), P["black_light"])   # 肩线高光
    # 躯干底 (28,16)-(36,20)：披风内衬
    rect(skin, (28, 16, 36, 20), P["black_deep"])

    # 躯干正面 (20,20)-(28,32)：领口 → 胸衣 → 腰 → 裙
    rect(skin, (20, 20, 28, 32), P["black"])
    # 白色高领衬衫（只在正中两格，两侧是披风翻领）
    rect(skin, (23, 20, 25, 22), P["white"])
    # 领口银色圆形扣针（披风扣）
    px(skin, 23, 21, P["silver"])
    px(skin, 24, 21, P["silver_light"])
    px(skin, 23, 20, P["silver_deep"])
    # 白色荷叶边领饰（jabot）—— 立绘上是从领口往下收窄的三角，不做满宽
    px(skin, 22, 22, P["white_shadow"])
    px(skin, 25, 22, P["white_shadow"])
    rect(skin, (23, 22, 25, 23), P["white"])
    px(skin, 23, 23, P["white_shadow"])
    px(skin, 24, 23, P["white_shadow"])
    px(skin, 22, 21, P["black_light"])   # 披风翻领
    px(skin, 25, 21, P["black_light"])
    # 深酒红胸衣（心形），第 24~26 行
    rect(skin, (21, 24, 27, 27), P["crimson"])
    px(skin, 21, 24, P["black"])          # 心形上缘两角收进去
    px(skin, 26, 24, P["black"])
    rect(skin, (22, 24, 26, 25), P["crimson_light"])   # 胸口高光
    rect(skin, (21, 26, 27, 27), P["crimson_deep"])    # 下缘暗部
    # 胸前交叉系带（黑色 X）
    px(skin, 23, 24, P["black"])
    px(skin, 24, 24, P["black"])
    px(skin, 22, 26, P["black"])
    px(skin, 25, 26, P["black"])
    # 腰部黑色紧身衣 + 红色竖条镶边（立绘上红条在身体两侧）
    rect(skin, (20, 27, 28, 30), P["black"])
    rect(skin, (20, 27, 21, 30), P["crimson"])
    rect(skin, (27, 27, 28, 30), P["crimson"])
    rect(skin, (22, 27, 26, 29), P["black_light"])     # 腹部高光
    # 腰带（金色卡扣）
    rect(skin, (20, 29, 28, 30), P["silver_deep"])
    px(skin, 23, 29, P["gold"])
    px(skin, 24, 29, P["gold"])
    # 黑色短裙，第 30~31 行（下摆略亮，做出裙褶）
    rect(skin, (20, 30, 28, 32), P["black"])
    for x in range(20, 28, 2):
        px(skin, x, 31, P["black_light"])

    # 躯干右面 (28,20)-(32,32)：体侧。立绘上披风从肩垂到腰，下摆外翻露出银灰内衬
    rect(skin, (28, 20, 32, 32), P["black"])
    rect(skin, (28, 20, 29, 32), P["black_light"])     # 肩线
    # 侧面的红色镶边（与正面腰部的红条连成一条）
    rect(skin, (28, 27, 29, 30), P["crimson"])
    # 披风下摆：银灰蓝内衬外翻
    rect(skin, (29, 30, 32, 32), P["lining"])
    px(skin, 29, 30, P["lining_light"])

    # 躯干左面 (16,20)-(20,32)
    rect(skin, (16, 20, 20, 32), P["black"])
    rect(skin, (19, 20, 20, 32), P["black_light"])
    rect(skin, (19, 27, 20, 30), P["crimson"])
    rect(skin, (16, 30, 19, 32), P["lining"])
    px(skin, 18, 30, P["lining_light"])

    # 躯干后面 (32,20)-(40,32)：披风背面。立绘上背面是整片黑披风 + 中缝
    rect(skin, (32, 20, 40, 32), P["black"])
    rect(skin, (32, 20, 40, 21), P["black_light"])     # 肩线高光
    # 披风中缝（两片披风的接缝）
    rect(skin, (35, 21, 37, 32), P["black_deep"])
    rect(skin, (35, 21, 36, 32), P["black_light"])
    # 下摆：银灰内衬 + 阴影
    rect(skin, (32, 31, 40, 32), P["black_deep"])
    rect(skin, (33, 30, 39, 31), P["lining"])
    px(skin, 34, 30, P["lining_light"])

    # ===== 身体 overlay (20,32)-(40,48) =====
    # overlay 层：裙摆的白色荷叶边衬裙
    rect(skin, (20, 32, 40, 48), P["none"])
    # 躯干正面 overlay：白荷叶边从裙下探出
    rect(skin, (20, 32, 28, 35), P["white"])
    rect(skin, (20, 35, 28, 36), P["white_shadow"])
    # 荷叶边锯齿
    for x in range(20, 28, 2):
        px(skin, x, 35, P["white"])
    px(skin, 21, 36, P["white_deep"])
    px(skin, 26, 36, P["white_deep"])
    # 侧面 overlay 同样有荷叶边
    rect(skin, (16, 32, 20, 34), P["white"])
    rect(skin, (28, 32, 32, 34), P["white"])
    rect(skin, (16, 34, 20, 35), P["white_shadow"])
    rect(skin, (28, 34, 32, 35), P["white_shadow"])
    # 背面 overlay：裙摆
    rect(skin, (32, 32, 40, 35), P["white"])
    rect(skin, (32, 35, 40, 36), P["white_shadow"])

    # ===== 右臂 (40,16)-(56,32) —— 角色左臂外侧 =====
    # 四个侧面 (40,20)-(56,32) 依次是：右 / 前 / 左 / 后
    ARM = [(40, 20, 44, 32), (44, 20, 48, 32), (48, 20, 52, 32), (52, 20, 56, 32)]
    rect(skin, (44, 16, 48, 20), P["black_light"])     # 肩顶（披风）
    rect(skin, (48, 16, 52, 20), P["black_deep"])
    for box in ARM:
        rect(skin, box, P["black"])
    # 上臂袖口：披风袖一直盖到肘部（立绘上黑手套 + 黑袍袖）
    rect(skin, (40, 20, 56, 22), P["black_light"])
    # 肘部以下换成长手套，做出材质转折：一道银灰线
    rect(skin, (40, 26, 56, 27), P["silver_deep"])
    # 手背高光（正面 44..48）
    rect(skin, (45, 28, 47, 32), P["black_light"])

    # 右臂 overlay (40,32)-(56,48)
    rect(skin, (40, 32, 56, 48), P["none"])
    # 手背 overlay：黑手套外层，比基础层略亮一档做出手套的硬边
    rect(skin, (44, 32, 48, 36), P["black_light"])

    # ===== 左臂 (32,48)-(48,64) =====
    # 四个侧面 (32,52)-(48,64) 依次是：右 / 前 / 左 / 后
    LARM = [(32, 52, 36, 64), (36, 52, 40, 64), (40, 52, 44, 64), (44, 52, 48, 64)]
    rect(skin, (36, 48, 40, 52), P["black_light"])
    rect(skin, (40, 48, 44, 52), P["black_deep"])
    for box in LARM:
        rect(skin, box, P["black"])
    rect(skin, (36, 48, 44, 50), P["black_light"])
    # 肘部银灰线（与右臂对称）
    rect(skin, (32, 58, 48, 59), P["silver_deep"])
    rect(skin, (37, 60, 39, 64), P["black_light"])     # 手背高光

    # 左臂 overlay (48,48)-(64,64)
    rect(skin, (48, 48, 64, 64), P["none"])
    rect(skin, (52, 48, 56, 52), P["black_light"])

    # ===== 右腿 (0,16)-(16,32) =====
    # 四个侧面 (0,20)-(16,32)：左 / 前 / 右 / 后 依次排列
    RLEG = [(0, 20, 4, 32), (4, 20, 8, 32), (8, 20, 12, 32), (12, 20, 16, 32)]
    rect(skin, (4, 16, 8, 20), P["black_light"])      # 腿顶
    rect(skin, (8, 16, 12, 20), P["black_deep"])      # 腿底
    for box in RLEG:
        rect(skin, box, P["black"])
    # 大腿顶缘：裙摆下的白色衬裙（只在正面与两侧露出，背面被裙盖住）
    rect(skin, (0, 20, 12, 21), P["white_shadow"])
    # 正面 (4,20)-(8,32) 的纵向高光，做出大腿到小腿的转折
    rect(skin, (5, 22, 6, 29), P["black_light"])
    # 膝盖（正面第 26 行）
    px(skin, 5, 26, P["black_light"])
    px(skin, 6, 26, P["black_light"])
    # 小腿正面下方的银色护胫（立绘上护胫裹在小腿前侧）
    rect(skin, (4, 29, 8, 32), P["silver"])
    rect(skin, (5, 29, 7, 30), P["silver_light"])
    px(skin, 4, 29, P["silver_deep"])
    px(skin, 7, 29, P["silver_deep"])
    # 外侧面 (0,20)-(4,32) 同做一条护胫延伸
    rect(skin, (0, 29, 2, 32), P["silver_deep"])
    # 脚踝
    rect(skin, (0, 31, 16, 32), P["black_deep"])

    # 右腿 overlay (0,32)-(16,48)：鞋
    # overlay 面片顺序与基础层一致：左(0,32) 前(4,32) 右(8,32) 后(12,32)
    rect(skin, (0, 32, 16, 48), P["none"])
    # 鞋尖银灰：做在正面临近脚背的两行
    rect(skin, (4, 32, 8, 34), P["silver"])
    rect(skin, (5, 32, 7, 33), P["silver_light"])
    rect(skin, (4, 34, 8, 35), P["silver_deep"])
    # 两侧鞋帮
    rect(skin, (0, 32, 2, 34), P["silver_deep"])
    rect(skin, (10, 32, 12, 34), P["silver_deep"])

    # ===== 左腿 (16,48)-(32,64) =====
    LLEG = [(16, 52, 20, 64), (20, 52, 24, 64), (24, 52, 28, 64), (28, 52, 32, 64)]
    rect(skin, (20, 48, 24, 52), P["black_light"])
    rect(skin, (24, 48, 28, 52), P["black_deep"])
    for box in LLEG:
        rect(skin, box, P["black"])
    rect(skin, (16, 52, 28, 53), P["white_shadow"])
    rect(skin, (21, 54, 22, 61), P["black_light"])
    px(skin, 21, 58, P["black_light"])
    px(skin, 22, 58, P["black_light"])
    # 小腿正面（局部 x=4..8 段 = (20,52)-(24,64)）的护胫
    rect(skin, (20, 61, 24, 64), P["silver"])
    rect(skin, (21, 61, 23, 62), P["silver_light"])
    px(skin, 20, 61, P["silver_deep"])
    px(skin, 23, 61, P["silver_deep"])
    rect(skin, (30, 61, 32, 64), P["silver_deep"])     # 外侧面延伸
    rect(skin, (16, 63, 32, 64), P["black_deep"])

    # 左腿 overlay (0,48)-(16,64)：鞋
    # 面片顺序：左(0,48) 前(4,48) 右(8,48) 后(12,48)
    rect(skin, (0, 48, 16, 64), P["none"])
    rect(skin, (4, 48, 8, 50), P["silver"])
    rect(skin, (5, 48, 7, 49), P["silver_light"])
    rect(skin, (4, 50, 8, 51), P["silver_deep"])
    rect(skin, (0, 48, 2, 50), P["silver_deep"])
    rect(skin, (10, 48, 12, 50), P["silver_deep"])


# ---------------------------------------------------------------- 预览

def render_sheet(skin):
    """把 64×64 皮肤按真实 UV 展开成四面拼图，逐像素自检用。"""
    F = {
        "head_top": (8, 0, 8, 8), "head_bottom": (16, 0, 8, 8),
        "head_left": (0, 8, 8, 8), "head_front": (8, 8, 8, 8),
        "head_right": (16, 8, 8, 8), "head_back": (24, 8, 8, 8),
        "body_top": (20, 16, 8, 4), "body_bottom": (28, 16, 8, 4),
        "body_left": (16, 20, 4, 12), "body_front": (20, 20, 8, 12),
        "body_right": (28, 20, 4, 12), "body_back": (32, 20, 8, 12),
        "rarm_front": (44, 20, 4, 12), "larm_front": (36, 52, 4, 12),
        "rleg_front": (4, 20, 4, 12), "lleg_front": (20, 52, 4, 12),
        "body_front_ov": (20, 36, 8, 12), "rleg_front_ov": (4, 36, 4, 12),
    }
    S = 16
    cols = 6
    order = list(F.keys())
    rows = (len(order) + cols - 1) // cols
    cw = max(F[f][2] for f in order) * S + 24
    ch = max(F[f][3] for f in order) * S + 24
    canvas = Image.new("RGBA", (cols * cw, rows * ch), (34, 34, 44, 255))
    d = ImageDraw.Draw(canvas)
    for i, f in enumerate(order):
        x, y, w, h = F[f]
        tile = skin.crop((x, y, x + w, y + h)).resize((w * S, h * S), Image.NEAREST)
        cx = (i % cols) * cw + 8
        cy = (i // cols) * ch + 8
        canvas.alpha_composite(tile, (cx, cy))
        d.text((cx, cy + h * S + 2), f, fill=(210, 210, 220, 255))
    return canvas


def render_preview(skin):
    """三视图（正 / 侧 / 背）示意，便于整体观感确认。"""
    S = 10
    cw, chh = 8 * S, 8 * S      # 头 8×8
    bw, bh = 8 * S, 12 * S      # 身 8×12
    aw, ah = 4 * S, 12 * S      # 臂 4×12
    lw, lh = 4 * S, 12 * S      # 腿 4×12
    total_w = cw + aw * 2
    total_h = chh + bh + lh
    canvas = Image.new("RGBA", (total_w * 3 + 80, total_h + 40), (42, 42, 54, 255))

    def blit(face_rect, dest_x, dest_y):
        x, y, w, h = face_rect
        canvas.alpha_composite(
            skin.crop((x, y, x + w, y + h)).resize((w * S, h * S), Image.NEAREST),
            (dest_x, dest_y))

    views = [
        # (头正, 身正, 右臂正, 左臂正, 右腿正, 左腿正)
        ((8, 8, 8, 8), (20, 20, 8, 12), (44, 20, 4, 12), (36, 52, 4, 12),
         (4, 20, 4, 12), (20, 52, 4, 12)),
        # 侧面
        ((16, 8, 8, 8), (28, 20, 4, 12), (48, 20, 4, 12), (40, 52, 4, 12),
         (8, 20, 4, 12), (24, 52, 4, 12)),
        # 背面
        ((24, 8, 8, 8), (32, 20, 8, 12), (52, 20, 4, 12), (44, 52, 4, 12),
         (12, 20, 4, 12), (28, 52, 4, 12)),
    ]
    for vi, (hf, bf, raf, laf, rlf, llf) in enumerate(views):
        ox = vi * (total_w + 26) + 12
        oy = 20
        blit(hf, ox + aw, oy)
        blit(bf, ox + aw, oy + chh)
        blit(raf, ox, oy + chh)
        blit(laf, ox + aw + bw, oy + chh)
        blit(rlf, ox + aw + S * 2, oy + chh + bh)
        blit(llf, ox + aw + bw - S * 6, oy + chh + bh)
    return canvas


def build_icon():
    """皮肤核心图标：16×16 头部特写，与既有皮肤核心（skin_core_18/22/23）同规格。

    规格约定（从既有图标反推）：
      - 画布 16×16，贴图中心是脸部
      - 头顶占 y=0..8，脸占 y=8..14，衣领/胸口占 y=14..16
      - 两侧 2px 是头发（外轮廓），中间 12px 是脸
    """
    icon = Image.new("RGBA", (16, 16), P["none"])
    # 两侧银白长发包住整个头部
    rect(icon, (0, 0, 16, 14), P["hair"])
    rect(icon, (0, 0, 2, 16), P["hair_shadow"])
    rect(icon, (14, 0, 16, 16), P["hair_shadow"])
    # 头顶高光
    rect(icon, (3, 0, 13, 2), P["hair_mid"])
    rect(icon, (5, 0, 11, 3), P["hair_light"])
    # 头顶两侧的黑角
    px(icon, 0, 0, P["black"])
    px(icon, 1, 0, P["black"])
    px(icon, 15, 0, P["black"])
    px(icon, 14, 0, P["black"])
    # 面部
    rect(icon, (2, 5, 14, 14), P["skin"])
    rect(icon, (2, 13, 14, 14), P["skin_shadow"])
    # 刘海盖到额头
    rect(icon, (2, 5, 14, 8), P["hair"])
    rect(icon, (2, 8, 14, 9), P["hair"])       # 齐刘海下缘
    px(icon, 7, 9, P["hair_light"])            # 中缝挑染
    px(icon, 8, 9, P["hair_mid"])
    # 两侧鬓发压住脸颊外缘
    rect(icon, (2, 5, 3, 14), P["hair_shadow"])
    rect(icon, (13, 5, 14, 14), P["hair_shadow"])
    # 金瞳（每只 2×2），位置与大凤系图标一致
    for ex in (4, 10):
        rect(icon, (ex, 9, ex + 2, 10), P["lash"])
        px(icon, ex, 10, P["eye_deep"])
        px(icon, ex + 1, 10, P["eye_light"])
        px(icon, ex, 11, P["eye_light"])
        px(icon, ex + 1, 11, P["eye_deep"])
    # 衣领（黑披风 + 白衬衫领 + 酒红胸衣）
    rect(icon, (0, 14, 16, 16), P["black"])
    rect(icon, (1, 14, 15, 15), P["black_light"])
    rect(icon, (6, 14, 10, 16), P["white"])
    rect(icon, (5, 15, 11, 16), P["crimson"])
    return icon


# ---------------------------------------------------------------- 主流程

def main():
    base = Image.open(TAIHOU_SKIN).convert("RGBA")
    skin = Image.new("RGBA", (SIZE, SIZE), P["none"])

    build_head(skin, base)
    build_body(skin, base)

    # (0,0)-(8,16) 是 64×64 皮肤里未使用的 UV 区（右腿 top/bottom 左侧的空白），
    # 大凤原图在该处有内容；这里清空，避免其它渲染器/工具误读成有效贴图。
    rect(skin, (0, 0, 8, 16), P["none"])

    SKIN_OUT.parent.mkdir(parents=True, exist_ok=True)
    skin.save(SKIN_OUT)

    icon = build_icon()
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(ICON_OUT)

    PREVIEW_OUT.parent.mkdir(parents=True, exist_ok=True)
    render_preview(skin).save(PREVIEW_OUT)
    render_sheet(skin).save(SHEET_OUT)
    print(f"已生成 {SKIN_OUT}")
    print(f"已生成 {ICON_OUT}")
    print(f"已生成 {PREVIEW_OUT}")
    print(f"已生成 {SHEET_OUT}")


if __name__ == "__main__":
    main()
