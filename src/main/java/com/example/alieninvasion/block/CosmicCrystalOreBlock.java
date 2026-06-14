package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CosmicCrystalOreBlock extends Block {
    public CosmicCrystalOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return checkMiningSpeed(state, player, world, pos, () -> super.getDestroyProgress(state, player, world, pos));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @org.jetbrains.annotations.Nullable BlockEntity blockEntity, ItemStack tool) {
        if (tool.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
    }

    public static float checkMiningSpeed(BlockState state, Player player, BlockGetter world, BlockPos pos, java.util.function.Supplier<Float> defaultProgress) {
        if (player.isCreative()) {
            return defaultProgress.get();
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            return defaultProgress.get();
        }
        // Mining speed is extremely slow if not using Nibirium Pickaxe
        return defaultProgress.get() * 0.03F;
    }
}
