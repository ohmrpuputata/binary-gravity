package com.example.alieninvasion.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * "Облучение" (lethal irradiation) — the 8-block zone of a pure-radiation block, or
 * carrying a raw radiation crystal without a hazmat suit. Once applied it sticks
 * until the victim dies. Cosmetic marker; the killing damage (1 every second, no
 * cap) is dealt by {@link com.example.alieninvasion.logic.RadiationFieldManager}.
 * Icon: skull.
 */
public class IrradiationEffect extends MobEffect {
    public IrradiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A4A1A);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            // Lethal: 1 damage every second, with no cap, until the victim dies.
            // The field manager applies this with a very long duration, so once you've
            // taken a fatal dose it sticks - exactly as the design spec wants.
            if (entity.tickCount % 20 == 0) {
                entity.hurt(entity.damageSources().magic(), 1.0F);
            }
            if (entity.level() instanceof ServerLevel sl && entity.tickCount % 4 == 0) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.SCULK_SOUL, entity.getX(), entity.getY() + h * 0.6, entity.getZ(),
                        2, w, h * 0.5, w, 0.01);
                sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, entity.getX(), entity.getY() + h, entity.getZ(),
                        1, w * 0.5, 0.1, w * 0.5, 0.0);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
