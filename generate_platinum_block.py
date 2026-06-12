# -*- coding: utf-8 -*-
"""Текстура платинового блока: берём palladium_block.png за основу
и перекрашиваем в холодный серебристо-белый металл платины."""
from PIL import Image

SRC = "src/main/resources/assets/alien-invasion/textures/block/palladium_block.png"
DST = "src/main/resources/assets/alien-invasion/textures/block/platinum_block.png"

img = Image.open(SRC).convert("RGBA")
out = Image.new("RGBA", img.size)

for y in range(img.height):
    for x in range(img.width):
        r, g, b, a = img.getpixel((x, y))
        # яркость исходного пикселя сохраняет рисунок блока
        lum = 0.299 * r + 0.587 * g + 0.114 * b
        # платина: светлый холодный металл с лёгкой голубизной
        nr = min(255, int(lum * 0.92 + 38))
        ng = min(255, int(lum * 0.95 + 40))
        nb = min(255, int(lum * 1.00 + 46))
        out.putpixel((x, y), (nr, ng, nb, a))

out.save(DST)
print("OK ->", DST)
