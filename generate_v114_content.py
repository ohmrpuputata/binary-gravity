"""Generate data and JSON assets for Alien Apocalypse v1.14.

Run from project root:
    .venv/Scripts/python.exe generate_v114_content.py
"""
import json
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(PROJECT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "alien-invasion")
DATA = os.path.join(RES, "data")
MOD_DATA = os.path.join(DATA, "alien-invasion")
NS = "alien-invasion"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("wrote", os.path.relpath(path, PROJECT))


def load_json(path):
    if not os.path.exists(path):
        return {}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def item_model(name):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}}


def block_cube(name, texture=None):
    tex = texture or name
    return {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{tex}"}}


def block_loot(block):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }]
    }


def ore_feature(name, block, size, discard=0.15, deepslate_block=None):
    targets = [{
        "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
        "state": {"Name": f"{NS}:{block}"}
    }]
    targets.append({
        "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
        "state": {"Name": f"{NS}:{deepslate_block or block}"}
    })
    w(os.path.join(MOD_DATA, "worldgen", "configured_feature", name + ".json"), {
        "type": "minecraft:ore",
        "config": {
            "size": size,
            "discard_chance_on_air_exposure": discard,
            "targets": targets
        }
    })


def placed_feature(name, count, min_y, max_y):
    w(os.path.join(MOD_DATA, "worldgen", "placed_feature", name + ".json"), {
        "feature": f"{NS}:{name}",
        "placement": [
            {"type": "minecraft:count", "count": count},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": min_y},
                "max_inclusive": {"absolute": max_y}
            }},
            {"type": "minecraft:biome"}
        ]
    })


def shaped(pattern, key, result, count=1):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": {k: {"item": v} for k, v in key.items()},
        "result": {"id": f"{NS}:{result}", "count": count}
    }


def shapeless(items, result, count=1):
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": item} for item in items],
        "result": {"id": f"{NS}:{result}", "count": count}
    }


def cooking(kind, ingredient, result, exp, time):
    return {
        "type": f"minecraft:{kind}",
        "ingredient": {"item": ingredient},
        "result": {"id": f"{NS}:{result}"},
        "experience": exp,
        "cookingtime": time
    }


def add_lang(entries):
    for lang in ("en_us", "ru_ru"):
        path = os.path.join(ASSETS, "lang", lang + ".json")
        data = load_json(path)
        data.update(entries[lang])
        w(path, data)


INFESTED_BLOCKS = [
    "infested_deepslate", "infested_dirt", "infested_sand", "infested_gravel",
    "infested_clay", "infested_netherrack", "infested_planks",
]

ORE_BLOCKS = [
    "uranium_ore", "deepslate_uranium_ore", "xenocrystal_ore", "bio_vein_ore",
    "plasma_ore", "iridium_ore", "dark_matter_ore",
]

STATION_BLOCKS = [
    "black_market_terminal", "purifier_station", "ore_washer", "radiation_forge",
    "alien_recycler", "blueprint_table",
]

DECOR_BLOCKS = [
    "warning_lamp", "cracked_alien_pipe", "toxic_barrel", "broken_lab_crate",
    "radiation_crystal_cluster", "contaminated_bones",
]

NEW_ITEMS = [
    "alien_scrap", "cosmic_credit", "uranium_dust", "uranium_rod", "xenocrystal",
    "bio_fiber", "plasma_core", "iridium_plate", "dark_matter_shard",
    "drill_fuel_cell", "reinforced_drill_head", "lava_cooling_module",
    "ore_filter_module", "toxic_seal_module", "storage_bay_module", "headlamp_module",
    "toxic_water_bucket", "uranium_sword", "uranium_pickaxe", "uranium_axe",
    "uranium_shovel", "uranium_hoe", "plasma_sword", "plasma_pickaxe",
    "plasma_axe", "plasma_shovel", "plasma_hoe", "iridium_sword",
    "iridium_pickaxe", "iridium_axe", "iridium_shovel", "iridium_hoe",
    "radiation_drill_head", "purifier_drill_head", "toxic_water_pump",
    "geiger_counter", "portable_purifier", "rad_pills", "bio_filter_mask",
    "contaminated_food", "purified_water_flask",
]


def generate_models_and_loot():
    block_names = INFESTED_BLOCKS + ORE_BLOCKS + STATION_BLOCKS + DECOR_BLOCKS
    for name in block_names:
        w(os.path.join(ASSETS, "models", "block", name + ".json"), block_cube(name))
        w(os.path.join(ASSETS, "blockstates", name + ".json"),
          {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        w(os.path.join(ASSETS, "models", "item", name + ".json"),
          {"parent": f"{NS}:block/{name}"})
        w(os.path.join(MOD_DATA, "loot_table", "blocks", name + ".json"), block_loot(name))

    w(os.path.join(ASSETS, "models", "block", "dead_infested_crop.json"),
      {"parent": "minecraft:block/cross", "textures": {"cross": f"{NS}:block/dead_infested_crop"}})
    w(os.path.join(ASSETS, "blockstates", "dead_infested_crop.json"),
      {"variants": {"": {"model": f"{NS}:block/dead_infested_crop"}}})
    w(os.path.join(ASSETS, "models", "item", "dead_infested_crop.json"), item_model("dead_infested_crop"))
    w(os.path.join(MOD_DATA, "loot_table", "blocks", "dead_infested_crop.json"), block_loot("dead_infested_crop"))

    w(os.path.join(ASSETS, "models", "block", "infested_log.json"), {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": f"{NS}:block/infested_log_top", "side": f"{NS}:block/infested_log"}
    })
    w(os.path.join(ASSETS, "blockstates", "infested_log.json"), {
        "variants": {
            "axis=x": {"model": f"{NS}:block/infested_log", "x": 90, "y": 90},
            "axis=y": {"model": f"{NS}:block/infested_log"},
            "axis=z": {"model": f"{NS}:block/infested_log", "x": 90}
        }
    })

    for name in NEW_ITEMS:
        w(os.path.join(ASSETS, "models", "item", name + ".json"), item_model(name))


def generate_tags():
    pickaxe = [
        f"{NS}:{b}" for b in ORE_BLOCKS + STATION_BLOCKS + DECOR_BLOCKS
    ] + [
        f"{NS}:infested_stone", f"{NS}:infested_deepslate", f"{NS}:infested_netherrack",
        f"{NS}:alien_residue", f"{NS}:alien_hive", f"{NS}:cosmic_ore",
        f"{NS}:cosmic_crystal", f"{NS}:cosmic_block", f"{NS}:plasma_turret",
        f"{NS}:swarm_beacon", f"{NS}:purifier",
    ]
    shovel = [f"{NS}:infested_dirt", f"{NS}:infested_sand", f"{NS}:infested_gravel", f"{NS}:infested_clay"]
    axe = [f"{NS}:infested_log", f"{NS}:infested_planks", f"{NS}:infested_leaves", f"{NS}:dead_infested_crop"]
    w(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"),
      {"replace": False, "values": sorted(set(pickaxe))})
    w(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "shovel.json"),
      {"replace": False, "values": sorted(set(shovel))})
    w(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "axe.json"),
      {"replace": False, "values": sorted(set(axe))})
    w(os.path.join(DATA, "minecraft", "tags", "block", "needs_iron_tool.json"),
      {"replace": False, "values": [f"{NS}:uranium_ore", f"{NS}:deepslate_uranium_ore", f"{NS}:xenocrystal_ore", f"{NS}:bio_vein_ore"]})
    w(os.path.join(DATA, "minecraft", "tags", "block", "needs_diamond_tool.json"),
      {"replace": False, "values": [f"{NS}:plasma_ore", f"{NS}:iridium_ore", f"{NS}:dark_matter_ore"]})


def generate_recipes():
    r = os.path.join(MOD_DATA, "recipe")
    # Ore chain: raw block -> dust/shard/fiber -> refined component -> tool.
    for ore in ("uranium_ore", "deepslate_uranium_ore"):
        w(os.path.join(r, f"uranium_dust_from_{ore}_blasting.json"),
          cooking("blasting", f"{NS}:{ore}", "uranium_dust", 0.8, 180))
    w(os.path.join(r, "uranium_rod.json"),
      shaped([" D ", "DID", " D "], {"D": f"{NS}:uranium_dust", "I": "minecraft:iron_ingot"}, "uranium_rod", 2))
    w(os.path.join(r, "xenocrystal_from_ore_blasting.json"),
      cooking("blasting", f"{NS}:xenocrystal_ore", "xenocrystal", 0.9, 180))
    w(os.path.join(r, "bio_fiber_from_ore_blasting.json"),
      cooking("blasting", f"{NS}:bio_vein_ore", "bio_fiber", 0.6, 160))
    w(os.path.join(r, "plasma_core.json"),
      shaped(["XPX", "PBP", "XPX"], {"X": f"{NS}:xenocrystal", "P": f"{NS}:plasma_cell", "B": f"{NS}:alien_battery"}, "plasma_core"))
    w(os.path.join(r, "plasma_core_from_ore_blasting.json"),
      cooking("blasting", f"{NS}:plasma_ore", "plasma_core", 1.0, 240))
    w(os.path.join(r, "iridium_plate.json"),
      shaped(["III", "IPI", "III"], {"I": "minecraft:iron_ingot", "P": f"{NS}:plasma_core"}, "iridium_plate", 2))
    w(os.path.join(r, "iridium_plate_from_ore_blasting.json"),
      cooking("blasting", f"{NS}:iridium_ore", "iridium_plate", 1.2, 260))
    w(os.path.join(r, "dark_matter_shard_from_ore_blasting.json"),
      cooking("blasting", f"{NS}:dark_matter_ore", "dark_matter_shard", 1.5, 320))

    w(os.path.join(r, "drill_fuel_cell.json"),
      shaped(["SUS", "ABA", "SUS"], {"S": f"{NS}:alien_scrap", "U": f"{NS}:uranium_dust", "A": f"{NS}:alien_alloy", "B": f"{NS}:alien_battery"}, "drill_fuel_cell", 2))
    w(os.path.join(r, "black_market_terminal.json"),
      shaped(["ICI", "ABA", "IRI"], {"I": "minecraft:iron_block", "C": f"{NS}:cosmic_credit", "A": f"{NS}:alien_alloy", "B": f"{NS}:alien_battery", "R": "minecraft:redstone_block"}, "black_market_terminal"))
    w(os.path.join(r, "purifier_station.json"),
      shaped(["GCG", "APA", "IRI"], {"G": "minecraft:glass", "C": f"{NS}:cosmic_shard", "A": f"{NS}:alien_alloy", "P": f"{NS}:purifier", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "purifier_station"))
    w(os.path.join(r, "ore_washer.json"),
      shaped(["BGB", "ICI", "BIB"], {"B": "minecraft:bucket", "G": "minecraft:glass", "I": "minecraft:iron_ingot", "C": f"{NS}:cosmic_shard"}, "ore_washer"))
    w(os.path.join(r, "radiation_forge.json"),
      shaped(["URU", "RFR", "URU"], {"U": f"{NS}:uranium_rod", "R": "minecraft:redstone_block", "F": "minecraft:blast_furnace"}, "radiation_forge"))
    w(os.path.join(r, "alien_recycler.json"),
      shaped(["ASA", "SCS", "ARA"], {"A": f"{NS}:alien_alloy", "S": f"{NS}:alien_scrap", "C": "minecraft:composter", "R": "minecraft:redstone"}, "alien_recycler"))
    w(os.path.join(r, "blueprint_table.json"),
      shaped(["PBP", "ACA", "I I"], {"P": "minecraft:paper", "B": "minecraft:book", "A": f"{NS}:alien_alloy", "C": f"{NS}:cosmic_credit", "I": "minecraft:iron_ingot"}, "blueprint_table"))

    for tier, mat, rod in [
        ("uranium", f"{NS}:uranium_rod", "minecraft:stick"),
        ("plasma", f"{NS}:plasma_core", f"{NS}:uranium_rod"),
        ("iridium", f"{NS}:iridium_plate", f"{NS}:uranium_rod"),
    ]:
        w(os.path.join(r, f"{tier}_sword.json"), shaped(["M", "M", "R"], {"M": mat, "R": rod}, f"{tier}_sword"))
        w(os.path.join(r, f"{tier}_pickaxe.json"), shaped(["MMM", " R ", " R "], {"M": mat, "R": rod}, f"{tier}_pickaxe"))
        w(os.path.join(r, f"{tier}_axe.json"), shaped(["MM", "MR", " R"], {"M": mat, "R": rod}, f"{tier}_axe"))
        w(os.path.join(r, f"{tier}_shovel.json"), shaped(["M", "R", "R"], {"M": mat, "R": rod}, f"{tier}_shovel"))
        w(os.path.join(r, f"{tier}_hoe.json"), shaped(["MM", " R", " R"], {"M": mat, "R": rod}, f"{tier}_hoe"))

    w(os.path.join(r, "reinforced_drill_head.json"),
      shaped(["IPI", "PDP", "IPI"], {"I": f"{NS}:iridium_plate", "P": f"{NS}:plasma_core", "D": "minecraft:diamond_block"}, "reinforced_drill_head"))
    w(os.path.join(r, "lava_cooling_module.json"),
      shaped(["OCO", "PBP", "OCO"], {"O": "minecraft:obsidian", "C": f"{NS}:cosmic_shard", "P": f"{NS}:plasma_core", "B": f"{NS}:alien_battery"}, "lava_cooling_module"))
    w(os.path.join(r, "ore_filter_module.json"),
      shaped(["IXI", "XBX", "IXI"], {"I": f"{NS}:iridium_plate", "X": f"{NS}:xenocrystal", "B": f"{NS}:alien_battery"}, "ore_filter_module"))
    w(os.path.join(r, "toxic_seal_module.json"),
      shaped(["RBR", "BHB", "RBR"], {"R": f"{NS}:uranium_rod", "B": f"{NS}:bio_fiber", "H": f"{NS}:hazmat_chestplate"}, "toxic_seal_module"))
    w(os.path.join(r, "storage_bay_module.json"),
      shaped(["ICI", "CBC", "ICI"], {"I": f"{NS}:iridium_plate", "C": "minecraft:chest", "B": f"{NS}:alien_battery"}, "storage_bay_module"))
    w(os.path.join(r, "headlamp_module.json"),
      shaped(["GLG", "CBC", "IRI"], {"G": "minecraft:glowstone_dust", "L": "minecraft:lantern", "C": f"{NS}:cosmic_shard", "B": f"{NS}:alien_battery", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "headlamp_module"))
    w(os.path.join(r, "radiation_drill_head.json"),
      shaped(["UUU", "UDU", "IRI"], {"U": f"{NS}:uranium_rod", "D": "minecraft:diamond", "I": f"{NS}:iridium_plate", "R": "minecraft:redstone_block"}, "radiation_drill_head"))
    w(os.path.join(r, "purifier_drill_head.json"),
      shaped(["XHX", "CDC", "IRI"], {"X": f"{NS}:xenocrystal", "H": f"{NS}:hive_core", "C": f"{NS}:cosmic_shard", "D": "minecraft:diamond", "I": f"{NS}:iridium_plate", "R": "minecraft:redstone"}, "purifier_drill_head"))
    w(os.path.join(r, "toxic_water_pump.json"),
      shaped(["IPI", "BHB", "IRI"], {"I": "minecraft:iron_ingot", "P": f"{NS}:plasma_core", "B": "minecraft:bucket", "H": f"{NS}:bio_filter_mask", "R": "minecraft:redstone"}, "toxic_water_pump"))
    w(os.path.join(r, "geiger_counter.json"),
      shaped(["IRI", "RCR", "IBI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "C": f"{NS}:uranium_dust", "B": f"{NS}:alien_battery"}, "geiger_counter"))
    w(os.path.join(r, "portable_purifier.json"),
      shaped(["GCG", "ABA", "IRI"], {"G": "minecraft:glass", "C": f"{NS}:cosmic_shard", "A": f"{NS}:alien_alloy", "B": f"{NS}:alien_battery", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "portable_purifier"))
    w(os.path.join(r, "rad_pills.json"),
      shapeless([f"{NS}:uranium_dust", f"{NS}:bio_serum", "minecraft:sugar"], "rad_pills", 4))
    w(os.path.join(r, "bio_filter_mask.json"),
      shaped(["CFC", "BHB", "CFC"], {"C": f"{NS}:bio_fiber", "F": "minecraft:charcoal", "B": "minecraft:black_wool", "H": f"{NS}:hazmat_helmet"}, "bio_filter_mask"))
    w(os.path.join(r, "purified_water_flask.json"),
      shapeless([f"{NS}:portable_purifier", "minecraft:glass_bottle", "minecraft:potion"], "purified_water_flask", 2))


def generate_worldgen():
    ore_feature("uranium_ore_vein", "uranium_ore", 6, 0.25, "deepslate_uranium_ore")
    placed_feature("uranium_ore_vein", 6, -32, 48)
    ore_feature("xenocrystal_ore_vein", "xenocrystal_ore", 5, 0.2)
    placed_feature("xenocrystal_ore_vein", 4, -48, 24)
    ore_feature("bio_vein_ore_vein", "bio_vein_ore", 8, 0.1)
    placed_feature("bio_vein_ore_vein", 5, -24, 56)
    ore_feature("plasma_ore_vein", "plasma_ore", 4, 0.35)
    placed_feature("plasma_ore_vein", 3, -48, 8)
    ore_feature("iridium_ore_vein", "iridium_ore", 4, 0.45)
    placed_feature("iridium_ore_vein", 2, -64, -8)
    ore_feature("dark_matter_ore_vein", "dark_matter_ore", 3, 0.6)
    placed_feature("dark_matter_ore_vein", 1, -64, -32)


def generate_lang():
    en = {
        "block.alien-invasion.toxic_water": "Toxic Water",
        "block.alien-invasion.infested_deepslate": "Infested Deepslate",
        "block.alien-invasion.infested_dirt": "Infested Dirt",
        "block.alien-invasion.infested_sand": "Infested Sand",
        "block.alien-invasion.infested_gravel": "Infested Gravel",
        "block.alien-invasion.infested_clay": "Infested Clay",
        "block.alien-invasion.infested_netherrack": "Infested Netherrack",
        "block.alien-invasion.infested_planks": "Infested Planks",
        "block.alien-invasion.dead_infested_crop": "Dead Infested Crop",
        "block.alien-invasion.uranium_ore": "Uranium Ore",
        "block.alien-invasion.deepslate_uranium_ore": "Deepslate Uranium Ore",
        "block.alien-invasion.xenocrystal_ore": "Xenocrystal Ore",
        "block.alien-invasion.bio_vein_ore": "Bio-Vein Ore",
        "block.alien-invasion.plasma_ore": "Plasma Ore",
        "block.alien-invasion.iridium_ore": "Iridium Ore",
        "block.alien-invasion.dark_matter_ore": "Dark Matter Ore",
        "block.alien-invasion.black_market_terminal": "Black Market Terminal",
        "block.alien-invasion.purifier_station": "Purifier Station",
        "block.alien-invasion.ore_washer": "Ore Washer",
        "block.alien-invasion.radiation_forge": "Radiation Forge",
        "block.alien-invasion.alien_recycler": "Alien Recycler",
        "block.alien-invasion.blueprint_table": "Blueprint Table",
        "block.alien-invasion.warning_lamp": "Warning Lamp",
        "block.alien-invasion.cracked_alien_pipe": "Cracked Alien Pipe",
        "block.alien-invasion.toxic_barrel": "Toxic Barrel",
        "block.alien-invasion.broken_lab_crate": "Broken Lab Crate",
        "block.alien-invasion.radiation_crystal_cluster": "Radiation Crystal Cluster",
        "block.alien-invasion.contaminated_bones": "Contaminated Bones",
    }
    ru = {
        "block.alien-invasion.toxic_water": "Токсичная вода",
        "block.alien-invasion.infested_deepslate": "Зараженный глубинный сланец",
        "block.alien-invasion.infested_dirt": "Зараженная земля",
        "block.alien-invasion.infested_sand": "Зараженный песок",
        "block.alien-invasion.infested_gravel": "Зараженный гравий",
        "block.alien-invasion.infested_clay": "Зараженная глина",
        "block.alien-invasion.infested_netherrack": "Зараженный незерак",
        "block.alien-invasion.infested_planks": "Зараженные доски",
        "block.alien-invasion.dead_infested_crop": "Мертвый зараженный росток",
        "block.alien-invasion.uranium_ore": "Урановая руда",
        "block.alien-invasion.deepslate_uranium_ore": "Глубинная урановая руда",
        "block.alien-invasion.xenocrystal_ore": "Ксенокристальная руда",
        "block.alien-invasion.bio_vein_ore": "Био-жила",
        "block.alien-invasion.plasma_ore": "Плазменная руда",
        "block.alien-invasion.iridium_ore": "Иридиевая руда",
        "block.alien-invasion.dark_matter_ore": "Руда темной материи",
        "block.alien-invasion.black_market_terminal": "Терминал черного рынка",
        "block.alien-invasion.purifier_station": "Станция очистки",
        "block.alien-invasion.ore_washer": "Промыватель руды",
        "block.alien-invasion.radiation_forge": "Радиационная кузница",
        "block.alien-invasion.alien_recycler": "Инопланетный переработчик",
        "block.alien-invasion.blueprint_table": "Стол чертежей",
        "block.alien-invasion.warning_lamp": "Сигнальная лампа",
        "block.alien-invasion.cracked_alien_pipe": "Треснувшая инопланетная труба",
        "block.alien-invasion.toxic_barrel": "Токсичная бочка",
        "block.alien-invasion.broken_lab_crate": "Сломанный лабораторный ящик",
        "block.alien-invasion.radiation_crystal_cluster": "Радиационный кристальный кластер",
        "block.alien-invasion.contaminated_bones": "Зараженные кости",
    }
    item_en = {
        "alien_scrap": "Alien Scrap",
        "cosmic_credit": "Cosmic Credit",
        "uranium_dust": "Uranium Dust",
        "uranium_rod": "Uranium Rod",
        "xenocrystal": "Xenocrystal",
        "bio_fiber": "Bio-Fiber",
        "plasma_core": "Plasma Core",
        "iridium_plate": "Iridium Plate",
        "dark_matter_shard": "Dark Matter Shard",
        "drill_fuel_cell": "Drill Fuel Cell",
        "reinforced_drill_head": "Reinforced Drill Head",
        "lava_cooling_module": "Lava Cooling Module",
        "ore_filter_module": "Ore Filter Module",
        "toxic_seal_module": "Toxic Seal Module",
        "storage_bay_module": "Storage Bay Module",
        "headlamp_module": "Headlamp Module",
        "toxic_water_bucket": "Toxic Water Bucket",
        "uranium_sword": "Uranium Sword",
        "uranium_pickaxe": "Uranium Pickaxe",
        "uranium_axe": "Uranium Axe",
        "uranium_shovel": "Uranium Shovel",
        "uranium_hoe": "Uranium Hoe",
        "plasma_sword": "Plasma Sword",
        "plasma_pickaxe": "Plasma Pickaxe",
        "plasma_axe": "Plasma Axe",
        "plasma_shovel": "Plasma Shovel",
        "plasma_hoe": "Plasma Hoe",
        "iridium_sword": "Iridium Sword",
        "iridium_pickaxe": "Iridium Pickaxe",
        "iridium_axe": "Iridium Axe",
        "iridium_shovel": "Iridium Shovel",
        "iridium_hoe": "Iridium Hoe",
        "radiation_drill_head": "Radiation Drill Head",
        "purifier_drill_head": "Purifier Drill Head",
        "toxic_water_pump": "Toxic-Water Pump",
        "geiger_counter": "Geiger Counter",
        "portable_purifier": "Portable Purifier",
        "rad_pills": "Rad Pills",
        "bio_filter_mask": "Bio-Filter Mask",
        "contaminated_food": "Contaminated Food",
        "purified_water_flask": "Purified Water Flask",
    }
    item_ru = {
        "alien_scrap": "Инопланетный лом",
        "cosmic_credit": "Космический кредит",
        "uranium_dust": "Урановая пыль",
        "uranium_rod": "Урановый стержень",
        "xenocrystal": "Ксенокристалл",
        "bio_fiber": "Био-волокно",
        "plasma_core": "Плазменное ядро",
        "iridium_plate": "Иридиевая пластина",
        "dark_matter_shard": "Осколок темной материи",
        "drill_fuel_cell": "Топливная ячейка бура",
        "reinforced_drill_head": "Усиленная буровая головка",
        "lava_cooling_module": "Лавовый модуль охлаждения",
        "ore_filter_module": "Рудный фильтр",
        "toxic_seal_module": "Токсичный гермомодуль",
        "storage_bay_module": "Грузовой модуль",
        "headlamp_module": "Модуль фары",
        "toxic_water_bucket": "Ведро токсичной воды",
        "uranium_sword": "Урановый меч",
        "uranium_pickaxe": "Урановая кирка",
        "uranium_axe": "Урановый топор",
        "uranium_shovel": "Урановая лопата",
        "uranium_hoe": "Урановая мотыга",
        "plasma_sword": "Плазменный меч",
        "plasma_pickaxe": "Плазменная кирка",
        "plasma_axe": "Плазменный топор",
        "plasma_shovel": "Плазменная лопата",
        "plasma_hoe": "Плазменная мотыга",
        "iridium_sword": "Иридиевый меч",
        "iridium_pickaxe": "Иридиевая кирка",
        "iridium_axe": "Иридиевый топор",
        "iridium_shovel": "Иридиевая лопата",
        "iridium_hoe": "Иридиевая мотыга",
        "radiation_drill_head": "Радиационная буровая головка",
        "purifier_drill_head": "Очистительная буровая головка",
        "toxic_water_pump": "Насос токсичной воды",
        "geiger_counter": "Счетчик Гейгера",
        "portable_purifier": "Портативный очиститель",
        "rad_pills": "Таблетки от радиации",
        "bio_filter_mask": "Био-фильтр маска",
        "contaminated_food": "Зараженная еда",
        "purified_water_flask": "Фляга очищенной воды",
    }
    for key, value in item_en.items():
        en[f"item.alien-invasion.{key}"] = value
    for key, value in item_ru.items():
        ru[f"item.alien-invasion.{key}"] = value
    add_lang({"en_us": en, "ru_ru": ru})


def main():
    generate_models_and_loot()
    generate_tags()
    generate_recipes()
    generate_worldgen()
    generate_lang()
    print("v1.14 content JSON generated.")


if __name__ == "__main__":
    main()
