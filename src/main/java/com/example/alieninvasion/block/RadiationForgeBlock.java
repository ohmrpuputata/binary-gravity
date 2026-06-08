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
 * Radiation Forge: uses ambient radiation as heat to smelt the mod's ores with
 * no fuel. Its niche is processing the cosmic line (and raw metals) for free,
 * which matters when you mine cosmic in bulk for apex gear.
 */
public class RadiationForgeBlock extends Block {
    public RadiationForgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Item out = null;
        if (stack.is(ItemRegistry.COSMIC_SHARD)) {
            out = ItemRegistry.COSMIC_INGOT;
        } else if (stack.is(ItemRegistry.RAW_PLATINUM)) {
            out = ItemRegistry.PLATINUM_INGOT;
        } else if (stack.is(ItemRegistry.RAW_PALLADIUM)) {
            out = ItemRegistry.PALLADIUM_INGOT;
        }
        if (out == null) {
            player.displayClientMessage(Component.literal(
                    "§a[Радиокузня] Плавит без топлива: cosmic shard / raw platinum / raw palladium."), true);
            return ItemInteractionResult.CONSUME;
        }
        int count = stack.getCount();
        stack.shrink(count);
        ItemStack result = new ItemStack(out, count);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.9F, 0.8F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    14, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        player.displayClientMessage(Component.literal("§a[Радиокузня] Выплавлено: " + count + "."), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(
                    "§a[Радиокузня] Плавка без топлива: cosmic shard -> ingot, raw metal -> ingot. ПКМ сырьём."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
