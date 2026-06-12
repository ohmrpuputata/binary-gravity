package com.example.alieninvasion.effect;

import com.example.alieninvasion.logic.RadiationManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class RadiationCleanseEffect extends MobEffect {
    public RadiationCleanseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x55FF55); // glowing light green
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            RadiationManager.reduceDose(player, 0.1F);
            
            // Visual feedback: happy green sparkles
            if (entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel sl) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        2, w, h * 0.5, w, 0.0);
            }

            // Automatically clean up when dose drops to 0
            if (RadiationManager.getDose(player) <= 0.01F) {
                player.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(com.example.alieninvasion.registry.ModEffects.RADIATION_CLEANSE));
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
