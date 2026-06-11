# -*- coding: utf-8 -*-
"""Вайринг червей/рапторов: реестры, яйца, слои, союзность, события, текстуры, ланг."""
import json
import random

from PIL import Image


def patch(path, pairs):
    src = open(path, encoding="utf-8").read()
    for old, new in pairs:
        if old not in src:
            print(f"  !! NOT FOUND in {path.split('/')[-1]}: {old[:60]}")
            continue
        src = src.replace(old, new, 1)
    open(path, "w", encoding="utf-8").write(src)
    print(f"  patched {path.split('/')[-1]}")


E = "src/main/java/com/example/alieninvasion"

# 1) EntityRegistry: червь + раптор + атрибуты.
patch(f"{E}/registry/EntityRegistry.java", [
    ("""    public static final EntityType<ParasiteEntity> PARASITE = Registry.register(""",
     """    public static final EntityType<com.example.alieninvasion.entity.InfestedWormEntity> INFESTED_WORM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_worm"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedWormEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.5f).build("infested_worm")
    );

    public static final EntityType<com.example.alieninvasion.entity.AlienRaptorEntity> ALIEN_RAPTOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_raptor"),
            EntityType.Builder.of(com.example.alieninvasion.entity.AlienRaptorEntity::new, MobCategory.MONSTER)
                    .sized(0.85f, 1.1f).build("alien_raptor")
    );

    public static final EntityType<ParasiteEntity> PARASITE = Registry.register("""),
    ("""        FabricDefaultAttributeRegistry.register(PARASITE, com.example.alieninvasion.entity.ParasiteEntity.createAttributes());""",
     """        FabricDefaultAttributeRegistry.register(PARASITE, com.example.alieninvasion.entity.ParasiteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_WORM, com.example.alieninvasion.entity.InfestedWormEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_RAPTOR, com.example.alieninvasion.entity.AlienRaptorEntity.createAttributes());"""),
])

# 2) ItemRegistry: яйца призыва.
patch(f"{E}/registry/ItemRegistry.java", [
    ("""    public static final Item PARASITE_SPAWN_EGG = registerItem("parasite_spawn_egg",""",
     """    public static final Item INFESTED_WORM_SPAWN_EGG = registerItem("infested_worm_spawn_egg",
            new SpawnEggItem(EntityRegistry.INFESTED_WORM, 0xB04467, 0x4E0E2C, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item ALIEN_RAPTOR_SPAWN_EGG = registerItem("alien_raptor_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_RAPTOR, 0x69784F, 0x2C381F, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item PARASITE_SPAWN_EGG = registerItem("parasite_spawn_egg","""),
])

# 3) ModModelLayers: три слоя.
patch(f"{E}/client/ModModelLayers.java", [
    ("""    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation(""",
     """    public static final ModelLayerLocation INFESTED_WORM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_worm"), "main");
    public static final ModelLayerLocation ALIEN_RAPTOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_raptor"), "main");
    public static final ModelLayerLocation PARASITE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "parasite"), "main");
    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation("""),
])

# 4) AlienUtils: новые — свои.
patch(f"{E}/entity/AlienUtils.java", [
    ("                || other instanceof AcidSpitterEntity || other instanceof SwarmMotherEntity) {",
     "                || other instanceof AcidSpitterEntity || other instanceof SwarmMotherEntity\n"
     "                || other instanceof InfestedWormEntity || other instanceof AlienRaptorEntity) {"),
])

# 5) ModEvents: хосты лопаются червями; раптор в подземных засадах.
patch(f"{E}/events/ModEvents.java", [
    ("""            // Infected hosts burst into a fast 1-HP brain-parasite on death.
            if ((entity instanceof com.example.alieninvasion.entity.InfestedZombieEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedCreeperEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedSkeletonEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedPlayerCloneEntity)
                    && entity.level() instanceof ServerLevel pl && pl.random.nextFloat() < 0.5F) {
                com.example.alieninvasion.entity.ParasiteEntity p = EntityRegistry.PARASITE.create(pl);
                if (p != null) {
                    p.moveTo(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
                    pl.addFreshEntity(p);
                }
            }""",
     """            // THE LIFE CYCLE: infected hosts burst into worm broodlings on death
            // (1-2 tiny worms; late-game one may emerge already grown), and sometimes
            // a brain-parasite skitters out too.
            if ((entity instanceof com.example.alieninvasion.entity.InfestedZombieEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedCreeperEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedSkeletonEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedPlayerCloneEntity)
                    && entity.level() instanceof ServerLevel pl) {
                int wormDay = SurvivalManager.getDay(pl);
                int worms = 1 + pl.random.nextInt(2);
                for (int i = 0; i < worms; i++) {
                    com.example.alieninvasion.entity.InfestedWormEntity worm = EntityRegistry.INFESTED_WORM.create(pl);
                    if (worm != null) {
                        worm.moveTo(entity.getX() + (pl.random.nextDouble() - 0.5) * 0.6,
                                entity.getY() + 0.2, entity.getZ() + (pl.random.nextDouble() - 0.5) * 0.6,
                                pl.random.nextFloat() * 360F, 0F);
                        worm.setStage(wormDay >= 5 && pl.random.nextFloat() < 0.35F ? 1 : 0);
                        pl.addFreshEntity(worm);
                    }
                }
                if (pl.random.nextFloat() < 0.25F) {
                    com.example.alieninvasion.entity.ParasiteEntity p = EntityRegistry.PARASITE.create(pl);
                    if (p != null) {
                        p.moveTo(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
                        pl.addFreshEntity(p);
                    }
                }
            }"""),
    ("""                            Mob a = level.random.nextBoolean()
                                    ? EntityRegistry.ALIEN_GRUNT.create(level)
                                    : EntityRegistry.CAVE_LURKER.create(level);""",
     """                            Mob a = switch (level.random.nextInt(3)) {
                                case 0 -> EntityRegistry.ALIEN_GRUNT.create(level);
                                case 1 -> EntityRegistry.CAVE_LURKER.create(level);
                                default -> EntityRegistry.ALIEN_RAPTOR.create(level);
                            };"""),
])

# 6) InfectionManager: на 75% из игрока выползает червь нулевой стадии.
patch(f"{E}/logic/InfectionManager.java", [
    ("""                com.example.alieninvasion.entity.ParasiteEntity worm =
                        com.example.alieninvasion.registry.EntityRegistry.PARASITE.create(level);""",
     """                com.example.alieninvasion.entity.InfestedWormEntity worm =
                        com.example.alieninvasion.registry.EntityRegistry.INFESTED_WORM.create(level);"""),
    ("""                if (worm != null) {
                    worm.moveTo(player.getX(), player.getY() + 0.3D, player.getZ(),
                            level.random.nextFloat() * 360.0F, 0.0F);
                    var scale = worm.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
                    if (scale != null) scale.setBaseValue(0.55D);""",
     """                if (worm != null) {
                    worm.moveTo(player.getX(), player.getY() + 0.3D, player.getZ(),
                            level.random.nextFloat() * 360.0F, 0.0F);
                    worm.setStage(0);"""),
])

# 7) Текстуры: червь и паразит (одна развёртка 32x32), раптор 64x64 + глоу-глаза.
random.seed(42)
TEX = "src/main/resources/assets/alien-invasion/textures/entity"


def clamp(v):
    return max(0, min(255, int(v)))


def fill_region(px, u, v, w, h, base, rnd, mottle=14, rings=False):
    for y in range(v, v + h):
        for x in range(u, u + w):
            j = rnd.randint(-mottle, mottle)
            ring = -22 if (rings and (x - u) % 4 == 0) else 0
            px[x, y] = (clamp(base[0] + j + ring), clamp(base[1] + j // 2 + ring),
                        clamp(base[2] + j + ring), 255)


def worm_tex(name, base, eye):
    skin = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    sp, ep = skin.load(), eyes.load()
    rnd = random.Random(hash(name) & 0xFFFF)
    fill_region(sp, 0, 0, 16, 8, base, rnd)                                   # голова
    fill_region(sp, 0, 9, 18, 9, base, rnd, rings=True)                       # кольца 5px
    fill_region(sp, 0, 18, 14, 7, base, rnd, rings=True)                      # кольца 3px
    fill_region(sp, 18, 0, 12, 6, tuple(clamp(c * 0.7) for c in base), rnd)   # хвост
    fill_region(sp, 0, 24, 12, 4, (200, 190, 170), rnd, mottle=8)             # челюсть
    for x in range(4, 12, 2):
        sp[x, 26] = (240, 235, 220, 255)                                      # зубы
    for (x, y) in ((5, 3), (10, 3)):
        sp[x, y] = (10, 8, 14, 255)
        ep[x, y] = (*eye, 255)
    skin.save(f"{TEX}/{name}.png")
    eyes.save(f"{TEX}/{name}_eyes.png")
    print(f"  tex {name} + eyes")


worm_tex("infested_worm", (172, 84, 110), (255, 120, 160))
worm_tex("parasite", (118, 150, 84), (160, 255, 120))


def raptor_tex():
    base = (104, 118, 82)
    belly = (150, 158, 128)
    eye = (255, 190, 60)
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    sp, ep = skin.load(), eyes.load()
    rnd = random.Random(777)
    fill_region(sp, 0, 0, 32, 16, base, rnd)                                  # корпус
    fill_region(sp, 33, 0, 20, 9, base, rnd)                                  # голова
    fill_region(sp, 33, 10, 12, 5, (70, 80, 56), rnd)                         # гребень
    fill_region(sp, 33, 16, 16, 6, belly, rnd, mottle=8)                      # челюсть
    fill_region(sp, 0, 17, 22, 11, base, rnd, rings=True)                     # хвост
    fill_region(sp, 0, 29, 18, 9, tuple(clamp(c * 0.75) for c in base), rnd)  # кончик
    fill_region(sp, 23, 17, 6, 6, (52, 50, 56), rnd, mottle=5)                # коготки
    fill_region(sp, 48, 24, 14, 10, base, rnd)                                # бедро
    fill_region(sp, 48, 35, 8, 9, base, rnd)                                  # голень
    fill_region(sp, 48, 42, 14, 6, (52, 50, 56), rnd, mottle=5)               # стопа
    for i in range(5):                                                        # полосы
        x0 = 6 + i * 5
        for y in range(2, 6):
            sp[x0, y] = (58, 66, 44, 255)
    for x in range(36, 48, 2):
        sp[x, 21] = (235, 230, 215, 255)                                      # зубы
    for (x, y) in ((36, 3), (44, 3)):
        sp[x, y] = (12, 10, 8, 255)
        ep[x, y] = (*eye, 255)
        ep[x + 1, y] = (*eye, 255)
    skin.save(f"{TEX}/alien_raptor.png")
    eyes.save(f"{TEX}/alien_raptor_eyes.png")
    print("  tex alien_raptor + eyes")


raptor_tex()

# 8) Lang.
for path, names in [
    ("src/main/resources/assets/alien-invasion/lang/ru_ru.json", {
        "entity.alien-invasion.infested_worm": "Заражённый червь",
        "entity.alien-invasion.alien_raptor": "Раптор роя",
        "item.alien-invasion.infested_worm_spawn_egg": "Яйцо призыва заражённого червя",
        "item.alien-invasion.alien_raptor_spawn_egg": "Яйцо призыва раптора роя"}),
    ("src/main/resources/assets/alien-invasion/lang/en_us.json", {
        "entity.alien-invasion.infested_worm": "Infested Worm",
        "entity.alien-invasion.alien_raptor": "Swarm Raptor",
        "item.alien-invasion.infested_worm_spawn_egg": "Infested Worm Spawn Egg",
        "item.alien-invasion.alien_raptor_spawn_egg": "Swarm Raptor Spawn Egg"})]:
    d = json.load(open(path, encoding="utf-8"))
    d.update(names)
    json.dump(d, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print("lang ok")
