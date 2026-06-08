package com.example.alieninvasion.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import net.minecraft.util.RandomSource;

public class RandomFlyGoal extends Goal {
    private final Mob mob;

    public RandomFlyGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.mob.getMoveControl().hasWanted() ? false : this.mob.getRandom().nextInt(50) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getMoveControl().hasWanted();
    }

    @Override
    public void start() {
        RandomSource random = this.mob.getRandom();
        double x = this.mob.getX() + (random.nextDouble() - 0.5D) * 32.0D;
        double y = this.mob.getY() + (random.nextDouble() - 0.5D) * 16.0D;
        double z = this.mob.getZ() + (random.nextDouble() - 0.5D) * 32.0D;
        this.mob.getMoveControl().setWantedPosition(x, y, z, 1.0D);
    }
}
