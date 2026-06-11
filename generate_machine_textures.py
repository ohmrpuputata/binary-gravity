# -*- coding: utf-8 -*-
"""Машины роя + клон: рендерер дрона, слой SKY_DRONE, замена рендера клона,
текстуры под новые развёртки (тарелка/бур/дрон/метеор/клон + глоу)."""
import random

from PIL import Image

CL = "src/main/java/com/example/alieninvasion/client"
TEX = "src/main/resources/assets/alien-invasion/textures/entity"


def patch(path, pairs):
    src = open(path, encoding="utf-8").read()
    changed = False
    for old, new in pairs:
        if old not in src:
            print(f"  !! NOT FOUND in {path.split('/')[-1]}: {old[:55]}")
            continue
        src = src.replace(old, new, 1)
        changed = True
    if changed:
        open(path, "w", encoding="utf-8").write(src)
        print(f"  patched {path.split('/')[-1]}")


# 1) SkyDroneRenderer: с фантома на собственную модель.
drone_renderer = '''package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.SkyDroneModel;
import com.example.alieninvasion.entity.SkyDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Recon quad-drone on its own model: spinning rotors, glowing sensor eye.
public class SkyDroneRenderer extends MobRenderer<SkyDroneEntity, SkyDroneModel<SkyDroneEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/sky_drone.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/sky_drone_eyes.png");

    public SkyDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new SkyDroneModel<>(context.bakeLayer(ModModelLayers.SKY_DRONE)), 0.5F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(SkyDroneEntity entity) {
        return TEXTURE;
    }
}
'''
open(f"{CL}/SkyDroneRenderer.java", "w", encoding="utf-8").write(drone_renderer)
print("  SkyDroneRenderer rewritten")

# 2) Слой SKY_DRONE + регистрация + замена рендера клона.
patch(f"{CL}/ModModelLayers.java", [
    ("    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation(",
     '''    public static final ModelLayerLocation SKY_DRONE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "sky_drone"), "main");
    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation('''),
])
patch("src/main/java/com/example/alieninvasion/AlienInvasionClient.java", [
    ("""                EntityRendererRegistry.register(EntityRegistry.INFESTED_PLAYER_CLONE,
                                ctx -> new net.minecraft.client.renderer.entity.ZombieRenderer(ctx));""",
     """                EntityRendererRegistry.register(EntityRegistry.INFESTED_PLAYER_CLONE,
                                com.example.alieninvasion.client.InfestedCloneRenderer::new);"""),
    ("""                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SWARM_MOTHER,
                                com.example.alieninvasion.client.model.SwarmMotherModel::createBodyLayer);""",
     """                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SWARM_MOTHER,
                                com.example.alieninvasion.client.model.SwarmMotherModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SKY_DRONE,
                                com.example.alieninvasion.client.model.SkyDroneModel::createBodyLayer);"""),
])

# ------------------------------------------------------------------ текстуры
random.seed(2030)


def clamp(v):
    return max(0, min(255, int(v)))


def fill(px, u, v, w, h, base, rnd, mottle=10, seams=True):
    for y in range(v, v + h):
        for x in range(u, u + w):
            j = rnd.randint(-mottle, mottle)
            seam = -16 if (seams and (x % 8 == 0 or y % 7 == 0)) else 0
            px[x, y] = (clamp(base[0] + j + seam), clamp(base[1] + j + seam),
                        clamp(base[2] + j + seam), 255)


# Тарелка 128x64: тёмный металл, стеклянный купол, яркие огни.
ufo = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
up = ufo.load()
rnd = random.Random(1)
fill(up, 0, 0, 96, 28, (88, 94, 108), rnd)                 # главный диск
fill(up, 0, 29, 64, 18, (74, 80, 94), rnd)                 # конусные пластины
fill(up, 64, 29, 64, 18, (74, 80, 94), rnd)
fill(up, 96, 0, 32, 12, (140, 200, 210), rnd, mottle=6, seams=False)  # купол-стекло
fill(up, 0, 48, 8, 4, (170, 255, 120), rnd, mottle=4, seams=False)    # огни
fill(up, 16, 48, 16, 7, (130, 230, 255), rnd, mottle=6, seams=False)  # эмиттер
ufo.save(f"{TEX}/ufo.png")
print("  tex ufo 128x64")

# Бур 64x64: сталь + спиральные полосы на конусе.
drill = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
dp = drill.load()
rnd = random.Random(2)
fill(dp, 0, 0, 32, 18, (96, 90, 84), rnd)                  # корпус
fill(dp, 33, 0, 12, 9, (60, 58, 66), rnd)                  # трастеры
for region, h in (((0, 19, 24, 10), 10), ((0, 30, 16, 8), 8), ((0, 39, 8, 6), 6)):
    u, v, w, hh = region
    fill(dp, u, v, w, hh, (150, 144, 130), rnd, mottle=8)
    for y in range(v, v + hh):                              # спиральные полосы
        x = u + ((y * 3) % w)
        dp[x, y] = (70, 64, 58, 255)
drill.save(f"{TEX}/drill.png")
print("  tex drill 64x64")

# Дрон 64x32 + глоу-глаз.
drone = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
de = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
np_, ne = drone.load(), de.load()
rnd = random.Random(3)
fill(np_, 0, 0, 24, 10, (52, 56, 66), rnd)                 # корпус
fill(np_, 25, 0, 6, 3, (255, 70, 70), rnd, mottle=4, seams=False)  # глаз
fill(np_, 32, 0, 4, 4, (90, 96, 108), rnd, seams=False)    # антенна
fill(np_, 0, 11, 12, 6, (70, 76, 88), rnd, seams=False)    # лучи
fill(np_, 0, 18, 14, 2, (120, 126, 138), rnd, mottle=5, seams=False)  # лопасти
for y in range(0, 3):
    for x in range(25, 31):
        ne[x, y] = (255, 80, 80, 255)
drone.save(f"{TEX}/sky_drone.png")
de.save(f"{TEX}/sky_drone_eyes.png")
print("  tex sky_drone + eyes")

# Метеор 64x64: порода с лавовыми трещинами.
met = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
mp = met.load()
rnd = random.Random(4)
fill(mp, 0, 0, 40, 20, (72, 62, 56), rnd, mottle=16, seams=False)
fill(mp, 0, 21, 18, 12, (62, 54, 48), rnd, mottle=16, seams=False)
fill(mp, 19, 21, 24, 9, (62, 54, 48), rnd, mottle=16, seams=False)
fill(mp, 41, 21, 16, 11, (62, 54, 48), rnd, mottle=16, seams=False)
for _ in range(26):                                         # лавовые прожилки
    x, y = rnd.randrange(64), rnd.randrange(33)
    if mp[x, y][3]:
        mp[x, y] = (255, 120 + rnd.randint(0, 60), 30, 255)
met.save(f"{TEX}/meteor.png")
print("  tex meteor 64x64")

# Клон игрока 64x64 (скин-развёртка): серая кожа, рваная тёмная одежда,
# фиолетовые вены; глаза — в глоу-слое.
skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
sp, ep = skin.load(), eyes.load()
rnd = random.Random(5)
SKINC = (132, 142, 128)
CLOTH = (52, 48, 62)
PANTS = (44, 42, 54)
fill(sp, 0, 0, 32, 16, SKINC, rnd, seams=False)            # голова
fill(sp, 16, 16, 24, 16, CLOTH, rnd, seams=False)          # торс
fill(sp, 40, 16, 16, 16, SKINC, rnd, seams=False)          # правая рука
fill(sp, 32, 48, 16, 16, SKINC, rnd, seams=False)          # левая рука
fill(sp, 0, 16, 16, 16, PANTS, rnd, seams=False)           # правая нога
fill(sp, 16, 48, 16, 16, PANTS, rnd, seams=False)          # левая нога
for _ in range(40):                                        # фиолетовые вены
    x, y = rnd.randrange(64), rnd.randrange(64)
    if sp[x, y][3]:
        sp[x, y] = (110, 60, 140, 255)
for _ in range(14):                                        # дыры в одежде
    x, y = 16 + rnd.randrange(24), 16 + rnd.randrange(16)
    sp[x, y] = (clamp(SKINC[0] - 20), clamp(SKINC[1] - 20), clamp(SKINC[2] - 20), 255)
for (x, y) in ((9, 12), (10, 12), (13, 12), (14, 12)):     # глаза (лицо 8..16, 8..16)
    sp[x, y] = (16, 10, 22, 255)
    ep[x, y] = (200, 110, 255, 255)
skin.save(f"{TEX}/infested_player_clone.png")
eyes.save(f"{TEX}/infested_player_clone_eyes.png")
print("  tex infested_player_clone + eyes")
print("done")
