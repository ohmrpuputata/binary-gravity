package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * Iridium sword: a dense, heavy blade. Every hit delivers a brutal knockback and
 * briefly slows the target - a control weapon for kiting and breaking pursuit,
 * distinct from the uranium (attrition) and plasma (burn) blades.
 */
public class IridiumSwordItem extends SwordItem {
    public IridiumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            target.knockback(0.7D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6D, target.getZ(),
                        12, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
        return result;
    }
}
