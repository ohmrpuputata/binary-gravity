"""
Clean re-texture pass for Alien Invasion.

Infested  = vanilla (or clean mod) texture + luminance-preserving PURPLE tint.
            NO veins, NO green/teal specks, NO pustules. Optional very-rare,
            rotated spore stamps (--spores). Ores keep their mineral, only the
            stone matrix is tinted.

Blood     = clean base + small, dark, dried-blood splatter (separate module).

Outputs to a STAGING dir; nothing touches the repo until a copy step is run
explicitly. Always review the contact sheet before promoting.
"""
import io, os, sys, zipfile, random, argparse
from PIL import Image

HOME = os.path.expanduser("~")
JAR = os.path.join(HOME, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client.jar")
REPO = "src/main/resources/assets/alien-invasion/textures/block"
STAGING = "build/texture-audit/staging/block"

_zip = zipfile.ZipFile(JAR)

def vanilla(name):
    data = _zip.read(f"assets/minecraft/textures/block/{name}.png")
    return Image.open(io.BytesIO(data)).convert("RGBA")

def repo(name):
    return Image.open(os.path.join(REPO, name)).convert("RGBA")

def clamp(v):
    return 0 if v < 0 else (255 if v > 255 else int(v))

PURPLE = (150, 70, 165)

# material -> tint strength t, darken dark
MAT = {
    "STONE":   dict(t=0.66, dark=0.90),
    "SOFT":    dict(t=0.58, dark=0.92),
    "SAND":    dict(t=0.40, dark=0.96),
    "WOOD":    dict(t=0.42, dark=0.85),
    "WOOD_DK": dict(t=0.46, dark=0.82),
    "LEAVES":  dict(t=0.60, dark=0.92),
    "GRASS":   dict(t=0.74, dark=0.90),
    "ICE":     dict(t=0.42, dark=1.00),
    "GLASS":   dict(t=0.42, dark=1.00),
    "WOOL":    dict(t=0.55, dark=0.95),
    "METAL":   dict(t=0.34, dark=0.92),
    "BLOCK":   dict(t=0.45, dark=0.90),
    "ALIEN":   dict(t=0.30, dark=0.94),
    "MACHINE": dict(t=0.30, dark=0.92),
    "BONE":    dict(t=0.30, dark=0.94),
    "ORE_MIN": dict(t=0.12, dark=0.96),
}

def tint_pixel(r, g, b, t, dark, tone=PURPLE):
    L = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    pr, pg, pb = tone[0] * L, tone[1] * L, tone[2] * L
    nr = (r * (1 - t) + pr * t) * dark
    ng = (g * (1 - t) + pg * t) * dark
    nb = (b * (1 - t) + pb * t) * dark
    return clamp(nr), clamp(ng), clamp(nb)

def tint(im, mat):
    p = MAT[mat]
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            nr, ng, nb = tint_pixel(r, g, b, p["t"], p["dark"])
            px[x, y] = (nr, ng, nb, a)
    return im

def color_dist(a, b):
    return ((a[0]-b[0])**2 + (a[1]-b[1])**2 + (a[2]-b[2])**2) ** 0.5

def tint_ore(ore_im, matrix_im, thresh=55):
    """Keep mineral pixels (far from matrix color), tint only the matrix."""
    op = ore_im.load()
    mp = matrix_im.load()
    w, h = ore_im.size
    mw, mh = matrix_im.size
    full = MAT["STONE"]
    keep = MAT["ORE_MIN"]
    for y in range(h):
        for x in range(w):
            r, g, b, a = op[x, y]
            if a == 0:
                continue
            mr, mg, mb, ma = mp[x % mw, y % mh]
            if color_dist((r, g, b), (mr, mg, mb)) > thresh:
                nr, ng, nb = tint_pixel(r, g, b, keep["t"], keep["dark"])
            else:
                nr, ng, nb = tint_pixel(r, g, b, full["t"], full["dark"])
            op[x, y] = (nr, ng, nb, a)
    return ore_im

# ---- optional spores -------------------------------------------------------
def spore_stamp(rng):
    """A tiny 5x5 hand-shaped spore cluster, random rotation + mirror."""
    base = Image.new("RGBA", (5, 5), (0, 0, 0, 0))
    px = base.load()
    core = (168, 92, 188)
    halo = (120, 60, 150)
    # a small irregular cluster around center
    cells = [(2, 2, core, 220), (2, 1, core, 170), (3, 2, core, 175),
             (1, 2, halo, 130), (2, 3, halo, 120), (3, 3, halo, 90)]
    if rng.random() < 0.5:
        cells.append((1, 3, halo, 80))
    for x, y, c, al in cells:
        px[x, y] = (c[0], c[1], c[2], al)
    # rotation/mirror so no two read alike and no tiling seam
    for _ in range(rng.randint(0, 3)):
        base = base.transpose(Image.ROTATE_90)
    if rng.random() < 0.5:
        base = base.transpose(Image.FLIP_LEFT_RIGHT)
    return base

def add_spores(im, seed, density=0.45, max_n=1):
    rng = random.Random("spore:" + seed)
    if rng.random() > density:
        return im
    w, h = im.size
    px = im.load()
    for _ in range(rng.randint(1, max_n)):
        st = spore_stamp(rng)
        sw, sh = st.size
        x = rng.randint(1, w - sw - 1)
        y = rng.randint(1, h - sh - 1)
        # only over opaque pixels
        if px[x + sw // 2, y + sh // 2][3] == 0:
            continue
        im.alpha_composite(st, (x, y))
    return im

# ---- source mapping --------------------------------------------------------
# target_filename -> (kind, source, material[, matrix])
V, M = "vanilla", "mod"
SPEC = {
    # ground / stone
    "infested_stone.png":            (V, "stone", "STONE"),
    "infested_deepslate.png":        (V, "deepslate", "STONE"),
    "infested_gravel.png":           (V, "gravel", "STONE"),
    "infested_netherrack.png":       (V, "netherrack", "STONE"),
    "infested_stone_bricks.png":     (V, "stone_bricks", "STONE"),
    "infested_terracotta.png":       (V, "terracotta", "STONE"),
    "infested_sandstone.png":        (V, "sandstone", "STONE"),
    "infested_dirt.png":             (V, "dirt", "SOFT"),
    "infested_clay.png":             (V, "clay", "SOFT"),
    "infested_sand.png":             (V, "sand", "SAND"),
    "infested_snow.png":             (V, "snow", "ICE"),
    "infested_ice.png":              (V, "ice", "ICE"),
    "infested_glass.png":            (V, "glass", "GLASS"),
    "infested_wool.png":             (V, "white_wool", "WOOL"),
    "infested_leaves.png":           (V, "oak_leaves", "LEAVES"),
    # grass
    "infested_grass.png":            (V, "grass_block_top", "GRASS"),
    "infested_grass_top.png":        (V, "grass_block_top", "GRASS"),
    "infested_grass_side.png":       (V, "grass_block_side", "GRASS"),
    "infested_grass_side_overlay.png": (V, "grass_block_side_overlay", "GRASS"),
    # wood
    "infested_log.png":              (V, "oak_log", "WOOD"),
    "infested_log_top.png":          (V, "oak_log_top", "WOOD"),
    "infested_planks.png":           (V, "oak_planks", "WOOD"),
    "infested_door_top.png":         (V, "oak_door_top", "WOOD"),
    "infested_door_bottom.png":      (V, "oak_door_bottom", "WOOD"),
    "infested_trapdoor.png":         (V, "oak_trapdoor", "WOOD"),
    "infested_crafting_table_top.png":   (V, "crafting_table_top", "WOOD"),
    "infested_crafting_table_front.png": (V, "crafting_table_front", "WOOD"),
    "infested_crafting_table_side.png":  (V, "crafting_table_side", "WOOD"),
    "infested_barrel_top.png":       (V, "barrel_top", "WOOD"),
    "infested_barrel_side.png":      (V, "barrel_side", "WOOD"),
    "infested_barrel_bottom.png":    (V, "barrel_bottom", "WOOD"),
    "infested_cartography_table_top.png":   (V, "cartography_table_top", "WOOD"),
    "infested_cartography_table_side1.png": (V, "cartography_table_side1", "WOOD"),
    "infested_cartography_table_side2.png": (V, "cartography_table_side2", "WOOD"),
    "infested_cartography_table_side3.png": (V, "cartography_table_side3", "WOOD"),
    "infested_smithing_table_top.png":    (V, "smithing_table_top", "WOOD_DK"),
    "infested_smithing_table_front.png":  (V, "smithing_table_front", "WOOD_DK"),
    "infested_smithing_table_side.png":   (V, "smithing_table_side", "WOOD_DK"),
    "infested_smithing_table_bottom.png": (V, "smithing_table_bottom", "WOOD_DK"),
    "infested_fletching_table_top.png":   (V, "fletching_table_top", "WOOD"),
    "infested_fletching_table_front.png": (V, "fletching_table_front", "WOOD"),
    "infested_fletching_table_side.png":  (V, "fletching_table_side", "WOOD"),
    "infested_loom_top.png":         (V, "loom_top", "WOOD"),
    "infested_loom_front.png":       (V, "loom_front", "WOOD"),
    "infested_loom_side.png":        (V, "loom_side", "WOOD"),
    "infested_loom_bottom.png":      (V, "loom_bottom", "WOOD"),
    "infested_stonecutter_top.png":    (V, "stonecutter_top", "STONE"),
    "infested_stonecutter_bottom.png": (V, "stonecutter_bottom", "WOOD"),
    "infested_stonecutter_side.png":   (V, "stonecutter_side", "STONE"),
    "infested_stonecutter_saw.png":    (V, "stonecutter_saw", "METAL"),
    "infested_grindstone_round.png": (V, "grindstone_round", "STONE"),
    "infested_grindstone_side.png":  (V, "grindstone_side", "WOOD"),
    "infested_grindstone_pivot.png": (V, "grindstone_pivot", "WOOD"),
    "infested_grindstone_leg.png":   (V, "stone", "STONE"),  # no vanilla leg tex
    # ores (mineral preserved, matrix tinted)
    "infested_diamond_ore.png":         (V, "diamond_ore", "ORE", "stone"),
    "infested_redstone_ore.png":        (V, "redstone_ore", "ORE", "stone"),
    "infested_cosmic_crystal_ore.png":  (M, "cosmic_ore.png", "ORE", "stone"),
    "infested_dark_matter_ore.png":     (M, "dark_matter_ore.png", "ORE", "deepslate"),
    "infested_palladium_ore.png":       (M, "palladium_ore.png", "ORE", "stone"),
    "infested_platinum_ore.png":        (M, "platinum_ore.png", "ORE", "stone"),
    # mod blocks (gentle unify from clean base)
    "infested_palladium_block.png":     (M, "palladium_block.png", "METAL"),
    "infested_platinum_block.png":      (M, "platinum_block.png", "METAL"),
    "infested_pure_radiation_block.png":(M, "pure_radiation_block.png", "BLOCK"),
    "infested_alien_beacon.png":        (M, "alien_beacon.png", "ALIEN"),
    "infested_alien_flesh.png":         (M, "alien_flesh.png", "ALIEN"),
    "infested_alien_heart.png":         (M, "alien_heart.png", "ALIEN"),
    "infested_alien_hive.png":          (M, "alien_hive.png", "ALIEN"),
    "infested_alien_portal.png":        (M, "alien_portal.png", "ALIEN"),
    # alien_residue family is ANIMATED (16x64 + .mcmeta) -> owned by
    # tools/design_alien_residue.py; intentionally excluded here.
    "infested_alien_stash.png":         (M, "alien_stash.png", "ALIEN"),
    "infested_alien_tendrils.png":      (M, "alien_tendrils.png", "ALIEN"),
    "infested_swarm_beacon.png":        (M, "swarm_beacon.png", "ALIEN"),
    "infested_black_market_terminal.png":(M, "black_market_terminal.png", "MACHINE"),
    "infested_blueprint_table.png":     (M, "blueprint_table.png", "MACHINE"),
    "infested_broken_lab_crate.png":    (M, "broken_lab_crate.png", "MACHINE"),
    "infested_cracked_alien_pipe.png":  (M, "cracked_alien_pipe.png", "MACHINE"),
    "infested_ore_washer.png":          (M, "ore_washer.png", "MACHINE"),
    "infested_planet_reactor.png":      (M, "planet_reactor_side.png", "MACHINE"),
    "infested_planet_reactor_top.png":  (M, "planet_reactor_top.png", "MACHINE"),
    "infested_plasma_turret.png":       (M, "plasma_turret.png", "MACHINE"),
    "infested_purifier.png":            (M, "purifier.png", "MACHINE"),
    "infested_purifier_station.png":    (M, "purifier_station.png", "MACHINE"),
    "infested_radio_transmitter.png":   (M, "radio_transmitter.png", "MACHINE"),
    "infested_toxic_barrel.png":        (M, "toxic_barrel.png", "MACHINE"),
    "infested_warning_lamp.png":        (M, "warning_lamp.png", "MACHINE"),
    "infested_contaminated_bones.png":  (M, "contaminated_bones.png", "BONE"),
}

SPORE_BLOCKS = {  # only these get the rare spore option
    "infested_stone.png", "infested_deepslate.png", "infested_dirt.png",
    "infested_netherrack.png", "infested_stone_bricks.png", "infested_leaves.png",
    "infested_terracotta.png", "infested_gravel.png", "infested_log.png",
    "infested_planks.png",
}

# ---- blood --------------------------------------------------------------
def grow(rng, n):
    """Irregular pixel blob: list of (dx,dy) offsets incl (0,0)."""
    cells = {(0, 0)}
    frontier = [(0, 0)]
    while frontier and len(cells) < n:
        cx, cy = frontier.pop(rng.randrange(len(frontier)))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if rng.random() < 0.55:
                nc = (cx + dx, cy + dy)
                if nc not in cells:
                    cells.add(nc)
                    frontier.append(nc)
    return list(cells)

# dried-blood palette (dark crimson, a touch desaturated)
BLOOD_CORE = (116, 14, 18)
BLOOD_MID = (92, 10, 14)
BLOOD_EDGE = (72, 8, 11)
BLOOD_DROP = (104, 12, 16)

def add_blood(base, seed):
    """Small, dark, irregular splatter over opaque pixels only. Varied by seed."""
    rng = random.Random(seed)
    w, h = base.size
    bp = base.load()
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    lp = layer.load()

    def put(x, y, col, al):
        if 0 <= x < w and 0 <= y < h and bp[x, y][3] > 0:
            if al > lp[x, y][3]:
                lp[x, y] = (col[0], col[1], col[2], al)

    for _ in range(rng.randint(1, 2)):
        ox, oy = rng.randint(3, w - 4), rng.randint(3, h - 4)
        cells = grow(rng, rng.randint(8, 16))
        cellset = set(cells)
        for dx, dy in cells:
            nb = sum((dx + ax, dy + ay) in cellset for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if nb >= 4:
                put(ox + dx, oy + dy, BLOOD_CORE, 216)
            elif nb >= 2:
                put(ox + dx, oy + dy, BLOOD_MID, 200)
            else:
                put(ox + dx, oy + dy, BLOOD_EDGE, 150)
    for _ in range(rng.randint(2, 5)):
        x, y = rng.randint(1, w - 2), rng.randint(1, h - 2)
        put(x, y, BLOOD_DROP, 175)
        if rng.random() < 0.4:
            put(x + rng.choice([-1, 1]), y, BLOOD_EDGE, 120)
    base.alpha_composite(layer)
    return base

def make_pool():
    """Standalone blood_pool decal: a clear but dark, dried puddle, transparent bg."""
    rng = random.Random("pool2")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    cx, cy, rx, ry = 8.0, 8.0, 6.0, 4.4
    for y in range(16):
        for x in range(16):
            nx = (x + 0.5 - cx) / rx
            ny = (y + 0.5 - cy) / ry
            d = nx * nx + ny * ny + rng.uniform(-0.13, 0.13)  # irregular edge
            if d <= 1.0:
                if d < 0.34:
                    p[x, y] = (112, 13, 17, 226)   # wet centre
                elif d < 0.72:
                    p[x, y] = (88, 9, 13, 210)
                else:
                    p[x, y] = (62, 7, 10, 172)     # thin drying rim
    # a couple of outlying droplets for a splashed look
    for _ in range(rng.randint(2, 4)):
        a = rng.uniform(0, 6.28)
        x = int(cx + (rx + rng.uniform(0.5, 2.0)) * 0.55 * (1 if rng.random() < .5 else -1))
        y = int(cy + rng.uniform(-ry, ry))
        if 0 <= x < 16 and 0 <= y < 16 and p[x, y][3] == 0:
            p[x, y] = (96, 11, 15, 165)
    return im

def build_blood():
    os.makedirs(STAGING, exist_ok=True)
    made, missing = 0, []
    for target, spec in SPEC.items():
        kind, src = spec[0], spec[1]
        stem = target[len("infested_"):]  # e.g. "planks.png"
        try:
            clean = (vanilla(src) if kind == V else repo(src)).copy()
        except (KeyError, FileNotFoundError):
            missing.append("bloody_" + stem)
            clean = None
        if clean is not None:
            add_blood(clean, "b:" + stem).save(os.path.join(STAGING, "bloody_" + stem))
            made += 1
        infp = os.path.join(STAGING, target)
        if os.path.exists(infp):
            inf = Image.open(infp).convert("RGBA").copy()
            add_blood(inf, "bi:" + stem).save(os.path.join(STAGING, "bloody_infested_" + stem))
            made += 1
        else:
            missing.append("bloody_infested_" + stem)
    make_pool().save(os.path.join(STAGING, "blood_pool.png"))
    print(f"blood: wrote {made} (+pool), missing {len(missing)}: {missing}")

def build_infested(spores=False):
    os.makedirs(STAGING, exist_ok=True)
    made, missing = 0, []
    for target, spec in SPEC.items():
        kind, src, mat = spec[0], spec[1], spec[2]
        try:
            base = vanilla(src) if kind == V else repo(src)
        except (KeyError, FileNotFoundError):
            missing.append(target)
            continue
        base = base.copy()
        if mat == "ORE":
            matrix = vanilla(spec[3])
            out = tint_ore(base, matrix)
        else:
            out = tint(base, mat)
        if spores and target in SPORE_BLOCKS:
            out = add_spores(out, target)
        out.save(os.path.join(STAGING, target))
        made += 1
    print(f"infested: wrote {made}, missing {len(missing)}: {missing}")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--spores", action="store_true")
    ap.add_argument("--mode", default="infested")
    a = ap.parse_args()
    if a.mode == "infested":
        build_infested(spores=a.spores)
    elif a.mode == "blood":
        build_blood()
    elif a.mode == "all":
        build_infested(spores=a.spores)
        build_blood()
