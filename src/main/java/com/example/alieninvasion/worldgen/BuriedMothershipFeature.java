package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * BURIED MOTHERSHIP: a colossal saucer (radius ~16) entombed deep under the
 * surface ages ago. The rarest, richest dungeon in the mod:
 *  - a breach shaft drops from a scarred, tendril-ringed crater on the surface
 *    straight through the hull;
 *  - inside: a glowing reactor core (radiation hazard), a command bridge with a
 *    black-market terminal and the jackpot chests, a cryo bay of infested-ice
 *    pods crawling with parasites, and a looted cargo hold;
 *  - garrisoned by a plasma caster, stalkers, a shaman and grunts.
 */
public class BuriedMothershipFeature extends Feature<NoneFeatureConfiguration> {
    public BuriedMothershipFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        // RECON PHASE: alien structures only appear once the invasion has landed.
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level.getLevel()) < 3) return false;
        RandomSource rng = ctx.random();
        BlockPos surface = ctx.origin();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, surface.getX(), surface.getZ());
        // The hull is entombed ~22 blocks down; bail out where the world is too thin.
        int centerY = surfaceY - 22;
        if (centerY - 8 <= level.getMinBuildHeight() + 4) return false;
        BlockPos c = new BlockPos(surface.getX(), centerY, surface.getZ());

        final int R = 16; // horizontal radius
        final int V = 6;  // vertical semi-axis
        BlockState hull = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState plate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        // --- 1) Ellipsoid hull: plated shell, hollow interior, porthole lights. ---
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -V; dy <= V; dy++) {
                    double nx = dx / (double) R, ny = dy / (double) V, nz = dz / (double) R;
                    double d2 = nx * nx + ny * ny + nz * nz;
                    if (d2 > 1.0) continue;
                    BlockPos p = c.offset(dx, dy, dz);
                    if (d2 >= 0.80) {
                        boolean porthole = dy == 0 && d2 >= 0.93
                                && Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dz, dx))), 30) < 4;
                        StructureUtil.set(level, p, porthole ? ModBlocks.DARK_MATTER_ORE.defaultBlockState()
                                : ((dx + dz) % 2 == 0 ? hull : plate));
                    } else {
                        StructureUtil.set(level, p, air);
                    }
                }
            }
        }

        // --- 2) Main deck: a walkable plate floor across the widest section. ---
        int deckY = -2;
        for (int dx = -R + 2; dx <= R - 2; dx++) {
            for (int dz = -R + 2; dz <= R - 2; dz++) {
                if (dx * dx + dz * dz > (R - 2) * (R - 2)) continue;
                StructureUtil.set(level, c.offset(dx, deckY, dz), plate);
            }
        }
        BlockPos deck = c.above(deckY + 1); // standing level on the deck

        // --- 3) Reactor core: a glowing, radioactive heart in the centre. ---
        for (int dy = deckY + 1; dy <= 3; dy++) {
            StructureUtil.set(level, c.offset(0, dy, 0), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
        }
        for (BlockPos ring : new BlockPos[]{deck.east(), deck.west(), deck.north(), deck.south()}) {
            StructureUtil.set(level, ring, rng.nextBoolean()
                    ? ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState()
                    : ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState());
        }
        StructureUtil.set(level, deck.offset(2, 2, 0), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, deck.offset(-2, 2, 0), ModBlocks.WARNING_LAMP.defaultBlockState());

        // --- 4) Command bridge (north): terminal + the jackpot. ---
        BlockPos bridge = deck.offset(0, 0, -(R - 6));
        StructureUtil.set(level, bridge, ModBlocks.BLACK_MARKET_TERMINAL.defaultBlockState());
        StructureUtil.set(level, bridge.above(), ModBlocks.DARK_MATTER_ORE.defaultBlockState());
        StructureUtil.placeLootChest(level, bridge.east(2), rng, ModFeatures.MOTHERSHIP_LOOT);
        StructureUtil.placeLootChest(level, bridge.west(2), rng, ModFeatures.MOTHERSHIP_LOOT);
        StructureUtil.spawnGuard(level, bridge.south(2), EntityRegistry.PLASMA_CASTER, rng);
        StructureUtil.spawnGuard(level, bridge.offset(3, 0, 2), EntityRegistry.ALIEN_STALKER, rng);
        StructureUtil.spawnGuard(level, bridge.offset(-3, 0, 2), EntityRegistry.ALIEN_STALKER, rng);

        // --- 5) Cryo bay (east): rows of infested-ice pods, parasites loose. ---
        BlockPos cryo = deck.offset(R - 6, 0, 0);
        for (int i = -2; i <= 2; i += 2) {
            StructureUtil.set(level, cryo.offset(0, 0, i), ModBlocks.INFESTED_ICE.defaultBlockState());
            StructureUtil.set(level, cryo.offset(0, 1, i), ModBlocks.INFESTED_ICE.defaultBlockState());
            StructureUtil.set(level, cryo.offset(2, 0, i), ModBlocks.INFESTED_ICE.defaultBlockState());
        }
        StructureUtil.set(level, cryo.offset(1, 0, 3), ModBlocks.ALIEN_STASH.defaultBlockState());
        StructureUtil.spawnGuard(level, cryo.offset(-1, 0, 0), EntityRegistry.PARASITE, rng);
        StructureUtil.spawnGuard(level, cryo.offset(-1, 0, 2), EntityRegistry.PARASITE, rng);
        StructureUtil.spawnGuard(level, cryo.offset(-2, 0, -2), EntityRegistry.HIVE_SHAMAN, rng);

        // --- 6) Cargo hold (west): crates, residue and two more caches. ---
        BlockPos cargo = deck.offset(-(R - 6), 0, 0);
        StructureUtil.placeLootChest(level, cargo, rng, ModFeatures.MOTHERSHIP_LOOT);
        StructureUtil.placeLootChest(level, cargo.offset(0, 0, 3), rng, ModFeatures.ALIEN_CITY_LOOT);
        StructureUtil.set(level, cargo.offset(1, 0, 1), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, cargo.offset(-1, 0, 2), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.set(level, cargo.offset(0, 0, -2), ModBlocks.ALIEN_STASH.defaultBlockState());
        for (int i = 0; i < 8; i++) {
            StructureUtil.set(level, cargo.offset(rng.nextInt(7) - 3, -1, rng.nextInt(7) - 3),
                    ModBlocks.ALIEN_RESIDUE.defaultBlockState());
        }
        StructureUtil.spawnGuard(level, cargo.offset(2, 0, -1), EntityRegistry.ALIEN_GRUNT, rng);
        StructureUtil.spawnGuard(level, cargo.offset(-2, 0, 1), EntityRegistry.ALIEN_GRUNT, rng);

        // --- 7) Breach shaft: from a scarred surface crater down through the hull. ---
        int sx = c.getX() + 5, sz = c.getZ() + 5;
        for (int y = surfaceY + 1; y > c.getY() + 2; y--) {
            for (int ox = 0; ox <= 1; ox++) {
                for (int oz = 0; oz <= 1; oz++) {
                    StructureUtil.set(level, new BlockPos(sx + ox, y, sz + oz), air);
                }
            }
        }
        // Register for the Radio Transmitter so players can hunt the signal.
        com.example.alieninvasion.logic.StructureLocationsData.get(level.getLevel()).add("mothership", c);

        // Scarred rim: infested ring + tendrils marking the entrance.
        for (int ox = -2; ox <= 3; ox++) {
            for (int oz = -2; oz <= 3; oz++) {
                BlockPos rim = new BlockPos(sx + ox, surfaceY - 1, sz + oz);
                BlockState rs = StructureUtil.getBlockState(level, rim);
                BlockState conv = com.example.alieninvasion.logic.ContaminationRules.contaminatedStateFor(rs);
                if (conv != null) StructureUtil.set(level, rim, conv);
                if (rng.nextInt(4) == 0 && StructureUtil.getBlockState(level, rim.above()).isAir()) {
                    StructureUtil.set(level, rim.above(), ModBlocks.ALIEN_TENDRILS.defaultBlockState());
                }
            }
        }
        return true;
    }
}
