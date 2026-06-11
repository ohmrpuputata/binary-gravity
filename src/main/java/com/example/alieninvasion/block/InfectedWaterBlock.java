package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Liquid block for {@link com.example.alieninvasion.fluid.InfectedWaterFluid}.
 * Behaves like ordinary water but counts as alien ground (standing in it fills
 * the infection meter — see ContaminationRules.isContaminated) and slowly
 * creeps into adjacent clean shallow water, so ponds rot from the edge inward.
 */
public class InfectedWaterBlock extends LiquidBlock {
    public InfectedWaterBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // DOWNWARD ROT: the infection always sinks. The clean water under the
        // surface film converts column by column, no shallow limit - dive into an
        // infected lake a few days later and it's infected all the way down.
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.WATER) && below.getFluidState().isSource()) {
            level.setBlockAndUpdate(pos.below(), ModBlocks.INFECTED_WATER.defaultBlockState());
            return;
        }
        if (random.nextInt(2) != 0) return;
        Direction dir = Direction.getRandom(random);
        if (dir == Direction.UP) dir = Direction.DOWN;
        BlockPos target = pos.relative(dir);
        BlockState targetState = level.getBlockState(target);
        if (targetState.is(Blocks.WATER) && targetState.getFluidState().isSource()
                && ToxicWaterBlock.isShallow(level, target)) {
            level.setBlockAndUpdate(target, ModBlocks.INFECTED_WATER.defaultBlockState());
        }
    }
}
