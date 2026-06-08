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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;

import java.util.Optional;

/**
 * Infected water - looks corrupted (greenish, animated, spore haze) but behaves
 * EXACTLY like vanilla water (it extends WaterFluid): you swim in it, it flows the
 * same, and it does NOT harm you (unlike toxic water). Purely a flavour fluid for
 * the corrupted world.
 */
public abstract class InfectedWaterFluid extends WaterFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.INFECTED_WATER_FLOWING;
    }

    @Override
    public Fluid getSource() {
        return ModFluids.INFECTED_WATER_STILL;
    }

    @Override
    public Item getBucket() {
        return ItemRegistry.INFECTED_WATER_BUCKET;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(),
                    pos.getY() + 0.85D, pos.getZ() + random.nextDouble(), 0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_WATER;
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.INFECTED_WATER.defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.INFECTED_WATER_STILL || fluid == ModFluids.INFECTED_WATER_FLOWING;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    public int getDropOff(LevelReader level) {
        return 1; // same as vanilla water
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    public static class Flowing extends InfectedWaterFluid {
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

    public static class Source extends InfectedWaterFluid {
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
