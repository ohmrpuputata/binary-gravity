package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * Rally Banner: a co-op team tool. Right-click to rally everyone within 24 blocks
 * (yourself included) - Regeneration, Resistance, Speed and Absorption for 30s.
 * Built for online last stands; long cooldown so it's a clutch button, not a toggle.
 */
public class RallyBannerItem extends Item {
    public RallyBannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            int rallied = 0;
            for (Player ally : level.getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(24.0D), Player::isAlive)) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1, false, true));
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1, false, true));
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, false, true));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1, false, true));
                ally.displayClientMessage(Component.literal("§6[Знамя] §fВас воодушевил " + player.getName().getString() + "!"), true);
                rallied++;
            }
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(),
                        60, 1.5, 1.0, 1.5, 0.3);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.2F);
            player.displayClientMessage(Component.literal("§6[Знамя сбора] Воодушевлено бойцов: §f" + rallied), false);
        }

        player.getCooldowns().addCooldown(this, 1200); // 60s
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
