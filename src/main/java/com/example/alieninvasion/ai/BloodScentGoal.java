package com.example.alieninvasion.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class BloodScentGoal extends NearestAttackableTargetGoal<Player> {
    private final double bloodScentRange;

    public BloodScentGoal(Mob mob, double range) {
        super(mob, Player.class, true, true); // true, true = check visibility, check navigation (maybe false for
                                              // scent?)
        this.bloodScentRange = range;
    }

    @Override
    protected AABB getTargetSearchArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(bloodScentRange, 16.0D, bloodScentRange);
    }

    @Override
    protected void findTarget() {
        // Custom finding logic to ignore visibility if bleeding, or just use larger
        // range
        // We defer to super but with a specific predicate
        super.findTarget();

        // If we found a target, ensure they are actually bleeding
        if (this.target != null) {
            if (this.target.getHealth() >= this.target.getMaxHealth() * 0.5f) {
                this.target = null; // Too healthy, can't smell 'em
            }
        }
    }
}
