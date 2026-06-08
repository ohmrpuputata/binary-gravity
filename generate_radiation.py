"""Textures for the Pure Radiation Block + raw Radiation Crystal item."""
import math
import os
import random
from PIL import Image

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "src", "main", "resources", "assets", "alien-invasion")
BLOCK = os.path.join(A, "textures", "block")
ITEM = os.path.join(A, "textures", "item")
for d in (BLOCK, ITEM):
    os.makedirs(d, exist_ok=True)


def pure_radiation_block():
    random.seed(909)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    base = (30, 44, 24)
    for y in range(16):
        for x in range(16):
            n = random.randint(-8, 8)
            px[x, y] = (max(0, base[0] + n), max(0, base[1] + n), max(0, base[2] + n), 255)
    # Glowing radioactive cells.
    cells = [(4, 4), (11, 5), (7, 9), (12, 12), (3, 12), (9, 13)]
    for (cx, cy) in cells:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.hypot(dx, dy)
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16 and d <= 2.2:
                    if d < 0.8:
                        px[x, y] = (214, 255, 150, 255)
                    elif d < 1.5:
                        px[x, y] = (150, 230, 64, 255)
                    else:
                        px[x, y] = (96, 168, 44, 255)
    # Dark fissures.
    for _ in range(4):
        x = random.randint(0, 15); y = random.randint(0, 15)
        for _ in range(random.randint(3, 6)):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (16, 24, 12, 255)
            x = max(0, min(15, x + random.randint(-1, 1)))
            y = max(0, min(15, y + random.randint(-1, 1)))
    img.save(os.path.join(BLOCK, "pure_radiation_block.png"))
    print("block pure_radiation_block")


def radiation_crystal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    random.seed(910)
    core = (210, 255, 150); body = (132, 224, 70); edge = (52, 116, 40); hi = (236, 255, 200)
    # Diamond/crystal silhouette by column span.
    span = {6: (5, 11), 7: (3, 13), 8: (2, 14), 9: (3, 13), 10: (5, 11),
            5: (6, 10), 11: (6, 10)}
    for x, (t, b) in span.items():
        for y in range(t, b + 1):
            mid = (t + b) / 2
            c = core if abs(y - mid) < 1.5 else body
            n = random.randint(-14, 14)
            px[x, y] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n)), max(0, min(255, c[2] + n)), 255)
    # Facet highlights + outline.
    for (hx, hy) in [(7, 6), (8, 5), (8, 8)]:
        px[hx, hy] = hi + (255,)

    def is_body(x, y):
        return x in span and span[x][0] <= y <= span[x][1]
    for x in range(16):
        for y in range(16):
            if not is_body(x, y):
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if is_body(x + dx, y + dy):
                        px[x, y] = edge + (255,)
                        break
    img.save(os.path.join(ITEM, "radiation_crystal.png"))
    print("item radiation_crystal")


if __name__ == "__main__":
    pure_radiation_block()
    radiation_crystal()
