# -*- coding: utf-8 -*-
"""
Перерисовка заражённых блоков на базе ВАНИЛЬНЫХ текстур + нормальная
анимированная вода (как у ванильной: 32 кадра) + новые блоки апокалипсиса.

Ванильные текстуры берутся из dev-джарника loom:
  ~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar

Фильтр "заражения": сдвиг тона в фиолет с сохранением структуры/яркости
оригинала, органические прожилки (замкнутые по тайлу) и светящиеся пустулы.
"""
import io
import json
import math
import os
import random
import zipfile

from PIL import Image

JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar")
ASSETS = "src/main/resources/assets/alien-invasion"
DATA = "src/main/resources/data/alien-invasion"
BLOCK_TEX = f"{ASSETS}/textures/block"

_zip = zipfile.ZipFile(JAR)


def vanilla(name):
    data = _zip.read(f"assets/minecraft/textures/block/{name}.png")
    return Image.open(io.BytesIO(data)).convert("RGBA")


def clamp(v):
    return max(0, min(255, int(v)))


def corrupt(im, seed, tone=(150, 60, 200), mix=0.55, dark=0.88,
            veins=2, pustules=3, grain=10):
    """Сдвиг палитры в 'инопланетный' тон с сохранением рельефа оригинала."""
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
            # лёгкая кривая, чтобы тени были глубже
            lum = lum ** 1.08
            tr = tone[0] * (0.25 + 0.95 * lum)
            tg = tone[1] * (0.25 + 0.95 * lum)
            tb = tone[2] * (0.25 + 0.95 * lum)
            nr = (r * (1 - mix) + tr * mix) * dark
            ng = (g * (1 - mix) + tg * mix) * dark
            nb = (b * (1 - mix) + tb * mix) * dark
            j = rnd.randint(-grain, grain)
            px[x, y] = (clamp(nr + j), clamp(ng + j // 2), clamp(nb + j), a)

    # Прожилки: блуждающие тёмные линии, замкнутые по тайлу (mod w/h).
    for _ in range(veins):
        x = rnd.randrange(w)
        y = rnd.randrange(h)
        dx = rnd.choice((-1, 0, 1))
        dy = rnd.choice((-1, 1))
        for _ in range(rnd.randint(10, 20)):
            for ox, oy, f in ((0, 0, 0.78), (1, 0, 0.35)):
                xx = (x + ox) % w
                yy = (y + oy) % h
                r, g, b, a = px[xx, yy]
                if a == 0:
                    continue
                px[xx, yy] = (clamp(r * (1 - f) + 58 * f), clamp(g * (1 - f) + 14 * f),
                              clamp(b * (1 - f) + 86 * f), a)
            if rnd.random() < 0.4:
                dx = rnd.choice((-1, 0, 1))
            x = (x + dx) % w
            y = (y + dy) % h

    # Пустулы: яркие точки с ореолом — "живое" свечение заразы.
    for _ in range(pustules):
        x = rnd.randrange(w)
        y = rnd.randrange(h)
        if px[x, y][3] == 0:
            continue
        for ox in (-1, 0, 1):
            for oy in (-1, 0, 1):
                xx = (x + ox) % w
                yy = (y + oy) % h
                r, g, b, a = px[xx, yy]
                if a == 0:
                    continue
                if ox == 0 and oy == 0:
                    px[xx, yy] = (228, 140, 255, a)
                elif rnd.random() < 0.7:
                    px[xx, yy] = (clamp(r * 0.4 + 160 * 0.6), clamp(g * 0.4 + 70 * 0.6),
                                  clamp(b * 0.4 + 210 * 0.6), a)
    return im


def save(im, name):
    im.save(f"{BLOCK_TEX}/{name}.png")
    print(f"  tex {name}.png")


# ---------------------------------------------------------------- блоки
print("[1/4] Заражённые текстуры из ванильных...")
PURPLE = (150, 60, 200)
RECIPES = [
    # (vanilla, out, params)
    ("stone",                  "infested_stone",        dict(seed=11)),
    ("deepslate",              "infested_deepslate",    dict(seed=12, dark=0.8)),
    ("dirt",                   "infested_dirt",         dict(seed=13)),
    ("sand",                   "infested_sand",         dict(seed=14, mix=0.45, dark=0.95, tone=(170, 90, 210))),
    ("gravel",                 "infested_gravel",       dict(seed=15)),
    ("clay",                   "infested_clay",         dict(seed=16, mix=0.5)),
    ("netherrack",             "infested_netherrack",   dict(seed=17, tone=(200, 40, 120))),
    ("oak_planks",             "infested_planks",       dict(seed=18, mix=0.5)),
    ("oak_log",                "infested_log",          dict(seed=19, mix=0.5)),
    ("oak_log_top",            "infested_log_top",      dict(seed=20, mix=0.5)),
    ("bone_block_side",        "contaminated_bones",    dict(seed=21, tone=(120, 165, 80), mix=0.4, veins=1)),
    ("deepslate_diamond_ore",  "infested_diamond_ore",  dict(seed=22, mix=0.45, dark=0.82)),
    ("deepslate_redstone_ore", "infested_redstone_ore", dict(seed=23, mix=0.45, dark=0.82, tone=(200, 40, 120))),
    ("sandstone",              "infested_sandstone",    dict(seed=24, mix=0.45, dark=0.95, tone=(170, 90, 210))),
    ("terracotta",             "infested_terracotta",   dict(seed=25, mix=0.5)),
    ("snow",                   "infested_snow",         dict(seed=26, mix=0.32, dark=1.0, tone=(175, 140, 215), grain=6, veins=1, pustules=2)),
]
for src, out, params in RECIPES:
    save(corrupt(vanilla(src), **params), out)

# Листва: ванильная — серая с альфой (красится биомом), запекаем фиолет сразу.
leaves = corrupt(vanilla("oak_leaves"), seed=27, tone=(135, 70, 195), mix=0.9, dark=1.0, veins=0, pustules=4)
save(leaves, "infested_leaves")

# Лёд: сохраняем родную альфу, тонируем в сине-фиолет.
ice = corrupt(vanilla("ice"), seed=28, tone=(140, 95, 225), mix=0.5, dark=1.0, veins=1, pustules=2, grain=5)
save(ice, "infested_ice")

# Трава: верх — заражённый дёрн, бок — заражённая земля с фиолетовой кромкой.
gtop = corrupt(vanilla("grass_block_top"), seed=29, tone=(125, 55, 180), mix=0.9, dark=0.95, veins=2, pustules=3)
save(gtop, "infested_grass_top")
gside = corrupt(vanilla("dirt"), seed=30)
band = corrupt(vanilla("grass_block_side_overlay"), seed=31, tone=(125, 55, 180), mix=0.9, dark=0.95, veins=0, pustules=0)
gside.alpha_composite(band)
save(gside, "infested_grass_side")
# старая монотекстура остаётся для совместимости, но перерисуем и её
save(gtop.copy(), "infested_grass")

# Мёртвый колос: пшеница 7-й стадии, высушенная и посеревшая.
crop = corrupt(vanilla("wheat_stage7"), seed=32, tone=(120, 90, 140), mix=0.75, dark=0.7, veins=0, pustules=2, grain=6)
save(crop, "dead_infested_crop")

# Слизь роя: земля, переваренная до фиолетовой массы.
residue = corrupt(vanilla("dirt"), seed=33, tone=(110, 25, 160), mix=0.85, dark=0.8, veins=4, pustules=6)
save(residue, "alien_residue")

# ------------------------------------------------------------- щупальца
print("[2/4] Щупальца роя...")
t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
tp = t.load()
rnd = random.Random(404)
for stalk in range(4):
    x = 2 + stalk * 4 + rnd.randint(-1, 1)
    height = rnd.randint(7, 13)
    for i in range(height):
        y = 15 - i
        frac = i / height
        col = (clamp(60 + 150 * frac), clamp(20 + 90 * frac), clamp(90 + 150 * frac), 255)
        tp[x % 16, y] = col
        if rnd.random() < 0.35:
            tp[(x + rnd.choice((-1, 1))) % 16, y] = (col[0] // 2 + 20, col[1] // 2 + 5, col[2] // 2 + 30, 255)
        if rnd.random() < 0.5:
            x += rnd.choice((-1, 0, 1))
            x = max(1, min(14, x))
    # светящаяся почка на верхушке
    ty = 15 - height
    tp[x % 16, ty] = (235, 150, 255, 255)
    tp[x % 16, min(15, ty + 1)] = (190, 90, 240, 255)
save(t, "alien_tendrils")

# ------------------------------------------------- вода: 32 кадра, как ванила
print("[3/4] Анимированная вода...")


def hash2(x, y):
    n = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n ^ (n >> 16)) & 0xFF


def water_texture(size, frames, scroll):
    img = Image.new("RGBA", (size, size * frames))
    px = img.load()
    for f in range(frames):
        t = f / frames * 2 * math.pi
        for y in range(size):
            for x in range(size):
                yy = (y + f * size // frames) % size if scroll else y
                v = math.sin(2 * math.pi * (x + yy) / size + t)
                v += 0.6 * math.sin(2 * math.pi * (2 * x - yy) / size - t)
                v += 0.4 * math.sin(2 * math.pi * yy / size + 2 * t + 1.05)
                grain = (hash2(x, yy) - 128) / 128.0
                lum = 178 + v * 26 + grain * 14
                r = clamp(lum * 0.93)
                g = clamp(lum)
                b = clamp(lum * 0.88)
                px[x, y + f * size] = (r, g, b, 208)
    return img


water_texture(16, 32, scroll=False).save(f"{BLOCK_TEX}/toxic_water_still.png")
water_texture(32, 32, scroll=True).save(f"{BLOCK_TEX}/toxic_water_flow.png")
with open(f"{BLOCK_TEX}/toxic_water_still.png.mcmeta", "w") as fh:
    json.dump({"animation": {"frametime": 3}}, fh)
with open(f"{BLOCK_TEX}/toxic_water_flow.png.mcmeta", "w") as fh:
    json.dump({"animation": {"frametime": 2}}, fh)
print("  toxic_water_still 16x512 (32 кадра), toxic_water_flow 32x1024 (32 кадра)")

# ----------------------------------------------------------- JSON-ассеты
print("[4/4] JSON для новых блоков...")


def jwrite(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
    print(f"  json {path.split('alien-invasion/', 1)[1]}")


CUBES = ["infested_sandstone", "infested_terracotta", "infested_snow", "infested_ice"]
for name in CUBES:
    jwrite(f"{ASSETS}/blockstates/{name}.json",
           {"variants": {"": {"model": f"alien-invasion:block/{name}"}}})
    jwrite(f"{ASSETS}/models/block/{name}.json",
           {"parent": "minecraft:block/cube_all",
            "textures": {"all": f"alien-invasion:block/{name}"}})
    jwrite(f"{ASSETS}/models/item/{name}.json",
           {"parent": f"alien-invasion:block/{name}"})
    jwrite(f"{DATA}/loot_table/blocks/{name}.json",
           {"type": "minecraft:block",
            "pools": [{"rolls": 1,
                       "entries": [{"type": "minecraft:item", "name": f"alien-invasion:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

jwrite(f"{ASSETS}/blockstates/alien_tendrils.json",
       {"variants": {"": {"model": "alien-invasion:block/alien_tendrils"}}})
jwrite(f"{ASSETS}/models/block/alien_tendrils.json",
       {"parent": "minecraft:block/cross",
        "textures": {"cross": "alien-invasion:block/alien_tendrils"}})
jwrite(f"{ASSETS}/models/item/alien_tendrils.json",
       {"parent": "minecraft:item/generated",
        "textures": {"layer0": "alien-invasion:block/alien_tendrils"}})

# Трава теперь трёхтекстурная: верх/бок/низ.
jwrite(f"{ASSETS}/models/block/infested_grass.json",
       {"parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": "alien-invasion:block/infested_grass_top",
                     "side": "alien-invasion:block/infested_grass_side",
                     "bottom": "alien-invasion:block/infested_dirt"}})

print("Готово.")
