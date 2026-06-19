"""Текстуры новых масок из био-фильтра: тканевый респиратор и боевой противогаз.

Перекрашиваем существующие текстуры (item 16x16 + модель лица 64x32) по яркости —
сохраняем ВСЮ детализацию/тени оригинала, меняем только палитру. Так новые маски
сразу выглядят как родные, без рисования с нуля.
"""
import os
from PIL import Image

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "alien-invasion", "textures")
ITEM = os.path.join(BASE, "item")
FACE = os.path.join(BASE, "models", "armor")


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def recolor(src_path, dst_path, dark, light):
    """Монохромный тинт по яркости: тёмное -> dark, светлое -> light, альфа сохр."""
    img = Image.open(src_path).convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            nr, ng, nb = lerp(dark, light, lum)
            px[x, y] = (nr, ng, nb, a)
    img.save(dst_path)
    print("wrote", os.path.relpath(dst_path, BASE))


# Тканевый респиратор — тёплый хаки/песочный, простая мягкая маска.
recolor(os.path.join(ITEM, "bio_filter_mask.png"), os.path.join(ITEM, "cloth_respirator.png"),
        dark=(58, 48, 36), light=(196, 176, 138))
recolor(os.path.join(FACE, "bio_filter_mask.png"), os.path.join(FACE, "cloth_respirator.png"),
        dark=(58, 48, 36), light=(196, 176, 138))

# Боевой противогаз — тёмная олива/чёрная резина, серьёзный вид.
recolor(os.path.join(ITEM, "bio_filter_mask.png"), os.path.join(ITEM, "gas_mask.png"),
        dark=(20, 26, 18), light=(108, 124, 86))
recolor(os.path.join(FACE, "bio_filter_mask.png"), os.path.join(FACE, "gas_mask.png"),
        dark=(20, 26, 18), light=(108, 124, 86))
