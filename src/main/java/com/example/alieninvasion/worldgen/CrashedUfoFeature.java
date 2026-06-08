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
 * Crashed UFO: a downed saucer half-buried in a scorched impact crater. A metal
 * dome of iron/cosmic plating over an infested-stone crater, glowing crystals,
 * and a loot chest in the wreck.
 */
public class CrashedUfoFeature extends Feature<NoneFeatureConfiguration> {
    public CrashedUfoFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        int baseY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (baseY <= level.getMinBuildHeight() + 4) return false;
        BlockPos c = new BlockPos(o.getX(), baseY - 1, o.getZ());

        // Wreck is built from decorative dark plating, NOT iron/cosmic BLOCKS - a
        // crashed saucer should not hand the player free stacks of valuable metal
        // the moment they find one. The reward is the loot chest + crystal core.
        BlockState crater = ModBlocks.INFESTED_STONE.defaultBlockState();
        BlockState hull = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState plate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();

        int R = 5;
        // Scorched bowl crater.
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R) continue;
                int depth = (int) (2 - d * 0.4);
                for (int dy = 0; dy <= depth; dy++) {
                    StructureUtil.set(level, c.offset(dx, -dy, dz), crater);
                }
                // clear a little air above the crater lip
                StructureUtil.set(level, c.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
            }
        }

        // Saucer dome on the crater floor.
        int domeR = 4;
        BlockPos hub = c.offset(0, 0, 0);
        for (int dx = -domeR; dx <= domeR; dx++) {
            for (int dz = -domeR; dz <= domeR; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > domeR) continue;
                int hgt = (int) Math.round(2.2 - d * 0.45);
                for (int dy = 0; dy <= hgt; dy++) {
                    boolean shell = dy == hgt || d > domeR - 1;
                    if (shell) {
                        StructureUtil.set(level, hub.offset(dx, dy, dz), (dx + dz) % 2 == 0 ? hull : plate);
                    } else {
                        StructureUtil.set(level, hub.offset(dx, dy, dz), Blocks.CAVE_AIR.defaultBlockState());
                    }
                }
            }
        }

        // Glowing innards + loot.
        StructureUtil.set(level, hub.offset(0, 1, 0), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, hub.offset(0, 0, 1), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
        StructureUtil.set(level, hub.offset(-2, 0, 0), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.placeLootChest(level, hub.offset(1, 0, 1), rng, ModFeatures.ALIEN_CITY_LOOT);
        return true;
    }
}
