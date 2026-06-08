package com.example.alieninvasion.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

public class XrayTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    public XrayTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee) {
        super(mob, targetClass, mustSee);
    }

    public XrayTargetGoal(Mob mob, Class<T> targetClass, boolean mustSee, boolean mustReach) {
        super(mob, targetClass, mustSee, mustReach);
    }

    @Override
    protected AABB getTargetSearchArea(double range) {
        return this.mob.getBoundingBox().inflate(range, range, range); // Full 3D range
    }

    @Override
    protected double getFollowDistance() {
        return super.getFollowDistance() * 2.0D; // Double detection range
    }
}
