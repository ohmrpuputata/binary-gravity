package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class TacticalRetreatGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private double retreatX;
    private double retreatY;
    private double retreatZ;

    public TacticalRetreatGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() == null)
            return false;
        if (this.mob.getHealth() > this.mob.getMaxHealth() * 0.25)
            return false; // Only retreat if < 25% HP

        // Find retreat pos
        Vec3 pos = findRetreatPosition();
        if (pos == null)
            return false;

        this.retreatX = pos.x;
        this.retreatY = pos.y;
        this.retreatZ = pos.z;
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.retreatX, this.retreatY, this.retreatZ, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() && this.mob.getHealth() < this.mob.getMaxHealth() * 0.30;
    }

    private Vec3 findRetreatPosition() {
        // Try to find a Brute? (Simplified: Just run AWAY from target for now, or find
        // safe spot)
        // TODO: Scan for Brute or Hive

        if (this.mob.getTarget() != null && this.mob instanceof net.minecraft.world.entity.PathfinderMob) {
            // Run away from target
            return DefaultRandomPos.getPosAway((net.minecraft.world.entity.PathfinderMob) this.mob, 16, 7,
                    this.mob.getTarget().position());
        }
        return null;
    }
}
