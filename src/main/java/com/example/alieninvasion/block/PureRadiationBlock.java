package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PureRadiationBlock extends Block {
    public PureRadiationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (player.isCreative()) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        // Mining speed is extremely slow if not using Nibirium Pickaxe (about 30x slower, like mining obsidian with a fist)
        return super.getDestroyProgress(state, player, world, pos) * 0.03F;
    }
}
