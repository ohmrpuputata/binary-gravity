"""
Better pixel-art for the core crafting materials (they were flat blobs before):
alien_alloy, cosmic_ingot, cosmic_shard, hive_core, infested_flesh, alien_battery.

Run: .venv/Scripts/python.exe generate_materials_art.py
"""
from PIL import Image
import os, math, random

PROJECT = os.path.dirname(os.path.abspath(__file__))
ITEM = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion", "textures", "item")
os.makedirs(ITEM, exist_ok=True)
OUTLINE = (16, 14, 24, 255)


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


def ingot(name, base, hi, dk, spark):
    img = blank(); px = img.load()
    # parallelogram ingot
    rect(px, 4, 7, 11, 11, base)
    rect(px, 5, 6, 10, 6, base)
    rect(px, 5, 6, 9, 6, hi)          # top highlight
    rect(px, 4, 11, 11, 11, dk)       # bottom shadow
    put(px, 4, 7, hi); put(px, 5, 7, hi)
    put(px, 9, 9, spark); put(px, 7, 8, hi)
    save(img, name)


def alien_alloy():
    ingot("alien_alloy", (54, 150, 120, 255), (130, 225, 185, 255), (28, 92, 70, 255), (200, 255, 230, 255))


def cosmic_ingot():
    ingot("cosmic_ingot", (104, 78, 196, 255), (172, 140, 248, 255), (58, 38, 116, 255), (150, 240, 255, 255))


def cosmic_shard():
    img = blank(); px = img.load()
    BASE = (120, 95, 215, 255); HI = (185, 160, 250, 255); DK = (64, 44, 130, 255); CORE = (190, 245, 255, 255)
    cells = {
        (8, 2): HI,
        (7, 3): BASE, (8, 3): HI, (9, 3): BASE,
        (6, 4): DK, (7, 4): BASE, (8, 4): CORE, (9, 4): BASE, (10, 4): DK,
        (6, 5): BASE, (7, 5): CORE, (8, 5): HI, (9, 5): BASE, (10, 5): DK,
        (6, 6): BASE, (7, 6): BASE, (8, 6): CORE, (9, 6): BASE, (10, 6): DK,
        (6, 7): DK, (7, 7): BASE, (8, 7): HI, (9, 7): BASE, (10, 7): DK,
        (7, 8): DK, (8, 8): BASE, (9, 8): BASE,
        (7, 9): DK, (8, 9): CORE, (9, 9): DK,
        (8, 10): BASE,
        (8, 11): DK,
    }
    for (x, y), c in cells.items():
        put(px, x, y, c)
    save(img, "cosmic_shard")


def hive_core():
    img = blank(); px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.0:
                if d < 1.6:
                    px[x, y] = (255, 235, 150, 255)
                elif d < 3.0:
                    px[x, y] = (255, 150, 60, 255)
                elif d < 4.6:
                    px[x, y] = (200, 60, 40, 255)
                else:
                    px[x, y] = (120, 24, 22, 255)
    # dark organic veins
    for (x, y) in [(7, 4), (5, 7), (10, 8), (8, 11), (6, 9)]:
        put(px, x, y, (70, 12, 14, 255))
    put(px, 6, 6, (255, 245, 200, 255))
    save(img, "hive_core")


def infested_flesh():
    img = blank(); px = img.load()
    random.seed(5)
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 8) ** 2 + (y - 8) ** 2 <= 24:
                if random.random() < 0.25:
                    px[x, y] = (158, 86, 96, 255)   # raw pink
                else:
                    n = random.randint(-18, 14)
                    px[x, y] = (max(0, 96 + n), max(0, 128 + n), max(0, 58 + n), 255)
    put(px, 6, 6, (190, 220, 150, 255))
    put(px, 10, 9, (190, 110, 120, 255))
    save(img, "infested_flesh")


def alien_battery():
    img = blank(); px = img.load()
    CASE = (60, 70, 64, 255); CASE_HI = (110, 124, 112, 255); CASE_DK = (38, 46, 42, 255)
    GLOW = (120, 240, 120, 255); GLOW_HI = (210, 255, 200, 255); CAP = (160, 160, 150, 255)
    BOLT = (250, 235, 90, 255)
    rect(px, 4, 3, 11, 13, CASE)
    rect(px, 4, 3, 4, 13, CASE_HI)
    rect(px, 11, 3, 11, 13, CASE_DK)
    rect(px, 6, 2, 9, 2, CAP)          # terminal
    rect(px, 5, 5, 10, 11, GLOW)       # energy window
    # lightning bolt
    for (x, y) in [(8, 5), (7, 6), (8, 7), (7, 8), (8, 8), (7, 10), (8, 6), (7, 7)]:
        put(px, x, y, BOLT)
    put(px, 6, 6, GLOW_HI)
    save(img, "alien_battery")


def main():
    alien_alloy(); cosmic_ingot(); cosmic_shard(); hive_core(); infested_flesh(); alien_battery()
    print("done")


if __name__ == "__main__":
    main()
