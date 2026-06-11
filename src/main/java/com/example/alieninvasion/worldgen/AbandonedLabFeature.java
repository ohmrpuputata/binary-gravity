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
 * Abandoned Lab: a buried, decayed research bunker. Deepslate-tile walls, a toxic
 * coolant spill, leaking pipes and barrels (radiation sources), warning lamps,
 * broken crates and the bones of the staff that didn't make it out. Guarded by
 * infested undead. Mid-tier loot - mining components and survival gear, not raw
 * piles of metal. Visually and thematically distinct from the organic dungeons.
 */
public class AbandonedLabFeature extends Feature<NoneFeatureConfiguration> {
    public AbandonedLabFeature(Codec<NoneFeatureConfiguration> codec) {
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
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();

        // Must be genuinely embedded in rock so the bunker doesn't hang in a cave.
        if (!isRock(level, o.above(4)) || !isRock(level, o.below())
                || !isRock(level, o.east(5)) || !isRock(level, o.west(5))
                || !isRock(level, o.north(5)) || !isRock(level, o.south(5))) {
            return false;
        }

        int rx = 4, rz = 4, ry = 4;
        BlockState wall = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        StructureUtil.fillBox(level, o.offset(-rx - 1, -1, -rz - 1), o.offset(rx + 1, ry + 1, rz + 1), wall, false);
        StructureUtil.fillBox(level, o.offset(-rx, 0, -rz), o.offset(rx, ry, rz), air, false);
        // Polished floor.
        StructureUtil.fillBox(level, o.offset(-rx, -1, -rz), o.offset(rx, -1, rz), floor, false);

        // Toxic coolant spill in one corner (a radiation/poison hazard).
        for (int dx = -rx; dx <= -rx + 2; dx++) {
            for (int dz = -rz; dz <= -rz + 2; dz++) {
                StructureUtil.set(level, o.offset(dx, -1, dz), ModBlocks.TOXIC_WATER.defaultBlockState());
            }
        }

        // Leaking barrels + pipes along the back wall.
        StructureUtil.set(level, o.offset(rx - 1, 0, -rz + 1), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.set(level, o.offset(rx - 1, 0, -rz + 2), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.set(level, o.offset(rx, 1, 0), ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState());
        StructureUtil.set(level, o.offset(rx, 2, 0), ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState());
        StructureUtil.set(level, o.offset(-rx, 1, rz - 1), ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState());

        // Warning lamps for that flickering-emergency-lighting feel.
        StructureUtil.set(level, o.offset(rx, ry, rz), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, o.offset(-rx, ry, -rz), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, o.offset(0, ry, 0), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, o.offset(-rx + 1, 0, -rz + 1), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());

        // Broken crates + the remains of the staff.
        StructureUtil.set(level, o.offset(-rx + 1, 0, rz - 1), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, o.offset(-rx + 2, 0, rz - 1), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            int dx = rng.nextInt(rx * 2 + 1) - rx;
            int dz = rng.nextInt(rz * 2 + 1) - rz;
            if (StructureUtil.getBlockState(level, o.offset(dx, 0, dz)).isAir()) {
                StructureUtil.set(level, o.offset(dx, 0, dz),
                        rng.nextBoolean() ? ModBlocks.CONTAMINATED_BONES.defaultBlockState()
                                : ModBlocks.ALIEN_RESIDUE.defaultBlockState());
            }
        }

        // A broken containment cell (iron bars) around a research workstation.
        StructureUtil.set(level, o.offset(2, 0, 2), Blocks.IRON_BARS.defaultBlockState());
        StructureUtil.set(level, o.offset(3, 0, 2), Blocks.IRON_BARS.defaultBlockState());
        StructureUtil.set(level, o.offset(2, 0, 3), Blocks.IRON_BARS.defaultBlockState());

        // Guards: infested staff. Undead, so they prowl the bunker.
        StructureUtil.spawnGuard(level, o.offset(1, 0, -1), EntityRegistry.INFESTED_SKELETON, rng);
        StructureUtil.spawnGuard(level, o.offset(-1, 0, 1), EntityRegistry.INFESTED_ZOMBIE, rng);

        // Two loot caches.
        StructureUtil.placeLootChest(level, o.offset(0, 0, rz - 1), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.placeLootChest(level, o.offset(rx - 1, 0, rz - 1), rng, ModFeatures.ABANDONED_LAB_LOOT);

        // REACTOR CLOSET: the breached power room - a humming radiation core behind
        // bars, warning lamps, and the experiment that walked out of its cell.
        BlockPos reactor = o.offset(-rx + 1, 0, -rz + 1);
        StructureUtil.set(level, reactor, ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
        StructureUtil.set(level, reactor.above(), ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState());
        StructureUtil.set(level, reactor.east(), Blocks.IRON_BARS.defaultBlockState());
        StructureUtil.set(level, reactor.south(), Blocks.IRON_BARS.defaultBlockState());
        StructureUtil.set(level, reactor.offset(2, 1, 0), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.spawnGuard(level, o.offset(2, 0, -2), EntityRegistry.INFESTED_PLAYER_CLONE, rng);
        return true;
    }
}
