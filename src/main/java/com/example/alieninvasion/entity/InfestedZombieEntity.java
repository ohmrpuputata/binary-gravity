package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Зараженный зомби (Infested Zombie):
 * Имеет костяной нарост-щит на руке, блокирующий 50% входящего урона спереди.
 */
public class InfestedZombieEntity extends Zombie {

    public InfestedZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            Vec3 lookVec = this.getViewVector(1.0F);
            Vec3 toAttacker = attacker.position().subtract(this.position()).normalize();
            double dot = lookVec.dot(toAttacker);
            
            // Если атакующий находится спереди (скалярное произведение > 0)
            if (dot > 0.0) {
                amount *= 0.5F; // Блокируем 50% урона
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected boolean isSunBurnTick() {
        return false; // Зараженные зомби не горят на солнце
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
