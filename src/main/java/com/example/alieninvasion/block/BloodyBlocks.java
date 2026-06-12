package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BLOODSTAINED BLOCKS: heavy wounds splatter the floor — the block under the
 * victim CONVERTS to a bloody twin (stairs stay stairs, fences stay fences,
 * every property preserved). RIGHT-CLICK any bloody block to wipe it clean.
 */
public final class BloodyBlocks {
    private BloodyBlocks() {}

    /** Bloody twin for a floor block, or null if this block doesn't stain. */
    public static BlockState bloodyFor(BlockState state) {
        if (state.is(BlockTags.WOODEN_STAIRS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_STAIRS.defaultBlockState());
        }
        if (state.is(BlockTags.STAIRS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_STONE_STAIRS.defaultBlockState());
        }
        if (state.is(BlockTags.WOODEN_SLABS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_SLAB.defaultBlockState());
        }
        if (state.is(BlockTags.SLABS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_STONE_SLAB.defaultBlockState());
        }
        if (state.is(BlockTags.WOODEN_FENCES)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_FENCE.defaultBlockState());
        }
        if (state.is(BlockTags.PLANKS)) return ModBlocks.BLOODY_PLANKS.defaultBlockState();
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.DIRT_PATH)) {
            return ModBlocks.BLOODY_DIRT.defaultBlockState();
        }
        if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)) {
            return ModBlocks.BLOODY_STONE_BRICKS.defaultBlockState();
        }
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.SMOOTH_STONE) || state.is(Blocks.TUFF)) {
            return ModBlocks.BLOODY_STONE.defaultBlockState();
        }
        return null;
    }

    /** Stain the block under the given position if it has a bloody twin. */
    public static void splatter(ServerLevel level, BlockPos under) {
        BlockState floor = level.getBlockState(under);
        BlockState bloody = bloodyFor(floor);
        if (bloody != null) {
            level.setBlock(under, bloody, 2 | 16);
        }
    }

    /** Right-click wipe: bloody block reverts to its clean form. */
    public static InteractionResult wipe(Level level, BlockPos pos, BlockState state, BlockState clean) {
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, ContaminationRules.copyProperties(state, clean));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 10, 0.3D, 0.1D, 0.3D, 0.0D);
            }
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Water touching a bloody block washes it instantly. */
    private static BlockState washedByWater(BlockState state, BlockState neighbor,
                                            net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                                            BlockState clean) {
        if (neighbor.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.3F);
            return ContaminationRules.copyProperties(state, clean);
        }
        return null;
    }

    /** Full-cube bloodstained block: wipe with right-click, washes off in water. */
    public static class Plain extends net.minecraft.world.level.block.Block {
        private final java.util.function.Supplier<BlockState> clean;

        public Plain(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, state, clean.get());
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean.get());
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }
    }

    public static class Stairs extends net.minecraft.world.level.block.StairBlock {
        private final java.util.function.Supplier<BlockState> clean;

        public Stairs(BlockState base, Properties props, java.util.function.Supplier<BlockState> clean) {
            super(base, props);
            this.clean = clean;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, state, clean.get());
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean.get());
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }
    }

    public static class Slab extends net.minecraft.world.level.block.SlabBlock {
        private final java.util.function.Supplier<BlockState> clean;

        public Slab(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, state, clean.get());
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean.get());
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }
    }

    public static class Fence extends net.minecraft.world.level.block.FenceBlock {
        private final java.util.function.Supplier<BlockState> clean;

        public Fence(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, state, clean.get());
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean.get());
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }
    }
}
