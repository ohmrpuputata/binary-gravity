"""
Textures for the suits + blaster round:
  * Proper Hazmat icons (gas-mask / suit / pants / boots, yellow hazard style).
  * New Chitin armour: item icons + worn body layers (chitin_layer_1/2.png).
  * plasma_bolt: the Alien Blaster's glowing projectile round.

Run with:  .venv/Scripts/python.exe generate_suits_art.py
"""
from PIL import Image
import os, random

PROJECT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion")
ITEM = os.path.join(A, "textures", "item")
ARMOR = os.path.join(A, "textures", "models", "armor")
for d in (ITEM, ARMOR):
    os.makedirs(d, exist_ok=True)

OUTLINE = (16, 14, 24, 255)


def clamp(v):
    return max(0, min(255, int(v)))


def lerp(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)


def add_outline(img, color=OUTLINE):
    src = img.copy(); sp = src.load(); px = img.load()
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    px[x, y] = color
                    break


def save(img, name):
    add_outline(img)
    img.save(os.path.join(ITEM, name + ".png"))
    print("item", name)


# --------------------------------------------------------------- HAZMAT
HAZ = (232, 200, 46, 255)
HAZ_HI = (255, 235, 120, 255)
HAZ_DK = (170, 140, 24, 255)
BLACK = (40, 40, 44, 255)
GLASS = (130, 210, 220, 255)
FILTER = (70, 74, 80, 255)


def hazmat_helmet():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 10, HAZ)
    rect(px, 4, 3, 11, 3, HAZ_HI)
    rect(px, 5, 5, 10, 8, BLACK)        # mask face
    rect(px, 6, 6, 9, 7, GLASS)         # visor
    rect(px, 6, 9, 9, 10, FILTER)       # filter
    put(px, 7, 9, HAZ_DK); put(px, 8, 9, HAZ_DK)
    save(img, "hazmat_helmet")


def hazmat_chestplate():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 12, HAZ)
    rect(px, 4, 3, 11, 3, HAZ_HI)
    rect(px, 3, 4, 3, 7, HAZ); rect(px, 12, 4, 12, 7, HAZ)
    rect(px, 7, 4, 8, 12, BLACK)        # zipper
    # hazard chevrons
    put(px, 5, 7, BLACK); put(px, 6, 8, BLACK); put(px, 10, 7, BLACK); put(px, 9, 8, BLACK)
    save(img, "hazmat_chestplate")


def hazmat_leggings():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 6, HAZ)
    rect(px, 4, 3, 11, 3, HAZ_HI)
    rect(px, 4, 7, 6, 13, HAZ); rect(px, 9, 7, 11, 13, HAZ)
    rect(px, 4, 9, 6, 9, BLACK); rect(px, 9, 9, 11, 9, BLACK)  # knee bands
    save(img, "hazmat_leggings")


def hazmat_boots():
    img = blank(); px = img.load()
    rect(px, 4, 5, 6, 11, HAZ); rect(px, 9, 5, 11, 11, HAZ)
    rect(px, 4, 5, 6, 5, HAZ_HI); rect(px, 9, 5, 11, 5, HAZ_HI)
    rect(px, 4, 12, 7, 12, BLACK); rect(px, 9, 12, 12, 12, BLACK)
    save(img, "hazmat_boots")


# --------------------------------------------------------------- CHITIN
CH = (92, 120, 58, 255)
CH_HI = (150, 185, 96, 255)
CH_DK = (48, 66, 32, 255)
SHEEN = (158, 96, 206, 255)   # purple iridescence
SHELL = (74, 60, 40, 255)     # brown shell edge


def chitin_helmet():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 9, CH)
    rect(px, 4, 3, 11, 3, CH_HI)
    rect(px, 4, 10, 5, 11, CH); rect(px, 10, 10, 11, 11, CH)
    rect(px, 6, 6, 9, 7, CH_DK)
    put(px, 6, 6, SHEEN); put(px, 9, 6, SHEEN)
    put(px, 7, 4, CH_HI)
    save(img, "chitin_helmet")


def chitin_chestplate():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 12, CH)
    rect(px, 4, 3, 11, 3, CH_HI)
    rect(px, 3, 4, 3, 7, SHELL); rect(px, 12, 4, 12, 7, SHELL)
    rect(px, 5, 5, 10, 5, CH_DK); rect(px, 5, 8, 10, 8, CH_DK)  # carapace ridges
    put(px, 7, 6, SHEEN); put(px, 9, 10, SHEEN)
    save(img, "chitin_chestplate")


def chitin_leggings():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 6, CH)
    rect(px, 4, 3, 11, 3, CH_HI)
    rect(px, 4, 7, 6, 13, CH); rect(px, 9, 7, 11, 13, CH)
    put(px, 5, 9, CH_DK); put(px, 10, 9, CH_DK)
    put(px, 7, 4, SHEEN)
    save(img, "chitin_leggings")


def chitin_boots():
    img = blank(); px = img.load()
    rect(px, 4, 5, 6, 11, CH); rect(px, 9, 5, 11, 11, CH)
    rect(px, 4, 5, 6, 5, CH_HI); rect(px, 9, 5, 11, 5, CH_HI)
    rect(px, 4, 12, 7, 12, SHELL); rect(px, 9, 12, 12, 12, SHELL)
    put(px, 5, 7, SHEEN)
    save(img, "chitin_boots")


# --- Region-aware worn-armor painter ----------------------------------------
# Front-face rectangles in the vanilla 64x32 humanoid UV (x0,y0,x1,y1 inclusive).
# We fill the whole sheet with a shaded suit base, then add detail on the visible
# front faces so the worn armour reads as an actual SUIT, not a flat striped sheet.
HEAD_FRONT = (8, 8, 15, 15)
BODY_FRONT = (20, 20, 27, 31)
ARM_FRONT = (44, 20, 47, 31)
LEG_FRONT = (4, 20, 7, 31)
ALL_FACES = [HEAD_FRONT, BODY_FRONT, ARM_FRONT, LEG_FRONT,
             (0, 8, 7, 15), (16, 8, 31, 15), (24, 8, 31, 15),          # head sides/back
             (16, 20, 19, 31), (28, 20, 39, 31),                       # body sides/back
             (40, 20, 43, 31), (48, 20, 55, 31),                       # arm sides/back
             (0, 20, 3, 31), (8, 20, 15, 31),                          # leg sides/back
             (8, 0, 23, 7), (44, 16, 51, 19), (4, 16, 11, 19), (20, 16, 35, 19)]


def _grad_fill(px, top, bottom):
    """Shade each face panel top-light -> bottom-dark for a moulded look."""
    for (x0, y0, x1, y1) in ALL_FACES:
        h = max(1, y1 - y0)
        for y in range(y0, y1 + 1):
            base = lerp(top, bottom, (y - y0) / h)
            rgba = (base[0], base[1], base[2], 255)
            for x in range(x0, x1 + 1):
                px[x, y] = rgba


def paint_layer(fname, top, bottom, detail):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); px = img.load()
    _grad_fill(px, top, bottom)
    detail(px)
    img.save(os.path.join(ARMOR, fname))
    print("armor", fname)


def chitin_layers():
    sheen = (158, 96, 206, 255)
    ridge = (44, 60, 30, 255)
    shell = (74, 60, 40, 255)

    def detail(px):
        # Head: dark eye slit + iridescent sheen.
        rect(px, 9, 10, 14, 11, ridge)
        px[10, 10] = sheen; px[13, 10] = sheen
        # Body: layered carapace ridges + shell edges + sheen highlight.
        for ry in (22, 25, 28):
            rect(px, 20, ry, 27, ry, ridge)
        rect(px, 20, 20, 20, 31, shell); rect(px, 27, 20, 27, 31, shell)
        px[23, 21] = sheen; px[25, 27] = sheen
        # Arms: ridge + sheen.
        rect(px, 44, 24, 47, 24, ridge); px[45, 21] = sheen
        # Legs: knee ridge + shell.
        rect(px, 4, 26, 7, 26, ridge); rect(px, 4, 20, 4, 31, shell)

    for fname in ("chitin_layer_1.png", "chitin_layer_2.png"):
        paint_layer(fname, (118, 150, 78), (52, 70, 36), detail)


# --------------------------------------------------------------- PLASMA BOLT
def plasma_bolt():
    img = blank(); px = img.load()
    import math
    OUT_ = (40, 110, 30, 255); MID = (90, 210, 70, 255); HI = (170, 255, 120, 255); CORE = (235, 255, 200, 255)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.6:
                if d < 1.4:
                    px[x, y] = CORE
                elif d < 2.8:
                    px[x, y] = HI
                elif d < 4.3:
                    px[x, y] = MID
                else:
                    px[x, y] = OUT_
    for (x, y) in [(3, 6), (12, 7), (7, 2), (9, 12)]:
        put(px, x, y, HI)
    save(img, "plasma_bolt")


def hazmat_layers():
    """Worn hazmat suit skin (vanilla 64x32 layout): a moulded yellow suit with a
    dark visor, chest seam, a waist hazard band, and dark cuffs/boots - reads as a
    proper suit instead of a flat sheet of diagonal stripes."""
    dark = (34, 34, 38, 255)
    glass = (130, 210, 220, 255)
    haz = (28, 28, 30, 255)
    yellow = (245, 215, 60, 255)

    def detail(px):
        # Head: respirator visor + filter.
        rect(px, 9, 10, 14, 12, dark)
        rect(px, 10, 11, 13, 11, glass)
        # Body: central zip seam + a black/yellow hazard band at the waist.
        rect(px, 23, 20, 24, 31, dark)
        for x in range(20, 28):
            px[x, 29] = haz if ((x) % 2 == 0) else yellow
            px[x, 30] = yellow if ((x) % 2 == 0) else haz
        # Arms: dark cuffs at the wrist.
        rect(px, 44, 30, 47, 31, dark)
        # Legs: knee band + dark boot.
        rect(px, 4, 26, 7, 26, dark)
        rect(px, 4, 30, 7, 31, dark)

    for fname in ("hazmat_layer_1.png", "hazmat_layer_2.png"):
        paint_layer(fname, (250, 220, 70), (205, 165, 24), detail)


def plasma_cell():
    img = blank(); px = img.load()
    CASE = (70, 76, 86, 255); CASE_HI = (120, 128, 140, 255); CASE_DK = (44, 48, 58, 255)
    GLOW = (110, 235, 90, 255); GLOW_HI = (200, 255, 180, 255); CAP = (150, 150, 160, 255)
    rect(px, 5, 3, 10, 12, CASE)
    rect(px, 5, 3, 5, 12, CASE_HI)
    rect(px, 10, 3, 10, 12, CASE_DK)
    rect(px, 6, 5, 9, 10, GLOW)
    put(px, 7, 6, GLOW_HI); put(px, 8, 8, GLOW_HI)
    rect(px, 6, 2, 9, 2, CAP)
    rect(px, 6, 13, 9, 13, CAP)
    save(img, "plasma_cell")


def rally_banner():
    img = blank(); px = img.load()
    POLE = (120, 80, 50, 255); POLE_DK = (80, 52, 32, 255)
    CLOTH = (104, 78, 196, 255); CLOTH_HI = (150, 120, 230, 255); EMB = (235, 205, 70, 255)
    for y in range(2, 15):
        put(px, 4, y, POLE)
    put(px, 4, 2, POLE_DK)
    rect(px, 5, 3, 12, 10, CLOTH)
    rect(px, 5, 3, 12, 3, CLOTH_HI)
    for x in (5, 7, 9, 11):
        put(px, x, 11, CLOTH)
    for (x, y) in [(8, 4), (7, 6), (8, 6), (9, 6), (8, 5), (8, 7)]:
        put(px, x, y, EMB)
    save(img, "rally_banner")


# --------------------------------------------------------------- LIGHT HAZMAT (alien-skin suit)
LH = (150, 168, 120, 255)
LH_HI = (188, 204, 152, 255)
LH_DK = (104, 120, 78, 255)
SEAM = (70, 84, 52, 255)
VIS = (150, 205, 185, 255)


def light_hazmat_helmet():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 10, LH)
    rect(px, 4, 3, 11, 3, LH_HI)
    rect(px, 5, 5, 10, 8, SEAM)        # hood opening
    rect(px, 6, 6, 9, 7, VIS)          # visor
    put(px, 7, 9, LH_DK); put(px, 8, 9, LH_DK)
    save(img, "light_hazmat_helmet")


def light_hazmat_chestplate():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 12, LH)
    rect(px, 4, 3, 11, 3, LH_HI)
    rect(px, 3, 4, 3, 7, LH); rect(px, 12, 4, 12, 7, LH)
    rect(px, 7, 4, 8, 12, SEAM)        # stitched seam
    put(px, 5, 7, LH_DK); put(px, 10, 9, LH_DK)
    save(img, "light_hazmat_chestplate")


def light_hazmat_leggings():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 6, LH)
    rect(px, 4, 3, 11, 3, LH_HI)
    rect(px, 4, 7, 6, 13, LH); rect(px, 9, 7, 11, 13, LH)
    rect(px, 5, 9, 5, 9, SEAM); rect(px, 10, 9, 10, 9, SEAM)
    save(img, "light_hazmat_leggings")


def light_hazmat_boots():
    img = blank(); px = img.load()
    rect(px, 4, 5, 6, 11, LH); rect(px, 9, 5, 11, 11, LH)
    rect(px, 4, 5, 6, 5, LH_HI); rect(px, 9, 5, 11, 5, LH_HI)
    rect(px, 4, 12, 7, 12, SEAM); rect(px, 9, 12, 12, 12, SEAM)
    save(img, "light_hazmat_boots")


def light_hazmat_layers():
    def detail(px):
        rect(px, 9, 10, 14, 12, SEAM)          # head: hood opening
        rect(px, 10, 11, 13, 11, VIS)          # visor
        rect(px, 23, 20, 24, 31, SEAM)         # body seam
        rect(px, 44, 30, 47, 31, LH_DK)        # cuffs
        rect(px, 4, 30, 7, 31, LH_DK)          # boot
    for fname in ("light_hazmat_layer_1.png", "light_hazmat_layer_2.png"):
        paint_layer(fname, (158, 176, 126), (108, 124, 80), detail)


def main():
    light_hazmat_helmet(); light_hazmat_chestplate(); light_hazmat_leggings(); light_hazmat_boots()
    light_hazmat_layers()
    hazmat_helmet(); hazmat_chestplate(); hazmat_leggings(); hazmat_boots()
    chitin_helmet(); chitin_chestplate(); chitin_leggings(); chitin_boots()
    chitin_layers()
    hazmat_layers()
    plasma_bolt()
    plasma_cell(); rally_banner()
    print("done")


if __name__ == "__main__":
    main()
