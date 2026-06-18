"""
Recolor vanilla armor sheets per mod set, with added thematic elements.

Takes a vanilla armor layer (diamond / iron / netherite / leather) from the
Loom-cached client jar and applies:
  (1) a strong luminance-preserving brand tint (keeps vanilla plating/shading
      and the perfect UV → real sleeves, open-face helmet, no clipping), plus
  (2) a bright ACCENT/trim pass on the highlights — rim-light "powered armor"
      edges so each set reads as distinct bio/tech gear, not tinted vanilla.

Brand tones mirror the HUD suit colours. Chem armour is vivid alien-green
(green alien skin), Hazmat is hazard-yellow, Cosmic violet, etc.

Usage:
    python tools/generate_recolored_armor.py            # -> staging + contact sheet
    python tools/generate_recolored_armor.py --promote  # copy staging into the repo
"""
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HOME = os.path.expanduser("~")
JAR = os.path.join(HOME, ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client.jar")
REPO = "src/main/resources/assets/alien-invasion/textures/models/armor"
STAGING = "build/texture-audit/staging/armor"

_zip = zipfile.ZipFile(JAR)


def vanilla(name):
    data = _zip.read(f"assets/minecraft/textures/models/armor/{name}.png")
    return Image.open(io.BytesIO(data)).convert("RGBA")


def clamp(v):
    return 0 if v < 0 else (255 if v > 255 else int(v))


def recolor(im, tone, t, accent, athr, astr, dark=1.0):
    """Tint toward `tone` (luminance-preserving), then push the highlights toward
    a bright `accent` (trim/rim light) above luminance `athr` with strength `astr`."""
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            L = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            nr = r * (1 - t) + tone[0] * L * t
            ng = g * (1 - t) + tone[1] * L * t
            nb = b * (1 - t) + tone[2] * L * t
            if L > athr:
                k = min(1.0, (L - athr) / max(1e-3, 1.0 - athr)) * astr
                nr = nr * (1 - k) + accent[0] * k
                ng = ng * (1 - k) + accent[1] * k
                nb = nb * (1 - k) + accent[2] * k
            px[x, y] = (clamp(nr * dark), clamp(ng * dark), clamp(nb * dark), a)
    return im


def base_layer(base, n):
    """Vanilla armour layer n; leather is flattened with its dyeable overlay so
    the whole cloth suit takes the tint."""
    im = vanilla(f"{base}_layer_{n}")
    if base == "leather":
        try:
            im.alpha_composite(vanilla(f"{base}_layer_{n}_overlay"))
        except KeyError:
            pass
    return im


# set -> (base, tone, tint, accent, accent_thresh, accent_strength, brightness)
SETS = {
    "cosmic":       ("netherite", (150, 90, 235),  0.62, (235, 155, 255), 0.60, 0.60, 1.05),
    "astral_prism": ("diamond",   (70, 200, 245),  0.60, (165, 246, 255), 0.64, 0.55, 1.02),
    "emeradium":    ("diamond",   (80, 210, 120),  0.60, (175, 255, 195), 0.64, 0.55, 1.02),
    "alien_hazmat": ("leather",   (240, 180, 40),  0.70, (255, 236, 120), 0.58, 0.55, 1.08),
    "alien_chem":   ("leather",   (80, 220, 70),   0.74, (185, 255, 140), 0.56, 0.60, 1.08),
    "platinum":     ("iron",      (205, 222, 245), 0.50, (255, 255, 255), 0.70, 0.50, 1.08),
    "palladium":    ("iron",      (85, 215, 190),  0.58, (170, 255, 235), 0.64, 0.55, 1.05),
}


def build(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    made = {}
    for name, (base, tone, t, accent, athr, astr, dark) in SETS.items():
        layers = []
        for n in (1, 2):
            im = recolor(base_layer(base, n), tone, t, accent, athr, astr, dark)
            im.save(os.path.join(out_dir, f"{name}_layer_{n}.png"))
            layers.append(im)
        made[name] = layers
        print(f"  {name:<13} <- {base}")
    return made


def contact_sheet(made, path):
    scale = 5
    cell_w, cell_h = 64 * scale, 32 * scale
    pad, label_h = 10, 16
    cols = 2
    rows = (len(made) + cols - 1) // cols
    sheet_w = cols * (cell_w * 2 + pad * 3)
    sheet_h = rows * (cell_h + label_h + pad) + pad
    sheet = Image.new("RGBA", (sheet_w, sheet_h), (28, 30, 34, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (name, layers) in enumerate(made.items()):
        cx = (i % cols) * (cell_w * 2 + pad * 3) + pad
        cy = (i // cols) * (cell_h + label_h + pad) + pad
        draw.text((cx, cy), name, fill=(235, 236, 239, 255))
        for j, layer in enumerate(layers):
            big = layer.resize((cell_w, cell_h), Image.NEAREST)
            sheet.alpha_composite(big, (cx + j * (cell_w + pad), cy + label_h))
    sheet.save(path)
    print(f"contact sheet -> {path}")


def promote(out_dir, repo_dir):
    import shutil
    os.makedirs(repo_dir, exist_ok=True)
    for f in sorted(os.listdir(out_dir)):
        if f.endswith("_layer_1.png") or f.endswith("_layer_2.png"):
            shutil.copy2(os.path.join(out_dir, f), os.path.join(repo_dir, f))
            print(f"  -> {f}")
    print("promoted to repo")


if __name__ == "__main__":
    if "--promote" in sys.argv:
        build(STAGING)
        promote(STAGING, REPO)
    else:
        made = build(STAGING)
        contact_sheet(made, os.path.join(STAGING, "_contact.png"))
