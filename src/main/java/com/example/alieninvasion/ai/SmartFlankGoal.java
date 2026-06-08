package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

public class SmartFlankGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private boolean strafingClockwise;
    private int strafeTime;

    public SmartFlankGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null)
            return false;

        // If target is low HP, just attack (don't flank)
        if (target.getHealth() < target.getMaxHealth() * 0.3)
            return false;

        // Check allies count
        List<Mob> allies = this.mob.level().getEntitiesOfClass(Mob.class, this.mob.getBoundingBox().inflate(15.0D),
                entity -> AlienUtils.isAlliedTo(this.mob, entity));

        // If we have a pack (>= 3 allies), interact with standard attack goal instead
        // (this goal returns false)
        // BUT, we want to flank IF we are ALONE or FEW.
        return allies.size() < 3;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.strafingClockwise = this.mob.getRandom().nextBoolean();
        this.strafeTime = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null)
            return;

        double distSqr = this.mob.distanceToSqr(target);
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Maintain distance of ~10 blocks (100 sqr)
        if (distSqr < 64.0D) {
            // Too close, back up while strafing
            this.mob.getNavigation().moveTo(this.mob.getX() - (target.getX() - this.mob.getX()), this.mob.getY(),
                    this.mob.getZ() - (target.getZ() - this.mob.getZ()), this.speedModifier);
        } else if (distSqr > 144.0D) {
            // Too far, approach
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        } else {
            // Circle/Strafe
            this.strafeTime++;
            if (this.strafeTime > 20 && this.mob.getRandom().nextInt(20) == 0) {
                this.strafingClockwise = !this.strafingClockwise;
                this.strafeTime = 0;
            }

            // Calculate strafe pos (simplified)
            // Ideally we'd use a proper strafe pathfinder, but for now random stroll mixed
            // with facing helps,
            // or just standard move control strafing if available.
            // Since we are standard Mob, we don't have direct strafe input.
            // We will sim strafe by moving perpendicular.

            double dx = target.getX() - this.mob.getX();
            double dz = target.getZ() - this.mob.getZ();
            double strafeX = this.strafingClockwise ? -dz : dz;
            double strafeZ = this.strafingClockwise ? dx : -dx;

            this.mob.getNavigation().moveTo(this.mob.getX() + strafeX, this.mob.getY(), this.mob.getZ() + strafeZ,
                    this.speedModifier);
        }
    }
}
