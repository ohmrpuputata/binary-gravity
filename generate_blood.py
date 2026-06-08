"""Generate a realistic blood-pool decal texture (16x16, top-down).

Irregular crimson pool: dark glossy centre, lighter rim, feathered transparent
edge, plus a few scattered droplets. Used by the flat BloodPoolBlock decal.
"""
import math
import os
import random
from PIL import Image

BLOCK = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                     "src", "main", "resources", "assets", "alien-invasion", "textures", "block")
os.makedirs(BLOCK, exist_ok=True)

DARK = (78, 4, 6)
MID = (126, 12, 12)
RIM = (168, 30, 24)
SHEEN = (206, 70, 58)


def gen():
    random.seed(20260607)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx, cy = 7.6, 8.0
    # Per-angle wobble radius for an irregular blob outline.
    wob = [3.9 + random.uniform(-1.4, 1.6) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            ang = int(((math.atan2(dy, dx) + math.pi) / (2 * math.pi)) * 16) % 16
            r = wob[ang]
            if d <= r:
                t = d / r  # 0 centre -> 1 rim
                if t < 0.35:
                    c = DARK
                elif t < 0.7:
                    c = MID
                else:
                    c = RIM
                n = random.randint(-10, 10)
                a = 255 if t < 0.82 else 200 if t < 0.93 else 130  # feathered edge
                px[x, y] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n // 2)),
                            max(0, min(255, c[2] + n // 2)), a)
    # Glossy highlights near the centre.
    for (hx, hy) in [(6, 6), (7, 7), (9, 8)]:
        px[hx, hy] = SHEEN + (255,)
    # A few stray droplets around the pool.
    for _ in range(5):
        bx = random.randint(1, 14)
        by = random.randint(1, 14)
        if math.hypot(bx - cx, by - cy) > 5.0:
            px[bx, by] = MID + (235,)
            if 0 <= bx + 1 < 16:
                px[bx + 1, by] = DARK + (190,)
    img.save(os.path.join(BLOCK, "blood_pool.png"))
    print("wrote blood_pool.png")


if __name__ == "__main__":
    gen()
