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
 * FIELD HOSPITAL: looks like salvation — white tents, cots, a medicine chest —
 * but the evacuation failed. The "patients" got back up. A baited dangerous
 * dungeon: great meds guarded by infested staff and a corpse-runner.
 */
public class FieldHospitalFeature extends Feature<NoneFeatureConfiguration> {
    public FieldHospitalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (y <= level.getMinBuildHeight() + 4) return false;
        BlockPos base = new BlockPos(o.getX(), y, o.getZ());
        if (!StructureUtil.getBlockState(level, base.below()).getFluidState().isEmpty()) return false;

        BlockState tent = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        // Two wool tents (A-frames) + a supply pile between them.
        for (int t = -1; t <= 1; t += 2) {
            BlockPos c = base.offset(t * 5, 0, 0);
            StructureUtil.fillBox(level, c.offset(-2, 0, -3), c.offset(2, 0, 3), tent, false);
            StructureUtil.fillBox(level, c.offset(-2, 1, -3), c.offset(2, 2, 3), tent, true);
            StructureUtil.fillBox(level, c.offset(-1, 0, -3), c.offset(1, 1, 3), air, false);
            StructureUtil.fillBox(level, c.offset(-1, 3, -3), c.offset(1, 3, 3), tent, false);
            // Cots and a bloody floor - something went very wrong here.
            StructureUtil.set(level, c.offset(-1, 1, -2), Blocks.WHITE_BED.defaultBlockState());
            StructureUtil.set(level, c.offset(1, 1, 1), Blocks.WHITE_BED.defaultBlockState());
            StructureUtil.set(level, c.offset(0, 0, 0), ModBlocks.BLOODY_PLANKS.defaultBlockState());
            StructureUtil.set(level, c.offset(t, 0, -1), ModBlocks.BLOODY_DIRT.defaultBlockState());
        }
        // Supply drop between tents: the prize.
        StructureUtil.set(level, base.offset(0, 0, 2), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, base.offset(0, 0, -2), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.placeLootChest(level, base.offset(0, 0, 0), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.placeLootChest(level, base.offset(1, 0, 0), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.set(level, base.offset(0, 1, 2), ModBlocks.WARNING_LAMP.defaultBlockState());

        // The patients never left.
        StructureUtil.spawnGuard(level, base.offset(-5, 1, 0), EntityRegistry.INFESTED_ZOMBIE, rng);
        StructureUtil.spawnGuard(level, base.offset(5, 1, 0), EntityRegistry.INFESTED_ZOMBIE, rng);
        StructureUtil.spawnGuard(level, base.offset(0, 1, 3), EntityRegistry.INFESTED_PLAYER_CLONE, rng);
        StructureUtil.spawnGuard(level, base.offset(2, 1, -3), EntityRegistry.INFESTED_WORM, rng);
        return true;
    }
}
