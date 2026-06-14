package com.example.alieninvasion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * СТОЙКАЯ кровавая декаль поверх ЛЮБОГО блока. В отличие от {@link BloodPoolBlock}
 * (след, который сам высыхает) — эта лужа держится, пока её не сотрут ПКМ или не
 * смоет вода. Кладётся в воздух НАД полом, поэтому сам блок-пол не меняется:
 * так кровь ложится на что угодно (листва, стекло, модовые блоки), не теряя их.
 */
public class BloodLayerBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public BloodLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Лежит на ЛЮБОМ непустом блоке (не только sturdy) — чтобы кровь была и на
        // листве/стекле/модовых блоках, а не только на полных кубах.
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        // Вода смывает кровь.
        if (neighbor.getFluidState().is(FluidTags.WATER) || state.getFluidState().is(FluidTags.WATER)) {
            level.scheduleTick(pos, this, 20);
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean touchingWater = state.getFluidState().is(FluidTags.WATER);
        for (Direction direction : Direction.values()) {
            touchingWater |= level.getFluidState(pos.relative(direction)).is(FluidTags.WATER);
        }
        if (!touchingWater) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D,
                3, 0.28D, 0.04D, 0.28D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.35F, 1.45F);
        } else {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                        8, 0.3D, 0.05D, 0.3D, 0.0D);
            }
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
