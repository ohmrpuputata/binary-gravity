package com.example.alieninvasion.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * "Облучение" — накапливается в RadiationManager по порогам дозы.
 * Блокирует исцеление (через Mixin в PlayerMixin) и снимает регенерацию.
 */
public class IrradiationEffect extends MobEffect {
    public IrradiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A4A1A);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            // Suppresses active regeneration so it cannot counteract irradiation.
            boolean hasFullEmeradium = false;
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                hasFullEmeradium = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_HELMET)
                        && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_CHESTPLATE)
                        && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_LEGGINGS)
                        && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_BOOTS);
            }
            if (!hasFullEmeradium && entity.hasEffect(MobEffects.REGENERATION)) {
                entity.removeEffect(MobEffects.REGENERATION);
            }
            if (entity.level() instanceof ServerLevel sl && entity.tickCount % 8 == 0) {
                double w = entity.getBbWidth() * 0.6;
                double h = entity.getBbHeight();
                sl.sendParticles(ParticleTypes.SCULK_SOUL,
                        entity.getX(), entity.getY() + h * 0.6, entity.getZ(),
                        1, w, h * 0.5, w, 0.01);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
