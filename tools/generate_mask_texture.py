"""Текстура маски (модель BioFilterMaskModel, 64x32) — рисуем УЗНАВАЕМЫЙ противогаз,
а не тёмное пятно: светлый корпус-резина, ТЁМНЫЙ визор/линза, светлые фильтры, тёмные
ремни. Контраст + светлая база -> на лице читается как маска.

Затем делаем варианты тем же luminance-тинтом, но НЕ в чёрный:
  cloth_respirator — песочный хаки; gas_mask — средняя олива (не почти-чёрный, как было).

Раскладка UV взята из боксов BioFilterMaskModel.createBodyLayer (texOffs/размеры).
"""
import os
from PIL import Image

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "alien-invasion", "textures", "models", "armor")
ITEM = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "alien-invasion", "textures", "item")
PREVIEW = os.path.join(os.path.dirname(__file__), "..", "build", "texture-audit")
os.makedirs(PREVIEW, exist_ok=True)

W, H = 64, 32

BODY = (98, 108, 88, 255)      # светлая олива-резина (корпус)
BODY_HI = (124, 134, 110, 255)  # блик корпуса
VISOR = (24, 28, 34, 255)      # тёмное стекло визора/линзы
VISOR_HI = (78, 128, 118, 255)  # блик на стекле
FILTER = (150, 150, 140, 255)  # светлый цилиндр фильтра
FILTER_D = (44, 46, 42, 255)   # отверстие/край фильтра
STRAP = (40, 42, 38, 255)      # тёмные ремни


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < W and 0 <= y < H:
                px[x, y] = c


def build_base():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    # --- face_mask: texOffs(0,0), бокс 7x4x1 (вся развёртка 0..16 x 0..5) ---
    rect(px, 0, 0, 16, 5, BODY)
    rect(px, 1, 0, 8, 1, BODY_HI)          # верхняя грань — блик
    # передняя грань (1,1)-(8,5): ВИЗОР
    rect(px, 1, 1, 8, 5, BODY)
    rect(px, 2, 1, 7, 4, VISOR)            # тёмное стекло
    px[3, 2] = VISOR_HI                     # блики-«глаза»
    px[5, 2] = VISOR_HI
    rect(px, 2, 4, 7, 5, BODY_HI)          # светлая кромка под визором
    # --- respirator: texOffs(0,10), бокс 3x2.8x1 -> фильтр-«рыло» ---
    rect(px, 0, 10, 5, 14, BODY)
    rect(px, 1, 11, 4, 14, FILTER)
    px[2, 12] = FILTER_D
    # --- боковые фильтры: texOffs(12,10) и (20,10), бокс 2x2.5x2 ---
    rect(px, 12, 10, 16, 15, BODY)
    rect(px, 14, 11, 16, 15, FILTER)
    px[14, 13] = FILTER_D
    rect(px, 20, 10, 24, 15, BODY)
    rect(px, 22, 11, 24, 15, FILTER)
    px[22, 13] = FILTER_D
    # --- ремни: texOffs(0,17) (верхний) и (20,17) (боковые) ---
    rect(px, 0, 17, 11, 19, STRAP)
    rect(px, 20, 17, 36, 25, STRAP)
    return img


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


def recolor(img, dark, light):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sp, op = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            t = lum((r, g, b))
            op[x, y] = (int(dark[0] + (light[0] - dark[0]) * t),
                        int(dark[1] + (light[1] - dark[1]) * t),
                        int(dark[2] + (light[2] - dark[2]) * t), a)
    return out


def make_item_icon(face, dst):
    """Простая иконка-предмет 16x16: берём переднюю грань визора крупно."""
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ip = icon.load()
    fp = face.load()
    # тело
    for y in range(3, 14):
        for x in range(3, 13):
            ip[x, y] = fp[min(15, 2 + (x - 3) // 2), min(4, 1 + (y - 3) // 3)]
    icon.save(dst)


base = build_base()
base.save(os.path.join(BASE, "bio_filter_mask.png"))
cloth = recolor(base, (58, 48, 36), (200, 182, 150))   # хаки
cloth.save(os.path.join(BASE, "cloth_respirator.png"))
gas = recolor(base, (34, 40, 30), (112, 126, 92))       # средняя олива (НЕ чёрный)
gas.save(os.path.join(BASE, "gas_mask.png"))

# контрольная картинка (увеличенная) для глаз-проверки
sheet = Image.new("RGBA", (W * 6 * 3 + 40, H * 6 + 20), (150, 150, 152, 255))
for i, im in enumerate([base, cloth, gas]):
    big = im.resize((W * 6, H * 6), Image.NEAREST)
    sheet.alpha_composite(big, (10 + i * (W * 6 + 10), 10))
sheet.save(os.path.join(PREVIEW, "mask_texture_preview.png"))
print("wrote bio_filter_mask / cloth_respirator / gas_mask (model + preview)")
