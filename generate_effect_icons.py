"""Generate 18x18 mob-effect status icons for the mod's custom effects.

Minecraft 1.20.5+/1.21 auto-loads effect icons from
assets/<namespace>/textures/mob_effect/<effect_id>.png (18x18), no registration
needed. We draw each symbol at high resolution and downscale with LANCZOS so the
tiny icons come out smooth instead of jagged.
"""
import math
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "src", "main", "resources", "assets", "alien-invasion", "textures", "mob_effect")
os.makedirs(OUT, exist_ok=True)

S = 72  # supersample canvas
C = S // 2


def canvas():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def finish(name, im):
    im = im.resize((18, 18), Image.LANCZOS)
    im.save(os.path.join(OUT, name + ".png"))
    print("effect icon", name)


def radiation():
    im = canvas()
    d = ImageDraw.Draw(im)
    col = (176, 214, 48, 255)
    dark = (44, 64, 10, 255)
    # Three 60-degree wedges with 60-degree gaps = radiation trefoil.
    for start in (30, 150, 270):
        d.pieslice([C - 30, C - 30, C + 30, C + 30], start, start + 60, fill=col, outline=dark, width=2)
    d.ellipse([C - 9, C - 9, C + 9, C + 9], fill=col, outline=dark, width=2)
    d.ellipse([C - 4, C - 4, C + 4, C + 4], fill=(28, 36, 10, 255))
    return im


def infection():
    im = canvas()
    d = ImageDraw.Draw(im)
    col = (122, 176, 70, 255)
    glow = (170, 222, 110, 255)
    # Biohazard-style: three rings around a hub (distinct from the radiation trefoil).
    for ang in (90, 210, 330):
        a = math.radians(ang)
        ox = C + 17 * math.cos(a)
        oy = C - 17 * math.sin(a)
        d.ellipse([ox - 13, oy - 13, ox + 13, oy + 13], outline=col, width=6)
    d.ellipse([C - 7, C - 7, C + 7, C + 7], outline=glow, width=5)
    d.ellipse([C - 2, C - 2, C + 2, C + 2], fill=glow)
    return im


def psychic_pressure():
    im = canvas()
    d = ImageDraw.Draw(im)
    col = (188, 96, 232, 255)
    # Hypnotic spiral of widening arcs.
    for i, r in enumerate(range(7, 33, 6)):
        d.arc([C - r, C - r, C + r, C + r], 10 + i * 55, 220 + i * 55, fill=col, width=5)
    d.ellipse([C - 4, C - 4, C + 4, C + 4], fill=(236, 196, 255, 255))
    return im


def anti_gravity():
    im = canvas()
    d = ImageDraw.Draw(im)
    col = (96, 214, 232, 255)
    # Three rising chevrons = lift / low gravity.
    for yy in (50, 34, 18):
        d.line([(C - 18, yy + 12), (C, yy)], fill=col, width=6, joint="curve")
        d.line([(C, yy), (C + 18, yy + 12)], fill=col, width=6, joint="curve")
    return im


def marked():
    im = canvas()
    d = ImageDraw.Draw(im)
    col = (232, 64, 64, 255)
    d.ellipse([C - 27, C - 27, C + 27, C + 27], outline=col, width=6)
    # Crosshair ticks.
    for (x0, y0, x1, y1) in [(C - 34, C, C - 12, C), (C + 12, C, C + 34, C),
                             (C, C - 34, C, C - 12), (C, C + 12, C, C + 34)]:
        d.line([(x0, y0), (x1, y1)], fill=col, width=6)
    d.ellipse([C - 5, C - 5, C + 5, C + 5], fill=col)
    return im


def _skull(d, cx, cy, r, bone, dark):
    # Stylised skull: domed cranium, dark sockets + nasal cavity, a row of teeth.
    d.ellipse([cx - r, cy - r, cx + r, cy + int(r * 0.7)], fill=bone, outline=dark, width=2)
    d.rectangle([cx - int(r * 0.6), cy + int(r * 0.3), cx + int(r * 0.6), cy + int(r * 0.95)], fill=bone, outline=dark, width=2)
    er = max(2, int(r * 0.34))
    d.ellipse([cx - int(r * 0.55) - er, cy - er, cx - int(r * 0.55) + er, cy + er], fill=dark)
    d.ellipse([cx + int(r * 0.55) - er, cy - er, cx + int(r * 0.55) + er, cy + er], fill=dark)
    d.polygon([(cx, cy + int(r * 0.1)), (cx - 3, cy + int(r * 0.45)), (cx + 3, cy + int(r * 0.45))], fill=dark)
    for tx in (-int(r * 0.4), 0, int(r * 0.4)):
        d.line([(cx + tx, cy + int(r * 0.55)), (cx + tx, cy + int(r * 0.95))], fill=dark, width=2)


def strong_radiation():
    im = radiation()  # start from the trefoil, then stamp a small skull in the hub
    d = ImageDraw.Draw(im)
    _skull(d, C, C, 13, (226, 232, 188, 255), (32, 40, 16, 255))
    return im


def irradiation():
    im = canvas()
    d = ImageDraw.Draw(im)
    _skull(d, C, C, 28, (210, 224, 176, 255), (30, 38, 18, 255))
    return im


def main():
    finish("strong_radiation", strong_radiation())
    finish("irradiation", irradiation())
    finish("radiation", radiation())
    finish("infection", infection())
    finish("psychic_pressure", psychic_pressure())
    finish("anti_gravity", anti_gravity())
    finish("marked", marked())
    print("done.")


if __name__ == "__main__":
    main()
