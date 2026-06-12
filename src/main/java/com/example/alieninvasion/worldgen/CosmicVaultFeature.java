package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Cosmic Vault: a rare, deep, fortified chamber walled in Cosmic Blocks, with a
 * cosmic-ore floor, crystal lighting, guard hives and a single jackpot chest -
 * the best loot in the world, deep down and well defended.
 */
public class CosmicVaultFeature extends Feature<NoneFeatureConfiguration> {
    public CosmicVaultFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean isRock(WorldGenLevel level, BlockPos p) {
        if (level.isOutsideBuildHeight(p) || !StructureUtil.hasChunk(level, p)) {
            return false;
        }
        var s = StructureUtil.getBlockState(level, p);
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(ModBlocks.INFESTED_STONE);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        // RECON PHASE: alien structures only appear once the invasion has landed.
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level.getLevel()) < 2) return false;
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        if (!isRock(level, o) || !isRock(level, o.above(4)) || !isRock(level, o.below())
                || !isRock(level, o.north(4)) || !isRock(level, o.south(4))) {
            return false;
        }

        int r = 3;
        var hull = ModBlocks.INFESTED_DEEPSLATE.defaultBlockState();
        var air = Blocks.CAVE_AIR.defaultBlockState();

        // Cosmic-block shell, hollow interior.
        StructureUtil.fillBox(level, o.offset(-r - 1, -1, -r - 1), o.offset(r + 1, r + 2, r + 1), hull, false);
        StructureUtil.fillBox(level, o.offset(-r, 0, -r), o.offset(r, r, r), air, false);

        // Cosmic-ore floor seam.
        for (int i = 0; i < 9; i++) {
            BlockPos ore = o.offset(rng.nextInt(r * 2 + 1) - r, -1, rng.nextInt(r * 2 + 1) - r);
            StructureUtil.set(level, ore, ModBlocks.PLATINUM_ORE.defaultBlockState());
        }

        // Crystal lighting in the ceiling corners.
        StructureUtil.set(level, o.offset(-r, r, -r), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, o.offset(r, r, r), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, o.offset(-r + 1, 0, r - 1), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());

        // HEAVY GUARDS: a brute spawner + hives - the vault is well defended.
        StructureUtil.set(level, o.offset(-r, 0, r), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.set(level, o.offset(r, 0, -r), ModBlocks.ALIEN_HIVE.defaultBlockState());
        // Spawn brutes to defend the vault
        StructureUtil.spawnGuard(level, o.offset(-r + 1, 0, -r + 1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_BRUTE, rng);
        StructureUtil.spawnGuard(level, o.offset(r - 1, 0, r - 1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_BRUTE, rng);

        // TOXIC MOAT + WARDEN: the jackpot is ringed by a toxic trench and tendril
        // snares, watched by a telekinetic - earning it is a fight AND a hazard.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 == 0) continue;
                if (d2 <= 2) {
                    StructureUtil.set(level, o.offset(dx, -1, dz), ModBlocks.TOXIC_WATER.defaultBlockState());
                } else if (rng.nextInt(3) == 0
                        && StructureUtil.getBlockState(level, o.offset(dx, 0, dz)).isAir()) {
                    StructureUtil.set(level, o.offset(dx, 0, dz), ModBlocks.ALIEN_TENDRILS.defaultBlockState());
                }
            }
        }
        StructureUtil.set(level, o.below(), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.spawnGuard(level, o.offset(0, 0, -r + 2),
                com.example.alieninvasion.registry.EntityRegistry.TELEKINETIC_ALIEN, rng);
        StructureUtil.placeLootChest(level, o, rng, ModFeatures.COSMIC_VAULT_LOOT);
        return true;
    }
}
