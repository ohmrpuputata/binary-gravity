package com.example.alieninvasion.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;

/** Shared helpers for the procedural structure features. */
public final class StructureUtil {
    private StructureUtil() {}

    /** Check if chunk is loaded/accessible in the level. */
    public static boolean hasChunk(WorldGenLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Safe block state query. */
    public static BlockState getBlockState(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !hasChunk(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return level.getBlockState(pos);
    }

    /** Safe single block set (respects build height and chunk loading bounds). */
    public static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.isOutsideBuildHeight(pos) && hasChunk(level, pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    /** Fill an inclusive box. If hollow, only the shell is placed. */
    public static void fillBox(WorldGenLevel level, BlockPos a, BlockPos b, BlockState state, boolean hollow) {
        int x0 = Math.min(a.getX(), b.getX()), x1 = Math.max(a.getX(), b.getX());
        int y0 = Math.min(a.getY(), b.getY()), y1 = Math.max(a.getY(), b.getY());
        int z0 = Math.min(a.getZ(), b.getZ()), z1 = Math.max(a.getZ(), b.getZ());
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    if (hollow && !shell) continue;
                    m.set(x, y, z);
                    set(level, m, state);
                }
            }
        }
    }

    /** Place a loot chest wired to one of our loot tables. */
    public static void placeLootChest(WorldGenLevel level, BlockPos pos, RandomSource rng, ResourceKey<LootTable> table) {
        if (level.isOutsideBuildHeight(pos) || !hasChunk(level, pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity rc) {
            rc.setLootTable(table, rng.nextLong());
        }
    }

    /** Place both halves of a bed with a valid facing and clear headroom. */
    public static void placeBed(WorldGenLevel level, BlockPos foot, Block bed, Direction facing) {
        BlockPos head = foot.relative(facing);
        set(level, foot.above(), Blocks.CAVE_AIR.defaultBlockState());
        set(level, head.above(), Blocks.CAVE_AIR.defaultBlockState());
        set(level, foot, bed.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.FOOT));
        set(level, head, bed.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** Spawn a dungeon guard (mob) that does not despawn. */
    public static void spawnGuard(WorldGenLevel level, BlockPos pos, EntityType<?> type, RandomSource rng) {
        if (level.isOutsideBuildHeight(pos) || !hasChunk(level, pos)) return;
        Mob mob = (Mob) type.create(level.getLevel());
        if (mob != null) {
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, rng.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
    }
}
