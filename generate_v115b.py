"""v1.15b: Nibirium tool textures + item models + recipes for the new gear.

  * Nibirium tool sprites (sword/pickaxe/axe/shovel/hoe), steel-violet.
  * Item models: light-hazmat icons (generated) + nibirium tools (handheld).
  * Recipes: light hazmat from alien skin; upgrade light->full hazmat with Nibirium;
    nibirium tools; gravity boots (2 alien battery + 2 nibirium).
"""
import json
import os
from PIL import Image

NS = "alien-invasion"
ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, "src", "main", "resources", "assets", NS)
D = os.path.join(ROOT, "src", "main", "resources", "data", NS)
ITEM_TEX = os.path.join(A, "textures", "item")
ITEM_MODEL = os.path.join(A, "models", "item")
RECIPE = os.path.join(D, "recipe")
for d in (ITEM_TEX, ITEM_MODEL, RECIPE):
    os.makedirs(d, exist_ok=True)


def wj(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


# ---- nibirium tool sprites (steel-violet, shaded, outlined) ----
HANDLE = (110, 78, 48, 255)
HANDLE_DK = (74, 50, 30, 255)
HEAD = (152, 142, 184, 255)
HEAD_HI = (206, 196, 230, 255)
HEAD_DK = (86, 78, 112, 255)


def tool(name, kind):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def p(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c

    def v(x, y0, y1, c):
        [p(x, y, c) for y in range(y0, y1 + 1)]

    def h(y, x0, x1, c):
        [p(x, y, c) for x in range(x0, x1 + 1)]

    if kind == "sword":
        v(7, 12, 15, HANDLE); v(8, 12, 15, HANDLE_DK)
        h(11, 5, 10, HEAD_DK)
        for y in range(2, 11):
            p(7, y, HEAD_HI); p(8, y, HEAD)
        p(8, 10, HEAD_DK)
    elif kind == "pickaxe":
        v(8, 5, 15, HANDLE); v(9, 5, 15, HANDLE_DK)
        h(3, 4, 11, HEAD); h(2, 5, 10, HEAD_HI)
        p(3, 4, HEAD); p(12, 4, HEAD); p(3, 5, HEAD_DK); p(12, 5, HEAD_DK)
    elif kind == "axe":
        v(8, 4, 15, HANDLE); v(9, 4, 15, HANDLE_DK)
        for y in range(2, 8):
            h(y, 4, 8, HEAD)
        v(3, 3, 6, HEAD_HI); h(2, 4, 8, HEAD_HI)
        for y in range(3, 8):
            p(8, y, HEAD_DK)
    elif kind == "shovel":
        v(7, 6, 15, HANDLE); v(8, 6, 15, HANDLE_DK)
        for y in range(2, 6):
            h(y, 6, 9, HEAD)
        h(2, 6, 9, HEAD_HI); p(6, 5, HEAD_DK); p(9, 5, HEAD_DK)
    else:  # hoe
        v(8, 4, 15, HANDLE); v(9, 4, 15, HANDLE_DK)
        h(2, 4, 9, HEAD); h(2, 4, 9, HEAD_HI)
        v(4, 3, 4, HEAD_DK)
    # outline
    src = img.copy(); sp = src.load()
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    px[x, y] = (20, 18, 24, 255)
                    break
    img.save(os.path.join(ITEM_TEX, name + ".png"))


def item_model(name, handheld=False):
    wj(os.path.join(ITEM_MODEL, name + ".json"), {
        "parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/{name}"}
    })


def shaped(name, pattern, key, result, count=1):
    wj(os.path.join(RECIPE, name + ".json"), {
        "type": "minecraft:crafting_shaped", "pattern": pattern,
        "key": {k: {"item": v} for k, v in key.items()},
        "result": {"id": f"{NS}:{result}", "count": count}
    })


def shapeless(name, items, result, count=1):
    wj(os.path.join(RECIPE, name + ".json"), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": i} for i in items],
        "result": {"id": f"{NS}:{result}", "count": count}
    })


def main():
    # nibirium tools
    for nm, kind in [("nibirium_sword", "sword"), ("nibirium_pickaxe", "pickaxe"), ("nibirium_axe", "axe"),
                     ("nibirium_shovel", "shovel"), ("nibirium_hoe", "hoe")]:
        tool(nm, kind)
        item_model(nm, handheld=True)
    # light hazmat icons (textures come from generate_suits_art.py)
    for nm in ("light_hazmat_helmet", "light_hazmat_chestplate", "light_hazmat_leggings", "light_hazmat_boots"):
        item_model(nm)

    SK = f"{NS}:alien_skin"
    NB = f"{NS}:nibirium_ingot"
    PL = f"{NS}:platinum_ingot"
    STICK = "minecraft:stick"
    # light hazmat from alien skin
    shaped("light_hazmat_helmet", ["SSS", "S S"], {"S": SK}, "light_hazmat_helmet")
    shaped("light_hazmat_chestplate", ["S S", "SSS", "SSS"], {"S": SK}, "light_hazmat_chestplate")
    shaped("light_hazmat_leggings", ["SSS", "S S", "S S"], {"S": SK}, "light_hazmat_leggings")
    shaped("light_hazmat_boots", ["S S", "S S"], {"S": SK}, "light_hazmat_boots")
    # upgrade light -> full hazmat with nibirium
    shapeless("hazmat_helmet_upgrade", [f"{NS}:light_hazmat_helmet", NB, NB], "hazmat_helmet")
    shapeless("hazmat_chestplate_upgrade", [f"{NS}:light_hazmat_chestplate", NB, NB, NB], "hazmat_chestplate")
    shapeless("hazmat_leggings_upgrade", [f"{NS}:light_hazmat_leggings", NB, NB], "hazmat_leggings")
    shapeless("hazmat_boots_upgrade", [f"{NS}:light_hazmat_boots", NB], "hazmat_boots")
    # nibirium tools
    shaped("nibirium_sword", ["N", "N", "I"], {"N": NB, "I": STICK}, "nibirium_sword")
    shaped("nibirium_pickaxe", ["NNN", " I ", " I "], {"N": NB, "I": STICK}, "nibirium_pickaxe")
    shaped("nibirium_axe", ["NN", "NI", " I"], {"N": NB, "I": STICK}, "nibirium_axe")
    shaped("nibirium_shovel", ["N", "I", "I"], {"N": NB, "I": STICK}, "nibirium_shovel")
    shaped("nibirium_hoe", ["NN", " I", " I"], {"N": NB, "I": STICK}, "nibirium_hoe")
    # gravity boots: 2 alien battery + 2 nibirium
    shaped("gravity_boots", ["B B", "N N"], {"B": f"{NS}:alien_battery", "N": NB}, "gravity_boots")
    print("v1.15b tool art + models + recipes generated.")


if __name__ == "__main__":
    main()
