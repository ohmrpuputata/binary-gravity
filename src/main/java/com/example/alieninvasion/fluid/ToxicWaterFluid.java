package com.example.alieninvasion.fluid;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;

import java.util.Optional;

public abstract class ToxicWaterFluid extends WaterFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.TOXIC_WATER_FLOWING;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.TOXIC_WATER_STILL;
    }

    @Override
    public Item getBucket() {
        return ItemRegistry.TOXIC_WATER_BUCKET;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(),
                    pos.getY() + 0.85D, pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
        if (random.nextInt(18) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.2F, 0.6F + random.nextFloat() * 0.4F, false);
        }
    }

    @Override
    public ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_HONEY;
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.isAir()) {
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, pos,
                    net.minecraft.world.level.block.Block.getId(state));
        }
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.TOXIC_WATER.defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.TOXIC_WATER_STILL || fluid == ModFluids.TOXIC_WATER_FLOWING;
    }

    @Override
    public int getDropOff(LevelReader level) {
        return 2;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    public static class Flowing extends ToxicWaterFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends ToxicWaterFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
