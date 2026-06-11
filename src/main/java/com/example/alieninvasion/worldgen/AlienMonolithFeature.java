package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.logic.ContaminationRules;
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
 * ALIEN MONOLITH: a rare black obelisk humming over a ring of corrupted ground —
 * visible from afar by its crystal beacon glow. Beneath it hides a sealed vault
 * (one telekinetic warden, crystal light, a single rich cache) reached through a
 * cracked stair shaft on the south side.
 */
public class AlienMonolithFeature extends Feature<NoneFeatureConfiguration> {
    public AlienMonolithFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        int baseY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (baseY <= level.getMinBuildHeight() + 12) return false;
        BlockPos base = new BlockPos(o.getX(), baseY, o.getZ());
        if (!StructureUtil.getBlockState(level, base.below()).getFluidState().isEmpty()) return false;

        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();

        // The obelisk: 3x3, 11 tall, veined with crying obsidian, crystal capstone.
        for (int dy = 0; dy < 11; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    StructureUtil.set(level, base.offset(dx, dy, dz),
                            rng.nextInt(5) == 0 ? crying : obsidian);
                }
            }
        }
        StructureUtil.set(level, base.above(11), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());

        // Corrupted ground ring + tendrils: the monolith poisons the earth around it.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 17) continue;
                int gy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, base.getX() + dx, base.getZ() + dz) - 1;
                BlockPos g = new BlockPos(base.getX() + dx, gy, base.getZ() + dz);
                BlockState gs = StructureUtil.getBlockState(level, g);
                BlockState conv = ContaminationRules.contaminatedStateFor(gs);
                if (conv != null) StructureUtil.set(level, g, conv);
                if (rng.nextInt(6) == 0 && StructureUtil.getBlockState(level, g.above()).isAir()) {
                    StructureUtil.set(level, g.above(), ModBlocks.ALIEN_TENDRILS.defaultBlockState());
                }
            }
        }

        // The vault: a sealed chamber under the obelisk.
        BlockPos vault = base.below(6);
        StructureUtil.fillBox(level, vault.offset(-3, -1, -3), vault.offset(3, 4, 3),
                Blocks.DEEPSLATE_TILES.defaultBlockState(), false);
        StructureUtil.fillBox(level, vault.offset(-2, 0, -2), vault.offset(2, 3, 2),
                Blocks.CAVE_AIR.defaultBlockState(), false);
        StructureUtil.set(level, vault.offset(-2, 1, -2), ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState());
        StructureUtil.set(level, vault.offset(2, 1, 2), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.placeLootChest(level, vault, rng, ModFeatures.COSMIC_VAULT_LOOT);
        StructureUtil.spawnGuard(level, vault.offset(1, 0, -1), EntityRegistry.TELEKINETIC_ALIEN, rng);

        // Cracked stair shaft on the south face down into the vault.
        for (int i = 0; i < 6; i++) {
            BlockPos step = base.offset(0, -i, 2 + Math.min(i, 1));
            StructureUtil.set(level, step, Blocks.CAVE_AIR.defaultBlockState());
            StructureUtil.set(level, step.above(), Blocks.CAVE_AIR.defaultBlockState());
        }
        StructureUtil.set(level, base.offset(0, 0, 3), ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState());

        // Register for the Radio Transmitter so players can hunt the signal.
        com.example.alieninvasion.logic.StructureLocationsData.get(level.getLevel()).add("monolith", base);
        return true;
    }
}
