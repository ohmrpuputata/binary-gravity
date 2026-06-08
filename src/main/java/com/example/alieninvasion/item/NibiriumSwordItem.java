package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * Nibirium sword. Netherite-grade, plus a "resonant cleave": every hit sends a
 * short shockwave that also damages nearby hostiles/aliens - the sword's
 * interesting effect (the design spec wanted an alternate attack on the swords).
 */
public class NibiriumSwordItem extends SwordItem {
    public NibiriumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            for (Mob nearby : attacker.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(2.5D),
                    e -> e != target && e != attacker && e.isAlive()
                            && (e instanceof Enemy || AlienUtils.isAlliedTo(null, e)))) {
                nearby.hurt(attacker.damageSources().mobAttack(attacker), 4.0F);
            }
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5D, target.getZ(),
                        4, 0.6D, 0.3D, 0.6D, 0.0D);
            }
        }
        return result;
    }
}
