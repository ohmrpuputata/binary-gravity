"""
High-quality 16x16 pixel-art icons for the alien-invasion mod's key items.

Hand-authored pixel art (shaded base/highlight/shadow + auto 1px outline) so the
gear reads clearly in-game instead of the old placeholder blobs. Also fixes the
three items whose models pointed at missing textures (alien_blaster,
invasion_tracker, gravity_boots) and draws the new cosmic_stimulant.

Run with the project venv:
    .venv/Scripts/python.exe generate_item_art.py
"""
from PIL import Image
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
ITEM_TEX = os.path.join(PROJECT, "src", "main", "resources", "assets",
                        "alien-invasion", "textures", "item")
os.makedirs(ITEM_TEX, exist_ok=True)

S = 16
OUTLINE = (16, 14, 24, 255)


def blank():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def put(px, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        px[x, y] = c


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)


def add_outline(img, color=OUTLINE):
    """Wrap every opaque cluster in a 1px dark outline on transparent neighbours."""
    src = img.copy()
    sp = src.load()
    px = img.load()
    for y in range(S):
        for x in range(S):
            if sp[x, y][3] != 0:
                continue
            touch = False
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1),
                           (1, 1), (1, -1), (-1, 1), (-1, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < S and 0 <= ny < S and sp[nx, ny][3] != 0:
                    touch = True
                    break
            if touch:
                px[x, y] = color


def save(img, name):
    add_outline(img)
    img.save(os.path.join(ITEM_TEX, name + ".png"))
    print("wrote", name + ".png")


# ---------------------------------------------------------------- bio_blade
def bio_blade():
    img = blank(); px = img.load()
    BLADE = (84, 230, 120, 255)
    BLADE_HI = (190, 255, 205, 255)
    BLADE_DK = (40, 150, 80, 255)
    GUARD = (60, 70, 80, 255)
    GUARD_HI = (110, 120, 130, 255)
    GRIP = (90, 200, 215, 255)
    GRIP_DK = (45, 120, 140, 255)
    # blade (upright, tip at top)
    put(px, 7, 1, BLADE_HI)
    for y in range(2, 10):
        put(px, 7, y, BLADE_HI if y < 4 else BLADE)
        put(px, 8, y, BLADE_DK)
    # vein detail
    put(px, 7, 5, BLADE); put(px, 7, 7, BLADE)
    # guard
    rect(px, 5, 10, 10, 10, GUARD)
    put(px, 6, 10, GUARD_HI); put(px, 9, 10, GUARD_HI)
    # grip
    for y in range(11, 14):
        put(px, 7, y, GRIP); put(px, 8, y, GRIP_DK)
    # pommel
    rect(px, 6, 14, 9, 14, GUARD)
    put(px, 7, 14, GUARD_HI)
    save(img, "bio_blade")


# ---------------------------------------------------------- cosmic_warhammer
def cosmic_warhammer():
    img = blank(); px = img.load()
    HEAD = (96, 70, 180, 255)
    HEAD_HI = (170, 130, 245, 255)
    HEAD_DK = (55, 35, 110, 255)
    SPARK = (140, 245, 255, 255)
    HANDLE = (120, 80, 55, 255)
    HANDLE_DK = (80, 50, 35, 255)
    # big head block
    rect(px, 4, 2, 11, 6, HEAD)
    rect(px, 4, 2, 11, 2, HEAD_DK)
    rect(px, 4, 6, 11, 6, HEAD_DK)
    rect(px, 5, 3, 6, 4, HEAD_HI)
    # star sparkles
    put(px, 9, 3, SPARK); put(px, 8, 5, SPARK); put(px, 10, 4, SPARK)
    # handle
    for y in range(7, 15):
        put(px, 7, y, HANDLE); put(px, 8, y, HANDLE_DK)
    save(img, "cosmic_warhammer")


# ------------------------------------------------------------- gravity_gun
def gravity_gun():
    img = blank(); px = img.load()
    BODY = (70, 78, 92, 255)
    BODY_HI = (120, 130, 145, 255)
    BODY_DK = (42, 48, 60, 255)
    GLOW = (90, 220, 255, 255)
    GLOW_HI = (200, 250, 255, 255)
    # main body
    rect(px, 3, 5, 11, 7, BODY)
    rect(px, 3, 5, 11, 5, BODY_HI)
    rect(px, 3, 7, 11, 7, BODY_DK)
    # grip
    rect(px, 4, 8, 5, 11, BODY)
    put(px, 4, 11, BODY_DK); put(px, 5, 11, BODY_DK)
    # trigger guard
    put(px, 6, 8, BODY_DK)
    # emitter claw (front, right)
    put(px, 12, 5, GLOW); put(px, 12, 7, GLOW)
    put(px, 13, 4, GLOW); put(px, 13, 8, GLOW)
    put(px, 12, 6, GLOW_HI)
    put(px, 13, 6, GLOW_HI)
    # core light
    put(px, 8, 6, GLOW_HI); put(px, 9, 6, GLOW)
    save(img, "gravity_gun")


# ------------------------------------------------------------ alien_blaster
def alien_blaster():
    img = blank(); px = img.load()
    BODY = (58, 95, 70, 255)
    BODY_HI = (110, 165, 120, 255)
    BODY_DK = (35, 60, 45, 255)
    PLASMA = (130, 255, 120, 255)
    PLASMA_HI = (215, 255, 200, 255)
    # barrel
    rect(px, 3, 6, 11, 8, BODY)
    rect(px, 3, 6, 11, 6, BODY_HI)
    rect(px, 3, 8, 11, 8, BODY_DK)
    # grip
    rect(px, 4, 9, 6, 12, BODY)
    put(px, 4, 12, BODY_DK); put(px, 6, 12, BODY_DK)
    # sight
    put(px, 6, 5, BODY_DK); put(px, 7, 5, BODY)
    # muzzle plasma
    put(px, 12, 7, PLASMA); put(px, 13, 7, PLASMA_HI)
    put(px, 12, 6, PLASMA); put(px, 12, 8, PLASMA)
    # energy cell window
    put(px, 8, 7, PLASMA_HI); put(px, 9, 7, PLASMA)
    save(img, "alien_blaster")


# -------------------------------------------------------- invasion_tracker
def invasion_tracker():
    img = blank(); px = img.load()
    CASE = (52, 58, 66, 255)
    CASE_HI = (95, 102, 112, 255)
    CASE_DK = (32, 36, 44, 255)
    SCREEN = (20, 60, 30, 255)
    BLIP = (90, 255, 110, 255)
    BLIP_HI = (200, 255, 200, 255)
    # rounded case
    rect(px, 5, 2, 10, 2, CASE)
    rect(px, 4, 3, 11, 12, CASE)
    rect(px, 5, 13, 10, 13, CASE)
    rect(px, 4, 3, 11, 3, CASE_HI)
    rect(px, 4, 12, 11, 12, CASE_DK)
    # screen
    rect(px, 5, 4, 10, 11, SCREEN)
    # radar sweep ring
    put(px, 7, 6, BLIP); put(px, 8, 6, BLIP)
    put(px, 6, 7, BLIP); put(px, 9, 7, BLIP)
    put(px, 6, 8, BLIP); put(px, 9, 8, BLIP)
    put(px, 7, 9, BLIP); put(px, 8, 9, BLIP)
    # center blip + needle
    put(px, 7, 7, BLIP_HI); put(px, 8, 8, BLIP_HI)
    put(px, 8, 5, BLIP_HI)
    save(img, "invasion_tracker")


# ----------------------------------------------------------- gravity_boots
def gravity_boots():
    img = blank(); px = img.load()
    BOOT = (88, 96, 150, 255)
    BOOT_HI = (140, 150, 215, 255)
    BOOT_DK = (52, 58, 100, 255)
    SOLE = (40, 42, 55, 255)
    THRUST = (110, 225, 255, 255)
    # shaft
    rect(px, 5, 2, 8, 9, BOOT)
    rect(px, 5, 2, 5, 9, BOOT_HI)
    rect(px, 8, 2, 8, 9, BOOT_DK)
    # cuff
    rect(px, 4, 2, 9, 3, BOOT_HI)
    # foot
    rect(px, 5, 10, 12, 11, BOOT)
    rect(px, 5, 10, 12, 10, BOOT_HI)
    # sole
    rect(px, 5, 12, 12, 12, SOLE)
    # anti-grav thrusters
    put(px, 6, 13, THRUST); put(px, 9, 13, THRUST); put(px, 11, 13, THRUST)
    save(img, "gravity_boots")


# -------------------------------------------------------- cosmic_stimulant
def cosmic_stimulant():
    img = blank(); px = img.load()
    GLASS = (200, 215, 230, 255)
    GLASS_DK = (140, 155, 175, 255)
    LIQUID = (90, 230, 220, 255)
    LIQUID_HI = (200, 255, 250, 255)
    METAL = (120, 128, 140, 255)
    NEEDLE = (180, 188, 200, 255)
    # needle
    put(px, 7, 1, NEEDLE); put(px, 7, 2, NEEDLE); put(px, 7, 3, NEEDLE)
    # barrel (glass)
    rect(px, 6, 4, 9, 10, GLASS)
    rect(px, 9, 4, 9, 10, GLASS_DK)
    # liquid
    rect(px, 6, 6, 8, 10, LIQUID)
    put(px, 7, 7, LIQUID_HI); put(px, 6, 9, LIQUID_HI)
    # plunger flange
    rect(px, 5, 11, 10, 11, METAL)
    # plunger rod + thumb rest
    rect(px, 7, 12, 8, 13, METAL)
    rect(px, 6, 14, 9, 14, METAL)
    save(img, "cosmic_stimulant")


def main():
    bio_blade()
    cosmic_warhammer()
    gravity_gun()
    alien_blaster()
    invasion_tracker()
    gravity_boots()
    cosmic_stimulant()
    print("done ->", ITEM_TEX)


if __name__ == "__main__":
    main()
