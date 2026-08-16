#!/usr/bin/env python3
from __future__ import annotations

import math
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_OUT = ROOT / "src/main/resources/assets/piranport/textures/entity/shipgirl/unicorn.png"
PREVIEW_OUT = ROOT / "build/offline-renders/unicorn_ysm_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/unicorn_ysm_texture_sheet.png"


PALETTE = {
    "skin": (252, 218, 207, 255),
    "skin_shadow": (229, 182, 178, 255),
    "skin_light": (255, 235, 226, 255),
    "blush": (240, 145, 169, 255),
    "hair_light": (240, 248, 255, 255),
    "hair": (200, 225, 249, 255),
    "hair_shadow": (135, 174, 220, 255),
    "hair_deep": (91, 126, 187, 255),
    "white": (250, 250, 247, 255),
    "white_shadow": (201, 219, 231, 255),
    "lace": (225, 239, 247, 255),
    "silver": (143, 155, 163, 255),
    "silver_dark": (74, 86, 98, 255),
    "pink": (239, 114, 165, 255),
    "coral": (231, 91, 108, 255),
    "yellow": (244, 196, 52, 255),
    "mint": (87, 201, 176, 255),
    "green": (63, 151, 101, 255),
    "gold": (221, 168, 37, 255),
    "gold_dark": (164, 111, 28, 255),
    "red": (156, 35, 48, 255),
    "navy": (42, 51, 84, 255),
    "blue": (66, 145, 225, 255),
    "blue_light": (131, 213, 253, 255),
    "eye_dark": (37, 38, 75, 255),
    "black": (24, 27, 35, 255),
    "teal": (61, 177, 163, 255),
    "lavender": (167, 146, 216, 255),
    "transparent": (0, 0, 0, 0),
}


SWATCHES = {
    (128, 0): "hair_light",
    (160, 0): "hair",
    (192, 0): "hair_shadow",
    (224, 0): "skin",
    (128, 32): "white",
    (160, 32): "white_shadow",
    (192, 32): "lace",
    (224, 32): "silver",
    (128, 64): "pink",
    (160, 64): "coral",
    (192, 64): "yellow",
    (224, 64): "mint",
    (128, 96): "green",
    (160, 96): "gold",
    (192, 96): "gold_dark",
    (224, 96): "red",
    (128, 128): "navy",
    (160, 128): "blue",
    (192, 128): "eye_dark",
    (224, 128): "black",
    (128, 160): "teal",
    (160, 160): "hair_deep",
    (192, 160): "silver_dark",
    (224, 160): "lavender",
}


def shade(color: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
    r, g, b, a = color
    return (
        max(0, min(255, round(r * factor))),
        max(0, min(255, round(g * factor))),
        max(0, min(255, round(b * factor))),
        a,
    )


def rect(image: Image.Image, x: int, y: int, w: int, h: int,
         color: tuple[int, int, int, int]) -> None:
    ImageDraw.Draw(image).rectangle((x, y, x + w - 1, y + h - 1), fill=color)


def draw_head_uv(texture: Image.Image) -> None:
    p = PALETTE
    # Standard player head UV at 2x density.
    regions = {
        "top": (16, 0, 16, 16),
        "bottom": (32, 0, 16, 16),
        "right": (0, 16, 16, 16),
        "front": (16, 16, 16, 16),
        "left": (32, 16, 16, 16),
        "back": (48, 16, 16, 16),
    }
    rect(texture, *regions["top"], p["hair_light"])
    rect(texture, *regions["bottom"], p["hair_shadow"])
    rect(texture, *regions["right"], p["hair"])
    rect(texture, *regions["left"], p["hair"])
    rect(texture, *regions["back"], p["hair_shadow"])
    rect(texture, *regions["front"], p["skin"])
    draw = ImageDraw.Draw(texture)

    ox, oy = 16, 16
    # Fine silver-blue fringe and face frame.
    draw.rectangle((ox, oy, ox + 15, oy + 2), fill=p["hair_light"])
    draw.rectangle((ox, oy + 2, ox + 1, oy + 15), fill=p["hair_shadow"])
    draw.rectangle((ox + 14, oy + 2, ox + 15, oy + 15), fill=p["hair"])
    fringe = ((2, 3, 3), (4, 3, 5), (6, 3, 3), (8, 3, 6),
              (10, 3, 4), (12, 3, 5), (13, 3, 3))
    for x, y, length in fringe:
        color = p["hair_light"] if x % 3 == 1 else p["hair"] if x % 2 else p["hair_shadow"]
        draw.line((ox + x, oy + y, ox + x, oy + y + length - 1), fill=color, width=1)

    # Four-pixel-tall anime eyes with lashes and highlights.
    for eye_x in (3, 10):
        draw.rectangle((ox + eye_x - 1, oy + 7, ox + eye_x + 3, oy + 7), fill=p["eye_dark"])
        draw.point((ox + eye_x - 1, oy + 8), fill=p["eye_dark"])
        draw.point((ox + eye_x + 3, oy + 8), fill=p["eye_dark"])
        draw.rectangle((ox + eye_x, oy + 8, ox + eye_x + 2, oy + 11), fill=p["blue"])
        draw.rectangle((ox + eye_x + 1, oy + 10, ox + eye_x + 2, oy + 11), fill=p["blue_light"])
        draw.point((ox + eye_x, oy + 8), fill=p["white"])
        draw.point((ox + eye_x + 2, oy + 11), fill=p["eye_dark"])
    draw.line((ox + 2, oy + 12, ox + 4, oy + 12), fill=p["blush"])
    draw.line((ox + 11, oy + 12, ox + 13, oy + 12), fill=p["blush"])
    draw.line((ox + 7, oy + 13, ox + 8, oy + 13), fill=(181, 82, 109, 255))

    for face_x in (0, 32, 48):
        for x in range(2, 16, 4):
            draw.line((face_x + x, 16, face_x + x - 1, 31),
                      fill=p["hair_light"] if x % 8 else p["hair_deep"])


def draw_body_uv(texture: Image.Image) -> None:
    p = PALETTE
    # Standard torso UV at 2x density.
    for region in ((40, 32, 16, 8), (56, 32, 16, 8), (32, 40, 8, 24),
                   (40, 40, 16, 24), (56, 40, 8, 24), (64, 40, 16, 24)):
        rect(texture, *region, p["white"])
    draw = ImageDraw.Draw(texture)
    ox, oy = 40, 40
    draw.rectangle((ox + 2, oy, ox + 13, oy + 3), fill=p["skin"])
    draw.line((ox + 2, oy + 3, ox + 6, oy + 8), fill=p["white_shadow"], width=2)
    draw.line((ox + 13, oy + 3, ox + 9, oy + 8), fill=p["white_shadow"], width=2)
    draw.rectangle((ox + 7, oy + 7, ox + 8, oy + 10), fill=p["coral"])
    draw.rectangle((ox + 1, oy + 19, ox + 14, oy + 23), fill=p["lace"])
    for x, color in ((3, p["pink"]), (6, p["yellow"]), (9, p["mint"]), (12, p["coral"])):
        draw.rectangle((ox + x, oy + 19, ox + x + 1, oy + 20), fill=color)
        draw.point((ox + x + 1, oy + 21), fill=p["green"])
    for side_x in (32, 56, 64):
        draw.line((side_x + 1, 42, side_x + 1, 62), fill=p["white_shadow"])


def create_texture() -> Image.Image:
    texture = Image.new("RGBA", (256, 256), PALETTE["transparent"])
    draw_head_uv(texture)
    draw_body_uv(texture)
    draw = ImageDraw.Draw(texture)
    for (x, y), color_name in SWATCHES.items():
        color = PALETTE[color_name]
        rect(texture, x, y, 32, 32, color)
        if color_name in {"hair_light", "hair", "hair_shadow", "hair_deep"}:
            for stripe in range(x + 3, x + 32, 6):
                draw.line((stripe, y, stripe - 2, y + 31), fill=shade(color, 1.07))
        elif color_name in {"white", "white_shadow", "lace"}:
            for row in range(y + 5, y + 32, 8):
                draw.line((x, row, x + 31, row), fill=shade(color, 0.97))
        elif color_name in {"gold", "gold_dark", "silver", "silver_dark"}:
            draw.line((x + 2, y + 2, x + 29, y + 2), fill=shade(color, 1.16))
            draw.line((x + 2, y + 29, x + 29, y + 29), fill=shade(color, 0.76))
    return texture


@dataclass
class Box:
    xyz: tuple[float, float, float]
    size: tuple[float, float, float]
    color: str


@dataclass
class Node:
    pivot: tuple[float, float, float] = (0.0, 0.0, 0.0)
    rotation: tuple[float, float, float] = (0.0, 0.0, 0.0)
    boxes: list[Box] = field(default_factory=list)
    children: list["Node"] = field(default_factory=list)
    parent: "Node | None" = None

    def child(self, pivot: tuple[float, float, float] = (0.0, 0.0, 0.0),
              rotation: tuple[float, float, float] = (0.0, 0.0, 0.0)) -> "Node":
        child = Node(pivot=pivot, rotation=rotation, parent=self)
        self.children.append(child)
        return child

    def box(self, xyz: tuple[float, float, float], size: tuple[float, float, float],
            color: str) -> "Node":
        self.boxes.append(Box(xyz, size, color))
        return self


def flower(parent: Node, x: float, y: float, z: float, color: str, scale: float) -> None:
    bloom = parent.child((x, y, z))
    for i in range(4):
        petal = bloom.child(rotation=(0.0, 0.0, i * math.pi / 2))
        petal.box((-0.58 * scale, -0.17 * scale, -0.16),
                  (1.16 * scale, 0.34 * scale, 0.32), color)
    bloom.box((-0.2 * scale, -0.2 * scale, -0.23),
              (0.4 * scale, 0.4 * scale, 0.46), "yellow")


def hair_strand(parent: Node, x: float, y: float, z: float, width: float,
                upper: float, tip_length: float, tilt: float, color: str) -> None:
    strand = parent.child((x, y, z), (0.045, 0.0, tilt))
    strand.box((-width / 2, 0.0, -0.38), (width, upper, 0.76), color)
    tip = strand.child((0.0, upper - 0.2, 0.0), (0.065, 0.0, tilt * 0.45))
    tip.box((-width * 0.4, 0.0, -0.31), (width * 0.8, tip_length, 0.62),
            "hair_shadow" if color != "hair_shadow" else "hair")


def face_pixels(head: Node) -> None:
    pixel = 7.1 / 16.0
    z = -3.575

    def add(px: int, py: int, w: int, h: int, color: str, depth: float = 0.055) -> None:
        head.box((-3.55 + px * pixel, -7.55 + py * pixel, z - depth),
                 (w * pixel, h * pixel, depth), color)

    # Eyelashes, iris gradients, blush, and mouth mirror the generated 16x16 face.
    for x in (2, 9):
        add(x, 7, 5, 1, "eye_dark")
        add(x, 8, 1, 1, "eye_dark")
        add(x + 4, 8, 1, 1, "eye_dark")
        add(x + 1, 8, 3, 4, "blue")
        add(x + 2, 10, 2, 2, "blue_light")
        add(x + 1, 8, 1, 1, "white")
    add(2, 12, 3, 1, "blush", 0.045)
    add(11, 12, 3, 1, "blush", 0.045)
    add(7, 13, 2, 1, "coral", 0.05)


def build_preview_model() -> Node:
    root = Node()
    head = root.child()
    body = root.child()
    right_arm = root.child((-3.25, 2.25, 0.0), (0.0, 0.0, -0.055))
    left_arm = root.child((3.25, 2.25, 0.0), (0.0, 0.0, 0.055))
    right_leg = root.child((-1.3, 12.0, 0.0))
    left_leg = root.child((1.3, 12.0, 0.0))

    head.box((-3.55, -7.55, -3.55), (7.1, 7.1, 7.1), "skin")
    face_pixels(head)
    head.box((-3.72, -7.82, -3.62), (7.44, 1.55, 7.24), "hair_light")
    head.box((-3.55, -7.68, -3.84), (7.1, 1.92, 0.42), "hair")
    head.box((-3.72, -7.68, 2.82), (7.44, 7.53, 0.82), "hair")
    head.box((-3.88, -7.68, -2.9), (0.72, 7.53, 5.8), "hair_shadow")
    head.box((3.16, -7.68, -2.9), (0.72, 7.53, 5.8), "hair_shadow")
    rear_x = (-3.05, -2.05, -1.02, 0.0, 1.02, 2.05, 3.05)
    rear_length = (6.25, 6.85, 7.15, 7.35, 7.1, 6.8, 6.2)
    for i, x in enumerate(rear_x):
        strand = head.child((x, -7.45, 3.55), (0.025, 0.0, (i - 3) * 0.025))
        strand.box((-0.48, 0.0, -0.2), (0.96, rear_length[i], 0.4),
                   "hair_shadow" if i % 3 == 0 else "hair_light" if i % 2 == 0 else "hair")
    for i, z in enumerate((-2.45, -0.85, 0.8, 2.35)):
        left_side = head.child((-3.82, -7.35, z), (0.02, 0.0, 0.035))
        left_side.box((-0.2, 0.0, -0.48), (0.4, 6.7 + i * 0.18, 0.96),
                      "hair" if i % 2 == 0 else "hair_shadow")
        right_side = head.child((3.82, -7.35, z), (0.02, 0.0, -0.035))
        right_side.box((-0.2, 0.0, -0.48), (0.4, 6.7 + i * 0.18, 0.96),
                       "hair" if i % 2 == 0 else "hair_shadow")
    bang_x = (-3.05, -2.2, -1.35, -0.48, 0.38, 1.25, 2.12, 2.98)
    bang_length = (2.3, 3.0, 2.6, 3.7, 3.2, 2.6, 3.0, 2.3)
    bang_tilt = (0.18, 0.11, 0.07, 0.03, -0.04, -0.08, -0.12, -0.18)
    hairline_x = (-2.85, -1.9, -0.95, 0.0, 0.95, 1.9, 2.85)
    hairline_y = (-5.95, -6.15, -5.82, -6.22, -5.88, -6.12, -5.94)
    for i, x in enumerate(hairline_x):
        hairline = head.child((x, hairline_y[i], -3.69), (0.0, 0.0, (i - 3) * 0.055))
        hairline.box((-0.5, -0.28, -0.18), (1.0, 0.56, 0.36),
                     "hair" if i % 2 == 0 else "hair_light")
    for i, x in enumerate(bang_x):
        bang = head.child((x, -6.85, -3.58), (-0.035, 0.0, bang_tilt[i]))
        bang.box((-0.36, 0.0, -0.22), (0.72, bang_length[i], 0.44),
                 "hair_light" if i % 3 == 1 else "hair")
    for i in range(4):
        top = head.child((-2.25 + i * 1.5, -7.72, -0.65),
                         (-0.16, 0.0, -0.2 + i * 0.08 if i < 2 else 0.12 + (i - 2) * 0.08))
        top.box((-0.34, -1.35, -0.32), (0.68, 1.55, 0.64),
                "hair_light" if i % 2 == 0 else "hair")
    for i in range(3):
        y = -6.25 + i * 1.75
        left_tuft = head.child((-3.62, y, 0.35), (0.04, 0.0, 0.34 + i * 0.05))
        left_tuft.box((-0.34, -0.2, -0.34), (0.68, 2.05, 0.68),
                      "hair_shadow" if i % 2 == 0 else "hair")
        right_tuft = head.child((3.62, y, 0.35), (0.04, 0.0, -0.34 - i * 0.05))
        right_tuft.box((-0.34, -0.2, -0.34), (0.68, 2.05, 0.68),
                       "hair_shadow" if i % 2 == 0 else "hair")

    left_lock = head.child((-3.52, -4.8, -2.65), (0.04, 0.08, 0.12))
    hair_strand(left_lock, -0.2, 0.0, 0.0, 1.15, 5.3, 4.0, 0.08, "hair")
    hair_strand(left_lock, 0.75, 0.75, -0.25, 0.72, 4.7, 3.1, -0.04, "hair_light")
    right_lock = head.child((3.52, -4.8, -2.65), (0.04, -0.08, -0.12))
    hair_strand(right_lock, 0.2, 0.0, 0.0, 1.15, 5.3, 4.0, -0.08, "hair")
    hair_strand(right_lock, -0.75, 0.75, -0.25, 0.72, 4.7, 3.1, 0.04, "hair_light")

    crown = head.child(rotation=(-0.08, 0.0, 0.0))
    crown.box((-3.35, -8.28, -3.15), (6.7, 0.22, 0.3), "green")
    for x, y, color, scale in ((-2.75, -8.35, "pink", 0.82), (-1.55, -8.52, "mint", 0.62),
                                (-0.2, -8.65, "yellow", 0.72), (1.2, -8.5, "coral", 0.65),
                                (2.62, -8.32, "pink", 0.95)):
        flower(crown, x, y, -3.3, color, scale)
    for x, y, color, scale in ((-3.72, -3.5, "pink", 0.7), (-3.82, -2.25, "yellow", 0.62),
                                (-3.7, -0.95, "mint", 0.56)):
        flower(head, x, y, -3.05, color, scale)

    # Fine upper hair panels and 9 articulated long strands.
    hair_root = body.child()
    for i, x in enumerate((-3.1, -2.05, -1.0, 0.0, 1.0, 2.05, 3.1)):
        panel = hair_root.child((x, -0.8, 2.95), (0.055, 0.0, (i - 3) * 0.022))
        panel.box((-0.58, 0.0, -0.35), (1.16, 6.4 + (i % 3) * 0.65, 0.7),
                  "hair_shadow" if i % 3 == 0 else "hair_light" if i % 2 == 0 else "hair")
    left_flow = hair_root.child(rotation=(0.045, -0.025, 0.0))
    for args in ((-3.35, 4.4, 1.0, 8.2, 6.4, 0.16, "hair_shadow"),
                 (-2.55, 4.0, 1.15, 9.0, 6.2, 0.1, "hair_light"),
                 (-1.62, 4.7, 0.92, 8.1, 5.5, 0.04, "hair")):
        hair_strand(left_flow, args[0], args[1], 3.15, args[2], args[3], args[4], args[5], args[6])
    center_flow = hair_root.child(rotation=(0.055, 0.0, 0.0))
    for args in ((-0.72, 4.9, 0.95, 8.7, 5.2, 0.025, "hair_light"),
                 (0.0, 4.5, 1.1, 9.1, 5.8, 0.0, "hair"),
                 (0.72, 4.9, 0.95, 8.7, 5.2, -0.025, "hair_light")):
        hair_strand(center_flow, args[0], args[1], 3.3, args[2], args[3], args[4], args[5], args[6])
    right_flow = hair_root.child(rotation=(0.045, 0.025, 0.0))
    for args in ((1.62, 4.7, 0.92, 8.1, 5.5, -0.04, "hair"),
                 (2.55, 4.0, 1.15, 9.0, 6.2, -0.1, "hair_light"),
                 (3.35, 4.4, 1.0, 8.2, 6.4, -0.16, "hair_shadow")):
        hair_strand(right_flow, args[0], args[1], 3.15, args[2], args[3], args[4], args[5], args[6])

    # Slender torso with layered bodice and lace collar.
    body.box((-2.7, 0.9, -1.25), (5.4, 10.1, 2.5), "white")
    body.box((-2.55, 0.55, -1.62), (5.1, 7.9, 0.34), "white")
    body.box((-2.1, 0.25, -1.82), (4.2, 1.25, 0.32), "white_shadow")
    body.box((-1.4, -0.05, -1.88), (2.8, 2.7, 0.3), "skin")
    body.box((-0.42, 1.55, -2.02), (0.84, 1.0, 0.25), "coral")
    for i in range(7):
        petal = body.child((-2.1 + i * 0.7, 1.15 + abs(i - 3) * 0.18, -2.05),
                           (0.0, 0.0, (i - 3) * 0.13))
        petal.box((-0.5, -0.18, -0.16), (1.0, 0.36, 0.32), "lace")
    body.box((-2.8, 7.85, -1.86), (5.6, 0.42, 3.72), "gold")
    body.box((-2.7, 8.32, -2.02), (5.4, 0.2, 0.24), "green")
    for x, color in zip((-2.05, -1.0, 0.05, 1.05, 2.1),
                        ("pink", "yellow", "mint", "coral", "pink")):
        flower(body, x, 8.34, -2.17, color, 0.55)
    for x, tilt in ((-1.55, -0.08), (1.55, 0.08)):
        seam = body.child((x, 2.65, -1.93), (0.0, 0.0, tilt))
        seam.box((-0.11, 0.0, -0.1), (0.22, 4.4, 0.2), "white_shadow")
    for i in range(3):
        body.box((-0.15, 3.05 + i * 1.25, -2.14), (0.3, 0.3, 0.24), "gold")

    # Hollow skirt shell made from thin front, side, and back folds.
    front_skirt = body.child((0.0, 8.25, -1.7), (-0.045, 0.0, 0.0))
    front_skirt.box((-2.25, 0.0, -0.22), (4.5, 10.5, 0.44), "white")
    front_skirt.box((-2.48, 4.15, -0.3), (4.96, 0.62, 0.58), "lace")
    front_skirt.box((-2.7, 9.55, -0.34), (5.4, 0.95, 0.66), "lace")
    for i, color in enumerate(("pink", "yellow", "mint", "coral", "pink")):
        flower(front_skirt, -1.75 + i * 0.88, 8.8 + abs(i - 2) * 0.2, -0.58, color, 0.5)
    left_skirt = body.child((-2.35, 8.15, -0.2), (0.0, -0.05, 0.16))
    left_skirt.box((-2.5, 0.0, -1.65), (2.75, 10.8, 0.38), "white_shadow")
    left_skirt.box((-2.68, 0.15, -1.45), (0.38, 10.45, 3.0), "white")
    left_skirt.box((-2.72, 9.75, -1.75), (3.05, 0.9, 0.58), "lace")
    flower(left_skirt, -2.8, 2.0, -1.72, "yellow", 0.62)
    flower(left_skirt, -2.75, 7.6, -1.72, "pink", 0.7)
    right_skirt = body.child((2.35, 8.15, -0.2), (0.0, 0.05, -0.16))
    right_skirt.box((-0.25, 0.0, -1.65), (2.75, 10.8, 0.38), "white_shadow")
    right_skirt.box((2.3, 0.15, -1.45), (0.38, 10.45, 3.0), "white")
    right_skirt.box((-0.33, 9.75, -1.75), (3.05, 0.9, 0.58), "lace")
    flower(right_skirt, 2.8, 2.0, -1.72, "mint", 0.62)
    flower(right_skirt, 2.75, 7.6, -1.72, "coral", 0.7)
    back_skirt = body.child((0.0, 8.2, 1.65), (0.07, 0.0, 0.0))
    back_skirt.box((-3.05, 0.0, -0.16), (6.1, 11.0, 0.36), "white_shadow")
    back_skirt.box((-3.35, 9.95, -0.22), (6.7, 0.98, 0.52), "lace")
    body.box((-3.15, 18.35, -1.72), (6.3, 1.0, 3.44), "lace")

    # Slim segmented arms, relaxed like the YSM references.
    for arm, side in ((right_arm, -1), (left_arm, 1)):
        arm.box((-0.85, -1.45, -1.0), (1.7, 5.5, 2.0), "skin")
        arm.box((-0.98, -1.62, -1.12), (1.96, 1.45, 2.24), "white")
        forearm = arm.child((0.0, 4.0, 0.0), (-0.13, 0.0, 0.0))
        forearm.box((-0.8, 0.0, -0.92), (1.6, 5.35, 1.84), "skin")
        forearm.box((-0.95, 0.48, -1.08), (1.9, 4.82, 2.16), "white")
        forearm.box((-1.08, 0.2, -1.18), (2.16, 0.7, 2.36), "lace")
        flower(forearm, side * 0.92, 1.15, -1.28, "mint" if side < 0 else "yellow", 0.42)

    for leg, side in ((right_leg, -1), (left_leg, 1)):
        leg.box((-1.1, 0.0, -1.2), (2.2, 11.8, 2.4), "skin")
        for i in range(4):
            vine = leg.child((0.0, 1.65 + i * 1.65, -1.3),
                             (0.0, 0.0, side * (0.58 if i % 2 == 0 else -0.58)))
            vine.box((-1.14, -0.12, -0.13), (2.28, 0.24, 0.26), "green" if i % 2 == 0 else "mint")
        leg.box((-1.28, 8.6, -1.78), (2.56, 3.25, 3.15), "white")
        leg.box((-1.4, 8.4, -1.48), (2.8, 0.6, 2.96), "lace")
        leg.box((-0.88, 9.45, -1.95), (1.76, 0.38, 0.48), "black")
        flower(leg, side * 1.12, 8.35, -1.42, "mint" if side < 0 else "pink", 0.46)

    # Waist ribbons.
    for x, tilt in ((-3.15, 0.18), (3.15, -0.18)):
        ribbon = body.child((x, 8.3, 1.92), (0.1, 0.0, tilt))
        ribbon.box((-0.375, 0.0, -0.18), (0.75, 6.8, 0.36), "white_shadow")
        lower = ribbon.child((-0.25 if x < 0 else 0.25, 6.2, 0.0),
                             (0.06, 0.0, -0.12 if x < 0 else 0.12))
        lower.box((-0.275, 0.0, -0.18), (0.55, 5.4, 0.36), "lace")

    # Multi-segment harp, mounted beside and behind the body.
    harp = body.child((8.0, -1.8, -1.6), (0.02, -0.12, -0.035))
    harp.box((-0.36, 0.0, -0.42), (0.72, 15.2, 0.84), "white")
    harp.box((-0.5, -0.15, -0.54), (1.0, 0.75, 1.08), "gold")
    for x, y, length, tilt in ((-0.05, 0.35, 3.0, -0.12), (-2.8, 0.92, 2.8, -0.28),
                               (-5.15, 2.15, 2.0, -0.52), (-6.25, 3.75, 1.7, -0.82)):
        beam = harp.child((x, y, 0.0), (0.0, 0.0, tilt))
        beam.box((-length, -0.32, -0.4), (length, 0.64, 0.8), "silver")
        beam.box((-length + 0.08, -0.4, -0.46), (length - 0.08, 0.18, 0.92), "hair_deep")
    sound = harp.child((-6.2, 12.6, 0.0), (0.0, 0.0, 0.25))
    sound.box((-0.25, -0.62, -0.52), (6.9, 1.24, 1.04), "silver")
    sound.box((1.2, 0.35, -0.58), (5.45, 1.25, 1.16), "red")
    sound.box((2.15, 1.3, -0.52), (4.45, 0.82, 1.04), "red")
    sound.box((3.2, 1.92, -0.44), (3.3, 0.48, 0.88), "gold_dark")
    string_colors = ("green", "yellow", "mint", "coral", "blue", "pink", "yellow", "mint")
    for i, color in enumerate(string_colors):
        string = harp.child((-5.75 + i * 0.67, 4.2 - i * 0.32, -0.06), (0.0, 0.0, -0.018))
        string.box((-0.075, 0.0, -0.11), (0.15, 7.3 + i * 0.42, 0.22), color)
    unicorn = harp.child((0.0, -0.65, 0.0), (0.0, 0.0, -0.06))
    unicorn.box((-0.35, -0.65, -0.52), (1.55, 1.3, 1.04), "white")
    unicorn.box((0.92, -0.32, -0.4), (0.85, 0.72, 0.8), "white")
    horn = unicorn.child((1.05, -0.45, -0.2), (0.0, 0.0, 0.48))
    horn.box((-0.12, -1.45, -0.12), (0.24, 1.55, 0.24), "gold")
    for i in range(5):
        wing = unicorn.child((-0.15, -0.05 + i * 0.28, 0.0),
                             (0.0, 0.0, -0.38 - i * 0.14))
        wing.box((-1.75 - i * 0.18, -0.18, -0.35),
                 (1.9 + i * 0.12, 0.36, 0.7), "gold" if i % 2 == 0 else "gold_dark")

    # Two small hummingbirds from the reference art.
    for x, y, z, facing in ((-7.2, -2.8, -0.8, 1), (9.0, 3.2, 1.8, -1)):
        bird = body.child((x, y, z), (0.0, -0.2 * facing, 0.0))
        bird.box((-0.6, -0.28, -0.32), (1.2, 0.56, 0.64), "teal")
        bird.box((0.35 if facing > 0 else -0.8, -0.48, -0.3), (0.45, 0.45, 0.6), "green")
        bird.box((0.75 if facing > 0 else -1.35, -0.32, -0.09), (0.6, 0.12, 0.18), "gold")
        wings = bird.child()
        wings.box((-0.15, -0.12, -1.45), (0.55, 0.24, 1.35), "blue")
        wings.box((-0.15, -0.12, 0.1), (0.55, 0.24, 1.35), "lavender")

    return root


def rotate_point(point: tuple[float, float, float], rotation: tuple[float, float, float]) -> tuple[float, float, float]:
    x, y, z = point
    rx, ry, rz = rotation
    c, s = math.cos(rx), math.sin(rx)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ry), math.sin(ry)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rz), math.sin(rz)
    x, y = x * c - y * s, x * s + y * c
    return x, y, z


def world_point(node: Node, point: tuple[float, float, float]) -> tuple[float, float, float]:
    current: Node | None = node
    result = point
    while current is not None:
        result = rotate_point(result, current.rotation)
        result = (result[0] + current.pivot[0], result[1] + current.pivot[1], result[2] + current.pivot[2])
        current = current.parent
    return result


FACES = (
    ((0, 1, 2, 3), 1.03),
    ((5, 4, 7, 6), 0.76),
    ((4, 0, 3, 7), 0.83),
    ((1, 5, 6, 2), 0.91),
    ((4, 5, 1, 0), 1.1),
    ((3, 2, 6, 7), 0.68),
)


def project(point: tuple[float, float, float], yaw: float, pitch: float,
            scale: float, cx: float, cy: float) -> tuple[float, float, float]:
    x, model_y, z = point
    y = 12.0 - model_y
    yaw_r = math.radians(yaw)
    pitch_r = math.radians(pitch)
    xr = math.cos(yaw_r) * x + math.sin(yaw_r) * z
    zr = -math.sin(yaw_r) * x + math.cos(yaw_r) * z
    yr = math.cos(pitch_r) * y - math.sin(pitch_r) * zr
    depth = math.sin(pitch_r) * y + math.cos(pitch_r) * zr
    return cx + xr * scale, cy - yr * scale, depth


def walk_boxes(node: Node):
    for box in node.boxes:
        yield node, box
    for child in node.children:
        yield from walk_boxes(child)


def render_view(model: Node, yaw: float, pitch: float, size: tuple[int, int]) -> Image.Image:
    supersample = 3
    width, height = size
    hi_size = (width * supersample, height * supersample)
    out = Image.new("RGBA", hi_size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(out, "RGBA")
    draw.ellipse((width * 0.2 * supersample, height * 0.91 * supersample,
                  width * 0.8 * supersample, height * 0.965 * supersample), fill=(63, 75, 89, 36))
    scale = min(width / 29.0, height / 33.0) * supersample
    polygons: list[tuple[float, list[tuple[float, float]], tuple[int, int, int, int]]] = []
    for node, box in walk_boxes(model):
        x, y, z = box.xyz
        w, h, d = box.size
        local = (
            (x, y, z), (x + w, y, z), (x + w, y + h, z), (x, y + h, z),
            (x, y, z + d), (x + w, y, z + d), (x + w, y + h, z + d), (x, y + h, z + d),
        )
        vertices = [project(world_point(node, point), yaw, pitch, scale,
                            width * supersample / 2, height * supersample * 0.51) for point in local]
        for indices, light in FACES:
            points = [(vertices[index][0], vertices[index][1]) for index in indices]
            depth = sum(vertices[index][2] for index in indices) / 4
            polygons.append((depth, points, shade(PALETTE[box.color], light)))
    for _, points, color in sorted(polygons, key=lambda item: item[0], reverse=True):
        draw.polygon(points, fill=color, outline=shade(color, 0.72), width=1)
    return out.resize(size, Image.Resampling.LANCZOS)


def label(draw: ImageDraw.ImageDraw, text: str, x: int, y: int) -> None:
    draw.rounded_rectangle((x - 8, y - 5, x + len(text) * 7 + 10, y + 17), radius=3,
                           fill=(249, 251, 252, 245), outline=(162, 174, 184, 255))
    draw.text((x, y), text, fill=(35, 45, 56, 255))


def write_preview(model: Node) -> None:
    PREVIEW_OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (1640, 900), (231, 235, 238, 255))
    draw = ImageDraw.Draw(canvas, "RGBA")
    draw.text((38, 24), "UNICORN / YSM-STYLE MODEL - OFFLINE ENGINEERING RENDER",
              fill=(31, 42, 54, 255))
    draw.text((38, 49), "fine-bone hair / hollow layered dress / articulated harp / front and back silhouette check",
              fill=(82, 96, 108, 255))
    views = (
        ("front 3/4", -26.0, 5.0, 12),
        ("front", 0.0, 3.0, 418),
        ("profile", 88.0, 4.0, 824),
        ("back 3/4", 154.0, 5.0, 1230),
    )
    for name, yaw, pitch, x in views:
        panel = render_view(model, yaw, pitch, (398, 760))
        canvas.alpha_composite(panel, (x, 82))
        label(draw, name, x + 24, 832)
    canvas.save(PREVIEW_OUT)


def write_sheet(texture: Image.Image) -> None:
    scale = 3
    sheet = Image.new("RGBA", (texture.width * scale, texture.height * scale + 38), (235, 238, 239, 255))
    sheet.alpha_composite(texture.resize((texture.width * scale, texture.height * scale), Image.Resampling.NEAREST))
    ImageDraw.Draw(sheet).text((8, texture.height * scale + 11),
                               "unicorn.png 256x256 / 2x player UV + custom material swatches",
                               fill=(35, 43, 52, 255))
    sheet.save(SHEET_OUT)


def main() -> None:
    texture = create_texture()
    TEXTURE_OUT.parent.mkdir(parents=True, exist_ok=True)
    texture.save(TEXTURE_OUT)
    model = build_preview_model()
    write_preview(model)
    write_sheet(texture)
    print(TEXTURE_OUT)
    print(PREVIEW_OUT)
    print(SHEET_OUT)


if __name__ == "__main__":
    main()
