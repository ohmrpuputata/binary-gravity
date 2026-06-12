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
 * SAFEHOUSE: a rare PEACEFUL find — a boarded-up survivor cabin abandoned in a
 * hurry. No monsters inside; beds, a furnace, a crafting corner and two supply
 * chests. A breath of safety in the apocalypse (built before the invasion, so
 * it generates from day 0).
 */
public class SafehouseFeature extends Feature<NoneFeatureConfiguration> {
    public SafehouseFeature(Codec<NoneFeatureConfiguration> codec) {
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

        BlockState wall = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        // 7x4x6 cabin, hollow, plank floor, cobble footing.
        StructureUtil.fillBox(level, base.offset(-3, -1, -3), base.offset(3, -1, 2),
                Blocks.COBBLESTONE.defaultBlockState(), false);
        StructureUtil.fillBox(level, base.offset(-3, 0, -3), base.offset(3, 3, 2), wall, true);
        StructureUtil.fillBox(level, base.offset(-2, 0, -2), base.offset(2, 2, 1), air, false);
        StructureUtil.fillBox(level, base.offset(-3, 0, -3), base.offset(3, 0, 2), floor, false);
        // Boarded windows + a door gap.
        StructureUtil.set(level, base.offset(0, 1, -3), Blocks.OAK_DOOR.defaultBlockState());
        StructureUtil.set(level, base.offset(0, 2, -3), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        StructureUtil.set(level, base.offset(-2, 2, -3), Blocks.OAK_TRAPDOOR.defaultBlockState());
        StructureUtil.set(level, base.offset(2, 2, -3), Blocks.OAK_TRAPDOOR.defaultBlockState());

        // Interior: beds, furnace, crafting table, lantern, supplies.
        StructureUtil.set(level, base.offset(-2, 1, 1), Blocks.RED_BED.defaultBlockState());
        StructureUtil.set(level, base.offset(2, 1, 1), Blocks.FURNACE.defaultBlockState());
        StructureUtil.set(level, base.offset(2, 1, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
        StructureUtil.set(level, base.offset(0, 3, 0), Blocks.LANTERN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        StructureUtil.placeLootChest(level, base.offset(-2, 1, -1), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.placeLootChest(level, base.offset(1, 1, 1), rng, ModFeatures.CAVE_DUNGEON_LOOT);
        StructureUtil.set(level, base.offset(-1, 1, 1), ModBlocks.WARNING_LAMP.defaultBlockState());
        return true;
    }
}
