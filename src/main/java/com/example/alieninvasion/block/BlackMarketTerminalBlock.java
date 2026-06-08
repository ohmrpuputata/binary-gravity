package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlackMarketTerminalBlock extends Block {
    public BlackMarketTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(ItemRegistry.ALIEN_SCRAP) && !stack.is(ItemRegistry.COSMIC_CREDIT)) {
            player.displayClientMessage(Component.literal(
                    "§6[Черный рынок] Оплата: alien scrap или cosmic credit. ПКМ валютой покупает припасы."), true);
            return ItemInteractionResult.CONSUME;
        }
        boolean paidCredit = stack.is(ItemRegistry.COSMIC_CREDIT);
        int price = paidCredit ? 1 : 8;
        if (stack.getCount() < price) {
            player.displayClientMessage(Component.literal("§c[Черный рынок] Нужно " + price + " шт."), true);
            return ItemInteractionResult.CONSUME;
        }
        stack.shrink(price);
        ItemStack reward = paidCredit
                ? new ItemStack(ItemRegistry.PLASMA_CORE, 1)
                : new ItemStack(ItemRegistry.DRILL_FUEL_CELL, 2);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        level.playSound(null, pos, SoundEvents.VILLAGER_TRADE, SoundSource.BLOCKS, 1.0F, 0.75F);
        player.displayClientMessage(Component.literal("§a[Черный рынок] Сделка выполнена."), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(
                    "§6[Черный рынок] 8 alien scrap -> 2 drill fuel cell, 1 cosmic credit -> plasma core."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
