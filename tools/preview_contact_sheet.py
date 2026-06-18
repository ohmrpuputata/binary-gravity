"""Assemble the in-game preview screenshots into one labelled contact sheet."""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "run", "screenshots")
REPORTS = os.path.join(ROOT, "build", "reports")

SLOT = ["alien", "green_ray", "gravity", "astral", "empty"]
cells = []
for n in range(16):
    p = os.path.join(SHOTS, f"ai_preview_{n}.png")
    if os.path.exists(p):
        cam = "1p" if n < 8 else "3p"
        cells.append((p, f"{n}: {SLOT[n % 5]} {cam}"))

if not cells:
    raise SystemExit("no screenshots found")

CW, CH = 320, 180
cols = 4
rows = (len(cells) + cols - 1) // cols
sheet = Image.new("RGBA", (cols * (CW + 8) + 8, rows * (CH + 22) + 8), (18, 20, 28, 255))
draw = ImageDraw.Draw(sheet)
for i, (p, label) in enumerate(cells):
    im = Image.open(p).convert("RGBA").resize((CW, CH), Image.BILINEAR)
    x = 8 + (i % cols) * (CW + 8)
    y = 8 + (i // cols) * (CH + 22)
    sheet.alpha_composite(im, (x, y))
    draw.text((x + 4, y + CH + 4), label, fill=(225, 230, 240, 255))
os.makedirs(REPORTS, exist_ok=True)
out = os.path.join(REPORTS, "ingame_preview.png")
sheet.save(out)
print("Wrote", out, f"({len(cells)} shots)")
