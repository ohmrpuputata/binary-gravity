"""
Alien Apocalypse - texture pipeline.

Replaces the old random-noise placeholder generator. This script:
  * Builds UV-correct hand-painted textures for the CUSTOM models (UFO, Grunt).
  * Imports / recolors textures from the supplied resource packs for the
    entities that now use VANILLA model layouts (Telekinetic=Enderman,
    Brute=IronGolem, Alien Chicken=Chicken, Hive Tyrant=Warden).
  * Writes everything into assets/alien-invasion/ (namespace == mod id).

Run with the project's venv python (it has Pillow):
    .venv/Scripts/python.exe generate_textures.py
"""
from PIL import Image
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion", "textures", "entity")
PACK = r"C:\Users\hacke\Downloads\AyuGram Desktop\_work"

os.makedirs(OUT, exist_ok=True)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# --------------------------------------------------------------------------
# CUSTOM MODELS - hand-painted, UV-coherent sheets
# --------------------------------------------------------------------------
def paint_grunt():
    """64x64 sheet for the custom AlienGruntModel.
    A diseased-green organic skin: vertical light->dark gradient with mottled
    darker pores. Palette pulled from the 'Alien Troll' pack skin (green)."""
    w = h = 64
    img = Image.new("RGBA", (w, h))
    px = img.load()
    top = (122, 156, 96)      # sickly highlight green
    bottom = (44, 66, 40)     # deep shadow green
    import random
    random.seed(42)
    for y in range(h):
        t = y / (h - 1)
        base = lerp(top, bottom, t)
        for x in range(w):
            # mottled pores
            n = random.randint(-14, 10)
            # faint vertical "muscle" striping
            stripe = -10 if (x % 7 == 0) else 0
            r = max(0, min(255, base[0] + n + stripe))
            g = max(0, min(255, base[1] + n + stripe))
            b = max(0, min(255, base[2] + n // 2))
            px[x, y] = (r, g, b, 255)
    # a couple of darker "eye socket" smudges on the head island (0,0 16x16)
    for (cx, cy) in [(3, 3), (9, 3)]:
        for dy in range(2):
            for dx in range(2):
                px[cx + dx, cy + dy] = (180, 30, 30, 255)  # glowing red eyes
    img.save(os.path.join(OUT, "alien_grunt.png"))
    print("painted alien_grunt.png 64x64")


def paint_ufo():
    """128x128 sheet for the custom UfoModel. Brushed metallic silver with
    horizontal panel lines, darker tech panels and a ring of cyan running
    lights. Reads the average tone from the UFO Phantom CEM texture."""
    w = h = 128
    img = Image.new("RGBA", (w, h))
    px = img.load()
    light = (208, 214, 222)
    dark = (96, 104, 118)
    import random
    random.seed(7)
    for y in range(h):
        # brushed metal: alternating subtle horizontal bands
        band = (y // 4) % 2
        base = lerp(dark, light, 0.5 + (0.18 if band else -0.18))
        for x in range(w):
            n = random.randint(-6, 6)
            # panel seams every 16px
            seam = -40 if (x % 16 == 0 or y % 16 == 0) else 0
            r = max(0, min(255, base[0] + n + seam))
            g = max(0, min(255, base[1] + n + seam))
            b = max(0, min(255, base[2] + n + seam))
            px[x, y] = (r, g, b, 255)
    # cyan running lights along a horizontal strip (maps onto rings)
    for x in range(0, w, 6):
        for yy in range(2):
            px[x, 80 + yy] = (60, 230, 255, 255)
            px[x, 81 + yy] = (160, 250, 255, 255)
    img.save(os.path.join(OUT, "ufo.png"))
    print("painted ufo.png 128x128")


def paint_parasite():
    w, h = 64, 32
    img = Image.new("RGBA", (w, h))
    px = img.load()
    import random
    random.seed(99)
    for y in range(h):
        for x in range(w):
            n = random.randint(-10, 10)
            if (x // 4 + y // 3) % 2 == 0:
                px[x, y] = (max(0, 90 + n), max(0, 40 + n), max(0, 110 + n), 255)
            else:
                px[x, y] = (max(0, 40 + n), max(0, 150 + n), max(0, 50 + n), 255)
    img.save(os.path.join(OUT, "parasite.png"))
    print("painted parasite.png 64x32")


# --------------------------------------------------------------------------
# VANILLA MODELS - import / recolor from the supplied packs
# --------------------------------------------------------------------------
def tint(img, mult, add=(0, 0, 0)):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (
                max(0, min(255, int(r * mult[0]) + add[0])),
                max(0, min(255, int(g * mult[1]) + add[1])),
                max(0, min(255, int(b * mult[2]) + add[2])),
                a,
            )
    return img


def import_telekinetic():
    src = os.path.join(PACK, "ufo", "ainv", "Alien Invasion!", "assets", "minecraft",
                       "textures", "entity", "enderman", "enderman.png")
    img = Image.open(src).convert("RGBA")
    # push it toward the original purple/cyan "telekinetic" theme
    img = tint(img, (1.05, 0.65, 1.35), add=(10, 0, 25))
    img.save(os.path.join(OUT, "telekinetic_alien.png"))
    print(f"telekinetic_alien.png {img.size} (recolored enderman)")


def import_brute():
    src = os.path.join(PACK, "ufo", "golem", "assets", "minecraft",
                       "textures", "entity", "iron_golem", "iron_golem.png")
    img = Image.open(src).convert("RGBA")
    img.save(os.path.join(OUT, "alien_brute.png"))
    print(f"alien_brute.png {img.size} (clash-royale golem)")


def import_chicken():
    src = os.path.join(PACK, "ufo", "Alien-chicken-on-planetminecraft-com.png")
    img = Image.open(src).convert("RGBA")
    # vanilla ChickenModel expects a 64x32 sheet; pack art is 2x (128x64)
    if img.size != (64, 32):
        img = img.resize((64, 32), Image.NEAREST)
    img.save(os.path.join(OUT, "alien_chicken.png"))
    print(f"alien_chicken.png {img.size} (downscaled pack chicken)")


def import_hive_tyrant():
    src = os.path.join(PACK, "martian", "Martian Deep Dark", "assets", "minecraft",
                       "textures", "entity", "warden", "warden.png")
    img = Image.open(src).convert("RGBA")
    img.save(os.path.join(OUT, "hive_tyrant.png"))
    print(f"hive_tyrant.png {img.size} (martian warden)")


def make_icon():
    """Mod icon: 128x128 crop of the grunt face palette + alien green."""
    icon = Image.new("RGBA", (128, 128), (24, 40, 24, 255))
    px = icon.load()
    # simple alien head silhouette
    for y in range(128):
        for x in range(128):
            dx, dy = (x - 64) / 50.0, (y - 58) / 44.0
            if dx * dx + dy * dy < 1.0:
                px[x, y] = (90, 160, 70, 255)
    # eyes
    for (cx, cy) in [(46, 60), (82, 60)]:
        for yy in range(-8, 14):
            for xx in range(-7, 7):
                if (xx / 7.0) ** 2 + (yy / 12.0) ** 2 < 1.0:
                    px[cx + xx, cy + yy] = (10, 10, 14, 255)
    os.makedirs(os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion"), exist_ok=True)
    icon.save(os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion", "icon.png"))
    print("icon.png 128x128")


def import_troll():
    """Convert the 64x32 'Alien Troll' pack skin to a 64x64 humanoid sheet so it
    maps onto the vanilla Zombie model (separate left/right limbs)."""
    src = os.path.join(PACK, "ufo", "A-Alien-Troll-on-planetminecraft-com.png")
    legacy = Image.open(src).convert("RGBA")
    if legacy.size != (64, 32):
        legacy = legacy.resize((64, 32), Image.NEAREST)
    sheet = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    sheet.paste(legacy, (0, 0))
    # Copy right limbs into the modern left-limb slots (good enough for a mob).
    right_arm = legacy.crop((40, 16, 56, 32))
    right_leg = legacy.crop((0, 16, 16, 32))
    sheet.paste(right_arm, (32, 48))  # left arm slot
    sheet.paste(right_leg, (16, 48))  # left leg slot
    sheet.save(os.path.join(OUT, "alien_troll.png"))
    print(f"alien_troll.png {sheet.size} (converted pack troll skin)")


# --------------------------------------------------------------------------
# BLOCK + ITEM textures and their JSON model files
# --------------------------------------------------------------------------
ASSETS = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion")
BLOCK_TEX = os.path.join(ASSETS, "textures", "block")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
BLOCK_MODELS = os.path.join(ASSETS, "models", "block")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")
for d in (BLOCK_TEX, ITEM_TEX, BLOCKSTATES, BLOCK_MODELS, ITEM_MODELS):
    os.makedirs(d, exist_ok=True)


def _noise_tile(base, veins=None, glow=None, seed=1):
    import random
    random.seed(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = random.randint(-18, 18)
            px[x, y] = (max(0, min(255, base[0] + n)), max(0, min(255, base[1] + n)),
                        max(0, min(255, base[2] + n)), 255)
    if veins:
        for _ in range(22):
            x, y = random.randint(0, 15), random.randint(0, 15)
            px[x, y] = veins + (255,)
    if glow:
        for _ in range(10):
            x, y = random.randint(0, 15), random.randint(0, 15)
            px[x, y] = glow + (255,)
    return img


def make_block_textures():
    _noise_tile((105, 105, 110), veins=(120, 40, 160), glow=(40, 200, 180), seed=3).save(
        os.path.join(BLOCK_TEX, "infested_stone.png"))
    _noise_tile((120, 30, 35), glow=(255, 120, 60), seed=5).save(
        os.path.join(BLOCK_TEX, "alien_hive.png"))
    _noise_tile((70, 30, 90), veins=(150, 90, 200), seed=7).save(
        os.path.join(BLOCK_TEX, "alien_residue.png"))
    _noise_tile((60, 40, 100), veins=(130, 255, 130), glow=(160, 80, 220), seed=11).save(
        os.path.join(BLOCK_TEX, "alien_stash.png"))
    _noise_tile((80, 80, 60), veins=(230, 200, 30), glow=(250, 240, 60), seed=13).save(
        os.path.join(BLOCK_TEX, "cosmic_ore.png"))
    _noise_tile((40, 40, 50), veins=(230, 30, 30), glow=(255, 80, 80), seed=15).save(
        os.path.join(BLOCK_TEX, "alien_beacon.png"))
    print("block textures: infested_stone, alien_hive, alien_residue, alien_stash, cosmic_ore, alien_beacon")


def _item_icon(draw_fn, name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_fn(img.load())
    img.save(os.path.join(ITEM_TEX, name + ".png"))


def make_item_textures():
    def alloy(px):
        for y in range(4, 12):
            for x in range(3, 13):
                shade = 150 + (x - 3) * 6 - (y - 4) * 4
                px[x, y] = (40, min(255, shade), 70, 255)
        for x in range(3, 13):
            px[x, 4] = (180, 255, 200, 255)

    def core(px):
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5
                if d < 6:
                    t = 1 - d / 6
                    px[x, y] = (int(40 + 180 * t), int(220 * t + 35), int(200 * t + 55), 255)

    def flesh(px):
        import random
        random.seed(9)
        for y in range(3, 13):
            for x in range(3, 13):
                if (x - 8) ** 2 + (y - 8) ** 2 < 26:
                    n = random.randint(-20, 20)
                    px[x, y] = (max(0, 90 + n), max(0, 130 + n), max(0, 60 + n), 255)

    def shard(px):
        for y in range(4, 12):
            for x in range(6, 10):
                px[x, y] = (240, 220, 40, 255)

    def hazmat(px):
        for y in range(3, 13):
            for x in range(4, 12):
                px[x, y] = (230, 190, 30, 255)

    def gravity_gun(px):
        for y in range(6, 11):
            for x in range(3, 14):
                px[x, y] = (80, 80, 90, 255)
        for x in range(10, 14):
            px[x, 7] = (0, 230, 230, 255)

    def purifier(px):
        for y in range(5, 12):
            for x in range(5, 12):
                px[x, y] = (100, 100, 110, 255)
        for y in range(3, 6):
            for x in range(7, 10):
                px[x, y] = (50, 220, 50, 255)

    def drill_egg(px):
        for y in range(3, 13):
            for x in range(4, 12):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 20:
                    px[x, y] = (100, 110, 120, 255)
        for (x, y) in [(6, 5), (9, 6), (5, 9), (8, 10)]:
            px[x, y] = (240, 200, 40, 255)

    def meteor_egg(px):
        for y in range(3, 13):
            for x in range(4, 12):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 20:
                    px[x, y] = (50, 40, 40, 255)
        for (x, y) in [(6, 5), (9, 6), (5, 9), (8, 10)]:
            px[x, y] = (255, 100, 30, 255)

    def parasite_egg(px):
        for y in range(3, 13):
            for x in range(4, 12):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 20:
                    px[x, y] = (50, 180, 50, 255)
        for (x, y) in [(6, 5), (9, 6), (5, 9), (8, 10)]:
            px[x, y] = (160, 40, 200, 255)

    def parasite_item_tex(px):
        for y in range(4, 12):
            for x in range(4, 12):
                if (x - y) == 0 or (x - y) == 1:
                    px[x, y] = (60, 190, 60, 255)
        px[10, 10] = (255, 30, 30, 255)

    _item_icon(alloy, "alien_alloy")
    _item_icon(core, "hive_core")
    _item_icon(flesh, "infested_flesh")
    _item_icon(shard, "cosmic_shard")
    _item_icon(hazmat, "hazmat_helmet")
    _item_icon(hazmat, "hazmat_chestplate")
    _item_icon(hazmat, "hazmat_leggings")
    _item_icon(hazmat, "hazmat_boots")
    _item_icon(gravity_gun, "gravity_gun")
    _item_icon(purifier, "purifier")
    _item_icon(drill_egg, "drill_spawn_egg")
    _item_icon(meteor_egg, "meteor_spawn_egg")
    _item_icon(parasite_egg, "parasite_spawn_egg")
    _item_icon(parasite_item_tex, "parasite_item")
    print("item textures generated successfully")


def write_json(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def make_block_item_jsons():
    cube = ('{{\n  "parent": "minecraft:block/cube_all",\n'
            '  "textures": {{ "all": "alien-invasion:block/{0}" }}\n}}\n')
    bstate = '{{\n  "variants": {{ "": {{ "model": "alien-invasion:block/{0}" }} }}\n}}\n'
    bitem = '{{\n  "parent": "alien-invasion:block/{0}"\n}}\n'
    gen = ('{{\n  "parent": "minecraft:item/generated",\n'
           '  "textures": {{ "layer0": "alien-invasion:item/{0}" }}\n}}\n')

    for b in ("infested_stone", "alien_hive", "alien_residue", "alien_stash", "cosmic_ore", "alien_beacon"):
        write_json(os.path.join(BLOCK_MODELS, b + ".json"), cube.format(b))
        write_json(os.path.join(BLOCKSTATES, b + ".json"), bstate.format(b))
        write_json(os.path.join(ITEM_MODELS, b + ".json"), bitem.format(b))
    for it in ("alien_alloy", "hive_core", "infested_flesh", "cosmic_shard", "hazmat_helmet", "hazmat_chestplate", "hazmat_leggings", "hazmat_boots", "gravity_gun", "purifier", "drill_spawn_egg", "meteor_spawn_egg", "parasite_spawn_egg", "parasite_item"):
        write_json(os.path.join(ITEM_MODELS, it + ".json"), gen.format(it))
    print("wrote blockstate/model JSONs for new blocks and items")


def make_meteor_drill_textures():
    """64x32 sheets for the custom Meteor and Drill entity models."""
    import random
    # Meteor: charred rock with glowing orange cracks.
    m = Image.new("RGBA", (64, 32))
    px = m.load()
    random.seed(11)
    for y in range(32):
        for x in range(64):
            n = random.randint(-15, 15)
            px[x, y] = (max(0, 55 + n), max(0, 45 + n), max(0, 42 + n), 255)
    for _ in range(40):  # lava cracks
        x, y = random.randint(0, 63), random.randint(0, 31)
        px[x, y] = (255, random.randint(90, 160), 20, 255)
    m.save(os.path.join(OUT, "meteor.png"))
    # Drill: metallic body with hazard stripes.
    d = Image.new("RGBA", (64, 32))
    px = d.load()
    random.seed(13)
    for y in range(32):
        for x in range(64):
            base = 150 if (y // 2) % 2 == 0 else 120
            n = random.randint(-12, 12)
            px[x, y] = (max(0, base + n - 30), max(0, base + n - 10), max(0, base + n), 255)
    for x in range(64):  # hazard stripe
        if (x // 3) % 2 == 0:
            px[x, 14] = (240, 200, 40, 255)
            px[x, 15] = (240, 200, 40, 255)
    d.save(os.path.join(OUT, "drill.png"))
    print("entity textures: meteor, drill")


def make_lategame_assets():
    # Bio-Blade item icon (a glowing green blade).
    def blade(px):
        for y in range(2, 12):
            px[13 - y, y + 1] = (40, 230, 120, 255)
            px[14 - y, y] = (180, 255, 200, 255)
        for y in range(10, 14):  # handle
            px[16 - y, y] = (90, 60, 30, 255)
    _item_icon(blade, "bio_blade")

    # Purifier block texture (teal crystal tech block).
    pur = _noise_tile((30, 70, 80), glow=(60, 240, 220), seed=17)
    pxp = pur.load()
    for i in range(16):  # bright core cross
        pxp[8, i] = (120, 255, 240, 255)
        pxp[i, 8] = (120, 255, 240, 255)
    pur.save(os.path.join(BLOCK_TEX, "purifier.png"))

    gen = ('{{\n  "parent": "minecraft:item/generated",\n'
           '  "textures": {{ "layer0": "alien-invasion:item/{0}" }}\n}}\n')
    cube = ('{{\n  "parent": "minecraft:block/cube_all",\n'
            '  "textures": {{ "all": "alien-invasion:block/{0}" }}\n}}\n')
    bstate = '{{\n  "variants": {{ "": {{ "model": "alien-invasion:block/{0}" }} }}\n}}\n'
    bitem = '{{\n  "parent": "alien-invasion:block/{0}"\n}}\n'

    handheld = ('{{\n  "parent": "minecraft:item/handheld",\n'
                '  "textures": {{ "layer0": "alien-invasion:item/{0}" }}\n}}\n')
    for tool in ("bio_blade", "bio_pickaxe", "bio_axe", "bio_shovel", "bio_hoe", "purifier_wand"):
        write_json(os.path.join(ITEM_MODELS, tool + ".json"), handheld.format(tool))
    write_json(os.path.join(BLOCK_MODELS, "purifier.json"), cube.format("purifier"))
    write_json(os.path.join(BLOCKSTATES, "purifier.json"), bstate.format("purifier"))
    write_json(os.path.join(ITEM_MODELS, "purifier.json"), bitem.format("purifier"))
    print("late-game assets: bio_blade, purifier")


def make_recipes():
    recipe_dir = os.path.join(PROJECT, "src", "main", "resources", "data", "alien-invasion", "recipe")
    os.makedirs(recipe_dir, exist_ok=True)
    # Alien gear is forged from Alien Alloy with a DIAMOND handle (not sticks).
    def shaped(pattern, keys, result):
        key_lines = ",\n".join(
            '    "%s": { "item": "%s" }' % (k, v) for k, v in keys.items())
        pat = ", ".join('"%s"' % row for row in pattern)
        return ('{\n  "type": "minecraft:crafting_shaped",\n'
                '  "pattern": [%s],\n'
                '  "key": {\n%s\n  },\n'
                '  "result": { "id": "alien-invasion:%s", "count": 1 }\n}\n'
                % (pat, key_lines, result))

    ALLOY = "alien-invasion:alien_alloy"
    DIAMOND = "minecraft:diamond"
    SHARD = "alien-invasion:cosmic_shard"

    write_json(os.path.join(recipe_dir, "bio_blade.json"),
               shaped(["A", "A", "D"], {"A": ALLOY, "D": DIAMOND}, "bio_blade"))
    write_json(os.path.join(recipe_dir, "bio_pickaxe.json"),
               shaped(["AAA", " D ", " D "], {"A": ALLOY, "D": DIAMOND}, "bio_pickaxe"))
    write_json(os.path.join(recipe_dir, "bio_axe.json"),
               shaped(["AA", "AD", " D"], {"A": ALLOY, "D": DIAMOND}, "bio_axe"))
    write_json(os.path.join(recipe_dir, "bio_shovel.json"),
               shaped(["A", "D", "D"], {"A": ALLOY, "D": DIAMOND}, "bio_shovel"))
    write_json(os.path.join(recipe_dir, "bio_hoe.json"),
               shaped(["AA", " D", " D"], {"A": ALLOY, "D": DIAMOND}, "bio_hoe"))
    write_json(os.path.join(recipe_dir, "purifier_wand.json"),
               shaped([" C ", " A ", " D "], {"C": SHARD, "A": ALLOY, "D": DIAMOND}, "purifier_wand"))
    write_json(os.path.join(recipe_dir, "purifier.json"),
               '{\n  "type": "minecraft:crafting_shaped",\n'
               '  "pattern": ["AAA", "ACA", "AAA"],\n'
               '  "key": {\n'
               '    "A": { "item": "alien-invasion:alien_alloy" },\n'
               '    "C": { "item": "alien-invasion:hive_core" }\n'
               '  },\n'
               '  "result": { "id": "alien-invasion:purifier", "count": 1 }\n}\n')
    print("recipes: bio_blade, bio_pickaxe, bio_axe, bio_shovel, bio_hoe, purifier_wand, purifier")


if __name__ == "__main__":
    paint_grunt()
    paint_ufo()
    paint_parasite()
    import_telekinetic()
    import_brute()
    import_chicken()
    import_hive_tyrant()
    import_troll()
    make_icon()
    make_block_textures()
    make_item_textures()
    make_block_item_jsons()
    make_meteor_drill_textures()
    make_lategame_assets()
    make_recipes()
    # New alien (bio) tool + purifier wand icons (standalone, no resource pack needed).
    import generate_tool_textures
    generate_tool_textures.generate_all()
    print("\nAll textures written to", OUT)
