package com.example.alieninvasion.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class AntiGravityEffect extends MobEffect {
    public AntiGravityEffect() {
        super(MobEffectCategory.NEUTRAL, 0x00E6E6);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Slowly float upward by modifying vertical motion
        entity.setDeltaMovement(entity.getDeltaMovement().x, 0.15D * (amplifier + 1), entity.getDeltaMovement().z);
        entity.hurtMarked = true;
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
