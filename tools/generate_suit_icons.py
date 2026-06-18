"""Unified sci-fi item icons for the 7 advanced suits (helmet/chest/legs/boots).

Deliberate 16x16 pixel art: a shared silhouette per piece, recoloured per suit with
shaded plates, dark seams and an emissive visor/core accent. NOT procedural noise.

Run:  python tools/generate_suit_icons.py
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "alien-invasion", "textures", "item")
REPORTS = os.path.join(ROOT, "build", "reports")

# suit -> palette: base, lit (highlight), dark (shadow), edge (seam/outline), glow (emissive)
SUITS = {
    "cosmic":       {"base": (96, 70, 150), "lit": (140, 110, 205), "dark": (52, 36, 92),  "edge": (28, 18, 52),  "glow": (122, 226, 255)},
    "astral_prism": {"base": (86, 96, 168), "lit": (130, 150, 220), "dark": (48, 52, 104), "edge": (26, 28, 60),  "glow": (120, 235, 255)},
    "emeradium":    {"base": (54, 150, 84), "lit": (96, 214, 130),  "dark": (26, 86, 50),  "edge": (14, 48, 30),  "glow": (150, 255, 150)},
    "platinum":     {"base": (150, 162, 176), "lit": (208, 220, 232), "dark": (92, 102, 116), "edge": (44, 52, 64), "glow": (120, 210, 255)},
    "palladium":    {"base": (78, 134, 120), "lit": (120, 188, 168), "dark": (40, 80, 72),  "edge": (20, 44, 40),  "glow": (150, 240, 120)},
    "alien_hazmat": {"base": (196, 138, 40), "lit": (240, 188, 78),  "dark": (132, 86, 22), "edge": (70, 44, 12),  "glow": (120, 226, 255)},
    "alien_chem":   {"base": (96, 78, 132), "lit": (138, 118, 180),  "dark": (54, 42, 78),  "edge": (28, 20, 44),  "glow": (150, 255, 120)},
}
PARTS = ["helmet", "chestplate", "leggings", "boots"]


def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), (c[0], c[1], c[2], 255))


def fill_rows(im, rows, p):
    """rows: dict y -> (x0, x1) inclusive span filled with base; auto edge/shade."""
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            px(im, x, y, p["base"])
    # outline + top highlight + bottom shade per span
    for y, (x0, x1) in rows.items():
        px(im, x0, y, p["edge"])
        px(im, x1, y, p["edge"])
        above = rows.get(y - 1)
        below = rows.get(y + 1)
        for x in range(x0, x1 + 1):
            if above is None or x < above[0] or x > above[1]:
                px(im, x, y, p["edge"] if y == min(rows) else p["lit"])
            if below is None or x < below[0] or x > below[1]:
                px(im, x, y, p["edge"])


def helmet(p):
    im = img()
    rows = {3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12),
            8: (3, 12), 9: (4, 11), 10: (4, 11), 11: (5, 10)}
    fill_rows(im, rows, p)
    # visor band (emissive) + cheek vents
    for x in range(4, 12):
        px(im, x, 7, p["glow"])
    px(im, 5, 7, (255, 255, 255)); px(im, 10, 7, (255, 255, 255))
    px(im, 4, 6, p["dark"]); px(im, 11, 6, p["dark"])
    # antenna
    px(im, 8, 2, p["glow"]); px(im, 8, 1, p["lit"])
    return im


def chest(p):
    im = img()
    rows = {2: (3, 12), 3: (2, 13), 4: (2, 13), 5: (3, 12), 6: (3, 12),
            7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (4, 11)}
    fill_rows(im, rows, p)
    # pauldron caps
    for x in (2, 3, 12, 13):
        px(im, x, 3, p["lit"])
    # central core (emissive) + chest seams
    for y in range(5, 9):
        px(im, 7, y, p["glow"]); px(im, 8, y, p["glow"])
    px(im, 7, 6, (255, 255, 255))
    px(im, 5, 4, p["dark"]); px(im, 10, 4, p["dark"])
    px(im, 4, 8, p["dark"]); px(im, 11, 8, p["dark"])
    return im


def leggings(p):
    im = img()
    rows = {}
    for y in range(2, 6):
        rows[y] = (4, 11)
    for y in range(6, 16):
        # two legs: fill, then carve the gap
        rows[y] = (4, 11)
    fill_rows(im, rows, p)
    # carve central gap for two legs (rows 7+)
    for y in range(7, 16):
        px(im, 7, y, (0, 0, 0, 0)); px(im, 8, y, (0, 0, 0, 0))
        im.putpixel((7, y), (0, 0, 0, 0)); im.putpixel((8, y), (0, 0, 0, 0))
        px(im, 6, y, p["edge"]); px(im, 9, y, p["edge"])
    # knee plates (emissive)
    for x in (5, 6):
        px(im, x, 9, p["glow"])
    for x in (9, 10):
        px(im, x, 9, p["glow"])
    px(im, 5, 9, (255, 255, 255)); px(im, 10, 9, (255, 255, 255))
    return im


def boots(p):
    im = img()
    rows = {}
    for y in range(8, 16):
        rows[y] = (4, 11)
    fill_rows(im, rows, p)
    # split into two boots + toe caps
    for y in range(8, 16):
        im.putpixel((7, y), (0, 0, 0, 0)); im.putpixel((8, y), (0, 0, 0, 0))
        px(im, 6, y, p["edge"]); px(im, 9, y, p["edge"])
    for x in (4, 5, 6):
        px(im, x, 14, p["glow"])
    for x in (9, 10, 11):
        px(im, x, 14, p["glow"])
    return im


BUILDERS = {"helmet": helmet, "chestplate": chest, "leggings": leggings, "boots": boots}


def main():
    os.makedirs(ITEM, exist_ok=True)
    os.makedirs(REPORTS, exist_ok=True)
    for suit, p in SUITS.items():
        for part in PARTS:
            im = BUILDERS[part](p)
            im.save(os.path.join(ITEM, f"{suit}_{part}.png"))

    # contact sheet
    S = 6
    cell = 16 * S
    sheet = Image.new("RGBA", (len(PARTS) * (cell + 10) + 90, len(SUITS) * (cell + 10) + 10), (26, 28, 38, 255))
    d = ImageDraw.Draw(sheet)
    for r, (suit, p) in enumerate(SUITS.items()):
        y = 10 + r * (cell + 10)
        d.text((4, y + cell // 2), suit, fill=(220, 225, 235, 255))
        for c, part in enumerate(PARTS):
            im = Image.open(os.path.join(ITEM, f"{suit}_{part}.png")).resize((cell, cell), Image.NEAREST)
            sheet.alpha_composite(im, (80 + c * (cell + 10), y))
    sheet.save(os.path.join(REPORTS, "suit_icons_preview.png"))
    print("Wrote 28 suit item icons + build/reports/suit_icons_preview.png")


if __name__ == "__main__":
    main()
