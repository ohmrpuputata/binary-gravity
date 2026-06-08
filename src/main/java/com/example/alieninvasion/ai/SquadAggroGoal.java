package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.EnumSet;

public class SquadAggroGoal extends Goal {
    private final Mob mob;
    private final double range;
    private int shareTimer;

    public SquadAggroGoal(Mob mob, double range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null;
    }

    // Keep running while we have a target so the hive mind keeps re-broadcasting
    // to allies that wander into range or were spawned after the initial alert.
    @Override
    public boolean canContinueToUse() {
        return this.mob.getTarget() != null;
    }

    @Override
    public void start() {
        this.shareTimer = 0;
        broadcast();
    }

    @Override
    public void tick() {
        if (--this.shareTimer <= 0) {
            broadcast();
            this.shareTimer = 20; // re-share once per second
        }
    }

    private void broadcast() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || AlienUtils.isAlliedTo(this.mob, target))
            return;

        AABB searchBox = this.mob.getBoundingBox().inflate(range, 10.0D, range);
        List<Mob> nearbyAliens = this.mob.level().getEntitiesOfClass(Mob.class, searchBox,
                entity -> AlienUtils.isAlliedTo(this.mob, entity) && entity != this.mob);

        for (Mob alien : nearbyAliens) {
            if (alien.getTarget() == null || !alien.getTarget().isAlive()) {
                alien.setTarget(target);
            }
        }
    }
}
