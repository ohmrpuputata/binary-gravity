"""
Generate 16x16 item icons for the new alien (bio) tools + purifier wand.
Standalone (no external resource pack needed) -- run with the project venv:
    .venv/Scripts/python.exe generate_tool_textures.py
"""
from PIL import Image
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
ITEM_TEX = os.path.join(PROJECT, "src", "main", "resources", "assets",
                        "alien-invasion", "textures", "item")
os.makedirs(ITEM_TEX, exist_ok=True)

# Palette ------------------------------------------------------------------
HEAD = (60, 200, 90, 255)        # alien-alloy green head
HEAD_HI = (180, 255, 200, 255)   # bright highlight
HEAD_DK = (28, 110, 55, 255)     # shadow
HANDLE = (95, 205, 220, 255)     # diamond-cyan handle
HANDLE_DK = (45, 130, 150, 255)  # handle shadow
GLOW = (130, 255, 230, 255)      # purifier crystal glow


def new_img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def save(img, name):
    img.save(os.path.join(ITEM_TEX, name + ".png"))
    print("wrote", name + ".png")


def put(px, pts, color):
    for (x, y) in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = color


def handle_diag(px):
    """Diamond-cyan handle running bottom-left -> top-right."""
    main = [(3 + i, 13 - i) for i in range(8)]          # (3,13)..(10,6)
    shadow = [(3 + i, 14 - i) for i in range(8)]        # one px lower
    put(px, shadow, HANDLE_DK)
    put(px, main, HANDLE)


def make_pickaxe():
    img = new_img(); px = img.load()
    handle_diag(px)
    # arched pick head across the top
    arc = [(6, 6), (7, 5), (8, 4), (9, 3), (10, 3), (11, 3), (12, 4), (13, 5), (14, 6)]
    put(px, arc, HEAD)
    put(px, [(9, 4), (10, 4), (11, 4)], HEAD_HI)
    put(px, [(6, 7), (14, 7)], HEAD_DK)
    save(img, "bio_pickaxe")


def make_axe():
    img = new_img(); px = img.load()
    handle_diag(px)
    # solid wedge head on the upper-right
    blob = []
    for y in range(3, 9):
        for x in range(9, 14):
            if (x - 9) + (y - 3) <= 6 and x - 9 >= (y - 6):
                blob.append((x, y))
    put(px, blob, HEAD)
    put(px, [(10, 4), (11, 4), (10, 5)], HEAD_HI)
    put(px, [(13, 7), (12, 8)], HEAD_DK)
    save(img, "bio_axe")


def make_shovel():
    img = new_img(); px = img.load()
    handle_diag(px)
    # rounded spade head at the top-right
    spade = []
    for y in range(2, 7):
        for x in range(9, 14):
            spade.append((x, y))
    put(px, spade, HEAD)
    put(px, [(10, 3), (11, 3), (10, 4)], HEAD_HI)
    put(px, [(9, 6), (13, 6), (13, 2)], HEAD_DK)
    save(img, "bio_shovel")


def make_hoe():
    img = new_img(); px = img.load()
    handle_diag(px)
    # L-shaped hoe head at the top
    head = [(9, 3), (10, 3), (11, 3), (12, 3), (13, 3), (13, 4), (13, 5)]
    put(px, head, HEAD)
    put(px, [(10, 3), (11, 3)], HEAD_HI)
    put(px, [(9, 4), (13, 6)], HEAD_DK)
    save(img, "bio_hoe")


def make_purifier_wand():
    img = new_img(); px = img.load()
    # vertical shaft
    for y in range(5, 14):
        put(px, [(7, y)], HANDLE)
        put(px, [(8, y)], HANDLE_DK)
    # crystal emitter at the top
    crystal = [(7, 2), (6, 3), (7, 3), (8, 3), (5, 4), (6, 4), (7, 4),
               (8, 4), (9, 4), (6, 5), (7, 5), (8, 5)]
    put(px, crystal, HEAD)
    put(px, [(7, 3), (7, 4)], GLOW)
    put(px, [(5, 4), (9, 4), (7, 2)], HEAD_HI)
    save(img, "purifier_wand")


def generate_all():
    make_pickaxe()
    make_axe()
    make_shovel()
    make_hoe()
    make_purifier_wand()
    print("done ->", ITEM_TEX)


if __name__ == "__main__":
    generate_all()
