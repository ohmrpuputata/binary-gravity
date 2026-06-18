"""Repair texture artifacts introduced by generated texture passes.

This keeps armor UV layouts from git HEAD and recolors only existing opaque
pixels, then rebuilds bloody block variants from their true base textures with
small per-texture decals instead of one repeated stamp.
"""
from __future__ import annotations

import hashlib
import io
import os
import random
import subprocess
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BLOCK_DIR = ROOT / "src/main/resources/assets/alien-invasion/textures/block"
ARMOR_DIR = ROOT / "src/main/resources/assets/alien-invasion/textures/models/armor"
REPORT_DIR = ROOT / "build/texture-fix-preview"

MINECRAFT_VERSION = "1.21.1"
MINECRAFT_JARS = [
    Path.home() / f".gradle/caches/fabric-loom/{MINECRAFT_VERSION}/minecraft-client.jar",
    ROOT / f".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-8bc85877dd/{MINECRAFT_VERSION}-loom.mappings.1_21_1.layered+hash.2198-v2/minecraft-merged-8bc85877dd-{MINECRAFT_VERSION}-loom.mappings.1_21_1.layered+hash.2198-v2.jar",
    Path(os.environ.get("APPDATA", "")) / f".minecraft/versions/{MINECRAFT_VERSION}/{MINECRAFT_VERSION}.jar",
]

VANILLA_NAME_MAP = {
    "barrel_bottom": "barrel_bottom",
    "barrel_side": "barrel_side",
    "barrel_top": "barrel_top",
    "cartography_table_side1": "cartography_table_side1",
    "cartography_table_side2": "cartography_table_side2",
    "cartography_table_side3": "cartography_table_side3",
    "cartography_table_top": "cartography_table_top",
    "clay": "clay",
    "crafting_table_front": "crafting_table_front",
    "crafting_table_side": "crafting_table_side",
    "crafting_table_top": "crafting_table_top",
    "deepslate": "deepslate",
    "diamond_ore": "diamond_ore",
    "dirt": "dirt",
    "door_bottom": "oak_door_bottom",
    "door_top": "oak_door_top",
    "fletching_table_front": "fletching_table_front",
    "fletching_table_side": "fletching_table_side",
    "fletching_table_top": "fletching_table_top",
    "glass": "glass",
    "grass": "grass_block_top",
    "grass_side": "grass_block_side",
    "grass_side_overlay": "grass_block_side_overlay",
    "grass_top": "grass_block_top",
    "gravel": "gravel",
    "grindstone_leg": "grindstone_pivot",
    "grindstone_pivot": "grindstone_pivot",
    "grindstone_round": "grindstone_round",
    "grindstone_side": "grindstone_side",
    "ice": "ice",
    "leaves": "oak_leaves",
    "log": "oak_log",
    "log_top": "oak_log_top",
    "loom_bottom": "loom_bottom",
    "loom_front": "loom_front",
    "loom_side": "loom_side",
    "loom_top": "loom_top",
    "netherrack": "netherrack",
    "planks": "oak_planks",
    "redstone_ore": "redstone_ore",
    "sand": "sand",
    "sandstone": "sandstone",
    "smithing_table_bottom": "smithing_table_bottom",
    "smithing_table_front": "smithing_table_front",
    "smithing_table_side": "smithing_table_side",
    "smithing_table_top": "smithing_table_top",
    "snow": "snow",
    "stone": "stone",
    "stone_bricks": "stone_bricks",
    "stonecutter_bottom": "stonecutter_bottom",
    "stonecutter_saw": "stonecutter_saw",
    "stonecutter_side": "stonecutter_side",
    "stonecutter_top": "stonecutter_top",
    "terracotta": "terracotta",
    "trapdoor": "oak_trapdoor",
    "wool": "white_wool",
}

LOCAL_NAME_MAP = {
    "cosmic_crystal_ore": "cosmic_ore",
    "planet_reactor": "planet_reactor_side",
}

BLOOD = {
    "dark": (68, 4, 10, 255),
    "mid": (120, 12, 20, 255),
    "wet": (168, 18, 28, 255),
    "dry": (84, 22, 24, 255),
}

LEAF_DARK = (25, 70, 25)
LEAF_MID = (45, 116, 37)
LEAF_LIGHT = (90, 162, 62)
GRASS_DARK = (42, 86, 31)
GRASS_MID = (83, 142, 50)
GRASS_LIGHT = (123, 183, 72)

ARMOR_PALETTES = {
    "alien_hazmat": {
        "dark": (48, 46, 24), "mid": (178, 143, 20), "light": (248, 222, 62),
        "accent": (36, 41, 38), "energy": (108, 226, 255),
    },
    "alien_chem": {
        "dark": (25, 54, 38), "mid": (58, 141, 78), "light": (130, 224, 106),
        "accent": (190, 236, 84), "energy": (158, 255, 118),
    },
    "platinum": {
        "dark": (73, 83, 91), "mid": (156, 174, 184), "light": (224, 236, 238),
        "accent": (85, 176, 222), "energy": (148, 230, 255),
    },
    "palladium": {
        "dark": (79, 65, 58), "mid": (158, 137, 121), "light": (226, 211, 190),
        "accent": (134, 42, 38), "energy": (220, 92, 78),
    },
    "emeradium": {
        "dark": (39, 76, 55), "mid": (84, 156, 98), "light": (174, 238, 146),
        "accent": (78, 222, 112), "energy": (134, 255, 146),
    },
    "cosmic": {
        "dark": (28, 31, 64), "mid": (67, 76, 134), "light": (142, 166, 232),
        "accent": (98, 218, 255), "energy": (112, 236, 255),
    },
    "astral_prism": {
        "dark": (53, 43, 89), "mid": (112, 87, 174), "light": (205, 190, 246),
        "accent": (120, 224, 255), "energy": (155, 238, 255),
    },
    "chitin": {
        "dark": (54, 31, 25), "mid": (121, 70, 48), "light": (203, 136, 83),
        "accent": (134, 54, 190), "energy": (184, 96, 230),
    },
    "bio_filter_mask": {
        "dark": (24, 31, 28), "mid": (63, 83, 72), "light": (132, 158, 132),
        "accent": (96, 206, 168), "energy": (126, 238, 184),
    },
}

ADVANCED_GLOW = {
    "alien_hazmat",
    "alien_chem",
    "platinum",
    "emeradium",
    "cosmic",
    "astral_prism",
}

GLOW_REGIONS_LAYER_1 = [(1, 33, 9, 36), (18, 33, 25, 38), (14, 43, 21, 48)]
GLOW_REGIONS_LAYER_2 = [(0, 52, 4, 55), (5, 52, 9, 55)]


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def luminance(pixel: tuple[int, int, int, int]) -> float:
    r, g, b, _ = pixel
    return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0


def stable_rng(name: str) -> random.Random:
    seed = int.from_bytes(hashlib.sha256(name.encode("utf-8")).digest()[:8], "big")
    return random.Random(seed)


def image_from_bytes(data: bytes) -> Image.Image:
    return Image.open(io.BytesIO(data)).convert("RGBA")


def read_head_png(relative_path: Path) -> Image.Image | None:
    key = relative_path.as_posix()
    try:
        data = subprocess.check_output(["git", "show", f"HEAD:{key}"], cwd=ROOT)
    except subprocess.CalledProcessError:
        return None
    return image_from_bytes(data)


def open_current(path: Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def find_minecraft_jar() -> Path:
    for jar in MINECRAFT_JARS:
        if jar.exists():
            return jar
    raise FileNotFoundError("Minecraft client jar not found in known Gradle/.minecraft locations")


_vanilla_jar: zipfile.ZipFile | None = None


def vanilla_texture(name: str) -> Image.Image | None:
    global _vanilla_jar
    if _vanilla_jar is None:
        _vanilla_jar = zipfile.ZipFile(find_minecraft_jar())
    asset = f"assets/minecraft/textures/block/{name}.png"
    try:
        return image_from_bytes(_vanilla_jar.read(asset))
    except KeyError:
        return None


def colorize_by_luma(img: Image.Image, dark: tuple[int, int, int], mid: tuple[int, int, int],
                     light: tuple[int, int, int]) -> Image.Image:
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    src = img.load()
    dst = out.load()
    for y in range(img.height):
        for x in range(img.width):
            p = src[x, y]
            if p[3] == 0:
                continue
            v = max(0.0, min(1.0, luminance(p) ** 0.75))
            rgb = mix(dark, mid, v * 2.0) if v < 0.5 else mix(mid, light, (v - 0.5) * 2.0)
            dst[x, y] = (*rgb, p[3])
    return out


def colorize_grass_top(img: Image.Image) -> Image.Image:
    return colorize_by_luma(img, GRASS_DARK, GRASS_MID, GRASS_LIGHT)


def colorize_grass_side(img: Image.Image) -> Image.Image:
    out = img.copy().convert("RGBA")
    src = img.load()
    dst = out.load()
    for y in range(min(6, img.height)):
        for x in range(img.width):
            p = src[x, y]
            if p[3] == 0:
                continue
            v = max(0.0, min(1.0, luminance(p) ** 0.75))
            rgb = mix(GRASS_DARK, GRASS_MID, v * 2.0) if v < 0.5 else mix(GRASS_MID, GRASS_LIGHT, (v - 0.5) * 2.0)
            dst[x, y] = (*rgb, p[3])
    return out


def colorize_leaves(img: Image.Image, infected: bool = False) -> Image.Image:
    out = colorize_by_luma(img, LEAF_DARK, LEAF_MID, LEAF_LIGHT)
    if infected:
        pixels = out.load()
        accents = [(2, 4), (5, 9), (10, 2), (13, 12), (7, 14)]
        for x, y in accents:
            if x < out.width and y < out.height and pixels[x, y][3] != 0:
                pixels[x, y] = (128, 52, 168, pixels[x, y][3])
            if x + 1 < out.width and y < out.height and pixels[x + 1, y][3] != 0:
                pixels[x + 1, y] = (95, 184, 58, pixels[x + 1, y][3])
    return out


def resolve_base_texture(rest_name: str) -> Image.Image | None:
    local_name = LOCAL_NAME_MAP.get(rest_name, rest_name)
    remapped = BLOCK_DIR / f"{local_name}.png"
    if remapped.exists():
        return open_current(remapped)

    current = BLOCK_DIR / f"{rest_name}.png"
    if current.exists():
        return open_current(current)

    vanilla_name = VANILLA_NAME_MAP.get(rest_name, rest_name)
    vanilla = vanilla_texture(vanilla_name)
    if vanilla is None:
        return None
    if rest_name in {"grass", "grass_top"}:
        return colorize_grass_top(vanilla)
    if rest_name in {"grass_side", "grass_side_overlay"}:
        return colorize_grass_side(vanilla)
    if rest_name == "leaves":
        return colorize_leaves(vanilla)
    return vanilla


def blood_points(name: str, width: int, height: int) -> list[tuple[int, int, tuple[int, int, int, int]]]:
    rng = stable_rng(name)
    points: dict[tuple[int, int], tuple[int, int, int, int]] = {}

    def add(x: int, y: int, color: tuple[int, int, int, int]) -> None:
        if 0 <= x < width and 0 <= y < height:
            points[(x, y)] = color

    side_like = any(token in name for token in [
        "side", "front", "door", "trapdoor", "barrel", "log", "loom", "reactor",
        "turret", "terminal", "crate", "pipe", "lamp", "stash", "washer",
        "purifier", "beacon", "heart", "hive",
    ])

    def random_walk() -> None:
        x = rng.randrange(max(1, width))
        y = rng.randrange(max(1, height))
        steps = 3 + rng.randrange(5)
        for i in range(steps):
            add(x, y, BLOOD["wet"] if i == 0 and rng.random() < 0.25 else rng.choice([BLOOD["mid"], BLOOD["dry"]]))
            if rng.random() < 0.35:
                add(x + rng.choice([-1, 1]), y + rng.choice([-1, 0, 1]), BLOOD["dark"])
            x += rng.choice([-1, 0, 1])
            y += rng.choice([-1, 0, 1])

    def oriented_smear() -> None:
        vectors = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1), (1, -1), (-1, -1)]
        dx, dy = rng.choice(vectors)
        x = rng.randrange(max(1, width))
        y = rng.randrange(max(1, height))
        length = 3 + rng.randrange(5)
        for i in range(length):
            add(x + dx * i, y + dy * i, BLOOD["mid"] if i < length - 1 else BLOOD["dry"])
            if rng.random() < 0.35:
                add(x + dx * i + rng.choice([-1, 0, 1]), y + dy * i + rng.choice([-1, 0, 1]), BLOOD["dark"])

    def droplets() -> None:
        for _ in range(3 + rng.randrange(4)):
            x = rng.randrange(max(1, width))
            y = rng.randrange(max(1, height))
            add(x, y, rng.choice([BLOOD["dark"], BLOOD["mid"], BLOOD["dry"]]))
            if rng.random() < 0.25:
                add(x + rng.choice([-1, 1]), y, BLOOD["dry"])

    def edge_splash() -> None:
        edge = rng.choice(["top", "bottom", "left", "right"])
        if edge == "top":
            x, y = rng.randrange(max(1, width)), rng.randrange(max(1, min(4, height)))
        elif edge == "bottom":
            x, y = rng.randrange(max(1, width)), height - 1 - rng.randrange(max(1, min(4, height)))
        elif edge == "left":
            x, y = rng.randrange(max(1, min(4, width))), rng.randrange(max(1, height))
        else:
            x, y = width - 1 - rng.randrange(max(1, min(4, width))), rng.randrange(max(1, height))
        for _ in range(4 + rng.randrange(4)):
            add(x + rng.randrange(-2, 3), y + rng.randrange(-2, 3), rng.choice([BLOOD["mid"], BLOOD["dry"]]))

    shapes = [random_walk, oriented_smear, droplets, edge_splash]
    shape_count = 3 + rng.randrange(3)
    for _ in range(shape_count):
        rng.choice(shapes)()

    if side_like and height >= 12:
        for _ in range(1 + rng.randrange(2)):
            x = rng.randrange(max(1, width))
            y = rng.randrange(max(1, height - 4))
            drip = 2 + rng.randrange(3)
            for i in range(drip):
                add(x, y + i, BLOOD["dark"] if i == drip - 1 else BLOOD["mid"])
            if rng.random() < 0.4:
                add(x - 1, y, BLOOD["dry"])

    return [(x, y, color) for (x, y), color in points.items()]


def add_blood_decal(base: Image.Image, name: str) -> Image.Image:
    out = base.copy().convert("RGBA")
    pixels = out.load()
    for x, y, color in blood_points(name, out.width, out.height):
        if pixels[x, y][3] == 0:
            continue
        existing = pixels[x, y]
        alpha = min(existing[3], color[3])
        # Keep the decal sitting on the block surface instead of recolouring the
        # whole texture; this makes stone stay stone and leaves stay leaves.
        mixed = tuple(clamp(existing[i] * 0.25 + color[i] * 0.75) for i in range(3))
        pixels[x, y] = (*mixed, alpha)
    return out


def repair_leaves() -> int:
    base = vanilla_texture("oak_leaves")
    if base is None:
        return 0
    fixed = {
        "infested_leaves.png": colorize_leaves(base, infected=True),
        "bloody_leaves.png": add_blood_decal(colorize_leaves(base), "bloody_leaves.png"),
        "bloody_infested_leaves.png": add_blood_decal(colorize_leaves(base, infected=True), "bloody_infested_leaves.png"),
    }
    for name, img in fixed.items():
        img.save(BLOCK_DIR / name)
    return len(fixed)


def repair_bloody_variants() -> tuple[int, list[str]]:
    changed = 0
    missing: list[str] = []
    for path in sorted(BLOCK_DIR.glob("bloody_*.png")):
        if path.name == "blood_pool.png":
            continue
        rest = path.stem.removeprefix("bloody_")
        base = resolve_base_texture(rest)
        if base is None:
            missing.append(path.name)
            continue
        add_blood_decal(base, path.name).save(path)
        changed += 1
    return changed, missing


def armor_color(pixel: tuple[int, int, int, int], x: int, y: int,
                palette: dict[str, tuple[int, int, int]]) -> tuple[int, int, int, int]:
    if pixel[3] == 0:
        return (0, 0, 0, 0)
    v = max(0.0, min(1.0, luminance(pixel)))
    sat = max(pixel[:3]) - min(pixel[:3])
    if v < 0.16:
        rgb = palette["dark"]
    elif sat > 70 and v > 0.22:
        shade = mix(palette["dark"], palette["light"], min(1.0, v + 0.1))
        rgb = mix(shade, palette["accent"], 0.55)
    else:
        rgb = mix(palette["dark"], palette["mid"], v * 2.0) if v < 0.5 else mix(palette["mid"], palette["light"], (v - 0.5) * 2.0)

    # Subtle suit-wide panel rhythm on the vanilla UV islands. It adds detail
    # without moving the original armor coordinates.
    if (x + 2 * y) % 13 == 0 and v > 0.22:
        rgb = mix(rgb, palette["light"], 0.12)
    if (x * 3 + y) % 17 == 0 and v > 0.20:
        rgb = mix(rgb, palette["dark"], 0.12)
    return (*rgb, pixel[3])


def recolor_armor_from_head(name: str) -> bool:
    rel = Path("src/main/resources/assets/alien-invasion/textures/models/armor") / name
    src = read_head_png(rel)
    if src is None:
        source_path = ARMOR_DIR / name
        if not source_path.exists():
            return False
        src = open_current(source_path)

    suit = name.removesuffix(".png").replace("_layer_1", "").replace("_layer_2", "")
    palette = ARMOR_PALETTES.get(suit)
    if palette is None:
        return False

    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    in_px = src.load()
    out_px = out.load()
    for y in range(src.height):
        for x in range(src.width):
            out_px[x, y] = armor_color(in_px[x, y], x, y, palette)
    out.save(ARMOR_DIR / name)
    return True


def make_glow(suit: str, layer: int) -> bool:
    palette = ARMOR_PALETTES.get(suit)
    base_path = ARMOR_DIR / f"{suit}_layer_{layer}.png"
    if palette is None or not base_path.exists():
        return False
    base = open_current(base_path)
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    px = out.load()
    color = palette["energy"]
    regions = GLOW_REGIONS_LAYER_1 if layer == 1 else GLOW_REGIONS_LAYER_2
    for x0, y0, x1, y1 in regions:
        for y in range(y0, min(y1, out.height)):
            for x in range(x0, min(x1, out.width)):
                px[x, y] = (*color, 220)
        cy = (y0 + y1) // 2
        for x in range(x0, min(x1, out.width)):
            if cy < out.height:
                px[x, cy] = (min(255, color[0] + 45), min(255, color[1] + 30), min(255, color[2] + 20), 255)
    out.save(ARMOR_DIR / f"{suit}_glow_{layer}.png")
    return True


def repair_armor() -> int:
    changed = 0
    for name in sorted(ARMOR_PALETTES):
        targets = [f"{name}_layer_1.png", f"{name}_layer_2.png"] if name != "bio_filter_mask" else ["bio_filter_mask.png"]
        for target in targets:
            if recolor_armor_from_head(target):
                changed += 1
        if name in ADVANCED_GLOW:
            for layer in (1, 2):
                if make_glow(name, layer):
                    changed += 1
    return changed


def contact_sheet(paths: list[Path], out_path: Path, cell: int = 72) -> None:
    if not paths:
        return
    cols = min(8, len(paths))
    rows = (len(paths) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (18, 20, 28, 255))
    draw = ImageDraw.Draw(sheet)
    for i, path in enumerate(paths):
        img = open_current(path)
        scale = min(cell - 16, cell - 16) // max(img.width, img.height)
        scale = max(1, scale)
        preview = img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST)
        x = (i % cols) * cell + (cell - preview.width) // 2
        y = (i // cols) * (cell + 14) + 6
        sheet.alpha_composite(preview, (x, y))
        label = path.stem[:14]
        draw.text(((i % cols) * cell + 3, (i // cols) * (cell + 14) + cell - 7), label, fill=(225, 230, 240, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)


def main() -> None:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    armor_changed = repair_armor()
    leaves_changed = repair_leaves()
    bloody_changed, missing = repair_bloody_variants()

    contact_sheet(
        [BLOCK_DIR / name for name in [
            "bloody_stone.png", "bloody_infested_stone.png", "bloody_leaves.png",
            "bloody_infested_leaves.png", "bloody_grass_top.png", "bloody_infested_grass_top.png",
            "bloody_planks.png", "bloody_infested_planks.png",
        ] if (BLOCK_DIR / name).exists()],
        REPORT_DIR / "bloody_blocks.png",
    )
    contact_sheet(sorted(ARMOR_DIR.glob("*_layer_*.png")) + [ARMOR_DIR / "bio_filter_mask.png"], REPORT_DIR / "armor_layers.png")

    print(f"Repaired armor textures: {armor_changed}")
    print(f"Repaired leaf textures: {leaves_changed}")
    print(f"Rebuilt bloody block textures: {bloody_changed}")
    if missing:
        print("Missing bases:", ", ".join(missing))


if __name__ == "__main__":
    main()
