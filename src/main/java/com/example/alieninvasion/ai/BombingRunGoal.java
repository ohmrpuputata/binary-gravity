package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.UfoEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import java.util.EnumSet;
import net.minecraft.world.phys.Vec3;

public class BombingRunGoal extends Goal {
    private final UfoEntity ufo;
    private int attackTimer;

    public BombingRunGoal(UfoEntity ufo) {
        this.ufo = ufo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.ufo.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.attackTimer = 0;
    }

    @Override
    public void stop() {
        this.ufo.setAggressive(false);
    }

    @Override
    public void tick() {
        LivingEntity target = this.ufo.getTarget();
        if (target == null)
            return;

        double distSqr = this.ufo.distanceToSqr(target);
        boolean canSee = this.ufo.getSensing().hasLineOfSight(target);

        // Hover pattern: Try to stay 10 blocks above target
        Vec3 hoverPos = target.position().add(0, 10, 0);
        this.ufo.getMoveControl().setWantedPosition(hoverPos.x, hoverPos.y, hoverPos.z, 1.0D);
        this.ufo.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distSqr < 400.0D && canSee) { // Within 20 blocks and visible
            this.ufo.setAggressive(true);
            this.attackTimer++;

            // Bomb every 2 seconds (40 ticks)
            if (this.attackTimer >= 40) {
                this.ufo.level().broadcastEntityEvent(this.ufo, (byte) 4); // Attack animation (optional)

                if (!this.ufo.level().isClientSide) {
                    PrimedTnt tnt = net.minecraft.world.entity.EntityType.TNT.create(this.ufo.level());
                    if (tnt != null) {
                        tnt.moveTo(this.ufo.getX(), this.ufo.getY() - 1.0D, this.ufo.getZ(), 0.0F, 0.0F);
                        tnt.setFuse(40); // 2 second fuse
                        this.ufo.level().addFreshEntity(tnt);
                    }
                }
                this.attackTimer = 0;
            }
        } else {
            this.ufo.setAggressive(false);
        }
    }
}
