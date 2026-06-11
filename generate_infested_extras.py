# -*- coding: utf-8 -*-
"""
Заражённые варианты построечных блоков: дверь, люк, стекло, каменные кирпичи,
шерсть. Текстуры — порча ванильных; blockstate двери/люка копируются из
ванильного джарника с заменой ссылок на модели (32 варианта руками не пишем).
"""
import io
import json
import os
import random
import zipfile

from PIL import Image

JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar")
ASSETS = "src/main/resources/assets/alien-invasion"
DATA = "src/main/resources/data/alien-invasion"

_zip = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(io.BytesIO(_zip.read(path))).convert("RGBA")


def clamp(v):
    return max(0, min(255, int(v)))


def corrupt(im, seed, tone=(150, 60, 200), mix=0.55, dark=0.88, veins=2, pustules=3, grain=10):
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()
    rnd = random.Random(seed)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            lum = lum ** 1.08
            nr = (r * (1 - mix) + tone[0] * (0.25 + 0.95 * lum) * mix) * dark
            ng = (g * (1 - mix) + tone[1] * (0.25 + 0.95 * lum) * mix) * dark
            nb = (b * (1 - mix) + tone[2] * (0.25 + 0.95 * lum) * mix) * dark
            j = rnd.randint(-grain, grain)
            px[x, y] = (clamp(nr + j), clamp(ng + j // 2), clamp(nb + j), a)
    for _ in range(veins):
        x, y = rnd.randrange(w), rnd.randrange(h)
        dx, dy = rnd.choice((-1, 0, 1)), rnd.choice((-1, 1))
        for _ in range(rnd.randint(10, 20)):
            r, g, b, a = px[x % w, y % h]
            if a:
                px[x % w, y % h] = (clamp(r * 0.25 + 58 * 0.75), clamp(g * 0.25 + 14 * 0.75),
                                    clamp(b * 0.25 + 86 * 0.75), a)
            if rnd.random() < 0.4:
                dx = rnd.choice((-1, 0, 1))
            x, y = (x + dx) % w, (y + dy) % h
    for _ in range(pustules):
        x, y = rnd.randrange(w), rnd.randrange(h)
        if px[x, y][3]:
            px[x, y] = (228, 140, 255, px[x, y][3])
    return im


def jwrite(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
    print("  json", path.split("alien-invasion/", 1)[1])


print("[1/4] Текстуры...")
B = f"{ASSETS}/textures/block"
corrupt(vanilla("assets/minecraft/textures/block/stone_bricks.png"), 51).save(f"{B}/infested_stone_bricks.png")
corrupt(vanilla("assets/minecraft/textures/block/white_wool.png"), 52, mix=0.6).save(f"{B}/infested_wool.png")
corrupt(vanilla("assets/minecraft/textures/block/glass.png"), 53, mix=0.5, veins=1, pustules=2, grain=4).save(f"{B}/infested_glass.png")
corrupt(vanilla("assets/minecraft/textures/block/oak_door_top.png"), 54, mix=0.5).save(f"{B}/infested_door_top.png")
corrupt(vanilla("assets/minecraft/textures/block/oak_door_bottom.png"), 55, mix=0.5).save(f"{B}/infested_door_bottom.png")
corrupt(vanilla("assets/minecraft/textures/block/oak_trapdoor.png"), 56, mix=0.5).save(f"{B}/infested_trapdoor.png")
os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
corrupt(vanilla("assets/minecraft/textures/item/oak_door.png"), 57, mix=0.5).save(f"{ASSETS}/textures/item/infested_door.png")
print("  6 block + 1 item текстур")

print("[2/4] Дверь и люк: blockstate из ванилы...")
for vanilla_name, ours in (("oak_door", "infested_door"), ("oak_trapdoor", "infested_trapdoor")):
    bs = _zip.read(f"assets/minecraft/blockstates/{vanilla_name}.json").decode("utf-8")
    bs = bs.replace(f"minecraft:block/{vanilla_name}", f"alien-invasion:block/{ours}")
    os.makedirs(f"{ASSETS}/blockstates", exist_ok=True)
    with open(f"{ASSETS}/blockstates/{ours}.json", "w") as fh:
        fh.write(bs)
    print(f"  blockstate {ours} (копия {vanilla_name})")

for part in ("bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
             "top_left", "top_left_open", "top_right", "top_right_open"):
    jwrite(f"{ASSETS}/models/block/infested_door_{part}.json",
           {"parent": f"minecraft:block/door_{part}",
            "textures": {"bottom": "alien-invasion:block/infested_door_bottom",
                         "top": "alien-invasion:block/infested_door_top"}})
for part, parent in (("bottom", "template_trapdoor_bottom"), ("top", "template_trapdoor_top"),
                     ("open", "template_trapdoor_open")):
    jwrite(f"{ASSETS}/models/block/infested_trapdoor_{part}.json",
           {"parent": f"minecraft:block/{parent}",
            "textures": {"texture": "alien-invasion:block/infested_trapdoor"}})

print("[3/4] Кубы + предметы...")
for name in ("infested_stone_bricks", "infested_wool", "infested_glass"):
    jwrite(f"{ASSETS}/blockstates/{name}.json", {"variants": {"": {"model": f"alien-invasion:block/{name}"}}})
    jwrite(f"{ASSETS}/models/block/{name}.json",
           {"parent": "minecraft:block/cube_all", "textures": {"all": f"alien-invasion:block/{name}"}})
    jwrite(f"{ASSETS}/models/item/{name}.json", {"parent": f"alien-invasion:block/{name}"})
jwrite(f"{ASSETS}/models/item/infested_door.json",
       {"parent": "minecraft:item/generated", "textures": {"layer0": "alien-invasion:item/infested_door"}})
jwrite(f"{ASSETS}/models/item/infested_trapdoor.json",
       {"parent": "alien-invasion:block/infested_trapdoor_bottom"})

print("[4/4] Лут...")
for name in ("infested_stone_bricks", "infested_wool", "infested_glass", "infested_trapdoor"):
    jwrite(f"{DATA}/loot_table/blocks/{name}.json",
           {"type": "minecraft:block",
            "pools": [{"rolls": 1,
                       "entries": [{"type": "minecraft:item", "name": f"alien-invasion:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
jwrite(f"{DATA}/loot_table/blocks/infested_door.json",
       {"type": "minecraft:block",
        "pools": [{"rolls": 1,
                   "entries": [{"type": "minecraft:item", "name": "alien-invasion:infested_door"}],
                   "conditions": [
                       {"condition": "minecraft:block_state_property",
                        "block": "alien-invasion:infested_door",
                        "properties": {"half": "lower"}},
                       {"condition": "minecraft:survives_explosion"}]}]})
print("Готово.")
