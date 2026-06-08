package com.example.alieninvasion.worldgen;

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
 * Infested Mineshaft: a corrupted dig site - a wide tunnel of infested stone with
 * log supports, cobwebs, exposed cosmic-ore in the walls, guard hives and two
 * mining-themed loot chests.
 */
public class InfestedMineFeature extends Feature<NoneFeatureConfiguration> {
    public InfestedMineFeature(Codec<NoneFeatureConfiguration> codec) {
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
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        if (!isRock(level, o) || !isRock(level, o.above(3)) || !isRock(level, o.east(5)) || !isRock(level, o.west(5))) {
            return false;
        }

        var wall = ModBlocks.INFESTED_STONE.defaultBlockState();
        var air = Blocks.CAVE_AIR.defaultBlockState();
        var log = ModBlocks.INFESTED_LOG.defaultBlockState();
        var toxicWater = ModBlocks.TOXIC_WATER.defaultBlockState();
        int h = 3;

        // Long tunnel: shell + hollow interior (runs along X).
        StructureUtil.fillBox(level, o.offset(-7, -1, -2), o.offset(7, h + 1, 2), wall, false);
        StructureUtil.fillBox(level, o.offset(-6, 0, -1), o.offset(6, h, 1), air, false);

        // Log support frames every 3 blocks.
        for (int x = -5; x <= 5; x += 3) {
            for (int z = -1; z <= 1; z += 2) {
                for (int y = 0; y <= h; y++) {
                    StructureUtil.set(level, o.offset(x, y, z), y == h ? wall : log);
                }
            }
        }

        // Cobwebs and exposed cosmic ore.
        for (int i = 0; i < 10; i++) {
            BlockPos web = o.offset(rng.nextInt(13) - 6, rng.nextInt(h + 1), rng.nextInt(3) - 1);
            if (StructureUtil.getBlockState(level, web).isAir()) StructureUtil.set(level, web, Blocks.COBWEB.defaultBlockState());
        }
        for (int i = 0; i < 7; i++) {
            BlockPos ore = o.offset(rng.nextInt(15) - 7, rng.nextInt(h + 2) - 1, rng.nextBoolean() ? 2 : -2);
            if (StructureUtil.getBlockState(level, ore).is(ModBlocks.INFESTED_STONE)) {
                StructureUtil.set(level, ore, randomMineOre(rng));
            }
        }

        // Toxic pools and radiation pockets make the mine dangerous before combat.
        for (int i = 0; i < 3; i++) {
            BlockPos pool = o.offset(rng.nextInt(11) - 5, -1, rng.nextInt(3) - 1);
            StructureUtil.set(level, pool, toxicWater);
            if (rng.nextBoolean()) {
                StructureUtil.set(level, pool.north(), toxicWater);
            }
            if (rng.nextBoolean()) {
                StructureUtil.set(level, pool.east(), toxicWater);
            }
        }
        for (int i = 0; i < 4; i++) {
            BlockPos pocket = o.offset(rng.nextInt(13) - 6, rng.nextInt(h + 1), rng.nextBoolean() ? 2 : -2);
            StructureUtil.set(level, pocket, rng.nextBoolean()
                    ? ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState()
                    : ModBlocks.TOXIC_BARREL.defaultBlockState());
        }

        // Broken lab clutter and warning lamps give the shaft a readable sci-fi look.
        for (int i = 0; i < 8; i++) {
            BlockPos clutter = o.offset(rng.nextInt(13) - 6, 0, rng.nextInt(3) - 1);
            if (StructureUtil.getBlockState(level, clutter).isAir()) {
                BlockState state = switch (rng.nextInt(5)) {
                    case 0 -> ModBlocks.BROKEN_LAB_CRATE.defaultBlockState();
                    case 1 -> ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState();
                    case 2 -> ModBlocks.CONTAMINATED_BONES.defaultBlockState();
                    case 3 -> ModBlocks.WARNING_LAMP.defaultBlockState();
                    default -> Blocks.COBWEB.defaultBlockState();
                };
                StructureUtil.set(level, clutter, state);
            }
        }

        // Guard hives at the tunnel ends + central crystal light.
        StructureUtil.set(level, o.offset(-6, 0, 0), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.set(level, o.offset(6, 0, 0), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.set(level, o.offset(0, h, 0), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, o.offset(2, 0, 0), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());

        // GUARDS: grunts in the middle of the shaft.
        StructureUtil.spawnGuard(level, o.offset(0, 0, 1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);
        StructureUtil.spawnGuard(level, o.offset(0, 0, -1),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);
        if (rng.nextBoolean()) {
            StructureUtil.spawnGuard(level, o.offset(5, 0, 1),
                    com.example.alieninvasion.registry.EntityRegistry.CAVE_LURKER, rng);
        }
        if (rng.nextInt(3) == 0) {
            StructureUtil.spawnGuard(level, o.offset(-5, 0, -1),
                    com.example.alieninvasion.registry.EntityRegistry.PLASMA_CASTER, rng);
        }

        // Two loot chests.
        StructureUtil.placeLootChest(level, o.offset(-3, 0, 0), rng, ModFeatures.MINE_LOOT);
        StructureUtil.placeLootChest(level, o.offset(3, 0, 0), rng, ModFeatures.MINE_LOOT);
        return true;
    }

    private static BlockState randomMineOre(RandomSource rng) {
        return switch (rng.nextInt(10)) {
            case 0 -> ModBlocks.DARK_MATTER_ORE.defaultBlockState();
            case 1 -> ModBlocks.IRIDIUM_ORE.defaultBlockState();
            case 2 -> ModBlocks.PLASMA_ORE.defaultBlockState();
            case 3, 4 -> ModBlocks.XENOCRYSTAL_ORE.defaultBlockState();
            case 5, 6 -> ModBlocks.URANIUM_ORE.defaultBlockState();
            case 7, 8 -> ModBlocks.BIO_VEIN_ORE.defaultBlockState();
            default -> ModBlocks.COSMIC_ORE.defaultBlockState();
        };
    }
}
