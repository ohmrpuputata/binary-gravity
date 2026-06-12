package com.example.alieninvasion.worldgen;

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
 * Alien City (Outpost): a surface settlement of the swarm - a central infested
 * spire crowned with an Orbital Strike Beacon, two organic huts, corrupted
 * ground, eerie crystal lights and THREE high-value loot chests. The richest
 * find in the overworld, and dangerous (the hives spawn defenders).
 */
public class AlienOutpostFeature extends Feature<NoneFeatureConfiguration> {
    public AlienOutpostFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private int surfaceY(WorldGenLevel level, BlockPos origin, int x, int z) {
        if (level.hasChunk(x >> 4, z >> 4)) {
            return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        }
        return origin.getY();
    }

    private void foundation(WorldGenLevel level, int x, int z, int topY, BlockState fill) {
        // Fill from one below the top down to the ground so buildings don't float.
        for (int y = topY; y > topY - 6; y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState state = StructureUtil.getBlockState(level, p);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                StructureUtil.set(level, p, fill);
            } else {
                break;
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        // RECON PHASE: alien structures only appear once the invasion has landed.
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level.getLevel()) < 2) return false;
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        int baseY = surfaceY(level, o, o.getX(), o.getZ());
        if (baseY <= level.getMinBuildHeight() + 4) return false;

        BlockState stone = ModBlocks.INFESTED_STONE.defaultBlockState();
        BlockState residue = ModBlocks.ALIEN_RESIDUE.defaultBlockState();
        BlockState log = ModBlocks.INFESTED_LOG.defaultBlockState();
        BlockState leaves = ModBlocks.INFESTED_LEAVES.defaultBlockState();

        // Big corrupted ground patch.
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (dx * dx + dz * dz > 81) continue;
                int gy = surfaceY(level, o, o.getX() + dx, o.getZ() + dz) - 1;
                BlockPos g = new BlockPos(o.getX() + dx, gy, o.getZ() + dz);
                if (!StructureUtil.getBlockState(level, g).isAir()) {
                    StructureUtil.set(level, g, rng.nextInt(3) == 0 ? residue : stone);
                }
            }
        }

        // Central spire: 5x5 hollow tower, now 10 tall, with a beacon on top.
        BlockPos spireBase = new BlockPos(o.getX(), baseY, o.getZ());
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                foundation(level, spireBase.getX() + dx, spireBase.getZ() + dz, baseY - 1, stone);
        StructureUtil.fillBox(level, spireBase.offset(-2, 0, -2), spireBase.offset(2, 9, 2), stone, true);
        StructureUtil.fillBox(level, spireBase.offset(-1, 1, -1), spireBase.offset(1, 8, 1),
                Blocks.CAVE_AIR.defaultBlockState(), false);
        StructureUtil.set(level, spireBase.offset(0, 1, -2), Blocks.AIR.defaultBlockState());
        StructureUtil.set(level, spireBase.offset(0, 2, -2), Blocks.AIR.defaultBlockState());
        // beacon + crystal crown
        StructureUtil.set(level, spireBase.offset(0, 10, 0), ModBlocks.ALIEN_BEACON.defaultBlockState());
        StructureUtil.set(level, spireBase.offset(-2, 10, -2), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, spireBase.offset(2, 10, 2), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, spireBase.offset(0, 1, 0), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
        // GUARD hives + two jackpot chests inside the spire
        StructureUtil.set(level, spireBase.offset(-1, 1, 1), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.placeLootChest(level, spireBase.offset(1, 1, 1), rng, ModFeatures.CAVE_DUNGEON_LOOT);
        StructureUtil.placeLootChest(level, spireBase.offset(1, 4, 1), rng, ModFeatures.MINE_LOOT);

        // Four organic huts around the spire.
        buildHut(level, rng, o, o.getX() + 7, o.getZ() + 6, log, leaves, stone);
        buildHut(level, rng, o, o.getX() - 7, o.getZ() - 6, log, leaves, stone);
        buildHut(level, rng, o, o.getX() + 7, o.getZ() - 6, log, leaves, stone);
        buildHut(level, rng, o, o.getX() - 7, o.getZ() + 6, log, leaves, stone);

        // Spawn grunts to defend the spire
        StructureUtil.spawnGuard(level, spireBase.offset(-1, 1, -1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);
        StructureUtil.spawnGuard(level, spireBase.offset(1, 1, -1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);

        // GUARDS: scatter alien hives on the corrupted ground - they breed
        // defenders at night, so the city fights back.
        for (int i = 0; i < 6; i++) {
            int gx = o.getX() + rng.nextInt(17) - 8;
            int gz = o.getZ() + rng.nextInt(17) - 8;
            int gy = surfaceY(level, o, gx, gz);
            BlockPos hp = new BlockPos(gx, gy, gz);
            if (StructureUtil.getBlockState(level, hp).isAir() && !StructureUtil.getBlockState(level, hp.below()).isAir()) {
                StructureUtil.set(level, hp, ModBlocks.ALIEN_HIVE.defaultBlockState());
            }
        }

        // TENDRIL PERIMETER: a glowing ring of growths marks the city's edge.
        for (int i = 0; i < 28; i++) {
            double ang = i / 28.0 * Math.PI * 2.0;
            int tx = o.getX() + (int) Math.round(Math.cos(ang) * 11);
            int tz = o.getZ() + (int) Math.round(Math.sin(ang) * 11);
            int ty = surfaceY(level, o, tx, tz);
            BlockPos tp = new BlockPos(tx, ty, tz);
            if (rng.nextInt(3) != 0 && StructureUtil.getBlockState(level, tp).isAir()
                    && !StructureUtil.getBlockState(level, tp.below()).isAir()) {
                StructureUtil.set(level, tp, ModBlocks.ALIEN_TENDRILS.defaultBlockState());
            }
        }

        // LANDING PAD: a parked scout saucer on the outskirts with its own cache
        // and a ranged guard - the city now has its own airfield.
        int px = o.getX() - 11, pz = o.getZ() + 10;
        int py = surfaceY(level, o, px, pz);
        BlockPos pad = new BlockPos(px, py, pz);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 3.2) continue;
                StructureUtil.set(level, pad.offset(dx, -1, dz), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                int hgt = (int) Math.round(1.8 - d * 0.5);
                for (int dy = 0; dy <= hgt; dy++) {
                    boolean shell = dy == hgt || d > 2.2;
                    StructureUtil.set(level, pad.offset(dx, dy, dz), shell
                            ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                            : Blocks.CAVE_AIR.defaultBlockState());
                }
            }
        }
        StructureUtil.set(level, pad.above(2), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.placeLootChest(level, pad, rng, ModFeatures.ALIEN_CITY_LOOT);
        StructureUtil.spawnGuard(level, pad.offset(3, 1, 0),
                com.example.alieninvasion.registry.EntityRegistry.PLASMA_CASTER, rng);
        StructureUtil.spawnGuard(level, pad.offset(-3, 1, 1),
                com.example.alieninvasion.registry.EntityRegistry.ACID_SPITTER, rng);

        return true;
    }

    private void buildHut(WorldGenLevel level, RandomSource rng, BlockPos origin, int cx, int cz,
                          BlockState log, BlockState leaves, BlockState stone) {
        int y = surfaceY(level, origin, cx, cz);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                foundation(level, cx + dx, cz + dz, y - 1, stone);
        BlockPos base = new BlockPos(cx, y, cz);
        StructureUtil.fillBox(level, base.offset(-2, 0, -2), base.offset(2, 3, 2), log, true);
        StructureUtil.fillBox(level, base.offset(-1, 1, -1), base.offset(1, 2, 1),
                Blocks.CAVE_AIR.defaultBlockState(), false);
        // leaf roof
        StructureUtil.fillBox(level, base.offset(-2, 4, -2), base.offset(2, 4, 2), leaves, false);
        // doorway
        StructureUtil.set(level, base.offset(0, 1, -2), Blocks.AIR.defaultBlockState());
        StructureUtil.set(level, base.offset(0, 2, -2), Blocks.AIR.defaultBlockState());
        StructureUtil.placeLootChest(level, base.offset(0, 1, 1), rng, ModFeatures.CAVE_DUNGEON_LOOT);
    }
}
