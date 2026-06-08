"""Draws the Alien Worker spawn-egg icon (16x16).

A spawn-egg silhouette in worker colours (acid-green shell, dark spots) with a
small amber "hi-vis" band so it reads as a labourer rather than a soldier. Soft
shading + a 1px dark outline keep it consistent with the other item art and
avoid the flat/eyestrain look of a placeholder.
"""
from PIL import Image

W = H = 16
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

SHELL      = (74, 163, 42, 255)    # acid green base
SHELL_HI   = (139, 214, 96, 255)   # highlight
SHELL_LO   = (47, 110, 26, 255)    # shadow
SPOT       = (28, 74, 18, 255)     # dark speckles
BAND       = (240, 176, 48, 255)   # hi-vis amber band
BAND_LO    = (186, 128, 24, 255)
OUTLINE    = (16, 34, 12, 255)

# egg silhouette: column -> (top, bottom) inclusive, narrow at top
SPAN = {
    3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (2, 13),
    7: (2, 13), 8: (2, 13), 9: (2, 13), 10: (3, 12),
    11: (4, 11), 12: (5, 10),
}

def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = c

# fill base
for x, (t, b) in SPAN.items():
    for y in range(t, b + 1):
        put(x, y, SHELL)

# vertical shading (left light, right dark)
for x, (t, b) in SPAN.items():
    for y in range(t, b + 1):
        if x <= 5:
            put(x, y, SHELL_HI if y < (t + b) // 2 else SHELL)
        elif x >= 10:
            put(x, y, SHELL_LO)

# hi-vis amber band across the middle
for x, (t, b) in SPAN.items():
    put(x, 7, BAND)
    put(x, 8, BAND if x <= 7 else BAND_LO)

# dark speckles (worker = grimy)
for (sx, sy) in [(5, 4), (8, 5), (10, 10), (6, 11), (9, 11), (5, 9)]:
    if sx in SPAN and SPAN[sx][0] <= sy <= SPAN[sx][1] and sy not in (7, 8):
        put(sx, sy, SPOT)

# 1px outline around the silhouette
def is_shell(x, y):
    return x in SPAN and SPAN[x][0] <= y <= SPAN[x][1]

for x in range(W):
    for y in range(H):
        if not is_shell(x, y):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if is_shell(x + dx, y + dy):
                    put(x, y, OUTLINE)
                    break

out = "src/main/resources/assets/alien-invasion/textures/item/alien_worker_spawn_egg.png"
img.save(out)
print("wrote", out)
