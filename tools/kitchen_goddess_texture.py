#!/usr/bin/env python3
"""Generate and offline-render the hand-authored Kitchen Goddess model asset."""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/piranport/textures/entity/shipgirl/kitchen_goddess.png"
PREVIEW = ROOT / "build/offline-renders/kitchen_goddess_model_preview.png"
SHEET = ROOT / "build/offline-renders/kitchen_goddess_texture_sheet.png"

P = {
    "skin": (248, 205, 190, 255), "skin_shadow": (208, 143, 135, 255),
    "hair": (91, 55, 57, 255), "hair_light": (145, 84, 83, 255),
    "hair_dark": (54, 34, 42, 255), "white": (245, 238, 220, 255),
    "white_shadow": (210, 202, 185, 255), "coat": (166, 38, 48, 255),
    "coat_dark": (94, 25, 34, 255), "gold": (226, 177, 58, 255),
    "steel": (111, 126, 137, 255), "eye": (74, 42, 52, 255),
    "blue": (72, 126, 173, 255), "transparent": (0, 0, 0, 0),
}


def shade(color: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, round(channel * factor))) for channel in color[:3]) + (color[3],)


def rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: str) -> None:
    draw.rectangle((x, y, x + w - 1, y + h - 1), fill=P[color])


def create_texture() -> Image.Image:
    image = Image.new("RGBA", (256, 256), P["transparent"])
    d = ImageDraw.Draw(image)

    # Standard 64x64 slim-player UV in the upper-left quarter. The Java model
    # uses a 0.25 UV scale against this 256x256 sheet.
    for box, color in (
        ((8, 0, 8, 8), "hair"), ((16, 0, 8, 8), "hair_dark"),
        ((0, 8, 8, 8), "hair_light"), ((8, 8, 8, 8), "skin"),
        ((16, 8, 8, 8), "hair"), ((24, 8, 8, 8), "hair_dark"),
    ):
        rect(d, *box, color)
    # Face, fringe, eyes, blush and mouth.
    rect(d, 8, 8, 8, 2, "hair")
    rect(d, 8, 10, 1, 6, "hair_light")
    rect(d, 15, 10, 1, 6, "hair_dark")
    for x in (10, 13):
        rect(d, x, 11, 2, 2, "eye")
        d.point((x, 11), fill=P["blue"])
    d.point((9, 14), fill=P["skin_shadow"])
    d.point((14, 14), fill=P["skin_shadow"])
    rect(d, 11, 14, 2, 1, "coat_dark")

    # Torso, slim arms and legs use standard player UV islands.
    for box in ((20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12),
                (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12)):
        rect(d, *box, "coat")
    rect(d, 22, 20, 4, 11, "white")
    rect(d, 23, 20, 1, 11, "gold")
    for y in (22, 25, 28):
        d.point((26, y), fill=P["gold"])
    # Right leg, right arm, left leg and left arm base islands.
    for box in ((4, 16, 8, 4), (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12),
                (12, 20, 8, 12), (36, 48, 4, 4), (32, 52, 4, 12), (36, 52, 4, 12),
                (40, 52, 4, 12), (44, 52, 8, 12), (20, 48, 8, 4), (16, 52, 4, 12),
                (20, 52, 8, 12), (28, 52, 4, 12)):
        rect(d, *box, "coat")
    for box in ((4, 28, 4, 4), (8, 28, 4, 4), (20, 60, 8, 4), (36, 60, 8, 4)):
        rect(d, *box, "coat_dark")
    for box in ((0, 20, 4, 2), (8, 20, 4, 2), (32, 52, 4, 2), (40, 52, 4, 2)):
        rect(d, *box, "skin")

    # Outer-layer UV receives brass edging so jacket/sleeves remain readable.
    for box in ((16, 32, 24, 16), (0, 32, 16, 16), (0, 48, 16, 16),
                (40, 32, 16, 16), (48, 48, 16, 16)):
        x, y, w, h = box
        rect(d, x, y, w, h, "coat")
        d.line((x, y + h - 1, x + w - 1, y + h - 1), fill=P["gold"])

    # Custom model UV swatches at the exact Java texOffs locations.
    rect(d, 192, 0, 16, 16, "gold")
    rect(d, 192, 16, 16, 32, "hair")
    rect(d, 192, 48, 16, 48, "coat")
    d.line((192, 56, 207, 56), fill=P["gold"])
    d.line((192, 72, 207, 72), fill=P["gold"])
    return image


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

    def child(self, pivot=(0.0, 0.0, 0.0), rotation=(0.0, 0.0, 0.0)) -> "Node":
        child = Node(pivot, rotation, parent=self)
        self.children.append(child)
        return child

    def box(self, xyz, size, color: str) -> "Node":
        self.boxes.append(Box(xyz, size, color))
        return self


def build_model() -> Node:
    root = Node()
    head = root.child()
    body = root.child()
    right_arm = root.child((-5.0, 2.5, 0.0), rotation=(0.0, 0.0, 0.05))
    left_arm = root.child((5.0, 2.5, 0.0), rotation=(0.0, 0.0, -0.05))
    right_leg = root.child((-1.9, 12.0, 0.0))
    left_leg = root.child((1.9, 12.0, 0.0))

    head.box((-4, -8, -4), (8, 8, 8), "skin")
    head.box((-4.4, -8.4, -4.4), (8.8, 2.6, 8.8), "hair")
    head.box((-4.35, -7.8, 2.9), (8.7, 7.8, 1.3), "hair_dark")
    head.box((-4.3, -7.6, -3.7), (1.1, 7.6, 6.6), "hair_light")
    head.box((3.2, -7.6, -3.7), (1.1, 7.6, 6.6), "hair")
    # Face pixels and model-only accessory/ponytail match KitchenGoddessModel.
    for x in (-2.0, 1.0):
        head.box((x, -4.4, -4.08), (1.2, 1.4, 0.15), "eye")
        head.box((x + 0.2, -4.2, -4.17), (0.45, 0.55, 0.1), "blue")
    head.box((-0.55, -1.7, -4.1), (1.1, 0.28, 0.18), "coat_dark")
    head.box((-2, -9.5, -1), (4, 2, 2), "gold")
    head.box((-1.5, -10, -0.5), (3, 1, 1), "coat")
    head.box((-1.5, -6, 4), (3, 8, 3), "hair")
    head.box((-1, 2, 4.5), (2, 5, 2), "hair_dark")

    body.box((-4, 0, -2), (8, 12, 4), "coat")
    body.box((-2.2, 0, -2.25), (4.4, 11.7, 0.45), "white")
    body.box((-0.25, 0, -2.5), (0.5, 11.5, 0.35), "gold")
    for y in (2.5, 5.5, 8.5):
        body.box((1.55, y, -2.55), (0.5, 0.5, 0.4), "gold")
    body.box((0.2, 6, -2.5), (3.8, 9, 0.4), "coat")
    body.box((0.5, 15, -2.4), (3.2, 3, 0.3), "coat_dark")
    body.box((-4, 6, -2.5), (3.8, 9, 0.4), "coat")
    body.box((-3.7, 15, -2.4), (3.2, 3, 0.3), "coat_dark")
    for arm in (right_arm, left_arm):
        arm.box((-1.5, -2, -2), (3, 12, 4), "coat")
        arm.box((-1.3, -2.1, -2.2), (2.6, 2.2, 4.4), "skin")
        arm.box((-1.65, 8.4, -2.2), (3.3, 1.2, 4.4), "gold")
    for leg in (right_leg, left_leg):
        leg.box((-2, 0, -2), (4, 12, 4), "coat_dark")
        leg.box((-2.1, 7.5, -2.35), (4.2, 4.5, 4.7), "steel")
    return root


def rotate(point, rotation):
    x, y, z = point
    for axis, angle in enumerate(rotation):
        c, s = math.cos(angle), math.sin(angle)
        if axis == 0:
            y, z = y * c - z * s, y * s + z * c
        elif axis == 1:
            x, z = x * c + z * s, -x * s + z * c
        else:
            x, y = x * c - y * s, x * s + y * c
    return x, y, z


def world_point(node: Node, point):
    while node is not None:
        point = rotate(point, node.rotation)
        point = tuple(point[i] + node.pivot[i] for i in range(3))
        node = node.parent
    return point


FACES = (((0, 1, 2, 3), 1.04), ((5, 4, 7, 6), 0.72),
         ((4, 0, 3, 7), 0.82), ((1, 5, 6, 2), 0.92),
         ((4, 5, 1, 0), 1.1), ((3, 2, 6, 7), 0.67))


def render_view(model: Node, yaw: float, size=(420, 720)) -> Image.Image:
    scale_factor = 3
    width, height = size
    out = Image.new("RGBA", (width * scale_factor, height * scale_factor), (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    polygons = []

    def walk(node):
        for box in node.boxes:
            yield node, box
        for child in node.children:
            yield from walk(child)

    yaw = math.radians(yaw)
    scale = min(width / 20, height / 37) * scale_factor
    for node, box in walk(model):
        x, y, z = box.xyz
        w, h, d = box.size
        local = ((x, y, z), (x+w, y, z), (x+w, y+h, z), (x, y+h, z),
                 (x, y, z+d), (x+w, y, z+d), (x+w, y+h, z+d), (x, y+h, z+d))
        vertices = []
        for point in local:
            px, py, pz = world_point(node, point)
            rx, depth = math.cos(yaw) * px + math.sin(yaw) * pz, -math.sin(yaw) * px + math.cos(yaw) * pz
            vertices.append((width*scale_factor/2 + rx*scale,
                             height*scale_factor*0.31 + (py-3)*scale, depth))
        for indices, light in FACES:
            points = [(vertices[i][0], vertices[i][1]) for i in indices]
            polygons.append((sum(vertices[i][2] for i in indices)/4, points, shade(P[box.color], light)))
    draw.ellipse((width*.2*scale_factor, height*.91*scale_factor,
                  width*.8*scale_factor, height*.965*scale_factor), fill=(44, 52, 60, 40))
    for _, points, color in sorted(polygons, key=lambda item: item[0], reverse=True):
        draw.polygon(points, fill=color, outline=shade(color, .72), width=2)
    return out.resize(size, Image.Resampling.LANCZOS)


def write_outputs(texture: Image.Image, model: Node) -> None:
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (1320, 820), (231, 235, 238, 255))
    d = ImageDraw.Draw(canvas)
    d.text((32, 22), "KITCHEN GODDESS - OFFLINE JAVA MODEL GEOMETRY CHECK", fill=(31, 42, 54, 255))
    d.text((32, 47), "front / profile / back; coat tails, ponytail and head accessory", fill=(82, 96, 108, 255))
    for index, (name, yaw) in enumerate((("front", 0), ("profile", 90), ("back", 180))):
        x = 20 + index * 430
        canvas.alpha_composite(render_view(model, yaw), (x, 75))
        d.text((x + 18, 785), name, fill=(35, 45, 56, 255))
    canvas.save(PREVIEW)
    sheet = Image.new("RGBA", (768, 806), (235, 238, 239, 255))
    sheet.alpha_composite(texture.resize((768, 768), Image.Resampling.NEAREST))
    ImageDraw.Draw(sheet).text((8, 780), "kitchen_goddess.png 256x256 / nearest-neighbor pixel sheet",
                               fill=(35, 43, 52, 255))
    sheet.save(SHEET)


def main() -> None:
    texture = create_texture()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    texture.save(OUT)
    write_outputs(texture, build_model())
    print(OUT)
    print(PREVIEW)
    print(SHEET)


if __name__ == "__main__":
    main()
