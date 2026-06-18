"""
Redesign of alien_residue: a living, animated alien biomass / spore slick.

16x16 tiling field of glossy purple spore-bubbles with bioluminescent cores,
animated across 4 frames as a slow "breathing" pulse (loops seamlessly).
Emits 16x64 sheets + .mcmeta for the whole family:
  alien_residue, infested_alien_residue, bloody_*, bloody_infested_*

Outputs to STAGING; promote with a copy step after reviewing the contact sheet.
"""
import os, math, random
from PIL import Image

STAGING = "build/texture-audit/staging/block"
N = 16          # frame size
FRAMES = 4
PURPLE_FAMILY = (150, 70, 165)

def clamp(v): return 0 if v < 0 else (255 if v > 255 else int(v))
def lerp(a, b, t): return a + (b - a) * t
def lerp3(a, b, t): return (lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t))

# ---- palette ----
BG_DARK  = (26, 15, 38)
BG_LIGHT = (46, 27, 64)
BODY_EDGE = (54, 28, 76)
BODY_MID  = (104, 58, 138)
RIM_DARK  = (32, 15, 48)
HILIGHT   = (210, 174, 232)
CORE_HOT  = (214, 150, 240)
CORE_CYAN = (150, 196, 255)
VEIN      = (40, 21, 56)

def build_field(seed, n_bubbles, glow_boost=1.0, cyan_chance=0.10):
    rng = random.Random("residue:" + seed)
    bubbles = []
    for _ in range(n_bubbles):
        bubbles.append(dict(
            cx=rng.uniform(0, N), cy=rng.uniform(0, N),
            r=rng.uniform(1.7, 3.3),
            phase=rng.uniform(0, math.tau),
            gphase=rng.uniform(0, math.tau),
            core=(rng.random() < 0.55),
            cyan=(rng.random() < cyan_chance),
            glow=rng.uniform(0.5, 1.0) * glow_boost,
        ))
    # a few faint organic veins (short tile-wrapped random walks)
    veins = []
    for _ in range(rng.randint(2, 3)):
        x, y = rng.uniform(0, N), rng.uniform(0, N)
        ang = rng.uniform(0, math.tau)
        pts = []
        for _ in range(rng.randint(6, 11)):
            pts.append((x % N, y % N))
            ang += rng.uniform(-0.7, 0.7)
            x += math.cos(ang); y += math.sin(ang)
        veins.append(pts)
    bg_tilt = rng.uniform(-0.18, 0.18)
    return dict(bubbles=bubbles, veins=veins, bg_tilt=bg_tilt)

def render_frame(field, k):
    ph = math.tau * (k / FRAMES)
    buf = [[None] * N for _ in range(N)]
    # background: soft vertical gradient + tilt, no hard noise
    for y in range(N):
        for x in range(N):
            t = y / (N - 1)
            t = min(1.0, max(0.0, t + field["bg_tilt"] * math.sin(x / N * math.tau)))
            buf[y][x] = list(lerp3(BG_DARK, BG_LIGHT, t)) + [255]

    def blend(x, y, col, a):
        x %= N; y %= N
        p = buf[y][x]
        p[0] = lerp(p[0], col[0], a); p[1] = lerp(p[1], col[1], a); p[2] = lerp(p[2], col[2], a)

    # veins (under bubbles)
    for pts in field["veins"]:
        for (vx, vy) in pts:
            blend(int(vx), int(vy), VEIN, 0.55)

    # glossy bubbles
    for b in field["bubbles"]:
        pulse = 0.5 + 0.5 * math.sin(ph + b["phase"])
        gpulse = 0.5 + 0.5 * math.sin(ph * 1.0 + b["gphase"])
        r = b["r"] * (0.82 + 0.20 * pulse)
        # highlight offset (top-left light)
        hx, hy = b["cx"] - 0.42 * r, b["cy"] - 0.42 * r
        R = int(r) + 2
        for dy in range(-R, R + 1):
            for dx in range(-R, R + 1):
                d = math.hypot(dx, dy)
                if d > r:
                    continue
                px, py = b["cx"] + dx, b["cy"] + dy
                tt = d / r                      # 0 centre .. 1 rim
                body = lerp3(BODY_MID, BODY_EDGE, tt ** 0.8)
                a = 0.94 * (1.0 - 0.12 * tt)
                blend(int(px), int(py), body, a)
                # dark rim so each bead reads as a distinct glossy egg
                if tt > 0.8:
                    blend(int(px), int(py), RIM_DARK, 0.6 * (tt - 0.8) / 0.2)
                # tight specular highlight
                hd = math.hypot((b["cx"] + dx) - hx, (b["cy"] + dy) - hy)
                hl = max(0.0, 1.0 - hd / (0.55 * r))
                if hl > 0:
                    blend(int(px), int(py), HILIGHT, 0.78 * hl ** 3 * (0.7 + 0.3 * pulse))
        # bioluminescent core
        if b["core"]:
            col = CORE_CYAN if b["cyan"] else CORE_HOT
            ga = 0.85 * b["glow"] * (0.45 + 0.55 * gpulse)
            blend(int(b["cx"]), int(b["cy"]), col, min(1.0, ga))
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                blend(int(b["cx"]) + ox, int(b["cy"]) + oy, col, ga * 0.35)

    out = Image.new("RGBA", (N, N))
    op = out.load()
    for y in range(N):
        for x in range(N):
            p = buf[y][x]
            op[x, y] = (clamp(p[0]), clamp(p[1]), clamp(p[2]), 255)
    return out

def blood_layer(seed):
    """16x16 dark dried-blood splatter, applied identically to every frame."""
    rng = random.Random("rblood:" + seed)
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    p = im.load()
    def grow(n):
        cells = {(0, 0)}; fr = [(0, 0)]
        while fr and len(cells) < n:
            cx, cy = fr.pop(rng.randrange(len(fr)))
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if rng.random() < 0.55:
                    nc = (cx + dx, cy + dy)
                    if nc not in cells:
                        cells.add(nc); fr.append(nc)
        return list(cells)
    def put(x, y, col, al):
        if 0 <= x < N and 0 <= y < N and al > p[x, y][3]:
            p[x, y] = (col[0], col[1], col[2], al)
    ox, oy = rng.randint(3, N - 4), rng.randint(3, N - 4)
    cells = grow(rng.randint(8, 14)); cs = set(cells)
    for dx, dy in cells:
        nb = sum((dx + ax, dy + ay) in cs for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        col, al = ((116, 14, 18), 216) if nb >= 4 else (((92, 10, 14), 200) if nb >= 2 else ((72, 8, 11), 150))
        put(ox + dx, oy + dy, col, al)
    for _ in range(rng.randint(2, 4)):
        put(rng.randint(1, N - 2), rng.randint(1, N - 2), (104, 12, 16), 175)
    return im

def tint_inplace(im, t, dark, tone=PURPLE_FAMILY):
    px = im.load(); w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0: continue
            L = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            nr = (r * (1 - t) + tone[0] * L * t) * dark
            ng = (g * (1 - t) + tone[1] * L * t) * dark
            nb = (b * (1 - t) + tone[2] * L * t) * dark
            px[x, y] = (clamp(nr), clamp(ng), clamp(nb), a)
    return im

def assemble(field, glow=1.0):
    sheet = Image.new("RGBA", (N, N * FRAMES), (0, 0, 0, 0))
    for k in range(FRAMES):
        sheet.alpha_composite(render_frame(field, k), (0, k * N))
    return sheet

def apply_blood(sheet, seed):
    bl = blood_layer(seed)
    out = sheet.copy()
    for k in range(FRAMES):
        out.alpha_composite(bl, (0, k * N))
    return out

MCMETA = '{"animation": {"frametime": 8, "interpolate": true}}'

def main():
    os.makedirs(STAGING, exist_ok=True)
    base_field = build_field("base", n_bubbles=7, glow_boost=1.0)
    inf_field  = build_field("base", n_bubbles=7, glow_boost=1.0)  # same layout
    # infested = same field but hotter glow + an extra spore or two
    inf_field["bubbles"] = base_field["bubbles"] + build_field("inf_extra", 2, glow_boost=1.0, cyan_chance=0.5)["bubbles"]
    for b in inf_field["bubbles"]:
        b["glow"] *= 1.6
        b["core"] = True

    base = assemble(base_field)
    infested = assemble(inf_field)

    out = {
        "alien_residue.png": base,
        "infested_alien_residue.png": infested,
        "bloody_alien_residue.png": apply_blood(base, "br"),
        "bloody_infested_alien_residue.png": apply_blood(infested, "bir"),
    }
    for name, img in out.items():
        img.save(os.path.join(STAGING, name))
        with open(os.path.join(STAGING, name + ".mcmeta"), "w") as f:
            f.write(MCMETA)
    print("wrote", list(out.keys()), "+ .mcmeta each ->", STAGING)

if __name__ == "__main__":
    main()
