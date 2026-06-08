package com.example.alieninvasion.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class AlienLeapGoal extends Goal {
    private final Mob mob;
    private LivingEntity target;
    private final float leapMotionY;
    private int cooldown;

    public AlienLeapGoal(Mob mob, float leapMotionY) {
        this.mob = mob;
        this.leapMotionY = leapMotionY;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.target = this.mob.getTarget();
        if (this.target == null) {
            return false;
        }
        double distanceSqr = this.mob.distanceToSqr(this.target);
        // Leap if between 4 and 16 blocks away (too far for melee, close enough to
        // pounce)
        return distanceSqr >= 16.0D && distanceSqr <= 256.0D && this.mob.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.onGround();
    }

    @Override
    public void start() {
        Vec3 vec3 = this.mob.getDeltaMovement();
        Vec3 vec31 = new Vec3(this.target.getX() - this.mob.getX(), 0.0D, this.target.getZ() - this.mob.getZ());
        if (vec31.lengthSqr() > 1.0E-7D) {
            vec31 = vec31.normalize().scale(0.8D).add(vec3.scale(0.2D)); // Speed scale
        }

        this.mob.setDeltaMovement(vec31.x, this.leapMotionY, vec31.z);
        this.cooldown = 40; // 2 seconds cooldown
    }
}
