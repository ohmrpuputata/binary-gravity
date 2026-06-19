package com.example.alieninvasion.world;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.entity.UfoEntity;
import com.example.alieninvasion.logic.AlienEvolution;
import com.example.alieninvasion.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public class AlienSpawner {

    // Hard cap on how many aliens may crowd a single player. Stops the swarm from
    // snowballing into hundreds of mobs (which both lags the game, leaves no room
    // for vanilla mobs to spawn, and is impossible to survive).
    private static final int MAX_NEARBY_ALIENS = 14;
    // Co-op: each extra player sharing the area raises the local cap, so a group
    // faces a bigger swarm instead of four players splitting one solo-sized cap.
    private static final int CAP_PER_EXTRA_PLAYER = 6;
    private static final double POP_RADIUS = 48.0D;
    // Server-wide ceiling: per-player events otherwise stack hundreds of
    // persistent aliens on a big server with players spread across the map.
    private static final int GLOBAL_CAP_BASE = 60;
    private static final int GLOBAL_CAP_PER_PLAYER = 30;

    public static void spawnerTick(ServerLevel level, int difficulty) {
        if (InvasionManager.get(level).isVictoryAchieved()) {
            return;
        }
        // Вторжение НАЧИНАЕТСЯ С ПЕРВОЙ НОЧИ: первая половина Дня 0 (до ночи) — чисто,
        // без спавна и заражения. Дальше рой давит круглосуточно (см. свет ниже).
        if (!com.example.alieninvasion.logic.SurvivalManager.isAlienInvasionActive(level)) {
            return;
        }
        if (level.random.nextInt(400) != 0)
            return; // a squad roughly every ~20 seconds

        List<ServerPlayer> players = level.players();
        if (players.isEmpty())
            return;

        if (countAllAliens(level) >= GLOBAL_CAP_BASE + GLOBAL_CAP_PER_PLAYER * players.size()) {
            return;
        }

        ServerPlayer player = players.get(level.random.nextInt(players.size()));

        // Respect the population cap so we never bury the player in mobs.
        if (countNearbyAliens(level, player.blockPosition(), POP_RADIUS) >= localAlienCap(level, player)) {
            return;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(),
                player.getBlockZ());

        // If the player is airborne high in the sky, dispatch interceptors
        if (player.getY() > surfaceY + 25) {
            spawnSkyInterceptor(level, player, difficulty);
            return;
        }

        // If the player has burrowed deep underground, the hive senses them and
        // ambushes from the surrounding caves - there is no safe hole to hide in.
        if (player.getBlockY() < surfaceY - 6) {
            spawnUndergroundAmbush(level, player, difficulty);
            return;
        }

        // Surface raid near the player.
        int range = 40;
        int x = (int) player.getX() + level.random.nextInt(range * 2) - range;
        int z = (int) player.getZ() + level.random.nextInt(range * 2) - range;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawnPos = new BlockPos(x, y, z);

        if (level.isLoaded(spawnPos)) {
            // Пришельцы лезут и ДНЁМ, но реже: в темноте/ночью — всегда, при дневном
            // свете — примерно 1/3 попыток (баланс, чтобы день не превратился в ад).
            boolean dark = level.getMaxLocalRawBrightness(spawnPos) < 7;
            if (dark || level.random.nextInt(3) == 0) {
                spawnSquad(level, spawnPos, difficulty);
            }
        }
    }

    private static void spawnSquad(ServerLevel level, BlockPos pos, int difficulty) {
        if (difficulty <= 0) {
            // DAY 0 - PURE RECON: the swarm only scouts. A dropship unloads
            // WORKERS that mine and haul; worms wriggle in the dirt; at most a
            // single lone soldier, rarely. No combat squads whatsoever.
            int workers = 1 + level.random.nextInt(2);
            int wormCargo = 1 + level.random.nextInt(2);
            int soloGrunt = level.random.nextFloat() < 0.15f ? 1 : 0;
            // EVERYTHING arrives by dropship - nothing pops out of thin air.
            spawnViaDropship(level, pos, soloGrunt, 0, 0, 0, 0, 0, 0, difficulty,
                    "workers:" + workers, "worms:" + wormCargo);
        } else if (difficulty < 3) {
            // DAYS 1-2 - PROBING: small grunt packs with worms; raptors join on
            // day 2. No casters, no brutes - escalation is gradual now.
            int grunts = 1 + level.random.nextInt(2);
            int trolls = level.random.nextFloat() < 0.15f ? 1 : 0;

            int wormCargo = level.random.nextInt(2);
            int raptorCargo = (difficulty >= 2 && level.random.nextFloat() < 0.35f) ? 1 : 0;
            spawnViaDropship(level, pos, grunts, 0, trolls, 0, 0, 0, 0, difficulty,
                    "worms:" + wormCargo, "raptors:" + raptorCargo,
                    "chickens:" + level.random.nextInt(2));
        } else if (difficulty < 6) {
            // Assault Squad: brutes only join from day 4.
            int brutes = difficulty >= 4 ? 1 : 0;
            int grunts = 2;
            int trolls = level.random.nextFloat() < 0.25f ? 1 : 0;
            int casters = (difficulty >= 4 && level.random.nextFloat() < 0.20f) ? 1 : 0;
            int stalkers = 0; // stalkers are elite hunters - Total War (day 6+) only
            
            int spitterCargo = (difficulty >= 3 && level.random.nextFloat() < 0.25f) ? 1 : 0;
            spawnViaDropship(level, pos, grunts, brutes, trolls, 0, stalkers, casters, 0, difficulty,
                    "chickens:" + level.random.nextInt(2), "spitters:" + spitterCargo);
        } else {
            // Total War: aerial / psychic threats, plus a rare Hive Tyrant boss + escorts.
            if (difficulty >= 8 && level.random.nextFloat() < 0.10f) {
                int shamans = level.random.nextFloat() < 0.5f ? 1 : 0;
                
                spawnViaDropship(level, pos, 0, 0, 0, shamans, 0, 0, 0, difficulty, "chickens:1");
                spawnMob(level, pos, EntityRegistry.HIVE_TYRANT, 1, difficulty); // boss bursts from the ground
            } else if (level.random.nextBoolean()) {
                spawnMob(level, pos.above(10), EntityRegistry.UFO, 1, difficulty);
            } else {
                int telekinetics = 1;
                int grunts = 2;
                int trolls = level.random.nextFloat() < 0.15f ? 1 : 0;
                int casters = level.random.nextFloat() < 0.18f ? 1 : 0;
                int stalkers = level.random.nextFloat() < 0.15f ? 1 : 0;
                int shamans = (difficulty >= 7 && level.random.nextFloat() < 0.10f) ? 1 : 0;
                
                spawnViaDropship(level, pos, grunts, 0, trolls, shamans, stalkers, casters, telekinetics, difficulty);
            }
        }
    }

    private static void spawnViaDropship(ServerLevel level, BlockPos groundPos, int grunts, int brutes, int trolls, int shamans, int stalkers, int casters, int telekinetics, int difficulty, String... extraCargo) {
        BlockPos spawnPos = groundPos.above(35);
        UfoEntity ufo = EntityRegistry.UFO.create(level);
        if (ufo != null) {
            ufo.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0);
            ufo.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            ufo.setVariant(UfoEntity.CARRIER); // Big Carrier variant
            ufo.addTag("Dropship");
            if (grunts > 0) ufo.addTag("grunts:" + grunts);
            if (brutes > 0) ufo.addTag("brutes:" + brutes);
            if (trolls > 0) ufo.addTag("trolls:" + trolls);
            if (shamans > 0) ufo.addTag("shamans:" + shamans);
            if (stalkers > 0) ufo.addTag("stalkers:" + stalkers);
            if (casters > 0) ufo.addTag("casters:" + casters);
            if (telekinetics > 0) ufo.addTag("teles:" + telekinetics);
            for (String extra : extraCargo) {
                ufo.addTag(extra);
            }
            ufo.addTag("diff:" + difficulty);
            ufo.setPersistenceRequired();
            level.addFreshEntity(ufo);
        } else {
            // Fallback to direct spawn if UFO fails to create
            if (grunts > 0) spawnMob(level, groundPos, EntityRegistry.ALIEN_GRUNT, grunts, difficulty);
            if (brutes > 0) spawnMob(level, groundPos, EntityRegistry.ALIEN_BRUTE, brutes, difficulty);
            if (trolls > 0) spawnMob(level, groundPos, EntityRegistry.ALIEN_TROLL, trolls, difficulty);
            if (shamans > 0) spawnMob(level, groundPos, EntityRegistry.HIVE_SHAMAN, shamans, difficulty);
            if (stalkers > 0) spawnMob(level, groundPos, EntityRegistry.ALIEN_STALKER, stalkers, difficulty);
            if (casters > 0) spawnMob(level, groundPos, EntityRegistry.PLASMA_CASTER, casters, difficulty);
            if (telekinetics > 0) spawnMob(level, groundPos, EntityRegistry.TELEKINETIC_ALIEN, telekinetics, difficulty);
        }
    }

    // Find dark air pockets in the caves around the player and spawn a small
    // burrowing squad there.
    private static void spawnUndergroundAmbush(ServerLevel level, ServerPlayer player, int difficulty) {
        int squadSize = 2 + level.random.nextInt(2);
        int spawned = 0;
        BlockPos origin = player.blockPosition();

        for (int attempt = 0; attempt < 16 && spawned < squadSize; attempt++) {
            BlockPos p = origin.offset(level.random.nextInt(25) - 12, level.random.nextInt(9) - 4,
                    level.random.nextInt(25) - 12);
            if (!level.isLoaded(p)) {
                continue;
            }
            boolean roomToStand = level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir();
            boolean solidFloor = level.getBlockState(p.below()).isSolidRender(level, p.below());
            if (roomToStand && solidFloor && level.getMaxLocalRawBrightness(p) < 8
                    && p.distSqr(origin) > 9) {
                if (level.random.nextFloat() < 0.25f) {
                    level.setBlockAndUpdate(p.below(), com.example.alieninvasion.registry.ModBlocks.ALIEN_HIVE.defaultBlockState());
                }
                EntityType<?> type = pickUndergroundType(level, difficulty);
                spawnMob(level, p, type, 1, difficulty);
                level.levelEvent(2001, p, net.minecraft.world.level.block.Block.getId(
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())); // dig effect
                spawned++;
            }
        }
    }

    private static EntityType<?> pickUndergroundType(ServerLevel level, int difficulty) {
        // Difficulty-gated weighted pool. The previous single-roll cascade had
        // mis-ordered thresholds (the plasma caster was shadowed by the breacher
        // and never spawned) and never included the acid spitter at all - this
        // weighted pick fixes both and keeps the day-by-day escalation.
        java.util.List<EntityType<?>> pool = new java.util.ArrayList<>();
        java.util.List<Integer> weights = new java.util.ArrayList<>();
        pool.add(EntityRegistry.ALIEN_GRUNT);   weights.add(difficulty < 1 ? 8 : 22);
        pool.add(EntityRegistry.INFESTED_WORM); weights.add(20);
        pool.add(EntityRegistry.ALIEN_TROLL);   weights.add(4);
        pool.add(EntityRegistry.ALIEN_CHICKEN); weights.add(6);
        if (difficulty >= 2) { pool.add(EntityRegistry.ALIEN_RAPTOR);      weights.add(12); }
        if (difficulty >= 2) { pool.add(EntityRegistry.CAVE_LURKER);       weights.add(14); }
        if (difficulty >= 3) { pool.add(EntityRegistry.ALIEN_BREACHER);    weights.add(14); }
        if (difficulty >= 3) { pool.add(EntityRegistry.ACID_SPITTER);      weights.add(12); }
        if (difficulty >= 4) { pool.add(EntityRegistry.PLASMA_CASTER);     weights.add(10); }
        if (difficulty >= 6) { pool.add(EntityRegistry.ALIEN_STALKER);     weights.add(9); }
        if (difficulty >= 6) { pool.add(EntityRegistry.TELEKINETIC_ALIEN); weights.add(8); }

        int total = 0;
        for (int w : weights) { total += w; }
        int roll = level.random.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) { return pool.get(i); }
        }
        return EntityRegistry.ALIEN_GRUNT;
    }

    private static int countNearbyAliens(ServerLevel level, BlockPos center, double radius) {
        return level.getEntitiesOfClass(Mob.class,
                new net.minecraft.world.phys.AABB(center).inflate(radius),
                e -> AlienUtils.isAlliedTo(null, e)).size();
    }

    private static int localAlienCap(ServerLevel level, ServerPlayer around) {
        int nearbyPlayers = 0;
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(around) <= 96.0D * 96.0D) {
                nearbyPlayers++;
            }
        }
        return MAX_NEARBY_ALIENS + CAP_PER_EXTRA_PLAYER * Math.max(0, nearbyPlayers - 1);
    }

    private static int countAllAliens(ServerLevel level) {
        int total = 0;
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (e instanceof Mob && AlienUtils.isAlliedTo(null, e)) {
                total++;
            }
        }
        return total;
    }

    private static void spawnMob(ServerLevel level, BlockPos pos, EntityType<?> type, int count, int difficulty) {
        for (int i = 0; i < count; i++) {
            Mob mob = (Mob) type.create(level);
            if (mob != null) {
                mob.moveTo(pos.getX() + level.random.nextDouble(), pos.getY() + 1,
                        pos.getZ() + level.random.nextDouble(), level.random.nextFloat() * 360F, 0);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                AlienEvolution.evolve(mob, difficulty); // scale with invasion progress
                // UFO variants: scouts early, destroyers/carriers in Total War.
                if (mob instanceof com.example.alieninvasion.entity.UfoEntity ufo) {
                    int v = com.example.alieninvasion.entity.UfoEntity.SCOUT;
                    if (difficulty >= 6 && level.random.nextFloat() < 0.45f) {
                        v = level.random.nextBoolean() ? com.example.alieninvasion.entity.UfoEntity.DESTROYER
                                : com.example.alieninvasion.entity.UfoEntity.CARRIER;
                    } else if (difficulty >= 4 && level.random.nextFloat() < 0.3f) {
                        v = com.example.alieninvasion.entity.UfoEntity.DESTROYER;
                    }
                    ufo.setVariant(v, difficulty);
                }
                level.addFreshEntity(mob);
            }
        }
    }

    private static void spawnSkyInterceptor(ServerLevel level, ServerPlayer player, int difficulty) {
        BlockPos pos = player.blockPosition().above(5 + level.random.nextInt(5));
        
        // Spawn 1-2 Sky Drones
        int dronesCount = 1 + level.random.nextInt(2);
        for (int i = 0; i < dronesCount; i++) {
            com.example.alieninvasion.entity.SkyDroneEntity drone = EntityRegistry.SKY_DRONE.create(level);
            if (drone != null) {
                drone.moveTo(player.getX() + (level.random.nextDouble() - 0.5) * 10,
                             player.getY() + 10 + level.random.nextInt(5),
                             player.getZ() + (level.random.nextDouble() - 0.5) * 10,
                             level.random.nextFloat() * 360F, 0);
                drone.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                AlienEvolution.evolve(drone, difficulty);
                drone.setTarget(player);
                level.addFreshEntity(drone);
            }
        }
        
        // 35% chance to also spawn a UFO interceptor
        if (level.random.nextFloat() < 0.35f) {
            com.example.alieninvasion.entity.UfoEntity ufo = EntityRegistry.UFO.create(level);
            if (ufo != null) {
                ufo.moveTo(player.getX() + (level.random.nextDouble() - 0.5) * 20,
                           player.getY() + 15 + level.random.nextInt(5),
                           player.getZ() + (level.random.nextDouble() - 0.5) * 20,
                           level.random.nextFloat() * 360F, 0);
                ufo.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                AlienEvolution.evolve(ufo, difficulty);
                ufo.setVariant(level.random.nextBoolean() ? UfoEntity.SCOUT : UfoEntity.DESTROYER, difficulty);
                ufo.setTarget(player);
                level.addFreshEntity(ufo);
            }
        }
    }
}
