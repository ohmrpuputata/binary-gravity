# -*- coding: utf-8 -*-
"""Большой баланс-патч: дни, спавнер, тролль, гейгер, самочувствие, кровь,
гейты данжей по дням, редкости, бункер/торговцы, новые данжи, ассеты крови."""
import io
import json
import random
import zipfile
import os

from PIL import Image

E = "src/main/java/com/example/alieninvasion"
ASSETS = "src/main/resources/assets/alien-invasion"
DATA = "src/main/resources/data/alien-invasion"


def patch(path, pairs, label=None):
    src = open(path, encoding="utf-8").read()
    ok = 0
    for old, new in pairs:
        if old not in src:
            print(f"  !! NOT FOUND in {path.split('/')[-1]}: {old[:58]!r}")
            continue
        src = src.replace(old, new, 1)
        ok += 1
    open(path, "w", encoding="utf-8").write(src)
    print(f"  {label or path.split('/')[-1]}: {ok}/{len(pairs)}")


# ============ 1. КРИВАЯ ЗАРАЖЕНИЯ: полное к дню 8, не к дню 5 ============
patch(f"{E}/logic/WorldContaminationManager.java", [
    ("""        switch (day) {
            case 1:  return 0.15f;
            case 2:  return 0.35f;
            case 3:  return 0.60f;
            case 4:  return 0.85f;
            default: return 1.0f;
        }""",
     """        switch (day) {
            case 1:  return 0.05f;
            case 2:  return 0.12f;
            case 3:  return 0.25f;
            case 4:  return 0.40f;
            case 5:  return 0.55f;
            case 6:  return 0.70f;
            case 7:  return 0.85f;
            default: return 1.0f;
        }"""),
    ("""    /** Fraction of water gone bad on day N: starts day 3, ALL of it by day 5. */
    private static float getWaterTarget(int day) {
        if (day < 3) return 0f;
        return getTarget(day);
    }""",
     """    /** Fraction of water gone bad on day N: starts day 4, lags the land slightly. */
    private static float getWaterTarget(int day) {
        if (day < 4) return 0f;
        return getTarget(day - 1);
    }"""),
    ("""    /** Caves rot one day behind the surface — the corruption sinks in from above. */
    private static float getCaveTarget(int day) {
        return getTarget(day - 1);
    }""",
     """    /** Caves rot two days behind the surface — the corruption sinks in from above. */
    private static float getCaveTarget(int day) {
        return getTarget(day - 2);
    }"""),
    ("        if (day < 2) return;\n        ChunkPos cpos = chunk.getPos();\n        int minY = level.getMinBuildHeight();\n\n        for (int slot = 0; slot < CRATER_SLOTS; slot++) {",
     "        if (day < 3) return;\n        ChunkPos cpos = chunk.getPos();\n        int minY = level.getMinBuildHeight();\n\n        for (int slot = 0; slot < CRATER_SLOTS; slot++) {"),
    ("            int activationDay = 2 + (int) ((h >>> 12) % 5L);       // strikes land on days 2..6",
     "            int activationDay = 3 + (int) ((h >>> 12) % 5L);       // strikes land on days 3..7"),
    ("        if (day < 3) return;\n        ChunkPos cpos = chunk.getPos();\n        int minY = level.getMinBuildHeight();\n\n        for (int slot = 0; slot < DRILL_SLOTS; slot++) {",
     "        if (day < 4) return;\n        ChunkPos cpos = chunk.getPos();\n        int minY = level.getMinBuildHeight();\n\n        for (int slot = 0; slot < DRILL_SLOTS; slot++) {"),
    ("            int activationDay = 3 + (int) ((h >>> 12) % 4L);       // drills hit on days 3..6",
     "            int activationDay = 4 + (int) ((h >>> 12) % 4L);       // drills hit on days 4..7"),
    ("        if (day >= 3 && prevDay < 3) {\n            long hh = mix(chunkSeed ^ 0xD1B54A32D192ED03L);",
     "        if (day >= 4 && prevDay < 4) {\n            long hh = mix(chunkSeed ^ 0xD1B54A32D192ED03L);"),
    ("                        if (day >= 2 && ((h >>> 40) & 0x3FL) == 0L) {",
     "                        if (day >= 3 && ((h >>> 40) & 0x3FL) == 0L) {"),
    ("                        if (day >= 2 && ((h >>> 46) & 0x1FL) == 0L) {",
     "                        if (day >= 3 && ((h >>> 46) & 0x1FL) == 0L) {"),
    ("        boolean undergroundNeeded = newDay > prevDay && prevDay < 7; // caves/ores keep growing until day 6",
     "        boolean undergroundNeeded = newDay > prevDay && prevDay < 9; // caves/ores keep growing until day 8"),
], "WorldContaminationManager (кривая+гейты)")

# ============ 2. СОБЫТИЯ ПО ДНЯМ ============
patch(f"{E}/events/ModEvents.java", [
    ("""                    int day = SurvivalManager.getDay(level);
                    if (day >= 3) {
                        BlockPos spawnPos = player.blockPosition().offset(level.random.nextInt(31) - 15, level.random.nextInt(9) - 4, level.random.nextInt(31) - 15);""",
     """                    int day = SurvivalManager.getDay(level);
                    if (day >= 4) {
                        BlockPos spawnPos = player.blockPosition().offset(level.random.nextInt(31) - 15, level.random.nextInt(9) - 4, level.random.nextInt(31) - 15);"""),
    ("                if (day >= 4 && level.random.nextFloat() < 0.35F && empTicksActive <= 0) {",
     "                if (day >= 5 && level.random.nextFloat() < 0.35F && empTicksActive <= 0) {"),
    ("                    && SurvivalManager.getDay(level) >= 4 && ACTIVE_CLOUDS.size() < 6) {",
     "                    && SurvivalManager.getDay(level) >= 5 && ACTIVE_CLOUDS.size() < 6) {"),
    ("            if (level.isNight() && level.getGameTime() % 900 == 0 && level.random.nextFloat() < 0.10F\n                    && !InvasionManager.get(level).isVictoryAchieved()) {",
     "            if (level.isNight() && level.getGameTime() % 900 == 0 && level.random.nextFloat() < 0.10F\n                    && SurvivalManager.getDay(level) >= 3\n                    && !InvasionManager.get(level).isVictoryAchieved()) {"),
    ("            if (level.getGameTime() % 600 == 0 && level.random.nextFloat() < 0.12F\n                    && !InvasionManager.get(level).isVictoryAchieved()) {",
     "            if (level.getGameTime() % 600 == 0 && level.random.nextFloat() < 0.12F\n                    && SurvivalManager.getDay(level) >= 2\n                    && !InvasionManager.get(level).isVictoryAchieved()) {"),
    ("""                if (player.tickCount % 100 == 0
                        && com.example.alieninvasion.logic.SurvivalManager.getDay(level) >= 4) {
                    for (net.minecraft.world.entity.animal.Animal animal : level.getEntitiesOfClass(""",
     """                if (player.tickCount % 100 == 0
                        && com.example.alieninvasion.logic.SurvivalManager.getDay(level) >= 5) {
                    for (net.minecraft.world.entity.animal.Animal animal : level.getEntitiesOfClass("""),
], "ModEvents (дни событий)")

# ============ 3. КРОВЬ: конверсия блоков вместо луж ============
patch(f"{E}/events/ModEvents.java", [
    ("""            // Heavy hits leave a real blood splat on the ground under the victim
            // (random rotation via the blockstate), which dries up on its own.
            if (amount >= 4.0F && level.random.nextFloat() < 0.4F) {
                BlockPos at = entity.blockPosition();
                BlockState cur = level.getBlockState(at);
                if ((cur.isAir() || cur.canBeReplaced()) && !cur.is(ModBlocks.BLOOD_POOL)
                        && level.getBlockState(at.below()).isFaceSturdy(level, at.below(),
                                net.minecraft.core.Direction.UP)) {
                    level.setBlock(at, ModBlocks.BLOOD_POOL.defaultBlockState(), 3);
                }
            }""",
     """            // Heavy hits STAIN the floor: the block under the victim converts to
            // its bloody twin (stairs/fences keep their shape). Wipe it with a
            // right-click or wash it off with water.
            if (amount >= 4.0F && level.random.nextFloat() < 0.5F && level instanceof ServerLevel splatLevel) {
                com.example.alieninvasion.block.BloodyBlocks.splatter(splatLevel, entity.blockPosition().below());
            }"""),
    ("""                    if (player.tickCount % cadence == 0 && (moving || heavy)) {
                        BlockPos feet = player.blockPosition();
                        BlockState at = level.getBlockState(feet);
                        if ((at.isAir() || at.canBeReplaced()) && !at.is(ModBlocks.BLOOD_POOL)
                                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) {
                            level.setBlock(feet, ModBlocks.BLOOD_POOL.defaultBlockState(), 3);
                        }
                    }""",
     """                    if (player.tickCount % cadence == 0 && (moving || heavy)) {
                        com.example.alieninvasion.block.BloodyBlocks.splatter(level, player.blockPosition().below());
                    }"""),
], "ModEvents (кровавые блоки)")

# ============ 4. ГЕЙГЕР: фон всегда + резче и громче ============
patch(f"{E}/events/ModEvents.java", [
    ("""                if (player.tickCount % 6 == 0 && player.getInventory().contains(
                        new ItemStack(ItemRegistry.GEIGER_COUNTER))) {
                    float fieldLevel = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(player);
                    int clicks = fieldLevel >= 18.0F ? 3 : fieldLevel >= 9.0F ? 2 : fieldLevel > 0.0F ? 1
                            : (level.random.nextInt(14) == 0 ? 1 : 0);
                    for (int c = 0; c < clicks; c++) {
                        level.playSound(null, player.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON,
                                SoundSource.PLAYERS, 0.22F, 1.7F + level.random.nextFloat() * 0.5F);
                    }
                }""",
     """                if (player.tickCount % 2 == 0 && player.getInventory().contains(
                        new ItemStack(ItemRegistry.GEIGER_COUNTER))) {
                    float fieldLevel = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(player);
                    // ALWAYS-ON BACKGROUND: a real counter never goes fully silent -
                    // lone random ticks at rest rising to a frantic crackle near a
                    // source. Sharp dry clicks, loud enough to actually hear.
                    float chance = 0.012F + fieldLevel * 0.045F;
                    if (level.random.nextFloat() < chance) {
                        int burst = 1 + (fieldLevel >= 18.0F ? level.random.nextInt(3)
                                : fieldLevel >= 9.0F ? level.random.nextInt(2) : 0);
                        for (int c = 0; c < burst; c++) {
                            level.playSound(null, player.blockPosition(),
                                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                                    0.55F, 1.8F + level.random.nextFloat() * 0.35F);
                        }
                    }

                    // SELF-AWARENESS: your character KNOWS something is wrong long
                    // before any meter maxes - quiet first-person thoughts, visible
                    // only to you.
                    if (player.tickCount % 160 == 0 && !player.isCreative()) {
                        float selfDose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(player);
                        float selfInf = com.example.alieninvasion.logic.InfectionManager.getMeter(player);
                        String thought = null;
                        if (selfDose >= 75.0F) thought = "Во рту привкус металла... дёсны кровоточат. Лечение, СРОЧНО.";
                        else if (selfInf >= 75.0F) thought = "Под кожей что-то ШЕВЕЛИТСЯ. Я чувствую, как оно растёт.";
                        else if (selfDose >= 40.0F) thought = "Накатывает слабость, кожу покалывает... это облучение.";
                        else if (selfInf >= 50.0F) thought = "Меня лихорадит. Голод не отпускает, руки дрожат.";
                        else if (player.getHealth() < player.getMaxHealth() * 0.25F) thought = "Перед глазами плывёт... слишком много крови потеряно.";
                        else if (selfDose >= 15.0F) thought = "Лёгкая тошнота. Где-то рядом что-то фонит.";
                        else if (selfInf >= 25.0F) thought = "Горло саднит, знобит. Кажется, я что-то подхватил.";
                        if (thought != null) {
                            player.displayClientMessage(Component.literal("§7§o" + thought), true);
                        }
                    }
                }""")
], "ModEvents (гейгер+самочувствие)")

# Самочувствие должно работать и БЕЗ гейгера — вынесем мысли из гейгер-блока?
# Нет: блок выше вложил мысли в гейгер-условие. Исправим: отдельный блок.
patch(f"{E}/events/ModEvents.java", [
    ("""                    // SELF-AWARENESS: your character KNOWS something is wrong long
                    // before any meter maxes - quiet first-person thoughts, visible
                    // only to you.
                    if (player.tickCount % 160 == 0 && !player.isCreative()) {""",
     """                }

                // SELF-AWARENESS: your character KNOWS something is wrong long
                // before any meter maxes - quiet first-person thoughts, only yours.
                {
                    if (player.tickCount % 160 == 0 && !player.isCreative()) {""")
], "ModEvents (мысли вне гейгера)")

# ============ 5. ОСАДЫ/ОРБИТАЛКА ПОЗЖЕ ============
patch(f"{E}/world/InvasionManager.java", [
    ("        if (this.invasionDays >= 3 && level.getGameTime() % 600L == 0L) {\n            orbitalStrikes(level);",
     "        if (this.invasionDays >= 4 && level.getGameTime() % 600L == 0L) {\n            orbitalStrikes(level);"),
    ("        if (this.invasionDays >= 4 && this.invasionDays % 2 == 0 && this.invasionDays != this.lastSiegeDay",
     "        if (this.invasionDays >= 5 && this.invasionDays % 2 == 0 && this.invasionDays != this.lastSiegeDay"),
], "InvasionManager (дни)")

# ============ 6. СПАВНЕР: день 0 почти мирный ============
patch(f"{E}/world/AlienSpawner.java", [
    ("""        if (difficulty < 3) {
            // Recon Squad: a couple of grunts + at most a lone scout chicken.
            int grunts = 1 + level.random.nextInt(2);
            int trolls = level.random.nextFloat() < 0.20f ? 1 : 0;
            int casters = (difficulty >= 2 && level.random.nextFloat() < 0.12f) ? 1 : 0;

            spawnViaDropship(level, pos, grunts, 0, trolls, 0, 0, casters, 0, difficulty);
            spawnMob(level, pos, EntityRegistry.ALIEN_CHICKEN, level.random.nextInt(2), difficulty); // chickens spawn on ground
        } else if (difficulty < 6) {
            // Assault Squad: a brute leading a small pack.
            int brutes = 1;
            int grunts = 2;
            int trolls = level.random.nextFloat() < 0.25f ? 1 : 0;
            int casters = level.random.nextFloat() < 0.20f ? 1 : 0;
            int stalkers = (difficulty >= 4 && level.random.nextFloat() < 0.12f) ? 1 : 0;""",
     """        if (difficulty <= 0) {
            // DAY 0 - PURE RECON: the swarm only scouts. A dropship unloads
            // WORKERS that mine and haul; worms wriggle in the dirt; at most a
            // single lone soldier, and rarely. No combat squads whatsoever.
            int workers = 1 + level.random.nextInt(2);
            for (int i = 0; i < workers; i++) {
                com.example.alieninvasion.entity.AlienGruntEntity worker = EntityRegistry.ALIEN_GRUNT.create(level);
                if (worker != null) {
                    worker.moveTo(pos.getX() + 0.5D + level.random.nextInt(5) - 2, pos.getY(),
                            pos.getZ() + 0.5D + level.random.nextInt(5) - 2, level.random.nextFloat() * 360F, 0F);
                    worker.setScavenger(true);
                    worker.setCustomName(net.minecraft.network.chat.Component.literal("§aПришелец-рабочий"));
                    level.addFreshEntity(worker);
                }
            }
            spawnMob(level, pos, EntityRegistry.INFESTED_WORM, 1 + level.random.nextInt(2), difficulty);
            if (level.random.nextFloat() < 0.15f) {
                spawnMob(level, pos, EntityRegistry.ALIEN_GRUNT, 1, difficulty);
            }
        } else if (difficulty < 3) {
            // DAYS 1-2 - PROBING: small grunt packs with worms; raptors join on
            // day 2. No casters, no brutes - escalation is gradual now.
            int grunts = 1 + level.random.nextInt(2);
            int trolls = level.random.nextFloat() < 0.15f ? 1 : 0;

            spawnViaDropship(level, pos, grunts, 0, trolls, 0, 0, 0, 0, difficulty);
            spawnMob(level, pos, EntityRegistry.INFESTED_WORM, level.random.nextInt(2), difficulty);
            if (difficulty >= 2 && level.random.nextFloat() < 0.35f) {
                spawnMob(level, pos, EntityRegistry.ALIEN_RAPTOR, 1, difficulty);
            }
            spawnMob(level, pos, EntityRegistry.ALIEN_CHICKEN, level.random.nextInt(2), difficulty);
        } else if (difficulty < 6) {
            // Assault Squad: brutes only join from day 4.
            int brutes = difficulty >= 4 ? 1 : 0;
            int grunts = 2;
            int trolls = level.random.nextFloat() < 0.25f ? 1 : 0;
            int casters = (difficulty >= 4 && level.random.nextFloat() < 0.20f) ? 1 : 0;
            int stalkers = (difficulty >= 4 && level.random.nextFloat() < 0.12f) ? 1 : 0;"""),
    ("""        pool.add(EntityRegistry.ALIEN_GRUNT);   weights.add(30);
        pool.add(EntityRegistry.ALIEN_TROLL);   weights.add(8);
        pool.add(EntityRegistry.ALIEN_CHICKEN); weights.add(6);
        if (difficulty >= 2) { pool.add(EntityRegistry.CAVE_LURKER);       weights.add(14); }""",
     """        pool.add(EntityRegistry.ALIEN_GRUNT);   weights.add(difficulty < 1 ? 8 : 22);
        pool.add(EntityRegistry.INFESTED_WORM); weights.add(20);
        pool.add(EntityRegistry.ALIEN_TROLL);   weights.add(4);
        pool.add(EntityRegistry.ALIEN_CHICKEN); weights.add(6);
        if (difficulty >= 2) { pool.add(EntityRegistry.ALIEN_RAPTOR);      weights.add(12); }
        if (difficulty >= 2) { pool.add(EntityRegistry.CAVE_LURKER);       weights.add(14); }"""),
], "AlienSpawner (день 0/1-2)")

# ============ 7. ТРОЛЛЬ: мелкий тощий воришка ============
patch(f"{E}/client/model/AlienHumanoidModel.java", [
    ("float chestW = switch (v) { case BRUTE -> 12; case TYRANT -> 11; case TROLL -> 10;",
     "float chestW = switch (v) { case BRUTE -> 12; case TYRANT -> 11; case TROLL -> 6;"),
    ("float armW = switch (v) { case BRUTE -> 4; case TYRANT, TROLL -> 3; default -> 2; };",
     "float armW = switch (v) { case BRUTE -> 4; case TYRANT -> 3; default -> 2; };"),
    ("boolean antennae = v == Variant.STALKER || v == Variant.TELEKINETIC || v == Variant.SHAMAN;",
     "boolean antennae = v == Variant.STALKER || v == Variant.TELEKINETIC || v == Variant.SHAMAN\n                || v == Variant.TROLL;"),
    ("boolean horns = v == Variant.TYRANT || v == Variant.TROLL;",
     "boolean horns = v == Variant.TYRANT;"),
    ("boolean tail = v == Variant.STALKER || v == Variant.TYRANT || v == Variant.SPITTER;",
     "boolean tail = v == Variant.STALKER || v == Variant.TYRANT || v == Variant.SPITTER\n                || v == Variant.TROLL;"),
], "AlienHumanoidModel (тролль-доходяга)")
patch(f"{E}/client/AlienTrollRenderer.java", [
    ("AlienHumanoidModel.Variant.TROLL), 0.7F);", "AlienHumanoidModel.Variant.TROLL), 0.4F);"),
    ("poseStack.scale(1.45F, 1.45F, 1.45F);", "poseStack.scale(0.85F, 0.85F, 0.85F);"),
], "AlienTrollRenderer (мелкий)")

# ============ 8. ГЕЙТЫ ДАНЖЕЙ ПО ДНЯМ (фаза разведки = без баз) ============
GATES = {
    "CaveDungeonFeature": 2, "InfestedMineFeature": 2, "CosmicVaultFeature": 2,
    "HiveNestFeature": 2, "AlienOutpostFeature": 2, "CrashedUfoFeature": 1,
    "BuriedMothershipFeature": 3, "AlienMonolithFeature": 2,
}
for name, day in GATES.items():
    p = f"{E}/worldgen/{name}.java"
    src = open(p, encoding="utf-8").read()
    if "RECON PHASE" in src:
        print(f"  {name}: gate уже есть")
        continue
    anchor = "        WorldGenLevel level = ctx.level();"
    if anchor not in src:
        print(f"  !! {name}: нет якоря place()")
        continue
    gate = anchor + f"""
        // RECON PHASE: alien structures only appear once the invasion has landed.
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level.getLevel()) < {day}) return false;"""
    src = src.replace(anchor, gate, 1)
    open(p, "w", encoding="utf-8").write(src)
    print(f"  {name}: появляется с дня {day}")

# ============ 9. РЕДКОСТИ: всё значительно реже ============
RARITY = {
    "cave_dungeon": (95, 260), "infested_mine": (120, 300), "abandoned_lab": (95, 280),
    "cosmic_vault": (440, 650), "hive_nest": (340, 520), "alien_outpost": (560, 750),
    "crashed_ufo": (360, 600), "alien_monolith": (500, 650), "buried_mothership": (700, 900),
    "survivor_bunker": (250, 400),
}
for f, (old, new) in RARITY.items():
    p = f"{DATA}/worldgen/placed_feature/{f}.json"
    s = open(p).read()
    if f'"chance": {old}' in s:
        open(p, "w").write(s.replace(f'"chance": {old}', f'"chance": {new}'))
        print(f"  {f}: 1/{old} -> 1/{new}")
    else:
        print(f"  !! {f}: chance {old} не найден")

# ============ 10. НОВЫЕ ДАНЖИ: регистрация + JSON ============
patch(f"{E}/worldgen/ModFeatures.java", [
    ("""    public static final Feature<NoneFeatureConfiguration> SURVIVOR_BUNKER =
            new SurvivorBunkerFeature(NoneFeatureConfiguration.CODEC);""",
     """    public static final Feature<NoneFeatureConfiguration> SURVIVOR_BUNKER =
            new SurvivorBunkerFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> SAFEHOUSE =
            new SafehouseFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> FIELD_HOSPITAL =
            new FieldHospitalFeature(NoneFeatureConfiguration.CODEC);"""),
    ('    public static final ResourceKey<PlacedFeature> SURVIVOR_BUNKER_PLACED = pf("survivor_bunker");',
     '''    public static final ResourceKey<PlacedFeature> SURVIVOR_BUNKER_PLACED = pf("survivor_bunker");
    public static final ResourceKey<PlacedFeature> SAFEHOUSE_PLACED = pf("safehouse");
    public static final ResourceKey<PlacedFeature> FIELD_HOSPITAL_PLACED = pf("field_hospital");'''),
    ('        Registry.register(BuiltInRegistries.FEATURE, rl("survivor_bunker"), SURVIVOR_BUNKER);',
     '''        Registry.register(BuiltInRegistries.FEATURE, rl("survivor_bunker"), SURVIVOR_BUNKER);
        Registry.register(BuiltInRegistries.FEATURE, rl("safehouse"), SAFEHOUSE);
        Registry.register(BuiltInRegistries.FEATURE, rl("field_hospital"), FIELD_HOSPITAL);'''),
    ("""        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, SURVIVOR_BUNKER_PLACED);""",
     """        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, SURVIVOR_BUNKER_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, SAFEHOUSE_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, FIELD_HOSPITAL_PLACED);"""),
], "ModFeatures (+2 данжа)")

for name, chance in (("safehouse", 450), ("field_hospital", 520)):
    json.dump({"type": f"alien-invasion:{name}", "config": {}},
              open(f"{DATA}/worldgen/configured_feature/{name}.json", "w"), indent=2)
    json.dump({"feature": f"alien-invasion:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": chance},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"}]},
        open(f"{DATA}/worldgen/placed_feature/{name}.json", "w"), indent=2)
    print(f"  {name}: 1/{chance}")

# ============ 11. БУНКЕР: база больше + 3 профиля торговцев ============
src = open(f"{E}/worldgen/SurvivorBunkerFeature.java", encoding="utf-8").read()
a = src.index("        // Shell 11x5x9, hollow interior.")
b = src.index("        // Ladder shaft from the surface hatch down into the bunker.")
src = src[:a] + """        // TWO-ROOM SHELTER: main hall + storage annex, properly lived-in.
        StructureUtil.fillBox(level, c.offset(-7, -1, -4), c.offset(7, 4, 4), wall, false);
        StructureUtil.fillBox(level, c.offset(-6, 0, -3), c.offset(2, 3, 3), air, false);   // main hall
        StructureUtil.fillBox(level, c.offset(4, 0, -3), c.offset(6, 3, 3), air, false);    // storage annex
        StructureUtil.set(level, c.offset(3, 1, 0), air);                                   // doorway
        StructureUtil.set(level, c.offset(3, 2, 0), air);

        // Lighting.
        StructureUtil.set(level, c.offset(-5, 3, -2), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(0, 3, 2), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(5, 3, 0), ModBlocks.WARNING_LAMP.defaultBlockState());

        // Living corner: two bunks, furnace + crafting, a little carrot patch.
        StructureUtil.set(level, c.offset(-6, 0, -3), Blocks.RED_BED.defaultBlockState());
        StructureUtil.set(level, c.offset(-6, 0, 3), Blocks.WHITE_BED.defaultBlockState());
        StructureUtil.set(level, c.offset(-4, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        StructureUtil.set(level, c.offset(-3, 0, 3), Blocks.FURNACE.defaultBlockState());
        StructureUtil.set(level, c.offset(-2, 0, 3), Blocks.SMOKER.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, -1, 2), Blocks.FARMLAND.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, 0, 2), Blocks.CARROTS.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, -1, 3), Blocks.WATER.defaultBlockState());

        // Storage annex: barrels, crates and the good chest.
        StructureUtil.set(level, c.offset(6, 0, -3), Blocks.BARREL.defaultBlockState());
        StructureUtil.set(level, c.offset(6, 1, -3), Blocks.BARREL.defaultBlockState());
        StructureUtil.set(level, c.offset(6, 0, 3), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, c.offset(5, 0, 3), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.placeLootChest(level, c.offset(6, 0, 0), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.placeLootChest(level, c.offset(4, 0, -3), rng, ModFeatures.CAVE_DUNGEON_LOOT);

""" + src[b:]

# Профили торговцев: медик / фермер / инженер.
a = src.index("        // THE TRADER: clean supplies for alien salvage.")
b = src.index("        return true;\n    }\n}")
src = src[:a] + """        // THE TRADER: each bunker shelters a DIFFERENT survivor - a medic, a
        // farmer or an engineer - with their own face, name and barter list.
        Villager trader = EntityType.VILLAGER.create(level.getLevel());
        if (trader != null) {
            trader.moveTo(c.getX() + 0.5D, c.getY(), c.getZ() + 0.5D, rng.nextFloat() * 360.0F, 0.0F);
            int profile = rng.nextInt(3);
            VillagerProfession prof = profile == 0 ? VillagerProfession.CLERIC
                    : profile == 1 ? VillagerProfession.FARMER : VillagerProfession.TOOLSMITH;
            String name = profile == 0 ? "§aВыживший медик"
                    : profile == 1 ? "§aВыживший фермер" : "§aВыживший инженер";
            trader.setVillagerData(trader.getVillagerData().setProfession(prof).setLevel(5));
            trader.setCustomName(net.minecraft.network.chat.Component.literal(name));
            trader.setCustomNameVisible(true);
            trader.setPersistenceRequired();
            trader.addTag("BunkerTrader");
            if (profile == 0) { // МЕДИК: лекарства за кристаллы и реагенты
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL, 2),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.RAD_PILLS, 2), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 3),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.WEAK_ANTIDOTE, 2), 10, 8, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.INFESTED_FLESH, 6),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.BIO_SERUM, 1), 8, 8, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 2),
                        new ItemStack(Items.GOLDEN_APPLE, 1), 6, 10, 0.05F));
            } else if (profile == 1) { // ФЕРМЕР: чистая еда и вода
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 2),
                        new ItemStack(Items.BREAD, 8), 16, 2, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 6),
                        new ItemStack(Items.GOLDEN_CARROT, 4), 12, 4, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_INGOT, 3),
                        new ItemStack(Items.MILK_BUCKET), 8, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 3),
                        new ItemStack(Items.COOKED_BEEF, 5), 14, 3, 0.05F));
            } else { // ИНЖЕНЕР: компоненты и снаряжение
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.ALIEN_BATTERY, 1), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_INGOT, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.PLASMA_CELL, 3), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 5),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.GEIGER_COUNTER, 1), 4, 12, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.RAD_PILLS, 1), 8, 6, 0.05F));
            }
            level.addFreshEntity(trader);
        }
""" + src[b:]
open(f"{E}/worldgen/SurvivorBunkerFeature.java", "w", encoding="utf-8").write(src)
print("  SurvivorBunkerFeature: база 2 комнаты + 3 профиля торговца")

# ============ 12. КРОВАВЫЕ АССЕТЫ ============
JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar")
_zip = zipfile.ZipFile(JAR)


def vanilla_tex(name):
    return Image.open(io.BytesIO(_zip.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def bloodify(im, seed):
    im = im.convert("RGBA")
    px = im.load()
    rnd = random.Random(seed)
    for _ in range(4):  # кляксы
        cx, cy, r = rnd.randint(2, 13), rnd.randint(2, 13), rnd.randint(2, 4)
        for y in range(16):
            for x in range(16):
                d2 = (x - cx) ** 2 + (y - cy) ** 2
                if d2 <= r * r:
                    o = px[x, y]
                    dark = rnd.randint(-20, 5)
                    blend = 0.85 if d2 <= (r - 1) ** 2 else 0.5
                    px[x, y] = (int(o[0] * (1 - blend) + (135 + dark) * blend),
                                int(o[1] * (1 - blend) + 10 * blend),
                                int(o[2] * (1 - blend) + 18 * blend), 255)
    for _ in range(10):  # капли
        x, y = rnd.randint(0, 15), rnd.randint(0, 15)
        px[x, y] = (120 + rnd.randint(-15, 10), 8, 14, 255)
    return im


BLOODY_TEX = {
    "bloody_planks": "oak_planks", "bloody_stone": "stone",
    "bloody_dirt": "dirt", "bloody_stone_bricks": "stone_bricks",
}
for out, srcname in BLOODY_TEX.items():
    bloodify(vanilla_tex(srcname), hash(out) & 0xFFFF).save(f"{ASSETS}/textures/block/{out}.png")
    print(f"  tex {out}")


def jw(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w"), indent=2)


# Кубы.
for name in BLOODY_TEX:
    jw(f"{ASSETS}/blockstates/{name}.json", {"variants": {"": {"model": f"alien-invasion:block/{name}"}}})
    jw(f"{ASSETS}/models/block/{name}.json",
       {"parent": "minecraft:block/cube_all", "textures": {"all": f"alien-invasion:block/{name}"}})
    jw(f"{ASSETS}/models/item/{name}.json", {"parent": f"alien-invasion:block/{name}"})

# Ступени: blockstate копия ванильной + 3 модели.
for name, tex in (("bloody_plank_stairs", "bloody_planks"), ("bloody_stone_stairs", "bloody_stone")):
    bs = _zip.read("assets/minecraft/blockstates/oak_stairs.json").decode()
    bs = bs.replace("minecraft:block/oak_stairs", f"alien-invasion:block/{name}")
    open(f"{ASSETS}/blockstates/{name}.json", "w").write(bs)
    for suff, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        jw(f"{ASSETS}/models/block/{name}{suff}.json",
           {"parent": f"minecraft:block/{parent}",
            "textures": {"bottom": f"alien-invasion:block/{tex}",
                         "top": f"alien-invasion:block/{tex}",
                         "side": f"alien-invasion:block/{tex}"}})
    jw(f"{ASSETS}/models/item/{name}.json", {"parent": f"alien-invasion:block/{name}"})

# Плиты.
for name, tex, full in (("bloody_plank_slab", "bloody_planks", "bloody_planks"),
                        ("bloody_stone_slab", "bloody_stone", "bloody_stone")):
    jw(f"{ASSETS}/blockstates/{name}.json", {"variants": {
        "type=bottom": {"model": f"alien-invasion:block/{name}"},
        "type=top": {"model": f"alien-invasion:block/{name}_top"},
        "type=double": {"model": f"alien-invasion:block/{full}"}}})
    for suff, parent in (("", "slab"), ("_top", "slab_top")):
        jw(f"{ASSETS}/models/block/{name}{suff}.json",
           {"parent": f"minecraft:block/{parent}",
            "textures": {"bottom": f"alien-invasion:block/{tex}",
                         "top": f"alien-invasion:block/{tex}",
                         "side": f"alien-invasion:block/{tex}"}})
    jw(f"{ASSETS}/models/item/{name}.json", {"parent": f"alien-invasion:block/{name}"})

# Забор: blockstate копия + post/side/inventory.
bs = _zip.read("assets/minecraft/blockstates/oak_fence.json").decode()
bs = bs.replace("minecraft:block/oak_fence", "alien-invasion:block/bloody_plank_fence")
open(f"{ASSETS}/blockstates/bloody_plank_fence.json", "w").write(bs)
for suff, parent in (("_post", "fence_post"), ("_side", "fence_side"), ("_inventory", "fence_inventory")):
    jw(f"{ASSETS}/models/block/bloody_plank_fence{suff}.json",
       {"parent": f"minecraft:block/{parent}",
        "textures": {"texture": "alien-invasion:block/bloody_planks"}})
jw(f"{ASSETS}/models/item/bloody_plank_fence.json",
   {"parent": "alien-invasion:block/bloody_plank_fence_inventory"})

# Лут: кровавые дропают ЧИСТЫЙ ванильный блок.
BLOODY_DROPS = {
    "bloody_planks": "minecraft:oak_planks", "bloody_stone": "minecraft:cobblestone",
    "bloody_dirt": "minecraft:dirt", "bloody_stone_bricks": "minecraft:stone_bricks",
    "bloody_plank_stairs": "minecraft:oak_stairs", "bloody_stone_stairs": "minecraft:stone_stairs",
    "bloody_plank_slab": "minecraft:oak_slab", "bloody_stone_slab": "minecraft:stone_slab",
    "bloody_plank_fence": "minecraft:oak_fence",
}
for name, drop in BLOODY_DROPS.items():
    jw(f"{DATA}/loot_table/blocks/{name}.json",
       {"type": "minecraft:block", "pools": [{"rolls": 1,
         "entries": [{"type": "minecraft:item", "name": drop}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# Lang.
ru = {"block.alien-invasion.bloody_planks": "Окровавленные доски",
      "block.alien-invasion.bloody_stone": "Окровавленный камень",
      "block.alien-invasion.bloody_dirt": "Окровавленная земля",
      "block.alien-invasion.bloody_stone_bricks": "Окровавленные каменные кирпичи",
      "block.alien-invasion.bloody_plank_stairs": "Окровавленные деревянные ступени",
      "block.alien-invasion.bloody_stone_stairs": "Окровавленные каменные ступени",
      "block.alien-invasion.bloody_plank_slab": "Окровавленная деревянная плита",
      "block.alien-invasion.bloody_stone_slab": "Окровавленная каменная плита",
      "block.alien-invasion.bloody_plank_fence": "Окровавленный забор"}
en = {k: k.split(".")[-1].replace("bloody_", "Bloody ").replace("_", " ").title() for k in ru}
for path, add in ((f"{ASSETS}/lang/ru_ru.json", ru), (f"{ASSETS}/lang/en_us.json", en)):
    d = json.load(open(path, encoding="utf-8"))
    d.update(add)
    json.dump(d, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# ============ 13. РЕЦЕПТЫ недостающим предметам ============
RECIPES = {
    "weak_antidote": {"type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:red_mushroom"},
                        {"item": "minecraft:sugar"}, {"item": "minecraft:spider_eye"}],
        "result": {"id": "alien-invasion:weak_antidote", "count": 1}},
    "herbal_salve": {"type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:bowl"}, {"item": "minecraft:red_mushroom"},
                        {"item": "minecraft:brown_mushroom"}],
        "result": {"id": "alien-invasion:herbal_salve", "count": 2}},
    "comms_beacon": {"type": "minecraft:crafting_shaped",
        "pattern": [" I ", "IBI", "RPR"],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "B": {"item": "alien-invasion:alien_battery"},
                "R": {"item": "minecraft:redstone"}, "P": {"item": "alien-invasion:platinum_ingot"}},
        "result": {"id": "alien-invasion:comms_beacon", "count": 1}},
    "cosmic_stimulant": {"type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "alien-invasion:cosmic_shard"},
                        {"item": "minecraft:sugar"}, {"item": "minecraft:glistering_melon_slice"}],
        "result": {"id": "alien-invasion:cosmic_stimulant", "count": 1}},
    "blink_core": {"type": "minecraft:crafting_shaped",
        "pattern": [" C ", "CEC", " B "],
        "key": {"C": {"item": "alien-invasion:cosmic_shard"}, "E": {"item": "minecraft:ender_pearl"},
                "B": {"item": "alien-invasion:alien_battery"}},
        "result": {"id": "alien-invasion:blink_core", "count": 1}},
}
added = 0
for name, recipe in RECIPES.items():
    p = f"{DATA}/recipe/{name}.json"
    if not os.path.exists(p):
        jw(p, recipe)
        added += 1
print(f"  рецептов добавлено: {added}")
print("DONE")
