package com.example.alieninvasion.effect;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PlatinumHealthBoostEffect extends MobEffect {
    public PlatinumHealthBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x55FFFF); // cyan/light blue
        // Adds +2.0 max health per level (amplifier + 1)
        this.addAttributeModifier(Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_health_boost"),
                2.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.level() instanceof ServerLevel sl && entity.tickCount % 10 == 0) {
            double w = entity.getBbWidth() * 0.5;
            double h = entity.getBbHeight();
            sl.sendParticles(ParticleTypes.HEART,
                    entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                    1, w, h * 0.4, w, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
