# -*- coding: utf-8 -*-
"""
Текстуры грунта под новую модель (AlienGruntModel) + светящийся слой глаз.
UV-раскладка зеркалит createBodyLayer — менять синхронно с моделью!
"""
import random
from PIL import Image

random.seed(777)
W = H = 64
skin = Image.new("RGBA", (W, H), (0, 0, 0, 0))
eyes = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sp = skin.load()
ep = eyes.load()


def clamp(v):
    return max(0, min(255, int(v)))


def region(u, v, bw, bh, bd):
    """Полный UV-прямоугольник бокса (включая все грани)."""
    return u, v, 2 * (bw + bd), bd + bh


def fill(u, v, w, h, base, mottle=14, vein=None):
    for y in range(v, min(H, v + h)):
        for x in range(u, min(W, u + w)):
            j = random.randint(-mottle, mottle)
            r, g, b = base
            sp[x, y] = (clamp(r + j), clamp(g + j), clamp(b + j), 255)
    if vein:
        for _ in range(max(1, w * h // 30)):
            x = random.randrange(u, min(W, u + w))
            y = random.randrange(v, min(H, v + h))
            sp[x, y] = (*vein, 255)


def front(u, v, bw, bh, bd):
    """Прямоугольник лицевой (северной) грани бокса."""
    return u + bd, v + bd, bw, bh


SKIN = (108, 122, 100)      # серо-зелёная кожа
SKIN_DARK = (84, 96, 78)
CARAPACE = (70, 78, 92)     # хитиновые пластины
BONE = (182, 176, 158)
CLAW = (44, 42, 50)
VEIN = (96, 62, 128)        # фиолетовые прожилки

# Голова-купол 8x7x8 @ (0,0): кожа + тёмный хитиновый верх + прожилки.
fill(*region(0, 0, 8, 7, 8), SKIN, vein=VEIN)
fill(8, 0, 8, 8, CARAPACE, mottle=10)            # верхняя грань — пластина
fx, fy, fw, fh = front(0, 0, 8, 7, 8)            # лицо: надбровная тень
for x in range(fx, fx + fw):
    sp[x, fy] = (*SKIN_DARK, 255)
    sp[x, fy + 1] = (*SKIN_DARK, 255)

# Челюсть 5x2x5 @ (0,16): бледная, с зубами по лицевой грани.
fill(*region(0, 16, 5, 2, 5), (140, 138, 120), mottle=8)
jx, jy, jw, jh = front(0, 16, 5, 2, 5)
for i in range(jw):
    if i % 2 == 0:
        sp[jx + i, jy] = (235, 230, 215, 255)    # зубы

# Глаза 3x2x1 @ (33,0): глянцево-чёрные с бликом; в eyes-слое — светятся.
fill(*region(33, 0, 3, 2, 1), (14, 12, 20), mottle=3)
ex0, ey0, ew, eh = region(33, 0, 3, 2, 1)
for y in range(ey0, ey0 + eh):
    for x in range(ex0, ex0 + ew):
        ep[x, y] = (110, 245, 190, 255)
sp[ex0 + 2, ey0 + 1] = (210, 255, 235, 255)      # блик

# Антенна 1x4x1 @ (42,0) + кончик 1x1x1 @ (48,0): кончик светится.
fill(*region(42, 0, 1, 4, 1), SKIN_DARK, mottle=8)
fill(*region(48, 0, 1, 1, 1), (205, 125, 255), mottle=6)
tx0, ty0, tw, th = region(48, 0, 1, 1, 1)
for y in range(ty0, ty0 + th):
    for x in range(tx0, tx0 + tw):
        ep[x, y] = (215, 145, 255, 255)

# Шипы хребта 1x2x1 @ (56,0): кость.
fill(*region(56, 0, 1, 2, 1), BONE, mottle=10)

# Тело: грудь 8x6x4 @ (0,24), талия 6x4x3 @ (24,16), таз 7x3x4 @ (0,35).
fill(*region(0, 24, 8, 6, 4), SKIN, vein=VEIN)
fill(*region(24, 16, 6, 4, 3), SKIN_DARK, vein=VEIN)
fill(*region(0, 35, 7, 3, 4), SKIN, mottle=10)
cx, cy, cw, ch = front(0, 24, 8, 6, 4)           # рёбра тенями на груди
for i in range(0, ch, 2):
    for x in range(cx + 1, cx + cw - 1):
        sp[x, cy + i] = (*SKIN_DARK, 255)

# Хвост 2x2x6 @ (44,8) и кончик 1x1x5 @ (44,17).
fill(*region(44, 8, 2, 2, 6), SKIN_DARK, vein=VEIN)
fill(*region(44, 17, 1, 1, 5), (96, 62, 128), mottle=10)

# Руки: плечо 3x3x3 @ (24,35), кисть 2x9x2 @ (38,35), коготь 1x4x1 @ (48,35).
fill(*region(24, 35, 3, 3, 3), CARAPACE, mottle=10)
fill(*region(38, 35, 2, 9, 2), SKIN, vein=VEIN)
fill(*region(48, 35, 1, 4, 1), CLAW, mottle=5)

# Ноги: бедро 4x6x4 @ (0,43), голень 3x5x3 @ (17,43), стопа 4x2x4 @ (30,50).
fill(*region(0, 43, 4, 6, 4), SKIN, vein=VEIN)
fill(*region(17, 43, 3, 5, 3), SKIN_DARK, mottle=10)
fill(*region(30, 50, 4, 2, 4), CLAW, mottle=6)

OUT = "src/main/resources/assets/alien-invasion/textures/entity"
skin.save(f"{OUT}/alien_grunt.png")
eyes.save(f"{OUT}/alien_grunt_eyes.png")
print("alien_grunt.png + alien_grunt_eyes.png")
