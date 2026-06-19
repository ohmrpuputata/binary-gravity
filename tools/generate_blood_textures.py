"""
Blood textures for Alien Invasion (red normal + purple infected/alien ichor).

(1) Particle droplets  -> textures/particle/blood_{0..2}.png (red, 8x8)
                          textures/particle/blood_purple_{0..2}.png (purple)
(2) Decal splatters     -> textures/block/blood_splat_a{0..3}_v{0..1}.png (red, 32x32)
                          textures/block/blood_splat_p_a{0..3}_v{0..1}.png (purple)
    Realistic, soft-edged, dark, GRADED by size (drops -> pool). Purple = the
    infection corrupted the blood (infected creatures + aliens).

Usage:
    python tools/generate_blood_textures.py            # -> staging + contact sheet
    python tools/generate_blood_textures.py --promote  # copy into the repo (particle/ + block/)
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

REPO = "src/main/resources/assets/alien-invasion/textures"
PART_OUT = os.path.join(REPO, "particle")
DECAL_REPO = os.path.join(REPO, "block")
DECAL_STAGING = "build/texture-audit/staging/blood_decal"

SZ, SS = 32, 4
W = SZ * SS

# (core tones, rim tones, deep-center) per blood kind
RED = {"core": [(78, 6, 8), (66, 5, 7), (88, 9, 10)], "rim": [(120, 14, 14), (104, 12, 13), (134, 18, 16)], "deep": (40, 3, 5)}
PURPLE = {"core": [(70, 18, 92), (60, 14, 80), (82, 24, 104)], "rim": [(112, 44, 150), (98, 36, 134), (126, 52, 166)], "deep": (38, 8, 54)}


def blob(draw, cx, cy, r, color, alpha, rng, jag=0.34, pts=22):
    poly = []
    for i in range(pts):
        a = 2 * math.pi * i / pts
        rr = r * (1.0 - jag + rng.random() * jag * 2.0)
        poly.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    draw.polygon(poly, fill=(*color, alpha))


def splat(amount, seed, pal):
    rng = random.Random(seed)
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = W / 2 + rng.uniform(-W * 0.06, W * 0.06)
    cy = W / 2 + rng.uniform(-W * 0.06, W * 0.06)
    rim = rng.choice(pal["rim"])
    core = rng.choice(pal["core"])
    main_r = {0: 0, 1: W * 0.16, 2: W * 0.26, 3: W * 0.38}[amount]
    if main_r > 0:
        blob(d, cx, cy, main_r * 1.12, rim, 235, rng)
        blob(d, cx, cy, main_r * 0.82, core, 255, rng)
        if amount >= 2:
            blob(d, cx, cy, main_r * 0.5, pal["deep"], 255, rng, jag=0.2)
    drops = {0: 7, 1: 9, 2: 12, 3: 16}[amount]
    spread = max(main_r, W * 0.12) + W * (0.06 + 0.04 * amount)
    for _ in range(drops):
        a = rng.uniform(0, 2 * math.pi)
        dist = (main_r * 0.7 if main_r else 0) + rng.uniform(0, spread)
        dx, dy = cx + dist * math.cos(a), cy + dist * math.sin(a)
        dr = rng.uniform(W * 0.012, W * 0.045)
        d.ellipse([dx - dr, dy - dr, dx + dr, dy + dr], fill=(*rng.choice(pal["core"]), 235))
    for _ in range(rng.randint(1, 2 + amount)):
        a = rng.uniform(0, 2 * math.pi)
        x0 = cx + (main_r * 0.6 if main_r else 0) * math.cos(a)
        y0 = cy + (main_r * 0.6 if main_r else 0) * math.sin(a)
        L = rng.uniform(W * 0.12, W * 0.3)
        d.line([x0, y0, x0 + L * math.cos(a), y0 + L * math.sin(a)], fill=(*core, 200), width=max(1, SS // 2))
    img = img.filter(ImageFilter.GaussianBlur(SS * 0.7))
    return img.resize((SZ, SZ), Image.LANCZOS)


def droplet(seed, pal):
    rng = random.Random(seed)
    im = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    base = rng.choice(pal["core"])
    r = 2.0 + rng.random() * 0.8
    d.ellipse([4 - r, 4 - r, 4 + r, 4 + r], fill=(*base, 240))
    return im


def contact(images, path, scale=6, cols=4):
    rows = (len(images) + cols - 1) // cols
    cw = ch = SZ * scale + 8
    sheet = Image.new("RGBA", (cols * cw, rows * ch), (150, 150, 152, 255))
    dr = ImageDraw.Draw(sheet)
    for i, (label, im) in enumerate(images):
        big = im.resize((SZ * scale, SZ * scale), Image.NEAREST)
        x, y = (i % cols) * cw + 4, (i // cols) * ch + 4
        sheet.alpha_composite(big, (x, y))
        dr.text((x + 2, y + 2), label, fill=(20, 20, 20, 255))
    sheet.save(path)
    print("contact ->", path)


KINDS = [("", RED), ("p_", PURPLE)]


def main():
    os.makedirs(PART_OUT, exist_ok=True)
    os.makedirs(DECAL_STAGING, exist_ok=True)
    for i in range(3):
        droplet(100 + i, RED).save(os.path.join(PART_OUT, f"blood_{i}.png"))
        droplet(200 + i, PURPLE).save(os.path.join(PART_OUT, f"blood_purple_{i}.png"))
    print(f"particles -> {PART_OUT} (blood_*, blood_purple_*)")

    grid = []
    for pfx, pal in KINDS:
        for a in range(4):
            for v in range(3):
                im = splat(a, 1000 + (1 if pfx else 0) * 500 + a * 10 + v, pal)
                im.save(os.path.join(DECAL_STAGING, f"blood_splat_{pfx}a{a}_v{v}.png"))
                grid.append((f"{pfx}a{a}v{v}", im))
    contact(grid, os.path.join(DECAL_STAGING, "_contact.png"))

    if "--promote" in sys.argv:
        import shutil
        os.makedirs(DECAL_REPO, exist_ok=True)
        for pfx, _ in KINDS:
            for a in range(4):
                for v in range(3):
                    f = f"blood_splat_{pfx}a{a}_v{v}.png"
                    shutil.copy2(os.path.join(DECAL_STAGING, f), os.path.join(DECAL_REPO, f))
        print(f"decals promoted -> {DECAL_REPO}")


if __name__ == "__main__":
    main()
