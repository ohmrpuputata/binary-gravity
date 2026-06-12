package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Hive Nest: a deep, large infested cavern - the swarm's breeding pit. A dome of
 * infested deepslate riddled with hive nodes (which spawn defenders), radiation
 * crystal clusters lighting the gloom, a residue floor, and a mini-boss (an Alien
 * Brute) flanked by shamans and grunts on a raised dais. The richest, most
 * dangerous underground find - and very rare.
 */
public class HiveNestFeature extends Feature<NoneFeatureConfiguration> {
    public HiveNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean isRock(WorldGenLevel level, BlockPos p) {
        if (level.isOutsideBuildHeight(p) || !StructureUtil.hasChunk(level, p)) {
            return false;
        }
        var s = StructureUtil.getBlockState(level, p);
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(ModBlocks.INFESTED_STONE)
                || s.is(ModBlocks.INFESTED_DEEPSLATE);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        // RECON PHASE: alien structures only appear once the invasion has landed.
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level.getLevel()) < 2) return false;
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();

        // Needs a big solid pocket of rock to carve into.
        if (!isRock(level, o.above(6)) || !isRock(level, o.below(2))
                || !isRock(level, o.east(7)) || !isRock(level, o.west(7))
                || !isRock(level, o.north(7)) || !isRock(level, o.south(7))) {
            return false;
        }

        int r = 6;
        BlockState shell = ModBlocks.INFESTED_DEEPSLATE.defaultBlockState();
        BlockState residue = ModBlocks.ALIEN_RESIDUE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        // Carve a rough spherical cavern with an infested-deepslate shell.
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dy = -r - 1; dy <= r + 1; dy++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.2 + dz * dz);
                    m.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
                    if (d <= r - 0.5) {
                        StructureUtil.set(level, m, air);
                    } else if (d <= r + 0.6) {
                        StructureUtil.set(level, m, shell);
                    }
                }
            }
        }

        // Residue floor across the bottom of the cavern.
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int fy = -(int) Math.sqrt(Math.max(0, r * r - dx * dx - dz * dz)) + 1;
                StructureUtil.set(level, o.offset(dx, fy, dz), residue);
            }
        }

        // Hive nodes around the walls (defenders) + radiation crystal lighting.
        for (int i = 0; i < 10; i++) {
            double ang = i / 10.0 * Math.PI * 2.0;
            int hx = (int) Math.round(Math.cos(ang) * (r - 1));
            int hz = (int) Math.round(Math.sin(ang) * (r - 1));
            int hy = rng.nextInt(3) - 1;
            BlockPos hp = o.offset(hx, hy, hz);
            if (!StructureUtil.getBlockState(level, hp).isAir()) continue;
            StructureUtil.set(level, hp, i % 3 == 0
                    ? ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState()
                    : ModBlocks.ALIEN_HIVE.defaultBlockState());
        }
        StructureUtil.set(level, o.above(r - 1), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());

        // Central dais with the mini-boss and its retinue.
        int baseY = -(int) Math.sqrt(Math.max(0, r * r)) + 1; // floor at center
        BlockPos dais = o.offset(0, baseY, 0);
        StructureUtil.fillBox(level, dais.offset(-2, 0, -2), dais.offset(2, 0, 2), shell, false);
        StructureUtil.set(level, dais.offset(0, 1, 0), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.set(level, dais.offset(2, 1, -2), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());

        StructureUtil.spawnGuard(level, dais.offset(0, 1, 1), EntityRegistry.ALIEN_BRUTE, rng);   // mini-boss
        StructureUtil.spawnGuard(level, dais.offset(2, 1, 0), EntityRegistry.HIVE_SHAMAN, rng);
        StructureUtil.spawnGuard(level, dais.offset(-2, 1, 0), EntityRegistry.ALIEN_GRUNT, rng);
        StructureUtil.spawnGuard(level, dais.offset(0, 1, -2), EntityRegistry.ALIEN_GRUNT, rng);

        // Two high-value caches flanking the dais.
        StructureUtil.placeLootChest(level, dais.offset(1, 1, 1), rng, ModFeatures.HIVE_NEST_LOOT);
        StructureUtil.placeLootChest(level, dais.offset(-1, 1, -1), rng, ModFeatures.HIVE_NEST_LOOT);

        // BREEDING POOL + GROWTH: an infected-water birthing pool and glowing
        // tendrils across the floor make the pit feel alive, with one spitter
        // lurking at the waterline.
        StructureUtil.fillBox(level, dais.offset(3, 0, 3), dais.offset(4, 0, 4),
                ModBlocks.INFECTED_WATER.defaultBlockState(), false);
        for (int i = 0; i < 8; i++) {
            BlockPos tp = o.offset(rng.nextInt(9) - 4, baseY + 1, rng.nextInt(9) - 4);
            if (StructureUtil.getBlockState(level, tp).isAir()
                    && !StructureUtil.getBlockState(level, tp.below()).isAir()) {
                StructureUtil.set(level, tp, ModBlocks.ALIEN_TENDRILS.defaultBlockState());
            }
        }
        StructureUtil.spawnGuard(level, dais.offset(3, 1, 2), EntityRegistry.ACID_SPITTER, rng);
        return true;
    }
}
