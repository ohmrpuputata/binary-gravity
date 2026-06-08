package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * Uranium sword: a "dirty" radioactive blade. Every hit irradiates the target
 * (Radiation + Poison damage-over-time) - armour-bypassing attrition that is its
 * niche against high-HP aliens, distinct from the plasma (burn) and iridium
 * (control) blades.
 */
public class UraniumSwordItem extends SwordItem {
    public UraniumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION), 120, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, true));
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 0.6D, target.getZ(),
                        10, 0.3D, 0.4D, 0.3D, 0.0D);
            }
        }
        return result;
    }
}
