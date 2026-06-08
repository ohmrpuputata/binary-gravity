package com.example.alieninvasion.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Liquid block for {@link com.example.alieninvasion.fluid.InfectedWaterFluid}.
 * Harmless on purpose - infected water behaves like ordinary water; only its look
 * is corrupted. (Subclasses LiquidBlock so we don't need an access widener.)
 */
public class InfectedWaterBlock extends LiquidBlock {
    public InfectedWaterBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
