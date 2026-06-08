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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Alien Recycler: feeds salvaged alien scrap back into usable alien alloy,
 * closing the combat -> material loop (kill aliens -> scrap -> recycler -> alloy
 * -> bio-gear). Right-click with alien scrap; processes every full batch at once.
 */
public class AlienRecyclerBlock extends Block {
    private static final int SCRAP_PER_ALLOY = 6;

    public AlienRecyclerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(ItemRegistry.ALIEN_SCRAP)) {
            player.displayClientMessage(Component.literal(
                    "§5[Переработчик] Заложите alien scrap: " + SCRAP_PER_ALLOY + " лома -> 1 alien alloy."), true);
            return ItemInteractionResult.CONSUME;
        }
        if (stack.getCount() < SCRAP_PER_ALLOY) {
            player.displayClientMessage(Component.literal("§c[Переработчик] Нужно минимум " + SCRAP_PER_ALLOY + " лома."), true);
            return ItemInteractionResult.CONSUME;
        }
        int batches = stack.getCount() / SCRAP_PER_ALLOY;
        stack.shrink(batches * SCRAP_PER_ALLOY);
        ItemStack out = new ItemStack(ItemRegistry.ALIEN_ALLOY, batches);
        if (!player.getInventory().add(out)) {
            player.drop(out, false);
        }
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.9F, 0.7F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        player.displayClientMessage(Component.literal("§a[Переработчик] +" + batches + " alien alloy."), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(
                    "§5[Переработчик] " + SCRAP_PER_ALLOY + " alien scrap -> 1 alien alloy. ПКМ ломом."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
