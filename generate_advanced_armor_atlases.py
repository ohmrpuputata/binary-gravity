"""Expand registered armor textures with a 64x64 custom-model atlas."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
ARMOR = ROOT / "src/main/resources/assets/alien-invasion/textures/models/armor"

PALETTES = {
    "cosmic": {
        "dark": (27, 12, 52, 255),
        "base": (82, 42, 141, 255),
        "light": (180, 112, 255, 255),
        "glow": (83, 240, 255, 255),
    },
    "alien_hazmat": {
        "dark": (49, 39, 24, 255),
        "base": (203, 132, 22, 255),
        "light": (255, 211, 83, 255),
        "glow": (92, 226, 242, 255),
    },
    "alien_chem": {
        "dark": (28, 24, 43, 255),
        "base": (91, 74, 137, 255),
        "light": (157, 124, 218, 255),
        "glow": (126, 255, 94, 255),
    },
    "platinum": {
        "dark": (66, 75, 91, 255),
        "base": (158, 173, 196, 255),
        "light": (239, 247, 255, 255),
        "glow": (93, 212, 255, 255),
    },
}


def expand(name, layer):
    path = ARMOR / f"{name}_layer_{layer}.png"
    source = Image.open(path).convert("RGBA")
    output = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    output.paste(source.crop((0, 0, 64, 32)), (0, 0))
    draw = ImageDraw.Draw(output)
    colors = PALETTES[name]

    for y in range(32, 64):
        for x in range(64):
            tile_edge = x % 8 in (0, 7) or y % 8 in (0, 7)
            diagonal = (x + y) % 11 == 0
            color = colors["dark"] if tile_edge else colors["light"] if diagonal else colors["base"]
            output.putpixel((x, y), color)

    draw.rectangle((0, 32, 17, 35), fill=colors["dark"])
    draw.rectangle((2, 33, 15, 34), fill=colors["glow"])
    draw.rectangle((0, 42, 29, 50), outline=colors["dark"], width=2)
    draw.rectangle((5, 44, 24, 46), fill=colors["glow"])
    draw.rectangle((32, 42, 47, 55), outline=colors["dark"], width=2)
    draw.rectangle((36, 45, 43, 52), fill=colors["glow"])
    draw.rectangle((48, 32, 63, 47), outline=colors["dark"], width=2)
    draw.rectangle((50, 34, 61, 36), fill=colors["light"])
    draw.rectangle((0, 52, 15, 63), outline=colors["dark"], width=2)
    draw.rectangle((3, 54, 12, 56), fill=colors["glow"])
    draw.rectangle((20, 52, 39, 63), outline=colors["dark"], width=2)
    draw.rectangle((23, 54, 36, 56), fill=colors["light"])

    output.save(path)


def main():
    for name in PALETTES:
        for layer in (1, 2):
            expand(name, layer)
    print("Expanded cosmic, hazmat, chem and platinum armor atlases to 64x64.")


if __name__ == "__main__":
    main()
