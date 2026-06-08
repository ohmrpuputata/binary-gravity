"""Organic 'bio' tool sprites (bio_blade + bio pickaxe/axe/shovel/hoe).

These are grown-not-forged alien tools: a dark chitin/bone handle and a glowing
bio-green head with a darker spine, glow nodes and a 1px outline. Overwrites the
old plain-green sprites. Run LAST so it wins over the older generators.
"""
import os
from PIL import Image

ITEM = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "src", "main", "resources", "assets", "alien-invasion", "textures", "item")
os.makedirs(ITEM, exist_ok=True)

BONE = (74, 82, 52, 255)        # chitin handle
BONE_DK = (50, 56, 34, 255)
BONE_HI = (110, 120, 78, 255)
FLESH = (116, 196, 92, 255)     # bio blade
FLESH_HI = (182, 240, 142, 255)
FLESH_DK = (60, 116, 54, 255)
VEIN = (42, 82, 40, 255)
GLOW = (214, 255, 176, 255)
OUTLINE = (20, 30, 18, 255)


def draw(tool):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def p(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c

    def vline(x, y0, y1, c):
        for y in range(y0, y1 + 1):
            p(x, y, c)

    def hline(y, x0, x1, c):
        for x in range(x0, x1 + 1):
            p(x, y, c)

    if tool == "blade":
        vline(7, 12, 15, BONE); vline(8, 12, 15, BONE_DK); p(7, 15, BONE_HI)  # grip
        hline(11, 5, 10, BONE)                                                # organic guard
        p(5, 11, GLOW); p(10, 11, GLOW)
        for y in range(2, 11):                                                # living blade
            p(7, y, FLESH_HI); p(8, y, FLESH)
        p(8, 10, FLESH_DK)
        for y in (4, 7, 9):                                                   # vein nodes
            p(8, y, VEIN)
        p(7, 3, GLOW)
    elif tool == "pickaxe":
        vline(8, 5, 15, BONE); vline(9, 5, 15, BONE_DK)
        hline(3, 4, 11, FLESH); hline(2, 5, 10, FLESH_HI)
        p(3, 4, FLESH); p(12, 4, FLESH); p(3, 5, FLESH_DK); p(12, 5, FLESH_DK)
        p(6, 3, VEIN); p(9, 3, GLOW)
    elif tool == "axe":
        vline(8, 4, 15, BONE); vline(9, 4, 15, BONE_DK)
        for y in range(2, 8):
            hline(y, 4, 8, FLESH)
        vline(3, 3, 6, FLESH_HI)
        hline(2, 4, 8, FLESH_HI)
        for y in range(3, 8):
            p(8, y, FLESH_DK)
        p(5, 5, VEIN); p(6, 3, GLOW)
    elif tool == "shovel":
        vline(7, 6, 15, BONE); vline(8, 6, 15, BONE_DK)
        for y in range(2, 6):
            hline(y, 6, 9, FLESH)
        hline(2, 6, 9, FLESH_HI); p(6, 5, FLESH_DK); p(9, 5, FLESH_DK)
        p(7, 4, VEIN); p(8, 3, GLOW)
    else:  # hoe
        vline(8, 4, 15, BONE); vline(9, 4, 15, BONE_DK)
        hline(2, 4, 9, FLESH); hline(2, 4, 9, FLESH_HI)
        vline(4, 3, 4, FLESH_DK)
        p(6, 2, GLOW)

    # 1px outline.
    src = img.copy(); sp = src.load()
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    px[x, y] = OUTLINE
                    break
    return img


def main():
    for name, tool in [("bio_blade", "blade"), ("bio_pickaxe", "pickaxe"),
                       ("bio_axe", "axe"), ("bio_shovel", "shovel"), ("bio_hoe", "hoe")]:
        draw(tool).save(os.path.join(ITEM, name + ".png"))
        print("bio tool", name)
    print("done")


if __name__ == "__main__":
    main()
