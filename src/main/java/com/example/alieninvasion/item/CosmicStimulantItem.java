package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Космический Стимулятор (Cosmic Stimulant):
 * Боевой "стим" - аварийная инъекция на случай, когда рой давит. Мгновенно лечит
 * и дает мощный набор бафов (защита, поглощение, регенерация, скорость, сила).
 * Дешевле, чем Био-Сыворотка, чтобы давать стабильное облегчение в бою.
 */
public class CosmicStimulantItem extends Item {
    public CosmicStimulantItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            player.heal(8.0F);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 2, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, false, true));
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        player.getCooldowns().addCooldown(this, 200); // 10s so it can't be spammed mid-fight
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
