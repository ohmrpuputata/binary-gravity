# -*- coding: utf-8 -*-
"""Текстуры финального акта: скин охотника Макса (чёрная броня, красный визор),
портал-разрыв, реактор Максбетова и жетон охотника."""
import math
import random
from PIL import Image, ImageDraw

random.seed(414)
BASE = "src/main/resources/assets/alien-invasion/textures/"


def noise_fill(img, box, base, spread=10):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            d = random.randint(-spread, spread)
            r = max(0, min(255, base[0] + d))
            g = max(0, min(255, base[1] + d))
            b = max(0, min(255, base[2] + d))
            img.putpixel((x, y), (r, g, b, 255))


def hunter_skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dark = (28, 28, 32)      # матовая чёрная броня
    plate = (45, 45, 52)     # пластины
    accent = (140, 30, 30)   # тёмно-красные вставки
    # Базовый слой: заполняем все стандартные UV-зоны скина бронёй.
    zones = [
        (0, 0, 32, 16),    # голова (все грани) + шлем-низ
        (32, 0, 64, 16),   # hat-слой головы — внешний шлем
        (0, 16, 56, 32),   # тело+рука+ноги верхний ряд
        (0, 32, 56, 48),   # второй слой (куртка)
        (16, 48, 48, 64),  # левая рука/нога 1.8-формат
    ]
    for z in zones:
        noise_fill(img, z, dark, 8)
    # Нагрудные пластины и пояс.
    noise_fill(img, (20, 21, 28, 27), plate, 6)   # грудь
    noise_fill(img, (20, 27, 28, 29), accent, 8)  # пояс
    # Плечи-акценты.
    noise_fill(img, (44, 20, 48, 22), accent, 8)
    # КРАСНЫЙ ВИЗОР на лице (front головы: x8..16, y8..16; глаза ~y11-13).
    d = ImageDraw.Draw(img)
    d.rectangle((9, 11, 14, 12), fill=(255, 40, 40, 255))
    # Визор на hat-слое (front: x40..48).
    d.rectangle((41, 11, 46, 12), fill=(255, 60, 60, 255))
    img.save(BASE + "entity/hunter.png")

    # Слой свечения: прозрачный везде, кроме визора.
    visor = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dv = ImageDraw.Draw(visor)
    dv.rectangle((9, 11, 14, 12), fill=(255, 50, 50, 255))
    dv.rectangle((41, 11, 46, 12), fill=(255, 70, 70, 255))
    visor.save(BASE + "entity/hunter_visor.png")


def portal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            dist = math.sqrt(dx * dx + dy * dy)
            ang = math.atan2(dy, dx)
            swirl = math.sin(ang * 3 + dist * 1.4) * 0.5 + 0.5
            v = int(70 + 110 * swirl)
            a = 190 if dist < 8.5 else 150
            img.putpixel((x, y), (v, int(v * 0.35), min(255, v + 70), a))
    img.save(BASE + "block/alien_portal.png")


def reactor():
    side = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    noise_fill(side, (0, 0, 16, 16), (52, 50, 48), 7)
    d = ImageDraw.Draw(side)
    d.rectangle((0, 0, 15, 1), fill=(30, 30, 30, 255))
    d.rectangle((0, 14, 15, 15), fill=(30, 30, 30, 255))
    # Светящееся ядро-полоса.
    for y in range(5, 11):
        for x in range(3, 13):
            pulse = 200 + random.randint(-25, 25)
            d.point((x, y), fill=(255, min(255, pulse), 40, 255))
    d.rectangle((3, 5, 12, 5), fill=(120, 60, 10, 255))
    d.rectangle((3, 10, 12, 10), fill=(120, 60, 10, 255))
    # Предупреждающие полосы по углам.
    for i in range(0, 16, 4):
        d.point((i, 2), fill=(230, 180, 30, 255))
        d.point((i + 1, 2), fill=(40, 40, 40, 255))
        d.point((i, 13), fill=(230, 180, 30, 255))
        d.point((i + 1, 13), fill=(40, 40, 40, 255))
    side.save(BASE + "block/planet_reactor_side.png")

    top = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    noise_fill(top, (0, 0, 16, 16), (48, 46, 44), 6)
    dt = ImageDraw.Draw(top)
    for r in range(7, 2, -2):
        dt.ellipse((8 - r, 8 - r, 7 + r, 7 + r), outline=(90, 90, 95, 255))
    dt.ellipse((5, 5, 10, 10), fill=(255, 170, 30, 255))
    dt.ellipse((6, 6, 9, 9), fill=(255, 230, 90, 255))
    top.save(BASE + "block/planet_reactor_top.png")


def token():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((2, 2, 13, 13), fill=(180, 140, 40, 255), outline=(90, 65, 15, 255))
    d.ellipse((4, 4, 11, 11), outline=(230, 200, 90, 255))
    # Гравировка "М"
    d.line((6, 10, 6, 6), fill=(60, 40, 10, 255))
    d.line((6, 6, 8, 8), fill=(60, 40, 10, 255))
    d.line((8, 8, 10, 6), fill=(60, 40, 10, 255))
    d.line((10, 6, 10, 10), fill=(60, 40, 10, 255))
    img.save(BASE + "item/hunter_token.png")


hunter_skin()
portal()
reactor()
token()
print("OK: hunter skin+visor, portal, reactor side/top, token")
