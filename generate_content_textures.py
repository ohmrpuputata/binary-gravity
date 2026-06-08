"""
Generate textures for the content-expansion update:
  * new item icons (cosmic ingot, bio serum, alien battery, gravity grenade, cosmic armor)
  * cosmic_block texture
  * 3 rare-alien entity sheets (stalker/spider, plasma caster/skeleton, hive shaman/humanoid)
  * cosmic armor body layers (cosmic_layer_1/2)
Standalone; run with the project venv:
    .venv/Scripts/python.exe generate_content_textures.py
"""
from PIL import Image
import os
import random

PROJECT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
BLOCK_TEX = os.path.join(ASSETS, "textures", "block")
ENTITY_TEX = os.path.join(ASSETS, "textures", "entity")
ARMOR_TEX = os.path.join(ASSETS, "textures", "models", "armor")
for d in (ITEM_TEX, BLOCK_TEX, ENTITY_TEX, ARMOR_TEX):
    os.makedirs(d, exist_ok=True)

CYAN = (90, 220, 230, 255)
CYAN_HI = (190, 255, 255, 255)
TEAL = (40, 150, 160, 255)
TEAL_DK = (24, 90, 100, 255)
GOLD = (230, 200, 60, 255)
GOLD_HI = (255, 240, 150, 255)
GREEN = (70, 210, 90, 255)
PURPLE = (150, 90, 210, 255)


def blank(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def save_item(img, name):
    img.save(os.path.join(ITEM_TEX, name + ".png"))
    print("item", name)


def put(px, pts, color):
    for (x, y) in pts:
        if 0 <= x < px_w[0] and 0 <= y < px_h[0]:
            px[x, y] = color


px_w = [16]
px_h = [16]


def rect(px, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = color


# ---- item icons (16x16) ----
def cosmic_ingot():
    img = blank(); px = img.load()
    for y in range(6, 11):
        for x in range(3, 13):
            shade = 30 + (x - 3) * 4 - (y - 6) * 6
            px[x, y] = (min(255, GOLD[0]), min(255, GOLD[1] + shade // 4), 40 + shade // 3, 255)
    for x in range(3, 13):
        px[x, 6] = GOLD_HI
    px[5, 7] = CYAN_HI
    px[9, 9] = CYAN
    save_item(img, "cosmic_ingot")


def bio_serum():
    img = blank(); px = img.load()
    # glass vial
    rect(px, 6, 2, 10, 4, (210, 230, 235, 255))   # cork/neck
    rect(px, 5, 4, 11, 14, (180, 210, 220, 120))  # glass
    rect(px, 6, 8, 10, 13, GREEN)                  # serum liquid
    px[7, 9] = (200, 255, 210, 255)
    save_item(img, "bio_serum")


def alien_battery():
    img = blank(); px = img.load()
    rect(px, 4, 3, 12, 14, (60, 70, 80, 255))      # casing
    rect(px, 5, 5, 11, 12, CYAN)                    # cell
    rect(px, 7, 1, 9, 3, (200, 200, 60, 255))       # + terminal
    px[6, 6] = CYAN_HI
    px[9, 10] = TEAL_DK
    save_item(img, "alien_battery")


def gravity_grenade():
    img = blank(); px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d < 5.5:
                t = 1 - d / 5.5
                px[x, y] = (int(60 + 90 * t), int(120 + 120 * t), int(150 + 100 * t), 255)
    # orbit ring
    for x in range(2, 14):
        px[x, 8] = CYAN_HI
    px[7, 8] = PURPLE
    save_item(img, "gravity_grenade")


def cosmic_piece(name, draw):
    img = blank(); px = img.load()
    draw(px)
    save_item(img, name)


def _helm(px):
    rect(px, 4, 3, 12, 10, TEAL)
    rect(px, 4, 7, 12, 10, TEAL_DK)        # face gap shadow
    rect(px, 5, 4, 11, 6, CYAN)            # visor
    for x in range(4, 12):
        px[x, 3] = CYAN_HI


def _chest(px):
    rect(px, 3, 3, 13, 13, TEAL)
    rect(px, 5, 4, 11, 11, CYAN)
    for y in range(4, 11):
        px[8, y] = CYAN_HI
    rect(px, 3, 3, 5, 6, TEAL_DK)
    rect(px, 11, 3, 13, 6, TEAL_DK)


def _legs(px):
    rect(px, 4, 2, 12, 5, TEAL)
    rect(px, 4, 5, 7, 14, TEAL)
    rect(px, 9, 5, 12, 14, TEAL)
    px[5, 7] = CYAN
    px[10, 7] = CYAN


def _boots(px):
    rect(px, 3, 8, 7, 14, TEAL)
    rect(px, 9, 8, 13, 14, TEAL)
    rect(px, 3, 12, 7, 14, TEAL_DK)
    rect(px, 9, 12, 13, 14, TEAL_DK)
    px[4, 9] = CYAN
    px[10, 9] = CYAN


# ---- block (16x16) ----
def cosmic_block():
    img = blank(); px = img.load()
    random.seed(21)
    for y in range(16):
        for x in range(16):
            n = random.randint(-12, 12)
            px[x, y] = (max(0, 40 + n), max(0, 130 + n), max(0, 140 + n), 255)
    for _ in range(14):  # cosmic sparkles
        x, y = random.randint(0, 15), random.randint(0, 15)
        px[x, y] = CYAN_HI
    for i in range(16):  # frame
        px[0, i] = TEAL_DK; px[15, i] = TEAL_DK; px[i, 0] = TEAL_DK; px[i, 15] = TEAL_DK
    img.save(os.path.join(BLOCK_TEX, "cosmic_block.png"))
    print("block cosmic_block")


# ---- entity sheets (noise tints) ----
def entity_sheet(name, w, h, base, vein, seed):
    img = Image.new("RGBA", (w, h)); px = img.load()
    random.seed(seed)
    for y in range(h):
        for x in range(w):
            n = random.randint(-18, 18)
            px[x, y] = (max(0, min(255, base[0] + n)), max(0, min(255, base[1] + n)),
                        max(0, min(255, base[2] + n)), 255)
    for _ in range((w * h) // 12):
        x, y = random.randint(0, w - 1), random.randint(0, h - 1)
        px[x, y] = vein + (255,)
    img.save(os.path.join(ENTITY_TEX, name + ".png"))
    print("entity", name, f"{w}x{h}")


# ---- cosmic armor body layers ----
def armor_layer(name, seed):
    img = Image.new("RGBA", (64, 32)); px = img.load()
    random.seed(seed)
    for y in range(32):
        for x in range(64):
            n = random.randint(-10, 10)
            band = -14 if (y % 4 == 0) else 0   # horizontal plate seams
            px[x, y] = (max(0, min(255, TEAL[0] + n + band)),
                        max(0, min(255, TEAL[1] + n + band)),
                        max(0, min(255, TEAL[2] + n + band)), 255)
    for _ in range(60):  # cyan rivets / glow
        x, y = random.randint(0, 63), random.randint(0, 31)
        px[x, y] = CYAN_HI
    img.save(os.path.join(ARMOR_TEX, name + ".png"))
    print("armor", name)


def weak_antidote():
    img = blank(); px = img.load()
    rect(px, 6, 2, 10, 4, (210, 230, 235, 255))
    rect(px, 5, 4, 11, 14, (180, 210, 220, 120))
    rect(px, 6, 9, 10, 13, (120, 210, 60, 255))
    px[7, 10] = (220, 255, 200, 255)
    save_item(img, "weak_antidote")


def bio_grappling_hook():
    img = blank(); px = img.load()
    rect(px, 7, 7, 9, 14, (100, 100, 100, 255))
    px[5, 4] = GREEN
    px[6, 5] = GREEN
    px[7, 6] = GREEN
    px[8, 6] = GREEN
    px[9, 5] = GREEN
    px[10, 4] = GREEN
    px[7, 7] = (50, 120, 60, 255)
    px[8, 7] = (50, 120, 60, 255)
    save_item(img, "bio_grappling_hook")


def cosmic_warhammer():
    img = blank(); px = img.load()
    for i in range(2, 14):
        px[15-i, i] = (120, 100, 70, 255)
    rect(px, 1, 1, 6, 6, TEAL)
    rect(px, 2, 2, 5, 5, CYAN_HI)
    save_item(img, "cosmic_warhammer")


def plasma_turret():
    img = blank(); px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx*dx + dy*dy)**0.5
            if d < 4:
                px[x, y] = CYAN
            else:
                px[x, y] = (80, 85, 90, 255)
    img.save(os.path.join(BLOCK_TEX, "plasma_turret.png"))
    print("block plasma_turret")


def swarm_beacon():
    img = blank(); px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx*dx + dy*dy)**0.5
            if d < 4:
                px[x, y] = PURPLE
            else:
                px[x, y] = (50, 45, 55, 255)
    img.save(os.path.join(BLOCK_TEX, "swarm_beacon.png"))
    print("block swarm_beacon")


def generate_all():
    cosmic_ingot()
    bio_serum()
    weak_antidote()
    bio_grappling_hook()
    cosmic_warhammer()
    alien_battery()
    gravity_grenade()
    cosmic_piece("cosmic_helmet", _helm)
    cosmic_piece("cosmic_chestplate", _chest)
    cosmic_piece("cosmic_leggings", _legs)
    cosmic_piece("cosmic_boots", _boots)
    cosmic_block()
    plasma_turret()
    swarm_beacon()
    # spider sheet = 64x32, skeleton sheet = 64x32, humanoid (zombie) sheet = 64x64
    entity_sheet("alien_stalker", 64, 32, (30, 40, 55), (90, 255, 160), 31)
    entity_sheet("plasma_caster", 64, 32, (40, 70, 55), (255, 120, 40), 32)
    entity_sheet("hive_shaman", 64, 64, (60, 45, 80), (190, 120, 255), 33)
    armor_layer("cosmic_layer_1", 41)
    armor_layer("cosmic_layer_2", 42)
    print("done")


if __name__ == "__main__":
    generate_all()
