package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * Plasma sword: a superheated energy blade. Every hit ignites the target, adding
 * a burning damage-over-time on top of the strike - its niche is chipping swarms
 * and forcing mobs to break off. (The design spec wanted each metal sword to have
 * its own identity instead of being a plain stat-stick.)
 */
public class PlasmaSwordItem extends SwordItem {
    public PlasmaSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            target.setRemainingFireTicks(100);
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5D, target.getZ(),
                        8, 0.3D, 0.4D, 0.3D, 0.02D);
            }
        }
        return result;
    }
}
