"""4 простых скина для выживших-NPC (формат player-скина 64x64).

Заливаем зоны частей тела (layer 0) цветом: голова/руки — кожа (на лбу — волосы,
на лице — глаза), торс — рубашка, ноги — штаны. Разные палитры = разные «выжившие».
Грубо, но на дистанции читается как разные люди. Второй слой (шапка/куртка) пуст.
"""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "alien-invasion", "textures", "entity")
os.makedirs(OUT, exist_ok=True)

# Прямоугольники зон (layer 0): голова, торс, прав.рука, лев.рука, прав.нога, лев.нога
HEAD = (0, 0, 32, 16)
BODY = (16, 16, 40, 32)
R_ARM = (40, 16, 56, 32)
L_ARM = (32, 48, 48, 64)
R_LEG = (0, 16, 16, 32)
L_LEG = (16, 48, 32, 64)

# (кожа, волосы, рубашка, штаны, обувь)
PALETTES = [
    ((224, 178, 140), (90, 60, 35), (70, 110, 70), (40, 48, 80), (40, 35, 30)),   # бледный, зелёная рубашка
    ((205, 150, 110), (25, 22, 20), (150, 55, 50), (90, 70, 45), (30, 25, 22)),   # загар, красная
    ((140, 100, 75), (20, 18, 16), (90, 92, 96), (45, 45, 50), (25, 22, 20)),     # тёмный, серая
    ((230, 190, 160), (190, 165, 90), (60, 90, 140), (80, 82, 86), (35, 30, 28)),  # блондин, синяя
]


def fill(px, box, c):
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            px[x, y] = (c[0], c[1], c[2], 255)


def make(idx, skin, hair, shirt, pants, shoe):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    fill(px, HEAD, skin)
    fill(px, BODY, shirt)
    fill(px, R_ARM, skin)
    fill(px, L_ARM, skin)
    fill(px, R_LEG, pants)
    fill(px, L_LEG, pants)
    # волосы: верх головы (top-грань 8,0..16,8) + затылок (24,8..32,16) + лоб (фронт верх)
    fill(px, (8, 0, 24, 4), hair)        # макушка/виски сверху
    fill(px, (24, 8, 32, 11), hair)      # затылок
    fill(px, (8, 8, 16, 10), hair)       # чёлка надо лбом (фронт-грань верх)
    # глаза на фронт-грани лица (8,8..16,16)
    px[10, 12] = (30, 30, 35, 255)
    px[13, 12] = (30, 30, 35, 255)
    # рукава рубашки на плечах
    fill(px, (R_ARM[0], R_ARM[1], R_ARM[2], R_ARM[1] + 4), shirt)
    fill(px, (L_ARM[0], L_ARM[1], L_ARM[2], L_ARM[1] + 4), shirt)
    # обувь: низ ног
    fill(px, (R_LEG[0], R_LEG[3] - 4, R_LEG[2], R_LEG[3]), shoe)
    fill(px, (L_LEG[0], L_LEG[3] - 4, L_LEG[2], L_LEG[3]), shoe)
    img.save(os.path.join(OUT, f"scavenger_{idx}.png"))
    print("wrote scavenger_" + str(idx) + ".png")


for i, p in enumerate(PALETTES):
    make(i, *p)
