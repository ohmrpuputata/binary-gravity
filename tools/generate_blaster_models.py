"""Turn the four flat blaster sprites into real 3D item models.

One shared energy-pistol geometry (grip + receiver + barrel + glowing core) is
described ONCE as a list of cubes; from that single description this script emits
both the model JSON (with per-face UVs) and a matching texture atlas per blaster,
plus the charge / cooling / firing animation-state variants (the core and muzzle
glow brighter) that the existing item-property predicates already swap between.

Deliberate, designed pixel art - NOT procedural noise.

Run:  python tools/generate_blaster_models.py
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "alien-invasion")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
ITEM_MODEL = os.path.join(ASSETS, "models", "item")
REPORTS = os.path.join(ROOT, "build", "reports")
ATLAS = 32  # texture is 32x32; model UVs are in 0..16 space, so scale by 16/ATLAS

MODID = "alien-invasion"


# --- shared geometry: cubes in the 0..16 item box -----------------------------
# name, from(x,y,z), to(x,y,z), uv-origin(u,v), is_glow_core
CUBES = [
    ("body",   (4, 6, 7), (12, 11, 10), (0, 0),  False),
    ("grip",   (5, 1, 7), (8, 7, 9),     (0, 8),  False),
    ("barrel", (11, 7, 7), (16, 10, 9),  (10, 8), False),
    ("core",   (6, 11, 7), (10, 12, 9),  (0, 16), True),
]


def box_faces(u, v, w, h, d):
    """Standard Minecraft box-UV unwrap -> pixel rects per face."""
    return {
        "up":    (u + d,         v,     u + d + w,         v + d),
        "down":  (u + d + w,     v,     u + d + 2 * w,     v + d),
        "east":  (u,             v + d, u + d,             v + d + h),
        "north": (u + d,         v + d, u + d + w,         v + d + h),
        "west":  (u + d + w,     v + d, u + d + w + d,     v + d + h),
        "south": (u + d + w + d, v + d, u + d + 2 * w + d, v + d + h),
    }


def uv16(rect):
    s = 16.0 / ATLAS
    return [round(rect[0] * s, 3), round(rect[1] * s, 3),
            round(rect[2] * s, 3), round(rect[3] * s, 3)]


# Pixel regions that the animation states light up.
CORE_REGION = (0, 16, 12, 19)          # the whole core-cube island
MUZZLE_REGIONS = [(10, 10, 12, 13),    # barrel east face (the emitter tip)
                  (12, 10, 17, 13)]    # barrel north face (front glow)

DISPLAY = {
    "gui": {"rotation": [8, -28, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.45, 0.45, 0.45]},
    "fixed": {"rotation": [0, -90, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_righthand": {"rotation": [-90, 0, -60], "translation": [2, 0.1, -3], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [-90, 0, 30], "translation": [2, 0.1, -3], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [-90, 0, -55], "translation": [1.13, 3.2, 1.13], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [-90, 0, 35], "translation": [1.13, 3.2, 1.13], "scale": [0.7, 0.7, 0.7]},
}


def build_geometry_model():
    elements = []
    for name, frm, to, _, _ in CUBES:
        u, v = dict((c[0], c[3]) for c in CUBES)[name]
        w, h, d = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
        rects = box_faces(u, v, w, h, d)
        faces = {}
        for fname, rect in rects.items():
            faces[fname] = {"uv": uv16(rect), "texture": "#tex"}
        elements.append({"from": list(frm), "to": list(to), "faces": faces})
    return {
        "credit": "Alien Invasion blaster - 3D energy pistol",
        "gui_light": "front",
        "textures": {"tex": f"{MODID}:item/alien_blaster", "particle": f"{MODID}:item/alien_blaster"},
        "display": DISPLAY,
        "elements": elements,
    }


# ------------------------------- palettes -------------------------------------
PALETTES = {
    "alien_blaster": {
        "body": (80, 92, 74), "lit": (104, 120, 92), "dark": (44, 52, 42),
        "grip": (122, 92, 64), "grip_dark": (78, 58, 40),
        "accent": (116, 255, 72), "hot": (224, 255, 196), "stripe": (60, 150, 50),
    },
    "green_ray_blaster": {
        "body": (126, 134, 140), "lit": (164, 172, 178), "dark": (70, 76, 82),
        "grip": (58, 64, 70), "grip_dark": (38, 42, 48),
        "accent": (72, 235, 110), "hot": (212, 255, 214), "stripe": (44, 168, 92),
    },
    "gravity_gun": {
        "body": (58, 64, 76), "lit": (86, 94, 108), "dark": (32, 36, 44),
        "grip": (40, 44, 54), "grip_dark": (26, 28, 36),
        "accent": (70, 229, 242), "hot": (226, 255, 255), "stripe": (169, 87, 255),
    },
    "astral_prism_gun": {
        "body": (120, 124, 142), "lit": (152, 156, 172), "dark": (72, 74, 92),
        "grip": (64, 60, 82), "grip_dark": (42, 40, 56),
        "accent": (72, 231, 255), "hot": (240, 247, 255), "stripe": (179, 83, 255),
    },
}


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def fill(px, rect, c):
    for y in range(rect[1], rect[3]):
        for x in range(rect[0], rect[2]):
            if 0 <= x < ATLAS and 0 <= y < ATLAS:
                px[x, y] = (c[0], c[1], c[2], 255)


def outline(px, rect, c):
    for x in range(rect[0], rect[2]):
        px[x, rect[1]] = (*c, 255)
        px[x, rect[3] - 1] = (*c, 255)
    for y in range(rect[1], rect[3]):
        px[rect[0], y] = (*c, 255)
        px[rect[2] - 1, y] = (*c, 255)


def paint_base(pal):
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    px = img.load()
    for name, frm, to, (u, v), glow in CUBES:
        w, h, d = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
        rects = box_faces(u, v, w, h, d)
        if name == "grip":
            base, lit, dark = pal["grip"], lerp(pal["grip"], (255, 255, 255), 0.18), pal["grip_dark"]
        elif glow:
            base, lit, dark = pal["accent"], pal["hot"], lerp(pal["accent"], (0, 0, 0), 0.35)
        else:
            base, lit, dark = pal["body"], pal["lit"], pal["dark"]
        for fname, rect in rects.items():
            shade = lit if fname == "up" else dark if fname == "down" else base
            fill(px, rect, shade)
            if not glow:
                outline(px, rect, pal["dark"])
        # a little identity stripe along the receiver top
        if name == "body":
            up = rects["up"]
            for x in range(up[0] + 1, up[2] - 1, 3):
                px[x, up[1] + 1] = (*pal["stripe"], 255)
        if glow:
            # bright core seed so it reads as an energy cell even at rest
            n = rects["north"]
            cx, cy = (n[0] + n[2]) // 2, (n[1] + n[3]) // 2
            px[cx, cy] = (*pal["hot"], 255)
    return img


def make_state(base_img, pal, kind, stage=0):
    img = base_img.copy()
    px = img.load()
    if kind == "charge":
        t = {1: 0.4, 2: 0.7, 3: 1.0}[stage]
        col = lerp(pal["accent"], pal["hot"], t)
        fill(px, CORE_REGION, col)
    elif kind == "cooling":
        col = lerp(pal["accent"], (255, 150, 70), 0.4)  # warm residual heat
        fill(px, CORE_REGION, lerp(pal["accent"], col, 0.5))
    elif kind == "firing":
        fill(px, CORE_REGION, pal["hot"])
        for r in MUZZLE_REGIONS:
            fill(px, r, pal["hot"])
    return img


# ----- per-gun model wiring (preserve each gun's existing predicate scheme) ----
def overrides_for(name):
    def ref(suffix):
        return f"{MODID}:item/{name}_{suffix}"
    if name in ("alien_blaster", "green_ray_blaster"):
        return [
            {"predicate": {f"{MODID}:heat": 0.03}, "model": ref("cooling")},
            {"predicate": {f"{MODID}:heat": 0.85}, "model": ref("firing")},
            {"predicate": {f"{MODID}:charge": 0.05}, "model": ref("charge_1")},
            {"predicate": {f"{MODID}:charge": 0.4}, "model": ref("charge_2")},
            {"predicate": {f"{MODID}:charge": 0.75}, "model": ref("charge_3")},
        ]
    if name == "gravity_gun":
        return [
            {"predicate": {f"{MODID}:shot": 0.05}, "model": ref("cooling")},
            {"predicate": {f"{MODID}:shot": 0.55}, "model": ref("firing")},
        ]
    # astral_prism_gun
    return [
        {"predicate": {f"{MODID}:shot": 0.05}, "model": ref("cooling")},
        {"predicate": {f"{MODID}:shot": 0.55}, "model": ref("firing")},
        {"predicate": {f"{MODID}:charge": 0.05}, "model": ref("charge_1")},
        {"predicate": {f"{MODID}:charge": 0.4}, "model": ref("charge_2")},
        {"predicate": {f"{MODID}:charge": 0.75}, "model": ref("charge_3")},
    ]


def states_for(name):
    s = ["cooling", "firing"]
    if name != "gravity_gun":
        s += ["charge_1", "charge_2", "charge_3"]
    return s


def write_json(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    os.makedirs(ITEM_TEX, exist_ok=True)
    os.makedirs(ITEM_MODEL, exist_ok=True)
    os.makedirs(REPORTS, exist_ok=True)

    # shared geometry + recoil variant
    write_json(os.path.join(ITEM_MODEL, "blaster_base.json"), build_geometry_model())
    recoil = {"parent": f"{MODID}:item/blaster_base",
              "display": dict(DISPLAY)}
    recoil["display"]["thirdperson_righthand"] = {"rotation": [-90, 0, -70], "translation": [1.2, 0.6, -2.4], "scale": [0.85, 0.85, 0.85]}
    recoil["display"]["firstperson_righthand"] = {"rotation": [-90, 0, -65], "translation": [0.5, 3.5, 1.5], "scale": [0.7, 0.7, 0.7]}
    write_json(os.path.join(ITEM_MODEL, "blaster_recoil.json"), recoil)

    previews = []
    for name, pal in PALETTES.items():
        base = paint_base(pal)
        base.save(os.path.join(ITEM_TEX, f"{name}.png"))
        previews.append((name, base.copy()))

        # base model: bind tex + keep this gun's override scheme
        write_json(os.path.join(ITEM_MODEL, f"{name}.json"), {
            "parent": f"{MODID}:item/blaster_base",
            "textures": {"tex": f"{MODID}:item/{name}", "particle": f"{MODID}:item/{name}"},
            "overrides": overrides_for(name),
        })

        for state in states_for(name):
            if state.startswith("charge"):
                img = make_state(base, pal, "charge", int(state[-1]))
            else:
                img = make_state(base, pal, state)
            img.save(os.path.join(ITEM_TEX, f"{name}_{state}.png"))
            parent = "blaster_recoil" if state == "firing" else "blaster_base"
            write_json(os.path.join(ITEM_MODEL, f"{name}_{state}.json"), {
                "parent": f"{MODID}:item/{parent}",
                "textures": {"tex": f"{MODID}:item/{name}_{state}", "particle": f"{MODID}:item/{name}_{state}"},
            })

    # contact sheet: each gun's base atlas + its charge_3/firing states, upscaled
    cols = [("base", None), ("charge_3", "charge_3"), ("firing", "firing")]
    S = 7
    cell = ATLAS * S
    sheet = Image.new("RGBA", (len(cols) * (cell + 14) + 14, len(PALETTES) * (cell + 26) + 14), (28, 32, 44, 255))
    draw = ImageDraw.Draw(sheet)
    for row, (name, pal) in enumerate(PALETTES.items()):
        base = paint_base(pal)
        variants = {
            "base": base,
            "charge_3": make_state(base, pal, "charge", 3),
            "firing": make_state(base, pal, "firing"),
        }
        for col, (label, key) in enumerate(cols):
            img = variants["base"] if key is None else variants[key]
            big = img.resize((cell, cell), Image.NEAREST)
            x = 14 + col * (cell + 14)
            y = 14 + row * (cell + 26)
            sheet.alpha_composite(big, (x, y))
            draw.text((x, y + cell + 6), f"{name} [{label}]", fill=(220, 225, 235, 255))
    sheet.save(os.path.join(REPORTS, "blaster_models_preview.png"))
    print("Generated 3D blaster models, atlases and animation states for:",
          ", ".join(PALETTES))


if __name__ == "__main__":
    main()
