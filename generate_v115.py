"""v1.15 economy content: platinum/palladium -> nibirium chain, alien skin/flesh,
and the day-4 infected gem ores.

Generates ALL the repetitive assets for the new items/blocks:
  * 16x16 item + block textures
  * item models (generated) and block-item models
  * block models (cube_all) + blockstates
  * block loot tables
  * crafting/blasting recipes

Java registration (ItemRegistry/ModBlocks), conversion logic, tags, lang and the
creative tab are wired by hand in the source - this only emits data/asset JSON+PNG.
"""
import json
import os
import random
from PIL import Image

NS = "alien-invasion"
ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, "src", "main", "resources", "assets", NS)
D = os.path.join(ROOT, "src", "main", "resources", "data", NS)
ITEM_TEX = os.path.join(A, "textures", "item")
BLOCK_TEX = os.path.join(A, "textures", "block")
ITEM_MODEL = os.path.join(A, "models", "item")
BLOCK_MODEL = os.path.join(A, "models", "block")
BLOCKSTATE = os.path.join(A, "blockstates")
LOOT = os.path.join(D, "loot_table", "blocks")
RECIPE = os.path.join(D, "recipe")
for d in (ITEM_TEX, BLOCK_TEX, ITEM_MODEL, BLOCK_MODEL, BLOCKSTATE, LOOT, RECIPE):
    os.makedirs(d, exist_ok=True)


def writejson(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


# ----------------------------------------------------------------- textures
def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def clamp(v):
    return max(0, min(255, int(v)))


def outline(img, col=(20, 18, 24, 255)):
    src = img.copy(); sp = src.load(); px = img.load()
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    px[x, y] = col
                    break


def save_item(name, img, do_outline=True):
    if do_outline:
        outline(img)
    img.save(os.path.join(ITEM_TEX, name + ".png"))


def save_block(name, img):
    img.save(os.path.join(BLOCK_TEX, name + ".png"))


def ingot(name, base, hi, dk):
    img = blank(); px = img.load()
    for y in range(6, 11):
        inset = (y - 6)
        for x in range(4 + inset // 2, 13 - inset // 2):
            px[x, y] = base
    for x in range(5, 11):
        px[x, 5] = hi
    for x in range(4, 12):
        px[x, 11] = dk
    px[6, 6] = hi; px[7, 6] = hi; px[9, 9] = hi
    save_item(name, img)


def chunk(name, base, hi):
    img = blank(); px = img.load()
    blobs = [(5, 8), (9, 6), (8, 10), (11, 9)]
    for (cx, cy) in blobs:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if abs(dx) + abs(dy) <= 1:
                    px[cx + dx, cy + dy] = base
        px[cx, cy] = hi
    save_item(name, img)


def raw_ore(name, rock, fleck, fleck_hi):
    img = blank(); px = img.load()
    random.seed(hash(name) & 0xffff)
    pts = {(x, y) for x in range(4, 12) for y in range(4, 12)
           if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 14}
    for (x, y) in pts:
        n = random.randint(-14, 14)
        px[x, y] = (clamp(rock[0] + n), clamp(rock[1] + n), clamp(rock[2] + n), 255)
    for _ in range(6):
        x = random.randint(4, 11); y = random.randint(4, 11)
        if (x, y) in pts:
            px[x, y] = fleck
            if (x + 1, y) in pts:
                px[x + 1, y] = fleck_hi
    save_item(name, img)


def alien_skin():
    img = blank(); px = img.load()
    random.seed(7)
    base = (118, 138, 92); dk = (78, 94, 58); sp = (152, 172, 116)
    for y in range(3, 14):
        for x in range(3, 13):
            if (x in (3, 12) and y in (3, 13)):
                continue
            n = random.randint(-12, 10)
            px[x, y] = (clamp(base[0] + n), clamp(base[1] + n), clamp(base[2] + n), 255)
    for _ in range(7):
        px[random.randint(4, 11), random.randint(4, 12)] = sp
    for y in range(4, 13, 2):  # stitch seam down the middle
        px[7, y] = dk
    save_item("alien_skin", img)


def stone_ore_block(name, stone, fleck, fleck_hi):
    img = blank(); px = img.load()
    random.seed(hash(name) & 0xffff)
    for y in range(16):
        for x in range(16):
            n = random.randint(-10, 10)
            px[x, y] = (clamp(stone[0] + n), clamp(stone[1] + n), clamp(stone[2] + n), 255)
    clusters = [(4, 4), (11, 5), (6, 11), (12, 12), (3, 12)]
    for (cx, cy) in clusters:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16 and abs(dx) + abs(dy) <= 1:
                    px[x, y] = fleck
        if 0 <= cx < 16 and 0 <= cy < 16:
            px[cx, cy] = fleck_hi
    save_block(name, img)


def flesh_block():
    img = blank(); px = img.load()
    random.seed(42)
    base = (150, 84, 86); dk = (112, 58, 62); vein = (96, 138, 80)
    for y in range(16):
        for x in range(16):
            n = random.randint(-12, 12)
            px[x, y] = (clamp(base[0] + n), clamp(base[1] + n), clamp(base[2] + n), 255)
    # Sinewy green veins.
    for _ in range(5):
        x = random.randint(1, 14); y = random.randint(1, 14)
        for _ in range(random.randint(4, 7)):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = vein
            x = max(0, min(15, x + random.randint(-1, 1)))
            y = max(0, min(15, y + random.randint(0, 1)))
    for _ in range(8):
        px[random.randint(0, 15), random.randint(0, 15)] = dk
    save_block("alien_flesh", img)


def gem_ore(name, gem, gem_hi):
    img = blank(); px = img.load()
    random.seed(hash(name) & 0xffff)
    stone = (66, 68, 74)  # deepslate-ish
    for y in range(16):
        for x in range(16):
            n = random.randint(-8, 8)
            px[x, y] = (clamp(stone[0] + n), clamp(stone[1] + n), clamp(stone[2] + n), 255)
    clusters = [(5, 5), (10, 6), (7, 11), (12, 11)]
    for (cx, cy) in clusters:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16 and abs(dx) + abs(dy) <= 1:
                    # gem tinted toward infected green
                    px[x, y] = (clamp((gem[0] + 70) / 2), clamp((gem[1] + 150) / 2), clamp((gem[2] + 70) / 2), 255)
        px[cx, cy] = gem_hi
    save_block(name, img)


# ----------------------------------------------------------------- models/state/loot/recipe
def item_model(name, parent_block=False):
    if parent_block:
        writejson(os.path.join(ITEM_MODEL, name + ".json"), {"parent": f"{NS}:block/{name}"})
    else:
        writejson(os.path.join(ITEM_MODEL, name + ".json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def block_assets(name):
    writejson(os.path.join(BLOCK_MODEL, name + ".json"),
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
    writejson(os.path.join(BLOCKSTATE, name + ".json"), {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    item_model(name, parent_block=True)


def loot_drop(block, item, mn=1, mx=1, ns=NS):
    entry = {"type": "minecraft:item", "name": f"{ns}:{item}"}
    if (mn, mx) != (1, 1):
        entry["functions"] = [{"function": "minecraft:set_count",
                               "count": {"type": "minecraft:uniform", "min": mn, "max": mx}, "add": False}]
    writejson(os.path.join(LOOT, block + ".json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}]
    })


def recipe_3x3(name, ingredient, result):
    writejson(os.path.join(RECIPE, name + ".json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["###", "###", "###"],
        "key": {"#": {"item": f"{NS}:{ingredient}"}},
        "result": {"id": f"{NS}:{result}", "count": 1}
    })


def recipe_blasting(name, ingredient, result):
    writejson(os.path.join(RECIPE, name + ".json"), {
        "type": "minecraft:blasting",
        "ingredient": {"item": f"{NS}:{ingredient}"},
        "result": {"id": f"{NS}:{result}"},
        "experience": 0.7, "cookingtime": 200
    })


def recipe_nibirium():
    writejson(os.path.join(RECIPE, "nibirium_ingot.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["PXP", "XPX", "PXP"],
        "key": {"P": {"item": f"{NS}:platinum_ingot"}, "X": {"item": f"{NS}:palladium_ingot"}},
        "result": {"id": f"{NS}:nibirium_ingot", "count": 2}
    })


def main():
    # --- item textures + models ---
    ingot("platinum_ingot", (200, 202, 210, 255), (240, 242, 250, 255), (122, 126, 138, 255))
    ingot("palladium_ingot", (200, 192, 168, 255), (232, 226, 202, 255), (130, 122, 100, 255))
    ingot("nibirium_ingot", (152, 142, 184, 255), (206, 196, 230, 255), (86, 78, 112, 255))
    chunk("platinum_chunk", (196, 200, 210, 255), (240, 242, 250, 255))
    chunk("palladium_chunk", (198, 190, 166, 255), (232, 226, 202, 255))
    raw_ore("raw_platinum", (112, 112, 120), (196, 200, 210, 255), (240, 242, 250, 255))
    raw_ore("raw_palladium", (110, 106, 94), (198, 190, 166, 255), (232, 226, 202, 255))
    alien_skin()
    for it in ("platinum_ingot", "palladium_ingot", "nibirium_ingot", "platinum_chunk",
               "palladium_chunk", "raw_platinum", "raw_palladium", "alien_skin"):
        item_model(it)

    # --- block textures + full block assets ---
    stone_ore_block("platinum_ore", (130, 130, 134), (214, 218, 228, 255), (244, 246, 252, 255))
    stone_ore_block("palladium_ore", (130, 130, 134), (210, 202, 176, 255), (236, 230, 206, 255))
    flesh_block()
    gem_ore("infested_diamond_ore", (120, 210, 210), (190, 248, 240, 255))
    gem_ore("infested_lapis_ore", (50, 80, 184), (120, 150, 240, 255))
    gem_ore("infested_redstone_ore", (180, 40, 40), (255, 110, 90, 255))
    for b in ("platinum_ore", "palladium_ore", "alien_flesh",
              "infested_diamond_ore", "infested_lapis_ore", "infested_redstone_ore"):
        block_assets(b)

    # --- loot tables ---
    loot_drop("platinum_ore", "platinum_chunk", 1, 3)
    loot_drop("palladium_ore", "palladium_chunk", 1, 3)
    loot_drop("alien_flesh", "alien_skin", 1, 2)
    loot_drop("infested_diamond_ore", "diamond", 1, 1, ns="minecraft")
    loot_drop("infested_lapis_ore", "lapis_lazuli", 4, 8, ns="minecraft")
    loot_drop("infested_redstone_ore", "redstone", 4, 5, ns="minecraft")

    # --- recipes ---
    recipe_3x3("raw_platinum", "platinum_chunk", "raw_platinum")
    recipe_3x3("raw_palladium", "palladium_chunk", "raw_palladium")
    recipe_blasting("platinum_ingot_from_blasting", "raw_platinum", "platinum_ingot")
    recipe_blasting("palladium_ingot_from_blasting", "raw_palladium", "palladium_ingot")
    recipe_nibirium()
    print("v1.15 assets generated.")


if __name__ == "__main__":
    main()
