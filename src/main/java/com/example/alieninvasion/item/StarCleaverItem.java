package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;

// Star Cleaver: a super cosmic battle-axe. Hits cleave into nearby enemies and
// deal bonus damage to the swarm. Keeps the axe's tree-stripping/utility.
public class StarCleaverItem extends AxeItem {
    public StarCleaverItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && !target.level().isClientSide) {
            // Cleave: splash damage to other foes around the main target.
            AABB area = target.getBoundingBox().inflate(2.0D);
            for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != target && e != attacker && e.isAlive()
                            && !(e instanceof net.minecraft.world.entity.player.Player))) {
                other.hurt(other.damageSources().mobAttack(attacker), 6.0F);
            }
            // Extra punch into the swarm itself.
            if (AlienUtils.isAlliedTo(attacker, target)) {
                target.hurt(target.damageSources().magic(), 5.0F);
            }
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.4, 0.2, 0.4, 0.0);
            }
        }
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
