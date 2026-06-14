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

        // Emerald trade for pills
        if (stack.is(net.minecraft.world.item.Items.EMERALD)) {
            boolean hasResonator = player.getInventory().contains(new ItemStack(ItemRegistry.EMERADIUM_RESONATOR));
            if (!hasResonator) {
                player.displayClientMessage(Component.literal(
                        "§c[Черный рынок] Сигнал слишком слаб... требуется Эмерадиевый резонатор"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (stack.getCount() < 4) {
                player.displayClientMessage(Component.literal("§c[Черный рынок] Нужно 4 изумруда."), true);
                return ItemInteractionResult.CONSUME;
            }
            stack.shrink(4);
            ItemStack reward = new ItemStack(level.random.nextBoolean() ? ItemRegistry.RAD_PILLS : ItemRegistry.INFECTION_PILLS, 1);
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
            level.playSound(null, pos, SoundEvents.VILLAGER_TRADE, SoundSource.BLOCKS, 1.0F, 0.75F);
            player.displayClientMessage(Component.literal("§a[Черный рынок] Сделка выполнена."), true);
            return ItemInteractionResult.CONSUME;
        }

        if (!stack.is(ItemRegistry.ALIEN_BATTERY) && !stack.is(ItemRegistry.COSMIC_SHARD)) {
            player.displayClientMessage(Component.literal(
                    "§6[Черный рынок] Оплата: alien battery или cosmic shard. ПКМ валютой покупает припасы."), true);
            return ItemInteractionResult.CONSUME;
        }

        boolean hasResonator = player.getInventory().contains(new ItemStack(ItemRegistry.EMERADIUM_RESONATOR));
        boolean paidShard = stack.is(ItemRegistry.COSMIC_SHARD);
        int price = paidShard ? 1 : (hasResonator ? 6 : 8);

        if (stack.getCount() < price) {
            player.displayClientMessage(Component.literal("§c[Черный рынок] Нужно " + price + " шт."), true);
            return ItemInteractionResult.CONSUME;
        }
        stack.shrink(price);

        ItemStack reward;
        if (paidShard) {
            int count = (hasResonator && level.random.nextFloat() < 0.20F) ? 2 : 1;
            reward = new ItemStack(ItemRegistry.COSMIC_INGOT, count);
        } else {
            reward = new ItemStack(ItemRegistry.COSMIC_SHARD, 1);
        }

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
            boolean hasResonator = player.getInventory().contains(new ItemStack(ItemRegistry.EMERADIUM_RESONATOR));
            if (hasResonator) {
                player.displayClientMessage(Component.literal(
                        "§6[Черный рынок] 6 alien battery -> 1 cosmic shard, 1 cosmic shard -> cosmic ingot (20% шанс x2), 4 emeralds -> 1 random pill."), true);
            } else {
                player.displayClientMessage(Component.literal(
                        "§6[Черный рынок] 8 alien battery -> 1 cosmic shard, 1 cosmic shard -> cosmic ingot. (Резонатор дает скидки и новые сделки)"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
