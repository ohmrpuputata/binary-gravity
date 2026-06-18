"""Offline preview of the Geiger dosimeter dial (mirrors InvasionHUDOverlay.drawGeigerDial
math) so the gauge geometry can be eyeballed without launching the client."""
import math
import os
from PIL import Image, ImageDraw

W, H, S = 112, 46, 6
R, MAX = 19, 25.0


def argb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def dark(c, t):
    r, g, b = argb(c)
    return (int(r * (1 - t)), int(g * (1 - t)), int(b * (1 - t)))


def draw_dial(field):
    im = Image.new("RGB", (W, H), (11, 13, 16))
    px, d = im.load(), ImageDraw.Draw(im)
    cx, cy = 4 + R + 1, H - 3
    frac = max(0.0, min(1.0, field / MAX))

    def dot(x, y, c):
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = c

    for i in range(65):
        tt = i / 64.0
        a = math.pi * (1 - tt)
        fv = tt * MAX
        zc = 0xFF6A4A if fv >= 18 else 0xFFC247 if fv >= 9 else 0x8FD46A
        ax = cx + round(R * math.cos(a))
        ay = cy - round(R * math.sin(a))
        c = argb(zc) if tt <= frac else dark(zc, 0.6)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                dot(ax + dx, ay + dy, c)
    for k in range(5):
        a = math.pi * (1 - k / 4.0)
        dot(cx + round((R - 4) * math.cos(a)), cy - round((R - 4) * math.sin(a)), (5, 6, 10))
    a = math.pi * (1 - frac)
    nx, ny = cx + round((R - 2) * math.cos(a)), cy - round((R - 2) * math.sin(a))
    steps = max(abs(nx - cx), abs(ny - cy)) or 1
    for i in range(steps + 1):
        dot(cx + (nx - cx) * i // steps, cy + (ny - cy) * i // steps, (216, 222, 227))
    for xx in range(cx - 2, cx + 3):
        for yy in range(cy - 2, cy + 1):
            dot(xx, yy, (5, 6, 10))
    d.text((cx + R + 6, cy - R + 3), "FON", fill=(154, 164, 172))
    d.text((cx + R + 6, cy - R + 12), f"{round(field*10)} uSv/h",
            fill=argb(0xFF6A4A if field >= 18 else 0xFFC247 if field >= 9 else 0x8FD46A))
    return im.resize((W * S, H * S), Image.NEAREST)


def main():
    fields = [1.5, 12.0, 22.0]
    sheet = Image.new("RGB", (W * S, H * S * len(fields)), (20, 22, 26))
    for i, f in enumerate(fields):
        sheet.paste(draw_dial(f), (0, i * H * S))
    out = "build/texture-audit/staging/geiger_dial_preview.png"
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
    print("->", out)


if __name__ == "__main__":
    main()
