#!/usr/bin/env python3
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SKIN_OUT = ROOT / "src/main/resources/assets/piranport/textures/skin/skin_23.png"
ICON_OUT = ROOT / "src/main/resources/assets/piranport/textures/item/skin_core_23.png"
PREVIEW_OUT = ROOT / "build/offline-renders/hood_skin_preview.png"
SHEET_OUT = ROOT / "build/offline-renders/hood_skin_sheet.png"
SKIN_BASE_SIZE = 64
SKIN_SCALE = 2
SKIN_SIZE = SKIN_BASE_SIZE * SKIN_SCALE


PALETTE = {
    "skin": (246, 214, 190, 255),
    "skin_shadow": (219, 172, 147, 255),
    "skin_blush": (238, 162, 166, 255),
    "hair": (226, 187, 82, 255),
    "hair_shadow": (164, 119, 45, 255),
    "hair_light": (255, 235, 142, 255),
    "navy": (31, 58, 88, 255),
    "navy_dark": (19, 35, 55, 255),
    "navy_light": (44, 82, 122, 255),
    "white": (236, 241, 239, 255),
    "white_shadow": (188, 203, 207, 255),
    "gold": (215, 179, 62, 255),
    "red": (160, 46, 50, 255),
    "blue": (49, 107, 160, 255),
    "eye": (62, 154, 226, 255),
    "eye_light": (141, 218, 255, 255),
    "black": (20, 24, 29, 255),
    "transparent": (0, 0, 0, 0),
}


def shade(color: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
    r, g, b, a = color
    return (
        max(0, min(255, int(r * factor))),
        max(0, min(255, int(g * factor))),
        max(0, min(255, int(b * factor))),
        a,
    )


def fill(img: Image.Image, xywh: tuple[int, int, int, int], color: tuple[int, int, int, int]) -> None:
    x, y, w, h = xywh
    ImageDraw.Draw(img).rectangle((x, y, x + w - 1, y + h - 1), fill=color)


def px(img: Image.Image, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    img.putpixel((x, y), color)


def local_px(img: Image.Image, ox: int, oy: int, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    px(img, ox + x, oy + y, color)


def local_rect(
    img: Image.Image,
    ox: int,
    oy: int,
    x: int,
    y: int,
    w: int,
    h: int,
    color: tuple[int, int, int, int],
) -> None:
    fill(img, (ox + x, oy + y, w, h), color)


def draw_head(img: Image.Image) -> None:
    p = PALETTE
    # Base head.
    fill(img, (8, 0, 8, 8), p["hair_light"])
    fill(img, (16, 0, 8, 8), p["hair_shadow"])
    fill(img, (0, 8, 8, 8), p["hair"])
    fill(img, (16, 8, 8, 8), p["hair"])
    fill(img, (24, 8, 8, 8), p["hair"])
    fill(img, (8, 8, 8, 8), p["skin"])

    # Hair framing and bangs.
    for ox in (8,):
        local_rect(img, ox, 8, 0, 0, 8, 2, p["hair"])
        local_rect(img, ox, 8, 0, 2, 2, 6, p["hair"])
        local_rect(img, ox, 8, 6, 2, 2, 6, p["hair"])
        local_px(img, ox, 8, 2, 2, p["hair_light"])
        local_px(img, ox, 8, 3, 2, p["hair_light"])
        local_px(img, ox, 8, 4, 2, p["hair_shadow"])
        local_px(img, ox, 8, 5, 2, p["hair_shadow"])
        local_px(img, ox, 8, 2, 3, p["hair"])
        local_px(img, ox, 8, 5, 3, p["hair"])

        local_px(img, ox, 8, 2, 3, (31, 66, 105, 255))
        local_px(img, ox, 8, 5, 3, (31, 66, 105, 255))
        local_px(img, ox, 8, 2, 4, p["eye"])
        local_px(img, ox, 8, 5, 4, p["eye"])
        local_px(img, ox, 8, 2, 5, p["eye_light"])
        local_px(img, ox, 8, 5, 5, p["eye_light"])
        local_px(img, ox, 8, 1, 5, (*p["skin_blush"][:3], 210))
        local_px(img, ox, 8, 6, 5, (*p["skin_blush"][:3], 210))
        local_px(img, ox, 8, 3, 6, (128, 70, 80, 255))
        local_px(img, ox, 8, 4, 6, (128, 70, 80, 255))

    # Sides and back hair detail.
    for face_x in (0, 16, 24):
        for i in range(8):
            c = p["hair_light"] if i in (1, 5) else p["hair_shadow"] if i in (3, 7) else p["hair"]
            local_px(img, face_x, 8, i, 0, c)
            local_px(img, face_x, 8, i, 7, shade(c, 0.82))
        local_rect(img, face_x, 8, 0, 5, 8, 3, p["hair_shadow"])

    # Head overlay: captain cap and longer hair volume.
    fill(img, (40, 8, 8, 8), p["transparent"])
    fill(img, (32, 8, 8, 8), p["transparent"])
    fill(img, (48, 8, 8, 8), p["transparent"])
    fill(img, (56, 8, 8, 8), p["transparent"])
    fill(img, (40, 0, 8, 8), p["transparent"])
    fill(img, (48, 0, 8, 8), p["transparent"])

    local_rect(img, 40, 8, 1, 0, 6, 1, p["white"])
    local_rect(img, 40, 8, 0, 1, 8, 1, p["white_shadow"])
    local_rect(img, 40, 8, 1, 2, 6, 1, p["navy"])
    local_px(img, 40, 8, 4, 1, p["red"])
    local_px(img, 40, 8, 5, 1, p["red"])
    local_rect(img, 40, 8, 0, 3, 2, 5, shade(p["hair"], 1.05))
    local_rect(img, 40, 8, 6, 2, 2, 6, shade(p["hair"], 0.93))
    local_px(img, 40, 8, 1, 4, p["hair_light"])
    local_px(img, 40, 8, 6, 4, p["hair_shadow"])
    local_px(img, 40, 8, 3, 2, p["gold"])

    fill(img, (40, 0, 8, 4), p["white"])
    local_rect(img, 40, 0, 1, 1, 6, 1, p["white_shadow"])
    local_rect(img, 40, 0, 2, 2, 4, 1, p["navy"])
    fill(img, (48, 0, 8, 4), p["white_shadow"])
    local_rect(img, 48, 0, 2, 1, 4, 1, p["navy"])
    for face_x in (32, 48, 56):
        local_rect(img, face_x, 8, 0, 0, 8, 2, p["hair"])
        local_rect(img, face_x, 8, 0, 2, 2, 6, p["hair_light"])
        local_rect(img, face_x, 8, 6, 2, 2, 6, p["hair_shadow"])


def draw_body(img: Image.Image) -> None:
    p = PALETTE
    # Base torso.
    fill(img, (20, 16, 8, 4), p["navy_light"])
    fill(img, (28, 16, 8, 4), p["navy_dark"])
    fill(img, (16, 20, 4, 12), p["navy_dark"])
    fill(img, (20, 20, 8, 12), p["navy"])
    fill(img, (28, 20, 4, 12), p["navy_light"])
    fill(img, (32, 20, 8, 12), p["navy_dark"])

    # Front uniform details.
    local_rect(img, 20, 20, 0, 0, 8, 2, p["white"])
    local_px(img, 20, 20, 3, 0, p["gold"])
    local_px(img, 20, 20, 4, 0, p["gold"])
    local_rect(img, 20, 20, 3, 1, 2, 4, p["red"])
    local_rect(img, 20, 20, 0, 1, 1, 7, p["white_shadow"])
    local_rect(img, 20, 20, 7, 1, 1, 7, p["white"])
    for y in (3, 5, 7):
        local_px(img, 20, 20, 5, y, p["gold"])
    local_px(img, 20, 20, 2, 5, p["gold"])
    local_px(img, 20, 20, 3, 6, p["gold"])
    local_px(img, 20, 20, 4, 6, p["gold"])
    local_px(img, 20, 20, 5, 5, p["gold"])
    local_rect(img, 20, 20, 0, 10, 8, 2, p["navy_dark"])
    local_px(img, 20, 20, 1, 10, p["blue"])
    local_px(img, 20, 20, 6, 10, p["blue"])

    # Body overlay: white cape with gold edging.
    fill(img, (16, 32, 24, 16), p["transparent"])
    local_rect(img, 20, 36, 0, 0, 8, 2, p["white"])
    local_rect(img, 20, 36, 0, 2, 2, 5, p["white"])
    local_rect(img, 20, 36, 6, 2, 2, 5, p["white_shadow"])
    for y in range(2, 8):
        local_px(img, 20, 36, 2, y, p["gold"])
        local_px(img, 20, 36, 5, y, p["gold"])
    local_rect(img, 20, 36, 0, 1, 1, 8, p["hair_light"])
    local_rect(img, 20, 36, 7, 1, 1, 8, p["hair_shadow"])
    local_px(img, 20, 36, 1, 6, p["hair"])
    local_px(img, 20, 36, 6, 6, p["hair"])
    local_rect(img, 32, 36, 0, 0, 8, 7, p["white_shadow"])
    local_rect(img, 32, 36, 2, 7, 4, 1, p["gold"])
    local_rect(img, 16, 36, 0, 0, 4, 7, p["white_shadow"])
    local_rect(img, 28, 36, 0, 0, 4, 7, p["white"])


def draw_arm_face(img: Image.Image, ox: int, oy: int, hand_rows: int = 3) -> None:
    p = PALETTE
    local_rect(img, ox, oy, 0, 0, 4, 7, p["navy"])
    local_rect(img, ox, oy, 0, 7, 4, 2, p["white"])
    local_rect(img, ox, oy, 0, 9, 4, hand_rows, p["skin"])
    local_px(img, ox, oy, 0, 1, p["gold"])
    local_px(img, ox, oy, 3, 1, p["gold"])
    local_rect(img, ox, oy, 0, 10, 4, 1, p["skin_shadow"])


def draw_arms(img: Image.Image) -> None:
    p = PALETTE
    # Right arm base.
    fill(img, (44, 16, 4, 4), p["navy_light"])
    fill(img, (48, 16, 4, 4), p["navy_dark"])
    for face in ((40, 20), (44, 20), (48, 20), (52, 20)):
        draw_arm_face(img, *face)
    # Left arm base.
    fill(img, (36, 48, 4, 4), p["navy_light"])
    fill(img, (40, 48, 4, 4), p["navy_dark"])
    for face in ((32, 52), (36, 52), (40, 52), (44, 52)):
        draw_arm_face(img, *face)

    # Sleeve overlays from the cape.
    fill(img, (40, 32, 16, 16), p["transparent"])
    fill(img, (48, 48, 16, 16), p["transparent"])
    local_rect(img, 44, 36, 0, 0, 4, 5, p["white_shadow"])
    local_rect(img, 52, 52, 0, 0, 4, 5, p["white"])
    local_rect(img, 44, 36, 0, 5, 4, 1, p["gold"])
    local_rect(img, 52, 52, 0, 5, 4, 1, p["gold"])
    local_rect(img, 40, 36, 0, 0, 4, 4, p["white_shadow"])
    local_rect(img, 48, 52, 0, 0, 4, 4, p["white"])


def draw_leg_face(img: Image.Image, ox: int, oy: int, left: bool) -> None:
    p = PALETTE
    local_rect(img, ox, oy, 0, 0, 4, 2, p["skin"])
    local_rect(img, ox, oy, 0, 2, 4, 1, p["blue"])
    local_rect(img, ox, oy, 0, 3, 4, 7, p["white"])
    local_rect(img, ox, oy, 0, 10, 4, 2, p["navy_dark"])
    local_px(img, ox, oy, 0 if left else 3, 4, p["white_shadow"])
    local_px(img, ox, oy, 0 if left else 3, 5, p["white_shadow"])
    local_px(img, ox, oy, 0 if left else 3, 6, p["white_shadow"])
    local_px(img, ox, oy, 0 if left else 3, 7, p["white_shadow"])


def draw_legs(img: Image.Image) -> None:
    p = PALETTE
    # Right leg base.
    fill(img, (4, 16, 4, 4), p["skin_shadow"])
    fill(img, (8, 16, 4, 4), p["navy_dark"])
    for face in ((0, 20), (4, 20), (8, 20), (12, 20)):
        draw_leg_face(img, *face, left=False)
    # Left leg base.
    fill(img, (20, 48, 4, 4), p["skin_shadow"])
    fill(img, (24, 48, 4, 4), p["navy_dark"])
    for face in ((16, 52), (20, 52), (24, 52), (28, 52)):
        draw_leg_face(img, *face, left=True)

    # Pants overlays as skirt panels.
    fill(img, (0, 32, 16, 16), p["transparent"])
    fill(img, (0, 48, 16, 16), p["transparent"])
    local_rect(img, 4, 36, 0, 0, 4, 3, p["navy_dark"])
    local_rect(img, 4, 52, 0, 0, 4, 3, p["navy_dark"])
    local_px(img, 4, 36, 1, 2, p["blue"])
    local_px(img, 4, 52, 2, 2, p["blue"])
    local_rect(img, 0, 36, 0, 0, 4, 2, p["navy_dark"])
    local_rect(img, 8, 36, 0, 0, 4, 2, p["navy_dark"])
    local_rect(img, 0, 52, 0, 0, 4, 2, p["navy_dark"])
    local_rect(img, 8, 52, 0, 0, 4, 2, p["navy_dark"])


def create_skin_base() -> Image.Image:
    img = Image.new("RGBA", (64, 64), PALETTE["transparent"])
    draw_head(img)
    draw_body(img)
    draw_arms(img)
    draw_legs(img)
    return img


def scaled_rect(
    draw: ImageDraw.ImageDraw,
    base_x: int,
    base_y: int,
    x: int,
    y: int,
    w: int,
    h: int,
    color: tuple[int, int, int, int],
) -> None:
    sx = base_x * SKIN_SCALE + x
    sy = base_y * SKIN_SCALE + y
    draw.rectangle((sx, sy, sx + w - 1, sy + h - 1), fill=color)


def enhance_high_resolution_skin(base: Image.Image) -> Image.Image:
    p = PALETTE
    skin = base.resize((SKIN_SIZE, SKIN_SIZE), Image.Resampling.NEAREST)
    draw = ImageDraw.Draw(skin, "RGBA")

    # Face: larger anime-style eyes and softer low-contrast mouth.
    fx, fy = 8, 8
    scaled_rect(draw, fx, fy, 4, 6, 3, 1, (24, 54, 88, 255))
    scaled_rect(draw, fx, fy, 10, 6, 3, 1, (24, 54, 88, 255))
    scaled_rect(draw, fx, fy, 4, 7, 3, 4, p["eye"])
    scaled_rect(draw, fx, fy, 10, 7, 3, 4, p["eye"])
    scaled_rect(draw, fx, fy, 4, 7, 1, 1, p["eye_light"])
    scaled_rect(draw, fx, fy, 10, 7, 1, 1, p["eye_light"])
    scaled_rect(draw, fx, fy, 5, 10, 2, 1, (110, 205, 248, 255))
    scaled_rect(draw, fx, fy, 11, 10, 2, 1, (110, 205, 248, 255))
    scaled_rect(draw, fx, fy, 2, 11, 2, 1, (*p["skin_blush"][:3], 130))
    scaled_rect(draw, fx, fy, 13, 11, 2, 1, (*p["skin_blush"][:3], 130))
    scaled_rect(draw, fx, fy, 7, 13, 3, 1, (126, 68, 82, 255))

    # Face overlay: crisper bangs and long side locks.
    ofx, ofy = 40, 8
    scaled_rect(draw, ofx, ofy, 0, 5, 3, 10, shade(p["hair"], 1.06))
    scaled_rect(draw, ofx, ofy, 13, 4, 3, 11, shade(p["hair"], 0.92))
    scaled_rect(draw, ofx, ofy, 2, 6, 1, 7, p["hair_light"])
    scaled_rect(draw, ofx, ofy, 13, 6, 1, 7, p["hair_shadow"])
    scaled_rect(draw, ofx, ofy, 4, 0, 8, 2, p["white"])
    scaled_rect(draw, ofx, ofy, 3, 2, 10, 2, p["white_shadow"])
    scaled_rect(draw, ofx, ofy, 4, 4, 9, 2, p["navy"])
    scaled_rect(draw, ofx, ofy, 8, 2, 3, 2, p["red"])
    scaled_rect(draw, ofx, ofy, 6, 5, 1, 1, p["gold"])

    # Torso: use the added pixels for finer buttons and chain detail.
    bx, by = 20, 20
    scaled_rect(draw, bx, by, 6, 6, 1, 1, p["gold"])
    scaled_rect(draw, bx, by, 9, 6, 1, 1, p["gold"])
    scaled_rect(draw, bx, by, 7, 7, 2, 1, p["gold"])
    scaled_rect(draw, bx, by, 8, 2, 3, 6, p["red"])
    scaled_rect(draw, bx, by, 5, 3, 1, 9, p["gold"])
    scaled_rect(draw, bx, by, 12, 3, 1, 9, p["gold"])

    # Cape/hair overlay on body: thinner golden edging at high resolution.
    cbx, cby = 20, 36
    scaled_rect(draw, cbx, cby, 4, 4, 1, 12, p["gold"])
    scaled_rect(draw, cbx, cby, 11, 4, 1, 12, p["gold"])
    scaled_rect(draw, cbx, cby, 1, 2, 2, 14, p["hair_light"])
    scaled_rect(draw, cbx, cby, 13, 2, 2, 14, p["hair_shadow"])
    return skin


def create_skin() -> Image.Image:
    return enhance_high_resolution_skin(create_skin_base())


PARTS = [
    (
        "head",
        (-4.0, 24.0, -4.0, 4.0, 32.0, 4.0),
        {
            "top": (8, 0, 8, 8),
            "bottom": (16, 0, 8, 8),
            "right": (0, 8, 8, 8),
            "front": (8, 8, 8, 8),
            "left": (16, 8, 8, 8),
            "back": (24, 8, 8, 8),
        },
        {
            "top": (40, 0, 8, 8),
            "bottom": (48, 0, 8, 8),
            "right": (32, 8, 8, 8),
            "front": (40, 8, 8, 8),
            "left": (48, 8, 8, 8),
            "back": (56, 8, 8, 8),
        },
    ),
    (
        "body",
        (-4.0, 12.0, -2.0, 4.0, 24.0, 2.0),
        {
            "top": (20, 16, 8, 4),
            "bottom": (28, 16, 8, 4),
            "right": (16, 20, 4, 12),
            "front": (20, 20, 8, 12),
            "left": (28, 20, 4, 12),
            "back": (32, 20, 8, 12),
        },
        {
            "top": (20, 32, 8, 4),
            "bottom": (28, 32, 8, 4),
            "right": (16, 36, 4, 12),
            "front": (20, 36, 8, 12),
            "left": (28, 36, 4, 12),
            "back": (32, 36, 8, 12),
        },
    ),
    (
        "right_arm",
        (-8.0, 12.0, -2.0, -4.0, 24.0, 2.0),
        {
            "top": (44, 16, 4, 4),
            "bottom": (48, 16, 4, 4),
            "right": (40, 20, 4, 12),
            "front": (44, 20, 4, 12),
            "left": (48, 20, 4, 12),
            "back": (52, 20, 4, 12),
        },
        {
            "top": (44, 32, 4, 4),
            "bottom": (48, 32, 4, 4),
            "right": (40, 36, 4, 12),
            "front": (44, 36, 4, 12),
            "left": (48, 36, 4, 12),
            "back": (52, 36, 4, 12),
        },
    ),
    (
        "left_arm",
        (4.0, 12.0, -2.0, 8.0, 24.0, 2.0),
        {
            "top": (36, 48, 4, 4),
            "bottom": (40, 48, 4, 4),
            "right": (32, 52, 4, 12),
            "front": (36, 52, 4, 12),
            "left": (40, 52, 4, 12),
            "back": (44, 52, 4, 12),
        },
        {
            "top": (52, 48, 4, 4),
            "bottom": (56, 48, 4, 4),
            "right": (48, 52, 4, 12),
            "front": (52, 52, 4, 12),
            "left": (56, 52, 4, 12),
            "back": (60, 52, 4, 12),
        },
    ),
    (
        "right_leg",
        (-4.0, 0.0, -2.0, 0.0, 12.0, 2.0),
        {
            "top": (4, 16, 4, 4),
            "bottom": (8, 16, 4, 4),
            "right": (0, 20, 4, 12),
            "front": (4, 20, 4, 12),
            "left": (8, 20, 4, 12),
            "back": (12, 20, 4, 12),
        },
        {
            "top": (4, 32, 4, 4),
            "bottom": (8, 32, 4, 4),
            "right": (0, 36, 4, 12),
            "front": (4, 36, 4, 12),
            "left": (8, 36, 4, 12),
            "back": (12, 36, 4, 12),
        },
    ),
    (
        "left_leg",
        (0.0, 0.0, -2.0, 4.0, 12.0, 2.0),
        {
            "top": (20, 48, 4, 4),
            "bottom": (24, 48, 4, 4),
            "right": (16, 52, 4, 12),
            "front": (20, 52, 4, 12),
            "left": (24, 52, 4, 12),
            "back": (28, 52, 4, 12),
        },
        {
            "top": (4, 48, 4, 4),
            "bottom": (8, 48, 4, 4),
            "right": (0, 52, 4, 12),
            "front": (4, 52, 4, 12),
            "left": (8, 52, 4, 12),
            "back": (12, 52, 4, 12),
        },
    ),
]


def face_point(
    box: tuple[float, float, float, float, float, float],
    face: str,
    u: float,
    v: float,
    tw: int,
    th: int,
) -> tuple[float, float, float]:
    x0, y0, z0, x1, y1, z1 = box
    fu = u / tw
    fv = v / th
    if face == "front":
        return (x0 + (x1 - x0) * fu, y1 - (y1 - y0) * fv, z0)
    if face == "back":
        return (x1 - (x1 - x0) * fu, y1 - (y1 - y0) * fv, z1)
    if face == "right":
        return (x0, y1 - (y1 - y0) * fv, z0 + (z1 - z0) * fu)
    if face == "left":
        return (x1, y1 - (y1 - y0) * fv, z1 - (z1 - z0) * fu)
    if face == "top":
        return (x0 + (x1 - x0) * fu, y1, z1 - (z1 - z0) * fv)
    if face == "bottom":
        return (x0 + (x1 - x0) * fu, y0, z0 + (z1 - z0) * fv)
    raise ValueError(face)


def inflate_box(
    box: tuple[float, float, float, float, float, float], amount: float
) -> tuple[float, float, float, float, float, float]:
    x0, y0, z0, x1, y1, z1 = box
    return (x0 - amount, y0 - amount, z0 - amount, x1 + amount, y1 + amount, z1 + amount)


def project(
    p: tuple[float, float, float], yaw_deg: float, pitch_deg: float, scale: float, cx: float, cy: float
) -> tuple[float, float, float]:
    x, y, z = p
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    xr = math.cos(yaw) * x + math.sin(yaw) * z
    zr = -math.sin(yaw) * x + math.cos(yaw) * z
    yr = math.cos(pitch) * y - math.sin(pitch) * zr
    zd = math.sin(pitch) * y + math.cos(pitch) * zr
    return (cx + xr * scale, cy - yr * scale, zd)


FACE_LIGHT = {
    "front": 1.04,
    "right": 0.84,
    "left": 0.91,
    "back": 0.72,
    "top": 1.10,
    "bottom": 0.62,
}


def render_model(skin: Image.Image, yaw: float, pitch: float, size: tuple[int, int]) -> Image.Image:
    w, h = size
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    polys: list[tuple[float, list[tuple[float, float]], tuple[int, int, int, int]]] = []
    texture_scale = max(1, skin.width // SKIN_BASE_SIZE)
    scale = min(w / 22, h / 38)
    cx = w / 2
    cy = h * 0.92

    for _, box, base_map, overlay_map in PARTS:
        for mapping, inflate in ((base_map, 0.0), (overlay_map, 0.33)):
            used_box = inflate_box(box, inflate) if inflate else box
            for face, (tx, ty, tw, th) in mapping.items():
                sample_w = tw * texture_scale
                sample_h = th * texture_scale
                for yy in range(sample_h):
                    for xx in range(sample_w):
                        color = skin.getpixel((tx * texture_scale + xx, ty * texture_scale + yy))
                        if color[3] == 0:
                            continue
                        x0 = xx / texture_scale
                        y0 = yy / texture_scale
                        x1 = (xx + 1) / texture_scale
                        y1 = (yy + 1) / texture_scale
                        pts3 = [
                            face_point(used_box, face, x0, y0, tw, th),
                            face_point(used_box, face, x1, y0, tw, th),
                            face_point(used_box, face, x1, y1, tw, th),
                            face_point(used_box, face, x0, y1, tw, th),
                        ]
                        pts2 = [project(pt, yaw, pitch, scale, cx, cy) for pt in pts3]
                        avg_depth = sum(pt[2] for pt in pts2) / 4
                        fill_color = shade(color, FACE_LIGHT[face])
                        polys.append((avg_depth, [(pt[0], pt[1]) for pt in pts2], fill_color))

    draw = ImageDraw.Draw(out, "RGBA")
    for _, poly, color in sorted(polys, key=lambda item: item[0], reverse=True):
        draw.polygon(poly, fill=color)
    return out


def create_icon(skin: Image.Image) -> Image.Image:
    texture_scale = max(1, skin.width // SKIN_BASE_SIZE)
    head_size = 8 * texture_scale
    head = Image.new("RGBA", (head_size, head_size), (0, 0, 0, 0))
    head.alpha_composite(skin.crop((8 * texture_scale, 8 * texture_scale,
                                    16 * texture_scale, 16 * texture_scale)))
    head.alpha_composite(skin.crop((40 * texture_scale, 8 * texture_scale,
                                    48 * texture_scale, 16 * texture_scale)))
    icon = head.resize((16, 16), Image.Resampling.NEAREST)
    draw = ImageDraw.Draw(icon)
    draw.rectangle((0, 0, 15, 15), outline=(44, 82, 122, 255))
    px(icon, 1, 1, PALETTE["gold"])
    px(icon, 14, 1, PALETTE["red"])
    return icon


def label(draw: ImageDraw.ImageDraw, text: str, x: int, y: int) -> None:
    draw.rectangle((x - 4, y - 2, x + len(text) * 6 + 6, y + 12), fill=(245, 248, 248, 230))
    draw.text((x, y), text, fill=(32, 45, 58, 255))


def write_sheet(skin: Image.Image) -> None:
    display_scale = 4 if skin.width > SKIN_BASE_SIZE else 8
    sheet = Image.new("RGBA", (skin.width * display_scale, skin.height * display_scale + 24),
                      (242, 242, 242, 255))
    sheet.alpha_composite(skin.resize((skin.width * display_scale, skin.height * display_scale),
                                      Image.Resampling.NEAREST), (0, 0))
    draw = ImageDraw.Draw(sheet)
    draw.text((4, skin.height * display_scale + 6),
              f"skin_23.png {skin.width}x{skin.height}", fill=(30, 30, 30, 255))
    SHEET_OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(SHEET_OUT)


def write_preview(skin: Image.Image, icon: Image.Image) -> None:
    PREVIEW_OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (1000, 720), (226, 231, 234, 255))
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((0, 0, 1000, 720), fill=(226, 231, 234, 255))

    views = [
        ("front three-quarter", -28, 8, (54, 64)),
        ("front", 0, 4, (360, 64)),
        ("back three-quarter", 150, 8, (666, 64)),
    ]
    for title, yaw, pitch, pos in views:
        rendered = render_model(skin, yaw, pitch, (280, 560))
        canvas.alpha_composite(rendered, pos)
        label(draw, title, pos[0] + 18, pos[1] + 538)

    draw.rectangle((32, 618, 164, 692), fill=(245, 248, 248, 255), outline=(164, 177, 188, 255))
    draw.text((48, 628), "skin core icon", fill=(40, 50, 60, 255))
    canvas.alpha_composite(icon.resize((64, 64), Image.Resampling.NEAREST), (80, 622))
    draw.text((188, 630), "Hood: blonde hair, white captain cap, navy uniform, red tie, white cape, white thigh-highs", fill=(38, 48, 58, 255))
    draw.text((188, 652),
              f"Generated as piranport skin_23 ({skin.width}x{skin.height}), rendered offline with the local software preview tool.",
              fill=(75, 88, 98, 255))
    canvas.save(PREVIEW_OUT)


def main() -> None:
    skin = create_skin()
    icon = create_icon(skin)
    SKIN_OUT.parent.mkdir(parents=True, exist_ok=True)
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    skin.save(SKIN_OUT)
    icon.save(ICON_OUT)
    write_sheet(skin)
    write_preview(skin, icon)
    print(SKIN_OUT)
    print(ICON_OUT)
    print(PREVIEW_OUT)
    print(SHEET_OUT)


if __name__ == "__main__":
    main()
