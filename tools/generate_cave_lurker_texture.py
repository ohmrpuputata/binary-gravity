"""Hand-authored texture for the Cave Lurker alien ambush-spider.

This is deliberate, designed pixel art (chitin plates with shaded faces, an ivory
maw, banded legs and a bioluminescent dorsal spine + eyes) painted directly onto
the UV islands declared by CaveLurkerModel.java. NOT procedural noise.

Run:  python tools/generate_cave_lurker_texture.py
"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENT = os.path.join(ROOT, "src", "main", "resources", "assets",
                   "alien-invasion", "textures", "entity")
REPORTS = os.path.join(ROOT, "build", "reports")
SIZE = 64

skin = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
eyes = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
sp = skin.load()
ep = eyes.load()

# --- alien cave-chitin palette ------------------------------------------------
BASE = (44, 58, 49)        # dark desaturated teal-green chitin
LIT = (64, 88, 70)         # lit dorsal plate
RIDGE = (92, 126, 96)      # plate highlight
BELLY = (126, 140, 120)    # cave-bleached underside
EDGE = (24, 32, 28)        # plate seam / outline
FANG = (214, 206, 176)     # ivory
FANG_DK = (150, 140, 110)
LEG = (50, 64, 53)
LEG_BAND = (30, 40, 33)
LEG_JOINT = (104, 134, 104)
ACCENT = (64, 214, 168)    # bioluminescent green-cyan (lit, on skin)
EYE = (122, 232, 255)      # glowing cyan (emissive layer)
EYE_CORE = (224, 248, 255)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))),
            max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), 255)


def rect(img, x0, y0, x1, y1, c):
    p = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                p[x, y] = c


def outline(img, x0, y0, x1, y1, c):
    """Darken the 1px border of a face for a plated, segmented look."""
    p = img.load()
    for x in range(x0, x1):
        if 0 <= x < SIZE:
            if 0 <= y0 < SIZE:
                p[x, y0] = c
            if 0 <= y1 - 1 < SIZE:
                p[x, y1 - 1] = c
    for y in range(y0, y1):
        if 0 <= y < SIZE:
            if 0 <= x0 < SIZE:
                p[x0, y] = c
            if 0 <= x1 - 1 < SIZE:
                p[x1 - 1, y] = c


def box(u, v, w, h, d, base, top_f=1.18, front_f=1.05, side_f=0.95,
        back_f=0.82, belly=None, seam=True):
    """Paint the six faces of a Minecraft box UV-island at (u,v)."""
    belly = belly if belly is not None else shade(base, 0.7)
    faces = {
        "up":    (u + d,         v,     u + d + w,         v + d,     shade(base, top_f)),
        "down":  (u + d + w,     v,     u + d + 2 * w,     v + d,     belly),
        "east":  (u,             v + d, u + d,             v + d + h, shade(base, side_f * 0.92)),
        "north": (u + d,         v + d, u + d + w,         v + d + h, shade(base, front_f)),
        "west":  (u + d + w,     v + d, u + d + w + d,     v + d + h, shade(base, side_f)),
        "south": (u + d + w + d, v + d, u + d + 2 * w + d, v + d + h, shade(base, back_f)),
    }
    for name, (x0, y0, x1, y1, c) in faces.items():
        rect(skin, x0, y0, x1, y1, c)
        if seam:
            outline(skin, x0, y0, x1, y1, EDGE)
    return faces


# --- THORAX (front body)  box 8x5x7 @ (0,0) -----------------------------------
tf = box(0, 0, 8, 5, 7, BASE)
# dorsal ridge highlight + biolum spine down the top face
up = tf["up"]
rect(skin, up[0] + 3, up[1] + 1, up[0] + 5, up[3] - 1, RIDGE)
rect(skin, up[0] + 3, up[1] + 2, up[0] + 5, up[1] + 3, ACCENT)

# --- ABDOMEN (raised rear bulb)  box 9x7x10 @ (0,14) --------------------------
af = box(0, 14, 9, 7, 10, BASE, top_f=1.22)
up = af["up"]
# segmented carapace plates + a glowing dorsal stripe (also on eyes layer)
cx = (up[0] + up[2]) // 2
for yy in range(up[1] + 1, up[3] - 1, 2):
    rect(skin, up[0] + 1, yy, up[2] - 1, yy + 1, shade(BASE, 1.3))
rect(skin, cx - 1, up[1] + 1, cx + 1, up[3] - 1, ACCENT)

# --- HEAD (maw)  box 6x4x5 @ (0,33) -------------------------------------------
hf = box(0, 33, 6, 4, 5, shade(BASE, 0.9), top_f=1.1)
# darken eye sockets on the front (north) face so they read even unlit
n = hf["north"]
rect(skin, n[0] + 1, n[1] + 1, n[0] + 5, n[1] + 3, shade(BASE, 0.45))

# --- FANGS  box 1x3x3 @ (24,33) -----------------------------------------------
box(24, 33, 1, 3, 3, FANG, top_f=1.05, front_f=1.0, side_f=0.9, back_f=0.85,
    belly=FANG_DK, seam=False)

# --- LEGS: shared femur 6x2x2 @ (40,8) and tibia 7x2x2 @ (40,15) ---------------
ff = box(40, 8, 6, 2, 2, LEG, top_f=1.2, belly=shade(LEG, 0.8))
# banding on the femur side faces + a bright joint at the outer end
for f in ("north", "south"):
    x0, y0, x1, y1, _ = ff[f]
    for xx in range(x0 + 1, x1, 2):
        rect(skin, xx, y0, xx + 1, y1, LEG_BAND)
    rect(skin, x1 - 1, y0, x1, y1, LEG_JOINT)

tf2 = box(40, 15, 7, 2, 2, LEG, top_f=1.15, belly=shade(LEG, 0.8))
for f in ("north", "south"):
    x0, y0, x1, y1, _ = tf2[f]
    for xx in range(x0 + 1, x1, 2):
        rect(skin, xx, y0, xx + 1, y1, LEG_BAND)
    rect(skin, x0, y0, x0 + 1, y1, LEG_JOINT)  # knee joint at inner end

# ============================== EMISSIVE EYES =================================
# Head north face lives at (d, v+d) = (5, 38), size 6x4 -> a clustered alien stare.
eye_spots = [(6, 39), (9, 39), (7, 40), (8, 41), (6, 41), (9, 40)]
for (x, y) in eye_spots:
    ep[x, y] = (*EYE, 255)
ep[7, 39] = (*EYE_CORE, 255)
ep[8, 40] = (*EYE_CORE, 255)

# Glowing dorsal stripe over the abdomen up-face (matches the skin accent line).
up = af["up"]
cx = (up[0] + up[2]) // 2
for yy in range(up[1] + 1, up[3] - 1):
    ep[cx, yy] = (*EYE, 200)
    if yy % 2 == 0:
        ep[cx - 1, yy] = (*EYE, 120)

os.makedirs(ENT, exist_ok=True)
skin.save(os.path.join(ENT, "cave_lurker.png"))
eyes.save(os.path.join(ENT, "cave_lurker_eyes.png"))

# ----------------------------- contact sheet ---------------------------------
os.makedirs(REPORTS, exist_ok=True)
SCALE = 7
sheet = Image.new("RGBA", (SIZE * SCALE * 2 + 48, SIZE * SCALE + 32), (32, 37, 50, 255))
skin_big = skin.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST)
combo = Image.alpha_composite(skin.convert("RGBA"), eyes)
combo_big = combo.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST)
sheet.alpha_composite(skin_big, (16, 16))
sheet.alpha_composite(combo_big, (SIZE * SCALE + 32, 16))
sheet.save(os.path.join(REPORTS, "cave_lurker_preview.png"))

print("Wrote cave_lurker.png + cave_lurker_eyes.png and build/reports/cave_lurker_preview.png")
