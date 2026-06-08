package com.example.alieninvasion.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * "Сильная радиация" — the 16-block zone of a pure-radiation block. This effect is
 * a cosmetic/HUD marker only; the actual damage (2 every 8s) is dealt by
 * {@link com.example.alieninvasion.logic.RadiationFieldManager} so the tiers stay
 * exactly as the design spec describes. Icon: radiation + skull.
 */
public class StrongRadiationEffect extends MobEffect {
    public StrongRadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0xE08A1E);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (!level.isClientSide) {
            // 2 damage every 8s (the field manager keeps this applied only while in
            // the 16-block strong zone, so it stops when you get clear).
            if (entity.tickCount % 160 == 0) {
                entity.hurt(entity.damageSources().magic(), 2.0F);
            }
            if (level instanceof ServerLevel sl && entity.tickCount % 6 == 0) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        2, w, h * 0.5, w, 0.02);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
