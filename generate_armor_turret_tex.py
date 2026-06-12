# -*- coding: utf-8 -*-
"""Голова турели + детальные текстуры всей брони (швы/панели/тени/акценты) +
нибириевая броня (layer + item-иконки)."""
import random
from PIL import Image

ASSETS = "src/main/resources/assets/alien-invasion"


def clamp(v):
    return max(0, min(255, int(v)))


# ---- голова турели 64x64 (тёмный металл + светящиеся жерла) ----
random.seed(900)
head = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
hp = head.load()
for y in range(64):
    for x in range(64):
        g = 70 + random.randint(-8, 8) - (12 if (x % 6 == 0 or y % 6 == 0) else 0)
        hp[x, y] = (g, clamp(g + 6), clamp(g + 12), 255)
# светящиеся жерла стволов (область texOffs 34,9 -> muzzle 3x3)
for bx in (34, 34):
    for y in range(9, 14):
        for x in range(34, 40):
            hp[x, y] = (140, 255, 150, 255)
head.save(ASSETS + "/textures/block/plasma_turret_head.png")
print("tex plasma_turret_head 64x64")


def armor_layer(name, base, accent, dark, seed, layer2=False):
    """Детальная броня: панели, заклёпки, тени по краям зон, акцентные канты."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p = img.load()
    rnd = random.Random(seed)
    # Зоны развёртки брони (vanilla humanoid armor layout).
    if not layer2:
        zones = [(0, 0, 32, 16, "head"), (16, 16, 24, 16, "chest"),
                 (40, 16, 16, 16, "rarm"), (0, 16, 16, 16, "rleg")]
    else:
        zones = [(0, 16, 16, 16, "rleg"), (16, 16, 16, 16, "leg2"), (0, 0, 16, 16, "boot")]
    for x0, y0, w, h, kind in zones:
        for y in range(y0, min(32, y0 + h)):
            for x in range(x0, min(64, x0 + w)):
                edge = (x == x0 or x == x0 + w - 1 or y == y0 or y == y0 + h - 1)
                seam = ((x - x0) % 8 == 0 or (y - y0) % 7 == 0)
                shade = 0
                if edge:
                    shade = -34          # вытравленный контур пластины
                elif seam:
                    shade = -16          # панельные швы
                j = rnd.randint(-8, 8)
                c = dark if edge else base
                p[x, y] = (clamp(c[0] + j + shade), clamp(c[1] + j + shade),
                           clamp(c[2] + j + shade), 255)
        # заклёпки по углам пластины
        for (rx, ry) in ((x0 + 2, y0 + 2), (x0 + w - 3, y0 + 2),
                         (x0 + 2, y0 + h - 3), (x0 + w - 3, y0 + h - 3)):
            if rx < 64 and ry < 32:
                p[rx, ry] = (clamp(base[0] + 40), clamp(base[1] + 40), clamp(base[2] + 40), 255)
    if not layer2:
        # светящийся визор на шлеме (лицо: 8..16 x 8..16) + нагрудный индикатор
        for y in range(10, 13):
            for x in range(9, 15):
                p[x, y] = (*accent, 255)
        for x in range(20, 28):  # кант наплечной пластины
            p[x, 16] = (clamp(base[0] * 0.5), clamp(base[1] * 0.5), clamp(base[2] * 0.5), 255)
        for y in range(22, 24):  # нагрудный индикатор
            for x in range(21, 27):
                p[x, y] = (*accent, 255)
    return img


# (имя, base, accent, dark)
SETS = {
    "platinum":     ((208, 212, 222), (120, 230, 255), (150, 154, 168)),
    "palladium":    ((150, 200, 188), (120, 255, 210), (96, 150, 140)),
    "alien_hazmat": ((226, 188, 74),  (255, 170, 60),  (160, 128, 40)),
    "alien_chem":   ((96, 162, 84),   (160, 255, 120), (60, 110, 56)),
    "cosmic":       ((124, 80, 188),  (255, 215, 120), (78, 48, 120)),
    "nibirium":     ((58, 60, 74),    (190, 90, 255),  (34, 34, 46)),  # тёмный металл, фиолет-акцент
}
for name, (base, accent, dark) in SETS.items():
    armor_layer(name, base, accent, dark, hash(name) & 0xFFFF, False).save(
        ASSETS + "/textures/models/armor/" + name + "_layer_1.png")
    armor_layer(name, base, accent, dark, (hash(name) >> 3) & 0xFFFF, True).save(
        ASSETS + "/textures/models/armor/" + name + "_layer_2.png")
    print("armor layers: " + name)


# ---- иконки предметов нибириевой брони (16x16) ----
def armor_icon(piece, base, accent, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    rnd = random.Random(seed)

    def box(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                edge = (x == x0 or x == x1 - 1 or y == y0 or y == y1 - 1)
                j = rnd.randint(-10, 10)
                c = tuple(clamp(v * 0.55) for v in base) if edge else base
                p[x, y] = (clamp(c[0] + j), clamp(c[1] + j), clamp(c[2] + j), 255)

    if piece == "helmet":
        box(4, 3, 12, 10)
        for x in range(5, 11):
            p[x, 7] = (*accent, 255)        # визор
    elif piece == "chestplate":
        box(4, 3, 12, 13)
        p[7, 6] = (*accent, 255); p[8, 6] = (*accent, 255)
    elif piece == "leggings":
        box(4, 3, 12, 9)
        box(5, 9, 7, 14)
        box(9, 9, 11, 14)
    else:  # boots
        box(3, 6, 7, 13)
        box(9, 6, 13, 13)
    return img


for piece in ("helmet", "chestplate", "leggings", "boots"):
    armor_icon(piece, (58, 60, 74), (190, 90, 255), hash(piece) & 0xFFFF).save(
        ASSETS + "/textures/item/nibirium_" + piece + ".png")
    print("icon nibirium_" + piece)
print("DONE TEX")
