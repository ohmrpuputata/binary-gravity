"""Тонкие лужи-декали от дождя: вода (синяя) и кислота (зелёная).

Полупрозрачный «мокрый» лист 16x16 с лёгкой рябью/бликами — кладётся плоской
верхней гранью на пол, сквозь альфу просвечивает земля. Тайлится бесшовно.
"""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "alien-invasion", "textures", "block")
os.makedirs(OUT, exist_ok=True)


def puddle(name, base, edge, sheen, alpha, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # лёгкая неоднородность цвета
            j = rnd.randint(-12, 12)
            r = max(0, min(255, base[0] + j))
            g = max(0, min(255, base[1] + j))
            b = max(0, min(255, base[2] + j))
            a = alpha + rnd.randint(-18, 14)
            px[x, y] = (r, g, b, max(40, min(210, a)))
    # тёмная кайма по краям — лужа «глубже» внутри
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            r, g, b, a = px[x, y]
            px[x, y] = (max(0, r - 25), max(0, g - 25), max(0, b - 20), max(30, a - 35))
    # несколько светлых бликов ряби/пузырей
    for _ in range(10):
        x = rnd.randint(1, 14)
        y = rnd.randint(1, 14)
        px[x, y] = (sheen[0], sheen[1], sheen[2], min(220, alpha + 60))
    img.save(os.path.join(OUT, name))
    print("wrote", name)


# вода: холодный синий, голубоватые блики
puddle("rain_puddle.png", base=(54, 104, 180), edge=(30, 60, 120),
       sheen=(150, 200, 245), alpha=120, seed=7)
# кислота: ядовито-зелёный, светло-зелёные пузыри
puddle("rain_puddle_acid.png", base=(92, 176, 58), edge=(50, 110, 30),
       sheen=(190, 245, 130), alpha=140, seed=13)
