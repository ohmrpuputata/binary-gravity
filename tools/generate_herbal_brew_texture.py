"""Generate the 16x16 item texture for the Herbal Brew consumable.

A small round-bottomed flask of green herbal infusion with a cork and a tiny
leaf sprig, in the muted Minecraft item palette. Pure pixel placement (no
procedural noise) so it stays crisp and reads clearly in the hotbar.
"""
from PIL import Image

W = H = 16
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

# Palette
GLASS_LIGHT = (203, 224, 220, 255)
GLASS_DARK = (150, 178, 173, 255)
GLASS_EDGE = (96, 120, 116, 255)
BREW_LIGHT = (118, 178, 70, 255)
BREW_DARK = (74, 128, 44, 255)
BREW_EDGE = (52, 92, 32, 255)
CORK = (146, 104, 58, 255)
CORK_DARK = (110, 76, 40, 255)
LEAF = (96, 168, 72, 255)
LEAF_DARK = (62, 120, 48, 255)


def fill(coords, color):
    for x, y in coords:
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = color


# Flask body outline (round bottom, rows 7..14)
body_rows = {
    7:  (6, 9),
    8:  (5, 10),
    9:  (4, 11),
    10: (4, 11),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}
for y, (x0, x1) in body_rows.items():
    for x in range(x0, x1 + 1):
        edge = (x == x0 or x == x1 or y == 14 or y == 7)
        px[x, y] = GLASS_EDGE if edge else GLASS_LIGHT

# Neck (rows 4..6)
for y in range(4, 7):
    for x in (6, 7, 8, 9):
        edge = (x == 6 or x == 9)
        px[x, y] = GLASS_EDGE if edge else GLASS_LIGHT

# Brew liquid fills the lower body (rows 9..13 inside the glass)
brew_rows = {
    9:  (5, 10),
    10: (5, 10),
    11: (5, 10),
    12: (5, 10),
    13: (6, 9),
}
for y, (x0, x1) in brew_rows.items():
    for x in range(x0, x1 + 1):
        if px[x, y] == GLASS_EDGE:
            continue
        if y == 9:
            px[x, y] = BREW_EDGE  # liquid surface line
        elif x == x0 or x == x1 or y == 13:
            px[x, y] = BREW_DARK
        else:
            px[x, y] = BREW_LIGHT

# Glass highlight streak
fill([(6, 10), (6, 11)], GLASS_LIGHT)
fill([(5, 10)], (230, 244, 240, 255))

# Cork stopper (rows 2..4)
for y in (2, 3):
    for x in (6, 7, 8, 9):
        px[x, y] = CORK if y == 2 else CORK_DARK
fill([(7, 1), (8, 1)], CORK)

# Leaf sprig poking out by the neck
fill([(10, 3), (11, 2)], LEAF)
fill([(11, 3), (10, 4)], LEAF_DARK)

img.save("src/main/resources/assets/alien-invasion/textures/item/herbal_brew.png")
print("wrote herbal_brew.png")
