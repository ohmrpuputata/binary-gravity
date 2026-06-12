"""Generate the palladium armor set and storage block textures.

The worn armor keeps the vanilla UV layout in the upper 64x32 area. The lower
half contains the atlas for the extra 3D visor, shoulders, reactor and leg
guards used by PalladiumArmorModel.
"""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "src/main/resources/assets/alien-invasion"
ITEM = ASSETS / "textures/item"
ARMOR = ASSETS / "textures/models/armor"
BLOCK = ASSETS / "textures/block"

OUTLINE = (18, 29, 29, 255)
DARK = (45, 78, 74, 255)
MID = (104, 157, 146, 255)
LIGHT = (178, 222, 208, 255)
SHINE = (226, 249, 240, 255)
SEAM = (28, 50, 49, 255)
GLOW = (155, 255, 65, 255)
GLOW_HI = (226, 255, 166, 255)


def blend(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def palladium_tone(luminance):
    t = luminance / 255.0
    if t < 0.45:
        return blend(DARK, MID, t / 0.45)
    if t < 0.82:
        return blend(MID, LIGHT, (t - 0.45) / 0.37)
    return blend(LIGHT, SHINE, (t - 0.82) / 0.18)


def recolor_template(source_name):
    source = Image.open(ARMOR / source_name).convert("RGBA")
    source = source.crop((0, 0, 64, 32))
    output = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    src = source.load()
    dst = output.load()

    for y in range(32):
        for x in range(64):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            luminance = round(r * 0.2126 + g * 0.7152 + b * 0.0722)
            tone = palladium_tone(luminance)
            dst[x, y] = tone[:3] + (a,)

    # Custom-model atlas. Dark borders and diagonal highlights keep the added
    # geometry readable without turning the whole suit neon green.
    for y in range(32, 64):
        for x in range(64):
            edge = x % 8 in (0, 7) or y % 8 in (0, 7)
            diagonal = (x + y) % 13 == 0
            dst[x, y] = SEAM if edge else (SHINE if diagonal else MID)

    return output


def rect(draw, box, color):
    draw.rectangle(box, fill=color)


def add_layer_details(image, layer_two):
    draw = ImageDraw.Draw(image)

    if layer_two:
        # Leg bands and knees in the standard leggings UV.
        rect(draw, (4, 25, 7, 26), SEAM)
        rect(draw, (20, 25, 23, 26), SEAM)
        rect(draw, (5, 25, 6, 25), GLOW)
        rect(draw, (21, 25, 22, 25), GLOW)
    else:
        # Narrow visor and restrained chest power indicator.
        rect(draw, (9, 10, 14, 11), SEAM)
        rect(draw, (10, 10, 13, 10), GLOW)
        rect(draw, (22, 22, 25, 24), SEAM)
        rect(draw, (23, 22, 24, 23), GLOW)

    # Extra model atlas details.
    rect(draw, (0, 32, 17, 35), SEAM)
    rect(draw, (2, 33, 15, 34), GLOW)
    rect(draw, (32, 32, 43, 41), SEAM)
    rect(draw, (35, 34, 40, 39), GLOW)
    rect(draw, (37, 35, 38, 38), GLOW_HI)
    rect(draw, (44, 32, 63, 41), DARK)
    rect(draw, (47, 33, 60, 34), LIGHT)
    rect(draw, (0, 43, 9, 48), DARK)
    rect(draw, (2, 44, 7, 45), GLOW)
    rect(draw, (12, 43, 31, 50), SEAM)
    rect(draw, (15, 44, 28, 45), LIGHT)


def armor_icon(piece):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    if piece == "helmet":
        draw.polygon([(4, 4), (6, 2), (10, 2), (12, 4), (12, 10), (10, 12),
                      (6, 12), (4, 10)], fill=OUTLINE)
        rect(draw, (5, 4, 11, 9), MID)
        rect(draw, (6, 3, 10, 4), LIGHT)
        rect(draw, (5, 7, 11, 8), SEAM)
        rect(draw, (7, 7, 10, 7), GLOW)
        rect(draw, (6, 9, 7, 11), DARK)
        rect(draw, (10, 9, 11, 11), DARK)
    elif piece == "chestplate":
        draw.polygon([(3, 3), (6, 2), (7, 4), (9, 4), (10, 2), (13, 3),
                      (12, 8), (11, 13), (5, 13), (4, 8)], fill=OUTLINE)
        draw.polygon([(4, 4), (6, 3), (7, 5), (9, 5), (10, 3), (12, 4),
                      (11, 8), (10, 12), (6, 12), (5, 8)], fill=MID)
        rect(draw, (5, 4, 6, 6), LIGHT)
        rect(draw, (10, 4, 11, 6), SHINE)
        rect(draw, (7, 6, 9, 9), SEAM)
        rect(draw, (8, 7, 8, 8), GLOW_HI)
        rect(draw, (6, 11, 10, 12), DARK)
    elif piece == "leggings":
        draw.polygon([(4, 3), (12, 3), (12, 7), (11, 13), (8, 13), (8, 8),
                      (7, 13), (4, 13), (3, 7)], fill=OUTLINE)
        rect(draw, (4, 4, 11, 7), MID)
        rect(draw, (5, 4, 10, 4), LIGHT)
        draw.polygon([(4, 7), (7, 7), (7, 12), (5, 12)], fill=MID)
        draw.polygon([(9, 7), (12, 7), (11, 12), (9, 12)], fill=MID)
        rect(draw, (5, 9, 7, 10), DARK)
        rect(draw, (9, 9, 11, 10), DARK)
        rect(draw, (6, 9, 6, 9), GLOW)
        rect(draw, (10, 9, 10, 9), GLOW)
    else:
        draw.polygon([(3, 6), (7, 6), (7, 11), (8, 13), (3, 13),
                      (2, 11), (9, 6), (13, 6), (14, 11), (13, 13),
                      (8, 13), (9, 11)], fill=OUTLINE)
        rect(draw, (3, 7, 6, 11), MID)
        rect(draw, (10, 7, 13, 11), MID)
        rect(draw, (3, 7, 6, 7), LIGHT)
        rect(draw, (10, 7, 13, 7), SHINE)
        rect(draw, (3, 10, 6, 11), DARK)
        rect(draw, (10, 10, 13, 11), DARK)
        rect(draw, (4, 9, 4, 9), GLOW)
        rect(draw, (11, 9, 11, 9), GLOW)

    return image.resize((32, 32), Image.Resampling.NEAREST)


def palladium_block():
    image = Image.new("RGBA", (16, 16), MID)
    px = image.load()
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            seam = x in (7, 8) or y in (7, 8)
            if border:
                px[x, y] = OUTLINE
            elif seam:
                px[x, y] = DARK
            elif (x + y) % 7 == 0:
                px[x, y] = LIGHT

    draw = ImageDraw.Draw(image)
    rect(draw, (3, 3, 5, 4), SHINE)
    rect(draw, (10, 10, 12, 11), SHINE)
    rect(draw, (7, 3, 8, 5), GLOW)
    rect(draw, (7, 10, 8, 12), GLOW)
    return image


def main():
    ARMOR.mkdir(parents=True, exist_ok=True)
    ITEM.mkdir(parents=True, exist_ok=True)
    BLOCK.mkdir(parents=True, exist_ok=True)

    for layer in (1, 2):
        image = recolor_template(f"platinum_layer_{layer}.png")
        add_layer_details(image, layer == 2)
        image.save(ARMOR / f"palladium_layer_{layer}.png")

    for piece in ("helmet", "chestplate", "leggings", "boots"):
        armor_icon(piece).save(ITEM / f"palladium_{piece}.png")

    palladium_block().save(BLOCK / "palladium_block.png")
    print("Generated palladium armor, icons and storage block textures.")


if __name__ == "__main__":
    main()
