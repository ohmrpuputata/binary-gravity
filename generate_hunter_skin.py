# -*- coding: utf-8 -*-
"""Скин Макса Максбетова: реальный скин из интернета (аккаунт «Slav»,
textures.minecraft.net — бородатый мужик в куртке, вылитый матёрый охотник).
Скачивает через официальный Mojang API и конвертирует legacy 64x32 в
современный 64x64 с по-граневым зеркалированием левых конечностей —
ровно так же, как это делает ваниль в HttpTexture.processLegacySkin."""
import base64
import json
import sys
import urllib.request

from PIL import Image

USERNAME = "Slav"
OUT = "src/main/resources/assets/alien-invasion/textures/entity/hunter.png"


def fetch_skin(username):
    with urllib.request.urlopen(
            f"https://api.mojang.com/users/profiles/minecraft/{username}", timeout=20) as r:
        uid = json.load(r)["id"]
    with urllib.request.urlopen(
            f"https://sessionserver.mojang.com/session/minecraft/profile/{uid}", timeout=20) as r:
        prof = json.load(r)
    tex = json.loads(base64.b64decode(prof["properties"][0]["value"]))
    url = tex["textures"]["SKIN"]["url"]
    return Image.open(urllib.request.urlopen(url, timeout=20)).convert("RGBA")


def flip(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def convert_legacy(src):
    """64x32 -> 64x64: низ прозрачный, левые рука/нога — зеркальные копии правых."""
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(src, (0, 0))

    def mirror_limb(sx, sy, dx, dy, w):
        # порядок граней в строке: right | front | left | back (+ top/bottom сверху)
        out.paste(flip(src.crop((sx + w, sy - 4, sx + w * 2, sy))), (dx + w, dy - 4))          # top
        out.paste(flip(src.crop((sx + w * 2, sy - 4, sx + w * 3, sy))), (dx + w * 2, dy - 4))  # bottom (w/o top row fix)
        out.paste(flip(src.crop((sx + w * 2, sy, sx + w * 3, sy + 12))), (dx, dy))             # left -> right
        out.paste(flip(src.crop((sx + w, sy, sx + w * 2, sy + 12))), (dx + w, dy))             # front
        out.paste(flip(src.crop((sx, sy, sx + w, sy + 12))), (dx + w * 2, dy))                 # right -> left
        out.paste(flip(src.crop((sx + w * 3, sy, sx + w * 4, sy + 12))), (dx + w * 3, dy))     # back

    mirror_limb(0, 20, 16, 52, 4)   # правая нога -> левая нога (16,48 зона)
    mirror_limb(40, 20, 32, 52, 4)  # правая рука -> левая рука (32,48 зона)
    return out


def main():
    try:
        skin = fetch_skin(USERNAME)
    except Exception as e:
        # Mojang API бывает капризен — берём ранее скачанную копию из TEMP.
        import os
        cached = os.path.join(os.environ.get("TEMP", "."), f"skin_{USERNAME}.png")
        print("API недоступен (", e, "), беру кэш:", cached)
        skin = Image.open(cached).convert("RGBA")
    if skin.size == (64, 32):
        skin = convert_legacy(skin)
    elif skin.size != (64, 64):
        print("Неожиданный размер скина:", skin.size)
        return 1
    skin.save(OUT)
    print("OK ->", OUT, skin.size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
