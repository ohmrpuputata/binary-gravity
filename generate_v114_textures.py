"""Generate handcrafted-looking procedural PNG textures for v1.14 content."""
import json
import os
import random
from PIL import Image

PROJECT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion")
ITEM = os.path.join(ASSETS, "textures", "item")
BLOCK = os.path.join(ASSETS, "textures", "block")
ENTITY = os.path.join(ASSETS, "textures", "entity")
for d in (ITEM, BLOCK, ENTITY):
    os.makedirs(d, exist_ok=True)

BLACK = (11, 13, 17, 255)
STONE = (72, 72, 76, 255)
DEEPSLATE = (45, 47, 54, 255)
GREEN = (110, 238, 78, 255)
GREEN_HI = (190, 255, 120, 255)
CYAN = (72, 220, 235, 255)
CYAN_HI = (180, 255, 255, 255)
PURPLE = (160, 90, 235, 255)
MAGENTA = (240, 70, 200, 255)
ORANGE = (255, 132, 52, 255)
YELLOW = (245, 226, 90, 255)
RED = (230, 56, 56, 255)
METAL = (96, 104, 112, 255)

# Muted "infested" vein palette - desaturated, darker versions of the neon set so
# corrupted blocks read as grimy/sickly instead of eye-searing acid colours.
INF_GREEN = (84, 140, 64, 255)
INF_GREEN_HI = (128, 184, 96, 255)
INF_CYAN = (74, 140, 152, 255)
INF_CYAN_HI = (120, 182, 192, 255)
INF_PURPLE = (116, 86, 150, 255)
INF_MAGENTA = (158, 96, 142, 255)


def img(w=16, h=16, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), fill)


def save(folder, name, image):
    image.save(os.path.join(folder, name + ".png"))
    print("png", name)


def rect(px, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            try:
                px[x, y] = color
            except IndexError:
                pass


def line(px, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    while True:
        try:
            px[x, y] = color
        except IndexError:
            pass
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy


def noise_tile(base, seed, contrast=18):
    random.seed(seed)
    image = img()
    px = image.load()
    for y in range(16):
        for x in range(16):
            n = random.randint(-contrast, contrast)
            px[x, y] = tuple(max(0, min(255, base[i] + n)) for i in range(3)) + (255,)
    return image


def cracks(px, seed, color=(18, 22, 24, 255), count=5):
    random.seed(seed)
    for _ in range(count):
        x = random.randint(0, 15)
        y = random.randint(0, 15)
        for _ in range(random.randint(3, 7)):
            nx = max(0, min(15, x + random.randint(-1, 1)))
            ny = max(0, min(15, y + random.randint(-1, 1)))
            line(px, x, y, nx, ny, color)
            x, y = nx, ny


def glow_veins(px, seed, color, hi, count=5):
    random.seed(seed)
    for _ in range(count):
        x = random.randint(1, 14)
        y = random.randint(1, 14)
        length = random.randint(4, 9)
        for i in range(length):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = hi if i % 4 == 0 else color
                if random.random() < 0.20 and x + 1 < 16:
                    px[x + 1, y] = color
                if random.random() < 0.14 and y + 1 < 16:
                    px[x, y + 1] = color
            x += random.choice([-1, 0, 1])
            y += random.choice([-1, 0, 1])


def block_texture(name, base, seed, vein=GREEN, hi=GREEN_HI, vein_count=5):
    image = noise_tile(base, seed)
    px = image.load()
    cracks(px, seed + 10)
    glow_veins(px, seed + 20, vein, hi, vein_count)
    save(BLOCK, name, image)


def ore_texture(name, base, seed, vein, hi, clusters=5):
    image = noise_tile(base, seed, 14)
    px = image.load()
    cracks(px, seed + 2, (28, 30, 34, 255), 4)
    random.seed(seed + 5)
    for _ in range(clusters):
        cx = random.randint(2, 13)
        cy = random.randint(2, 13)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) <= random.randint(1, 2):
                    x, y = cx + dx, cy + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        px[x, y] = hi if random.random() < 0.35 else vein
    save(BLOCK, name, image)


def item_crystal(name, color, hi, seed):
    image = img()
    px = image.load()
    points = [(8, 1), (12, 6), (10, 14), (5, 14), (3, 6)]
    random.seed(seed)
    for y in range(2, 15):
        for x in range(3, 13):
            if abs(x - 8) + abs(y - 8) < 8:
                shade = random.randint(-20, 20)
                px[x, y] = tuple(max(0, min(255, color[i] + shade)) for i in range(3)) + (255,)
    for a, b in zip(points, points[1:] + [points[0]]):
        line(px, a[0], a[1], b[0], b[1], hi)
    px[7, 5] = hi
    px[9, 8] = hi
    save(ITEM, name, image)


def item_ingot(name, color, hi, seed):
    image = img()
    px = image.load()
    random.seed(seed)
    for y in range(6, 12):
        for x in range(3, 13):
            if 4 <= x + (y - 6) // 2 <= 14:
                n = random.randint(-16, 16)
                px[x, y] = tuple(max(0, min(255, color[i] + n)) for i in range(3)) + (255,)
    for x in range(4, 12):
        px[x, 6] = hi
    save(ITEM, name, image)


def item_tool(name, head, accent, seed, tool):
    """Proper shaded 16x16 tool sprite: wooden handle + metal head with a highlight
    edge, a darker shadow side, and a 1px dark outline. Replaces the old line-art."""
    image = img()
    px = image.load()
    handle = (110, 78, 48, 255)
    handle_dk = (74, 50, 30, 255)
    handle_hi = (146, 110, 72, 255)

    def shade(c, f):
        return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
                max(0, min(255, int(c[2] * f))), 255)

    head_dk = shade(head, 0.6)

    def p(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c

    def vline(x, y0, y1, c):
        for y in range(y0, y1 + 1):
            p(x, y, c)

    def hline(y, x0, x1, c):
        for x in range(x0, x1 + 1):
            p(x, y, c)

    if tool == "sword":
        vline(7, 12, 15, handle); vline(8, 12, 15, handle_dk); p(7, 15, handle_hi)
        hline(11, 5, 10, head_dk)            # crossguard
        for y in range(2, 11):               # blade
            p(7, y, accent); p(8, y, head)
        p(8, 10, head_dk)
    elif tool == "pickaxe":
        vline(8, 5, 15, handle); vline(9, 5, 15, handle_dk)
        hline(3, 4, 11, head); hline(2, 5, 10, accent)   # arched head
        p(3, 4, head); p(12, 4, head); p(3, 5, head_dk); p(12, 5, head_dk)  # prongs
    elif tool == "axe":
        vline(8, 4, 15, handle); vline(9, 4, 15, handle_dk)
        for y in range(2, 8):
            hline(y, 4, 8, head)             # head block
        vline(3, 3, 6, accent)               # blade edge
        hline(2, 4, 8, accent)
        for y in range(3, 8):
            p(8, y, head_dk)
    elif tool == "shovel":
        vline(7, 6, 15, handle); vline(8, 6, 15, handle_dk)
        for y in range(2, 6):
            hline(y, 6, 9, head)             # scoop
        hline(2, 6, 9, accent); p(6, 5, head_dk); p(9, 5, head_dk)
    else:  # hoe
        vline(8, 4, 15, handle); vline(9, 4, 15, handle_dk)
        hline(2, 4, 9, head); hline(2, 4, 9, accent)
        vline(4, 3, 4, head_dk)              # the bend

    # 1px dark outline
    src = image.copy(); sp = src.load()
    outline = (18, 16, 22, 255)
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    px[x, y] = outline
                    break
    save(ITEM, name, image)


def item_module(name, color, hi, seed):
    image = img()
    px = image.load()
    rect(px, 3, 3, 13, 13, (38, 44, 50, 255))
    rect(px, 5, 5, 11, 11, color)
    px[6, 6] = hi
    px[9, 9] = hi
    cracks(px, seed, (10, 12, 14, 255), 3)
    save(ITEM, name, image)


def simple_icon(name, color, hi, seed):
    image = img()
    px = image.load()
    random.seed(seed)
    for y in range(4, 13):
        for x in range(4, 13):
            if (x - 8) * (x - 8) + (y - 8) * (y - 8) < 26:
                n = random.randint(-22, 22)
                px[x, y] = tuple(max(0, min(255, color[i] + n)) for i in range(3)) + (255,)
    for _ in range(8):
        px[random.randint(4, 12), random.randint(4, 12)] = hi
    save(ITEM, name, image)


def bucket_icon():
    image = img()
    px = image.load()
    rect(px, 4, 5, 12, 14, (124, 130, 136, 255))
    rect(px, 5, 7, 11, 13, (82, 180, 50, 220))
    rect(px, 6, 8, 10, 10, GREEN_HI)
    line(px, 4, 5, 11, 5, (210, 215, 220, 255))
    save(ITEM, "toxic_water_bucket", image)


def borer_entity():
    image = Image.new("RGBA", (64, 48), (0, 0, 0, 0))
    px = image.load()
    random.seed(114)
    # Hull: two-tone gunmetal with light grain (low contrast - looks like painted steel).
    for y in range(48):
        for x in range(64):
            base = (62, 68, 74) if y < 28 else (40, 42, 46)
            n = random.randint(-7, 7)
            px[x, y] = tuple(max(0, min(255, base[i] + n)) for i in range(3)) + (255,)
    # Recessed panel lines + rivets so the hull reads as plated metal.
    for y in (8, 16, 24):
        for x in range(2, 62):
            r, g, b, _ = px[x, y]
            px[x, y] = (max(0, r - 20), max(0, g - 20), max(0, b - 20), 255)
    for (rx, ry) in [(4, 4), (59, 4), (4, 26), (59, 26), (32, 5)]:
        px[rx, ry] = (152, 158, 164, 255)
    # Glowing fuel capsules along the top (muted cyan, not neon).
    for x0 in (10, 24, 38, 52):
        rect(px, x0, 5, x0 + 6, 14, (26, 46, 50, 255))     # dark casing
        rect(px, x0 + 1, 6, x0 + 5, 13, INF_CYAN)          # coolant
        rect(px, x0 + 2, 7, x0 + 4, 11, INF_CYAN_HI)       # glow core
    # Front drill: a shaded conical bit with spiral flutes and a bright tip.
    cx, cy = 32, 24
    for y in range(16, 33):
        for x in range(22, 43):
            d = abs(x - cx) + abs(y - cy)
            if d < 14:
                s = 150 - d * 8
                px[x, y] = (max(46, s), max(40, s - 8), max(32, s - 16), 255)
    for a in range(0, 13):                                  # spiral grooves
        px[min(63, cx + a), max(0, cy - a // 2)] = (26, 24, 20, 255)
        px[max(0, cx - a), min(47, cy + a // 2)] = (26, 24, 20, 255)
    px[cx, cy] = (214, 204, 172, 255)                       # tip highlight
    px[cx + 1, cy] = (192, 184, 152, 255)
    px[cx, cy + 1] = (192, 184, 152, 255)
    # Dirty caterpillar tracks along the bottom with a regular tread pattern.
    for y in range(34, 45):
        for x in range(4, 60):
            tread = (x // 2) % 2 == 0
            px[x, y] = (54, 50, 42, 255) if tread else (24, 24, 26, 255)
    save(ENTITY, "borer", image)


def fluid_textures():
    still = Image.new("RGBA", (16, 16), (74, 151, 44, 190))
    flow = Image.new("RGBA", (16, 16), (64, 138, 50, 190))
    for seed, image in ((7, still), (9, flow)):
        px = image.load()
        random.seed(seed)
        for _ in range(34):
            x, y = random.randint(0, 15), random.randint(0, 15)
            px[x, y] = (170, 255, 110, 210)
        for i in range(16):
            px[i, (i + seed) % 16] = (118, 218, 70, 220)
    save(BLOCK, "toxic_water_still", still)
    save(BLOCK, "toxic_water_flow", flow)
    for name in ("toxic_water_still", "toxic_water_flow"):
        path = os.path.join(BLOCK, name + ".png.mcmeta")
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"animation": {"frametime": 4}}, f, indent=2)
            f.write("\n")
        print("mcmeta", name)


def generate_blocks():
    # Infested blocks use the MUTED vein palette + sparser veins (count 3) so the
    # corruption looks grimy/sickly rather than neon "вырвиглаз".
    block_texture("infested_dirt", (74, 54, 40, 255), 101, INF_GREEN, INF_GREEN_HI, 3)
    block_texture("infested_sand", (150, 136, 88, 255), 102, INF_CYAN, INF_CYAN_HI, 3)
    block_texture("infested_gravel", (90, 88, 86, 255), 103, INF_GREEN, INF_GREEN_HI, 3)
    block_texture("infested_clay", (104, 118, 126, 255), 104, INF_CYAN, INF_CYAN_HI, 3)
    block_texture("infested_deepslate", DEEPSLATE, 105, INF_PURPLE, INF_MAGENTA, 3)
    block_texture("infested_netherrack", (96, 34, 34, 255), 106, INF_GREEN, INF_GREEN_HI, 3)
    block_texture("infested_planks", (88, 62, 38, 255), 107, INF_CYAN, INF_CYAN_HI, 3)
    block_texture("infested_log", (84, 56, 36, 255), 108, INF_GREEN, INF_GREEN_HI, 3)
    block_texture("infested_log_top", (78, 52, 35, 255), 109, INF_GREEN, INF_GREEN_HI, 3)
    block_texture("infested_leaves", (46, 78, 50, 255), 110, INF_GREEN, INF_GREEN_HI, 4)
    block_texture("dead_infested_crop", (66, 46, 34, 255), 111, INF_PURPLE, INF_GREEN_HI, 3)
    ore_texture("uranium_ore", STONE, 201, GREEN, GREEN_HI, 6)
    ore_texture("deepslate_uranium_ore", DEEPSLATE, 202, GREEN, GREEN_HI, 6)
    ore_texture("xenocrystal_ore", STONE, 203, CYAN, CYAN_HI, 5)
    ore_texture("bio_vein_ore", STONE, 204, (140, 220, 95, 255), GREEN_HI, 8)
    ore_texture("plasma_ore", DEEPSLATE, 205, ORANGE, YELLOW, 5)
    ore_texture("iridium_ore", DEEPSLATE, 206, (195, 185, 210, 255), CYAN_HI, 5)
    ore_texture("dark_matter_ore", DEEPSLATE, 207, PURPLE, MAGENTA, 4)
    block_texture("black_market_terminal", (35, 40, 46, 255), 301, CYAN, CYAN_HI, 4)
    block_texture("purifier_station", (58, 82, 76, 255), 302, CYAN, GREEN_HI, 4)
    block_texture("ore_washer", (72, 82, 92, 255), 303, CYAN, CYAN_HI, 4)
    block_texture("radiation_forge", (84, 72, 54, 255), 304, GREEN, YELLOW, 5)
    block_texture("alien_recycler", (44, 70, 62, 255), 305, GREEN, CYAN_HI, 4)
    block_texture("blueprint_table", (70, 54, 42, 255), 306, CYAN, CYAN_HI, 3)
    block_texture("warning_lamp", (42, 42, 34, 255), 401, YELLOW, GREEN_HI, 4)
    block_texture("cracked_alien_pipe", (50, 64, 64, 255), 402, CYAN, CYAN_HI, 3)
    block_texture("toxic_barrel", (58, 70, 52, 255), 403, GREEN, GREEN_HI, 6)
    block_texture("broken_lab_crate", (82, 72, 62, 255), 404, CYAN, CYAN_HI, 3)
    block_texture("contaminated_bones", (130, 126, 104, 255), 406, GREEN, CYAN_HI, 3)


def generate_items():
    simple_icon("alien_scrap", METAL, CYAN_HI, 501)
    simple_icon("cosmic_credit", (194, 168, 74, 255), CYAN_HI, 502)
    simple_icon("uranium_dust", (88, 190, 64, 255), GREEN_HI, 503)
    item_ingot("uranium_rod", (84, 168, 68, 255), GREEN_HI, 504)
    item_crystal("xenocrystal", CYAN, CYAN_HI, 505)
    simple_icon("bio_fiber", (92, 188, 74, 255), GREEN_HI, 506)
    item_crystal("plasma_core", ORANGE, YELLOW, 507)
    item_ingot("iridium_plate", (164, 162, 180, 255), CYAN_HI, 508)
    item_crystal("dark_matter_shard", PURPLE, MAGENTA, 509)
    item_module("drill_fuel_cell", GREEN, GREEN_HI, 510)
    item_module("reinforced_drill_head", METAL, CYAN_HI, 511)
    item_module("lava_cooling_module", ORANGE, YELLOW, 512)
    item_module("ore_filter_module", CYAN, CYAN_HI, 513)
    item_module("toxic_seal_module", GREEN, GREEN_HI, 514)
    item_module("storage_bay_module", (122, 92, 60, 255), YELLOW, 515)
    item_module("headlamp_module", YELLOW, CYAN_HI, 516)
    bucket_icon()
    for tier, head, accent, seed in [
        ("uranium", (120, 196, 96, 255), (176, 232, 140, 255), 530),
        ("plasma", ORANGE, YELLOW, 540),
        ("iridium", (180, 182, 205, 255), (228, 230, 245, 255), 550),
    ]:
        for i, tool in enumerate(("sword", "pickaxe", "axe", "shovel", "hoe")):
            item_tool(f"{tier}_{tool}", head, accent, seed + i, tool)
    item_module("radiation_drill_head", GREEN, YELLOW, 560)
    item_module("purifier_drill_head", CYAN, GREEN_HI, 561)
    item_module("toxic_water_pump", GREEN, CYAN_HI, 562)
    item_module("geiger_counter", (54, 62, 60, 255), GREEN_HI, 563)
    item_module("portable_purifier", CYAN, CYAN_HI, 564)
    simple_icon("rad_pills", (220, 230, 180, 255), GREEN_HI, 565)
    simple_icon("infection_pills", (230, 190, 220, 255), MAGENTA, 570)
    item_module("bio_filter_mask", (42, 72, 60, 255), GREEN_HI, 566)
    simple_icon("contaminated_food", (96, 72, 46, 255), GREEN_HI, 567)
    simple_icon("purified_water_flask", (96, 180, 230, 255), CYAN_HI, 568)
    simple_icon("dead_infested_crop", (78, 52, 34, 255), GREEN_HI, 569)


def main():
    generate_blocks()
    generate_items()
    fluid_textures()
    borer_entity()
    print("v1.14 textures generated.")


if __name__ == "__main__":
    main()
