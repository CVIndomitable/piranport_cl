#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""舰装 Bedrock 模型构建器（GeckoLib）。

这一轮产出「通用舰装架」：背部基座 + 烟囱/桅杆 + 左右机械臂（末端两个炮位平台）。
之后航母甲板 / 腰部鱼雷架复用同一套 BedrockModel + Atlas，不用另起炉灶。

约定（与 RiggingAttachedLayer 一致，别搞反）：
    1 单位 = 1/16 格；原点 = 躯干 body pivot；
    +Y 朝上（游戏里先 Axis.XP(180) 翻转，所以模型 +Y 对应玩家 -Y）；
    -Z 朝前、+Z 朝后；+X 左舷、-X 右舷。

贴图策略：每个面的 uv_size 严格等于面尺寸（1 单位 = 1 像素），
所有唯一 (材质, 宽, 高) 面自动打包进 64×64；纹理细节只靠面板缝/铆钉/炮座环，
不堆几何。几何总块数控制在 30 以内。

用法：
    python3 build_rigging_hull_models.py           # 写 geo/png/animation
    python3 build_rigging_hull_models.py --dry-run # 只打印统计，不落盘
"""
import argparse
import json
import os
from dataclasses import dataclass, field

from PIL import Image

# ---------------------------------------------------------------- 调色板
# 与 sy1_launcher 同一套蓝灰家族：10 个不透明色 + 透明，够用且不脏。
PALETTE = {
    "shadow": (36, 38, 44, 255),
    "dark": (80, 84, 94, 255),
    "mid": (118, 123, 136, 255),
    "base": (152, 157, 168, 255),
    "light": (188, 193, 202, 255),
    "pale": (222, 226, 232, 255),
    "gold": (184, 146, 74, 255),
    "gold_dark": (122, 94, 48, 255),
    "red": (160, 58, 50, 255),
    "red_dark": (110, 42, 36, 255),
}

# 材质 -> (填充色, 描边色, 内缝色)
MATERIALS = {
    "armor_light": ("light", "mid", "base"),
    "armor_mid": ("base", "dark", "mid"),
    "armor_white": ("pale", "light", "light"),
    "metal_dark": ("dark", "shadow", "base"),
    "trim_gold": ("gold", "gold_dark", "gold_dark"),
    "red_accent": ("red", "red_dark", "red_dark"),
    # 特殊绘制
    "platform_ring": None,
    "funnel_mouth": None,
    "vent_panel": None,
    "gold_stripe": None,
}

FACES = ("north", "south", "east", "west", "up", "down")


def face_dims(size):
    """面尺寸：(宽, 高)。Bedrock 的 north/east 等面按此取 uv_size。"""
    w, h, d = size
    return {
        "north": (w, h), "south": (w, h),
        "east": (d, h), "west": (d, h),
        "up": (w, d), "down": (w, d),
    }


# ---------------------------------------------------------------- 贴图绘制
def _fill(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color


def _hline(px, x0, y, w, color):
    for x in range(x0, x0 + w):
        px[x, y] = color


def _vline(px, x, y0, h, color):
    for y in range(y0, y0 + h):
        px[x, y] = color


def _paint_ring(px, x0, y0, w, h):
    """炮座顶面：一圈暗色环 + 中心亮毂。尺寸太小就退化成「暗底亮线」。"""
    light, dark, pale = PALETTE["light"], PALETTE["dark"], PALETTE["pale"]
    _fill(px, x0, y0, w, h, light)
    if w == 2 and h == 3:
        # 2×3 能画出最小的「凹座」：上下暗、中间亮
        _hline(px, x0, y0, w, dark)
        _hline(px, x0, y0 + 2, w, dark)
        _hline(px, x0, y0 + 1, w, pale)
        return
    if w <= 2 or h <= 2:
        _hline(px, x0, y0 + h - 1, w, dark)
        return
    rx = (w - 1) / 2.0
    ry = (h - 1) / 2.0
    for y in range(h):
        for x in range(w):
            dx = (x - rx) / (rx + 0.5)
            dy = (y - ry) / (ry + 0.5)
            r2 = dx * dx + dy * dy
            if r2 > 0.82:
                px[x0 + x, y0 + y] = dark
            elif r2 < 0.18:
                px[x0 + x, y0 + y] = pale


def _paint_mouth(px, x0, y0, w, h):
    """烟囱口：黄铜边 + 深色内膛。"""
    gold_dark, shadow, dark = PALETTE["gold_dark"], PALETTE["shadow"], PALETTE["dark"]
    _fill(px, x0, y0, w, h, shadow)
    if w == 4 and h == 2:
        _hline(px, x0, y0, w, gold_dark)
        px[x0, y0 + 1] = gold_dark
        px[x0 + w - 1, y0 + 1] = gold_dark
        _hline(px, x0 + 1, y0 + 1, w - 2, dark)
        return
    for x in range(w):
        px[x0 + x, y0] = gold_dark
        px[x0 + x, y0 + h - 1] = gold_dark
    for y in range(h):
        px[x0, y0 + y] = gold_dark
        px[x0 + w - 1, y0 + y] = gold_dark


def _paint_vent(px, x0, y0, w, h):
    """装甲背板上的散热格栅：横向暗槽，槽间留 1px 亮线。"""
    base, dark, mid = PALETTE["base"], PALETTE["dark"], PALETTE["mid"]
    _fill(px, x0, y0, w, h, base)
    for x in range(x0, x0 + w):
        px[x, y0] = dark
        px[x, y0 + h - 1] = dark
    # 格栅只放奇数行，避免 4 行高的面板全黑
    for y in range(y0 + 1, y0 + h - 1, 2):
        _hline(px, x0 + 1, y, w - 2, dark)
    for y in range(y0 + 2, y0 + h - 1, 2):
        _hline(px, x0 + 1, y, w - 2, mid)


def _paint_gold_stripe(px, x0, y0, w, h):
    """舷侧装甲：中段一条黄铜饰条，对应原画的金属配色。"""
    base, dark, gold = PALETTE["base"], PALETTE["dark"], PALETTE["gold"]
    _fill(px, x0, y0, w, h, base)
    for x in range(x0, x0 + w):
        px[x, y0] = dark
        px[x, y0 + h - 1] = dark
    if h >= 3:
        _hline(px, x0, y0 + h // 2, w, gold)


def paint_face(px, x0, y0, w, h, material):
    """按材质画一个面区域。纯色块，无抗锯齿、无噪点。"""
    if material == "platform_ring":
        _paint_ring(px, x0, y0, w, h)
        return
    if material == "funnel_mouth":
        _paint_mouth(px, x0, y0, w, h)
        return
    if material == "vent_panel":
        _paint_vent(px, x0, y0, w, h)
        return
    if material == "gold_stripe":
        _paint_gold_stripe(px, x0, y0, w, h)
        return

    spec = MATERIALS[material]
    fill_name, border_name, seam_name = spec
    fill = PALETTE[fill_name]
    border = PALETTE[border_name]
    seam = PALETTE[seam_name]
    _fill(px, x0, y0, w, h, fill)

    if w >= 4 and h >= 3:
        # 大面板：1px 面板缝 + 中缝
        for x in range(x0, x0 + w):
            px[x, y0] = border
            px[x, y0 + h - 1] = border
        for y in range(y0, y0 + h):
            px[x0, y] = border
            px[x0 + w - 1, y] = border
        if w >= 6 and h >= 4:
            _hline(px, x0 + 1, y0 + h // 2, w - 2, seam)
        if h >= 6 and w >= 5:
            _vline(px, x0 + w // 2, y0 + 1, h - 2, seam)
    elif h >= 2 and w >= 2:
        # 窄面：只压底边，避免 2×2 全变成描边色
        _hline(px, x0, y0 + h - 1, w, border)
        if w >= 3:
            _vline(px, x0 + w - 1, y0, h, border)
        elif w == 2:
            px[x0 + 1, y0 + h - 1] = border
    elif h >= 2:
        # 1×N
        px[x0, y0] = border
        px[x0, y0 + h - 1] = border
    elif w >= 2:
        # N×1
        px[x0, y0] = border
        px[x0 + w - 1, y0] = border
    else:
        px[x0, y0] = fill


# ---------------------------------------------------------------- 模型数据
@dataclass
class Cube:
    origin: tuple
    size: tuple
    material: str
    face_materials: dict = field(default_factory=dict)
    rotation: tuple = None
    pivot: tuple = None


@dataclass
class Bone:
    name: str
    parent: str = None
    pivot: tuple = (0, 0, 0)
    rotation: tuple = None
    cubes: list = field(default_factory=list)


class BedrockModel:
    """最小可用的 Bedrock geo 构造器。

    - 每个 cube 可单独指定面材质（例如顶面炮座环）；
    - 贴图区域按 (材质, 宽, 高) 去重后自动打包进 64×64；
    - 面 uv_size 严格等于面尺寸 × texel_scale，不拉伸。

    texel_scale 是「1 模型单位用几个纹理像素」：1 = 和 sy1 一样 1:1；
    本模型的面很小，用 2 可以把 64×64 用满一些，面板缝仍然是 1px。
    """

    def __init__(self, identifier, texture_size=64,
                 visible_bounds=(2, 2, (0, 0.2, 0)), texel_scale=1):
        self.identifier = identifier
        self.texture_size = texture_size
        self.visible_bounds = visible_bounds
        self.texel_scale = int(texel_scale)
        self.bones = []
        self._by_name = {}

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        b = Bone(name=name, parent=parent, pivot=tuple(pivot),
                 rotation=tuple(rotation) if rotation else None)
        self.bones.append(b)
        self._by_name[name] = b
        return b

    def cube(self, bone, origin, size, material, face_materials=None,
             rotation=None, pivot=None):
        assert all(int(v) == v for v in size), f"面尺寸必须是整数: {size}"
        assert material in MATERIALS, material
        for f, m in (face_materials or {}).items():
            assert f in FACES, f
            assert m in MATERIALS, m
        self._by_name[bone].cubes.append(Cube(
            origin=tuple(float(v) for v in origin),
            size=tuple(int(v) for v in size),
            material=material,
            face_materials=dict(face_materials or {}),
            rotation=tuple(rotation) if rotation else None,
            pivot=tuple(pivot) if pivot else None,
        ))

    # ---- 贴图打包 ----
    def _region_keys(self):
        keys = []
        seen = set()
        s = self.texel_scale
        for b in self.bones:
            for c in b.cubes:
                dims = face_dims(c.size)
                for f in FACES:
                    mat = c.face_materials.get(f, c.material)
                    w, h = dims[f]
                    key = (mat, int(w * s), int(h * s))
                    if key not in seen:
                        seen.add(key)
                        keys.append(key)
        return keys

    @staticmethod
    def _pack(keys, size, gap=0):
        """简单货架打包：按高度降序，放不下就换行。"""
        placed = {}
        x = y = row_h = 0
        for key in sorted(keys, key=lambda k: (-k[2], -k[1], k[0])):
            w, h = key[1], key[2]
            if x + w > size:
                x = 0
                y += row_h + gap
                row_h = 0
            if y + h > size:
                raise RuntimeError(
                    f"贴图 64×64 放不下：失败于 {key}（已放 {len(placed)} 块）")
            placed[key] = (x, y)
            x += w + gap
            row_h = max(row_h, h)
        return placed

    def build_texture(self):
        keys = self._region_keys()
        placed = self._pack(keys, self.texture_size)
        img = Image.new("RGBA", (self.texture_size, self.texture_size), (0, 0, 0, 0))
        px = img.load()
        for (mat, w, h), (x, y) in placed.items():
            paint_face(px, x, y, w, h, mat)
        used = sum(w * h for (_, w, h) in keys)
        return img, placed, used

    # ---- 输出 ----
    def _clean(self, v):
        if isinstance(v, float) and v.is_integer():
            return int(v)
        if isinstance(v, float):
            # 3 位小数足够，避免 -0.0
            r = round(v, 3)
            return 0 if r == 0 else r
        return v

    def geo_dict(self, placed):
        s = self.texel_scale
        bones = []
        for b in self.bones:
            entry = {"name": b.name}
            if b.parent:
                entry["parent"] = b.parent
            entry["pivot"] = [self._clean(v) for v in b.pivot]
            if b.rotation:
                entry["rotation"] = [self._clean(v) for v in b.rotation]
            if b.cubes:
                cubes = []
                for c in b.cubes:
                    dims = face_dims(c.size)
                    uv = {}
                    for f in FACES:
                        mat = c.face_materials.get(f, c.material)
                        w, h = dims[f]
                        u, v = placed[(mat, int(w * s), int(h * s))]
                        uv[f] = {
                            "uv": [self._clean(u), self._clean(v)],
                            "uv_size": [self._clean(w * s), self._clean(h * s)],
                        }
                    cube = {
                        "origin": [self._clean(v) for v in c.origin],
                        "size": [self._clean(v) for v in c.size],
                        "uv": uv,
                    }
                    if c.rotation:
                        cube["rotation"] = [self._clean(v) for v in c.rotation]
                    if c.pivot:
                        cube["pivot"] = [self._clean(v) for v in c.pivot]
                    cubes.append(cube)
                entry["cubes"] = cubes
            bones.append(entry)

        w, h, off = self.visible_bounds
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": self.identifier,
                    "texture_width": self.texture_size,
                    "texture_height": self.texture_size,
                    "visible_bounds_width": w,
                    "visible_bounds_height": h,
                    "visible_bounds_offset": [self._clean(v) for v in off],
                },
                "bones": bones,
            }],
        }

    def stats(self):
        cubes = sum(len(b.cubes) for b in self.bones)
        mats = sorted({c.material for b in self.bones for c in b.cubes} |
                      {m for b in self.bones for c in b.cubes
                       for m in c.face_materials.values()})
        return cubes, len(self.bones), mats


# ---------------------------------------------------------------- 通用舰装架
def build_general_hull():
    """7 号原画的通用火炮舰装架：背部基座 + 烟囱桅杆 + 左右机械臂。

    尺寸全部对齐 RiggingSocketTransforms.mountPose（模型空间 = 玩家空间 Y/Z 取反）：
      SIDE_LEFT_1  (4.0, -0.2, -0.4)   SIDE_LEFT_2  (4.0, -3.6,  0.4)
      SIDE_RIGHT_1 (-4.0,-0.2, -0.4)   SIDE_RIGHT_2 (-4.0,-3.6,  0.4)
      BACK_1       (2.2, -2.2,  3.4)   BACK_2       (-2.2,-2.2,  3.4)
    平台顶面正好落在挂点 y 上；机械臂主体藏在玩家手臂后方（z≥1），
    从正面不会挡住太多。
    """
    m = BedrockModel("geometry.general_hull", texture_size=64,
                     visible_bounds=(2, 2, (0, 0.25, 0)), texel_scale=2)
    m.bone("root")
    m.bone("hull_base", parent="root", pivot=(0, 0, 0))
    m.bone("stack", parent="hull_base", pivot=(0, 0, 4))
    m.bone("arm_left", parent="hull_base", pivot=(4, -1.8, 1.5))
    m.bone("arm_right", parent="hull_base", pivot=(-4, -1.8, 1.5))

    # ---- 背部基座（贴着躯干背面 z=2，向下覆盖到玩家 y≈4.6）----
    m.cube("hull_base", (-4, -4.6, 2), (8, 4, 1), "armor_light",         # 背板
           {"south": "vent_panel"})
    m.cube("hull_base", (-4, -0.8, 1.6), (8, 1, 1), "armor_light",      # 肩轭 + 红饰条
           {"north": "red_accent", "south": "red_accent"})
    m.cube("hull_base", (-2, -1.4, 3), (4, 2, 2), "armor_light")        # 烟囱基座
    m.cube("hull_base", (-1.5, -2.2, 3.2), (3, 1, 1), "metal_dark")      # BACK 挂点横梁
    # BACK_1 / BACK_2：2×2 小平台，顶面 y=-2.2 与挂点齐平
    m.cube("hull_base", (1, -3.2, 2.6), (2, 1, 2), "armor_light",
           {"up": "platform_ring"})
    m.cube("hull_base", (-3, -3.2, 2.6), (2, 1, 2), "armor_light",
           {"up": "platform_ring"})

    # ---- 烟囱 / 桅杆（用贴图表现细节，几何只分 6 块）----
    m.cube("stack", (-1.5, 0.6, 3), (3, 3, 2), "armor_light")           # 烟囱下段
    m.cube("stack", (-1, 3.6, 3), (2, 1, 2), "armor_light")             # 烟囱上段收窄
    m.cube("stack", (-2, 4.4, 2.8), (4, 1, 2), "armor_white",           # 烟囱冠
           {"up": "funnel_mouth", "down": "armor_mid"})
    m.cube("stack", (-0.5, 5.4, 4.2), (1, 11, 1), "armor_light")        # 桅杆
    m.cube("stack", (-3, 13.4, 4.2), (6, 1, 1), "armor_mid")            # 横桁
    m.cube("stack", (-0.5, 16.4, 4.2), (1, 1, 1), "trim_gold")          # 桅顶金饰

    # ---- 左右机械臂（镜像）；平台顶面 = 挂点 y ----
    def arm(side, sign):
        # side: "left"/"right"；sign: +1 左舷 / -1 右舷
        mirror = (lambda o, s: ((-(o[0] + s[0]), o[1], o[2]), s)) if sign < 0 else \
                 (lambda o, s: (o, s))
        b = f"arm_{side}"
        # 横臂：从背板伸到外侧立柱，藏在玩家手臂后面
        o, s = mirror((2, -2.4, 1), (3, 1, 1))
        m.cube(b, o, s, "armor_light")
        # 肩块：把横臂和上层平台连成一体，从背面看是机械肩甲
        o, s = mirror((3, -1.4, 1), (2, 2, 1))
        m.cube(b, o, s, "armor_light")
        # 外侧立柱
        o, s = mirror((4, -5.4, 1), (1, 4, 1))
        m.cube(b, o, s, "armor_mid")
        # 内侧立柱
        o, s = mirror((2.5, -5.4, 1), (1, 4, 1))
        m.cube(b, o, s, "armor_mid")
        # 上层炮位平台（SIDE_*_1 挂点）
        o, s = mirror((3, -1.2, -1.6), (2, 1, 3))
        m.cube(b, o, s, "armor_light", {"up": "platform_ring"})
        # 下层炮位平台（SIDE_*_2 挂点）
        o, s = mirror((3, -4.6, -1.1), (2, 1, 3))
        m.cube(b, o, s, "armor_light", {"up": "platform_ring"})
        # 底部船体块（半个船身的下缘），外侧面一条黄铜水线
        o, s = mirror((3, -5.4, -1.9), (2, 1, 4))
        m.cube(b, o, s, "armor_mid",
               {"east": "gold_stripe", "west": "gold_stripe"})

    arm("left", +1)
    arm("right", -1)
    return m


# ---------------------------------------------------------------- 动画
def build_animation():
    """idle：整机极轻浮动 + 烟囱/桅杆缓慢呼吸。

    注意几何里平台承载挂点，但武器不在这个模型里。浮动手感必须很小
    （<0.5/16 格），否则平台和武器会肉眼可见地脱开。
    """
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    "hull_base": {
                        "position": {
                            "0.0": [0, 0, 0],
                            "1.0": [0, 0.35, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0, -0.22, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "stack": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [-0.8, 0.5, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [0.6, -0.5, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "arm_left": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.3, 0, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.22, 0, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                    "arm_right": {
                        "rotation": {
                            "0.0": [0, 0, 0],
                            "1.0": [0.3, 0, 0],
                            "2.0": [0, 0, 0],
                            "3.0": [-0.22, 0, 0],
                            "4.0": [0, 0, 0],
                        }
                    },
                },
            }
        },
    }


# ---------------------------------------------------------------- 落盘
def write_assets(model, repo_root, dry_run=False):
    img, placed, used = model.build_texture()
    geo = model.geo_dict(placed)
    anim = build_animation()

    base = os.path.join(repo_root, "src/main/resources/assets/piranport")
    geo_path = os.path.join(base, "geo/rigging/hull/general_hull.geo.json")
    tex_path = os.path.join(base, "textures/rigging/hull/general_hull.png")
    anim_path = os.path.join(base, "animations/rigging/hull/general_hull.animation.json")

    cubes, bones, mats = model.stats()
    print(f"cubes={cubes} bones={bones} materials={len(mats)}")
    print("materials:", ", ".join(mats))
    print(f"atlas regions={len(placed)} used_px={used}/4096 "
          f"({used / 4096 * 100:.1f}%)")
    print(f"palette opaque={len(PALETTE)}")

    if dry_run:
        print("dry-run: 不写文件")
        return

    for p in (geo_path, tex_path, anim_path):
        os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(geo_path, "w", encoding="utf-8") as f:
        json.dump(geo, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    img.save(tex_path)
    with open(anim_path, "w", encoding="utf-8") as f:
        json.dump(anim, f, ensure_ascii=False, indent="\t")
        f.write("\n")
    print("wrote", geo_path)
    print("wrote", tex_path)
    print("wrote", anim_path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    write_assets(build_general_hull(), repo_root, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
