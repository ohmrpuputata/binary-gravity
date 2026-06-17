"""Offline 3D preview of the blaster item models.

Rasterises the exact cube geometry + atlas UVs that Minecraft uses (no game
launch needed) so we can eyeball how the assembled 3D gun looks before committing.
Painter's-algorithm texel quads with simple directional lighting.

Run:  python tools/render_blaster_preview.py
"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_TEX = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "alien-invasion", "textures", "item")
REPORTS = os.path.join(ROOT, "build", "reports")
ATLAS = 32

# Mirror of CUBES in generate_blaster_models.py
CUBES = [
    ("body",   (4, 6, 7), (12, 11, 10), (0, 0)),
    ("grip",   (5, 1, 7), (8, 7, 9),    (0, 8)),
    ("barrel", (11, 7, 7), (16, 10, 9), (10, 8)),
    ("core",   (6, 11, 7), (10, 12, 9), (0, 16)),
]


def box_faces(u, v, w, h, d):
    return {
        "up":    (u + d,         v,     u + d + w,         v + d),
        "down":  (u + d + w,     v,     u + d + 2 * w,     v + d),
        "east":  (u,             v + d, u + d,             v + d + h),
        "north": (u + d,         v + d, u + d + w,         v + d + h),
        "west":  (u + d + w,     v + d, u + d + w + d,     v + d + h),
        "south": (u + d + w + d, v + d, u + d + 2 * w + d, v + d + h),
    }


# face -> (normal, corner-order as functions of box min/max) with A=(0,0) uv origin
def face_corners(name, p0, p1):
    x0, y0, z0 = p0
    x1, y1, z1 = p1
    f = {
        "up":    ((0, 1, 0), [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)]),
        "down":  ((0, -1, 0), [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)]),
        "north": ((0, 0, -1), [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)]),
        "south": ((0, 0, 1), [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]),
        "east":  ((1, 0, 0), [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]),
        "west":  ((-1, 0, 0), [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]),
    }
    return f[name]


YAW = math.radians(-32)
PITCH = math.radians(22)
LIGHT = (-0.35, 0.78, 0.52)
_ll = math.sqrt(sum(c * c for c in LIGHT))
LIGHT = tuple(c / _ll for c in LIGHT)
CENTER = (8.0, 6.0, 8.5)


def rot(p):
    x, y, z = p[0] - CENTER[0], p[1] - CENTER[1], p[2] - CENTER[2]
    cy, sy = math.cos(YAW), math.sin(YAW)
    x, z = x * cy + z * sy, -x * sy + z * cy
    cp, sp = math.cos(PITCH), math.sin(PITCH)
    y, z = y * cp - z * sp, y * sp + z * cp
    return (x, y, z)


def bilerp(c, s, t):
    a, b, cc, d = c
    return tuple(a[k] * (1 - s) * (1 - t) + b[k] * s * (1 - t)
                 + cc[k] * s * t + d[k] * (1 - s) * t for k in range(3))


def render(atlas_path, size=224):
    atlas = Image.open(atlas_path).convert("RGBA")
    ap = atlas.load()
    scale = 11.0
    ox, oy = size / 2, size / 2 + 18
    quads = []
    for name, p0, p1, (u, v) in CUBES:
        w, h, d = p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]
        rects = box_faces(u, v, w, h, d)
        for fname, rect in rects.items():
            normal, corners = face_corners(fname, p0, p1)
            shade = 0.5 + 0.5 * max(0.0, sum(normal[k] * LIGHT[k] for k in range(3)))
            uw = rect[2] - rect[0]
            uh = rect[3] - rect[1]
            if uw <= 0 or uh <= 0:
                continue
            for j in range(uh):
                for i in range(uw):
                    col = ap[rect[0] + i, rect[1] + j]
                    if col[3] == 0:
                        continue
                    s0, s1 = i / uw, (i + 1) / uw
                    t0, t1 = j / uh, (j + 1) / uh
                    pts3 = [bilerp(corners, s0, t0), bilerp(corners, s1, t0),
                            bilerp(corners, s1, t1), bilerp(corners, s0, t1)]
                    rp = [rot(p) for p in pts3]
                    depth = sum(p[2] for p in rp) / 4.0
                    scr = [(ox + p[0] * scale, oy - p[1] * scale) for p in rp]
                    c = (min(255, int(col[0] * shade)), min(255, int(col[1] * shade)),
                         min(255, int(col[2] * shade)), 255)
                    quads.append((depth, scr, c))
    quads.sort(key=lambda q: q[0])  # far (smaller z) first
    img = Image.new("RGBA", (size, size), (28, 32, 44, 255))
    draw = ImageDraw.Draw(img)
    for _, scr, c in quads:
        draw.polygon(scr, fill=c)
    return img


def main():
    os.makedirs(REPORTS, exist_ok=True)
    guns = ["alien_blaster", "green_ray_blaster", "gravity_gun", "astral_prism_gun"]
    cols = ["", "_charge_3", "_firing"]
    labels = ["base", "charge", "firing"]
    S = 224
    sheet = Image.new("RGBA", (len(cols) * (S + 10) + 10, len(guns) * (S + 24) + 10), (20, 23, 32, 255))
    draw = ImageDraw.Draw(sheet)
    for r, gun in enumerate(guns):
        for cidx, suffix in enumerate(cols):
            path = os.path.join(ITEM_TEX, f"{gun}{suffix}.png")
            if not os.path.exists(path):
                path = os.path.join(ITEM_TEX, f"{gun}.png")
            im = render(path, S)
            x = 10 + cidx * (S + 10)
            y = 10 + r * (S + 24)
            sheet.alpha_composite(im, (x, y))
            draw.text((x + 4, y + S + 4), f"{gun} [{labels[cidx]}]", fill=(225, 230, 240, 255))
    out = os.path.join(REPORTS, "blaster_3d_preview.png")
    sheet.save(out)
    print("Wrote", out)


if __name__ == "__main__":
    main()
