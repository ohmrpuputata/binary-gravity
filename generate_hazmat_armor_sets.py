"""Generate distinct textures for the two registered hazmat armor sets."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "src/main/resources/assets/alien-invasion"
ITEM = ASSETS / "textures/item"
ARMOR = ASSETS / "textures/models/armor"

OUTLINE = (18, 18, 24, 255)


def scaled_icon(draw_piece):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_piece(ImageDraw.Draw(image))
    return image.resize((32, 32), Image.Resampling.NEAREST)


def rect(draw, box, color):
    draw.rectangle(box, fill=color)


def save_set(prefix, palette, style):
    base, light, dark, seam, visor, accent = palette

    def helmet(draw):
        if style == "suit":
            draw.polygon([(4, 4), (6, 2), (10, 2), (12, 4), (12, 10),
                          (10, 12), (6, 12), (4, 10)], fill=OUTLINE)
            rect(draw, (5, 4, 11, 10), base)
            rect(draw, (6, 3, 10, 4), light)
            rect(draw, (5, 6, 11, 8), seam)
            rect(draw, (6, 6, 10, 7), visor)
            rect(draw, (7, 9, 9, 11), dark)
            rect(draw, (8, 9, 8, 9), accent)
        else:
            draw.polygon([(4, 4), (5, 2), (11, 2), (13, 5), (12, 11),
                          (10, 13), (6, 13), (3, 10)], fill=OUTLINE)
            rect(draw, (5, 3, 11, 10), dark)
            rect(draw, (6, 3, 10, 5), base)
            rect(draw, (4, 6, 12, 8), seam)
            rect(draw, (5, 6, 11, 7), visor)
            rect(draw, (6, 9, 10, 11), base)
            rect(draw, (7, 10, 9, 10), accent)

    def chestplate(draw):
        if style == "suit":
            draw.polygon([(3, 4), (5, 2), (7, 4), (9, 4), (11, 2),
                          (13, 4), (12, 9), (11, 13), (5, 13), (4, 9)],
                         fill=OUTLINE)
            draw.polygon([(4, 4), (5, 3), (7, 5), (9, 5), (11, 3),
                          (12, 4), (11, 12), (5, 12)], fill=base)
            rect(draw, (5, 4, 6, 7), light)
            rect(draw, (10, 4, 11, 7), light)
            rect(draw, (7, 5, 8, 12), seam)
            rect(draw, (5, 9, 6, 10), seam)
            rect(draw, (9, 9, 10, 10), seam)
            rect(draw, (7, 7, 8, 7), accent)
        else:
            draw.polygon([(2, 4), (5, 2), (7, 4), (9, 4), (11, 2),
                          (14, 4), (12, 9), (11, 14), (5, 14), (4, 9)],
                         fill=OUTLINE)
            rect(draw, (4, 4, 11, 12), dark)
            rect(draw, (3, 4, 5, 8), base)
            rect(draw, (10, 4, 12, 8), base)
            draw.polygon([(6, 4), (10, 4), (11, 7), (9, 12),
                          (7, 12), (5, 7)], fill=base)
            rect(draw, (7, 5, 9, 9), seam)
            rect(draw, (8, 6, 8, 8), accent)
            rect(draw, (5, 11, 10, 12), light)

    def leggings(draw):
        rect(draw, (3, 3, 12, 7), OUTLINE)
        rect(draw, (4, 4, 11, 7), base)
        if style == "suit":
            rect(draw, (5, 4, 10, 4), light)
            rect(draw, (4, 7, 7, 13), base)
            rect(draw, (9, 7, 12, 13), base)
            rect(draw, (5, 9, 7, 10), seam)
            rect(draw, (9, 9, 11, 10), seam)
            rect(draw, (6, 9, 6, 9), accent)
            rect(draw, (10, 9, 10, 9), accent)
        else:
            rect(draw, (4, 7, 7, 14), dark)
            rect(draw, (9, 7, 12, 14), dark)
            rect(draw, (5, 7, 7, 10), base)
            rect(draw, (9, 7, 11, 10), base)
            rect(draw, (4, 10, 7, 12), seam)
            rect(draw, (9, 10, 12, 12), seam)
            rect(draw, (5, 10, 6, 10), accent)
            rect(draw, (10, 10, 11, 10), accent)

    def boots(draw):
        if style == "suit":
            rect(draw, (3, 6, 7, 13), OUTLINE)
            rect(draw, (9, 6, 13, 13), OUTLINE)
            rect(draw, (4, 6, 6, 11), base)
            rect(draw, (10, 6, 12, 11), base)
            rect(draw, (4, 6, 6, 6), light)
            rect(draw, (10, 6, 12, 6), light)
            rect(draw, (3, 11, 7, 12), seam)
            rect(draw, (9, 11, 13, 12), seam)
        else:
            rect(draw, (3, 5, 7, 14), OUTLINE)
            rect(draw, (9, 5, 13, 14), OUTLINE)
            rect(draw, (4, 6, 6, 10), base)
            rect(draw, (10, 6, 12, 10), base)
            rect(draw, (3, 9, 7, 12), seam)
            rect(draw, (9, 9, 13, 12), seam)
            rect(draw, (4, 9, 5, 9), accent)
            rect(draw, (11, 9, 12, 9), accent)
            rect(draw, (3, 12, 7, 13), dark)
            rect(draw, (9, 12, 13, 13), dark)

    for piece, painter in (
        ("helmet", helmet),
        ("chestplate", chestplate),
        ("leggings", leggings),
        ("boots", boots),
    ):
        scaled_icon(painter).save(ITEM / f"{prefix}_{piece}.png")


FACES_LAYER_1 = [
    (8, 0, 15, 7), (16, 0, 23, 7),
    (0, 8, 7, 15), (8, 8, 15, 15), (16, 8, 23, 15), (24, 8, 31, 15),
    (20, 16, 27, 19), (44, 16, 47, 19), (4, 16, 7, 19),
    (16, 20, 19, 31), (20, 20, 27, 31), (28, 20, 31, 31),
    (40, 20, 43, 31), (44, 20, 47, 31), (48, 20, 51, 31),
    (0, 20, 3, 31), (4, 20, 7, 31), (8, 20, 11, 31),
]

FACES_LAYER_2 = [
    (4, 16, 7, 19), (20, 16, 23, 19),
    (0, 20, 3, 31), (4, 20, 7, 31), (8, 20, 11, 31),
    (16, 20, 19, 31), (20, 20, 23, 31), (24, 20, 27, 31),
]


def worn_layer(path, palette, style, layer_two):
    base, light, dark, seam, visor, accent = palette
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    faces = FACES_LAYER_2 if layer_two else FACES_LAYER_1

    for x0, y0, x1, y1 in faces:
        for y in range(y0, y1 + 1):
            t = (y - y0) / max(1, y1 - y0)
            color = tuple(round(light[i] + (base[i] - light[i]) * t) for i in range(4))
            rect(draw, (x0, y, x1, y), color)
        rect(draw, (x0, y0, x1, y0), light)
        rect(draw, (x0, y1, x1, y1), dark)
        rect(draw, (x0, y0, x0, y1), dark)

    if layer_two:
        if style == "suit":
            rect(draw, (4, 25, 7, 26), seam)
            rect(draw, (20, 25, 23, 26), seam)
            rect(draw, (5, 25, 6, 25), accent)
            rect(draw, (21, 25, 22, 25), accent)
            rect(draw, (4, 30, 7, 31), dark)
            rect(draw, (20, 30, 23, 31), dark)
        else:
            rect(draw, (3, 24, 8, 27), seam)
            rect(draw, (19, 24, 24, 27), seam)
            rect(draw, (4, 25, 7, 25), accent)
            rect(draw, (20, 25, 23, 25), accent)
            rect(draw, (4, 29, 7, 31), dark)
            rect(draw, (20, 29, 23, 31), dark)
    else:
        if style == "suit":
            rect(draw, (9, 10, 14, 12), seam)
            rect(draw, (10, 10, 13, 11), visor)
            rect(draw, (23, 20, 24, 31), seam)
            rect(draw, (20, 28, 27, 30), seam)
            for x in range(20, 28):
                image.putpixel((x, 29), accent if x % 2 == 0 else dark)
            rect(draw, (44, 29, 47, 31), dark)
        else:
            rect(draw, (8, 9, 15, 12), seam)
            rect(draw, (9, 10, 14, 11), visor)
            rect(draw, (19, 20, 28, 22), dark)
            rect(draw, (21, 22, 26, 29), seam)
            rect(draw, (23, 23, 24, 27), accent)
            rect(draw, (40, 20, 47, 23), dark)
            rect(draw, (44, 21, 47, 22), accent)
            rect(draw, (0, 25, 7, 27), seam)
            rect(draw, (4, 25, 7, 25), accent)

    image.save(path)


def main():
    ITEM.mkdir(parents=True, exist_ok=True)
    ARMOR.mkdir(parents=True, exist_ok=True)

    hazmat = (
        (219, 151, 31, 255),
        (255, 211, 83, 255),
        (139, 79, 19, 255),
        (39, 42, 48, 255),
        (96, 218, 235, 255),
        (247, 240, 77, 255),
    )
    chem = (
        (91, 74, 137, 255),
        (155, 123, 214, 255),
        (43, 38, 63, 255),
        (24, 28, 34, 255),
        (133, 237, 211, 255),
        (128, 255, 100, 255),
    )

    save_set("alien_hazmat", hazmat, "suit")
    save_set("alien_chem", chem, "armor")
    worn_layer(ARMOR / "alien_hazmat_layer_1.png", hazmat, "suit", False)
    worn_layer(ARMOR / "alien_hazmat_layer_2.png", hazmat, "suit", True)
    worn_layer(ARMOR / "alien_chem_layer_1.png", chem, "armor", False)
    worn_layer(ARMOR / "alien_chem_layer_2.png", chem, "armor", True)
    print("Generated distinct alien hazmat and chem armor textures.")


if __name__ == "__main__":
    main()
