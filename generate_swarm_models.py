# -*- coding: utf-8 -*-
"""Переводит 8 гуманоидов роя на общую AlienHumanoidModel: переписывает рендереры,
добавляет слои, регистрирует их в клиенте и рисует текстуры (скин + глоу-глаза)
под общую UV-развёртку 96x96. Размеры регионов зеркалят AlienHumanoidModel."""
import os
import random
import re

from PIL import Image

CL = "src/main/java/com/example/alieninvasion/client"
TEX = "src/main/resources/assets/alien-invasion/textures/entity"

# (Renderer, Entity, texture, LAYER, Variant, scale, shadow, palette, eyeColor)
TABLE = [
    ("AlienBruteRenderer", "AlienBruteEntity", "alien_brute", "ALIEN_BRUTE", "BRUTE", 1.7, 0.9,
     (96, 86, 110), (255, 120, 90)),
    ("AlienStalkerRenderer", "AlienStalkerEntity", "alien_stalker", "ALIEN_STALKER", "STALKER", 1.0, 0.5,
     (132, 140, 150), (140, 255, 200)),
    ("PlasmaCasterRenderer", "PlasmaCasterEntity", "plasma_caster", "PLASMA_CASTER", "CASTER", 1.05, 0.5,
     (98, 118, 96), (120, 230, 255)),
    ("HiveShamanRenderer", "HiveShamanEntity", "hive_shaman", "HIVE_SHAMAN", "SHAMAN", 1.1, 0.5,
     (120, 80, 150), (255, 210, 120)),
    ("TelekineticAlienRenderer", "TelekineticAlienEntity", "telekinetic_alien", "TELEKINETIC_ALIEN", "TELEKINETIC", 1.25, 0.5,
     (58, 48, 78), (220, 130, 255)),
    ("AlienTrollRenderer", "AlienTrollEntity", "alien_troll", "ALIEN_TROLL", "TROLL", 1.45, 0.7,
     (104, 110, 78), (255, 200, 90)),
    ("HiveTyrantRenderer", "HiveTyrantEntity", "hive_tyrant", "HIVE_TYRANT", "TYRANT", 2.0, 1.0,
     (118, 52, 60), (255, 90, 70)),
    ("AcidSpitterRenderer", "AcidSpitterEntity", "acid_spitter", "ACID_SPITTER", "SPITTER", 1.0, 0.5,
     (110, 128, 84), (190, 255, 90)),
]

RENDERER = """package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.{entity};
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// {variant} build of the shared swarm-humanoid skeleton, with glowing eyes.
public class {renderer} extends MobRenderer<{entity}, AlienHumanoidModel<{entity}>> {{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/{tex}.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/{tex}_eyes.png");

    public {renderer}(EntityRendererProvider.Context context) {{
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.{layer}),
                AlienHumanoidModel.Variant.{variant}), {shadow}F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }}

    @Override
    protected void scale({entity} entity, PoseStack poseStack, float partialTick) {{
        poseStack.scale({scale}F, {scale}F, {scale}F);
    }}

    @Override
    public ResourceLocation getTextureLocation({entity} entity) {{
        return TEXTURE;
    }}
}}
"""

for renderer, entity, tex, layer, variant, scale, shadow, _, _ in TABLE:
    with open(f"{CL}/{renderer}.java", "w", encoding="utf-8") as f:
        f.write(RENDERER.format(renderer=renderer, entity=entity, tex=tex, layer=layer,
                                variant=variant, scale=scale, shadow=shadow))
    print(f"  renderer {renderer} -> {variant}")

# --- ModModelLayers: добавить 8 констант ---
p = f"{CL}/ModModelLayers.java"
src = open(p, encoding="utf-8").read()
adds = []
for _, _, tex, layer, _, _, _, _, _ in TABLE:
    if layer not in src:
        adds.append(f'    public static final ModelLayerLocation {layer} = new ModelLayerLocation(\n'
                    f'            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "{tex}"), "main");')
if adds:
    src = src.rstrip().rstrip("}") + "\n" + "\n".join(adds) + "\n}\n"
    open(p, "w", encoding="utf-8").write(src)
print(f"  ModModelLayers +{len(adds)}")

# --- Клиент: регистрация слоёв ---
p = f"src/main/java/com/example/alieninvasion/AlienInvasionClient.java"
src = open(p, encoding="utf-8").read()
if "ALIEN_BRUTE," not in src:
    reg = "\n".join(
        f"                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.{layer},\n"
        f"                                () -> com.example.alieninvasion.client.model.AlienHumanoidModel"
        f".createBodyLayer(com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.{variant}));"
        for _, _, _, layer, variant, _, _, _, _ in TABLE)
    anchor = "                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SWARM_MOTHER,"
    idx = src.index(anchor)
    end = src.index(";", idx) + 1
    src = src[:end] + "\n" + reg + src[end:]
    open(p, "w", encoding="utf-8").write(src)
    print("  client layers registered")

# ------------------------------------------------------------------ текстуры
random.seed(2026)


def clamp(v):
    return max(0, min(255, int(v)))


def sizes(variant):
    headW = {"BRUTE": 7, "TELEKINETIC": 9, "TYRANT": 8}.get(variant, 8)
    chestW = {"BRUTE": 12, "TYRANT": 11, "TROLL": 10, "SPITTER": 9, "CASTER": 8, "TELEKINETIC": 6}.get(variant, 7)
    chestH = 7 if variant in ("BRUTE", "TYRANT") else 6
    armW = {"BRUTE": 4, "TYRANT": 3, "TROLL": 3}.get(variant, 2)
    armLen = {"BRUTE": 11, "TELEKINETIC": 12, "STALKER": 12}.get(variant, 9)
    return headW, headW - 1, chestW, chestH, armW, armLen


def regions(variant):
    headW, headH, chestW, chestH, armW, armLen = sizes(variant)
    r = {
        "head":  (0, 0, 4 * headW, headW + headH),
        "jaw":   (0, 20, 20, 7),
        "eye":   (41, 0, 8, 3),
        "ant":   (41, 4, 4, 6), "tip": (47, 4, 4, 2),
        "horn":  (52, 0, 8, 6), "crest": (52, 8, 12, 10),
        "chest": (0, 30, 2 * (chestW + 5), 5 + chestH),
        "waist": (37, 30, 2 * (chestW - 2 + 4), 8),
        "pelvis": (0, 45, 2 * (chestW - 1 + 5), 8),
        "spike": (62, 0, 4, 3),
        "tail1": (37, 40, 18, 9), "tail2": (37, 50, 14, 7),
        "shoulder": (0, 55, 2 * (armW + 4), 8),
        "arm":   (17, 55, 4 * armW, armW + armLen),
        "cannon": (30, 55, 20, 11), "claw": (51, 55, 4, 5),
        "thigh": (0, 70, 16, 10), "shin": (21, 70, 12, 8), "foot": (38, 70, 16, 6),
    }
    return r


def paint(variant, base, eye_color, tex_name):
    skin = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    sp, ep = skin.load(), eyes.load()
    rnd = random.Random(hash(variant) & 0xFFFF)
    dark = tuple(clamp(c * 0.55) for c in base)
    vein = (clamp(base[0] * 0.4 + 70), 20, clamp(base[2] * 0.4 + 80))

    def fill(key, color=None, mottle=12):
        u, v, w, h = regions(variant)[key]
        col = color or base
        for y in range(v, min(96, v + h)):
            for x in range(u, min(96, u + w)):
                j = rnd.randint(-mottle, mottle)
                sp[x, y] = (clamp(col[0] + j), clamp(col[1] + j), clamp(col[2] + j), 255)
        # прожилки
        for _ in range(max(1, w * h // 40)):
            x, y = rnd.randrange(u, u + w), rnd.randrange(v, v + h)
            sp[min(95, x), min(95, y)] = (*vein, 255)

    for key in ("head", "jaw", "chest", "waist", "pelvis", "shoulder", "arm",
                "thigh", "shin"):
        fill(key)
    fill("foot", dark); fill("claw", (40, 38, 46)); fill("spike", (180, 174, 158))
    fill("tail1", dark); fill("tail2", vein)
    fill("ant", dark); fill("tip", (205, 125, 255))
    fill("horn", (188, 178, 156)); fill("crest", (base[0], clamp(base[1] * 0.6), clamp(base[2] + 50)))
    fill("cannon", (70, 80, 78))
    # глянцево-чёрные глаза + блик; в глоу-слое — цвет варианта
    u, v, w, h = regions(variant)["eye"]
    for y in range(v, v + h):
        for x in range(u, u + w):
            sp[x, y] = (14, 12, 20, 255)
            ep[x, y] = (*eye_color, 255)
    sp[u + 2, v + 1] = (235, 245, 255, 255)
    # светятся также кончики антенн и жерло пушки
    for key in ("tip",) + (("cannon",) if variant == "CASTER" else ()):
        u, v, w, h = regions(variant)[key]
        for y in range(v, v + h):
            for x in range(u, u + w):
                ep[x, y] = (*eye_color, 255)
    skin.save(f"{TEX}/{tex_name}.png")
    eyes.save(f"{TEX}/{tex_name}_eyes.png")
    print(f"  tex {tex_name} 96x96 + eyes")


for _, _, tex, _, variant, _, _, palette, eye in TABLE:
    paint(variant, palette, eye, tex)

print("done")
