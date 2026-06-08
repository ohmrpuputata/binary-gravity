"""
Textures for: the final boss (Swarm Mother, 128x128 organic chitin sheet for the
custom SwarmMotherModel), updated Cosmic armour (item icons + worn body layers),
a clearer Cosmic Warhammer, and the new super-tools / items.

Self-contained (no external resource packs). Run with:
    .venv/Scripts/python.exe generate_boss_armor_art.py
"""
from PIL import Image
import os, random, math

PROJECT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion")
ITEM = os.path.join(A, "textures", "item")
ENT = os.path.join(A, "textures", "entity")
ARMOR = os.path.join(A, "textures", "models", "armor")
for d in (ITEM, ENT, ARMOR):
    os.makedirs(d, exist_ok=True)

OUTLINE = (16, 14, 24, 255)


def clamp(v):
    return max(0, min(255, int(v)))


def lerp(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def blank(n=16):
    return Image.new("RGBA", (n, n), (0, 0, 0, 0))


def put(px, x, y, c, n=16):
    if 0 <= x < n and 0 <= y < n:
        px[x, y] = c


def rect(px, x0, y0, x1, y1, c, n=16):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c, n)


def add_outline(img, color=OUTLINE):
    n = img.width
    src = img.copy(); sp = src.load(); px = img.load()
    for y in range(n):
        for x in range(n):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < n and 0 <= ny < n and sp[nx, ny][3] != 0:
                    px[x, y] = color
                    break


def save_item(img, name):
    add_outline(img)
    img.save(os.path.join(ITEM, name + ".png"))
    print("item", name)


# =============================================================== BOSS SHEET
def swarm_mother():
    n = 128
    img = Image.new("RGBA", (n, n))
    px = img.load()
    random.seed(2026)
    top = (118, 70, 150)     # violet highlight chitin
    bottom = (38, 22, 58)    # deep shadow
    vein = (210, 80, 215)    # glowing magenta veins
    for y in range(n):
        base = lerp(top, bottom, y / (n - 1))
        for x in range(n):
            m = random.randint(-16, 12)             # mottled pores
            plate = -14 if ((x // 6 + y // 6) % 2 == 0) else 0  # carapace plates
            px[x, y] = (clamp(base[0] + m + plate), clamp(base[1] + m + plate // 2),
                        clamp(base[2] + m), 255)
    # glowing magenta veins
    for _ in range(120):
        x = random.randint(0, n - 1); y = random.randint(0, n - 1)
        px[x, y] = vein + (255,)
        if x + 1 < n:
            px[x + 1, y] = lerp(vein, (255, 200, 255), 0.4) + (255,)
    # GLOWING EYES region (matches eye cube texOffs 100,0 and 110,0)
    for (u, col) in ((100, (255, 70, 40)), (110, (255, 70, 40))):
        for yy in range(0, 4):
            for xx in range(u, u + 6):
                px[xx, yy] = col + (255,)
        px[u + 2, 1] = (255, 230, 160, 255)
        px[u + 3, 1] = (255, 230, 160, 255)
    img.save(os.path.join(ENT, "swarm_mother.png"))
    print("entity swarm_mother.png 128x128")


# ====================================================== COSMIC ARMOR LAYERS
def cosmic_armor_layers():
    """Worn-on-body armour: vanilla layout (64x32). Filled with cosmic metal so
    every UV region maps onto plating; sparkles + cyan trim banding for detail."""
    for fname in ("cosmic_layer_1.png", "cosmic_layer_2.png"):
        img = Image.new("RGBA", (64, 32))
        px = img.load()
        random.seed(hash(fname) & 0xffff)
        top = (120, 92, 205); bottom = (52, 34, 104)
        for y in range(32):
            base = lerp(top, bottom, y / 31)
            for x in range(64):
                m = random.randint(-10, 8)
                seam = -28 if (x % 8 == 0 or y % 8 == 0) else 0
                px[x, y] = (clamp(base[0] + m + seam), clamp(base[1] + m + seam),
                            clamp(base[2] + m + seam), 255)
        # cyan trim band + star sparkles
        for x in range(64):
            if (x // 2) % 2 == 0:
                px[x, 1] = (120, 235, 255, 255)
        for _ in range(60):
            x = random.randint(0, 63); y = random.randint(0, 31)
            px[x, y] = (200, 245, 255, 255)
        img.save(os.path.join(ARMOR, fname))
        print("armor", fname)


# ====================================================== COSMIC ARMOR ICONS
COS = (104, 78, 196, 255)
COS_HI = (172, 140, 248, 255)
COS_DK = (58, 38, 116, 255)
TRIM = (120, 235, 255, 255)
SPARK = (220, 250, 255, 255)


def cosmic_helmet():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 9, COS)
    rect(px, 4, 3, 11, 3, COS_HI)
    rect(px, 4, 10, 5, 11, COS); rect(px, 10, 10, 11, 11, COS)  # cheek guards
    rect(px, 6, 6, 9, 7, COS_DK)  # visor slot
    put(px, 6, 6, TRIM); put(px, 9, 6, TRIM)
    put(px, 7, 4, SPARK); put(px, 10, 5, SPARK)
    save_item(img, "cosmic_helmet")


def cosmic_chestplate():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 12, COS)
    rect(px, 4, 3, 11, 3, COS_HI)
    rect(px, 3, 4, 3, 7, COS); rect(px, 12, 4, 12, 7, COS)  # shoulders
    rect(px, 7, 4, 8, 11, COS_DK)  # chest seam
    put(px, 7, 5, TRIM); put(px, 8, 5, TRIM)
    put(px, 5, 6, SPARK); put(px, 10, 9, SPARK)
    save_item(img, "cosmic_chestplate")


def cosmic_leggings():
    img = blank(); px = img.load()
    rect(px, 4, 3, 11, 6, COS)
    rect(px, 4, 3, 11, 3, COS_HI)
    rect(px, 4, 7, 6, 13, COS); rect(px, 9, 7, 11, 13, COS)  # legs
    put(px, 5, 8, TRIM); put(px, 10, 8, TRIM)
    put(px, 7, 4, SPARK)
    save_item(img, "cosmic_leggings")


def cosmic_boots():
    img = blank(); px = img.load()
    rect(px, 4, 5, 6, 11, COS); rect(px, 9, 5, 11, 11, COS)
    rect(px, 4, 5, 6, 5, COS_HI); rect(px, 9, 5, 11, 5, COS_HI)
    rect(px, 4, 12, 7, 12, COS_DK); rect(px, 9, 12, 12, 12, COS_DK)  # soles
    put(px, 5, 7, TRIM); put(px, 10, 7, TRIM)
    save_item(img, "cosmic_boots")


# ====================================================== CLEARER WARHAMMER
def cosmic_warhammer():
    img = blank(); px = img.load()
    HEAD = (104, 78, 196, 255); HEAD_HI = (172, 140, 248, 255); HEAD_DK = (58, 38, 116, 255)
    HANDLE = (120, 80, 55, 255); HANDLE_DK = (80, 50, 35, 255)
    # big chunky double hammer head, top
    rect(px, 3, 2, 12, 7, HEAD)
    rect(px, 3, 2, 12, 2, HEAD_HI)        # top highlight
    rect(px, 3, 7, 12, 7, HEAD_DK)        # bottom shadow
    rect(px, 3, 2, 3, 7, HEAD_DK)         # left face shadow
    rect(px, 12, 2, 12, 7, HEAD_DK)       # right face shadow
    rect(px, 5, 3, 7, 4, HEAD_HI)         # face highlight
    put(px, 9, 4, SPARK); put(px, 10, 3, SPARK); put(px, 8, 6, TRIM)  # cosmic sparkle
    # collar
    rect(px, 6, 8, 9, 8, HEAD_DK)
    # handle
    for y in range(9, 15):
        put(px, 7, y, HANDLE); put(px, 8, y, HANDLE_DK)
    rect(px, 6, 14, 9, 14, HANDLE_DK)     # grip end
    save_item(img, "cosmic_warhammer")


# ====================================================== NEW SUPER TOOLS
def _cosmic_handle(px):
    main = [(3 + i, 13 - i) for i in range(8)]
    shadow = [(3 + i, 14 - i) for i in range(8)]
    for (x, y) in shadow:
        put(px, x, y, (60, 42, 120, 255))
    for (x, y) in main:
        put(px, x, y, (140, 110, 220, 255))


def cosmic_pickaxe():
    img = blank(); px = img.load()
    _cosmic_handle(px)
    arc = [(6, 6), (7, 5), (8, 4), (9, 3), (10, 3), (11, 3), (12, 4), (13, 5), (14, 6)]
    for (x, y) in arc:
        put(px, x, y, COS)
    for (x, y) in [(9, 4), (10, 4), (11, 4)]:
        put(px, x, y, COS_HI)
    put(px, 10, 3, SPARK); put(px, 13, 5, TRIM)
    save_item(img, "cosmic_pickaxe")


def star_cleaver():
    img = blank(); px = img.load()
    _cosmic_handle(px)
    # broad axe/cleaver head upper-right
    blob = []
    for y in range(2, 10):
        for x in range(9, 15):
            if (x - 9) + (y - 2) <= 8 and x - 9 >= (y - 9):
                blob.append((x, y))
    for (x, y) in blob:
        put(px, x, y, COS)
    for (x, y) in [(10, 4), (11, 4), (10, 5), (11, 5)]:
        put(px, x, y, COS_HI)
    put(px, 13, 6, COS_DK); put(px, 12, 8, COS_DK)
    put(px, 11, 3, SPARK); put(px, 13, 5, TRIM)
    save_item(img, "star_cleaver")


def blink_core():
    img = blank(); px = img.load()
    OUT_ = (60, 40, 120, 255); MID = (110, 90, 220, 255); HI = (180, 230, 255, 255); CORE = (235, 250, 255, 255)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d < 6.2:
                if d < 1.6:
                    px[x, y] = CORE
                elif d < 3.2:
                    px[x, y] = HI
                elif d < 5.0:
                    px[x, y] = MID
                else:
                    px[x, y] = OUT_
    # orbiting sparkles
    for (x, y) in [(3, 7), (12, 6), (8, 2), (9, 13)]:
        put(px, x, y, CORE)
    save_item(img, "blink_core")


def comms_beacon():
    img = blank(); px = img.load()
    BASE = (60, 66, 76, 255); BASE_HI = (110, 118, 130, 255); BASE_DK = (38, 42, 52, 255)
    SIG = (110, 235, 140, 255); SIG_HI = (210, 255, 210, 255)
    # base box
    rect(px, 5, 9, 10, 13, BASE)
    rect(px, 5, 9, 10, 9, BASE_HI)
    rect(px, 5, 13, 10, 13, BASE_DK)
    put(px, 6, 11, SIG); put(px, 7, 11, SIG_HI)  # status light
    # antenna
    rect(px, 7, 3, 8, 9, BASE)
    put(px, 7, 3, BASE_HI)
    # signal waves
    put(px, 9, 2, SIG); put(px, 10, 1, SIG); put(px, 5, 2, SIG); put(px, 4, 1, SIG)
    put(px, 8, 1, SIG_HI)
    save_item(img, "comms_beacon")


def main():
    swarm_mother()
    cosmic_armor_layers()
    cosmic_helmet(); cosmic_chestplate(); cosmic_leggings(); cosmic_boots()
    cosmic_warhammer()
    cosmic_pickaxe(); star_cleaver(); blink_core(); comms_beacon()
    print("done")


if __name__ == "__main__":
    main()
