package com.example.alieninvasion.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

public class FollowLeaderGoal extends Goal {
    private final Mob mob;
    private final Class<? extends Mob> leaderClass;
    private Mob leader;
    private final double speedModifier;

    public FollowLeaderGoal(Mob mob, Class<? extends Mob> leaderClass, double speedModifier) {
        this.mob = mob;
        this.leaderClass = leaderClass;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.leader != null && this.leader.isAlive())
            return true;

        List<? extends Mob> leaders = this.mob.level().getEntitiesOfClass(this.leaderClass,
                this.mob.getBoundingBox().inflate(15.0D));
        if (!leaders.isEmpty()) {
            this.leader = leaders.get(0);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.leader != null && this.leader.isAlive() && this.mob.distanceToSqr(this.leader) < 256.0D;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        if (this.leader != null) {
            if (this.mob.distanceToSqr(this.leader) > 10.0D) {
                this.mob.getNavigation().moveTo(this.leader, this.speedModifier);
            } else {
                this.mob.getNavigation().stop(); // Wait near leader
            }
        }
    }
}
