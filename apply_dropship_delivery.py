# -*- coding: utf-8 -*-
"""Высадка ВСЕХ наземных пришельцев тарелками + осада десантом + аудит крафтов."""
import json
import os
import re


def patch(path, pairs):
    src = open(path, encoding="utf-8").read()
    for old, new in pairs:
        if old not in src:
            print("  !! NOT FOUND in %s: %r" % (path.split("/")[-1], old[:58]))
            continue
        src = src.replace(old, new, 1)
    open(path, "w", encoding="utf-8").write(src)
    print("  patched " + path.split("/")[-1])


E = "src/main/java/com/example/alieninvasion"

# 1) UfoEntity: новые виды груза в манифесте дропшипа.
patch(E + "/entity/UfoEntity.java", [
    ("""    private void spawnSquadMobs(ServerLevel level) {
        int grunts = 0, brutes = 0, trolls = 0, shamans = 0, stalkers = 0, casters = 0, telekinetics = 0, difficulty = 1;
        for (String tag : this.getTags()) {
            if (tag.startsWith("grunts:")) grunts = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("brutes:")) brutes = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("trolls:")) trolls = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("shamans:")) shamans = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("stalkers:")) stalkers = Integer.parseInt(tag.substring(9));
            else if (tag.startsWith("casters:")) casters = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("teles:")) telekinetics = Integer.parseInt(tag.substring(6));
            else if (tag.startsWith("diff:")) difficulty = Integer.parseInt(tag.substring(5));
        }

        BlockPos spawnPos = this.blockPosition().below(2);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, grunts, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_BRUTE, brutes, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_TROLL, trolls, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.HIVE_SHAMAN, shamans, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_STALKER, stalkers, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.PLASMA_CASTER, casters, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.TELEKINETIC_ALIEN, telekinetics, difficulty);
    }""",
     """    private void spawnSquadMobs(ServerLevel level) {
        int grunts = 0, brutes = 0, trolls = 0, shamans = 0, stalkers = 0, casters = 0, telekinetics = 0;
        int workers = 0, worms = 0, raptors = 0, chickens = 0, spitters = 0, breachers = 0, difficulty = 1;
        for (String tag : this.getTags()) {
            if (tag.startsWith("grunts:")) grunts = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("brutes:")) brutes = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("trolls:")) trolls = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("shamans:")) shamans = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("stalkers:")) stalkers = Integer.parseInt(tag.substring(9));
            else if (tag.startsWith("casters:")) casters = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("teles:")) telekinetics = Integer.parseInt(tag.substring(6));
            else if (tag.startsWith("workers:")) workers = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("worms:")) worms = Integer.parseInt(tag.substring(6));
            else if (tag.startsWith("raptors:")) raptors = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("chickens:")) chickens = Integer.parseInt(tag.substring(9));
            else if (tag.startsWith("spitters:")) spitters = Integer.parseInt(tag.substring(9));
            else if (tag.startsWith("breachers:")) breachers = Integer.parseInt(tag.substring(10));
            else if (tag.startsWith("diff:")) difficulty = Integer.parseInt(tag.substring(5));
        }

        BlockPos spawnPos = this.blockPosition().below(2);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, grunts, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_BRUTE, brutes, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_TROLL, trolls, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.HIVE_SHAMAN, shamans, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_STALKER, stalkers, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.PLASMA_CASTER, casters, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.TELEKINETIC_ALIEN, telekinetics, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.INFESTED_WORM, worms, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_RAPTOR, raptors, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_CHICKEN, chickens, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ACID_SPITTER, spitters, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_BREACHER, breachers, difficulty);
        // Workers ride the same ship: grunts flagged as scavengers on touchdown.
        for (int i = 0; i < workers; i++) {
            com.example.alieninvasion.entity.AlienGruntEntity worker =
                    com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT.create(level);
            if (worker != null) {
                worker.moveTo(spawnPos.getX() + this.random.nextDouble() * 2.0D - 1.0D, spawnPos.getY(),
                        spawnPos.getZ() + this.random.nextDouble() * 2.0D - 1.0D, this.random.nextFloat() * 360F, 0);
                worker.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                worker.setScavenger(true);
                worker.setCustomName(net.minecraft.network.chat.Component.literal("\\u00a7a\\u041f\\u0440\\u0438\\u0448\\u0435\\u043b\\u0435\\u0446-\\u0440\\u0430\\u0431\\u043e\\u0447\\u0438\\u0439"));
                worker.setCustomNameVisible(true);
                worker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0));
                level.addFreshEntity(worker);
            }
        }
    }"""),
])

# 2) AlienSpawner: все поверхностные спавны - только высадкой.
patch(E + "/world/AlienSpawner.java", [
    ("    private static void spawnViaDropship(ServerLevel level, BlockPos groundPos, int grunts, int brutes, int trolls, int shamans, int stalkers, int casters, int telekinetics, int difficulty) {",
     "    private static void spawnViaDropship(ServerLevel level, BlockPos groundPos, int grunts, int brutes, int trolls, int shamans, int stalkers, int casters, int telekinetics, int difficulty, String... extraCargo) {"),
    ('            ufo.addTag("diff:" + difficulty);',
     '            for (String extra : extraCargo) {\n                ufo.addTag(extra);\n            }\n            ufo.addTag("diff:" + difficulty);'),
    ("""            int workers = 1 + level.random.nextInt(2);
            for (int i = 0; i < workers; i++) {
                com.example.alieninvasion.entity.AlienGruntEntity worker = EntityRegistry.ALIEN_GRUNT.create(level);
                if (worker != null) {
                    worker.moveTo(pos.getX() + 0.5D + level.random.nextInt(5) - 2, pos.getY(),
                            pos.getZ() + 0.5D + level.random.nextInt(5) - 2, level.random.nextFloat() * 360F, 0F);
                    worker.setScavenger(true);
                    worker.setCustomName(net.minecraft.network.chat.Component.literal("\\u00a7a\\u041f\\u0440\\u0438\\u0448\\u0435\\u043b\\u0435\\u0446-\\u0440\\u0430\\u0431\\u043e\\u0447\\u0438\\u0439"));
                    level.addFreshEntity(worker);
                }
            }
            spawnMob(level, pos, EntityRegistry.INFESTED_WORM, 1 + level.random.nextInt(2), difficulty);
            if (level.random.nextFloat() < 0.15f) {
                spawnMob(level, pos, EntityRegistry.ALIEN_GRUNT, 1, difficulty);
            }""",
     """            int workers = 1 + level.random.nextInt(2);
            int wormCargo = 1 + level.random.nextInt(2);
            int soloGrunt = level.random.nextFloat() < 0.15f ? 1 : 0;
            spawnViaDropship(level, pos, soloGrunt, 0, 0, 0, 0, 0, 0, difficulty,
                    "workers:" + workers, "worms:" + wormCargo);"""),
    ("""            spawnViaDropship(level, pos, grunts, 0, trolls, 0, 0, 0, 0, difficulty);
            spawnMob(level, pos, EntityRegistry.INFESTED_WORM, level.random.nextInt(2), difficulty);
            if (difficulty >= 2 && level.random.nextFloat() < 0.35f) {
                spawnMob(level, pos, EntityRegistry.ALIEN_RAPTOR, 1, difficulty);
            }
            spawnMob(level, pos, EntityRegistry.ALIEN_CHICKEN, level.random.nextInt(2), difficulty);""",
     """            int wormCargo = level.random.nextInt(2);
            int raptorCargo = (difficulty >= 2 && level.random.nextFloat() < 0.35f) ? 1 : 0;
            spawnViaDropship(level, pos, grunts, 0, trolls, 0, 0, 0, 0, difficulty,
                    "worms:" + wormCargo, "raptors:" + raptorCargo,
                    "chickens:" + level.random.nextInt(2));"""),
    ("""            spawnViaDropship(level, pos, grunts, brutes, trolls, 0, stalkers, casters, 0, difficulty);
            spawnMob(level, pos, EntityRegistry.ALIEN_CHICKEN, level.random.nextInt(2), difficulty);
            if (difficulty >= 3 && level.random.nextFloat() < 0.25f) {
                spawnMob(level, pos, EntityRegistry.ACID_SPITTER, 1, difficulty); // acid siege unit
            }""",
     """            int spitterCargo = (difficulty >= 3 && level.random.nextFloat() < 0.25f) ? 1 : 0;
            spawnViaDropship(level, pos, grunts, brutes, trolls, 0, stalkers, casters, 0, difficulty,
                    "chickens:" + level.random.nextInt(2), "spitters:" + spitterCargo);"""),
    ("""                spawnViaDropship(level, pos, 0, 0, 0, shamans, 0, 0, 0, difficulty);
                spawnMob(level, pos, EntityRegistry.HIVE_TYRANT, 1, difficulty);
                spawnMob(level, pos, EntityRegistry.ALIEN_CHICKEN, 1, difficulty);""",
     """                spawnViaDropship(level, pos, 0, 0, 0, shamans, 0, 0, 0, difficulty, "chickens:1");
                spawnMob(level, pos, EntityRegistry.HIVE_TYRANT, 1, difficulty); // boss bursts from the ground"""),
])

# 3) Осада: десант с корабля-носителя.
patch(E + "/world/InvasionManager.java", [
    ("""                    int count = 4 + level.random.nextInt(3) + this.invasionDays / 3;
                    for (int i = 0; i < count; i++) {
                        int bx = cp.getMinBlockX() + level.random.nextInt(16);
                        int bz = cp.getMinBlockZ() + level.random.nextInt(16);
                        int by = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx, bz);
                        net.minecraft.world.entity.Mob raider = (i % 4 == 3
                                ? com.example.alieninvasion.registry.EntityRegistry.ALIEN_BREACHER
                                : com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT).create(level);
                        if (raider != null) {
                            raider.moveTo(bx + 0.5D, by, bz + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                            raider.setTarget(player);
                            level.addFreshEntity(raider);
                        }
                    }""",
     """                    // The siege arrives BY DROPSHIP: a carrier descends over the
                    // reclaimed chunk and unloads the assault squad on a light beam.
                    int count = 4 + level.random.nextInt(3) + this.invasionDays / 3;
                    int breacherCount = Math.max(1, count / 4);
                    int gruntCount = count - breacherCount;
                    int bx = cp.getMinBlockX() + 8;
                    int bz = cp.getMinBlockZ() + 8;
                    int by = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    com.example.alieninvasion.entity.UfoEntity carrier =
                            com.example.alieninvasion.registry.EntityRegistry.UFO.create(level);
                    if (carrier != null) {
                        carrier.moveTo(bx + 0.5D, by + 32, bz + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                        carrier.setVariant(com.example.alieninvasion.entity.UfoEntity.CARRIER);
                        carrier.addTag("Dropship");
                        carrier.addTag("grunts:" + gruntCount);
                        carrier.addTag("breachers:" + breacherCount);
                        carrier.addTag("diff:" + this.invasionDays);
                        carrier.setPersistenceRequired();
                        level.addFreshEntity(carrier);
                    }"""),
])

# 4) Наземные события: вылазят ИЗ земли с эффектом копания.
patch(E + "/events/ModEvents.java", [
    ("""                            if (level.getBlockState(sp).isAir()) {
                                AlienGruntEntity g = EntityRegistry.ALIEN_GRUNT.create(level);
                                if (g != null) {
                                    g.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
                                    g.setTarget(p);
                                    level.addFreshEntity(g);
                                }
                            }""",
     """                            if (level.getBlockState(sp).isAir()) {
                                AlienGruntEntity g = EntityRegistry.ALIEN_GRUNT.create(level);
                                if (g != null) {
                                    g.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
                                    g.setTarget(p);
                                    level.addFreshEntity(g);
                                    // It CLAWS OUT of the rotten ground, not out of thin air.
                                    level.levelEvent(2001, sp.below(), net.minecraft.world.level.block.Block.getId(
                                            level.getBlockState(sp.below())));
                                }
                            }"""),
    ("""                    if (level.isLoaded(ppos) && level.getBlockState(ppos).isAir()) {
                        com.example.alieninvasion.entity.ParasiteEntity parasite = EntityRegistry.PARASITE.create(level);
                        if (parasite != null) {
                            parasite.moveTo(px, py, pz, level.random.nextFloat() * 360F, 0F);
                            parasite.setTarget(player);
                            level.addFreshEntity(parasite);
                        }
                    }""",
     """                    if (level.isLoaded(ppos) && level.getBlockState(ppos).isAir()) {
                        com.example.alieninvasion.entity.ParasiteEntity parasite = EntityRegistry.PARASITE.create(level);
                        if (parasite != null) {
                            parasite.moveTo(px, py, pz, level.random.nextFloat() * 360F, 0F);
                            parasite.setTarget(player);
                            level.addFreshEntity(parasite);
                            // Wriggles up out of the soil.
                            level.levelEvent(2001, ppos.below(), net.minecraft.world.level.block.Block.getId(
                                    level.getBlockState(ppos.below())));
                        }
                    }"""),
])
print("spawn delivery done")

# ============ 5) Полный аудит крафтов ============
items = set()
reg = open(E + "/registry/ItemRegistry.java", encoding="utf-8").read()
items.update(re.findall(r'registerItem\("([a-z_0-9]+)"', reg))
crafted = set()
RD = "src/main/resources/data/alien-invasion/recipe"
for f in os.listdir(RD):
    try:
        d = json.load(open(RD + "/" + f, encoding="utf-8"))
    except Exception:
        continue
    rid = (d.get("result") or {}).get("id", "")
    if rid.startswith("alien-invasion:"):
        crafted.add(rid.split(":", 1)[1])

missing = sorted(i for i in items if i not in crafted
                 and not i.endswith("_spawn_egg")
                 and i not in ("plasma_bolt",))
print("БЕЗ РЕЦЕПТА: " + (", ".join(missing) if missing else "нет"))


def jw(name, obj):
    json.dump(obj, open(RD + "/" + name + ".json", "w"), indent=2)


def armor_set(material, ingot):
    pats = {"helmet": ["III", "I I"], "chestplate": ["I I", "III", "III"],
            "leggings": ["III", "I I", "I I"], "boots": ["I I", "I I"]}
    for piece, pattern in pats.items():
        name = material + "_" + piece
        if name in missing and name not in crafted:
            jw(name, {"type": "minecraft:crafting_shaped", "pattern": pattern,
                      "key": {"I": {"item": ingot}},
                      "result": {"id": "alien-invasion:" + name, "count": 1}})
            crafted.add(name)
            print("  + " + name)


def tool_set(material, ingot):
    pats = {"sword": ["I", "I", "S"], "pickaxe": ["III", " S ", " S "],
            "axe": ["II", "IS", " S"], "shovel": ["I", "S", "S"], "hoe": ["II", " S", " S"]}
    for piece, pattern in pats.items():
        name = material + "_" + piece
        if name in missing and name not in crafted:
            jw(name, {"type": "minecraft:crafting_shaped", "pattern": pattern,
                      "key": {"I": {"item": ingot}, "S": {"item": "minecraft:stick"}},
                      "result": {"id": "alien-invasion:" + name, "count": 1}})
            crafted.add(name)
            print("  + " + name)


armor_set("platinum", "alien-invasion:platinum_ingot")
armor_set("palladium", "alien-invasion:palladium_ingot")
armor_set("cosmic", "alien-invasion:cosmic_ingot")
tool_set("platinum", "alien-invasion:platinum_ingot")
tool_set("palladium", "alien-invasion:palladium_ingot")
tool_set("nibirium", "alien-invasion:nibirium_ingot")

for piece, pattern in (("helmet", ["SSS", "SPS"]), ("chestplate", ["S S", "SPS", "SSS"]),
                        ("leggings", ["SPS", "S S", "S S"]), ("boots", ["S S", "P P"])):
    for mat, metal in (("alien_hazmat", "alien-invasion:platinum_ingot"),
                       ("alien_chem", "alien-invasion:palladium_ingot")):
        name = mat + "_" + piece
        if name in missing and name not in crafted:
            jw(name, {"type": "minecraft:crafting_shaped", "pattern": pattern,
                      "key": {"S": {"item": "alien-invasion:alien_skin"}, "P": {"item": metal}},
                      "result": {"id": "alien-invasion:" + name, "count": 1}})
            crafted.add(name)
            print("  + " + name)

SPECIALS = {
    "gravity_boots": {"type": "minecraft:crafting_shaped", "pattern": ["B B", "P P", "C C"],
        "key": {"B": {"item": "alien-invasion:alien_battery"},
                "P": {"item": "alien-invasion:palladium_ingot"},
                "C": {"item": "alien-invasion:cosmic_shard"}},
        "result": {"id": "alien-invasion:gravity_boots", "count": 1}},
    "cosmic_ingot": {"type": "minecraft:crafting_shaped", "pattern": ["CC", "CC"],
        "key": {"C": {"item": "alien-invasion:cosmic_shard"}},
        "result": {"id": "alien-invasion:cosmic_ingot", "count": 1}},
    "platinum_ingot": {"type": "minecraft:smelting",
        "ingredient": {"item": "alien-invasion:raw_platinum"},
        "result": {"id": "alien-invasion:platinum_ingot"}, "experience": 0.8, "cookingtime": 200},
    "palladium_ingot": {"type": "minecraft:smelting",
        "ingredient": {"item": "alien-invasion:raw_palladium"},
        "result": {"id": "alien-invasion:palladium_ingot"}, "experience": 0.8, "cookingtime": 200},
}
for name, recipe in SPECIALS.items():
    if name in missing and name not in crafted:
        fname = name if not name.endswith("_ingot") else name + "_smelting"
        jw(fname, recipe)
        crafted.add(name)
        print("  + " + name)

left = sorted(i for i in missing if i not in crafted)
print("ЛУТ-ОНЛИ (без крафта осознанно): " + (", ".join(left) if left else "нет"))
