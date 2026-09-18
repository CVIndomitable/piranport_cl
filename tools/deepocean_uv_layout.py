"""深海驱逐 贴图 UV 布局 —— 模型与贴图生成器共用的唯一事实来源.

每个条目 = 一个 box, 记录它的 texOffs 与尺寸.
MC 的 box UV 展开规则:
    以 (u, v) 为左上角, 占用宽 2*(W+D), 高 (D+H) 的矩形.
    其中: 上/下面 = W x D, 前/后面 = W x H, 左/右面 = D x H.
LC_UV 表把每个 box 的"正面(face)"区域单独标出, 便于画脸/图案.

用法: 模型侧 addBox 用这里的 (W,H,D,U,V); 贴图侧按同一 (U,V) 与展开尺寸绘制.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Box:
    name: str
    w: int
    h: int
    d: int
    u: int
    v: int

    @property
    def atlas_w(self) -> int:
        return 2 * (self.w + self.d)

    @property
    def atlas_h(self) -> int:
        return self.d + self.h

    # MC 展开的六个面 (x0, y0, x1, y1)
    @property
    def top(self):
        return (self.u + self.d, self.v, self.u + self.d + self.w, self.v + self.d)

    @property
    def bottom(self):
        u = self.u + self.d + self.w
        return (u, self.v, u + self.w, self.v + self.d)

    @property
    def right(self):
        return (self.u, self.v + self.d, self.u + self.d, self.v + self.d + self.h)

    @property
    def front(self):
        u = self.u + self.d
        return (u, self.v + self.d, u + self.w, self.v + self.d + self.h)

    @property
    def left(self):
        u = self.u + self.d + self.w
        return (u, self.v + self.d, u + self.d, self.v + self.d + self.h)

    @property
    def back(self):
        u = self.u + self.d + self.w + self.d
        return (u, self.v + self.d, u + self.w, self.v + self.d + self.h)

    @property
    def rect(self):
        return (self.u, self.v, self.u + self.atlas_w, self.v + self.atlas_h)


# ============================================================ 布局表
# 约束: 各 box 的 rect 不得重叠; 全部落在 128x128 内.
# 命名与 DeepOceanDestroyerModel 的 part 对应.

LAYOUT = [
    # ---- 人物 (左上区 0..64 x 0..64) ----
    Box("body",        5, 6, 3,   0,  0),   # rect x0..16  y0..9
    Box("waist",       4, 2, 2,  18,  0),   # rect x18..30 y0..4
    Box("hips",        4, 1, 2,  18,  6),   # rect x18..30 y6..9
    Box("head",        5, 5, 4,   0, 24),   # rect x0..18  y24..33
    Box("chin",        4, 1, 1,   0, 36),   # rect x0..10  y36..38
    Box("hair_top",    5, 1, 4,  20, 24),   # rect x20..38 y24..29
    Box("bangs",       3, 4, 1,  20, 32),   # rect x20..28 y32..37
    Box("hair_back",   4, 6, 3,  40, 24),   # rect x40..54 y24..33
    Box("hair_sideL",  2, 6, 3,  56, 24),   # rect x56..66 y24..33  (越界? 见下)
    Box("hair_sideR",  2, 6, 3,  56, 36),   # rect x56..66 y36..45

    # ---- 手臂 ----
    Box("armL_upper",  2, 5, 2,   0, 42),   # rect x0..8   y42..49
    Box("armL_lower",  2, 3, 2,   0, 50),   # rect x0..8   y50..55
    Box("armR_upper",  2, 5, 2,  10, 42),   # rect x10..18 y42..49
    Box("armR_lower",  2, 3, 2,  10, 50),   # rect x10..18 y50..55

    # ---- 腿部六边形鳞甲 ----
    Box("legL_upper",  2, 3, 2,  20, 42),
    Box("legL_lower",  2, 3, 2,  20, 50),
    Box("legR_upper",  2, 3, 2,  30, 42),
    Box("legR_lower",  2, 3, 2,  30, 50),

    # ---- 发光件 (64..96 x 0..24) ----
    Box("chest_core",  2, 2, 1,  64,  0),
    Box("face_glow",   3, 2, 1,  64,  8),
    Box("visor",       1, 2, 1,  74,  8),
    Box("eye_L",       1, 2, 1,  78,  8),

    # ---- 六边形鳞甲束 (触手) 48..64 x 48..96 太挤, 改放 96..128 x 64..96 ----
    Box("tentacle_a",  3, 3, 2,  96, 64),
    Box("tentacle_b",  3, 3, 2,  96, 72),
    Box("tentacle_c",  3, 3, 2, 108, 64),
    Box("tentacle_d",  3, 3, 2, 108, 72),

    # ---- 黑鱼 (96..128 x 0..64) ----
    Box("fish_body",   3, 5, 2,  96,  0),   # rect x96..106 y0..7
    Box("fish_fin",    3, 2, 2,  96,  9),   # rect x96..106 y9..13
    Box("fish_mount",  4, 3, 3, 108,  0),   # rect x108..122 y0..6
    Box("fish_gun",    1, 1, 6, 108,  8),   # rect x108..122 y8..15
    Box("tail_stem",   1, 4, 1,  96, 16),   # rect x96..100 y16..21
    Box("tail_fluke",  6, 2, 1, 102, 16),   # rect x102..116 y16..19

    # ---- 黑盾 (0..96 x 64..112) ----
    Box("shield_upper", 7, 4, 1,   0, 64),  # rect x0..16 y64..69
    Box("shield_left",  3, 7, 1,  18, 64),  # rect x18..26 y64..72
    Box("shield_right", 3, 7, 1,  28, 64),  # rect x28..36 y64..72
    Box("shield_lower", 5, 4, 1,  38, 64),  # rect x38..50 y64..69
    Box("shield_inner", 6, 6, 1,   0, 72),  # rect x0..14 y72..79

    # ---- 盾内粉色电路 ----
    Box("circuit_spoke", 1, 6, 1, 52, 64),
    Box("circuit_arcA",  6, 1, 1,  52, 72),
    Box("circuit_arcB",  7, 1, 1,  52, 76),

    # ---- 两侧炮舱 (0..64 x 80..128) ----
    Box("rig_L_body",  4, 2, 2,   0, 80),
    Box("rig_L_gun",   4, 1, 1,  12, 80),
    Box("rig_L_pod",   3, 3, 2,  22, 80),
    Box("rig_L_mount", 4, 1, 3,   0, 92),
    Box("rig_R_body",  4, 2, 2,   0, 100),
    Box("rig_R_gun",   4, 1, 1,  12, 100),
    Box("rig_R_pod",   3, 3, 2,  22, 100),
    Box("rig_R_mount", 4, 1, 3,   0, 112),

    # ---- 炮管电路发光 ----
    Box("gun_glow_a",  4, 2, 1,  36, 80),
    Box("gun_glow_b",  4, 2, 1,  36, 86),
    Box("pod_eye",     2, 1, 1,  36, 92),
]

BY_NAME = {b.name: b for b in LAYOUT}


def validate():
    """检查重叠与越界 —— 布局错了就该立刻发现, 而不是等渲染出花屏."""
    problems = []
    cells = {}
    for b in LAYOUT:
        x0, y0, x1, y1 = b.rect
        if x1 > 128 or y1 > 128:
            problems.append(f"{b.name}: 越界 {b.rect}")
        for y in range(y0, y1):
            for x in range(x0, x1):
                if (x, y) in cells:
                    problems.append(f"{b.name} 与 {cells[(x, y)]} 在 ({x},{y}) 重叠")
                cells[(x, y)] = b.name
    return problems


if __name__ == "__main__":
    errs = validate()
    if errs:
        print(f"{len(errs)} 个布局问题:")
        for e in sorted(set(errs))[:40]:
            print("  " + e)
    else:
        print("布局校验通过, 无重叠无越界")
    for b in LAYOUT:
        print(f"  {b.name:14s} {b.w}x{b.h}x{b.d}  texOffs({b.u},{b.v})  rect={b.rect}")
