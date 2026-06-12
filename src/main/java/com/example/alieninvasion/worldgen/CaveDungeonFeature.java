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
 * Cave Dungeon: a buried infested vault carved straight into the bedrock of the
 * caves. Walls of infested stone, glowing alien hives (which spawn defenders at
 * night), eerie cosmic-crystal light, and a loot chest in the middle.
 */
public class CaveDungeonFeature extends Feature<NoneFeatureConfiguration> {
    public CaveDungeonFeature(Codec<NoneFeatureConfiguration> codec) {
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

        // Only carve when genuinely embedded in rock (not in an open cave / water).
        if (!isRock(level, o) || !isRock(level, o.above(3)) || !isRock(level, o.below())
                || !isRock(level, o.east(3)) || !isRock(level, o.west(3))) {
            return false;
        }

        int rx = 3, rz = 3, ry = 3;
        var wall = ModBlocks.INFESTED_STONE.defaultBlockState();
        var air = Blocks.CAVE_AIR.defaultBlockState();

        // Shell of infested stone, hollow interior.
        StructureUtil.fillBox(level, o.offset(-rx - 1, -1, -rz - 1), o.offset(rx + 1, ry + 1, rz + 1), wall, false);
        StructureUtil.fillBox(level, o.offset(-rx, 0, -rz), o.offset(rx, ry, rz), air, false);

        // Hive nodes in two corners (light + night-time defenders).
        StructureUtil.set(level, o.offset(rx, 0, rz), ModBlocks.ALIEN_HIVE.defaultBlockState());
        StructureUtil.set(level, o.offset(-rx, 0, -rz), ModBlocks.ALIEN_HIVE.defaultBlockState());

        // Residue splatter on the floor.
        for (int i = 0; i < 6; i++) {
            int dx = rng.nextInt(rx * 2 + 1) - rx;
            int dz = rng.nextInt(rz * 2 + 1) - rz;
            StructureUtil.set(level, o.offset(dx, -1, dz), ModBlocks.ALIEN_RESIDUE.defaultBlockState());
        }

        // Cosmic crystal hanging from the ceiling for eerie glow.
        StructureUtil.set(level, o.above(ry), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        // A node of pure radiation makes the vault a hazard, not just a fight.
        StructureUtil.set(level, o.offset(-rx, 0, rz), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());

        // GUARDS: grunts keep the vault dangerous day and night.
        StructureUtil.spawnGuard(level, o.offset(2, 0, -2),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);
        StructureUtil.spawnGuard(level, o.offset(-2, 0, 2),
                com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, rng);

        // The prize: a loot chest on the floor.
        StructureUtil.placeLootChest(level, o, rng, ModFeatures.CAVE_DUNGEON_LOOT);

        // INNER SANCTUM: a second sealed chamber behind a tendril-choked corridor,
        // holding the real prize under a shaman's watch. Turns the vault from a
        // single box into a small two-room crawl.
        BlockPos corridor = o.offset(rx + 1, 0, 0);
        StructureUtil.fillBox(level, corridor.offset(0, -1, -1), corridor.offset(4, 3, 1), wall, false);
        StructureUtil.fillBox(level, corridor.offset(0, 0, 0), corridor.offset(4, 2, 0), air, false);
        if (rng.nextBoolean()) {
            StructureUtil.set(level, corridor.offset(2, 0, 0), ModBlocks.ALIEN_TENDRILS.defaultBlockState());
        }
        BlockPos sanctum = o.offset(rx + 8, 0, 0);
        StructureUtil.fillBox(level, sanctum.offset(-3, -1, -3), sanctum.offset(3, 4, 3), wall, false);
        StructureUtil.fillBox(level, sanctum.offset(-2, 0, -2), sanctum.offset(2, 3, 2), air, false);
        StructureUtil.set(level, sanctum.above(3), ModBlocks.COSMIC_CRYSTAL.defaultBlockState());
        StructureUtil.set(level, sanctum.offset(-2, 0, 2), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
        StructureUtil.set(level, sanctum.offset(2, -1, -2), ModBlocks.TOXIC_WATER.defaultBlockState());
        StructureUtil.placeLootChest(level, sanctum, rng, ModFeatures.CAVE_DUNGEON_LOOT);
        StructureUtil.spawnGuard(level, sanctum.offset(1, 0, 1),
                com.example.alieninvasion.registry.EntityRegistry.HIVE_SHAMAN, rng);
        return true;
    }
}
