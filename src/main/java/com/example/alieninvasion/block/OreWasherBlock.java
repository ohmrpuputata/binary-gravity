package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ore Washer: scrubs raw precious metal and doubles the ingot yield (raw -> 2x
 * ingot) - the classic ore-doubler, here accelerating the platinum/palladium ->
 * nibirium economy. Output is ingots (terminal), so it cannot be looped.
 */
public class OreWasherBlock extends Block {
    public OreWasherBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Item out = null;
        if (stack.is(ItemRegistry.RAW_PLATINUM)) {
            out = ItemRegistry.PLATINUM_INGOT;
        } else if (stack.is(ItemRegistry.RAW_PALLADIUM)) {
            out = ItemRegistry.PALLADIUM_INGOT;
        }
        if (out == null) {
            player.displayClientMessage(Component.literal(
                    "§b[Промывщик] Заложите raw platinum/palladium: даёт 2x слитка."), true);
            return ItemInteractionResult.CONSUME;
        }
        int count = stack.getCount();
        stack.shrink(count);
        ItemStack result = new ItemStack(out, count * 2);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.9F, 1.1F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    16, 0.3D, 0.2D, 0.3D, 0.1D);
        }
        player.displayClientMessage(Component.literal("§a[Промывщик] +" + (count * 2) + " слитка."), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(
                    "§b[Промывщик] raw platinum/palladium -> 2x слитка (удвоение). ПКМ сырьём."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
