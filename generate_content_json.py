"""Emit all JSON assets for the content-expansion update (models, blockstates,
recipes, loot tables). Run with any python: python generate_content_json.py"""
import json
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(PROJECT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "alien-invasion")
DATA = os.path.join(RES, "data", "alien-invasion")
NS = "alien-invasion"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("wrote", os.path.relpath(path, PROJECT))


def item_model(name):
    return {"parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NS}:item/{name}"}}


def shaped(pattern, key, result, count=1):
    return {"type": "minecraft:crafting_shaped", "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()},
            "result": {"id": f"{NS}:{result}", "count": count}}


def shapeless(items, result, count=1):
    return {"type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": i} for i in items],
            "result": {"id": f"{NS}:{result}", "count": count}}


def cooking(kind, ingredient, result, exp, time):
    return {"type": f"minecraft:{kind}", "ingredient": {"item": ingredient},
            "result": {"id": f"{NS}:{result}"}, "experience": exp, "cookingtime": time}


def block_loot(block):
    return {"type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


# ---- generated item models ----
for it in ["cosmic_ingot", "bio_serum", "alien_battery", "gravity_grenade",
           "cosmic_helmet", "cosmic_chestplate", "cosmic_leggings", "cosmic_boots"]:
    w(os.path.join(ASSETS, "models", "item", it + ".json"), item_model(it))

# ---- spawn egg models ----
for egg in ["alien_stalker_spawn_egg", "plasma_caster_spawn_egg", "hive_shaman_spawn_egg"]:
    w(os.path.join(ASSETS, "models", "item", egg + ".json"),
      {"parent": "minecraft:item/template_spawn_egg"})

# ---- cosmic_block: block model, blockstate, item model, loot ----
w(os.path.join(ASSETS, "models", "block", "cosmic_block.json"),
  {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/cosmic_block"}})
w(os.path.join(ASSETS, "blockstates", "cosmic_block.json"),
  {"variants": {"": {"model": f"{NS}:block/cosmic_block"}}})
w(os.path.join(ASSETS, "models", "item", "cosmic_block.json"),
  {"parent": f"{NS}:block/cosmic_block"})
w(os.path.join(DATA, "loot_table", "blocks", "cosmic_block.json"), block_loot("cosmic_block"))

# ---- recipes ----
R = os.path.join(DATA, "recipe")
w(os.path.join(R, "cosmic_ingot_from_shard_smelting.json"),
  cooking("smelting", f"{NS}:cosmic_shard", "cosmic_ingot", 0.5, 200))
w(os.path.join(R, "cosmic_ingot_from_shard_blasting.json"),
  cooking("blasting", f"{NS}:cosmic_shard", "cosmic_ingot", 0.5, 100))
w(os.path.join(R, "cosmic_ingot_from_ore_smelting.json"),
  cooking("smelting", f"{NS}:cosmic_ore", "cosmic_ingot", 0.7, 200))
w(os.path.join(R, "cosmic_ingot_from_ore_blasting.json"),
  cooking("blasting", f"{NS}:cosmic_ore", "cosmic_ingot", 0.7, 100))

w(os.path.join(R, "cosmic_block.json"),
  shaped(["III", "III", "III"], {"I": f"{NS}:cosmic_ingot"}, "cosmic_block"))
w(os.path.join(R, "cosmic_ingot_from_block.json"),
  shapeless([f"{NS}:cosmic_block"], "cosmic_ingot", 9))

w(os.path.join(R, "bio_serum.json"),
  shaped([" C ", " F ", " B "],
         {"C": f"{NS}:cosmic_shard", "F": f"{NS}:infested_flesh", "B": "minecraft:glass_bottle"},
         "bio_serum"))
w(os.path.join(R, "alien_battery.json"),
  shaped(["ARA", "RCR", "ARA"],
         {"A": f"{NS}:alien_alloy", "R": "minecraft:redstone", "C": f"{NS}:cosmic_shard"},
         "alien_battery"))
w(os.path.join(R, "gravity_grenade.json"),
  shaped(["AAA", "ABA", "AAA"],
         {"A": f"{NS}:alien_alloy", "B": f"{NS}:alien_battery"}, "gravity_grenade", 2))

w(os.path.join(R, "cosmic_helmet.json"),
  shaped(["III", "I I"], {"I": f"{NS}:cosmic_ingot"}, "cosmic_helmet"))
w(os.path.join(R, "cosmic_chestplate.json"),
  shaped(["I I", "III", "III"], {"I": f"{NS}:cosmic_ingot"}, "cosmic_chestplate"))
w(os.path.join(R, "cosmic_leggings.json"),
  shaped(["III", "I I", "I I"], {"I": f"{NS}:cosmic_ingot"}, "cosmic_leggings"))
w(os.path.join(R, "cosmic_boots.json"),
  shaped(["I I", "I I"], {"I": f"{NS}:cosmic_ingot"}, "cosmic_boots"))

print("\nAll content JSON written.")
