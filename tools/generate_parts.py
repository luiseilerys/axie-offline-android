#!/usr/bin/env python3
"""Generate composite creature body-part PNGs (256x256, transparent) for Axie Offline."""

import math
import os
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("Pillow required: pip install Pillow")
    raise

SIZE = 256
CX, CY = 128, 128
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "drawable"

CLASSES = {
    "beast":   {"p": (245, 158, 11),  "s": (217, 119, 6),  "a": (254, 243, 199), "sh": (120, 53, 15)},
    "plant":   {"p": (34, 197, 94),   "s": (22, 163, 74),  "a": (187, 247, 208), "sh": (20, 83, 45)},
    "reptile": {"p": (16, 185, 129),  "s": (5, 150, 105),  "a": (167, 243, 208), "sh": (6, 78, 59)},
    "aquatic": {"p": (59, 130, 246),  "s": (37, 99, 235),  "a": (191, 219, 254), "sh": (30, 58, 138)},
    "bird":    {"p": (139, 92, 246),  "s": (124, 58, 237), "a": (221, 214, 254), "sh": (76, 29, 149)},
    "bug":     {"p": (163, 230, 53),  "s": (132, 204, 22), "a": (236, 252, 203), "sh": (63, 98, 18)},
    "mech":    {"p": (148, 163, 184), "s": (100, 116, 139),"a": (226, 232, 240), "sh": (51, 65, 85)},
    "dawn":    {"p": (251, 191, 36),  "s": (245, 158, 11), "a": (254, 249, 195), "sh": (146, 64, 14)},
    "dusk":    {"p": (99, 102, 241),  "s": (79, 70, 229),  "a": (199, 210, 254), "sh": (49, 46, 129)},
}

OUTLINE = (20, 20, 30, 255)
STROKE = 5
VARIANTS = 12


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def draw_ellipse(d, box, fill, outline=OUTLINE, width=STROKE):
    d.ellipse(box, fill=fill, outline=outline, width=width)


def draw_poly(d, pts, fill, outline=OUTLINE, width=STROKE):
    d.polygon(pts, fill=fill, outline=outline)
    # thicker outline via lines
    for i in range(len(pts)):
        d.line([pts[i], pts[(i + 1) % len(pts)]], fill=outline, width=width)


def bezier_points(p0, p1, p2, p3, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        x = u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0]
        y = u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def draw_bezier(d, p0, p1, p2, p3, fill=None, outline=OUTLINE, width=STROKE):
    pts = bezier_points(p0, p1, p2, p3)
    if fill and len(pts) > 2:
        d.polygon(pts, fill=fill)
    d.line(pts, fill=outline, width=width)


def save(img, name):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / f"{name}.png"
    img.save(path, "PNG")
    return path


# ---- BODY ----
def gen_body(cls, colors):
    img = new_img()
    d = ImageDraw.Draw(img)
    # radial-ish body: main ellipse + lighter top
    draw_ellipse(d, [CX - 55, CY - 45, CX + 55, CY + 55], colors["p"])
    draw_ellipse(d, [CX - 35, CY - 40, CX + 35, CY + 10], colors["a"])
    # belly
    draw_ellipse(d, [CX - 30, CY + 5, CX + 30, CY + 45], colors["s"])
    return save(img, f"body_{cls}")


# ---- TAIL variants ----
def gen_tail(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    # base attach near bottom-right of body area
    base = (CX + 40, CY + 30)
    if v == 0:  # simple curve
        draw_bezier(d, base, (CX + 80, CY + 20), (CX + 100, CY + 60), (CX + 90, CY + 90),
                    fill=colors["p"], width=8)
        draw_ellipse(d, [CX + 82, CY + 82, CX + 100, CY + 100], colors["a"])
    elif v == 1:  # forked
        draw_bezier(d, base, (CX + 70, CY + 40), (CX + 90, CY + 50), (CX + 110, CY + 40), width=7)
        draw_bezier(d, base, (CX + 70, CY + 50), (CX + 90, CY + 70), (CX + 105, CY + 90), width=7)
        draw_ellipse(d, [CX + 100, CY + 30, CX + 118, CY + 48], colors["p"])
        draw_ellipse(d, [CX + 95, CY + 82, CX + 113, CY + 100], colors["p"])
    elif v == 2:  # arrow tip
        draw_bezier(d, base, (CX + 75, CY + 35), (CX + 95, CY + 55), (CX + 100, CY + 75), width=8)
        draw_poly(d, [(CX + 100, CY + 75), (CX + 120, CY + 70), (CX + 100, CY + 95)], colors["s"])
    elif v == 3:  # balls
        draw_bezier(d, base, (CX + 70, CY + 45), (CX + 85, CY + 70), (CX + 90, CY + 85), width=6)
        for i, (bx, by) in enumerate([(90, 50), (100, 70), (95, 90)]):
            r = 10 - i
            draw_ellipse(d, [CX + bx - r, CY + by - r, CX + bx + r, CY + by + r], colors["p"])
    elif v == 4:  # spiral
        pts = []
        for i in range(30):
            ang = i * 0.4
            rad = 15 + i * 1.5
            pts.append((CX + 50 + math.cos(ang) * rad, CY + 40 + math.sin(ang) * rad))
        d.line(pts, fill=OUTLINE, width=7)
        d.line(pts, fill=colors["p"], width=4)
    elif v == 5:  # fin
        draw_poly(d, [base, (CX + 100, CY + 20), (CX + 110, CY + 50), (CX + 70, CY + 55)], colors["p"])
    elif v == 6:  # fluffy
        for i in range(5):
            ox = 50 + i * 12
            oy = 25 + (i % 2) * 15
            draw_ellipse(d, [CX + ox - 12, CY + oy - 12, CX + ox + 12, CY + oy + 12], colors["a"])
    elif v == 7:  # spike chain
        for i in range(4):
            x = CX + 45 + i * 18
            y = CY + 25 + i * 12
            draw_poly(d, [(x, y - 12), (x + 10, y), (x, y + 12), (x - 8, y)], colors["s"])
    elif v == 8:  # ribbon
        draw_bezier(d, base, (CX + 80, CY + 10), (CX + 60, CY + 60), (CX + 100, CY + 80),
                    fill=colors["a"], width=6)
    elif v == 9:  # club
        draw_bezier(d, base, (CX + 70, CY + 40), (CX + 85, CY + 55), (CX + 95, CY + 70), width=8)
        draw_ellipse(d, [CX + 85, CY + 65, CX + 115, CY + 95], colors["p"])
    elif v == 10:  # double curve
        draw_bezier(d, base, (CX + 75, CY + 15), (CX + 95, CY + 40), (CX + 85, CY + 60), width=6)
        draw_bezier(d, base, (CX + 65, CY + 50), (CX + 90, CY + 70), (CX + 100, CY + 90), width=6)
    else:  # leaf tip
        draw_bezier(d, base, (CX + 80, CY + 30), (CX + 100, CY + 50), (CX + 95, CY + 75), width=7)
        draw_poly(d, [(CX + 95, CY + 75), (CX + 115, CY + 60), (CX + 110, CY + 90)], colors["a"])
    return save(img, f"tail_{cls}_{v}")


# ---- BACK variants ----
def gen_back(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    if v in (0, 1):  # wings
        spread = 30 + v * 20
        draw_poly(d, [(CX - 20, CY - 20), (CX - 60 - spread // 2, CY - 50), (CX - 40, CY + 10)], colors["p"])
        draw_poly(d, [(CX + 20, CY - 20), (CX + 60 + spread // 2, CY - 50), (CX + 40, CY + 10)], colors["p"])
        draw_poly(d, [(CX - 20, CY - 15), (CX - 50, CY - 35), (CX - 30, CY + 5)], colors["a"])
        draw_poly(d, [(CX + 20, CY - 15), (CX + 50, CY - 35), (CX + 30, CY + 5)], colors["a"])
    elif v == 2:  # fins
        draw_poly(d, [(CX - 30, CY - 10), (CX - 70, CY - 30), (CX - 50, CY + 20)], colors["s"])
        draw_poly(d, [(CX + 30, CY - 10), (CX + 70, CY - 30), (CX + 50, CY + 20)], colors["s"])
    elif v == 3:  # shell
        draw_ellipse(d, [CX - 50, CY - 55, CX + 50, CY + 5], colors["p"])
        draw_ellipse(d, [CX - 35, CY - 50, CX + 35, CY - 10], colors["a"])
    elif v == 4:  # spikes
        for i in range(-2, 3):
            x = CX + i * 18
            draw_poly(d, [(x, CY - 55), (x + 10, CY - 20), (x - 10, CY - 20)], colors["s"])
    elif v == 5:  # backpack
        draw_ellipse(d, [CX - 35, CY - 50, CX + 35, CY - 5], colors["p"])
        d.rounded_rectangle([CX - 20, CY - 45, CX + 20, CY - 15], radius=6, fill=colors["a"], outline=OUTLINE, width=3)
    elif v == 6:  # ring
        d.ellipse([CX - 45, CY - 50, CX + 45, CY + 5], outline=OUTLINE, width=8)
        d.ellipse([CX - 40, CY - 45, CX + 40, CY], outline=colors["p"], width=5)
    elif v == 7:  # cape
        draw_poly(d, [(CX - 25, CY - 25), (CX - 70, CY + 40), (CX - 10, CY + 30), (CX + 10, CY + 30), (CX + 70, CY + 40), (CX + 25, CY - 25)], colors["s"])
    elif v == 8:  # plates
        for i in range(3):
            y = CY - 45 + i * 12
            draw_ellipse(d, [CX - 40 + i * 3, y, CX + 40 - i * 3, y + 18], colors["p"])
    elif v == 9:  # crystals
        for i, ox in enumerate([-25, 0, 25]):
            draw_poly(d, [(CX + ox, CY - 55), (CX + ox + 12, CY - 25), (CX + ox - 12, CY - 25)], colors["a"])
    elif v == 10:  # sails
        draw_poly(d, [(CX, CY - 20), (CX - 50, CY - 60), (CX - 20, CY - 15)], colors["p"])
        draw_poly(d, [(CX, CY - 20), (CX + 50, CY - 60), (CX + 20, CY - 15)], colors["p"])
    else:  # fluff
        for ox, oy in [(-40, -30), (-20, -45), (0, -50), (20, -45), (40, -30)]:
            draw_ellipse(d, [CX + ox - 14, CY + oy - 14, CX + ox + 14, CY + oy + 14], colors["a"])
    return save(img, f"back_{cls}_{v}")


# ---- MOUTH ----
def gen_mouth(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    my = CY + 25
    if v == 0:  # smile
        d.arc([CX - 25, my - 15, CX + 25, my + 20], 20, 160, fill=OUTLINE, width=4)
    elif v == 1:  # fangs
        d.arc([CX - 22, my - 10, CX + 22, my + 18], 10, 170, fill=OUTLINE, width=4)
        draw_poly(d, [(CX - 12, my), (CX - 8, my + 18), (CX - 4, my)], colors["a"])
        draw_poly(d, [(CX + 4, my), (CX + 8, my + 18), (CX + 12, my)], colors["a"])
    elif v == 2:  # beak
        draw_poly(d, [(CX - 18, my - 5), (CX + 18, my - 5), (CX, my + 25)], colors["s"])
    elif v == 3:  # tongue out
        d.arc([CX - 20, my - 12, CX + 20, my + 15], 0, 180, fill=OUTLINE, width=4)
        draw_ellipse(d, [CX - 8, my + 5, CX + 8, my + 28], (239, 68, 68, 255))
    elif v == 4:  # open
        draw_ellipse(d, [CX - 20, my - 5, CX + 20, my + 25], (30, 30, 40, 255))
        draw_ellipse(d, [CX - 12, my + 5, CX + 12, my + 18], (239, 68, 68, 200))
    elif v == 5:  # grin teeth
        d.arc([CX - 24, my - 12, CX + 24, my + 18], 15, 165, fill=OUTLINE, width=4)
        for i in range(-3, 4):
            x = CX + i * 7
            d.rectangle([x - 2, my, x + 2, my + 8], fill=colors["a"], outline=OUTLINE)
    elif v == 6:  # cat
        d.arc([CX - 18, my - 8, CX + 18, my + 12], 30, 150, fill=OUTLINE, width=3)
        d.line([(CX, my - 5), (CX, my + 8)], fill=OUTLINE, width=2)
    elif v == 7:  # snout
        draw_ellipse(d, [CX - 22, my - 8, CX + 22, my + 20], colors["p"])
        draw_ellipse(d, [CX - 8, my + 2, CX - 2, my + 10], OUTLINE)
        draw_ellipse(d, [CX + 2, my + 2, CX + 8, my + 10], OUTLINE)
    elif v == 8:  # underbite
        draw_ellipse(d, [CX - 20, my, CX + 20, my + 22], colors["s"])
        d.arc([CX - 18, my - 10, CX + 18, my + 10], 200, 340, fill=OUTLINE, width=3)
    elif v == 9:  # sucker
        draw_ellipse(d, [CX - 16, my - 5, CX + 16, my + 22], colors["p"])
        draw_ellipse(d, [CX - 8, my + 2, CX + 8, my + 16], colors["a"])
    elif v == 10:  # neutral
        d.line([(CX - 15, my + 5), (CX + 15, my + 5)], fill=OUTLINE, width=4)
    else:  # smirk
        d.arc([CX - 5, my - 10, CX + 28, my + 18], 40, 140, fill=OUTLINE, width=4)
    return save(img, f"mouth_{cls}_{v}")


# ---- HORN ----
def gen_horn(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    hy = CY - 50
    if v == 0:  # triangle
        draw_poly(d, [(CX, hy - 35), (CX + 18, hy + 10), (CX - 18, hy + 10)], colors["p"])
    elif v == 1:  # diamond
        draw_poly(d, [(CX, hy - 30), (CX + 16, hy - 5), (CX, hy + 15), (CX - 16, hy - 5)], colors["s"])
    elif v == 2:  # leaf
        draw_ellipse(d, [CX - 15, hy - 35, CX + 15, hy + 10], colors["a"])
        d.line([(CX, hy - 30), (CX, hy + 5)], fill=OUTLINE, width=2)
    elif v == 3:  # spiral horn
        pts = []
        for i in range(20):
            ang = -math.pi / 2 + i * 0.35
            rad = 8 + i * 1.2
            pts.append((CX + math.cos(ang) * rad, hy + 5 + math.sin(ang) * rad - i * 1.5))
        d.line(pts, fill=OUTLINE, width=6)
        d.line(pts, fill=colors["p"], width=3)
    elif v == 4:  # double
        draw_poly(d, [(CX - 20, hy + 10), (CX - 28, hy - 30), (CX - 8, hy + 5)], colors["p"])
        draw_poly(d, [(CX + 20, hy + 10), (CX + 28, hy - 30), (CX + 8, hy + 5)], colors["p"])
    elif v == 5:  # antler
        d.line([(CX, hy + 10), (CX, hy - 30)], fill=OUTLINE, width=5)
        d.line([(CX, hy - 15), (CX - 20, hy - 35)], fill=OUTLINE, width=4)
        d.line([(CX, hy - 20), (CX + 20, hy - 35)], fill=OUTLINE, width=4)
        d.line([(CX, hy + 10), (CX, hy - 30)], fill=colors["s"], width=2)
    elif v == 6:  # orb
        d.line([(CX, hy + 10), (CX, hy - 15)], fill=OUTLINE, width=5)
        draw_ellipse(d, [CX - 14, hy - 35, CX + 14, hy - 7], colors["a"])
    elif v == 7:  # blade
        draw_poly(d, [(CX - 6, hy + 10), (CX - 6, hy - 25), (CX, hy - 40), (CX + 6, hy - 25), (CX + 6, hy + 10)], colors["p"])
    elif v == 8:  # crown
        for i in range(-2, 3):
            x = CX + i * 14
            h = 20 + (2 - abs(i)) * 10
            draw_poly(d, [(x - 6, hy + 5), (x, hy + 5 - h), (x + 6, hy + 5)], colors["s"])
    elif v == 9:  # horns side
        draw_poly(d, [(CX - 25, hy + 5), (CX - 45, hy - 25), (CX - 15, hy - 5)], colors["p"])
        draw_poly(d, [(CX + 25, hy + 5), (CX + 45, hy - 25), (CX + 15, hy - 5)], colors["p"])
    elif v == 10:  # stubby
        draw_ellipse(d, [CX - 12, hy - 15, CX + 12, hy + 10], colors["p"])
    else:  # crystal cluster
        for ox, h in [(-12, 25), (0, 35), (12, 25)]:
            draw_poly(d, [(CX + ox, hy + 5 - h), (CX + ox + 8, hy + 5), (CX + ox - 8, hy + 5)], colors["a"])
    return save(img, f"horn_{cls}_{v}")


# ---- EARS ----
def gen_ears(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    if v == 0:  # round
        draw_ellipse(d, [CX - 70, CY - 40, CX - 35, CY - 5], colors["p"])
        draw_ellipse(d, [CX + 35, CY - 40, CX + 70, CY - 5], colors["p"])
        draw_ellipse(d, [CX - 62, CY - 35, CX - 42, CY - 12], colors["a"])
        draw_ellipse(d, [CX + 42, CY - 35, CX + 62, CY - 12], colors["a"])
    elif v == 1:  # pointed
        draw_poly(d, [(CX - 40, CY - 10), (CX - 65, CY - 55), (CX - 25, CY - 25)], colors["p"])
        draw_poly(d, [(CX + 40, CY - 10), (CX + 65, CY - 55), (CX + 25, CY - 25)], colors["p"])
    elif v == 2:  # fins
        draw_poly(d, [(CX - 35, CY - 15), (CX - 75, CY - 35), (CX - 50, CY + 5)], colors["s"])
        draw_poly(d, [(CX + 35, CY - 15), (CX + 75, CY - 35), (CX + 50, CY + 5)], colors["s"])
    elif v == 3:  # antennae
        d.line([(CX - 30, CY - 20), (CX - 50, CY - 55)], fill=OUTLINE, width=4)
        d.line([(CX + 30, CY - 20), (CX + 50, CY - 55)], fill=OUTLINE, width=4)
        draw_ellipse(d, [CX - 58, CY - 65, CX - 42, CY - 49], colors["a"])
        draw_ellipse(d, [CX + 42, CY - 65, CX + 58, CY - 49], colors["a"])
    elif v == 4:  # feathers
        for side in (-1, 1):
            for i in range(3):
                x = CX + side * (40 + i * 5)
                y = CY - 30 - i * 8
                draw_ellipse(d, [x - 8, y - 18, x + 8, y + 5], colors["p"])
    elif v == 5:  # floppy
        draw_ellipse(d, [CX - 75, CY - 25, CX - 30, CY + 25], colors["p"])
        draw_ellipse(d, [CX + 30, CY - 25, CX + 75, CY + 25], colors["p"])
    elif v == 6:  # tufts
        draw_poly(d, [(CX - 45, CY - 15), (CX - 55, CY - 45), (CX - 30, CY - 25)], colors["a"])
        draw_poly(d, [(CX + 45, CY - 15), (CX + 55, CY - 45), (CX + 30, CY - 25)], colors["a"])
    elif v == 7:  # tech panels
        d.rounded_rectangle([CX - 65, CY - 40, CX - 35, CY - 5], radius=4, fill=colors["s"], outline=OUTLINE, width=3)
        d.rounded_rectangle([CX + 35, CY - 40, CX + 65, CY - 5], radius=4, fill=colors["s"], outline=OUTLINE, width=3)
    elif v == 8:  # long rabbit
        draw_ellipse(d, [CX - 55, CY - 70, CX - 30, CY - 5], colors["p"])
        draw_ellipse(d, [CX + 30, CY - 70, CX + 55, CY - 5], colors["p"])
        draw_ellipse(d, [CX - 50, CY - 60, CX - 35, CY - 15], colors["a"])
        draw_ellipse(d, [CX + 35, CY - 60, CX + 50, CY - 15], colors["a"])
    elif v == 9:  # small nubs
        draw_ellipse(d, [CX - 50, CY - 30, CX - 30, CY - 10], colors["p"])
        draw_ellipse(d, [CX + 30, CY - 30, CX + 50, CY - 10], colors["p"])
    elif v == 10:  # bat
        draw_poly(d, [(CX - 30, CY - 15), (CX - 70, CY - 40), (CX - 55, CY - 5), (CX - 70, CY + 10), (CX - 25, CY - 5)], colors["s"])
        draw_poly(d, [(CX + 30, CY - 15), (CX + 70, CY - 40), (CX + 55, CY - 5), (CX + 70, CY + 10), (CX + 25, CY - 5)], colors["s"])
    else:  # side fins up
        draw_poly(d, [(CX - 35, CY - 10), (CX - 70, CY - 50), (CX - 45, CY + 5)], colors["p"])
        draw_poly(d, [(CX + 35, CY - 10), (CX + 70, CY - 50), (CX + 45, CY + 5)], colors["p"])
    return save(img, f"ears_{cls}_{v}")


# ---- EYES ----
def gen_eyes(cls, colors, v):
    img = new_img()
    d = ImageDraw.Draw(img)
    ly, ry = CX - 28, CX + 28
    ey = CY - 10
    if v == 0:  # round
        for x in (ly, ry):
            draw_ellipse(d, [x - 16, ey - 16, x + 16, ey + 16], (255, 255, 255, 255))
            draw_ellipse(d, [x - 8, ey - 8, x + 8, ey + 8], OUTLINE)
            draw_ellipse(d, [x - 3, ey - 5, x + 2, ey], (255, 255, 255, 255))
    elif v == 1:  # slanted
        for x, flip in ((ly, 1), (ry, -1)):
            draw_poly(d, [(x - 14, ey), (x + 14, ey - 10 * flip), (x + 14, ey + 8), (x - 14, ey + 10)], (255, 255, 255, 255))
            draw_ellipse(d, [x - 6, ey - 4, x + 6, ey + 6], OUTLINE)
    elif v == 2:  # shiny big
        for x in (ly, ry):
            draw_ellipse(d, [x - 18, ey - 18, x + 18, ey + 18], colors["a"])
            draw_ellipse(d, [x - 10, ey - 10, x + 10, ey + 10], OUTLINE)
            draw_ellipse(d, [x - 5, ey - 8, x + 1, ey - 2], (255, 255, 255, 255))
            draw_ellipse(d, [x + 4, ey + 2, x + 8, ey + 6], (255, 255, 255, 180))
    elif v == 3:  # brow
        for x in (ly, ry):
            draw_ellipse(d, [x - 14, ey - 12, x + 14, ey + 14], (255, 255, 255, 255))
            draw_ellipse(d, [x - 7, ey - 5, x + 7, ey + 7], OUTLINE)
            d.arc([x - 16, ey - 22, x + 16, ey - 5], 200, 340, fill=OUTLINE, width=4)
    elif v == 4:  # closed happy
        for x in (ly, ry):
            d.arc([x - 14, ey - 10, x + 14, ey + 10], 200, 340, fill=OUTLINE, width=4)
    elif v == 5:  # angry
        for x, s in ((ly, 1), (ry, -1)):
            draw_ellipse(d, [x - 14, ey - 10, x + 14, ey + 14], (255, 255, 255, 255))
            draw_ellipse(d, [x - 6, ey - 2, x + 6, ey + 8], OUTLINE)
            d.line([(x - 14, ey - 14), (x + 14, ey - 6 * s)], fill=OUTLINE, width=4)
    elif v == 6:  # dot
        for x in (ly, ry):
            draw_ellipse(d, [x - 10, ey - 10, x + 10, ey + 10], OUTLINE)
            draw_ellipse(d, [x - 4, ey - 4, x + 4, ey + 4], colors["a"])
    elif v == 7:  # cross
        for x in (ly, ry):
            draw_ellipse(d, [x - 14, ey - 14, x + 14, ey + 14], (255, 255, 255, 255))
            d.line([(x - 6, ey - 6), (x + 6, ey + 6)], fill=OUTLINE, width=3)
            d.line([(x + 6, ey - 6), (x - 6, ey + 6)], fill=OUTLINE, width=3)
    elif v == 8:  # sleepy
        for x in (ly, ry):
            draw_ellipse(d, [x - 14, ey - 6, x + 14, ey + 12], (255, 255, 255, 255))
            draw_ellipse(d, [x - 6, ey, x + 6, ey + 8], OUTLINE)
            d.arc([x - 14, ey - 14, x + 14, ey], 200, 340, fill=OUTLINE, width=3)
    elif v == 9:  # vertical pupil
        for x in (ly, ry):
            draw_ellipse(d, [x - 14, ey - 16, x + 14, ey + 16], (255, 255, 255, 255))
            d.ellipse([x - 4, ey - 12, x + 4, ey + 12], fill=OUTLINE)
    elif v == 10:  # star sparkle
        for x in (ly, ry):
            draw_ellipse(d, [x - 15, ey - 15, x + 15, ey + 15], colors["a"])
            draw_ellipse(d, [x - 6, ey - 6, x + 6, ey + 6], OUTLINE)
            d.line([(x, ey - 20), (x, ey - 12)], fill=(255, 255, 200, 255), width=2)
    else:  # winking (left closed)
        d.arc([ly - 14, ey - 10, ly + 14, ey + 10], 200, 340, fill=OUTLINE, width=4)
        draw_ellipse(d, [ry - 16, ey - 16, ry + 16, ey + 16], (255, 255, 255, 255))
        draw_ellipse(d, [ry - 8, ey - 8, ry + 8, ey + 8], OUTLINE)
        draw_ellipse(d, [ry - 3, ey - 5, ry + 2, ey], (255, 255, 255, 255))
    return save(img, f"eyes_{cls}_{v}")


def main():
    count = 0
    for cls, colors in CLASSES.items():
        gen_body(cls, colors)
        count += 1
        for v in range(VARIANTS):
            gen_tail(cls, colors, v)
            gen_back(cls, colors, v)
            gen_mouth(cls, colors, v)
            gen_horn(cls, colors, v)
            gen_ears(cls, colors, v)
            gen_eyes(cls, colors, v)
            count += 6
    print(f"Generated {count} part images in {OUT}")


if __name__ == "__main__":
    main()
