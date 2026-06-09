package com.example.alieninvasion.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class InfectionEffect extends MobEffect {
    public InfectionEffect() {
        super(MobEffectCategory.HARMFUL, 0x5D8A00); // Alien green
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (!level.isClientSide) {
            // Creepy alien spore aura around the host.
            if (level instanceof ServerLevel sl && entity.tickCount % 5 == 0) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.WARPED_SPORE,
                        entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        2 + amplifier, w, h * 0.5, w, 0.0);
                if (level.random.nextInt(18) == 0) {
                    sl.sendParticles(ParticleTypes.SCULK_SOUL,
                            entity.getX(), entity.getY() + h, entity.getZ(),
                            1, 0.2, 0.2, 0.2, 0.01);
                }
                if (amplifier >= 1 && level.random.nextInt(24) == 0) {
                    sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                            entity.getX(), entity.getY() + h * 0.6, entity.getZ(),
                            3, w, h * 0.4, w, 0.02);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
