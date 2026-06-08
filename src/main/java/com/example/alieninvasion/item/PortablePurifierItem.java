package com.example.alieninvasion.item;

import com.example.alieninvasion.logic.ContaminationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PortablePurifierItem extends Item {
    public PortablePurifierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            int cleaned = 0;
            BlockPos center = player.blockPosition();
            for (int i = 0; i < 48; i++) {
                BlockPos pos = center.offset(level.random.nextInt(13) - 6, level.random.nextInt(7) - 3,
                        level.random.nextInt(13) - 6);
                BlockState clean = ContaminationRules.cleanStateFor(level.getBlockState(pos));
                if (clean != null) {
                    level.setBlockAndUpdate(pos, clean);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, 3, 0.2D, 0.2D, 0.2D, 0.0D);
                    cleaned++;
                }
            }
            // Also decontaminates the user: shaves off some accumulated dose.
            com.example.alieninvasion.logic.RadiationManager.reduceDose(player, 30.0F);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 1.45F);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(Math.max(1, cleaned), player, LivingEntity.getSlotForHand(hand));
            }
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
