"""Emissive glow maps for the advanced-armor suits.

Painted deterministically (NOT extracted): bright strips only at the UV regions of
the helmet visor / crest and the chest core, in each suit's energy colour. The
renderer draws these full-bright via RenderType.eyes, so just those accents glow in
the dark - the metal plates stay normal.

Run:  python tools/generate_armor_glow.py
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARMOR = os.path.join(ROOT, "src", "main", "resources", "assets",
                     "alien-invasion", "textures", "models", "armor")

# suit textureName -> energy glow colour
GLOW = {
    "cosmic":       (140, 232, 255),
    "platinum":     (150, 222, 255),
    "emeradium":    (160, 255, 160),
    "astral_prism": (150, 240, 255),
    "alien_hazmat": (150, 232, 255),
    "alien_chem":   (170, 255, 140),
}

# Glow regions on the 64x64 layer (detail-part UV faces): (x0, y0, x1, y1) exclusive.
# Generous spans so they land on the visor/crest/core front faces across variants.
LAYER1_REGIONS = [
    (1, 33, 9, 36),    # helmet visor band (front face of texOffs 0,32 part)
    (18, 33, 25, 38),  # helmet crest / faceplate (secondary helmet detail)
    (14, 43, 21, 48),  # chest core / reactor (chest detail front)
]
LAYER2_REGIONS = [
    (0, 52, 4, 55),    # knee accent (right)
    (5, 52, 9, 55),    # knee accent (left, mirrored)
]


def paint(dst_path, w, h, regions, color):
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    p = out.load()
    for (x0, y0, x1, y1) in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                if 0 <= x < w and 0 <= y < h:
                    p[x, y] = (color[0], color[1], color[2], 255)
        # bright core line through the middle for a hotter center
        cy = (y0 + y1) // 2
        for x in range(x0, x1):
            if 0 <= x < w and 0 <= cy < h:
                p[x, cy] = (min(255, color[0] + 40), min(255, color[1] + 20),
                            min(255, color[2] + 20), 255)
    out.save(dst_path)


def main():
    n = 0
    for suit, color in GLOW.items():
        # size matches the base layer (64x64 here)
        base = os.path.join(ARMOR, f"{suit}_layer_1.png")
        w, h = Image.open(base).size if os.path.exists(base) else (64, 64)
        paint(os.path.join(ARMOR, f"{suit}_glow_1.png"), w, h, LAYER1_REGIONS, color)
        paint(os.path.join(ARMOR, f"{suit}_glow_2.png"), w, h, LAYER2_REGIONS, color)
        n += 2
    print(f"Wrote {n} painted armor glow maps for: {', '.join(GLOW)}")


if __name__ == "__main__":
    main()
