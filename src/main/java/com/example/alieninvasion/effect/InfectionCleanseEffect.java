package com.example.alieninvasion.effect;

import com.example.alieninvasion.logic.InfectionManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class InfectionCleanseEffect extends MobEffect {
    public InfectionCleanseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF5588); // glowing pinkish/magenta
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            InfectionManager.reduceMeter(player, 0.1F);
            
            // Visual feedback: pinkish/magenta glow sparkles
            if (entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel sl) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.GLOW,
                        entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        2, w, h * 0.5, w, 0.0);
            }

            // Automatically cure and clean up when infection meter drops to 0
            if (InfectionManager.getMeter(player) <= 0.01F) {
                player.addTag("CuredByAntidote");
                player.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(com.example.alieninvasion.registry.ModEffects.INFECTION));
                player.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(com.example.alieninvasion.registry.ModEffects.INFECTION_CLEANSE));
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
