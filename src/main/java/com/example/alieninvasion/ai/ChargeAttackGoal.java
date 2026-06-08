package com.example.alieninvasion.ai;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// Heavy charge: when the target is at mid range with line of sight, the mob
// sprints straight at it and slams on contact (bonus damage + big knockback).
// Designed for the Brute, but works for any PathfinderMob.
public class ChargeAttackGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final float impactDamage;
    private LivingEntity target;
    private int cooldown;
    private int chargeTime;
    private boolean hasHit;

    public ChargeAttackGoal(PathfinderMob mob, double speedModifier, float impactDamage) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.impactDamage = impactDamage;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.target = this.mob.getTarget();
        if (this.target == null || !this.target.isAlive() || !this.mob.onGround()) {
            return false;
        }
        double distSqr = this.mob.distanceToSqr(this.target);
        if (distSqr < 36.0D || distSqr > 576.0D) { // only between 6 and 24 blocks
            return false;
        }
        if (!this.mob.getSensing().hasLineOfSight(this.target)) {
            return false;
        }
        return this.mob.getRandom().nextInt(20) == 0;
    }

    @Override
    public void start() {
        this.chargeTime = 0;
        this.hasHit = false;
        this.mob.setSprinting(true);
        this.mob.getNavigation().moveTo(this.target, this.speedModifier);
        this.mob.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.8F);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.hasHit && this.chargeTime < 60 && this.target != null && this.target.isAlive()
                && this.mob.distanceToSqr(this.target) > 4.0D;
    }

    @Override
    public void tick() {
        this.chargeTime++;
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        this.mob.getNavigation().moveTo(this.target, this.speedModifier);

        if (this.mob.distanceToSqr(this.target) <= 6.25D) {
            this.hasHit = true;
            double dx = this.target.getX() - this.mob.getX();
            double dz = this.target.getZ() - this.mob.getZ();
            this.target.knockback(2.5D, -dx, -dz);
            this.target.hurt(this.mob.damageSources().mobAttack(this.mob), this.impactDamage);
            this.mob.level().playSound(null, this.mob.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                    this.mob.getSoundSource(), 1.4F, 0.5F);
        }
    }

    @Override
    public void stop() {
        this.mob.setSprinting(false);
        this.cooldown = 160; // 8s between charges
        this.target = null;
    }
}
